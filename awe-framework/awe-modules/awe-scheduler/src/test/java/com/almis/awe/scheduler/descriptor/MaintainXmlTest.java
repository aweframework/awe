package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural (golden) checks on {@code global/Maintain.xml}: the three new database-mode
 * execution log targets exist with the expected key columns, {@code loadExecutionScreen}'s
 * locator variable is store-neutral, and the shared {@code purgeExecutionLogs} target (constant
 * {@code PURGE_EXECUTION_LOGS}) deletes the deferred rows too, besides the server-only
 * mark (#886) — a diff-based assertion
 * so any accidental future edit fails loudly.
 */
class MaintainXmlTest {

  private static final String RESOURCE = "application/awe-scheduler/global/Maintain.xml";

  // Whitespace-normalized: unaffected by incidental re-indentation, strict about content/order.
  private static final String PURGE_EXECUTIONS_DELETE =
    "<delete> <table id=\"AweSchExe\" /> <where> "
      + "<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/> "
      + "<filter left-field=\"ExeTsk\" condition=\"in\" right-variable=\"executions\"/> </where> "
      + "<variable id=\"taskId\" type=\"INTEGER\" name=\"taskId\" /> "
      + "<variable id=\"executions\" type=\"INTEGER\" name=\"executionId\" /> </delete>";

  @Test
  void purgeExecutionLogsDeletesTheDeferredRowsOfTheSameExecutionsBeforeTheExecutions() throws IOException {
    String target = target(readDescriptor(), "purgeExecutionLogs").replaceAll("\\s+", " ");

    String deferredDelete = PURGE_EXECUTIONS_DELETE.replace("AweSchExe\"", "AweSchExeExt\"");
    assertTrue(target.contains(deferredDelete), "deferred rows must be deleted with the same filters");
    assertTrue(target.contains(PURGE_EXECUTIONS_DELETE), "executions must still be deleted as before");
    assertTrue(target.indexOf(deferredDelete) < target.indexOf(PURGE_EXECUTIONS_DELETE));
  }

  @Test
  void deletingATaskDeletesTheDeferredRowsOfItsExecutions() throws IOException {
    String xml = readDescriptor();
    int start = xml.indexOf("<target name=\"DeleteSchedulerTask\"");
    String target = xml.substring(start, xml.indexOf("</target>", start)).replaceAll("\\s+", " ");

    assertTrue(target.contains("<delete> <table id=\"AweSchExeExt\"/> <where> <and> "
      + "<filter left-field=\"IdeTsk\" condition=\"in\" right-variable=\"IdeTsk\"/> </and> </where>"));
  }

  @Test
  void startTaskRemovesTheStaleDeferredRowsOfTheTaskBeforeInsertingTheExecution() throws IOException {
    String target = target(readDescriptor(), "startTask").replaceAll("\\s+", " ");

    int delete = target.indexOf("<delete> <table id=\"AweSchExeExt\" />");
    assertTrue(delete >= 0 && delete < target.indexOf("<insert>"), "stale rows must be deleted first");
    assertTrue(target.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/> "
      + "<filter condition=\"not exists\" query=\"getExecutionOfDeferredExecution\"/>"));
  }

  @Test
  void theEditorUpdateChecksTheParentsForCyclesBeforeChangingAnything() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"UpdateSchedulerTaskEditor\"");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "UpdateSchedulerTaskEditor target must exist");
    String target = xml.substring(start, end);

    int validation = target.indexOf("<serve service=\"validateTaskParents\">");
    assertTrue(validation >= 0, "UpdateSchedulerTaskEditor must validate the parents");
    assertTrue(validation < target.indexOf("<update"), "the validation must run before the first update");
  }

  @Test
  void loadExecutionScreenLocatorVariableIsRenamedToTheStoreNeutralName() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"loadExecutionScreen\"");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "loadExecutionScreen target must exist");
    String target = xml.substring(start, end);

    assertTrue(target.contains("<variable id=\"executionLogLocator\" type=\"STRING\" name=\"buttonValue\"/>"));
    assertFalse(target.contains("executionLogPath"));
  }

  @Test
  void insertExecutionLogLinesInsertsKeyedByTaskExecutionSectionAndSlot() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"insertExecutionLogLines\" server-only=\"true\">");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "insertExecutionLogLines target must exist");
    String target = xml.substring(start, end);

    assertTrue(target.contains("<insert multiple=\"true\">"));
    assertTrue(target.contains("<table id=\"AweSchExeLog\"/>"));
    assertTrue(target.contains("<field id=\"IdeTsk\" variable=\"taskId\" key=\"true\"/>"));
    assertTrue(target.contains("<field id=\"ExeTsk\" variable=\"executionId\" key=\"true\"/>"));
    assertTrue(target.contains("<field id=\"Src\" variable=\"origin\" key=\"true\"/>"));
    assertTrue(target.contains("<field id=\"Sec\" variable=\"section\" key=\"true\"/>"));
    assertTrue(target.contains("<field id=\"Slt\" variable=\"slot\" key=\"true\"/>"));
    assertTrue(target.contains("<field id=\"LogDat\" variable=\"logDate\"/>"));
    assertFalse(target.contains("<constant id=\"LogDat\" type=\"SYSTEM_TIMESTAMP\"/>"));
  }

  @Test
  void updateExecutionLogLinesUpdatesKeyedByTaskExecutionSectionAndSlot() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"updateExecutionLogLines\" server-only=\"true\">");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "updateExecutionLogLines target must exist");
    String target = xml.substring(start, end);

    assertTrue(target.contains("<update multiple=\"true\">"));
    assertTrue(target.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/>"));
    assertTrue(target.contains("<filter left-field=\"ExeTsk\" condition=\"eq\" right-variable=\"executionId\"/>"));
    assertTrue(target.contains("<filter left-field=\"Src\" condition=\"eq\" right-variable=\"origin\"/>"));
    assertTrue(target.contains("<filter left-field=\"Sec\" condition=\"eq\" right-variable=\"section\"/>"));
    assertTrue(target.contains("<filter left-field=\"Slt\" condition=\"eq\" right-variable=\"slot\"/>"));
    assertTrue(target.contains("<field id=\"LogDat\" variable=\"logDate\"/>"));
    assertFalse(target.contains("<constant id=\"LogDat\" type=\"SYSTEM_TIMESTAMP\"/>"));
  }

  @Test
  void purgeExecutionLogLinesDeletesKeyedByTaskAndExecution() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"purgeExecutionLogLines\" server-only=\"true\">");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "purgeExecutionLogLines target must exist");
    String target = xml.substring(start, end);

    assertTrue(target.contains("<delete multiple=\"true\">"));
    assertTrue(target.contains("<table id=\"AweSchExeLog\"/>"));
    assertTrue(target.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/>"));
    assertTrue(target.contains("<filter left-field=\"ExeTsk\" condition=\"eq\" right-variable=\"executionId\"/>"));
  }

  @Test
  void closeRunningExecutionUpdatesTheStatusOnlyOfARunningExecution() throws IOException {
    String target = target(readDescriptor(), "closeRunningExecution");

    assertTrue(target.contains("<table id=\"AweSchExe\" />"));
    assertTrue(target.contains("<field id=\"Sta\" variable=\"newStatus\"/>"));
    assertTrue(target.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/>"));
    assertTrue(target.contains("<filter left-field=\"ExeTsk\" condition=\"eq\" right-variable=\"taskExecution\"/>"));
    assertTrue(target.contains("<filter left-field=\"Sta\" condition=\"eq\" right-variable=\"status\"/>"));
    assertTrue(target.contains("<variable id=\"status\" type=\"INTEGER\" value=\"4\" />"));
  }

  @Test
  void insertDeferredExecutionRegistersTheReferenceAndTheDates() throws IOException {
    String target = target(readDescriptor(), "insertDeferredExecution");

    assertTrue(target.contains("<table id=\"AweSchExeExt\" />"));
    assertTrue(target.contains("<field id=\"IdeTsk\" variable=\"taskId\"/>"));
    assertTrue(target.contains("<field id=\"ExeTsk\" variable=\"taskExecution\"/>"));
    assertTrue(target.contains("<field id=\"ExtRef\" variable=\"externalReference\"/>"));
    assertTrue(target.contains("<field id=\"DfrDat\" variable=\"acceptedDate\"/>"));
    assertTrue(target.contains("<field id=\"DueDat\" variable=\"dueDate\"/>"));
  }

  @Test
  void theTraceOfADeferredExecutionIsUpdatedByItsExecutionKey() throws IOException {
    String completion = target(readDescriptor(), "updateDeferredCompletion");
    String cancellation = target(readDescriptor(), "updateDeferredCancellation");

    for (String target : new String[]{completion, cancellation}) {
      assertTrue(target.contains("<table id=\"AweSchExeExt\" />"));
      assertTrue(target.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/>"));
      assertTrue(target.contains("<filter left-field=\"ExeTsk\" condition=\"eq\" right-variable=\"taskExecution\"/>"));
    }
    assertTrue(completion.contains("<field id=\"CmpDat\" variable=\"completionDate\"/>"));
    assertTrue(completion.contains("<field id=\"CmpSrc\" variable=\"completionSource\"/>"));
    assertTrue(cancellation.contains("<field id=\"CncSta\" variable=\"cancellationStatus\"/>"));
    assertTrue(cancellation.contains("<field id=\"CncErr\" variable=\"cancellationError\"/>"));
  }

  @Test
  void theTaskStoresItsCancellationMaintain() throws IOException {
    String xml = readDescriptor();
    for (String name : new String[]{"NewSchedulerTaskEditor", "UpdateSchedulerTaskEditor"}) {
      int start = xml.indexOf("<target name=\"" + name + "\"");
      assertTrue(start >= 0, name + " target must exist");
      String target = xml.substring(start, xml.indexOf("</target>", start));
      assertTrue(target.contains("<field id=\"CncMnt\" variable=\"CncMnt\" />"), name);
      assertTrue(target.contains("<variable id=\"CncMnt\" type=\"STRING\" name=\"CncMnt\" />"), name);
    }
  }

  @Test
  void startupInterruptionSkipsTheDeferredExecutions() throws IOException {
    String target = target(readDescriptor(), "updateInterruptedTasks");

    assertTrue(target.contains("<filter condition=\"not exists\" query=\"getDeferredExecutionState\"/>"));
  }

  @Test
  void stopTaskClosesTheDeferredExecutionsOfTheSelectedTask() throws IOException {
    String xml = readDescriptor();
    int start = xml.indexOf("<target name=\"StpTsk\" permission=\"scheduler.execute\">");
    assertTrue(start >= 0, "StpTsk target must exist");
    String target = xml.substring(start, xml.indexOf("</target>", start));

    assertTrue(target.contains("<serve service=\"stopTask\">"));
    assertTrue(target.contains("<variable id=\"IdeTsk\" type=\"INTEGER\" name=\"IdeTsk.selected\" />"));
  }

  private String target(String xml, String name) {
    int start = xml.indexOf("<target name=\"" + name + "\" server-only=\"true\">");
    assertTrue(start >= 0, name + " target must exist");
    return xml.substring(start, xml.indexOf("</target>", start));
  }

  private String readDescriptor() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
      assertNotNull(stream, RESOURCE + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
