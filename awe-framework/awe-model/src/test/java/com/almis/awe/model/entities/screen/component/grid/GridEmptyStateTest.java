package com.almis.awe.model.entities.screen.component.grid;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
    JsonNode json = new JsonMapper().valueToTree(grid);

    // Assert
    assertEquals("filter", json.get("emptyIcon").asString());
    assertEquals("EMPTY_TITLE", json.get("emptyTitle").asString());
    assertEquals("EMPTY_TEXT", json.get("emptyText").asString());
    assertEquals("ClearFilters", json.get("emptyAction").asString());
  }

  /**
   * Without configuration the clients receive no empty state values and apply their defaults
   */
  @Test
  void serializeGridWithoutEmptyState() {
    // Run
    JsonNode json = new JsonMapper().valueToTree(new Grid());

    // Assert
    assertTrue(json.get("emptyTitle") == null || json.get("emptyTitle").isNull());
    assertTrue(json.get("emptyAction") == null || json.get("emptyAction").isNull());
  }
}
