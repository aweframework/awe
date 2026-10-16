package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The optional "deferred" attribute of dialogs and tab pages (tabcontainer) is declared in the screen schema (#824)
 */
class DeferredAttributeXsdTest {

  private static Schema schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    Path schemasDir = Paths.get(DeferredAttributeXsdTest.class.getResource("/schemas/awe/catalog.xml").toURI()).getParent();
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setFeature(XMLConstants.USE_CATALOG, true);
    factory.setProperty(CatalogFeatures.Feature.FILES.getPropertyName(), schemasDir.resolve("catalog.xml").toUri().toString());
    schema = factory.newSchema(schemasDir.resolve("screen.xsd").toFile());
  }

  private static void validate(String body) throws Exception {
    String xml = "<screen label=\"SCREEN\" template=\"window\">" + body + "</screen>";
    schema.newValidator().validate(new StreamSource(new StringReader(xml)));
  }

  @Test
  void dialogAcceptsDeferredAttribute() throws Exception {
    validate("<tag source=\"center\"><dialog id=\"Dia\" deferred=\"true\"/></tag>");
    validate("<tag source=\"center\"><dialog id=\"Dia\" deferred=\"false\"/></tag>");
  }

  @Test
  void tabContainerAcceptsDeferredAttribute() throws Exception {
    validate("<tag source=\"center\"><tab id=\"Tab\" initial-load=\"enum\" target-action=\"Opts\">"
      + "<tabcontainer id=\"Page\" label=\"PAGE\" deferred=\"true\"/></tab></tag>");
  }

  @Test
  void deferredAttributeMustBeBoolean() {
    assertThatThrownBy(() -> validate("<tag source=\"center\"><dialog id=\"Dia\" deferred=\"maybe\"/></tag>"))
      .isInstanceOf(SAXException.class);
  }
}
