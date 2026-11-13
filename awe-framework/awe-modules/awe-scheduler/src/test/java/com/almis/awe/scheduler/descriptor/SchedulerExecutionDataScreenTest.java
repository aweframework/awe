package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on {@code screen/scheduler-execution-data.xml}: the {@code executionLogViewer} widget reads
 * through the scheduler {@code get-execution-log} action (permission {@code scheduler.read}) by the hidden task and
 * execution id criteria, in both store modes, and the screen carries no client-side file path criterion.
 */
class SchedulerExecutionDataScreenTest {

  private static final String RESOURCE = "application/awe-scheduler/screen/scheduler-execution-data.xml";

  @Test
  void hiddenIdCriteriaExistAndNoFilePathCriterion() throws IOException {
    String xml = readScreen();

    assertFalse(xml.contains("<criteria id=\"path\""));
    assertTrue(xml.contains("<criteria id=\"execution-log-task-id\" component=\"hidden\"/>"));
    assertTrue(xml.contains("<criteria id=\"execution-log-execution-id\" component=\"hidden\"/>"));
  }

  @Test
  void executionLogViewerWidgetReadsThroughTheSchedulerAction() throws IOException {
    String xml = readScreen();

    assertTrue(xml.contains("<widget type=\"log-viewer\" id=\"executionLogViewer\" style=\"expand scrollable black-log\" server-action=\"get-execution-log\">"));
    assertFalse(xml.contains("get-log-file"));
    assertTrue(xml.contains("<dependency source-type=\"value\" target-type=\"enable-autorefresh\" value=\"5\">"));
  }

  private String readScreen() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
      assertNotNull(stream, RESOURCE + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
