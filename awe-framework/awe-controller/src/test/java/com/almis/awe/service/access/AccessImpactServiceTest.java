package com.almis.awe.service.access;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The preview of a bulk change of the access window (access-impact): the options whose access changes for the subject,
 * and for a profile which of its users lose or gain them and which keep them through their own rule. Nothing is stored.
 */
class AccessImpactServiceTest extends AccessServiceFixture {

  private static final int AMORALES = 9;
  private static final int BLOPEZ = 10;
  private static final int KCRUZ = 11;

  /**
   * OPE (operator.xml denies settings) has four users: jmartin, amorales (allows users), blopez (denies sites) and kcruz
   * (own base administrator.xml)
   */
  private void opeUsers() {
    users.add(new Object[]{JMARTIN, "jmartin", "Juan Martin", OPE, null});
    users.add(new Object[]{AMORALES, "amorales", "Ana Morales", OPE, null});
    users.add(new Object[]{BLOPEZ, "blopez", "Bea Lopez", OPE, ""});
    users.add(new Object[]{KCRUZ, "kcruz", "Kai Cruz", OPE, "administrator"});
    users.add(new Object[]{99, "admin", "Admin", ADM, null});
    rules.add(new Object[]{AMORALES, null, "users", "A"});
    rules.add(new Object[]{BLOPEZ, null, "sites", "R"});
  }

  @Test
  void denyingOptionsToAProfileCountsTheOptionsAndTheUsersThatLoseOrKeepThem() throws AWException {
    opeUsers();

    List<Map<String, CellData>> summary = impactService.getAccessImpact("profile", null, OPE, List.of("users", "sites", "settings"),
      "deny").getDataList().getRows();

    // sites, users and user-detail (inherited) become denied; settings already is
    assertThat(summary).singleElement().satisfies(row -> {
      assertThat(text(row, "change")).isEqualTo("deny");
      assertThat(row.get("selected-count").getIntegerValue()).isEqualTo(3);
      assertThat(row.get("options-count").getIntegerValue()).isEqualTo(3);
      assertThat(row.get("users-total").getIntegerValue()).isEqualTo(4);
      // kcruz (administrator.xml) also loses settings: the new rule of OPE denies it to him
      assertThat(row.get("users-count").getIntegerValue()).isEqualTo(4);
      assertThat(row.get("keep-count").getIntegerValue()).isEqualTo(1);
      assertThat(text(row, "title")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_TITLE_DENY[3, SCREEN_TEXT_ACCESS_SUBJECT_PROFILE[OPE]]");
      assertThat(text(row, "description")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_OPTIONS_DENY[3] · "
        + "SCREEN_TEXT_ACCESS_IMPACT_USERS_DENY[4, 4, OPE] · SCREEN_TEXT_ACCESS_IMPACT_KEEP[1]");
      assertThat(text(row, "icon")).isEqualTo("lucide:ban");
      assertThat(text(row, "iconTone")).isEqualTo("danger");
    });
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void theOptionsOfTheChangeShowTheirAccessBeforeAndAfterAndWhatDecidesToday() throws AWException {
    opeUsers();
    rules.add(new Object[]{null, OPE, "sites", "A"});

    List<Map<String, CellData>> options = impactService.getAccessImpactOptions("profile", null, OPE,
      List.of("settings", "users", "sites"), "deny").getDataList().getRows();

    // The selected options and the descendants that change, in the order of the menu
    assertThat(options).extracting(row -> text(row, "impact-option-key"), row -> text(row, "impact-option"),
        row -> text(row, "impact-before-text"), row -> text(row, "impact-before-tone"), row -> text(row, "impact-after-text"),
        row -> text(row, "impact-after-tone"), row -> text(row, "impact-decides"), row -> text(row, "impact-changes"))
      .containsExactly(
        tuple("sites", "MENU_SITES", "SCREEN_TEXT_ACCESS_ALLOWED", "success", "SCREEN_TEXT_ACCESS_DENIED", "danger",
          "SCREEN_TEXT_ACCESS_REASON_PROFILE[OPE]", "true"),
        tuple("users", "MENU_USERS", "SCREEN_TEXT_ACCESS_ALLOWED", "success", "SCREEN_TEXT_ACCESS_DENIED", "danger",
          "SCREEN_TEXT_ACCESS_REASON_NONE", "true"),
        tuple("user-detail", "MENU_USER_DETAIL", "SCREEN_TEXT_ACCESS_ALLOWED", "success", "SCREEN_TEXT_ACCESS_DENIED",
          "danger", "SCREEN_TEXT_ACCESS_REASON_NONE", "true"),
        tuple("settings", "MENU_SETTINGS", "SCREEN_TEXT_ACCESS_DENIED", "danger", "SCREEN_TEXT_ACCESS_DENIED", "danger",
          "SCREEN_TEXT_ACCESS_REASON_BASE[operator]", "false"));
  }

  @Test
  void theUsersOfTheProfileSayWhatTheyLoseAndWhatTheyKeepThroughTheirOwnRule() throws AWException {
    opeUsers();

    List<Map<String, CellData>> rows = impactService.getAccessImpactUsers("profile", null, OPE, List.of("users", "sites"),
      "deny").getDataList().getRows();

    // blopez denies sites himself and loses the others; kcruz has another base file, but loses the same options
    assertThat(rows).extracting(row -> text(row, "impact-user"), row -> text(row, "impact-user-name"),
        row -> text(row, "impact-user-change"), row -> text(row, "impact-user-tone"))
      .containsExactly(
        tuple("amorales", "Ana Morales", "SCREEN_TEXT_ACCESS_IMPACT_KEEPS[MENU_USERS, MENU_USER_DETAIL] · "
          + "SCREEN_TEXT_ACCESS_IMPACT_LOSES[MENU_SITES]", "danger"),
        tuple("blopez", "Bea Lopez", "SCREEN_TEXT_ACCESS_IMPACT_LOSES[MENU_USERS, MENU_USER_DETAIL]", "danger"),
        tuple("jmartin", "Juan Martin", "SCREEN_TEXT_ACCESS_IMPACT_LOSES[MENU_SITES, MENU_USERS, MENU_USER_DETAIL]",
          "danger"),
        tuple("kcruz", "Kai Cruz", "SCREEN_TEXT_ACCESS_IMPACT_LOSES[MENU_SITES, MENU_USERS, MENU_USER_DETAIL]", "danger"));
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void allowingAnOptionUsesTheOwnBaseFileOfEachUser() throws AWException {
    opeUsers();
    rules.add(new Object[]{JMARTIN, null, "security", "R"});

    List<Map<String, CellData>> rows = impactService.getAccessImpactUsers("profile", null, OPE, List.of("settings"), "allow")
      .getDataList().getRows();

    // kcruz (administrator.xml) already opens settings: no change. jmartin keeps security denied by his own rule
    assertThat(rows).extracting(row -> text(row, "impact-user"), row -> text(row, "impact-user-change"),
        row -> text(row, "impact-user-tone"))
      .containsExactly(
        tuple("amorales", "SCREEN_TEXT_ACCESS_IMPACT_GAINS[MENU_SETTINGS, MENU_SECURITY]", "success"),
        tuple("blopez", "SCREEN_TEXT_ACCESS_IMPACT_GAINS[MENU_SETTINGS, MENU_SECURITY]", "success"),
        tuple("jmartin", "SCREEN_TEXT_ACCESS_IMPACT_KEEPS_DENIED[MENU_SECURITY] · "
          + "SCREEN_TEXT_ACCESS_IMPACT_GAINS[MENU_SETTINGS]", "success"));
    assertThat(impactService.getAccessImpact("profile", null, OPE, List.of("settings"), "allow").getDataList().getRows())
      .singleElement().satisfies(row -> {
        assertThat(text(row, "description")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_OPTIONS_ALLOW[2] · "
          + "SCREEN_TEXT_ACCESS_IMPACT_USERS_ALLOW[3, 4, OPE] · SCREEN_TEXT_ACCESS_IMPACT_KEEP[1]");
        assertThat(text(row, "icon")).isEqualTo("lucide:circle-check");
        assertThat(text(row, "iconTone")).isEqualTo("success");
      });
  }

  @Test
  void removingTheRuleOfAProfileFallsBackToTheLayersBelow() throws AWException {
    opeUsers();
    rules.add(new Object[]{null, OPE, "settings", "A"});

    List<Map<String, CellData>> summary = impactService.getAccessImpact("profile", null, OPE, List.of("settings", "sites"),
      "remove").getDataList().getRows();

    // operator.xml denies settings again (and security); kcruz has administrator.xml: no change for him
    assertThat(summary).singleElement().satisfies(row -> {
      assertThat(row.get("options-count").getIntegerValue()).isEqualTo(2);
      assertThat(row.get("users-count").getIntegerValue()).isEqualTo(3);
      assertThat(text(row, "title")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_TITLE_REMOVE[2, SCREEN_TEXT_ACCESS_SUBJECT_PROFILE[OPE]]");
      assertThat(text(row, "iconTone")).isEqualTo("info");
    });
  }

  @Test
  void forAUserTheOnlyAffectedUserIsItself() throws AWException {
    opeUsers();

    List<Map<String, CellData>> summary = impactService.getAccessImpact("user", JMARTIN, null, List.of("sites"), "deny")
      .getDataList().getRows();
    List<Map<String, CellData>> rows = impactService.getAccessImpactUsers("user", JMARTIN, null, List.of("sites"), "deny")
      .getDataList().getRows();

    assertThat(summary).singleElement().satisfies(row -> {
      assertThat(row.get("options-count").getIntegerValue()).isEqualTo(1);
      assertThat(row.get("users-total").getIntegerValue()).isEqualTo(1);
      assertThat(row.get("users-count").getIntegerValue()).isEqualTo(1);
      assertThat(row.get("keep-count").getIntegerValue()).isZero();
      assertThat(text(row, "title")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_TITLE_DENY[1, SCREEN_TEXT_ACCESS_SUBJECT_USER[jmartin]]");
      // A user has no other users nor own rules over its own rules
      assertThat(text(row, "description")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_OPTIONS_DENY[1]");
    });
    assertThat(rows).extracting(row -> text(row, "impact-user"), row -> text(row, "impact-user-name"),
        row -> text(row, "impact-user-change"))
      .containsExactly(tuple("jmartin", "Juan Martin", "SCREEN_TEXT_ACCESS_IMPACT_LOSES[MENU_SITES]"));
    verifyNoInteractions(menuRestrictionTreeService);
  }

  @Test
  void theUsersAreListedUpToALimitAndTheRestAreCounted() throws AWException {
    for (int user = 100; user < 100 + AccessImpactService.IMPACT_USERS_SHOWN + 3; user++) {
      users.add(new Object[]{user, "user" + user, "User " + user, OPE, null});
    }

    List<Map<String, CellData>> rows = impactService.getAccessImpactUsers("profile", null, OPE, List.of("sites"), "deny")
      .getDataList().getRows();

    assertThat(rows).hasSize(AccessImpactService.IMPACT_USERS_SHOWN + 1);
    assertThat(rows.getLast()).satisfies(row -> {
      assertThat(text(row, "impact-user")).isEqualTo("SCREEN_TEXT_ACCESS_IMPACT_MORE_USERS[3]");
      assertThat(text(row, "impact-user-name")).isEmpty();
      assertThat(text(row, "impact-user-change")).isEmpty();
    });
  }

  @Test
  void aLongListOfOptionsIsShortened() throws AWException {
    users.add(new Object[]{JMARTIN, "jmartin", "Juan Martin", OPE, null});

    List<Map<String, CellData>> rows = impactService.getAccessImpactUsers("profile", null, OPE, List.of("tools"), "deny")
      .getDataList().getRows();

    // tools, sites, users and user-detail
    assertThat(rows).extracting(row -> text(row, "impact-user-change")).containsExactly(
      "SCREEN_TEXT_ACCESS_IMPACT_LOSES[SCREEN_TEXT_ACCESS_IMPACT_MORE_OPTIONS[MENU_TOOLS, MENU_SITES, MENU_USERS, 1]]");
  }

  @Test
  void withoutSubjectOptionsOrAKnownChangeThereIsNoPreview() throws AWException {
    assertThat(impactService.getAccessImpact("profile", null, null, List.of("sites"), "deny").getDataList().getRows()).isEmpty();
    assertThat(impactService.getAccessImpact("profile", null, OPE, List.of(), "deny").getDataList().getRows()).isEmpty();
    assertThat(impactService.getAccessImpact("profile", null, OPE, null, "deny").getDataList().getRows()).isEmpty();
    assertThat(impactService.getAccessImpactOptions("profile", null, OPE, List.of("sites"), "other").getDataList().getRows())
      .isEmpty();
    assertThat(impactService.getAccessImpactUsers("profile", null, OPE, List.of("sites"), null).getDataList().getRows())
      .isEmpty();
  }
}
