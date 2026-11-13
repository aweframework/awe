package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The published screen schema accepts the attributes of a progress grid column that tune its bar (issue #831)
 */
class ColumnBarSchemaTest {

  private static File schemaFile;
  private static CatalogResolver catalogResolver;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(ColumnBarSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    // The published schemas include each other by URL: the catalog resolves them locally, so no network is needed
    catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(),
      ColumnBarSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI());
  }

  private static void validateColumn(String attributes) throws Exception {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(catalogResolver);
    Validator validator = factory.newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader(
      "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\"><grid id=\"Grd\">" +
        "<column name=\"name\" label=\"NAME\" component=\"progress\" " + attributes + "/></grid></tag></screen>")));
  }

  @Test
  void acceptsABarScale() {
    assertThatCode(() -> validateColumn("bar-min=\"-20.5\" bar-max=\"40\""))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsASecondFieldAndAStack() {
    assertThatCode(() -> validateColumn("bar-second-field=\"target\" bar-stack-fields=\"done,doing\""))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsLeafOnly() {
    assertThatCode(() -> validateColumn("bar-leaf-only=\"true\""))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsANonDecimalBarMax() {
    assertThatThrownBy(() -> validateColumn("bar-max=\"lots\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void rejectsANonDecimalBarMin() {
    assertThatThrownBy(() -> validateColumn("bar-min=\"none\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void rejectsANonBooleanLeafOnly() {
    assertThatThrownBy(() -> validateColumn("bar-leaf-only=\"yes\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void keepsProgressColumnsWithoutTheAttributesValid() {
    assertThatCode(() -> validateColumn(""))
      .doesNotThrowAnyException();
  }
}
