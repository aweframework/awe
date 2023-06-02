package com.almis.awe.model.builder;

import com.almis.awe.model.entities.Element;
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

import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class MenuScreenBuilder {
  Option menuOption;

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
      .filter(option -> !option.isRestricted())
      .map(this::generateOptionElement)
      .collect(Collectors.toList()));

    // Retrieve screen with menuOption screen name
    return (Screen) screen.setId(menuOption.getScreen());
  }

  /**
   * Get option element (button or group)
   *
   * @param option Option to generate
   * @return Generated element based on option
   */
  private Element generateOptionElement(Option option) {
    if (option.getOptions().isEmpty()) {
      // Generate button
      return generateOptionButton(option);
    } else {
      // Generate group
      return generateOptionGroup(option);
    }
  }

  private Element generateOptionButton(Option option) {
    // Retrieve filled button
    return new Button()
      .setIcon(option.getIcon())
      .addElement(new ButtonAction()
        .setTarget(option.getName())
        .setType("screen"))
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
      .filter(child -> !child.isRestricted())
      .forEach(child -> optionGroup.addElement(generateOptionButton(child)));

    // Retrieve filled option group
    return optionGroup;
  }

}
