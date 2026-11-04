package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Review (S7) and view as (S8) of the access window on the embedded database: the review of every rule of every profile
 * and user with its fixes (audited in HISAweScrRes), and the menu that a profile or a user would see, which stores
 * nothing.
 */
@Tag("integration")
@DisplayName("Access window review and view as tests")
class AccessReviewQueryTest extends AbstractAccessAdminTest {

  // A user that no longer exists
  private static final int REMOVED_USER = 999999;

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theReviewFindsTheRulesThatDoNotMakeSenseAndFixesThem() throws Exception {
    insertRule(FIRST_ROW, null, OPE, "old-sales-report", "R");
    // operator.xml denies tools: OPE cannot reach sites from the menu
    insertRule(FIRST_ROW + 1, null, OPE, "sites", "A");
    // administrator.xml has no rules: donald can open themes anyway
    insertRule(FIRST_ROW + 2, DONALD, null, "themes", "A");
    insertRule(FIRST_ROW + 3, REMOVED_USER, null, "users", "R");
    String audited = "SELECT COUNT(*) FROM HISAweScrRes WHERE Opt IN ('old-sales-report', 'sites', 'tools', 'themes', "
      + "'users')";
    int audit = count(audited);

    assertThat(findings()).extracting(row -> number(row.get("review-rule")), AccessReviewQueryTest::group,
        row -> row.get("review-who"), row -> row.get("review-message"))
      .containsExactly(
        tuple(FIRST_ROW, "1", "OPE",
          "The option old-sales-report is not in the private menu of this application: the rule has no effect"),
        tuple(FIRST_ROW + 3, "1", "User #999999 (removed)", "The user no longer exists: the rule has no effect"),
        tuple(FIRST_ROW + 1, "2", "OPE",
          "Tools is denied (Base profile operator.xml), so Sites does not appear in the menu of OPE although it is allowed"),
        tuple(FIRST_ROW + 2, "3", "donald", "Without this rule the result is the same: donald can open Themes anyway"));
    assertThat(String.valueOf(rows("access-review-label", "{}").getFirst().get("value"))).startsWith("Review · ");

    // Remove one finding: the grid sends the rule of its row as a list
    assertOk(maintain("access-review-remove", "{\"review-rule\":[" + FIRST_ROW + "]}"));
    assertThat(count("SELECT COUNT(*) FROM AweScrRes WHERE IdeAweScrRes = " + FIRST_ROW)).isZero();

    // Allow the denied parent to the subject of the rule: OPE reaches sites
    assertOk(maintain("access-review-allow-parent", "{\"review-rule\":[" + (FIRST_ROW + 1) + "]}"));
    assertThat(rules()).containsExactlyInAnyOrder("null|3|sites|A", "null|3|tools|A", "2|null|themes|A",
      "999999|null|users|R");

    // The rules without effect: donald's, the one of the removed user and now the allow of sites under tools
    assertOk(maintain("access-review-clean", "{}"));
    assertThat(rules()).containsExactly("null|3|tools|A");
    assertThat(findings()).isEmpty();
    // Every fix goes through the audited restriction maintains
    assertThat(count(audited)).isEqualTo(audit + 5);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void viewAsShowsTheMenuThatTheSubjectWouldSeeWithoutStoringAnything() throws Exception {
    // operator.xml denies tools: sites is allowed but not shown
    insertRule(FIRST_ROW, null, OPE, "sites", "A");
    int stored = count("SELECT COUNT(*) FROM AweScrRes");
    String ope = "{\"max\":0,\"subject-type\":\"profile\",\"IdePro\":" + OPE + "}";

    List<Map<String, Object>> menu = rows("access-menu-preview", ope);
    List<Map<String, Object>> hidden = rows("access-menu-preview-hidden", ope);

    // The scheduler is allowed; information is never shown in a menu (invisible)
    assertThat(menu).extracting(row -> row.get("preview-id")).contains("scheduler")
      .doesNotContain("tools", "sites", "information");
    assertThat(hidden).extracting(row -> row.get("hidden-option-key"), row -> row.get("hidden-reason"))
      .contains(tuple("tools", "Base profile operator.xml"), tuple("sites", "Its parent Tools is not shown"));
    Map<String, Object> counts = rows("access-menu-preview-counts", ope).getFirst();
    assertThat(number(counts.get("visible-count")) + number(counts.get("hidden-count")))
      .isEqualTo(number(counts.get("total-count")));
    assertThat(number(counts.get("hidden-count"))).isEqualTo(hidden.size());
    assertThat(counts.get("description")).isEqualTo("Sites does not appear: its parent Tools is denied");
    assertThat(rows("access-menu-preview-banner", ope).getFirst())
      .containsEntry("title", "Viewing as OPE · every module")
      .containsEntry("description", "This is a preview of the menu: you do not sign in as OPE nor open their screens.");

    // donald in module Base: the options of the module Test are hidden
    assertThat(rows("access-menu-preview-hidden", "{" + DONALD_TREE + ",\"access-module\":\"Base\"}"))
      .extracting(row -> row.get("hidden-option-key"), row -> row.get("hidden-reason"))
      .contains(tuple("test", "Belongs to module Test"));
    assertThat(count("SELECT COUNT(*) FROM AweScrRes")).isEqualTo(stored);
  }

  /**
   * Findings of the review on the rules of the test
   */
  private List<Map<String, Object>> findings() throws Exception {
    return rows("access-review", "{}").stream()
      .filter(row -> number(row.get("review-rule")) >= FIRST_ROW)
      .toList();
  }

  /**
   * Position of the group of a finding
   */
  private static String group(Map<String, Object> row) {
    return String.valueOf(((Map<?, ?>) row.get("review-group")).get("value"));
  }
}
