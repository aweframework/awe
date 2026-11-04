package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The tree of the access window: the access and the reason of every option, its filters, search and counts
 */
class AccessTreeServiceTest extends AccessServiceFixture {

  @Test
  void theTreeOfAProfileHasTheAccessAndTheReasonOfEveryOption() throws AWException {
    rules.add(new Object[]{null, null, "sites", "R"});
    rules.add(new Object[]{null, OPE, "users", "R"});

    List<Map<String, CellData>> rows = tree("profile", null, OPE, null, "", "");

    assertThat(rows).extracting(row -> text(row, "option-id"))
      .containsExactly("tools", "sites", "users", "user-detail", "settings", "security", "reports", "sales");
    assertThat(rows).extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "reason-layer"),
        row -> text(row, "reason"))
      .containsExactly(
        tuple("tools", "allowed", "none", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("sites", "denied", "global", "SCREEN_TEXT_ACCESS_REASON_GLOBAL"),
        tuple("users", "denied", "profile", "SCREEN_TEXT_ACCESS_REASON_PROFILE[OPE]"),
        tuple("user-detail", "denied", "inherited", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_USERS]"),
        tuple("settings", "denied", "base", "SCREEN_TEXT_ACCESS_REASON_BASE[operator]"),
        tuple("security", "denied", "inherited", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_SETTINGS]"),
        tuple("reports", "allowed", "none", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("sales", "allowed", "none", "SCREEN_TEXT_ACCESS_REASON_NONE"));

    Map<String, CellData> users = row(rows, "users");
    assertThat(text(users, "parent-option")).isEqualTo("tools");
    assertThat(text(users, "option-is-leaf")).isEqualTo("false");
    assertThat(text(row(rows, "sites"), "option-is-leaf")).isEqualTo("true");
    assertThat(text(users, "option-key")).isEqualTo("users");
    assertThat(text(users, "access-text")).isEqualTo("SCREEN_TEXT_ACCESS_DENIED");
    assertThat(text(users, "access-tone")).isEqualTo("danger");
    assertThat(text(row(rows, "tools"), "access-text")).isEqualTo("SCREEN_TEXT_ACCESS_ALLOWED");
    assertThat(text(row(rows, "tools"), "access-tone")).isEqualTo("success");
    assertThat(text(users, "own-rule")).isEqualTo("R");
    assertThat(text(users, "own-rule-text")).isEqualTo("SCREEN_TEXT_ACCESS_DENY");
    assertThat(text(users, "own-rule-tone")).isEqualTo("danger");
    assertThat(text(row(rows, "sites"), "own-rule")).isEmpty();
    assertThat(text(row(rows, "sites"), "own-rule-text")).isEmpty();
    assertThat(text(row(rows, "sites"), "own-rule-tone")).isEmpty();
    // The screen sends the option of the row to the maintains and shows its icon, and the side panel its key and subject
    assertThat(text(users, "access-options")).isEqualTo("users");
    assertThat(text(users, "option-icon")).isEqualTo("icon-users");
    assertThat(text(users, "option-subtitle")).isEqualTo("users · OPE");
    assertThat(text(users, "warning-icon")).isEmpty();
    assertThat(text(users, "context")).isEqualTo("false");
    assertThat(text(users, "warning")).isEmpty();
    assertThat(text(users, "warning-code")).isEmpty();
    assertThat(text(row(rows, "tools"), "parent-option")).isEmpty();
    // The label of the option, without its name, with its icon
    JsonNode name = (JsonNode) users.get("option-name").getObjectValue();
    assertThat(name.get("value").stringValue()).isEqualTo("users");
    assertThat(name.get("label").stringValue()).isEqualTo("MENU_USERS");
    assertThat(name.get("icon").stringValue()).isEqualTo("icon-users");
  }

  @Test
  void aUserGetsTheRulesOfItsProfileAndItsOwnRules() throws AWException {
    rules.add(new Object[]{null, OPE, "users", "R"});
    rules.add(new Object[]{JMARTIN, null, "users", "A"});
    rules.add(new Object[]{JMARTIN, null, "settings", "A"});
    // Another user and another profile do not count
    rules.add(new Object[]{99, null, "sites", "R"});
    rules.add(new Object[]{null, ADM, "sites", "R"});

    List<Map<String, CellData>> rows = tree("user", JMARTIN, ADM, null, "", "");

    assertThat(rows).extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "reason"),
        row -> text(row, "own-rule"))
      .contains(
        tuple("sites", "allowed", "SCREEN_TEXT_ACCESS_REASON_NONE", ""),
        tuple("users", "allowed", "SCREEN_TEXT_ACCESS_REASON_USER[jmartin]", "A"),
        tuple("settings", "allowed", "SCREEN_TEXT_ACCESS_REASON_USER[jmartin]", "A"),
        tuple("security", "allowed", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_SETTINGS]", ""));
    assertThat(text(row(rows, "users"), "option-subtitle")).isEqualTo("users · jmartin");
    assertThat(text(row(rows, "users"), "own-rule-tone")).isEqualTo("success");
  }

  @Test
  void theBaseRestrictionOfAUserIsItsOwnElseItsProfileElseTheDefault() throws AWException {
    // jmartin has no own restriction: operator.xml of its profile denies settings
    assertThat(text(row(tree("user", JMARTIN, null, null, "", ""), "settings"), "reason"))
      .isEqualTo("SCREEN_TEXT_ACCESS_REASON_BASE[operator]");
    // norestriction has none and no profile: general.xml (the default) denies reports
    List<Map<String, CellData>> rows = tree("user", NO_RES, null, null, "", "");
    assertThat(text(row(rows, "reports"), "reason")).isEqualTo("SCREEN_TEXT_ACCESS_REASON_BASE[general]");
    assertThat(text(row(rows, "settings"), "access")).isEqualTo("allowed");
  }

  @Test
  void aProfileWithoutBaseFileHasNoBaseRules() throws AWException {
    when(queryService.launchPrivateQuery(eq("access-subject-profile"), any(ObjectNode.class)))
      .thenReturn(data(row("profile", "NEW", "restriction", "missing-file")));

    assertThat(tree("profile", null, 50, null, "", ""))
      .allSatisfy(row -> assertThat(text(row, "access")).isEqualTo("allowed"));
  }

  @Test
  void optionsOfAnotherModuleAreDeniedWithTheModuleReason() throws AWException {
    List<Map<String, CellData>> rows = tree("profile", null, ADM, "Tools", "", "");

    assertThat(rows).extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "reason-layer"),
        row -> text(row, "reason"))
      .contains(
        tuple("reports", "denied", "module", "SCREEN_TEXT_ACCESS_REASON_MODULE[Reports]"),
        tuple("sales", "denied", "module", "SCREEN_TEXT_ACCESS_REASON_MODULE[Reports]"),
        tuple("tools", "allowed", "none", "SCREEN_TEXT_ACCESS_REASON_NONE"));
  }

  @Test
  void warningsAreShownOnTheirOption() throws AWException {
    rules.add(new Object[]{null, null, "tools", "R"});
    rules.add(new Object[]{null, ADM, "users", "A"});
    rules.add(new Object[]{null, ADM, "security", "A"});

    List<Map<String, CellData>> rows = tree("profile", null, ADM, null, "", "");

    assertThat(row(rows, "users")).extractingByKeys("warning-code", "warning")
      .extracting(cell -> ((CellData) cell).getStringValue())
      .containsExactly("parent-denied", "SCREEN_TEXT_ACCESS_WARNING_PARENT_DENIED");
    assertThat(row(rows, "security")).extractingByKeys("warning-code", "warning")
      .extracting(cell -> ((CellData) cell).getStringValue())
      .containsExactly("redundant", "SCREEN_TEXT_ACCESS_WARNING_REDUNDANT");
    assertThat(text(row(rows, "security"), "warning-icon")).isEqualTo("lucide:triangle-alert");
  }

  @Test
  void theFilterKeepsTheMatchingOptionsWithTheirAncestorsAsContext() throws AWException {
    rules.add(new Object[]{null, ADM, "user-detail", "R"});
    rules.add(new Object[]{null, ADM, "security", "A"});

    assertThat(contextOf(tree("profile", null, ADM, null, "denied", "")))
      .containsExactly(tuple("tools", "true"), tuple("users", "true"), tuple("user-detail", "false"));
    assertThat(contextOf(tree("profile", null, ADM, null, "with-rule", "")))
      .containsExactly(tuple("tools", "true"), tuple("users", "true"), tuple("user-detail", "false"),
        tuple("settings", "true"), tuple("security", "false"));
    assertThat(contextOf(tree("profile", null, ADM, null, "warnings", "")))
      .containsExactly(tuple("settings", "true"), tuple("security", "false"));
    assertThat(tree("profile", null, ADM, null, "allowed", "")).hasSize(7);
    assertThat(tree("profile", null, ADM, null, null, "")).hasSize(8);
  }

  @Test
  void theSearchFindsOptionsByLabelOrNameIgnoringCase() throws AWException {
    // By label (MENU_USER_DETAIL) and by name (user-detail)
    assertThat(contextOf(tree("profile", null, ADM, null, "", "user_DETAIL")))
      .containsExactly(tuple("tools", "true"), tuple("users", "true"), tuple("user-detail", "false"));
    assertThat(contextOf(tree("profile", null, ADM, null, "", "SECURITY")))
      .containsExactly(tuple("settings", "true"), tuple("security", "false"));
    // A matching parent does not bring its children
    assertThat(contextOf(tree("profile", null, ADM, null, "", "menu_settings")))
      .containsExactly(tuple("settings", "false"));
    // Search and filter together
    assertThat(tree("profile", null, ADM, null, "denied", "sales")).isEmpty();
  }

  @Test
  void withoutSubjectTheTreeIsEmpty() throws AWException {
    assertThat(tree("profile", null, null, null, "", "")).isEmpty();
    assertThat(tree("user", null, ADM, null, "", "")).isEmpty();
    assertThat(tree("profile", null, 404, null, "", "")).isEmpty();
  }

  @Test
  void theCountsAreTheRowsOfEachQuickFilterWithTheSearch() throws AWException {
    rules.add(new Object[]{null, null, "tools", "R"});
    rules.add(new Object[]{null, ADM, "users", "A"});
    rules.add(new Object[]{null, ADM, "sites", "R"});

    assertThat(counts(treeService.getAccessTreeCounts("profile", null, ADM, null, "", null)))
      .containsExactly(
        tuple(1, "", "SCREEN_TEXT_ACCESS_FILTER_ALL", 8, ""),
        tuple(2, "allowed", "SCREEN_TEXT_ACCESS_FILTER_ALLOWED", 6, "success"),
        tuple(3, "denied", "SCREEN_TEXT_ACCESS_FILTER_DENIED", 2, "danger"),
        tuple(4, "with-rule", "SCREEN_TEXT_ACCESS_FILTER_WITH_RULE", 2, ""),
        tuple(5, "warnings", "SCREEN_TEXT_ACCESS_FILTER_WARNINGS", 3, "warning"));
    // Only the options that match the search count, not their ancestors
    assertThat(counts(treeService.getAccessTreeCounts("profile", null, ADM, null, "user", null)))
      .extracting(count -> count.toList().get(3))
      .containsExactly(2, 2, 0, 1, 2);
    assertThat(counts(treeService.getAccessTreeCounts("profile", null, null, null, "", null)))
      .extracting(count -> count.toList().get(3))
      .containsExactly(0, 0, 0, 0, 0);
  }
}
