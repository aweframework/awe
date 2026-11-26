package com.almis.awe.scheduler.descriptor;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the Motor tab of the scheduler configuration screen ({@code scheduler-configuration}, #993,
 * mock-up 06 artboards 3A and 3B): the engine card is filled by the engine service as a card (status chip once, without
 * a second status icon, sentence, facts as tiles, technical details) full width above the schedules, which take the
 * rest of the tab (no vertical scroll). The actions are the entries of the «Acciones» menu of the header, only on the engine tab: each one asks
 * a confirmation, runs the existing engine maintain and reads the card, the status, the note, the schedules and the
 * label of the tab again, and is enabled by the status of the engine. «Detener y vaciar…» opens its own dialog, reset
 * before it opens, whose button is enabled only by the «Entiendo…» checkbox. The note of the executions waiting for a
 * confirmation is one line with its link to Actividad above the schedules grid, and the schedules grid
 * reads the triggers of the engine with its state filter, links each task to its task page and sends its values only
 * on its tab.
 */
class SchedulerConfigurationEngineDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final List<String> STATUSES = List.of("running", "paused", "paused-empty", "stopped", "unavailable");
  private static final List<String> REFRESH = List.of("filter:CrdEng", "server:getConfigurationEngine",
    "filter:CrdEngDfr", "filter:CrdEngTrg", "filter:GrdEngTrg", "filter:CfgTab");

  private static Document screen;
  private static Document queries;
  private static Document maintain;
  private static Document enumerated;
  private static Document spanish;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-configuration.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
    enumerated = parse("global/Enumerated.xml");
    spanish = parse("locale/Locale-es-ES.xml");
  }

  @Test
  void theEngineCardIsFilledByTheEngineServiceWithItsStatusSentenceFactsAndTechnicalDetails() {
    Element card = byId(screen, "card", "CrdEng");
    assertEquals("CfgEngBdy", ancestorWithId(card));
    assertEquals("SCREEN_TEXT_SCHEDULER_ENGINE_TITLE", card.getAttribute("title"));
    assertEquals("getConfigurationEngineCard", card.getAttribute("target-action"));
    Element query = byId(queries, "query", "getConfigurationEngineCard");
    assertEquals("getConfigurationEngine", query.getAttribute("service"));
    assertEquals("scheduler.read", query.getAttribute("permission"));
    // The status is told once, by the chip and its text (never by the colour alone): no icon tile repeats it
    assertEquals("", card.getAttribute("icon"));
    assertEquals(List.of("EngSta", "chip", "description", "footer", "tiles", "details", "chipTone", "detailsLabel"),
      fieldsOf(query));
    assertEquals(STATUSES, optionValues("CfgEngStaTon"));
    assertEquals("Detalles técnicos", localeValue("SCREEN_TEXT_SCHEDULER_ENGINE_DETAILS"));
  }

  @Test
  void theEngineAndTheSchedulesAreFullWidthBlocksOfTheBodyWithoutAnActionsCard() {
    // The actions card left a gap between the engine and the schedules: the actions are the menu of the header, and
    // the body holds the engine card and the schedules card one under the other, each one full width
    Element body = byId(screen, "tag", "CfgEngBdy");
    List<Element> blocks = children(body, null);
    assertEquals(2, blocks.size());
    Element block = blocks.get(0);
    assertEquals("tag", block.getTagName());
    assertEquals("CrdEng", children(block, "card").get(0).getAttribute("id"));
    // The engine card goes in a grid of cards, at its natural height: the gap after the grid keeps it apart from the
    // schedules card (both cards touched)
    assertEquals("div", block.getAttribute("type"));
    assertEquals("awe-card-grid", block.getAttribute("style"));
    assertEquals("", block.getAttribute("expandible"));
    // The schedules card goes in a grid of cards too: in React a grid of cards insets its cards (--awe-card-grid-inset),
    // so both cards in the same container keep the same width and their edges aligned in both engines
    Element schedules = blocks.get(1);
    assertEquals("tag", schedules.getTagName());
    assertEquals("div", schedules.getAttribute("type"));
    assertEquals("awe-card-grid expand", schedules.getAttribute("style"));
    assertEquals(List.of(byId(screen, "card", "CrdEngTrg")), children(schedules, null));
    assertNull(byAttribute(screen.getDocumentElement(), "card", "id", "CrdEngAct"));
    for (Element tag : descendants(body, "tag")) {
      assertNotEquals("row", tag.getAttribute("style"), "no row of cards in the engine tab");
    }
  }

  @Test
  void theTabFitsTheWindowWithTheSchedulesGridTakingTheRemainingHeight() {
    // No vertical scroll at 1600 x 900: the body does not scroll, the schedules card expands under the engine card and
    // its grid takes the free height of the card
    Element body = byId(screen, "tag", "CfgEngBdy");
    assertEquals("expand", body.getAttribute("style"));
    assertEquals("vertical", body.getAttribute("expandible"));
    Element card = byId(screen, "card", "CrdEngTrg");
    assertEquals(body, card.getParentNode().getParentNode());
    assertEquals("expand", card.getAttribute("style"));
    assertEquals("expand", byId(screen, "grid", "GrdEngTrg").getAttribute("style"));
  }

  @Test
  void theNoteOfTheSchedulesAndTheLegendShareOneLineUnderTheGrid() {
    Element note = byId(screen, "card", "CrdEngTrgNot");
    assertEquals("card-note awe-editor-help", note.getAttribute("style"));
    assertEquals("SCREEN_TEXT_SCHEDULER_ENGINE_SCHEDULES_NOTE", note.getAttribute("description"));
    Element line = (Element) note.getParentNode();
    assertEquals("col-xs-12 awe-editor-inline", line.getAttribute("style"));
    assertEquals("CrdEngTrg", ((Element) line.getParentNode()).getAttribute("id"));
    // The legend: its label and its pills apart by the gap of an inline row (existing classes only)
    Element legend = byId(screen, "tag", "EngTrgLeg");
    assertEquals(line, legend.getParentNode());
    assertEquals("awe-editor-inline awe-editor-help", legend.getAttribute("style"));
    // Each state a pill as the «Estado del trigger» cells: its icon and no dot (awe-cell-status-plain)
    assertEquals(List.of("", "awe-cell-status awe-cell-status-warning awe-cell-status-plain",
      "awe-cell-status awe-cell-status-info awe-cell-status-plain",
      "awe-cell-status awe-cell-status-danger awe-cell-status-plain",
      "awe-cell-status awe-cell-status-neutral awe-cell-status-plain"),
      children(legend, "tag").stream().map(pill -> pill.getAttribute("style")).toList());
  }

  @Test
  void eachActionOfTheHeaderMenuAsksAConfirmationRunsTheEngineMaintainAndReadsTheTabAgain() {
    // The «Acciones» menu of the header, next to the refresh and only on the engine tab
    Element menu = byId(screen, "button", "ButEng");
    assertEquals("buttons", ((Element) menu.getParentNode()).getAttribute("source"));
    assertEquals("has-menu", menu.getAttribute("style"));
    assertEquals("scheduler.execute", menu.getAttribute("permission"));
    assertEquals(List.of("show-context-menu:"), actionsOf(menu));
    assertEquals("CfgEng", shownOnTab(menu));
    assertEquals("Acciones", localeValue(menu.getAttribute("label")));
    // No tooltip: it covered the first entry of the open menu
    assertEquals("", menu.getAttribute("help"));
    // Right after the refresh of the tab
    List<Element> header = children((Element) menu.getParentNode(), "button");
    assertEquals(header.indexOf(byId(screen, "button", "ButEngRef")) + 1, header.indexOf(menu));

    Map<String, List<String>> expected = new LinkedHashMap<>();
    expected.put("CtxEngPau", chain("confirm:EngPauMsg", "server:stopScheduler"));
    expected.put("CtxEngRes", chain("confirm:EngResMsg", "server:startScheduler"));
    expected.put("CtxEngRst", chain("confirm:EngRstMsg", "server:restartScheduler"));
    expected.put("CtxEngClr", List.of("reset:DlgEngClr", "filter:CrdEngClrDfr", "dialog:DlgEngClr"));
    Map<String, List<String>> actual = new LinkedHashMap<>();
    for (Element entry : children(menu, "context-button")) {
      actual.put(entry.getAttribute("id"), actionsOf(entry));
      assertEquals("scheduler.execute", entry.getAttribute("permission"), entry.getAttribute("id"));
    }
    assertEquals(expected, actual);
    assertEquals("Detener y vaciar…", localeValue(byId(screen, "context-button", "CtxEngClr").getAttribute("label")));
    for (String target : List.of("stopScheduler", "startScheduler", "restartScheduler", "clearAndStop")) {
      assertEquals("scheduler.execute", byAttribute(maintain.getDocumentElement(), "target", "name", target)
        .getAttribute("permission"), target);
    }
    for (String message : List.of("EngPauMsg", "EngResMsg", "EngRstMsg")) {
      assertNotNull(byId(screen, "message", message));
    }
    // The header refresh, only on the engine tab, reads the same
    Element refresh = byId(screen, "button", "ButEngRef");
    assertEquals("buttons", ((Element) refresh.getParentNode()).getAttribute("source"));
    assertEquals(REFRESH, actionsOf(refresh));
    assertEquals("CfgEng", shownOnTab(refresh));
  }

  @Test
  void theActionsAreEnabledByTheStatusOfTheEngine() {
    assertEquals(List.of("running"), disabledUnless("CtxEngPau"));
    assertEquals(List.of("paused", "paused-empty"), disabledUnless("CtxEngRes"));
    assertEquals(List.of(), disabledUnless("CtxEngRst"));
    assertEquals(List.of("running", "paused"), disabledUnless("CtxEngClr"));
    // The status and the count of the waiting executions are hidden criteria, loaded with the screen and filled again
    // by the engine service (its fields are named as them)
    assertEquals("getConfigurationEngineStatus", byId(screen, "criteria", "EngSta").getAttribute("target-action"));
    assertEquals("getConfigurationEngineDeferredCount",
      byId(screen, "criteria", "EngDfrCnt").getAttribute("target-action"));
    for (String criterion : List.of("EngSta", "EngDfrCnt")) {
      Element element = byId(screen, "criteria", criterion);
      assertEquals("value", element.getAttribute("initial-load"), criterion);
      assertEquals(List.of("value"), fieldsOf(byId(queries, "query", element.getAttribute("target-action"))));
      assertTrue(fieldsOf(byId(queries, "query", "getConfigurationEngine")).contains(criterion), criterion);
    }
  }

  @Test
  void stopAndClearIsConfirmedInItsDialogOnlyOnceTheUserTicksThatTheyUnderstand() {
    Element dialog = byId(screen, "dialog", "DlgEngClr");
    assertEquals("SCREEN_TEXT_SCHEDULER_ENGINE_CLEAR_DIALOG", dialog.getAttribute("label"));
    assertEquals("¿Detener el motor y vaciar sus programaciones?",
      localeValue("SCREEN_TEXT_SCHEDULER_ENGINE_CLEAR_DIALOG"));
    // The reset of the opener clears every criterion of the dialog: the checkbox is the only one
    List<Element> criteria = descendants(dialog, "criteria");
    assertEquals(1, criteria.size());
    assertEquals("EngClrAck", criteria.get(0).getAttribute("id"));
    assertEquals("checkbox", criteria.get(0).getAttribute("component"));
    assertEquals("Entiendo que no se ejecutará ninguna tarea hasta que reanude el motor.",
      localeValue(criteria.get(0).getAttribute("label")));
    Element confirm = byId(screen, "button", "ButEngClrCnf");
    assertEquals(chain("server:clearAndStop", "close:DlgEngClr"), actionsOf(confirm));
    Element condition = children(children(confirm, "dependency").get(0), "dependency-element").get(0);
    assertEquals(List.of("EngClrAck", "ne", "1"), List.of(condition.getAttribute("id"),
      condition.getAttribute("condition"), condition.getAttribute("value")));
    assertEquals("disable", children(confirm, "dependency").get(0).getAttribute("target-type"));
    assertEquals(List.of("close-cancel:DlgEngClr"), actionsOf(byId(screen, "button", "ButEngClrCnl")));
    assertEquals("Qué deja de pasar", localeValue("SCREEN_TEXT_SCHEDULER_ENGINE_CLEAR_STOPS"));
    assertEquals("Qué no se pierde", localeValue("SCREEN_TEXT_SCHEDULER_ENGINE_CLEAR_KEEPS"));
  }

  @Test
  void theNoteOfAnExecutionWaitingForAConfirmationIsOneLineAboveTheGridWithItsLinkAndShowsOnlyWhenThereIsOne() {
    for (String id : List.of("CrdEngDfr", "CrdEngClrDfr")) {
      assertEquals("getConfigurationEngineDeferred", byId(screen, "card", id).getAttribute("target-action"), id);
    }
    assertEquals("EngDfrCnt:gt:0", showCondition(byId(screen, "card", "CrdEngClrDfr")));
    assertEquals(List.of("description"), fieldsOf(byId(queries, "query", "getConfigurationEngineDeferred")));
    // The note and «Ver en Actividad» on one line inside the schedules card, above its grid
    Element line = byId(screen, "tag", "EngDfrBar");
    Element schedules = byId(screen, "card", "CrdEngTrg");
    assertEquals(schedules, line.getParentNode());
    List<Element> body = children(schedules, null);
    assertEquals(body.indexOf(line) + 1, body.indexOf(byId(screen, "grid", "GrdEngTrg")));
    assertEquals("col-xs-12 awe-editor-inline hidden", line.getAttribute("style"));
    assertEquals(List.of("CrdEngDfr", "EngDfrAct"), children(line, null).stream()
      .map(element -> element.getAttribute("id")).toList());
    // The note takes the width of its text; the box of the button takes the free room with the button at its right,
    // so the note keeps apart from «Ver en Actividad»
    assertEquals("card-note", byId(screen, "card", "CrdEngDfr").getAttribute("style"));
    Element linkBox = byId(screen, "tag", "EngDfrAct");
    assertEquals("awe-editor-help text-right", linkBox.getAttribute("style"));
    assertEquals(linkBox, byId(screen, "button", "ButEngDfrAct").getParentNode());
    // Hidden until an execution waits: the count of the waiting executions shows it and hides it again
    List<String> toggles = new ArrayList<>();
    for (Element dependency : children(byId(screen, "criteria", "EngDfrCnt"), "dependency")) {
      Element condition = children(dependency, "dependency-element").get(0);
      Element action = children(dependency, "dependency-action").get(0);
      toggles.add(String.join(":", condition.getAttribute("id"), condition.getAttribute("condition"),
        condition.getAttribute("value"), dependency.getAttribute("invert"), action.getAttribute("type"),
        action.getAttribute("target"), action.getAttribute("target-action"), dependency.getAttribute("initial")));
    }
    assertEquals(List.of("EngDfrCnt:gt:0::remove-class:#EngDfrBar:hidden:true",
      "EngDfrCnt:gt:0:true:add-class:#EngDfrBar:hidden:true"), toggles);
    Element link = byId(screen, "button", "ButEngDfrAct");
    assertEquals(List.of("screen:scheduler-activity"), actionsOf(link));
    assertEquals(List.of("close-cancel:DlgEngClr", "screen:scheduler-activity"),
      actionsOf(byId(screen, "button", "ButEngClrAct")));
    assertEquals("Ver en Actividad", localeValue("BUTTON_SCHEDULER_ENGINE_VIEW_ACTIVITY"));
  }

  @Test
  void theSchedulesGridReadsTheTriggersOfTheEngineWithTheirStateFilterAndLinksEachTaskToItsPage() {
    Element grid = byId(screen, "grid", "GrdEngTrg");
    assertEquals("getConfigurationEngineTriggers", grid.getAttribute("target-action"));
    // The card maximizes (its header button), not the grid: the grid has no Maximize button of its own and fills the
    // body of the card, maximized or not
    Element card = byId(screen, "card", "CrdEngTrg");
    assertEquals("true", card.getAttribute("maximize"));
    assertEquals("false", grid.getAttribute("show-maximize-button"));
    assertEquals("getConfigurationEngineSchedulesTitle", card.getAttribute("target-action"));
    Element filter = children(grid, "quick-filter").get(0);
    assertEquals("CrtEngTrg", filter.getAttribute("id"));
    assertEquals("getConfigurationEngineTriggerCounts", filter.getAttribute("target-action"));
    Element variable = children(byId(queries, "query", "getConfigurationEngineTriggers"), "variable").get(0);
    assertEquals("CrtEngTrg", variable.getAttribute("name"));
    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationEngineTriggers"));
    for (Element column : children(grid, "column")) {
      String name = column.getAttribute("name");
      assertTrue(fields.contains(name), name + " is not a field of getConfigurationEngineTriggers");
      // Only the task is sent: the task page reads IdeTsk.selected
      assertEquals("IdeTsk".equals(name), !"false".equals(column.getAttribute("sendable")), name);
      assertEquals(!"IdeTsk".equals(name) && !"true".equals(column.getAttribute("hidden")),
        "false".equals(column.getAttribute("sortable")) && column.hasAttribute("label"), name);
    }
    Element task = byAttribute(grid, "column", "name", "TrgTskNam");
    assertEquals("link", task.getAttribute("component"));
    assertEquals("TrgTskRun", task.getAttribute("secondary-field"));
    assertEquals(List.of("screen:scheduler-task"), actionsOf(task));
    Element state = byAttribute(grid, "column", "name", "TrgStaLbl");
    assertEquals("TrgStaTon", state.getAttribute("status-field"));
    assertEquals("TrgStaIcn", state.getAttribute("icon-field"));
    List<String> states = List.of("normal", "paused", "blocked", "error", "complete", "none");
    for (String group : List.of("CfgEngTrgStaTxt", "CfgEngTrgStaTon", "CfgEngTrgStaIcn")) {
      assertEquals(states, optionValues(group), group);
    }
    // The servers panel has an IdeTsk column too: each grid sends its values only on its own tab
    assertEquals("false", grid.getAttribute("send-when-hidden"));
    assertEquals("CfgEng", shownOnTab(grid));
    Element serverTasks = byId(screen, "grid", "GrdSrvUse");
    assertEquals("false", serverTasks.getAttribute("send-when-hidden"));
    assertEquals("CfgSrv", shownOnTab(serverTasks));
  }

  private static List<String> chain(String... first) {
    List<String> actions = new ArrayList<>(List.of(first));
    actions.addAll(REFRESH);
    return actions;
  }

  /**
   * The statuses of the engine that enable a button: the values of its disable dependency (each one «ne»)
   */
  private static List<String> disabledUnless(String button) {
    List<String> statuses = new ArrayList<>();
    for (Element dependency : children(byId(screen, "context-button", button), "dependency")) {
      assertEquals("disable", dependency.getAttribute("target-type"), button);
      assertEquals("true", dependency.getAttribute("initial"), button);
      for (Element element : children(dependency, "dependency-element")) {
        assertEquals("EngSta", element.getAttribute("id"), button);
        assertEquals("ne", element.getAttribute("condition"), button);
        statuses.add(element.getAttribute("value"));
      }
    }
    return statuses;
  }

  private static String showCondition(Element element) {
    Element dependency = children(element, "dependency").get(0);
    assertEquals("show", dependency.getAttribute("target-type"));
    Element condition = children(dependency, "dependency-element").get(0);
    return condition.getAttribute("id") + ":" + condition.getAttribute("condition") + ":"
      + condition.getAttribute("value");
  }

  private static String shownOnTab(Element element) {
    for (Element dependency : children(element, "dependency")) {
      if ("show".equals(dependency.getAttribute("target-type"))) {
        for (Element condition : children(dependency, "dependency-element")) {
          if ("CfgTab".equals(condition.getAttribute("id")) && "eq".equals(condition.getAttribute("condition"))) {
            return condition.getAttribute("value");
          }
        }
      }
    }
    return "";
  }

  private static List<String> optionValues(String group) {
    Element element = byId(enumerated, "group", group);
    return children(element, "option").stream().map(option -> option.getAttribute("value")).toList();
  }

  private static String localeValue(String name) {
    Element element = byAttribute(spanish.getDocumentElement(), "locale", "name", name);
    assertNotNull(element, name);
    return element.getAttribute("value");
  }

  private static String ancestorWithId(Element element) {
    for (Node node = element.getParentNode(); node instanceof Element parent; node = parent.getParentNode()) {
      if (parent.hasAttribute("id") && parent.getAttribute("id").startsWith("Cfg")) {
        return parent.getAttribute("id");
      }
    }
    return "";
  }

  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      String type = action.getAttribute("type");
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target")));
    }
    return actions;
  }

  private static List<String> fieldsOf(Element query) {
    List<String> fields = new ArrayList<>();
    for (String tag : List.of("field", "computed", "compound")) {
      for (Element field : children(query, tag)) {
        fields.add(field.hasAttribute("alias") ? field.getAttribute("alias") : field.getAttribute("id"));
      }
    }
    return fields;
  }

  private static List<Element> descendants(Element parent, String tag) {
    List<Element> list = new ArrayList<>();
    NodeList nodes = parent.getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      list.add((Element) nodes.item(index));
    }
    return list;
  }

  private static List<Element> children(Element parent, String tag) {
    List<Element> list = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element && (tag == null || tag.equals(element.getTagName()))) {
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
    try (InputStream stream = SchedulerConfigurationEngineDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
