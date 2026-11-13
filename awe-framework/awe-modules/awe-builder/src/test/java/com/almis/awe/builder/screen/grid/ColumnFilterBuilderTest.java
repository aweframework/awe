package com.almis.awe.builder.screen.grid;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.type.ColumnFilterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Builders can declare the autofilter of a grid column (#931), as the XML attributes filter and filter-fields (#912)
 */
@DisplayName("Column autofilter in the column builders")
class ColumnFilterBuilderTest {

  private JsonNode toJson(Column column) {
    return JsonMapper.builder().build().valueToTree(column);
  }

  @Test
  @DisplayName("A column built with a filter and its fields serializes them as the XML column does")
  void columnWithFilter() throws AWException {
    Column column = new TextColumnBuilder().setName("Pro")
      .setFilter(ColumnFilterType.TEXT)
      .setFilterFields("IdePro,Nam")
      .build();

    JsonNode json = toJson(column);
    assertEquals("text", json.get("filter").asString());
    assertEquals("IdePro,Nam", json.get("filterFields").asString());
  }

  @Test
  @DisplayName("A column built without a filter serializes as before")
  void columnWithoutFilter() throws AWException {
    Column column = new TextColumnBuilder().setName("Pro").build();

    JsonNode json = toJson(column);
    assertFalse(json.has("filter"));
    assertFalse(json.has("filterFields"));
  }
}
