package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the task screen ({@code scheduler-task}, #993): the task travels in the hidden criteria the
 * existing actions read and no grid sends it again, the screen and its cards read the task page service and load in
 * the request of the screen (they share its one computation of the page), its header runs, edits, activates,
 * deactivates and deletes the task with the existing actions, its tabs are the ones of their enumerated, its history is
 * the activity list of the task with its own copy of the activity side panel, and its dependencies are the one-level
 * flow graph.
 */
class SchedulerTaskScreenDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String TASK = "IdeTsk.selected";

  private static Document screen;
  private static Document queries;
  private static Document maintain;
  private static Document enumerated;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-task.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    enumerated = parse("global/Enumerated.xml");
  }

  @Test
  void theTaskTravelsInTheHiddenCriteriaOfTheExistingActionsAndNoGridSendsItAgain() {
    for (String criterion : List.of(TASK, "IdeTsk", "CrtExeTskIde", "execution-log-task-id")) {
      Element hidden = byId(screen, "criteria", criterion);
      assertEquals("hidden", hidden.getAttribute("component"), criterion);
      assertEquals(TASK, hidden.getAttribute("variable"), criterion);
    }
    NodeList columns = screen.getElementsByTagName("column");
    for (int index = 0; index < columns.getLength(); index++) {
      assertFalse("IdeTsk".equals(((Element) columns.item(index)).getAttribute("name")), "No grid sends IdeTsk");
    }
    // The actions of the header read the task as the task manager screens send it
    for (String target : List.of("LchTsk", "ActSchTsk", "DeaSchTsk")) {
      assertTrue(variablesOf(byAttribute(maintain.getDocumentElement(), "target", "name", target)).contains(TASK),
        target);
    }
    assertTrue(variablesOf(byAttribute(maintain.getDocumentElement(), "target", "name", "DeleteSchedulerTask"))
      .contains("IdeTsk"));
  }

  @Test
  void theScreenAndItsCardsReadTheTaskPageServiceAndEveryCriterionIsAFieldOfIt() {
    assertEquals("getTaskPage", screen.getDocumentElement().getAttribute("target"));
    List<String> fields = fieldsOf(byId(queries, "query", "getTaskPage"));
    for (String criterion : List.of("TskAct", "TskVar", "TskLstHas", "TskLstDes", "TskLstExe", "TskLstRun",
      "TskLstOri", "TskLstBy", "TskLstIni", "TskLstDur", "TskCfgRun", "TskCfgWhr", "TskCfgSto", "TskCfgTim",
      "TskCfgWhn", "TskCfgPrn", "TskCfgChl", "TskCfgRpt")) {
      assertNotNull(byId(screen, "criteria", criterion));
      assertTrue(fields.contains(criterion), criterion + " is not a field of getTaskPage");
    }
    Map<String, String> cards = new LinkedHashMap<>();
    NodeList list = screen.getElementsByTagName("card");
    for (int index = 0; index < list.getLength(); index++) {
      Element card = (Element) list.item(index);
      if (card.getAttribute("target-action").startsWith("getTaskPage")) {
        cards.put(card.getAttribute("id"), card.getAttribute("target-action"));
        Element query = byId(queries, "query", card.getAttribute("target-action"));
        assertEquals("getTaskPage", query.getAttribute("service"), card.getAttribute("id"));
        assertEquals("scheduler.read", query.getAttribute("permission"), card.getAttribute("id"));
        assertEquals(List.of(TASK), variableNamesOf(query), card.getAttribute("id"));
        for (Element field : children(query, "field")) {
          assertTrue(fields.contains(field.getAttribute("id")), field.getAttribute("id"));
        }
      }
    }
    assertEquals(Map.of("CrdTskPag", "getTaskPageHeader", "CrdTskOk", "getTaskPageSuccess",
      "CrdTskAvg", "getTaskPageAverage", "CrdTskNxt", "getTaskPageNext", "CrdTskHis", "getTaskPageHistoryCount",
      "CrdTskLst", "getTaskPageLast"), cards);
    for (String query : List.of("getTaskPageTask", "getTaskPageExecutions")) {
      assertEquals("true", byId(queries, "query", query).getAttribute("server-only"), query);
      assertEquals("scheduler.read", byId(queries, "query", query).getAttribute("permission"), query);
    }
    // Where the task runs and who gets its report: only in the query of the editors
    List<String> restricted = List.of("CmdExePth", "SrvExeNam", "RepEmaDst", "RepMntId");
    Element editors = byId(queries, "query", "getTaskPageTaskRestricted");
    assertEquals("true", editors.getAttribute("server-only"));
    assertEquals("scheduler.update", editors.getAttribute("permission"));
    assertEquals(restricted, fieldsOf(editors));
    assertTrue(fieldsOf(byId(queries, "query", "getTaskPageTask")).stream().noneMatch(restricted::contains));
  }

  @Test
  void theCardsLoadWithTheScreenSoTheyShareTheOneComputationOfTheTaskPageOfItsRequest() {
    // The service computes the page once per request: a card inside a deferred tab or dialog would load in a request of
    // its own and compute it again
    List<String> loaded = new ArrayList<>();
    for (String tag : List.of("card", "grid")) {
      NodeList list = screen.getElementsByTagName(tag);
      for (int index = 0; index < list.getLength(); index++) {
        Element component = (Element) list.item(index);
        if (component.getAttribute("target-action").startsWith("getTaskPage")) {
          assertEquals("query", component.getAttribute("initial-load"), component.getAttribute("id"));
          for (Node parent = component.getParentNode(); parent instanceof Element container;
               parent = container.getParentNode()) {
            assertFalse("true".equals(container.getAttribute("deferred")), component.getAttribute("id"));
          }
          loaded.add(component.getAttribute("id"));
        }
      }
    }
    assertEquals(Set.of("CrdTskPag", "CrdTskOk", "CrdTskAvg", "CrdTskNxt", "CrdTskHis", "CrdTskLst"),
      Set.copyOf(loaded));
  }

  @Test
  void theBrowserTabKeepsTheTaskOfTheScreenSoAReloadOpensItAgain() {
    assertEquals("true", byId(screen, "criteria", TASK).getAttribute("keep-on-reload"));
  }

  @Test
  void theHeaderRunsEditsDuplicatesActivatesDeactivatesAndDeletesTheTask() {
    assertEquals(List.of("server:LchTsk", "server:getTaskPage", "filter:GrdTskExe"),
      actionsOf(byId(screen, "button", "ButTskRun")));
    assertEquals(List.of("filter:GrdTskVar", "dialog:task-variables"), actionsOf(byId(screen, "button", "ButTskRunVar")));
    assertEquals("ne", condition(byId(screen, "button", "ButTskRun"), "TskVar"));
    assertEquals("eq", condition(byId(screen, "button", "ButTskRunVar"), "TskVar"));
    assertNotNull(byAttribute(screen.getDocumentElement(), "include", "target-screen", "scheduler-launch-variables"));
    // Edit and Duplicate open the task editor in edit or copy mode with the task of the screen
    assertEquals(List.of("value:TskEdtMod=edit", "screen:scheduler-task-editor"), actionsOf(byId(screen, "button", "ButTskEdt")));
    assertEquals(List.of("value:TskEdtMod=edit", "screen:scheduler-task-editor"),
      actionsOf(byId(screen, "button", "ButTskCfgEdt")));
    for (String criterion : List.of("TskEdtMod", "TskEdtSec")) {
      assertEquals("hidden", byId(screen, "criteria", criterion).getAttribute("component"), criterion);
    }

    Element more = byId(screen, "button", "ButTskMor");
    assertEquals(List.of("show-context-menu:"), actionsOf(more));
    Map<String, List<String>> menu = new LinkedHashMap<>();
    for (Element button : children(more, "context-button")) {
      menu.put(button.getAttribute("id"), actionsOf(button));
    }
    assertEquals(List.of("server:ActSchTsk", "server:getTaskPage", "filter:CrdTskPag", "filter:CrdTskNxt"),
      menu.get("CtxTskAct"));
    assertEquals(List.of("server:DeaSchTsk", "server:getTaskPage", "filter:CrdTskPag", "filter:CrdTskNxt"),
      menu.get("CtxTskDea"));
    assertEquals(List.of("value:TskEdtMod=copy", "screen:scheduler-task-editor"), menu.get("CtxTskDup"));
    assertEquals(List.of("confirm:DelMsg", "server:DeleteSchedulerTask", "screen:scheduler-task-catalog"),
      menu.get("CtxTskDel"));
    assertEquals("eq", condition(byId(screen, "context-button", "CtxTskAct"), "TskAct"));
    assertEquals("ne", condition(byId(screen, "context-button", "CtxTskDea"), "TskAct"));
    assertEquals("scheduler.execute", byId(screen, "button", "ButTskRun").getAttribute("permission"));
    assertEquals("scheduler.update", more.getAttribute("permission"));
    assertEquals("scheduler.update", byId(screen, "button", "ButTskEdt").getAttribute("permission"));
  }

  @Test
  void theTabsAreTheOptionsOfTheirEnumeratedAndTheHistoryButtonOpensTheHistoryTab() {
    Element tab = byId(screen, "tab", "TskPagTab");
    List<String> options = children(byId(enumerated, "group", tab.getAttribute("target-action")), "option").stream()
      .map(option -> option.getAttribute("value")).toList();
    List<String> containers = children(tab, "tabcontainer").stream().map(container -> container.getAttribute("id"))
      .toList();
    assertEquals(List.of("TskPagSum", "TskPagHis", "TskPagDep", "TskPagCfg"), containers);
    assertEquals(containers, options);
    Element open = children(tab, "dependency").get(0);
    assertEquals("TskPagHis", open.getAttribute("value"));
    assertEquals("ButTskHisAll", children(open, "dependency-element").get(0).getAttribute("id"));
  }

  @Test
  void theHistoryIsTheActivityListOfTheTaskWithItsOwnCopyOfTheActivitySidePanel() {
    Element grid = byId(screen, "grid", "GrdTskExe");
    assertEquals("getActivityExecutionList", grid.getAttribute("target-action"));
    assertTrue(variableNamesOf(byId(queries, "query", "getActivityExecutionList")).contains("CrtExeTskIde"));
    assertTrue(variableNamesOf(byId(queries, "query", "getActivityStateCounts")).contains("CrtExeTskIde"));
    List<String> fields = fieldsOf(byId(queries, "query", "getActivityExecutionList"));
    for (Element column : children(grid, "column")) {
      // The «⋯» button reads no value of the row (sendable="false"): it opens the row menu
      if (!"false".equals(column.getAttribute("sendable"))) {
        assertTrue(fields.contains(column.getAttribute("name")), column.getAttribute("name"));
      }
    }
    // The panel and the last execution read the history grid of this screen, never the grids of the activity screen
    NodeList elements = screen.getElementsByTagName("dependency-element");
    for (int index = 0; index < elements.getLength(); index++) {
      String id = ((Element) elements.item(index)).getAttribute("id");
      assertFalse(Set.of("GrdExeAct", "GrdExeRun").contains(id), id);
    }
    assertEquals("GrdTskExe", byId(screen, "side-panel", "PnlTskExe").getAttribute("header-grid"));
    List<String> viewLog = actionsOf(byId(screen, "button", "ButTskLstLog"));
    assertEquals("select-first-row:GrdTskExe", viewLog.get(0));
    assertEquals("dialog:PnlTskExe", viewLog.get(viewLog.size() - 1));
  }

  @Test
  void theLogOfTheHistoryPanelIsFollowedOnlyWhileTheExecutionRunsOrIsQueued() {
    Element status = byId(screen, "criteria", "execution-log-status");
    assertEquals("hidden", status.getAttribute("component"));
    Element row = byAttribute(status, "dependency-element", "attribute", "selectedRowValue");
    assertEquals("GrdTskExe", row.getAttribute("id"));
    assertEquals("Sta", row.getAttribute("column"));

    Element enable = children(byId(screen, "widget", "executionLogViewer"), "dependency").get(0);
    assertEquals("enable-autorefresh", enable.getAttribute("target-type"));
    List<Element> elements = children(enable, "dependency-element");
    assertEquals(List.of("PnlTskExe", "execution-log-status"), elements.stream()
      .map(element -> element.getAttribute("id")).toList());
    assertEquals("open", elements.get(0).getAttribute("event"));
    assertEquals("in", elements.get(1).getAttribute("condition"));
    assertEquals(TaskStatus.JOB_RUNNING.getValue() + "," + TaskStatus.JOB_QUEUED.getValue(),
      elements.get(1).getAttribute("value"));
  }

  @Test
  void theDependenciesAreTheOneLevelFlowGraphOfTheTaskWhoseNodesOpenTheirTaskInThisScreen() {
    Element graph = byId(screen, "widget", "TskDpnGph");
    assertEquals("flow-graph", graph.getAttribute("type"));
    assertEquals("getTaskFlowGraph", graph.getAttribute("target-action"));
    Map<String, String> parameters = new LinkedHashMap<>();
    for (Element parameter : children(graph, "widget-parameter")) {
      parameters.put(parameter.getAttribute("name"), parameter.getAttribute("value"));
    }
    // A node gives its task to the criterion the screen reads (kept on reload) and opens this screen again
    assertEquals(Map.of("mode", "task", "target", "scheduler-task", "criterion", TASK), parameters);
    assertEquals("true", byId(screen, "criteria", TASK).getAttribute("keep-on-reload"));
    List<String> variables = variableNamesOf(byId(queries, "query", "getTaskFlowGraph"));
    assertTrue(variables.contains(TASK));
    assertTrue(fieldsOf(byId(queries, "query", "getTaskFlowGraph")).containsAll(List.of("lastStatus", "lastWhen", "nextRun")));
    // «Change what it depends on» opens the task editor on its «When»
    assertEquals(List.of("value:TskEdtMod=edit", "value:TskEdtSec=when", "screen:scheduler-task-editor"),
      actionsOf(byId(screen, "button", "ButTskDep")));
  }

  private static String condition(Element element, String criterion) {
    for (Element dependency : children(element, "dependency")) {
      for (Element dependencyElement : children(dependency, "dependency-element")) {
        if (criterion.equals(dependencyElement.getAttribute("id"))) {
          return dependencyElement.getAttribute("condition");
        }
      }
    }
    return "";
  }

  private static List<String> variablesOf(Element target) {
    assertNotNull(target);
    List<String> names = new ArrayList<>();
    NodeList variables = target.getElementsByTagName("variable");
    for (int index = 0; index < variables.getLength(); index++) {
      names.add(((Element) variables.item(index)).getAttribute("name"));
    }
    return names;
  }

  private static List<String> variableNamesOf(Element query) {
    return children(query, "variable").stream().map(variable -> variable.getAttribute("name")).toList();
  }

  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      String type = action.getAttribute("type");
      // A value action also tells the value it sets (the mode of the task editor, its section)
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target"))
        + ("value".equals(type) ? "=" + action.getAttribute("value") : ""));
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

  private static Element byId(Document document, String tag, String id) {
    Element element = byAttribute(document.getDocumentElement(), tag, "id", id);
    assertNotNull(element, tag + " " + id + " must exist");
    return element;
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
    try (InputStream stream = SchedulerTaskScreenDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
