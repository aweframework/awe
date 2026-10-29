package com.almis.awe.builder.screen.grid;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.entities.screen.component.grid.Grid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Row grouping attributes in the grid builder")
class GroupingGridBuilderTest {

  @Test
  @DisplayName("A grid built with grouping keeps the attributes and loads every row")
  void gridKeepsGrouping() throws AWException {
    Grid grid = new GridBuilder().setId("Grd")
      .setGroupBy("country,city")
      .setGroupSelector(true)
      .setGroupSubtotals(false)
      .build();

    assertEquals("country,city", grid.getGroupBy());
    assertTrue(grid.getGroupSelector());
    assertFalse(grid.getGroupSubtotals());
    assertTrue(grid.isLoadAll());
  }

  @Test
  @DisplayName("A grid built without grouping sends no group keys")
  void gridWithoutGrouping() throws AWException {
    Grid grid = new GridBuilder().setId("Grd").build();
    JsonNode json = new ObjectMapper().valueToTree(grid);

    List.of("groupBy", "groupSelector", "groupSubtotals")
      .forEach(property -> assertFalse(json.has(property), property));
    assertFalse(grid.isLoadAll());
  }
}
