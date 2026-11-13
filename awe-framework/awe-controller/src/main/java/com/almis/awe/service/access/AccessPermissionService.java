package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.dto.UserProfile;
import com.almis.awe.model.entities.permissions.EffectivePermissions;
import com.almis.awe.model.entities.permissions.EffectivePermissions.Exclusion;
import com.almis.awe.model.entities.permissions.EffectivePermissions.Origin;
import com.almis.awe.model.entities.permissions.EffectivePermissions.OriginKind;
import com.almis.awe.model.entities.permissions.EffectivePermissions.ProfileSource;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessSubjects.CompareWith;
import com.almis.awe.service.access.AccessSubjects.Subject;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.service.user.PredefinedProfiles;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.service.access.AccessData.DESCRIPTION;
import static com.almis.awe.service.access.AccessData.FALSE;
import static com.almis.awe.service.access.AccessData.ICON;
import static com.almis.awe.service.access.AccessData.ICON_TONE;
import static com.almis.awe.service.access.AccessData.INFO;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.SEPARATOR;
import static com.almis.awe.service.access.AccessData.SUBJECT_PROFILE;
import static com.almis.awe.service.access.AccessData.SUBJECT_USER;
import static com.almis.awe.service.access.AccessData.SUCCESS;
import static com.almis.awe.service.access.AccessData.TITLE;
import static com.almis.awe.service.access.AccessData.TRUE;
import static com.almis.awe.service.access.AccessData.WARNING;
import static com.almis.awe.service.access.AccessData.WARNING_ICON;
import static com.almis.awe.service.access.AccessData.asServiceData;

/**
 * Permissions view of the access window (access-admin): the effective permissions of a user (main profile plus
 * additional profiles) or of a profile, one row per permission of the catalog grouped by resource, with its state, its
 * origins and the exclusion of a profile file that another origin overrides; the counts of its quick filters, its
 * summary, the comparison with another user or profile and why a permission is granted, profile by profile. They are
 * the permissions the login of the subject loads (AweUserDetailService), as configured now: a change applies at the
 * next login. Nothing is stored.
 */
public class AccessPermissionService {

  static final String FILTER_GRANTED = "granted";
  static final String FILTER_NOT_GRANTED = "not-granted";
  static final String FILTER_WARNINGS = "warnings";
  static final String FILTER_DIFFERENCES = "differences";
  private static final String TEXT = PREFIX + "PERMISSION_";
  private static final String NEUTRAL = "neutral";
  // Origins are shown as chips: one chip per origin
  private static final String CHIP_SEPARATOR = "|";
  private static final String DIFFERS_ICON = "lucide:arrow-left-right";

  private final AccessSubjects subjects;
  private final AccessTexts texts;
  private final ObjectProvider<AweUserDetailService> userDetailService;

  /**
   * Profile of a subject
   *
   * @param acronym Profile acronym
   * @param name    Profile name
   * @param main    Whether it is the main profile of a user, or the profile subject
   */
  record SubjectProfile(String acronym, String name, boolean main) {
  }

  /**
   * Effective permissions of a subject
   *
   * @param subject     Subject
   * @param profiles    Profiles of the subject, main profile first
   * @param permissions Effective permissions with their provenance
   */
  record SubjectPermissions(Subject subject, List<SubjectProfile> profiles, EffectivePermissions permissions) {
  }

  /**
   * Autowired constructor
   *
   * @param subjects          Subjects of the access window
   * @param texts             Texts of the access window
   * @param userDetailService User detail service: the effective permissions as the login computes them
   */
  public AccessPermissionService(AccessSubjects subjects, AccessTexts texts,
                                 ObjectProvider<AweUserDetailService> userDetailService) {
    this.subjects = subjects;
    this.texts = texts;
    this.userDetailService = userDetailService;
  }

  /**
   * Retrieve one row per permission of the catalog with the state of the subject, its origins and its warning, grouped
   * by resource, filtered by the quick filter and the search
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param filter      Quick filter: "", granted, not-granted, warnings or differences
   * @param search      Search on the identifier or the label of the permission or its resource (optional)
   * @param compareWith Profile or user to compare with: profile:IdePro or user:IdeOpe (optional). It adds its state and
   *                    why it changes
   * @return Permission rows (empty without subject)
   * @throws AWException Error retrieving data
   */
  public ServiceData getPermissions(String subjectType, Integer user, Integer profile, String filter, String search,
                                    String compareWith) throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<SubjectPermissions> subject = getSubjectPermissions(subjectType, user, profile);
    if (subject.isPresent()) {
      SubjectPermissions compared = getCompared(compareWith).orElse(null);
      PermissionCatalog catalog = getCatalog();
      Map<String, Integer> resources = getResourcePositions(catalog);
      Predicate<String> matches = matchesFilter(filter, subject.get(), compared).and(matchesSearch(search, catalog));
      for (String permission : catalog.getPermissionIds()) {
        if (matches.test(permission)) {
          Map<String, CellData> row = getPermissionRow(permission, subject.get(), catalog, resources);
          if (compared != null) {
            addCompareColumns(row, permission, subject.get(), compared);
          }
          rows.add(row);
        }
      }
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the options of the quick filter with the number of permissions of each one, with the search. The counts are
   * the ones of the subject, also while comparing; the comparison adds the number of differences
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param search      Search (optional)
   * @param compareWith Profile or user to compare with (optional)
   * @return Quick filter options: pos, value, label, count and tone
   * @throws AWException Error retrieving data
   */
  public ServiceData getPermissionCounts(String subjectType, Integer user, Integer profile, String search,
                                         String compareWith) throws AWException {
    Optional<SubjectPermissions> subject = getSubjectPermissions(subjectType, user, profile);
    SubjectPermissions compared = subject.isPresent() ? getCompared(compareWith).orElse(null) : null;
    PermissionCatalog catalog = getCatalog();
    List<String> permissions = subject.isPresent()
      ? catalog.getPermissionIds().stream().filter(matchesSearch(search, catalog)).toList() : List.of();
    List<Map<String, CellData>> rows = new ArrayList<>();
    rows.add(getCountRow(1, "", "FILTER_ALL", permissions, "", subject.orElse(null), compared));
    rows.add(getCountRow(2, FILTER_GRANTED, "FILTER_GRANTED", permissions, SUCCESS, subject.orElse(null), compared));
    rows.add(getCountRow(3, FILTER_NOT_GRANTED, "FILTER_NOT_GRANTED", permissions, NEUTRAL, subject.orElse(null),
      compared));
    rows.add(getCountRow(4, FILTER_WARNINGS, "FILTER_WARNINGS", permissions, WARNING, subject.orElse(null), compared));
    if (compared != null) {
      rows.add(getCountRow(5, FILTER_DIFFERENCES, "FILTER_DIFFERENCES", permissions, INFO, subject.get(), compared));
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the summary of the subject as a card: how many permissions it has of the catalog, its profiles and its
   * warnings; while comparing, how many permissions differ and how many each one has alone
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param compareWith Profile or user to compare with (optional)
   * @return One row (title, description, icon and iconTone), none without subject
   * @throws AWException Error retrieving data
   */
  public ServiceData getPermissionSummary(String subjectType, Integer user, Integer profile, String compareWith)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<SubjectPermissions> found = getSubjectPermissions(subjectType, user, profile);
    if (found.isPresent()) {
      SubjectPermissions subject = found.get();
      List<String> permissions = getCatalog().getPermissionIds();
      Optional<SubjectPermissions> compared = getCompared(compareWith);
      Map<String, CellData> row = new HashMap<>();
      if (compared.isPresent()) {
        long differences = permissions.stream().filter(permission -> differs(permission, subject, compared.get())).count();
        long onlySubject = permissions.stream().filter(permission -> isGranted(subject, permission)
          && !isGranted(compared.get(), permission)).count();
        row.put(TITLE, new CellData(locale(TEXT + "SUMMARY_DIFFERENCES", differences, permissions.size())));
        row.put(DESCRIPTION, new CellData(locale(TEXT + "SUMMARY_COMPARE",
          AccessTexts.getSubjectName(subject.subject()), onlySubject,
          AccessTexts.getSubjectName(compared.get().subject()), differences - onlySubject,
          permissions.size() - differences)));
        row.put(ICON, new CellData("lucide:git-compare"));
        row.put(ICON_TONE, new CellData(INFO));
      } else {
        long granted = permissions.stream().filter(permission -> isGranted(subject, permission)).count();
        long warnings = permissions.stream().filter(permission -> hasWarning(subject, permission)).count();
        List<String> description = new ArrayList<>();
        // Composed here: a parameter of a locale is escaped as HTML
        description.add(locale(TEXT + "SUMMARY_PROFILES") + " " + subject.profiles().stream()
          .map(this::getProfileText).collect(Collectors.joining(", ")));
        if (warnings > 0) {
          description.add(locale(TEXT + "SUMMARY_WARNINGS", warnings));
        }
        row.put(TITLE, new CellData(locale(TEXT + "SUMMARY_GRANTED", granted, permissions.size())));
        row.put(DESCRIPTION, new CellData(String.join(SEPARATOR, description)));
        row.put(ICON, new CellData("lucide:shield-check"));
        row.put(ICON_TONE, new CellData(SUCCESS));
      }
      rows.add(row);
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve why a permission is granted or not, profile by profile: what its profile file and its assignments give,
   * and whether the profile grants it; then the catalog, when the permission is granted always
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param permission  Permission identifier
   * @return Rows: why-profile, why-profile-sub, why-file, why-assignment, why-verdict-text and why-verdict-tone (none without
   * subject or for a permission out of the catalog)
   * @throws AWException Error retrieving data
   */
  public ServiceData getPermissionWhy(String subjectType, Integer user, Integer profile, String permission)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<SubjectPermissions> subject = getSubjectPermissions(subjectType, user, profile);
    PermissionCatalog catalog = getCatalog();
    if (subject.isPresent() && catalog.contains(permission)) {
      EffectivePermissions permissions = subject.get().permissions();
      for (SubjectProfile subjectProfile : subject.get().profiles()) {
        permissions.getProfiles().stream()
          .filter(source -> source.profile().equals(subjectProfile.acronym()))
          .findFirst()
          .ifPresent(source -> rows.add(getWhyRow(permission, subject.get(), subjectProfile, source)));
      }
      if (catalog.getGrantedAlways().contains(permission)) {
        Map<String, CellData> row = new HashMap<>();
        row.put("why-profile", new CellData(locale(TEXT + "ORIGIN_ALWAYS")));
        row.put("why-profile-sub", new CellData(locale(TEXT + "WHY_CATALOG")));
        row.put("why-file", new CellData(locale(TEXT + "WHY_ALWAYS")));
        row.put("why-assignment", new CellData(""));
        putVerdict(row, true);
        rows.add(row);
      }
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve whether the subject has a permission and what gives it, as the card of the side panel
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param permission  Permission identifier
   * @return One row (title, description, icon and iconTone), none without subject or permission
   * @throws AWException Error retrieving data
   */
  public ServiceData getPermissionWhyResult(String subjectType, Integer user, Integer profile, String permission)
    throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<SubjectPermissions> subject = getSubjectPermissions(subjectType, user, profile);
    PermissionCatalog catalog = getCatalog();
    if (subject.isPresent() && catalog.contains(permission)) {
      boolean granted = isGranted(subject.get(), permission);
      Map<String, CellData> row = new HashMap<>();
      row.put(TITLE, new CellData(locale(TEXT + (granted ? "RESULT_GRANTED" : "RESULT_NOT_GRANTED"),
        AccessTexts.getSubjectName(subject.get().subject()), getResourceLabel(catalog, permission),
        locale(catalog.getLabel(permission)))));
      row.put(DESCRIPTION, new CellData(getReason(permission, subject.get()) + SEPARATOR
        + locale(TEXT + "RESULT_HINT")));
      row.put(ICON, new CellData(granted ? "lucide:shield-check" : "lucide:shield-off"));
      row.put(ICON_TONE, new CellData(granted ? SUCCESS : NEUTRAL));
      rows.add(row);
    }
    return asServiceData(rows);
  }

  /**
   * Profiles and effective permissions of the subject: a user with its main profile and its additional profiles, or a
   * profile
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @return Subject permissions, empty when the subject is not given or does not exist
   * @throws AWException Error retrieving data
   */
  Optional<SubjectPermissions> getSubjectPermissions(String subjectType, Integer user, Integer profile)
    throws AWException {
    Optional<Subject> subject = subjects.get(subjectType, user, profile);
    if (subject.isEmpty()) {
      return Optional.empty();
    }
    List<SubjectProfile> profiles = new ArrayList<>();
    if (StringUtils.isNotBlank(subject.get().profileAcronym())) {
      profiles.add(new SubjectProfile(subject.get().profileAcronym(),
        subjects.getProfileName(subject.get().profile()).orElse(""), true));
    }
    if (subject.get().layer() == Layer.USER) {
      for (UserProfile additional : userDetailService.getObject().getAdditionalProfiles(subject.get().userName())) {
        profiles.add(new SubjectProfile(additional.acronym(), StringUtils.defaultString(additional.name()), false));
      }
    }
    EffectivePermissions permissions = userDetailService.getObject()
      .getEffectivePermissionDetail(profiles.stream().map(SubjectProfile::acronym).toList());
    return Optional.of(new SubjectPermissions(subject.get(), profiles, permissions));
  }

  /**
   * The user or the profile to compare with, with its own profiles
   */
  private Optional<SubjectPermissions> getCompared(String compareWith) throws AWException {
    Optional<CompareWith> target = CompareWith.parse(compareWith);
    if (target.isEmpty()) {
      return Optional.empty();
    }
    return target.get().isUser() ? getSubjectPermissions(SUBJECT_USER, target.get().id(), null)
      : getSubjectPermissions(SUBJECT_PROFILE, null, target.get().id());
  }

  private PermissionCatalog getCatalog() {
    AweElements elements = texts.getElements();
    PermissionCatalog catalog = elements == null ? null : elements.getPermissionCatalog();
    return catalog == null ? PermissionCatalog.empty() : catalog;
  }

  /**
   * Position of every resource of the catalog, in the order of its first permission
   */
  private static Map<String, Integer> getResourcePositions(PermissionCatalog catalog) {
    Map<String, Integer> positions = new LinkedHashMap<>();
    catalog.getPermissionIds().forEach(permission -> positions.putIfAbsent(getResource(permission),
      positions.size() + 1));
    return positions;
  }

  private static String getResource(String permission) {
    return permission.substring(0, permission.indexOf(PermissionCatalog.SEPARATOR));
  }

  private static boolean isGranted(SubjectPermissions subject, String permission) {
    return subject.permissions().isGranted(permission);
  }

  /**
   * A profile file excludes the permission and another origin grants it
   */
  private static boolean hasWarning(SubjectPermissions subject, String permission) {
    return isGranted(subject, permission) && !subject.permissions().getExclusions(permission).isEmpty();
  }

  private static boolean differs(String permission, SubjectPermissions subject, SubjectPermissions compared) {
    return isGranted(subject, permission) != isGranted(compared, permission);
  }

  /**
   * Permissions of a quick filter; only differences filters nothing without a comparison
   */
  private static Predicate<String> matchesFilter(String filter, SubjectPermissions subject,
                                                 SubjectPermissions compared) {
    return switch (Optional.ofNullable(filter).orElse("")) {
      case FILTER_GRANTED -> permission -> isGranted(subject, permission);
      case FILTER_NOT_GRANTED -> permission -> !isGranted(subject, permission);
      case FILTER_WARNINGS -> permission -> hasWarning(subject, permission);
      case FILTER_DIFFERENCES -> compared == null ? permission -> true
        : permission -> differs(permission, subject, compared);
      default -> permission -> true;
    };
  }

  private Predicate<String> matchesSearch(String search, PermissionCatalog catalog) {
    if (StringUtils.isBlank(search)) {
      return permission -> true;
    }
    String text = search.trim();
    return permission -> StringUtils.containsIgnoreCase(permission, text)
      || StringUtils.containsIgnoreCase(locale(catalog.getLabel(permission)), text)
      || StringUtils.containsIgnoreCase(getResourceLabel(catalog, permission), text);
  }

  private String getResourceLabel(PermissionCatalog catalog, String permission) {
    String resource = getResource(permission);
    return Optional.ofNullable(catalog.getResourceLabel(resource)).map(label -> locale(label)).orElse(resource);
  }

  /**
   * Text of a locale key in the language of the session, with its parameters
   */
  private String locale(String key, Object... parameters) {
    if (parameters.length == 0) {
      return texts.getLocale(key);
    }
    return texts.getLocale(key, Arrays.stream(parameters).map(String::valueOf).toArray(String[]::new));
  }

  private Map<String, CellData> getPermissionRow(String permission, SubjectPermissions subject,
                                                 PermissionCatalog catalog, Map<String, Integer> resources) {
    boolean granted = isGranted(subject, permission);
    String resource = getResource(permission);
    List<String> resourcePermissions = catalog.getPermissionIds().stream()
      .filter(id -> getResource(id).equals(resource)).toList();
    long resourceGranted = resourcePermissions.stream().filter(id -> isGranted(subject, id)).count();
    Map<String, CellData> row = new HashMap<>();
    row.put("permission-id", new CellData(permission));
    row.put("permission-name", new CellData(locale(catalog.getLabel(permission))));
    row.put("permission-key", new CellData(permission));
    // Title of the side panel: the resource and the verb or action
    row.put("permission-title", new CellData(getResourceLabel(catalog, permission) + SEPARATOR
      + locale(catalog.getLabel(permission))));
    row.put("permission-subtitle", new CellData(permission + SEPARATOR
      + AccessTexts.getSubjectName(subject.subject())));
    // The group of the resource: its position keeps the order of the catalog
    row.put("permission-resource", new CellData(JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, String.format("%03d", resources.get(resource)))
      .put(JSON_LABEL_PARAMETER, locale(TEXT + "GROUP", getResourceLabel(catalog, permission), resource,
        resourceGranted, resourcePermissions.size()))));
    row.put("permission-state", new CellData(granted ? FILTER_GRANTED : FILTER_NOT_GRANTED));
    row.put("permission-state-text", new CellData(getStateText(granted)));
    row.put("permission-state-tone", new CellData(getStateTone(granted)));
    row.put("permission-origins", new CellData(getOrigins(permission, subject).stream().map(this::getOriginText)
      .collect(Collectors.joining(CHIP_SEPARATOR))));
    row.put("permission-warning", new CellData(getWarning(permission, subject)));
    row.put("permission-warning-icon", new CellData(hasWarning(subject, permission) ? WARNING_ICON : ""));
    return row;
  }

  /**
   * Origins of a granted permission; a permission granted always shows only that origin
   */
  private static List<Origin> getOrigins(String permission, SubjectPermissions subject) {
    List<Origin> origins = subject.permissions().getOrigins(permission);
    return origins.stream().anyMatch(origin -> origin.kind() == OriginKind.ALWAYS)
      ? origins.stream().filter(origin -> origin.kind() == OriginKind.ALWAYS).toList() : origins;
  }

  private String getOriginText(Origin origin) {
    return switch (origin.kind()) {
      case FILE -> locale(TEXT + "ORIGIN_FILE", origin.profile(), origin.file(), origin.entry());
      case ASSIGNMENT -> locale(TEXT + "ORIGIN_ASSIGNMENT", origin.profile(), origin.entry());
      case ALWAYS -> locale(TEXT + "ORIGIN_ALWAYS");
    };
  }

  /**
   * Warning of a permission: the exclusion of a profile file that another origin overrides, or, when it is not
   * granted, the exclusion that removes it
   */
  private String getWarning(String permission, SubjectPermissions subject) {
    List<Exclusion> exclusions = subject.permissions().getExclusions(permission);
    if (exclusions.isEmpty()) {
      return "";
    }
    Exclusion exclusion = exclusions.getFirst();
    return locale(TEXT + (isGranted(subject, permission) ? "WARNING_EXCLUDED" : "EXCLUDED"),
      exclusion.profile(), exclusion.file(), exclusion.entry());
  }

  /**
   * What gives a permission to the subject, or why nothing does
   */
  private String getReason(String permission, SubjectPermissions subject) {
    if (isGranted(subject, permission)) {
      return getOrigins(permission, subject).stream().map(this::getOriginText).collect(Collectors.joining(", "));
    }
    String warning = getWarning(permission, subject);
    return warning.isEmpty() ? locale(TEXT + "NONE") : warning;
  }

  private String getStateText(boolean granted) {
    return locale(TEXT + (granted ? "GRANTED" : "NOT_GRANTED"));
  }

  private static String getStateTone(boolean granted) {
    return granted ? SUCCESS : NEUTRAL;
  }

  /**
   * State with the other user or profile and why it changes ("Same" when it does not)
   */
  private void addCompareColumns(Map<String, CellData> row, String permission, SubjectPermissions subject,
                                 SubjectPermissions compared) {
    boolean granted = isGranted(compared, permission);
    boolean differs = differs(permission, subject, compared);
    row.put("compare-permission-state", new CellData(granted ? FILTER_GRANTED : FILTER_NOT_GRANTED));
    row.put("compare-permission-text", new CellData(getStateText(granted)));
    row.put("compare-permission-tone", new CellData(getStateTone(granted)));
    row.put("compare-permission-differs", new CellData(differs ? TRUE : FALSE));
    row.put("compare-permission-reason", new CellData(differs
      ? AccessTexts.getSubjectName(subject.subject()) + ": " + getReason(permission, subject) + "; "
      + AccessTexts.getSubjectName(compared.subject()) + ": " + getReason(permission, compared)
      : locale(PREFIX + "COMPARE_SAME")));
    row.put("compare-permission-icon", new CellData(differs ? DIFFERS_ICON : ""));
  }

  private static Map<String, CellData> getCountRow(int position, String value, String label, List<String> permissions,
                                                   String tone, SubjectPermissions subject,
                                                   SubjectPermissions compared) {
    Map<String, CellData> row = new HashMap<>();
    row.put("pos", new CellData(position));
    row.put("value", new CellData(value));
    row.put("label", new CellData(TEXT + label));
    row.put("count", new CellData(subject == null ? 0
      : (int) permissions.stream().filter(matchesFilter(value, subject, compared)).count()));
    row.put("tone", new CellData(tone));
    return row;
  }

  /**
   * Profile as a text: its acronym and its name, and "(main)" for the main profile of a user
   */
  private String getProfileText(SubjectProfile profile) {
    String text = StringUtils.isBlank(profile.name()) ? profile.acronym()
      : profile.acronym() + SEPARATOR + profile.name();
    return profile.main() ? text + " " + locale(TEXT + "MAIN") : text;
  }

  /**
   * What a profile gives to a permission: its profile file, its first matching assignment and its verdict
   */
  private Map<String, CellData> getWhyRow(String permission, SubjectPermissions subject, SubjectProfile profile,
                                          ProfileSource source) {
    List<Origin> origins = subject.permissions().getOrigins(permission).stream()
      .filter(origin -> profile.acronym().equals(origin.profile())).toList();
    Optional<Origin> fileOrigin = origins.stream().filter(origin -> origin.kind() == OriginKind.FILE).findFirst();
    Optional<Origin> assignment = origins.stream().filter(origin -> origin.kind() == OriginKind.ASSIGNMENT).findFirst();
    Optional<Exclusion> exclusion = subject.permissions().getExclusions(permission).stream()
      .filter(excluded -> profile.acronym().equals(excluded.profile())).findFirst();
    Map<String, CellData> row = new HashMap<>();
    row.put("why-profile", new CellData(getProfileText(new SubjectProfile(profile.acronym(), profile.name(), false))));
    row.put("why-profile-sub", new CellData(getProfileRole(subject, profile, source)));
    String file;
    if (source.file() == null) {
      file = locale(TEXT + "WHY_NO_FILE");
    } else if (fileOrigin.isPresent()) {
      file = locale(TEXT + "WHY_FILE_GRANTS", source.file(), fileOrigin.get().entry());
    } else if (exclusion.isPresent()) {
      file = locale(TEXT + "WHY_FILE_EXCLUDES", source.file(), exclusion.get().entry());
    } else {
      file = locale(TEXT + "WHY_FILE_NOT_INCLUDED", source.file());
    }
    row.put("why-file", new CellData(file));
    String assignmentText;
    if (assignment.isPresent()) {
      assignmentText = locale(TEXT + "WHY_ASSIGNMENT_GRANTS", assignment.get().entry());
    } else if (source.assignments().isEmpty()) {
      assignmentText = locale(TEXT + "WHY_NO_ASSIGNMENTS");
    } else {
      assignmentText = locale(TEXT + "WHY_ASSIGNMENT_NOT_INCLUDED");
    }
    row.put("why-assignment", new CellData(assignmentText));
    putVerdict(row, !origins.isEmpty());
    return row;
  }

  /**
   * Role of a profile for the subject (main or additional profile of a user, or the profile), and whether it is
   * predefined (it is not edited, it is duplicated)
   */
  private String getProfileRole(SubjectPermissions subject, SubjectProfile profile, ProfileSource source) {
    String role;
    if (subject.subject().layer() != Layer.USER) {
      role = locale("PARAMETER_PROFILE");
    } else {
      role = locale(TEXT + (profile.main() ? "MAIN_PROFILE" : "ADDITIONAL_PROFILE"));
    }
    return PredefinedProfiles.find(profile.acronym(), source.file()).isPresent()
      ? role + SEPARATOR + locale(TEXT + "PREDEFINED") : role;
  }

  private void putVerdict(Map<String, CellData> row, boolean grants) {
    row.put("why-verdict-text", new CellData(locale(TEXT + (grants ? "GRANTS" : "DOES_NOT_GRANT"))));
    row.put("why-verdict-tone", new CellData(grants ? SUCCESS : NEUTRAL));
  }
}
