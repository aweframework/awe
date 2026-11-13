package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sparkline column attributes")
class ColumnSparklineTest {

  private static final String SPARKLINE_COLUMN = "<column name=\"load\" component=\"sparkline\" sparkline-type=\"bar\""
    + " sparkline-color=\"#3b82f6\" sparkline-negative-color=\"#d64550\" sparkline-last-point=\"true\""
    + " sparkline-min-max=\"true\"/>";

  private static XStream xStream;

  @BeforeAll
  static void beforeAll() {
    xStream = new XStream();
    xStream.autodetectAnnotations(true);
    xStream.processAnnotations(Column.class);
    xStream.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStream.aliasSystemAttribute(null, "class");
  }

  @Test
  @DisplayName("The sparkline attributes are parsed from the column element")
  void sparklineAttributesAreParsed() {
    Column column = (Column) xStream.fromXML(SPARKLINE_COLUMN);

    assertEquals("sparkline", column.getComponentType());
    assertEquals("bar", column.getSparklineType());
    assertEquals("#3b82f6", column.getSparklineColor());
    assertEquals("#d64550", column.getSparklineNegativeColor());
    assertTrue(column.getSparklineLastPoint());
    assertTrue(column.getSparklineMinMax());
  }

  @Test
  @DisplayName("The sparkline attributes reach the clients as JSON properties")
  void sparklineAttributesAreSerialized() {
    JsonNode json = new ObjectMapper().valueToTree(xStream.fromXML(SPARKLINE_COLUMN));

    assertEquals("bar", json.get("sparklineType").asText());
    assertEquals("#3b82f6", json.get("sparklineColor").asText());
    assertEquals("#d64550", json.get("sparklineNegativeColor").asText());
    assertTrue(json.get("sparklineLastPoint").asBoolean());
    assertTrue(json.get("sparklineMinMax").asBoolean());
  }

  @Test
  @DisplayName("Without them the clients receive no sparkline attributes and apply their defaults")
  void sparklineAttributesAreOptional() {
    Column column = (Column) xStream.fromXML("<column name=\"load\" component=\"sparkline\"/>");
    JsonNode json = new ObjectMapper().valueToTree(column);

    assertNull(column.getSparklineType());
    assertTrue(json.get("sparklineType") == null || json.get("sparklineType").isNull());
    assertTrue(json.get("sparklineMinMax") == null || json.get("sparklineMinMax").isNull());
  }
}
