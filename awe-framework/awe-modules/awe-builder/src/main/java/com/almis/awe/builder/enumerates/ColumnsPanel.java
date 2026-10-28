package com.almis.awe.builder.enumerates;

/**
 * How the toolbar of a grid shows its Columns: a popover anchored to the Columns button, a modal
 * dialog or a side panel
 */
public enum ColumnsPanel {
  POPOVER("popover"),
  DIALOG("dialog"),
  SIDE_PANEL("side-panel");

  private final String value;

  ColumnsPanel(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
