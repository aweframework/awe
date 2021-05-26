package com.almis.awe.developer.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.developer.util.LocaleUtil;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.XMLFile;
import com.almis.awe.model.entities.locale.Locales;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author agomez
 */
@Log4j2
public class LocaleFileService extends ServiceConfig {

  private static final String FILE_DESCRIPTION = "Application Locales";

  // Autowired services
  private final PathService pathService;
  private final XStreamSerializer serializer;

  @Value("${extensions.xml:.xml}")
  private String xmlExtension;
  // Locale file names without language code
  @Value("${application.files.locale:Locale-}")
  private String localeFile;

  /**
   * Autowired constructor
   *
   * @param pathService Path service
   * @param serializer  Serializer
   */
  @Autowired
  public LocaleFileService(PathService pathService, XStreamSerializer serializer) {
    this.pathService = pathService;
    this.serializer = serializer;
  }

  /**
   * Retrieve language list
   *
   * @return Language list
   */
  public List<String> getLanguageList() throws AWException {
    return LocaleUtil.getLanguageList(pathService.getPath());
  }

  /**
   * Read local list from file
   *
   * @param codeLang Language code (ES, EN, FR...)
   * @return List of locales loaded
   */
  public Locales readLocalesFromFile(String codeLang) throws AWException {

    String fileName = localeFile + codeLang.toUpperCase();
    String path = pathService.getPath() + fileName + xmlExtension;

    return (Locales) readXmlFile(path);
  }

  /**
   * Read all XML files and return them
   *
   * @param path File path
   * @return Xml file object
   */
  private XMLFile readXmlFile(String path) {
    XMLFile xml = null;
    try {
      // Unmarshall XML
      File file = new File(path);
      if (file.exists()) {
        try (InputStream resourceInputStream = new FileInputStream(file)) {
          xml = serializer.getObjectFromXml((Class<? extends XMLFile>) Locales.class, resourceInputStream);
          log.debug("Reading '{}' - OK", path);
        }
      } else {
        log.debug("Reading '{}' - NOT FOUND", path);
      }
    } catch (IOException exc) {
      log.error("Error parsing XML - '{}'", path, exc);
    }
    return xml;
  }

  /**
   * Store local file
   *
   * @param codeLang Language
   */
  public void storeLocaleListFile(String codeLang, Locales locales) throws AWException {

    String fileName = localeFile + codeLang.toUpperCase();
    XStream xstream;
    // Define XML path
    String xmlPth = pathService.getPath() + fileName + xmlExtension;

    try (FileOutputStream fileOutputStream = new FileOutputStream(xmlPth)) {
      // Retrieve xstream serializer
      xstream = new XStream(new XppDriver() {
        @Override
        public HierarchicalStreamWriter createWriter(Writer out) {
          return new PrettyPrintWriter(out) {
            @Override
            protected void writeText(QuickWriter writer, String text) {
              if (!text.trim().isEmpty()) {
                writer.write("<![CDATA[");
                writer.write(text);
                writer.write("]]>");
              }
            }
          };
        }
      });

      // Process locales annotations
      xstream.processAnnotations(Locales.class);

      // Generate xml file
      BufferedWriter xmlOut = new BufferedWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8));
      LocaleUtil.printHeader(xmlOut, fileName, FILE_DESCRIPTION, true);
      xstream.toXML(locales, xmlOut);
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_STORE_FILE"),
        getLocale("ERROR_MESSAGE_STORE_FILE", fileName), exc);
    }
  }
}
