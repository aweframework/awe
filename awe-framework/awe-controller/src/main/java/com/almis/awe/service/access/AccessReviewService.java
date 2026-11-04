package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.access.AccessCascade.Layer;
import com.almis.awe.service.access.AccessCascade.OptionAccess;
import com.almis.awe.service.access.AccessCascade.Result;
import com.almis.awe.service.access.AccessSubjects.Subject;
import com.almis.awe.service.menu.MenuRestrictionTreeService;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.service.access.AccessData.CHANGE_REMOVE;
import static com.almis.awe.service.access.AccessData.DESCRIPTION;
import static com.almis.awe.service.access.AccessData.ICON;
import static com.almis.awe.service.access.AccessData.ICON_TONE;
import static com.almis.awe.service.access.AccessData.OPTION;
import static com.almis.awe.service.access.AccessData.PREFIX;
import static com.almis.awe.service.access.AccessData.PROFILE_ID;
import static com.almis.awe.service.access.AccessData.RESTRICTION;
import static com.almis.awe.service.access.AccessData.SUBJECT_PROFILE;
import static com.almis.awe.service.access.AccessData.SUBJECT_USER;
import static com.almis.awe.service.access.AccessData.SUCCESS;
import static com.almis.awe.service.access.AccessData.TITLE;
import static com.almis.awe.service.access.AccessData.USER_ID;
import static com.almis.awe.service.access.AccessData.WARNING;
import static com.almis.awe.service.access.AccessData.WARNING_ICON;
import static com.almis.awe.service.access.AccessData.allRows;
import static com.almis.awe.service.access.AccessData.asServiceData;
import static com.almis.awe.service.access.AccessData.integerOf;
import static com.almis.awe.service.access.AccessData.stringOf;
import static com.almis.awe.service.access.AccessData.valueData;

/**
 * Review of the access window (access-admin): every active rule of every profile and user, global rules included, that
 * no longer makes sense (obsolete, allowed with the parent denied, redundant) with its fix, found with
 * {@link AccessReview} on the cascade of each subject
 */
public class AccessReviewService {

  // Every active rule, of every subject (review)
  static final String REVIEW_RULES_QUERY = "access-review-rules";
  private static final String ALLOW_PARENT = "allow-parent";

  private final AccessSubjects subjects;
  private final AccessTexts texts;
  private final QueryService queryService;
  private final MenuRestrictionTreeService menuRestrictionTreeService;

  /**
   * Autowired constructor
   *
   * @param subjects                   Subjects of the access window
   * @param texts                      Texts of the access window
   * @param queryService               Query service
   * @param menuRestrictionTreeService Menu restriction tree service (stores the rules)
   */
  public AccessReviewService(AccessSubjects subjects, AccessTexts texts, QueryService queryService,
                             MenuRestrictionTreeService menuRestrictionTreeService) {
    this.subjects = subjects;
    this.texts = texts;
    this.queryService = queryService;
    this.menuRestrictionTreeService = menuRestrictionTreeService;
  }

  /**
   * An active rule of the review
   *
   * @param id      Rule identifier (IdeAweScrRes)
   * @param user    User of the rule (null for a profile or a global rule)
   * @param profile Profile of the rule (null for a user or a global rule)
   * @param option  Option name
   * @param rule    Rule: "A" (allow) or "R" (deny)
   */
  private record ReviewRule(Integer id, Integer user, Integer profile, String option, String rule) {
  }

  /**
   * A finding of the review on a rule
   *
   * @param rule    Rule
   * @param subject Subject of the rule (null for a global rule or a subject that no longer exists)
   * @param finding Finding
   */
  private record ReviewFinding(ReviewRule rule, Subject subject, AccessReview.Finding finding) {
  }

  /**
   * Review of every active rule
   *
   * @param findings Findings, by group and rule
   * @param rules    Number of rules reviewed
   * @param profiles Number of profiles with rules
   * @param users    Number of users with rules
   */
  private record Review(List<ReviewFinding> findings, int rules, int profiles, int users) {

    int count(AccessReview.Group group) {
      return (int) findings.stream().filter(finding -> finding.finding().group() == group).count();
    }

    List<ReviewFinding> withoutEffect() {
      return findings.stream().filter(finding -> finding.finding().group().withoutEffect()).toList();
    }
  }

  /**
   * Retrieve the review of every rule of every profile and user, global rules included: the rules on options that are
   * not in the private menu or of a profile or a user that no longer exists (obsolete), the allowed options whose parent
   * is denied (they cannot be reached from the menu) and the rules that do not change the result (redundant), each one
   * with its fix. Each subject with rules is evaluated with its own cascade, without module; a rule is redundant only
   * when it changes nothing in any module of the menu either
   *
   * @return Findings, by group and rule: review-rule (the rule identifier), review-group (position and label), and
   * review-group-code, -option, -option-key, -who, -who-sub, -who-type, -rule-value, -rule-text, -rule-tone, -message,
   * -fix, -remove and -allow-parent (the button cells); the column names of a screen are unique
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessReview() throws AWException {
    return asServiceData(review().findings().stream().map(this::getFindingRow).toList());
  }

  /**
   * Retrieve the summary of the review, as a card: the number of warnings, of each group, of the rules without effect
   * (obsolete and redundant), and of the rules, profiles and users reviewed
   *
   * @return One row: warnings, obsolete, parent-denied, redundant, without-effect, rules, profiles, users, title,
   * description, icon and iconTone
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessReviewCounts() throws AWException {
    Review review = review();
    int warnings = review.findings().size();
    Map<String, CellData> row = new HashMap<>();
    row.put("warnings", new CellData(warnings));
    for (AccessReview.Group group : AccessReview.Group.values()) {
      row.put(getGroupCode(group), new CellData(review.count(group)));
    }
    row.put("without-effect", new CellData(review.withoutEffect().size()));
    row.put("rules", new CellData(review.rules()));
    row.put("profiles", new CellData(review.profiles()));
    row.put("users", new CellData(review.users()));
    row.put(TITLE, new CellData(warnings == 0 ? texts.getLocale(PREFIX + "REVIEW_NO_WARNINGS")
      : texts.getLocale(PREFIX + "REVIEW_WARNINGS", String.valueOf(warnings))));
    row.put(DESCRIPTION, new CellData(texts.getLocale(PREFIX + "REVIEW_SUMMARY", String.valueOf(review.rules()),
      String.valueOf(review.profiles()), String.valueOf(review.users()))));
    row.put(ICON, new CellData(warnings == 0 ? "lucide:circle-check" : WARNING_ICON));
    row.put(ICON_TONE, new CellData(warnings == 0 ? SUCCESS : WARNING));
    return asServiceData(List.of(row));
  }

  /**
   * Retrieve the label of a button of the review with its count: the button that opens it (review), with the number
   * of warnings, or the one that removes the rules without effect (clean), with their number
   *
   * @param kind Button: review or clean
   * @return One row with the label as value
   * @throws AWException Error retrieving data
   */
  public ServiceData getAccessReviewLabel(String kind) throws AWException {
    Review review = review();
    String label;
    if ("clean".equals(kind)) {
      label = texts.getLocale(PREFIX + "REVIEW_CLEAN", String.valueOf(review.withoutEffect().size()));
    } else {
      int warnings = review.findings().size();
      label = warnings == 0 ? texts.getLocale(PREFIX + "REVIEW")
        : texts.getLocale(PREFIX + "REVIEW_BUTTON", String.valueOf(warnings));
    }
    return valueData(label);
  }

  /**
   * Allow the denied parent of the options of some rules to the subject of each rule, so they can be reached from the
   * menu: the first denied parent from the top. A rule without that finding is skipped
   *
   * @param rules Rule identifiers (IdeAweScrRes): one, or the list sent by a grid
   * @return Service data
   * @throws AWException Error storing the rules
   */
  public ServiceData allowReviewParents(List<Integer> rules) throws AWException {
    Set<Integer> selected = new LinkedHashSet<>(Optional.ofNullable(rules).orElse(Collections.emptyList()));
    if (selected.isEmpty()) {
      return new ServiceData();
    }
    for (ReviewFinding finding : review().findings()) {
      if (finding.finding().group() == AccessReview.Group.PARENT_DENIED && selected.contains(finding.rule().id())) {
        menuRestrictionTreeService.allowMenuOption(finding.rule().user(), finding.rule().profile(),
          finding.finding().parent().option().getName());
      }
    }
    return new ServiceData();
  }

  /**
   * Remove every rule without effect: the obsolete and the redundant ones, each one of its own subject
   *
   * @return Service data
   * @throws AWException Error removing the rules
   */
  public ServiceData removeRulesWithoutEffect() throws AWException {
    for (ReviewFinding finding : review().withoutEffect()) {
      menuRestrictionTreeService.removeRestriction(finding.rule().user(), finding.rule().profile(),
        finding.rule().option());
    }
    return new ServiceData();
  }

  /**
   * Review every active rule: the global rules on options that are not in the menu, and the own rules of each profile
   * and user with its own cascade (a subject that no longer exists makes its rules obsolete). A rule with a user and a
   * profile is ignored, as the real menu does
   */
  private Review review() throws AWException {
    Menu menu = subjects.getMenu();
    List<ReviewRule> rules = queryService.launchPrivateQuery(REVIEW_RULES_QUERY, allRows())
      .getDataList().getRows().stream()
      .map(row -> new ReviewRule(integerOf(row.get("id")), integerOf(row.get(USER_ID)), integerOf(row.get(PROFILE_ID)),
        StringUtils.trim(stringOf(row.get(OPTION))), stringOf(row.get(RESTRICTION))))
      .filter(rule -> rule.user() == null || rule.profile() == null)
      .toList();
    Map<List<Integer>, List<ReviewRule>> bySubject = new LinkedHashMap<>();
    rules.forEach(rule -> bySubject.computeIfAbsent(Arrays.asList(rule.user(), rule.profile()),
      key -> new ArrayList<>()).add(rule));

    List<ReviewFinding> findings = new ArrayList<>();
    for (List<ReviewRule> subjectRules : bySubject.values()) {
      findings.addAll(reviewSubject(menu, subjectRules));
    }
    findings.sort(Comparator.comparingInt((ReviewFinding finding) -> finding.finding().group().ordinal())
      .thenComparing(finding -> Optional.ofNullable(finding.rule().id()).orElse(0)));
    long profiles = bySubject.keySet().stream().filter(key -> key.get(0) == null && key.get(1) != null).count();
    long users = bySubject.keySet().stream().filter(key -> key.get(0) != null).count();
    return new Review(List.copyOf(findings), rules.size(), (int) profiles, (int) users);
  }

  /**
   * Review the rules of one subject (all of them share its user and profile) with the cascade of that subject
   */
  private List<ReviewFinding> reviewSubject(Menu menu, List<ReviewRule> subjectRules) throws AWException {
    ReviewRule first = subjectRules.getFirst();
    Layer layer = first.user() != null ? Layer.USER : Layer.PROFILE;
    Optional<Subject> subject = first.user() == null && first.profile() == null ? Optional.empty()
      : subjects.get(layer == Layer.USER ? SUBJECT_USER : SUBJECT_PROFILE, first.user(), first.profile());
    // Global rules, and the rules of a subject that no longer exists, are only checked against the menu
    Result result = AccessCascade.evaluate(menu, null,
      subject.isPresent() ? subjects.getRules(subject.get()) : Collections.emptyList(), layer);
    List<ReviewFinding> findings = new ArrayList<>();
    for (ReviewRule rule : subjectRules) {
      Optional<AccessReview.Finding> finding = subject.isPresent() ? AccessReview.review(result, layer, rule.option())
        : getObsolete(result, rule);
      finding.ifPresent(found -> findings.add(new ReviewFinding(rule, subject.orElse(null), found)));
    }
    return findings;
  }

  /**
   * A global rule is obsolete when its option is not in the menu; a rule of a subject that no longer exists always is
   */
  private static Optional<AccessReview.Finding> getObsolete(Result result, ReviewRule rule) {
    Optional<OptionAccess> access = result.find(rule.option());
    boolean removedSubject = rule.user() != null || rule.profile() != null;
    return access.isEmpty() || removedSubject
      ? Optional.of(new AccessReview.Finding(AccessReview.Group.OBSOLETE, access.orElse(null), null))
      : Optional.empty();
  }

  private Map<String, CellData> getFindingRow(ReviewFinding reviewFinding) {
    ReviewRule rule = reviewFinding.rule();
    AccessReview.Finding finding = reviewFinding.finding();
    boolean allowParent = finding.group() == AccessReview.Group.PARENT_DENIED;
    Map<String, CellData> row = new HashMap<>();
    row.put("id", new CellData(rule.id()));
    // Sent by the screen to the maintains of the fixes: the rule of the selected row
    row.put("review-rule", new CellData(rule.id()));
    row.put("review-group", new CellData(JsonNodeFactory.instance.objectNode()
      .put(JSON_VALUE_PARAMETER, String.valueOf(finding.group().position()))
      .put(JSON_LABEL_PARAMETER, texts.getLocale(PREFIX + "REVIEW_GROUP_" + finding.group().name()))));
    row.put("review-group-code", new CellData(getGroupCode(finding.group())));
    row.put("review-option", new CellData(finding.access() == null ? rule.option()
      : texts.getOptionLabel(finding.access().option())));
    row.put("review-option-key", new CellData(rule.option()));
    addWho(row, rule, reviewFinding.subject());
    row.put("review-rule-value", new CellData(rule.rule()));
    row.put("review-rule-text", new CellData(texts.getRuleText(rule.rule())));
    row.put("review-rule-tone", new CellData(AccessTexts.getRuleTone(rule.rule())));
    row.put("review-message", new CellData(getFindingMessage(reviewFinding)));
    row.put("review-fix", new CellData(allowParent ? ALLOW_PARENT : CHANGE_REMOVE));
    // An icon button, as the row actions of the tree: its title is the column one (an empty label, or AngularJS shows the
    // value as its text)
    row.put("review-remove", new CellData(JsonNodeFactory.instance.objectNode().put(JSON_VALUE_PARAMETER, CHANGE_REMOVE)
      .put(JSON_LABEL_PARAMETER, "")));
    // Only an option hidden by its parent can allow it; the button is hidden on the other rows
    row.put("review-allow-parent", new CellData(allowParent
      ? JsonNodeFactory.instance.objectNode().put(JSON_VALUE_PARAMETER, ALLOW_PARENT)
      .put(JSON_LABEL_PARAMETER, texts.getLocale(PREFIX + "REVIEW_ALLOW_PARENT",
        texts.getOptionLabel(finding.parent().option())))
      : JsonNodeFactory.instance.objectNode().put(JSON_VALUE_PARAMETER, "").put("style", "hidden")));
    return row;
  }

  /**
   * Who has the rule: a profile, a user (with its full name) or every user; a subject that no longer exists is shown by
   * its identifier
   */
  private void addWho(Map<String, CellData> row, ReviewRule rule, Subject subject) {
    String whoType;
    String who;
    String whoSub;
    if (rule.user() != null) {
      whoType = SUBJECT_USER;
      who = subject == null ? texts.getLocale(PREFIX + "REVIEW_REMOVED_USER", String.valueOf(rule.user()))
        : subject.userName();
      whoSub = texts.getLocale("PARAMETER_USER");
    } else if (rule.profile() != null) {
      whoType = SUBJECT_PROFILE;
      who = subject == null ? texts.getLocale(PREFIX + "REVIEW_REMOVED_PROFILE", String.valueOf(rule.profile()))
        : subject.profileAcronym();
      whoSub = texts.getLocale("PARAMETER_PROFILE");
    } else {
      whoType = "global";
      who = texts.getLocale(PREFIX + "WHO_GLOBAL");
      whoSub = texts.getLocale(PREFIX + "REASON_GLOBAL");
    }
    row.put("review-who-type", new CellData(whoType));
    row.put("review-who", new CellData(who));
    row.put("review-who-sub", new CellData(whoSub));
  }

  private String getFindingMessage(ReviewFinding reviewFinding) {
    AccessReview.Finding finding = reviewFinding.finding();
    Subject subject = reviewFinding.subject();
    ReviewRule rule = reviewFinding.rule();
    return switch (finding.group()) {
      case OBSOLETE -> {
        if (subject == null && rule.user() != null) {
          yield texts.getLocale(PREFIX + "REVIEW_OBSOLETE_USER");
        } else if (subject == null && rule.profile() != null) {
          yield texts.getLocale(PREFIX + "REVIEW_OBSOLETE_PROFILE");
        }
        yield texts.getLocale(PREFIX + "REVIEW_OBSOLETE_OPTION", rule.option());
      }
      case PARENT_DENIED -> texts.getLocale(PREFIX + "REVIEW_PARENT_DENIED",
        texts.getOptionLabel(finding.parent().option()), texts.getReason(finding.parent(), subject),
        texts.getOptionLabel(finding.access().option()), AccessTexts.getSubjectName(subject));
      case REDUNDANT -> texts.getLocale(PREFIX + (finding.access().allowed() ? "REVIEW_REDUNDANT_ALLOWED"
        : "REVIEW_REDUNDANT_DENIED"), AccessTexts.getSubjectName(subject), texts.getOptionLabel(finding.access().option()));
    };
  }

  private static String getGroupCode(AccessReview.Group group) {
    return switch (group) {
      case OBSOLETE -> "obsolete";
      case PARENT_DENIED -> "parent-denied";
      case REDUNDANT -> "redundant";
    };
  }
}
