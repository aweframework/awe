package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.GridColumnLayout;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.grid.AbstractGrid;
import com.almis.awe.model.util.data.QueryUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Manage the column layout (order and hidden columns) that each user chooses for a grid.
 *
 * <p>The user always comes from the session, never from the parameters of the request.</p>
 */
@Slf4j
public class GridLayoutService extends ServiceConfig {

  private static final String ERROR_TITLE_INVALID_GRID_LAYOUT = "ERROR_TITLE_INVALID_GRID_LAYOUT";
  private static final String ERROR_MESSAGE_INVALID_GRID_LAYOUT = "ERROR_MESSAGE_INVALID_GRID_LAYOUT";
  private static final String ERROR_MESSAGE_GRID_LAYOUT_TOO_LARGE = "ERROR_MESSAGE_GRID_LAYOUT_TOO_LARGE";

  private static final String SCREEN = "screen";
  private static final String GRID_ID = "gridId";
  private static final String LAYOUT = "layout";

  // Sizes of the columns of AweUsrGrdCnf
  private static final int MAX_IDENTIFIER_LENGTH = 100;
  private static final int MAX_LAYOUT_LENGTH = 4000;

  private final ObjectMapper mapper = new ObjectMapper();
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
  public GridLayoutService(QueryService queryService, QueryUtil queryUtil, MaintainService maintainService) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
  }

  /**
   * Save the layout of a grid for the session user, replacing the one stored before
   *
   * @param screen Screen identifier
   * @param gridId Grid identifier
   * @param layout Layout as JSON text ({@code {"order":[...],"hidden":[...]}})
   * @return Service data
   * @throws AWException Invalid layout, no user in the session or storage error
   */
  public ServiceData saveGridLayout(String screen, String gridId, String layout) throws AWException {
    requireSessionUser();
    validateIdentifier(screen);
    validateIdentifier(gridId);
    String normalized = normalize(layout);
    validateGridExists(screen, gridId);

    ObjectNode parameters = getParameters(screen, gridId);
    parameters.put(LAYOUT, normalized);

    if (isStored(parameters)) {
      return maintainService.launchPrivateMaintain("updateGridLayout", parameters);
    }

    try {
      return maintainService.launchPrivateMaintain("insertGridLayout", parameters);
    } catch (AWException exc) {
      // Only a lost race is converted into an update: a concurrent save of the same grid stored the row first.
      // Any other error (database down, constraint, ...) surfaces so the user knows the layout was not saved
      if (!isStored(parameters)) {
        throw exc;
      }
      log.warn("Insert of the layout of grid '{}' in screen '{}' lost a race with another save; updating it", gridId, screen, exc);
      return maintainService.launchPrivateMaintain("updateGridLayout", parameters);
    }
  }

  /**
   * Remove the layout of a grid for the session user, so the grid shows its XML definition again
   *
   * @param screen Screen identifier
   * @param gridId Grid identifier
   * @return Service data
   * @throws AWException Invalid identifiers, no user in the session or storage error
   */
  public ServiceData resetGridLayout(String screen, String gridId) throws AWException {
    requireSessionUser();
    validateIdentifier(screen);
    validateIdentifier(gridId);
    return maintainService.launchPrivateMaintain("deleteGridLayout", getParameters(screen, gridId));
  }

  /**
   * Retrieve the layouts the session user stored for the grids of a screen. A failure never breaks the screen:
   * the grids are then shown with their XML definition.
   *
   * @param screen Screen identifier
   * @return Layouts by grid identifier
   */
  public Map<String, GridColumnLayout> getScreenLayouts(String screen) {
    Map<String, GridColumnLayout> layouts = new HashMap<>();
    if (getSession().getUser() == null || getSession().getUser().isBlank()) {
      return layouts;
    }

    try {
      ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
      parameters.put(SCREEN, screen);
      DataList dataList = queryService.launchPrivateQuery("getUserGridLayouts", parameters).getDataList();
      if (dataList != null && dataList.getRows() != null) {
        for (Map<String, CellData> row : dataList.getRows()) {
          readLayout(row.get(GRID_ID).getStringValue(), row.get(LAYOUT).getStringValue(), layouts);
        }
      }
    } catch (Exception exc) {
      log.warn("The grid layouts of screen '{}' could not be read; the grids use their XML definition", screen, exc);
    }
    return layouts;
  }

  /**
   * Merge the layout stored for a grid with the columns of its XML definition
   *
   * @param grid    Grid
   * @param layouts Layouts of the screen by grid identifier
   * @return Layout to send to the client, or null when the user has none for the grid
   */
  public GridColumnLayout resolveLayout(AbstractGrid grid, Map<String, GridColumnLayout> layouts) {
    GridColumnLayout stored = layouts.get(grid.getElementKey());
    if (stored == null || !grid.isShowToolbar()) {
      return null;
    }
    return GridColumnLayout.merge(stored, grid.getLayoutColumns());
  }

  private void readLayout(String gridId, String stored, Map<String, GridColumnLayout> layouts) {
    try {
      layouts.put(gridId, mapper.readValue(stored, GridColumnLayout.class));
    } catch (JacksonException exc) {
      log.warn("The stored layout of grid '{}' is not valid and is ignored", gridId, exc);
    }
  }

  private ObjectNode getParameters(String screen, String gridId) {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(SCREEN, screen);
    parameters.put(GRID_ID, gridId);
    return parameters;
  }

  private boolean isStored(ObjectNode parameters) throws AWException {
    DataList dataList = queryService.launchPrivateQuery("getUserGridLayout", parameters).getDataList();
    return dataList != null && dataList.getRows() != null && !dataList.getRows().isEmpty();
  }

  private void requireSessionUser() throws AWException {
    String user = getSession().getUser();
    if (user == null || user.isBlank()) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_INVALID_GRID_LAYOUT));
    }
  }

  /**
   * Check that the grid belongs to the screen, so a user cannot store layouts for grids that do not exist
   */
  private void validateGridExists(String screenId, String gridId) throws AWException {
    boolean exists;
    try {
      exists = getElements().getScreen(screenId).getElementsById(gridId).stream().anyMatch(AbstractGrid.class::isInstance);
    } catch (AWException exc) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_INVALID_GRID_LAYOUT), exc);
    }
    if (!exists) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_INVALID_GRID_LAYOUT));
    }
  }

  private void validateIdentifier(String identifier) throws AWException {
    if (identifier == null || identifier.isBlank() || identifier.length() > MAX_IDENTIFIER_LENGTH) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_INVALID_GRID_LAYOUT));
    }
  }

  /**
   * Parse the layout sent by the client and write it back in its compact form, dropping anything unexpected
   */
  private String normalize(String layout) throws AWException {
    try {
      JsonNode node = layout == null ? null : mapper.readTree(layout);
      if (node == null || !node.path("order").isArray() || (node.has("hidden") && !node.get("hidden").isArray())) {
        throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_INVALID_GRID_LAYOUT));
      }
      GridColumnLayout parsed = mapper.treeToValue(node, GridColumnLayout.class);
      if (parsed.getHidden() == null) {
        parsed.setHidden(new ArrayList<>());
      }
      for (String id : parsed.getOrder()) {
        validateIdentifier(id);
      }
      for (String id : parsed.getHidden()) {
        validateIdentifier(id);
      }
      String normalized = mapper.writeValueAsString(parsed);
      if (normalized.length() > MAX_LAYOUT_LENGTH) {
        throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_GRID_LAYOUT_TOO_LARGE));
      }
      return normalized;
    } catch (JacksonException exc) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_GRID_LAYOUT), getLocale(ERROR_MESSAGE_INVALID_GRID_LAYOUT), exc);
    }
  }
}
