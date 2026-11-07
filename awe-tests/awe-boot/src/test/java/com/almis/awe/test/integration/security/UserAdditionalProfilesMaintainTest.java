package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administration of the additional profiles of a user (AweOpeRol) in the users screen (user-admin): UsrNew and UsrUpd
 * store the profiles of the AddPro criterion without the main one, UsrDel and ProDel delete the rows of the user and of
 * the profile, and UsrAddProSel loads the current ones into the side panel.
 */
@Tag("integration")
@DisplayName("User additional profiles maintain tests")
class UserAdditionalProfilesMaintainTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "usrrol932";
  private static final String USER = PREFIX + "usr";
  private static final String NEW_USER = PREFIX + "new";

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;
  private int userId;
  private int main;
  private int first;
  private int second;

  @BeforeEach
  void createUserAndProfiles() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    deleteUserAndProfiles();
    main = number("SELECT MAX(IdePro) FROM AwePro") + 1000;
    first = main + 1;
    second = main + 2;
    insertProfile(main, "Q9M", " main");
    insertProfile(first, "Q9A", " first");
    insertProfile(second, "Q9B", " second");
    userId = support.copyTestUser(USER);
    execute("UPDATE ope SET IdePro = " + main + " WHERE IdeOpe = " + userId);
  }

  @AfterEach
  void deleteUserAndProfiles() throws Exception {
    String users = "SELECT IdeOpe FROM ope WHERE l1_nom LIKE '" + PREFIX + "%'";
    String profiles = "SELECT IdePro FROM AwePro WHERE Nam LIKE '" + PREFIX + "%'";
    execute("DELETE FROM AweOpeRol WHERE IdeOpe IN (" + users + ") OR IdePro IN (" + profiles + ")");
    execute("DELETE FROM HISAweOpeRol WHERE IdeOpe IN (" + users + ") OR IdePro IN (" + profiles + ")");
    support.deleteUser(USER);
    support.deleteUser(NEW_USER);
    execute("DELETE FROM AwePro WHERE Nam LIKE '" + PREFIX + "%'");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrNew stores the selected additional profiles of the new user, audited")
  void newUserStoresItsAdditionalProfiles() throws Exception {
    ObjectNode parameters = userParameters(main, first, second);
    parameters.putArray("IdeOpe");
    parameters.put("Usr", NEW_USER);
    parameters.put("Pas", "newpass1");
    parameters.put("PwdLck", 0);

    assertOk(maintain("UsrNew", parameters));

    int created = number("SELECT IdeOpe FROM ope WHERE l1_nom = '" + NEW_USER + "'");
    assertThat(additionalProfiles(created)).containsExactly(first, second);
    assertThat(number("SELECT COUNT(*) FROM HISAweOpeRol WHERE HISact = 'I' AND IdeOpe = " + created)).isEqualTo(2);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrNew without additional profiles stores none")
  void newUserWithoutAdditionalProfiles() throws Exception {
    ObjectNode parameters = userParameters(main);
    parameters.putArray("IdeOpe");
    parameters.put("Usr", NEW_USER);
    parameters.put("Pas", "newpass1");
    parameters.put("PwdLck", 0);

    assertOk(maintain("UsrNew", parameters));

    assertThat(additionalProfiles(number("SELECT IdeOpe FROM ope WHERE l1_nom = '" + NEW_USER + "'"))).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrUpd replaces the additional profiles of the user with the selected ones")
  void updateReplacesTheAdditionalProfiles() throws Exception {
    insertAdditionalProfile(first);

    assertOk(maintain("UsrUpd", updateParameters(main, second)));

    assertThat(additionalProfiles(userId)).containsExactly(second);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrUpd with no additional profile selected removes them all")
  void updateWithoutAdditionalProfilesRemovesThem() throws Exception {
    insertAdditionalProfile(first);
    insertAdditionalProfile(second);

    assertOk(maintain("UsrUpd", updateParameters(main)));

    assertThat(additionalProfiles(userId)).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The main profile is never stored as an additional profile: it leaves them when it becomes the main one")
  void mainProfileIsNotStored() throws Exception {
    insertAdditionalProfile(first);
    insertAdditionalProfile(second);

    assertOk(maintain("UsrUpd", updateParameters(first, first, second)));

    assertThat(number("SELECT IdePro FROM ope WHERE IdeOpe = " + userId)).isEqualTo(first);
    assertThat(additionalProfiles(userId)).containsExactly(second);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrUpd without additional profiles (legacy UsrUpd screen) keeps them, except the new main profile")
  void legacyUpdateKeepsTheAdditionalProfiles() throws Exception {
    insertAdditionalProfile(first);
    insertAdditionalProfile(second);
    ObjectNode parameters = updateParameters(main);
    parameters.remove("AddPro");
    parameters.remove("AddProUpd");
    parameters.put("IdeOpe", userId);

    assertOk(maintain("UsrUpd", parameters));
    assertThat(additionalProfiles(userId)).containsExactly(first, second);

    parameters.put("Pro", second);
    assertOk(maintain("UsrUpd", parameters));
    assertThat(additionalProfiles(userId)).containsExactly(first);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrDel deletes the additional profiles of the user with the user")
  void deleteUserDeletesItsAdditionalProfiles() throws Exception {
    insertAdditionalProfile(first);
    insertAdditionalProfile(second);

    assertOk(maintain("UsrDel", objectMapper.createObjectNode().<ObjectNode>set("IdeOpe",
      objectMapper.createArrayNode().add(userId))));

    assertThat(number("SELECT COUNT(*) FROM ope WHERE IdeOpe = " + userId)).isZero();
    assertThat(additionalProfiles(userId)).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ProDel deletes the rows of the profile as an additional profile of any user")
  void deleteProfileDeletesItsRows() throws Exception {
    insertAdditionalProfile(first);
    insertAdditionalProfile(second);

    assertOk(maintain("ProDel", objectMapper.createObjectNode().<ObjectNode>set("IdePro",
      objectMapper.createArrayNode().add(first))));

    assertThat(number("SELECT COUNT(*) FROM AwePro WHERE IdePro = " + first)).isZero();
    assertThat(additionalProfiles(userId)).containsExactly(second);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The side panel loads the current additional profiles of the selected user")
  void editQueryReturnsTheCurrentAdditionalProfiles() throws Exception {
    insertAdditionalProfile(second);
    insertAdditionalProfile(first);

    List<Map<String, Object>> rows = rows("UsrAddProSel", "{\"IdeOpeSel\":" + userId + "}");

    assertThat(rows).extracting(row -> String.valueOf(row.get("value"))).containsExactly(String.valueOf(first),
      String.valueOf(second));
    assertThat(rows).extracting(row -> String.valueOf(row.get("label"))).containsExactly("Q9A - " + PREFIX + " first",
      "Q9B - " + PREFIX + " second");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The check target of the additional profiles answers the label of each selected profile")
  void checkQueryReturnsTheLabelsOfTheSelectedProfiles() throws Exception {
    List<Map<String, Object>> rows = rows("UsrAddProIni", "{\"suggest\":[" + second + "," + first + "]}");

    assertThat(rows).extracting(row -> String.valueOf(row.get("value"))).containsExactly(String.valueOf(first),
      String.valueOf(second));
    assertThat(rows).extracting(row -> String.valueOf(row.get("label"))).containsExactly("Q9A - " + PREFIX + " first",
      "Q9B - " + PREFIX + " second");
  }

  /**
   * Parameters of the side panel of the users screen with a main profile and the selected additional profiles
   */
  private ObjectNode userParameters(int mainProfile, int... additionalProfiles) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("Sta", 1);
    parameters.put("Eml", PREFIX + "@almis.example");
    parameters.put("Lan", "en-GB");
    parameters.put("Nam", "Additional profiles");
    parameters.put("Pro", mainProfile);
    ArrayNode selected = parameters.putArray("AddPro");
    for (int profile : additionalProfiles) {
      selected.add(profile);
    }
    parameters.put("AddProUpd", 1);
    parameters.put("max", 30);
    return parameters;
  }

  private ObjectNode updateParameters(int mainProfile, int... additionalProfiles) {
    ObjectNode parameters = userParameters(mainProfile, additionalProfiles);
    parameters.set("IdeOpe", objectMapper.createArrayNode().add(userId));
    parameters.put("PwdLck", 0);
    parameters.put("FlgPwdUpd", 0);
    parameters.put("Pas", "");
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

  private void insertProfile(int id, String acronym, String name) throws Exception {
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + id + ", '" + acronym + "', '"
      + PREFIX + name + "', 1, NULL, NULL, 1)");
  }

  private void insertAdditionalProfile(int profile) throws Exception {
    int id = number("SELECT COALESCE(MAX(IdeAweOpeRol), 0) FROM AweOpeRol") + 1;
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + id + ", " + userId + ", " + profile
      + ")");
  }

  private List<Integer> additionalProfiles(int user) throws Exception {
    List<Integer> profiles = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT IdePro FROM AweOpeRol WHERE IdeOpe = " + user + " ORDER BY IdePro");
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        profiles.add(result.getInt(1));
      }
    }
    return profiles;
  }

  private void execute(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }

  private int number(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1);
    }
  }
}
