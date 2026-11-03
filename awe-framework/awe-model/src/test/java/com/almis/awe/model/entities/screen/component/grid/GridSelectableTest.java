package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Non-selectable grids (#303): selectable="false" keeps the grid from selecting rows
 */
class GridSelectableTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private static Grid parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(new Class[]{Grid.class});
    return (Grid) xstream.fromXML(xml);
  }

  @Test
  void aGridIsSelectableByDefault() {
    Grid grid = parse("<grid id=\"GrdUsr\"/>");

    assertThat(grid.isSelectable()).isTrue();
    assertThat(mapper.valueToTree(grid).get("selectable").asBoolean()).isTrue();
  }

  @Test
  void selectableFalseReachesTheClients() {
    Grid grid = parse("<grid id=\"GrdUsr\" selectable=\"false\"/>");

    assertThat(grid.isSelectable()).isFalse();
    JsonNode json = mapper.valueToTree(grid);
    assertThat(json.get("selectable").asBoolean()).isFalse();
  }

  @Test
  void selectableTrueIsKept() {
    assertThat(parse("<grid id=\"GrdUsr\" selectable=\"true\"/>").isSelectable()).isTrue();
  }

  @Test
  void aCopyKeepsTheAttribute() {
    Grid grid = parse("<grid id=\"GrdUsr\" selectable=\"false\"/>");

    assertThat(grid.copy().isSelectable()).isFalse();
  }
}
