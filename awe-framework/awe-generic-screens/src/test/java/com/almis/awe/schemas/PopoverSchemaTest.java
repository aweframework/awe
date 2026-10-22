package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The screen schema declares the popover container (issue #843): a dialog anchored to a control of the screen.
 */
class PopoverSchemaTest {

  private static File schemaFile;
  private static URI catalog;
  private static Document schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(PopoverSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    catalog = PopoverSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI();
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
    // The catalog resolves the included general.xsd to the local file, so the test does not need the network
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalog));
    Validator validator = factory.newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader(
      "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\">" + body + "</tag></screen>")));
  }

  @Test
  void declaresThePopoverElement() {
    Element popover = named("element", "popover");

    assertThat(popover).as("popover element").isNotNull();
    assertThat(popover.getAttribute("type")).isEqualTo("popoverType");
  }

  @Test
  void acceptsAPopoverWithItsAttributesAndChildren() {
    assertThatCode(() -> validate("<button id=\"BtnInf\" label=\"LABEL\"/>" +
      "<popover id=\"Pop\" label=\"LABEL\" anchor=\"BtnInf\" position=\"right\" max-width=\"20rem\" " +
      "deferred=\"true\" on-close=\"reject\" icon=\"info\"><tag type=\"div\" style=\"modal-footer\"/></popover>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsEveryPosition() {
    for (String position : new String[]{"bottom", "top", "left", "right"}) {
      assertThatCode(() -> validate("<popover id=\"Pop\" anchor=\"BtnInf\" position=\"" + position + "\"/>"))
        .as(position).doesNotThrowAnyException();
    }
  }

  @Test
  void rejectsAPositionOtherThanBottomTopLeftOrRight() {
    assertThatThrownBy(() -> validate("<popover id=\"Pop\" anchor=\"BtnInf\" position=\"center\"/>"))
      .hasMessageContaining("center");
  }

  @Test
  void rejectsAPopoverWithoutAnchor() {
    assertThatThrownBy(() -> validate("<popover id=\"Pop\"/>"))
      .hasMessageContaining("anchor");
  }

  @Test
  void sharesTheIdUniquenessOfTheDialogs() {
    assertThatThrownBy(() -> validate("<dialog id=\"Dlg\"/><popover id=\"Dlg\" anchor=\"BtnInf\"/>"))
      .hasMessageContaining("DialogIdUnique");
  }
}
