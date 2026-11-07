package com.almis.awe.service.access;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.entities.access.Profile;
import com.almis.awe.model.entities.access.Restriction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.type.RestrictionType;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessCascade.Rule;
import com.almis.awe.service.menu.MenuProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.almis.awe.model.constant.AweConstants.COMPONENT_MAX;
import static com.almis.awe.model.constant.AweConstants.PRIVATE_MENU;
import static com.almis.awe.service.access.AccessData.DENY;
import static com.almis.awe.service.access.AccessData.FULL_NAME;
import static com.almis.awe.service.access.AccessData.OPTION;
import static com.almis.awe.service.access.AccessData.PROFILE;
import static com.almis.awe.service.access.AccessData.PROFILE_ID;
import static com.almis.awe.service.access.AccessData.RESTRICTION;
import static com.almis.awe.service.access.AccessData.SUBJECT_PROFILE;
import static com.almis.awe.service.access.AccessData.SUBJECT_USER;
import static com.almis.awe.service.access.AccessData.USER;
import static com.almis.awe.service.access.AccessData.USER_ID;
import static com.almis.awe.service.access.AccessData.USER_RESTRICTION;
import static com.almis.awe.service.access.AccessData.allRows;
import static com.almis.awe.service.access.AccessData.integerOf;
import static com.almis.awe.service.access.AccessData.stringOf;

/**
 * Subjects of the access window (access-admin) and their rules: a profile, or a user with its profile, with the same
 * rules as the real menu of a session: the base profile file of the subject, the global rules, the rules of its profile
 * and its own rules (AweScrRes). The services of the window evaluate the access of a subject with {@link AccessCascade}
 * through this one.
 */
@Slf4j
public class AccessSubjects extends ServiceConfig {

  // Rules of a subject in the order the real menu applies them (menu-restrictions has no order)
  static final String SUBJECT_RULES_QUERY = "access-subject-rules";
  private static final String SUBJECT_USER_QUERY = "access-subject-user";
  private static final String SUBJECT_PROFILE_QUERY = "access-subject-profile";
  private static final String RULE_QUERY = "access-rule";
  // Profiles and users to compare with whose names contain a text
  static final String COMPARE_PROFILES_QUERY = "access-compare-profiles";
  static final String COMPARE_USERS_QUERY = "access-compare-users";
  // A suggest shows a page of options: more profiles or users than these are not read
  private static final int COMPARE_CANDIDATES_MAX = 50;
  private static final String SEARCH = "search";
  private static final String NAME = "name";

  private final MenuProvider menuProvider;
  private final QueryService queryService;
  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Autowired constructor
   *
   * @param menuProvider             Menu provider
   * @param queryService             Query service
   * @param securityConfigProperties Security configuration properties
   */
  public AccessSubjects(MenuProvider menuProvider, QueryService queryService,
                        SecurityConfigProperties securityConfigProperties) {
    this.menuProvider = menuProvider;
    this.queryService = queryService;
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Subject of the window: a profile, or a user with its profile
   *
   * @param layer          Layer of its own rules (PROFILE or USER)
   * @param user           User identifier (users only)
   * @param profile        Profile identifier (null for a user without profile)
   * @param userName       User name (users only)
   * @param fullName       Full name of the user (users only)
   * @param profileAcronym Profile acronym
   * @param restriction    Base profile file (restriction) of the subject
   * @param ownRestriction Own base profile file of the user, which wins over the one of its profile (users only)
   */
  record Subject(Layer layer, Integer user, Integer profile, String userName, String fullName,
                 String profileAcronym, String restriction, String ownRestriction) {

    /**
     * A user rule has no profile and a profile rule has no user, as the real menu reads them
     *
     * @return User of the own rules of the subject
     */
    Integer ruleUser() {
      return layer == Layer.USER ? user : null;
    }

    /**
     * A profile rule has no user
     *
     * @return Profile of the own rules of the subject
     */
    Integer ruleProfile() {
      return layer == Layer.USER ? null : profile;
    }
  }

  /**
   * Profile or user to compare with, as the compare-with suggest sends it: profile:IdePro or user:IdeOpe
   *
   * @param type Subject type: profile or user
   * @param id   Profile or user identifier
   */
  record CompareWith(String type, Integer id) {

    private static final Pattern VALUE = Pattern.compile("(" + SUBJECT_PROFILE + "|" + SUBJECT_USER + "):(\\d{1,9})");

    /**
     * Read a compare-with value
     *
     * @param value Value (optional)
     * @return Profile or user, empty for a blank value or one that is not profile:number or user:number
     */
    static Optional<CompareWith> parse(String value) {
      Matcher matcher = VALUE.matcher(StringUtils.defaultString(value));
      return matcher.matches() ? Optional.of(new CompareWith(matcher.group(1), Integer.valueOf(matcher.group(2))))
        : Optional.empty();
    }

    boolean isUser() {
      return SUBJECT_USER.equals(type);
    }

    /**
     * @return Value of the suggest
     */
    String value() {
      return type + ":" + id;
    }
  }

  /**
   * Profile or user to compare with, as an option of the compare-with suggest
   *
   * @param target Profile or user
   * @param code   Profile acronym or user name
   * @param name   Profile name or full name of the user (optional)
   */
  record CompareCandidate(CompareWith target, String code, String name) {
  }

  /**
   * The private menu
   *
   * @return Private menu
   * @throws AWException Error retrieving the menu
   */
  Menu getMenu() throws AWException {
    return menuProvider.getMenu(PRIVATE_MENU);
  }

  /**
   * Resolve the subject: a user with its profile and its base restriction (its own, else the one of its profile, else
   * the default), or a profile with its restriction (else the default)
   *
   * @param subjectType Subject type: profile or user (a user when blank and a user is given)
   * @param user        User identifier
   * @param profile     Profile identifier
   * @return Subject, empty when it is not given or does not exist
   * @throws AWException Error retrieving data
   */
  Optional<Subject> get(String subjectType, Integer user, Integer profile) throws AWException {
    boolean isUser = SUBJECT_USER.equalsIgnoreCase(subjectType) || (StringUtils.isBlank(subjectType) && user != null);
    if (isUser && user != null) {
      ObjectNode parameters = allRows().put(USER, user);
      return queryService.launchPrivateQuery(SUBJECT_USER_QUERY, parameters).getDataList().getRows().stream()
        .findFirst()
        .map(row -> new Subject(Layer.USER, user, integerOf(row.get(PROFILE_ID)), stringOf(row.get(USER)),
          stringOf(row.get(FULL_NAME)), stringOf(row.get(PROFILE)),
          Stream.of(stringOf(row.get(USER_RESTRICTION)), stringOf(row.get("profileRestriction")))
            .filter(StringUtils::isNotBlank)
            .findFirst().orElse(securityConfigProperties.getDefaultRestriction()),
          stringOf(row.get(USER_RESTRICTION))));
    } else if (!isUser && profile != null) {
      ObjectNode parameters = allRows().put(PROFILE, profile);
      return queryService.launchPrivateQuery(SUBJECT_PROFILE_QUERY, parameters).getDataList().getRows().stream()
        .findFirst()
        .map(row -> new Subject(Layer.PROFILE, null, profile, null, null, stringOf(row.get(PROFILE)),
          Optional.ofNullable(stringOf(row.get(RESTRICTION))).filter(StringUtils::isNotBlank)
            .orElse(securityConfigProperties.getDefaultRestriction()), null));
    }
    return Optional.empty();
  }

  /**
   * Resolve the subject of a change of its own rules
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @return Subject
   * @throws AWException Without subject
   */
  Subject getToChange(String subjectType, Integer user, Integer profile) throws AWException {
    return get(subjectType, user, profile)
      .orElseThrow(() -> new AWException(getLocale("ERROR_TITLE_ACCESS_NO_SUBJECT"),
        getLocale("ERROR_MESSAGE_ACCESS_NO_SUBJECT")));
  }

  /**
   * Subject to compare with, as the compare-with suggest sends it (profile:IdePro or user:IdeOpe): another profile, or the
   * user with another profile, which keeps its own base file (it wins over the one of the profile, as in its real menu)
   * and its own rules; or another user, with its real profile, its base file and its own rules
   *
   * @param subject     Subject
   * @param compareWith Profile or user to compare with (optional)
   * @return Subject to compare with, empty without it or with a value that is not a profile or a user
   * @throws AWException Error retrieving data
   */
  Optional<Subject> getCompared(Subject subject, String compareWith) throws AWException {
    Optional<CompareWith> target = CompareWith.parse(compareWith);
    if (target.isEmpty()) {
      return Optional.empty();
    } else if (target.get().isUser()) {
      return get(SUBJECT_USER, target.get().id(), null);
    }
    Integer compareProfile = target.get().id();
    Optional<Subject> other = get(SUBJECT_PROFILE, null, compareProfile);
    if (other.isEmpty() || subject.layer() == Layer.PROFILE) {
      return other;
    }
    String restriction = StringUtils.isNotBlank(subject.ownRestriction()) ? subject.ownRestriction()
      : other.get().restriction();
    return Optional.of(new Subject(Layer.USER, subject.user(), compareProfile, subject.userName(), subject.fullName(),
      other.get().profileAcronym(), restriction, subject.ownRestriction()));
  }

  /**
   * Profiles and users to compare with: the selected one (profile:IdePro or user:IdeOpe), or the profiles whose acronym or
   * name and the users whose user or full name contain the text (all of them without text), profiles first
   *
   * @param suggest Text to search, or the selected value
   * @return Profiles and users to compare with
   * @throws AWException Error retrieving data
   */
  List<CompareCandidate> getCompareCandidates(String suggest) throws AWException {
    Optional<CompareWith> selected = CompareWith.parse(suggest);
    if (selected.isPresent()) {
      return getCompareCandidate(selected.get()).stream().toList();
    }
    // Without text (the arrow of the suggest) the search lists every profile and user, up to the maximum per type
    ObjectNode parameters = allRows().put(COMPONENT_MAX, COMPARE_CANDIDATES_MAX).put(SEARCH, StringUtils.trimToEmpty(suggest));
    List<CompareCandidate> candidates = new ArrayList<>();
    queryService.launchPrivateQuery(COMPARE_PROFILES_QUERY, parameters).getDataList().getRows()
      .forEach(row -> candidates.add(new CompareCandidate(new CompareWith(SUBJECT_PROFILE, integerOf(row.get(PROFILE_ID))),
        stringOf(row.get(PROFILE)), stringOf(row.get(NAME)))));
    queryService.launchPrivateQuery(COMPARE_USERS_QUERY, parameters).getDataList().getRows()
      .forEach(row -> candidates.add(new CompareCandidate(new CompareWith(SUBJECT_USER, integerOf(row.get(USER_ID))),
        stringOf(row.get(USER)), stringOf(row.get(FULL_NAME)))));
    return candidates;
  }

  /**
   * Name of a profile
   *
   * @param profile Profile identifier
   * @return Profile name, empty for a null or unknown profile
   * @throws AWException Error retrieving data
   */
  Optional<String> getProfileName(Integer profile) throws AWException {
    if (profile == null) {
      return Optional.empty();
    }
    ObjectNode parameters = allRows().put(PROFILE, profile);
    return queryService.launchPrivateQuery(SUBJECT_PROFILE_QUERY, parameters).getDataList().getRows().stream()
      .findFirst()
      .map(row -> stringOf(row.get(NAME)));
  }

  private Optional<CompareCandidate> getCompareCandidate(CompareWith selected) throws AWException {
    String query = selected.isUser() ? SUBJECT_USER_QUERY : SUBJECT_PROFILE_QUERY;
    ObjectNode parameters = allRows().put(selected.isUser() ? USER : PROFILE, selected.id());
    return queryService.launchPrivateQuery(query, parameters).getDataList().getRows().stream()
      .findFirst()
      .map(row -> selected.isUser()
        ? new CompareCandidate(selected, stringOf(row.get(USER)), stringOf(row.get(FULL_NAME)))
        : new CompareCandidate(selected, stringOf(row.get(PROFILE)), stringOf(row.get(NAME))));
  }

  /**
   * Evaluate the cascade for the subject on the private menu
   *
   * @param subject Subject
   * @param module  Selected module (optional)
   * @return Access of every option
   * @throws AWException Error retrieving data
   */
  Result evaluate(Subject subject, String module) throws AWException {
    return evaluate(subject, module, getMenu());
  }

  /**
   * Evaluate the cascade for the subject on a menu
   *
   * @param subject Subject
   * @param module  Selected module (optional)
   * @param menu    Menu
   * @return Access of every option
   * @throws AWException Error retrieving data
   */
  Result evaluate(Subject subject, String module, Menu menu) throws AWException {
    return AccessCascade.evaluate(menu, StringUtils.isBlank(module) ? null : module, getRules(subject),
      subject.layer());
  }

  /**
   * Rules of the subject: its base profile file, the global rules, the rules of its profile and, for a user, its own
   *
   * @param subject Subject
   * @return Rules, in the order of the real menu
   * @throws AWException Error retrieving data
   */
  List<Rule> getRules(Subject subject) throws AWException {
    List<Rule> rules = new ArrayList<>(getBaseRules(subject.restriction()));
    rules.addAll(toRules(Layer.GLOBAL, getRules(null, null, null)));
    if (subject.profile() != null) {
      rules.addAll(toRules(Layer.PROFILE, getRules(null, subject.profile(), null)));
    }
    if (subject.layer() == Layer.USER) {
      rules.addAll(toRules(Layer.USER, getRules(subject.user(), null, null)));
    }
    return rules;
  }

  /**
   * Rules of the base profile file; an unknown file has none
   *
   * @param restriction Base profile file
   * @return Rules
   */
  List<Rule> getBaseRules(String restriction) {
    Profile baseProfile;
    try {
      baseProfile = getElements().getProfile(restriction);
    } catch (AWException exc) {
      log.warn("Base profile {} not found", restriction, exc);
      baseProfile = null;
    }
    if (baseProfile == null || baseProfile.getRestrictions() == null) {
      return Collections.emptyList();
    }
    List<Rule> rules = new ArrayList<>();
    for (Restriction baseRestriction : baseProfile.getRestrictions()) {
      rules.add(new Rule(Layer.BASE_PROFILE, baseRestriction.getOption(),
        RestrictionType.R.equals(baseRestriction.getRestrictionType())));
    }
    return rules;
  }

  /**
   * Active database rules of a user, a profile or global (both null), optionally of one option. The option is always
   * given, null for every option: a variable missing from the parameters is read from the request (an AngularJS screen
   * sends an option parameter), and the cacheable query would keep that result under the key of these parameters
   *
   * @param user    User of the rules
   * @param profile Profile of the rules
   * @param option  Option of the rules (null for every option)
   * @return Rules (option and restriction), in the order of the real menu
   * @throws AWException Error retrieving data
   */
  List<Map<String, CellData>> getRules(Integer user, Integer profile, String option) throws AWException {
    ObjectNode parameters = allRows();
    parameters.put(USER, user);
    parameters.put(PROFILE, profile);
    parameters.put(OPTION, option);
    return queryService.launchPrivateQuery(SUBJECT_RULES_QUERY, parameters).getDataList().getRows();
  }

  /**
   * A rule by identifier: its user, its profile, its option and its rule (none for a null or unknown identifier)
   *
   * @param rule Rule identifier (IdeAweScrRes)
   * @return Rule
   * @throws AWException Error retrieving data
   */
  Optional<Map<String, CellData>> getRule(Integer rule) throws AWException {
    if (rule == null) {
      return Optional.empty();
    }
    ObjectNode parameters = allRows().put("rule", rule);
    return queryService.launchPrivateQuery(RULE_QUERY, parameters).getDataList().getRows().stream().findFirst();
  }

  private static List<Rule> toRules(Layer layer, List<Map<String, CellData>> rows) {
    return rows.stream()
      .map(row -> new Rule(layer, stringOf(row.get(OPTION)), DENY.equals(stringOf(row.get(RESTRICTION)))))
      .toList();
  }
}
