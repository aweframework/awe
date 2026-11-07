package com.almis.awe.service.user;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWEUnauthorizedException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.security.authorization.PermissionTargetAuthorizer;
import com.almis.awe.security.authorization.TargetKind;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.user.PredefinedProfiles.PredefinedProfile;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Administration of the permissions of the profiles in the profiles screen (profile-admin): the options of the
 * permission catalog, the access of the user to them, their storage, the guard that keeps the predefined profiles
 * unchanged and the duplication of a profile with its effective permissions. The options come from the permission
 * catalog loaded at run time, which combines AWE, its modules and the application
 */
public class ProfilePermissionService extends ServiceConfig {

  private static final String PROFILES_QUERY = "ProAcrRes";
  private static final String ASSIGNMENTS_QUERY = "ProPerSel";
  private static final String ASSIGNMENTS_LIST_QUERY = "ProPerLst";
  private static final String STORED_ASSIGNMENTS_QUERY = "ProPerDst";
  private static final String CREATE_MAINTAIN = "ProNew";
  private static final String ASSIGNMENTS_MAINTAIN = "ProPerUpd";
  private static final String PROFILE_ID = "IdePro";
  private static final String PERMISSIONS = "Per";
  private static final String ACCESS_NONE = "none";
  private static final String ACCESS_READ = "read";
  private static final String ACCESS_UPDATE = "update";
  private static final String VALUE = "value";
  private static final String LABEL = "label";
  private static final String GROUP = "group";
  private static final String SEPARATOR = " · ";
  private static final String ALL_RESOURCES_LABEL = "PERMISSION_RESOURCE_ALL";
  private static final String ALL_PERMISSIONS_LABEL = "PERMISSION_VERB_ALL";
  private static final String UNAVAILABLE_GROUP = "PERMISSION_GROUP_UNAVAILABLE";
  private static final String UNAVAILABLE_LABEL = "PERMISSION_UNAVAILABLE";

  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;
  private final ObjectProvider<AweUserDetailService> userDetailService;
  private final SecurityConfigProperties securityConfigProperties;
  private final PermissionTargetAuthorizer permissionTargetAuthorizer;

  /**
   * Autowired constructor
   *
   * @param queryService               Query service
   * @param maintainService            Maintain service
   * @param queryUtil                  Query utilities
   * @param userDetailService          User detail service, reads the permissions of the profile files
   * @param securityConfigProperties   Security configuration properties
   * @param permissionTargetAuthorizer Permission target authorizer
   */
  public ProfilePermissionService(QueryService queryService, MaintainService maintainService, QueryUtil queryUtil,
                                  ObjectProvider<AweUserDetailService> userDetailService,
                                  SecurityConfigProperties securityConfigProperties,
                                  PermissionTargetAuthorizer permissionTargetAuthorizer) {
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
    this.userDetailService = userDetailService;
    this.securityConfigProperties = securityConfigProperties;
    this.permissionTargetAuthorizer = permissionTargetAuthorizer;
  }

  /**
   * Options of the permission catalog, grouped by resource: every permission ({@code *}) and one verb on every
   * resource ({@code *.read}) under "All resources" for the verbs of several resources (the own actions of a resource
   * and a verb of only one resource, as {@code default.write}, are only under it), then every resource with all its permissions
   * ({@code users.*}) and each of them. The label is "Resource · Verb" in the language of the user, and the group is
   * the locale key of the resource. The permissions granted always are left out: no profile needs them. Last, the
   * assignments stored in any profile that are not offered (a permission no longer in the catalog or one the editor
   * does not list), with a warning, so they are seen and can be revoked
   *
   * @return Options with value, label and group
   * @throws AWException Error reading the stored assignments
   */
  public ServiceData getPermissionOptions() throws AWException {
    PermissionCatalog catalog = getPermissionCatalog();
    Set<String> grantedAlways = catalog.getGrantedAlways();
    Map<String, String> verbLabels = new LinkedHashMap<>();
    Map<String, Integer> verbResources = new HashMap<>();
    Map<String, List<String>> resources = new LinkedHashMap<>();
    for (String permission : catalog.getPermissionIds()) {
      if (grantedAlways.contains(permission)) {
        continue;
      }
      int separator = permission.indexOf(PermissionCatalog.SEPARATOR);
      if (!catalog.isAction(permission)) {
        String verb = permission.substring(separator + 1);
        verbLabels.putIfAbsent(verb, catalog.getLabel(permission));
        verbResources.merge(verb, 1, Integer::sum);
      }
      resources.computeIfAbsent(permission.substring(0, separator), resource -> new ArrayList<>()).add(permission);
    }

    DataList options = new DataList();
    String allResources = getLocale(ALL_RESOURCES_LABEL);
    String allPermissions = getLocale(ALL_PERMISSIONS_LABEL);
    addOption(options, PermissionCatalog.WILDCARD, allResources + SEPARATOR + allPermissions, ALL_RESOURCES_LABEL);
    verbLabels.entrySet().stream().filter(verb -> verbResources.get(verb.getKey()) > 1)
      .forEach(verb -> addOption(options, PermissionCatalog.WILDCARD + PermissionCatalog.SEPARATOR + verb.getKey(),
        allResources + SEPARATOR + getLocale(verb.getValue()), ALL_RESOURCES_LABEL));
    resources.forEach((resource, permissions) -> {
      String group = catalog.getResourceLabel(resource);
      String resourceLabel = getLocale(group);
      addOption(options, resource + PermissionCatalog.SEPARATOR + PermissionCatalog.WILDCARD,
        resourceLabel + SEPARATOR + allPermissions, group);
      permissions.forEach(permission -> addOption(options, permission,
        resourceLabel + SEPARATOR + getLocale(catalog.getLabel(permission)), group));
    });
    Set<String> offered = new HashSet<>();
    options.getRows().forEach(row -> offered.add(row.get(VALUE).getStringValue()));
    for (String permission : readValues(STORED_ASSIGNMENTS_QUERY, allRows())) {
      if (offered.add(permission)) {
        addOption(options, permission, getLocale(UNAVAILABLE_LABEL, permission), UNAVAILABLE_GROUP);
      }
    }
    options.setRecords(options.getRows().size());
    return new ServiceData().setDataList(options);
  }

  /**
   * Permissions assigned to a profile (AweProPer), all of them: the profile panel shows them all, the ones the catalog
   * does not offer with a warning, and saving it replaces them with the ones shown
   *
   * @param profileId Profile identifier
   * @return Assignments as values, in alphabetical order
   * @throws AWException Error reading the assignments
   */
  public ServiceData getProfileAssignments(Integer profileId) throws AWException {
    DataList assignments = new DataList();
    if (profileId != null) {
      for (String permission : readAssignments(profileId)) {
        Map<String, CellData> row = new HashMap<>();
        row.put(VALUE, new CellData(permission));
        assignments.addRow(row);
      }
    }
    assignments.setRecords(assignments.getRows().size());
    return new ServiceData().setDataList(assignments);
  }

  /**
   * Access of the session user to the permissions of the profiles, as the screen elements are decided: only in
   * {@code enforce} mode a user can lack it. {@code none} without the permission of the query of the assignments
   * (ProPerSel), {@code read} without the permission of their storage (ProPerUpd), {@code update} otherwise
   *
   * @return One row whose value is none, read or update
   */
  public ServiceData getPermissionAccess() {
    String access;
    if (!canRead()) {
      access = ACCESS_NONE;
    } else if (isEnforced() && !permissionTargetAuthorizer.canLaunch(ASSIGNMENTS_MAINTAIN, TargetKind.MAINTAIN)) {
      access = ACCESS_READ;
    } else {
      access = ACCESS_UPDATE;
    }
    DataList value = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(VALUE, new CellData(access));
    value.addRow(row);
    value.setRecords(1);
    return new ServiceData().setDataList(value);
  }

  /**
   * Store the permissions of a profile sent by the profiles screen: launched by the maintains that create and update
   * profiles, after the profile is stored. Nothing is done without the flag of the screen (the legacy screens do not
   * send it), for a user that cannot read the permissions (they are not shown to it) or when they did not change; a
   * change requires the permission of ProPerUpd, checked as a client launch of that target. Every stored assignment is
   * shown, so the selection replaces them all
   *
   * @param profileIds  Profile identifier (a value or a one element list)
   * @param permissions Selected permissions
   * @param flag        1 when the screen sends the permissions
   * @return Empty service data
   * @throws AWException When the user cannot change the permissions, or they cannot be stored
   */
  public ServiceData storeProfilePermissions(List<Object> profileIds, List<Object> permissions, Integer flag)
    throws AWException {
    List<Integer> ids = toIds(profileIds);
    if (!Integer.valueOf(1).equals(flag) || ids.size() != 1 || !canRead()) {
      return new ServiceData();
    }
    Set<String> selected = new LinkedHashSet<>();
    if (permissions != null) {
      permissions.stream().filter(Objects::nonNull).map(permission -> String.valueOf(permission).trim())
        .filter(permission -> !permission.isEmpty()).forEach(selected::add);
    }
    int profileId = ids.getFirst();
    Set<String> stored = new HashSet<>(readAssignments(profileId));
    if (selected.equals(stored)) {
      return new ServiceData();
    }
    if (!permissionTargetAuthorizer.authorize(ASSIGNMENTS_MAINTAIN, TargetKind.MAINTAIN)) {
      throw new AWEUnauthorizedException(getLocale(AWEUnauthorizedException.TITLE_KEY),
        getLocale(AWEUnauthorizedException.MESSAGE_KEY));
    }
    replaceAssignments(profileId, selected);
    return new ServiceData();
  }

  /**
   * Whether some profiles are predefined
   *
   * @param profileIds Profile identifiers (the selected rows of the profiles grid)
   * @return One row whose value is 1 when one of the profiles is predefined, 0 otherwise
   * @throws AWException Error reading the profiles
   */
  public ServiceData getPredefinedFlag(List<Object> profileIds) throws AWException {
    boolean predefined = findPredefined(readProfiles(profileIds)).isPresent();
    DataList flag = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(VALUE, new CellData(predefined ? 1 : 0));
    flag.addRow(row);
    flag.setRecords(1);
    return new ServiceData().setDataList(flag);
  }

  /**
   * Refuse to change a predefined profile: launched first by the maintains that update or delete profiles, so nothing
   * is changed
   *
   * @param profileIds Profile identifiers
   * @return Empty service data
   * @throws AWException When one of the profiles is predefined
   */
  public ServiceData checkEditableProfiles(List<Object> profileIds) throws AWException {
    Optional<StoredProfile> predefined = findPredefined(readProfiles(profileIds));
    if (predefined.isPresent()) {
      throw predefinedProfileError(predefined.get().acronym());
    }
    return new ServiceData();
  }

  /**
   * Create a copy of a profile that can be changed: a new profile with the data entered, whose permissions are the
   * effective permissions of the source copied as assignments: those of its profile file minus its exclusions
   * (expanded, without the permissions granted always) and its own assignments, as they are. The copy of a predefined
   * profile does not keep the profile file of the predefined profile, since its permissions are copied as assignments
   *
   * @param profileIds  Identifier of the source profile (the selected row of the profiles grid)
   * @param acronym     Acronym of the copy
   * @param name        Name of the copy
   * @param theme       Theme of the copy
   * @param screen      Initial screen of the copy
   * @param restriction Profile file of the copy
   * @param active      Active flag of the copy
   * @return Empty service data
   * @throws AWException Error reading the source or creating the copy
   */
  public ServiceData duplicateProfile(List<Object> profileIds, String acronym, String name, Integer theme,
                                      String screen, String restriction, Integer active) throws AWException {
    List<StoredProfile> sources = readProfiles(profileIds);
    if (sources.size() != 1) {
      throw new AWException(getLocale("ERROR_TITLE_DUPLICATE_PROFILE"), getLocale("ERROR_MESSAGE_DUPLICATE_PROFILE"));
    }
    StoredProfile source = sources.getFirst();
    Set<String> grantedAlways = getPermissionCatalog().getGrantedAlways();
    Set<String> permissions = new LinkedHashSet<>();
    userDetailService.getObject().getProfileFilePermissions(source.restriction()).stream()
      .filter(permission -> !grantedAlways.contains(permission))
      .forEach(permissions::add);
    permissions.addAll(readAssignments(source.id()));

    boolean keepsPredefinedFile = predefinedOf(source)
      .map(PredefinedProfile::file)
      .filter(file -> restriction != null && file.equals(restriction.trim()))
      .isPresent();
    ObjectNode copy = queryUtil.getParameters(JsonNodeFactory.instance.objectNode());
    copy.put("Acr", acronym);
    copy.put("Nam", name);
    copy.put("IdeThm", theme);
    copy.put("ScrIni", screen);
    copy.put("Res", keepsPredefinedFile ? null : restriction);
    copy.put("Act", active);
    maintainService.launchPrivateMaintain(CREATE_MAINTAIN, copy);
    replaceAssignments(Integer.valueOf(copy.get(PROFILE_ID).asString()), permissions);
    return new ServiceData();
  }

  private void replaceAssignments(int profileId, Set<String> permissions) throws AWException {
    ObjectNode assignments = queryUtil.getParameters(JsonNodeFactory.instance.objectNode());
    assignments.put(PROFILE_ID, profileId);
    ArrayNode selected = assignments.putArray(PERMISSIONS);
    permissions.forEach(selected::add);
    maintainService.launchPrivateMaintain(ASSIGNMENTS_MAINTAIN, assignments);
  }

  /**
   * Whether the session user can read the permissions of the profiles: always, unless the authorization mode is
   * enforce and the user cannot launch the query of the assignments
   */
  private boolean canRead() {
    return !isEnforced() || permissionTargetAuthorizer.canLaunch(ASSIGNMENTS_QUERY, TargetKind.QUERY);
  }

  private boolean isEnforced() {
    return securityConfigProperties.getAuthorization().getMode() == AuthorizationMode.ENFORCE;
  }

  private static List<Integer> toIds(List<Object> profileIds) {
    return profileIds == null ? List.of() : profileIds.stream()
      .filter(Objects::nonNull)
      .map(id -> String.valueOf(id).trim())
      .filter(id -> !id.isEmpty())
      .map(Integer::valueOf)
      .toList();
  }

  private List<StoredProfile> readProfiles(List<Object> profileIds) throws AWException {
    List<Integer> ids = toIds(profileIds);
    if (ids.isEmpty()) {
      return List.of();
    }
    ObjectNode parameters = allRows();
    ArrayNode selected = parameters.putArray(PROFILE_ID);
    ids.forEach(selected::add);
    return queryService.launchPrivateQuery(PROFILES_QUERY, parameters).getDataList().getRows().stream()
      .map(row -> new StoredProfile(row.get(PROFILE_ID).getIntegerValue(), text(row.get("Acr")), text(row.get("Res"))))
      .toList();
  }

  private List<String> readAssignments(int profileId) throws AWException {
    ObjectNode parameters = allRows();
    parameters.put(PROFILE_ID, profileId);
    return readValues(ASSIGNMENTS_LIST_QUERY, parameters);
  }

  private List<String> readValues(String query, ObjectNode parameters) throws AWException {
    return queryService.launchPrivateQuery(query, parameters).getDataList().getRows().stream()
      .map(row -> text(row.get(VALUE)))
      .filter(Objects::nonNull)
      .toList();
  }

  /**
   * Parameters of a query read whole: first page without a page size
   */
  private ObjectNode allRows() {
    return queryUtil.getParameters(JsonNodeFactory.instance.objectNode(), null, "1", "0");
  }

  private static Optional<StoredProfile> findPredefined(List<StoredProfile> profiles) {
    return profiles.stream().filter(profile -> predefinedOf(profile).isPresent()).findFirst();
  }

  private static Optional<PredefinedProfile> predefinedOf(StoredProfile profile) {
    return PredefinedProfiles.find(profile.acronym(), profile.restriction());
  }

  private AWException predefinedProfileError(String acronym) {
    return new AWException(getLocale("ERROR_TITLE_PREDEFINED_PROFILE"),
      getLocale("ERROR_MESSAGE_PREDEFINED_PROFILE", acronym));
  }

  private PermissionCatalog getPermissionCatalog() {
    AweElements elements = getElements();
    PermissionCatalog catalog = elements == null ? null : elements.getPermissionCatalog();
    return catalog == null ? PermissionCatalog.empty() : catalog;
  }

  private static void addOption(DataList options, String value, String label, String group) {
    Map<String, CellData> row = new HashMap<>();
    row.put(VALUE, new CellData(value));
    row.put(LABEL, new CellData(label));
    row.put(GROUP, new CellData(group));
    options.addRow(row);
  }

  private static String text(CellData cell) {
    String value = cell == null ? null : cell.getStringValue();
    return value == null || value.isBlank() ? null : value.trim();
  }

  /**
   * Stored profile
   *
   * @param id          Profile identifier
   * @param acronym     Acronym
   * @param restriction Profile file
   */
  private record StoredProfile(Integer id, String acronym, String restriction) {
  }
}
