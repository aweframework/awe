package com.almis.awe.model.entities.screen.component.grid;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grid empty state serialization tests
 */
class GridEmptyStateTest {

  /**
   * The empty state attributes must reach the clients
   */
  @Test
  void serializeGridWithItsEmptyState() {
    // Prepare
    Grid grid = new Grid();
    grid.setId("Grd");
    grid.setEmptyIcon("filter");
    grid.setEmptyTitle("EMPTY_TITLE");
    grid.setEmptyText("EMPTY_TEXT");
    grid.setEmptyAction("ClearFilters");

    // Run
    JsonNode json = new ObjectMapper().valueToTree(grid);

    // Assert
    assertEquals("filter", json.get("emptyIcon").asText());
    assertEquals("EMPTY_TITLE", json.get("emptyTitle").asText());
    assertEquals("EMPTY_TEXT", json.get("emptyText").asText());
    assertEquals("ClearFilters", json.get("emptyAction").asText());
  }

  /**
   * Without configuration the clients receive no empty state values and apply their defaults
   */
  @Test
  void serializeGridWithoutEmptyState() {
    // Run
    JsonNode json = new ObjectMapper().valueToTree(new Grid());

    // Assert
    assertTrue(json.get("emptyTitle") == null || json.get("emptyTitle").isNull());
    assertTrue(json.get("emptyAction") == null || json.get("emptyAction").isNull());
  }
}
