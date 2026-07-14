package com.almis.awe.service.hotreload;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.service.BroadcastService;
import com.almis.awe.service.hotreload.XmlSchemaValidator.ValidationError;
import com.almis.awe.service.hotreload.XmlSchemaValidator.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Classifies changed XML definition files and triggers the matching {@link AweElements}
 * reload. Development-only service, wired when {@code awe.application.xml-hot-reload}
 * is enabled.
 *
 * @author pvidal
 */
@Slf4j
public class XmlHotReloadService {

  private static final String QUERY_DATA_CACHE = "queryData";
  private static final String RELOAD_PAGE_ACTION = "reload-page";
  // Logical (classpath-style) folder separator, always '/' regardless of the OS file separator
  private static final String FOLDER_SEPARATOR = "/";

  // Autowired services
  private final AweElements aweElements;
  private final BaseConfigProperties baseConfigProperties;
  private final CacheManager cacheManager;
  private final XmlSchemaValidator schemaValidator;
  private final BroadcastService broadcastService;
  // Sorted once at construction (ascending order); consulted only when classify() returns NONE
  private final List<XmlReloadHandler> reloadHandlers;
  // Built lazily: one entry per distinct catalog string that a handler declares via its schema catalog,
  // reused across events instead of rebuilding a CatalogResolver per file change
  private final Map<String, XmlSchemaValidator> handlerSchemaValidators = new ConcurrentHashMap<>();

  /**
   * XML artifact types handled by the hot reload
   */
  public enum XmlArtifactType {
    SCREEN, MENU, LOCALE, PROFILE, QUERIES, MAINTAIN, SERVICES, ENUMERATED, QUEUES, EMAIL, ACTIONS, NONE
  }

  /**
   * Autowired constructor
   *
   * @param aweElements          Awe elements
   * @param baseConfigProperties Base configuration properties
   * @param cacheManager         Cache manager (nullable, cache may not be configured)
   * @param schemaValidator      XML schema validator (guards reloads against invalid edits)
   * @param broadcastService     Broadcast service (nullable, tells connected clients to refresh)
   * @param reloadHandlers       Registered handlers for XML types unrecognized by {@link #classify(Path, Path)}
   *                             (nullable/empty: zero handlers is a pure no-op)
   */
  public XmlHotReloadService(AweElements aweElements, BaseConfigProperties baseConfigProperties, CacheManager cacheManager,
                             XmlSchemaValidator schemaValidator, BroadcastService broadcastService,
                             List<XmlReloadHandler> reloadHandlers) {
    this.aweElements = aweElements;
    this.baseConfigProperties = baseConfigProperties;
    this.cacheManager = cacheManager;
    this.schemaValidator = schemaValidator;
    this.broadcastService = broadcastService;
    this.reloadHandlers = (reloadHandlers == null ? List.<XmlReloadHandler>of() : reloadHandlers).stream()
      .sorted(Comparator.comparingInt(XmlReloadHandler::order))
      .toList();
  }

  /**
   * Classify a changed file into the XML artifact type it defines
   *
   * @param changedFile Changed file path
   * @return XML artifact type
   */
  public XmlArtifactType classify(Path changedFile) {
    return classify(changedFile.toString());
  }

  /**
   * Classify a changed file into the XML artifact type it defines, using the module root
   * it belongs to: the FIRST folder segment under the module decides the type, so module
   * names matching a category folder (e.g. a module literally named {@code screen}) never
   * misclassify files. Falls back to the path-based classification when no module root
   * is available.
   *
   * @param moduleRoot  Module root the changed file belongs to (nullable)
   * @param changedFile Changed file path
   * @return XML artifact type
   */
  public XmlArtifactType classify(Path moduleRoot, Path changedFile) {
    if (moduleRoot == null || !changedFile.startsWith(moduleRoot) || moduleRoot.getNameCount() >= changedFile.getNameCount()) {
      return classify(changedFile);
    }

    Path relative = moduleRoot.relativize(changedFile);
    String relativePath = FOLDER_SEPARATOR + relative.toString().replace('\\', '/');
    if (!relativePath.endsWith(baseConfigProperties.getExtensionXml()) || relative.getNameCount() < 2) {
      return XmlArtifactType.NONE;
    }

    // The first folder segment under the module root decides the category
    String firstFolder = FOLDER_SEPARATOR + relative.getName(0) + FOLDER_SEPARATOR;
    BaseConfigProperties.Paths paths = baseConfigProperties.getPaths();
    if (firstFolder.equals(paths.getScreen())) {
      return XmlArtifactType.SCREEN;
    } else if (firstFolder.equals(paths.getMenu())) {
      return XmlArtifactType.MENU;
    } else if (firstFolder.equals(paths.getLocale())) {
      return XmlArtifactType.LOCALE;
    } else if (firstFolder.equals(paths.getProfile())) {
      return XmlArtifactType.PROFILE;
    } else if (firstFolder.equals(paths.getGlobal())) {
      return classifyGlobalFile(relativePath);
    }
    return XmlArtifactType.NONE;
  }

  /**
   * Classify a changed file into the XML artifact type it defines. The classification is a
   * pure function of the file path: the containing folder decides the type, and files under
   * the global folder are classified by their file name.
   *
   * @param changedFile Changed file path
   * @return XML artifact type
   */
  public XmlArtifactType classify(String changedFile) {
    String path = changedFile.replace('\\', '/');
    if (!path.endsWith(baseConfigProperties.getExtensionXml())) {
      return XmlArtifactType.NONE;
    }

    BaseConfigProperties.Paths paths = baseConfigProperties.getPaths();
    if (path.contains(paths.getScreen())) {
      return XmlArtifactType.SCREEN;
    } else if (path.contains(paths.getMenu())) {
      return XmlArtifactType.MENU;
    } else if (path.contains(paths.getLocale())) {
      return XmlArtifactType.LOCALE;
    } else if (path.contains(paths.getProfile())) {
      return XmlArtifactType.PROFILE;
    } else if (path.contains(paths.getGlobal())) {
      return classifyGlobalFile(path);
    }
    return XmlArtifactType.NONE;
  }

  /**
   * Classify a file under the global folder by its file name
   *
   * @param path Normalized changed file path
   * @return XML artifact type
   */
  private XmlArtifactType classifyGlobalFile(String path) {
    String fileName = path.substring(path.lastIndexOf('/') + 1)
      .replace(baseConfigProperties.getExtensionXml(), "");
    BaseConfigProperties.Files files = baseConfigProperties.getFiles();
    if (fileName.equals(files.getQuery())) {
      return XmlArtifactType.QUERIES;
    } else if (fileName.equals(files.getMaintain())) {
      return XmlArtifactType.MAINTAIN;
    } else if (fileName.equals(files.getServices())) {
      return XmlArtifactType.SERVICES;
    } else if (fileName.equals(files.getEnumerated())) {
      return XmlArtifactType.ENUMERATED;
    } else if (fileName.equals(files.getQueue())) {
      return XmlArtifactType.QUEUES;
    } else if (fileName.equals(files.getEmail())) {
      return XmlArtifactType.EMAIL;
    } else if (fileName.equals(files.getActions())) {
      return XmlArtifactType.ACTIONS;
    }
    return XmlArtifactType.NONE;
  }

  /**
   * Classify a changed file and trigger the matching reload
   *
   * @param changedFile Changed file path
   */
  public void reloadFor(Path changedFile) {
    reloadFor(changedFile.toString());
  }

  /**
   * Classify a changed file using its module root and trigger the matching reload
   *
   * @param moduleRoot  Module root the changed file belongs to (nullable)
   * @param changedFile Changed file path
   */
  public void reloadFor(Path moduleRoot, Path changedFile) {
    XmlArtifactType type = classify(moduleRoot, changedFile);
    if (type == XmlArtifactType.NONE) {
      dispatchToHandlers(moduleRoot, changedFile);
      return;
    }
    reloadFor(type, changedFile.toString());
  }

  /**
   * Classify a changed file and trigger the matching reload
   *
   * @param changedFile Changed file path
   */
  public void reloadFor(String changedFile) {
    reloadFor(classify(changedFile), changedFile);
  }

  /**
   * Trigger the reload matching an XML artifact type
   *
   * @param type        XML artifact type
   * @param changedFile Changed file path (for logging)
   */
  private void reloadFor(XmlArtifactType type, String changedFile) {
    if (type != XmlArtifactType.NONE && isInvalidAgainstSchema(changedFile)) {
      return;
    }
    switch (type) {
      case SCREEN -> aweElements.reloadScreens();
      case MENU -> aweElements.reloadMenus();
      case LOCALE -> aweElements.reloadLocales();
      case PROFILE -> aweElements.reloadProfiles();
      case QUERIES -> {
        aweElements.reloadQueries();
        evictQueryDataCache();
      }
      case MAINTAIN -> {
        // Maintains can invalidate cached query results (mirrors MaintainLauncher eviction)
        aweElements.reloadMaintains();
        evictQueryDataCache();
      }
      case SERVICES -> aweElements.reloadServices();
      case ENUMERATED -> aweElements.reloadEnumerated();
      case QUEUES -> aweElements.reloadQueues();
      case EMAIL -> aweElements.reloadEmails();
      case ACTIONS -> aweElements.reloadActions();
      default -> {
        log.debug("XML hot reload: ignored change in '{}'", changedFile);
        return;
      }
    }
    // Reload failures are logged by the failing layer, so only report what was triggered
    log.info("XML hot reload: {} reload triggered by change in '{}'", type, changedFile);
    broadcastReload();
  }

  /**
   * Consult the registered handlers, ascending by {@link XmlReloadHandler#order()}, for a
   * changed file the built-in classification could not recognize. The first handler whose
   * {@link XmlReloadHandler#supports} matches is invoked; the rest are not consulted for this
   * event. When the matching handler declares a {@link XmlReloadHandler#schemaCatalog()}, the
   * framework validates the changed file against it BEFORE calling {@link XmlReloadHandler#reload}:
   * a validated-and-invalid file is never reloaded, mirroring the built-in validation gate. A
   * failing handler (or an unexpected validation error) is caught and logged so it never kills
   * the watch loop
   *
   * @param moduleRoot  Module root the changed file belongs to (nullable)
   * @param changedFile Changed file path
   */
  private void dispatchToHandlers(Path moduleRoot, Path changedFile) {
    for (XmlReloadHandler handler : reloadHandlers) {
      if (!handler.supports(moduleRoot, changedFile)) {
        continue;
      }
      try {
        if (isInvalidAgainstHandlerSchema(handler, changedFile)) {
          return;
        }
        XmlReloadHandler.ReloadResult result = handler.reload(moduleRoot, changedFile);
        if (result == XmlReloadHandler.ReloadResult.HANDLED) {
          log.info("XML hot reload: '{}' reload triggered by change in '{}'", handler.getClass().getSimpleName(), changedFile);
          broadcastReload();
        } else {
          log.debug("XML hot reload: '{}' left '{}' unhandled ({})", handler.getClass().getSimpleName(), changedFile, result);
        }
      } catch (Exception exc) {
        log.error("XML hot reload: handler '{}' failed for '{}'. The watcher keeps running", handler.getClass().getSimpleName(), changedFile, exc);
      }
      return;
    }
    log.debug("XML hot reload: ignored change in '{}' (no handler matched)", changedFile);
  }

  /**
   * Validate a changed definition against its schema before reloading. Fails open: only a
   * definition that actually validated and broke its schema blocks the reload; anything the
   * validator could not check (missing catalog, deleted file) proceeds as before
   *
   * @param changedFile Changed file path
   * @return Whether the reload must be skipped because the definition is invalid
   */
  private boolean isInvalidAgainstSchema(String changedFile) {
    ValidationResult validation = schemaValidator.validate(Paths.get(changedFile));
    if (validation.isValidated() && !validation.isValid()) {
      log.warn("XML hot reload: '{}' is not valid against its schema; keeping the previous version and skipping reload:\n{}",
        changedFile, formatErrors(validation.getErrors()));
      return true;
    }
    return false;
  }

  /**
   * Validate a changed file against the catalog a handler declares through
   * {@link XmlReloadHandler#schemaCatalog()}, before that handler's reload is invoked. Fails
   * open, exactly like {@link #isInvalidAgainstSchema}: only a definition that actually
   * validated and broke its schema blocks the reload. A handler declaring no catalog (default:
   * empty) skips validation entirely — the framework calls {@link XmlReloadHandler#reload}
   * unconditionally
   *
   * @param handler     Matching handler (already selected by {@code supports})
   * @param changedFile Changed file path
   * @return Whether the reload must be skipped because the definition is invalid
   */
  private boolean isInvalidAgainstHandlerSchema(XmlReloadHandler handler, Path changedFile) {
    Optional<String> catalog = handler.schemaCatalog();
    if (catalog.isEmpty()) {
      return false;
    }
    // Chain the framework base catalog BEFORE the handler catalog so framework schemas a
    // handler XSD <xs:include>s (e.g. queries.xsd, mapped only by the base catalog) resolve,
    // while the handler catalog still resolves its own definitions (e.g. treatments/kuts).
    // Cache keyed by the handler catalog string, one validator per distinct handler catalog
    XmlSchemaValidator validator = handlerSchemaValidators.computeIfAbsent(catalog.get(),
      handlerCatalog -> new XmlSchemaValidator(List.of(XmlSchemaValidator.AWE_BASE_CATALOG, handlerCatalog)));
    ValidationResult validation = validator.validate(changedFile);
    if (validation.isValidated() && !validation.isValid()) {
      log.warn("XML hot reload: '{}' is not valid against handler '{}' schema catalog '{}'; keeping the previous version"
          + " and skipping reload:\n{}",
        changedFile, handler.getClass().getSimpleName(), catalog.get(), formatErrors(validation.getErrors()));
      return true;
    }
    return false;
  }

  /**
   * Format schema validation errors as an indented, one-per-line block for the log
   *
   * @param errors Validation errors
   * @return Human-readable multiline message
   */
  private String formatErrors(List<ValidationError> errors) {
    return errors.stream()
      .map(error -> "  - line " + error.line() + ", column " + error.column() + ": " + error.message())
      .collect(Collectors.joining("\n"));
  }

  /**
   * Reload every element map and evict the query data cache. Used when file events have
   * been lost (watch service overflow) and the changed files are unknown
   */
  public void reloadAll() {
    aweElements.reloadAll();
    evictQueryDataCache();
    log.info("XML hot reload: full reload triggered");
    broadcastReload();
  }

  /**
   * Refresh every connected client without reloading any server-side definition. Used when a
   * frontend asset (JS/CSS) is rebuilt by the webpack watcher, so connected browsers reload and
   * fetch the new bundle — the frontend counterpart of the XML reload broadcast
   */
  public void refreshClients() {
    broadcastReload();
  }

  /**
   * Tell every connected client to refresh so a hot-reloaded definition is picked up without a
   * manual browser refresh. Fails open: when no broadcast service is available (no websocket
   * broker configured) the reload still happens and the developer refreshes manually
   */
  private void broadcastReload() {
    if (broadcastService != null) {
      broadcastService.broadcastMessage(new ClientAction(RELOAD_PAGE_ACTION).setAsync(true));
      log.debug("XML hot reload: '{}' broadcast to connected clients", RELOAD_PAGE_ACTION);
    }
  }

  /**
   * Evict the query data cache (if a cache manager is configured)
   */
  private void evictQueryDataCache() {
    if (cacheManager != null) {
      Cache queryDataCache = cacheManager.getCache(QUERY_DATA_CACHE);
      if (queryDataCache != null) {
        queryDataCache.clear();
        log.info("XML hot reload: '{}' cache evicted", QUERY_DATA_CACHE);
      }
    }
  }
}
