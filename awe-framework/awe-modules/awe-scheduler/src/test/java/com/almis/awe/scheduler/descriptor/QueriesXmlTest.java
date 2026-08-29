package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural (golden) checks on {@code global/Queries.xml}: the database-mode execution log
 * queries exist with the expected shape. Full query execution against a real datasource is out
 * of scope for this module's unit test tier (covered by the awe-boot integration tests).
 */
class QueriesXmlTest {

  private static final String RESOURCE = "application/awe-scheduler/global/Queries.xml";

  @Test
  void getExecutionLogLinesOrdersByEventTimestampThenOriginThenLineNumberAndReturnsTheFullStoredWindow() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<query id=\"getExecutionLogLines\">");
    int end = xml.indexOf("</query>", start);
    assertTrue(start >= 0, "getExecutionLogLines query must exist");
    String query = xml.substring(start, end);

    assertTrue(query.contains("<table id=\"AweSchExeLog\"/>"));
    assertTrue(query.contains("<field id=\"Src\" alias=\"origin\"/>"));
    assertTrue(query.contains("<field id=\"LinTxt\" alias=\"lineText\"/>"));
    assertTrue(query.contains("<field id=\"LogDat\" alias=\"logDate\"/>"));
    // Read merges every origin's rows into one time-ordered stream (D5): the query pre-sorts by
    // event timestamp, then origin, then line number, so the Java merge tie-break is stable.
    int logDatOrder = query.indexOf("<order-by field=\"LogDat\" type=\"ASC\"/>");
    int srcOrder = query.indexOf("<order-by field=\"Src\" type=\"ASC\"/>");
    int linNumOrder = query.indexOf("<order-by field=\"LinNum\" type=\"ASC\"/>");
    assertTrue(logDatOrder >= 0 && srcOrder > logDatOrder && linNumOrder > srcOrder,
      "Expected order-by clauses in the sequence LogDat, Src, LinNum");
    assertTrue(query.contains("<filter left-field=\"IdeTsk\" condition=\"eq\" right-variable=\"taskId\"/>"));
    assertTrue(query.contains("<filter left-field=\"ExeTsk\" condition=\"eq\" right-variable=\"executionId\"/>"));
    assertTrue(query.contains("<variable id=\"taskId\" type=\"INTEGER\" name=\"taskId\"/>"));
    assertTrue(query.contains("<variable id=\"executionId\" type=\"INTEGER\" name=\"executionId\"/>"));
    // No offset/LinNum pushdown filter: DatabaseExecutionLogStore.read needs the full window.
    assertFalse(query.contains("offsetLine"));
  }

  @Test
  void getExecutionLogKeysReturnsDistinctTaskAndExecutionIdentifiers() throws IOException {
    String xml = readDescriptor();

    int start = xml.indexOf("<query id=\"getExecutionLogKeys\"");
    int end = xml.indexOf("</query>", start);
    assertTrue(start >= 0, "getExecutionLogKeys query must exist");
    String query = xml.substring(start, end);

    assertTrue(query.contains("distinct=\"true\""));
    assertTrue(query.contains("<table id=\"AweSchExeLog\"/>"));
    assertTrue(query.contains("<field id=\"IdeTsk\" alias=\"taskId\"/>"));
    assertTrue(query.contains("<field id=\"ExeTsk\" alias=\"executionId\"/>"));
  }

  private String readDescriptor() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(RESOURCE)) {
      assertTrue(stream != null, RESOURCE + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
