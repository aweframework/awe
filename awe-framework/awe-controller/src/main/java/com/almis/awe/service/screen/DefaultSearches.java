package com.almis.awe.service.screen;

import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.DefaultSearch;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
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
 * context variable comes in the request keeps it, and the criteria the window no longer has are ignored
 */
@Slf4j
public final class DefaultSearches {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String VALUE = "value";

  private DefaultSearches() {
  }

  /**
   * Default searches applied to a screen
   *
   * @param parameters Stored parameters, with the values of the default searches when one was applied
   * @param searches   Default search applied to each window, by window identifier
   * @param criteria   Criteria filled by a default search
   */
  public record Resolution(ObjectNode parameters, Map<String, DefaultSearch> searches, Set<String> criteria) {

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
   * Apply the default search of each window of the screen with saved searches whose criteria keep-criteria did not
   * store. The stored parameters are not changed: the values go to a copy. A default search that cannot be read or
   * applied is skipped, so the screen is generated as it would be without it
   *
   * @param screen             Screen
   * @param storedParameters   Parameters stored by keep-criteria for the screen
   * @param requestParameters  Parameters of the request (context variables)
   * @param savedSearchService Saved search service (none: no default search is applied)
   * @return Resolution
   */
  public static Resolution resolve(Screen screen, ObjectNode storedParameters, ObjectNode requestParameters,
                                   SavedSearchService savedSearchService) {
    List<Window> windows = screen.getElementsByType(Window.class).stream().filter(Window::hasSavedSearches).toList();
    if (savedSearchService == null || windows.isEmpty()) {
      return Resolution.none(storedParameters);
    }

    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    Map<String, DefaultSearch> searches = new HashMap<>();
    Set<String> criteria = new HashSet<>();
    for (Window window : windows) {
      // The last search used in the session (keep-criteria) wins over the default search
      if (window.getSearchCriteria().stream().noneMatch(storedParameters::has)) {
        try {
          savedSearchService.getDefaultWindowSearch(screen.getId(), window.getElementKey())
            .flatMap(search -> apply(window, search, requestParameters, parameters, criteria))
            .ifPresent(search -> searches.put(window.getElementKey(), search));
        } catch (RuntimeException exc) {
          log.warn("The default saved search of window '{}' in screen '{}' could not be applied; the window starts without it",
            window.getElementKey(), screen.getId(), exc);
        }
      }
    }

    if (searches.isEmpty()) {
      return Resolution.none(storedParameters);
    }
    ObjectNode merged = storedParameters.deepCopy();
    merged.setAll(parameters);
    return new Resolution(merged, searches, criteria);
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
