package com.almis.awe.scheduler.descriptor;

import com.almis.awe.scheduler.dao.task.SchedulerConfigurationPresenter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural checks on the configuration screen of the scheduler ({@code scheduler-configuration}, #993): its three
 * tabs are part of the route and come from the tabs service, the servers grid reads the servers service with its search
 * and active filter, its cells are the pills, the connection type and the link to the tasks of the server (the task
 * catalog keeps the server as its filter), the row menu and «Probar todos» launch the existing server actions with the
 * values those actions read, and «Borrar» is disabled while a task uses the server. «Nuevo servidor» and «Probar todos»
 * are buttons of the screen header on the servers tab, and the row actions live only in the row menu: the grid has no
 * buttons. The server side panel (new and edit) is filled by its loader without the secrets, saves with the existing server targets and their exact parameter
 * names (the server id comes from the selected row, never from a criterion), and lists the tasks of the server. Each
 * saved secret is its field, read-only and empty with a placeholder that tells it is saved, and its buttons in a column
 * beside it.
 */
class SchedulerConfigurationScreenDescriptorTest {

  private static final String BASE = "application/awe-scheduler/";
  private static final String GRID = "GrdSrv";
  private static final String PANEL = "PnlSrv";
  private static final String LOAD = "loadConfigurationServer";
  private static final List<String> EDIT_ACTIONS = List.of("value:SrvPnlMod=edit", "server:" + LOAD,
    "filter:GrdSrvUse", "dialog:" + PANEL);

  private static final List<String> ENGINE_TARGETS = List.of("stopScheduler", "startScheduler", "restartScheduler",
    "clearAndStop");

  private static Document screen;
  private static Document catalog;
  private static Document queries;
  private static Document maintain;

  @BeforeAll
  static void load() throws Exception {
    screen = parse("screen/scheduler-configuration.xml");
    catalog = parse("screen/scheduler-task-catalog.xml");
    queries = parse("global/Queries.xml");
    maintain = parse("global/Maintain.xml");
  }

  @Test
  void theScreenHasThreeTabsFromTheTabsServiceAndTheTabIsPartOfTheRoute() {
    assertEquals("MENU_SCHEDULER_CONFIGURATION", screen.getDocumentElement().getAttribute("label"));
    Element tab = byId(screen, "tab", "CfgTab");
    assertEquals("query", tab.getAttribute("initial-load"));
    assertEquals("getConfigurationTabs", tab.getAttribute("target-action"));
    // Another screen opens it on a tab by sending CfgTab
    assertEquals("CfgTab", tab.getAttribute("variable"));
    List<String> containers = children(tab, "tabcontainer").stream().map(container -> container.getAttribute("id"))
      .toList();
    assertEquals(List.of(SchedulerConfigurationPresenter.TAB_SERVERS, SchedulerConfigurationPresenter.TAB_CALENDARS,
      SchedulerConfigurationPresenter.TAB_ENGINE), containers);
    assertEquals("scheduler.read", byId(queries, "query", "getConfigurationTabs").getAttribute("permission"));
    // The calendars and the engine tabs fill their own bodies
    assertNotNull(byId(screen, "tag", "CfgCalBdy"));
    assertNotNull(byId(screen, "tag", "CfgEngBdy"));
  }

  @Test
  void theCardsOfTheServersAndCalendarsTabsGoInAGridOfCardsAsTheEngineCards() {
    // In React a grid of cards insets its cards (--awe-card-grid-inset): the cards of every tab go in one, so they all
    // keep the same width and edges as the engine cards in both engines, and still fill the tab
    Element servers = (Element) byId(screen, "card", "CrdSrvLst").getParentNode();
    assertCardGrid(servers);
    assertEquals(SchedulerConfigurationPresenter.TAB_SERVERS, ((Element) servers.getParentNode()).getAttribute("id"));
    Element calendars = (Element) byId(screen, "card", "CrdCalLst").getParentNode();
    assertCardGrid(calendars);
    assertEquals("CfgCalBdy", ((Element) calendars.getParentNode()).getAttribute("id"));
  }

  private static void assertCardGrid(Element container) {
    assertEquals("tag", container.getTagName());
    assertEquals("div", container.getAttribute("type"));
    assertEquals("awe-card-grid expand", container.getAttribute("style"));
    assertEquals(1, children(container, "card").size());
  }

  @Test
  void theServersGridReadsTheServersServiceWithItsSearchAndItsActiveFilter() {
    Element grid = byId(screen, "grid", GRID);
    assertEquals("getConfigurationServers", grid.getAttribute("target-action"));
    assertEquals("CrtSrvSch", children(grid, "search").get(0).getAttribute("id"));
    List<Element> quickFilters = children(grid, "quick-filter");
    assertEquals(1, quickFilters.size());
    assertEquals("CrtSrvAct", quickFilters.get(0).getAttribute("id"));
    assertEquals("getConfigurationServerActiveCounts", quickFilters.get(0).getAttribute("target-action"));
    assertEquals(List.of("CrtSrvSch", "CrtSrvAct"), variablesOf("getConfigurationServers"));
    assertEquals(List.of("CrtSrvSch"), variablesOf("getConfigurationServerActiveCounts"));

    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationServers"));
    for (Element column : children(grid, "column")) {
      if (!"MorBtn".equals(column.getAttribute("name"))) {
        assertTrue(fields.contains(column.getAttribute("name")), column.getAttribute("name")
          + " is not a field of getConfigurationServers");
      }
    }
  }

  @Test
  void theCellsAreTheConnectionTypeTheActiveAndTestPillsAndTheLinkToTheTasksOfTheServer() {
    assertColumn("SrvTypTxt", "", "", "SrvTypIcn", "SrvTypSub");
    assertColumn("SrvActTxt", "", "SrvActTon", "SrvActIcn", "");
    assertColumn("SrvTstTxt", "", "SrvTstTon", "SrvTstIcn", "SrvTstSub");
    // The host column has its own label («Servidor»); the shared «Servidor (host)» stays the label of the host field
    assertEquals("PARAMETER_SCHEDULER_SERVER_COLUMN", column("SrvHstTxt").getAttribute("label"));
    assertEquals("PARAMETER_SCHEDULER_SERVER_HOST", byId(screen, "criteria", "Hst").getAttribute("label"));
    Element usedBy = column("SrvUseTxt");
    assertEquals("link", usedBy.getAttribute("component"));
    assertEquals("SrvUseSub", usedBy.getAttribute("secondary-field"));
    // A selected calendar would filter the catalog too: its rows are unselected first
    assertEquals(List.of("unselect-all-rows:GrdCal", "screen:scheduler-task-catalog"), actionsOf(usedBy));
    // The catalog keeps the server of the selected row as its filter, and can drop it
    Element filter = byId(catalog, "criteria", "CrtTskSrv");
    assertEquals("hidden", filter.getAttribute("component"));
    assertEquals("IdeSrv.selected", filter.getAttribute("variable"));
    assertEquals(List.of("value:CrtTskSrv=", "filter:GrdTskLst"), actionsOf(byId(catalog, "button", "ButTskSrvClr")));
  }

  @Test
  void theRowMenuAndTestAllLaunchTheServerActionsWithTheValuesTheyRead() {
    Map<String, List<String>> expected = new LinkedHashMap<>();
    expected.put("CtxSrvEdt", EDIT_ACTIONS);
    expected.put("CtxSrvTst", List.of("server:TstSchSrvCnxSel", "filter:GrdSrv"));
    expected.put("CtxSrvAct", List.of("server:ActSchSrv", "filter:GrdSrv"));
    expected.put("CtxSrvDea", List.of("confirm:SrvDeaMsg", "server:DeaSchSrv", "filter:GrdSrv"));
    expected.put("CtxSrvDel", List.of("confirm:SrvDelMsg", "server:DeleteConfigurationServer", "filter:GrdSrv"));
    Map<String, List<String>> actual = new LinkedHashMap<>();
    for (Element button : children(byId(screen, "grid", GRID), "context-button")) {
      actual.put(button.getAttribute("id"), actionsOf(button));
    }
    assertEquals(expected, actual);
    assertEquals(List.of("server:TstAllSchSrvCnx", "filter:GrdSrv"), actionsOf(byId(screen, "button", "ButSrvTstAll")));

    // The row sends the server as each action reads it (Ide, Ide.selected, IdeSrv, IdeSrv.selected); no other value
    // of the row is sent, so the values of the row never reach a form of the screen
    assertEquals("Ide", variableName("ActSchSrv", "Ide"));
    assertEquals("Ide.selected", variableName("DeaSchSrv", "Ide"));
    assertEquals("IdeSrv.selected", variableName("TstSchSrvCnxSel", "serverId"));
    assertEquals("IdeSrv", variableName("DeleteConfigurationServer", "Ide"));
    for (Element column : children(byId(screen, "grid", GRID), "column")) {
      String name = column.getAttribute("name");
      assertEquals(List.of("Ide", "IdeSrv").contains(name), !"false".equals(column.getAttribute("sendable")), name);
    }
  }

  @Test
  void theGridHasNoButtonsSoTheRowActionsLiveOnlyInTheRowMenu() {
    // No selection buttons: selecting a row draws no selection bar (the rows do not move) and the grid has no bottom
    // bar; the row menu (right click and «⋯») holds the actions of the row
    Element grid = byId(screen, "grid", GRID);
    assertEquals(0, grid.getElementsByTagName("button").getLength());
    assertEquals(List.of("CtxSrvEdt", "CtxSrvTst", "CtxSrvAct", "CtxSrvDea", "CtxSrvDel"),
      children(grid, "context-button").stream().map(button -> button.getAttribute("id")).toList());
    assertEquals("show-context-menu:", actionsOf(column("MorBtn")).get(0));
    // The toolbar of the grid has its Maximize button
    assertEquals("true", grid.getAttribute("show-maximize-button"));
  }

  @Test
  void activateAndDeactivateFollowTheStateOfTheRowAndDeleteIsDisabledWhileATaskUsesTheServer() {
    assertEquals("ne", condition("CtxSrvAct", "show", "Act"));
    assertEquals("eq", condition("CtxSrvDea", "show", "Act"));
    assertEquals("eq", condition("CtxSrvDel", "enable", "SrvUseCnt"));
    assertEquals("0", value("CtxSrvDel", "enable", "SrvUseCnt"));
  }

  @Test
  void everyActionOfTheScreenDeclaresItsPermission() {
    NodeList actions = screen.getElementsByTagName("button-action");
    for (int index = 0; index < actions.getLength(); index++) {
      Element action = (Element) actions.item(index);
      String name = action.getAttribute("target-action");
      if ("server".equals(action.getAttribute("type")) && "update-model".equals(action.getAttribute("server-action"))) {
        // The engine tab reads its status again from a query of the engine
        assertEquals("scheduler.read", byId(queries, "query", name).getAttribute("permission"), name);
      } else if ("server".equals(action.getAttribute("type"))) {
        Element target = byAttribute(maintain.getDocumentElement(), "target", "name", name);
        assertNotNull(target, name);
        // The side panels read the server or the calendar with scheduler.read (silently); the actions of the engine
        // operate it (scheduler.execute); every other action writes or connects
        boolean loader = LOAD.equals(name) || "loadConfigurationCalendar".equals(name);
        boolean engine = ENGINE_TARGETS.contains(name);
        assertEquals(loader ? "scheduler.read" : engine ? "scheduler.execute" : "scheduler.update",
          target.getAttribute("permission"), name);
        assertEquals(loader ? "maintain-silent" : "maintain", action.getAttribute("server-action"), name);
      }
    }
    for (String id : List.of("CtxSrvTst", "CtxSrvAct", "CtxSrvDea", "CtxSrvDel")) {
      assertEquals("scheduler.update", byId(screen, "context-button", id).getAttribute("permission"), id);
    }
    for (String id : List.of("ButSrvTstAll", "ButSrvNew", "ButSrvPnlTst", "ButSrvPnlNew", "ButSrvPnlUpd")) {
      assertEquals("scheduler.update", byId(screen, "button", id).getAttribute("permission"), id);
    }
    assertFalse(byId(screen, "message", "SrvDeaMsg").getAttribute("message").isBlank());
  }

  @Test
  void newServerOpensTheSidePanelWithNoSelectedServerAndEditOpensItWithTheServerOfTheRow() {
    // «Nuevo servidor» drops the selected row first: no server id reaches the loader, the test nor NewSchedulerServer,
    // and the header of the panel shows its label
    assertEquals(List.of("unselect-all-rows:" + GRID, "value:SrvPnlMod=new", "server:" + LOAD, "filter:GrdSrvUse",
      "dialog:" + PANEL), actionsOf(byId(screen, "button", "ButSrvNew")));
    // «Nuevo servidor» and «Probar todos» are buttons of the screen header (as «Nuevo usuario» in the users screen),
    // shown only on the servers tab
    for (String id : List.of("ButSrvTstAll", "ButSrvNew")) {
      Element button = byId(screen, "button", id);
      assertEquals("buttons", ((Element) button.getParentNode()).getAttribute("source"), id);
      assertEquals(List.of("show:CfgTab eq CfgSrv"), dependenciesOf(button), id);
    }
    // «Editar» is the first entry of the row menu, and the name of the server opens it too
    Element first = children(byId(screen, "grid", GRID), "context-button").get(0);
    assertEquals("CtxSrvEdt", first.getAttribute("id"));
    Element name = column("Nom");
    assertEquals("link", name.getAttribute("component"));
    assertEquals(EDIT_ACTIONS, actionsOf(name));
    // The loader reads the selected server only, and fills the criteria of the panel, never a secret
    assertEquals("IdeSrv.selected", variableName(LOAD, "serverId"));
    for (String field : SchedulerConfigurationPresenter.SERVER_PANEL_FIELDS) {
      assertNotNull(byAttribute(byId(screen, "side-panel", PANEL), "criteria", "id", field), field);
    }
  }

  @Test
  void theSidePanelShowsTheServerOfTheRowInItsHeaderAndSavesWithTheExistingServerTargets() {
    Element panel = byId(screen, "side-panel", PANEL);
    assertEquals("40rem", panel.getAttribute("max-width"));
    assertEquals(GRID, panel.getAttribute("header-grid"));
    assertEquals("Nom", panel.getAttribute("header-title-field"));
    assertEquals("SrvPnlSub", panel.getAttribute("header-subtitle-field"));
    assertEquals("SrvTypIcn", panel.getAttribute("header-avatar-icon-field"));
    assertEquals("SCREEN_TEXT_SCHEDULER_SERVER_NEW", panel.getAttribute("label"));
    assertTrue(fieldsOf(byId(queries, "query", "getConfigurationServers")).contains("SrvPnlSub"));

    // The criteria of the panel are the parameters of NewSchedulerServer and UpdSchSrv, with their exact names; the
    // server id is never a criterion of the screen: UpdSchSrv and TstSchSrvCnx read the selected row of the grid
    for (String target : List.of("NewSchedulerServer", "UpdSchSrv", "TstSchSrvCnx")) {
      for (Element variable : descendants(byAttribute(maintain.getDocumentElement(), "target", "name", target), "variable")) {
        String parameter = variable.getAttribute("name");
        if (!parameter.isEmpty() && !"IdeSrv".equals(parameter)) {
          assertNotNull(byAttribute(panel, "criteria", "id", parameter), target + " reads " + parameter);
        }
      }
    }
    NodeList criteria = screen.getElementsByTagName("criteria");
    for (int index = 0; index < criteria.getLength(); index++) {
      String id = ((Element) criteria.item(index)).getAttribute("id");
      assertFalse(List.of("Ide", "IdeSrv", "Ide.selected", "IdeSrv.selected").contains(id), id);
    }
    assertEquals(List.of("check-one-selected:" + GRID, "validate:PnlSrvBdy", "server:UpdSchSrv", "close:" + PANEL,
      "filter:" + GRID, "filter:CfgTab"), actionsOf(byId(screen, "button", "ButSrvPnlUpd")));
    assertEquals(List.of("validate:PnlSrvBdy", "server:NewSchedulerServer", "close:" + PANEL, "filter:" + GRID,
      "filter:CfgTab"), actionsOf(byId(screen, "button", "ButSrvPnlNew")));
    assertEquals(List.of("validate:PnlSrvBdy", "server:TstSchSrvCnx"), actionsOf(byId(screen, "button", "ButSrvPnlTst")));
    assertEquals(List.of("close-cancel:" + PANEL), actionsOf(byId(screen, "button", "ButSrvPnlCnl")));
  }

  @Test
  void eachSavedSecretIsAReadOnlyEmptyFieldWithItsButtonsBesideItAndTheButtonsSetTheFlagsOfUpdSchSrv() {
    // «Cambiar» and «Reemplazar…» make the field editable (its flag to 0); the update flag is 1 only when something is
    // typed (a blank field keeps the saved secret)
    assertEquals(List.of("value:SrvPwdSav=0"), actionsOf(byId(screen, "button", "ButSrvPwdChg")));
    assertEquals(List.of("value:SrvKeySav=0"), actionsOf(byId(screen, "button", "ButSrvKeyChg")));
    assertEquals(List.of("value:SrvKpwSav=0"), actionsOf(byId(screen, "button", "ButSrvKpwChg")));
    // «Quitar» removes the saved key with its passphrase (FlgSshKeyDel)
    assertEquals(List.of("value:FlgSshKeyDel=1", "value:SrvKeySav=0", "value:SrvKpwSav=0"),
      actionsOf(byId(screen, "button", "ButSrvKeyDel")));
    // Each secret field is always shown: read-only while its secret is saved (its flag is 1), with a placeholder that
    // tells it is saved, and editable without placeholder otherwise. Its buttons sit in a column beside it, shown only
    // while it is saved: field and buttons are two columns of a line that aligns them at the bottom (awe-editor-join),
    // and the field is no awe-editor-part, so its input keeps its rounded corners and the columns keep their gap
    Map<String, List<String>> secrets = new LinkedHashMap<>();
    secrets.put("SshPwd", List.of("SrvPwdSav", "SCREEN_TEXT_SCHEDULER_SERVER_SECRET_PLACEHOLDER", "ButSrvPwdChg"));
    secrets.put("SshKey", List.of("SrvKeySav", "SCREEN_TEXT_SCHEDULER_SERVER_KEY_PLACEHOLDER", "ButSrvKeyChg",
      "ButSrvKeyDel"));
    secrets.put("SshKeyPass", List.of("SrvKpwSav", "SCREEN_TEXT_SCHEDULER_SERVER_SECRET_PLACEHOLDER", "ButSrvKpwChg"));
    for (Map.Entry<String, List<String>> secret : secrets.entrySet()) {
      String field = secret.getKey();
      String flag = secret.getValue().get(0);
      Element criterion = byId(screen, "criteria", field);
      assertEquals(List.of("set-readonly:" + flag + " eq 1"), dependenciesOf(criterion).stream()
        .filter(dependency -> !dependency.startsWith("attribute:")).toList(), field);
      assertEquals("true", children(criterion, "dependency").get(0).getAttribute("initial"), field);
      assertEquals(List.of(":" + secret.getValue().get(1) + " when " + flag + " eq 1", "invert: when " + flag + " eq 1"),
        placeholdersOf(criterion), field);
      Element wrapper = (Element) criterion.getParentNode();
      assertEquals("awe-editor-join", wrapper.getAttribute("style"), field);
      assertTrue(criterion.getAttribute("style").startsWith("col-xs-8"), field);
      NodeList inside = wrapper.getElementsByTagName("*");
      for (int index = 0; index < inside.getLength(); index++) {
        assertFalse(((Element) inside.item(index)).getAttribute("style").contains("awe-editor-part"), field);
      }
      for (String id : secret.getValue().subList(2, secret.getValue().size())) {
        Element button = byId(screen, "button", id);
        Element buttons = (Element) button.getParentNode();
        assertTrue(buttons.getAttribute("style").startsWith("col-xs-4"), id);
        assertEquals(wrapper, buttons.getParentNode(), id + " sits beside " + field);
        assertEquals(criterion, previousElement(buttons), id + " sits beside " + field);
        assertEquals(List.of("show:" + flag + " eq 1"), dependenciesOf(button), id);
        assertEquals("true", children(button, "dependency").get(0).getAttribute("initial"), id);
      }
      // The saved flag only holds its value: no box nor text of a saved secret any more
      assertTrue(children(byId(screen, "criteria", flag), "dependency").isEmpty(), flag);
      assertEquals(null, byAttribute(screen.getDocumentElement(), "tag", "id", flag + "Box"), flag);
    }
    for (String text : List.of("SrvPwdViw", "SrvKeyViw", "SrvKpwViw")) {
      assertEquals(null, byAttribute(screen.getDocumentElement(), "criteria", "id", text), text);
      assertFalse(SchedulerConfigurationPresenter.SERVER_PANEL_FIELDS.contains(text), text);
    }
    // The password goes on its own line below the user (the user is a row of its own), half of the row as the user
    Element password = (Element) byId(screen, "criteria", "SshPwd").getParentNode();
    assertTrue(byId(screen, "criteria", "SshPwd").getAttribute("style").contains("col-sm-6"));
    Element user = byId(screen, "criteria", "SshUsr");
    assertEquals("row", ((Element) user.getParentNode()).getAttribute("style"));
    assertEquals(user.getParentNode(), previousElement(password));
    for (String flag : List.of("FlgSshPwdUpd", "FlgSshKeyUpd", "FlgSshKeyPassUpd", "FlgSshKeyDel")) {
      assertEquals("hidden", byId(screen, "criteria", flag).getAttribute("component"), flag);
    }
    // Each update flag is 1 while its field is not empty and 0 while it is: a condition on the field, never a formule
    // that interpolates the secret (a multi-line key or a quote in a password would break it and keep the flag at 0)
    Map<String, String> flags = Map.of("FlgSshPwdUpd", "SshPwd", "FlgSshKeyUpd", "SshKey", "FlgSshKeyPassUpd",
      "SshKeyPass");
    for (Map.Entry<String, String> flag : flags.entrySet()) {
      List<String> dependencies = new ArrayList<>();
      for (Element dependency : children(byId(screen, "criteria", flag.getKey()), "dependency")) {
        assertFalse(dependency.hasAttribute("formule"), flag.getKey());
        for (Element element : children(dependency, "dependency-element")) {
          dependencies.add(dependency.getAttribute("source-type") + ":" + dependency.getAttribute("value") + " when "
            + element.getAttribute("id") + " " + element.getAttribute("condition"));
        }
      }
      assertEquals(List.of("value:1 when " + flag.getValue() + " is not empty",
        "value:0 when " + flag.getValue() + " is empty"), dependencies, flag.getKey());
    }
    assertEquals("password", byId(screen, "criteria", "SshPwd").getAttribute("component"));
    assertEquals("password", byId(screen, "criteria", "SshKeyPass").getAttribute("component"));
    assertEquals("textarea", byId(screen, "criteria", "SshKey").getAttribute("component"));
    Element type = byId(screen, "criteria", "Pro");
    assertEquals("button-radio", type.getAttribute("component"));
    assertTrue(type.getAttribute("style").contains("awe-choice-cards"));
    assertEquals("getConfigurationServerTypes", type.getAttribute("target-action"));
    assertEquals("scheduler.read", byId(queries, "query", "getConfigurationServerTypes").getAttribute("permission"));
  }

  @ParameterizedTest
  @CsvSource({"es-ES, Clave guardada", "en-GB, Saved key", "fr-FR, Clé enregistrée"})
  void thePlaceholdersOfTheSavedSecretsAreInEveryLanguage(String language, String key) throws Exception {
    Document locale = parse("locale/Locale-" + language + ".xml");
    assertEquals("••••••••", localeValue(locale, "SCREEN_TEXT_SCHEDULER_SERVER_SECRET_PLACEHOLDER"));
    assertEquals(key, localeValue(locale, "SCREEN_TEXT_SCHEDULER_SERVER_KEY_PLACEHOLDER"));
    // The texts of the old saved boxes are gone
    for (String old : List.of("SCREEN_TEXT_SCHEDULER_SERVER_SECRET_SAVED", "SCREEN_TEXT_SCHEDULER_SERVER_KEY_SAVED")) {
      assertEquals(null, byAttribute(locale.getDocumentElement(), "locale", "name", old), old);
    }
  }

  private static String localeValue(Document locale, String name) {
    Element element = byAttribute(locale.getDocumentElement(), "locale", "name", name);
    assertNotNull(element, name);
    return element.getAttribute("value");
  }

  @Test
  void theSidePanelListsTheTasksOfTheServerWithALinkToEachTaskPage() {
    Element grid = byId(screen, "grid", "GrdSrvUse");
    assertEquals("getConfigurationServerTasks", grid.getAttribute("target-action"));
    assertEquals(List.of("IdeSrv.selected"), variablesOf("getConfigurationServerTasks"));
    assertEquals("scheduler.read", byId(queries, "query", "getConfigurationServerTasks").getAttribute("permission"));
    Element task = byAttribute(grid, "column", "name", "TskNam");
    assertNotNull(task);
    assertEquals("link", task.getAttribute("component"));
    assertEquals(List.of("close:" + PANEL, "screen:scheduler-task"), actionsOf(task));
    // The task page reads IdeTsk.selected: the only column of the list that is sent
    List<String> fields = fieldsOf(byId(queries, "query", "getConfigurationServerTasks"));
    for (Element column : children(grid, "column")) {
      String columnName = column.getAttribute("name");
      assertTrue(fields.contains(columnName), columnName);
      assertEquals("IdeTsk".equals(columnName), !"false".equals(column.getAttribute("sendable")), columnName);
    }
    // The server of the panel is read by the loader only: its query has no client
    assertEquals("true", byId(queries, "query", "getConfigurationServer").getAttribute("server-only"));
  }

  private static List<Element> descendants(Element parent, String tag) {
    List<Element> list = new ArrayList<>();
    NodeList nodes = parent.getElementsByTagName(tag);
    for (int index = 0; index < nodes.getLength(); index++) {
      list.add((Element) nodes.item(index));
    }
    return list;
  }

  private static List<String> dependenciesOf(Element element) {
    List<String> dependencies = new ArrayList<>();
    for (Element dependency : children(element, "dependency")) {
      List<String> elements = new ArrayList<>();
      for (Element item : children(dependency, "dependency-element")) {
        elements.add(String.join(" ", item.getAttribute("id"), item.getAttribute("column"), item.getAttribute("attribute"),
          item.getAttribute("event"), item.getAttribute("condition"), item.getAttribute("value")).trim()
          .replaceAll(" +", " "));
      }
      dependencies.add(dependency.getAttribute("target-type") + ":" + String.join(", ", elements));
    }
    return dependencies;
  }

  private static List<String> placeholdersOf(Element criterion) {
    List<String> placeholders = new ArrayList<>();
    for (Element dependency : children(criterion, "dependency")) {
      if ("attribute".equals(dependency.getAttribute("target-type"))) {
        assertEquals("placeholder", dependency.getAttribute("target-action"));
        assertEquals("value", dependency.getAttribute("source-type"));
        assertEquals("true", dependency.getAttribute("initial"));
        Element element = children(dependency, "dependency-element").get(0);
        placeholders.add(("true".equals(dependency.getAttribute("invert")) ? "invert" : "") + ":"
          + dependency.getAttribute("value") + " when " + element.getAttribute("id") + " "
          + element.getAttribute("condition") + " " + element.getAttribute("value"));
      }
    }
    return placeholders;
  }

  private static Element previousElement(Element element) {
    Node node = element.getPreviousSibling();
    while (node != null && !(node instanceof Element)) {
      node = node.getPreviousSibling();
    }
    return (Element) node;
  }

  private static String condition(String button, String targetType, String column) {
    return dependencyElement(button, targetType, column).getAttribute("condition");
  }

  private static String value(String button, String targetType, String column) {
    return dependencyElement(button, targetType, column).getAttribute("value");
  }

  private static Element dependencyElement(String button, String targetType, String column) {
    for (Element dependency : children(byId(screen, "context-button", button), "dependency")) {
      if (targetType.equals(dependency.getAttribute("target-type"))) {
        for (Element element : children(dependency, "dependency-element")) {
          if (column.equals(element.getAttribute("column"))) {
            return element;
          }
        }
      }
    }
    throw new AssertionError(button + " has no " + targetType + " dependency on " + column);
  }

  private static String variableName(String target, String variable) {
    Element element = byAttribute(maintain.getDocumentElement(), "target", "name", target);
    assertNotNull(element, target);
    Element definition = byAttribute(element, "variable", "id", variable);
    assertNotNull(definition, target + " " + variable);
    return definition.getAttribute("name");
  }

  private static void assertColumn(String name, String component, String status, String icon, String secondary) {
    Element column = column(name);
    assertEquals(component, column.getAttribute("component"), name);
    assertEquals(status, column.getAttribute("status-field"), name);
    assertEquals(icon, column.getAttribute("icon-field"), name);
    assertEquals(secondary, column.getAttribute("secondary-field"), name);
  }

  private static List<String> variablesOf(String query) {
    return children(byId(queries, "query", query), "variable").stream().map(variable -> variable.getAttribute("name"))
      .toList();
  }

  private static Element column(String name) {
    Element column = byAttribute(byId(screen, "grid", GRID), "column", "name", name);
    assertNotNull(column, name);
    return column;
  }

  private static List<String> actionsOf(Element element) {
    List<String> actions = new ArrayList<>();
    for (Element action : children(element, "button-action")) {
      String type = action.getAttribute("type");
      actions.add(type + ":" + ("server".equals(type) ? action.getAttribute("target-action") : action.getAttribute("target"))
        + ("value".equals(type) ? "=" + action.getAttribute("value") : ""));
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
    try (InputStream stream = SchedulerConfigurationScreenDescriptorTest.class.getClassLoader()
      .getResourceAsStream(BASE + path)) {
      assertNotNull(stream, path);
      return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
    }
  }
}
