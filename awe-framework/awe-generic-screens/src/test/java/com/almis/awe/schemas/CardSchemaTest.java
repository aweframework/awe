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
 * The screen schema declares the card component (issue #899): a metric tile able to host other components.
 */
class CardSchemaTest {

  private static File schemaFile;
  private static URI catalog;
  private static Document schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(CardSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    catalog = CardSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI();
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
  void declaresTheCardElement() {
    Element card = named("element", "card");

    assertThat(card).as("card element").isNotNull();
    assertThat(card.getAttribute("type")).isEqualTo("cardType");
  }

  @Test
  void acceptsAMetricCardFilledFromAQuery() {
    assertThatCode(() -> validate("<card id=\"CrdCustomers\" icon=\"users\" initial-load=\"query\" " +
      "target-action=\"CardActiveCustomers\" autorefresh=\"60\"/>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsEveryStaticAttribute() {
    assertThatCode(() -> validate("<card id=\"Crd\" title=\"TITLE\" subtitle=\"SUB\" icon=\"bug\" " +
      "icon-tone=\"warning\" value=\"1.2\" unit=\"%\" delta=\"-0.4\" delta-unit=\"pt\" trend=\"down\" " +
      "caption=\"CAPTION\" description=\"DESCRIPTION\" footer=\"FOOTER\" target=\"2\" good-trend=\"down\" " +
      "tone=\"info\" number-decimals=\"1\" style=\"expand\"/>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsComponentsInItsBody() {
    assertThatCode(() -> validate("<card id=\"CrdSales\" title=\"SCREEN_TEXT_SALES\" icon=\"chart-line\">" +
      "<chart id=\"ChrSales\" type=\"line\"/><button id=\"BtnDetail\" label=\"LABEL\"/></card>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsCardsInsideCards() {
    assertThatCode(() -> validate("<window label=\"LABEL\"><card id=\"Crd\"><card id=\"Inner\"/></card></window>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsEveryTrendAndGoodTrend() {
    for (String trend : new String[]{"up", "down", "flat"}) {
      assertThatCode(() -> validate("<card id=\"Crd\" trend=\"" + trend + "\"/>")).as(trend)
        .doesNotThrowAnyException();
    }
    for (String goodTrend : new String[]{"up", "down"}) {
      assertThatCode(() -> validate("<card id=\"Crd\" good-trend=\"" + goodTrend + "\"/>")).as(goodTrend)
        .doesNotThrowAnyException();
    }
  }

  @Test
  void acceptsEveryTone() {
    for (String tone : new String[]{"success", "warning", "danger", "info", "neutral"}) {
      assertThatCode(() -> validate("<card id=\"Crd\" tone=\"" + tone + "\" icon-tone=\"" + tone + "\"/>"))
        .as(tone).doesNotThrowAnyException();
    }
  }

  @Test
  void acceptsASparklineAndEveryProgressStyle() {
    for (String type : new String[]{"line", "area", "bar"}) {
      assertThatCode(() -> validate("<card id=\"Crd\" sparkline=\"[1,2,3]\" sparkline-type=\"" + type + "\"/>"))
        .as(type).doesNotThrowAnyException();
    }
    for (String style : new String[]{"bar", "gauge"}) {
      assertThatCode(() -> validate("<card id=\"Crd\" progress-style=\"" + style + "\"/>")).as(style)
        .doesNotThrowAnyException();
    }
  }

  @Test
  void rejectsAnUnknownProgressStyle() {
    assertThatThrownBy(() -> validate("<card id=\"Crd\" progress-style=\"donut\"/>"))
      .hasMessageContaining("donut");
  }

  @Test
  void rejectsAGoodTrendOtherThanUpOrDown() {
    assertThatThrownBy(() -> validate("<card id=\"Crd\" good-trend=\"flat\"/>"))
      .hasMessageContaining("flat");
  }

  @Test
  void rejectsAnUnknownTone() {
    assertThatThrownBy(() -> validate("<card id=\"Crd\" tone=\"red\"/>"))
      .hasMessageContaining("red");
  }

  @Test
  void rejectsACardWithoutId() {
    assertThatThrownBy(() -> validate("<card title=\"TITLE\"/>"))
      .hasMessageContaining("id");
  }

  @Test
  void acceptsAChipWithEveryTone() {
    for (String tone : new String[]{"success", "warning", "danger", "info", "neutral"}) {
      assertThatCode(() -> validate("<card id=\"Crd\" chip=\"CHIP\" chip-tone=\"" + tone + "\"/>")).as(tone)
        .doesNotThrowAnyException();
    }
  }

  @Test
  void rejectsAnUnknownChipTone() {
    assertThatThrownBy(() -> validate("<card id=\"Crd\" chip=\"CHIP\" chip-tone=\"red\"/>"))
      .hasMessageContaining("red");
  }
}
