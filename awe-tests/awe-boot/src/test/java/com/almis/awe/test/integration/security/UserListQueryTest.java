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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The user list (UsrLst) returns, for the users screen, the full name, a secondary line with the login and the email and
 * the state of the account (locked, inactive or active) with its label and its tone, the name and the tone of the
 * avatar of the user, and the text, icon and tone of its two-factor tag. It keeps every previous field.
 */
@Tag("integration")
@DisplayName("User list query tests")
class UserListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String PREFIX = "usrlst793";
  private static final String LOCKED_USER = PREFIX + "lck";
  private static final String INACTIVE_USER = PREFIX + "ina";
  private static final String NO_EMAIL_USER = PREFIX + "nml";
  private static final String ONE_WORD_USER = PREFIX + "one";
  private static final String EMPTY_NAME_USER = PREFIX + "emp";
  private static final String BLANK_NAME_USER = PREFIX + "blk";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createUsers() throws Exception {
    int id = nextUserId();
    insertUser(id, LOCKED_USER, "Locked Person", "locked@almis.example", 1, 1);
    insertUser(id + 1, INACTIVE_USER, null, "inactive@almis.example", 0, 0);
    insertUser(id + 2, NO_EMAIL_USER, "No Email", "", 1, 0);
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
  void userListReturnsNameSecondaryLineAndState() throws Exception {
    List<Map<String, Object>> rows = userList("{\"CrtUsr\":\"" + PREFIX + "\",\"lang\":\"en-GB\"}");

    assertThat(rows).hasSize(3);
    Map<String, Object> locked = row(rows, LOCKED_USER);
    assertThat(locked)
      .containsEntry("FulNam", "Locked Person")
      .containsEntry("UsrSec", LOCKED_USER + " · locked@almis.example")
      .containsEntry("StaCod", "locked")
      .containsEntry("StaTxt", "Locked")
      .containsEntry("StaTon", "danger");

    // Without a full name the main line is the login; an inactive user is not locked
    Map<String, Object> inactive = row(rows, INACTIVE_USER);
    assertThat(inactive)
      .containsEntry("FulNam", INACTIVE_USER)
      .containsEntry("UsrSec", INACTIVE_USER + " · inactive@almis.example")
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");

    // Without an email the secondary line is just the login
    Map<String, Object> noEmail = row(rows, NO_EMAIL_USER);
    assertThat(noEmail)
      .containsEntry("FulNam", "No Email")
      .containsEntry("UsrSec", NO_EMAIL_USER)
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");

    // The previous fields stay
    assertThat(locked).containsKeys("IdeOpe", "Nom", "Nam", "ProTxt", "Sta", "Lck", "Eml", "LanTxt", "StaIco", "BlkIco", "2faIco");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListReturnsAvatarNameAndTone() throws Exception {
    int id = nextUserId();
    insertUser(id, ONE_WORD_USER, "Admin", "", 1, 0);
    insertUser(id + 1, EMPTY_NAME_USER, "", "", 1, 0);
    insertUser(id + 2, BLANK_NAME_USER, "   ", "", 1, 0);

    List<Map<String, Object>> rows = userList("{\"CrtUsr\":\"" + PREFIX + "\",\"lang\":\"en-GB\"}");

    // The avatar is built from the full name (the engine takes its initials), or from the login without a full name
    assertThat(row(rows, LOCKED_USER)).containsEntry("Ini", "Locked Person");
    assertThat(row(rows, ONE_WORD_USER)).containsEntry("Ini", "Admin");
    assertThat(row(rows, INACTIVE_USER)).containsEntry("Ini", INACTIVE_USER);
    assertThat(row(rows, EMPTY_NAME_USER)).containsEntry("Ini", EMPTY_NAME_USER);
    assertThat(row(rows, BLANK_NAME_USER)).containsEntry("Ini", BLANK_NAME_USER);

    // The tone depends only on the identifier of the user, so the same user always gets the same colour
    List<String> tones = List.of("primary", "info", "success", "warning", "danger", "neutral");
    rows.forEach(row -> assertThat(row).containsEntry("IniTon",
      tones.get(Integer.parseInt(String.valueOf(row.get("IdeOpe"))) % tones.size())));
    assertThat(rows.stream().map(row -> row.get("IniTon")).distinct().count()).isGreaterThan(1);
    List<Map<String, Object>> again = userList("{\"CrtUsr\":\"" + PREFIX + "\",\"lang\":\"en-GB\"}");
    rows.forEach(row -> assertThat(row(again, String.valueOf(row.get("Nom")).trim())).containsEntry("IniTon", row.get("IniTon")));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListReturnsTwoFactorTextIconAndTone() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET enable2fa = 1 WHERE l1_nom = ?")) {
      statement.setString(1, LOCKED_USER);
      assertThat(statement.executeUpdate()).isEqualTo(1);
      commit(connection);
    }

    List<Map<String, Object>> rows = userList("{\"CrtUsr\":\"" + PREFIX + "\",\"lang\":\"en-GB\"}");

    // With two-factor: "On" with the shield icon; without it: "Off" without icon. The tag is always neutral
    assertThat(row(rows, LOCKED_USER))
      .containsEntry("2faTxt", "On")
      .containsEntry("2faIcn", "lucide:shield-check")
      .containsEntry("2faTon", "neutral")
      .containsKey("2faIco");
    Map<String, Object> without = row(rows, INACTIVE_USER);
    assertThat(without).containsEntry("2faTxt", "Off").containsEntry("2faTon", "neutral");
    assertThat(String.valueOf(without.get("2faIcn")).trim()).isIn("", "null");
    assertThat(row(rows, NO_EMAIL_USER)).containsEntry("2faTxt", "Off");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListReturnsTheSubtitleAndTheTwoFactorTagOfTheSidePanelHeader() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET enable2fa = 1 WHERE l1_nom = ?")) {
      statement.setString(1, LOCKED_USER);
      assertThat(statement.executeUpdate()).isEqualTo(1);
      commit(connection);
    }

    List<Map<String, Object>> rows = userList("{\"CrtUsr\":\"" + PREFIX + "\",\"lang\":\"en-GB\"}");

    // Subtitle of the header: the login and the name of the profile
    String profile = String.valueOf(row(rows, LOCKED_USER).get("ProTxt")).trim();
    assertThat(profile).isNotBlank();
    assertThat(row(rows, LOCKED_USER)).containsEntry("UsrPro", LOCKED_USER + " · " + profile);
    assertThat(row(rows, INACTIVE_USER)).containsEntry("UsrPro", INACTIVE_USER + " · " + profile);
    // Two-factor tag of the header: "Two-factor on" only when it is on, empty (the tag is hidden) when it is off
    assertThat(row(rows, LOCKED_USER)).containsEntry("2faHdr", "Two-factor on");
    assertThat(String.valueOf(row(rows, INACTIVE_USER).get("2faHdr")).trim()).isIn("", "null");
    // The fields of the grid stay
    assertThat(row(rows, LOCKED_USER)).containsEntry("2faTxt", "On").containsEntry("FulNam", "Locked Person");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userListFiltersByState() throws Exception {
    assertThat(names(userList("{\"CrtUsr\":\"" + PREFIX + "\",\"CrtUsrSta\":\"locked\"}"))).containsExactly(LOCKED_USER);
    assertThat(names(userList("{\"CrtUsr\":\"" + PREFIX + "\",\"CrtUsrSta\":\"inactive\"}"))).containsExactly(INACTIVE_USER);
    assertThat(names(userList("{\"CrtUsr\":\"" + PREFIX + "\",\"CrtUsrSta\":\"active\"}"))).containsExactly(NO_EMAIL_USER);
  }

  private List<Map<String, Object>> userList(String parameters) throws Exception {
    String result = mockMvc.perform(post("/action/data/UsrLst")
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(parameters)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    List<List<Map<String, Object>>> rows = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows");
    assertThat(rows).hasSize(1);
    return rows.getFirst();
  }

  private static Map<String, Object> row(List<Map<String, Object>> rows, String user) {
    return rows.stream()
      .filter(row -> user.equals(String.valueOf(row.get("Nom")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + user));
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

  private void insertUser(int id, String name, String fullName, String email, int active, int locked) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO ope (IdeOpe, l1_nom, l1_act, PwdLck, NumLog, EmlAdr, OpeNam, IdePro, l1_lan, enable2fa) "
             + "SELECT ?, ?, ?, ?, 0, ?, ?, IdePro, 'en-GB', 0 FROM ope WHERE l1_nom = ?")) {
      statement.setInt(1, id);
      statement.setString(2, name);
      statement.setInt(3, active);
      statement.setInt(4, locked);
      statement.setString(5, email);
      statement.setString(6, fullName);
      statement.setString(7, SIGNED_IN_USER);
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
