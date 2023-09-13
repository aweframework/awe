package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.actions.ComponentAddress;
import com.almis.awe.model.util.data.QueryUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;

public class UserService extends ServiceConfig {

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;

  public UserService(QueryService queryService, QueryUtil queryUtil, MaintainService maintainService) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
  }

  /**
   * Switch user theme mode from light to dark or viceversa
   * @param user User name
   * @param mode New mode
   * @return Service data with client actions
   * @throws AWException IO errors
   */
  public ServiceData changeUserMode(String user, String mode) throws AWException {
    ServiceData serviceData;
    ObjectNode parameters = queryUtil.getParameters();
    parameters.put("user", user);
    parameters.put("mode", mode);

    // Check if user has settings
    DataList userParameters = queryService.launchPrivateQuery("getUserThemeMode", parameters).getDataList();

    // If there are not user parameters, add them, else update them
    if (userParameters.getRows().isEmpty()) {
      serviceData = maintainService.launchPrivateMaintain("addUserSettings", parameters);
    } else {
      serviceData = maintainService.launchPrivateMaintain("updateUserMode", parameters);
    }

    // Update session variable
    getSession().setParameter("themeMode", mode);

    return serviceData.addClientAction(new ClientAction("select")
      .setSilent(true)
      .setAsync(true)
      .setAddress(new ComponentAddress().setView("base").setComponent("currentMode"))
      .addParameter("values", Collections.singletonList(mode)));
  }
}
