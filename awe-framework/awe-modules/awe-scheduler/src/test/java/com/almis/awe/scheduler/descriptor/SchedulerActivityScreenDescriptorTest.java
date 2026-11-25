package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.dao.task.FlowRunRepository;
import com.almis.awe.scheduler.dao.task.TaskExecutionPresenter;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the activity screen ({@code scheduler-activity}, #993): it reads the activity queries, its
 * criteria are the variables of the execution list, the counters are cards whose value is their counter, the header
 * only shows the status of the engine (its actions are in the configuration screen) and the side panel shows the live
 * log of the execution.
 */
class SchedulerActivityScreenDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String GRID = "GrdExeAct";
  private static final String PANEL = "PnlExeAct";
  /**
   * Cards of the counters and the counter of getActivityCounters each one shows, in the order of the service fields
   */
  private static final Map<String, String> CARDS = new LinkedHashMap<>();

  static {
    CARDS.put("CrdActRun", "running");
    CARDS.put("CrdActWai", "waiting");
    CARDS.put("CrdActQue", "queued");
    CARDS.put("CrdActErr", "errorsLast24h");
    CARDS.put("CrdActUpc", "upcomingNext24h");
  }

  private static Document screen;
  private static Document queries;
  private static Document maintain;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-activity.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
  }

  @Test
  void theScreenLoadsTheEngineStatusIntoACriterionAndTheCountersIntoTheirCards() {
    String target = screen.getDocumentElement().getAttribute("target");
    assertTrue(target.contains("getSchedulerMetadata"), target);
    assertFalse(target.contains("getActivityCounters"), "the cards load their own counter: " + target);

    assertNotNull(byId("criteria", "schedulerStatusCode"));
    List<String> counters = fieldsOf(byId(queries, "query", "getActivityCounters"));
    assertEquals(List.copyOf(CARDS.values()), counters);
    for (String counter : counters) {
      assertNull(byAttribute(screen, "criteria", "id", counter), counter + " is the value of its card, not a criterion");
    }
  }

  @Test
  void theListIsTheExecutionPageOfTheServiceAndOnlyTheColumnsItSortsAreSortable() {
    Element grid = byId("grid", GRID);
    assertEquals("getActivityExecutionList", grid.getAttribute("target-action"));

    // The sort id of each sortable column (its sort-field, else its name): one the service sorts in the database and a
    // field of the page, so the engine sorting the page again keeps the order of the database
    Map<String, String> sortable = Map.of("ExeStaTxt", "Sta", "TskNam", "TskNam", "ExeOriTxt", "TypLch",
      "ExeLchBy", "ExeLchBy", "ExeLstTim", "ExeLstTim", "ExePrg", "ExeEla");
    List<String> fields = fieldsOf(byId(queries, "query", "getActivityExecutionList"));
    List<String> sorted = new ArrayList<>();
    for (Element column : children(grid, "column")) {
      String name = column.getAttribute("name");
      // The «⋯» button reads no value of the row (sendable="false"): it opens the row menu
      if ("false".equals(column.getAttribute("sendable"))) {
        continue;
      }
      assertTrue(fields.contains(name), name + " is not a field of getActivityExecutionList");
      if ("true".equals(column.getAttribute("sortable"))) {
        String sortId = column.getAttribute("sort-field").isEmpty() ? name : column.getAttribute("sort-field");
        assertEquals(sortable.get(name), sortId, name + " sort");
        assertTrue(fields.contains(sortId), sortId + " is not a field of getActivityExecutionList");
        sorted.add(name);
      } else if (!"true".equals(column.getAttribute("hidden"))) {
        assertEquals("false", column.getAttribute("sortable"), name + " must not be sortable");
      }
    }
    assertEquals(sortable.keySet(), Set.copyOf(sorted));
  }

  @Test
  void theScreenHasNoCriteriaWindowOnlyTheCountersAndTheExecutionWindow() {
    NodeList windows = screen.getElementsByTagName("window");
    assertEquals(1, windows.getLength(), "only the window of the executions");
    assertEquals(GRID, children((Element) windows.item(0), "grid").get(0).getAttribute("id"));
    for (String removed : new String[]{"CrtExeSta", "CrtExeDfr"}) {
      assertNull(byAttribute(screen, "criteria", "id", removed), removed);
    }
    assertNull(byAttribute(screen, "button", "id", "ButSch"), "no search button: the grid filters itself");
  }

  @Test
  void theGridToolbarCarriesTheTaskSearchAndTheStateOriginAndStartQuickFilters() {
    Element grid = byId("grid", GRID);
    List<String> variables = variablesOf("getActivityExecutionList");
    List<String> counted = variablesOf("getActivityStateCounts");

    Element search = children(grid, "search").get(0);
    assertEquals("CrtExeTsk", search.getAttribute("id"));
    assertTrue(variables.contains("CrtExeTsk"));
    assertTrue(counted.contains("CrtExeTsk"));
    // The state (with its counts), the origin and the start, in this order, all of them unset
    Map<String, String> quickFilters = new LinkedHashMap<>();
    Map<String, String> displays = new LinkedHashMap<>();
    for (Element quickFilter : children(grid, "quick-filter")) {
      quickFilters.put(quickFilter.getAttribute("id"), quickFilter.getAttribute("target-action"));
      displays.put(quickFilter.getAttribute("id"), quickFilter.getAttribute("display"));
      assertEquals("query", quickFilter.getAttribute("initial-load"));
      assertEquals("", quickFilter.getAttribute("value"));
      assertTrue(variables.contains(quickFilter.getAttribute("id")), quickFilter.getAttribute("id") + " is not read by the list");
    }
    assertEquals(List.of("CrtExeQck", "CrtExeOri", "CrtExeWin"), List.copyOf(quickFilters.keySet()));
    assertEquals(List.of("getActivityStateCounts", "getActivityOrigins", "getActivityWindows"),
      List.copyOf(quickFilters.values()));
    // One toolbar line: the state stays segmented, the origin is a dropdown and the start a date range (its last hours
    // or a custom range of start days, sent as CrtExeWin.from and CrtExeWin.to)
    assertEquals(List.of("", "dropdown", "date-range"), List.copyOf(displays.values()));
    // The counts of the states follow the other filters of the list
    for (String criterion : new String[]{"CrtExeOri", "CrtExeWin", "CrtExeWin.from", "CrtExeWin.to"}) {
      assertTrue(variables.contains(criterion), criterion + " is not read by the list");
      assertTrue(counted.contains(criterion), criterion + " is not read by the counts");
    }
    // The origin and start filters are quick filters, no longer hidden criteria nor menus of the header
    for (String criterion : new String[]{"CrtExeOri", "CrtExeWin"}) {
      assertNull(byAttribute(screen, "criteria", "id", criterion), criterion);
    }
    assertNull(byAttribute(screen, "button", "id", "ButOri"), "the origin is a quick filter of the grid");
  }

  @Test
  void theOriginAndStartOptionsAreEnumeratedQueriesWithAnEmptyFirstOption() throws Exception {
    Document enumerated = parse("global/Enumerated.xml");
    Map<String, List<String>> expected = Map.of(
      "getActivityOrigins", List.of("", "scheduled", "manual", "file", "dependency", "webhook"),
      "getActivityWindows", List.of("", "24", "168", "720"));
    expected.forEach((queryId, values) -> {
      Element query = byId(queries, "query", queryId);
      assertEquals("scheduler.read", query.getAttribute("permission"), queryId);
      assertEquals(List.of("value", "label"), fieldsOf(query), queryId);
      Element group = byId(enumerated, "group", query.getAttribute("enumerated"));
      assertEquals(values, children(group, "option").stream().map(option -> option.getAttribute("value")).toList(), queryId);
    });
    // The origins of the filter are the ones the service accepts
    List<String> origins = children(byId(enumerated, "group", "ExeOri"), "option").stream()
      .map(option -> option.getAttribute("value")).toList();
    assertEquals(origins, expected.get("getActivityOrigins").subList(1, 6));
  }

  @Test
  void theGridPageIsTheDefaultPageOfTheService() {
    assertEquals(String.valueOf(TaskExecutionPresenter.DEFAULT_PAGE_SIZE), byId("grid", GRID).getAttribute("max"));
  }

  @Test
  void everyCounterIsACardWhoseValueIsItsCounterReadByAQueryOfTheCountersService() {
    for (Map.Entry<String, String> entry : CARDS.entrySet()) {
      Element card = byId("card", entry.getKey());
      assertEquals("query", card.getAttribute("initial-load"), entry.getKey());
      Element query = byId(queries, "query", card.getAttribute("target-action"));
      assertEquals("getActivityCounters", query.getAttribute("service"), entry.getKey());
      assertEquals("scheduler.read", query.getAttribute("permission"), entry.getKey());
      // The counter is the native value of the card (its big number), not a component of its body
      List<Element> fields = children(query, "field");
      assertEquals(1, fields.size(), entry.getKey());
      assertEquals(entry.getValue(), fields.get(0).getAttribute("id"), entry.getKey());
      assertEquals("value", fields.get(0).getAttribute("alias"), entry.getKey());
      assertEquals(0, card.getElementsByTagName("*").getLength(),
        entry.getKey() + " is informative: no button nor criterion inside");
    }
    assertFalse(byId("card", "CrdActUpc").getAttribute("description").isBlank(), "the upcoming card says what it counts");
  }

  @Test
  void theCardsAreTheDefaultCardGridWithTheirFullIconTile() {
    Element grid = (Element) byId("card", "CrdActRun").getParentNode();
    List<String> styles = List.of(grid.getAttribute("style").split("\\s+"));
    assertTrue(styles.contains("awe-card-grid"), grid.getAttribute("style"));
    assertFalse(styles.contains("awe-card-compact"), "the compact grid shrinks the icon tile");
    for (String card : CARDS.keySet()) {
      assertEquals(grid, byId("card", card).getParentNode(), card);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"ButRel", "ButActLch", "ButActStp"})
  void everyActionThatChangesTheCountersReloadsEveryCard(String buttonId) {
    List<String> actions = actionsOf(byId("button", buttonId));
    for (String card : CARDS.keySet()) {
      assertTrue(actions.contains("filter:" + card), buttonId + " does not reload " + card);
    }
    assertFalse(actions.contains("server:getActivityCounters"), buttonId + " fills no counter criterion");
  }

  @Test
  void theCustomRangeIsPartOfTheStartQuickFilterNotAPopoverOfTheHeader() {
    assertNull(byAttribute(screen, "button", "id", "ButRng"), "the custom range is in the start quick filter");
    assertNull(byAttribute(screen, "popover", "id", "PopRng"), "the custom range is in the start quick filter");
    for (String criterion : new String[]{"CrtExeIni", "CrtExeEnd"}) {
      assertNull(byAttribute(screen, "criteria", "id", criterion), criterion);
    }
  }

  @Test
  void theRowMenuOpensTheDetailOfTheExecutionLikeTheLogButton() {
    List<Element> menu = children(byId("grid", GRID), "context-button");
    assertEquals(1, menu.size());
    Element details = menu.get(0);
    assertEquals("CtxExeDet", details.getAttribute("id"));
    assertEquals("BUTTON_ACTIVITY_VIEW_DETAILS", details.getAttribute("label"));
    assertEquals(List.of("server:loadActivityExecution", "reset:executionLogViewer", "filter:executionLogViewer",
      "filter:CrdExePrg", "filter:GrdExePar", "dialog:" + PANEL), actionsOf(details));

    // The maintain fills the panel from the execution of the selected row
    Element target = byAttribute(maintain, "target", "name", "loadActivityExecution");
    assertNotNull(target);
    assertEquals("scheduler.read", target.getAttribute("permission"));
    Element serve = children(target, "serve").get(0);
    assertEquals("reloadExecutionScreen", serve.getAttribute("service"));
    assertEquals(List.of("IdeTsk.selected", "ExeTsk.selected"), children(serve, "variable").stream()
      .map(variable -> variable.getAttribute("name")).toList());
    // The log viewer reads the execution of the selected row
    Element panel = byId("side-panel", PANEL);
    assertEquals("IdeTsk", byAttribute(descendant(panel, "criteria", "execution-log-task-id"), "dependency-element",
      "attribute", "selectedRowValue").getAttribute("column"));
    assertEquals("ExeTsk", byAttribute(descendant(panel, "criteria", "execution-log-execution-id"), "dependency-element",
      "attribute", "selectedRowValue").getAttribute("column"));
  }

  @Test
  void theHeaderShowsTheEngineStatusAsAPillOverItsLineWithoutTheEngineMenu() {
    // One pill per status, shown by the status code once the other pills are hidden (never two at once)
    Map<String, String> pills = new LinkedHashMap<>();
    for (Element dependency : children(byId("criteria", "schedulerStatusCode"), "dependency")) {
      String status = children(dependency, "dependency-element").get(0).getAttribute("value");
      List<String> types = new ArrayList<>();
      for (Element action : children(dependency, "dependency-action")) {
        types.add(action.getAttribute("type"));
        if ("remove-class".equals(action.getAttribute("type"))) {
          pills.put(status, action.getAttribute("target"));
        }
      }
      assertEquals(List.of("add-class", "remove-class"), types, status + ": hide the others, then show its pill");
    }
    assertEquals(Map.of("started", "#EngStaStr", "standby", "#EngStaStb", "stopped", "#EngStaStp"), pills);
    assertEquals("SCREEN_TEXT_ACTIVITY_ENGINE_STARTED", byAttribute(screen, "tag", "id", "EngStaStr").getAttribute("label"));
    assertEquals("SCREEN_TEXT_ACTIVITY_ENGINE_STANDBY", byAttribute(screen, "tag", "id", "EngStaStb").getAttribute("label"));
    assertEquals("SCREEN_TEXT_ACTIVITY_ENGINE_STOPPED", byAttribute(screen, "tag", "id", "EngStaStp").getAttribute("label"));
    assertEquals("text-view", byId("criteria", "schedulerEngineLine").getAttribute("component"));
    assertTrue(fieldsOf(byId(queries, "query", "getSchedulerMetadata")).contains("schedulerEngineLine"));

    // The engine is operated from the Motor tab of the configuration screen: no engine menu nor its confirmations here
    assertNull(byAttribute(screen, "button", "id", "ButEng"), "the engine menu moved to the configuration screen");
    assertNull(byAttribute(screen, "context-button", "id", "CtxEngPau"), "the engine menu moved to the configuration screen");
    for (String message : new String[]{"EngRstMsg", "EngClrMsg"}) {
      assertNull(byAttribute(screen, "message", "id", message), message);
    }
  }

  @Test
  void theTaskAndTheFlowRunAreLinksToTheirScreens() {
    Element task = column("TskNam");
    assertEquals("link", task.getAttribute("component"));
    assertEquals(List.of("screen:scheduler-task"), actionsOf(task));

    Element run = column("ExeRun");
    assertEquals("link", run.getAttribute("component"));
    assertEquals(List.of("screen:scheduler-flow-run"), actionsOf(run));

    assertEquals("progress", column("ExePrg").getAttribute("component"));
    assertNotNull(column("ExeDueTim"));
  }

  @Test
  void theGridFitsWithoutHorizontalScrollWithTheTaskAsTheFillColumn() {
    // Width of a column in the React engine: (charlength + 4) * 8 px. At 1600 px, the side menu and the paddings leave
    // about 1240 px to the grid: the fixed columns and the minimum of the task column must fit in them
    int width = 0;
    List<String> visible = new ArrayList<>();
    for (Element column : children(byId("grid", GRID), "column")) {
      if (!"true".equals(column.getAttribute("hidden"))) {
        visible.add(column.getAttribute("name"));
        width += (Integer.parseInt(column.getAttribute("charlength")) + 4) * 8;
      }
    }
    assertTrue(width <= 1240, "the visible columns need " + width + " px: " + visible);
    assertEquals("*", column("TskNam").getAttribute("width"), "the task takes the free width");
    // The execution number and the due time are not columns: the side panel shows the first, the status the second
    for (String hidden : new String[]{"ExeTsk", "ExeDueTim"}) {
      assertEquals("true", column(hidden).getAttribute("hidden"), hidden);
    }
  }

  @Test
  void theStatusIsAPillWithItsIconAndTheDueTimeOfAWaitingExecutionUnderIt() {
    Element status = column("ExeStaTxt");
    assertEquals("", status.getAttribute("component"), "a rich cell, not a component");
    assertEquals("ExeStaTon", status.getAttribute("status-field"));
    assertEquals("ExeStaIcn", status.getAttribute("icon-field"));
    assertEquals("ExeDueLbl", status.getAttribute("secondary-field"));
    assertNull(byAttribute(byId("grid", GRID), "column", "name", "ExeStaIco"), "the old status column is gone");
    // The side panel header shows the same pill
    Element tag = children(byId("side-panel", PANEL), "header-tag").get(0);
    assertEquals("ExeStaTxt", tag.getAttribute("field"));
    assertEquals("ExeStaTon", tag.getAttribute("tone-field"));
    assertEquals("ExeStaIcn", tag.getAttribute("icon-field"));
  }

  @Test
  void theOriginIsAPillWithItsIconInOneNeutralTone() {
    Element origin = column("ExeOriTxt");
    assertEquals("", origin.getAttribute("component"), "a rich cell, not a component");
    assertEquals("ExeOriTon", origin.getAttribute("status-field"));
    assertEquals("ExeOriIcn", origin.getAttribute("icon-field"));
    assertNull(byAttribute(byId("grid", GRID), "column", "name", "ExeLchIco"), "the coloured origin column is gone");
  }

  @Test
  void theLogButtonIsAGhostButton() {
    assertTrue(List.of(column("ExecutionLogPath").getAttribute("style").split("\\s+")).contains("btn-ghost"));
  }

  @Test
  void theSidePanelShowsTheFlowLabelOfTheRowAndOpensTheRunByItsId() {
    Element label = column("ExeRunLbl");
    assertNotNull(label, "the grid carries the flow label of the row");
    assertEquals("true", label.getAttribute("hidden"));
    Element panel = byId("side-panel", PANEL);

    Element flow = descendant(panel, "criteria", "ViwExeRun");
    assertEquals("ExeRunLbl", byAttribute(flow, "dependency-element", "attribute", "selectedRowValue")
      .getAttribute("column"));
    Element openRun = descendant(panel, "button", "ButActOpnRun");
    assertEquals("ExeRun", byAttribute(openRun, "dependency-element", "attribute", "selectedRowValue")
      .getAttribute("column"));
  }

  @Test
  void theLogIsFollowedOnlyWhileTheExecutionRunsOrIsQueuedAndAnEndedOneIsReadOnce() {
    Element panel = byId("side-panel", PANEL);
    // The status of the selected row of either grid
    Element status = descendant(panel, "criteria", "execution-log-status");
    assertEquals("hidden", status.getAttribute("component"));
    List<String> sources = new ArrayList<>();
    for (Element dependency : children(status, "dependency")) {
      Element row = byAttribute(dependency, "dependency-element", "attribute", "selectedRowValue");
      sources.add(row.getAttribute("id") + "." + row.getAttribute("column"));
    }
    assertEquals(List.of(GRID + ".Sta", "GrdExeRun.Sta"), sources);

    // Enabled on open only for a running (or waiting) or queued execution, disabled on close
    Element log = descendant(panel, "widget", "executionLogViewer");
    List<Element> dependencies = children(log, "dependency");
    assertEquals(2, dependencies.size());
    Element enable = dependencies.get(0);
    assertEquals("enable-autorefresh", enable.getAttribute("target-type"));
    assertEquals("5", enable.getAttribute("value"));
    List<Element> elements = children(enable, "dependency-element");
    assertEquals(2, elements.size());
    assertEquals(PANEL, elements.get(0).getAttribute("id"));
    assertEquals("open", elements.get(0).getAttribute("event"));
    assertEquals("execution-log-status", elements.get(1).getAttribute("id"));
    assertEquals("in", elements.get(1).getAttribute("condition"));
    assertEquals(TaskStatus.JOB_RUNNING.getValue() + "," + TaskStatus.JOB_QUEUED.getValue(),
      elements.get(1).getAttribute("value"));
    assertEquals("disable-autorefresh", dependencies.get(1).getAttribute("target-type"));

    // An ended execution is read once by the filter of the viewer: from the row menu and from the log button (the
    // maintain answers with it)
    assertTrue(actionsOf(byId("context-button", "CtxExeDet")).contains("filter:executionLogViewer"));
  }

  @Test
  void aRowOpensTheSidePanelWithTheLiveLogOfItsExecution() {
    assertEquals(List.of("server:loadExecutionScreen", "filter:CrdExePrg", "filter:GrdExePar", "dialog:" + PANEL),
      actionsOf(column("ExecutionLogPath")));

    Element panel = byId("side-panel", PANEL);
    assertEquals(GRID, panel.getAttribute("header-grid"));
    Element log = descendant(panel, "widget", "executionLogViewer");
    assertEquals("log-viewer", log.getAttribute("type"));
    assertEquals("get-execution-log", log.getAttribute("server-action"));
    assertNotNull(descendant(panel, "criteria", "execution-log-task-id"));
    assertNotNull(descendant(panel, "criteria", "execution-log-execution-id"));
    // The deferred state in plain words: the due time of the row
    assertNotNull(descendant(panel, "criteria", "ViwExeDue"));

    assertTrue(actionsOf(descendant(panel, "button", "ButActStp"))
      .containsAll(List.of("confirm:ExeStpMsg", "server:StpTsk")));
    assertTrue(actionsOf(descendant(panel, "button", "ButActLch")).contains("server:LchTsk"));
  }

  @Test
  void theSidePanelShowsTheProgressOfTheSelectedExecutionInACardOfItsOwnService() throws Exception {
    Element card = descendant(byId("side-panel", PANEL), "card", "CrdExePrg");
    Element query = byId(queries, "query", card.getAttribute("target-action"));
    assertEquals("scheduler.read", query.getAttribute("permission"));
    assertEquals(List.of("title", "value", "unit", "target", "numberDecimals", "description", "iconTone"), fieldsOf(query));
    assertEquals(List.of("IdeTsk.selected", "ExeTsk.selected"), variablesOf(query.getAttribute("id")));

    Element service = byId(parse("global/Services.xml"), "service", query.getAttribute("service"));
    assertEquals("scheduler.read", service.getAttribute("permission"));
    Element java = children(service, "java").get(0);
    assertEquals("getExecutionProgress", java.getAttribute("method"));
    assertEquals(List.of("taskId", "executionId"), children(java, "service-parameter").stream()
      .map(parameter -> parameter.getAttribute("name")).toList());
    // A waiting execution shows its due time instead of a progress
    Element waiting = byAttribute(card, "dependency-element", "column", "ExeDfr");
    assertEquals("ne", waiting.getAttribute("condition"));
    assertEquals("1", waiting.getAttribute("value"));
  }

  @Test
  void theSidePanelListsTheLaunchParametersOfTheFlowRunOfTheExecutionFromItsService() throws Exception {
    Element grid = descendant(byId("side-panel", PANEL), "grid", "GrdExePar");
    Element query = byId(queries, "query", grid.getAttribute("target-action"));
    assertEquals("scheduler.read", query.getAttribute("permission"));
    assertEquals(List.of("ExeRun.selected"), variablesOf(query.getAttribute("id")));
    assertFalse("true".equals(query.getAttribute("server-only")), "the grid reads it");
    List<String> fields = fieldsOf(query);
    for (Element column : children(grid, "column")) {
      assertTrue(fields.contains(column.getAttribute("name")), column.getAttribute("name") + " is not a field of the query");
    }

    // The service merges the operator rows and the resolved rows of the run (without their prefix)
    Element service = byId(parse("global/Services.xml"), "service", query.getAttribute("service"));
    assertEquals("scheduler.read", service.getAttribute("permission"));
    Element java = children(service, "java").get(0);
    assertEquals("getExecutionParameters", java.getAttribute("method"));
    assertEquals(List.of("runId"), children(java, "service-parameter").stream()
      .map(parameter -> parameter.getAttribute("name")).toList());
    // Without rows, the note says why
    assertEquals("SCREEN_TEXT_ACTIVITY_NO_PARAMETERS_TEXT",
      descendant(byId("side-panel", PANEL), "card", "CrdExeParEmp").getAttribute("description"));
  }

  @Test
  void aTaskParameterNameCannotStartWithThePrefixOfTheResolvedRunParameters() throws Exception {
    Element name = byAttribute(parse("screen/scheduler-task-editor.xml").getDocumentElement(), "column", "name", "ParNam");
    assertNotNull(name);
    String validation = name.getAttribute("validation");
    assertTrue(validation.contains("required: true"), validation);
    Matcher pattern = Pattern.compile("pattern: \\{value: '([^']*)'").matcher(validation);
    assertTrue(pattern.find(), validation);
    Pattern names = Pattern.compile(pattern.group(1));
    assertFalse(names.matcher(FlowRunRepository.RESOLVED_PARAMETER_PREFIX + "date").find());
    assertTrue(names.matcher("date").find());
    assertTrue(names.matcher("a=b").find());
  }

  @Test
  void theWaitingNoteAndItsDueTimeShowOnlyForTheDeferredFlagOfTheSelectedRow() {
    Element panel = byId("side-panel", PANEL);
    // ExeDfr is 1 only for a running execution waiting for its confirmation: an ended one never shows the wait
    for (Element waiting : List.of(descendant(panel, "card", "CrdExeWai"), descendant(panel, "criteria", "ViwExeDue"))) {
      Element show = showDependency(waiting);
      assertEquals("true", show.getAttribute("initial"), waiting.getAttribute("id"));
      Element element = children(show, "dependency-element").get(0);
      assertEquals(GRID, element.getAttribute("id"), waiting.getAttribute("id"));
      assertEquals("ExeDfr", element.getAttribute("column"), waiting.getAttribute("id"));
      assertEquals("selectedRowValue", element.getAttribute("attribute"), waiting.getAttribute("id"));
      assertEquals("eq", element.getAttribute("condition"), waiting.getAttribute("id"));
      assertEquals("1", element.getAttribute("value"), waiting.getAttribute("id"));
    }
    // No class toggled on a container by a hidden criterion: it kept the block of a previously opened waiting row
    assertNull(byAttribute(panel, "criteria", "id", "ViwExeDfr"), "the deferred flag is read from the row");
    assertNull(byAttribute(panel, "dependency-action", "target", "#ActExeWai"), "no class toggle on the waiting block");
  }

  @Test
  void theParametersGridShowsOnlyWithRowsAndANoteSaysWhenThereAreNone() {
    Element panel = byId("side-panel", PANEL);
    Element grid = descendant(panel, "grid", "GrdExePar");
    assertShownByTheParameterCount(grid, "gt");
    assertShownByTheParameterCount(descendant(panel, "card", "CrdExeParEmp"), "eq");
    // The grid holds its own height, so its container collapses when it hides
    Element container = (Element) grid.getParentNode();
    assertFalse(List.of(container.getAttribute("style").split("\\s+")).contains("height-sm"), container.getAttribute("style"));
  }

  private static void assertShownByTheParameterCount(Element component, String condition) {
    Element show = showDependency(component);
    assertEquals("true", show.getAttribute("initial"), component.getAttribute("id"));
    Element element = children(show, "dependency-element").get(0);
    assertEquals("GrdExePar", element.getAttribute("id"), component.getAttribute("id"));
    assertEquals("totalValues", element.getAttribute("attribute"), component.getAttribute("id"));
    assertEquals(condition, element.getAttribute("condition"), component.getAttribute("id"));
    assertEquals("0", element.getAttribute("value"), component.getAttribute("id"));
  }

  private static Element showDependency(Element component) {
    Element show = byAttribute(component, "dependency", "target-type", "show");
    assertNotNull(show, component.getAttribute("id") + " has no show dependency");
    return show;
  }

  @Test
  void everyMaintainOfTheScreenDeclaresItsPermission() {
    NodeList actions = screen.getElementsByTagName("button-action");
    List<String> targets = new ArrayList<>();
    for (int index = 0; index < actions.getLength(); index++) {
      Element action = (Element) actions.item(index);
      if (action.getAttribute("server-action").startsWith("maintain")) {
        targets.add(action.getAttribute("target-action"));
      }
    }
    assertFalse(targets.isEmpty());
    for (String target : targets) {
      Element element = byAttribute(maintain, "target", "name", target);
      assertNotNull(element, target + " is not a maintain target");
      assertFalse(element.getAttribute("permission").isBlank(), target + " has no permission");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"en-GB", "es-ES", "fr-FR"})
  void everyLocaleCarriesTheActivityKeys(String locale) throws Exception {
    Document locales = parse("locale/Locale-" + locale + ".xml");
    for (String key : new String[]{"MENU_SCHEDULER_ACTIVITY", "HELP_SCREEN_SCHEDULER_ACTIVITY",
      "SCREEN_TEXT_ACTIVITY_ENGINE_STARTED", "SCREEN_TEXT_ACTIVITY_ENGINE_STANDBY", "SCREEN_TEXT_ACTIVITY_ENGINE_STOPPED",
      "HELP_ACTIVITY_RELOAD_BUTTON",
      "BUTTON_ACTIVITY_RELOAD",
      "SCREEN_TEXT_ACTIVITY_RUNNING", "SCREEN_TEXT_ACTIVITY_WAITING", "SCREEN_TEXT_ACTIVITY_QUEUED",
      "SCREEN_TEXT_ACTIVITY_ERRORS_24H", "SCREEN_TEXT_ACTIVITY_UPCOMING_24H", "SCREEN_TEXT_ACTIVITY_UPCOMING_TEXT",
      "PARAMETER_ACTIVITY_ORIGIN",
      "SCREEN_TEXT_ACTIVITY_ALL", "SCREEN_TEXT_ACTIVITY_SEARCH", "BUTTON_ACTIVITY_ORIGIN_ALL", "BUTTON_ACTIVITY_VIEW_DETAILS",
      "BUTTON_ACTIVITY_DATES_ALL",
      "BUTTON_ACTIVITY_DATES_24H", "BUTTON_ACTIVITY_DATES_7D", "BUTTON_ACTIVITY_DATES_30D", "ENUM_STATUS_INFO",
      "PARAMETER_ACTIVITY_FLOW_RUN", "PARAMETER_ACTIVITY_DUE_AT",
      "SCREEN_TEXT_ACTIVITY_DUE_SHORT",
      "BUTTON_ACTIVITY_OPEN_TASK", "SCREEN_TEXT_ACTIVITY_WAITING_TEXT", "SCREEN_TEXT_ACTIVITY_LOG",
      "BUTTON_ACTIVITY_RELAUNCH", "HELP_ACTIVITY_RELAUNCH_BUTTON", "BUTTON_ACTIVITY_STOP",
      "SCREEN_TEXT_ACTIVITY_NO_PARAMETERS", "SCREEN_TEXT_ACTIVITY_NO_PARAMETERS_TEXT",
      "VALIDATOR_MESSAGE_SCHEDULER_PARAMETER_NAME",
      "CONFIRM_TITLE_ACTIVITY_STOP", "CONFIRM_MESSAGE_ACTIVITY_STOP"}) {
      Element entry = byAttribute(locales, "locale", "name", key);
      assertNotNull(entry, locale + " is missing " + key);
      assertFalse(entry.getAttribute("value").isBlank(), locale + " has an empty " + key);
    }
    // The counters are no longer buttons, the origin no longer a menu, the custom range no longer a popover of the
    // header and the engine menu moved to the configuration screen: their labels are gone
    for (String key : new String[]{"BUTTON_ACTIVITY_SHOW_RUNNING", "BUTTON_ACTIVITY_SHOW_WAITING",
      "BUTTON_ACTIVITY_SHOW_QUEUED", "BUTTON_ACTIVITY_SHOW_ERRORS", "HELP_ACTIVITY_ORIGIN_BUTTON",
      "BUTTON_ACTIVITY_ORIGIN_SCHEDULED", "BUTTON_ACTIVITY_ORIGIN_MANUAL", "BUTTON_ACTIVITY_ORIGIN_FILE",
      "BUTTON_ACTIVITY_ORIGIN_DEPENDENCY", "HELP_ACTIVITY_DATES_BUTTON", "BUTTON_ACTIVITY_DATES_CUSTOM",
      "SCREEN_TEXT_ACTIVITY_DATES_CUSTOM", "BUTTON_ACTIVITY_DATES_APPLY", "PARAMETER_ACTIVITY_START_FROM",
      "PARAMETER_ACTIVITY_START_TO", "BUTTON_ACTIVITY_ENGINE", "HELP_ACTIVITY_ENGINE_BUTTON",
      "BUTTON_ACTIVITY_ENGINE_PAUSE", "BUTTON_ACTIVITY_ENGINE_RESUME", "BUTTON_ACTIVITY_ENGINE_RESTART",
      "BUTTON_ACTIVITY_ENGINE_CLEAR_AND_STOP", "CONFIRM_TITLE_ACTIVITY_ENGINE_RESTART",
      "CONFIRM_MESSAGE_ACTIVITY_ENGINE_RESTART", "CONFIRM_TITLE_ACTIVITY_ENGINE_CLEAR",
      "CONFIRM_MESSAGE_ACTIVITY_ENGINE_CLEAR"}) {
      assertNull(byAttribute(locales, "locale", "name", key), locale + " still carries " + key);
    }
  }

  private static List<String> variablesOf(String query) {
    List<String> variables = new ArrayList<>();
    for (Element variable : children(byId(queries, "query", query), "variable")) {
      variables.add(variable.getAttribute("name"));
    }
    return variables;
  }

  private static Element column(String name) {
    return byAttribute(byId("grid", GRID), "column", "name", name);
  }

  /**
   * Actions of an element as type:target (the target action of a server action)
   */
  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      String type = action.getAttribute("type");
      String target = "server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target");
      actions.add(type + ":" + target);
    }
    return actions;
  }

  private static List<String> fieldsOf(Element query) {
    List<String> fields = new ArrayList<>();
    for (Element field : children(query, "field")) {
      fields.add(field.hasAttribute("alias") ? field.getAttribute("alias") : field.getAttribute("id"));
    }
    return fields;
  }

  private static List<Element> children(Element parent, String tag) {
    List<Element> list = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && tag.equals(element.getTagName())) {
        list.add(element);
      }
    }
    return list;
  }

  private static Element byId(String tag, String id) {
    Element element = byAttribute(screen, tag, "id", id);
    assertNotNull(element, tag + " " + id + " must exist");
    return element;
  }

  private static Element byId(Document document, String tag, String id) {
    Element element = byAttribute(document, tag, "id", id);
    assertNotNull(element, tag + " " + id + " must exist");
    return element;
  }

  private static Element descendant(Element parent, String tag, String id) {
    Element element = byAttribute(parent, tag, "id", id);
    assertNotNull(element, tag + " " + id + " must be inside " + parent.getAttribute("id"));
    return element;
  }

  private static Element byAttribute(Document document, String tag, String attribute, String value) {
    return byAttribute(document.getDocumentElement(), tag, attribute, value);
  }

  private static Element byAttribute(Element parent, String tag, String attribute, String value) {
    NodeList nodes = parent.getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      Element element = (Element) nodes.item(index);
      if (value.equals(element.getAttribute(attribute))) {
        return element;
      }
    }
    return null;
  }

  private static Document parse(String path) throws Exception {
    try (InputStream stream = SchedulerActivityScreenDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      if (stream == null) {
        throw new IOException(BASE + path + " must be on the test classpath");
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(stream);
    }
  }
}
