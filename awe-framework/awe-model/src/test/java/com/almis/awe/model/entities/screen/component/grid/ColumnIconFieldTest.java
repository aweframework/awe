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
 * Icon attribute of a grid column (issue #793): it is read from the XML and reaches the clients.
 */
class ColumnIconFieldTest {

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
    return (Column) xStreamMarshaller.fromXML("<column name=\"twoFactor\" label=\"2FA\" " + attributes + "/>");
  }

  @Test
  @DisplayName("The icon field is parsed from the column element, with the status field it combines with")
  void iconFieldIsParsed() {
    Column column = parse("icon-field=\"2faIcn\" status-field=\"2faTon\"");

    assertEquals("2faIcn", column.getIconField());
    assertEquals("2faTon", column.getStatusField());
  }

  @Test
  @DisplayName("A column without the icon field keeps it unset, so existing grids are unchanged")
  void columnWithoutIconField() {
    assertNull(parse("").getIconField());
  }

  @Test
  @DisplayName("The icon field reaches the clients with its camelCase name")
  void serializeColumnWithIconField() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column().setIconField("2faIcn"));

    assertEquals("2faIcn", json.get("iconField").asString());
  }

  @Test
  @DisplayName("Without the icon field the clients receive nothing new")
  void serializeColumnWithoutIconField() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column());

    assertFalse(json.has("iconField"));
  }
}
