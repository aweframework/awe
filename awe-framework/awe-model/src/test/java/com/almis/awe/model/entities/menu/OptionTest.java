package com.almis.awe.model.entities.menu;

import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Option serialization tests
 */
class OptionTest {

  private static final List<String> EXPECTED_FIELDS = List.of("options", "actions", "name", "label", "icon", "visible", "restricted");

  /**
   * Children options must be serialized only once, under "options"
   *
   * @throws Exception Test error
   */
  @Test
  void serializeOptionWithoutElementList() throws Exception {
    // Prepare a three level menu tree: root > child > grandchild
    Option grandchild = buildOption("grandchild");
    Option child = buildOption("child");
    child.addElement(grandchild);
    Option root = buildOption("root");
    root.addElement(child);

    // Run
    JsonNode json = new ObjectMapper().valueToTree(root);

    // Assert
    List<JsonNode> serializedOptions = new ArrayList<>();
    collectOptions(json, serializedOptions);
    assertEquals(3, serializedOptions.size());
    for (JsonNode option : serializedOptions) {
      assertFalse(option.has("elementList"), "Option must not serialize elementList: " + option.get("name"));
      EXPECTED_FIELDS.forEach(field -> assertTrue(option.has(field), "Missing field " + field + " in " + option.get("name")));
    }
    assertEquals("grandchild", json.get("options").get(0).get("options").get(0).get("name").asText());
  }

  /**
   * The description is serialized as a key, next to the label
   */
  @Test
  void serializeDescription() {
    Option option = buildOption("described").setDescription("DESC_described");

    JsonNode json = new ObjectMapper().valueToTree(option);

    assertEquals("DESC_described", json.get("description").asText());
  }

  /**
   * The description attribute is read from the menu XML
   */
  @Test
  void parseDescriptionAttribute() {
    XStream xStream = new XStream();
    xStream.autodetectAnnotations(true);
    xStream.processAnnotations(Option.class);
    xStream.allowTypesByWildcard(new String[]{"com.almis.awe.model.entities.**"});
    xStream.aliasSystemAttribute(null, "class");

    Option option = (Option) xStream.fromXML("<option name=\"opt\" label=\"L\" description=\"DESC_opt\"/>");

    assertEquals("DESC_opt", option.getDescription());
  }

  private Option buildOption(String name) {
    Option option = new Option().setIcon("icon-" + name).setActionList(List.of(new ButtonAction()));
    option.setName(name);
    option.setLabel("LABEL_" + name);
    return option;
  }

  private void collectOptions(JsonNode option, List<JsonNode> collected) {
    collected.add(option);
    option.get("options").forEach(child -> collectOptions(child, collected));
  }
}
