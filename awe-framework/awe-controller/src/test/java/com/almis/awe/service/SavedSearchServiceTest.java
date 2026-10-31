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
import com.almis.awe.model.util.data.QueryUtil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Saved search service")
class SavedSearchServiceTest {

  private static final String PUBLISH_PROFILE = "savedSearchPublishProfile";

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

  private SavedSearchConfigProperties properties;

  @BeforeEach
  void setUp() throws Exception {
    properties = new SavedSearchConfigProperties();
    savedSearchService = new SavedSearchService(queryService, queryUtil, maintainService, menuService, properties);
    savedSearchService.setApplicationContext(applicationContext);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(queryUtil.getParameters(null, "1", "0")).thenAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    when(aweSession.getUser()).thenReturn("john");
    mockScreen();
    mockProfiles();
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
  }

  private static Criteria criterion(String id, String component) {
    Criteria criterion = new Criteria();
    criterion.setComponentType(component);
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

  /**
   * Screen "Scr" with the window "Win" (saved searches of CrtUsr and CrtThm), the window "NoSav" (advanced block without
   * saved searches) and the window "Plain" (no advanced block)
   */
  private void mockScreen() throws Exception {
    Screen screen = new Screen();
    screen.setElementList(new ArrayList<>(List.of(
      window("Win", true, criterion("CrtUsr", "text"), criterion("Key", "hidden"), advanced(criterion("CrtThm", "select"))),
      window("NoSav", null, criterion("CrtUsr", "text"), advanced(criterion("CrtThm", "select"))),
      window("Plain", true, criterion("CrtUsr", "text")))));
    when(aweElements.getScreen("Scr")).thenReturn(screen);
  }

  private void mockProfiles() throws Exception {
    when(queryService.launchPrivateQuery(eq("getSavedSearchProfileList"), any(ObjectNode.class)))
      .thenReturn(rows(row("value", "ADM", "label", "administrator"), row("value", "GNR", "label", "general")));
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
      Object value = nameAndValue[index + 1];
      row.put((String) nameAndValue[index], value instanceof Integer number ? new CellData(number) : new CellData((String) value));
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

  private ObjectNode maintainParameters(String target) throws AWException {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq(target), captor.capture());
    return captor.getValue();
  }

  private void verifyNoMaintain() throws AWException {
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  private void mockStoredSearch(int id, String user, String profile) throws Exception {
    when(queryService.launchPrivateQuery(eq("getSavedSearch"), any(ObjectNode.class)))
      .thenReturn(rows(row("id", id, "user", user == null ? "" : user, "profile", profile == null ? "" : profile,
        "screen", "Scr", "windowId", "Win", "name", "Mine")));
  }

  private static final String VALUES = "{\"CrtUsr\":[{\"value\":\"john\",\"label\":\"John\"}],\"CrtThm\":[{\"value\":1,\"label\":\"Sunset\"},"
    + "{\"value\":2,\"label\":\"Sky\"}]}";

  @Test
  @DisplayName("Saving a new own search inserts it for the session user, never from the parameters")
  void saveOwnSearch() throws Exception {
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class))).thenReturn(noRows());

    savedSearchService.saveWindowSearch("Scr", "Win", " Mine ", VALUES, null);

    ObjectNode parameters = maintainParameters("insertUserSavedSearch");
    assertEquals("Scr", parameters.get("screen").asString());
    assertEquals("Win", parameters.get("windowId").asString());
    assertEquals("Mine", parameters.get("name").asString());
    assertEquals(VALUES, parameters.get("values").asString());
    assertFalse(parameters.has("user"), "The user must come from the session, never from the parameters");
    verify(maintainService, never()).launchPrivateMaintain(eq("insertProfileSavedSearch"), any(ObjectNode.class));
    verify(aweSession, never()).setParameter(eq(PUBLISH_PROFILE), any());
  }

  @Test
  @DisplayName("Saving an own search with a name that exists overwrites its values")
  void saveOverwritesTheSameName() throws Exception {
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class))).thenReturn(rows(row("id", 7)));

    savedSearchService.saveWindowSearch("Scr", "Win", "Mine", VALUES, "");

    ObjectNode parameters = maintainParameters("updateUserSavedSearchValues");
    assertEquals(7, parameters.get("id").asInt());
    assertEquals(VALUES, parameters.get("values").asString());
    verify(maintainService, never()).launchPrivateMaintain(eq("insertUserSavedSearch"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Losing an insert race converges by updating the search")
  void saveRetriesAsUpdateWhenTheInsertLosesTheRace() throws Exception {
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class))).thenReturn(noRows(), rows(row("id", 9)));
    when(maintainService.launchPrivateMaintain(eq("insertUserSavedSearch"), any(ObjectNode.class))).thenThrow(new AWException("dup", "dup"));

    savedSearchService.saveWindowSearch("Scr", "Win", "Mine", VALUES, null);

    assertEquals(9, maintainParameters("updateUserSavedSearchValues").get("id").asInt());
  }

  @Test
  @DisplayName("A user allowed to publish saves a search for a profile, through the session guard")
  void publishAllowed() throws Exception {
    mockPublishAllowed(true);
    when(queryService.launchPrivateQuery(eq("getProfileSavedSearchByName"), any(ObjectNode.class))).thenReturn(noRows());

    savedSearchService.saveWindowSearch("Scr", "Win", "Shared", VALUES, "GNR");

    ObjectNode parameters = maintainParameters("insertProfileSavedSearch");
    assertEquals("Shared", parameters.get("name").asString());
    assertFalse(parameters.has("profile"), "The profile of the row comes from the session guard set by the service");
    InOrder order = inOrder(aweSession, maintainService);
    order.verify(aweSession).setParameter(PUBLISH_PROFILE, "GNR");
    order.verify(maintainService).launchPrivateMaintain(eq("insertProfileSavedSearch"), any(ObjectNode.class));
    order.verify(aweSession).removeParameter(PUBLISH_PROFILE);
  }

  @Test
  @DisplayName("Publishing again a name that exists for the profile overwrites it")
  void publishOverwritesTheSameName() throws Exception {
    mockPublishAllowed(true);
    when(queryService.launchPrivateQuery(eq("getProfileSavedSearchByName"), any(ObjectNode.class))).thenReturn(rows(row("id", 4)));

    savedSearchService.saveWindowSearch("Scr", "Win", "Shared", VALUES, "GNR");

    assertEquals(4, maintainParameters("updateProfileSavedSearchValues").get("id").asInt());
    verify(aweSession).removeParameter(PUBLISH_PROFILE);
  }

  @Test
  @DisplayName("Publishing is rejected without access to the publish option, or when the option is not in the menu")
  void publishRejectedWithoutPermission() throws Exception {
    mockPublishAllowed(false);
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Shared", VALUES, "GNR"));

    properties.setPublishOption("no-such-option");
    mockPublishAllowed(true);
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Shared", VALUES, "GNR"));

    verifyNoMaintain();
    verify(aweSession, never()).setParameter(eq(PUBLISH_PROFILE), any());
  }

  @Test
  @DisplayName("Publishing for a profile that does not exist is rejected")
  void publishForAnUnknownProfileIsRejected() throws Exception {
    mockPublishAllowed(true);

    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Shared", VALUES, "XYZ"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("The values keep only the criteria of the window search, and only their value and label")
  void valuesAreLimitedToTheSearchCriteria() throws Exception {
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class))).thenReturn(noRows());

    savedSearchService.saveWindowSearch("Scr", "Win", "Mine",
      "{\"CrtUsr\":[{\"value\":\"john\",\"label\":\"John\",\"extra\":true}],\"Key\":[{\"value\":\"secret\"}],\"Other\":[{\"value\":1}]}", null);

    assertEquals("{\"CrtUsr\":[{\"value\":\"john\",\"label\":\"John\"}]}", maintainParameters("insertUserSavedSearch").get("values").asString());
  }

  @Test
  @DisplayName("Values that are not a JSON object of lists of objects, or longer than 4000 characters, are rejected")
  void invalidValuesAreRejected() throws Exception {
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", "not json", null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", "[]", null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", "{\"CrtUsr\":\"john\"}", null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", "{\"CrtUsr\":[\"john\"]}", null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", null, null));
    String large = "{\"CrtUsr\":[{\"value\":\"" + "x".repeat(4000) + "\"}]}";
    AWException tooLarge = assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", large, null));
    assertEquals("ERROR_MESSAGE_SAVED_SEARCH_TOO_LARGE", tooLarge.getMessage());
    verifyNoMaintain();
  }

  @Test
  @DisplayName("Identifiers and names must not be blank nor longer than 100, and the window must offer saved searches")
  void invalidTargetsAreRejected() throws Exception {
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("", "Win", "Mine", VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", null, "Mine", VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", " ", VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "N".repeat(101), VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "NoSav", "Mine", VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Plain", "Mine", VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "NoSuchWindow", "Mine", VALUES, null));
    when(aweElements.getScreen("Missing")).thenThrow(new AWException("missing", "missing"));
    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Missing", "Win", "Mine", VALUES, null));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("Without a session user nothing is saved, renamed or deleted")
  void anonymousUserIsRejected() throws Exception {
    when(aweSession.getUser()).thenReturn(null);

    assertThrows(AWException.class, () -> savedSearchService.saveWindowSearch("Scr", "Win", "Mine", VALUES, null));
    assertThrows(AWException.class, () -> savedSearchService.renameWindowSearch("7", "New"));
    assertThrows(AWException.class, () -> savedSearchService.deleteWindowSearch("7"));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("The user renames and deletes an own search")
  void renameAndDeleteOwn() throws Exception {
    mockStoredSearch(7, "john", null);
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class))).thenReturn(noRows());

    savedSearchService.renameWindowSearch("7", " Renamed ");
    savedSearchService.deleteWindowSearch("7");

    ObjectNode rename = maintainParameters("renameUserSavedSearch");
    assertEquals(7, rename.get("id").asInt());
    assertEquals("Renamed", rename.get("name").asString());
    assertEquals(7, maintainParameters("deleteUserSavedSearch").get("id").asInt());
    verify(aweSession, never()).setParameter(eq(PUBLISH_PROFILE), any());
  }

  @Test
  @DisplayName("Renaming to a name another search of the same owner already has is rejected")
  void renameToAnExistingNameIsRejected() throws Exception {
    mockStoredSearch(7, "john", null);
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class))).thenReturn(rows(row("id", 8)));

    AWException exists = assertThrows(AWException.class, () -> savedSearchService.renameWindowSearch("7", "Other"));
    assertEquals("ERROR_MESSAGE_SAVED_SEARCH_NAME_EXISTS", exists.getMessage());
    verifyNoMaintain();
  }

  @Test
  @DisplayName("A published search is renamed and deleted only by a user allowed to publish")
  void renameAndDeletePublishedOnlyWhenAllowed() throws Exception {
    mockStoredSearch(4, null, "GNR");
    mockPublishAllowed(false);

    assertThrows(AWException.class, () -> savedSearchService.renameWindowSearch("4", "Renamed"));
    assertThrows(AWException.class, () -> savedSearchService.deleteWindowSearch("4"));
    verifyNoMaintain();

    mockPublishAllowed(true);
    when(queryService.launchPrivateQuery(eq("getProfileSavedSearchByName"), any(ObjectNode.class))).thenReturn(noRows());
    savedSearchService.renameWindowSearch("4", "Renamed");
    savedSearchService.deleteWindowSearch("4");

    assertEquals(4, maintainParameters("renameProfileSavedSearch").get("id").asInt());
    assertEquals(4, maintainParameters("deleteProfileSavedSearch").get("id").asInt());
    verify(aweSession, org.mockito.Mockito.times(2)).setParameter(PUBLISH_PROFILE, "GNR");
    verify(aweSession, org.mockito.Mockito.times(2)).removeParameter(PUBLISH_PROFILE);
  }

  @Test
  @DisplayName("The search of another user, or one that does not exist, is rejected")
  void anotherUsersSearchIsRejected() throws Exception {
    mockStoredSearch(5, "mallory", null);
    mockPublishAllowed(true);

    assertThrows(AWException.class, () -> savedSearchService.renameWindowSearch("5", "Mine"));
    assertThrows(AWException.class, () -> savedSearchService.deleteWindowSearch("5"));

    when(queryService.launchPrivateQuery(eq("getSavedSearch"), any(ObjectNode.class))).thenReturn(noRows());
    assertThrows(AWException.class, () -> savedSearchService.deleteWindowSearch("6"));
    assertThrows(AWException.class, () -> savedSearchService.deleteWindowSearch("abc"));
    assertThrows(AWException.class, () -> savedSearchService.deleteWindowSearch(null));
    verifyNoMaintain();
  }

  @Test
  @DisplayName("The list holds the own searches first and then the ones shared with the profile, each by name")
  void listOwnAndShared() throws Exception {
    when(queryService.launchPrivateQuery(eq("getUserSavedSearches"), any(ObjectNode.class)))
      .thenReturn(rows(row("id", 2, "name", "zeta", "values", "{}"), row("id", 1, "name", "Alpha", "values", VALUES)));
    when(queryService.launchPrivateQuery(eq("getProfileSavedSearches"), any(ObjectNode.class)))
      .thenReturn(rows(row("id", 3, "name", "Beta", "values", "{}", "profile", "GNR")));

    DataList list = savedSearchService.getWindowSavedSearches("Scr", "Win").getDataList();

    assertEquals(3, list.getRows().size());
    assertEquals(List.of("Alpha", "zeta", "Beta"), list.getRows().stream().map(row -> row.get("name").getStringValue()).toList());
    Map<String, CellData> own = list.getRows().get(0);
    assertEquals(1, own.get("id").getIntegerValue());
    assertEquals(VALUES, own.get("values").getStringValue());
    assertEquals("", own.get("profile").getStringValue());
    assertEquals(1, own.get("own").getIntegerValue());
    Map<String, CellData> shared = list.getRows().get(2);
    assertEquals("GNR", shared.get("profile").getStringValue());
    assertEquals(0, shared.get("own").getIntegerValue());

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq("getUserSavedSearches"), captor.capture());
    assertEquals("Scr", captor.getValue().get("screen").asString());
    assertEquals("Win", captor.getValue().get("windowId").asString());
    assertFalse(captor.getValue().has("user"));
  }

  @Test
  @DisplayName("A user who may publish lists the searches published for every profile, so it can manage them")
  void publisherListsTheSearchesPublishedForEveryProfile() throws Exception {
    mockPublishAllowed(true);
    when(queryService.launchPrivateQuery(eq("getUserSavedSearches"), any(ObjectNode.class))).thenReturn(rows());
    when(queryService.launchPrivateQuery(eq("getPublishedSavedSearches"), any(ObjectNode.class)))
      .thenReturn(rows(row("id", 4, "name", "Operators", "values", "{}", "profile", "TST"),
        row("id", 5, "name", "Admins", "values", "{}", "profile", "ADM")));

    DataList list = savedSearchService.getWindowSavedSearches("Scr", "Win").getDataList();

    assertEquals(List.of("Admins", "Operators"), list.getRows().stream().map(row -> row.get("name").getStringValue()).toList());
    assertEquals("TST", list.getRows().get(1).get("profile").getStringValue());
    assertEquals(0, list.getRows().get(1).get("own").getIntegerValue());
    verify(queryService, never()).launchPrivateQuery(eq("getProfileSavedSearches"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("The searches published for every profile are read only while the session marks a user who may publish")
  void publishedSearchesAreReadOnlyWhileThePublisherIsMarked() throws Exception {
    mockPublishAllowed(true);
    when(queryService.launchPrivateQuery(eq("getUserSavedSearches"), any(ObjectNode.class))).thenReturn(rows());
    when(queryService.launchPrivateQuery(eq("getPublishedSavedSearches"), any(ObjectNode.class))).thenAnswer(invocation -> {
      verify(aweSession).setParameter("savedSearchPublisher", "1");
      verify(aweSession, never()).removeParameter("savedSearchPublisher");
      return rows();
    });

    savedSearchService.getWindowSavedSearches("Scr", "Win");

    verify(aweSession).removeParameter("savedSearchPublisher");
  }

  @Test
  @DisplayName("A user who may not publish never gets the session mark of a publisher")
  void nonPublisherIsNeverMarkedAsPublisher() throws Exception {
    mockPublishAllowed(false);
    when(queryService.launchPrivateQuery(eq("getUserSavedSearches"), any(ObjectNode.class))).thenReturn(rows());
    when(queryService.launchPrivateQuery(eq("getProfileSavedSearches"), any(ObjectNode.class))).thenReturn(rows());

    savedSearchService.getWindowSavedSearches("Scr", "Win");

    verify(aweSession, never()).setParameter(eq("savedSearchPublisher"), any());
  }

  @Test
  @DisplayName("A user who may not publish only lists the searches published for its own profile")
  void nonPublisherListsOnlyTheSearchesOfItsProfile() throws Exception {
    mockPublishAllowed(false);
    when(queryService.launchPrivateQuery(eq("getUserSavedSearches"), any(ObjectNode.class))).thenReturn(rows());
    when(queryService.launchPrivateQuery(eq("getProfileSavedSearches"), any(ObjectNode.class))).thenReturn(rows());

    savedSearchService.getWindowSavedSearches("Scr", "Win");

    verify(queryService).launchPrivateQuery(eq("getProfileSavedSearches"), any(ObjectNode.class));
    verify(queryService, never()).launchPrivateQuery(eq("getPublishedSavedSearches"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("Without a session user there are no saved searches")
  void noSearchesWithoutUser() throws Exception {
    when(aweSession.getUser()).thenReturn(null);

    assertTrue(savedSearchService.getWindowSavedSearches("Scr", "Win").getDataList().getRows().isEmpty());
    verify(queryService, never()).launchPrivateQuery(eq("getUserSavedSearches"), any(ObjectNode.class));
  }

  @Test
  @DisplayName("The profiles to publish for are listed only for a user allowed to publish")
  void profilesOnlyWhenAllowed() throws Exception {
    mockPublishAllowed(false);
    assertTrue(savedSearchService.getSavedSearchProfiles().getDataList().getRows().isEmpty());
    verify(queryService, never()).launchPrivateQuery(eq("getSavedSearchProfileList"), any(ObjectNode.class));

    mockPublishAllowed(true);
    DataList profiles = savedSearchService.getSavedSearchProfiles().getDataList();
    assertEquals(List.of("ADM", "GNR"), profiles.getRows().stream().map(row -> row.get("value").getStringValue()).toList());
    assertEquals("general", profiles.getRows().get(1).get("label").getStringValue());
  }

  @Test
  @DisplayName("A menu that cannot be read means the user may not publish")
  void menuFailureMeansNoPermission() throws Exception {
    when(menuService.getMenuWithRestrictions()).thenThrow(new AWException("menu", "menu"));

    assertFalse(savedSearchService.canPublish());
    assertTrue(savedSearchService.getSavedSearchProfiles().getDataList().getRows().isEmpty());
  }

  @Test
  @DisplayName("The publish option is the profiles administration option by default")
  void defaultPublishOption() {
    assertEquals("profiles", new SavedSearchConfigProperties().getPublishOption());
  }
}
