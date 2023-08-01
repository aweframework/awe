package com.almis.awe.service.screen;


import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;

import java.util.Map;

/**
 * Manage the specific configuration of a screen
 */
public class ScreenRestrictionGenerator extends ServiceConfig {

  /**
   * Store screen target data in components
   * @param screenRestriction Screen restriction data
   * @param menu Component map
   */
  public void applyScreenRestriction(DataList screenRestriction, Menu menu) {
    // For each column, store value in components
    for (Map<String, CellData> rule : screenRestriction.getRows()) {
      String optionName = rule.get("option").getStringValue();
      String restricted = rule.get("restricted").getStringValue();
      menu.getOptionsByName(optionName)
        .forEach(o -> o.setRestricted(Boolean.parseBoolean(restricted)));
    }
  }

  /**
   * Store screen target data in components
   * @param module Current module
   * @param menu Component map
   */
  public void applyModuleRestriction(String module, Menu menu) {
    // Apply module restriction to menu
    if (module != null) {
      menu.getElementsByType(Option.class).stream()
        .filter(option -> option.getModule() != null && !module.equalsIgnoreCase(option.getModule()))
        .forEach(this::deepRestriction);
    }
  }

  /**
   * Apply deep restriction
   * @param currentOption Current option
   */
  private void deepRestriction(Option currentOption) {
    // Set option as restricted
    currentOption.setRestricted(true);

    // Retrieve each option child and restrict it
    currentOption.getElementsByType(Option.class).forEach(option -> option.setRestricted(true));
  }
}
