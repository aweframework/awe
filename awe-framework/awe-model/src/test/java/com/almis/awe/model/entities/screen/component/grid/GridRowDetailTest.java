package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grid row detail model: the row-detail element of the XML reaches the clients as the rowDetail attribute
 */
@DisplayName("Grid row detail model")
class GridRowDetailTest {

  private static Grid parse(String xml) {
    XStream xstream = new XStream();
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Grid.class);
    return (Grid) xstream.fromXML(xml);
  }

  @Test
  @DisplayName("The row-detail body is read from the XML and sent as the template")
  void rowDetailTemplateIsParsedAndSerialized() {
    Grid grid = parse("<grid id=\"Grd\"><column name=\"name\"/><column name=\"notes\" hidden=\"true\"/>" +
      "<row-detail><![CDATA[Name: {name}\nNotes: <{notes}>]]></row-detail></grid>");

    JsonNode json = new ObjectMapper().valueToTree(grid);

    assertEquals("Name: {name}\nNotes: <{notes}>", json.get("rowDetail").get("template").asString());
    assertEquals(2, grid.getElementsByType(Column.class).size());
  }

  @Test
  @DisplayName("The style of the row-detail is sent with its template, and only when it is set")
  void rowDetailStyleIsSerialized() {
    JsonNode styled = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><row-detail style=\"detail-card bg-soft\">{name}</row-detail></grid>"));
    JsonNode plain = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><row-detail>{name}</row-detail></grid>"));

    assertEquals("detail-card bg-soft", styled.get("rowDetail").get("style").asString());
    assertEquals("{name}", styled.get("rowDetail").get("template").asString());
    assertNull(plain.get("rowDetail").get("style"));
  }

  @Test
  @DisplayName("A grid without row-detail sends no rowDetail attribute")
  void gridWithoutRowDetail() {
    JsonNode json = new ObjectMapper().valueToTree(parse("<grid id=\"Grd\"><column name=\"name\"/></grid>"));

    assertTrue(json.get("rowDetail") == null || json.get("rowDetail").isNull());
  }

  @Test
  @DisplayName("Copying a grid keeps its row detail")
  void copyKeepsRowDetail() {
    Grid grid = parse("<grid id=\"Grd\"><row-detail>{name}</row-detail></grid>");

    Grid copy = grid.copy();

    assertEquals("{name}", copy.getRowDetail().getContent());
    assertNull(parse("<grid id=\"Grd\"/>").getRowDetail());
  }
}
