package com.almis.awe.test.integration.security;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration of the permissions of the profiles in the profiles screen (profile-admin): the profile panel lists the
 * permission catalog of the application and ProNew and ProUpd store the assignments of the profile (AweProPer) when
 * they change, ProDel deletes them with the profile, the predefined profiles (Reader and Contributor) cannot be
 * changed, and ProDup copies a profile with its effective permissions as assignments of the copy.
 */
@Tag("integration")
@DisplayName("Profile permissions administration tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class ProfilePermissionsAdminTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "prfper932";
  private static final String ACRONYM = "Q7A";
  private static final String COPY_ACRONYM = "Q7C";
  private static final String NEW_ACRONYM = "Q7N";

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AweElements aweElements;

  private int profile;

  @BeforeEach
  void createProfile() {
    deleteRows();
    profile = jdbcTemplate.queryForObject("SELECT MAX(IdePro) FROM AwePro", Integer.class) + 1000;
    // Audit left by a deleted profile that had the same identifier
    jdbcTemplate.update("DELETE FROM HISAweProPer WHERE IdePro = ?", profile);
    jdbcTemplate.update("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (?, ?, ?, 1, NULL, "
      + "NULL, 1)", profile, ACRONYM, PREFIX + " one");
  }

  @AfterEach
  void deleteRows() {
    String profiles = "SELECT IdePro FROM AwePro WHERE Nam LIKE '" + PREFIX + "%'";
    jdbcTemplate.update("DELETE FROM AweProPer WHERE IdePro IN (" + profiles + ")");
    jdbcTemplate.update("DELETE FROM HISAweProPer WHERE IdePro IN (" + profiles + ")");
    jdbcTemplate.update("DELETE FROM HISAwePro WHERE Nam LIKE ?", PREFIX + "%");
    jdbcTemplate.update("DELETE FROM AwePro WHERE Nam LIKE ?", PREFIX + "%");
  }

  @Test
  @DisplayName("ProUpd replaces the assignments with the selected permissions, audited, only when they change")
  void updateReplacesTheAssignments() throws Exception {
    assign(profile, "users.read");

    assertOk(maintain("ProUpd", profileParameters(profile, ACRONYM, "users.*", "themes.read")));

    assertThat(assignments(profile)).containsExactly("themes.read", "users.*");
    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE HISact = 'I' AND IdePro = " + profile)).isEqualTo(2);
    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE HISact = 'D' AND IdePro = " + profile)).isEqualTo(1);

    assertOk(maintain("ProUpd", profileParameters(profile, ACRONYM, "themes.read", "users.*")));

    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE IdePro = " + profile)).isEqualTo(3);
  }

  @Test
  @DisplayName("ProUpd compares every stored assignment, not a page of them: unchanged ones are not rewritten")
  void updateComparesEveryAssignment() throws Exception {
    String[] permissions = new String[35];
    for (int index = 0; index < permissions.length; index++) {
      permissions[index] = String.format("q7r%02d.read", index);
      assign(profile, permissions[index]);
    }

    assertOk(maintain("ProUpd", profileParameters(profile, ACRONYM, permissions)));

    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE IdePro = " + profile)).isZero();
    assertThat(assignments(profile)).hasSize(35);
  }

  @Test
  @DisplayName("The options show the stored assignments the catalog does not offer, with a warning")
  void catalogOptionsShowTheUnavailableAssignments() throws Exception {
    assignUnavailable();

    List<Map<String, Object>> rows = rows("ProPerCat", "{}");

    assertThat(option(rows, "users.read")).containsEntry("group", "PERMISSION_RESOURCE_USERS");
    for (String permission : List.of("*.execute", "*.unlock", "q7rgone.read")) {
      assertThat(option(rows, permission)).containsEntry("group", "PERMISSION_GROUP_UNAVAILABLE")
        .containsEntry("label", aweElements.getLocaleWithLanguage("PERMISSION_UNAVAILABLE", aweElements.getLanguage(),
          permission));
    }
  }

  @Test
  @DisplayName("ProUpd sending back the stored assignments, unavailable ones included, changes none of them")
  void updateKeepsTheAssignmentsSentBack() throws Exception {
    assignUnavailable();
    ObjectNode parameters = profileParameters(profile, ACRONYM, "users.read", "*.execute", "*.unlock", "q7rgone.read");
    parameters.put("Nam", PREFIX + " renamed");

    assertOk(maintain("ProUpd", parameters));

    assertThat(assignments(profile)).containsExactly("*.execute", "*.unlock", "q7rgone.read", "users.read");
    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE IdePro = " + profile)).isZero();
  }

  @Test
  @DisplayName("ProUpd revokes the unavailable assignments that are not sent back")
  void updateRevokesTheUnavailableAssignments() throws Exception {
    assignUnavailable();

    assertOk(maintain("ProUpd", profileParameters(profile, ACRONYM, "users.read", "*.execute")));

    assertThat(assignments(profile)).containsExactly("*.execute", "users.read");
  }

  @Test
  @DisplayName("ProUpd without selected permissions removes every assignment of the profile")
  void updateWithoutPermissionsRemovesThem() throws Exception {
    assign(profile, "users.read");
    assign(profile, "themes.*");

    assertOk(maintain("ProUpd", profileParameters(profile, ACRONYM)));

    assertThat(assignments(profile)).isEmpty();
  }

  @Test
  @DisplayName("ProUpd without the flag of the profiles screen (legacy PrfUpd screen) keeps the assignments")
  void legacyUpdateKeepsTheAssignments() throws Exception {
    assign(profile, "users.read");
    ObjectNode parameters = profileParameters(profile, ACRONYM);
    parameters.remove("PerUpd");
    parameters.remove("Per");

    assertOk(maintain("ProUpd", parameters));

    assertThat(assignments(profile)).containsExactly("users.read");
  }

  @Test
  @DisplayName("ProNew stores the selected permissions of the new profile")
  void newProfileStoresItsPermissions() throws Exception {
    ObjectNode parameters = profileParameters(profile, NEW_ACRONYM, "scheduler.*", "users.read");
    parameters.put("Nam", PREFIX + " new");

    assertOk(maintain("ProNew", parameters));

    int created = profileId(NEW_ACRONYM);
    assertThat(created).isNotEqualTo(profile);
    assertThat(assignments(created)).containsExactly("scheduler.*", "users.read");
    assertThat(assignments(profile)).isEmpty();
  }

  @Test
  @DisplayName("The profile panel loads the assignments of the selected profile")
  void selectionQueryReturnsTheAssignments() throws Exception {
    assign(profile, "users.read");
    assign(profile, "*");

    List<Map<String, Object>> rows = rows("ProPerSel", "{\"IdeProSel\":" + profile + "}");

    assertThat(rows).extracting(row -> String.valueOf(row.get("value"))).containsExactly("*", "users.read");
  }

  @Test
  @DisplayName("The options are the merged catalog grouped by resource, with all of a resource or of a verb")
  void catalogOptionsAreGroupedByResource() throws Exception {
    List<Map<String, Object>> rows = rows("ProPerCat", "{}");

    List<String> values = rows.stream().map(row -> String.valueOf(row.get("value"))).toList();
    // All resources offers the verbs of several resources, not the own actions of a resource (users.unlock is only
    // under Users) nor a verb of one resource (default.write is only under Default)
    assertThat(rows.stream().filter(row -> "PERMISSION_RESOURCE_ALL".equals(row.get("group")))
      .map(row -> String.valueOf(row.get("value")))).contains("*", "*.read", "*.update")
      .doesNotContain("*.execute", "*.unlock", "*.reset-2fa", "*.write");
    assertThat(values).startsWith("*")
      .containsSubsequence("users.*", "users.read", "users.reset-2fa", "profiles.*", "profiles.read")
      .doesNotContain("self.read", "self.update", "self.*");
    assertThat(option(rows, "users.delete")).containsEntry("group", "PERMISSION_RESOURCE_USERS")
      .containsEntry("label", locale("PERMISSION_RESOURCE_USERS") + " · " + locale("PERMISSION_VERB_DELETE"));
    assertThat(option(rows, "users.*"))
      .containsEntry("label", locale("PERMISSION_RESOURCE_USERS") + " · " + locale("PERMISSION_VERB_ALL"));
    assertThat(option(rows, "*")).containsEntry("group", "PERMISSION_RESOURCE_ALL");
    // A resource and an action of a module catalog (awe-scheduler), with their labels
    assertThat(values).containsSubsequence("scheduler.*", "scheduler.read", "scheduler.execute");
    assertThat(option(rows, "scheduler.execute")).containsEntry("group", "PERMISSION_RESOURCE_SCHEDULER")
      .containsEntry("label", locale("PERMISSION_RESOURCE_SCHEDULER") + " · " + locale("PERMISSION_SCHEDULER_EXECUTE"));
    // The catalog of the application (awe-boot, first module): its own resource and verb, and the AWE resource and
    // action it redeclares, with its labels
    assertThat(option(rows, "invoices.approve")).containsEntry("group", "PERMISSION_RESOURCE_INVOICES")
      .containsEntry("label", locale("PERMISSION_RESOURCE_INVOICES") + " · " + locale("PERMISSION_VERB_APPROVE"));
    assertThat(option(rows, "themes.export")).containsEntry("group", "PERMISSION_RESOURCE_APP_THEMES")
      .containsEntry("label", locale("PERMISSION_RESOURCE_APP_THEMES") + " · " + locale("PERMISSION_VERB_EXPORT"));
    assertThat(option(rows, "themes.delete")).containsEntry("group", "PERMISSION_RESOURCE_APP_THEMES");
    assertThat(option(rows, "users.unlock")).containsEntry("label",
      locale("PERMISSION_RESOURCE_USERS") + " · " + locale("PERMISSION_USERS_UNLOCK_ACCOUNT"));
    assertThat(option(rows, "*.export")).containsEntry("group", "PERMISSION_RESOURCE_ALL");
  }

  @Test
  @DisplayName("ProDel deletes the assignments of the profile with the profile, audited")
  void deleteRemovesTheAssignments() throws Exception {
    assign(profile, "users.read");
    assign(profile, "themes.*");

    assertOk(maintain("ProDel", parameters(profile)));

    assertThat(count("SELECT COUNT(*) FROM AwePro WHERE IdePro = " + profile)).isZero();
    assertThat(assignments(profile)).isEmpty();
    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE HISact = 'D' AND IdePro = " + profile)).isEqualTo(1);
  }

  @Test
  @DisplayName("The predefined profiles cannot be updated, deleted or given permissions")
  void predefinedProfilesCannotBeChanged() throws Exception {
    int reader = profileId("RDR");
    Map<String, Object> stored = jdbcTemplate.queryForMap("SELECT * FROM AwePro WHERE IdePro = ?", reader);
    ObjectNode update = parameters(reader);
    update.put("Acr", "RDR");
    update.put("Nam", "Changed reader");
    update.put("Res", "general");
    update.put("Act", 1);

    update.put("PerUpd", 1);
    update.putArray("Per").add("*");

    assertRefused(maintain("ProUpd", update), "RDR");
    assertRefused(maintain("ProDel", parameters(reader, profile)), "RDR");

    assertThat(jdbcTemplate.queryForMap("SELECT * FROM AwePro WHERE IdePro = ?", reader)).isEqualTo(stored);
    assertThat(count("SELECT COUNT(*) FROM AwePro WHERE IdePro = " + profile)).isEqualTo(1);
    assertThat(assignments(reader)).isEmpty();
  }

  @Test
  @DisplayName("The predefined flag of the selected profile is 1 for the Reader and 0 for any other profile")
  void predefinedFlagOfTheSelectedProfile() throws Exception {
    assertThat(rows("ProPdfSel", "{\"IdePro\":[" + profileId("RDR") + "]}"))
      .extracting(row -> String.valueOf(row.get("value"))).containsExactly("1");
    assertThat(rows("ProPdfSel", "{\"IdePro\":[" + profile + "]}"))
      .extracting(row -> String.valueOf(row.get("value"))).containsExactly("0");
  }

  @Test
  @DisplayName("ProDup copies the Contributor with the permissions of its file minus its exclusions as assignments")
  void duplicateCopiesThePermissionsOfTheProfileFile() throws Exception {
    ObjectNode duplicate = duplicateParameters(profileId("CTB"), "contributor");

    assertOk(maintain("ProDup", duplicate));

    Map<String, Object> copy = jdbcTemplate.queryForMap("SELECT * FROM AwePro WHERE Acr = ?", COPY_ACRONYM);
    assertThat(String.valueOf(copy.get("NAM")).trim()).isEqualTo(PREFIX + " copy");
    assertThat(copy.get("RES") == null ? "" : String.valueOf(copy.get("RES")).trim())
      .as("the copy does not keep the predefined profile file").isEmpty();
    int copyId = ((Number) copy.get("IDEPRO")).intValue();
    List<String> expected = aweElements.getPermissionCatalog().getPermissionIds().stream()
      .filter(id -> !id.startsWith("users.") && !id.startsWith("profiles.") && !id.startsWith("permissions."))
      .filter(id -> !aweElements.getPermissionCatalog().getGrantedAlways().contains(id))
      .sorted().toList();
    assertThat(assignments(copyId)).isEqualTo(expected);
    assertThat(count("SELECT COUNT(*) FROM HISAweProPer WHERE HISact = 'I' AND IdePro = " + copyId))
      .isEqualTo(expected.size());
    assertThat(count("SELECT COUNT(*) FROM HISAwePro WHERE HISact = 'I' AND Acr = '" + COPY_ACRONYM + "'"))
      .isEqualTo(1);
  }

  @Test
  @DisplayName("ProDup copies the assignments of a profile without file permissions as they are")
  void duplicateCopiesTheAssignments() throws Exception {
    assign(profile, "users.*");
    assign(profile, "themes.read");

    assertOk(maintain("ProDup", duplicateParameters(profile, "general")));

    Map<String, Object> copy = jdbcTemplate.queryForMap("SELECT * FROM AwePro WHERE Acr = ?", COPY_ACRONYM);
    assertThat(String.valueOf(copy.get("RES")).trim()).isEqualTo("general");
    assertThat(assignments(((Number) copy.get("IDEPRO")).intValue())).containsExactly("themes.read", "users.*");
    assertThat(assignments(profile)).containsExactly("themes.read", "users.*");
  }

  private ObjectNode parameters(int... profileIds) {
    ObjectNode parameters = objectMapper.createObjectNode();
    ArrayNode ids = parameters.putArray("IdePro");
    for (int id : profileIds) {
      ids.add(id);
    }
    parameters.put("max", 30);
    return parameters;
  }

  /**
   * Parameters of the profile panel with the selected permissions
   */
  private ObjectNode profileParameters(int profileId, String acronym, String... permissions) {
    ObjectNode parameters = parameters(profileId);
    parameters.put("Acr", acronym);
    parameters.put("Nam", PREFIX + " one");
    parameters.put("IdeThm", 1);
    parameters.putNull("ScrIni");
    parameters.putNull("Res");
    parameters.put("Act", 1);
    ArrayNode selected = parameters.putArray("Per");
    for (String permission : permissions) {
      selected.add(permission);
    }
    parameters.put("PerUpd", 1);
    return parameters;
  }

  private static Map<String, Object> option(List<Map<String, Object>> rows, String value) {
    List<Map<String, Object>> options = rows.stream().filter(row -> value.equals(row.get("value"))).toList();
    assertThat(options).as(value).hasSize(1);
    return options.getFirst();
  }

  private String locale(String key) {
    return aweElements.getLocaleWithLanguage(key, aweElements.getLanguage());
  }

  private ObjectNode duplicateParameters(int source, String restriction) {
    ObjectNode parameters = parameters(source);
    parameters.put("Acr", COPY_ACRONYM);
    parameters.put("Nam", PREFIX + " copy");
    parameters.put("IdeThm", 1);
    parameters.putNull("ScrIni");
    parameters.put("Res", restriction);
    parameters.put("Act", 1);
    return parameters;
  }

  private String maintain(String target, ObjectNode parameters) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(parameters))
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  private static void assertOk(String response) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }

  private static void assertRefused(String response, String acronym) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("error");
    List<String> messages = JsonPath.read(response, "$[?(@.type == 'message')].parameters.message");
    assertThat(messages).as(response).singleElement().asString().contains(acronym);
  }

  private List<Map<String, Object>> rows(String query, String parameters) throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", "test").session(session)).andReturn();
    String result = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content(parameters)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    List<List<Map<String, Object>>> rows = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows");
    assertThat(rows).as("fill of %s: %s", query, result).hasSize(1);
    return rows.getFirst();
  }

  private void assign(int profileId, String permission) {
    int id = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(IdeAweProPer), 0) FROM AweProPer", Integer.class) + 1;
    jdbcTemplate.update("INSERT INTO AweProPer (IdeAweProPer, IdePro, Per) VALUES (?, ?, ?)", id, profileId,
      permission);
  }

  /**
   * Assign a permission the editor offers and three it does not: a verb of one resource and an own action on every
   * resource, and a permission no longer in the catalog
   */
  private void assignUnavailable() {
    assign(profile, "users.read");
    assign(profile, "*.execute");
    assign(profile, "*.unlock");
    assign(profile, "q7rgone.read");
  }

  private List<String> assignments(int profileId) {
    return jdbcTemplate.queryForList("SELECT Per FROM AweProPer WHERE IdePro = ? ORDER BY Per", String.class,
      profileId).stream().map(String::trim).toList();
  }

  private int profileId(String acronym) {
    return jdbcTemplate.queryForObject("SELECT IdePro FROM AwePro WHERE Acr = ?", Integer.class, acronym);
  }

  private int count(String sql) {
    return jdbcTemplate.queryForObject(sql, Integer.class);
  }
}
