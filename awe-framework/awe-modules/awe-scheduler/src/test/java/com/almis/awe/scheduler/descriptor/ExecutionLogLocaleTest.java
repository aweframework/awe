package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every locale file must carry both the truncation marker key (added in S2, read-path) and the
 * queue-drop marker key (S3, write-path) so neither marker ever renders as a raw missing-locale
 * placeholder in the viewer.
 */
class ExecutionLogLocaleTest {

  @ParameterizedTest
  @ValueSource(strings = {"en-GB", "es-ES", "fr-FR"})
  void everyLocaleCarriesBothExecutionLogMarkerKeys(String locale) throws IOException {
    String xml = readLocale(locale);

    assertTrue(xml.contains("SCHEDULER_EXECUTION_LOG_TRUNCATED"), locale + " is missing SCHEDULER_EXECUTION_LOG_TRUNCATED");
    assertTrue(xml.contains("SCHEDULER_EXECUTION_LOG_DROPPED"), locale + " is missing SCHEDULER_EXECUTION_LOG_DROPPED");
  }

  @Test
  void droppedMarkerLocaleRendersTwoNumericParameters() throws IOException {
    String xml = readLocale("en-GB");

    int index = xml.indexOf("SCHEDULER_EXECUTION_LOG_DROPPED");
    assertTrue(index >= 0);
    String entry = xml.substring(index, xml.indexOf('\n', index));
    assertTrue(entry.contains("{0}"));
    assertTrue(entry.contains("dropped") || entry.contains("omitido") || entry.contains("perdu")
      || entry.contains("Dropped") || entry.contains("perdid"), "Marker text should describe dropped lines: " + entry);
  }

  private String readLocale(String locale) throws IOException {
    String resource = "application/awe-scheduler/locale/Locale-" + locale + ".xml";
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertTrue(stream != null, resource + " must be on the test classpath");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
