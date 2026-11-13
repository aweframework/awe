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
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quick filters of the users screen (user-admin): the options query (UsrStaCnt) returns All, Locked, Without 2FA and
 * Inactive with their counts, and each count is the number of rows the user list (UsrLst) shows for that option, with
 * the same search criteria. The search of the grid (CrtUsrTxt) filters both by full name, login, email or profile name.
 */
@Tag("integration")
@DisplayName("User state quick filter tests")
class UserStateQuickFilterTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String PREFIX = "usrqck793";
  private static final String LOCKED_2FA = PREFIX + "lck";
  private static final String LOCKED_INACTIVE = PREFIX + "lckina";
  private static final String INACTIVE_NULL_2FA = PREFIX + "ina";
  private static final String ACTIVE_NO_2FA = PREFIX + "act";
  private static final String ACTIVE_2FA = PREFIX + "act2fa";
  private static final String SEARCH = "\"CrtUsr\":\"" + PREFIX + "\"";
  // Words of a single user: its full name, its email and its profile (operator, the others have the one of test)
  private static final String FULL_NAME = "Fullname Qzx793";
  private static final String EMAIL = "mailbox.wqv793@almis.example";
  private static final String PROFILE = "operator";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createUsers() throws Exception {
    int id = nextUserId();
    insertUser(id, LOCKED_2FA, 1, 1, 1, LOCKED_2FA, EMAIL, null);
    // Locked and inactive: the grid shows it as locked, so it counts only as locked
    insertUser(id + 1, LOCKED_INACTIVE, 0, 1, 0);
    insertUser(id + 2, INACTIVE_NULL_2FA, 0, 0, null);
    insertUser(id + 3, ACTIVE_NO_2FA, 1, 0, 0, FULL_NAME, ACTIVE_NO_2FA + "@almis.example", null);
    insertUser(id + 4, ACTIVE_2FA, 1, 0, 1, ACTIVE_2FA, ACTIVE_2FA + "@almis.example", PROFILE);
  }

  @AfterEach
  void deleteUsers() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM ope WHERE l1_nom LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListFiltersUsersWithout2fa() throws Exception {
    // Two-factor disabled (0) or never set (null), whatever the state of the account
    assertThat(names(rows("UsrLst", "{" + SEARCH + ",\"CrtUsrSta\":\"no2fa\"}")))
      .containsExactlyInAnyOrder(LOCKED_INACTIVE, INACTIVE_NULL_2FA, ACTIVE_NO_2FA);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListKeepsTheStateChoices() throws Exception {
    assertThat(names(rows("UsrLst", "{" + SEARCH + "}")))
      .containsExactlyInAnyOrder(LOCKED_2FA, LOCKED_INACTIVE, INACTIVE_NULL_2FA, ACTIVE_NO_2FA, ACTIVE_2FA);
    assertThat(names(rows("UsrLst", "{" + SEARCH + ",\"CrtUsrSta\":\"locked\"}")))
      .containsExactlyInAnyOrder(LOCKED_2FA, LOCKED_INACTIVE);
    assertThat(names(rows("UsrLst", "{" + SEARCH + ",\"CrtUsrSta\":\"inactive\"}")))
      .containsExactly(INACTIVE_NULL_2FA);
    assertThat(names(rows("UsrLst", "{" + SEARCH + ",\"CrtUsrSta\":\"active\"}")))
      .containsExactlyInAnyOrder(ACTIVE_NO_2FA, ACTIVE_2FA);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrderWithTheirTone() throws Exception {
    assertThat(rows("UsrStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), row -> count(row),
        row -> String.valueOf(row.get("tone")))
      .containsExactly(
        tuple("", "SCREEN_TEXT_USER_FILTER_ALL", 5, ""),
        tuple("locked", "SCREEN_TEXT_USER_FILTER_LOCKED", 2, "danger"),
        tuple("no2fa", "SCREEN_TEXT_USER_FILTER_WITHOUT_2FA", 3, ""),
        tuple("inactive", "SCREEN_TEXT_USER_FILTER_INACTIVE", 1, ""));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfRowsOfItsOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    for (Map<String, Object> option : rows("UsrStaCnt", "{" + SEARCH + ",\"CrtUsrSta\":\"locked\"}")) {
      String value = String.valueOf(option.get("value"));
      List<Map<String, Object>> gridRows = rows("UsrLst", "{" + SEARCH + ",\"CrtUsrSta\":\"" + value + "\"}");
      assertThat(gridRows).as("rows of option '%s'", value).hasSize(count(option));
    }
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsFollowTheSearchCriteria() throws Exception {
    assertThat(rows("UsrStaCnt", "{\"CrtUsr\":\"" + LOCKED_INACTIVE + "\"}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> count(row))
      .containsExactly(tuple("", 1), tuple("locked", 1), tuple("no2fa", 1), tuple("inactive", 0));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListSearchesByFullNameLoginEmailOrProfileIgnoringCase() throws Exception {
    assertThat(names(rows("UsrLst", "{\"CrtUsrTxt\":\"qZX793\"}"))).containsExactly(ACTIVE_NO_2FA);
    assertThat(names(rows("UsrLst", "{\"CrtUsrTxt\":\"" + INACTIVE_NULL_2FA.toUpperCase() + "\"}"))).containsExactly(INACTIVE_NULL_2FA);
    assertThat(names(rows("UsrLst", "{\"CrtUsrTxt\":\"BOX.WQV793\"}"))).containsExactly(LOCKED_2FA);
    assertThat(names(rows("UsrLst", "{" + SEARCH + ",\"CrtUsrTxt\":\"OPERAT\"}"))).containsExactly(ACTIVE_2FA);
    assertThat(rows("UsrLst", "{\"CrtUsrTxt\":\"nobody-matches-qzx793-wqv793\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListWithAnEmptySearchShowsEveryUser() throws Exception {
    List<String> all = names(rows("UsrLst", "{}"));

    assertThat(names(rows("UsrLst", "{\"CrtUsrTxt\":\"\"}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(names(rows("UsrLst", "{\"CrtUsrTxt\":null}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(all).contains(SIGNED_IN_USER, LOCKED_2FA, LOCKED_INACTIVE, INACTIVE_NULL_2FA, ACTIVE_NO_2FA, ACTIVE_2FA);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListSearchKeepsTheOtherFilters() throws Exception {
    // The search and the quick filter together: the users of the prefix whose login has "lck" and have no 2FA
    assertThat(names(rows("UsrLst", "{\"CrtUsrTxt\":\"" + PREFIX + "LCK\",\"CrtUsrSta\":\"no2fa\"}")))
      .containsExactly(LOCKED_INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsFollowTheSearchOfTheGrid() throws Exception {
    assertThat(rows("UsrStaCnt", "{\"CrtUsrTxt\":\"wqv793\"}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> count(row))
      .containsExactly(tuple("", 1), tuple("locked", 1), tuple("no2fa", 0), tuple("inactive", 0));
    assertThat(rows("UsrStaCnt", "{" + SEARCH + ",\"CrtUsrTxt\":\"Operator\"}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> count(row))
      .containsExactly(tuple("", 1), tuple("locked", 0), tuple("no2fa", 0), tuple("inactive", 0));
    assertThat(rows("UsrStaCnt", "{\"CrtUsrTxt\":\"" + PREFIX.toUpperCase() + "\"}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> count(row))
      .containsExactly(tuple("", 5), tuple("locked", 2), tuple("no2fa", 3), tuple("inactive", 1));
    // Each count is the number of rows of its option with the same search
    for (Map<String, Object> option : rows("UsrStaCnt", "{\"CrtUsrTxt\":\"" + PREFIX + "lck\"}")) {
      String value = String.valueOf(option.get("value"));
      assertThat(rows("UsrLst", "{\"CrtUsrTxt\":\"" + PREFIX + "lck\",\"CrtUsrSta\":\"" + value + "\"}"))
        .as("rows of option '%s'", value).hasSize(count(option));
    }
  }

  private List<Map<String, Object>> rows(String query, String parameters) throws Exception {
    String result = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(parameters)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    List<List<Map<String, Object>>> rows = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows");
    assertThat(rows).as("fill of %s: %s", query, result).hasSize(1);
    return rows.getFirst();
  }

  private static int count(Map<String, Object> row) {
    return Integer.parseInt(String.valueOf(row.get("count")));
  }

  private static List<String> names(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("Nom")).trim()).toList();
  }

  private MockHttpSession signedInSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user")
        .with(csrf())
        .param("value", SIGNED_IN_USER)
        .session(session))
      .andReturn();
    return session;
  }

  private int nextUserId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeOpe) FROM ope");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void insertUser(int id, String name, int active, int locked, Integer twoFactor) throws Exception {
    insertUser(id, name, active, locked, twoFactor, name, name + "@almis.example", null);
  }

  /**
   * Insert a user with its full name, its email and a profile by its name (null: the profile of test)
   */
  private void insertUser(int id, String name, int active, int locked, Integer twoFactor, String fullName, String email,
                          String profile) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO ope (IdeOpe, l1_nom, l1_act, PwdLck, NumLog, EmlAdr, OpeNam, IdePro, l1_lan, enable2fa) "
             + "SELECT ?, ?, ?, ?, 0, ?, ?, COALESCE((SELECT IdePro FROM AwePro WHERE Nam = ?), IdePro), 'en-GB', ? "
             + "FROM ope WHERE l1_nom = ?")) {
      statement.setInt(1, id);
      statement.setString(2, name);
      statement.setInt(3, active);
      statement.setInt(4, locked);
      statement.setString(5, email);
      statement.setString(6, fullName);
      statement.setString(7, profile);
      if (twoFactor == null) {
        statement.setNull(8, Types.INTEGER);
      } else {
        statement.setInt(8, twoFactor);
      }
      statement.setString(9, SIGNED_IN_USER);
      assertThat(statement.executeUpdate()).isEqualTo(1);
      commit(connection);
    }
  }

  private static void commit(Connection connection) throws Exception {
    if (!connection.getAutoCommit()) {
      connection.commit();
    }
  }
}
