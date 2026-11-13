package com.almis.awe.builder.screen.grid;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Secondary line, status and chips column attributes in the column builders")
class CellContentColumnBuilderTest {

  @Test
  @DisplayName("A column built with a secondary line and a status keeps them")
  void columnKeepsSecondaryAndStatus() throws AWException {
    Column column = new TextColumnBuilder().setId("name")
      .setSecondaryField("email")
      .setExportSecondary(true)
      .setSecondaryLabel("EMAIL")
      .setStatusField("tone")
      .build();

    assertEquals("email", column.getSecondaryField());
    assertTrue(column.getExportSecondary());
    assertEquals("EMAIL", column.getSecondaryLabel());
    assertEquals("tone", column.getStatusField());
  }

  @Test
  @DisplayName("A column built with chips keeps the separator and the maximum")
  void columnKeepsChips() throws AWException {
    Column column = new TextColumnBuilder().setId("tags").setChips(true).setChipSeparator(";").setMaxChips(3).build();

    assertTrue(column.getChips());
    assertEquals(";", column.getChipSeparator());
    assertEquals(3, column.getMaxChips());
  }

  @Test
  @DisplayName("A column built without them keeps them unset")
  void columnWithoutCellContent() throws AWException {
    Column column = new TextColumnBuilder().setId("name").build();

    assertNull(column.getSecondaryField());
    assertNull(column.getExportSecondary());
    assertNull(column.getSecondaryLabel());
    assertNull(column.getStatusField());
    assertNull(column.getChips());
    assertNull(column.getChipSeparator());
    assertNull(column.getMaxChips());
  }
}
