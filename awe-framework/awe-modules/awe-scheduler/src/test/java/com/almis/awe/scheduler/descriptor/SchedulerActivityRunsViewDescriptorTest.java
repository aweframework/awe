package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Descriptor of the runs view of the activity screen (#993, phase 2b): the executions grouped by flow run in a tree grid
 * with the column names of the executions grid, shown instead of it by the «Group by flow» switch of the grid toolbars,
 * with its own toolbar filters and the same side panel
 */
class SchedulerActivityRunsViewDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String FLAT = "GrdExeAct";
  private static final String TREE = "GrdExeRun";
  private static final String SWITCH = "ActGrpFlw";
  private static final String SWITCH_COPY = "ActGrpFlwRun";

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
  void bothGridsAreShownOneAtATimeByTheGroupByFlowSwitchAndSendNothingWhileHidden() {
    for (String[] grid : new String[][]{{FLAT, "ne"}, {TREE, "eq"}}) {
      Element element = byId(screen, "grid", grid[0]);
      assertEquals("false", element.getAttribute("send-when-hidden"), grid[0]);
      Element show = children(element, "dependency").stream()
        .filter(dependency -> "show".equals(dependency.getAttribute("target-type"))).findFirst().orElseThrow();
      assertEquals("true", show.getAttribute("initial"), grid[0]);
      Element condition = children(show, "dependency-element").get(0);
      assertEquals(SWITCH, condition.getAttribute("id"));
      assertEquals(grid[1], condition.getAttribute("condition"));
      assertEquals("1", condition.getAttribute("value"));
    }
  }

  @Test
  void theGroupByFlowSwitchInTheToolbarOfEachGridGivesTheFiltersOfTheToolbarThatIsLeftToTheOtherViewAndReloadsIt() {
    // A slide switch, off (the flat list) on load, in the toolbar of each grid (toolbar-criteria): the one of the
    // executions grid switches the view, the one of the tree is its copy; no view quick filter
    Element viewSwitch = byId(screen, "toolbar-criteria", SWITCH);
    Element treeSwitch = byId(screen, "toolbar-criteria", SWITCH_COPY);
    for (Element toggle : List.of(viewSwitch, treeSwitch)) {
      assertEquals("checkbox", toggle.getAttribute("component"));
      assertTrue(toggle.getAttribute("style").contains("switch"), toggle.getAttribute("style"));
      assertEquals("PARAMETER_ACTIVITY_GROUP_BY_FLOW", toggle.getAttribute("label"));
      assertEquals("", toggle.getAttribute("checked"), "off on load");
    }
    assertEquals(byId(screen, "grid", FLAT), viewSwitch.getParentNode());
    assertEquals(byId(screen, "grid", TREE), treeSwitch.getParentNode());
    assertNull(byAttribute(screen.getDocumentElement(), "criteria", "id", SWITCH), "no switch over the grids");
    // The copy of the tree is turned on with the view switch and, turned off, resets it (an empty value is not copied):
    // each dependency reads one switch only, so a change never comes back to its switch
    assertEquals(List.of(), children(viewSwitch, "dependency").stream()
      .filter(dependency -> dependency.hasAttribute("source-type")).toList());
    Element copy = children(treeSwitch, "dependency").get(0);
    assertEquals("launcher", copy.getAttribute("source-type"));
    assertEquals("input", copy.getAttribute("target-type"));
    assertEquals("value", copy.getAttribute("target-action"));
    Element copied = children(copy, "dependency-element").get(0);
    assertEquals(List.of(SWITCH, "eq", "1", "value"), List.of(copied.getAttribute("id"), copied.getAttribute("condition"),
      copied.getAttribute("value"), copied.getAttribute("alias")));
    assertEquals(List.of(SWITCH_COPY + " ne 1: reset " + SWITCH), children(treeSwitch, "dependency").stream()
      .filter(dependency -> !dependency.hasAttribute("source-type")).map(SchedulerActivityRunsViewDescriptorTest::describe).toList());
    for (Element toggle : List.of(viewSwitch, treeSwitch)) {
      assertTrue(toggle.getElementsByTagName("dependency-element").getLength() > 0);
      for (Element dependency : children(toggle, "dependency")) {
        assertEquals(1, children(dependency, "dependency-element").size(), "one switch per dependency");
      }
    }

    for (String grid : List.of(FLAT, TREE)) {
      assertTrue(children(byId(screen, "grid", grid), "quick-filter").stream()
        .noneMatch(quickFilter -> quickFilter.getAttribute("id").endsWith("Vw")), grid + " has no view quick filter");
    }
    assertNull(byAttribute(screen.getDocumentElement(), "criteria", "id", "ActVw"), "the switch is the view");

    List<Element> dependencies = children(viewSwitch, "dependency").stream()
      .filter(dependency -> !dependency.hasAttribute("source-type")).toList();
    assertEquals(List.of(SWITCH + " eq 1: switchActivityToRuns, unselect-all-rows " + FLAT + ", filter " + TREE,
        SWITCH + " ne 1: switchActivityToExecutions, unselect-all-rows " + TREE + ", filter " + FLAT),
      dependencies.stream().map(SchedulerActivityRunsViewDescriptorTest::describe).toList());

    assertEquals(List.of("run", "CrtExeTsk", "CrtExeQck", "CrtExeOri", "CrtExeWin"), variables("switchActivityToRuns"));
    assertEquals(List.of("exe", "CrtRunTsk", "CrtRunQck", "CrtRunOri", "CrtRunWin"), variables("switchActivityToExecutions"));
  }

  @Test
  void theStateFilterOfTheTreeReadsItsOptionsFromAReadOnlyQueryOfItsEnumerated() {
    // A quick filter loaded from an enumerated is reloaded by the client as a query of that name: a query, as the other
    // quick filters of the screen
    Element state = byId(screen, "quick-filter", "CrtRunQck");
    assertEquals("query", state.getAttribute("initial-load"));
    Element query = byId(queries, "query", state.getAttribute("target-action"));
    assertEquals("scheduler.read", query.getAttribute("permission"));
    assertEquals("ActRunSta", query.getAttribute("enumerated"));
  }

  @Test
  void theTreeLoadsEveryRunOfItsToolbarFiltersWithTheColumnNamesOfTheExecutionsGrid() {
    Element tree = byId(screen, "grid", TREE);
    assertEquals("true", tree.getAttribute("treegrid"));
    assertEquals("id", tree.getAttribute("tree-id"));
    assertEquals("RunPar", tree.getAttribute("tree-parent"));
    assertEquals("RunLeaf", tree.getAttribute("tree-leaf"));
    assertEquals("true", tree.getAttribute("load-all"));
    assertEquals("getActivityRunList", tree.getAttribute("target-action"));
    assertEquals("", tree.getAttribute("initial-load"), "loaded when the view switches to it");

    List<String> read = variablesOf("getActivityRunList");
    assertEquals("CrtRunTsk", children(tree, "search").get(0).getAttribute("id"));
    for (Element quickFilter : children(tree, "quick-filter")) {
      String id = quickFilter.getAttribute("id");
      assertTrue(read.contains(id), id + " is not read by the runs list");
    }

    Set<String> flatColumns = columnNames(byId(screen, "grid", FLAT));
    Set<String> treeColumns = columnNames(tree);
    treeColumns.removeAll(Set.of("id", "RunPar", "RunLeaf"));
    assertEquals(flatColumns, treeColumns);
    assertEquals(List.of("screen scheduler-flow-run"), actions(column(tree, "ExeRun")));
    assertEquals(List.of("screen scheduler-task"), actions(column(tree, "TskNam")));
    assertEquals(actions(column(byId(screen, "grid", FLAT), "ExecutionLogPath")), actions(column(tree, "ExecutionLogPath")));
    assertEquals(actions(byId(screen, "context-button", "CtxExeDet")), actions(byId(screen, "context-button", "CtxRunDet")));
  }

  @Test
  void theSidePanelFollowsTheRowsOfTheTreeAsItFollowsTheRowsOfTheExecutionsGrid() {
    Element panel = byId(screen, "side-panel", "PnlExeAct");
    Set<String> flat = new LinkedHashSet<>();
    Set<String> tree = new LinkedHashSet<>();
    NodeList elements = panel.getElementsByTagName("dependency-element");
    for (int index = 0; index < elements.getLength(); index++) {
      Element element = (Element) elements.item(index);
      String key = ((Element) element.getParentNode().getParentNode()).getAttribute("id") + ":"
        + element.getAttribute("column") + element.getAttribute("event");
      if (FLAT.equals(element.getAttribute("id"))) {
        flat.add(key);
      } else if (TREE.equals(element.getAttribute("id"))) {
        tree.add(key);
      }
    }
    assertTrue(tree.containsAll(flat), "missing for the tree: " + flat.stream().filter(key -> !tree.contains(key)).toList());
  }

  private static String describe(Element dependency) {
    Element condition = children(dependency, "dependency-element").get(0);
    List<String> steps = new ArrayList<>();
    for (Element action : children(dependency, "dependency-action")) {
      steps.add("server".equals(action.getAttribute("type")) ? action.getAttribute("target-action")
        : action.getAttribute("type") + " " + action.getAttribute("target"));
    }
    return condition.getAttribute("id") + " " + condition.getAttribute("condition") + " " + condition.getAttribute("value")
      + ": " + String.join(", ", steps);
  }

  private static List<String> variables(String target) {
    Element serve = children(byAttribute(maintain.getDocumentElement(), "target", "name", target), "serve").get(0);
    return children(serve, "variable").stream()
      .map(variable -> variable.hasAttribute("value") ? variable.getAttribute("value") : variable.getAttribute("name"))
      .toList();
  }

  private static List<String> variablesOf(String query) {
    return children(byId(queries, "query", query), "variable").stream().map(variable -> variable.getAttribute("name"))
      .toList();
  }

  private static Set<String> columnNames(Element grid) {
    Set<String> names = new LinkedHashSet<>();
    children(grid, "column").forEach(column -> names.add(column.getAttribute("name")));
    return names;
  }

  private static Element column(Element grid, String name) {
    return children(grid, "column").stream().filter(column -> name.equals(column.getAttribute("name"))).findFirst()
      .orElseThrow();
  }

  private static List<String> actions(Element element) {
    return children(element, "button-action").stream().map(action -> action.getAttribute("type") + " "
      + (action.hasAttribute("target-action") ? action.getAttribute("target-action") : action.getAttribute("target")))
      .toList();
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
    try (InputStream stream = SchedulerActivityRunsViewDescriptorTest.class.getClassLoader()
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
