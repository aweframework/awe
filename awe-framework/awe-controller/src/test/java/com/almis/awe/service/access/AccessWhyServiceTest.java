package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The why side panel of the access window: the cascade of one option, its result and the rules of an option
 */
class AccessWhyServiceTest extends AccessServiceFixture {

  @Test
  void whyListsTheCascadeOfTheOptionAndTheLayerThatDecides() throws AWException {
    rules.add(new Object[]{null, null, "users", "R"});
    rules.add(new Object[]{null, OPE, "users", "A"});
    rules.add(new Object[]{JMARTIN, null, "tools", "R"});

    assertThat(why(whyService.getAccessWhy("user", JMARTIN, null, "Tools", "user-detail")))
      .containsExactly(
        tuple(1, "module", "SCREEN_TEXT_ACCESS_STEP_MODULE", "SCREEN_TEXT_ACCESS_DETAIL_MODULE_VISIBLE[Tools]", "", "", "false"),
        tuple(2, "base", "SCREEN_TEXT_ACCESS_STEP_BASE", "SCREEN_TEXT_ACCESS_DETAIL_BASE_NONE[operator]", "", "", "false"),
        tuple(3, "global", "SCREEN_TEXT_ACCESS_STEP_GLOBAL", "SCREEN_TEXT_ACCESS_DETAIL_GLOBAL_NONE", "", "", "false"),
        tuple(4, "profile", "SCREEN_TEXT_ACCESS_STEP_PROFILE", "SCREEN_TEXT_ACCESS_DETAIL_PROFILE_NONE[OPE]", "", "", "false"),
        tuple(5, "user", "SCREEN_TEXT_ACCESS_STEP_USER", "SCREEN_TEXT_ACCESS_DETAIL_USER_NONE[jmartin]", "", "", "false"),
        tuple(6, "inherited", "SCREEN_TEXT_ACCESS_STEP_INHERITANCE",
          "SCREEN_TEXT_ACCESS_DETAIL_INHERITED[MENU_TOOLS, SCREEN_TEXT_ACCESS_REASON_USER[jmartin]]", "R",
          "SCREEN_TEXT_ACCESS_DENY", "true"));
    assertThat(why(whyService.getAccessWhy("user", JMARTIN, null, null, "users")))
      .extracting(step -> step.toList().get(1), step -> step.toList().get(3), step -> step.toList().get(4),
        step -> step.toList().get(6))
      .containsExactly(
        tuple("module", "SCREEN_TEXT_ACCESS_DETAIL_NO_MODULE", "", "false"),
        tuple("base", "SCREEN_TEXT_ACCESS_DETAIL_BASE_NONE[operator]", "", "false"),
        tuple("global", "SCREEN_TEXT_ACCESS_DETAIL_GLOBAL_RULE", "R", "false"),
        tuple("profile", "SCREEN_TEXT_ACCESS_DETAIL_PROFILE_RULE[OPE]", "A", "false"),
        tuple("user", "SCREEN_TEXT_ACCESS_DETAIL_USER_NONE[jmartin]", "", "false"),
        // The user rule on its parent comes last and wins
        tuple("inherited", "SCREEN_TEXT_ACCESS_DETAIL_INHERITED[MENU_TOOLS, SCREEN_TEXT_ACCESS_REASON_USER[jmartin]]", "R",
          "true"));
    // The verdict of each layer as a tag, and the layer that decides marked
    assertThat(whyService.getAccessWhy("user", JMARTIN, null, null, "users").getDataList().getRows())
      .extracting(step -> text(step, "verdict-tone"), step -> text(step, "decides-text"))
      .containsExactly(tuple("", ""), tuple("", ""), tuple("danger", ""), tuple("success", ""), tuple("", ""),
        tuple("danger", "SCREEN_TEXT_ACCESS_DECIDES"));
  }

  @Test
  void whyOfAProfileSkipsTheUserLayerAndShowsTheModuleAndBase() throws AWException {
    rules.add(new Object[]{null, OPE, "security", "A"});

    assertThat(why(whyService.getAccessWhy("profile", null, OPE, "Tools", "sales")))
      .extracting(step -> step.toList().get(1), step -> step.toList().get(3), step -> step.toList().get(4),
        step -> step.toList().get(6))
      .containsExactly(
        tuple("module", "SCREEN_TEXT_ACCESS_DETAIL_MODULE_HIDDEN[Reports, Tools]", "R", "true"),
        tuple("base", "SCREEN_TEXT_ACCESS_DETAIL_BASE_NONE[operator]", "", "false"),
        tuple("global", "SCREEN_TEXT_ACCESS_DETAIL_GLOBAL_NONE", "", "false"),
        tuple("profile", "SCREEN_TEXT_ACCESS_DETAIL_PROFILE_NONE[OPE]", "", "false"),
        tuple("user", "SCREEN_TEXT_ACCESS_DETAIL_USER_NOT_APPLICABLE", "", "false"),
        tuple("inherited", "SCREEN_TEXT_ACCESS_DETAIL_NOT_INHERITED", "", "false"));
    assertThat(why(whyService.getAccessWhy("profile", null, OPE, null, "settings")))
      .extracting(step -> step.toList().get(1), step -> step.toList().get(3), step -> step.toList().get(4),
        step -> step.toList().get(6))
      .contains(tuple("base", "SCREEN_TEXT_ACCESS_DETAIL_BASE_RULE[operator]", "R", "true"));
    assertThat(why(whyService.getAccessWhy("profile", null, OPE, null, "security")))
      .extracting(step -> step.toList().get(1), step -> step.toList().get(3), step -> step.toList().get(4),
        step -> step.toList().get(6))
      .contains(tuple("profile", "SCREEN_TEXT_ACCESS_DETAIL_PROFILE_RULE[OPE]", "A", "true"),
        tuple("inherited", "SCREEN_TEXT_ACCESS_DETAIL_NOT_INHERITED", "", "false"));
    assertThat(why(whyService.getAccessWhy("profile", null, OPE, null, "missing"))).isEmpty();
    assertThat(why(whyService.getAccessWhy("profile", null, null, null, "settings"))).isEmpty();
  }

  @Test
  void theRulesOfAnOptionSayWhoHasThem() throws AWException {
    when(queryService.launchPrivateQuery(eq("access-option-rule-list"), any(ObjectNode.class))).thenReturn(data(
      row("id", 11, "userId", null, "profileId", OPE, "user", null, "userName", null, "profile", "OPE",
        "profileName", "operator", "rule", "R"),
      row("id", 12, "userId", JMARTIN, "profileId", null, "user", "jmartin", "userName", "Juan Martin",
        "profile", null, "profileName", null, "rule", "A"),
      row("id", 13, "userId", null, "profileId", null, "user", null, "userName", null, "profile", null,
        "profileName", null, "rule", "R"),
      // A row with a user and a profile is ignored by the real menu
      row("id", 14, "userId", JMARTIN, "profileId", OPE, "user", "jmartin", "userName", "Juan Martin",
        "profile", "OPE", "profileName", "operator", "rule", "R")));

    List<Map<String, CellData>> rows = whyService.getOptionRules("users").getDataList().getRows();

    assertThat(rows).extracting(row -> row.get("id").getIntegerValue(), row -> text(row, "who-type"),
        row -> text(row, "who"), row -> text(row, "who-sub"), row -> text(row, "rule"), row -> text(row, "rule-text"))
      .containsExactly(
        tuple(13, "global", "SCREEN_TEXT_ACCESS_WHO_GLOBAL", "SCREEN_TEXT_ACCESS_REASON_GLOBAL", "R", "SCREEN_TEXT_ACCESS_DENY"),
        tuple(11, "profile", "OPE", "operator", "R", "SCREEN_TEXT_ACCESS_DENY"),
        tuple(12, "user", "jmartin", "Juan Martin", "A", "SCREEN_TEXT_ACCESS_ALLOW"));
    // The screen sends the rule of the row to access-remove-rule
    assertThat(rows).extracting(row -> row.get("access-rule").getIntegerValue(), row -> text(row, "rule-tone"))
      .containsExactly(tuple(13, "danger"), tuple(11, "danger"), tuple(12, "success"));
    verify(queryService).launchPrivateQuery(eq("access-option-rule-list"),
      org.mockito.ArgumentMatchers.argThat((ObjectNode parameters) -> "users".equals(parameters.get("option").stringValue())));
  }

  @Test
  void theResultSaysWhetherTheSubjectCanOpenTheOptionAndWhatDecides() throws AWException {
    rules.add(new Object[]{null, OPE, "users", "R"});

    assertThat(result(whyService.getAccessResult("user", JMARTIN, null, null, "users")))
      .containsExactly(tuple("SCREEN_TEXT_ACCESS_RESULT_DENIED[jmartin, MENU_USERS]", "SCREEN_TEXT_ACCESS_REASON_PROFILE[OPE]",
        "lucide:ban", "danger"));
    assertThat(result(whyService.getAccessResult("profile", null, OPE, null, "sites")))
      .containsExactly(tuple("SCREEN_TEXT_ACCESS_RESULT_ALLOWED[OPE, MENU_SITES]", "SCREEN_TEXT_ACCESS_REASON_NONE",
        "lucide:circle-check", "success"));
    assertThat(result(whyService.getAccessResult("profile", null, OPE, null, "missing"))).isEmpty();
    assertThat(result(whyService.getAccessResult("profile", null, null, null, "sites"))).isEmpty();
  }

  private static List<org.assertj.core.groups.Tuple> result(ServiceData data) {
    return data.getDataList().getRows().stream()
      .map(row -> tuple(text(row, "title"), text(row, "description"), text(row, "icon"), text(row, "iconTone")))
      .toList();
  }

  private static List<org.assertj.core.groups.Tuple> why(ServiceData data) {
    return data.getDataList().getRows().stream()
      .map(row -> tuple(row.get("step").getIntegerValue(), text(row, "layer"), text(row, "title"), text(row, "detail"),
        text(row, "verdict"), text(row, "verdict-text"), text(row, "decides")))
      .toList();
  }
}
