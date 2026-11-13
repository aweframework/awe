package com.almis.awe.model.builder;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.button.Button;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Menu screen (launcher) builder tests: descriptions on cards and nested launchers
 */
class MenuScreenBuilderTest {

  private static Option option(String name, String screen) {
    Option option = new Option().setScreen(screen).setIcon("icon-" + name);
    option.setName(name);
    option.setLabel("LABEL_" + name);
    return option;
  }

  private static Option launcher(String name, Option... children) {
    Option launcher = option(name, name + "-screen").setMenuScreen(true);
    for (Option child : children) {
      launcher.addElement(child);
    }
    return launcher;
  }

  private static Option described(Option option, String description) {
    return option.setDescription(description);
  }

  private static Screen build(Option launcher) {
    Menu menu = new Menu();
    menu.addElement(launcher);
    launcher.defineRelationship();
    return new MenuScreenBuilder(launcher, menu).build();
  }

  private static List<Element> cards(Screen screen) {
    Tag center = (Tag) screen.getElementList().get(0);
    Tag container = (Tag) center.getElementList().get(0);
    return container.getElementList();
  }

  @Test
  void optionWithoutDescriptionIsAPlainButton() {
    Screen screen = build(launcher("root", option("leaf", "leaf-screen")));

    assertThat(cards(screen)).hasSize(1);
    Element card = cards(screen).get(0);
    assertThat(card).isInstanceOf(Button.class);
    assertThat(card.getId()).isEqualTo("leaf");
    assertThat(card.getStyle()).isEqualTo("menu-screen-button");
  }

  @Test
  void optionWithDescriptionIsACardWithASecondaryLine() {
    Screen screen = build(launcher("root", described(option("leaf", "leaf-screen"), "DESC_leaf")));

    Element card = cards(screen).get(0);
    assertThat(card).isInstanceOf(Tag.class);
    assertThat(card.getId()).isEqualTo("leaf-card");
    assertThat(card.getStyle()).isEqualTo("menu-screen-card");
    List<Element> parts = card.getElementList();
    assertThat(parts).hasSize(2);
    assertThat(parts.get(0)).isInstanceOf(Button.class);
    assertThat(parts.get(0).getId()).isEqualTo("leaf");
    assertThat(parts.get(1)).isInstanceOf(Tag.class);
    assertThat(parts.get(1).getId()).isEqualTo("leaf-description");
    assertThat(parts.get(1).getStyle()).isEqualTo("menu-screen-description");
    assertThat(parts.get(1).getLabel()).isEqualTo("DESC_leaf");
  }

  @Test
  void blankDescriptionIsTreatedAsAbsent() {
    Screen screen = build(launcher("root", described(option("empty", "empty-screen"), ""),
      described(option("blank", "blank-screen"), "  ")));

    assertThat(cards(screen)).extracting(Element::getId).containsExactly("empty", "blank");
    assertThat(cards(screen)).allMatch(Button.class::isInstance);
  }

  @Test
  void groupChildrenShowTheirDescription() {
    Option group = option("group", null);
    group.addElement(described(option("leaf", "leaf-screen"), "DESC_leaf"));
    Screen screen = build(launcher("root", group));

    Tag groupTag = (Tag) cards(screen).get(0);
    assertThat(groupTag.getId()).isEqualTo("group-group");
    // Header + card
    assertThat(groupTag.getElementList()).hasSize(2);
    assertThat(groupTag.getElementList().get(1).getId()).isEqualTo("leaf-card");
  }

  @Test
  void nestedLauncherIsACardThatOpensTheLauncher() {
    Option nested = described(launcher("nested", option("leaf", "leaf-screen")), "DESC_nested");
    Screen screen = build(launcher("root", nested));

    assertThat(cards(screen)).hasSize(1);
    Element card = cards(screen).get(0);
    assertThat(card.getId()).isEqualTo("nested-card");
    Button button = (Button) card.getElementList().get(0);
    assertThat(button.getId()).isEqualTo("nested");
    assertThat(button.getElementList()).hasSize(1);
  }

  @Test
  void nestedLauncherWithoutDescriptionIsAPlainButton() {
    Screen screen = build(launcher("root", launcher("nested", option("leaf", "leaf-screen"))));

    assertThat(cards(screen).get(0)).isInstanceOf(Button.class);
    assertThat(cards(screen).get(0).getId()).isEqualTo("nested");
  }

  @Test
  void nestedLauncherInsideAGroupIsACard() {
    Option group = option("group", null);
    group.addElement(launcher("nested", option("leaf", "leaf-screen")));
    Screen screen = build(launcher("root", group));

    Tag groupTag = (Tag) cards(screen).get(0);
    assertThat(groupTag.getElementList().get(1)).isInstanceOf(Button.class);
    assertThat(groupTag.getElementList().get(1).getId()).isEqualTo("nested");
  }

  @Test
  void restrictedNestedLauncherIsNotShown() {
    Option nested = launcher("nested", option("leaf", "leaf-screen"));
    Option root = launcher("root", nested, option("other", "other-screen"));
    nested.setRestricted(true);
    Screen screen = build(root);

    assertThat(cards(screen)).extracting(Element::getId).containsExactly("other");
  }

  @Test
  void nestedLauncherWithoutAvailableOptionsIsNotShown() {
    Option nested = launcher("nested", option("leaf", "leaf-screen"));
    Option root = launcher("root", nested, option("other", "other-screen"));
    nested.getOptions().get(0).setRestricted(true);
    Screen screen = build(root);

    assertThat(cards(screen)).extracting(Element::getId).containsExactly("other");
  }

  @Test
  void nestedLauncherWithOnlyARestrictedGroupIsNotShown() {
    Option group = option("group", null);
    Option leaf = option("leaf", "leaf-screen");
    group.addElement(leaf);
    Option nested = launcher("nested", group);
    Option root = launcher("root", nested, option("other", "other-screen"));
    leaf.setRestricted(true);
    Screen screen = build(root);

    assertThat(cards(screen)).extracting(Element::getId).containsExactly("other");
  }

  @Test
  void nestedLauncherWithOnlySeparatorsAndInvisibleOptionsIsNotShown() {
    Option separator = option("separator", null).setSeparator(true);
    Option invisible = option("invisible", "invisible-screen").setInvisible(true);
    Option root = launcher("root", launcher("nested", separator, invisible), option("other", "other-screen"));
    Screen screen = build(root);

    assertThat(cards(screen)).extracting(Element::getId).containsExactly("other");
  }

  @Test
  void restrictedDescribedOptionIsNotShown() {
    Option leaf = described(option("leaf", "leaf-screen"), "DESC_leaf");
    Option root = launcher("root", leaf);
    leaf.setRestricted(true);

    assertThat(cards(build(root))).isEmpty();
  }
}
