package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scheduler screens replaced by Activity, Tasks, the task editor and Configuration (#993, S11) are gone: none of
 * them is a screen of the module any more, and no kept screen nor global descriptor opens, includes or names them
 */
class RemovedSchedulerScreensTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final List<String> REMOVED = List.of("new-scheduler-calendar", "new-scheduler-server",
    "new-scheduler-task", "scheduler-calendars", "scheduler-dashboard", "scheduler-dialog",
    "scheduler-execution-data", "scheduler-flow-graph", "scheduler-management", "scheduler-runs", "scheduler-server",
    "scheduler-tasks", "update-scheduler-calendar", "update-scheduler-server", "update-scheduler-task");
  private static final List<String> GLOBAL = List.of("Actions.xml", "Email.xml", "Enumerated.xml", "Maintain.xml",
    "Permissions.xml", "Queries.xml", "Services.xml");

  @Test
  void theRemovedScreensDoNotExist() {
    for (String screen : REMOVED) {
      assertThat(getClass().getClassLoader().getResource(BASE + "screen/" + screen + ".xml")).as(screen).isNull();
    }
  }

  @Test
  void noKeptScreenOrGlobalDescriptorOpensIncludesOrNamesARemovedScreen() throws Exception {
    List<String> descriptors = new ArrayList<>();
    for (Path screen : keptScreens()) {
      descriptors.add("screen/" + screen.getFileName());
    }
    GLOBAL.forEach(file -> descriptors.add("global/" + file));
    assertThat(descriptors).contains("screen/scheduler-task-editor.xml", "screen/scheduler-activity.xml");

    for (String descriptor : descriptors) {
      String xml = read(descriptor);
      for (String screen : REMOVED) {
        // An attribute whose value is the screen: target-screen of an include, target of a screen action, an option
        assertThat(xml).as("%s names %s", descriptor, screen).doesNotContain("=\"" + screen + "\"");
      }
    }
  }

  private List<Path> keptScreens() throws IOException, URISyntaxException {
    URL folder = getClass().getClassLoader().getResource(BASE + "screen");
    assertThat(folder).isNotNull();
    try (Stream<Path> files = Files.list(Path.of(folder.toURI()))) {
      return files.filter(file -> file.getFileName().toString().endsWith(".xml")).sorted().toList();
    }
  }

  private String read(String resource) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(BASE + resource)) {
      assertThat(stream).as(resource).isNotNull();
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
