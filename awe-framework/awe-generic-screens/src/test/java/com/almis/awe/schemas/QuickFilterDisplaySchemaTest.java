package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The screen schema lets a quick filter of a grid choose how it is shown: a segmented control (default), a dropdown
 * button or a date range button (#993)
 */
@DisplayName("Quick filter display in the screen schema")
class QuickFilterDisplaySchemaTest {

  private static File schemaFile;
  private static URI catalog;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(QuickFilterDisplaySchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    catalog = QuickFilterDisplaySchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI();
  }

  private static void validate(String display) throws Exception {
    // The catalog resolves the included general.xsd to the local file, so the test does not need the network
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalog));
    Validator validator = factory.newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader("<screen template=\"window\" label=\"LABEL\"><tag source=\"center\">" +
      "<grid id=\"Grd\" initial-load=\"query\" target-action=\"Lst\"><quick-filter id=\"CrtFlt\" initial-load=\"enum\" " +
      "target-action=\"Opt\"" + display + "/></grid></tag></screen>")));
  }

  @Test
  @DisplayName("A quick filter accepts display segmented, dropdown and date-range, or none")
  void acceptsTheDisplays() {
    for (String display : new String[]{"", " display=\"segmented\"", " display=\"dropdown\"", " display=\"date-range\""}) {
      assertThatCode(() -> validate(display)).as(display).doesNotThrowAnyException();
    }
  }

  @Test
  @DisplayName("A quick filter rejects another display")
  void rejectsAnotherDisplay() {
    assertThatThrownBy(() -> validate(" display=\"chip\"")).hasMessageContaining("chip");
  }
}
