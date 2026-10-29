package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The quick filter of a grid is a criterion of the screen: it is generated, loads with the screen and the grid names it
 */
@DisplayName("Quick filter of a grid")
class ScreenComponentGeneratorQuickFilterTest {

  private ScreenModelGenerator screenModelGenerator;
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() {
    screenModelGenerator = mock(ScreenModelGenerator.class);
    AweRequest request = mock(AweRequest.class);
    when(request.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    generator = new ScreenComponentGenerator(request, screenModelGenerator, mock(InitialLoadDao.class),
      mock(AweElementsDao.class), mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS), mock(ScreenConfigurationGenerator.class),
      mock(GridLayoutService.class));
  }

  private static Screen screen(String content) {
    XStream xstream = new XStream(new DomDriver());
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Screen.class);
    Screen screen = (Screen) xstream.fromXML("<screen template=\"window\"><tag source=\"center\">" + content + "</tag></screen>");
    screen.setId("users");
    return screen;
  }

  @Test
  @DisplayName("The quick filter is generated as a component that loads its options with the screen, and the grid names it")
  void quickFilterIsGenerated() {
    Screen screen = screen("<grid id=\"GrdUsr\" server-action=\"data\" target-action=\"UsrLst\" initial-load=\"query\">" +
      "<quick-filter id=\"CrtUsrSta\" initial-load=\"query\" target-action=\"UsrStaCnt\" value=\"\"/>" +
      "<column name=\"usr\" label=\"USER\"/></grid>");

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(), JsonNodeFactory.instance.objectNode());

    assertThat(componentMap).containsOnlyKeys("GrdUsr", "CrtUsrSta");
    JsonNode grid = new ObjectMapper().valueToTree(componentMap.get("GrdUsr").getController());
    JsonNode quickFilter = new ObjectMapper().valueToTree(componentMap.get("CrtUsrSta").getController());
    assertThat(grid.get("quickFilter").asString()).isEqualTo("CrtUsrSta");
    assertThat(grid.get("columnModel")).extracting(column -> column.get("name").asString()).containsExactly("usr");
    assertThat(quickFilter.get("component").asString()).isEqualTo("quick-filter");
    assertThat(quickFilter.get("serverAction").asString()).isEqualTo("data");
    assertThat(quickFilter.get("targetAction").asString()).isEqualTo("UsrStaCnt");
    assertThat(quickFilter.get("initialLoad").asString()).isEqualTo("query");

    ArgumentCaptor<Component> loaded = ArgumentCaptor.forClass(Component.class);
    verify(screenModelGenerator, atLeast(1)).addComponentTarget(any(), loaded.capture());
    assertThat(loaded.getAllValues()).extracting(Component::getId).containsExactly("GrdUsr", "CrtUsrSta");
  }

  @Test
  @DisplayName("The screen template of the grid has no markup for the quick filter: the grid shows it in its toolbar")
  void quickFilterHasNoMarkupInTheGrid() {
    Screen screen = screen("<grid id=\"GrdUsr\"><quick-filter id=\"CrtUsrSta\" initial-load=\"enum\" target-action=\"Es1Es0\"/>" +
      "<column name=\"usr\"/></grid>");
    Component quickFilter = screen.getElementsByType(Component.class).stream()
      .filter(component -> "CrtUsrSta".equals(component.getId())).findFirst().orElseThrow();

    assertThat(quickFilter.getTemplate()).isEqualTo("empty");
  }
}
