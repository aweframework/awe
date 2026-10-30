package com.almis.awe.service.screen;

import com.almis.awe.builder.enumerates.AdvancedMode;
import com.almis.awe.builder.enumerates.ButtonType;
import com.almis.awe.builder.screen.AdvancedBuilder;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.WindowBuilder;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.criteria.SelectCriteriaBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The clients receive the advanced search of a window: its lists in the window controller and the advanced block as a
 * node of the screen structure (#790)
 */
class ScreenComponentAdvancedSearchTest {

  private final JsonMapper mapper = new JsonMapper();
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() {
    AweRequest request = mock(AweRequest.class);
    when(request.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    generator = new ScreenComponentGenerator(request, mock(ScreenModelGenerator.class), mock(InitialLoadDao.class),
      mock(AweElementsDao.class), mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS), mock(ScreenConfigurationGenerator.class),
      mock(GridLayoutService.class));
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweElements.class)).thenReturn(mock(AweElements.class, RETURNS_DEEP_STUBS));
    generator.setApplicationContext(context);
  }

  private static Screen screen() throws Exception {
    return new ScreenBuilder().setId("search").addTag(new TagBuilder().setSource("center").setType("div")
      .addWindow(new WindowBuilder().setId("WinCrt").setAdvancedMode(AdvancedMode.SIDE_PANEL)
        .addTag(new TagBuilder().setType("div").setStyle("panel-body")
          .addCriteria(new TextCriteriaBuilder().setId("CrtUsr"))
          .addAdvanced(new AdvancedBuilder().setId("AdvCrt").addCriteria(new SelectCriteriaBuilder().setId("CrtThm"))))
        .addButton(new ButtonBuilder().setButtonType(ButtonType.SUBMIT).setId("ButSch")))
      .addWindow(new WindowBuilder().setId("WinDat").addCriteria(new TextCriteriaBuilder().setId("CrtDat"))))
      .build();
  }

  @Test
  void theWindowControllerCarriesTheAdvancedSearchWithoutItsChildren() throws Exception {
    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen(), new ScreenData(), new Menu(),
      JsonNodeFactory.instance.objectNode());

    JsonNode window = mapper.valueToTree(componentMap.get("WinCrt")).get("controller");
    assertThat(window.has("elementList")).isFalse();
    assertThat(window.get("advancedMode").asString()).isEqualTo("side-panel");
    assertThat(window.get("advancedCriteria").toString()).isEqualTo("[\"CrtThm\"]");
    assertThat(window.get("searchCriteria").toString()).isEqualTo("[\"CrtUsr\",\"CrtThm\"]");

    JsonNode plainWindow = mapper.valueToTree(componentMap.get("WinDat")).get("controller");
    assertThat(plainWindow.has("advancedMode")).isFalse();
    assertThat(plainWindow.has("searchCriteria")).isFalse();

    JsonNode advanced = mapper.valueToTree(componentMap.get("AdvCrt")).get("controller");
    assertThat(advanced.get("elementType").asString()).isEqualTo("Advanced");
    assertThat(advanced.has("elementList")).isFalse();
  }

  @Test
  void theAdvancedBlockIsANodeOfTheScreenStructure() throws Exception {
    JsonNode body = mapper.valueToTree(screen()).at("/elementList/0/elementList/0/elementList/0");

    JsonNode advanced = body.get("elementList").get(1);
    assertThat(advanced.get("elementType").asString()).isEqualTo("Advanced");
    assertThat(advanced.get("id").asString()).isEqualTo("AdvCrt");
    assertThat(advanced.get("elementList").get(0).get("id").asString()).isEqualTo("CrtThm");
  }
}
