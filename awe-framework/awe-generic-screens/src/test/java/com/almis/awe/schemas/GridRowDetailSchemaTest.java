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
 * The published screen schema accepts one row-detail template inside a grid
 */
class GridRowDetailSchemaTest {

  private static File schemaFile;
  private static CatalogResolver catalogResolver;

  @BeforeAll
  static void loadSchema() throws Exception {
    schemaFile = Paths.get(GridRowDetailSchemaTest.class.getResource("/schemas/awe/screen.xsd").toURI()).toFile();
    // The published schemas include each other by URL: the catalog resolves them locally, so no network is needed
    catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(),
      GridRowDetailSchemaTest.class.getResource("/schemas/awe/catalog.xml").toURI());
  }

  private static void validate(String body) throws Exception {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(catalogResolver);
    Validator validator = factory.newSchema(schemaFile).newValidator();
    validator.validate(new StreamSource(new StringReader(
      "<screen template=\"window\" label=\"LABEL\"><tag source=\"center\">" + body + "</tag></screen>")));
  }

  @Test
  void acceptsARowDetailTemplate() {
    assertThatCode(() -> validate("<grid id=\"Grd\" server-action=\"data\" target-action=\"Qry\">" +
      "<column name=\"name\" label=\"NAME\"/><row-detail><![CDATA[Name: {name}]]></row-detail></grid>"))
      .doesNotThrowAnyException();
  }

  @Test
  void acceptsTheStyleOfTheRowDetail() {
    assertThatCode(() -> validate("<grid id=\"Grd\"><column name=\"name\" label=\"NAME\"/>" +
      "<row-detail style=\"detail-card\"><![CDATA[Name: {name}]]></row-detail></grid>"))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsUnknownAttributesOfTheRowDetail() {
    assertThatThrownBy(() -> validate("<grid id=\"Grd\"><row-detail source=\"query\">{name}</row-detail></grid>"))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void keepsGridsWithoutRowDetailValid() {
    assertThatCode(() -> validate("<grid id=\"Grd\"><column name=\"name\" label=\"NAME\"/></grid>"))
      .doesNotThrowAnyException();
  }

  @Test
  void rejectsNestedElementsInsideTheTemplate() {
    assertThatThrownBy(() -> validate("<grid id=\"Grd\"><row-detail><column name=\"name\"/></row-detail></grid>"))
      .isInstanceOf(SAXException.class);
  }
}
