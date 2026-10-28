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
 * Secondary line, status and chips attributes of a grid column (issue #832): they are read from the XML and reach the
 * clients.
 */
class ColumnCellContentTest {

  private static final List<String> NEW_PROPERTIES = List.of("secondaryField", "exportSecondary", "secondaryLabel",
    "statusField", "chips", "chipSeparator", "maxChips");

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
  @DisplayName("The secondary line attributes are parsed from the column element")
  void secondaryAttributesAreParsed() {
    Column column = parse("secondary-field=\"email\" export-secondary=\"true\" secondary-label=\"EMAIL\"");

    assertEquals("email", column.getSecondaryField());
    assertTrue(column.getExportSecondary());
    assertEquals("EMAIL", column.getSecondaryLabel());
  }

  @Test
  @DisplayName("The status field is parsed from the column element")
  void statusFieldIsParsed() {
    assertEquals("tone", parse("status-field=\"tone\"").getStatusField());
  }

  @Test
  @DisplayName("The chips attributes are parsed from the column element")
  void chipsAttributesAreParsed() {
    Column column = parse("chips=\"true\" chip-separator=\";\" max-chips=\"3\"");

    assertTrue(column.getChips());
    assertEquals(";", column.getChipSeparator());
    assertEquals(3, column.getMaxChips());
  }

  @Test
  @DisplayName("A column without the attributes keeps them unset, so existing grids are unchanged")
  void columnWithoutAttributes() {
    Column column = parse("");

    assertNull(column.getSecondaryField());
    assertNull(column.getExportSecondary());
    assertNull(column.getSecondaryLabel());
    assertNull(column.getStatusField());
    assertNull(column.getChips());
    assertNull(column.getChipSeparator());
    assertNull(column.getMaxChips());
  }

  @Test
  @DisplayName("The attributes reach the clients with their camelCase names")
  void serializeColumnWithCellContent() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column()
      .setSecondaryField("email")
      .setExportSecondary(true)
      .setSecondaryLabel("EMAIL")
      .setStatusField("tone")
      .setChips(true)
      .setChipSeparator(";")
      .setMaxChips(3));

    assertEquals("email", json.get("secondaryField").asString());
    assertTrue(json.get("exportSecondary").asBoolean());
    assertEquals("EMAIL", json.get("secondaryLabel").asString());
    assertEquals("tone", json.get("statusField").asString());
    assertTrue(json.get("chips").asBoolean());
    assertEquals(";", json.get("chipSeparator").asString());
    assertEquals(3, json.get("maxChips").asInt());
  }

  @Test
  @DisplayName("Without the attributes the clients receive nothing new")
  void serializeColumnWithoutCellContent() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column());

    NEW_PROPERTIES.forEach(property -> assertFalse(json.has(property), property));
  }
}
