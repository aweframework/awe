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
import org.springframework.security.test.context.support.WithMockUser;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The mail server list (EmlSrvLst) returns, for the mail servers screen (email-server-admin), the alias, the host, the
 * security mode text (None when the server has none), the authentication (none, user and password or OAuth2) and the
 * state (active or inactive) of each server with its label and its tone. The search of the grid (CrtNam) finds servers by
 * alias ignoring case, the quick filter (CrtEmlSrvSta) by state, and the options query (EmlSrvStaCnt, with
 * EmlSrvStaCntAct and EmlSrvStaCntIna) counts the servers of each option with the same search.
 */
@Tag("integration")
@DisplayName("Mail server list query tests")
class EmailServerListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "emlsrvlst";
  private static final String ACTIVE = PREFIX + " active";
  private static final String INACTIVE = PREFIX + " inactive";
  private static final String SEARCH = "\"CrtNam\":\"" + PREFIX + "\"";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createServers() throws Exception {
    deleteServers();
    int id = nextServerId();
    insertServer(id, ACTIVE, 1, 0, null);
    insertServer(id + 1, INACTIVE, 0, 2, "STARTTLS");
  }

  @AfterEach
  void deleteServers() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweEmlSrv WHERE SrvNam LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void serverListReturnsHostSecurityAuthenticationAndState() throws Exception {
    List<Map<String, Object>> rows = rows("EmlSrvLst", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(names(rows)).containsExactlyInAnyOrder(ACTIVE, INACTIVE);
    assertThat(row(rows, ACTIVE))
      .containsEntry("Hst", "smtp.emlsrvlst.test")
      .containsEntry("SecTxt", "None")
      .containsEntry("AthTxt", "None")
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");
    assertThat(row(rows, INACTIVE))
      .containsEntry("SecTxt", "STARTTLS")
      .containsEntry("AthTxt", "OAuth2")
      .containsEntry("AthTon", "info")
      .containsEntry("AthIcn", "fa-key")
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void serverListSearchesByAliasIgnoringCase() throws Exception {
    assertThat(names(rows("EmlSrvLst", "{\"CrtNam\":\"" + INACTIVE.toUpperCase() + "\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("EmlSrvLst", "{\"CrtNam\":\"" + PREFIX.toUpperCase() + "\"}")))
      .containsExactlyInAnyOrder(ACTIVE, INACTIVE);
    assertThat(rows("EmlSrvLst", "{\"CrtNam\":\"nobody-matches-qzx-wqv\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void serverListFiltersByState() throws Exception {
    assertThat(names(rows("EmlSrvLst", "{" + SEARCH + ",\"CrtEmlSrvSta\":\"active\"}"))).containsExactly(ACTIVE);
    assertThat(names(rows("EmlSrvLst", "{" + SEARCH + ",\"CrtEmlSrvSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("EmlSrvLst", "{" + SEARCH + ",\"CrtEmlSrvSta\":\"\"}")))
      .containsExactlyInAnyOrder(ACTIVE, INACTIVE);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("EmlSrvStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), EmailServerListQueryTest::count)
      .containsExactly(
        tuple("", "SCREEN_TEXT_EMAIL_SERVER_FILTER_ALL", 2),
        tuple("active", "SCREEN_TEXT_EMAIL_SERVER_FILTER_ACTIVE", 1),
        tuple("inactive", "SCREEN_TEXT_EMAIL_SERVER_FILTER_INACTIVE", 1));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void countsFollowTheSearchAndNotTheChosenOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    assertThat(rows("EmlSrvStaCnt", "{\"CrtNam\":\"" + INACTIVE.toUpperCase() + "\",\"CrtEmlSrvSta\":\"active\"}"))
      .extracting(row -> String.valueOf(row.get("value")), EmailServerListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 0), tuple("inactive", 1));
  }

  private List<Map<String, Object>> rows(String query, String parameters) throws Exception {
    String result = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
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

  private static Map<String, Object> row(List<Map<String, Object>> rows, String name) {
    return rows.stream()
      .filter(row -> name.equals(String.valueOf(row.get("SrvNam")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + name));
  }

  private static List<String> names(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("SrvNam")).trim()).toList();
  }

  private int nextServerId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeAweEmlSrv) FROM AweEmlSrv");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void insertServer(int id, String name, int active, int authentication, String securityMode) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweEmlSrv (IdeAweEmlSrv, SrvNam, Hst, Ath, Act, SecMod) VALUES (?, ?, ?, ?, ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, name);
      statement.setString(3, "smtp.emlsrvlst.test");
      statement.setInt(4, authentication);
      statement.setInt(5, active);
      statement.setString(6, securityMode);
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
