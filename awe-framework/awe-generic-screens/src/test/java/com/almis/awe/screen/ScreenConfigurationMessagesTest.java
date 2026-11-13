package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.text.MessageFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Messages of the screen configuration validation (#792): they are formatted with MessageFormat, where a single
 * apostrophe quotes what follows, so a quoted parameter must use two
 */
class ScreenConfigurationMessagesTest {

  @ParameterizedTest
  @ValueSource(strings = {"Locale-en-GB.xml", "Locale-es-ES.xml", "Locale-fr-FR.xml"})
  @DisplayName("Every screen configuration message shows the values of its parameters")
  void messagesShowTheirParameters(String locale) throws Exception {
    NodeList nodes;
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream("application/awe/locale/" + locale)) {
      nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream).getElementsByTagName("locale");
    }
    int checked = 0;
    for (int i = 0; i < nodes.getLength(); i++) {
      Element node = (Element) nodes.item(i);
      String value = node.getAttribute("value");
      if (node.getAttribute("name").startsWith("ERROR_MESSAGE_SCREEN_CONFIGURATION_") && value.contains("{0}")) {
        String text = MessageFormat.format(value, "PARAM0", "PARAM1", "PARAM2");
        assertThat(text).as(locale + " " + node.getAttribute("name")).contains("PARAM0").doesNotContain("{");
        checked++;
      }
    }
    assertThat(checked).isGreaterThan(3);
  }
}
