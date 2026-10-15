package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.service.menu.MenuOptionLookup;
import com.almis.awe.service.menu.MenuProvider;
import com.almis.awe.service.menu.MenuRestrictionTreeService;
import com.almis.awe.service.menu.MenuScreenResolver;
import com.almis.awe.service.menu.MenuSuggestionLists;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.List;

/**
 * Manage application Menus.
 *
 * <p>Facade over the menu collaborators of {@code com.almis.awe.service.menu}. Every method delegates to a
 * collaborator, so overriding a method of this class only affects direct callers: collaborators call each other,
 * not this class. Customize a behavior by declaring your own collaborator bean instead.</p>
 */
@Slf4j
public class MenuService extends ServiceConfig {

  public static final String OPTION = MenuRestrictionTreeService.OPTION;
  public static final String RESTRICTION = MenuRestrictionTreeService.RESTRICTION;
  public static final String ACCESS = MenuRestrictionTreeService.ACCESS;
  public static final String TEXT_SUCCESS = MenuRestrictionTreeService.TEXT_SUCCESS;
  public static final String PROFILE = MenuRestrictionTreeService.PROFILE;
  private final MenuProvider menuProvider;
  private final MenuScreenResolver menuScreenResolver;
  private final MenuOptionLookup menuOptionLookup;
  private final MenuSuggestionLists menuSuggestionLists;
  private final MenuRestrictionTreeService menuRestrictionTreeService;
  // Collaborators built by the deprecated constructor; they receive this service's application context
  private final List<ServiceConfig> ownedCollaborators;

  /**
   * Autowired constructor
   *
   * @param menuProvider               Menu provider
   * @param menuScreenResolver         Menu screen resolver
   * @param menuOptionLookup           Menu option lookup
   * @param menuSuggestionLists        Menu suggestion lists
   * @param menuRestrictionTreeService Menu restriction tree service
   */
  public MenuService(MenuProvider menuProvider, MenuScreenResolver menuScreenResolver,
                     MenuOptionLookup menuOptionLookup, MenuSuggestionLists menuSuggestionLists,
                     MenuRestrictionTreeService menuRestrictionTreeService) {
    this.menuProvider = menuProvider;
    this.menuScreenResolver = menuScreenResolver;
    this.menuOptionLookup = menuOptionLookup;
    this.menuSuggestionLists = menuSuggestionLists;
    this.menuRestrictionTreeService = menuRestrictionTreeService;
    this.ownedCollaborators = Collections.emptyList();
  }

  /**
   * Legacy constructor: builds the default menu collaborators, which share this service's application context
   *
   * @param queryService               Query service
   * @param screenComponentGenerator   Screen component generator
   * @param initialLoadDao             Initial load service
   * @param baseConfigProperties       Base configuration properties
   * @param securityConfigProperties   Security configuration properties
   * @param favouriteService           Favourites service
   * @param launcherService            Service launcher service
   * @deprecated since 5.0.0, use {@link #MenuService(MenuProvider, MenuScreenResolver, MenuOptionLookup,
   * MenuSuggestionLists, MenuRestrictionTreeService)} with the collaborator beans
   */
  @Deprecated(since = "5.0.0")
  public MenuService(QueryService queryService, ScreenComponentGenerator screenComponentGenerator,
                     InitialLoadDao initialLoadDao, BaseConfigProperties baseConfigProperties,
                     SecurityConfigProperties securityConfigProperties, FavouriteService favouriteService,
                     LauncherService launcherService) {
    this.menuProvider = new MenuProvider(queryService, screenComponentGenerator, baseConfigProperties,
      favouriteService);
    this.menuOptionLookup = new MenuOptionLookup(menuProvider, baseConfigProperties);
    this.menuScreenResolver = new MenuScreenResolver(menuProvider, menuOptionLookup, initialLoadDao, launcherService,
      baseConfigProperties);
    this.menuSuggestionLists = new MenuSuggestionLists(menuOptionLookup, menuScreenResolver);
    this.menuRestrictionTreeService = new MenuRestrictionTreeService(menuProvider, menuScreenResolver, queryService,
      securityConfigProperties);
    this.ownedCollaborators = List.of(menuProvider, menuOptionLookup, menuScreenResolver, menuSuggestionLists,
      menuRestrictionTreeService);
  }

  /**
   * Store the application context, also in the collaborators built by the legacy constructor
   *
   * @param context application context
   */
  @Override
  public void setApplicationContext(ApplicationContext context) {
    super.setApplicationContext(context);
    ownedCollaborators.forEach(collaborator -> collaborator.setApplicationContext(context));
  }

  /**
   * Retrieve the menu for the user
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenu() throws AWException {
    return menuProvider.getMenu();
  }

  /**
   * Retrieve the menu for the user
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions() throws AWException {
    return menuProvider.getMenuWithRestrictions();
  }

  /**
   * Retrieve the menu for the user
   *
   * @param menuType Menu type
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions(String menuType) throws AWException {
    return menuProvider.getMenuWithRestrictions(menuType);
  }

  /**
   * Retrieve the menu for the user
   *
   * @param menu Menu
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions(Menu menu) throws AWException {
    return menuProvider.getMenuWithRestrictions(menu);
  }

  /**
   * Retrieve the menu with all restrictions (included modules)
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithAllRestrictions() throws AWException {
    return menuProvider.getMenuWithAllRestrictions();
  }

  /**
   * Retrieve the menu with all restrictions (included modules)
   *
   * @param menu Menu
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithAllRestrictions(Menu menu) throws AWException {
    return menuProvider.getMenuWithAllRestrictions(menu);
  }

  /**
   * Retrieve a menu
   *
   * @param menuId Menu identifier
   * @return Menu retrieved
   * @throws AWException Menu has not been found
   */
  public Menu getMenu(String menuId) throws AWException {
    return menuProvider.getMenu(menuId);
  }

  /**
   * Retrieve the menu default screen
   *
   * @return Default screen
   * @throws AWException Default screen has not been defined
   */
  public Screen getDefaultScreen() throws AWException {
    return menuScreenResolver.getDefaultScreen();
  }

  /**
   * Retrieve the menu default screen
   *
   * @param menu Menu
   * @return Default screen
   * @throws AWException Default screen has not been defined
   */
  public Screen getDefaultScreen(Menu menu) throws AWException {
    return menuScreenResolver.getDefaultScreen(menu);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getOptionScreen(String optionId) throws AWException {
    return menuScreenResolver.getOptionScreen(optionId);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getOptionScreen(String optionId, Menu menu) throws AWException {
    return menuScreenResolver.getOptionScreen(optionId, menu);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getAvailableOptionScreen(String optionId) throws AWException {
    return menuScreenResolver.getAvailableOptionScreen(optionId);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getAvailableOptionScreen(String optionId, Menu menu) throws AWException {
    return menuScreenResolver.getAvailableOptionScreen(optionId, menu);
  }

  /**
   * Retrieve an screen
   *
   * @param screenId Screen identifier
   * @return Screen retrieved
   * @throws AWException Screen has not been found
   */
  public Screen getScreen(String screenId) throws AWException {
    return menuScreenResolver.getScreen(screenId);
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
    return menuScreenResolver.getScreen(screenId, optionId);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getOptionByName(String optionId) throws AWException {
    return menuOptionLookup.getOptionByName(optionId);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getOptionByName(String optionId, Menu menu) throws AWException {
    return menuOptionLookup.getOptionByName(optionId, menu);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getAvailableOptionByName(String optionId) throws AWException {
    return menuOptionLookup.getAvailableOptionByName(optionId);
  }

  /**
   * Retrieve an option screen from the user menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getAvailableOptionByName(String optionId, Menu menu) throws AWException {
    return menuOptionLookup.getAvailableOptionByName(optionId, menu);
  }

  /**
   * Retrieve a list of available screens
   *
   * @param suggest Screen typed
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public ServiceData getAvailableScreenList(String suggest) throws AWException {
    return menuSuggestionLists.getAvailableScreenList(suggest);
  }

  /**
   * Retrieve a list of all screens
   *
   * @param suggest Screen typed
   * @return Screen list retrieved
   */
  public ServiceData getAllScreenList(String suggest) {
    return menuSuggestionLists.getAllScreenList(suggest);
  }

  /**
   * Check if address is valid
   *
   * @param address Option to check
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public boolean checkOptionAddress(String address) throws AWException {
    return menuOptionLookup.checkOptionAddress(address);
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
    return menuOptionLookup.isAvailableOption(optionId, menuType);
  }

  /**
   * Retrieve a list of available modules
   *
   * @return Module list retrieved
   * @throws AWException Error retrieving module list
   */
  public ServiceData getModuleList() throws AWException {
    return menuSuggestionLists.getModuleList();
  }

  /**
   * Retrieve a list of available options
   *
   * @param suggest Option typed
   * @return Screen list retrieved
   * @throws AWException Option has not been found
   */
  public ServiceData getNameOptionList(String suggest) throws AWException {
    return menuSuggestionLists.getNameOptionList(suggest);
  }

  /**
   * Retrieve a list of available screens
   *
   * @param restriction Restriction profile
   * @return Screen restriction list
   * @throws AWException Screen restriction retrieval failure
   */
  public ServiceData getScreenRestrictions(String restriction) throws AWException {
    return menuRestrictionTreeService.getScreenRestrictions(restriction);
  }

  /**
   * Generates an array node with all options in the menu
   *
   * @return Menu options
   * @throws AWException Error generating menu
   */
  public ServiceData refreshMenu() throws AWException {
    return menuProvider.refreshMenu();
  }

  /**
   * Retrieve the menu without restrictions
   *
   * @return Full menu without restrictions
   */
  public ServiceData getMenuOptionTree() throws AWException {
    return menuRestrictionTreeService.getMenuOptionTree();
  }

  /**
   * Retrieve the menu without restrictions
   *
   * @return Full menu without restrictions
   */
  public ServiceData getMenuOptionTreeByModule(Integer user, Integer profile, String module) throws AWException {
    return menuRestrictionTreeService.getMenuOptionTreeByModule(user, profile, module);
  }

  /**
   * Update menu restriction tree
   *
   * @param option Selected option
   * @return Service data
   * @throws AWException Error retrieving data
   */
  public ServiceData updateMenuRestrictionTree(String option) throws AWException {
    return menuRestrictionTreeService.updateMenuRestrictionTree(option);
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
    return menuRestrictionTreeService.allowMenuOption(user, profile, option);
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
    return menuRestrictionTreeService.restrictMenuOption(user, profile, option);
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
    return menuRestrictionTreeService.removeRestriction(user, profile, option);
  }
}
