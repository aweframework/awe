package com.almis.awe.model.entities.screen.component.grid;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * End buttons of the grid toolbar that can show their label, in toolbar order. Their names are the values of the
 * grid attribute toolbar-labels
 */
@Getter
public enum GridToolbarButton {
  GROUP_BY("group-by"),
  SAVED_SEARCHES("saved-searches"),
  COLUMNS("columns"),
  MAXIMIZE("maximize");

  private final String buttonName;

  GridToolbarButton(String buttonName) {
    this.buttonName = buttonName;
  }

  /**
   * Find a button by its name, ignoring case and surrounding spaces
   *
   * @param name Button name
   * @return Button, empty when the name is unknown
   */
  public static Optional<GridToolbarButton> fromName(String name) {
    String trimmed = name == null ? "" : name.trim();
    return Arrays.stream(values()).filter(button -> button.buttonName.equalsIgnoreCase(trimmed)).findFirst();
  }
}
