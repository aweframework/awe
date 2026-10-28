package com.almis.awe.builder.enumerates;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Values of the columns-panel attribute of a grid
 */
class ColumnsPanelTest {

  @Test
  void writesTheValuesOfTheColumnsPanelAttribute() {
    assertEquals("popover", ColumnsPanel.POPOVER.toString());
    assertEquals("dialog", ColumnsPanel.DIALOG.toString());
    assertEquals("side-panel", ColumnsPanel.SIDE_PANEL.toString());
  }
}
