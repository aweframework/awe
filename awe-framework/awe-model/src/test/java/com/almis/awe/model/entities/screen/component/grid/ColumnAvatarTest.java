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

/**
 * Avatar attributes of a grid column (issue #793): they are read from the XML and reach the clients.
 */
class ColumnAvatarTest {

  private static final List<String> NEW_PROPERTIES = List.of("avatarField", "avatarToneField");

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
    return (Column) xStreamMarshaller.fromXML("<column name=\"user\" label=\"USER\" " + attributes + "/>");
  }

  @Test
  @DisplayName("The avatar attributes are parsed from the column element")
  void avatarAttributesAreParsed() {
    Column column = parse("avatar-field=\"Ini\" avatar-tone-field=\"IniTon\" secondary-field=\"login\"");

    assertEquals("Ini", column.getAvatarField());
    assertEquals("IniTon", column.getAvatarToneField());
    assertEquals("login", column.getSecondaryField());
  }

  @Test
  @DisplayName("A column without the avatar attributes keeps them unset, so existing grids are unchanged")
  void columnWithoutAvatar() {
    Column column = parse("");

    assertNull(column.getAvatarField());
    assertNull(column.getAvatarToneField());
  }

  @Test
  @DisplayName("The avatar attributes reach the clients with their camelCase names")
  void serializeColumnWithAvatar() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column()
      .setAvatarField("Ini")
      .setAvatarToneField("IniTon"));

    assertEquals("Ini", json.get("avatarField").asString());
    assertEquals("IniTon", json.get("avatarToneField").asString());
  }

  @Test
  @DisplayName("Without the avatar attributes the clients receive nothing new")
  void serializeColumnWithoutAvatar() {
    JsonNode json = JsonMapper.builder().build().valueToTree(new Column());

    NEW_PROPERTIES.forEach(property -> assertFalse(json.has(property), property));
  }
}
