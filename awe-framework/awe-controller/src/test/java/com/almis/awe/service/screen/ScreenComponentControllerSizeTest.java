package com.almis.awe.service.screen;

import com.almis.awe.builder.enumerates.InitialLoad;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.WindowBuilder;
import com.almis.awe.builder.screen.button.ButtonBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.TextColumnBuilder;
import com.almis.awe.builder.screen.tab.TabBuilder;
import com.almis.awe.builder.screen.tab.TabContainerBuilder;
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
import com.almis.awe.service.WindowStateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The controllers of the screen components do not repeat the subtree of each component: the clients build the tree
 * from the screen structure, so each definition is sent once (#907)
 */
@Slf4j
class ScreenComponentControllerSizeTest {

  private static final int PAGES = 7;
  private static final int CRITERIA = 10;
  private static final int COLUMNS = 10;

  private final JsonMapper mapper = new JsonMapper();
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() {
    AweRequest request = mock(AweRequest.class);
    when(request.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    generator = new ScreenComponentGenerator(request, mock(ScreenModelGenerator.class), mock(InitialLoadDao.class),
      mock(AweElementsDao.class), mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS), mock(ScreenConfigurationGenerator.class),
      mock(GridLayoutService.class), mock(WindowStateService.class));
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweElements.class)).thenReturn(mock(AweElements.class, RETURNS_DEEP_STUBS));
    generator.setApplicationContext(context);
  }

  private Map<String, ScreenComponent> generate(Screen screen) {
    ScreenData data = new ScreenData();
    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, data, new Menu(), JsonNodeFactory.instance.objectNode());
    assertThat(data.getActions()).isEmpty();
    return componentMap;
  }

  /**
   * A tab of several pages, each one with a window of criteria, a grid with columns and buttons, and a nested tab
   */
  private static Screen largeScreen() throws Exception {
    TabBuilder tab = new TabBuilder().setId("Tab").setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("TabOpts");
    for (int page = 1; page <= PAGES; page++) {
      WindowBuilder window = new WindowBuilder().setId("Win" + page).setLabel("Window " + page);
      for (int criterion = 1; criterion <= CRITERIA; criterion++) {
        window.addCriteria(new TextCriteriaBuilder().setId("Crt" + page + "_" + criterion).setLabel("Criterion " + criterion));
      }
      GridBuilder grid = new GridBuilder().setId("Grd" + page);
      for (int column = 1; column <= COLUMNS; column++) {
        grid.addColumn(new TextColumnBuilder().setId("Col" + column).setName("Col" + column).setLabel("Column " + column));
      }
      grid.addButton(new ButtonBuilder().setId("BtnAdd" + page).setLabel("Add"), new ButtonBuilder().setId("BtnDel" + page).setLabel("Delete"));
      window.addGrid(grid);
      window.addTab(new TabBuilder().setId("Inner" + page).setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("InnerOpts")
        .addTabContainerList(new TabContainerBuilder().setId("InnerPage" + page)
          .addCriteria(new TextCriteriaBuilder().setId("Deep" + page).setLabel("Deep"))));
      tab.addTabContainerList(new TabContainerBuilder().setId("Page" + page).addWindow(window));
    }
    return new ScreenBuilder().setId("large").addTag(new TagBuilder().setSource("center").setType("div").addTab(tab)).build();
  }

  private static int occurrences(String json, String id) {
    Matcher matcher = Pattern.compile("\"id\":\"" + Pattern.quote(id) + "\"").matcher(json);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  @Test
  void eachComponentDefinitionIsSentOnce() throws Exception {
    Screen screen = largeScreen();
    Map<String, ScreenComponent> componentMap = generate(screen);

    String components = mapper.writeValueAsString(componentMap.values());
    String structure = mapper.writeValueAsString(screen);
    log.info("Screen with {} components: components {} bytes, structure {} bytes", componentMap.size(), components.length(), structure.length());

    StringBuilder controllers = new StringBuilder();
    for (JsonNode component : mapper.readTree(components)) {
      assertThat(component.get("controller").has("elementList")).as(component.get("id").asString()).isFalse();
      controllers.append(component.get("controller"));
    }
    for (int page = 1; page <= PAGES; page++) {
      assertThat(occurrences(controllers.toString(), "Page" + page)).as("Page" + page).isEqualTo(1);
      assertThat(occurrences(controllers.toString(), "Crt" + page + "_1")).as("Crt" + page + "_1").isEqualTo(1);
      assertThat(occurrences(controllers.toString(), "Deep" + page)).as("Deep" + page).isEqualTo(1);
    }
    // With the subtrees the components were almost five times the structure (369,605 bytes against 77,360)
    assertThat(components.length()).isLessThan(2 * structure.length());
  }

  @Test
  void theFieldsTheClientsReadFromTheChildrenAreStillSent() throws Exception {
    Map<String, ScreenComponent> componentMap = generate(largeScreen());

    JsonNode grid = mapper.valueToTree(componentMap.get("Grd1")).get("controller");

    assertThat(grid.get("columnModel")).hasSize(COLUMNS);
    assertThat(grid.get("buttonModel")).hasSize(2);
    // The generated controllers keep their children: only their serialization leaves them out
    assertThat(componentMap.get("Tab").getController().getElementList()).hasSize(PAGES);
  }
}
