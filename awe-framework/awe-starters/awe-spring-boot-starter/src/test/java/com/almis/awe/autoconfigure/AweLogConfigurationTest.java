package com.almis.awe.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the property expressions of the logback configuration shipped with AWE (awe-log.xml).
 * Logback silently ignores a stray closing brace, so the resolved values hide such a typo: the raw source is checked.
 */
class AweLogConfigurationTest {

  @Test
  @DisplayName("Every property expression in awe-log.xml has balanced braces")
  void propertyExpressionsAreBalanced() throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("com/almis/awe/logging/awe-log.xml")) {
      assertThat(stream).isNotNull();
      String xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      xml.lines()
        .filter(line -> line.contains("<property "))
        .forEach(line -> assertThat(count(line, '}')).as(line).isEqualTo(count(line, '{')));
    }
  }

  private static long count(String text, char character) {
    return text.chars().filter(c -> c == character).count();
  }
}
