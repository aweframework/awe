package com.almis.awe.service.data.builder.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SqlListValues}
 */
class SqlListValuesTest {

  private static final JsonNodeFactory NODES = JsonNodeFactory.instance;
  private static final Function<JsonNode, String> AS_STRING = node -> node.isNull() ? null : node.asString("");

  @Test
  @DisplayName("A list of several values binds all of them, empty ones included")
  void severalValuesAreAllBound() {
    ArrayNode list = NODES.arrayNode().add("a").add("").add(3);

    assertThat(SqlListValues.valuesToBind(list)).containsExactly(list.get(0), list.get(1), list.get(2));
  }

  @Test
  @DisplayName("A single value is bound only when it is not empty")
  void singleValueIsBoundWhenNotEmpty() {
    ArrayNode single = NODES.arrayNode().add("a");

    assertThat(SqlListValues.valuesToBind(single)).containsExactly(single.get(0));
    assertThat(SqlListValues.valuesToBind(NODES.arrayNode().add(""))).isEmpty();
    assertThat(SqlListValues.valuesToBind(NODES.arrayNode())).isEmpty();
  }

  @Test
  @DisplayName("A scalar has no size, so it is not bound")
  void scalarIsNotBound() {
    assertThat(SqlListValues.valuesToBind(NODES.textNode("a"))).isEmpty();
  }

  @Test
  @DisplayName("join joins the non-empty values of a list, or a single value, with commas")
  void joinNonEmptyValues() {
    ArrayNode list = NODES.arrayNode().add("a").add("").add(NODES.nullNode()).add(3).add("b");

    assertThat(SqlListValues.join(list, AS_STRING)).isEqualTo("a,3,b");
    assertThat(SqlListValues.join(NODES.textNode("x"), AS_STRING)).isEqualTo("x");
  }

  @Test
  @DisplayName("join of a null, empty or blank list is empty")
  void joinOfNothingIsEmpty() {
    assertThat(SqlListValues.join(null, AS_STRING)).isEmpty();
    assertThat(SqlListValues.join(NODES.arrayNode(), AS_STRING)).isEmpty();
    assertThat(SqlListValues.join(NODES.arrayNode().add(""), AS_STRING)).isEmpty();
    assertThat(SqlListValues.join(NODES.textNode(""), AS_STRING)).isEmpty();
  }
}
