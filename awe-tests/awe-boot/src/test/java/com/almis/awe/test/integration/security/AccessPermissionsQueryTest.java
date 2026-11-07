package com.almis.awe.test.integration.security;

import com.almis.awe.service.user.AweUserDetailService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The permissions view of the access window (access-admin) on the embedded database: the effective permissions of a
 * user with a main profile and two additional profiles, with the origin of each one (the profile file minus its
 * exclusions, the assignments of AweProPer and the permissions granted always), the warning of an exclusion that an
 * assignment overrides, the counts of its quick filter, the comparison with another user, why a permission is granted
 * profile by profile, and the same permissions as the login of the user loads.
 */
@Tag("integration")
@DisplayName("Access window permissions view tests")
class AccessPermissionsQueryTest extends AbstractAccessAdminTest {

  private static final String PREFIX = "accper775";
  private static final int FIRST_ID = 7750001;
  private static final int LAST_ID = 7750099;
  private static final String RANGE = " BETWEEN " + FIRST_ID + " AND " + LAST_ID;
  // Users: ana has Q8M (contributor file, users.update assigned) as main profile, and Q8R (reader file) and Q8S (users.unlock and an
  // unknown permission assigned) as additional profiles; luis has only Q8R
  private static final int ANA = 7750001;
  private static final int LUIS = 7750002;
  private static final int MANAGER = 7750011;
  private static final int READER = 7750012;
  private static final int SUPPORT = 7750013;
  private static final String ANA_SUBJECT = "\"max\":0,\"subject-type\":\"user\",\"IdeOpe\":" + ANA;
  private static final String MANAGER_SUBJECT = "\"max\":0,\"subject-type\":\"profile\",\"IdePro\":" + MANAGER;

  @Autowired
  private AweUserDetailService userDetailService;

  @BeforeEach
  void createUsersAndProfiles() throws Exception {
    deleteUsersAndProfiles();
    insertProfile(MANAGER, "Q8M", "Manager", "contributor");
    insertProfile(READER, "Q8R", "Reader copy", "reader");
    insertProfile(SUPPORT, "Q8S", "Support", "general");
    insertUser(ANA, PREFIX + "ana", MANAGER);
    insertUser(LUIS, PREFIX + "luis", READER);
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (7750021, " + ANA + ", " + READER + ")");
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (7750022, " + ANA + ", " + SUPPORT + ")");
    insertAssignment(7750031, MANAGER, "users.update");
    insertAssignment(7750032, SUPPORT, "users.unlock");
    insertAssignment(7750033, SUPPORT, "ghost.read");
    // The inserts do not clear the cached queries
    deleteRules();
  }

  @AfterEach
  void deleteUsersAndProfiles() throws Exception {
    execute("DELETE FROM AweOpeRol WHERE IdeAweOpeRol" + RANGE + " OR IdeOpe" + RANGE + " OR IdePro" + RANGE);
    execute("DELETE FROM AweProPer WHERE IdeAweProPer" + RANGE + " OR IdePro" + RANGE);
    execute("DELETE FROM ope WHERE IdeOpe" + RANGE);
    execute("DELETE FROM AwePro WHERE IdePro" + RANGE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void aUserHasThePermissionsOfAllItsProfilesWithTheirOrigins() throws Exception {
    List<Map<String, Object>> rows = rows("access-permissions", "{" + ANA_SUBJECT + "}");

    assertThat(permission(rows, "users.update"))
      .containsEntry("permission-state", "granted")
      .containsEntry("permission-state-text", "Granted")
      .containsEntry("permission-origins", "Q8M · assignment users.update")
      .containsEntry("permission-warning", "Q8M excludes it in its file contributor (users.*); another origin grants it")
      .containsEntry("permission-warning-icon", "lucide:triangle-alert");
    assertThat(permission(rows, "users.read"))
      .containsEntry("permission-origins", "Q8R · file reader (*.read)")
      .containsEntry("permission-warning", "Q8M excludes it in its file contributor (users.*); another origin grants it");
    assertThat(permission(rows, "users.unlock")).containsEntry("permission-origins", "Q8S · assignment users.unlock");
    assertThat(permission(rows, "users.delete"))
      .containsEntry("permission-state", "not-granted")
      .containsEntry("permission-state-tone", "neutral")
      .containsEntry("permission-origins", "")
      .containsEntry("permission-warning", "Excluded by Q8M in its file contributor (users.*)")
      .containsEntry("permission-warning-icon", "");
    assertThat(permission(rows, "audit.read"))
      .containsEntry("permission-origins", "Q8M · file contributor (*)|Q8R · file reader (*.read)");
    // Granted always: only that origin, from the catalog
    assertThat(permission(rows, "self.update")).containsEntry("permission-origins", "Granted always");
    assertThat(JsonPath.<String>read(permission(rows, "users.update").get("permission-resource"), "$.label"))
      .isEqualTo("Users · users · 3 of 6 granted");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theGrantedPermissionsAreTheOnesTheLoginOfTheUserLoads() throws Exception {
    List<String> login = userDetailService.loadUserByUsername(PREFIX + "ana").getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .filter(authority -> authority.startsWith(AweUserDetailService.PERMISSION_AUTHORITY_PREFIX))
      .map(authority -> authority.substring(AweUserDetailService.PERMISSION_AUTHORITY_PREFIX.length()))
      .toList();

    List<Object> granted = rows("access-permissions", "{" + ANA_SUBJECT + ",\"access-permission-filter\":\"granted\"}")
      .stream().map(row -> row.get("permission-id")).toList();

    assertThat(granted).containsExactlyInAnyOrderElementsOf(login).contains("users.update", "users.unlock");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfPermissionsOfItsQuickFilterAndTheSummaryTellsThem() throws Exception {
    List<Map<String, Object>> counts = rows("access-permission-counts", "{" + MANAGER_SUBJECT + "}");

    assertThat(counts).extracting(row -> number(row.get("pos")), row -> text(row, "value"), row -> row.get("label"),
        row -> text(row, "tone"))
      .containsExactly(
        tuple(1, "", "SCREEN_TEXT_ACCESS_PERMISSION_FILTER_ALL", ""),
        tuple(2, "granted", "SCREEN_TEXT_ACCESS_PERMISSION_FILTER_GRANTED", "success"),
        tuple(3, "not-granted", "SCREEN_TEXT_ACCESS_PERMISSION_FILTER_NOT_GRANTED", "neutral"),
        tuple(4, "warnings", "SCREEN_TEXT_ACCESS_PERMISSION_FILTER_WARNINGS", "warning"));
    for (Map<String, Object> option : counts) {
      int matching = rows("access-permissions", "{" + MANAGER_SUBJECT + ",\"access-permission-filter\":\""
        + text(option, "value") + "\"}").size();
      assertThat(number(option.get("count"))).as("count of '%s'", text(option, "value")).isEqualTo(matching);
    }
    // The profile alone: users.update is its only warning
    assertThat(number(counts.getLast().get("count"))).isOne();
    assertThat(rows("access-permissions", "{" + MANAGER_SUBJECT + ",\"access-permission-search\":\"UNLOCK\"}"))
      .extracting(row -> row.get("permission-id")).containsExactly("users.unlock");

    assertThat(rows("access-permission-summary", "{" + ANA_SUBJECT + "}")).singleElement().satisfies(row -> assertThat(row)
      .containsEntry("description", "Profiles: Q8M · Manager (main), Q8R · Reader copy, Q8S · Support · "
        + "5 excluded in a profile file and granted another way"));
    assertThat(rows("access-permissions", "{\"max\":0,\"subject-type\":\"user\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void compareShowsThePermissionsOfAnotherUserAndCountsTheDifferences() throws Exception {
    String withLuis = "{" + ANA_SUBJECT + ",\"compare-with\":\"user:" + LUIS + "\"";

    List<Map<String, Object>> rows = rows("access-permissions", withLuis + "}");

    assertThat(permission(rows, "users.update"))
      .containsEntry("compare-permission-state", "not-granted")
      .containsEntry("compare-permission-differs", "true")
      .containsEntry("compare-permission-reason", PREFIX + "ana: Q8M · assignment users.update; " + PREFIX
        + "luis: No profile grants it");
    assertThat(permission(rows, "users.read")).containsEntry("compare-permission-reason", "Same");
    List<Map<String, Object>> counts = rows("access-permission-counts", withLuis + "}");
    int differences = rows("access-permissions", withLuis + ",\"access-permission-filter\":\"differences\"}").size();
    assertThat(counts.getLast()).containsEntry("value", "differences");
    assertThat(number(counts.getLast().get("count"))).isEqualTo(differences).isPositive();
    // The other counts are the ones of the subject, not of the comparison
    assertThat(counts.get(1).get("count")).isEqualTo(rows("access-permission-counts", "{" + ANA_SUBJECT + "}").get(1)
      .get("count"));
    assertThat(rows("access-permission-summary", withLuis + "}")).singleElement().satisfies(row -> assertThat(row)
      .containsEntry("title", differences + " differences of " + rows.size() + " permissions"));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void whyTellsWhatEachProfileOfTheUserGivesToAPermission() throws Exception {
    String usersUpdate = "{" + ANA_SUBJECT + ",\"access-permission\":\"users.update\"}";

    assertThat(rows("access-permission-why", usersUpdate))
      .extracting(row -> row.get("why-profile"), row -> row.get("why-profile-sub"), row -> row.get("why-file"),
        row -> row.get("why-assignment"), row -> row.get("why-verdict-text"))
      .containsExactly(
        tuple("Q8M · Manager", "Main profile", "File contributor excludes it (users.*): the file does not give it",
          "Assignment users.update grants it", "Grants it"),
        tuple("Q8R · Reader copy", "Additional profile", "File reader does not include it", "Without assignments",
          "Does not grant it"),
        tuple("Q8S · Support", "Additional profile", "File general does not include it", "No assignment includes it",
          "Does not grant it"));
    assertThat(rows("access-permission-why-result", usersUpdate)).singleElement().satisfies(row -> assertThat(row)
      .containsEntry("title", PREFIX + "ana has Users · Update")
      .containsEntry("iconTone", "success"));
    // Granted always: the catalog grants it
    assertThat(rows("access-permission-why", "{" + ANA_SUBJECT + ",\"access-permission\":\"self.read\"}").getLast())
      .containsEntry("why-profile", "Granted always")
      .containsEntry("why-verdict-text", "Grants it");
    assertThat(rows("access-permission-why", "{" + ANA_SUBJECT + ",\"access-permission\":\"ghost.read\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theUsersShortcutOpensThePermissionsViewWithTheSelectedUser() throws Exception {
    // View permissions of the users screen sends access-view and access-user with the values of that screen
    String shortcut = screenData("{\"view\":\"base\",\"access-view\":\"permissions\",\"access-user\":[" + ANA + "]}");

    assertThat(selected(shortcut, "access-view")).containsExactly("permissions");
    assertThat(selected(shortcut, "IdeOpe")).containsExactly(String.valueOf(ANA));
    // Opened from the menu, the screen keeps its default view and no user
    String menu = screenData("{\"view\":\"base\"}");
    assertThat(selected(menu, "access-view")).doesNotContain("permissions");
    assertThat(selected(menu, "IdeOpe")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theUsersShortcutLoadsTheAccessWindowWithAllTheValuesOfTheUsersScreen() throws Exception {
    // The engines send every value of the users screen: its grid sends the columns of the selected row as lists, and
    // its IdeOpe column has the name of the user criterion of the access window
    String shortcut = screenData("{\"view\":\"base\",\"access-view\":\"permissions\",\"access-user\":" + ANA
      + ",\"GrdUsrAdm\":[" + ANA + "],\"GrdUsrAdm.selected\":" + ANA + ",\"IdeOpe\":[" + ANA + "],\"IdeOpe.selected\":"
      + ANA + ",\"Nam\":[\"" + PREFIX + "ana\"],\"Pro\":" + MANAGER + ",\"Res\":\"contributor\",\"Sta\":\"1\"}");

    assertThat(JsonPath.<List<Object>>read(shortcut, "$.actions[?(@.type == 'message')].parameters.message")).isEmpty();
    assertThat(selected(shortcut, "IdeOpe")).containsExactly(String.valueOf(ANA));
    // The user suggest shows the user, not only its id
    assertThat(JsonPath.<List<Object>>read(shortcut, "$.components[?(@.id == 'IdeOpe')].model.values[*].label"))
      .containsExactly(PREFIX + "ana");
  }

  /**
   * Data of the access window (its screen-access option) for the signed-in user, who can open it (administrator.xml)
   */
  private String screenData(String body) throws Exception {
    MockHttpSession session = signedInSession();
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session))
      .andReturn();
    return mockMvc.perform(post("/screen-data/screen-access")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  /**
   * Selected values of a component of a screen, as texts
   */
  private static List<String> selected(String screen, String component) {
    List<Object> values = JsonPath.read(screen, "$.components[?(@.id == '" + component + "')].model.selected[*]");
    return values.stream().map(value -> value instanceof Map<?, ?> cell ? String.valueOf(cell.get("value"))
      : String.valueOf(value)).toList();
  }

  private static Map<String, Object> permission(List<Map<String, Object>> rows, String permission) {
    return rows.stream().filter(row -> permission.equals(row.get("permission-id"))).findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + permission));
  }

  private void insertProfile(int id, String acronym, String name, String file) throws Exception {
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + id + ", '" + acronym + "', '"
      + name + "', 1, NULL, '" + file + "', 1)");
  }

  /**
   * Insert a user as a copy of the main data of test (its theme and language) with a profile
   */
  private void insertUser(int id, String name, int profile) throws Exception {
    execute("INSERT INTO ope (IdeOpe, l1_nom, l1_act, PwdLck, NumLog, EmlAdr, OpeNam, IdePro, l1_lan) "
      + "SELECT " + id + ", '" + name + "', 1, 0, 0, '" + name + "@almis.example', '" + name + "', " + profile
      + ", l1_lan FROM ope WHERE l1_nom = 'test'");
  }

  private void insertAssignment(int id, int profile, String permission) throws Exception {
    execute("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) VALUES (" + id + ", " + profile + ", '" + permission
      + "')");
  }
}
