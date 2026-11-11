package com.almis.awe.model.entities.screen.component.grid;

import com.almis.awe.model.dto.GridColumnLayout;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grid toolbar model: opt-out attribute, layout columns and group membership
 */
@DisplayName("Grid toolbar model")
class GridToolbarTest {

  private static Column column(String name, boolean hidden) {
    Column column = new Column();
    column.setName(name);
    column.setHidden(hidden);
    return column;
  }

  private static Grid gridWithGroups() {
    GroupHeader group = new GroupHeader();
    group.setElementList(List.of(column("b", false), column("c", true), column("d", false)));
    Grid grid = new Grid();
    grid.setId("Grd");
    grid.setElementList(List.of(column("a", false), group, column("key", true), column("e", false)));
    return grid;
  }

  @Test
  @DisplayName("The toolbar is shown unless the grid opts out")
  void toolbarIsOnByDefault() {
    Grid grid = new Grid();
    assertTrue(new ObjectMapper().valueToTree(grid).get("showToolbar").asBoolean());

    grid.setShowToolbar(false);
    assertEquals(false, new ObjectMapper().valueToTree(grid).get("showToolbar").asBoolean());
  }

  @Test
  @DisplayName("The show-toolbar attribute is read from the XML")
  void toolbarAttributeIsParsed() {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);

    Grid grid = (Grid) xstream.fromXML("<grid id=\"Grd\" show-toolbar=\"false\"/>");

    assertEquals(false, grid.isShowToolbar());
  }

  @Test
  @DisplayName("The Columns and Maximize buttons are shown unless the grid hides them")
  void toolbarButtonsAreOnByDefault() {
    Grid grid = new Grid();
    JsonNode json = new ObjectMapper().valueToTree(grid);
    assertTrue(json.get("showColumnsButton").asBoolean());
    assertTrue(json.get("showMaximizeButton").asBoolean());

    grid.setShowColumnsButton(false);
    grid.setShowMaximizeButton(false);
    json = new ObjectMapper().valueToTree(grid);
    assertFalse(json.get("showColumnsButton").asBoolean());
    assertFalse(json.get("showMaximizeButton").asBoolean());
  }

  private static List<String> toolbarLabels(String value) {
    Grid grid = new Grid();
    grid.setToolbarLabels(value);
    List<String> names = new ArrayList<>();
    new ObjectMapper().valueToTree(grid).get("toolbarLabels").forEach(name -> names.add(name.asText()));
    return names;
  }

  @Test
  @DisplayName("A grid without toolbar-labels leaves the labels to the application default")
  void toolbarLabelsIsOnlySentWhenTheGridSetsIt() {
    assertTrue(new ObjectMapper().valueToTree(new Grid()).path("toolbarLabels").isMissingNode());
  }

  @Test
  @DisplayName("toolbar-labels=\"true\" shows the label of every end button")
  void toolbarLabelsTrueListsEveryButton() {
    assertEquals(List.of("group-by", "saved-searches", "columns", "maximize"), toolbarLabels("true"));
  }

  @Test
  @DisplayName("toolbar-labels=\"false\" shows only the icon of every end button")
  void toolbarLabelsFalseListsNoButton() {
    assertEquals(List.of(), toolbarLabels("false"));
  }

  @Test
  @DisplayName("A toolbar-labels list is sent in the order of the toolbar")
  void toolbarLabelsListKeepsTheToolbarOrder() {
    assertEquals(List.of("saved-searches", "columns"), toolbarLabels("columns, saved-searches"));
  }

  @Test
  @DisplayName("An unknown name of the toolbar-labels list is ignored")
  void toolbarLabelsIgnoresUnknownNames() {
    assertEquals(List.of("maximize"), toolbarLabels("maximize,unknown"));
  }

  @Test
  @DisplayName("The toolbar button attributes are read from the XML and survive a copy")
  void toolbarButtonAttributesAreParsed() {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);

    Grid grid = (Grid) xstream.fromXML(
      "<grid id=\"Grd\" show-columns-button=\"false\" show-maximize-button=\"false\" toolbar-labels=\"saved-searches\"/>");

    assertFalse(grid.isShowColumnsButton());
    assertFalse(grid.isShowMaximizeButton());
    assertEquals("saved-searches", grid.getToolbarLabels());
    Grid copy = grid.copy();
    assertFalse(copy.isShowColumnsButton());
    assertFalse(copy.isShowMaximizeButton());
    assertEquals("saved-searches", copy.getToolbarLabels());
  }

  @Test
  @DisplayName("The column layout is enabled only when the toolbar shows the Columns button")
  void columnLayoutNeedsTheColumnsButton() {
    Grid grid = new Grid();
    assertTrue(grid.isColumnLayoutEnabled());

    grid.setShowColumnsButton(false);
    assertFalse(grid.isColumnLayoutEnabled());

    Grid withoutToolbar = new Grid();
    withoutToolbar.setShowToolbar(false);
    assertFalse(withoutToolbar.isColumnLayoutEnabled());
    assertFalse(new ObjectMapper().valueToTree(grid).has("columnLayoutEnabled"));
  }

  @Test
  @DisplayName("The layout columns skip XML hidden columns and carry their group")
  void layoutColumns() {
    Map<String, String> expected = new LinkedHashMap<>();
    expected.put("a", null);
    expected.put("b", "0");
    expected.put("d", "0");
    expected.put("e", null);

    assertEquals(expected, gridWithGroups().getLayoutColumns());
  }

  @Test
  @DisplayName("A group header lists the names of its columns")
  void groupHeaderColumnNames() {
    GroupHeader group = (GroupHeader) gridWithGroups().getElementList().get(1);

    JsonNode json = new ObjectMapper().valueToTree(group);

    assertEquals("b", json.get("columnNames").get(0).asText());
    assertEquals(3, json.get("columnNames").size());
  }

  @Test
  @DisplayName("A grid without columns-panel leaves the Columns to the application default")
  void columnsPanelIsOnlySentWhenTheGridSetsIt() {
    Grid grid = new Grid();
    assertTrue(new ObjectMapper().valueToTree(grid).path("columnsPanel").isMissingNode());

    grid.setColumnsPanel("side-panel");
    assertEquals("side-panel", new ObjectMapper().valueToTree(grid).get("columnsPanel").asText());
  }

  @Test
  @DisplayName("The columns-panel attribute is read from the XML and survives a copy")
  void columnsPanelAttributeIsParsed() {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);

    Grid grid = (Grid) xstream.fromXML("<grid id=\"Grd\" columns-panel=\"side-panel\"/>");

    assertEquals("side-panel", grid.getColumnsPanel());
    assertEquals("side-panel", grid.copy().getColumnsPanel());
  }

  @Test
  @DisplayName("The grid serializes its column layout when it has one")
  void columnLayoutIsSerialized() {
    Grid grid = new Grid();
    grid.setColumnLayout(new GridColumnLayout(List.of("b", "a"), List.of("a")));

    JsonNode json = new ObjectMapper().valueToTree(grid);

    assertEquals("b", json.get("columnLayout").get("order").get(0).asText());
    assertEquals("a", json.get("columnLayout").get("hidden").get(0).asText());
    JsonNode without = new ObjectMapper().valueToTree(new Grid()).get("columnLayout");
    assertTrue(without == null || without.isNull());
  }
}
