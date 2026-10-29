package com.almis.awe.service.menu;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.builder.MenuScreenBuilder;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.panelable.Panelable;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.type.LoadType;
import com.almis.awe.service.LauncherService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Resolves the screen of a menu or option and initializes it with its initial-load data
 */
@Slf4j
public class MenuScreenResolver extends ServiceConfig {

  private static final String ERROR_TITLE_SCREEN_NOT_DEFINED = "ERROR_TITLE_SCREEN_NOT_DEFINED";
  private final MenuProvider menuProvider;
  private final MenuOptionLookup menuOptionLookup;
  private final InitialLoadDao initialLoadDao;
  private final LauncherService launcherService;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param menuProvider         Menu provider
   * @param menuOptionLookup     Menu option lookup
   * @param initialLoadDao       Initial load service
   * @param launcherService      Service launcher service
   * @param baseConfigProperties Base configuration properties
   */
  public MenuScreenResolver(MenuProvider menuProvider, MenuOptionLookup menuOptionLookup, InitialLoadDao initialLoadDao,
                            LauncherService launcherService, BaseConfigProperties baseConfigProperties) {
    this.menuProvider = menuProvider;
    this.menuOptionLookup = menuOptionLookup;
    this.initialLoadDao = initialLoadDao;
    this.launcherService = launcherService;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve the user menu default screen
   *
   * @return Default screen
   * @throws AWException Default screen has not been defined
   */
  public Screen getDefaultScreen() throws AWException {
    return getDefaultScreen(menuProvider.getMenu());
  }

  /**
   * Retrieve the menu default screen
   *
   * @param menu Menu
   * @return Default screen
   * @throws AWException Default screen has not been defined
   */
  public Screen getDefaultScreen(Menu menu) throws AWException {

    // Get default screen identifier
    String defaultScreenId = menu.getScreen();

    // Check if option has a screen
    if (defaultScreenId == null) {
      throw new AWException(getLocale(ERROR_TITLE_SCREEN_NOT_DEFINED),
        getLocale("ERROR_MESSAGE_MENU_HAS_NOT_DEFAULT_SCREEN"));
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
    return getOptionScreen(optionId, menuProvider.getMenu());
  }

  /**
   * Retrieve an option screen from a menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getOptionScreen(String optionId, Menu menu) throws AWException {
    if (optionId.equalsIgnoreCase(baseConfigProperties.getScreen().getHome())) {
      // Get home screen from menu
      return getScreen(menu.getScreen(), optionId);
    } else {
      // Find option
      Option option = menuOptionLookup.getOptionByName(optionId, menu);

      // Get screen identifier
      return getScreenByOption(option, menu);
    }
  }

  /**
   * Retrieve an available option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getAvailableOptionScreen(String optionId) throws AWException {
    return getAvailableOptionScreen(optionId, menuProvider.getMenu());
  }

  /**
   * Retrieve an available option screen from a menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getAvailableOptionScreen(String optionId, Menu menu) throws AWException {

    if (optionId.equalsIgnoreCase(baseConfigProperties.getScreen().getHome())) {
      // Get home screen from menu
      return getScreen(menu.getScreen(), optionId);
    } else {
      // Find option
      Option option = menuOptionLookup.getAvailableOptionByName(optionId, menu);

      // Get screen identifier
      return getScreenByOption(option, menu);
    }
  }

  /**
   * Retrieve a screen
   *
   * @param screenId Screen identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getScreen(String screenId) throws AWException {
    return getScreen(screenId, null);
  }

  /**
   * Retrieve a screen, initializing it the first time
   *
   * @param screenId Screen identifier
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getScreen(String screenId, String optionId) throws AWException {
    // Check if option has a screen
    if (screenId == null) {
      throw new AWENotFoundException(getLocale(ERROR_TITLE_SCREEN_NOT_DEFINED),
        getLocale("ERROR_MESSAGE_SCREEN_NOT_DEFINED", optionId));
    }

    // Get screen
    Screen screen = getElements().getScreen(screenId).copy();

    // Add screen data
    if (!screen.isInitialized()) {
      initializeScreen(screen);

      // Store an independent copy: the caller goes on changing this one with the user configuration
      getElements().setScreen(screen.copy());
    }

    // Retrieve screen
    return screen;
  }

  /**
   * Retrieve screen by option
   *
   * @param option Option
   * @param menu   Menu
   * @return Screen retrieved (or generated)
   * @throws AWException Screen has not been found
   */
  private Screen getScreenByOption(Option option, Menu menu) throws AWException {
    if (option.isMenuScreen()) {
      // Menu screen
      return new MenuScreenBuilder(option, menu).build();
    } else if (option.isDynamic()) {
      // Dynamic screen
      return (Screen) launcherService.callService(option.getDynamicScreenService(), null).getData();
    } else {
      // Static screen
      return getScreen(option.getScreen(), option.getId());
    }
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
  }

  /**
   * Launch the initial load of each panelable
   *
   * @param screen Screen
   * @return Initial load result per panelable
   * @throws AWException Initial load failure
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
            panelableData.put(row.get(AweConstants.JSON_VALUE_PARAMETER).getStringValue(),
              row.get(AweConstants.JSON_LABEL_PARAMETER).getStringValue());
          }
        }

        // Set data into panelable
        panelable.setTabValues(panelableData);
      } catch (InterruptedException exc) {
        log.error(getLocale("ERROR_MESSAGE_RETRIEVING_INITIAL_DATA", panelable.getTargetAction()), exc);
        Thread.currentThread().interrupt();
      } catch (Exception exc) {
        log.error(getLocale("ERROR_MESSAGE_RETRIEVING_INITIAL_DATA", panelable.getTargetAction()), exc);
      }
    }
  }
}
