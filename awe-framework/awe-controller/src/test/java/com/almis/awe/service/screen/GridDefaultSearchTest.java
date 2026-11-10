package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import com.almis.awe.service.SavedSearchService;
import com.almis.awe.service.WindowStateService;
import com.almis.awe.service.savedsearch.DefaultSavedSearch;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Default saved search of a grid, applied before its initial load (#945, S2, S5, S6): when keep-criteria brought nothing
 * for the grid, its quick filter and search fill their criteria, its sort and column filters go to the data of the grid,
 * and only the initial load of that grid gets them. The grid tells the clients the search applied
 */
@DisplayName("Default search of a grid")
class GridDefaultSearchTest {

  private static final String DATA = ".data";
  private static final String COLUMN_FILTERS = "[{\"id\":\"Nam\",\"fields\":[\"Nam\"],\"type\":\"text\",\"union\":\"and\","
    + "\"conditions\":[{\"operator\":\"contains\",\"value\":\"jo\"}]}]";
  private static final String SORT = "[{\"id\":\"Nam\",\"direction\":\"desc\"}]";
  private static final String STATE = "{\"columnFilters\":" + COLUMN_FILTERS + ",\"columnFilterLabels\":{},\"sort\":" + SORT
    + ",\"quickFilter\":\"1\",\"search\":\"john\"}";

  private final JsonMapper mapper = new JsonMapper();
  private SavedSearchService savedSearchService;
  private InitialLoadDao initialLoadDao;
  private AweRequest request;
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() throws Exception {
    savedSearchService = mock(SavedSearchService.class);
    when(savedSearchService.getDefaultGridSearch(eq("Scr"), any())).thenReturn(Optional.of(new DefaultSavedSearch(9, "Mine", STATE)));

    request = new AweRequest(mock(HttpServletRequest.class), null, mapper);
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweRequest.class)).thenReturn(request);
    when(context.getBean(AweElements.class)).thenReturn(mock(AweElements.class, RETURNS_DEEP_STUBS));
    initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any()))
      .thenAnswer(invocation -> CompletableFuture.completedFuture(new ServiceData().addVariable(AweConstants.ACTION_DATA, new CellData(new DataList()))));
    BaseConfigProperties properties = new BaseConfigProperties();
    ScreenModelGenerator screenModelGenerator = new ScreenModelGenerator(initialLoadDao, properties);
    screenModelGenerator.setApplicationContext(context);
    generator = new ScreenComponentGenerator(request, screenModelGenerator, mock(InitialLoadDao.class), mock(AweElementsDao.class),
      properties, mock(ScreenConfigurationGenerator.class), mock(GridLayoutService.class), mock(WindowStateService.class));
    generator.setApplicationContext(context);
  }

  /**
   * Screen "Scr": the window "WinSch" with saved searches (CrtNam, then the advanced CrtCod), the grid "GrdUsr" with saved
   * searches (quick filter QckSta, search SchUsr, columns Nam and Sta) and the grid "GrdOth" without them
   */
  private static Screen screen() {
    XStream xstream = new XStream(new DomDriver());
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Screen.class);
    Screen screen = (Screen) xstream.fromXML("<screen template=\"window\"><tag source=\"center\">"
      + "<window id=\"WinSch\" saved-searches=\"true\"><criteria id=\"CrtNam\" component=\"text\"/>"
      + "<advanced id=\"AdvSch\"><criteria id=\"CrtCod\" component=\"text\"/></advanced></window>"
      + "<window id=\"WinGrd\">"
      + "<grid id=\"GrdUsr\" server-action=\"data\" target-action=\"UsrLst\" initial-load=\"query\" saved-searches=\"true\">"
      + "<quick-filter id=\"QckSta\"/><search id=\"SchUsr\"/>"
      + "<column name=\"Nam\" sort-field=\"Nam\" filter=\"text\"/><column name=\"Sta\" filter=\"values\"/></grid>"
      + "<grid id=\"GrdOth\" server-action=\"data\" target-action=\"OthLst\" initial-load=\"query\"><column name=\"Nam\"/></grid>"
      + "</window></tag></screen>");
    screen.setId("Scr");
    return screen;
  }

  private static ObjectNode empty() {
    return JsonNodeFactory.instance.objectNode();
  }

  private DefaultSearches.Resolution resolve(Screen screen, ObjectNode stored) {
    return DefaultSearches.resolve(screen, stored, empty(), savedSearchService, DATA);
  }

  /**
   * Parameters of the initial load of a component
   */
  private ObjectNode initialLoadOf(String componentId) throws Exception {
    ArgumentCaptor<AweThreadInitialization> loads = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao, atLeastOnce()).launchInitialLoad(loads.capture());
    return loads.getAllValues().stream().filter(load -> componentId.equals(load.getComponentId()) && load.getColumnId() == null)
      .findFirst().orElseThrow().getParameters();
  }

  private JsonNode controllerOf(Map<String, ScreenComponent> componentMap, String id) {
    return mapper.valueToTree(componentMap.get(id)).get("controller");
  }

  @Test
  @DisplayName("Without anything kept for the grid, its quick filter and search fill their criteria, and its sort and column filters its data")
  void appliedWhenNothingIsKept() throws Exception {
    ObjectNode stored = empty();

    DefaultSearches.Resolution resolution = resolve(screen(), stored);

    assertThat(resolution.parameters()).isEqualTo(mapper.readTree("{\"QckSta\":\"1\",\"SchUsr\":\"john\",\"GrdUsr.data\":"
      + "{\"columnFilters\":" + COLUMN_FILTERS + ",\"sort\":" + SORT + "}}"));
    assertThat(resolution.criteria()).containsExactlyInAnyOrder("QckSta", "SchUsr");
    assertThat(resolution.grids()).containsOnlyKeys("GrdUsr");
    assertThat(stored.isEmpty()).as("The stored parameters of the session are not changed").isTrue();
    verify(savedSearchService, never()).getDefaultGridSearch(eq("Scr"), argThat(grid -> "GrdOth".equals(grid.getId())));
  }

  @ParameterizedTest(name = "{0}")
  @DisplayName("When keep-criteria stored the data, the quick filter or the search of the grid, the last state wins")
  @ValueSource(strings = {"GrdUsr.data", "QckSta", "SchUsr"})
  void notAppliedWhenKept(String kept) {
    ObjectNode stored = empty().put(kept, "");

    DefaultSearches.Resolution resolution = resolve(screen(), stored);

    assertThat(resolution.parameters()).isSameAs(stored);
    assertThat(resolution.grids()).isEmpty();
    verify(savedSearchService, never()).getDefaultGridSearch(any(), any());
  }

  @Test
  @DisplayName("The default of a window and the default of a grid apply together")
  void windowAndGridDefaultsApplyTogether() {
    when(savedSearchService.getDefaultWindowSearch("Scr", "WinSch"))
      .thenReturn(Optional.of(new DefaultSavedSearch(7, "Window", "{\"CrtNam\":[{\"value\":\"Ana\"}]}")));

    DefaultSearches.Resolution resolution = resolve(screen(), empty());

    assertThat(resolution.searches()).containsOnlyKeys("WinSch");
    assertThat(resolution.grids()).containsOnlyKeys("GrdUsr");
    assertThat(resolution.parameters().get("CrtNam").asString()).isEqualTo("Ana");
    assertThat(resolution.parameters().has("GrdUsr.data")).isTrue();
  }

  @Test
  @DisplayName("A grid default that cannot be read or is not valid is skipped, and the rest of the screen keeps its defaults")
  void errorsSkipTheGridDefault() {
    when(savedSearchService.getDefaultWindowSearch("Scr", "WinSch"))
      .thenReturn(Optional.of(new DefaultSavedSearch(7, "Window", "{\"CrtNam\":[{\"value\":\"Ana\"}]}")));
    when(savedSearchService.getDefaultGridSearch(eq("Scr"), any())).thenThrow(new IllegalStateException("down"));

    DefaultSearches.Resolution failed = resolve(screen(), empty());

    assertThat(failed.grids()).isEmpty();
    assertThat(failed.searches()).containsOnlyKeys("WinSch");
    assertThat(failed.parameters().toString()).isEqualTo("{\"CrtNam\":\"Ana\"}");

    when(savedSearchService.getDefaultGridSearch(eq("Scr"), any())).thenReturn(Optional.of(new DefaultSavedSearch(9, "Bad", "[]")));
    assertThat(resolve(screen(), empty()).grids()).isEmpty();
  }

  @Test
  @DisplayName("Only the initial load of the grid gets its default, and only that grid tells it, with its sort column marked")
  void defaultReachesOnlyTheInitialLoadOfItsGrid() throws Exception {
    Screen screen = screen();
    DefaultSearches.Resolution resolution = resolve(screen, empty());
    // As the screen service does: the stored parameters become the parameters of the request
    request.setParameterList(resolution.parameters());

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(), resolution);

    ObjectNode grid = initialLoadOf("GrdUsr");
    assertThat(grid.get("columnFilters")).isEqualTo(mapper.readTree(COLUMN_FILTERS));
    assertThat(grid.get("sort")).isEqualTo(mapper.readTree(SORT));
    assertThat(grid.get("QckSta").asString()).isEqualTo("1");
    assertThat(grid.get("SchUsr").asString()).isEqualTo("john");
    assertThat(initialLoadOf("GrdOth").has("columnFilters")).as("Another grid does not get the column filters").isFalse();
    assertThat(request.getParameterList().has("columnFilters")).as("The request keeps no global column filters").isFalse();

    assertThat(controllerOf(componentMap, "GrdUsr").get("defaultSearch")).isEqualTo(mapper.readTree("{\"id\":9,\"name\":\"Mine\","
      + "\"columnFilters\":" + COLUMN_FILTERS + ",\"columnFilterLabels\":{},\"sort\":" + SORT + ",\"quickFilter\":\"1\",\"search\":\"john\"}"));
    assertThat(controllerOf(componentMap, "GrdOth").has("defaultSearch")).isFalse();
    assertThat(controllerOf(componentMap, "GrdUsr").get("columnModel").get(0).get("sortColumn").asString()).isEqualTo("DESC");
    assertThat(componentMap.get("QckSta").getModel().getSelected()).extracting(CellData::getStringValue).containsExactly("1");
  }

  @Test
  @DisplayName("Without a default search the grid sends none")
  void noDefaultNoSearch() {
    Screen screen = screen();

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(),
      DefaultSearches.Resolution.none(empty()));

    assertThat(controllerOf(componentMap, "GrdUsr").has("defaultSearch")).isFalse();
  }

  @Test
  @DisplayName("A grid of a deferred container loads with its default when it is opened, over the data the client sends")
  void deferredGridLoadsWithItsDefault() throws Exception {
    Screen screen = screen();
    DefaultSearches.Resolution resolution = resolve(screen, empty());
    request.getParameterList().set("GrdUsr.data", mapper.readTree("{\"max\":5,\"page\":1}"));

    generator.generateDeferredComponentMap(screen, screen.getElementsById("WinGrd").get(0), new ScreenData(), new Menu(), resolution, Set.of());

    ObjectNode grid = initialLoadOf("GrdUsr");
    assertThat(grid.get("columnFilters")).isEqualTo(mapper.readTree(COLUMN_FILTERS));
    assertThat(grid.get("sort")).isEqualTo(mapper.readTree(SORT));
    assertThat(grid.get("max").asInt()).isEqualTo(5);
    assertThat(initialLoadOf("GrdOth").has("columnFilters")).isFalse();
  }
}
