package com.almis.awe.service.screen;

import com.almis.awe.builder.screen.AdvancedBuilder;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.WindowBuilder;
import com.almis.awe.builder.screen.criteria.SelectCriteriaBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.DefaultSearch;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import com.almis.awe.service.WindowStateService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The screen generated with the default search of a window (#915, S2): the window tells the clients the search applied,
 * and the criteria it filled keep the values of their definition as the values Clear all goes back to
 */
@DisplayName("Screen components with a default search")
class ScreenComponentDefaultSearchTest {

  private final JsonMapper mapper = new JsonMapper();
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() {
    AweRequest request = new AweRequest(mock(HttpServletRequest.class), null, mapper);
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweRequest.class)).thenReturn(request);
    when(context.getBean(AweElements.class)).thenReturn(mock(AweElements.class, RETURNS_DEEP_STUBS));
    BaseConfigProperties properties = mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS);
    ScreenModelGenerator screenModelGenerator = new ScreenModelGenerator(mock(InitialLoadDao.class), properties);
    screenModelGenerator.setApplicationContext(context);
    generator = new ScreenComponentGenerator(request, screenModelGenerator, mock(InitialLoadDao.class), mock(AweElementsDao.class),
      properties, mock(ScreenConfigurationGenerator.class), mock(GridLayoutService.class), mock(WindowStateService.class));
    generator.setApplicationContext(context);
  }

  /**
   * The window "WinCrt" (user, then the advanced theme, "1" in its definition) and the window "WinDat" (date)
   */
  private static Screen screen() throws Exception {
    return new ScreenBuilder().setId("search").addTag(new TagBuilder().setSource("center").setType("div")
        .addWindow(new WindowBuilder().setId("WinCrt").setSavedSearches(true)
          .addCriteria(new TextCriteriaBuilder().setId("CrtUsr"))
          .addAdvanced(new AdvancedBuilder().setId("AdvCrt").addCriteria(new SelectCriteriaBuilder().setId("CrtThm").setValue("1"))))
        .addWindow(new WindowBuilder().setId("WinDat").addCriteria(new TextCriteriaBuilder().setId("CrtDat"))))
      .build();
  }

  private static DefaultSearch search() {
    ObjectNode values = JsonNodeFactory.instance.objectNode();
    values.putArray("CrtThm").addObject().put("value", "2").put("label", "Sky");
    return new DefaultSearch(7, "Mine", values, List.of("CrtThm"));
  }

  private Map<String, ScreenComponent> generate(DefaultSearches.Resolution resolution) throws Exception {
    return generator.generateComponentMap(screen(), new ScreenData(), new Menu(), resolution);
  }

  private JsonNode controllerOf(Map<String, ScreenComponent> componentMap, String id) {
    return mapper.valueToTree(componentMap.get(id)).get("controller");
  }

  @Test
  @DisplayName("Only the window a default search was applied to carries it, with its id, name, values and layout")
  void defaultSearchIsSentOnlyWhenApplied() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("CrtThm", "2");

    Map<String, ScreenComponent> componentMap = generate(new DefaultSearches.Resolution(parameters, Map.of("WinCrt", search()), Set.of("CrtThm")));

    assertThat(controllerOf(componentMap, "WinCrt").get("defaultSearch").toString())
      .isEqualTo("{\"id\":7,\"name\":\"Mine\",\"values\":{\"CrtThm\":[{\"value\":\"2\",\"label\":\"Sky\"}]},\"layout\":[\"CrtThm\"]}");
    assertThat(controllerOf(componentMap, "WinDat").has("defaultSearch")).isFalse();
    assertThat(controllerOf(generate(DefaultSearches.Resolution.none(JsonNodeFactory.instance.objectNode())), "WinCrt").has("defaultSearch"))
      .isFalse();
  }

  @Test
  @DisplayName("A criterion filled by the default search selects its values and keeps the values of its definition as default")
  void criteriaFilledByTheDefaultSearchKeepTheirDefinitionDefaults() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode().put("CrtThm", "2").put("CrtUsr", "ana");

    // CrtUsr comes from keep-criteria: its stored value is still its default value, as today
    Map<String, ScreenComponent> componentMap = generate(new DefaultSearches.Resolution(parameters, Map.of("WinCrt", search()), Set.of("CrtThm")));

    assertThat(componentMap.get("CrtThm").getModel().getSelected()).containsExactly(new CellData("2"));
    assertThat(componentMap.get("CrtThm").getModel().getDefaultValues()).containsExactly(new CellData("1"));
    assertThat(componentMap.get("CrtUsr").getModel().getSelected()).containsExactly(new CellData("ana"));
    assertThat(componentMap.get("CrtUsr").getModel().getDefaultValues()).containsExactly(new CellData("ana"));
  }
}
