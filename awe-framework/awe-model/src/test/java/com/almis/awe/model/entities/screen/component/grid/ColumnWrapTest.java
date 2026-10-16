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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wrapping attributes of a grid column (issue #825): they are read from the XML and reach the clients.
 */
class ColumnWrapTest {

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
    return (Column) xStreamMarshaller.fromXML("<column name=\"description\" label=\"DESC\" " + attributes + "/>");
  }

  @Test
  @DisplayName("wrap=\"true\" is parsed from the column element")
  void wrapIsParsed() {
    assertTrue(parse("wrap=\"true\"").getWrap());
  }

  @Test
  @DisplayName("max-lines is parsed from the column element")
  void maxLinesIsParsed() {
    assertEquals(3, parse("max-lines=\"3\"").getMaxLines());
  }

  @Test
  @DisplayName("A column without the attributes does not wrap, so existing grids are unchanged")
  void columnWithoutAttributesDoesNotWrap() {
    Column column = parse("");

    assertNull(column.getWrap());
    assertNull(column.getMaxLines());
    assertFalse(column.isWrapped());
  }

  @Test
  @DisplayName("A column wraps when it declares wrap or a maximum number of lines")
  void columnWrapsWithWrapOrMaxLines() {
    assertTrue(new Column().setWrap(true).isWrapped());
    assertTrue(new Column().setMaxLines(2).isWrapped());
    assertFalse(new Column().setWrap(false).isWrapped());
    assertFalse(new Column().setWrap(false).setMaxLines(2).isWrapped());
    assertFalse(new Column().setMaxLines(0).isWrapped());
  }

  @Test
  @DisplayName("The wrapping attributes reach the clients")
  void serializeWrappingColumn() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column().setWrap(true).setMaxLines(3));

    assertTrue(json.get("wrap").asBoolean());
    assertEquals(3, json.get("maxLines").asInt());
  }

  @Test
  @DisplayName("Without the attributes the clients receive nothing new")
  void serializeColumnWithoutWrapping() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column());

    assertFalse(json.has("wrap"));
    assertFalse(json.has("maxLines"));
  }
}
