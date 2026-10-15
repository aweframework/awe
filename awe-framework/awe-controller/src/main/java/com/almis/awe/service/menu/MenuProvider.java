package com.almis.awe.service.menu;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.Favourite;
import com.almis.awe.model.dto.ScreenRestriction;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.FavouriteService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import com.almis.awe.service.screen.ScreenRestrictionGenerator;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.SESSION_MODULE;

/**
 * Loads the application menus and applies user restrictions and favourites to them
 */
public class MenuProvider extends ServiceConfig {

  private final QueryService queryService;
  private final ScreenComponentGenerator screenComponentGenerator;
  private final BaseConfigProperties baseConfigProperties;
  private final FavouriteService favouriteService;

  /**
   * Autowired constructor
   *
   * @param queryService             Query service
   * @param screenComponentGenerator Screen component generator
   * @param baseConfigProperties     Base configuration properties
   * @param favouriteService         Favourites service
   */
  public MenuProvider(QueryService queryService, ScreenComponentGenerator screenComponentGenerator,
                      BaseConfigProperties baseConfigProperties, FavouriteService favouriteService) {
    this.queryService = queryService;
    this.screenComponentGenerator = screenComponentGenerator;
    this.baseConfigProperties = baseConfigProperties;
    this.favouriteService = favouriteService;
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
   * Retrieve the menu for the user with screen restrictions applied
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions() throws AWException {
    return getMenuWithRestrictions(getMenu());
  }

  /**
   * Retrieve a menu with screen restrictions applied
   *
   * @param menuType Menu type
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithRestrictions(String menuType) throws AWException {
    return getMenuWithRestrictions(getMenu(menuType));
  }

  /**
   * Apply screen restrictions to a menu
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
      List<ScreenRestriction> screenRestrictions = DataListUtil.asBeanList(queryOutput.getDataList(),
        ScreenRestriction.class);

      // Apply restrictions
      new ScreenRestrictionGenerator().applyScreenRestriction(screenRestrictions, menu);
    }

    return menu;
  }

  /**
   * Retrieve the menu for the user with all restrictions (included modules)
   *
   * @return Retrieved menu
   * @throws AWException Menu has not been found
   */
  public Menu getMenuWithAllRestrictions() throws AWException {
    return getMenuWithAllRestrictions(getMenu());
  }

  /**
   * Apply all restrictions (included modules) to a menu
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
              .map(JsonNode::stringValue)
              .orElse(getSession().getParameter(String.class, AweConstants.SESSION_MODULE));

      // Apply module restrictions
      new ScreenRestrictionGenerator().applyModuleRestriction(module, restrictedMenu);

      // Apply configuration restrictions
      restrictedMenu = getMenuWithRestrictions(restrictedMenu);
    }

    return restrictedMenu;
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
        .filter(o -> !(o instanceof Option option)
          || !Arrays.asList("favourites", "favourites-separator").contains((option).getName())))
        .toList());
    }

    return newMenu;
  }
}
