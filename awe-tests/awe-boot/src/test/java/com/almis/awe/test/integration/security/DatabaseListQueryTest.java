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
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The database list (DbsLst) returns, for the databases screen (database-admin), the name and the tone of the avatar of
 * the connection and its state (active or inactive) with its label and its tone, and keeps every previous field. The
 * search of the grid (CrtDbsTxt) finds connections by alias, description, connection or user, the quick filter
 * (CrtDbsSta) by state, and the options query (DbsStaCnt) counts the connections of each option with the same search.
 */
@Tag("integration")
@DisplayName("Database list query tests")
class DatabaseListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  // The alias of a connection takes 15 characters at most
  private static final String PREFIX = "dblst793";
  // Active, development
  private static final String DEVELOPMENT = PREFIX + " dev";
  // Active, production
  private static final String PRODUCTION = PREFIX + " pro";
  // Inactive
  private static final String INACTIVE = PREFIX + " ina";
  private static final String SEARCH = "\"CrtDbsTxt\":\"" + PREFIX + "\"";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createDatabases() throws Exception {
    deleteDatabases();
    int id = nextDatabaseId();
    insertDatabase(id, DEVELOPMENT, "Ledger of the north", "J", "Des", "hsql", "jdbc:hsqldb:mem:qzxledger793", "ledgerusr793", 1);
    insertDatabase(id + 1, PRODUCTION, null, "D", "Pro", "ora", "java:comp/env/jdbc/qzxpayroll793", null, 1);
    insertDatabase(id + 2, INACTIVE, "Old archive", "J", "Pre", "h2", "jdbc:h2:mem:qzxarchive793", "archusr793", 0);
  }

  @AfterEach
  void deleteDatabases() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweDbs WHERE Als LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void databaseListReturnsAvatarAndState() throws Exception {
    List<Map<String, Object>> rows = rows("DbsLst", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(aliases(rows)).containsExactlyInAnyOrder(DEVELOPMENT, PRODUCTION, INACTIVE);
    assertThat(row(rows, DEVELOPMENT))
      .containsEntry("Des", "Ledger of the north")
      .containsEntry("TypTxt", "Development")
      .containsEntry("DbtTxt", "HSQL")
      .containsEntry("DctTxt", "Jdbc")
      .containsEntry("Ini", DEVELOPMENT)
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");
    assertThat(row(rows, PRODUCTION))
      .containsEntry("TypTxt", "Production")
      .containsEntry("DbtTxt", "ORACLE")
      .containsEntry("DctTxt", "Datasource")
      .containsEntry("StaCod", "active");
    assertThat(row(rows, INACTIVE))
      .containsEntry("TypTxt", "Preproduction")
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");

    // The tone of the avatar depends only on the identifier, so a connection always gets the same colour
    List<String> tones = List.of("primary", "info", "success", "warning", "danger", "neutral");
    rows.forEach(row -> assertThat(row).containsEntry("IniTon",
      tones.get(Integer.parseInt(String.valueOf(row.get("IdeDbs"))) % tones.size())));

    // The previous fields stay: the update and view screens and the side panel load them from the selected row
    assertThat(row(rows, DEVELOPMENT)).containsKeys("id", "IdeDbs", "Als", "Des", "Dct", "DctTxt", "Typ", "TypTxt", "Dbc",
        "Dbt", "DbtTxt", "DbsUsr", "Drv", "DrvTxt", "Act", "ActIco")
      .containsEntry("Dct", "J").containsEntry("Typ", "Des").containsEntry("Dbt", "hsql")
      .containsEntry("Dbc", "jdbc:hsqldb:mem:qzxledger793").containsEntry("DbsUsr", "ledgerusr793");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void databaseListDoesNotSendThePassword() throws Exception {
    // The response of a query holds every field of the query, also the ones without a grid column
    String storedPassword = "qzxStoredPwd793";
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE AweDbs SET DbsPwd = ? WHERE Als = ?")) {
      statement.setString(1, storedPassword);
      statement.setString(2, DEVELOPMENT);
      assertThat(statement.executeUpdate()).isEqualTo(1);
      commit(connection);
    }

    List<Map<String, Object>> rows = rows("DbsLst", "{" + SEARCH + "}");

    assertThat(rows).hasSize(3).allSatisfy(row -> assertThat(row).doesNotContainKey("DbsPwd").doesNotContainValue(storedPassword));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theStateIsTranslated() throws Exception {
    assertThat(row(rows("DbsLst", "{" + SEARCH + ",\"lang\":\"es-ES\"}"), INACTIVE))
      .containsEntry("StaTxt", "Inactivo").containsEntry("TypTxt", "Preprodución");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void databaseListSearchesByAliasDescriptionConnectionOrUserIgnoringCase() throws Exception {
    assertThat(aliases(rows("DbsLst", "{\"CrtDbsTxt\":\"" + PREFIX.toUpperCase() + " DEV\"}"))).containsExactly(DEVELOPMENT);
    assertThat(aliases(rows("DbsLst", "{\"CrtDbsTxt\":\"OLD ARCHIVE\"}"))).containsExactly(INACTIVE);
    assertThat(aliases(rows("DbsLst", "{\"CrtDbsTxt\":\"QZXPAYROLL793\"}"))).containsExactly(PRODUCTION);
    assertThat(aliases(rows("DbsLst", "{\"CrtDbsTxt\":\"LedgerUsr793\"}"))).containsExactly(DEVELOPMENT);
    assertThat(rows("DbsLst", "{\"CrtDbsTxt\":\"nobody-matches-qzx793-wqv793\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void databaseListWithAnEmptySearchShowsEveryConnection() throws Exception {
    List<String> all = aliases(rows("DbsLst", "{}"));

    assertThat(aliases(rows("DbsLst", "{\"CrtDbsTxt\":\"\"}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(aliases(rows("DbsLst", "{\"CrtDbsTxt\":null}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(all).contains(DEVELOPMENT, PRODUCTION, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void databaseListFiltersByState() throws Exception {
    assertThat(aliases(rows("DbsLst", "{" + SEARCH + ",\"CrtDbsSta\":\"active\"}"))).containsExactlyInAnyOrder(DEVELOPMENT, PRODUCTION);
    assertThat(aliases(rows("DbsLst", "{" + SEARCH + ",\"CrtDbsSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(aliases(rows("DbsLst", "{" + SEARCH + ",\"CrtDbsSta\":\"\"}"))).containsExactlyInAnyOrder(DEVELOPMENT, PRODUCTION, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void databaseListKeepsItsPreviousFilters() throws Exception {
    // The criteria of the deprecated Dbs screen still filter the list
    assertThat(aliases(rows("DbsLst", "{" + SEARCH + ",\"CrtAct\":0}"))).containsExactly(INACTIVE);
    assertThat(aliases(rows("DbsLst", "{" + SEARCH + ",\"CrtDct\":\"D\"}"))).containsExactly(PRODUCTION);
    assertThat(aliases(rows("DbsLst", "{" + SEARCH + ",\"CrtTyp\":\"Des\"}"))).containsExactly(DEVELOPMENT);
    int id = Integer.parseInt(String.valueOf(row(rows("DbsLst", "{" + SEARCH + "}"), PRODUCTION).get("IdeDbs")));
    assertThat(aliases(rows("DbsLst", "{\"CrtAls\":" + id + "}"))).containsExactly(PRODUCTION);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("DbsStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), DatabaseListQueryTest::count,
        row -> String.valueOf(row.get("tone")))
      .containsExactly(
        tuple("", "SCREEN_TEXT_DATABASE_FILTER_ALL", 3, ""),
        tuple("active", "SCREEN_TEXT_DATABASE_FILTER_ACTIVE", 2, ""),
        tuple("inactive", "SCREEN_TEXT_DATABASE_FILTER_INACTIVE", 1, ""));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsFollowTheSearchAndTheOtherFilters() throws Exception {
    assertThat(rows("DbsStaCnt", "{\"CrtDbsTxt\":\"archusr793\"}"))
      .extracting(row -> String.valueOf(row.get("value")), DatabaseListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 0), tuple("inactive", 1));
    assertThat(rows("DbsStaCnt", "{" + SEARCH + ",\"CrtTyp\":\"Pro\"}"))
      .extracting(row -> String.valueOf(row.get("value")), DatabaseListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 1), tuple("inactive", 0));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfRowsOfItsOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    for (Map<String, Object> option : rows("DbsStaCnt", "{" + SEARCH + ",\"CrtDbsSta\":\"inactive\"}")) {
      String value = String.valueOf(option.get("value"));
      assertThat(rows("DbsLst", "{" + SEARCH + ",\"CrtDbsSta\":\"" + value + "\"}"))
        .as("rows of option '%s'", value).hasSize(count(option));
    }
    // Without search, All is every connection
    List<Map<String, Object>> counts = rows("DbsStaCnt", "{}");
    assertThat(count(counts.getFirst())).isEqualTo(rows("DbsLst", "{}").size());
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

  private static Map<String, Object> row(List<Map<String, Object>> rows, String alias) {
    return rows.stream()
      .filter(row -> alias.equals(String.valueOf(row.get("Als")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + alias));
  }

  private static List<String> aliases(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("Als")).trim()).toList();
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

  private int nextDatabaseId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeDbs) FROM AweDbs");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void insertDatabase(int id, String alias, String description, String connectionType, String environment,
                              String databaseType, String connection, String user, int active) throws Exception {
    try (Connection jdbc = dataSource.getConnection();
         PreparedStatement statement = jdbc.prepareStatement(
           "INSERT INTO AweDbs (IdeDbs, Als, Des, Dct, Typ, Dbt, Dbc, DbsUsr, Act) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, alias);
      statement.setString(3, description);
      statement.setString(4, connectionType);
      statement.setString(5, environment);
      statement.setString(6, databaseType);
      statement.setString(7, connection);
      statement.setString(8, user);
      statement.setInt(9, active);
      assertThat(statement.executeUpdate()).isEqualTo(1);
      commit(jdbc);
    }
  }

  private static void commit(Connection connection) throws Exception {
    if (!connection.getAutoCommit()) {
      connection.commit();
    }
  }
}
