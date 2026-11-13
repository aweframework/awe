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
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.entities.screen.component.grid.AbstractGrid;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.screen.component.grid.GridSearch;
import com.almis.awe.model.entities.screen.component.grid.QuickFilter;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.savedsearch.DefaultSavedSearch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Saved searches of a grid (#912, S20-S22): the same service and storage as the searches of a window, with the grid id
 * as window id and the state of the grid as values
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Saved search service: grids")
class SavedSearchServiceGridTest {

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
    when(queryService.launchPrivateQuery(eq("getUserSavedSearchByName"), any(ObjectNode.class)))
      .thenReturn(new ServiceData().setDataList(new DataList()));
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
    mockScreen();
  }

  private static Column column(String name, String filter, String sortField, String filterFields) {
    Column column = new Column();
    column.setName(name);
    column.setFilter(filter);
    column.setField(sortField);
    column.setFilterFields(filterFields);
    return column;
  }

  private static Grid grid(String id, Boolean savedSearches, Element... children) {
    Grid grid = new Grid();
    grid.setId(id);
    grid.setSavedSearches(savedSearches);
    grid.setElementList(new ArrayList<>(List.of(children)));
    return grid;
  }

  private static QuickFilter quickFilter() {
    QuickFilter quickFilter = new QuickFilter();
    quickFilter.setId("QckSta");
    return quickFilter;
  }

  private static GridSearch search() {
    GridSearch search = new GridSearch();
    search.setId("SchUsr");
    return search;
  }

  /**
   * Screen "Scr" with the grid "GrdUsr" (saved searches; columns with a filter of values, text, date and number, one
   * without filter, one that cannot be sorted; a quick filter and a search), the grid "GrdNoSav" without saved searches
   * and the grid "GrdBare" with saved searches and neither quick filter nor search
   */
  private void mockScreen() throws Exception {
    Column unsortable = column("Eml", "text", null, null);
    unsortable.setSortable(false);
    Window window = new Window();
    window.setElementList(new ArrayList<>(List.of(
      grid("GrdUsr", true, column("Pro", "values", "IdePro", null), column("Nam", "text", null, "Nam,FulNam"),
        column("LstAcc", "date", null, null), column("Lvl", "number", null, null), column("Plain", null, null, null),
        unsortable, quickFilter(), search()),
      grid("GrdNoSav", null, column("Nam", "text", null, null)),
      grid("GrdBare", true, column("Nam", "text", null, null)))));
    Screen screen = new Screen();
    screen.setElementList(new ArrayList<>(List.of(window)));
    when(aweElements.getScreen("Scr")).thenReturn(screen);
  }

  private String savedValues() throws AWException {
    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("insertUserSavedSearch"), captor.capture());
    return captor.getValue().get("values").asString();
  }

  private void verifyNoMaintain() throws AWException {
    verify(maintainService, never()).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  private void save(String grid, String values) throws AWException {
    savedSearchService.saveWindowSearch("Scr", grid, "Mine", values, null);
  }

  @Test
  @DisplayName("A grid search keeps the column filters, their labels, the sort, the quick filter and the search, in their compact form")
  void saveGridSearch() throws Exception {
    save("GrdUsr", "{\"columnFilters\":[{\"id\":\"Pro\",\"fields\":[\"IdePro\"],\"type\":\"values\",\"values\":[\"ADM\",null],"
      + "\"empty\":true,\"extra\":1},{\"id\":\"Nam\",\"fields\":[\"Nam\",\"FulNam\"],\"type\":\"text\",\"union\":\"or\","
      + "\"conditions\":[{\"operator\":\"contains\",\"value\":\"jo\"},{\"operator\":\"empty\"}]},"
      + "{\"id\":\"LstAcc\",\"type\":\"date\",\"conditions\":[{\"operator\":\"between\",\"value\":\"01/10/2026\",\"to\":\"31/10/2026\"}]},"
      + "{\"id\":\"Lvl\",\"type\":\"number\",\"conditions\":[{\"operator\":\"gt\",\"value\":1.5}]}],"
      + "\"columnFilterLabels\":{\"Pro\":{\"ADM\":\"Administrator\"}},"
      + "\"sort\":[{\"id\":\"IdePro\",\"direction\":\"DESC\",\"order\":-1},{\"id\":\"Nam\",\"direction\":\"asc\"}],"
      + "\"quickFilter\":\"active\",\"search\":\"john\",\"other\":true}");

    ArgumentCaptor<ObjectNode> captor = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("insertUserSavedSearch"), captor.capture());
    assertEquals("GrdUsr", captor.getValue().get("windowId").asString());
    assertEquals("{\"columnFilters\":[{\"id\":\"Pro\",\"fields\":[\"IdePro\"],\"type\":\"values\",\"values\":[\"ADM\",null],\"empty\":true},"
      + "{\"id\":\"Nam\",\"fields\":[\"Nam\",\"FulNam\"],\"type\":\"text\",\"union\":\"or\","
      + "\"conditions\":[{\"operator\":\"contains\",\"value\":\"jo\"},{\"operator\":\"empty\"}]},"
      + "{\"id\":\"LstAcc\",\"fields\":[\"LstAcc\"],\"type\":\"date\",\"union\":\"and\","
      + "\"conditions\":[{\"operator\":\"between\",\"value\":\"01/10/2026\",\"to\":\"31/10/2026\"}]},"
      + "{\"id\":\"Lvl\",\"fields\":[\"Lvl\"],\"type\":\"number\",\"union\":\"and\",\"conditions\":[{\"operator\":\"gt\",\"value\":\"1.5\"}]}],"
      + "\"columnFilterLabels\":{\"Pro\":{\"ADM\":\"Administrator\"}},"
      + "\"sort\":[{\"id\":\"IdePro\",\"direction\":\"desc\"},{\"id\":\"Nam\",\"direction\":\"asc\"}],"
      + "\"quickFilter\":\"active\",\"search\":\"john\"}", captor.getValue().get("values").asString());
  }

  @Test
  @DisplayName("Filters, labels and sorts of columns the grid does not filter or sort are dropped")
  void unknownColumnsAreDropped() throws Exception {
    save("GrdUsr", "{\"columnFilters\":[{\"id\":\"Plain\",\"type\":\"text\",\"conditions\":[{\"operator\":\"eq\",\"value\":\"x\"}]},"
      + "{\"id\":\"Gone\",\"type\":\"values\",\"values\":[1]}],"
      + "\"columnFilterLabels\":{\"Gone\":{\"1\":\"One\"}},"
      + "\"sort\":[{\"id\":\"Gone\",\"direction\":\"asc\"},{\"id\":\"Eml\",\"direction\":\"asc\"},{\"id\":\"Plain\",\"direction\":\"desc\"}]}");

    assertEquals("{\"columnFilters\":[],\"columnFilterLabels\":{},\"sort\":[{\"id\":\"Plain\",\"direction\":\"desc\"}]}", savedValues());
  }

  @Test
  @DisplayName("The quick filter and the search are kept only when the grid has them")
  void quickFilterAndSearchOnlyWhenTheGridHasThem() throws Exception {
    save("GrdBare", "{\"quickFilter\":\"active\",\"search\":\"john\"}");

    assertEquals("{}", savedValues());
  }

  @ParameterizedTest(name = "{0}")
  @DisplayName("An invalid grid state is rejected and nothing is saved")
  @ValueSource(strings = {
    "[]",
    "{\"columnFilters\":{}}",
    "{\"columnFilters\":[{\"id\":\"Nam\",\"type\":\"text\",\"conditions\":[{\"operator\":\"like\",\"value\":\"x\"}]}]}",
    "{\"columnFilters\":[{\"id\":\"Nam\",\"type\":\"text\",\"conditions\":[{\"operator\":\"eq\",\"value\":\"x\"},"
      + "{\"operator\":\"eq\",\"value\":\"y\"},{\"operator\":\"eq\",\"value\":\"z\"}]}]}",
    "{\"columnFilters\":[{\"id\":\"Nam\",\"type\":\"number\",\"conditions\":[{\"operator\":\"gt\",\"value\":\"1\"}]}]}",
    "{\"columnFilters\":[{\"id\":\"Nam\",\"fields\":[\"Pwd\"],\"type\":\"text\",\"conditions\":[{\"operator\":\"eq\",\"value\":\"x\"}]}]}",
    "{\"columnFilterLabels\":[]}",
    "{\"columnFilters\":[{\"id\":\"Pro\",\"fields\":[\"IdePro\"],\"type\":\"values\",\"values\":[\"ADM\"]}],"
      + "\"columnFilterLabels\":{\"Pro\":{\"ADM\":{\"html\":true}}}}",
    "{\"sort\":{\"id\":\"Nam\"}}",
    "{\"sort\":[{\"id\":\"Nam\",\"direction\":\"up\"}]}",
    "{\"quickFilter\":[\"active\"]}",
    "{\"search\":{\"text\":\"john\"}}"
  })
  void invalidGridStatesAreRejected(String values) throws Exception {
    AWException invalid = assertThrows(AWException.class, () -> save("GrdUsr", values));

    assertEquals("ERROR_MESSAGE_INVALID_SAVED_SEARCH", invalid.getMessage());
    verifyNoMaintain();
  }

  @Test
  @DisplayName("A grid without saved-searches=\"true\", or one that is not in the screen, is rejected")
  void gridMustOfferSavedSearches() throws Exception {
    assertThrows(AWException.class, () -> save("GrdNoSav", "{}"));
    assertThrows(AWException.class, () -> save("GrdMissing", "{}"));
    verifyNoMaintain();
  }

  private static Map<String, CellData> row(Object... nameAndValue) {
    Map<String, CellData> row = new HashMap<>();
    for (int index = 0; index < nameAndValue.length; index += 2) {
      row.put((String) nameAndValue[index], new CellData(nameAndValue[index + 1]));
    }
    return row;
  }

  private void mockQuery(String query, Map<String, CellData> row) throws AWException {
    DataList dataList = new DataList();
    dataList.addRow(row);
    when(queryService.launchPrivateQuery(eq(query), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
  }

  @Test
  @DisplayName("A user marks a search of a grid with saved searches as its default for that grid, and clears it (#945, S1, S6)")
  void gridDefaultIsSetAndCleared() throws Exception {
    mockQuery("getApplicableSavedSearch", row("id", 7, "name", "Mine", "values", "{}"));

    savedSearchService.setWindowDefaultSearch("Scr", "GrdUsr", "7");
    savedSearchService.setWindowDefaultSearch("Scr", "GrdUsr", "");

    ArgumentCaptor<ObjectNode> stored = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("insertWindowDefaultSearch"), stored.capture());
    assertEquals("GrdUsr", stored.getValue().get("windowId").asString());
    assertEquals(7, stored.getValue().get("defaultSearch").asInt());
    verify(maintainService).launchPrivateMaintain(eq("clearWindowDefaultSearch"), any(ObjectNode.class));
    assertThrows(AWException.class, () -> savedSearchService.setWindowDefaultSearch("Scr", "GrdNoSav", "7"));
  }

  @Test
  @DisplayName("A publisher marks a search published for a profile as the default of that profile in the grid (#945, S1)")
  void profileDefaultOfAGrid() throws Exception {
    Option profiles = new Option();
    profiles.setName("profiles");
    Menu menu = new Menu();
    menu.setElementList(new ArrayList<>(List.of(profiles)));
    when(menuService.getMenuWithRestrictions()).thenReturn(menu);
    mockQuery("getSavedSearch", row("id", 4, "user", "", "profile", "GNR", "screen", "Scr", "windowId", "GrdUsr"));

    savedSearchService.setProfileDefaultSearch("Scr", "GrdUsr", "4", "1");

    ArgumentCaptor<ObjectNode> marked = ArgumentCaptor.forClass(ObjectNode.class);
    verify(maintainService).launchPrivateMaintain(eq("updateProfileDefaultSearch"), marked.capture());
    assertEquals("GrdUsr", marked.getValue().get("windowId").asString());
    assertEquals(4, marked.getValue().get("id").asInt());
  }

  @Test
  @DisplayName("The default search of a grid comes checked against the grid: filters and sorts of columns it no longer has are dropped (#945, S5)")
  void defaultGridSearchDropsUnknownColumns() throws Exception {
    AbstractGrid grid = (AbstractGrid) aweElements.getScreen("Scr").getElementsById("GrdUsr").get(0);
    mockQuery("getUserWindowState", row("defaultSearch", 7));
    mockQuery("getApplicableSavedSearch", row("id", 7, "name", "Mine", "values",
      "{\"columnFilters\":[{\"id\":\"Gone\",\"type\":\"values\",\"values\":[1]},"
        + "{\"id\":\"Pro\",\"fields\":[\"IdePro\"],\"type\":\"values\",\"values\":[\"ADM\"]}],"
        + "\"columnFilterLabels\":{\"Gone\":{\"1\":\"One\"}},\"sort\":[{\"id\":\"Gone\",\"direction\":\"asc\"}],\"quickFilter\":\"active\"}"));

    assertEquals(Optional.of(new DefaultSavedSearch(7, "Mine", "{\"columnFilters\":[{\"id\":\"Pro\",\"fields\":[\"IdePro\"],"
        + "\"type\":\"values\",\"values\":[\"ADM\"]}],\"columnFilterLabels\":{},\"sort\":[],\"quickFilter\":\"active\"}")),
      savedSearchService.getDefaultGridSearch("Scr", grid));

    // A state that is no longer valid for the grid is skipped: the grid starts without a default
    mockQuery("getApplicableSavedSearch", row("id", 7, "name", "Mine", "values", "{\"sort\":{}}"));
    assertTrue(savedSearchService.getDefaultGridSearch("Scr", grid).isEmpty());
  }
}
