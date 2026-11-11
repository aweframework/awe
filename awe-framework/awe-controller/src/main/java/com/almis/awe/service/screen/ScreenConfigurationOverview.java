package com.almis.awe.service.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Classifies the active rules of a screen for the personalization overview: which ones no longer work and why.
 * Pure rules, no Spring: the screen and the stored rules are given by the caller.
 */
public final class ScreenConfigurationOverview {

  /** Why a rule no longer works */
  public enum Problem {
    MISSING_ELEMENT, INVALID_ATTRIBUTE, MISSING_SUBJECT, DUPLICATE
  }

  /**
   * A stored rule. User and profile ids are empty for everyone; a name is empty when the id has no user or profile
   *
   * @param id          Rule id
   * @param component   Component id
   * @param attribute   Attribute name
   * @param userId      User id
   * @param userName    User name
   * @param profileId   Profile id
   * @param profileName Profile name
   * @param value       Value
   */
  public record Rule(int id, String component, String attribute, String userId, String userName, String profileId, String profileName, String value) {
  }

  /**
   * A rule with its problem
   *
   * @param rule           Rule
   * @param problem        Problem, null when the rule works
   * @param duplicateGroup Rules of the same element, option and scope share the group (only for duplicates)
   */
  public record Row(Rule rule, Problem problem, String duplicateGroup) {
  }

  private ScreenConfigurationOverview() {
  }

  /**
   * Classify the rules of a screen. A rule has one problem: a missing element comes first, then an option not valid for it,
   * then a missing profile or user; only the working rules can be duplicates of each other
   *
   * @param rules          Active rules
   * @param elementExists  Whether a component is in the screen
   * @param attributeValid Whether an attribute can be configured in a component
   * @return One row per rule, in the order given
   */
  public static List<Row> classify(List<Rule> rules, Predicate<String> elementExists, BiPredicate<String, String> attributeValid) {
    Map<String, Integer> sameScope = new LinkedHashMap<>();
    rules.stream().filter(rule -> basicProblem(rule, elementExists, attributeValid) == null)
      .forEach(rule -> sameScope.merge(key(rule), 1, Integer::sum));
    List<Row> rows = new ArrayList<>();
    for (Rule rule : rules) {
      Problem problem = basicProblem(rule, elementExists, attributeValid);
      boolean duplicate = problem == null && sameScope.get(key(rule)) > 1;
      rows.add(new Row(rule, duplicate ? Problem.DUPLICATE : problem, duplicate ? key(rule) : null));
    }
    return rows;
  }

  private static Problem basicProblem(Rule rule, Predicate<String> elementExists, BiPredicate<String, String> attributeValid) {
    if (!elementExists.test(rule.component())) {
      return Problem.MISSING_ELEMENT;
    }
    if (!attributeValid.test(rule.component(), rule.attribute())) {
      return Problem.INVALID_ATTRIBUTE;
    }
    boolean missingUser = !rule.userId().isEmpty() && rule.userName().isEmpty();
    boolean missingProfile = !rule.profileId().isEmpty() && rule.profileName().isEmpty();
    return missingUser || missingProfile ? Problem.MISSING_SUBJECT : null;
  }

  private static String key(Rule rule) {
    return String.join("|", rule.component(), rule.attribute(), rule.userId(), rule.profileId());
  }
}
