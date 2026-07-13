package com.almis.awe.service.hotreload;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validates an AWE XML definition against its declared schema, reusing the same OASIS catalog
 * ({@code schemas/awe/catalog.xml}, shipped by awe-generic-screens) that the compile-time
 * {@code xml-maven-plugin} uses. A definition that already passes the compile-time validation
 * therefore validates identically here, so hot reload can skip a broken edit and log a clean
 * message instead of letting the parse error crash mid-reload.
 *
 * <p>The validator fails open: whenever validation cannot run (no catalog on the classpath, a
 * deleted file, or any parser configuration problem) it reports "not validated" so the caller
 * keeps its previous behaviour and never blocks a legitimate reload.
 *
 * @author pvidal
 */
@Slf4j
public class XmlSchemaValidator {

  /**
   * The framework's own base OASIS catalog (shipped by awe-generic-screens). It maps the
   * framework schemas — such as {@code queries.xsd} — that handler-declared schemas
   * {@code <xs:include>}. Callers chaining a handler catalog must always include this one too,
   * otherwise an included framework schema cannot be resolved
   */
  public static final String AWE_BASE_CATALOG = "schemas/awe/catalog.xml";
  private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
  private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

  // Classpath locations of the OASIS catalogs, chained in order. Each catalog resolves the
  // schemas it maps; an entity missing from one continues to the next (RESOLVE=continue)
  private final List<String> catalogResources;
  // Resolved once at construction (the bean is created only when hot reload is enabled) and
  // only ever read afterwards, so no synchronization is needed. Null when no catalog is found
  private final CatalogResolver catalogResolver;

  /**
   * Default constructor: resolves schemas through the framework's base classpath catalog
   */
  public XmlSchemaValidator() {
    this(AWE_BASE_CATALOG);
  }

  /**
   * Constructor with a single explicit catalog resource (testing seam / built-in path)
   *
   * @param catalogResource Classpath location of the OASIS catalog
   */
  XmlSchemaValidator(String catalogResource) {
    this(List.of(catalogResource));
  }

  /**
   * Constructor chaining several catalog resources. The catalogs are consulted in order and a
   * systemId missing from one falls through to the next, so a handler-declared catalog can be
   * chained after the framework base catalog: the base catalog resolves included framework
   * schemas (e.g. {@code queries.xsd}) while the handler catalog resolves its own definitions
   * (e.g. {@code treatments.xsd}), with no key conflict between them
   *
   * @param catalogResources Classpath locations of the OASIS catalogs, in resolution order
   */
  public XmlSchemaValidator(List<String> catalogResources) {
    this.catalogResources = List.copyOf(catalogResources);
    this.catalogResolver = buildCatalogResolver();
  }

  /**
   * Validate an XML definition against its declared schema
   *
   * @param xmlFile XML definition file
   * @return Validation result (not-validated when validation could not run)
   */
  public ValidationResult validate(Path xmlFile) {
    if (xmlFile == null || !Files.isRegularFile(xmlFile)) {
      return ValidationResult.notValidated();
    }
    if (catalogResolver == null) {
      return ValidationResult.notValidated();
    }

    ErrorCollector collector = new ErrorCollector();

    // Build the hardened, validating reader first. A parser-configuration failure (an
    // unsupported feature/property) must fail open, never masquerade as a schema violation
    XMLReader reader;
    try {
      reader = createValidatingReader();
    } catch (Exception exc) {
      log.debug("XML hot reload: schema validation could not be configured for '{}'", xmlFile, exc);
      return ValidationResult.notValidated();
    }

    try (InputStream input = Files.newInputStream(xmlFile)) {
      reader.setEntityResolver(catalogResolver);
      reader.setErrorHandler(collector);
      InputSource source = new InputSource(input);
      source.setSystemId(xmlFile.toUri().toString());
      reader.parse(source);
    } catch (SAXParseException exc) {
      collector.add(exc);
    } catch (SAXException exc) {
      // A parse problem the handler did not surface: record it so the reload is skipped
      if (collector.errors.isEmpty()) {
        collector.errors.add(new ValidationError(-1, -1, exc.getMessage()));
      }
    } catch (Exception exc) {
      // Infrastructure problem (parser configuration, IO): fail open, do not block the reload
      log.debug("XML hot reload: schema validation could not run for '{}'", xmlFile, exc);
      return ValidationResult.notValidated();
    }
    return ValidationResult.validated(collector.errors);
  }

  /**
   * Build a namespace-aware, DTD-validating XML reader hardened against XXE and external
   * reference resolution. AWE definitions never declare a DOCTYPE, so disallowing doctypes and
   * external entities/DTDs is safe and keeps the single watch thread from fetching remote
   * resources. External schema access is restricted to local protocols so an unmapped schema
   * fails locally instead of hitting the network; catalog-resolved local/jar schemas still load.
   *
   * @return Configured XML reader
   * @throws Exception When a required feature/property is not supported (caller fails open)
   */
  private XMLReader createValidatingReader() throws Exception {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(true);
    factory.setXIncludeAware(false);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    SAXParser parser = factory.newSAXParser();
    parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
    // Local-only access: an unmapped schema fails locally instead of reaching the network,
    // while catalog-resolved local ('file') and packaged ('jar') schemas keep loading
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar");
    return parser.getXMLReader();
  }

  /**
   * Build the catalog resolver from every configured classpath catalog, chained in order.
   * {@link CatalogManager#catalogResolver(CatalogFeatures, URI...)} accepts several catalog
   * URIs and consults them in sequence, so a systemId missing from the first catalog is looked
   * up in the next. Fails open: if any configured catalog is missing from the classpath (or
   * cannot be turned into a URI) validation is disabled entirely and the caller keeps its
   * previous behaviour, exactly as with a single missing catalog
   *
   * @return Catalog resolver or null when any catalog cannot be loaded
   */
  private CatalogResolver buildCatalogResolver() {
    List<URI> catalogUris = new ArrayList<>();
    for (String resource : catalogResources) {
      URL catalogUrl = classLoaderResource(resource);
      if (catalogUrl == null) {
        log.warn("XML hot reload: schema catalog '{}' not found on the classpath; XML schema validation is disabled", resource);
        return null;
      }
      try {
        catalogUris.add(catalogUrl.toURI());
      } catch (Exception exc) {
        log.warn("XML hot reload: could not load schema catalog '{}'; XML schema validation is disabled", resource, exc);
        return null;
      }
    }
    if (catalogUris.isEmpty()) {
      return null;
    }
    try {
      // RESOLVE=continue: an entity missing from every catalog falls back to normal resolution
      // instead of throwing, so an unmapped schema never turns into a spurious validation error
      CatalogFeatures features = CatalogFeatures.builder()
        .with(CatalogFeatures.Feature.RESOLVE, "continue")
        .build();
      return CatalogManager.catalogResolver(features, catalogUris.toArray(new URI[0]));
    } catch (Exception exc) {
      log.warn("XML hot reload: could not load schema catalogs {}; XML schema validation is disabled", catalogResources, exc);
      return null;
    }
  }

  /**
   * Locate a catalog resource through the available class loaders
   *
   * @param resource Classpath location of the catalog
   * @return Catalog URL or null when not found
   */
  private URL classLoaderResource(String resource) {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    URL url = contextClassLoader != null ? contextClassLoader.getResource(resource) : null;
    return url != null ? url : getClass().getClassLoader().getResource(resource);
  }

  /**
   * Collects validation errors without aborting on recoverable ones. Fatal (not well-formed)
   * errors stop the parse; recoverable schema errors are accumulated
   */
  private static final class ErrorCollector extends DefaultHandler {
    private final List<ValidationError> errors = new ArrayList<>();

    @Override
    public void warning(SAXParseException exc) {
      // Warnings do not make a definition unusable: ignore
    }

    @Override
    public void error(SAXParseException exc) {
      add(exc);
    }

    @Override
    public void fatalError(SAXParseException exc) throws SAXException {
      add(exc);
      throw exc;
    }

    private void add(SAXParseException exc) {
      errors.add(new ValidationError(exc.getLineNumber(), exc.getColumnNumber(), exc.getMessage()));
    }
  }

  /**
   * A single schema validation error, located in the source file
   *
   * @param line    Line number (-1 when unknown)
   * @param column  Column number (-1 when unknown)
   * @param message Human-readable message
   */
  public record ValidationError(int line, int column, String message) {
    @Override
    public String toString() {
      return line + ":" + column + " " + message;
    }
  }

  /**
   * Outcome of a validation attempt.
   *
   * <p>{@code validated} tells whether validation actually ran; when false the caller must fail
   * open. {@code valid} is only meaningful when {@code validated} is true.
   */
  @Getter
  public static final class ValidationResult {

    private final boolean validated;
    private final List<ValidationError> errors;

    private ValidationResult(boolean validated, List<ValidationError> errors) {
      this.validated = validated;
      this.errors = Collections.unmodifiableList(errors);
    }

    /**
     * @return Result for a validation that could not run (fail open)
     */
    static ValidationResult notValidated() {
      return new ValidationResult(false, Collections.emptyList());
    }

    /**
     * @param errors Collected validation errors (empty means valid)
     * @return Result for a validation that ran
     */
    static ValidationResult validated(List<ValidationError> errors) {
      return new ValidationResult(true, new ArrayList<>(errors));
    }

    /**
     * @return Whether the definition satisfies its schema (true when validation did not run)
     */
    public boolean isValid() {
      return errors.isEmpty();
    }
  }
}
