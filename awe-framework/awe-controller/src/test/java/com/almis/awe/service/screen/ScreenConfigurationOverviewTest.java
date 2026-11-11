package com.almis.awe.service.screen;

import com.almis.awe.service.screen.ScreenConfigurationOverview.Problem;
import com.almis.awe.service.screen.ScreenConfigurationOverview.Row;
import com.almis.awe.service.screen.ScreenConfigurationOverview.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenConfigurationOverviewTest {

  private static Rule rule(int id, String component, String attribute, String userId, String userName, String profileId, String profileName) {
    return new Rule(id, component, attribute, userId, userName, profileId, profileName, "x");
  }

  private static Rule everyone(int id, String component, String attribute) {
    return rule(id, component, attribute, "", "", "", "");
  }

  private static List<Row> classify(List<Rule> rules) {
    // The screen has every element but Gone; an element can be configured with label and visible only
    return ScreenConfigurationOverview.classify(rules, component -> !"Gone".equals(component),
      (component, attribute) -> List.of("label", "visible").contains(attribute));
  }

  @Test
  void aWorkingRuleHasNoProblem() {
    assertThat(classify(List.of(everyone(1, "Btn", "label")))).singleElement().satisfies(row -> {
      assertThat(row.problem()).isNull();
      assertThat(row.duplicateGroup()).isNull();
    });
  }

  @Test
  void anElementNoLongerOnTheScreenIsReported() {
    assertThat(classify(List.of(everyone(1, "Gone", "label"))).get(0).problem()).isEqualTo(Problem.MISSING_ELEMENT);
  }

  @Test
  void anOptionNotValidForTheElementIsReported() {
    assertThat(classify(List.of(everyone(1, "Btn", "align"))).get(0).problem()).isEqualTo(Problem.INVALID_ATTRIBUTE);
  }

  @Test
  void aProfileThatNoLongerExistsIsReported() {
    assertThat(classify(List.of(rule(1, "Btn", "label", "", "", "7", ""))).get(0).problem()).isEqualTo(Problem.MISSING_SUBJECT);
  }

  @Test
  void aUserThatNoLongerExistsIsReported() {
    assertThat(classify(List.of(rule(1, "Btn", "label", "9", "", "", ""))).get(0).problem()).isEqualTo(Problem.MISSING_SUBJECT);
  }

  @Test
  void aRuleOfAnExistingUserAndProfileHasNoProblem() {
    assertThat(classify(List.of(rule(1, "Btn", "label", "9", "ana", "7", "Admin"))).get(0).problem()).isNull();
  }

  @Test
  void twoRulesOfTheSameElementOptionAndScopeAreDuplicates() {
    List<Row> rows = classify(List.of(everyone(1, "Btn", "label"), everyone(2, "Btn", "label"), everyone(3, "Btn", "visible")));

    assertThat(rows.get(0).problem()).isEqualTo(Problem.DUPLICATE);
    assertThat(rows.get(1).problem()).isEqualTo(Problem.DUPLICATE);
    assertThat(rows.get(0).duplicateGroup()).isNotNull().isEqualTo(rows.get(1).duplicateGroup());
    assertThat(rows.get(2).problem()).isNull();
  }

  @Test
  void rulesOfDifferentScopesAreNotDuplicates() {
    List<Row> rows = classify(List.of(everyone(1, "Btn", "label"), rule(2, "Btn", "label", "", "", "7", "Admin"),
      rule(3, "Btn", "label", "9", "ana", "", "")));

    assertThat(rows).hasSize(3).allSatisfy(row -> assertThat(row.problem()).isNull());
  }

  @Test
  void aRuleWithAnotherProblemIsNotAlsoADuplicate() {
    List<Row> rows = classify(List.of(everyone(1, "Gone", "label"), everyone(2, "Gone", "label")));

    assertThat(rows).hasSize(2).allSatisfy(row -> {
      assertThat(row.problem()).isEqualTo(Problem.MISSING_ELEMENT);
      assertThat(row.duplicateGroup()).isNull();
    });
  }
}
