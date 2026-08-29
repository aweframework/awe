package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural (golden) checks on {@code global/Maintain.xml}: the three new database-mode
 * execution log targets exist with the expected key columns, {@code loadExecutionScreen}'s
 * locator variable is store-neutral, and the shared {@code purgeExecutionLogs} target (constant
 * {@code PURGE_EXECUTION_LOGS}) is byte-identical to before this change — a diff-based assertion
 * so any accidental future edit fails loudly.
 */
class MaintainXmlTest {

  private static final String RESOURCE = "application/awe-scheduler/global/Maintain.xml";

  // Whitespace-normalized: unaffected by incidental re-indentation, strict about content/order.
  private static final String UNCHANGED_PURGE_EXECUTION_LOGS_TARGET =
    "<target name=\"purgeExecutionLogs\"> <delete> <table id=\"AweSchExe\" /> <where> "
      + "<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/> "
      + "<filter left-field=\"ExeTsk\" condition=\"in\" right-variable=\"executions\"/> </where> "
      + "<variable id=\"taskId\" type=\"INTEGER\" name=\"taskId\" /> "
      + "<variable id=\"executions\" type=\"INTEGER\" name=\"executionId\" /> </delete> </target>";

  @Test
  void purgeExecutionLogsTargetIsByteIdenticalToBeforeThisChange() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"purgeExecutionLogs\">");
    int end = xml.indexOf("</target>", start) + "</target>".length();
    assertTrue(start >= 0, "purgeExecutionLogs target must exist");
    String target = xml.substring(start, end);

    assertEquals(UNCHANGED_PURGE_EXECUTION_LOGS_TARGET, target.replaceAll("\\s+", " ").trim());
  }

  @Test
  void loadExecutionScreenLocatorVariableIsRenamedToTheStoreNeutralName() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"loadExecutionScreen\">");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "loadExecutionScreen target must exist");
    String target = xml.substring(start, end);

    assertTrue(target.contains("<variable id=\"executionLogLocator\" type=\"STRING\" name=\"buttonValue\"/>"));
    assertFalse(target.contains("executionLogPath"));
  }

  @Test
  void insertExecutionLogLinesInsertsKeyedByTaskExecutionSectionAndSlot() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<target name=\"insertExecutionLogLines\">");
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

    int start = xml.indexOf("<target name=\"updateExecutionLogLines\">");
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

    int start = xml.indexOf("<target name=\"purgeExecutionLogLines\">");
    int end = xml.indexOf("</target>", start);
    assertTrue(start >= 0, "purgeExecutionLogLines target must exist");
    String target = xml.substring(start, end);

    assertTrue(target.contains("<delete multiple=\"true\">"));
    assertTrue(target.contains("<table id=\"AweSchExeLog\"/>"));
    assertTrue(target.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/>"));
    assertTrue(target.contains("<filter left-field=\"ExeTsk\" condition=\"eq\" right-variable=\"executionId\"/>"));
  }

  private String readDescriptor() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
      assertTrue(stream != null, RESOURCE + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
