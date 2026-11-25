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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the flow run screen ({@code scheduler-flow-run}, #993): the run travels as the selected flow run
 * of the links (kept on reload) to the read services of the run, the header is one compact card of its summary with the
 * rule behind an info button, the views are one run graph each in a maximizable card, shown one at a time by the
 * segmented control of its header, and scroll inside their own area, a node opens the execution side panel of the
 * screen, every flow run link of the activity and task screens opens it and its texts
 * exist in every language of the module.
 */
class SchedulerFlowRunScreenDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String SCREEN = "scheduler-flow-run";
  private static final String RUN = "ExeRun.selected";
  private static final String PANEL = "PnlRunExe";
  private static final String STATUS = "RunExeSta";
  private static final Set<String> FRAMEWORK_KEYS = Set.of("BUTTON_SCREEN_HELP", "HELP_HELP_BUTTON", "BUTTON_CLOSE",
    "HELP_CLOSE_HELP_BUTTON", "PARAMETER_STATUS", "PARAMETER_DESCRIPTION");
  private static final String SWITCH = "RunPagVw";
  private static final String CARD = "CrdRunVw";
  // The run graph of each view and the option of the view switch that shows it
  private static final Map<String, String> VIEWS = Map.of("RunGph", "graph", "RunTml", "timeline", "RunLst", "list");
  private static final Map<String, String> OPTIONS = Map.of("RunGph", "RunPagGph", "RunTml", "RunPagTml",
    "RunLst", "RunPagLst");

  private static Document screen;
  private static Document queries;
  private static Document maintain;
  private static Document enumerated;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/" + SCREEN + ".xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    enumerated = parse("global/Enumerated.xml");
  }

  @Test
  void theRunOfTheLinksIsKeptOnReloadAndReachesTheReadServicesOfTheRunAsIdeRun() {
    Element kept = byId(screen, "criteria", RUN);
    assertEquals("hidden", kept.getAttribute("component"));
    assertEquals(RUN, kept.getAttribute("variable"));
    assertEquals("true", kept.getAttribute("keep-on-reload"));
    Element run = byId(screen, "criteria", "IdeRun");
    assertEquals("hidden", run.getAttribute("component"));
    assertEquals(RUN, run.getAttribute("variable"));
    for (String query : List.of("getFlowRunSummary", "getFlowRunGraph", "getFlowRunPageHeader", "getFlowRunPageRule")) {
      assertEquals(List.of("IdeRun"), variableNamesOf(byId(queries, "query", query)), query);
    }
  }

  @Test
  void theHeaderIsOneCompactCardOfTheSummaryWithTheRuleBehindTheInfoButtonOfItsNote() {
    Element header = byId(queries, "query", "getFlowRunPageHeader");
    Element rule = byId(queries, "query", "getFlowRunPageRule");
    List<String> summary = fieldsOf(byId(queries, "query", "getFlowRunSummary"));
    for (Element query : List.of(header, rule)) {
      assertEquals("getFlowRunSummary", query.getAttribute("service"));
      assertEquals("scheduler.read", query.getAttribute("permission"));
      assertTrue(summary.containsAll(aliasesOf(query).values()), query.getAttribute("id"));
    }
    // The run, its status, its facts as pills and its launch parameters as one line (the footer)
    assertEquals(Map.of("title", "RunLbl", "chip", "ExeStaTxt", "chipTone", "ExeStaTon", "pills", "RunFct",
      "footer", "RunParTxt"), aliasesOf(header));
    assertEquals(Map.of("title", "RunPntTxt", "icon", "ExeStaIcn", "iconTone", "ExeStaTon"), aliasesOf(rule));

    // One card at its natural height in a grid of cards, as the views card under it: both in the same container, so
    // they have the same width and edges, and the gap after the grid of the header keeps them apart (both cards
    // touched). The views are the last block of the page (the popover and the side panel go before the cards), so no
    // gap is left under them. No readouts nor grid: the note is in its body
    Element card = byId(screen, "card", "CrdRunPag");
    assertEquals("getFlowRunPageHeader", card.getAttribute("target-action"));
    Element wrapper = (Element) card.getParentNode();
    Element views = (Element) byId(screen, "card", CARD).getParentNode();
    assertEquals(List.of("div", "awe-card-grid", ""), List.of(wrapper.getAttribute("type"),
      wrapper.getAttribute("style"), wrapper.getAttribute("expandible")));
    assertEquals(List.of("div", "awe-card-grid expand", ""), List.of(views.getAttribute("type"),
      views.getAttribute("style"), views.getAttribute("expandible")));
    assertEquals(List.of(card), childElements(wrapper));
    assertEquals(List.of(byId(screen, "card", CARD)), childElements(views));
    List<Node> center = childElements((Element) wrapper.getParentNode());
    assertEquals("center", ((Element) wrapper.getParentNode()).getAttribute("source"));
    assertEquals(List.of(wrapper, views), center.subList(center.size() - 2, center.size()));
    assertEquals(0, card.getElementsByTagName("criteria").getLength());
    assertEquals(0, screen.getElementsByTagName("grid").getLength());
    assertFalse(screen.getDocumentElement().hasAttribute("target"));
    Element note = byAttribute(card, "card", "id", "CrdRunRul");
    assertNotNull(note);
    assertEquals("getFlowRunPageRule", note.getAttribute("target-action"));
    assertEquals("card-note", note.getAttribute("style"));

    // The rule of the final status: the popover of the info button right after the note, both one item at the start of
    // the bar of the card (the only one), so the popover opens below it on the left of the page, never past its edge
    Element info = byAttribute(card, "button", "id", "ButRunRul");
    assertNotNull(info);
    assertEquals(List.of("dialog:PopRunRul"), actionsOf(info, "button-action"));
    Element item = (Element) info.getParentNode();
    assertEquals("awe-action-bar", item.getAttribute("style"));
    assertEquals(List.of(note.getParentNode(), info), childElements(item));
    Element bar = (Element) item.getParentNode();
    assertEquals("awe-action-bar", bar.getAttribute("style"));
    assertEquals(List.of(item), childElements(bar));
    Element popover = byId(screen, "popover", "PopRunRul");
    assertEquals("ButRunRul", popover.getAttribute("anchor"));
    assertEquals("bottom", popover.getAttribute("position"));
    assertNotNull(byAttribute(popover, "tag", "label", "SCREEN_TEXT_FLOW_RUN_RULE"));
  }

  @Test
  void theViewsAreTheOptionsOfTheirEnumeratedAndEachOneIsOneRunGraphOfItsView() {
    // No tabs nor windows: the views are in one maximizable card with a title, at the rest of the height of the page
    assertEquals(0, screen.getElementsByTagName("tab").getLength());
    assertEquals(0, screen.getElementsByTagName("window").getLength());
    Element card = byId(screen, "card", CARD);
    assertEquals("true", card.getAttribute("maximize"));
    assertEquals("SCREEN_TEXT_FLOW_RUN_VIEWS", card.getAttribute("title"));
    assertEquals("expand", card.getAttribute("style"));
    assertEquals("center", ((Element) card.getParentNode().getParentNode()).getAttribute("source"));

    // Its header actions: the view switch, a segmented control (button-radio) of the options of its enumerated, without
    // icons, at the right of the header (before the maximize button), with «Grafo» chosen when the page opens: a radio
    // takes its value as chosen only when checked (its value alone is the value of the option), and checked chooses
    // it on every load, so nothing is kept on reload
    Element actions = children(card, "tag").get(0);
    assertEquals(actions, firstElement(card));
    assertEquals(List.of("div", "awe-card-header-actions"), List.of(actions.getAttribute("type"),
      actions.getAttribute("style")));
    Element viewSwitch = byId(screen, "criteria", SWITCH);
    assertEquals(List.of(viewSwitch), childElements(actions));
    assertEquals("button-radio", viewSwitch.getAttribute("component"));
    assertEquals(List.of("enum", SWITCH), List.of(viewSwitch.getAttribute("initial-load"),
      viewSwitch.getAttribute("target-action")));
    assertEquals(List.of("RunPagGph", "true"), List.of(viewSwitch.getAttribute("value"),
      viewSwitch.getAttribute("checked")));
    assertFalse(viewSwitch.hasAttribute("keep-on-reload"));
    assertEquals("no-label", viewSwitch.getAttribute("style"));
    List<Element> options = children(byAttribute(enumerated.getDocumentElement(), "group", "id", SWITCH), "option");
    assertEquals(List.of("RunPagGph", "RunPagTml", "RunPagLst"),
      options.stream().map(option -> option.getAttribute("value")).toList());
    assertTrue(options.stream().noneMatch(option -> option.hasAttribute("icon")), "no icons");

    // Then the views in its body, one run graph each, filling the card and scrolling inside the widget, never the page
    List<Element> graphs = children(card, "widget");
    assertEquals(List.of("RunGph", "RunTml", "RunLst"), graphs.stream().map(graph -> graph.getAttribute("id")).toList());
    assertEquals(1 + graphs.size(), childElements(card).size());
    for (Element graph : graphs) {
      assertEquals("flow-graph", graph.getAttribute("type"));
      assertEquals("expand", graph.getAttribute("style"));
      assertEquals("getFlowRunGraph", graph.getAttribute("target-action"));
      assertEquals(Map.of("mode", "run", "view", VIEWS.get(graph.getAttribute("id"))), parametersOf(graph));
    }
  }

  @Test
  void theViewSwitchShowsOneViewAtATimeAndTheGraphWhenNoneIsChosen() {
    // The timeline and the list are shown on their option; the graph is hidden on theirs only, so a switch left without
    // a choice still shows a view
    for (String view : VIEWS.keySet()) {
      Element graph = byId(screen, "widget", view);
      List<Element> visibility = children(graph, "dependency").stream()
        .filter(dependency -> dependency.hasAttribute("target-type")).toList();
      assertEquals(1, visibility.size(), view);
      Element dependency = visibility.get(0);
      assertEquals("true", dependency.getAttribute("initial"), view);
      Element element = children(dependency, "dependency-element").get(0);
      assertEquals(SWITCH, element.getAttribute("id"), view);
      String expected = "RunGph".equals(view) ? "hide in RunPagTml,RunPagLst" : "show eq " + OPTIONS.get(view);
      assertEquals(expected, dependency.getAttribute("target-type") + " " + element.getAttribute("condition") + " "
        + element.getAttribute("value"), view);
    }
  }

  @Test
  void aNodeWithAnExecutionOpensTheExecutionSidePanelOfThisScreenFromTheColumnsOfItsRow() {
    List<String> graphFields = fieldsOf(byId(queries, "query", "getFlowRunGraph"));
    for (String view : VIEWS.keySet()) {
      Element graph = byId(screen, "widget", view);
      Element click = children(graph, "dependency").get(0);
      List<Element> elements = children(click, "dependency-element");
      assertEquals(1, elements.size());
      assertEquals(graph.getAttribute("id"), elements.get(0).getAttribute("id"));
      assertEquals("click", elements.get(0).getAttribute("event"));
      assertEquals(List.of("server:loadExecutionScreen", "server:getFlowRunExecutionStatus", "filter:CrdExePrg",
        "dialog:" + PANEL), actionsOf(click, "dependency-action"));
    }
    // React keeps the node out of the selected value of the widget: nothing reads it
    NodeList elements = screen.getElementsByTagName("dependency-element");
    for (int index = 0; index < elements.getLength(); index++) {
      Element element = (Element) elements.item(index);
      assertFalse(element.hasAttribute("attribute"), element.getAttribute("id"));
    }
    // The panel reads the columns the node sends as <column>.selected: its log locator, its task and its execution
    assertTrue(variablesOf(byAttribute(maintain.getDocumentElement(), "target", "name", "loadExecutionScreen"))
      .contains("ExecutionLogPath.selected"));
    assertTrue(graphFields.containsAll(List.of("ExecutionLogPath", "IdeTsk", "ExeTsk")));
    Element status = byId(queries, "query", "getFlowRunExecutionStatus");
    assertEquals("scheduler.read", status.getAttribute("permission"));
    assertEquals(List.of("IdeTsk.selected", "ExeTsk.selected"), variableNamesOf(status));
    assertEquals(List.of(STATUS), fieldsOf(status));
    assertEquals(List.of("IdeTsk.selected", "ExeTsk.selected"),
      variableNamesOf(byId(queries, "query", "getActivityExecutionProgress")));
    Element panel = byId(screen, "side-panel", PANEL);
    assertEquals("hidden", byAttribute(panel, "criteria", "id", STATUS).getAttribute("component"));
    assertEquals(List.of("close:" + PANEL, "screen:scheduler-task"),
      actionsOf(byAttribute(panel, "button", "id", "ButRunOpnTsk"), "button-action"));
  }

  @Test
  void theLogIsFollowedAndTheExecutionStoppedOnlyWhileItRuns() {
    Element enable = children(byId(screen, "widget", "executionLogViewer"), "dependency").get(0);
    assertEquals("enable-autorefresh", enable.getAttribute("target-type"));
    List<Element> elements = children(enable, "dependency-element");
    assertEquals(List.of(PANEL, STATUS), elements.stream().map(element -> element.getAttribute("id")).toList());
    assertEquals("open", elements.get(0).getAttribute("event"));
    assertEquals("in", elements.get(1).getAttribute("condition"));
    assertEquals(TaskStatus.JOB_RUNNING.getValue() + "," + TaskStatus.JOB_QUEUED.getValue(),
      elements.get(1).getAttribute("value"));

    Element stop = byId(screen, "button", "ButRunExeStp");
    assertTrue(actionsOf(stop, "button-action").containsAll(List.of("confirm:ExeStpMsg", "server:StpTsk")));
    Element shown = children(children(stop, "dependency").get(0), "dependency-element").get(0);
    assertEquals(STATUS, shown.getAttribute("id"));
    assertEquals("eq", shown.getAttribute("condition"));
    assertEquals(String.valueOf(TaskStatus.JOB_RUNNING.getValue()), shown.getAttribute("value"));
  }

  @Test
  void everyFlowRunLinkOfTheActivityAndTaskScreensOpensThisScreen() throws Exception {
    Document activity = parse("screen/scheduler-activity.xml");
    Document task = parse("screen/scheduler-task.xml");
    for (Element grid : List.of(byId(activity, "grid", "GrdExeAct"), byId(activity, "grid", "GrdExeRun"),
      byId(task, "grid", "GrdTskExe"))) {
      Element run = byAttribute(grid, "column", "name", "ExeRun");
      assertEquals(List.of("screen:" + SCREEN), actionsOf(run, "button-action"), grid.getAttribute("id"));
    }
    assertEquals(List.of("close:PnlExeAct", "screen:" + SCREEN),
      actionsOf(byId(activity, "button", "ButActOpnRun"), "button-action"));
    assertEquals(List.of("close:PnlTskExe", "screen:" + SCREEN),
      actionsOf(byId(task, "button", "ButTskOpnRun"), "button-action"));
    assertEquals(List.of("select-first-row:GrdTskExe", "screen:" + SCREEN),
      actionsOf(byId(task, "button", "ButTskLstRun"), "button-action"));
  }

  @Test
  void everyTextOfTheScreenExistsInEveryLanguageOfTheModule() throws Exception {
    Set<String> keys = new LinkedHashSet<>();
    collectKeys(screen.getDocumentElement(), keys);
    NodeList options = byAttribute(enumerated.getDocumentElement(), "group", "id", SWITCH).getElementsByTagName("option");
    for (int index = 0; index < options.getLength(); index++) {
      keys.add(((Element) options.item(index)).getAttribute("label"));
    }
    assertTrue(keys.containsAll(List.of("MENU_SCHEDULER_FLOW_RUN", "HELP_SCREEN_SCHEDULER_FLOW_RUN")));
    for (String language : List.of("en-GB", "es-ES", "fr-FR")) {
      Set<String> defined = new LinkedHashSet<>();
      NodeList locales = parse("locale/Locale-" + language + ".xml").getElementsByTagName("locale");
      for (int index = 0; index < locales.getLength(); index++) {
        defined.add(((Element) locales.item(index)).getAttribute("name"));
      }
      Set<String> missing = new LinkedHashSet<>(keys);
      missing.removeAll(defined);
      // The texts of the framework (buttons, help, common fields) live in the locales of the generic screens
      missing.removeAll(FRAMEWORK_KEYS);
      assertEquals(Set.of(), missing, language);
    }
  }

  private static void collectKeys(Element element, Set<String> keys) {
    for (String attribute : List.of("label", "help", "title", "description", "message", "empty-title", "empty-text")) {
      String value = element.getAttribute(attribute);
      if (value.matches("[A-Z][A-Z0-9_]+")) {
        keys.add(value);
      }
    }
    for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element child) {
        collectKeys(child, keys);
      }
    }
  }

  private static Map<String, String> parametersOf(Element widget) {
    Map<String, String> parameters = new LinkedHashMap<>();
    for (Element parameter : children(widget, "widget-parameter")) {
      parameters.put(parameter.getAttribute("name"), parameter.getAttribute("value"));
    }
    return parameters;
  }

  private static Map<String, String> aliasesOf(Element query) {
    Map<String, String> aliases = new LinkedHashMap<>();
    for (Element field : children(query, "field")) {
      aliases.put(field.getAttribute("alias"), field.getAttribute("id"));
    }
    return aliases;
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

  private static List<String> actionsOf(Element element, String tag) {
    assertNotNull(element);
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, tag)) {
      String type = action.getAttribute("type");
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target")));
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

  private static Node firstElement(Element parent) {
    return childElements(parent).get(0);
  }

  private static List<Node> childElements(Element parent) {
    List<Node> list = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element) {
        list.add(node);
      }
    }
    return list;
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
    try (InputStream stream = SchedulerFlowRunScreenDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
