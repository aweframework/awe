package com.almis.awe.service.screen;


import com.almis.awe.model.dto.ScreenRestriction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;

import java.util.List;

/**
 * Manage the specific configuration of a screen
 */
public class ScreenRestrictionGenerator {

  /**
   * Store screen target data in components
   *
   * @param screenRestrictions Screen restriction list
   * @param menu               Component map
   */
  public void applyScreenRestriction(List<ScreenRestriction> screenRestrictions, Menu menu) {
    // Apply global restrictions
    screenRestrictions.stream()
      .filter(screenRestriction -> screenRestriction.getUser() == null && screenRestriction.getProfile() == null)
      .forEach(screenRestriction -> applyScreenRestriction(screenRestriction, menu));

    // Apply profile restrictions
    screenRestrictions.stream()
      .filter(screenRestriction -> screenRestriction.getUser() == null && screenRestriction.getProfile() != null)
      .forEach(screenRestriction -> applyScreenRestriction(screenRestriction, menu));

    // Apply user restrictions
    screenRestrictions.stream()
      .filter(screenRestriction -> screenRestriction.getUser() != null && screenRestriction.getProfile() == null)
      .forEach(screenRestriction -> applyScreenRestriction(screenRestriction, menu));
  }

  /**
   * Apply screen restriction to options
   *
   * @param screenRestriction Screen restriction
   * @param menu              Menu to apply restrictions
   */
  private void applyScreenRestriction(ScreenRestriction screenRestriction, Menu menu) {
    menu.getOptionsByName(screenRestriction.getOption())
      .forEach(o -> deepRestriction(o, screenRestriction.isRestricted()));
  }

  /**
   * Store screen target data in components
   *
   * @param module Current module
   * @param menu   Component map
   */
  public void applyModuleRestriction(String module, Menu menu) {
    // Apply module restriction to menu
    if (module != null) {
      menu.getElementsByType(Option.class).stream()
        .filter(option -> option.getModule() != null && !module.equalsIgnoreCase(option.getModule()))
        .forEach(o -> deepRestriction(o, true));
    }
  }

  /**
   * Apply deep restriction
   *
   * @param currentOption Current option
   */
  private void deepRestriction(Option currentOption, boolean restricted) {
    // Set option as restricted
    currentOption.setRestricted(restricted);

    // Retrieve each option child and restrict it
    currentOption.getElementsByType(Option.class).forEach(option -> option.setRestricted(restricted));
  }
}
