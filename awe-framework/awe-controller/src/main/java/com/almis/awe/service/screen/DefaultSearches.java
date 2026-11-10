package com.almis.awe.service.screen;

import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.DefaultSearch;
import com.almis.awe.model.entities.screen.component.GridDefaultSearch;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.entities.screen.component.criteria.AbstractCriteria;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.component.grid.AbstractGrid;
import com.almis.awe.service.SavedSearchService;
import com.almis.awe.service.savedsearch.DefaultSavedSearch;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default saved searches of the windows of a screen (#915): before the initial load, the default search of each window
 * with saved searches (the one of the user, else the one of its profile) fills the criteria of the window when
 * keep-criteria brought none of them. Its values are written in the stored parameters as the client sends the criteria,
 * so the criteria models and the initial loads read them as they read the values of keep-criteria. A criterion whose
 * context variable comes in the request keeps it, and the criteria the window no longer has are ignored.
 *
 * <p>The default search of each grid with saved searches (#945) applies the same way when keep-criteria brought nothing
 * for the grid (its data, its quick filter or its search): the values of its quick filter and its search fill their
 * criteria, and its sort and column filters go to the data of the grid ({@code <grid>.data}), which only the initial load
 * of that grid reads. The default of a window and the defaults of its grids apply together.</p>
 */
@Slf4j
public final class DefaultSearches {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String VALUE = "value";
  private static final String COLUMN_FILTERS = "columnFilters";
  private static final String COLUMN_FILTER_LABELS = "columnFilterLabels";
  private static final String SORT = "sort";
  private static final String QUICK_FILTER = "quickFilter";
  private static final String SEARCH = "search";

  private DefaultSearches() {
  }

  /**
   * Default searches applied to a screen
   *
   * @param parameters Stored parameters, with the values of the default searches when one was applied
   * @param searches   Default search applied to each window, by window identifier
   * @param criteria   Criteria filled by a default search
   * @param grids      Default search applied to each grid, by grid identifier
   */
  public record Resolution(ObjectNode parameters, Map<String, DefaultSearch> searches, Set<String> criteria,
                           Map<String, GridDefaultSearch> grids) {

    /**
     * Default searches applied to the windows only
     *
     * @param parameters Stored parameters, with the values of the default searches
     * @param searches   Default search applied to each window, by window identifier
     * @param criteria   Criteria filled by a default search
     */
    public Resolution(ObjectNode parameters, Map<String, DefaultSearch> searches, Set<String> criteria) {
      this(parameters, searches, criteria, Map.of());
    }

    /**
     * No default search applied: the stored parameters as they are
     *
     * @param parameters Stored parameters
     * @return Resolution
     */
    public static Resolution none(ObjectNode parameters) {
      return new Resolution(parameters, Map.of(), Set.of());
    }
  }

  /**
   * Apply the default search of each window and grid of the screen with saved searches for which keep-criteria stored
   * nothing. The stored parameters are not changed: the values go to a copy. A default search that cannot be read or
   * applied is skipped, so the screen is generated as it would be without it
   *
   * @param screen             Screen
   * @param storedParameters   Parameters stored by keep-criteria for the screen
   * @param requestParameters  Parameters of the request (context variables)
   * @param savedSearchService Saved search service (none: no default search is applied)
   * @param dataSuffix         Suffix of the data parameter of a component ({@code .data})
   * @return Resolution
   */
  public static Resolution resolve(Screen screen, ObjectNode storedParameters, ObjectNode requestParameters,
                                   SavedSearchService savedSearchService, String dataSuffix) {
    List<Window> windows = screen.getElementsByType(Window.class).stream().filter(Window::hasSavedSearches).toList();
    List<AbstractGrid> savedSearchGrids = screen.getElementsByType(AbstractGrid.class).stream().filter(AbstractGrid::hasSavedSearches).toList();
    if (savedSearchService == null || (windows.isEmpty() && savedSearchGrids.isEmpty())) {
      return Resolution.none(storedParameters);
    }

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    Set<String> criteria = new HashSet<>();
    Map<String, DefaultSearch> searches = resolveWindows(screen, windows, storedParameters, requestParameters,
      savedSearchService, parameters, criteria);
    Map<String, GridDefaultSearch> grids = resolveGrids(screen, savedSearchGrids, storedParameters, requestParameters,
      savedSearchService, dataSuffix, parameters, criteria);

    if (searches.isEmpty() && grids.isEmpty()) {
      return Resolution.none(storedParameters);
    }
    ObjectNode merged = storedParameters.deepCopy();
    merged.setAll(parameters);
    return new Resolution(merged, searches, criteria, grids);
  }

  /**
   * Apply the default search of each window for which keep-criteria stored no criterion
   *
   * @return Default searches applied, by window identifier
   */
  private static Map<String, DefaultSearch> resolveWindows(Screen screen, List<Window> windows, ObjectNode storedParameters,
                                                           ObjectNode requestParameters, SavedSearchService savedSearchService,
                                                           ObjectNode parameters, Set<String> criteria) {
    Map<String, DefaultSearch> searches = new HashMap<>();
    for (Window window : windows) {
      // The last search used in the session (keep-criteria) wins over the default search
      if (window.getSearchCriteria().stream().anyMatch(storedParameters::has)) {
        continue;
      }
      try {
        savedSearchService.getDefaultWindowSearch(screen.getId(), window.getElementKey())
          .flatMap(search -> apply(window, search, requestParameters, parameters, criteria))
          .ifPresent(search -> searches.put(window.getElementKey(), search));
      } catch (RuntimeException exc) {
        log.warn("The default saved search of window '{}' in screen '{}' could not be applied; the window starts without it",
          window.getElementKey(), screen.getId(), exc);
      }
    }
    return searches;
  }

  /**
   * Apply the default search of each grid for which keep-criteria stored no state
   *
   * @return Default searches applied, by grid identifier
   */
  private static Map<String, GridDefaultSearch> resolveGrids(Screen screen, List<AbstractGrid> grids, ObjectNode storedParameters,
                                                             ObjectNode requestParameters, SavedSearchService savedSearchService,
                                                             String dataSuffix, ObjectNode parameters, Set<String> criteria) {
    Map<String, GridDefaultSearch> searches = new HashMap<>();
    for (AbstractGrid grid : grids) {
      // The last state of the grid in the session (keep-criteria) wins over the default search
      if (isKept(grid, storedParameters, dataSuffix)) {
        continue;
      }
      try {
        savedSearchService.getDefaultGridSearch(screen.getId(), grid)
          .flatMap(search -> applyGrid(grid, search, requestParameters, dataSuffix, parameters, criteria))
          .ifPresent(search -> searches.put(grid.getElementKey(), search));
      } catch (RuntimeException exc) {
        log.warn("The default saved search of grid '{}' in screen '{}' could not be applied; the grid starts without it",
          grid.getElementKey(), screen.getId(), exc);
      }
    }
    return searches;
  }

  /**
   * Whether keep-criteria stored a state of the grid: its data (sort, page...), the value of its quick filter or the text
   * of its search. The column filters of the request are not of one grid, so they do not count
   */
  private static boolean isKept(AbstractGrid grid, ObjectNode storedParameters, String dataSuffix) {
    return storedParameters.has(grid.getElementKey() + dataSuffix)
      || Optional.ofNullable(grid.getQuickFilter()).map(AbstractCriteria::getElementKey).filter(storedParameters::has).isPresent()
      || Optional.ofNullable(grid.getSearch()).map(AbstractCriteria::getElementKey).filter(storedParameters::has).isPresent();
  }

  /**
   * Write the state of the default search of a grid in the parameters: the value of its quick filter and the text of its
   * search as the client sends them (unless their context variable comes in the request), and its sort and column
   * filters in the data of the grid. The state comes checked against the grid by the saved search service
   *
   * @return Search applied, empty when its state is not valid (nothing is written then)
   */
  private static Optional<GridDefaultSearch> applyGrid(AbstractGrid grid, DefaultSavedSearch search, ObjectNode requestParameters,
                                                       String dataSuffix, ObjectNode parameters, Set<String> criteria) {
    JsonNode state = MAPPER.readTree(search.values());
    if (state == null || !state.isObject()) {
      log.warn("The default saved search '{}' of grid '{}' has a state that is not valid; the grid starts without it", search.id(),
        grid.getElementKey());
      return Optional.empty();
    }

    ObjectNode gridParameters = JsonNodeFactory.instance.objectNode();
    Set<String> gridCriteria = new HashSet<>();
    JsonNode quickFilter = setCriterion(grid.getQuickFilter(), state.get(QUICK_FILTER), requestParameters, gridParameters, gridCriteria);
    JsonNode text = setCriterion(grid.getSearch(), state.get(SEARCH), requestParameters, gridParameters, gridCriteria);
    JsonNode columnFilters = arrayOf(state.get(COLUMN_FILTERS));
    JsonNode sort = arrayOf(state.get(SORT));
    JsonNode labels = state.get(COLUMN_FILTER_LABELS) != null && state.get(COLUMN_FILTER_LABELS).isObject() ?
      state.get(COLUMN_FILTER_LABELS) : JsonNodeFactory.instance.objectNode();

    // Only the initial load of the grid reads its data: the column filters of the request are left as they are
    ObjectNode data = JsonNodeFactory.instance.objectNode();
    if (!columnFilters.isEmpty()) {
      data.set(COLUMN_FILTERS, columnFilters);
    }
    if (!sort.isEmpty()) {
      data.set(SORT, sort);
    }
    if (!data.isEmpty()) {
      gridParameters.set(grid.getElementKey() + dataSuffix, data);
    }

    parameters.setAll(gridParameters);
    criteria.addAll(gridCriteria);
    return Optional.of(new GridDefaultSearch(search.id(), search.name(), columnFilters, labels, sort, quickFilter, text));
  }

  /**
   * Write the value of a criterion of a grid (its quick filter or its search) as the client sends it, unless its context
   * variable comes in the request
   *
   * @return Value of the search for the criterion, null when the grid has no such criterion or the search sets no value
   */
  private static JsonNode setCriterion(AbstractCriteria criterion, JsonNode value, ObjectNode requestParameters,
                                       ObjectNode parameters, Set<String> criteria) {
    if (criterion == null || value == null || value.isNull()) {
      return null;
    }
    if (!ScreenModelGenerator.checkParameter(criterion.getVariable(), requestParameters)) {
      parameters.set(criterion.getElementKey(), value);
      criteria.add(criterion.getElementKey());
    }
    return value;
  }

  private static JsonNode arrayOf(JsonNode node) {
    return node != null && node.isArray() ? node : JsonNodeFactory.instance.arrayNode();
  }

  /**
   * Write the values of the default search of a window in the parameters, for the criteria the window still has and
   * whose context variable does not come in the request
   *
   * @return Search applied, empty when its values are not valid (nothing is written then)
   */
  private static Optional<DefaultSearch> apply(Window window, DefaultSavedSearch search, ObjectNode requestParameters,
                                               ObjectNode parameters, Set<String> criteria) {
    JsonNode stored = MAPPER.readTree(search.values());
    if (stored == null || !stored.isObject()) {
      return invalid(window, search);
    }

    List<String> searchCriteria = window.getSearchCriteria();
    ObjectNode values = JsonNodeFactory.instance.objectNode();
    ObjectNode windowParameters = JsonNodeFactory.instance.objectNode();
    Set<String> windowCriteria = new HashSet<>();
    for (Criteria criterion : window.getElementsByType(Criteria.class)) {
      JsonNode selection = stored.get(criterion.getId());
      if (selection == null || !searchCriteria.contains(criterion.getId())) {
        continue;
      }
      Optional<JsonNode> parameter = toParameter(criterion, selection);
      if (parameter.isEmpty()) {
        return invalid(window, search);
      }
      values.set(criterion.getId(), selection);
      // A context variable keeps winning over the default search, as it wins over keep-criteria
      if (!ScreenModelGenerator.checkParameter(criterion.getVariable(), requestParameters)) {
        windowParameters.set(criterion.getElementKey(), parameter.get());
        windowCriteria.add(criterion.getElementKey());
      }
    }

    parameters.setAll(windowParameters);
    criteria.addAll(windowCriteria);
    return Optional.of(new DefaultSearch(search.id(), search.name(), values, getLayout(stored.get(SavedSearchService.LAYOUT), window)));
  }

  private static Optional<DefaultSearch> invalid(Window window, DefaultSavedSearch search) {
    log.warn("The default saved search '{}' of window '{}' has values that are not valid; the window starts without it", search.id(),
      window.getElementKey());
    return Optional.empty();
  }

  /**
   * Value of a criterion as the client sends it: the value alone for a criterion of one value, else the list of values
   *
   * @return Parameter, empty when the selection is not a list of values
   */
  private static Optional<JsonNode> toParameter(Criteria criterion, JsonNode selection) {
    if (!selection.isArray()) {
      return Optional.empty();
    }
    ArrayNode values = JsonNodeFactory.instance.arrayNode();
    for (JsonNode selected : selection) {
      if (!selected.isObject()) {
        return Optional.empty();
      }
      if (selected.has(VALUE) && !selected.get(VALUE).isNull()) {
        values.add(selected.get(VALUE));
      }
    }
    if (ScreenModelGenerator.isMultipleValueCriteria(criterion)) {
      return Optional.of(values);
    }
    return Optional.of(values.isEmpty() ? JsonNodeFactory.instance.stringNode("") : values.get(0));
  }

  /**
   * Advanced criteria of the window the search shows in the main filters: the ones the window no longer has are ignored
   */
  private static List<String> getLayout(JsonNode layout, Window window) {
    Set<String> criteria = new LinkedHashSet<>();
    if (layout != null && layout.isArray()) {
      List<String> advancedCriteria = window.getAdvancedCriteria();
      layout.forEach(criterion -> {
        if (criterion.isString() && advancedCriteria.contains(criterion.stringValue())) {
          criteria.add(criterion.stringValue());
        }
      });
    }
    return new ArrayList<>(criteria);
  }
}
