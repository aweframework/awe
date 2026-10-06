package com.almis.awe.model.entities.queues;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Message parameter tests
 */
class MessageParameterTest {

  private static MessageParameter parameter(String type) {
    MessageParameter parameter = new MessageParameter();
    parameter.setName("param");
    parameter.setType(type);
    return parameter;
  }

  @Test
  void numericParameterWithoutValueIsTextNull() {
    assertNull(parameter("INTEGER").getParameterValueText(Collections.emptyMap()));
  }

  @Test
  void stringParameterWithoutValueIsTextNull() {
    assertNull(parameter("STRING").getParameterValueText(Collections.emptyMap()));
  }

  @Test
  void numericParameterValueIsConvertedToText() {
    Map<String, Object> values = Map.of("param", 25);

    assertEquals("25", parameter("INTEGER").getParameterValueText(values));
  }
}
