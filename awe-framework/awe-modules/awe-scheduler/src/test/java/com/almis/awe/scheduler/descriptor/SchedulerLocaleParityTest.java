package com.almis.awe.scheduler.descriptor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Every language of the scheduler module carries every key of the English locales, with a text
 */
class SchedulerLocaleParityTest {

  @ParameterizedTest
  @ValueSource(strings = {"es-ES", "fr-FR"})
  void everyLanguageCarriesEveryEnglishKey(String language) throws Exception {
    Map<String, String> english = read("en-GB");
    Map<String, String> other = read(language);

    List<String> missing = english.keySet().stream()
      .filter(key -> other.getOrDefault(key, "").isBlank())
      .toList();
    assertEquals(List.of(), missing, language + " lacks these keys");
  }

  private static Map<String, String> read(String language) throws Exception {
    String resource = "application/awe-scheduler/locale/Locale-" + language + ".xml";
    Map<String, String> locales = new LinkedHashMap<>();
    try (InputStream stream = SchedulerLocaleParityTest.class.getClassLoader().getResourceAsStream(resource)) {
      assertNotNull(stream, resource);
      NodeList entries = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        .getElementsByTagName("locale");
      for (int index = 0; index < entries.getLength(); index++) {
        Element entry = (Element) entries.item(index);
        locales.put(entry.getAttribute("name"), entry.getAttribute("value"));
      }
    }
    return locales;
  }
}
