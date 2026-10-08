package com.almis.awe.schemas;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the published XSDs and the OASIS catalog (schemas/awe/catalog.xml) in sync.
 * The catalog is what lets the build, the hot-reload validator and the IDEs resolve the
 * schema URLs referenced by the application XML (and the xs:include of each XSD) to the
 * local files, so a missing entry only shows up later as a validation failure or a network call.
 */
class XsdCatalogConsistencyTest {

  private static final String CATALOG_NS = "urn:oasis:names:tc:entity:xmlns:xml:catalog";
  private static final String XSD_NS = XMLConstants.W3C_XML_SCHEMA_NS_URI;

  private static Path schemasDir;
  private static Map<String, String> catalog;
  private static List<String> systemIds;

  @BeforeAll
  static void loadCatalog() throws Exception {
    schemasDir = Paths.get(XsdCatalogConsistencyTest.class.getResource("/schemas/awe/catalog.xml").toURI()).getParent();
    catalog = new TreeMap<>();
    systemIds = new ArrayList<>();
    Element root = parse(schemasDir.resolve("catalog.xml")).getDocumentElement();
    NodeList systems = root.getElementsByTagNameNS(CATALOG_NS, "system");
    for (int i = 0; i < systems.getLength(); i++) {
      Element system = (Element) systems.item(i);
      systemIds.add(system.getAttribute("systemId"));
      catalog.put(system.getAttribute("systemId"), system.getAttribute("uri"));
    }
  }

  /**
   * A repeated system identifier would silently collapse into a single map entry
   */
  @Test
  void catalogSystemIdsAreUnique() {
    List<String> duplicated = systemIds.stream()
      .filter(systemId -> systemIds.indexOf(systemId) != systemIds.lastIndexOf(systemId)).distinct().toList();
    assertThat(duplicated).as("system ids declared more than once in catalog.xml").isEmpty();
  }

  /**
   * Every XSD shipped in the schemas folder must be reachable through the catalog
   */
  @Test
  void everyXsdHasACatalogEntry() throws Exception {
    List<String> mappedFiles = new ArrayList<>();
    catalog.values().forEach(uri -> mappedFiles.add(fileName(uri)));

    List<String> xsds = listXsds();
    assertThat(xsds).as("published XSDs").isNotEmpty();
    assertThat(xsds.stream().filter(xsd -> !mappedFiles.contains(xsd)).toList())
      .as("XSDs without a <system> entry in catalog.xml").isEmpty();
  }

  /**
   * Every catalog entry must point to an existing XSD
   */
  @Test
  void everyCatalogEntryPointsToAnExistingXsd() {
    assertThat(catalog).as("catalog entries").isNotEmpty();
    catalog.forEach((systemId, uri) -> assertThat(schemasDir.resolve(uri))
      .as("catalog entry %s -> %s", systemId, uri).isRegularFile());
  }

  /**
   * Every xs:include / xs:import schemaLocation of the XSDs must be resolved by the catalog
   * (the XSDs reference each other by the published URL, never by a relative path)
   */
  @Test
  void everySchemaLocationResolvesThroughTheCatalog() throws Exception {
    List<String> unresolved = new ArrayList<>();
    for (String xsd : listXsds()) {
      Element schema = parse(schemasDir.resolve(xsd)).getDocumentElement();
      for (String tag : new String[]{"include", "import", "redefine"}) {
        NodeList nodes = schema.getElementsByTagNameNS(XSD_NS, tag);
        for (int i = 0; i < nodes.getLength(); i++) {
          Element reference = (Element) nodes.item(i);
          if (!reference.hasAttribute("schemaLocation")) {
            // An xs:import may omit schemaLocation (namespace-only hint): there is nothing to resolve
            continue;
          }
          String location = reference.getAttribute("schemaLocation");
          String uri = catalog.get(location);
          if (uri == null || !Files.isRegularFile(schemasDir.resolve(uri))) {
            unresolved.add(xsd + " -> " + location);
          }
        }
      }
    }
    assertThat(unresolved).as("schemaLocation values not resolved by catalog.xml").isEmpty();
  }

  /**
   * The system identifiers are the published URLs, named after the XSD they map to
   */
  @Test
  void catalogSystemIdsAreThePublishedUrls() {
    catalog.forEach((systemId, uri) -> assertThat(URI.create(systemId).getPath())
      .as("system id %s", systemId).startsWith("/awe/docs/schemas/").endsWith("/" + fileName(uri)));
  }

  private static List<String> listXsds() throws Exception {
    try (Stream<Path> files = Files.list(schemasDir)) {
      return files.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".xsd")).sorted().toList();
    }
  }

  private static String fileName(String uri) {
    return Paths.get(uri).getFileName().toString();
  }

  private static Document parse(Path file) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    return factory.newDocumentBuilder().parse(file.toFile());
  }
}
