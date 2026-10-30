package com.almis.awe.model.entities.screen.component.grid;

import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bar attributes of a progress grid column (issue #831): they are read from the XML and reach the clients.
 */
class ColumnBarTest {

  private static final List<String> NEW_PROPERTIES = List.of("barMin", "barMax", "barSecondField", "barStackFields",
    "barLeafOnly");

  private static final String SECOND_FIELD = "target";
  private static final String STACK_FIELDS = "done,doing";

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
    return (Column) xStreamMarshaller.fromXML("<column name=\"progress\" label=\"PROGRESS\" component=\"progress\" "
      + attributes + "/>");
  }

  @Test
  @DisplayName("The bar scale attributes are parsed from the column element")
  void barScaleIsParsed() {
    Column column = parse("bar-min=\"-20.5\" bar-max=\"40\"");

    assertEquals(-20.5, column.getBarMin());
    assertEquals(40.0, column.getBarMax());
  }

  @Test
  @DisplayName("The second value, stack and leaf only attributes are parsed from the column element")
  void barContentIsParsed() {
    Column column = parse("bar-second-field=\"" + SECOND_FIELD + "\" bar-stack-fields=\"" + STACK_FIELDS
      + "\" bar-leaf-only=\"true\"");

    assertEquals(SECOND_FIELD, column.getBarSecondField());
    assertEquals(STACK_FIELDS, column.getBarStackFields());
    assertTrue(column.getBarLeafOnly());
  }

  @Test
  @DisplayName("A column without the attributes keeps them unset, so existing grids are unchanged")
  void columnWithoutAttributes() {
    Column column = parse("");

    assertNull(column.getBarMin());
    assertNull(column.getBarMax());
    assertNull(column.getBarSecondField());
    assertNull(column.getBarStackFields());
    assertNull(column.getBarLeafOnly());
  }

  @Test
  @DisplayName("The attributes reach the clients with their camelCase names")
  void serializeColumnWithBar() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column()
      .setBarMin(-10.0)
      .setBarMax(40.0)
      .setBarSecondField(SECOND_FIELD)
      .setBarStackFields(STACK_FIELDS)
      .setBarLeafOnly(true));

    assertEquals(-10.0, json.get("barMin").asDouble());
    assertEquals(40.0, json.get("barMax").asDouble());
    assertEquals(SECOND_FIELD, json.get("barSecondField").asString());
    assertEquals(STACK_FIELDS, json.get("barStackFields").asString());
    assertTrue(json.get("barLeafOnly").asBoolean());
  }

  @Test
  @DisplayName("Without the attributes the clients receive nothing new")
  void serializeColumnWithoutBar() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column());

    NEW_PROPERTIES.forEach(property -> assertFalse(json.has(property), property));
  }
}
