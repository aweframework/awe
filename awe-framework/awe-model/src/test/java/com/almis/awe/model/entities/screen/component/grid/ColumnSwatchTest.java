package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Colour swatch attribute of a grid column (issue #793): it is read from the XML and reaches the clients.
 */
class ColumnSwatchTest {

  private static XStream xStreamMarshaller;

  @BeforeAll
  static void beforeAll() {
    xStreamMarshaller = new XStream();
    xStreamMarshaller.autodetectAnnotations(true);
    xStreamMarshaller.processAnnotations(Column.class);
    xStreamMarshaller.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStreamMarshaller.aliasSystemAttribute(null, "class");
  }

  private Column parse(String attributes) {
    return (Column) xStreamMarshaller.fromXML("<column name=\"theme\" label=\"THEME\" " + attributes + "/>");
  }

  @Test
  @DisplayName("The swatch attribute is parsed from the column element")
  void swatchAttributeIsParsed() {
    Column column = parse("swatch-field=\"PriSwt\" secondary-field=\"ThmSec\"");

    assertEquals("PriSwt", column.getSwatchField());
    assertEquals("ThmSec", column.getSecondaryField());
  }

  @Test
  @DisplayName("A column without the swatch attribute keeps it unset, so existing grids are unchanged")
  void columnWithoutSwatch() {
    assertNull(parse("").getSwatchField());
  }

  @Test
  @DisplayName("The swatch attribute reaches the clients with its camelCase name")
  void serializeColumnWithSwatch() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column().setSwatchField("PriSwt"));

    assertEquals("PriSwt", json.get("swatchField").asString());
  }

  @Test
  @DisplayName("Without the swatch attribute the clients receive nothing new")
  void serializeColumnWithoutSwatch() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column());

    assertFalse(json.has("swatchField"));
  }
}
