package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The screen schema declares the side panel container (issue #843): a dialog anchored to the left or right edge.
 */
class SidePanelSchemaTest {

  private static File schemaFile;
  private static Document schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(SidePanelSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    schema = factory.newDocumentBuilder().parse(schemaFile);
  }

  private static Element named(String tag, String name) {
    NodeList nodes = schema.getElementsByTagNameNS(XMLConstants.W3C_XML_SCHEMA_NS_URI, tag);
    for (int i = 0; i < nodes.getLength(); i++) {
      Element node = (Element) nodes.item(i);
      if (name.equals(node.getAttribute("name"))) {
        return node;
      }
    }
    return null;
  }

  private static void validate(String body) throws Exception {
    Validator validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader(
      "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\">" + body + "</tag></screen>")));
  }

  @Test
  void declaresTheSidePanelElement() {
    Element sidePanel = named("element", "side-panel");

    assertThat(sidePanel).as("side-panel element").isNotNull();
    assertThat(sidePanel.getAttribute("type")).isEqualTo("sidePanelType");
  }

  @Test
  void acceptsASidePanelWithItsAttributesAndChildren() {
    assertThatCode(() -> validate("<side-panel id=\"Pnl\" label=\"LABEL\" position=\"left\" max-width=\"30rem\" " +
      "deferred=\"true\" on-close=\"reject\" icon=\"info\"><tag type=\"div\" style=\"modal-footer\"/></side-panel>"))
      .doesNotThrowAnyException();
  }

  @Test
  void gridChoosesTheColumnsPanel() {
    assertThatCode(() -> validate("<grid id=\"Grd\" columns-panel=\"side-panel\"/><grid id=\"Grd2\" columns-panel=\"dialog\"/>"))
      .doesNotThrowAnyException();
    assertThatThrownBy(() -> validate("<grid id=\"Grd\" columns-panel=\"popup\"/>")).hasMessageContaining("popup");
  }

  @Test
  void rejectsAPositionOtherThanLeftOrRight() {
    assertThatThrownBy(() -> validate("<side-panel id=\"Pnl\" position=\"top\"/>"))
      .hasMessageContaining("top");
  }
}
