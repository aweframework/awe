package com.almis.awe.model.builder;

import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.button.Button;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class MenuScreenBuilder {
  Option menuOption;
  Menu menu;

  /**
   * Build a menu option screen based on options
   *
   * @return Menu option screen
   */
  public Screen build() {
    if (menuOption == null) {
      log.error("No menu option defined. Retrieving empty screen");
      return (Screen) new Screen().setId("_none_");
    }

    // Generate screen
    Element screen = new Screen()
      .setTemplate("window")
      .setLabel(menuOption.getLabel());

    // Add center tag
    Tag center = new Tag().setSource("center");
    screen.addElement(center);

    Tag container = (Tag) new Tag()
      .setType("div")
      .setStyle("menu-screen-container")
      .setId(menuOption.getName() + "-center");
    center.addElement(container);

    // Add every option as button
    container.setElementList(menuOption.getOptions().stream()
      .filter(this::isAvailable)
      .map(this::generateOptionElement)
      .toList());

    // Retrieve screen with menuOption screen name
    return (Screen) screen.setId(menuOption.getScreen());
  }

  /**
   * Check if an option can be shown: it is not restricted and, when it is a nested launcher, it has any option available
   *
   * @param option Option to check
   * @return Option is available
   */
  private boolean isAvailable(Option option) {
    if (option.isRestricted()) {
      return false;
    }
    return !option.isMenuScreen() || option.getOptions().isEmpty() || option.getOptions().stream().anyMatch(this::hasAvailableLeaf);
  }

  /**
   * Check if an option is not restricted and it is a leaf or has any available leaf below (through groups and launchers)
   *
   * @param option Option to check
   * @return Option has something to show
   */
  private boolean hasAvailableLeaf(Option option) {
    if (option.isRestricted() || option.isSeparator() || option.isInvisible()) {
      return false;
    }
    return (option.getOptions().isEmpty() || option.getOptions().stream().anyMatch(this::hasAvailableLeaf));
  }

  /**
   * Get option element (button or group)
   *
   * @param option Option to generate
   * @return Generated element based on option
   */
  private Element generateOptionElement(Option option) {
    if (option.getOptions().isEmpty() || option.isMenuScreen()) {
      // Generate button (a nested launcher is a button which opens its own launcher)
      return generateOptionButton(option);
    } else {
      // Generate group
      return generateOptionGroup(option);
    }
  }

  /**
   * Get the card of an option: the button, or the button with its description as secondary line (if any)
   *
   * @param option Option to generate
   * @return Button, or card with the button and the description
   */
  private Element generateOptionButton(Option option) {
    Element button = generateButton(option);
    if (option.getDescription() == null || option.getDescription().isBlank()) {
      return button;
    }

    // Generate card with the button and its description
    Element card = new Tag()
      .setType("div")
      .setStyle("menu-screen-card")
      .setId(option.getName() + "-card");
    card.addElement(button);
    card.addElement(new Tag()
      .setType("p")
      .setStyle("menu-screen-description")
      .setLabel(option.getDescription())
      .setId(option.getName() + "-description"));
    return card;
  }

  private Element generateButton(Option option) {
    // Retrieve filled button
    return new Button()
      .setIcon(option.getIcon())
      .addElement(ButtonAction.builder()
        .type(Optional.ofNullable(option.getServerAction()).map(action -> "server").orElse("screen"))
        .serverAction(Optional.ofNullable(option.getServerAction()).orElse(menu.getDefaultAction()))
        .targetAction(option.getTargetAction())
        .target(option.getName())
        .screenContext(Optional.ofNullable(option.getScreenContext()).orElse(menu.getScreenContext()))
        .build())
      .setLabel(option.getLabel())
      .setStyle("menu-screen-button")
      .setId(option.getName());
  }

  private Element generateOptionGroup(Option option) {
    Element optionGroup = new Tag()
      .setType("div")
      .setId(option.getName() + "-group");

    // Generate group header
    Element header = new Tag()
      .setType("h4")
      .setStyle("menu-screen-header")
      .setLabel(option.getLabel())
      .setId(option.getName());
    optionGroup.addElement(header);

    // Generate options
    option.getOptions().stream()
      .filter(this::isAvailable)
      .forEach(child -> optionGroup.addElement(generateOptionButton(child)));

    // Retrieve filled option group
    return optionGroup;
  }

}
