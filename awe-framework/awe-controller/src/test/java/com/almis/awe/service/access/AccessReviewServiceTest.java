package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Review of the access window (S7): every rule of every profile and user, global rules included, that has no effect or
 * does not make sense, in three groups with their fix. The rule identifier is its position in the list of rules.
 */
class AccessReviewServiceTest extends AccessServiceFixture {

  private static final int REMOVED_USER = 99;
  private static final int REMOVED_PROFILE = 77;

  @Test
  void aRuleOnAnOptionOrOfASubjectThatNoLongerExistsIsObsolete() throws AWException {
    rules.add(new Object[]{null, OPE, "old-report", "R"});
    rules.add(new Object[]{null, null, "gone", "R"});
    rules.add(new Object[]{REMOVED_USER, null, "sites", "A"});
    rules.add(new Object[]{null, REMOVED_PROFILE, "sites", "R"});
    // Ignored by the real menu (a user and a profile), and a rule that makes sense
    rules.add(new Object[]{JMARTIN, OPE, "sites", "R"});
    rules.add(new Object[]{null, OPE, "users", "R"});

    List<Map<String, CellData>> findings = review();

    assertThat(findings).extracting(row -> number(row, "review-rule"), row -> group(row), row -> text(row, "review-option"),
        row -> text(row, "review-option-key"), row -> text(row, "review-who"), row -> text(row, "review-who-sub"), row -> text(row, "review-who-type"),
        row -> text(row, "review-rule-text"), row -> text(row, "review-message"), row -> text(row, "review-fix"))
      .containsExactly(
        tuple(1, "1|SCREEN_TEXT_ACCESS_REVIEW_GROUP_OBSOLETE", "old-report", "old-report", "OPE", "PARAMETER_PROFILE",
          "profile", "SCREEN_TEXT_ACCESS_DENY", "SCREEN_TEXT_ACCESS_REVIEW_OBSOLETE_OPTION[old-report]", "remove"),
        tuple(2, "1|SCREEN_TEXT_ACCESS_REVIEW_GROUP_OBSOLETE", "gone", "gone", "SCREEN_TEXT_ACCESS_WHO_GLOBAL",
          "SCREEN_TEXT_ACCESS_REASON_GLOBAL", "global", "SCREEN_TEXT_ACCESS_DENY",
          "SCREEN_TEXT_ACCESS_REVIEW_OBSOLETE_OPTION[gone]", "remove"),
        tuple(3, "1|SCREEN_TEXT_ACCESS_REVIEW_GROUP_OBSOLETE", "MENU_SITES", "sites",
          "SCREEN_TEXT_ACCESS_REVIEW_REMOVED_USER[99]", "PARAMETER_USER", "user", "SCREEN_TEXT_ACCESS_ALLOW",
          "SCREEN_TEXT_ACCESS_REVIEW_OBSOLETE_USER", "remove"),
        tuple(4, "1|SCREEN_TEXT_ACCESS_REVIEW_GROUP_OBSOLETE", "MENU_SITES", "sites",
          "SCREEN_TEXT_ACCESS_REVIEW_REMOVED_PROFILE[77]", "PARAMETER_PROFILE", "profile", "SCREEN_TEXT_ACCESS_DENY",
          "SCREEN_TEXT_ACCESS_REVIEW_OBSOLETE_PROFILE", "remove"));
    // Every finding can be removed; only the ones of a denied parent can allow it
    assertThat(findings).allSatisfy(row -> {
      assertThat(button(row, "review-remove")).isEqualTo("remove||");
      assertThat(button(row, "review-allow-parent")).isEqualTo("||hidden");
    });
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void anOptionAllowedWithADeniedParentIsFixedByAllowingTheParent() throws AWException {
    // operator.xml denies settings; OPE denies tools and jmartin (OPE) allows user-detail, under tools > users
    rules.add(new Object[]{null, OPE, "security", "A"});
    rules.add(new Object[]{null, OPE, "tools", "R"});
    rules.add(new Object[]{JMARTIN, null, "user-detail", "A"});

    List<Map<String, CellData>> findings = review();

    assertThat(findings).extracting(row -> number(row, "review-rule"), row -> group(row), row -> text(row, "review-who"),
        row -> text(row, "review-message"), row -> text(row, "review-fix"), row -> button(row, "review-allow-parent"))
      .containsExactly(
        tuple(1, "2|SCREEN_TEXT_ACCESS_REVIEW_GROUP_PARENT_DENIED", "OPE",
          "SCREEN_TEXT_ACCESS_REVIEW_PARENT_DENIED[MENU_SETTINGS, SCREEN_TEXT_ACCESS_REASON_BASE[operator], MENU_SECURITY, OPE]",
          "allow-parent", "allow-parent|SCREEN_TEXT_ACCESS_REVIEW_ALLOW_PARENT[MENU_SETTINGS]|"),
        // The parent to allow is the first denied one from the top: allowing users would leave tools denied
        tuple(3, "2|SCREEN_TEXT_ACCESS_REVIEW_GROUP_PARENT_DENIED", "jmartin",
          "SCREEN_TEXT_ACCESS_REVIEW_PARENT_DENIED[MENU_TOOLS, SCREEN_TEXT_ACCESS_REASON_PROFILE[OPE], MENU_USER_DETAIL, jmartin]",
          "allow-parent", "allow-parent|SCREEN_TEXT_ACCESS_REVIEW_ALLOW_PARENT[MENU_TOOLS]|"));

    // The parent is allowed to the subject of the rule; a rule without that finding is skipped
    reviewService.allowReviewParents(Arrays.asList(1, 2, 3, null, 404));
    reviewService.allowReviewParents(null);

    verify(menuRestrictionTreeService).allowMenuOption(null, OPE, "settings");
    verify(menuRestrictionTreeService).allowMenuOption(JMARTIN, null, "tools");
    verifyNoMoreInteractions(menuRestrictionTreeService);
  }

  @Test
  void aRuleThatDoesNotChangeTheResultIsRedundant() throws AWException {
    // operator.xml already denies settings; settings, without module, is allowed to norestriction without his rule
    rules.add(new Object[]{null, OPE, "settings", "R"});
    rules.add(new Object[]{NO_RES, null, "settings", "A"});
    rules.add(new Object[]{null, OPE, "users", "R"});

    assertThat(review()).extracting(row -> number(row, "review-rule"), row -> group(row), row -> text(row, "review-option"),
        row -> text(row, "review-message"), row -> text(row, "review-fix"))
      .containsExactly(
        tuple(1, "3|SCREEN_TEXT_ACCESS_REVIEW_GROUP_REDUNDANT", "MENU_SETTINGS",
          "SCREEN_TEXT_ACCESS_REVIEW_REDUNDANT_DENIED[OPE, MENU_SETTINGS]", "remove"),
        tuple(2, "3|SCREEN_TEXT_ACCESS_REVIEW_GROUP_REDUNDANT", "MENU_SETTINGS",
          "SCREEN_TEXT_ACCESS_REVIEW_REDUNDANT_ALLOWED[norestriction, MENU_SETTINGS]", "remove"));
  }

  @Test
  void theRulesWithoutEffectAreTheObsoleteAndTheRedundantOnes() throws AWException {
    rules.add(new Object[]{null, OPE, "settings", "R"});
    rules.add(new Object[]{NO_RES, null, "settings", "A"});
    rules.add(new Object[]{null, OPE, "old-report", "R"});
    rules.add(new Object[]{REMOVED_USER, null, "sites", "A"});
    rules.add(new Object[]{null, OPE, "security", "A"});
    rules.add(new Object[]{null, OPE, "users", "R"});
    // reports is allowed to jmartin without his rule, but in module Tools only his rule shows it
    rules.add(new Object[]{JMARTIN, null, "reports", "A"});

    reviewService.removeRulesWithoutEffect();

    // Each one with its own subject; the parent denied one and the ones that make sense stay
    verify(menuRestrictionTreeService).removeRestriction(null, OPE, "settings");
    verify(menuRestrictionTreeService).removeRestriction(NO_RES, null, "settings");
    verify(menuRestrictionTreeService).removeRestriction(null, OPE, "old-report");
    verify(menuRestrictionTreeService).removeRestriction(REMOVED_USER, null, "sites");
    verifyNoMoreInteractions(menuRestrictionTreeService);
  }

  @Test
  void theCountsAndTheLabelsSayHowManyWarningsThereAre() throws AWException {
    rules.add(new Object[]{null, OPE, "settings", "R"});
    rules.add(new Object[]{NO_RES, null, "settings", "A"});
    rules.add(new Object[]{null, OPE, "old-report", "R"});
    rules.add(new Object[]{REMOVED_USER, null, "sites", "A"});
    rules.add(new Object[]{null, OPE, "security", "A"});
    rules.add(new Object[]{null, null, "users", "R"});

    Map<String, CellData> counts = single(reviewService.getAccessReviewCounts());

    assertThat(Arrays.asList("warnings", "obsolete", "parent-denied", "redundant", "without-effect", "rules", "profiles",
      "users")).extracting(column -> counts.get(column).getIntegerValue())
      .containsExactly(5, 2, 1, 2, 4, 6, 1, 2);
    assertThat(Arrays.asList("title", "description", "icon", "iconTone")).extracting(column -> text(counts, column))
      .containsExactly("SCREEN_TEXT_ACCESS_REVIEW_WARNINGS[5]", "SCREEN_TEXT_ACCESS_REVIEW_SUMMARY[6, 1, 2]",
        "lucide:triangle-alert", "warning");
    assertThat(text(single(reviewService.getAccessReviewLabel("review")), "value")).isEqualTo("SCREEN_TEXT_ACCESS_REVIEW_BUTTON[5]");
    assertThat(text(single(reviewService.getAccessReviewLabel("clean")), "value")).isEqualTo("SCREEN_TEXT_ACCESS_REVIEW_CLEAN[4]");
  }

  @Test
  void withoutWarningsTheReviewSaysSo() throws AWException {
    rules.add(new Object[]{null, OPE, "users", "R"});

    assertThat(review()).isEmpty();
    Map<String, CellData> counts = single(reviewService.getAccessReviewCounts());
    assertThat(Arrays.asList("title", "icon", "iconTone")).extracting(column -> text(counts, column))
      .containsExactly("SCREEN_TEXT_ACCESS_REVIEW_NO_WARNINGS", "lucide:circle-check", "success");
    assertThat(text(single(reviewService.getAccessReviewLabel("review")), "value")).isEqualTo("SCREEN_TEXT_ACCESS_REVIEW");
    assertThat(text(single(reviewService.getAccessReviewLabel("clean")), "value")).isEqualTo("SCREEN_TEXT_ACCESS_REVIEW_CLEAN[0]");
  }

  private List<Map<String, CellData>> review() throws AWException {
    return reviewService.getAccessReview().getDataList().getRows();
  }

  private static Map<String, CellData> single(ServiceData data) {
    assertThat(data.getDataList().getRows()).hasSize(1);
    return data.getDataList().getRows().getFirst();
  }

  private static Integer number(Map<String, CellData> row, String column) {
    return row.get(column).getIntegerValue();
  }

  /**
   * Group of a finding: its value (the order of the groups) and its label
   */
  private static String group(Map<String, CellData> row) {
    JsonNode group = (JsonNode) row.get("review-group").getObjectValue();
    return group.get("value").stringValue() + "|" + group.get("label").stringValue();
  }

  /**
   * Button cell of a finding: value, label and style
   */
  private static String button(Map<String, CellData> row, String column) {
    JsonNode cell = (JsonNode) row.get(column).getObjectValue();
    return String.join("|", List.of("value", "label", "style").stream()
      .map(field -> cell.has(field) ? cell.get(field).stringValue() : "").toList());
  }
}
