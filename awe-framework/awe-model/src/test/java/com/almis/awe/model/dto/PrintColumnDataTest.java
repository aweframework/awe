package com.almis.awe.model.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visible column info sent by the clients to print or export a grid
 */
class PrintColumnDataTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  @DisplayName("The secondary field to export is read from the visible column info (issue #832)")
  void readsTheSecondaryFieldToExport() {
    PrintColumnData column = mapper.readValue(
      "{\"name\":\"name\",\"label\":\"Name\",\"secondaryField\":\"email\",\"exportSecondary\":true,\"secondaryLabel\":\"E-mail\"}",
      PrintColumnData.class);

    assertEquals("email", column.getSecondaryField());
    assertTrue(column.getExportSecondary());
    assertEquals("E-mail", column.getSecondaryLabel());
  }

  @Test
  @DisplayName("Visible column info without a secondary field exports nothing more")
  void readsAColumnWithoutSecondaryField() {
    PrintColumnData column = mapper.readValue("{\"name\":\"name\",\"label\":\"Name\"}", PrintColumnData.class);

    assertNull(column.getSecondaryField());
    assertNull(column.getExportSecondary());
    assertNull(column.getSecondaryLabel());
  }

  @Test
  @DisplayName("Null secondary values sent by the clients are accepted")
  void readsNullSecondaryValues() {
    PrintColumnData column = mapper.readValue(
      "{\"name\":\"name\",\"secondaryField\":null,\"exportSecondary\":null,\"secondaryLabel\":null}", PrintColumnData.class);

    assertNull(column.getSecondaryField());
    assertNull(column.getExportSecondary());
    assertNull(column.getSecondaryLabel());
  }
}
