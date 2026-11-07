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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Additional profiles column of the users screen (user-admin): the user list (UsrLst) returns, in AddProTxt, the names
 * of the additional profiles of each user (AweOpeRol) ordered by name and separated by "|" (the chip separator of the
 * column, so a name with a comma stays one chip), and null for a user without additional profiles (the cell shows no
 * chip).
 */
@Tag("integration")
@DisplayName("User additional profiles column tests")
class UserAdditionalProfilesColumnTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "usrcol774";
  private static final int FIRST_ID = 7740001;
  private static final int LAST_ID = 7740099;
  private static final String RANGE = " BETWEEN " + FIRST_ID + " AND " + LAST_ID;

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createUsersAndProfiles() throws Exception {
    deleteUsersAndProfiles();
    insertUser(7740001, PREFIX + "none");
    insertUser(7740002, PREFIX + "one");
    insertUser(7740003, PREFIX + "three");
    // Inserted out of order: the column lists them by name. A comma in a name is not a separator
    insertProfile(7740011, "Q7Z", PREFIX + " zeta, omega");
    insertProfile(7740012, "Q7A", PREFIX + " alpha");
    insertProfile(7740013, "Q7M", PREFIX + " mid");
    insertAdditionalProfile(7740021, 7740002, 7740013);
    insertAdditionalProfile(7740022, 7740003, 7740011);
    insertAdditionalProfile(7740023, 7740003, 7740012);
    insertAdditionalProfile(7740024, 7740003, 7740013);
  }

  @AfterEach
  void deleteUsersAndProfiles() throws Exception {
    execute("DELETE FROM AweOpeRol WHERE IdeAweOpeRol" + RANGE + " OR IdeOpe" + RANGE + " OR IdePro" + RANGE);
    execute("DELETE FROM ope WHERE IdeOpe" + RANGE);
    execute("DELETE FROM AwePro WHERE IdePro" + RANGE);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("UsrLst lists the additional profiles of each user by name separated by |, null without any")
  void userListShowsTheAdditionalProfilesOfEachUser() throws Exception {
    assertThat(rows("UsrLst", "{\"CrtUsrTxt\":\"" + PREFIX + "\"}"))
      .extracting(row -> String.valueOf(row.get("Nom")).trim(), row -> row.getOrDefault("AddProTxt", "missing"))
      .containsExactlyInAnyOrder(
        tuple(PREFIX + "none", null),
        tuple(PREFIX + "one", PREFIX + " mid"),
        tuple(PREFIX + "three", PREFIX + " alpha|" + PREFIX + " mid|" + PREFIX + " zeta, omega"));
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

  /**
   * Insert a user as a copy of the main data of test (its profile, theme and language)
   */
  private void insertUser(int id, String name) throws Exception {
    execute("INSERT INTO ope (IdeOpe, l1_nom, l1_act, PwdLck, NumLog, EmlAdr, OpeNam, IdePro, l1_lan) "
      + "SELECT " + id + ", '" + name + "', 1, 0, 0, '" + name + "@almis.example', '" + name + "', IdePro, l1_lan "
      + "FROM ope WHERE l1_nom = 'test'");
  }

  private void insertProfile(int id, String acronym, String name) throws Exception {
    execute("INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) VALUES (" + id + ", '" + acronym + "', '"
      + name + "', 1, NULL, NULL, 1)");
  }

  private void insertAdditionalProfile(int id, int user, int profile) throws Exception {
    execute("INSERT INTO AweOpeRol (IdeAweOpeRol, IdeOpe, IdePro) VALUES (" + id + ", " + user + ", " + profile + ")");
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
}
