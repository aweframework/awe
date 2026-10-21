package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The components that open a context menu on right click (issue #801) accept context buttons and separators in the
 * published screen schema.
 */
class ContextMenuOwnersSchemaTest {

  private static Document schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    schema = factory.newDocumentBuilder()
      .parse(Paths.get(ContextMenuOwnersSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile());
  }

  private static boolean typeAccepts(String typeName, String elementName) {
    NodeList types = schema.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "complexType");
    for (int i = 0; i < types.getLength(); i++) {
      Element type = (Element) types.item(i);
      if (typeName.equals(type.getAttribute("name"))) {
        NodeList refs = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "element");
        for (int j = 0; j < refs.getLength(); j++) {
          if (elementName.equals(((Element) refs.item(j)).getAttribute("ref"))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @ParameterizedTest
  @ValueSource(strings = {"buttonType", "tabType", "wizardType", "criteriaType"})
  void ownerAcceptsContextButtons(String typeName) {
    assertThat(typeAccepts(typeName, "context-button")).as("context-button in " + typeName).isTrue();
    assertThat(typeAccepts(typeName, "context-separator")).as("context-separator in " + typeName).isTrue();
  }
}
