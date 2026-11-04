package com.almis.awe.test.integration.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The access window reads every rule and every user whatever the request pages: a card or a grid of a panel sends no
 * max (the engine pages with gridRowsPerPage) or its own one, and the queries the services launch would inherit it.
 * More global rules than a page must not hide the last ones from the cascade, the review nor its clean.
 */
@Tag("integration")
@DisplayName("Access window paging tests")
class AccessAdminPagingTest extends AbstractAccessAdminTest {

  // More rules and users than a page of the request (gridRowsPerPage is 30, the panel grids send 20)
  private static final int FILLERS = 35;

  @BeforeEach
  @AfterEach
  void deleteUsers() throws Exception {
    execute("DELETE FROM ope WHERE IdeOpe >= " + FIRST_ROW);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void aGlobalRuleAfterAPageOfRulesDecidesTheResultAndTheCascade() throws Exception {
    insertFillers();
    insertRule(FIRST_ROW + 40, null, null, "themes", "R");

    // The result card sends no max
    assertThat(rows("access-why-result", "{" + OPE_SUBJECT + ",\"access-option\":\"themes\"}"))
      .extracting(row -> row.get("description")).containsExactly("Global rule");
    // The grid of the cascade sends its max
    assertThat(rows("access-why", "{" + OPE_SUBJECT + ",\"max\":20,\"access-option\":\"themes\"}"))
      .filteredOn(row -> "global".equals(row.get("layer")))
      .extracting(row -> row.get("verdict"), row -> row.get("decides"))
      .containsExactly(tuple("R", "true"));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theCleanOfTheReviewKeepsARuleThatOverridesAGlobalRuleAfterAPage() throws Exception {
    // OPE allows information over a later global deny: the rule decides the access of OPE
    insertRule(FIRST_ROW, null, OPE, "information", "A");
    insertFillers();
    insertRule(FIRST_ROW + 40, null, null, "information", "R");

    assertOk(maintain("access-review-clean", "{}"));

    assertThat(count("SELECT COUNT(*) FROM AweScrRes WHERE IdeAweScrRes = " + FIRST_ROW)).isEqualTo(1);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void thePreviewOfAChangeCountsEveryUserOfTheProfile() throws Exception {
    for (int index = 1; index <= FILLERS; index++) {
      execute("INSERT INTO ope (IdeOpe, l1_nom, l1_act, IdePro) VALUES (" + (FIRST_ROW + index) + ", 'zz-access-" + index
        + "', 1, " + OPE + ")");
    }
    int opeUsers = count("SELECT COUNT(*) FROM ope WHERE IdePro = " + OPE);

    // operator.xml denies tools: allowing it, every user of OPE gains it. The card sends no max
    assertThat(rows("access-impact", "{" + OPE_SUBJECT + ",\"access-options\":[\"tools\"],\"access-change\":\"allow\"}"))
      .singleElement().satisfies(row -> {
        assertThat(number(row.get("users-total"))).isEqualTo(opeUsers);
        assertThat(number(row.get("users-count"))).isEqualTo(opeUsers);
      });
  }

  /**
   * Global rules on options that are not in the menu, before the rules of the test
   */
  private void insertFillers() throws Exception {
    for (int index = 1; index <= FILLERS; index++) {
      insertRule(FIRST_ROW + index, null, null, "zz-filler-" + index, "R");
    }
  }
}
