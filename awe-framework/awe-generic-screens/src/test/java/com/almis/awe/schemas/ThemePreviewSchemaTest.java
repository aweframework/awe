package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
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
 * The screen schema declares the theme preview (#949): the criteria of a theme it shows a sample of
 */
class ThemePreviewSchemaTest {

  private static File schemaFile;
  private static URI catalog;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(ThemePreviewSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    catalog = ThemePreviewSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI();
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
  void acceptsAThemePreviewOfTheCriteriaOfATheme() {
    assertThatCode(() -> validate("<tag type=\"div\"><theme-preview id=\"ThmPrv\" label=\"LABEL\" style=\"col-xs-12\" " +
      "primary=\"PriPal\" primary-hex=\"PriHex\" surface=\"SrfPal\" surface-hex=\"SrfHex\" mode=\"Mde\"/></tag>"))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsAnUnknownAttribute() {
    assertThatThrownBy(() -> validate("<theme-preview id=\"ThmPrv\" primary=\"PriPal\" density=\"Den\"/>"))
      .hasMessageContaining("density");
  }
}
