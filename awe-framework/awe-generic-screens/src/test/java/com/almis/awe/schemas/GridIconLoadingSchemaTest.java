package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
 * The loading icons of the published screen schema offer both grid skeletons: skeleton (placeholder rows on every load) and
 * skeleton-dim (placeholder rows on the first load, dimmed rows on a reload).
 */
class GridIconLoadingSchemaTest {

  private static Document schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    schema = factory.newDocumentBuilder()
      .parse(Paths.get(GridIconLoadingSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile());
  }

  private static List<String> loadingIcons() {
    List<String> values = new ArrayList<>();
    NodeList types = schema.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "simpleType");
    for (int i = 0; i < types.getLength(); i++) {
      Element type = (Element) types.item(i);
      if ("loadingIconEnum".equals(type.getAttribute("name"))) {
        NodeList enumerations = type.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "enumeration");
        for (int j = 0; j < enumerations.getLength(); j++) {
          values.add(((Element) enumerations.item(j)).getAttribute("value"));
        }
      }
    }
    return values;
  }

  @Test
  void loadingIconsOfferBothGridSkeletons() {
    assertThat(loadingIcons()).contains("skeleton", "skeleton-dim", "spinner");
  }
}
