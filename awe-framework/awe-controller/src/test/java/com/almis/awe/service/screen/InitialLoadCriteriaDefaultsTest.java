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
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import com.almis.awe.service.WindowStateService;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Initial load of a grid with the default values of the screen criteria (#947): the server initial load gets the same
 * criteria values the client sends on a reload, keyed by the criterion id
 */
@DisplayName("Criteria defaults in the initial load")
class InitialLoadCriteriaDefaultsTest {
  private final JsonMapper mapper = new JsonMapper();
  private InitialLoadDao initialLoadDao;
  private AweRequest request;
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() throws Exception {
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
   * Screen with the grid "GrdCst" before the window of its criteria, so the criteria models are built after the grid
   */
  private static Screen screen() {
    XStream xstream = new XStream(new DomDriver());
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Screen.class);
    Screen screen = (Screen) xstream.fromXML("<screen template=\"window\"><tag source=\"center\">"
      + "<window id=\"WinGrd\"><grid id=\"GrdCst\" server-action=\"data\" target-action=\"CstLst\" initial-load=\"query\">"
      + "<column name=\"Nam\"/></grid></window>"
      + "<window id=\"WinCrt\">"
      + "<criteria id=\"CrtCtr\" component=\"text\" variable=\"country\" value=\"ES\"/>"
      + "<criteria id=\"CrtSta\" component=\"select-multiple\" value=\"1, 2\"/>"
      + "<criteria id=\"CrtPrd\" component=\"date-range\" value=\"01/01/2026, 31/01/2026\"/>"
      + "<criteria id=\"CrtNam\" component=\"text\"/>"
      + "<criteria id=\"CrtSeg\" component=\"picklist\" value=\"R\"/>"
      + "<criteria id=\"CrtEnd\" component=\"date-range\" value=\"01/02/2026\"/>"
      + "</window></tag></screen>");
    screen.setId("Scr");
    return screen;
  }

  private ObjectNode initialLoadOfGrid() throws Exception {
    ArgumentCaptor<AweThreadInitialization> loads = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao, atLeastOnce()).launchInitialLoad(loads.capture());
    return loads.getAllValues().stream().filter(load -> "GrdCst".equals(load.getComponentId()) && load.getColumnId() == null)
      .findFirst().orElseThrow().getParameters();
  }

  @Test
  @DisplayName("The XML defaults of the criteria reach the initial load as the client sends them")
  void xmlDefaultsReachTheInitialLoad() throws Exception {
    generator.generateComponentMap(screen(), new ScreenData(), new Menu(), DefaultSearches.Resolution.none(request.getParametersSafe()));

    ObjectNode grid = initialLoadOfGrid();
    assertThat(grid.get("CrtCtr").asString()).isEqualTo("ES");
    assertThat(grid.get("CrtSta")).isEqualTo(mapper.readTree("[\"1\",\"2\"]"));
    assertThat(grid.get("CrtPrd.from").asString()).isEqualTo("01/01/2026");
    assertThat(grid.get("CrtPrd.to").asString()).isEqualTo("31/01/2026");
    assertThat(grid.get("CrtSeg")).as("A picklist goes as a list").isEqualTo(mapper.readTree("[\"R\"]"));
    assertThat(grid.get("CrtEnd.from").asString()).isEqualTo("01/02/2026");
    assertThat(grid.get("CrtEnd.to").isNull()).as("An open end goes as null").isTrue();
    assertThat(grid.has("CrtNam")).as("A criterion without a default stays absent").isFalse();
    assertThat(request.getParameterList().has("CrtCtr")).as("The request parameters are not changed").isFalse();
  }

  @Test
  @DisplayName("A value of the request or keep-criteria wins over the XML default")
  void requestValueWinsOverTheDefault() throws Exception {
    request.getParameterList().put("CrtCtr", "FR");
    request.getParameterList().putArray("CrtSta").add("3");

    generator.generateComponentMap(screen(), new ScreenData(), new Menu(), DefaultSearches.Resolution.none(request.getParametersSafe()));

    ObjectNode grid = initialLoadOfGrid();
    assertThat(grid.get("CrtCtr").asString()).isEqualTo("FR");
    assertThat(grid.get("CrtSta")).isEqualTo(mapper.readTree("[\"3\"]"));
  }

  @Test
  @DisplayName("A date range of the request wins over its default")
  void requestDateRangeWinsOverTheDefault() throws Exception {
    request.getParameterList().put("CrtPrd.from", "05/05/2026");
    request.getParameterList().putNull("CrtPrd.to");

    generator.generateComponentMap(screen(), new ScreenData(), new Menu(), DefaultSearches.Resolution.none(request.getParametersSafe()));

    ObjectNode grid = initialLoadOfGrid();
    assertThat(grid.get("CrtPrd.from").asString()).isEqualTo("05/05/2026");
    assertThat(grid.get("CrtPrd.to").isNull()).isTrue();
  }

  @Test
  @DisplayName("A stored criterion (keep-criteria) reaches the initial load instead of the XML default")
  void storedCriterionWinsOverTheDefault() throws Exception {
    ObjectNode stored = request.getParametersSafe().put("CrtCtr", "IT");

    generator.generateComponentMap(screen(), new ScreenData(), new Menu(), DefaultSearches.Resolution.none(stored));

    assertThat(initialLoadOfGrid().get("CrtCtr").asString()).isEqualTo("IT");
  }
}
