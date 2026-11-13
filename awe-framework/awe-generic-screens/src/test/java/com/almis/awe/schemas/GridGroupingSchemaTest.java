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
 * The published screen schema accepts the row grouping attributes of a grid (issue #833)
 */
class GridGroupingSchemaTest {

  private static File schemaFile;
  private static CatalogResolver catalogResolver;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(GridGroupingSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    // The published schemas include each other by URL: the catalog resolves them locally, so no network is needed
    catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(),
      GridGroupingSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI());
  }

  private static void validateGrid(String attributes) throws Exception {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(catalogResolver);
    Validator validator = factory.newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader(
      "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\"><grid id=\"Grd\" " + attributes + ">" +
        "<column name=\"country\" label=\"COUNTRY\"/></grid></tag></screen>")));
  }

  @Test
  void acceptsTheGroupingAttributes() {
    assertThatCode(() -> validateGrid("group-by=\"country,city\" group-selector=\"true\" group-subtotals=\"false\""))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsAGroupSelectorWithoutGroupBy() {
    assertThatCode(() -> validateGrid("group-selector=\"true\""))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsANonBooleanGroupSelector() {
    assertThatThrownBy(() -> validateGrid("group-selector=\"yes\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void rejectsANonBooleanGroupSubtotals() {
    assertThatThrownBy(() -> validateGrid("group-subtotals=\"never\""))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void keepsGridsWithoutTheAttributesValid() {
    assertThatCode(() -> validateGrid(""))
      .doesNotThrowAnyException();
  }
}
