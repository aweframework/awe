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
 * The "map" element (Leaflet map showing one marker per data row) is declared in the screen schema (#798)
 */
class MapElementXsdTest {

  private static Schema schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    Path schemasDir = Paths.get(MapElementXsdTest.class.getResource("/schemas/awe/catalog.xml").toURI()).getParent();
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
  void mapAcceptsAllAttributesAndDependencies() throws Exception {
    validate("<tag source=\"center\"><map id=\"fleet\" initial-load=\"query\" target-action=\"TstMapPoints\""
      + " latitude=\"Lat\" longitude=\"Lon\" marker-label=\"Lug\" zoom=\"4\""
      + " center-latitude=\"43.86\" center-longitude=\"1.11\" style=\"expand\">"
      + "<dependency initial=\"true\" server-action=\"data\" target-action=\"Other\">"
      + "<dependency-element id=\"fleet\" condition=\"eq\" value=\"1\"/></dependency></map></tag>");
  }

  @Test
  void mapRejectsUnknownAttribute() {
    assertThatThrownBy(() -> validate("<tag source=\"center\"><map id=\"fleet\" bogus=\"x\"/></tag>"))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void mapRejectsNonNumericZoom() {
    assertThatThrownBy(() -> validate("<tag source=\"center\"><map id=\"fleet\" zoom=\"far\"/></tag>"))
      .isInstanceOf(SAXException.class);
  }

  @Test
  void mapIdMustBeUnique() {
    assertThatThrownBy(() -> validate("<tag source=\"center\"><map id=\"fleet\"/><map id=\"fleet\"/></tag>"))
      .isInstanceOf(SAXException.class);
  }
}
