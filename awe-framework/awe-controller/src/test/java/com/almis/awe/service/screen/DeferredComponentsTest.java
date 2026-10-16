package com.almis.awe.service.screen;

import com.almis.awe.builder.enumerates.InitialLoad;
import com.almis.awe.builder.screen.DialogBuilder;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.builder.screen.tab.TabBuilder;
import com.almis.awe.builder.screen.tab.TabContainerBuilder;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredComponentsTest {

  private Screen buildScreen() throws Exception {
    return new ScreenBuilder().setId("deferred").addTag(new TagBuilder().setSource("center").setType("div")
      .addCriteria(new TextCriteriaBuilder().setId("Out"))
      .addDialog(new DialogBuilder().setId("Outer").setDeferred(true)
        .addCriteria(new TextCriteriaBuilder().setId("InOuter"))
        .addTab(new TabBuilder().setId("Tab").setInitialLoad(InitialLoad.ENUMERATED).setTargetAction("TabOpts")
          .addTabContainerList(new TabContainerBuilder().setId("Page").setDeferred(true)
            .addCriteria(new TextCriteriaBuilder().setId("InPage")))))
      .addDialog(new DialogBuilder().setId("Plain").addCriteria(new TextCriteriaBuilder().setId("InPlain")))).build();
  }

  private static List<String> ids(Iterable<? extends Component> components) {
    return java.util.stream.StreamSupport.stream(components.spliterator(), false).map(Component::getElementKey).toList();
  }

  @Test
  void everyComponentBelowADeferredContainerIsDeferred() throws Exception {
    assertThat(ids(DeferredComponents.getDeferredComponents(buildScreen())))
      .containsExactlyInAnyOrder("InOuter", "Tab", "Page", "InPage");
  }

  @Test
  void findsOnlyDeferredContainers() throws Exception {
    Screen screen = buildScreen();

    assertThat(DeferredComponents.findDeferredContainer(screen, "Outer")).isNotNull();
    assertThat(DeferredComponents.findDeferredContainer(screen, "Page")).isNotNull();
    assertThat(DeferredComponents.findDeferredContainer(screen, "Plain")).isNull();
    assertThat(DeferredComponents.findDeferredContainer(screen, "Missing")).isNull();
    assertThat(DeferredComponents.findDeferredContainer(screen, null)).isNull();
  }

  @Test
  void aContainerLoadsItsOwnComponentsButNotTheNestedDeferredOnes() throws Exception {
    Element outer = DeferredComponents.findDeferredContainer(buildScreen(), "Outer");

    assertThat(ids(DeferredComponents.getComponentsToLoad(outer))).containsExactlyInAnyOrder("InOuter", "Tab", "Page");
  }

  @Test
  void aNestedContainerLoadsItsComponentsWhenItIsOpened() throws Exception {
    Element page = DeferredComponents.findDeferredContainer(buildScreen(), "Page");

    assertThat(ids(DeferredComponents.getComponentsToLoad(page))).containsExactly("InPage");
  }
}
