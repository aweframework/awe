package com.almis.awe.testing.selenium;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds XPath string literals from text supplied by a test (labels, searches, identifiers), so a quote in the text
 * cannot break the expression or change what it selects.
 */
final class XpathLiterals {

  private XpathLiterals() {
    // Utility class
  }

  /**
   * Get the XPath literal of a text: {@code 'x'} when it has no apostrophe, {@code "x"} when it has apostrophes but no
   * double quotes, and a {@code concat(...)} of both kinds of quote otherwise
   *
   * @param text Text
   * @return XPath expression that evaluates to the text
   */
  static String of(String text) {
    String value = String.valueOf(text);
    if (!value.contains("'")) {
      return "'" + value + "'";
    }
    if (!value.contains("\"")) {
      return "\"" + value + "\"";
    }
    // Split on the apostrophes: 'a', "'", 'b' ...
    List<String> arguments = new ArrayList<>();
    String[] parts = value.split("'", -1);
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) {
        arguments.add("\"'\"");
      }
      if (!parts[i].isEmpty()) {
        arguments.add("'" + parts[i] + "'");
      }
    }
    return "concat(" + String.join(", ", arguments) + ")";
  }
}
