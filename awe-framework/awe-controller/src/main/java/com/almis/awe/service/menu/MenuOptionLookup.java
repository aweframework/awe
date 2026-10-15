package com.almis.awe.service.menu;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWESessionException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.almis.awe.model.constant.AweConstants.PRIVATE_MENU;

/**
 * Finds menu options and checks whether they are available for the current user
 */
public class MenuOptionLookup extends ServiceConfig {

  private static final String ERROR_TITLE_OPTION_NOT_DEFINED = "ERROR_TITLE_OPTION_NOT_DEFINED";
  private static final String ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED = "ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED";
  private final MenuProvider menuProvider;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Autowired constructor
   *
   * @param menuProvider         Menu provider
   * @param baseConfigProperties Base configuration properties
   */
  public MenuOptionLookup(MenuProvider menuProvider, BaseConfigProperties baseConfigProperties) {
    this.menuProvider = menuProvider;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Retrieve an option from the user menu
   *
   * @param optionId Option identifier
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getOptionByName(String optionId) throws AWException {
    return getOptionByName(optionId, menuProvider.getMenu());
  }

  /**
   * Retrieve an option from a menu (or from the public menu if the user is authenticated)
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getOptionByName(String optionId, Menu menu) throws AWException {
    // Get screen from option
    Option option = menu.getOptionByName(optionId);

    // If option hasn't been found, search in public menu
    if (option == null && getSession().isAuthenticated()) {
      option = menuProvider.getMenu(baseConfigProperties.getFiles().getMenuPublic()).getOptionByName(optionId);
    }

    // Check if option is defined
    if (option == null) {
      // Check if session expired
      if (sessionExpired(optionId)) {
        throw new AWESessionException(getLocale("ERROR_TITLE_SESSION_EXPIRED"),
          getLocale("ERROR_MESSAGE_SESSION_EXPIRED"));
      } else {
        throw new AWENotFoundException(getLocale(ERROR_TITLE_OPTION_NOT_DEFINED),
          getLocale(ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED, optionId));
      }
    }

    // Retrieve option
    return option;
  }

  /**
   * Retrieve a non-restricted option from the user menu
   *
   * @param optionId Option identifier
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getAvailableOptionByName(String optionId) throws AWException {
    return getAvailableOptionByName(optionId, menuProvider.getMenu());
  }

  /**
   * Retrieve a non-restricted option from a menu or from the public menu
   *
   * @param optionId Option identifier
   * @param menu     Menu
   * @return Option retrieved
   * @throws AWException Option has not been found
   */
  public Option getAvailableOptionByName(String optionId, Menu menu) throws AWException {
    // Find option
    Option option = getAvailableOption(optionId, menu);

    // Check if option is defined
    if (option == null) {
      throw new AWENotFoundException(getLocale(ERROR_TITLE_OPTION_NOT_DEFINED),
        getLocale(ERROR_MESSAGE_OPTION_HAS_NOT_BEEN_DEFINED, optionId));
    }

    // Retrieve option
    return option;
  }

  /**
   * Check if address is valid
   *
   * @param address Option to check
   * @return Address is valid
   * @throws AWException Option has not been found
   */
  public boolean checkOptionAddress(String address) throws AWException {
    // Step 1: Check if option is private
    String optionId = address.startsWith(AweConstants.JSON_SCREEN)
      ? address.substring(address.lastIndexOf('/') + 1) : null;
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
   * @return Option is available
   * @throws AWException Option has not been found
   */
  public boolean isAvailableOption(String optionId, String menuType) throws AWException {
    // Get screen from option
    Menu menu = menuProvider.getMenuWithRestrictions(menuType);
    List<Option> optionList = menu.getElementsByType(Option.class);
    for (Option option : optionList) {
      if (!option.isRestricted() && optionId.equalsIgnoreCase(option.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieve all options of the user menu (and the public menu if the user is authenticated)
   *
   * @return Option list
   * @throws AWException Option has not been found
   */
  public List<Option> getAllOptions() throws AWException {
    // Get options
    Menu menu = menuProvider.getMenu();
    List<Option> optionList = menu.getElementsByType(Option.class);

    // Add public menu options if authenticated
    if (getSession().isAuthenticated()) {
      menu = menuProvider.getMenu(AweConstants.PUBLIC_MENU);
      optionList.addAll(menu.getElementsByType(Option.class));
    }

    return optionList;
  }

  /**
   * Retrieve all non-restricted options of the user menu and the public menu
   *
   * @return Option list
   * @throws AWException Option has not been found
   */
  public List<Option> getAllAvailableOptions() throws AWException {
    return getAllAvailableOptions(menuProvider.getMenu());
  }

  /**
   * Retrieve all non-restricted options of a menu and the public menu
   *
   * @param baseMenu Menu to retrieve
   * @return Option list
   * @throws AWException Option has not been found
   */
  public List<Option> getAllAvailableOptions(Menu baseMenu) throws AWException {
    // Get options
    Menu menu = menuProvider.getMenuWithAllRestrictions(baseMenu);
    List<Option> optionList = menu.getElementsByType(Option.class);

    // Add public menu options
    menu = menuProvider.getMenuWithRestrictions(AweConstants.PUBLIC_MENU);
    optionList.addAll(menu.getElementsByType(Option.class));

    return filterRestrictedOptions(optionList);
  }

  /**
   * Check if session has expired and user is trying to access a private option
   *
   * @return Session has expired
   * @throws AWException Menu has not been found
   */
  private boolean sessionExpired(String optionId) throws AWException {
    if (getSession().isAuthenticated()) {
      return false;
    }
    Menu menu = menuProvider.getMenu(baseConfigProperties.getFiles().getMenuPrivate());
    return !getSession().isAuthenticated() && menu.getOptionByName(optionId) != null;
  }

  /**
   * Retrieve the last available option matching the name
   *
   * @return Option found or null
   * @throws AWException Option has not been found
   */
  private Option getAvailableOption(String optionName, Menu menu) throws AWException {
    // Get options
    Option optionFound = null;
    List<Option> optionList = getAllAvailableOptions(menu);

    // Add public menu options if authenticated
    for (Option option : optionList) {
      if (optionName.equalsIgnoreCase(option.getName())) {
        optionFound = option;
      }
    }

    return optionFound;
  }

  /**
   * Remove restricted options from a list
   *
   * @param optionList Option list
   * @return Option list filtered
   */
  private List<Option> filterRestrictedOptions(List<Option> optionList) {
    return optionList.stream()
      .filter(option -> !option.isRestricted())
      .collect(Collectors.toCollection(ArrayList::new));
  }
}
