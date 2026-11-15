package com.almis.awe.schemas;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Builds validators for the screen schema without the network: catalog.xml resolves the included general.xsd to the
 * local file instead of the website.
 */
public final class ScreenSchema {

  private ScreenSchema() {
  }

  /**
   * New validator for screen.xsd, with every include resolved through catalog.xml
   *
   * @return validator of screen descriptors
   * @throws SAXException       the schema is not valid
   * @throws URISyntaxException a schema resource has an invalid location
   */
  public static Validator validator() throws SAXException, URISyntaxException {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    factory.setResourceResolver(CatalogManager.catalogResolver(CatalogFeatures.defaults(),
      resource("/schemas/awe/catalog.xml").toURI()));
    File schema = Paths.get(resource("/schemas/awe/screen.xsd").toURI()).toFile();
    return factory.newSchema(schema).newValidator();
  }

  private static URL resource(String path) {
    return ScreenSchema.class.getResource(path);
  }
}
