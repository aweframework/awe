package com.almis.awe.service;

import com.almis.awe.config.SavedSearchConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ColumnFilter;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.Element;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.component.Window;
import com.almis.awe.model.entities.screen.component.grid.AbstractGrid;
import com.almis.awe.model.entities.screen.component.grid.Column;
import com.almis.awe.model.entities.screen.component.grid.GroupHeader;
import com.almis.awe.model.type.ColumnFilterType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.savedsearch.DefaultSavedSearch;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Manage the saved searches of the advanced block of the windows (#790) and of the grids (#912): each user saves, renames
 * and deletes their own searches, and a user with access to the publish option (property
 * {@code awe.saved-search.publish-option}) publishes searches for a profile, which the users of that profile see and
 * apply but do not change. Each user marks one search of a window as its default search, and a user who may publish
 * marks one published search as the default search of its profile (#915), also in a grid (#945). The searches of a grid
 * are stored as the ones of a window, with the grid id as window id (component ids are unique in a screen), and keep the
 * state of the grid: its column filters (with the labels of their values), its sort, the value of its quick filter and
 * the text of its search.
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
  private static final String DEFAULT = "default";
  private static final String PROFILE_DEFAULT = "profileDefault";
  private static final String DEFAULT_SEARCH = "defaultSearch";
  private static final String MARKED = "1";
  private static final String UNMARKED = "0";

  /**
   * Reserved key of the values of a window search (#915): the advanced criteria the search shows in the main filters
   */
  public static final String LAYOUT = "$layout";

  // Keys of the state of a grid
  private static final String COLUMN_FILTERS = "columnFilters";
  private static final String COLUMN_FILTER_LABELS = "columnFilterLabels";
  private static final String SORT = "sort";
  private static final String DIRECTION = "direction";
  private static final String QUICK_FILTER = "quickFilter";
  private static final String SEARCH = "search";
  private static final Set<String> SORT_DIRECTIONS = Set.of("asc", "desc");

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
   * @return Searches with the columns id, name, values (JSON), profile (empty for the own ones), own (1 or 0), default (1
   * for the default search of the user in the window, else 0) and profileDefault (1 for the default search of its
   * profile, else 0)
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
    Integer userDefault = getUserDefaultSearchId(parameters).orElse(null);
    addSearches(dataList, rowsOf(queryService.launchPrivateQuery("getUserSavedSearches", parameters)), true, userDefault);
    // A user who may publish sees the searches published for every profile, so it can rename and delete them
    if (canPublish()) {
      addSearches(dataList, asPublisherReader(() -> rowsOf(queryService.launchPrivateQuery("getPublishedSavedSearches", parameters))), false,
        userDefault);
    } else {
      addSearches(dataList, rowsOf(queryService.launchPrivateQuery("getProfileSavedSearches", parameters)), false, userDefault);
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
   * Save a search of a window or a grid: an own search of the session user, or a search published for a profile when
   * the user may publish. A search with the same name for the same owner replaces its values
   *
   * @param screen   Screen identifier
   * @param windowId Window or grid identifier
   * @param name     Search name
   * @param values   Values as JSON text: of a window {@code {"<criterion>": [{"value": ..., "label": ...}], "$layout":
   *                 ["<advanced criterion>", ...]}} (the layout is optional), of a grid
   *                 {@code {"columnFilters": [...], "columnFilterLabels": {...}, "sort": [...], "quickFilter": ...,
   *                 "search": ...}}
   * @param profile  Profile acronym to publish the search for, empty for an own search
   * @return Service data
   * @throws AWException Invalid search, not allowed to publish, no user in the session or storage error
   */
  public ServiceData saveWindowSearch(String screen, String windowId, String name, String values, String profile) throws AWException {
    requireSessionUser();
    validateIdentifier(screen);
    validateIdentifier(windowId);
    String searchName = validateName(name);
    String normalized = normalize(values, getSavedSearchComponent(screen, windowId));
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
   * Delete a search: an own search of the session user, or a published one when the user may publish. A search that was
   * the default search of a user stops being it
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
      ServiceData deleted = maintainService.launchPrivateMaintain("deleteUserSavedSearch", parameters);
      asOwner(() -> maintainService.launchPrivateMaintain("clearOwnSearchAsUserDefault", parameters));
      return deleted;
    }
    return asPublisher(search.profile(), () -> {
      ServiceData deleted = maintainService.launchPrivateMaintain("deleteProfileSavedSearch", parameters);
      maintainService.launchPrivateMaintain("clearProfileSearchAsUserDefault", parameters);
      return deleted;
    });
  }

  /**
   * Mark a search as the default search of a window (#915) or a grid (#945) for the session user, or clear it: the search
   * must be one the user can apply in that window or grid, an own search or one published for the profile of the user. It
   * is stored in the row of the window state of the user, which keeps its advanced block state
   *
   * @param screen   Screen identifier
   * @param windowId Window or grid identifier
   * @param id       Search identifier, empty to clear the default search
   * @return Service data
   * @throws AWException Invalid window, unknown search or one the user cannot apply, no user in the session or storage
   *                     error
   */
  public ServiceData setWindowDefaultSearch(String screen, String windowId, String id) throws AWException {
    requireSessionUser();
    validateIdentifier(screen);
    validateIdentifier(windowId);
    validateSavedSearchWindow(screen, windowId);

    ObjectNode parameters = getParameters(screen, windowId);
    if (id == null || id.isBlank()) {
      return asOwner(() -> maintainService.launchPrivateMaintain("clearWindowDefaultSearch", parameters));
    }

    int searchId = parseId(id);
    ObjectNode search = getParameters(screen, windowId);
    search.put(ID, searchId);
    if (rowsOf(queryService.launchPrivateQuery("getApplicableSavedSearch", search)).isEmpty()) {
      throw notFound();
    }
    parameters.put(DEFAULT_SEARCH, searchId);
    return asOwner(() -> saveDefaultSearch(parameters));
  }

  /**
   * Mark a search published for a profile as the default search of that profile in its window (#915) or grid (#945), or
   * unmark it. Only a user who may publish does it, and a profile has one default search per window or grid: marking one
   * unmarks the other
   *
   * @param screen    Screen identifier
   * @param windowId  Window or grid identifier
   * @param id        Search identifier
   * @param isDefault "1" to mark the search as the default of its profile, "0" to unmark it
   * @return Service data
   * @throws AWException Invalid parameters, unknown search, own search, not allowed, no user in the session or storage
   *                     error
   */
  public ServiceData setProfileDefaultSearch(String screen, String windowId, String id, String isDefault) throws AWException {
    requireSessionUser();
    requirePublishAllowed();
    validateIdentifier(screen);
    validateIdentifier(windowId);
    if (!MARKED.equals(isDefault) && !UNMARKED.equals(isDefault)) {
      throw invalid(null);
    }
    validateSavedSearchWindow(screen, windowId);
    StoredSearch search = getManageableSearch(id);
    if (search.isOwn() || !search.screen().equals(screen) || !search.windowId().equals(windowId)) {
      throw notFound();
    }

    ObjectNode parameters = getParameters(screen, windowId);
    parameters.put(ID, search.id());
    parameters.put(DEFAULT, Integer.parseInt(isDefault));
    return asPublisher(search.profile(), () -> {
      if (MARKED.equals(isDefault)) {
        maintainService.launchPrivateMaintain("clearProfileDefaultSearches", parameters);
      }
      return maintainService.launchPrivateMaintain("updateProfileDefaultSearch", parameters);
    });
  }

  /**
   * Retrieve the default search of a window for the session user (#915): its own default while the user can still apply
   * it, else the default of its profile. A failure never breaks the screen: the window then starts without a default
   *
   * @param screen   Screen identifier
   * @param windowId Window identifier
   * @return Default search, empty when there is none
   */
  public Optional<DefaultSavedSearch> getDefaultWindowSearch(String screen, String windowId) {
    if (!hasSessionUser()) {
      return Optional.empty();
    }

    try {
      ObjectNode parameters = getParameters(screen, windowId);
      Optional<Integer> userDefault = getUserDefaultSearchId(parameters);
      if (userDefault.isPresent()) {
        ObjectNode search = getParameters(screen, windowId);
        search.put(ID, userDefault.get());
        Optional<DefaultSavedSearch> applicable = firstSearch(queryService.launchPrivateQuery("getApplicableSavedSearch", search));
        if (applicable.isPresent()) {
          return applicable;
        }
      }
      return firstSearch(queryService.launchPrivateQuery("getProfileDefaultSavedSearch", parameters));
    } catch (Exception exc) {
      log.warn("The default saved search of window '{}' in screen '{}' could not be read; the window starts without it", windowId, screen, exc);
      return Optional.empty();
    }
  }

  /**
   * Retrieve the default search of a grid for the session user (#945), as the default search of a window, with its state
   * checked against the current grid: the column filters and sorts of the columns the grid no longer has are dropped. A
   * state that is no longer valid for the grid is skipped, so the grid starts without a default
   *
   * @param screen Screen identifier
   * @param grid   Grid
   * @return Default search with its state in its compact form, empty when there is none or it is not valid
   */
  public Optional<DefaultSavedSearch> getDefaultGridSearch(String screen, AbstractGrid grid) {
    return getDefaultWindowSearch(screen, grid.getElementKey()).flatMap(search -> {
      try {
        return Optional.of(new DefaultSavedSearch(search.id(), search.name(), normalize(search.values(), grid)));
      } catch (AWException exc) {
        log.warn("The default saved search '{}' of grid '{}' in screen '{}' is not valid; the grid starts without it", search.id(),
          grid.getElementKey(), screen, exc);
        return Optional.empty();
      }
    });
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

  /**
   * Store the default search of the session user in the row of the window state: the row is updated when it exists, and
   * inserted without an advanced block state otherwise
   */
  private ServiceData saveDefaultSearch(ObjectNode parameters) throws AWException {
    if (isWindowStateStored(parameters)) {
      return maintainService.launchPrivateMaintain("updateWindowDefaultSearch", parameters);
    }

    try {
      return maintainService.launchPrivateMaintain("insertWindowDefaultSearch", parameters);
    } catch (AWException exc) {
      // Only a lost race is converted into an update: a concurrent save of the same window stored the row first
      if (!isWindowStateStored(parameters)) {
        throw exc;
      }
      log.warn("Insert of the default saved search of window '{}' lost a race with another save; updating it", parameters.get(WINDOW_ID).asString(), exc);
      return maintainService.launchPrivateMaintain("updateWindowDefaultSearch", parameters);
    }
  }

  private boolean isWindowStateStored(ObjectNode parameters) throws AWException {
    return !rowsOf(queryService.launchPrivateQuery("getUserWindowState", parameters)).isEmpty();
  }

  /**
   * Identifier of the default search the session user stored for the window, if any
   */
  private Optional<Integer> getUserDefaultSearchId(ObjectNode parameters) throws AWException {
    return rowsOf(queryService.launchPrivateQuery("getUserWindowState", parameters)).stream()
      .map(row -> row.get(DEFAULT_SEARCH))
      .filter(cell -> cell != null && !cell.isEmpty())
      .map(CellData::getIntegerValue)
      .findFirst();
  }

  private static Optional<DefaultSavedSearch> firstSearch(ServiceData serviceData) {
    return rowsOf(serviceData).stream()
      .findFirst()
      .map(row -> new DefaultSavedSearch(row.get(ID).getIntegerValue(), text(row, NAME), text(row, VALUES)));
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
    int searchId = parseId(id);

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

  private int parseId(String id) throws AWException {
    try {
      return Integer.parseInt(id == null ? "" : id.trim());
    } catch (NumberFormatException exc) {
      throw invalid(exc);
    }
  }

  private void addSearches(DataList dataList, List<Map<String, CellData>> rows, boolean own, Integer userDefault) {
    rows.stream()
      .sorted(Comparator.comparing((Map<String, CellData> row) -> text(row, NAME), String.CASE_INSENSITIVE_ORDER))
      .forEach(row -> {
        Map<String, CellData> search = new HashMap<>();
        search.put(ID, new CellData(row.get(ID).getIntegerValue()));
        search.put(NAME, new CellData(text(row, NAME)));
        search.put(VALUES, new CellData(text(row, VALUES)));
        search.put(PROFILE, new CellData(own ? "" : text(row, PROFILE)));
        search.put(OWN, new CellData(own ? 1 : 0));
        search.put(DEFAULT, new CellData(row.get(ID).getIntegerValue().equals(userDefault) ? 1 : 0));
        search.put(PROFILE_DEFAULT, new CellData(!own && MARKED.equals(text(row, PROFILE_DEFAULT)) ? 1 : 0));
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
   * Check that the default search is managed for a component of the screen with saved searches: a window (#915) or a
   * grid (#945)
   */
  private void validateSavedSearchWindow(String screenId, String windowId) throws AWException {
    getSavedSearchComponent(screenId, windowId);
  }

  /**
   * Retrieve the component of the screen whose saved searches are managed: a window that holds an advanced block, or a
   * grid, that opts in
   */
  private Element getSavedSearchComponent(String screenId, String componentId) throws AWException {
    Optional<Element> component;
    try {
      component = getElements().getScreen(screenId).getElementsById(componentId).stream()
        .filter(element -> element instanceof Window window && window.hasSavedSearches()
          || element instanceof AbstractGrid grid && grid.hasSavedSearches())
        .findFirst();
    } catch (AWException exc) {
      throw invalid(exc);
    }
    return component.orElseThrow(() -> invalid(null));
  }

  /**
   * Parse the values sent by the client and write them back in their compact form: the state of a grid, or the
   * criteria of the search of a window with the value and label of each selected value
   */
  private String normalize(String values, Element component) throws AWException {
    try {
      JsonNode node = values == null ? null : mapper.readTree(values);
      if (node == null || !node.isObject()) {
        throw invalid(null);
      }
      ObjectNode normalized = switch (component) {
        case AbstractGrid grid -> normalizeGridState(node, grid);
        case Window window -> normalizeCriteria(node, window);
        case null, default -> throw invalid(null);
      };
      String text = mapper.writeValueAsString(normalized);
      if (text.length() > MAX_VALUES_LENGTH) {
        throw new AWException(getLocale(ERROR_TITLE_INVALID_SAVED_SEARCH), getLocale(ERROR_MESSAGE_SAVED_SEARCH_TOO_LARGE));
      }
      return text;
    } catch (JacksonException exc) {
      throw invalid(exc);
    }
  }

  private ObjectNode normalizeCriteria(JsonNode node, Window window) throws AWException {
    ObjectNode normalized = JsonNodeFactory.instance.objectNode();
    // A window without an advanced block has no search criteria (null): nothing is kept
    for (String criterion : Objects.requireNonNullElse(window.getSearchCriteria(), List.<String>of())) {
      if (node.has(criterion)) {
        normalized.set(criterion, normalizeSelection(node.get(criterion)));
      }
    }
    if (node.has(LAYOUT)) {
      normalized.set(LAYOUT, normalizeLayout(node.get(LAYOUT), Objects.requireNonNullElse(window.getAdvancedCriteria(), List.of())));
    }
    return normalized;
  }

  /**
   * Check the layout of a window search (#915): a list of advanced criteria of the window, each one once
   */
  private ArrayNode normalizeLayout(JsonNode layout, List<String> advancedCriteria) throws AWException {
    if (!layout.isArray()) {
      throw invalid(null);
    }
    Set<String> criteria = new HashSet<>();
    ArrayNode normalized = JsonNodeFactory.instance.arrayNode();
    for (JsonNode criterion : layout) {
      if (!criterion.isString() || !advancedCriteria.contains(criterion.stringValue()) || !criteria.add(criterion.stringValue())) {
        throw invalid(null);
      }
      normalized.add(criterion.stringValue());
    }
    return normalized;
  }

  /**
   * Keep the state of a grid: its column filters (valid and on columns that declare that filter), the labels of their
   * values, its sort (on columns it can sort by), and the value of its quick filter and the text of its search when it
   * has them. Filters and sorts of columns the grid no longer has are dropped; any other wrong value is rejected
   */
  private ObjectNode normalizeGridState(JsonNode state, AbstractGrid grid) throws AWException {
    List<Column> columns = getGridColumns(grid);
    ObjectNode normalized = JsonNodeFactory.instance.objectNode();
    Set<String> filtered = new HashSet<>();
    if (state.has(COLUMN_FILTERS)) {
      normalized.set(COLUMN_FILTERS, normalizeColumnFilters(state.get(COLUMN_FILTERS), columns, filtered));
    }
    if (state.has(COLUMN_FILTER_LABELS)) {
      normalized.set(COLUMN_FILTER_LABELS, normalizeColumnFilterLabels(state.get(COLUMN_FILTER_LABELS), filtered));
    }
    if (state.has(SORT)) {
      normalized.set(SORT, normalizeSort(state.get(SORT), columns));
    }
    if (grid.getQuickFilter() != null && state.has(QUICK_FILTER)) {
      JsonNode quickFilter = state.get(QUICK_FILTER);
      if (!quickFilter.isValueNode()) {
        throw invalid(null);
      }
      normalized.set(QUICK_FILTER, quickFilter);
    }
    if (grid.getSearch() != null && state.has(SEARCH)) {
      JsonNode search = state.get(SEARCH);
      if (!search.isString() && !search.isNull()) {
        throw invalid(null);
      }
      normalized.set(SEARCH, search);
    }
    return normalized;
  }

  /**
   * Columns of a grid, also the ones of its group headers
   */
  private static List<Column> getGridColumns(AbstractGrid grid) {
    List<Column> columns = new ArrayList<>();
    for (Element element : grid.getElementList()) {
      if (element instanceof Column column) {
        columns.add(column);
      } else if (element instanceof GroupHeader groupHeader) {
        columns.addAll(groupHeader.getChildrenByType(Column.class));
      }
    }
    return columns;
  }

  private ArrayNode normalizeColumnFilters(JsonNode node, List<Column> columns, Set<String> filtered) throws AWException {
    if (!node.isArray()) {
      throw invalid(null);
    }
    List<ColumnFilter> filters;
    try {
      filters = ColumnFilter.fromJson(node);
    } catch (IllegalArgumentException exc) {
      throw invalid(exc);
    }
    ArrayNode normalized = JsonNodeFactory.instance.arrayNode();
    for (ColumnFilter filter : filters) {
      Optional<Column> column = columns.stream().filter(candidate -> filter.getId().equals(candidate.getName())).findFirst();
      Optional<ColumnFilterType> type = column.flatMap(candidate -> ColumnFilterType.fromCode(candidate.getFilter()));
      if (type.isEmpty() || !filtered.add(filter.getId())) {
        continue;
      }
      // The filter of a column always has the type and the fields the column declares
      if (filter.getType() != type.get() || !filter.getFields().equals(getFilterFields(column.get()))) {
        throw invalid(null);
      }
      normalized.add(filter.toJson());
    }
    return normalized;
  }

  /**
   * Fields a column filters: its filter-fields, else its sort-field, else its name (as the clients send them)
   */
  private static List<String> getFilterFields(Column column) {
    List<String> fields = column.getFilterFields() == null ? List.of() : Arrays.stream(column.getFilterFields().split(","))
      .map(String::trim)
      .filter(field -> !field.isEmpty())
      .toList();
    if (!fields.isEmpty()) {
      return fields;
    }
    return List.of(column.getField() == null || column.getField().isBlank() ? column.getName() : column.getField());
  }

  private ObjectNode normalizeColumnFilterLabels(JsonNode node, Set<String> filtered) throws AWException {
    if (!node.isObject()) {
      throw invalid(null);
    }
    ObjectNode normalized = JsonNodeFactory.instance.objectNode();
    for (String column : node.propertyNames()) {
      JsonNode labels = node.get(column);
      if (!labels.isObject()) {
        throw invalid(null);
      }
      for (String value : labels.propertyNames()) {
        if (!labels.get(value).isString()) {
          throw invalid(null);
        }
      }
      if (filtered.contains(column)) {
        normalized.set(column, labels);
      }
    }
    return normalized;
  }

  private ArrayNode normalizeSort(JsonNode node, List<Column> columns) throws AWException {
    if (!node.isArray()) {
      throw invalid(null);
    }
    ArrayNode normalized = JsonNodeFactory.instance.arrayNode();
    for (JsonNode entry : node) {
      JsonNode id = entry.get(ID);
      JsonNode direction = entry.get(DIRECTION);
      if (!entry.isObject() || id == null || !id.isString() || direction == null || !direction.isString()
        || !SORT_DIRECTIONS.contains(direction.stringValue().toLowerCase(Locale.ROOT))) {
        throw invalid(null);
      }
      String column = id.stringValue();
      boolean sortable = columns.stream().anyMatch(candidate -> candidate.isSortable()
        && (column.equals(candidate.getName()) || column.equals(candidate.getField())));
      if (sortable) {
        normalized.addObject()
          .put(ID, column)
          .put(DIRECTION, direction.stringValue().toLowerCase(Locale.ROOT));
      }
    }
    return normalized;
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
