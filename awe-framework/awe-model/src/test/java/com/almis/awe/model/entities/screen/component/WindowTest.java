package com.almis.awe.model.entities.screen.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Window serialization tests
 */
class WindowTest {

  /**
   * The window identifier must reach the clients, which expose it so dependency actions can target the window
   */
  @Test
  void serializeWindowWithItsIdentifier() {
    // Prepare
    Window window = new Window();
    window.setId("ExecutionsWindow");

    // Run
    JsonNode json = new ObjectMapper().valueToTree(window);

    // Assert
    assertEquals("ExecutionsWindow", json.get("id").asText());
  }
}
