package com.almis.awe.model.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWERuntimeException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.dto.XMLInitData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Actions;
import com.almis.awe.model.entities.email.Email;
import com.almis.awe.model.entities.email.Emails;
import com.almis.awe.model.entities.enumerated.Enumerated;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.maintain.Maintain;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.entities.permissions.Permissions;
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queues.Queue;
import com.almis.awe.model.entities.queues.Queues;
import com.almis.awe.model.entities.screen.Include;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.screen.component.action.ButtonAction;
import com.almis.awe.model.entities.screen.component.button.AbstractButton;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.RowDetail;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.model.entities.services.Services;
import com.almis.awe.model.type.LaunchPhaseType;
import jakarta.annotation.PostConstruct;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.almis.awe.model.constant.AweConstants.*;

/**
 * The type Awe elements.
 *
 * @author pgarcia
 */
@Slf4j
public class AweElements {

  private static final String NOT_FOUND = " not found: ";
  private static final String PROFILE = "profile";
  private static final String SCREEN = "screen";
  private static final String PERMISSION_DETAIL = "' (permission '";
  private static final String KEEPING_PREVIOUS_CATALOG = "Invalid permission catalog: keeping previous definitions. {}";
  // Autowired services
  private final WebApplicationContext context;
  private final AweElementsDao elementsDao;
  private final Environment environment;
  private final BaseConfigProperties baseConfigProperties;
  // Elements (volatile so reloads can atomically swap the whole map while requests keep reading)
  private volatile Map<String, EnumeratedGroup> enumeratedList;
  private volatile Map<String, Query> queryList;
  private volatile Map<String, Queue> queueList;
  private volatile Map<String, Target> maintainList;
  private volatile Map<String, Email> emailList;
  private volatile Map<String, Service> serviceList;
  private volatile Map<String, Action> actionList;
  private volatile Map<String, Profile> profileList;
  private volatile Map<String, Menu> menuList;
  private volatile Map<String, Screen> screenMap;
  // Swapped whole on reload while requests keep reading it
  private final AtomicReference<PermissionCatalog> permissionCatalog = new AtomicReference<>();
  // Incremented each time the screens, menus, queries or maintains are loaded again
  private final AtomicLong reloadVersion = new AtomicLong();
  // Locale list
  private volatile Map<String, Map<String, String>> localeList;

  /**
   * Autowired constructor
   *
   * @param context              Application context
   * @param baseConfigProperties base configuration properties
   * @param elementsDao          Element DAO
   */
  public AweElements(WebApplicationContext context, BaseConfigProperties baseConfigProperties, AweElementsDao elementsDao) {
    this.context = context;
    this.baseConfigProperties = baseConfigProperties;
    this.elementsDao = elementsDao;
    this.environment = context.getEnvironment();
  }

  /**
   * Read all XML files and store them in the component
   */
  @PostConstruct
  public void init() {
    log.info(LOG_LINE);
    log.info("----------------------------- AWE starting ... -----------------------------------");
    log.info(LOG_LINE);

    // Initialize Awe Elements (Read all XML sources)
    log.info("=============================");
    log.info("===== Reading XML Files =====");
    log.info("=============================");

    // Initialize global files
    log.info(" ===== Initializing global, screen and locale files ===== ");
    waitForTermination(launchGlobalScreenAndLocaleFiles());
    log.info(" ===== Finished loading global, screen and locale files  ===== ");

    // Initialize menu files
    log.info(" ===== Initializing menu files ===== ");
    reloadMenus();
    log.info(" ===== Finished loading menu files  ===== ");
  }

  /**
   * Version of the loaded definitions. It changes each time the screens, menus, queries or maintains are loaded
   * again, so a cache built from them can tell when it is stale
   *
   * @return Reload version
   */
  public long getReloadVersion() {
    return reloadVersion.get();
  }

  /**
   * Reload all element maps (global files, screens, locales, profiles and menus).
   * Each map is fully rebuilt before being swapped in, so concurrent readers never
   * observe a partially loaded state.
   */
  public void reloadAll() {
    waitForTermination(launchGlobalScreenAndLocaleFiles());
    reloadMenus();
  }

  /**
   * Reload the enumerated map from the XML sources
   */
  public void reloadEnumerated() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchEnumerated(initData));
    enumeratedList = keepPreviousOnEmptyRebuild(initData.getEnumerated(), enumeratedList, "enumerated");
  }

  /**
   * Reload the query map from the XML sources
   */
  public void reloadQueries() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchQueries(initData));
    Map<String, Query> newQueryList = keepPreviousOnEmptyRebuild(initData.getQueries(), queryList, "query");
    List<String> problems = findQueryPermissionProblems(newQueryList, getPermissionCatalog());
    if (!problems.isEmpty()) {
      log.error("Invalid permissions in the queries: keeping previous definitions. {}", problems);
      return;
    }
    queryList = newQueryList;
    reloadVersion.incrementAndGet();
  }

  /**
   * Reload the queue map from the XML sources
   */
  public void reloadQueues() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchQueues(initData));
    queueList = keepPreviousOnEmptyRebuild(initData.getQueues(), queueList, "queue");
  }

  /**
   * Reload the maintain map from the XML sources
   */
  public void reloadMaintains() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchMaintains(initData));
    Map<String, Target> newMaintainList = keepPreviousOnEmptyRebuild(initData.getMaintains(), maintainList, "maintain");
    List<String> problems = findMaintainPermissionProblems(newMaintainList, getPermissionCatalog());
    if (!problems.isEmpty()) {
      log.error("Invalid permissions in the maintain targets: keeping previous definitions. {}", problems);
      return;
    }
    maintainList = newMaintainList;
    reloadVersion.incrementAndGet();
  }

  /**
   * Reload the email map from the XML sources
   */
  public void reloadEmails() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchEmails(initData));
    emailList = keepPreviousOnEmptyRebuild(initData.getEmails(), emailList, "email");
  }

  /**
   * Reload the service map from the XML sources
   */
  public void reloadServices() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchServices(initData));
    Map<String, Service> newServiceList = keepPreviousOnEmptyRebuild(initData.getServices(), serviceList, "service");
    List<String> problems = findServicePermissionProblems(newServiceList, getPermissionCatalog());
    if (!problems.isEmpty()) {
      log.error("Invalid permissions in the services: keeping previous definitions. {}", problems);
      return;
    }
    serviceList = newServiceList;
  }

  /**
   * Reload the action map from the XML sources
   */
  public void reloadActions() {
    XMLInitData initData = new XMLInitData();
    waitForFuture(launchActions(initData));
    actionList = keepPreviousOnEmptyRebuild(initData.getActions(), actionList, "action");
  }

  /**
   * Reload the permission catalog from the XML sources. An invalid catalog, or one without a permission that a query,
   * a maintain target, a service, a screen element or a profile file requires, keeps the previous one
   */
  public void reloadPermissions() {
    PermissionCatalog newCatalog = buildPermissionCatalog(launchPermissions());
    List<String> problems = findPermissionProblems(queryList, maintainList, serviceList, newCatalog);
    problems.addAll(findScreenPermissionProblems(screenMap, newCatalog));
    problems.addAll(findProfilePermissionProblems(profileList, newCatalog));
    if (!problems.isEmpty() && permissionCatalog.get() != null) {
      log.error(KEEPING_PREVIOUS_CATALOG, problems);
      return;
    }
    permissionCatalog.set(newCatalog);
  }

  /**
   * Reload the profile map from the XML sources. Profile files with a permission or an exclusion that is not valid
   * against the catalog keep the previous profiles
   */
  public void reloadProfiles() {
    swapProfiles(keepPreviousOnEmptyRebuild(mergeModuleResults(launchProfiles(), PROFILE), profileList, PROFILE),
      false);
  }

  /**
   * Reload the whole screen map from the XML sources. The full per-module folder scan
   * is repeated because include targets are embedded into containing screens at read
   * time, so evicting a single screen is not enough. Screens with an invalid permission
   * attribute keep the previous screens.
   */
  public void reloadScreens() {
    Map<String, Screen> newScreenMap = keepPreviousOnEmptyRebuild(mergeModuleResults(launchScreens(), SCREEN),
      screenMap, SCREEN);
    List<String> problems = findScreenPermissionProblems(newScreenMap, getPermissionCatalog());
    if (!problems.isEmpty()) {
      log.error("Invalid permissions in the screens: keeping previous definitions. {}", problems);
      return;
    }
    screenMap = newScreenMap;
    reloadVersion.incrementAndGet();
  }

  /**
   * Reload the locale maps from the XML sources
   */
  public void reloadLocales() {
    localeList = keepPreviousLocalesOnEmptyRebuild(mergeLocaleResults(launchLocales()), localeList);
  }

  /**
   * Reload the menu map from the XML sources
   */
  public void reloadMenus() {
    // Init menus in cache
    Map<String, Menu> newMenuList = new ConcurrentHashMap<>();
    // Public menu
    reloadMenu(newMenuList, baseConfigProperties.getFiles().getMenuPublic());
    // Private menu
    reloadMenu(newMenuList, baseConfigProperties.getFiles().getMenuPrivate());
    menuList = newMenuList;
    reloadVersion.incrementAndGet();
  }

  /**
   * Read a menu file into the new menu map. When the menu cannot be read, keep the
   * previously loaded menu (never store a null menu)
   *
   * @param newMenuList New menu map being built
   * @param menuId      Menu identifier
   */
  private void reloadMenu(Map<String, Menu> newMenuList, String menuId) {
    Menu menu = null;
    try {
      menu = readMenuFile(menuId);
    } catch (AWException exc) {
      log.error("Error reading menu '{}'", menuId, exc);
      exc.log();
    }
    if (menu != null) {
      newMenuList.put(menuId, menu);
      return;
    }

    // Keep the previous menu definition when available
    Map<String, Menu> currentMenuList = menuList;
    if (currentMenuList != null && currentMenuList.containsKey(menuId)) {
      log.error("Menu '{}' could not be read: keeping previous definitions", menuId);
      newMenuList.put(menuId, currentMenuList.get(menuId));
    } else {
      log.error("Menu '{}' could not be read and there is no previous definition to keep", menuId);
    }
  }

  /**
   * Wait executor for termination and swap the element maps once fully built
   *
   * @param initData Initialize data threads
   */
  protected void waitForTermination(XMLInitData initData) {
    for (Future<String> result : initData.getGeneral()) {
      waitForFuture(result);
    }

    // Combine the permission catalog and check against it the permissions of the queries, targets and services
    boolean startup = permissionCatalog.get() == null;
    swapPermissionDefinitions(buildPermissionCatalog(initData.getPermissionResults()),
      keepPreviousOnEmptyRebuild(initData.getQueries(), queryList, "query"),
      keepPreviousOnEmptyRebuild(initData.getMaintains(), maintainList, "maintain"),
      keepPreviousOnEmptyRebuild(initData.getServices(), serviceList, "service"));

    // Swap global maps (fully built at this point, empty rebuilds keep the previous maps)
    enumeratedList = keepPreviousOnEmptyRebuild(initData.getEnumerated(), enumeratedList, "enumerated");
    queueList = keepPreviousOnEmptyRebuild(initData.getQueues(), queueList, "queue");
    emailList = keepPreviousOnEmptyRebuild(initData.getEmails(), emailList, "email");
    actionList = keepPreviousOnEmptyRebuild(initData.getActions(), actionList, "action");

    // Read profile list, checking its permissions and exclusions against the swapped catalog
    swapProfiles(keepPreviousOnEmptyRebuild(mergeModuleResults(initData.getProfileResults(), PROFILE), profileList,
      PROFILE), startup);

    // Read screen list, checking the permissions of its elements against the swapped catalog
    swapScreens(keepPreviousOnEmptyRebuild(mergeModuleResults(initData.getScreenResults(), SCREEN), screenMap, SCREEN),
      startup);

    // Read locale list
    localeList = keepPreviousLocalesOnEmptyRebuild(mergeLocaleResults(initData.getLocaleResults()), localeList);
    reloadVersion.incrementAndGet();
  }

  /**
   * Swap the profiles. A permission or an exclusion of a profile file that is not valid against the catalog fails the
   * startup; on a reload the previous profiles are kept
   *
   * @param newProfileList New profile map
   * @param startup        The definitions are loaded for the first time
   */
  private void swapProfiles(Map<String, Profile> newProfileList, boolean startup) {
    List<String> problems = findProfilePermissionProblems(newProfileList, getPermissionCatalog());
    if (problems.isEmpty()) {
      profileList = newProfileList;
    } else if (startup) {
      throw new AWERuntimeException("Invalid permissions: " + String.join("; ", problems));
    } else {
      log.error("Invalid permissions in the profiles: keeping previous definitions. {}", problems);
    }
  }

  /**
   * Swap the screens. A screen element whose permission is not valid against the catalog fails the startup; on a
   * reload the previous screens are kept
   *
   * @param newScreenMap New screen map
   * @param startup      The definitions are loaded for the first time
   */
  private void swapScreens(Map<String, Screen> newScreenMap, boolean startup) {
    List<String> problems = findScreenPermissionProblems(newScreenMap, permissionCatalog.get());
    if (problems.isEmpty()) {
      screenMap = newScreenMap;
    } else if (startup) {
      throw new AWERuntimeException("Invalid permissions: " + String.join("; ", problems));
    } else {
      log.error("Invalid permissions in the screens: keeping previous definitions. {}", problems);
    }
  }

  /**
   * Swap the permission catalog, the queries, the maintain targets and the services. A definition whose
   * {@code permission} is not valid against the catalog fails the startup; on a reload the previous queries, targets
   * or services are kept, and the previous catalog is kept when the new one misses a permission they, the loaded
   * screens or the loaded profiles require. The new
   * definitions are then checked again against the previous catalog, so the swapped definitions always match the
   * swapped catalog
   *
   * @param newCatalog      New permission catalog
   * @param newQueryList    New query map
   * @param newMaintainList New maintain map
   * @param newServiceList  New service map
   */
  private void swapPermissionDefinitions(PermissionCatalog newCatalog, Map<String, Query> newQueryList,
                                         Map<String, Target> newMaintainList, Map<String, Service> newServiceList) {
    if (permissionCatalog.get() == null) {
      List<String> problems = findPermissionProblems(newQueryList, newMaintainList, newServiceList, newCatalog);
      if (!problems.isEmpty()) {
        throw new AWERuntimeException("Invalid permissions: " + String.join("; ", problems));
      }
      permissionCatalog.set(newCatalog);
      queryList = newQueryList;
      maintainList = newMaintainList;
      serviceList = newServiceList;
      return;
    }

    Map<String, Query> queries = newQueryList;
    List<String> queryProblems = findQueryPermissionProblems(queries, newCatalog);
    if (!queryProblems.isEmpty()) {
      log.error("Invalid permissions in the queries: keeping previous definitions. {}", queryProblems);
      queries = queryList;
    }
    Map<String, Target> maintains = newMaintainList;
    List<String> maintainProblems = findMaintainPermissionProblems(maintains, newCatalog);
    if (!maintainProblems.isEmpty()) {
      log.error("Invalid permissions in the maintain targets: keeping previous definitions. {}", maintainProblems);
      maintains = maintainList;
    }
    Map<String, Service> services = newServiceList;
    List<String> serviceProblems = findServicePermissionProblems(services, newCatalog);
    if (!serviceProblems.isEmpty()) {
      log.error("Invalid permissions in the services: keeping previous definitions. {}", serviceProblems);
      services = serviceList;
    }
    List<String> catalogProblems = findPermissionProblems(queries, maintains, services, newCatalog);
    catalogProblems.addAll(findScreenPermissionProblems(screenMap, newCatalog));
    catalogProblems.addAll(findProfilePermissionProblems(profileList, newCatalog));
    if (catalogProblems.isEmpty()) {
      permissionCatalog.set(newCatalog);
    } else {
      log.error(KEEPING_PREVIOUS_CATALOG, catalogProblems);
      PermissionCatalog currentCatalog = permissionCatalog.get();
      if (!findQueryPermissionProblems(queries, currentCatalog).isEmpty()) {
        log.error("The queries need the rejected permission catalog: keeping previous definitions");
        queries = queryList;
      }
      if (!findMaintainPermissionProblems(maintains, currentCatalog).isEmpty()) {
        log.error("The maintain targets need the rejected permission catalog: keeping previous definitions");
        maintains = maintainList;
      }
      if (!findServicePermissionProblems(services, currentCatalog).isEmpty()) {
        log.error("The services need the rejected permission catalog: keeping previous definitions");
        services = serviceList;
      }
    }
    queryList = queries;
    maintainList = maintains;
    serviceList = services;
  }

  /**
   * Check the {@code permission} attribute of the queries, the maintain targets and the services against a catalog
   *
   * @param queries   Query map
   * @param maintains Maintain map
   * @param services  Service map
   * @param catalog   Permission catalog
   * @return Problems found, naming the definition and the permission (empty when every attribute is valid)
   */
  private static List<String> findPermissionProblems(Map<String, Query> queries, Map<String, Target> maintains,
                                                     Map<String, Service> services, PermissionCatalog catalog) {
    List<String> problems = new ArrayList<>(findQueryPermissionProblems(queries, catalog));
    problems.addAll(findMaintainPermissionProblems(maintains, catalog));
    problems.addAll(findServicePermissionProblems(services, catalog));
    return problems;
  }

  private static List<String> findQueryPermissionProblems(Map<String, Query> queries, PermissionCatalog catalog) {
    return queries == null ? List.of() : queries.values().stream()
      .flatMap(query -> catalog.validatePermissionAttribute(query.getPermission(), query.isPublic()).stream()
        .map(problem -> "query '" + query.getId() + PERMISSION_DETAIL + query.getPermission() + "'): " + problem))
      .sorted()
      .toList();
  }

  private static List<String> findMaintainPermissionProblems(Map<String, Target> maintains, PermissionCatalog catalog) {
    return maintains == null ? List.of() : maintains.values().stream()
      .flatMap(target -> catalog.validatePermissionAttribute(target.getPermission(), target.isPublic()).stream()
        .map(problem -> "maintain '" + target.getName() + PERMISSION_DETAIL + target.getPermission() + "'): "
          + problem))
      .sorted()
      .toList();
  }

  /**
   * Check the {@code permission} attribute of the services. A service authorized by its target is never checked
   * itself, so it cannot declare a permission
   */
  private static List<String> findServicePermissionProblems(Map<String, Service> services, PermissionCatalog catalog) {
    return services == null ? List.of() : services.values().stream()
      .flatMap(service -> getServicePermissionProblems(service, catalog).stream()
        .map(problem -> "service '" + service.getId() + PERMISSION_DETAIL + service.getPermission() + "'): "
          + problem))
      .sorted()
      .toList();
  }

  /**
   * Check the {@code permission} attribute of the buttons, context buttons and columns of the screens, and the
   * {@code edit-permission} attribute of their button actions
   */
  private static List<String> findScreenPermissionProblems(Map<String, Screen> screens, PermissionCatalog catalog) {
    if (screens == null) {
      return List.of();
    }
    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, Screen> screen : screens.entrySet()) {
      for (Element element : screen.getValue().getElementsByType(Element.class)) {
        String attribute = switch (element) {
          case AbstractButton button -> button.getPermission();
          case Column column -> column.getPermission();
          case ButtonAction action -> action.getEditPermission();
          default -> null;
        };
        catalog.validatePermissionAttribute(attribute, false).forEach(problem -> problems.add("screen '"
          + screen.getKey() + "' (" + describe(element) + " permission '" + attribute + "'): " + problem));
      }
    }
    return problems.stream().sorted().toList();
  }

  /**
   * Check the permissions and the exclusions of the profile files: catalog identifiers or wildcards that match some
   * permission
   */
  private static List<String> findProfilePermissionProblems(Map<String, Profile> profiles, PermissionCatalog catalog) {
    if (profiles == null) {
      return List.of();
    }
    List<String> problems = new ArrayList<>();
    profiles.forEach((name, profile) -> {
      profile.getPermissionIds().forEach(entry -> catalog.validateAssignment(entry).forEach(problem ->
        problems.add("profile '" + name + PERMISSION_DETAIL + entry + "'): " + problem)));
      profile.getExclusionIds().forEach(entry -> catalog.validateAssignment(entry).forEach(problem ->
        problems.add("profile '" + name + "' (exclude '" + entry + "'): " + problem)));
    });
    return problems.stream().sorted().toList();
  }

  private static String describe(Element element) {
    return switch (element) {
      case ButtonAction action -> "button action of target '" + action.getTarget() + "', edit";
      case Column column -> "column '" + column.getName() + "',";
      default -> "element '" + element.getElementKey() + "',";
    };
  }

  private static List<String> getServicePermissionProblems(Service service, PermissionCatalog catalog) {
    if (service.isAuthorizedByTarget() && service.getPermission() != null) {
      return List.of("a service authorized by its target cannot declare a permission");
    }
    return catalog.validatePermissionAttribute(service.getPermission(), false);
  }

  /**
   * Combine the Permissions.xml files of the modules into the permission catalog. An invalid catalog fails the
   * startup; on a reload it keeps the previous catalog, as an invalid XML file does
   *
   * @param result Permissions files, in module order
   * @return Permission catalog to swap in
   */
  private PermissionCatalog buildPermissionCatalog(Future<List<Permissions>> result) {
    List<Permissions> files = List.of();
    try {
      files = result.get();
    } catch (InterruptedException exc) {
      log.error(" ===== ERROR loading permission files  ===== ", exc);
      Thread.currentThread().interrupt();
    } catch (ExecutionException exc) {
      log.error(" ===== ERROR loading permission files  ===== ", exc);
    }

    PermissionCatalog currentCatalog = permissionCatalog.get();
    try {
      return PermissionCatalog.build(files);
    } catch (AWERuntimeException exc) {
      if (currentCatalog == null) {
        throw exc;
      }
      log.error(KEEPING_PREVIOUS_CATALOG, exc.getMessage());
      return currentCatalog;
    }
  }

  /**
   * Keep the previous map when a rebuild produced an empty map while the current one has
   * definitions: a completely empty rebuild means the reload failed, and replacing the live
   * definitions would leave the application unusable
   *
   * @param newMap     Newly built map
   * @param currentMap Currently active map
   * @param fileType   File type (for logging)
   * @return Map to swap in
   */
  private <T> Map<String, T> keepPreviousOnEmptyRebuild(Map<String, T> newMap, Map<String, T> currentMap, String fileType) {
    if (newMap.isEmpty() && currentMap != null && !currentMap.isEmpty()) {
      log.error("XML reload produced no {} definitions: keeping previous definitions", fileType);
      return currentMap;
    }
    return newMap;
  }

  /**
   * Keep the previous locale maps when a rebuild produced no locale at all for any language
   * while the current maps have definitions
   *
   * @param newLocales     Newly built locale maps per language
   * @param currentLocales Currently active locale maps per language
   * @return Locale maps to swap in
   */
  private Map<String, Map<String, String>> keepPreviousLocalesOnEmptyRebuild(Map<String, Map<String, String>> newLocales,
                                                                             Map<String, Map<String, String>> currentLocales) {
    boolean newEmpty = newLocales.values().stream().allMatch(Map::isEmpty);
    boolean currentEmpty = currentLocales == null || currentLocales.values().stream().allMatch(Map::isEmpty);
    if (newEmpty && !currentEmpty) {
      log.error("XML reload produced no locale definitions: keeping previous definitions");
      return currentLocales;
    }
    return newLocales;
  }

  /**
   * Wait for a future termination
   *
   * @param result Future result
   */
  private void waitForFuture(Future<String> result) {
    try {
      result.get();
    } catch (InterruptedException exc) {
      log.error(" ===== ERROR loading XML initialization files  ===== ", exc);
      Thread.currentThread().interrupt();
    } catch (ExecutionException exc) {
      // Never interrupt the calling thread on a load failure: reloads must keep it usable
      log.error(" ===== ERROR loading XML initialization files  ===== ", exc);
    }
  }

  /**
   * Wait executor for termination
   *
   * @param result   future results
   * @param fileType File type
   */
  protected <T> Map<String, T> waitForMap(Future<Map<String, T>> result, String fileType) {
    try {
      return result.get();
    } catch (InterruptedException exc) {
      log.error(" ===== ERROR loading {} files  ===== ", fileType, exc);
      Thread.currentThread().interrupt();
    } catch (ExecutionException exc) {
      // Never interrupt the calling thread on a load failure: reloads must keep it usable
      log.error(" ===== ERROR loading {} files  ===== ", fileType, exc);
    }
    return Collections.emptyMap();
  }

  /**
   * Merge per-module results into a new map keeping module precedence (first module wins)
   *
   * @param results  Per-module future results, in module order
   * @param fileType File type (for logging)
   * @return Merged map
   */
  private <T> Map<String, T> mergeModuleResults(List<Future<Map<String, T>>> results, String fileType) {
    Map<String, T> merged = new ConcurrentHashMap<>();
    results.stream()
      .map(result -> waitForMap(result, fileType))
      .forEach(map -> map.forEach(merged::putIfAbsent));
    return merged;
  }

  /**
   * Merge per-module locale results into a new map keeping module precedence per language
   *
   * @param results Per-module locale future results, in module order
   * @return Merged locale map
   */
  private Map<String, Map<String, String>> mergeLocaleResults(List<Map<String, Future<Map<String, String>>>> results) {
    Map<String, Map<String, String>> merged = new ConcurrentHashMap<>();
    results
      .forEach(result -> baseConfigProperties.getLanguageList()
        .forEach(language -> {
          Map<String, String> mergedLocales = Optional.ofNullable(merged.get(language)).orElse(new ConcurrentHashMap<>());
          waitForMap(result.get(language), "locale").forEach(mergedLocales::putIfAbsent);
          merged.put(language, mergedLocales);
        }));
    return merged;
  }

  /**
   * Launch the read of all global, screen and locale XML files into new maps
   *
   * @return Initialization data with the new maps and the futures that fill them
   */
  private XMLInitData launchGlobalScreenAndLocaleFiles() {
    XMLInitData initData = new XMLInitData();

    // Init global files
    initData.getGeneral().add(launchEnumerated(initData));
    initData.getGeneral().add(launchQueries(initData));
    initData.getGeneral().add(launchQueues(initData));
    initData.getGeneral().add(launchMaintains(initData));
    initData.getGeneral().add(launchEmails(initData));
    initData.getGeneral().add(launchServices(initData));
    initData.getGeneral().add(launchActions(initData));

    // Init permissions
    initData.setPermissionResults(launchPermissions());

    // Init profiles
    initData.setProfileResults(launchProfiles());

    // Init screens
    initData.setScreenResults(launchScreens());

    // For each language read local files
    initData.setLocaleResults(launchLocales());
    return initData;
  }

  /**
   * Compose a global file path
   *
   * @param fileName File name
   * @return Global file path
   */
  private String getGlobalFilePath(String fileName) {
    return baseConfigProperties.getPaths().getGlobal() + fileName + baseConfigProperties.getExtensionXml();
  }

  /**
   * Launch the read of the enumerated files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchEnumerated(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Enumerated.class, initData.getEnumerated(), getGlobalFilePath(baseConfigProperties.getFiles().getEnumerated()));
  }

  /**
   * Launch the read of the query files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchQueries(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Queries.class, initData.getQueries(), getGlobalFilePath(baseConfigProperties.getFiles().getQuery()));
  }

  /**
   * Launch the read of the queue files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchQueues(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Queues.class, initData.getQueues(), getGlobalFilePath(baseConfigProperties.getFiles().getQueue()));
  }

  /**
   * Launch the read of the maintain files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchMaintains(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Maintain.class, initData.getMaintains(), getGlobalFilePath(baseConfigProperties.getFiles().getMaintain()));
  }

  /**
   * Launch the read of the email files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchEmails(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Emails.class, initData.getEmails(), getGlobalFilePath(baseConfigProperties.getFiles().getEmail()));
  }

  /**
   * Launch the read of the service files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchServices(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Services.class, initData.getServices(), getGlobalFilePath(baseConfigProperties.getFiles().getServices()));
  }

  /**
   * Launch the read of the action files
   *
   * @param initData Initialization data
   * @return Read future
   */
  private Future<String> launchActions(XMLInitData initData) {
    return elementsDao.readXmlFilesAsync(Actions.class, initData.getActions(), getGlobalFilePath(baseConfigProperties.getFiles().getActions()));
  }

  /**
   * Launch the read of the permission files of every module
   *
   * @return Read future
   */
  private Future<List<Permissions>> launchPermissions() {
    String filePath = getGlobalFilePath(baseConfigProperties.getFiles().getPermissions());
    return elementsDao.readXmlFileListAsync(Permissions.class, filePath);
  }

  /**
   * Launch the read of the profile folders per module
   *
   * @return Per-module read futures, in module order
   */
  private List<Future<Map<String, Profile>>> launchProfiles() {
    return Arrays.stream(baseConfigProperties.getModuleList())
      .sequential()
      .map(module -> elementsDao.readModuleFolderXmlFile(Profile.class, baseConfigProperties.getPaths().getApplication() +
        module +
        baseConfigProperties.getPaths().getProfile()))
      .toList();
  }

  /**
   * Launch the read of the screen folders per module
   *
   * @return Per-module read futures, in module order
   */
  private List<Future<Map<String, Screen>>> launchScreens() {
    return Arrays.stream(baseConfigProperties.getModuleList())
      .sequential()
      .map(module -> elementsDao.readModuleFolderXmlFile(Screen.class, baseConfigProperties.getPaths().getApplication() +
        module +
        baseConfigProperties.getPaths().getScreen()))
      .toList();
  }

  /**
   * Launch the read of the locale files per module and language
   *
   * @return Per-module locale read futures, in module order
   */
  private List<Map<String, Future<Map<String, String>>>> launchLocales() {
    return Arrays.stream(baseConfigProperties.getModuleList())
      .sequential()
      .map(module -> baseConfigProperties.getLanguageList()
        .stream()
        .flatMap(language -> {
          Map<String, Future<Map<String, String>>> languageMap = new ConcurrentHashMap<>();
          languageMap.put(language, elementsDao.readLocaleModuleAsync(Path.of(baseConfigProperties.getPaths().getApplication(),
              module,
              baseConfigProperties.getPaths().getLocale() +
                baseConfigProperties.getFiles().getLocale() +
                language +
                baseConfigProperties.getExtensionXml())
            .toString()));
          return languageMap.entrySet().stream();
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
      .toList();
  }

  /**
   * Get an EnumeratedGroup with the identifier
   *
   * @param groupId Group identifier
   * @return The ENUMERATED group corresponding the groupId
   * @throws AWException Clone not supported
   */
  public EnumeratedGroup getEnumerated(String groupId) throws AWException {
    try {
      // Clone from list
      return enumeratedList.get(groupId);
    } catch (Exception exc) {
      throw new AWException("Enumerated group" + NOT_FOUND + groupId, exc);
    }
  }

  /**
   * Get a Query with the identifier
   *
   * @param queryId Query identifier
   * @return The query corresponding the queryId
   * @throws AWException Clone not supported
   */
  public Query getQuery(String queryId) throws AWException {
    try {
      // Clone from list
      return queryList.get(queryId);
    } catch (Exception exc) {
      throw new AWException("Query" + NOT_FOUND + queryId, exc);
    }
  }

  /**
   * Get a Queue with the identifier
   *
   * @param queueId Queue id
   * @return The query corresponding the queryId
   * @throws AWException Clone not supported
   */
  public Queue getQueue(String queueId) throws AWException {
    try {
      // Clone from list
      return queueList.get(queueId);
    } catch (Exception exc) {
      throw new AWException("Queue" + NOT_FOUND + queueId, exc);
    }
  }

  /**
   * Get a MAINTAIN operation with the identifier
   *
   * @param maintainId Maintain operation identifier
   * @return The MAINTAIN operation corresponding the maintainId
   * @throws AWException Clone not supported
   */
  public Target getMaintain(String maintainId) throws AWException {
    try {
      // Clone from list
      return maintainList.get(maintainId);
    } catch (Exception exc) {
      throw new AWException("Maintain" + NOT_FOUND + maintainId, exc);
    }
  }

  /**
   * Get an email operation with the identifier
   *
   * @param emailId Email operation identifier
   * @return The email operation corresponding the mntId
   * @throws AWException Clone not supported
   */
  public Email getEmail(String emailId) throws AWException {
    try {
      // Clone from list
      return emailList.get(emailId);
    } catch (Exception exc) {
      throw new AWException("Email" + NOT_FOUND + emailId, exc);
    }
  }

  /**
   * Get a service with the identifier
   *
   * @param serviceId Service identifier
   * @return The service corresponding the serviceId
   * @throws AWException Clone not supported
   */
  public Service getService(String serviceId) throws AWException {
    try {
      // Clone from list
      return serviceList.get(serviceId);
    } catch (Exception exc) {
      throw new AWException("Service" + NOT_FOUND + serviceId, exc);
    }
  }

  /**
   * Get an action with the identifier
   *
   * @param actionId Action identifier
   * @return The action corresponding the actionId
   * @throws AWException Clone not supported
   */
  public Action getAction(String actionId) throws AWException {
    try {
      // Clone from list
      return actionList.get(actionId);
    } catch (Exception exc) {
      throw new AWException("Action" + NOT_FOUND + actionId, exc);
    }
  }

  /**
   * Get a screen with the identifier
   *
   * @param screenId Screen identifier
   * @return The screen corresponding the screenId
   * @throws AWException Clone not supported
   */
  public Screen getScreen(String screenId) throws AWException {
    Screen screen;
    // Capture the current map reference: a concurrent reload may swap the field
    Map<String, Screen> currentScreenMap = screenMap;
    if (currentScreenMap.containsKey(screenId) && currentScreenMap.get(screenId).getId() != null) {
      screen = currentScreenMap.get(screenId);
    } else {
      // Get Action
      screen = readScreen(screenId, new HashSet<>());

      // Set screen identifier
      screen.setId(screenId);

      // Store screen
      setScreen(screen);
    }

    // Retrieve screen
    return screen;
  }

  /**
   * Inserts or updates given screen
   *
   * @param screen Screen
   * @return Screen
   */
  public Screen setScreen(@NonNull Screen screen) {
    // Store screen
    screenMap.put(screen.getId(), screen);

    // Retrieve screen
    return screen;
  }

  /**
   * Retrieve a screen and store identifiers
   *
   * @param screenId        Screen identifier
   * @param includedScreens Included screens
   * @return Screen retrieved
   * @throws AWException Clone not supported
   */
  private Screen readScreen(String screenId, Set<String> includedScreens) throws AWException {
    Screen screen;
    int identifier = 1;
    String path = baseConfigProperties.getPaths().getScreen();
    String file = screenId + baseConfigProperties.getExtensionXml();

    // Clone from list (capture the current map reference: a concurrent reload may swap the field)
    Map<String, Screen> currentScreenMap = screenMap;
    if (currentScreenMap.containsKey(screenId)) {
      screen = currentScreenMap.get(screenId);
    } else {
      screen = elementsDao.readXmlFile(Screen.class, path, file);
    }

    if (screen == null) {
      throw new AWENotFoundException(getLocale("ERROR_TITLE_SCREEN_NOT_DEFINED"), getLocale("ERROR_MESSAGE_SCREEN_NOT_DEFINED", screenId));
    }

    // Clone screen
    screen = screen.copy();

    // Set screen identifier
    List<Element> elements = withRowDetailElements(screen.getElementsByType(Element.class));
    for (Element element : elements) {

      // Generate component key if not defined
      if (element instanceof Component component && NO_KEY.equalsIgnoreCase(element.getElementKey())
              && !NO_TAG.equalsIgnoreCase(component.getComponentTag())) {
        element.setId(screenId + "-" + component.getComponentTag() + "-" + (identifier++));
      }

      // If component is an included, retrieve included screen and add it
      if (element instanceof Include include) {
        includeScreen(screenId, include, new HashSet<>(includedScreens));
      }
    }

    // Get Action
    return screen;
  }

  /**
   * Add to a list of elements the ones inside their row details, which the searches by type do not return
   *
   * @param elements Elements
   * @return Elements and the elements of their row details
   */
  private List<Element> withRowDetailElements(List<Element> elements) {
    List<Element> allElements = new ArrayList<>();
    for (Element element : elements) {
      allElements.add(element);
      if (element instanceof RowDetail rowDetail) {
        allElements.addAll(withRowDetailElements(rowDetail.getDetailElements()));
      }
    }
    return allElements;
  }

  /**
   * Include a nested screen
   *
   * @param screenId        Screen id
   * @param include         Include tag
   * @param includedScreens Included screens
   * @throws AWException Error retrieving screen or tag
   */
  private void includeScreen(String screenId, Include include, Set<String> includedScreens) throws AWException {
    if (include.getTargetScreen() != null && include.getTargetSource() != null) {
      String includeKey = include.getTargetScreen() + "-" + include.getTargetSource();
      if (!includedScreens.contains(includeKey)) {
        // Add the key to the included screens
        includedScreens.add(includeKey);

        // Retrieve include screen
        Screen includeScreen = readScreen(include.getTargetScreen(), new HashSet<>(includedScreens));

        // Retrieve include screen source
        Tag source = getScreenSource(includeScreen, include.getTargetSource());

        // Store tag element list in include
        if (source != null) {
          include.setElementList(source.getElementList());
        } else {
          throw new AWException(getLocale("ERROR_TITLE_BAD_INCLUDE_DEFINITION"),
            getLocale("ERROR_MESSAGE_BAD_INCLUDE_DEFINITION", screenId, include.getTargetScreen(), include.getTargetSource()));
        }
      } else {
        throw new AWException(getLocale("ERROR_TITLE_NESTED_INCLUDE"),
          getLocale("ERROR_MESSAGE_NESTED_INCLUDE", screenId, include.getTargetScreen(), include.getTargetSource()));
      }
    } else {
      throw new AWException(getLocale("ERROR_TITLE_BAD_INCLUDE_DEFINITION"),
        getLocale("ERROR_MESSAGE_BAD_INCLUDE_DEFINITION", screenId, include.getTargetScreen(), include.getTargetSource()));
    }
  }

  /**
   * Retrieve screen source tag
   *
   * @param screen Screen
   * @param source Source
   * @return Tag
   */
  private Tag getScreenSource(Screen screen, String source) {
    for (Tag child : screen.getChildrenByType(Tag.class)) {
      if (source.equalsIgnoreCase(child.getSource())) {
        return child.copy();
      }
    }
    return null;
  }

  /**
   * Get menu object
   *
   * @param menuId Menu name
   * @return Menu object
   * @throws AWException Clone not supported
   */
  public Menu getMenu(String menuId) throws AWException {
    try {
      // Clone from list
      return menuList.get(menuId);
    } catch (Exception exc) {
      throw new AWException("Menu" + NOT_FOUND + menuId, exc);
    }
  }

  /**
   * Inserts or updates given menu
   *
   * @param menuId menu id
   * @param menu   Menu
   * @return Menu
   */
  public Menu setMenu(String menuId, Menu menu) {
    // Store menu in list
    menuList.put(menuId, menu);

    // Retrieve added menu
    return menu;
  }

  /**
   * Get menu object
   *
   * @param menuId Menu name
   * @return Menu object
   * @throws AWException Clone not supported
   */
  private Menu readMenuFile(String menuId) throws AWException {
    Menu menu;
    String path = baseConfigProperties.getPaths().getMenu();
    String fileName = menuId + baseConfigProperties.getExtensionXml();
    try {
      // Clone from list
      menu = elementsDao.readXmlFile(Menu.class, path, fileName);
      if (menu != null) {
        // Set menu identifier
        menu = (Menu) menu.copy().setId(menuId);
        // Set menu layout type
        if (StringUtils.isEmpty(menu.getScreen())) {
          menu.setScreen(baseConfigProperties.getMenuType().getScreen());
        }
      }
    } catch (Exception exc) {
      throw new AWException("Menu" + NOT_FOUND + menuId, exc);
    }

    // Get Action
    return menu;
  }

  /**
   * Get profile object
   *
   * @param profile Profile name
   * @return Profile object
   * @throws AWException Clone not supported
   */
  public Profile getProfile(String profile) throws AWException {
    try {
      // Clone from list
      return profileList.get(profile);
    } catch (Exception exc) {
      throw new AWException("Profile" + NOT_FOUND + profile, exc);
    }
  }

  /**
   * Get available profile list
   *
   * @return Profile object
   */
  public Set<String> getProfileList() {
    return profileList.keySet();
  }

  /**
   * Get available screen list
   *
   * @return Screen list
   */
  public Set<String> getScreenList() {
    return screenMap.keySet();
  }

  /**
   * Returns all locales. Do not cache the returned reference across reloads: a hot reload
   * swaps the whole map object, so cached references would keep serving stale definitions
   *
   * @return locales
   */
  public Map<String, Map<String, String>> getLocales() {
    return localeList;
  }

  /**
   * Retrieve language
   *
   * @return Language
   */
  public String getLanguage() {
    String language;
    try {
      AweSession session = context.getBean(AweSession.class);
      language = session.getParameter(String.class, AweConstants.SESSION_LANGUAGE);
      language = language == null ? baseConfigProperties.getLanguageDefault() : language;
    } catch (Exception exc) {
      language = baseConfigProperties.getLanguageDefault();
    }
    return language;
  }

  /**
   * Returns a locale based on its identifier
   *
   * @param localeIdentifier Local identifier
   * @return Selected locale
   */
  public String getLocale(String localeIdentifier) {
    return getLocaleWithLanguage(localeIdentifier, getLanguage());
  }

  /**
   * Returns a locale based on its identifier
   *
   * @param localeIdentifier Local identifier
   * @return Selected locale
   */
  public String getLocale(String localeIdentifier, Object... tokenList) {
    return getLocaleWithLanguage(localeIdentifier, getLanguage(), tokenList);
  }

  /**
   * Returns a locale based on its identifier
   *
   * @param localeIdentifier Local identifier
   * @return Selected locale
   */
  public String getLocaleWithLanguage(String localeIdentifier, String language) {
    String locale = localeIdentifier;
    Map<String, String> locales = language == null ? null : localeList.get(language);

    // A language that is not configured (e.g. the language of a user) falls back to the default language
    if (locales == null) {
      locales = localeList.get(baseConfigProperties.getLanguageDefault());
    }

    // Check if locale exists, and retrieve it
    if (localeIdentifier != null && locales != null && locales.containsKey(localeIdentifier)) {
      locale = locales.get(localeIdentifier);
    }

    // Get Action
    return locale;
  }

  /**
   * Returns a locale based on its identifier replacing a set of tokens by a string array
   *
   * @param localeIdentifier Local identifier
   * @param tokenList        Token list to replace
   * @return Selected locale
   */
  public String getLocaleWithLanguage(String localeIdentifier, String language, Object... tokenList) {
    String locale = getLocaleWithLanguage(localeIdentifier, language);
    int index = 0;

    // Escape HTML
    for (Object token : tokenList) {
      if (token instanceof String stringToken) {
        tokenList[index++] = StringEscapeUtils.escapeHtml4(stringToken);
      }
    }

    return MessageFormat.format(locale, tokenList);
  }

  /**
   * Retrieve a property value
   *
   * @param propertyIdentifier Property identifier
   * @return Selected locale
   */
  public String getProperty(String propertyIdentifier) {
    return environment.getProperty(propertyIdentifier);
  }

  /**
   * Retrieve a property value reading as a class
   *
   * @param propertyIdentifier propertyIdentifier Property identifier
   * @param tClass             Class to read the property value
   * @param <T>                property class
   * @return Selected locale
   */
  public <T> T getProperty(String propertyIdentifier, Class<T> tClass) {
    return environment.getProperty(propertyIdentifier, tClass);
  }

  /**
   * Retrieve a property value
   *
   * @param propertyIdentifier Property identifier
   * @param defaultValue       Default value
   * @return Selected locale
   */
  public String getProperty(String propertyIdentifier, String defaultValue) {
    String propertyValue = environment.getProperty(propertyIdentifier);
    if (propertyValue == null) {
      propertyValue = defaultValue;
    }
    return propertyValue;
  }

  /**
   * Get phase SERVICES
   *
   * @param phase Phase to look into
   * @return Start Service list
   * @throws AWException Error starting service
   */
  public List<Service> getPhaseServices(LaunchPhaseType phase) throws AWException {

    // Variable definition
    List<Service> phaseServices = new ArrayList<>();

    for (Service service : serviceList.values()) {
      if (phase.toString().equalsIgnoreCase(service.getLaunchPhase())) {
        phaseServices.add(service.copy());
      }
    }

    // Return object
    return phaseServices;
  }

  /**
   * Retrieve the permission catalog
   *
   * @return Permission catalog
   */
  public PermissionCatalog getPermissionCatalog() {
    PermissionCatalog catalog = permissionCatalog.get();
    return catalog == null ? PermissionCatalog.empty() : catalog;
  }

  /**
   * Retrieve query map (query name + query object)
   *
   * @return Query map
   */
  public Map<String, Query> getQueryMap() {
    return new TreeMap<>(queryList);
  }

  /**
   * Retrieve maintain map (maintain name + maintain object)
   *
   * @return Maintain map
   */
  public Map<String, Target> getMaintainMap() {
    return new TreeMap<>(maintainList);
  }

  /**
   * Retrieve the application context
   *
   * @return Application context
   */
  public ApplicationContext getApplicationContext() {
    return context;
  }
}
