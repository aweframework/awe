package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Include;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The components inside a grid row detail are not screen components: they are sent as a template of the grid (#884)
 */
@DisplayName("Components of a grid row detail")
class ScreenComponentGeneratorRowDetailTest {

  private static final String ORDERS_GRID = "<grid id=\"GrdOrders\" server-action=\"data\" target-action=\"QryOrders\" initial-load=\"query\" max=\"5\">" +
    "<column name=\"order\" label=\"ORDER\"/><column name=\"status\" label=\"STATUS\" component=\"select\" initial-load=\"enum\" target-action=\"Sta\"/>" +
    "<button id=\"BtnOpen\" label=\"OPEN\"/></grid>";

  private ScreenModelGenerator screenModelGenerator;
  private GridLayoutService gridLayoutService;
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() {
    screenModelGenerator = mock(ScreenModelGenerator.class);
    gridLayoutService = mock(GridLayoutService.class);
    AweRequest request = mock(AweRequest.class);
    when(request.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    generator = new ScreenComponentGenerator(request, screenModelGenerator, mock(InitialLoadDao.class),
      mock(AweElementsDao.class), mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS), mock(ScreenConfigurationGenerator.class),
      gridLayoutService);
    // Locales are returned as "<key>|<parameters>"
    AweElements elements = mock(AweElements.class, invocation -> "getLocaleWithLanguage".equals(invocation.getMethod().getName()) ?
      invocation.getArgument(0) + "|" + String.join(",", Arrays.stream(invocation.getArguments()).skip(2).map(String::valueOf).toList()) : null);
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    generator.setApplicationContext(context);
  }

  private static Screen screen(String grids) {
    XStream xstream = new XStream(new DomDriver());
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Screen.class);
    Screen screen = (Screen) xstream.fromXML("<screen template=\"window\"><tag source=\"center\">" + grids + "</tag></screen>");
    screen.setId("products");
    return screen;
  }

  private static Screen productsWith(String rowDetail) {
    return screen("<grid id=\"GrdProducts\" server-action=\"data\" target-action=\"QryProducts\" initial-load=\"query\">" +
      "<column name=\"id\" label=\"ID\"/>" + rowDetail + "</grid>");
  }

  private static List<String> errors(ScreenData data) {
    return data.getActions().stream().map(ClientAction::getParameters)
      .map(parameters -> ((CellData) parameters.get("title")).getStringValue() + " / " + ((CellData) parameters.get("message")).getStringValue())
      .toList();
  }

  @Test
  @DisplayName("A grid of the row-detail is not registered, does not load with the screen and has no screen layout")
  void rowDetailGridIsNotAScreenComponent() {
    Screen screen = productsWith("<row-detail style=\"detail-card\">" + ORDERS_GRID + "</row-detail>");
    ScreenData data = new ScreenData();

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, data, new Menu(), JsonNodeFactory.instance.objectNode());

    assertThat(errors(data)).isEmpty();
    assertThat(componentMap).containsOnlyKeys("GrdProducts");
    ArgumentCaptor<Component> loaded = ArgumentCaptor.forClass(Component.class);
    verify(screenModelGenerator, atLeast(0)).addComponentTarget(any(), loaded.capture());
    assertThat(loaded.getAllValues()).extracting(Component::getId).containsExactly("GrdProducts");
    verify(screenModelGenerator, never()).addColumnTarget(any(), any(), any());
    verify(gridLayoutService, times(1)).resolveLayout(any(), any());
  }

  @Test
  @DisplayName("The grid controller sends the row-detail component as a template: the nested grid controller and its empty model")
  void rowDetailComponentTemplateIsSent() {
    Screen screen = productsWith("<row-detail style=\"detail-card\">" + ORDERS_GRID + "</row-detail>");

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(), JsonNodeFactory.instance.objectNode());

    JsonNode products = new ObjectMapper().valueToTree(componentMap.get("GrdProducts").getController());
    JsonNode rowDetail = products.get("rowDetail");
    JsonNode component = rowDetail.get("component");
    assertThat(rowDetail.get("style").asString()).isEqualTo("detail-card");
    assertThat(rowDetail.has("template")).isFalse();
    assertThat(component.get("id").asString()).isEqualTo("GrdOrders");
    assertThat(component.get("controller").get("id").asString()).isEqualTo("GrdOrders");
    assertThat(component.get("controller").get("serverAction").asString()).isEqualTo("data");
    assertThat(component.get("controller").get("targetAction").asString()).isEqualTo("QryOrders");
    assertThat(component.get("controller").get("max").asInt()).isEqualTo(5);
    assertThat(component.get("controller").get("columnModel")).extracting(column -> column.get("name").asString())
      .containsExactly("order", "status");
    assertThat(component.get("controller").get("buttonModel")).extracting(button -> button.get("id").asString())
      .containsExactly("BtnOpen");
    // The layout of a grid is stored by screen and grid: an instance in a row detail cannot store it
    assertThat(component.get("controller").get("showToolbar").asBoolean()).isFalse();
    assertThat(component.get("controller").has("screenId")).isFalse();
    assertThat(component.get("model").get("values").isEmpty()).isTrue();
    // The parent grid keeps its own columns only
    assertThat(products.get("columnModel")).extracting(column -> column.get("name").asString()).containsExactly("id");
  }

  @Test
  @DisplayName("A template row-detail and a grid without row-detail are unchanged")
  void templateRowDetailIsUnchanged() {
    Screen screen = screen("<grid id=\"GrdProducts\"><column name=\"id\"/><row-detail style=\"card\">Id: {id}</row-detail></grid>" +
      "<grid id=\"GrdPlain\"><column name=\"id\"/></grid>");

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(), JsonNodeFactory.instance.objectNode());

    JsonNode rowDetail = new ObjectMapper().valueToTree(componentMap.get("GrdProducts").getController()).get("rowDetail");
    assertThat(rowDetail.get("template").asString()).isEqualTo("Id: {id}");
    assertThat(rowDetail.get("style").asString()).isEqualTo("card");
    assertThat(rowDetail.has("component")).isFalse();
    assertThat(new ObjectMapper().valueToTree(componentMap.get("GrdPlain").getController()).has("rowDetail")).isFalse();
  }

  @Test
  @DisplayName("A grid brought by an include of the row-detail is its component")
  void includedRowDetailGridIsSent() {
    Screen screen = productsWith("<row-detail><include target-screen=\"orders\" target-source=\"detail\"/></row-detail>");
    Grid products = (Grid) screen.getElementsById("GrdProducts").get(0);
    // Includes are resolved when the screen is read
    Include include = (Include) products.getRowDetail().getElementList().get(0);
    include.setElementList(List.<Element>of(((Grid) productsWith("<row-detail>" + ORDERS_GRID + "</row-detail>")
      .getElementsById("GrdProducts").get(0)).getRowDetail().getDetailGrid()));
    ScreenData data = new ScreenData();

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, data, new Menu(), JsonNodeFactory.instance.objectNode());

    assertThat(errors(data)).isEmpty();
    assertThat(componentMap).containsOnlyKeys("GrdProducts");
    JsonNode component = new ObjectMapper().valueToTree(componentMap.get("GrdProducts").getController()).get("rowDetail").get("component");
    assertThat(component.get("id").asString()).isEqualTo("GrdOrders");
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "Id: {id}<grid id=\"GrdOrders\"/>|ERROR_MESSAGE_ROW_DETAIL_TEMPLATE_AND_COMPONENTS|GrdProducts,products",
    "<grid id=\"GrdOrders\"/><grid id=\"GrdOther\"/>|ERROR_MESSAGE_ROW_DETAIL_NOT_A_GRID|GrdProducts,products",
    "<criteria id=\"CrtName\"/>|ERROR_MESSAGE_ROW_DETAIL_NOT_A_GRID|GrdProducts,products",
    "<grid id=\"GrdOrders\" editable=\"true\"/>|ERROR_MESSAGE_ROW_DETAIL_EDITABLE_GRID|GrdProducts,products",
    "<grid id=\"GrdOrders\" send-operations=\"true\"/>|ERROR_MESSAGE_ROW_DETAIL_EDITABLE_GRID|GrdProducts,products",
    "<grid id=\"GrdOrders\"><column name=\"order\"><dependency><dependency-element id=\"GrdProducts\"/></dependency></column></grid>|ERROR_MESSAGE_ROW_DETAIL_DEPENDENCY|GrdProducts,products"})
  @DisplayName("A wrong row-detail stops the screen with a clear error")
  void wrongRowDetailIsAScreenError(String content, String message, String parameters) {
    ScreenData data = new ScreenData();

    generator.generateComponentMap(productsWith("<row-detail>" + content + "</row-detail>"), data, new Menu(), JsonNodeFactory.instance.objectNode());

    assertThat(errors(data)).containsExactly("ERROR_TITLE_BAD_ROW_DETAIL_DEFINITION| / " + message + "|" + parameters);
  }
}
