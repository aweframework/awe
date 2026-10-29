package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Row grouping attributes of a grid (issue #833): they are read from the XML, reach the clients and make the grid
 * load every row.
 */
@DisplayName("Grid row grouping model")
class GridGroupingTest {

  private static final List<String> GROUP_PROPERTIES = List.of("groupBy", "groupSelector", "groupSubtotals");

  private static XStream xstream;

  @BeforeAll
  static void beforeAll() {
    xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
  }

  private static Grid parse(String attributes) {
    return (Grid) xstream.fromXML("<grid id=\"Grd\" " + attributes + "/>");
  }

  private static JsonNode json(Grid grid) {
    return new ObjectMapper().valueToTree(grid);
  }

  @Test
  @DisplayName("The grouping attributes are parsed from the grid element")
  void groupingAttributesAreParsed() {
    Grid grid = parse("group-by=\"country,city\" group-selector=\"true\" group-subtotals=\"false\"");

    assertThat(grid.getGroupBy()).isEqualTo("country,city");
    assertThat(grid.getGroupSelector()).isTrue();
    assertThat(grid.getGroupSubtotals()).isFalse();
  }

  @Test
  @DisplayName("The grouping attributes reach the clients with their camelCase names and load every row")
  void groupingAttributesAreSerialized() {
    JsonNode json = json(parse("group-by=\"country,city\" group-selector=\"true\" group-subtotals=\"false\""));

    assertThat(json.get("groupBy").asString()).isEqualTo("country,city");
    assertThat(json.get("groupSelector").asBoolean()).isTrue();
    assertThat(json.get("groupSubtotals").asBoolean()).isFalse();
    assertThat(json.get("loadAll").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("A grid with group-by loads every row whatever its load-all")
  void groupByLoadsAll() {
    Grid grid = parse("group-by=\"country\" load-all=\"false\"");

    assertThat(grid.isLoadAll()).isTrue();
    assertThat(json(grid).get("loadAll").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("A grid with only the group selector loads every row")
  void groupSelectorLoadsAll() {
    Grid grid = parse("group-selector=\"true\"");

    assertThat(grid.isLoadAll()).isTrue();
    assertThat(json(grid).get("loadAll").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("A blank group-by or a disabled group selector keep the previous load-all")
  void blankGroupingKeepsLoadAll() {
    assertThat(parse("group-by=\" \"").isLoadAll()).isFalse();
    assertThat(parse("group-selector=\"false\"").isLoadAll()).isFalse();
  }

  @Test
  @DisplayName("A grid without grouping sends nothing new and keeps its load-all")
  void gridWithoutGrouping() {
    Grid grid = parse("");
    JsonNode json = json(grid);

    GROUP_PROPERTIES.forEach(property -> assertThat(json.has(property)).as(property).isFalse());
    assertThat(grid.getGroupBy()).isNull();
    assertThat(grid.getGroupSelector()).isNull();
    assertThat(grid.getGroupSubtotals()).isNull();
    assertThat(json.get("loadAll").asBoolean()).isFalse();
    assertThat(json(parse("load-all=\"true\"")).get("loadAll").asBoolean()).isTrue();
  }

  @Test
  @DisplayName("Grouping is ignored on tree grids, which keep their own load-all")
  void treeGridKeepsLoadAll() {
    Grid grid = parse("treegrid=\"true\" group-by=\"country\" group-selector=\"true\"");

    assertThat(grid.isLoadAll()).isFalse();
    assertThat(json(grid).get("loadAll").asBoolean()).isFalse();
    assertThat(parse("treegrid=\"true\" group-by=\"country\" load-all=\"true\"").isLoadAll()).isTrue();
  }

  @Test
  @DisplayName("The grouping attributes survive a copy")
  void groupingAttributesAreCopied() {
    Grid copy = parse("group-by=\"country\" group-selector=\"true\" group-subtotals=\"false\"").copy();

    assertThat(copy.getGroupBy()).isEqualTo("country");
    assertThat(copy.getGroupSelector()).isTrue();
    assertThat(copy.getGroupSubtotals()).isFalse();
    assertThat(copy.isLoadAll()).isTrue();
  }
}
