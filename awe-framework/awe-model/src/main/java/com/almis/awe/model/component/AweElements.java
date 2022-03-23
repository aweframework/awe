package com.almis.awe.model.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dao.AweElementsDao;
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
import com.almis.awe.model.entities.queries.Queries;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.queues.Queue;
import com.almis.awe.model.entities.queues.Queues;
import com.almis.awe.model.entities.screen.Include;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.model.entities.screen.component.Component;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.model.entities.services.Services;
import com.almis.awe.model.type.LaunchPhaseType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.almis.awe.model.constant.AweConstants.*;

/**
 * The type Awe elements.
 *
 * @author pgarcia
 */
@Slf4j
public class AweElements {

  // Autowired services
  private final WebApplicationContext context;
  private final AweElementsDao elementsDao;
  private final Environment environment;
  private final BaseConfigProperties baseConfigProperties;

  // Elements
  private Map<String, EnumeratedGroup> enumeratedList;
  private Map<String, Query> queryList;
  private Map<String, Queue> queueList;
  private Map<String, Target> maintainList;
  private Map<String, Email> emailList;
  private Map<String, Service> serviceList;
  private Map<String, Action> actionList;
  private Map<String, Profile> profileList;
  private Map<String, Menu> menuList;
  private Map<String, Screen> screenMap;

  // Locale list
  private Map<String, Map<String, String>> localeList;

  private static final String NOT_FOUND = " not found: ";

  /**
   * Autowired constructor
   *
   * @param context Application context
   * @param baseConfigProperties base configuration properties
   * @param elementsDao Element DAO
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
    log.info( "----------------------------- AWE starting ... -----------------------------------");
    log.info(LOG_LINE);

    // Initialize Awe Elements (Read all XML sources)
    log.info("=============================");
    log.info("===== Reading XML Files =====");
    log.info("=============================");

    // Initialize global files
    log.info(" ===== Initializing global and screen files ===== ");
    waitForTermination(initGlobalFiles(), "global");
    log.info(" ===== Finished loading global and screen files  ===== ");

    // Initialize menu files
    log.info(" ===== Initializing menu files ===== ");
    initMenuFiles();
    log.info(" ===== Finished loading menu files  ===== ");

    // Initialize locale files
    log.info(" ===== Initializing locale files ===== ");
    waitForTermination(initLocaleFiles(), "locale");
    log.info(" ===== Finished loading locale files  ===== ");
  }

  /**
   * Wait executor for termination
   *
   * @param results future results
   */
  protected void waitForTermination(List<Future<String>> results, String fileType) {
    for (Future<String> result : results) {
      try {
        result.get();
      } catch (InterruptedException | ExecutionException exc) {
        log.error(" ===== ERROR loading {} Files  ===== ", fileType, exc);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Read all XML files and store them in the component
   */
  private List<Future<String>> initGlobalFiles() {
    List<Future<String>> results = new ArrayList<>();

    // Init enumerated
    enumeratedList = new ConcurrentHashMap<>();
    String path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getEnumerated() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Enumerated.class, enumeratedList, path));

    // Init queries
    queryList = new ConcurrentHashMap<>();
    path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getQuery() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Queries.class, queryList, path));

    // Init queues
    queueList = new ConcurrentHashMap<>();
    path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getQueue() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Queues.class, queueList, path));

    // Init maintains
    maintainList = new ConcurrentHashMap<>();
    path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getMaintain() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Maintain.class, maintainList, path));

    // Init emails
    emailList = new ConcurrentHashMap<>();
    path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getEmail() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Emails.class, emailList, path));

    // Init service
    serviceList = new ConcurrentHashMap<>();
    path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getServices() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Services.class, serviceList, path));

    // Init actions
    actionList = new ConcurrentHashMap<>();
    path = baseConfigProperties.getPaths().getGlobal() + baseConfigProperties.getFiles().getActions() + baseConfigProperties.getExtensionXml();
    results.add(elementsDao.readXmlFilesAsync(Actions.class, actionList, path));

    // Init profiles
    profileList = new ConcurrentHashMap<>();
    results.add(elementsDao.readFolderXmlFilesAsync(Profile.class, baseConfigProperties.getPaths().getProfile(), profileList));

    // Init screens
    screenMap = new ConcurrentHashMap<>();
    if (baseConfigProperties.isPreloadScreens()) {
      results.add(elementsDao.readFolderXmlFilesAsync(Screen.class, baseConfigProperties.getPaths().getScreen(), screenMap));
    }

    return results;
  }

  /**
   * Read all XML files and store them in the component
   */
  private void initMenuFiles() {
    // Init menus in cache
    try {
      menuList = new ConcurrentHashMap<>();
      menuList.put(baseConfigProperties.getFiles().getMenuPublic(), readMenuFile(baseConfigProperties.getFiles().getMenuPublic()));
      menuList.put(baseConfigProperties.getFiles().getMenuPrivate(), readMenuFile(baseConfigProperties.getFiles().getMenuPrivate()));
    } catch (AWException exc) {
      log.error("Error initializing menus", exc);
      exc.log();
    }
  }

  /**
   * Read all XML files and store them in the component
   */
  private List<Future<String>> initLocaleFiles() {
    List<Future<String>> results = new ArrayList<>();

    // Initialize locales
    localeList = new ConcurrentHashMap<>();
    // For each language read local files
    for (String language : baseConfigProperties.getLanguageList()) {
      // Get file name
      String fileName = baseConfigProperties.getFiles().getLocale() + language.toUpperCase();
      String path = baseConfigProperties.getPaths().getLocale() + fileName + baseConfigProperties.getExtensionXml();

      // Read locale file for language
      results.add(elementsDao.readLocaleAsync(path, language, localeList));
    }

    return results;
  }

  /**
   * Get an EnumeratedGroup with the identifier
   *
   * @param groupId Group identifier
   * @return The ENUMERATED group corresponding the groupId
   * @throws AWException Clone not supported
   */
  @Cacheable(value = "enumerated", key = "#p0")
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
  @Cacheable(value = "query", key = "#p0")
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
  @Cacheable(value = "queue", key = "#p0")
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
  @Cacheable(value = "maintain", key = "#p0")
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
  @Cacheable(value = "email", key = "#p0")
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
  @Cacheable(value = "service", key = "#p0")
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
  @Cacheable(value = "action", key = "#p0")
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
  @Cacheable(value = "screen", key = "#p0")
  public Screen getScreen(String screenId) throws AWException {
    Screen screen;
    if (screenMap.containsKey(screenId) && screenMap.get(screenId).getId() != null) {
      screen = screenMap.get(screenId);
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
  @CachePut(value = "screen", key = "#p0.getId()")
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
    String path = baseConfigProperties.getPaths().getScreen() + screenId + baseConfigProperties.getExtensionXml();

    // Clone from list
    if (screenMap.containsKey(screenId)) {
      screen = screenMap.get(screenId);
    } else {
      screen = elementsDao.readXmlFile(Screen.class, path);
    }

    if (screen == null) {
      throw new AWException("Screen" + NOT_FOUND + screenId);
    }

    // Clone screen
    screen = screen.copy();

    // Set screen identifier
    List<Element> elements = screen.getElementsByType(Element.class);
    for (Element element : elements) {

      // Generate component key if not defined
      if (element instanceof Component && NO_KEY.equalsIgnoreCase(element.getElementKey())) {
        Component component = (Component) element;
        if (!NO_TAG.equalsIgnoreCase(component.getComponentTag())) {
          element.setId(screenId + "-" + component.getComponentTag() + "-" + (identifier++));
        }
      }

      // If component is an included, retrieve included screen and add it
      if (element instanceof Include) {
        includeScreen(screenId, (Include) element, new HashSet<>(includedScreens));
      }
    }

    // Get Action
    return screen;
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
   * @throws AWException Error retrieving screen source
   */
  private Tag getScreenSource(Screen screen, String source) throws AWException {
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
  @Cacheable(value = "menu", key = "#p0")
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
  @CachePut(value = "menu", key = "#p0")
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
    String path = baseConfigProperties.getPaths().getMenu() + menuId + baseConfigProperties.getExtensionXml();
    try {
      // Clone from list
      menu = elementsDao.readXmlFile(Menu.class, path);
      if (menu != null) {
        // Set menu identifier
        menu = (Menu) menu.copy().setId(menuId);
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
  @Cacheable(value = "profile", key = "#p0")
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
   * Returns all locales
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
  @Cacheable(value = "locale", key = "{ #p0, #p1 }")
  public String getLocaleWithLanguage(String localeIdentifier, String language) {
    String locale = localeIdentifier;
    Map<String, String> locales = localeList.get(language.toLowerCase());

    // Check if locale exists, and retrieve it
    if (localeIdentifier != null && locales.containsKey(localeIdentifier)) {
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
  @Cacheable(value = "locale", key = "{ #p0, #p1, #p2.toString() }")
  public String getLocaleWithLanguage(String localeIdentifier, String language, Object... tokenList) {
    String locale = getLocaleWithLanguage(localeIdentifier, language);
    int index = 0;

    // Escape HTML
    for (Object token : tokenList) {
      if (token instanceof String) {
        String stringToken = (String) token;
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

    // Search from application file to awe file
    for (Service service : serviceList.values()) {
      if (phase.toString().equalsIgnoreCase(service.getLaunchPhase())) {
        phaseServices.add(service.copy());
      }
    }

    // Return object
    return phaseServices;
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
