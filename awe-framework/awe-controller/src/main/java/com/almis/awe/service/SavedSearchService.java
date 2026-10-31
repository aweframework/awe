package com.almis.awe.service;

import com.almis.awe.config.SavedSearchConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.util.data.QueryUtil;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manage the saved searches of the advanced block of the windows (#790): each user saves, renames and deletes their own
 * searches, and a user with access to the publish option (property {@code awe.saved-search.publish-option}) publishes
 * searches for a profile, which the users of that profile see and apply but do not change.
 *
 * <p>The user and the profile always come from the session, never from the parameters of the request. The maintain
 * targets that write the searches published for a profile read the profile from a session parameter that only this
 * service sets, after checking the permission, so they do nothing when they are launched directly.</p>
 */
@Slf4j
public class SavedSearchService extends ServiceConfig {

  private static final String ERROR_TITLE_INVALID_SAVED_SEARCH = "ERROR_TITLE_INVALID_SAVED_SEARCH";
  private static final String ERROR_MESSAGE_INVALID_SAVED_SEARCH = "ERROR_MESSAGE_INVALID_SAVED_SEARCH";
  private static final String ERROR_MESSAGE_SAVED_SEARCH_TOO_LARGE = "ERROR_MESSAGE_SAVED_SEARCH_TOO_LARGE";
  private static final String ERROR_MESSAGE_SAVED_SEARCH_NOT_ALLOWED = "ERROR_MESSAGE_SAVED_SEARCH_NOT_ALLOWED";
  private static final String ERROR_MESSAGE_SAVED_SEARCH_NOT_FOUND = "ERROR_MESSAGE_SAVED_SEARCH_NOT_FOUND";
  private static final String ERROR_MESSAGE_SAVED_SEARCH_NAME_EXISTS = "ERROR_MESSAGE_SAVED_SEARCH_NAME_EXISTS";

  // Session parameter read by the maintain targets of the searches published for a profile
  private static final String PUBLISH_PROFILE = "savedSearchPublishProfile";
  // Session parameter that lets the queries read the searches published for any profile: it only lives while the
  // service reads them for a user who may publish, so the queries launched directly return none
  private static final String PUBLISHER = "savedSearchPublisher";
  // Session parameter that the targets of the own searches read the user from: it only lives while the service writes
  // them after its checks, so the targets launched directly change nothing
  private static final String OWNER = "savedSearchOwner";

  private static final String ID = "id";
  private static final String NAME = "name";
  private static final String VALUES = "values";
  private static final String PROFILE = "profile";
  private static final String OWN = "own";
  private static final String USER = "user";
  private static final String SCREEN = "screen";
  private static final String WINDOW_ID = "windowId";
  private static final String VALUE = "value";
  private static final String LABEL = "label";

  // Sizes of the columns of AweSavSch
  private static final int MAX_IDENTIFIER_LENGTH = 100;
  private static final int MAX_VALUES_LENGTH = 4000;

  private final ObjectMapper mapper = new ObjectMapper();
  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final MaintainService maintainService;
  private final MenuService menuService;
  private final SavedSearchConfigProperties properties;

  /**
   * Autowired constructor
   *
   * @param queryService    Query service
   * @param queryUtil       Query utilities
   * @param maintainService Maintain service
   * @param menuService     Menu service
   * @param properties      Saved search properties
   */
  public SavedSearchService(QueryService queryService, QueryUtil queryUtil, MaintainService maintainService,
                            MenuService menuService, SavedSearchConfigProperties properties) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.maintainService = maintainService;
    this.menuService = menuService;
    this.properties = properties;
  }

  /**
   * Retrieve the searches of a window the session user can apply: the own ones first and then the ones published for
   * the profile of the user (for every profile when the user may publish, so it can manage them), each group by name
   *
   * @param screen   Screen identifier
   * @param windowId Window identifier
   * @return Searches with the columns id, name, values (JSON), profile (empty for the own ones) and own (1 or 0)
   * @throws AWException Invalid identifiers or storage error
   */
  public ServiceData getWindowSavedSearches(String screen, String windowId) throws AWException {
    DataList dataList = new DataList();
    if (!hasSessionUser()) {
      return new ServiceData().setDataList(dataList);
    }
    validateIdentifier(screen);
    validateIdentifier(windowId);

    ObjectNode parameters = getParameters(screen, windowId);
    addSearches(dataList, rowsOf(queryService.launchPrivateQuery("getUserSavedSearches", parameters)), true);
    // A user who may publish sees the searches published for every profile, so it can rename and delete them
    if (canPublish()) {
      addSearches(dataList, asPublisherReader(() -> rowsOf(queryService.launchPrivateQuery("getPublishedSavedSearches", parameters))), false);
    } else {
      addSearches(dataList, rowsOf(queryService.launchPrivateQuery("getProfileSavedSearches", parameters)), false);
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Retrieve the profiles the session user may publish searches for: none when the user may not publish
   *
   * @return Profiles with the columns value (acronym) and label (name)
   * @throws AWException Storage error
   */
  public ServiceData getSavedSearchProfiles() throws AWException {
    DataList dataList = new DataList();
    if (hasSessionUser() && canPublish()) {
      for (Map<String, CellData> row : getProfiles()) {
        Map<String, CellData> profile = new HashMap<>();
        profile.put(VALUE, new CellData(row.get(VALUE).getStringValue()));
        profile.put(LABEL, new CellData(row.get(LABEL).getStringValue()));
        dataList.addRow(profile);
      }
    }
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Save a search of a window: an own search of the session user, or a search published for a profile when the user
   * may publish. A search with the same name for the same owner replaces its values
   *
   * @param screen   Screen identifier
   * @param windowId Window identifier
   * @param name     Search name
   * @param values   Values as JSON text ({@code {"<criterion>": [{"value": ..., "label": ...}]}})
   * @param profile  Profile acronym to publish the search for, empty for an own search
   * @return Service data
   * @throws AWException Invalid search, not allowed to publish, no user in the session or storage error
   */
  public ServiceData saveWindowSearch(String screen, String windowId, String name, String values, String profile) throws AWException {
    requireSessionUser();
    validateIdentifier(screen);
    validateIdentifier(windowId);
    String searchName = validateName(name);
    Window window = getSavedSearchWindow(screen, windowId);
    String normalized = normalize(values, window.getSearchCriteria());
    boolean publish = profile != null && !profile.isBlank();
    if (publish) {
      requirePublishAllowed();
      validateProfile(profile);
    }

    ObjectNode parameters = getParameters(screen, windowId);
    parameters.put(NAME, searchName);
    parameters.put(VALUES, normalized);

    if (publish) {
      return asPublisher(profile, () -> save(parameters, "getProfileSavedSearchByName", "insertProfileSavedSearch", "updateProfileSavedSearchValues"));
    }
    return asOwner(() -> save(parameters, "getUserSavedSearchByName", "insertUserSavedSearch", "updateUserSavedSearchValues"));
  }

  /**
   * Rename a search: an own search of the session user, or a published one when the user may publish
   *
   * @param id   Search identifier
   * @param name New name
   * @return Service data
   * @throws AWException Invalid name, unknown search, not allowed, no user in the session or storage error
   */
  public ServiceData renameWindowSearch(String id, String name) throws AWException {
    requireSessionUser();
    String searchName = validateName(name);
    StoredSearch search = getManageableSearch(id);

    ObjectNode parameters = getParameters(search.screen(), search.windowId());
    parameters.put(NAME, searchName);
    if (search.isOwn()) {
      return asOwner(() -> rename(search, parameters, "getUserSavedSearchByName", "renameUserSavedSearch"));
    }
    return asPublisher(search.profile(), () -> rename(search, parameters, "getProfileSavedSearchByName", "renameProfileSavedSearch"));
  }

  /**
   * Delete a search: an own search of the session user, or a published one when the user may publish
   *
   * @param id Search identifier
   * @return Service data
   * @throws AWException Unknown search, not allowed, no user in the session or storage error
   */
  public ServiceData deleteWindowSearch(String id) throws AWException {
    requireSessionUser();
    StoredSearch search = getManageableSearch(id);

    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(ID, search.id());
    if (search.isOwn()) {
      return maintainService.launchPrivateMaintain("deleteUserSavedSearch", parameters);
    }
    return asPublisher(search.profile(), () -> maintainService.launchPrivateMaintain("deleteProfileSavedSearch", parameters));
  }

  /**
   * Whether the session user may publish searches for a profile: the restricted menu of the user holds the publish
   * option and it is not restricted. A missing option, or a menu that cannot be read, means the user may not publish
   *
   * @return The user may publish
   */
  public boolean canPublish() {
    String option = properties.getPublishOption();
    if (option == null || option.isBlank()) {
      return false;
    }
    try {
      Menu menu = menuService.getMenuWithRestrictions();
      return menu.getOptionsByName(option).stream().anyMatch(candidate -> !candidate.isRestricted());
    } catch (AWException | RuntimeException exc) {
      log.warn("The menu of the user could not be read; saved searches cannot be published", exc);
      return false;
    }
  }

  private ServiceData save(ObjectNode parameters, String findQuery, String insertTarget, String updateTarget) throws AWException {
    Optional<Integer> stored = findByName(findQuery, parameters);
    if (stored.isPresent()) {
      return update(parameters, stored.get(), updateTarget);
    }

    try {
      return maintainService.launchPrivateMaintain(insertTarget, parameters);
    } catch (AWException exc) {
      // Only a lost race is converted into an update: a concurrent save of the same name stored the search first.
      // Any other error surfaces so the user knows the search was not saved
      Optional<Integer> concurrent = findByName(findQuery, parameters);
      if (concurrent.isEmpty()) {
        throw exc;
      }
      log.warn("Insert of the saved search '{}' lost a race with another save; updating it", parameters.get(NAME).asString(), exc);
      return update(parameters, concurrent.get(), updateTarget);
    }
  }

  private ServiceData rename(StoredSearch search, ObjectNode parameters, String findQuery, String renameTarget) throws AWException {
    // Another search of the same owner, screen and window already has the name
    Optional<Integer> sameName = findByName(findQuery, parameters);
    if (sameName.isPresent() && sameName.get() != search.id()) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_SAVED_SEARCH), getLocale(ERROR_MESSAGE_SAVED_SEARCH_NAME_EXISTS));
    }
    parameters.put(ID, search.id());
    return maintainService.launchPrivateMaintain(renameTarget, parameters);
  }

  private ServiceData update(ObjectNode parameters, int id, String updateTarget) throws AWException {
    parameters.put(ID, id);
    return maintainService.launchPrivateMaintain(updateTarget, parameters);
  }

  private Optional<Integer> findByName(String query, ObjectNode parameters) throws AWException {
    return rowsOf(queryService.launchPrivateQuery(query, parameters)).stream()
      .map(row -> row.get(ID).getIntegerValue())
      .findFirst();
  }

  /**
   * Retrieve a search the session user may change: an own search, or a published one when the user may publish
   */
  private StoredSearch getManageableSearch(String id) throws AWException {
    int searchId;
    try {
      searchId = Integer.parseInt(id == null ? "" : id.trim());
    } catch (NumberFormatException exc) {
      throw invalid(exc);
    }

    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(ID, searchId);
    PublisherOperation<List<Map<String, CellData>>> read = () -> rowsOf(queryService.launchPrivateQuery("getSavedSearch", parameters));
    // Only a user who may publish reads the searches published for a profile
    List<Map<String, CellData>> rows = canPublish() ? asPublisherReader(read) : read.run();
    if (rows.isEmpty()) {
      throw notFound();
    }
    Map<String, CellData> row = rows.get(0);
    StoredSearch search = new StoredSearch(searchId, text(row, USER), text(row, PROFILE), text(row, SCREEN), text(row, WINDOW_ID));
    if (search.isOwn()) {
      if (!search.user().equalsIgnoreCase(getSession().getUser())) {
        throw notFound();
      }
    } else if (search.profile().isEmpty()) {
      throw notFound();
    } else {
      requirePublishAllowed();
    }
    return search;
  }

  private void addSearches(DataList dataList, List<Map<String, CellData>> rows, boolean own) {
    rows.stream()
      .sorted(Comparator.comparing((Map<String, CellData> row) -> text(row, NAME), String.CASE_INSENSITIVE_ORDER))
      .forEach(row -> {
        Map<String, CellData> search = new HashMap<>();
        search.put(ID, new CellData(row.get(ID).getIntegerValue()));
        search.put(NAME, new CellData(text(row, NAME)));
        search.put(VALUES, new CellData(text(row, VALUES)));
        search.put(PROFILE, new CellData(own ? "" : text(row, PROFILE)));
        search.put(OWN, new CellData(own ? 1 : 0));
        dataList.addRow(search);
      });
  }

  private List<Map<String, CellData>> getProfiles() throws AWException {
    return rowsOf(queryService.launchPrivateQuery("getSavedSearchProfileList", queryUtil.getParameters(null, "1", "0")));
  }

  private void validateProfile(String profile) throws AWException {
    boolean exists = getProfiles().stream().anyMatch(row -> profile.equals(text(row, VALUE)));
    if (!exists) {
      throw invalid(null);
    }
  }

  private void requirePublishAllowed() throws AWException {
    if (!canPublish()) {
      throw new AWException(getLocale(ERROR_TITLE_INVALID_SAVED_SEARCH), getLocale(ERROR_MESSAGE_SAVED_SEARCH_NOT_ALLOWED));
    }
  }

  /**
   * Write the own searches of the session user: the targets read the user from the session parameter, which only lives
   * while the operation runs
   */
  private <T> T asOwner(PublisherOperation<T> operation) throws AWException {
    getSession().setParameter(OWNER, getSession().getUser());
    try {
      return operation.run();
    } finally {
      getSession().removeParameter(OWNER);
    }
  }

  /**
   * Read the searches published for any profile: the queries only return them while the session parameter lives, which
   * is set for a user who may publish
   */
  private <T> T asPublisherReader(PublisherOperation<T> operation) throws AWException {
    getSession().setParameter(PUBLISHER, "1");
    try {
      return operation.run();
    } finally {
      getSession().removeParameter(PUBLISHER);
    }
  }

  /**
   * Launch an operation on the searches published for a profile: the targets read the profile from the session
   * parameter, which only lives while the operation runs
   */
  private <T> T asPublisher(String profile, PublisherOperation<T> operation) throws AWException {
    getSession().setParameter(PUBLISH_PROFILE, profile);
    try {
      return operation.run();
    } finally {
      getSession().removeParameter(PUBLISH_PROFILE);
    }
  }

  /**
   * Retrieve the window of the screen whose saved searches are managed: it must hold an advanced block and opt in
   */
  private Window getSavedSearchWindow(String screenId, String windowId) throws AWException {
    Optional<Window> window;
    try {
      window = getElements().getScreen(screenId).getElementsById(windowId).stream()
        .filter(Window.class::isInstance)
        .map(Window.class::cast)
        .filter(Window::hasSavedSearches)
        .findFirst();
    } catch (AWException exc) {
      throw invalid(exc);
    }
    return window.orElseThrow(() -> invalid(null));
  }

  /**
   * Parse the values sent by the client and write them back in their compact form, keeping only the criteria of the
   * window search and the value and label of each selected value
   */
  private String normalize(String values, List<String> searchCriteria) throws AWException {
    try {
      JsonNode node = values == null ? null : mapper.readTree(values);
      if (node == null || !node.isObject()) {
        throw invalid(null);
      }
      ObjectNode normalized = JsonNodeFactory.instance.objectNode();
      for (String criterion : searchCriteria) {
        if (node.has(criterion)) {
          normalized.set(criterion, normalizeSelection(node.get(criterion)));
        }
      }
      String text = mapper.writeValueAsString(normalized);
      if (text.length() > MAX_VALUES_LENGTH) {
        throw new AWException(getLocale(ERROR_TITLE_INVALID_SAVED_SEARCH), getLocale(ERROR_MESSAGE_SAVED_SEARCH_TOO_LARGE));
      }
      return text;
    } catch (JacksonException exc) {
      throw invalid(exc);
    }
  }

  private ArrayNode normalizeSelection(JsonNode selection) throws AWException {
    if (!selection.isArray()) {
      throw invalid(null);
    }
    ArrayNode normalized = JsonNodeFactory.instance.arrayNode();
    for (JsonNode selected : selection) {
      if (!selected.isObject()) {
        throw invalid(null);
      }
      ObjectNode value = normalized.addObject();
      if (selected.has(VALUE)) {
        value.set(VALUE, selected.get(VALUE));
      }
      if (selected.has(LABEL)) {
        value.set(LABEL, selected.get(LABEL));
      }
    }
    return normalized;
  }

  private ObjectNode getParameters(String screen, String windowId) {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(SCREEN, screen);
    parameters.put(WINDOW_ID, windowId);
    return parameters;
  }

  private static List<Map<String, CellData>> rowsOf(ServiceData serviceData) {
    DataList dataList = serviceData == null ? null : serviceData.getDataList();
    return dataList == null || dataList.getRows() == null ? new ArrayList<>() : dataList.getRows();
  }

  private static String text(Map<String, CellData> row, String column) {
    CellData cell = row.get(column);
    String value = cell == null ? null : cell.getStringValue();
    return value == null ? "" : value;
  }

  private boolean hasSessionUser() {
    String user = getSession().getUser();
    return user != null && !user.isBlank();
  }

  private void requireSessionUser() throws AWException {
    if (!hasSessionUser()) {
      throw invalid(null);
    }
  }

  private String validateName(String name) throws AWException {
    String trimmed = name == null ? null : name.trim();
    validateIdentifier(trimmed);
    return trimmed;
  }

  private void validateIdentifier(String identifier) throws AWException {
    if (identifier == null || identifier.isBlank() || identifier.length() > MAX_IDENTIFIER_LENGTH) {
      throw invalid(null);
    }
  }

  private AWException invalid(Exception cause) {
    return new AWException(getLocale(ERROR_TITLE_INVALID_SAVED_SEARCH), getLocale(ERROR_MESSAGE_INVALID_SAVED_SEARCH), cause);
  }

  private AWException notFound() {
    return new AWException(getLocale(ERROR_TITLE_INVALID_SAVED_SEARCH), getLocale(ERROR_MESSAGE_SAVED_SEARCH_NOT_FOUND));
  }

  /**
   * Operation launched while the session holds the profile of the published searches
   */
  @FunctionalInterface
  private interface PublisherOperation<T> {
    T run() throws AWException;
  }

  /**
   * A stored search: own when it has a user, published for a profile otherwise
   */
  private record StoredSearch(int id, String user, String profile, String screen, String windowId) {
    boolean isOwn() {
      return !user.isEmpty();
    }
  }
}
