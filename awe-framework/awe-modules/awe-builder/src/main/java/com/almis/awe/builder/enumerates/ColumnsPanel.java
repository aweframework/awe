package com.almis.awe.builder.enumerates;

/**
 * How the toolbar of a grid shows its Columns: a modal dialog or a side panel
 */
public enum ColumnsPanel {
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
