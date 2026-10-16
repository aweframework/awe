package com.almis.awe.service.screen;

import com.almis.awe.builder.enumerates.InitialLoad;
import com.almis.awe.builder.screen.DialogBuilder;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.criteria.SelectCriteriaBuilder;
import com.almis.awe.builder.screen.grid.GridBuilder;
import com.almis.awe.builder.screen.grid.SelectColumnBuilder;
import com.almis.awe.builder.screen.tab.TabBuilder;
import com.almis.awe.builder.screen.tab.TabContainerBuilder;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The initial load of the components inside deferred dialogs and tab pages does not run at screen generation (#824)
 */
class ScreenComponentGeneratorTest {

  private ScreenModelGenerator screenModelGenerator;
  private ScreenComponentGenerator generator;

  @BeforeEach
  void setUp() {
    screenModelGenerator = mock(ScreenModelGenerator.class);
    AweRequest request = mock(AweRequest.class);
    when(request.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    generator = new ScreenComponentGenerator(request, screenModelGenerator, mock(InitialLoadDao.class),
      mock(AweElementsDao.class), mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS), mock(ScreenConfigurationGenerator.class));
  }

  private static SelectCriteriaBuilder select(String id) throws Exception {
    return new SelectCriteriaBuilder().setId(id).setInitialLoad(InitialLoad.QUERY).setTargetAction("Query" + id);
  }

  private List<String> loadedComponents(Screen screen) {
    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(), JsonNodeFactory.instance.objectNode());
    ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
    verify(screenModelGenerator, org.mockito.Mockito.atLeast(0)).addComponentTarget(any(), captor.capture());
    // Every component keeps its controller and an empty model, loaded or not
    componentMap.values().forEach(component -> assertThat(component.getModel().getValues()).isEmpty());
    return captor.getAllValues().stream().map(Component::getElementKey).toList();
  }

  @Test
  void withoutDeferredContainersEveryInitialLoadRuns() throws Exception {
    Screen screen = new ScreenBuilder().setId("plain").addTag(new TagBuilder().setSource("center").setType("div")
      .addCriteria(select("Out"))
      .addDialog(new DialogBuilder().setId("Dia").addCriteria(select("InDialog")))
      .addTab(new TabBuilder().setId("Tab").setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("TabOpts")
        .addTabContainerList(new TabContainerBuilder().setId("Page").addCriteria(select("InTab"))))).build();

    assertThat(loadedComponents(screen)).contains("Out", "InDialog", "InTab", "Tab");
  }

  @Test
  void deferredDialogComponentsAreNotLoadedButStillRegistered() throws Exception {
    Screen screen = new ScreenBuilder().setId("deferred-dialog").addTag(new TagBuilder().setSource("center").setType("div")
      .addCriteria(select("Out"))
      .addDialog(new DialogBuilder().setId("Dia").setDeferred(true).addCriteria(select("InDialog")))).build();

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, new ScreenData(), new Menu(), JsonNodeFactory.instance.objectNode());
    ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
    verify(screenModelGenerator, times(1)).addComponentTarget(any(), captor.capture());

    assertThat(captor.getValue().getElementKey()).isEqualTo("Out");
    assertThat(componentMap).containsKeys("Out", "InDialog", "Dia");
  }

  @Test
  void deferredTabPageComponentsAreNotLoadedButOtherPagesAre() throws Exception {
    Screen screen = new ScreenBuilder().setId("deferred-tab").addTag(new TagBuilder().setSource("center").setType("div")
      .addTab(new TabBuilder().setId("Tab").setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("TabOpts")
        .addTabContainerList(new TabContainerBuilder().setId("First").addCriteria(select("InFirst")))
        .addTabContainerList(new TabContainerBuilder().setId("Second").setDeferred(true).addCriteria(select("InSecond"))))).build();

    List<String> loaded = loadedComponents(screen);

    assertThat(loaded).contains("Tab", "InFirst").doesNotContain("InSecond");
  }

  @Test
  void componentsNestedInsideADeferredContainerAreDeferredToo() throws Exception {
    Screen screen = new ScreenBuilder().setId("nested").addTag(new TagBuilder().setSource("center").setType("div")
      .addCriteria(select("Out"))
      .addDialog(new DialogBuilder().setId("Dia").setDeferred(true)
        .addTab(new TabBuilder().setId("InnerTab").setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("TabOpts")
          .addTabContainerList(new TabContainerBuilder().setId("Page").addCriteria(select("Deep")))))).build();

    List<String> loaded = loadedComponents(screen);

    assertThat(loaded).contains("Out").doesNotContain("InnerTab", "Deep");
  }

  @Test
  void gridColumnsOfADeferredGridKeepTheirLoadAtScreenGeneration() throws Exception {
    Screen screen = new ScreenBuilder().setId("columns").addTag(new TagBuilder().setSource("center").setType("div")
      .addDialog(new DialogBuilder().setId("Dia").setDeferred(true)
        .addGrid(new GridBuilder().setId("Grid").setInitialLoad(InitialLoad.QUERY).setTargetAction("GridQuery")
          .addColumn(new SelectColumnBuilder().setId("Col").setName("Col").setInitialLoad(InitialLoad.QUERY).setTargetAction("ColQuery"))))).build();

    generator.generateComponentMap(screen, new ScreenData(), new Menu(), JsonNodeFactory.instance.objectNode());

    // The grid data is deferred, the option lists of its columns are part of the grid controller and still load
    verify(screenModelGenerator, never()).addComponentTarget(any(), any());
    verify(screenModelGenerator, times(1)).addColumnTarget(any(), org.mockito.ArgumentMatchers.eq("Grid"), any());
  }

  @Test
  void openingADeferredContainerLoadsItsOwnComponentsOnly() throws Exception {
    Screen screen = new ScreenBuilder().setId("open").addTag(new TagBuilder().setSource("center").setType("div")
      .addCriteria(select("Out"))
      .addDialog(new DialogBuilder().setId("Dia").setDeferred(true)
        .addCriteria(select("InDialog"))
        .addCriteria(select("AlreadyLoaded"))
        .addGrid(new GridBuilder().setId("Grid").setInitialLoad(InitialLoad.QUERY).setTargetAction("GridQuery")
          .addColumn(new SelectColumnBuilder().setId("Col").setName("Col").setInitialLoad(InitialLoad.QUERY).setTargetAction("ColQuery")))
        .addTab(new TabBuilder().setId("InnerTab").setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("TabOpts")
          .addTabContainerList(new TabContainerBuilder().setId("Page").setDeferred(true).addCriteria(select("Deep")))))).build();
    Element dialog = DeferredComponents.findDeferredContainer(screen, "Dia");

    Map<String, ScreenComponent> componentMap = generator.generateDeferredComponentMap(screen, dialog, new ScreenData(), new Menu(),
      JsonNodeFactory.instance.objectNode(), Set.of("AlreadyLoaded"));

    ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
    verify(screenModelGenerator, org.mockito.Mockito.atLeastOnce()).addComponentTarget(any(), captor.capture());
    assertThat(captor.getAllValues().stream().map(Component::getElementKey)).containsExactlyInAnyOrder("InDialog", "Grid", "InnerTab");
    assertThat(componentMap).containsKeys("InDialog", "Grid", "InnerTab", "Page").doesNotContainKeys("Out", "AlreadyLoaded", "Deep");
    // The column lists were loaded with the screen
    verify(screenModelGenerator, never()).addColumnTarget(any(), any(), any());
  }
}
