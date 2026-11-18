package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the flow run screen ({@code scheduler-runs}), its queries, the run link of the execution grid
 * and its locales (#978), and the flow graph of the flows and runs (#992).
 */
class SchedulerRunsDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";

  @Test
  void runListQueryReadsRunsWithRootTaskNameAndStatusIconUnderSchedulerRead() throws IOException {
    String query = query("getFlowRunList");

    assertTrue(query.contains("permission=\"scheduler.read\""));
    assertTrue(query.contains("<table id=\"AweSchRun\" alias=\"Run\"/>"));
    assertTrue(query.contains("<table id=\"AweSchTsk\" alias=\"Tsk\"/>"));
    assertTrue(query.contains("<compound alias=\"RunStaIco\">"));
    assertTrue(query.contains("translate=\"StaIco\""));
    assertTrue(query.contains("right-variable=\"runId\" optional=\"true\""));
    assertTrue(query.contains("<variable id=\"runId\" type=\"STRING\" name=\"ExeRun.selected\" optional=\"true\"/>"));
  }

  @Test
  void runExecutionsQueryFiltersTheExecutionsOfTheSelectedRun() throws IOException {
    String query = query("getFlowRunExecutionList");

    assertTrue(query.contains("permission=\"scheduler.read\""));
    assertTrue(query.contains("<table id=\"AweSchExe\" alias=\"Exe\"/>"));
    assertTrue(query.contains("<filter left-field=\"IdeRun\" left-table=\"Exe\" condition=\"eq\" right-variable=\"runId\"/>"));
    assertTrue(query.contains("<variable id=\"runId\" type=\"STRING\" name=\"IdeRun.selected\"/>"));
  }

  @Test
  void skippedTasksQueryFiltersTheSkippedJoinRowsOfTheSelectedRun() throws IOException {
    String query = query("getFlowRunSkippedList");

    assertTrue(query.contains("permission=\"scheduler.read\""));
    assertTrue(query.contains("<table id=\"AweSchRunTsk\" alias=\"Rtk\"/>"));
    assertTrue(query.contains("<constant value=\"SKIPPED\" type=\"STRING\"/>"));
    assertTrue(query.contains("<variable id=\"runId\" type=\"STRING\" name=\"IdeRun.selected\"/>"));
  }

  @Test
  void executionListQueriesCarryTheRunIdAsExeRun() throws IOException {
    assertTrue(query("getTaskExecutionList").contains("<field id=\"IdeRun\" alias=\"ExeRun\"/>"));
    assertTrue(query("getTaskExecutionListFixed").contains("<field id=\"ExeRun\"/>"));
  }

  @Test
  void runsScreenBindsTheRunExecutionAndSkippedGrids() throws IOException {
    String xml = read("screen/scheduler-runs.xml");

    assertTrue(xml.contains("target-action=\"getFlowRunList\""));
    assertTrue(xml.contains("target-action=\"getFlowRunExecutionList\""));
    assertTrue(xml.contains("target-action=\"getFlowRunSkippedList\""));
    assertTrue(xml.contains("name=\"RunStaIco\" sortable=\"false\" component=\"icon\""));
  }

  @Test
  void tasksScreenOpensTheRunOfTheSelectedExecution() throws IOException {
    String xml = read("screen/scheduler-tasks.xml");

    assertTrue(xml.contains("<column name=\"ExeRun\" hidden=\"true\" />"));
    assertTrue(xml.contains("<button-action type=\"check-one-selected\" target=\"GrdExeLst\" />"));
    assertTrue(xml.contains("<button-action type=\"screen\" target=\"scheduler-runs\" />"));
  }

  @Test
  void flowGraphOfTheTasksAndOfTheSelectedRunOpensTheClickedTask() throws IOException {
    String widget = "<widget type=\"flow-graph\" id=\"IdeTsk\" server-action=\"data\" target-action=\"getFlowGraph\"";
    String openTask = "<widget-parameter type=\"string\" name=\"target\" value=\"update-scheduler-task\"/>";
    String graph = read("screen/scheduler-flow-graph.xml");
    String runs = read("screen/scheduler-runs.xml");

    assertTrue(graph.contains(widget + " initial-load=\"query\" style=\"expand\">") && graph.contains(openTask));
    assertTrue(runs.contains(widget + ">") && runs.contains(openTask));
    assertTrue(runs.contains("<dependency-action type=\"filter\" target=\"IdeTsk\" async=\"true\" silent=\"true\"/>"));
    assertTrue(read("screen/scheduler-tasks.xml")
      .contains("<button-action type=\"screen\" target=\"scheduler-flow-graph\" />"));
    assertTrue(query("getFlowGraph").contains("permission=\"scheduler.read\""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"en-GB", "es-ES", "fr-FR"})
  void everyLocaleCarriesTheFlowRunKeys(String locale) throws IOException {
    String xml = read("locale/Locale-" + locale + ".xml");

    for (String key : new String[]{"MENU_SCHEDULER_RUNS", "HELP_SCREEN_SCHEDULER_RUNS", "SCREEN_TEXT_FLOW_RUNS",
      "SCREEN_TEXT_RUN_EXECUTIONS", "SCREEN_TEXT_RUN_SKIPPED", "PARAMETER_ROOT_TASK", "PARAMETER_ROOT_EXECUTION",
      "PARAMETER_ENDED_AT", "BUTTON_OPEN_RUN", "HELP_OPEN_RUN_BUTTON", "MENU_SCHEDULER_FLOW_GRAPH",
      "HELP_SCREEN_SCHEDULER_FLOW_GRAPH", "SCREEN_TEXT_FLOW_GRAPH", "BUTTON_FLOW_GRAPH", "HELP_FLOW_GRAPH_BUTTON",
      "SCREEN_TEXT_FLOW_GRAPH_EMPTY", "SCREEN_TEXT_FLOW_GRAPH_WAITING", "SCREEN_TEXT_FLOW_GRAPH_SKIPPED",
      "SCREEN_TEXT_FLOW_GRAPH_NOT_REACHED", "SCREEN_TEXT_FLOW_GRAPH_INACTIVE", "SCREEN_TEXT_FLOW_GRAPH_ON_WARNING",
      "SCREEN_TEXT_FLOW_GRAPH_ON_ERROR"}) {
      assertTrue(xml.contains("<locale name=\"" + key + "\""), locale + " is missing " + key);
    }
  }

  private String query(String id) throws IOException {
    String xml = read("global/Queries.xml");
    int start = xml.indexOf("<query id=\"" + id + "\"");
    assertTrue(start >= 0, id + " query must exist");
    return xml.substring(start, xml.indexOf("</query>", start));
  }

  private String read(String path) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(BASE + path)) {
      assertNotNull(stream, BASE + path + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
