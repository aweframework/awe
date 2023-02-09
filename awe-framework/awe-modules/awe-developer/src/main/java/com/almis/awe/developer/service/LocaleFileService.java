package com.almis.awe.developer.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.XStreamSerializer;
import com.almis.awe.model.entities.XMLFile;
import com.almis.awe.model.entities.locale.Locales;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author agomez
 */
@Slf4j
public class LocaleFileService extends ServiceConfig {

  private static final String FILE_DESCRIPTION = "Application Locales";

  // Autowired services
  private final PathService pathService;
  private final XStreamSerializer serializer;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param pathService          Path service
   * @param serializer           Serializer
   * @param baseConfigProperties Base config properties
   */
  @Autowired
  public LocaleFileService(PathService pathService, XStreamSerializer serializer, BaseConfigProperties baseConfigProperties) {
    this.pathService = pathService;
    this.serializer = serializer;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve language list
   *
   * @return Language list
   */
  public List<String> getLanguageList() throws AWException {
    return getLanguageList(pathService.getPath());
  }

  /**
   * Read local list from file
   *
   * @param codeLang Language code (ES, EN, FR...)
   * @return List of locales loaded
   */
  public Locales readLocalesFromFile(String codeLang) throws AWException {

    String fileName = baseConfigProperties.getFiles().getLocale() + codeLang.toUpperCase();
    File xmlFile = Paths.get(pathService.getPath(),fileName + baseConfigProperties.getExtensionXml()).toFile();
    return (Locales) readXmlFile(xmlFile);
  }

  /**
   * Read all XML files and return them
   *
   * @param xmlFile Xml file path
   * @return Xml file object
   */
  private XMLFile readXmlFile(File xmlFile) {
    XMLFile xml = null;
    try {
      // Unmarshall XML
      if (xmlFile.exists()) {
        try (InputStream resourceInputStream = Files.newInputStream(xmlFile.toPath())) {
          xml = serializer.getObjectFromXml((Class<? extends XMLFile>) Locales.class, resourceInputStream);
          log.debug("Reading '{}' - OK", xmlFile);
        }
      } else {
        log.debug("Reading '{}' - NOT FOUND", xmlFile);
      }
    } catch (IOException exc) {
      log.error("Error parsing XML - '{}'", xmlFile, exc);
    }
    return xml;
  }

  /**
   * Store local file
   *
   * @param codeLang Language
   */
  public void storeLocaleListFile(String codeLang, Locales locales) throws AWException {

    String fileName = baseConfigProperties.getFiles().getLocale() + codeLang.toUpperCase();
    XStream xstream;
    // Define XML file
    File xmlFile = Paths.get(pathService.getPath(), fileName + baseConfigProperties.getExtensionXml()).toFile();

    try (FileOutputStream fileOutputStream = new FileOutputStream(xmlFile)) {
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
      printHeader(xmlOut, fileName, FILE_DESCRIPTION, true);
      xstream.toXML(locales, xmlOut);
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_STORE_FILE"),
        getLocale("ERROR_MESSAGE_STORE_FILE", fileName), exc);
    }
  }

  /**
   * Given a path, obtains the list of languages whose locale file is defined
   *
   * @param path Path
   * @return Language list
   */
  private List<String> getLanguageList(String path) {
    ArrayList<String> languages = new ArrayList<>();

    try {
      File folder = new File(path);
      if (folder.exists() && folder.isDirectory()) {

        String patternString = baseConfigProperties.getFiles().getLocale() + "([a-zA-Z]+)" + baseConfigProperties.getExtensionXml();
        final Pattern pattern = Pattern.compile(patternString);
        String[] files = folder.list((File dir, String name) -> pattern.matcher(name).matches());
        for (String file : Objects.requireNonNull(files)) {
          Matcher matcher = pattern.matcher(file);
          if (matcher.find()) {
            languages.add(matcher.group(1));
          }
        }
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return languages;
  }

  /**
   * Print an XML header into a file output stream
   *
   * @param out    Output Stream
   * @param doc    Document name
   * @param des    Document description
   * @param addHdg Enable print headers
   * @throws IOException IO exception error
   */
  public void printHeader(Writer out, String doc, String des, boolean addHdg) throws IOException {
    String strOut = "";
    strOut += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    if (addHdg) {
      strOut += "<!--\n";
      strOut += "  Document   : " + doc + "\n";
      strOut += "  Description: " + des + "\n";
      strOut += "-->\n\n";
    }
    // Add SVN id property to be expanded when committing a file
    strOut += "<!--" + (char) 36 + "Id" + (char) 36 + "-->\n\n";

    out.write(strOut);
  }
}
