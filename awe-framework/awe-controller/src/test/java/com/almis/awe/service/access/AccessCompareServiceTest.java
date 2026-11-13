package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Compare in the access window (compare-with: profile:IdePro or user:IdeOpe): the access of the subject next to the access
 * with another profile or the real access of another user, why it changes and the quick filter of the differences. A user
 * is simulated with the other profile, keeping its own rules. Nothing is stored. The compare-with suggest searches the
 * profiles and the users at once, each one with its type before its name.
 */
class AccessCompareServiceTest extends AccessServiceFixture {

  private static final int KCRUZ = 12;

  @Test
  void aProfileIsComparedWithAnotherProfile() throws AWException {
    rules.add(new Object[]{null, OPE, "users", "R"});
    rules.add(new Object[]{null, ADM, "reports", "R"});

    List<Map<String, CellData>> rows = tree("profile", null, OPE, null, "", "", "profile:" + ADM);

    assertThat(rows).extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "compare-access"),
        row -> text(row, "compare-differs"), row -> text(row, "compare-reason"))
      .containsExactly(
        tuple("tools", "allowed", "allowed", "false", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("sites", "allowed", "allowed", "false", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("users", "denied", "allowed", "true", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("user-detail", "denied", "allowed", "true", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("settings", "denied", "allowed", "true", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("security", "denied", "allowed", "true", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("reports", "allowed", "denied", "true", "SCREEN_TEXT_ACCESS_REASON_PROFILE[ADM]"),
        tuple("sales", "allowed", "denied", "true", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_REPORTS]"));
    // The access with the other profile is a tag; a differing row is marked with an icon on why it changes
    assertThat(row(rows, "reports")).extractingByKeys("compare-access-text", "compare-access-tone", "compare-icon")
      .extracting(cell -> ((CellData) cell).getStringValue())
      .containsExactly("SCREEN_TEXT_ACCESS_DENIED", "danger", "lucide:arrow-left-right");
    assertThat(row(rows, "tools")).extractingByKeys("compare-access-text", "compare-access-tone", "compare-icon")
      .extracting(cell -> ((CellData) cell).getStringValue())
      .containsExactly("SCREEN_TEXT_ACCESS_ALLOWED", "success", "");
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void onlyDifferencesKeepsTheDifferingOptionsWithTheirAncestorsAndIsCounted() throws AWException {
    rules.add(new Object[]{null, OPE, "users", "R"});
    rules.add(new Object[]{null, ADM, "reports", "R"});

    assertThat(contextOf(tree("profile", null, OPE, null, "differences", "", "profile:" + ADM)))
      .containsExactly(tuple("tools", "true"), tuple("users", "false"), tuple("user-detail", "false"),
        tuple("settings", "false"), tuple("security", "false"), tuple("reports", "false"), tuple("sales", "false"));
    assertThat(contextOf(tree("profile", null, OPE, null, "differences", "sales", "profile:" + ADM)))
      .containsExactly(tuple("reports", "true"), tuple("sales", "false"));
    assertThat(counts(treeService.getAccessTreeCounts("profile", null, OPE, null, "", "profile:" + ADM)))
      .containsExactly(
        tuple(1, "", "SCREEN_TEXT_ACCESS_FILTER_ALL", 8, ""),
        tuple(2, "allowed", "SCREEN_TEXT_ACCESS_FILTER_ALLOWED", 4, "success"),
        tuple(3, "denied", "SCREEN_TEXT_ACCESS_FILTER_DENIED", 4, "danger"),
        tuple(4, "with-rule", "SCREEN_TEXT_ACCESS_FILTER_WITH_RULE", 1, ""),
        tuple(5, "warnings", "SCREEN_TEXT_ACCESS_FILTER_WARNINGS", 0, "warning"),
        tuple(6, "differences", "SCREEN_TEXT_ACCESS_FILTER_DIFFERENCES", 6, "info"));
  }

  @Test
  void aUserIsSimulatedWithTheOtherProfileKeepingItsOwnRules() throws AWException {
    rules.add(new Object[]{JMARTIN, null, "users", "A"});
    rules.add(new Object[]{null, ADM, "tools", "R"});

    List<Map<String, CellData>> rows = tree("user", JMARTIN, null, null, "", "", "profile:" + ADM);

    // administrator.xml instead of operator.xml, the rules of ADM, and the own allow of users still wins
    assertThat(rows).extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "compare-access"),
        row -> text(row, "compare-reason"))
      .containsExactly(
        tuple("tools", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_PROFILE[ADM]"),
        tuple("sites", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_TOOLS]"),
        tuple("users", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("user-detail", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("settings", "denied", "allowed", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("security", "denied", "allowed", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("reports", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("sales", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"));
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void aUserWithItsOwnBaseFileKeepsItWithTheOtherProfile() throws AWException {
    when(queryService.launchPrivateQuery(eq("access-subject-user"),
      argThat((ObjectNode parameters) -> parameters.get("user").intValue() == KCRUZ)))
      .thenReturn(data(row("user", "kcruz", "fullName", "Kai Cruz", "profileId", OPE, "profile", "OPE",
        "userRestriction", "general", "profileRestriction", "operator")));

    // general.xml denies reports, with OPE and with ADM, as the real menu of the user would
    assertThat(row(tree("user", KCRUZ, null, null, "", "", "profile:" + ADM), "reports"))
      .extractingByKeys("access", "compare-access", "compare-differs")
      .extracting(cell -> ((CellData) cell).getStringValue())
      .containsExactly("denied", "denied", "false");
  }

  @Test
  void aProfileIsComparedWithTheRealAccessOfAUser() throws AWException {
    rules.add(new Object[]{JMARTIN, null, "sites", "R"});
    rules.add(new Object[]{null, OPE, "reports", "R"});

    List<Map<String, CellData>> rows = tree("profile", null, ADM, null, "", "", "user:" + JMARTIN);

    // jmartin with its real profile (OPE, operator.xml) and its own rules
    assertThat(rows).extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "compare-access"),
        row -> text(row, "compare-reason"))
      .containsExactly(
        tuple("tools", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("sites", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_USER[jmartin]"),
        tuple("users", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("user-detail", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("settings", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_BASE[operator]"),
        tuple("security", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_SETTINGS]"),
        tuple("reports", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_PROFILE[OPE]"),
        tuple("sales", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_REPORTS]"));
    assertThat(contextOf(tree("profile", null, ADM, null, "differences", "", "user:" + JMARTIN)))
      .containsExactly(tuple("tools", "true"), tuple("sites", "false"), tuple("settings", "false"),
        tuple("security", "false"), tuple("reports", "false"), tuple("sales", "false"));
    assertThat(counts(treeService.getAccessTreeCounts("profile", null, ADM, null, "", "user:" + JMARTIN)))
      .extracting(count -> count.toList().get(1), count -> count.toList().get(3))
      .containsExactly(tuple("", 8), tuple("allowed", 8), tuple("denied", 0), tuple("with-rule", 0),
        tuple("warnings", 0), tuple("differences", 5));
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void aUserIsComparedWithAnotherUserWithItsOwnProfileAndRules() throws AWException {
    rules.add(new Object[]{JMARTIN, null, "sites", "R"});

    // norestriction: no profile, the default general.xml and no own rules; the own rule of jmartin does not travel
    assertThat(tree("user", JMARTIN, null, null, "", "", "user:" + NO_RES))
      .extracting(row -> text(row, "option-id"), row -> text(row, "access"), row -> text(row, "compare-access"),
        row -> text(row, "compare-reason"))
      .containsExactly(
        tuple("tools", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("sites", "denied", "allowed", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("users", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("user-detail", "allowed", "allowed", "SCREEN_TEXT_ACCESS_COMPARE_SAME"),
        tuple("settings", "denied", "allowed", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("security", "denied", "allowed", "SCREEN_TEXT_ACCESS_REASON_NONE"),
        tuple("reports", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_BASE[general]"),
        tuple("sales", "allowed", "denied", "SCREEN_TEXT_ACCESS_REASON_INHERITED[MENU_REPORTS]"));
  }

  @Test
  void compareWithNamesTheProfileOrTheUserByItsType() throws AWException {
    rules.add(new Object[]{null, ADM, "reports", "R"});

    // The same identifier is a profile or a user by the type before it: profile 1 is ADM, user 1 does not exist
    assertThat(row(tree("profile", null, OPE, null, "", "", "profile:" + ADM), "reports"))
      .extractingByKeys("compare-access", "compare-reason")
      .extracting(cell -> ((CellData) cell).getStringValue())
      .containsExactly("denied", "SCREEN_TEXT_ACCESS_REASON_PROFILE[ADM]");
    assertThat(tree("profile", null, OPE, null, "", "", "user:" + ADM))
      .hasSize(8)
      .allSatisfy(row -> assertThat(row.keySet()).noneMatch(column -> column.startsWith("compare-")));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "profile", "profile:", "user:", "user:abc", "group:1", "1", "Profile:1", "profile:1:2",
    "profile:-1", "profile:404", "user:404", "profile:99999999999"})
  void withoutAValidProfileOrUserToCompareWithTheTreeAndTheCountsAreTheUsualOnes(String compareWith) throws AWException {
    rules.add(new Object[]{null, OPE, "users", "R"});

    assertThat(tree("profile", null, OPE, null, "", "", compareWith))
      .hasSize(8)
      .allSatisfy(row -> assertThat(row.keySet()).noneMatch(column -> column.startsWith("compare-")));
    assertThat(counts(treeService.getAccessTreeCounts("profile", null, OPE, null, "", compareWith)))
      .extracting(count -> count.toList().get(1))
      .containsExactly("", "allowed", "denied", "with-rule", "warnings");
    // Without a comparison, only differences filters nothing
    assertThat(tree("profile", null, OPE, null, "differences", "", compareWith)).hasSize(8);
  }

  @Test
  void theSuggestSearchesTheProfilesAndTheUsersGroupedByTheirType() throws AWException {
    when(queryService.launchPrivateQuery(eq(AccessSubjects.COMPARE_PROFILES_QUERY),
      argThat((ObjectNode parameters) -> "o".equals(parameters.get("search").stringValue()))))
      .thenReturn(data(row("profileId", OPE, "profile", "OPE", "name", "Operator")));
    when(queryService.launchPrivateQuery(eq(AccessSubjects.COMPARE_USERS_QUERY),
      argThat((ObjectNode parameters) -> "o".equals(parameters.get("search").stringValue()))))
      .thenReturn(data(row("userId", JMARTIN, "user", "jmartin", "fullName", "Juan Martin"),
        row("userId", NO_RES, "user", "norestriction", "fullName", null)));

    // Profiles first, then users; the value carries the type and the group is its locale key, not part of the label
    assertThat(suggest(" o "))
      .containsExactly(
        tuple("profile:" + OPE, "OPE - Operator", "PARAMETER_PROFILE"),
        tuple("user:" + JMARTIN, "jmartin - Juan Martin", "PARAMETER_USER"),
        tuple("user:" + NO_RES, "norestriction", "PARAMETER_USER"));
  }

  @Test
  void theSuggestListsTheProfilesAndTheUsersWithoutText() throws AWException {
    // The arrow of the suggest opens it without text: it lists every profile and user (up to the maximum per type)
    when(queryService.launchPrivateQuery(eq(AccessSubjects.COMPARE_PROFILES_QUERY),
      argThat((ObjectNode parameters) -> "".equals(parameters.get("search").stringValue()))))
      .thenReturn(data(row("profileId", OPE, "profile", "OPE", "name", "Operator")));
    when(queryService.launchPrivateQuery(eq(AccessSubjects.COMPARE_USERS_QUERY),
      argThat((ObjectNode parameters) -> "".equals(parameters.get("search").stringValue()))))
      .thenReturn(data(row("userId", JMARTIN, "user", "jmartin", "fullName", "Juan Martin")));

    List<org.assertj.core.groups.Tuple> all = List.of(
      tuple("profile:" + OPE, "OPE - Operator", "PARAMETER_PROFILE"),
      tuple("user:" + JMARTIN, "jmartin - Juan Martin", "PARAMETER_USER"));
    assertThat(suggest("")).containsExactlyElementsOf(all);
    assertThat(suggest(" ")).containsExactlyElementsOf(all);
    assertThat(suggest(null)).containsExactlyElementsOf(all);
  }

  @Test
  void theSuggestResolvesTheLabelOfASelectedValue() throws AWException {
    assertThat(suggest("profile:" + OPE)).containsExactly(tuple("profile:" + OPE, "OPE - Operator", "PARAMETER_PROFILE"));
    assertThat(suggest("user:" + JMARTIN)).containsExactly(tuple("user:" + JMARTIN, "jmartin - Juan Martin", "PARAMETER_USER"));
    assertThat(suggest("user:404")).isEmpty();
    // A selected value is not searched as a text
    verify(queryService, never()).launchPrivateQuery(eq(AccessSubjects.COMPARE_PROFILES_QUERY), any(ObjectNode.class));
    verify(queryService, never()).launchPrivateQuery(eq(AccessSubjects.COMPARE_USERS_QUERY), any(ObjectNode.class));
  }

  private List<org.assertj.core.groups.Tuple> suggest(String suggest) throws AWException {
    return compareService.getCompareSuggest(suggest).getDataList().getRows().stream()
      .map(row -> tuple(text(row, "value"), text(row, "label"), text(row, "group")))
      .toList();
  }
}
