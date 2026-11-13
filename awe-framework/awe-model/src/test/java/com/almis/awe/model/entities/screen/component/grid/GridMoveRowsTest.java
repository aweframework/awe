package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grid move-rows model: opt-in attribute read from the XML and sent to the client
 */
@DisplayName("Grid move-rows model")
class GridMoveRowsTest {

  @Test
  @DisplayName("Rows cannot be dragged unless the grid opts in")
  void moveRowsIsOffByDefault() {
    Grid grid = new Grid();
    assertFalse(grid.isMoveRows());
    assertFalse(new ObjectMapper().valueToTree(grid).get("moveRows").asBoolean());

    grid.setMoveRows(true);
    assertTrue(new ObjectMapper().valueToTree(grid).get("moveRows").asBoolean());
  }

  @Test
  @DisplayName("The move-rows attribute is read from the XML")
  void moveRowsAttributeIsParsed() {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);

    Grid grid = (Grid) xstream.fromXML("<grid id=\"Grd\" move-rows=\"true\"/>");

    assertEquals(true, grid.isMoveRows());
  }
}
