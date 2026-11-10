package com.almis.awe.service;

import com.almis.awe.config.SavedSearchConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Advanced;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.savedsearch.DefaultSavedSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Default saved search of a window (#915, S2, S3): the default search of the session user, the default search of a
 * profile, the flags of the list and the resolution used when the screen is generated. A window search also keeps the
 * layout of its criteria (S1)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Saved search service: default search")
class SavedSearchServiceDefaultTest {

  private static final String OWNER = "savedSearchOwner";
  private static final String PUBLISH_PROFILE = "savedSearchPublishProfile";
  private static final String VALUES = "{\"CrtUsr\":[{\"value\":\"john\",\"label\":\"John\"}]}";

  private SavedSearchService savedSearchService;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private MaintainService maintainService;

  @Mock
  private MenuService menuService;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweElements aweElements;

  @BeforeEach
  void setUp() throws Exception {
    savedSearchService = new SavedSearchService(queryService, queryUtil, maintainService, menuService, new SavedSearchConfigProperties());
    savedSearchService.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(queryUtil.getParameters(null, "1", "0")).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    when(aweSession.getUser()).thenReturn("john");
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(noRows());
    mockPublishAllowed(false);
    mockScreen();
  }

  /**
   * Screen "Scr" with the window "Win" (saved searches of CrtUsr and CrtThm), the window "NoSav" (advanced block without
   * saved searches) and the grid "Grd" with saved searches
   */
  private void mockScreen() throws Exception {
    Screen screen = new Screen();
    screen.setElementList(new ArrayList<>(List.of(
      window("Win", true, criterion("CrtUsr"), advanced(criterion("CrtThm"))),
      window("NoSav", null, criterion("CrtUsr"), advanced(criterion("CrtThm"))),
      grid())));
    when(aweElements.getScreen("Scr")).thenReturn(screen);
  }

  private static Criteria criterion(String id) {
    Criteria criterion = new Criteria();
    criterion.setComponentType("text");
    criterion.setId(id);
    return criterion;
  }

  private static Window window(String id, Boolean savedSearches, Element... children) {
    Window window = new Window();
    window.setId(id);
    window.setSavedSearches(savedSearches);
    window.setElementList(new ArrayList<>(List.of(children)));
    return window;
  }

  private static Advanced advanced(Element... children) {
    Advanced advanced = new Advanced();
    advanced.setElementList(new ArrayList<>(List.of(children)));
    return advanced;
  }

  private static Grid grid() {
    Grid grid = new Grid();
    grid.setId("Grd");
    grid.setSavedSearches(true);
    grid.setElementList(new ArrayList<>());
    return grid;
  }

  /**
   * The menu of the session user, with the profiles option available or restricted
   */
  private void mockPublishAllowed(boolean allowed) throws Exception {
    Option profiles = new Option();
    profiles.setName("profiles");
    profiles.setRestricted(!allowed);
    Option tools = new Option();
    tools.setName("tools");
    tools.setElementList(new ArrayList<>(List.of(profiles)));
    Menu menu = new Menu();
    menu.setElementList(new ArrayList<>(List.of(tools)));
    when(menuService.getMenuWithRestrictions()).thenReturn(menu);
  }

  private static Map<String, CellData> row(Object... nameAndValue) {
    Map<String, CellData> row = new HashMap<>();
    for (int index = 0; index < nameAndValue.length; index += 2) {
      row.put((String) nameAndValue[index], new CellData(nameAndValue[index + 1]));
    }
    return row;
  }

  @SafeVarargs
  private static ServiceData rows(Map<String, CellData>... rows) {
    DataList dataList = new DataList();
    for (Map<String, CellData> row : rows) {
      dataList.addRow(row);
    }
    return new ServiceData().setDataList(dataList);
  }

  private static ServiceData noRows() {
    return new ServiceData().setDataList(new DataList());
  }

  private void mockQuery(String query, ServiceData result) throws AWException {
    when(queryService.launchPrivateQuery(eq(query), any(ObjectNode.class))).thenReturn(result);
  }

  private ObjectNode maintainParameters(String target) throws AWException {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq(target), captor.capture());
    return captor.getValue();
  }

  private void verifyNoMaintain() throws AWException {
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  private void mockStoredSearch(int id, String user, String profile) throws Exception {
    mockQuery("getSavedSearch", rows(row("id", id, "user", user == null ? "" : user, "profile", profile == null ? "" : profile,
      "screen", "Scr", "windowId", "Win", "name", "Mine")));
  }

  @Test
  @DisplayName("A search the user may apply becomes its default of the window, stored for the session user")
  void setUserDefault() throws Exception {
    mockQuery("getApplicableSavedSearch", rows(row("id", 7, "name", "Mine", "values", VALUES)));

    savedSearchService.setWindowDefaultSearch("Scr", "Win", "7");

    ObjectNode parameters = maintainParameters("insertWindowDefaultSearch");
    assertEquals("Scr", parameters.get("screen").asString());
    assertEquals("Win", parameters.get("windowId").asString());
    assertEquals(7, parameters.get("defaultSearch").asInt());
    assertFalse(parameters.has("user"), "The user must come from the session, never from the parameters");
    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(OWNER, "john");
    order.verify(maintainService).launchPrivateMaintain(eq("insertWindowDefaultSearch"), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(OWNER);
    ArgumentCaptor<ObjectNode> read = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("getApplicableSavedSearch"), read.capture());
    assertEquals(7, read.getValue().get("id").asInt());
    assertEquals("Win", read.getValue().get("windowId").asString());
  }

  @Test
  @DisplayName("The default of a window that already has a stored row updates only that row")
  void setUserDefaultOnAStoredRow() throws Exception {
    mockQuery("getApplicableSavedSearch", rows(row("id", 7, "name", "Mine", "values", VALUES)));
    mockQuery("getUserWindowState", rows(row("advancedOpen", 1, "defaultSearch", 3)));

    savedSearchService.setWindowDefaultSearch("Scr", "Win", "7");

    assertEquals(7, maintainParameters("updateWindowDefaultSearch").get("defaultSearch").asInt());
    verify(maintainService, never()).launchPrivateMaintain(eq("insertWindowDefaultSearch"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("An empty id clears the default of the user without reading any search")
  void clearUserDefault() throws Exception {
    savedSearchService.setWindowDefaultSearch("Scr", "Win", "");

    ObjectNode parameters = maintainParameters("clearWindowDefaultSearch");
    assertEquals("Scr", parameters.get("screen").asString());
    assertEquals("Win", parameters.get("windowId").asString());
    verify(queryService, never()).launchPrivateQuery(eq("getApplicableSavedSearch"), any(ObjectNode.class));
    verify(aweSession).removeParameter(OWNER);
  }

  @Test
  @DisplayName("A search the user may not apply (of another user, profile or window, or missing) is rejected and nothing is stored")
  void foreignSearchIsRejected() throws Exception {
    AWException notFound = assertThrows(AWException.class, () -> savedSearchService.setWindowDefaultSearch("Scr", "Win", "5"));

    assertEquals("ERROR_MESSAGE_SAVED_SEARCH_NOT_FOUND", notFound.getMessage());
    assertThrows(AWException.class, () -> savedSearchService.setWindowDefaultSearch("Scr", "Win", "abc"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("Only a window with saved searches has a default search: a grid or another window is rejected")
  void onlyWindowsWithSavedSearches() throws Exception {
    mockQuery("getApplicableSavedSearch", rows(row("id", 7, "name", "Mine", "values", VALUES)));

    assertThrows(AWException.class, () -> savedSearchService.setWindowDefaultSearch("Scr", "Grd", "7"));
    assertThrows(AWException.class, () -> savedSearchService.setWindowDefaultSearch("Scr", "NoSav", "7"));
    when(aweSession.getUser()).thenReturn(null);
    assertThrows(AWException.class, () -> savedSearchService.setWindowDefaultSearch("Scr", "Win", "7"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("Marking the default of a profile needs the publish permission: without it nothing is stored")
  void profileDefaultNeedsPublishPermission() throws Exception {
    mockPublishAllowed(false);
    mockStoredSearch(4, null, "GNR");

    AWException notAllowed = assertThrows(AWException.class, () -> savedSearchService.setProfileDefaultSearch("Scr", "Win", "4", "1"));

    assertEquals("ERROR_MESSAGE_SAVED_SEARCH_NOT_ALLOWED", notAllowed.getMessage());
    verifyNoMaintain();
    verify(aweSession, never()).setParameter(eq(PUBLISH_PROFILE), any());
  }

  @Test
  @DisplayName("Marking the default of a profile first clears the other default of that profile and window")
  void profileDefaultReplacesTheOtherOne() throws Exception {
    mockPublishAllowed(true);
    mockStoredSearch(4, null, "GNR");

    savedSearchService.setProfileDefaultSearch("Scr", "Win", "4", "1");

    ObjectNode clear = maintainParameters("clearProfileDefaultSearches");
    assertEquals("Scr", clear.get("screen").asString());
    assertEquals("Win", clear.get("windowId").asString());
    ObjectNode mark = maintainParameters("updateProfileDefaultSearch");
    assertEquals(4, mark.get("id").asInt());
    assertEquals(1, mark.get("default").asInt());
    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(PUBLISH_PROFILE, "GNR");
    order.verify(maintainService).launchPrivateMaintain(eq("clearProfileDefaultSearches"), any(ObjectNode.class));
    order.verify(maintainService).launchPrivateMaintain(eq("updateProfileDefaultSearch"), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(PUBLISH_PROFILE);
  }

  @Test
  @DisplayName("Unmarking the default of a profile changes only that search")
  void profileDefaultIsUnmarked() throws Exception {
    mockPublishAllowed(true);
    mockStoredSearch(4, null, "GNR");

    savedSearchService.setProfileDefaultSearch("Scr", "Win", "4", "0");

    assertEquals(0, maintainParameters("updateProfileDefaultSearch").get("default").asInt());
    verify(maintainService, never()).launchPrivateMaintain(eq("clearProfileDefaultSearches"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("An own search, a search of another window or an invalid mark is never the default of a profile")
  void profileDefaultOnlyForPublishedSearchesOfTheWindow() throws Exception {
    mockPublishAllowed(true);
    mockStoredSearch(7, "john", null);
    assertThrows(AWException.class, () -> savedSearchService.setProfileDefaultSearch("Scr", "Win", "7", "1"));

    mockStoredSearch(4, null, "GNR");
    assertThrows(AWException.class, () -> savedSearchService.setProfileDefaultSearch("Scr", "NoSav", "4", "1"));
    assertThrows(AWException.class, () -> savedSearchService.setProfileDefaultSearch("Scr", "Win", "4", "yes"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("The default of the user wins over the default of its profile")
  void userDefaultWinsOverProfileDefault() throws Exception {
    mockQuery("getUserWindowState", rows(row("advancedOpen", 0, "defaultSearch", 7)));
    mockQuery("getApplicableSavedSearch", rows(row("id", 7, "name", "Mine", "values", VALUES)));
    mockQuery("getProfileDefaultSavedSearch", rows(row("id", 4, "name", "Shared", "values", "{}")));

    assertEquals(Optional.of(new DefaultSavedSearch(7, "Mine", VALUES)), savedSearchService.getDefaultWindowSearch("Scr", "Win"));
    verify(queryService, never()).launchPrivateQuery(eq("getProfileDefaultSavedSearch"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("A default of the user the user may no longer apply falls back to the default of its profile")
  void staleUserDefaultFallsBackToTheProfileDefault() throws Exception {
    mockQuery("getUserWindowState", rows(row("advancedOpen", 0, "defaultSearch", 7)));
    mockQuery("getProfileDefaultSavedSearch", rows(row("id", 4, "name", "Shared", "values", "{}")));

    assertEquals(Optional.of(new DefaultSavedSearch(4, "Shared", "{}")), savedSearchService.getDefaultWindowSearch("Scr", "Win"));
  }

  @Test
  @DisplayName("Without any default, without a user or when the storage fails there is no default search")
  void noDefaultSearch() throws Exception {
    assertTrue(savedSearchService.getDefaultWindowSearch("Scr", "Win").isEmpty());

    when(queryService.launchPrivateQuery(eq("getUserWindowState"), any(ObjectNode.class))).thenThrow(new AWException("down", "down"));
    assertTrue(savedSearchService.getDefaultWindowSearch("Scr", "Win").isEmpty());

    when(aweSession.getUser()).thenReturn(null);
    assertTrue(savedSearchService.getDefaultWindowSearch("Scr", "Win").isEmpty());
  }

  @Test
  @DisplayName("Deleting a search clears it where it was the default of a user")
  void deleteClearsTheDefault() throws Exception {
    mockStoredSearch(7, "john", null);
    savedSearchService.deleteWindowSearch("7");
    assertEquals(7, maintainParameters("clearOwnSearchAsUserDefault").get("id").asInt());

    mockPublishAllowed(true);
    mockStoredSearch(4, null, "GNR");
    savedSearchService.deleteWindowSearch("4");
    assertEquals(4, maintainParameters("clearProfileSearchAsUserDefault").get("id").asInt());
    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(PUBLISH_PROFILE, "GNR");
    order.verify(maintainService).launchPrivateMaintain(eq("deleteProfileSavedSearch"), any(ObjectNode.class));
    order.verify(maintainService).launchPrivateMaintain(eq("clearProfileSearchAsUserDefault"), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(PUBLISH_PROFILE);
  }

  @Test
  @DisplayName("A window search keeps the advanced criteria it shows in the main filters, also without values")
  void layoutIsKept() throws Exception {
    savedSearchService.saveWindowSearch("Scr", "Win", "Layout", "{\"$layout\":[\"CrtThm\"]}", null);
    assertEquals("{\"$layout\":[\"CrtThm\"]}", maintainParameters("insertUserSavedSearch").get("values").asString());

    savedSearchService.saveWindowSearch("Scr", "Win", "Both", "{\"$layout\":[\"CrtThm\"],\"CrtUsr\":[{\"value\":\"john\"}]}", null);
    ArgumentCaptor<ObjectNode> saved = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService, times(2)).launchPrivateMaintain(eq("insertUserSavedSearch"), saved.capture());
    assertEquals("{\"CrtUsr\":[{\"value\":\"john\"}],\"$layout\":[\"CrtThm\"]}", saved.getValue().get("values").asString());
  }

  @Test
  @DisplayName("A layout that is not a list of distinct advanced criteria of the window is rejected and nothing is stored")
  void invalidLayoutIsRejected() throws Exception {
    for (String layout : List.of("\"CrtThm\"", "[\"CrtUsr\"]", "[\"Other\"]", "[\"CrtThm\",\"CrtThm\"]", "[1]", "[null]")) {
      AWException invalid = assertThrows(AWException.class,
        () -> savedSearchService.saveWindowSearch("Scr", "Win", "Bad", "{\"$layout\":" + layout + "}", null), layout);
      assertEquals("ERROR_MESSAGE_INVALID_SAVED_SEARCH", invalid.getMessage(), layout);
    }
    verifyNoMaintain();
  }

  @Test
  @DisplayName("The list marks the default of the user and the defaults of the profiles")
  void listMarksTheDefaults() throws Exception {
    mockQuery("getUserWindowState", rows(row("advancedOpen", 1, "defaultSearch", 2)));
    mockQuery("getUserSavedSearches", rows(row("id", 1, "name", "Alpha", "values", "{}"), row("id", 2, "name", "Beta", "values", "{}")));
    mockQuery("getProfileSavedSearches", rows(row("id", 3, "name", "Shared", "values", "{}", "profile", "GNR", "profileDefault", 1)));

    List<Map<String, CellData>> list = savedSearchService.getWindowSavedSearches("Scr", "Win").getDataList().getRows();

    assertEquals(List.of(0, 1, 0), list.stream().map(row -> row.get("default").getIntegerValue()).toList());
    assertEquals(List.of(0, 0, 1), list.stream().map(row -> row.get("profileDefault").getIntegerValue()).toList());
  }
}
