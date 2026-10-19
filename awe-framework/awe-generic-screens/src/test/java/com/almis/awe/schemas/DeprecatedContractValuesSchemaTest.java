package com.almis.awe.schemas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Values of the screen contract that no client engine handles are deprecated, not removed, in AWE 5.0: applications
 * that use them keep validating, and the schema documents the deprecation (issue #823).
 */
class DeprecatedContractValuesSchemaTest {

  private static final String DEPRECATION = "Deprecated in AWE 5.0 and removed in 6.0";

  private static Document schema(String file) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory.newDocumentBuilder()
      .parse(Paths.get(DeprecatedContractValuesSchemaTest.class.getResource("/schemas/awe/" + file).toURI()).toFile());
  }

  private static Element named(Document schema, String tag, String name) {
    NodeList nodes = schema.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, tag);
    for (int i = 0; i < nodes.getLength(); i++) {
      Element node = (Element) nodes.item(i);
      if (name.equals(node.getAttribute("name"))) {
        return node;
      }
    }
    return null;
  }

  private static List<Element> enumerations(String file, String simpleType) throws Exception {
    Element type = named(schema(file), "simpleType", simpleType);
    assertThat(type).as("simple type %s of %s", simpleType, file).isNotNull();
    NodeList nodes = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "enumeration");
    List<Element> values = new ArrayList<>();
    for (int i = 0; i < nodes.getLength(); i++) {
      values.add((Element) nodes.item(i));
    }
    return values;
  }

  private static Element enumeration(String file, String simpleType, String value) throws Exception {
    return enumerations(file, simpleType).stream()
      .filter(enumeration -> value.equals(enumeration.getAttribute("value"))).findFirst().orElse(null);
  }

  @ParameterizedTest
  @CsvSource({
    "general.xsd, buttonActionEnum, forward",
    "general.xsd, buttonActionEnum, check-leaf-selected",
    "general.xsd, buttonActionEnum, check-parent-expanded",
    "general.xsd, buttonActionEnum, get-locals",
    "screen.xsd, criteriaComponentType, progress"
  })
  void deadValueIsStillValidButDeprecated(String file, String simpleType, String value) throws Exception {
    Element enumeration = enumeration(file, simpleType, value);

    assertThat(enumeration).as("%s keeps accepting %s", simpleType, value).isNotNull();
    assertThat(enumeration.getTextContent()).contains(DEPRECATION);
  }

  @ParameterizedTest
  @CsvSource({"frame", "javascript"})
  void deadElementIsStillValidButDeprecated(String name) throws Exception {
    Element element = named(schema("screen.xsd"), "element", name);

    assertThat(element).as("screen.xsd keeps declaring <%s>", name).isNotNull();
    assertThat(element.getTextContent()).contains(DEPRECATION);
  }

  @Test
  void onlyTheDeadButtonActionsAreDeprecated() throws Exception {
    List<String> deprecated = enumerations("general.xsd", "buttonActionEnum").stream()
      .filter(enumeration -> enumeration.getTextContent().contains(DEPRECATION))
      .map(enumeration -> enumeration.getAttribute("value")).toList();

    assertThat(deprecated).containsExactlyInAnyOrder("forward", "check-leaf-selected", "check-parent-expanded", "get-locals");
  }

  @Test
  void closeWindowIsAButtonAction() throws Exception {
    assertThat(enumeration("general.xsd", "buttonActionEnum", "close-window"))
      .as("close-window, handled by both engines").isNotNull();
  }
}
