package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWESessionException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.builder.MenuScreenBuilder;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.*;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.Restriction;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.panelable.Panelable;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.type.LoadType;
import com.almis.awe.model.type.RestrictionType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.StringUtil;
import com.almis.awe.service.data.builder.DataListBuilder;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import com.almis.awe.service.screen.ScreenRestrictionGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.*;

/**
 * Manage application Menus
 */
@Slf4j
public class MenuService extends ServiceConfig {

  public static final String OPTION = "option";
  public static final String RESTRICTION = "restriction";
  public static final String ACCESS = "access";
  public static final String TEXT_SUCCESS = "text-success";
  public static final String PROFILE = "profile";
  private static final String ERROR_TITLE_SCREEN_NOT_DEFINED = "ERROR_TITLE_SCREEN_NOT_DEFINED";
  // Autowired services
  private final QueryService queryService;
  private final ScreenComponentGenerator screenComponentGenerator;
  private final InitialLoadDao initialLoadDao;
  private final BaseConfigProperties baseConfigProperties;
  private final SecurityConfigProperties securityConfigProperties;
  private final FavouriteService favouriteService;
  private final LauncherService launcherService;

  /**
   * Autowired constructor
   *
   * @param queryService               Query service
   * @param screenComponentGenerator   Screen component generator
   * @param initialLoadDao             Initial load service
   * @param baseConfigProperties       Base configuration properties
   * @param securityConfigProperties   Security configuration properties
   * @param favouriteService           Favourites service
   * @param launcherService            Service launcher service
   */
  public MenuService(QueryService queryService, ScreenComponentGenerator screenComponentGenerator,
                     InitialLoadDao initialLoadDao, BaseConfigProperties baseConfigProperties,
                     SecurityConfigProperties securityConfigProperties, FavouriteService favouriteService,
                     LauncherService launcherService) {
    this.queryService = queryService;
    this.screenComponentGenerator = screenComponentGenerator;
    this.initialLoadDao = initialLoadDao;
    this.baseConfigProperties = baseConfigProperties;
    this.securityConfigProperties = securityConfigProperties;
    this.favouriteService = favouriteService;
    this.launcherService = launcherService;
  }

  /**
   * Retrieve the menu for the user
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenu() throws AWException {
    String menuId = baseConfigProperties.getFiles().getMenuPublic();
    try {
      boolean isAuthenticated = getSession().isAuthenticated();
      // Check if user is logged in
      if (isAuthenticated) {
        menuId = baseConfigProperties.getFiles().getMenuPrivate();
      }
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_AUTHENTICATE"), getLocale("ERROR_MESSAGE_AUTHENTICATE"), exc);
    }

    // Get menu for user
    Menu menu = getMenu(menuId);

    // Add favourites (if exists)
    return addFavouritesToMenu(menu);
  }

  /**
   * Check if session has expired and user is trying to access a private option
   *
   * @return Session has expired
   * @throws AWException Menu has not been found
   */
  private boolean sessionExpired(String optionId) throws AWException {
    Menu menu = getMenu(baseConfigProperties.getFiles().getMenuPrivate());
    return !getSession().isAuthenticated() && menu.getOptionByName(optionId) != null;
  }

  /**
   * Retrieve the menu for the user
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions() throws AWException {
    return getMenuWithRestrictions(getMenu());
  }

  /**
   * Retrieve the menu for the user
   *
   * @param menuType Menu type
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions(String menuType) throws AWException {
    return getMenuWithRestrictions(getMenu(menuType));
  }

  /**
   * Retrieve the menu for the user
   *
   * @param menu Menu
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions(Menu menu) throws AWException {

    // Apply restrictions if logged in
    if (getSession().isAuthenticated()) {
      // Get restrictions
      ServiceData queryOutput = queryService.launchPrivateQuery(AweConstants.SCREEN_RESTRICTION_QUERY, "1", "0");
      List<ScreenRestriction> screenRestrictions = DataListUtil.asBeanList(queryOutput.getDataList(), ScreenRestriction.class);

      // Apply restrictions
      new ScreenRestrictionGenerator().applyScreenRestriction(screenRestrictions, menu);
    }

    return menu;
  }

  /**
   * Retrieve the menu with all restrictions (included modules)
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithAllRestrictions() throws AWException {
    return getMenuWithAllRestrictions(getMenu());
  }

  /**
   * Retrieve the menu with all restrictions (included modules)
   *
   * @param menuType Menu type
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithAllRestrictions(String menuType) throws AWException {
    return getMenuWithAllRestrictions(getMenu(menuType));
  }

  /**
   * Retrieve the menu with all restrictions (included modules)
   *
   * @param menu Menu
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithAllRestrictions(Menu menu) throws AWException {

    Menu restrictedMenu = menu;

    // Apply restrictions if logged in
    if (getSession().isAuthenticated()) {
      String module = Optional.ofNullable(getRequest().getParameter(SESSION_MODULE))
              .map(JsonNode::textValue)
              .orElse(getSession().getParameter(String.class, AweConstants.SESSION_MODULE));

      // Apply module restrictions
      new ScreenRestrictionGenerator().applyModuleRestriction(module, restrictedMenu);

      // Apply configuration restrictions
      restrictedMenu = getMenuWithRestrictions(restrictedMenu);
    }

    return restrictedMenu;
  }

  /**
   * Retrieve a menu
   *
   * @param menuId Menu identifier
   * @return Menu retrieved
   * @throws AWException Menu has not been found
   */
  public Menu getMenu(String menuId) throws AWException {
    // Retrieve menu
    Menu menu = getElements().getMenu(menuId).copy();
    menu.defineRelationship();
    return menu;
  }

  /**
   * Retrieve the menu default screen
   *
   * @return Default screen
   * @throws AWException Default screen has not been defined
   */
  public Screen getDefaultScreen() throws AWException {
    // Get screen from option
    Menu menu = getMenu();

    // Get default screen identifier
    String defaultScreenId = menu.getScreen();

    // Check if option has a screen
    if (defaultScreenId == null) {
      throw new AWException(getLocale(ERROR_TITLE_SCREEN_NOT_DEFINED), getLocale("ERROR_MESSAGE_MENU_HAS_NOT_DEFAULT_SCREEN"));
    }

    // Retrieve screen
    return getScreen(defaultScreenId);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getOptionScreen(String optionId) throws AWException {
    // Get current menu
    Menu menu = getMenu();

    if (optionId.equalsIgnoreCase(baseConfigProperties.getScreen().getHome())) {
      // Get home screen from menu
      return getScreen(menu.getScreen(), optionId);
    } else {
      // Find option
      Option option = getOptionByName(optionId);

      // Get screen identifier
      return getScreenByOption(option, menu);
    }
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getAvailableOptionScreen(String optionId) throws AWException {
    // Get current menu
    Menu menu = getMenu();

    if (optionId.equalsIgnoreCase(baseConfigProperties.getScreen().getHome())) {
      // Get home screen from menu
      return getScreen(menu.getScreen(), optionId);
    } else {
      // Find option
      Option option = getAvailableOptionByName(optionId);

      // Get screen identifier
      return getScreenByOption(option, menu);
    }
  }

  /**
   * Retrieve screen by option
   * @param option Option
   * @param menu Menu
   * @return Screen retrieved (or generated)
   * @throws AWException
   */
  private Screen getScreenByOption(Option option, Menu menu) throws AWException {
    if (option.isMenuScreen()) {
      // Menu screen
      return generateMenuScreen(option, menu);
    } else if (option.isDynamic()) {
      // Dynamic screen
      return (Screen) launcherService.callService(option.getDynamicScreenService(), null).getData();
    } else {
      // Static screen
      return getScreen(option.getScreen(), option.getId());
    }
  }

  /**
   * Retrieve an screen
   *
   * @param screenId Screen identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getScreen(String screenId) throws AWException {
    return getScreen(screenId, null);
  }

  /**
   * Retrieve an screen
   *
   * @param screenId Screen identifier
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getScreen(String screenId, String optionId) throws AWException {
    // Check if option has a screen
    if (screenId == null) {
      throw new AWENotFoundException(getLocale(ERROR_TITLE_SCREEN_NOT_DEFINED), getLocale("ERROR_MESSAGE_SCREEN_NOT_DEFINED", optionId));
    }

    // Get screen
    Screen screen = getElements().getScreen(screenId).copy();

    // Add screen data
    if (!screen.isInitialized()) {
      initializeScreen(screen);
    }

    // Retrieve screen
    return screen;
  }

  /**
   * Initialize a screen
   *
   * @param screen Screen
   * @throws AWException Screen has not been found
   */
  private void initializeScreen(Screen screen) throws AWException {
    // Find all panelables and retrieve enumerated information
    Map<Panelable, Future<ServiceData>> resultMap = getPanelableMap(screen);

    // Assign result to each panelable
    updatePanelableData(resultMap);

    // Set screen as initialized
    screen.setInitialized(true);
    getElements().setScreen(screen);
  }

  /**
   * Initialize a screen
   *
   * @param screen Screen
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  private Map<Panelable, Future<ServiceData>> getPanelableMap(Screen screen) throws AWException {
    // Find all panelables and retrieve enumerated information
    List<Panelable> panelableList = screen.getElementsByType(Panelable.class);
    Map<Panelable, Future<ServiceData>> resultMap = new HashMap<>();

    // Generate a thread for each initialization
    for (Panelable panelable : panelableList) {
      if (panelable.getInitialLoad() != null) {
        // Launch
        Future<ServiceData> taskResult = initialLoadDao.launchInitialLoad(new AweThreadInitialization()
          .setInitialLoadType(LoadType.valueOf(panelable.getInitialLoad().toUpperCase()))
          .setTarget(panelable.getTargetAction()));
        resultMap.put(panelable, taskResult);
      }
    }

    return resultMap;
  }

  /**
   * Update retrieved data into each panelable
   *
   * @param resultMap Result map
   */
  private void updatePanelableData(Map<Panelable, Future<ServiceData>> resultMap) {
    // Assign result to each panelable
    for (Map.Entry<Panelable, Future<ServiceData>> entry : resultMap.entrySet()) {
      Panelable panelable = entry.getKey();
      try {
        ServiceData result = entry.getValue().get();
        DataList componentData = (DataList) result.getVariableMap().get(AweConstants.ACTION_DATA).getObjectValue();
        Map<String, String> panelableData = new HashMap<>();
        // For each column, store value in components
        if (!componentData.getRows().isEmpty()) {
          for (Map<String, CellData> row : componentData.getRows()) {
            panelableData.put(row.get(AweConstants.JSON_VALUE_PARAMETER).getStringValue(), row.get(AweConstants.JSON_LABEL_PARAMETER).getStringValue());
          }
        }

        // Set data into panelable
        panelable.setTabValues(panelableData);
      } catch (Exception exc) {
        log.error(getLocale("ERROR_MESSAGE_RETRIEVING_INITIAL_DATA", panelable.getTargetAction()), exc);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getOptionByName(String optionId) throws AWException {
    // Get screen from option
    Menu menu = getMenu();

    // Find option
    Option option = menu.getOptionByName(optionId);

    // If option hasn't been found, search in public menu
    if (option == null) {
      option = getMenu(baseConfigProperties.getFiles().getMenuPublic()).getOptionByName(optionId);
    }

    // Check if option is defined
    if (option == null) {
      // Check if session expired
      if (sessionExpired(optionId)) {
        throw new AWESessionException(getLocale("ERROR_TITLE_SESSION_EXPIRED"), getLocale("ERROR_MESSAGE_SESSION_EXPIRED"));
      } else {
        throw new AWENotFoundException(getLocale("ERROR_TITLE_OPTION_NOT_DEFINED"), getLocale("ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED", optionId));
      }
    }

    // Retrieve option
    return option;
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getAvailableOptionByName(String optionId) throws AWException {

    // Find option
    Option option = getAvailableOption(optionId);

    // Check if option is defined
    if (option == null) {
      throw new AWENotFoundException(getLocale("ERROR_TITLE_OPTION_NOT_DEFINED"), getLocale("ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED", optionId));
    }

    // Retrieve option
    return option;
  }

  /**
   * Retrieve a list of available screens
   *
   * @param suggest Screen typed
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public ServiceData getAvailableScreenList(String suggest) throws AWException {
    // Get screen from option
    Set<String> addedScreens = new HashSet<>();
    List<Option> optionList = getAllAvailableOptions();
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    for (Option option : optionList) {
      String screenId = option.getScreen();
      if (screenId != null && !addedScreens.contains(screenId)) {
        // Add to list
        addOptionToList(suggest, screenId, option, dataList, addedScreens);
      }
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Retrieve a list of all screens
   *
   * @param suggest Screen typed
   * @return Screen list retrieved
   */
  public ServiceData getAllScreenList(String suggest) {
    // Get screen from option
    Set<String> addedScreens = new HashSet<>();
    Set<String> screenList = getElements().getScreenList();
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    for (String screen : screenList) {
      // Add to list
      addScreenToList(suggest, screen, screen, dataList, addedScreens);
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Add an option to the datalist
   *
   * @param suggest         Suggest to search
   * @param id              Option/screen identifier
   * @param option          Option
   * @param dataList        Datalist to add data
   * @param previouslyAdded Previously added identifiers
   * @throws AWException Error adding option to list
   */
  private void addOptionToList(String suggest, String id, Option option, DataList dataList, Set<String> previouslyAdded) throws AWException {
    // Get screen label
    String label = option.getLabel();
    String screenId = option.getScreen();
    if (label == null && screenId != null) {
      Screen screen = getScreen(screenId);
      label = screen.getLabel();
    }

    // Get option label locale
    label = label == null ? id : getLocale(label) + " (" + id + ")";

    // Add screen to list
    addScreenToList(suggest, id, label, dataList, previouslyAdded);
  }

  /**
   * Add an option to the datalist
   *
   * @param suggest         Suggest to search
   * @param id              Option/screen identifier
   * @param label           Screen label
   * @param dataList        Datalist to add data
   * @param previouslyAdded Previously added identifiers
   */
  private void addScreenToList(String suggest, String id, String label, DataList dataList, Set<String> previouslyAdded) {
    Map<String, CellData> row = new HashMap<>();

    // Add screen if matches with screen or locale
    if (StringUtil.containsIgnoreCase(label, suggest.trim())) {
      // Set screen name
      row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(id));

      // Store screen label
      row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(label));

      // Store row
      dataList.addRow(row);
      previouslyAdded.add(id);
    }
  }

  /**
   * Check if address is valid
   *
   * @param address Option to check
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public boolean checkOptionAddress(String address) throws AWException {
    // Step 1: Check if option is private
    String optionId = address.startsWith(AweConstants.JSON_SCREEN) ? address.substring(address.lastIndexOf('/') + 1) : null;
    if (address.startsWith(AweConstants.JSON_SCREEN + "/" + PRIVATE_MENU)) {
      return getSession().isAuthenticated() && isAvailableOption(optionId, PRIVATE_MENU);
    } else if (address.startsWith(AweConstants.JSON_SCREEN + "/")) {
      return isAvailableOption(optionId, AweConstants.PUBLIC_MENU);
    } else {
      return false;
    }
  }

  /**
   * Retrieve if option is available
   *
   * @param optionId Option to check
   * @param menuType Menu type
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public boolean isAvailableOption(String optionId, String menuType) throws AWException {
    // Get screen from option
    Menu menu = getMenuWithRestrictions(menuType);
    List<Option> optionList = menu.getElementsByType(Option.class);
    for (Option option : optionList) {
      if (!option.isRestricted() && optionId.equalsIgnoreCase(option.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieve a list of available modules
   *
   * @return Module list retrieved
   * @throws AWException Error retrieving module list
   */
  public ServiceData getModuleList() throws AWException {
    // Get screen from option
    Set<String> modules = new HashSet<>();
    List<Option> optionList = getAllOptions();
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    for (Option option : optionList) {
      String module = option.getModule();
      if (module != null && !modules.contains(module)) {
        Map<String, CellData> row = new HashMap<>();
        // Set screen name
        row.put(AweConstants.JSON_VALUE_PARAMETER, new CellData(module));

        // Store screen label
        row.put(AweConstants.JSON_LABEL_PARAMETER, new CellData(module));

        // Store row
        dataList.addRow(row);
        modules.add(module);
      }
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Retrieve a list of available options
   *
   * @param suggest Option typed
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public ServiceData getNameOptionList(String suggest) throws AWException {
    // Get screen from option
    Set<String> addedOptions = new HashSet<>();
    List<Option> optionList = getAllOptions();
    ServiceData serviceData = new ServiceData();
    DataList dataList = new DataList();
    for (Option option : optionList) {
      String optionName = option.getName();
      if (optionName != null && !addedOptions.contains(optionName)) {
        // Add the option to the list
        addOptionToList(suggest, optionName, option, dataList, addedOptions);
      }
    }

    // Set records
    dataList.setRecords(dataList.getRows().size());

    // Sort results
    DataListUtil.sort(dataList, AweConstants.JSON_LABEL_PARAMETER, "asc");

    // Set datalist to service
    serviceData.setDataList(dataList);
    return serviceData;
  }

  /**
   * Retrieve a list of available screens
   *
   * @param restriction Restriction profile
   * @return Screen restriction list
   * @throws AWException Screen restriction retrieval failure
   */
  public ServiceData getScreenRestrictions(String restriction) throws AWException {
    // Step 1: Get profile restrictions
    String restrictionToSearch = restriction == null || restriction.isEmpty() ? securityConfigProperties.getDefaultRestriction() : restriction;
    Profile baseProfile = getElements().getProfile(restrictionToSearch).copy();

    // Step 2: Generate a datalist from base profile
    DataList baseRestrictions = new DataList();
    if (baseProfile.getRestrictions() != null) {
      for (Restriction baseRestriction : baseProfile.getRestrictions()) {
        Map<String, CellData> row = new HashMap<>();
        // Set option name
        row.put(AweConstants.JSON_OPTION, new CellData(baseRestriction.getOption()));

        // Store screen label
        row.put(AweConstants.JSON_RESTRICTED, new CellData(RestrictionType.R.equals(baseRestriction.getRestrictionType())));
        baseRestrictions.addRow(row);
      }
    }
    // Set records
    baseRestrictions.setRecords(baseRestrictions.getRows().size());

    // Step 3: Get database restrictions and apply profile restrictions if it doesn't exist
    ServiceData serviceData = queryService.launchPrivateQuery(AweConstants.SCREEN_DATABASE_RESTRICTION_QUERY, "1", "0");
    DataList databaseRestrictions = serviceData.getDataList();

    // Step 4: Merge datalists
    DataList result = getBean(DataListBuilder.class).addDataList(baseRestrictions).addDataList(databaseRestrictions).build();

    // Set datalist to service
    serviceData.setDataList(result);
    return serviceData;
  }

  /**
   * Generates an array node with all options in the menu
   *
   * @return Menu options
   * @throws AWException Error generating menu
   */
  public ServiceData refreshMenu() throws AWException {
    ServiceData serviceData = new ServiceData();
    Menu menu = getMenuWithRestrictions();

    // Generate json data with model and controller
    try {
      // Apply option actions
      screenComponentGenerator.applyOptionActions(menu);
    } catch (Exception exc) {
      throw new AWException(getLocale("ERROR_TITLE_MENU"), getLocale("ERROR_MESSAGE_MENU_NOT_CLONEABLE"), exc);
    }

    // Add favourites (if exists)
    Menu favouritesMenu = addFavouritesToMenu(menu);

    // Store json data as javascript
    serviceData.addVariable(AweConstants.ACTION_MENU_OPTIONS, new CellData(favouritesMenu.getElementList()));

    // Return service data
    return serviceData;
  }

  /**
   * Retrieve the menu without restrictions
   *
   * @return Full menu without restrictions
   */
  public ServiceData getMenuOptionTree() throws AWException {
    // Retrieve the full menu
    Menu menu = getMenu(PRIVATE_MENU);

    // Retrieve the number of restrictions per option and transform datalist into map
    DataList restrictionsDataList = queryService.launchPrivateQuery(AweConstants.RESTRICTIONS_PER_OPTION_QUERY, "1", "0").getDataList();
    Map<String, Integer> numberOfRestrictions = restrictionsDataList.getRows().stream()
      .collect(Collectors.toMap(row -> row.get(OPTION).getStringValue(), row -> row.get("restrictions").getIntegerValue(), (a, b) -> a, HashMap::new));

    // Generate datalist with menu and restrictions
    List<Map<String, CellData>> menuTree = getMenuOptionsAsTree(menu.getChildrenByType(Option.class), numberOfRestrictions, null);

    // Return the menu with applied restrictions
    return new ServiceData().setDataList(new DataList().setRows(menuTree).setRecords(menuTree.size()));
  }

  /**
   * Retrieve the menu without restrictions
   *
   * @return Full menu without restrictions
   */
  public ServiceData getMenuOptionTreeByModule(Integer user, Integer profile, String module) throws AWException {
    // Retrieve the full menu
    Menu menu = getMenu(PRIVATE_MENU);
    new ScreenRestrictionGenerator().applyModuleRestriction(module, menu);

    // Retrieve the number of restrictions per option and transform datalist into map
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("user", user);
    parameters.put(PROFILE, profile);
    DataList restrictionsDataList = queryService.launchPrivateQuery(AweConstants.MENU_RESTRICTIONS, parameters).getDataList();
    Map<String, String> restrictions = restrictionsDataList.getRows().stream()
      .collect(Collectors.toMap(row -> row.get(OPTION).getStringValue(), row -> row.get(RESTRICTION).getStringValue(), (a, b) -> a, HashMap::new));

    DataList previousRestrictions = new DataList();
    if (user != null) {
      previousRestrictions = queryService.launchPrivateQuery(AweConstants.MENU_RESTRICTIONS_WITH_USER_PROFILE, parameters).getDataList();
      parameters.set("user", null);
      previousRestrictions.getRows().addAll(queryService.launchPrivateQuery(AweConstants.MENU_RESTRICTIONS, parameters).getDataList().getRows());
    } else if (profile != null) {
      parameters.set(PROFILE, null);
      previousRestrictions = queryService.launchPrivateQuery(AweConstants.MENU_RESTRICTIONS, parameters).getDataList();
    }

    Map<String, String> previousRestrictionsMap = previousRestrictions.getRows().stream()
      .collect(Collectors.toMap(row -> row.get(OPTION).getStringValue(), row -> row.get(RESTRICTION).getStringValue(), (a, b) -> a, HashMap::new));

    // Generate datalist with menu and restrictions
    List<Map<String, CellData>> menuTree = getMenuOptionsAsTreeWithIcons(menu.getChildrenByType(Option.class), restrictions, previousRestrictionsMap, null);

    // Return the menu with applied restrictions
    return new ServiceData().setDataList(new DataList().setRows(menuTree).setRecords(menuTree.size()));
  }

  /**
   * Update menu restriction tree
   *
   * @param option Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData updateMenuRestrictionTree(String option) throws AWException {
    // Retrieve restriction number
    DataList restrictionsDataList = queryService.launchPrivateQuery(AweConstants.RESTRICTIONS_PER_OPTION_QUERY, "1", "0").getDataList();
    CellData restrictions = restrictionsDataList.getRows().stream().findFirst().map(row -> row.get("restrictions")).orElse(new CellData(0));

    // Store restrictions
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("data", JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, restrictions.getIntegerValue())
      .put(JSON_LABEL_PARAMETER, restrictions.getIntegerValue())
      .put(JSON_CELL_STYLE_PARAMETER, restrictions.getIntegerValue() > 0 ? TEXT_SUCCESS : "text-light-gray"));

    // Generate update-cell action
    return new ServiceData().addClientAction(new ClientAction()
      .setAddress(new ComponentAddress("report", "menu-option-tree", option, "number-of-restrictions"))
      .setType("update-cell")
      .setParameterMap(parameters));
  }

  /**
   * Allow menu option with restrictions
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData allowMenuOption(Integer user, Integer profile, String option) throws AWException {
    return changeMenuOptionRestriction(user, profile, option, "A");
  }

  /**
   * Restrict menu option with restrictions
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData restrictMenuOption(Integer user, Integer profile, String option) throws AWException {
    return changeMenuOptionRestriction(user, profile, option, "R");
  }

  /**
   * Remove menu option restriction
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData removeRestriction(Integer user, Integer profile, String option) throws AWException {
    return changeMenuOptionRestriction(user, profile, option, "");
  }

  /**
   * Add favourite option list to private menu
   *
   * @param menu Menu
   */
  private Menu addFavouritesToMenu(Menu menu) throws AWException {
    // Copy menu
    Menu newMenu = menu.copy();

    // Get user
    String user = getSession().getUser();
    if (user == null) {
      return newMenu;
    }

    // Add favourites (if exists)
    List<Favourite> favourites = favouriteService.getFavourites(user);
    if (!favourites.isEmpty()) {
      // Generate favourite option
      Option favourite = (Option) new Option()
        .setIcon("star")
        .setName("favourites")
        .setLabel("MENU_FAVOURITES");

      // Generate favourite separator
      Option separator = (Option) new Option()
        .setSeparator(true)
        .setName("favourites-separator");

      // Add favourites
      favourite.setElementList(favourites.stream()
        .map(Favourite::getOption)
        .map(menu::getOptionByName)
        .map(option -> option.copy()
          .setParent(favourite)
          .setElementList(Collections.emptyList())
          .setId("favourite-" + option.getName()))
        .toList());

      // Update menu
      newMenu.setElementList(Stream.concat(Stream.of(favourite, separator), newMenu.getElementList().stream()
        .filter(o -> !(o instanceof Option option) || !Arrays.asList("favourites", "favourites-separator").contains((option).getName())))
        .toList());
    }

    return newMenu;
  }

  /**
   * Allow menu option with restrictions
   *
   * @param user    Selected user
   * @param profile Selected profile
   * @param option  Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  private ServiceData changeMenuOptionRestriction(Integer user, Integer profile, String option, String restrictionValue) throws AWException {
    // Retrieve previous restriction (if exists)
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("user", user);
    parameters.put(PROFILE, profile);
    parameters.put(OPTION, option);
    DataList restrictionsDataList = queryService.launchPrivateQuery(MENU_RESTRICTIONS, parameters).getDataList();
    Optional<Map<String, CellData>> restriction = restrictionsDataList.getRows().stream().findFirst();

    // Update or delete
    parameters.put(RESTRICTION, restrictionValue);
    parameters.put("active", 1);

    // Get maintain service bean
    MaintainService maintainService = getBean(MaintainService.class);

    if (restriction.isPresent()) {
      if (restrictionValue.isEmpty()) {
        // Delete
        maintainService.launchPrivateMaintain(DELETE_RESTRICTION, parameters);
      } else {
        // Update
        maintainService.launchPrivateMaintain(UPDATE_RESTRICTION, parameters);
      }
    } else {
      // Insert
      maintainService.launchPrivateMaintain(NEW_RESTRICTION, parameters);
    }

    // Store restrictions
    Map<String, Object> restrictionIcon = new HashMap<>();
    restrictionIcon.put("data", JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, restrictionValue)
      .put(JSON_LABEL_PARAMETER, getLocale(getRestrictionLabel(restrictionValue)))
      .put(JSON_ICON_PARAMETER, getRestrictionIcon(restrictionValue))
      .put(JSON_STYLE_PARAMETER, getRestrictionStyle(restrictionValue)));

    // Generate update-cell action
    return new ServiceData().addClientAction(new ClientAction()
      .setAddress(new ComponentAddress("report", "menu-option-tree", option, RESTRICTION))
      .setType("update-cell")
      .setParameterMap(restrictionIcon));
  }

  private List<Map<String, CellData>> getMenuOptionsAsTree(List<Option> options, Map<String, Integer> numberOfRestrictions, String parentId) {
    List<Map<String, CellData>> listOptions = options.stream()
      .map(option -> {
        Map<String, CellData> row = new HashMap<>();
        getMenuOptionTreeCellData(parentId, option, row);
        row.put("module-name", new CellData(option.getModule()));
        int restrictions = Optional.ofNullable(numberOfRestrictions.get(option.getName())).orElse(0);
        row.put("number-of-restrictions", new CellData(JsonNodeFactory.instance.objectNode()
          .put(JSON_VALUE_PARAMETER, restrictions)
          .put(JSON_LABEL_PARAMETER, restrictions)
          .put(JSON_CELL_STYLE_PARAMETER, restrictions > 0 ? TEXT_SUCCESS : "text-light-gray")));
        row.put("option-is-leaf", new CellData(option.getOptions().isEmpty() ? "true" : "false"));
        return row;
      })
      .collect(Collectors.toList());

    // Add children
    listOptions.addAll(options.stream()
      .filter(option -> !option.getOptions().isEmpty())
      .flatMap(option -> getMenuOptionsAsTree(option.getOptions(), numberOfRestrictions, option.getName()).stream())
      .toList()
    );

    return listOptions;
  }

  private void getMenuOptionTreeCellData(String parentId, Option option, Map<String, CellData> row) {
    ObjectNode optionNode = JsonNodeFactory.instance.objectNode();
    optionNode.put(JSON_VALUE_PARAMETER, option.getName());
    optionNode.put(JSON_ICON_PARAMETER, Optional.ofNullable(option.getIcon()).orElse(option.isSeparator() ? "minus fa-rotate-90" : null));
    optionNode.put(JSON_LABEL_PARAMETER, getLocale(Optional.ofNullable(option.getLabel())
      .orElse(Optional.ofNullable(option.getScreen())
        .map(screen -> {
          try {
            return this.getScreen(screen);
          } catch (Exception exc) {
            return null;
          }
        })
        .map(Element::getLabel)
        .orElse(option.isSeparator() ? getLocale("ELEMENT_TYPE_CONTEXTSEPARATOR") : "NO LABEL"))) + " (" + option.getName() + ")");

    row.put("option-id", new CellData(option.getName()));
    row.put("parent-option", new CellData(parentId));
    row.put("option-name", new CellData(optionNode));
  }

  private List<Map<String, CellData>> getMenuOptionsAsTreeWithIcons(List<Option> options, Map<String, String> restrictions, Map<String, String> previousRestrictions, String parentId) {
    List<Map<String, CellData>> listOptions = options.stream()
      .filter(option -> !option.isRestricted())
      .map(option -> {
        Map<String, CellData> row = new HashMap<>();
        getMenuOptionTreeCellData(parentId, option, row);
        String status = Optional.ofNullable(previousRestrictions.get(option.getName())).orElse("A");
        row.put(ACCESS, new CellData(JsonNodeFactory.instance.objectNode()
          .put(JSON_VALUE_PARAMETER, status)
          .put(JSON_LABEL_PARAMETER, getLocale(getRestrictionLabel(status)))
          .put(JSON_ICON_PARAMETER, getRestrictionIcon(status))
          .put(JSON_STYLE_PARAMETER, "")));
        String restriction = Optional.ofNullable(restrictions.get(option.getName())).orElse("");
        row.put(RESTRICTION, new CellData(JsonNodeFactory.instance.objectNode()
          .put(JSON_VALUE_PARAMETER, restrictions.get(option.getName()))
          .put(JSON_LABEL_PARAMETER, getLocale(getRestrictionLabel(restriction)))
          .put(JSON_ICON_PARAMETER, getRestrictionIcon(restriction))
          .put(JSON_STYLE_PARAMETER, getRestrictionStyle(restriction))));
        row.put("option-is-leaf", new CellData(option.getOptions().isEmpty() ? "true" : "false"));
        return row;
      })
      .collect(Collectors.toList());

    // Add children
    listOptions.addAll(options.stream()
      .filter(option -> !option.getOptions().isEmpty())
      .flatMap(option -> getMenuOptionsAsTreeWithIcons(option.getOptions(), restrictions, previousRestrictions, option.getName()).stream())
      .toList()
    );

    return listOptions;
  }

  private String getRestrictionLabel(String restriction) {
    return switch (Optional.ofNullable(restriction).orElse("")) {
      case "R" -> "ENUM_REST_MODE_RESTRICTED";
      case "A" -> "ENUM_REST_MODE_ALLOW";
      default -> "";
    };
  }

  private String getRestrictionIcon(String restriction) {
    return switch (Optional.ofNullable(restriction).orElse("")) {
      case "R" -> "fa-minus-circle";
      case "A" -> "fa-check-circle-o";
      default -> "";
    };
  }

  private String getRestrictionStyle(String restriction) {
    return switch (Optional.ofNullable(restriction).orElse("")) {
      case "R" -> "text-danger";
      case "A" -> TEXT_SUCCESS;
      default -> "";
    };
  }

  /**
   * Generate a new screen based on options
   *
   * @param option Menu screen option
   * @param menu Menu
   * @return Generated menu screen
   */
  private Screen generateMenuScreen(Option option, Menu menu) {
    return new MenuScreenBuilder(option, menu).build();
  }

  /**
   * Retrieve a list of available options
   *
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  private List<Option> getAllOptions() throws AWException {
    // Get options
    Menu menu = getMenu();
    List<Option> optionList = menu.getElementsByType(Option.class);

    // Add public menu options if authenticated
    if (getSession().isAuthenticated()) {
      menu = getMenu(AweConstants.PUBLIC_MENU);
      optionList.addAll(menu.getElementsByType(Option.class));
    }

    return optionList;
  }

  /**
   * Retrieve a list of available options
   *
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  private List<Option> getAllAvailableOptions() throws AWException {
    // Get options
    Menu menu = getMenuWithAllRestrictions();
    List<Option> optionList = menu.getElementsByType(Option.class);

    // Add public menu options if authenticated
    if (getSession().isAuthenticated()) {
      menu = getMenuWithRestrictions(AweConstants.PUBLIC_MENU);
      optionList.addAll(menu.getElementsByType(Option.class));
    }

    return filterRestrictedOptions(optionList);
  }

  /**
   * Retrieve a map of available options
   *
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  private Option getAvailableOption(String optionName) throws AWException {
    // Get options
    Option optionFound = null;
    List<Option> optionList = getAllAvailableOptions();

    // Add public menu options if authenticated
    for (Option option : optionList) {
      if (optionName.equalsIgnoreCase(option.getName())) {
        optionFound = option;
      }
    }

    return optionFound;
  }

  /**
   * Retrieve a list of available options
   *
   * @param optionList Option list
   * @return Option list filtered
   */
  private List<Option> filterRestrictedOptions(List<Option> optionList) {
    // Get options
    List<Option> availableOptionList = new ArrayList<>();
    for (Option option : optionList) {
      if (!option.isRestricted()) {
        availableOptionList.add(option);
      }
    }

    return availableOptionList;
  }
}