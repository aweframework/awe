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
 * The published screen schema accepts the attributes of a grid column that show a secondary line, a status or a list
 * of chips in its cells (issue #832)
 */
class ColumnCellContentSchemaTest {

  private static File schemaFile;
  private static CatalogResolver catalogResolver;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(ColumnCellContentSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    // The published schemas include each other by URL: the catalog resolves them locally, so no network is needed
    catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(),
      ColumnCellContentSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI());
  }

  private static void validateColumn(String attributes) throws Exception {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(catalogResolver);
    Validator validator = factory.newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader(
      "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\"><grid id=\"Grd\">" +
        "<column name=\"name\" label=\"NAME\" " + attributes + "/></grid></tag></screen>")));
  }

  @Test
  void acceptsASecondaryFieldExportedWithItsOwnLabel() {
    assertThatCode(() -> validateColumn("secondary-field=\"email\" export-secondary=\"true\" secondary-label=\"EMAIL\""))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsAStatusField() {
    assertThatCode(() -> validateColumn("status-field=\"tone\" secondary-field=\"since\""))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsChipsWithSeparatorAndMaximum() {
    assertThatCode(() -> validateColumn("chips=\"true\" chip-separator=\";\" max-chips=\"3\""))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsASwatchField() {
    assertThatCode(() -> validateColumn("swatch-field=\"color\" secondary-field=\"surface\""))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsANonBooleanExportSecondary() {
    assertThatThrownBy(() -> validateColumn("secondary-field=\"email\" export-secondary=\"yes\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void rejectsANonBooleanChips() {
    assertThatThrownBy(() -> validateColumn("chips=\"many\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void rejectsAZeroMaxChips() {
    assertThatThrownBy(() -> validateColumn("chips=\"true\" max-chips=\"0\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void rejectsANonNumericMaxChips() {
    assertThatThrownBy(() -> validateColumn("chips=\"true\" max-chips=\"all\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void keepsColumnsWithoutTheAttributesValid() {
    assertThatCode(() -> validateColumn(""))
      .doesNotThrowAnyException();
  }
}
