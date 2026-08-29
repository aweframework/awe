package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural (golden) checks on {@code screen/scheduler-execution-data.xml}: the database-mode
 * hidden id criteria are added, while the file-mode {@code path} criterion and the shared
 * {@code executionLogViewer} widget (still statically wired to {@code get-log-file}) stay exactly
 * as they were. Widget routing between modes is a runtime {@code serverAction} controller-attribute
 * swap issued by {@code DatabaseExecutionLogStore.applyViewerSelection} (verified in
 * {@code DatabaseExecutionLogStoreTest}), not a second widget or a screen-level dependency.
 */
class SchedulerExecutionDataScreenTest {

  private static final String RESOURCE = "application/awe-scheduler/screen/scheduler-execution-data.xml";

  @Test
  void hiddenIdCriteriaExistAlongsideTheUnchangedFileModePathCriterion() throws IOException {
    String xml = readScreen();

    assertTrue(xml.contains("<criteria id=\"path\" component=\"hidden\"/>"));
    assertTrue(xml.contains("<criteria id=\"execution-log-task-id\" component=\"hidden\"/>"));
    assertTrue(xml.contains("<criteria id=\"execution-log-execution-id\" component=\"hidden\"/>"));
  }

  @Test
  void executionLogViewerWidgetKeepsItsStaticFileModeWiring() throws IOException {
    String xml = readScreen();

    assertTrue(xml.contains("<widget type=\"log-viewer\" id=\"executionLogViewer\" style=\"expand scrollable black-log\" server-action=\"get-log-file\">"));
    assertTrue(xml.contains("<dependency source-type=\"value\" target-type=\"enable-autorefresh\" value=\"5\">"));
  }

  private String readScreen() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
      assertTrue(stream != null, RESOURCE + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
