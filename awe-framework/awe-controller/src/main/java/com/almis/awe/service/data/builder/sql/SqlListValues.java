package com.almis.awe.service.data.builder.sql;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Selects the values of a list variable that a filter or a {@code LIST_TO_STRING} variable binds. Used by
 * {@code SQLBuilder}; it is not an extension point.
 */
public final class SqlListValues {

  private SqlListValues() {
  }

  /**
   * Retrieve the values of a list variable to bind in an IN filter. A list of several values binds all of them, even
   * the empty ones; a single value is bound only when it is not empty.
   *
   * @param variableValue List variable value
   * @return Values to bind, in order; empty when there is nothing to bind
   */
  public static List<JsonNode> valuesToBind(JsonNode variableValue) {
    List<JsonNode> values = new ArrayList<>();
    int variableValuesLength = variableValue.size();
    if (variableValuesLength > 1) {
      // For each value, add a ? to the statement
      for (int i = 0; i < variableValuesLength; i++) {
        values.add(variableValue.get(i));
      }
    } else if (variableValuesLength == 1 && variableValue.isArray() && !variableValue.get(0).asString("").isEmpty()) {
      // One value NOT empty (in array)
      values.add(variableValue.get(0));
    } else if (variableValuesLength == 1 && !variableValue.asString("").isEmpty()) {
      // One value NOT empty
      values.add(variableValue);
    }
    return values;
  }

  /**
   * Join the non-empty values of a list, or a single value, with commas
   *
   * @param valueList List or single value, may be null
   * @param asString  Converts a value to text, null when it has no text
   * @return Joined values; empty when the list is null or has no non-empty value
   */
  public static String join(JsonNode valueList, Function<JsonNode, String> asString) {
    List<String> stringValues = new ArrayList<>();
    if (valueList != null && valueList.isArray()) {
      for (JsonNode value : valueList) {
        addIfNotEmpty(stringValues, asString.apply(value));
      }
    } else if (valueList != null) {
      addIfNotEmpty(stringValues, asString.apply(valueList));
    }
    return String.join(",", stringValues);
  }

  private static void addIfNotEmpty(List<String> list, String text) {
    if (text != null && !text.isEmpty()) {
      list.add(text);
    }
  }
}
