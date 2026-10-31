package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.util.data.QueryUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Manage the state that each user leaves in the windows of a screen: whether the advanced block of a window was left
 * open or closed (#790)
 *
 * <p>The user always comes from the session, never from the parameters of the request.</p>
 */
@Slf4j
public class WindowStateService extends ServiceConfig {

  private static final String ERROR_TITLE_INVALID_ARGUMENTS = "ERROR_TITLE_INVALID_ARGUMENTS";
  private static final String ERROR_MESSAGE_INVALID_ARGUMENTS = "ERROR_MESSAGE_INVALID_ARGUMENTS";

  private static final String SCREEN = "screen";
  private static final String WINDOW_ID = "windowId";
  private static final String ADVANCED_OPEN = "advancedOpen";
  private static final String OPEN = "1";
  private static final String CLOSED = "0";

  // Size of the identifier columns of AweUsrWinCnf
  private static final int MAX_IDENTIFIER_LENGTH = 100;

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;

  /**
   * Autowired constructor
   *
   * @param queryService    Query service
   * @param queryUtil       Query utilities
   * @param maintainService Maintain service
   */
  public WindowStateService(QueryService queryService, QueryUtil queryUtil, MaintainService maintainService) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
  }

  /**
   * Save whether the advanced block of a window was left open for the session user, replacing the state stored before
   *
   * @param screen   Screen identifier
   * @param windowId Window identifier
   * @param open     "1" when the block was left open, "0" when it was closed
   * @return Service data
   * @throws AWException Invalid parameters, unknown window, no user in the session or storage error
   */
  public ServiceData saveAdvancedState(String screen, String windowId, String open) throws AWException {
    requireSessionUser();
    validateIdentifier(screen);
    validateIdentifier(windowId);
    if (!OPEN.equals(open) && !CLOSED.equals(open)) {
      throw invalidArguments(null);
    }
    validateWindowHasAdvancedBlock(screen, windowId);

    ObjectNode parameters = getParameters(screen, windowId);
    parameters.put(ADVANCED_OPEN, Integer.parseInt(open));

    if (isStored(parameters)) {
      return maintainService.launchPrivateMaintain("updateWindowAdvancedState", parameters);
    }

    try {
      return maintainService.launchPrivateMaintain("insertWindowAdvancedState", parameters);
    } catch (AWException exc) {
      // Only a lost race is converted into an update: a concurrent save of the same window stored the row first.
      // Any other error (database down, constraint, ...) surfaces
      if (!isStored(parameters)) {
        throw exc;
      }
      log.warn("Insert of the advanced state of window '{}' in screen '{}' lost a race with another save; updating it", windowId, screen, exc);
      return maintainService.launchPrivateMaintain("updateWindowAdvancedState", parameters);
    }
  }

  /**
   * Retrieve the advanced block states the session user stored for the windows of a screen. A failure never breaks the
   * screen: the windows then start as if nothing was stored.
   *
   * @param screen Screen identifier
   * @return Whether the advanced block was left open, by window identifier
   */
  public Map<String, Boolean> getScreenAdvancedStates(String screen) {
    Map<String, Boolean> states = new HashMap<>();
    if (getSession().getUser() == null || getSession().getUser().isBlank()) {
      return states;
    }

    try {
      ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
      parameters.put(SCREEN, screen);
      DataList dataList = queryService.launchPrivateQuery("getUserWindowStates", parameters).getDataList();
      if (dataList != null && dataList.getRows() != null) {
        for (Map<String, CellData> row : dataList.getRows()) {
          states.put(row.get(WINDOW_ID).getStringValue(), OPEN.equals(row.get(ADVANCED_OPEN).getStringValue()));
        }
      }
    } catch (Exception exc) {
      log.warn("The window states of screen '{}' could not be read; the windows start with their default state", screen, exc);
    }
    return states;
  }

  private ObjectNode getParameters(String screen, String windowId) {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(SCREEN, screen);
    parameters.put(WINDOW_ID, windowId);
    return parameters;
  }

  private boolean isStored(ObjectNode parameters) throws AWException {
    DataList dataList = queryService.launchPrivateQuery("getUserWindowState", parameters).getDataList();
    return dataList != null && dataList.getRows() != null && !dataList.getRows().isEmpty();
  }

  private void requireSessionUser() throws AWException {
    String user = getSession().getUser();
    if (user == null || user.isBlank()) {
      throw invalidArguments(null);
    }
  }

  /**
   * Check that the window belongs to the screen and holds an advanced block, so a user cannot store states for windows
   * that do not exist or have no advanced search
   */
  private void validateWindowHasAdvancedBlock(String screenId, String windowId) throws AWException {
    boolean valid;
    try {
      valid = getElements().getScreen(screenId).getElementsById(windowId).stream()
        .anyMatch(element -> element instanceof Window window && window.hasAdvancedBlock());
    } catch (AWException exc) {
      throw invalidArguments(exc);
    }
    if (!valid) {
      throw invalidArguments(null);
    }
  }

  private void validateIdentifier(String identifier) throws AWException {
    if (identifier == null || identifier.isBlank() || identifier.length() > MAX_IDENTIFIER_LENGTH) {
      throw invalidArguments(null);
    }
  }

  private AWException invalidArguments(Exception cause) {
    return new AWException(getLocale(ERROR_TITLE_INVALID_ARGUMENTS), getLocale(ERROR_MESSAGE_INVALID_ARGUMENTS), cause);
  }
}
