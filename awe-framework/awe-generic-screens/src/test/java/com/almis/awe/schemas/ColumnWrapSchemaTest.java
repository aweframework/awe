package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The grid column (attribute group columnAttr) declares the attributes that make its text wrap (issue #825) in the published screen schema.
 */
class ColumnWrapSchemaTest {

  private static Element columnType;

  @BeforeAll
  static void loadSchema() throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Document schema = factory.newDocumentBuilder()
      .parse(Paths.get(ColumnWrapSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile());
    NodeList types = schema.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "attributeGroup");
    for (int i = 0; i < types.getLength(); i++) {
      Element type = (Element) types.item(i);
      if ("columnAttr".equals(type.getAttribute("name"))) {
        columnType = type;
      }
    }
  }

  private static Element attribute(String name) {
    NodeList attributes = columnType.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, "attribute");
    for (int i = 0; i < attributes.getLength(); i++) {
      Element attribute = (Element) attributes.item(i);
      if (name.equals(attribute.getAttribute("name"))) {
        return attribute;
      }
    }
    return null;
  }

  @Test
  void columnDeclaresWrap() {
    Element wrap = attribute("wrap");

    assertThat(wrap).as("wrap attribute of the column").isNotNull();
    assertThat(wrap.getAttribute("type")).isEqualTo("booleanEnum");
    assertThat(wrap.getTextContent()).contains("lines");
  }

  @Test
  void columnDeclaresMaxLines() {
    Element maxLines = attribute("max-lines");

    assertThat(maxLines).as("max-lines attribute of the column").isNotNull();
    assertThat(maxLines.getAttribute("type")).isEqualTo("xs:positiveInteger");
    assertThat(maxLines.getTextContent()).contains("ellipsis");
  }
}
