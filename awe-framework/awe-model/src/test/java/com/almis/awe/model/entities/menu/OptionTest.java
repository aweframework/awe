package com.almis.awe.model.entities.menu;

import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
