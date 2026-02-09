package com.almis.awe.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.Favourite;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Service to manage favourite options for user
 */
@Slf4j
public class FavouriteService {

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;

  public FavouriteService(QueryService queryService, QueryUtil queryUtil, MaintainService maintainService) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
  }

  /**
   * Click on favourite button: Add/Remove option from favourites and refresh menu
   *
   * @param user   User name
   * @param option Option to add/remove
   * @return Service data
   */
  public ServiceData clickFavourite(String user, String option) throws AWException {
    // Get parameters
    ObjectNode parameters = getUserAndOptionParameters(user, option);
    List<Favourite> favouriteList = getFavourites(user);

    // Check if is favourite
    boolean isFavourite = isFavourite(favouriteList, option);

    // Add or remove to favourites
    ServiceData output = maintainService.launchPrivateMaintain(isFavourite ? "removeFromFavourites" : "addToFavourites", parameters);

    // Call check favourites and retrieve the client action list
    output.setClientActionList(checkFavourites(favouriteList, option).getClientActionList());

    // Add a refresh menu option
    output.addClientAction(new ClientAction("server")
      .setSilent(true)
      .addParameter("serverAction", "refresh-menu")
    );

    return output;
  }

  /**
   * Check favourite option to keep button state updated
   *
   * @param user   User name
   * @param option Option to check
   * @return Service data
   */
  public ServiceData checkFavourites(String user, String option) throws AWException {
    return checkFavourites(getFavourites(user), option);
  }

  /**
   * Check favourite option to keep button state updated
   *
   * @param favouriteList Favourite list
   * @param option Option to check
   * @return Service data
   */
  public ServiceData checkFavourites(List<Favourite> favouriteList, String option) {
    ServiceData output = new ServiceData();

    // Check if is favourite
    boolean isFavourite = isFavourite(favouriteList, option);

    // Change icon
    output.addClientAction(new ClientAction("update-controller")
      .setSilent(true)
      .addParameter("attribute", "icon")
      .addParameter("value", isFavourite ? "star" : "star-o")
    );

    // Add/remove class
    output.addClientAction(new ClientAction(isFavourite ? "add-class" : "remove-class")
      .setSilent(true)
      .setTarget("#favourite .nav-icon")
      .addParameter("targetAction", "is-favourite")
    );

    // Change label
    output.addClientAction(new ClientAction("update-controller")
      .setSilent(true)
      .addParameter("attribute", "title")
      .addParameter("value", isFavourite ? "BUTTON_REMOVE_FAVOURITE" : "BUTTON_ADD_FAVOURITE")
    );

    return output;
  }

  /**
   * Get favourite list of screens
   *
   * @param user User to retrieve the list
   * @return List of favourites
   */
  public List<Favourite> getFavourites(String user) throws AWException {
    // Get favourites
    ObjectNode parameters = getUserAndOptionParameters(user, null);
    ServiceData serviceData = queryService.launchPrivateQuery("getFavourites", parameters);
    return DataListUtil.asBeanList(serviceData.getDataList(), Favourite.class);
  }

  /**
   * Check if an option is favourite for a user
   *
   * @param favouriteList Favourite list
   * @param option Option
   * @return Option is favourite
   */
  private boolean isFavourite(List<Favourite> favouriteList, String option) {
    // Return if option is favourite
    return favouriteList.stream().anyMatch(f -> f.getOption().equalsIgnoreCase(option));
  }

  /**
   * Retrieve user and option parameters in objectNode
   *
   * @param user   User
   * @param option Option
   * @return Parameters object
   */
  private ObjectNode getUserAndOptionParameters(String user, String option) {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put("user", user);
    parameters.put("option", option);
    return parameters;
  }
}
