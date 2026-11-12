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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The "image" element declares the title that both engines render as the tooltip of the picture
 */
class ImageElementXsdTest {

  private static Schema schema;

  @BeforeAll
  static void loadSchema() throws Exception {
    Path schemasDir = Paths.get(ImageElementXsdTest.class.getResource("/schemas/awe/catalog.xml").toURI()).getParent();
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
  void imageAcceptsATitle() {
    assertThatCode(() -> validate("<tag source=\"center\"><image id=\"Photo\" url=\"/images/photo.webp\""
      + " alternate-url=\"\" title=\"PHOTO_DESCRIPTION\" style=\"photo\"/></tag>")).doesNotThrowAnyException();
  }

  @Test
  void imageRejectsUnknownAttribute() {
    assertThatThrownBy(() -> validate("<tag source=\"center\"><image id=\"Photo\" bogus=\"x\"/></tag>"))
      .isInstanceOf(SAXException.class);
  }
}
