package com.almis.awe.screen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generic screens of the React engine show texts through locale keys (label, title, placeholder...). Every key
 * they use has to be defined by this module, in the awe-react or awe locale files, in English, Spanish and French, so
 * an application does not have to define the texts of the AWE screens itself
 */
@DisplayName("Locale keys of the React generic screens")
class ReactScreenLocaleKeysTest {

  private static final Path APPLICATION = Path.of("src/main/resources/application");
  private static final List<String> LANGUAGES = List.of("en-GB", "es-ES", "fr-FR");
  private static final List<String> TEXT_ATTRIBUTES = List.of("label", "title", "placeholder", "help", "tooltip");
  private static final Pattern KEY = Pattern.compile("[A-Z][A-Z0-9_]+");
  /**
   * Screens of the awe module that the React shell opens by itself (the Settings entry of the user menu)
   */
  private static final List<String> SHELL_SCREENS = List.of("awe/screen/users/user-settings.xml");

  @Test
  @DisplayName("every key of the awe-react screens and the shell screens is defined in awe-react or awe, in every language")
  void screenKeysAreDefinedInEveryLanguage() throws Exception {
    Set<String> used = keysUsedByScreens();
    assertThat(used).as("keys found in the awe-react screens").isNotEmpty();
    for (String language : LANGUAGES) {
      Set<String> defined = new TreeSet<>(definedKeys("awe-react", language));
      defined.addAll(definedKeys("awe", language));
      Set<String> missing = new TreeSet<>(used);
      missing.removeAll(defined);
      assertThat(missing).as("keys used by the awe-react screens and missing in " + language).isEmpty();
    }
  }

  private static Set<String> keysUsedByScreens() throws Exception {
    Set<String> keys = new TreeSet<>();
    Path screens = APPLICATION.resolve("awe-react/screen");
    List<Path> files;
    try (Stream<Path> walk = Files.walk(screens)) {
      files = Stream.concat(walk.filter(path -> path.toString().endsWith(".xml")),
        SHELL_SCREENS.stream().map(APPLICATION::resolve)).toList();
    }
    for (Path file : files) {
      NodeList elements = parse(file).getElementsByTagName("*");
      for (int index = 0; index < elements.getLength(); index++) {
        NamedNodeMap attributes = ((Element) elements.item(index)).getAttributes();
        for (String name : TEXT_ATTRIBUTES) {
          if (attributes.getNamedItem(name) != null && KEY.matcher(attributes.getNamedItem(name).getNodeValue()).matches()) {
            keys.add(attributes.getNamedItem(name).getNodeValue());
          }
        }
      }
    }
    return keys;
  }

  private static Set<String> definedKeys(String module, String language) throws Exception {
    Set<String> keys = new TreeSet<>();
    NodeList locales = parse(APPLICATION.resolve(module + "/locale/Locale-" + language + ".xml")).getElementsByTagName("locale");
    for (int index = 0; index < locales.getLength(); index++) {
      keys.add(((Element) locales.item(index)).getAttribute("name"));
    }
    return keys;
  }

  private static Document parse(Path file) throws IOException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      return factory.newDocumentBuilder().parse(file.toFile());
    } catch (Exception exception) {
      throw new IOException("Cannot parse " + file, exception);
    }
  }
}
