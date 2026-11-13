package com.almis.awe.test.integration.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The data of the access window (access-admin) on the embedded database: the tree with the effective access of a
 * profile (OPE, base profile operator.xml) and of a user (donald, profile ADM), the counts of its quick filter, the
 * cascade of an option, the rules of an option, and the maintains that allow, deny and remove the own rule of the
 * subject on one or more options (audited in HISAweScrRes) without touching the other rules. The preview of a change
 * and the comparison with another profile are simulated: they store nothing.
 */
@Tag("integration")
@DisplayName("Access window query and maintain tests")
class AccessAdminQueryTest extends AbstractAccessAdminTest {

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theTreeOfAProfileShowsItsBaseProfileAndTheModule() throws Exception {
    List<Map<String, Object>> rows = rows("access-tree", "{" + OPE_TREE + ",\"access-module\":\"Test\"}");

    assertThat(row(rows, "tools"))
      .containsEntry("access", "denied")
      .containsEntry("access-text", "Denied")
      .containsEntry("access-tone", "danger")
      .containsEntry("reason-layer", "base")
      .containsEntry("reason", "Base profile operator.xml")
      .containsEntry("option-key", "tools")
      .containsEntry("option-is-leaf", "false")
      .containsEntry("own-rule", "")
      .containsEntry("context", "false");
    assertThat(row(rows, "sites"))
      .containsEntry("parent-option", "tools")
      .containsEntry("reason-layer", "inherited")
      .containsEntry("reason", "Inherited from Tools");
    assertThat(JsonPath.<String>read(row(rows, "sites").get("option-name"), "$.label")).isEqualTo("Sites");
    assertThat(row(rows, "information"))
      .containsEntry("access", "allowed")
      .containsEntry("reason-layer", "none")
      .containsEntry("reason", "No rules: allowed by default");
    assertThat(row(rows, "information-restricted"))
      .containsEntry("access", "denied")
      .containsEntry("reason-layer", "module")
      .containsEntry("reason", "Belongs to module Inf");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theTreeOfAUserHasTheRulesOfItsProfileAndItsOwnRules() throws Exception {
    insertRule(FIRST_ROW, DONALD, null, "sites", "A");
    insertRule(FIRST_ROW + 1, null, ADM, "users", "A");
    insertRule(FIRST_ROW + 2, null, null, "encrypt-tools", "R");
    // Rules of another profile and of another user do not count
    insertRule(FIRST_ROW + 3, null, OPE, "themes", "R");
    insertRule(FIRST_ROW + 4, 3, null, "themes", "R");
    // administrator.xml has no rules
    insertRule(FIRST_ROW + 5, null, null, "tools", "R");

    List<Map<String, Object>> rows = rows("access-tree", "{" + DONALD_TREE + "}");

    assertThat(rows).extracting(row -> row.get("option-id"), row -> row.get("access"), row -> row.get("reason"),
        row -> row.get("own-rule"), row -> row.get("warning-code"))
      .contains(
        tuple("tools", "denied", "Global rule", "", ""),
        tuple("sites", "allowed", "Rule of user donald", "A", "parent-denied"),
        tuple("users", "allowed", "Rule of profile ADM", "", "parent-denied"),
        tuple("encrypt-tools", "denied", "Global rule", "", ""),
        tuple("themes", "denied", "Inherited from Tools", "", ""));
    assertThat(row(rows, "sites").get("warning")).asString().startsWith("Allowed, but its parent is denied");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theRulesOfALayerApplyInTheOrderOfTheRealMenu() throws Exception {
    // The real menu applies the rules of a layer by IdeAweScrRes: the later deny of tools wins over the earlier allow
    // of sites, even when the deny row was inserted first
    insertRule(FIRST_ROW + 1, null, OPE, "tools", "R");
    insertRule(FIRST_ROW, null, OPE, "sites", "A");

    List<Map<String, Object>> rows = rows("access-tree", "{" + OPE_TREE + "}");

    assertThat(row(rows, "sites"))
      .containsEntry("access", "denied")
      .containsEntry("reason-layer", "inherited");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void anOptionParameterOfTheRequestDoesNotFilterTheRulesOfTheSubject() throws Exception {
    // A private query reads a variable missing from its parameters from the request, and caches the result under the key
    // of its parameters: the AngularJS screen sends an option parameter, which must not filter (nor poison) the rules
    insertRule(FIRST_ROW, null, OPE, "sites", "A");

    assertThat(row(rows("access-tree", "{" + OPE_TREE + ",\"option\":\"tools\"}"), "sites")).containsEntry("own-rule", "A");
    assertThat(row(rows("access-tree", "{" + OPE_TREE + "}"), "sites")).containsEntry("own-rule", "A");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theFilterKeepsTheMatchingOptionsWithTheirAncestors() throws Exception {
    insertRule(FIRST_ROW, null, OPE, "sites", "A");

    List<Map<String, Object>> rows = rows("access-tree", "{" + OPE_TREE + ",\"access-filter\":\"with-rule\"}");

    assertThat(rows).extracting(row -> row.get("option-id"), row -> row.get("context"))
      .containsExactly(tuple("tools", "true"), tuple("sites", "false"));
    assertThat(rows("access-tree", "{" + OPE_TREE + ",\"access-search\":\"SITES\"}"))
      .extracting(row -> row.get("option-id")).contains("tools", "sites");
    assertThat(rows("access-tree", "{\"max\":0,\"subject-type\":\"profile\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfOptionsOfItsQuickFilter() throws Exception {
    insertRule(FIRST_ROW, null, OPE, "sites", "A");

    List<Map<String, Object>> counts = rows("access-tree-counts", "{" + OPE_TREE + "}");

    assertThat(counts).extracting(row -> number(row.get("pos")), row -> text(row, "value"), row -> row.get("label"),
        row -> text(row, "tone"))
      .containsExactly(
        tuple(1, "", "SCREEN_TEXT_ACCESS_FILTER_ALL", ""),
        tuple(2, "allowed", "SCREEN_TEXT_ACCESS_FILTER_ALLOWED", "success"),
        tuple(3, "denied", "SCREEN_TEXT_ACCESS_FILTER_DENIED", "danger"),
        tuple(4, "with-rule", "SCREEN_TEXT_ACCESS_FILTER_WITH_RULE", ""),
        tuple(5, "warnings", "SCREEN_TEXT_ACCESS_FILTER_WARNINGS", "warning"));
    for (Map<String, Object> option : counts) {
      long matching = rows("access-tree", "{" + OPE_TREE + ",\"access-filter\":\"" + text(option, "value") + "\"}")
        .stream().filter(row -> "false".equals(row.get("context"))).count();
      assertThat(number(option.get("count"))).as("count of '%s'", text(option, "value")).isEqualTo((int) matching);
    }
    assertThat(number(counts.get(3).get("count"))).isEqualTo(1);
    assertThat(number(counts.getFirst().get("count"))).isGreaterThan(number(counts.get(2).get("count")));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void whyListsTheCascadeOfAnOption() throws Exception {
    List<Map<String, Object>> steps = rows("access-why", "{" + OPE_SUBJECT + ",\"max\":20,\"access-option\":\"sites\"}");

    assertThat(steps).extracting(row -> number(row.get("step")), row -> row.get("layer"), row -> row.get("verdict"),
        row -> row.get("decides"))
      .containsExactly(
        tuple(1, "module", "", "false"),
        tuple(2, "base", "", "false"),
        tuple(3, "global", "", "false"),
        tuple(4, "profile", "", "false"),
        tuple(5, "user", "", "false"),
        tuple(6, "inherited", "R", "true"));
    assertThat(steps.getLast())
      .containsEntry("title", "Inheritance")
      .containsEntry("detail", "Inherited from Tools: Base profile operator.xml")
      .containsEntry("verdict-text", "Deny");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theRulesOfAnOptionSayWhoHasThem() throws Exception {
    insertRule(FIRST_ROW, DONALD, null, "databases", "R");
    insertRule(FIRST_ROW + 1, null, OPE, "databases", "A");
    insertRule(FIRST_ROW + 2, null, null, "databases", "R");
    // Ignored by the real menu: a user and a profile; inactive
    insertRule(FIRST_ROW + 3, DONALD, OPE, "databases", "R");
    execute("INSERT INTO AweScrRes (IdeAweScrRes, IdeOpe, IdePro, Opt, AccMod, Act) VALUES (" + (FIRST_ROW + 4)
      + ", NULL, " + ADM + ", 'databases', 'R', 0)");

    List<Map<String, Object>> rules = rows("access-option-rules", "{\"access-option\":\"databases\"}");

    assertThat(rules).extracting(row -> number(row.get("id")), row -> row.get("who-type"), row -> row.get("who"),
        row -> row.get("who-sub"), row -> row.get("rule"), row -> row.get("rule-text"))
      .containsExactly(
        tuple(FIRST_ROW + 2, "global", "Every user", "Global rule", "R", "Deny"),
        tuple(FIRST_ROW + 1, "profile", "OPE", "operator", "A", "Allow"),
        tuple(FIRST_ROW, "user", "donald", "Pato Donald", "R", "Deny"));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void allowDenyAndRemoveChangeOnlyTheOwnRulesOfTheSubject() throws Exception {
    // Another subject has a rule on the same option
    insertRule(FIRST_ROW, DONALD, null, "sites", "R");
    int audit = count("SELECT COUNT(*) FROM HISAweScrRes WHERE Opt IN ('sites', 'users')");

    assertOk(maintain("access-deny", "{" + OPE_SUBJECT + ",\"access-options\":[\"sites\",\"users\"]}"));
    assertThat(rules()).containsExactlyInAnyOrder("null|3|sites|R", "null|3|users|R", "2|null|sites|R");

    assertOk(maintain("access-allow", "{" + OPE_SUBJECT + ",\"access-options\":[\"sites\"]}"));
    assertThat(rules()).containsExactlyInAnyOrder("null|3|sites|A", "null|3|users|R", "2|null|sites|R");

    // An option without own rule is skipped
    assertOk(maintain("access-remove", "{" + OPE_SUBJECT + ",\"access-options\":[\"sites\",\"users\",\"databases\"]}"));
    assertThat(rules()).containsExactly("2|null|sites|R");

    // One option as a single value; a user rule has no profile
    assertOk(maintain("access-allow", "{" + DONALD_SUBJECT + ",\"access-options\":\"users\"}"));
    assertThat(rules()).containsExactlyInAnyOrder("2|null|sites|R", "2|null|users|A");
    assertThat(row(rows("access-tree", "{" + DONALD_TREE + "}"), "users")).containsEntry("own-rule", "A");

    // Every change is audited by the restriction maintains
    assertThat(count("SELECT COUNT(*) FROM HISAweScrRes WHERE Opt IN ('sites', 'users')")).isEqualTo(audit + 6);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void removeRuleRemovesTheRuleOfAnySubjectByIdentifier() throws Exception {
    // The side panel of an option lists the rules of every subject: the rule removed is not of the chosen subject
    insertRule(FIRST_ROW, DONALD, null, "databases", "R");
    insertRule(FIRST_ROW + 1, null, OPE, "databases", "A");
    // Another row of the same subject and option: only the row of the identifier is removed
    insertRule(FIRST_ROW + 2, DONALD, null, "databases", "A");
    String audited = "SELECT COUNT(*) FROM HISAweScrRes WHERE Opt = 'databases' AND IdeAweScrRes = " + FIRST_ROW
      + " AND IdeOpe = " + DONALD + " AND AccMod = 'R'";
    int audit = count(audited);

    // The grid of the side panel sends the rule of the selected row as a list
    assertOk(maintain("access-remove-rule", "{" + OPE_SUBJECT + ",\"access-rule\":[" + FIRST_ROW + "]}"));

    assertThat(rules()).containsExactlyInAnyOrder("null|3|databases|A", "2|null|databases|A");
    assertThat(count(audited)).isEqualTo(audit + 1);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void switchRuleTurnsTheRuleOfAnySubjectIntoTheOtherOne() throws Exception {
    insertRule(FIRST_ROW, DONALD, null, "databases", "R");
    insertRule(FIRST_ROW + 1, null, OPE, "databases", "A");
    // Another row of the same subject and option: only the row of the identifier changes
    insertRule(FIRST_ROW + 2, null, OPE, "databases", "A");
    int audit = count("SELECT COUNT(*) FROM HISAweScrRes WHERE Opt = 'databases'");

    // The grid of the side panel sends the rule of the selected row as a list; it can be of another subject
    assertOk(maintain("access-switch-rule", "{" + OPE_SUBJECT + ",\"access-rule\":[" + FIRST_ROW + "]}"));
    assertThat(rules()).containsExactlyInAnyOrder("2|null|databases|A", "null|3|databases|A", "null|3|databases|A");

    assertOk(maintain("access-switch-rule", "{" + OPE_SUBJECT + ",\"access-rule\":[" + (FIRST_ROW + 1) + "]}"));
    assertThat(rules()).containsExactlyInAnyOrder("2|null|databases|A", "null|3|databases|R", "null|3|databases|A");
    assertThat(count("SELECT COUNT(*) FROM AweScrRes WHERE IdeAweScrRes = " + (FIRST_ROW + 1) + " AND AccMod = 'R'"))
      .isEqualTo(1);
    // Each switch is an audited update of the rule
    assertThat(count("SELECT COUNT(*) FROM HISAweScrRes WHERE Opt = 'databases'")).isEqualTo(audit + 2);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theResultOfAnOptionSaysWhetherTheSubjectCanOpenItAndWhatDecides() throws Exception {
    insertRule(FIRST_ROW, null, OPE, "databases", "R");

    assertThat(rows("access-why-result", "{" + OPE_SUBJECT + ",\"access-option\":\"databases\"}"))
      .extracting(row -> row.get("title"), row -> row.get("description"), row -> row.get("icon"), row -> row.get("iconTone"))
      .containsExactly(tuple("OPE cannot open Databases", "Rule of profile OPE", "lucide:ban", "danger"));
    assertThat(rows("access-why-result", "{" + DONALD_SUBJECT + ",\"access-option\":\"information\"}"))
      .extracting(row -> row.get("title"), row -> row.get("iconTone"))
      .containsExactly(tuple("donald can open Information", "success"));
    assertThat(rows("access-why-result", "{" + OPE_SUBJECT + "}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void thePreviewOfAChangeSimulatesItForTheProfileAndItsUsersWithoutStoringAnything() throws Exception {
    // donald (ADM) allows users with his own rule
    insertRule(FIRST_ROW, DONALD, null, "users", "A");
    int stored = count("SELECT COUNT(*) FROM AweScrRes");
    int audit = count("SELECT COUNT(*) FROM HISAweScrRes");
    int admUsers = count("SELECT COUNT(*) FROM ope WHERE IdePro = " + ADM);
    String denyUsersAndSites = ",\"access-options\":[\"users\",\"sites\"],\"access-change\":\"deny\"}";
    String admSubject = "\"subject-type\":\"profile\",\"IdePro\":" + ADM;

    // OPE (operator.xml) already denies tools and everything below it: nothing changes
    assertThat(rows("access-impact", "{" + OPE_SUBJECT + denyUsersAndSites)).singleElement().satisfies(row -> {
      assertThat(row).containsEntry("title", "Deny the selected options to profile OPE (2)");
      assertThat(number(row.get("options-count"))).isZero();
    });
    assertThat(rows("access-impact-options", "{" + OPE_SUBJECT + denyUsersAndSites))
      .extracting(row -> row.get("impact-option-key"), row -> row.get("impact-after-text"), row -> row.get("impact-changes"))
      .containsExactly(tuple("sites", "Denied", "false"), tuple("users", "Denied", "false"));

    // ADM (administrator.xml): its users lose both, but donald keeps users
    assertThat(rows("access-impact", "{" + admSubject + denyUsersAndSites)).singleElement().satisfies(row -> {
      assertThat(number(row.get("options-count"))).isEqualTo(2);
      assertThat(number(row.get("users-total"))).isEqualTo(admUsers);
      assertThat(number(row.get("users-count"))).isEqualTo(admUsers);
      assertThat(number(row.get("keep-count"))).isEqualTo(1);
      assertThat(row).containsEntry("description", "Options that become denied: 2 · Users of ADM that lose some: " + admUsers
        + " of " + admUsers + " · Users that keep some through their own rule: 1");
    });
    assertThat(rows("access-impact-users", "{" + admSubject + denyUsersAndSites))
      .extracting(row -> row.get("impact-user"), row -> row.get("impact-user-change"))
      .contains(tuple("donald", "Keeps Users through their own rule · Loses Sites"), tuple("test", "Loses Sites, Users"));

    assertThat(count("SELECT COUNT(*) FROM AweScrRes")).isEqualTo(stored);
    assertThat(count("SELECT COUNT(*) FROM HISAweScrRes")).isEqualTo(audit);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void compareShowsTheAccessWithAnotherProfileAndCountsTheDifferences() throws Exception {
    String withAdm = "{" + OPE_TREE + ",\"compare-with\":\"profile:" + ADM + "\"";

    List<Map<String, Object>> rows = rows("access-tree", withAdm + "}");

    assertThat(row(rows, "tools"))
      .containsEntry("access", "denied")
      .containsEntry("compare-access-text", "Allowed")
      .containsEntry("compare-differs", "true")
      .containsEntry("compare-reason", "No rules: allowed by default");
    assertThat(row(rows, "information")).containsEntry("compare-reason", "Same");
    List<Map<String, Object>> counts = rows("access-tree-counts", withAdm + "}");
    assertThat(counts.getLast()).containsEntry("value", "differences");
    long differences = rows("access-tree", withAdm + ",\"access-filter\":\"differences\"}").stream()
      .filter(row -> "false".equals(row.get("context"))).count();
    assertThat(number(counts.getLast().get("count"))).isEqualTo((int) differences).isPositive();

    // A user is simulated with the other profile: operator.xml denies tools to donald
    String donaldWithOpe = "{" + DONALD_TREE + ",\"compare-with\":\"profile:" + OPE + "\"}";
    assertThat(row(rows("access-tree", donaldWithOpe), "tools"))
      .containsEntry("access", "allowed")
      .containsEntry("compare-access", "denied")
      .containsEntry("compare-reason", "Base profile operator.xml");

    // Another user, with its real profile and its own rules: test has the profile of donald (ADM) and no rules
    assertThat(rows("access-tree", "{" + DONALD_TREE + ",\"compare-with\":\"user:1\"}"))
      .allSatisfy(row -> assertThat(row).containsEntry("compare-differs", "false"));
    // A value that is not a profile or a user compares nothing, and is no error
    assertThat(rows("access-tree", "{" + DONALD_TREE + ",\"compare-with\":\"user:abc\"}"))
      .isNotEmpty()
      .allSatisfy(row -> assertThat(row).doesNotContainKey("compare-access"));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theCompareSuggestSearchesTheProfilesAndTheUsersAndResolvesASelectedValue() throws Exception {
    // Profiles first (acronym or name), then users (user or full name), each one in the group of its type
    assertThat(suggestions("o"))
      .contains(tuple("profile:" + OPE, "OPE - operator", "PARAMETER_PROFILE"),
        tuple("user:" + DONALD, "donald - Pato Donald", "PARAMETER_USER"))
      .doesNotContain(tuple("user:1", "test - Manager", "PARAMETER_USER"));
    assertThat(suggestions("ADM")).containsExactly(tuple("profile:" + ADM, "ADM - administrator", "PARAMETER_PROFILE"));
    assertThat(suggestions("pato")).containsExactly(tuple("user:" + DONALD, "donald - Pato Donald", "PARAMETER_USER"));
    // The selected value resolves the same label
    assertThat(suggestions("profile:" + OPE)).containsExactly(tuple("profile:" + OPE, "OPE - operator", "PARAMETER_PROFILE"));
    assertThat(suggestions("user:" + DONALD)).containsExactly(tuple("user:" + DONALD, "donald - Pato Donald", "PARAMETER_USER"));
    assertThat(suggestions("user:999999")).isEmpty();
  }

  private List<org.assertj.core.groups.Tuple> suggestions(String suggest) throws Exception {
    return rows("AccessCompareSug", "{\"suggest\":\"" + suggest + "\"}").stream()
      .map(row -> tuple(row.get("value"), row.get("label"), row.get("group")))
      .toList();
  }
}
