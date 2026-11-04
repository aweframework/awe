package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Saved searches of a grid (issue #912, S20): the saved-searches attribute is read from the XML and reaches the clients
 * only when the grid opts in.
 */
class GridSavedSearchesTest {

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Grid.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  private Grid parse(String attributes) {
    return (Grid) xStreamMarshaller.fromXML("<grid id=\"GrdUsr\" " + attributes + "/>");
  }

  private JsonNode toJson(Grid grid) {
    return JsonMapper.builder().build().valueToTree(grid);
  }

  @Test
  @DisplayName("saved-searches=\"true\" is parsed and reaches the clients as savedSearches")
  void gridWithSavedSearches() {
    Grid grid = parse("saved-searches=\"true\"");

    assertThat(grid.hasSavedSearches()).isTrue();
    assertThat(toJson(grid).get("savedSearches").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("A grid without the attribute, or with it false, sends no savedSearches")
  void gridWithoutSavedSearches() {
    assertThat(parse("").hasSavedSearches()).isFalse();
    assertThat(toJson(parse("")).has("savedSearches")).isFalse();
    assertThat(toJson(parse("saved-searches=\"false\"")).has("savedSearches")).isFalse();
  }
}
