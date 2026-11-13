package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessCascade.Rule;
import com.almis.awe.service.access.AccessSubjects.Subject;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.almis.awe.service.access.AccessData.CHANGE_ALLOW;
import static com.almis.awe.service.access.AccessData.CHANGE_DENY;
import static com.almis.awe.service.access.AccessData.CHANGE_REMOVE;
import static com.almis.awe.service.access.AccessData.DANGER;
import static com.almis.awe.service.access.AccessData.DENY;
import static com.almis.awe.service.access.AccessData.DESCRIPTION;
import static com.almis.awe.service.access.AccessData.FALSE;
import static com.almis.awe.service.access.AccessData.FULL_NAME;
import static com.almis.awe.service.access.AccessData.ICON;
import static com.almis.awe.service.access.AccessData.ICON_TONE;
import static com.almis.awe.service.access.AccessData.INFO;
import static com.almis.awe.service.access.AccessData.OPTION;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.PROFILE;
import static com.almis.awe.service.access.AccessData.RESTRICTION;
import static com.almis.awe.service.access.AccessData.SEPARATOR;
import static com.almis.awe.service.access.AccessData.SUCCESS;
import static com.almis.awe.service.access.AccessData.TITLE;
import static com.almis.awe.service.access.AccessData.TRUE;
import static com.almis.awe.service.access.AccessData.USER;
import static com.almis.awe.service.access.AccessData.USER_ID;
import static com.almis.awe.service.access.AccessData.USER_RESTRICTION;
import static com.almis.awe.service.access.AccessData.allRows;
import static com.almis.awe.service.access.AccessData.asServiceData;
import static com.almis.awe.service.access.AccessData.distinct;
import static com.almis.awe.service.access.AccessData.integerOf;
import static com.almis.awe.service.access.AccessData.stringOf;

/**
 * Preview of a bulk change of the access window (access-admin): the options whose access changes when the own rule of
 * the subject on some options is allowed, denied or removed, and the users of the subject that lose or gain them or
 * keep them through their own rule. The change is simulated with {@link AccessCascade#change}: nothing is stored.
 */
public class AccessImpactService {

  // Users of a profile and their own rules (preview of a change)
  static final String PROFILE_USERS_QUERY = "access-profile-users";
  static final String PROFILE_USER_RULES_QUERY = "access-profile-user-rules";
  // Users listed by the preview of a change; the rest are counted
  static final int IMPACT_USERS_SHOWN = 10;
  // Option labels listed in a change of a user of the preview; the rest are counted
  private static final int IMPACT_OPTIONS_SHOWN = 3;
  private static final List<String> CHANGES = List.of(CHANGE_ALLOW, CHANGE_DENY, CHANGE_REMOVE);

  private final AccessSubjects subjects;
  private final AccessTexts texts;
  private final QueryService queryService;

  /**
   * Autowired constructor
   *
   * @param subjects     Subjects of the access window
   * @param texts        Texts of the access window
   * @param queryService Query service
   */
  public AccessImpactService(AccessSubjects subjects, AccessTexts texts, QueryService queryService) {
    this.subjects = subjects;
    this.texts = texts;
    this.queryService = queryService;
  }

  /**
   * What a user of the subject loses, gains and keeps through its own rule with a change
   *
   * @param user       User name
   * @param fullName   Full name
   * @param lost       Options it can no longer open
   * @param gained     Options it can open now
   * @param kept       Options the subject loses but it keeps through its own rule
   * @param keptDenied Options the subject gains but its own rule keeps denied
   */
  private record UserImpact(String user, String fullName, List<Option> lost, List<Option> gained, List<Option> kept,
                            List<Option> keptDenied) {

    boolean changes() {
      return !lost.isEmpty() || !gained.isEmpty();
    }

    boolean keeps() {
      return !kept.isEmpty() || !keptDenied.isEmpty();
    }
  }

  /**
   * Preview of a change of the own rules of the subject on some options
   *
   * @param subject    Subject
   * @param change     Change: allow, deny or remove
   * @param selected   Selected options
   * @param before     Access of the subject today
   * @param after      Access of the subject with the change (both in the order of the menu)
   * @param users      Users that change or keep something, by user name
   * @param usersTotal Users of the subject (1 for a user)
   */
  private record Impact(Subject subject, String change, List<String> selected, Result before, Result after,
                        List<UserImpact> users, int usersTotal) {

    boolean changes(int index) {
      return before.options().get(index).allowed() != after.options().get(index).allowed();
    }
  }

  /**
   * Retrieve the summary of the preview of a change of the own rules of the subject on some options, as a card: how
   * many options change, and for a profile how many of its users lose or gain them and how many keep them through their
   * own rule. The change is simulated: nothing is stored
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param options     Selected options
   * @param change      Change: allow, deny or remove
   * @return One row (title, description, icon, iconTone, change, selected-count, options-count, users-count,
   * users-total and keep-count), none without subject, options or a known change
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessImpact(String subjectType, Integer user, Integer profile, List<String> options,
                                     String change) throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Impact> found = getImpact(subjectType, user, profile, options, change);
    if (found.isPresent()) {
      Impact impact = found.get();
      Subject subject = impact.subject();
      String key = impact.change().toUpperCase();
      int optionsCount = (int) IntStream.range(0, impact.before().options().size()).filter(impact::changes).count();
      int usersCount = (int) impact.users().stream().filter(UserImpact::changes).count();
      int keepCount = (int) impact.users().stream().filter(UserImpact::keeps).count();
      String subjectText = subject.layer() == Layer.USER ? texts.getLocale(PREFIX + "SUBJECT_USER", subject.userName())
        : texts.getLocale(PREFIX + "SUBJECT_PROFILE", subject.profileAcronym());
      List<String> description = new ArrayList<>();
      description.add(texts.getLocale(PREFIX + "IMPACT_OPTIONS_" + key, String.valueOf(optionsCount)));
      if (subject.layer() == Layer.PROFILE) {
        description.add(texts.getLocale(PREFIX + "IMPACT_USERS_" + key, String.valueOf(usersCount),
          String.valueOf(impact.usersTotal()), subject.profileAcronym()));
        description.add(texts.getLocale(PREFIX + "IMPACT_KEEP", String.valueOf(keepCount)));
      }
      Map<String, CellData> row = new HashMap<>();
      row.put("change", new CellData(impact.change()));
      row.put("selected-count", new CellData(impact.selected().size()));
      row.put("options-count", new CellData(optionsCount));
      row.put("users-count", new CellData(usersCount));
      row.put("users-total", new CellData(impact.usersTotal()));
      row.put("keep-count", new CellData(keepCount));
      row.put(TITLE, new CellData(texts.getLocale(PREFIX + "IMPACT_TITLE_" + key,
        String.valueOf(impact.selected().size()), subjectText)));
      row.put(DESCRIPTION, new CellData(String.join(SEPARATOR, description)));
      row.put(ICON, new CellData(switch (impact.change()) {
        case CHANGE_ALLOW -> "lucide:circle-check";
        case CHANGE_DENY -> "lucide:ban";
        default -> "lucide:eraser";
      }));
      row.put(ICON_TONE, new CellData(switch (impact.change()) {
        case CHANGE_ALLOW -> SUCCESS;
        case CHANGE_DENY -> DANGER;
        default -> INFO;
      }));
      rows.add(row);
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the options of the preview of a change: the selected options and the ones whose access changes (their
   * descendants), in the order of the menu, with the access of the subject before and after, and what decides today.
   * Nothing is stored
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param options     Selected options
   * @param change      Change: allow, deny or remove
   * @return Options: impact-option, impact-option-key, impact-before-text, impact-before-tone, impact-after-text,
   * impact-after-tone, impact-decides and impact-changes
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessImpactOptions(String subjectType, Integer user, Integer profile, List<String> options,
                                            String change) throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Impact> found = getImpact(subjectType, user, profile, options, change);
    if (found.isPresent()) {
      Impact impact = found.get();
      Set<String> selected = impact.selected().stream().map(String::toLowerCase).collect(Collectors.toSet());
      for (int index = 0; index < impact.before().options().size(); index++) {
        OptionAccess before = impact.before().options().get(index);
        OptionAccess after = impact.after().options().get(index);
        if (impact.changes(index) || selected.contains(before.option().getName().toLowerCase())) {
          Map<String, CellData> row = new HashMap<>();
          row.put("impact-option", new CellData(texts.getOptionLabel(before.option())));
          row.put("impact-option-key", new CellData(before.option().getName()));
          row.put("impact-before-text", new CellData(texts.getAccessText(before.allowed())));
          row.put("impact-before-tone", new CellData(AccessTexts.getAccessTone(before.allowed())));
          row.put("impact-after-text", new CellData(texts.getAccessText(after.allowed())));
          row.put("impact-after-tone", new CellData(AccessTexts.getAccessTone(after.allowed())));
          row.put("impact-decides", new CellData(texts.getReason(before, impact.subject())));
          row.put("impact-changes", new CellData(impact.changes(index) ? TRUE : FALSE));
          rows.add(row);
        }
      }
    }
    return asServiceData(rows);
  }

  /**
   * Retrieve the users of the preview of a change: for a profile, its users that lose or gain options or keep them
   * through their own rule (their own base file and their own rules apply on top of the profile); for a user, itself.
   * The first ones by user name, and a last row with the number of the rest. Nothing is stored
   *
   * @param subjectType Subject type: profile or user
   * @param user        User identifier
   * @param profile     Profile identifier
   * @param options     Selected options
   * @param change      Change: allow, deny or remove
   * @return Users: impact-user, impact-user-name, impact-user-change and impact-user-tone
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessImpactUsers(String subjectType, Integer user, Integer profile, List<String> options,
                                          String change) throws AWException {
    List<Map<String, CellData>> rows = new ArrayList<>();
    Optional<Impact> found = getImpact(subjectType, user, profile, options, change);
    if (found.isPresent()) {
      List<UserImpact> users = found.get().users();
      users.stream().limit(IMPACT_USERS_SHOWN).forEach(userImpact -> rows.add(getImpactUserRow(userImpact.user(),
        userImpact.fullName(), getUserChange(userImpact), getUserChangeTone(userImpact))));
      if (users.size() > IMPACT_USERS_SHOWN) {
        String more = String.valueOf(users.size() - IMPACT_USERS_SHOWN);
        rows.add(getImpactUserRow(texts.getLocale(PREFIX + "IMPACT_MORE_USERS", more), "", "", ""));
      }
    }
    return asServiceData(rows);
  }

  /**
   * Simulate a change of the own rules of the subject on some options, for the subject and, for a profile, for each of
   * its users: their own base file and their own rules apply on top of the rules of the profile. The access is
   * evaluated without module: the change applies to every module
   */
  private Optional<Impact> getImpact(String subjectType, Integer user, Integer profile, List<String> options,
                                     String change) throws AWException {
    List<String> selected = distinct(options);
    if (selected.isEmpty() || change == null || !CHANGES.contains(change)) {
      return Optional.empty();
    }
    Optional<Subject> found = subjects.get(subjectType, user, profile);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    Subject subject = found.get();
    Boolean restricted = CHANGE_REMOVE.equals(change) ? null : CHANGE_DENY.equals(change);
    Menu menu = subjects.getMenu();
    List<Rule> rules = subjects.getRules(subject);
    Result before = AccessCascade.evaluate(menu, null, rules, subject.layer());
    List<Rule> changed = AccessCascade.change(rules, subject.layer(), selected, restricted);
    Result after = AccessCascade.evaluate(menu, null, changed, subject.layer());

    List<UserImpact> users = new ArrayList<>();
    int usersTotal = 1;
    if (subject.layer() == Layer.USER) {
      users.add(getUserImpact(subject.userName(), subject.fullName(), before, after, before, after));
    } else {
      List<Map<String, CellData>> profileUsers = queryService.launchPrivateQuery(PROFILE_USERS_QUERY,
        allRows().put(PROFILE, subject.profile())).getDataList().getRows();
      usersTotal = profileUsers.size();
      Map<Integer, List<Rule>> userRules = getProfileUserRules(subject.profile());
      // Global and profile rules, the same for every user
      List<Rule> profileRules = rules.stream().filter(rule -> rule.layer() != Layer.BASE_PROFILE).toList();
      Map<String, List<Rule>> baseRules = new HashMap<>();
      for (Map<String, CellData> profileUser : profileUsers) {
        String restriction = Optional.ofNullable(stringOf(profileUser.get(USER_RESTRICTION)))
          .filter(StringUtils::isNotBlank).orElse(subject.restriction());
        List<Rule> userBefore = new ArrayList<>(baseRules.computeIfAbsent(restriction, subjects::getBaseRules));
        userBefore.addAll(profileRules);
        userBefore.addAll(userRules.getOrDefault(integerOf(profileUser.get(USER_ID)), Collections.emptyList()));
        List<Rule> userAfter = AccessCascade.change(userBefore, Layer.PROFILE, selected, restricted);
        users.add(getUserImpact(stringOf(profileUser.get(USER)), stringOf(profileUser.get(FULL_NAME)),
          AccessCascade.evaluate(menu, null, userBefore, Layer.USER),
          AccessCascade.evaluate(menu, null, userAfter, Layer.USER), before, after));
      }
    }
    List<UserImpact> affected = users.stream()
      .filter(userImpact -> userImpact.changes() || userImpact.keeps())
      .sorted(Comparator.comparing(userImpact -> Optional.ofNullable(userImpact.user()).orElse(""),
        String.CASE_INSENSITIVE_ORDER))
      .toList();
    return Optional.of(new Impact(subject, change, selected, before, after, affected, usersTotal));
  }

  /**
   * What a user loses, gains and keeps through its own rule: an option it keeps is one the subject changes and its own
   * rule leaves as it was, against the new access of the subject
   */
  private static UserImpact getUserImpact(String user, String fullName, Result before, Result after,
                                          Result subjectBefore, Result subjectAfter) {
    List<Option> lost = new ArrayList<>();
    List<Option> gained = new ArrayList<>();
    List<Option> kept = new ArrayList<>();
    List<Option> keptDenied = new ArrayList<>();
    for (int index = 0; index < before.options().size(); index++) {
      OptionAccess userBefore = before.options().get(index);
      OptionAccess userAfter = after.options().get(index);
      boolean subjectAllowed = subjectAfter.options().get(index).allowed();
      boolean subjectChanges = subjectBefore.options().get(index).allowed() != subjectAllowed;
      if (userBefore.allowed() && !userAfter.allowed()) {
        lost.add(userAfter.option());
      } else if (!userBefore.allowed() && userAfter.allowed()) {
        gained.add(userAfter.option());
      } else if (subjectChanges && userAfter.layer() == Layer.USER && userAfter.allowed() != subjectAllowed) {
        (userAfter.allowed() ? kept : keptDenied).add(userAfter.option());
      }
    }
    return new UserImpact(user, fullName, lost, gained, kept, keptDenied);
  }

  /**
   * Own rules of the users of a profile, by user, in the order of the real menu
   */
  private Map<Integer, List<Rule>> getProfileUserRules(Integer profile) throws AWException {
    Map<Integer, List<Rule>> rules = new HashMap<>();
    ObjectNode parameters = allRows().put(PROFILE, profile);
    queryService.launchPrivateQuery(PROFILE_USER_RULES_QUERY, parameters).getDataList().getRows()
      .forEach(row -> rules.computeIfAbsent(integerOf(row.get(USER_ID)), userId -> new ArrayList<>())
        .add(new Rule(Layer.USER, stringOf(row.get(OPTION)), DENY.equals(stringOf(row.get(RESTRICTION))))));
    return rules;
  }

  private static Map<String, CellData> getImpactUserRow(String user, String fullName, String change, String tone) {
    Map<String, CellData> row = new HashMap<>();
    row.put("impact-user", new CellData(user));
    row.put("impact-user-name", new CellData(Optional.ofNullable(fullName).orElse("")));
    row.put("impact-user-change", new CellData(change));
    row.put("impact-user-tone", new CellData(tone));
    return row;
  }

  /**
   * What changes for a user: the options it keeps through its own rule, then the ones it loses and gains
   */
  private String getUserChange(UserImpact userImpact) {
    List<String> changes = new ArrayList<>();
    if (!userImpact.kept().isEmpty()) {
      changes.add(texts.getLocale(PREFIX + "IMPACT_KEEPS", getOptionLabels(userImpact.kept())));
    }
    if (!userImpact.keptDenied().isEmpty()) {
      changes.add(texts.getLocale(PREFIX + "IMPACT_KEEPS_DENIED", getOptionLabels(userImpact.keptDenied())));
    }
    if (!userImpact.lost().isEmpty()) {
      changes.add(texts.getLocale(PREFIX + "IMPACT_LOSES", getOptionLabels(userImpact.lost())));
    }
    if (!userImpact.gained().isEmpty()) {
      changes.add(texts.getLocale(PREFIX + "IMPACT_GAINS", getOptionLabels(userImpact.gained())));
    }
    return String.join(SEPARATOR, changes);
  }

  private static String getUserChangeTone(UserImpact userImpact) {
    if (!userImpact.lost().isEmpty()) {
      return DANGER;
    }
    return userImpact.gained().isEmpty() ? INFO : SUCCESS;
  }

  /**
   * Labels of some options: the first ones, and the number of the rest
   */
  private String getOptionLabels(List<Option> options) {
    String labels = options.stream().limit(IMPACT_OPTIONS_SHOWN).map(texts::getOptionLabel)
      .collect(Collectors.joining(", "));
    return options.size() > IMPACT_OPTIONS_SHOWN
      ? texts.getLocale(PREFIX + "IMPACT_MORE_OPTIONS", labels, String.valueOf(options.size() - IMPACT_OPTIONS_SHOWN))
      : labels;
  }
}
