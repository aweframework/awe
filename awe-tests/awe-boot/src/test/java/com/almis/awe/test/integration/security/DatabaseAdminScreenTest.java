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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The databases screen (database-admin) sends the identifier of the connections from its grid, as a list, to the
 * maintains it reuses (DbsNew, DbsUpd, DbsDel), with the criteria of its side panel and the rows of its sites and modules
 * grid (SitModDbsLst). With one selected row the grid also sends the identifier as a single value (IdeDbs.selected),
 * which DbsUpd uses for every row of the sites and modules grid. DbsUpd keeps working with the identifier as a single
 * value (legacy DbsUpd screen) and only writes the password when FlgPwd is 1. DbsDel is a multiple maintain, so it needs
 * the list that every grid sends (the legacy Dbs screen too).
 */
@Tag("integration")
@DisplayName("Database admin screen tests")
class DatabaseAdminScreenTest extends AbstractSpringAppIntegrationTest {

  // The alias of a connection takes 15 characters at most
  private static final String PREFIX = "dbsadm793";
  private static final String DATABASE = PREFIX + " one";
  private static final String OTHER_DATABASE = PREFIX + " two";
  private static final String NEW_DATABASE = PREFIX + " new";
  private static final String STORED_PASSWORD = "stored-secret-793";
  private static final String SIGNED_IN_USER = "dbsadm793usr";
  // Sites and modules of the test data
  private static final int MADRID = 10;
  private static final int ONATE = 17;
  private static final int BASE = 28;
  private static final int TEST = 916;

  @Autowired
  private DataSource dataSource;

  private int databaseId;
  private int otherId;
  private int siteRowId;

  @BeforeEach
  void createDatabases() throws Exception {
    deleteDatabases();
    databaseId = nextId("IdeDbs", "AweDbs");
    otherId = databaseId + 1;
    siteRowId = nextId("IdeSitModDbs", "AweSitModDbs");
    execute("INSERT INTO AweDbs (IdeDbs, Als, Des, Dct, Typ, Dbt, Dbc, Drv, DbsUsr, DbsPwd, Act) VALUES (" + databaseId + ", '"
      + DATABASE + "', 'First', 'J', 'Des', 'hsql', 'jdbc:hsqldb:mem:dbsadm793one', 'org.hsqldb.jdbc.JDBCDriver', 'usr1', '"
      + STORED_PASSWORD + "', 0)");
    execute("INSERT INTO AweDbs (IdeDbs, Als, Des, Dct, Typ, Dbt, Dbc, DbsUsr, DbsPwd, Act) VALUES (" + otherId + ", '"
      + OTHER_DATABASE + "', 'Second', 'J', 'Pro', 'ora', 'jdbc:oracle:thin:@nowhere:1521:dbsadm793', 'usr2', '"
      + STORED_PASSWORD + "', 0)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + siteRowId + ", " + MADRID + ", "
      + BASE + ", " + databaseId + ", 1)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + (siteRowId + 1) + ", " + ONATE
      + ", " + BASE + ", " + otherId + ", 1)");
  }

  @AfterEach
  void deleteDatabases() throws Exception {
    execute("DELETE FROM AweSitModDbs WHERE IdeDbs IN (SELECT IdeDbs FROM AweDbs WHERE Als LIKE '" + PREFIX + "%')");
    execute("DELETE FROM AweDbs WHERE Als LIKE '" + PREFIX + "%'");
    execute("DELETE FROM HISAweDbs WHERE Als LIKE '" + PREFIX + "%'");
    new SecurityAuditTestSupport(dataSource).deleteUser(SIGNED_IN_USER);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsUpd updates only the connection and keeps every site row on it when the grid sends its identifier as a list")
  void updateWithTheIdentifierAsAList() throws Exception {
    ObjectNode parameters = panel(DATABASE + "x", "", "0");
    parameters.set("IdeDbs", objectMapper.createArrayNode().add(databaseId));
    parameters.put("IdeDbs.selected", databaseId);
    // The stored row changes its order, and two rows are added
    sites(parameters, rowTypes("UPDATE", "INSERT", "INSERT"), ids(siteRowId, null, null), ints(MADRID, ONATE, MADRID),
      ints(BASE, BASE, TEST), ints(4, 5, 6));

    assertOk(maintain("DbsUpd", parameters));

    assertThat(database(databaseId)).containsEntry("ALS", DATABASE + "x").containsEntry("DES", "Changed")
      .containsEntry("DCT", "D").containsEntry("TYP", "Pre").containsEntry("DBT", "h2").containsEntry("DBC", "java:comp/env/jdbc/x")
      .containsEntry("DRV", "org.h2.Driver").containsEntry("DBSUSR", "usrx").containsEntry("ACT", "0");
    assertThat(sites(databaseId)).containsExactly(MADRID + "/" + BASE + "/4", ONATE + "/" + BASE + "/5", MADRID + "/" + TEST + "/6");
    assertThat(database(otherId)).containsEntry("ALS", OTHER_DATABASE).containsEntry("DES", "Second");
    assertThat(sites(otherId)).containsExactly(ONATE + "/" + BASE + "/1");
    // Audited with its identifier
    assertThat(select("SELECT * FROM HISAweDbs WHERE Als = '" + DATABASE + "x' AND HISact = 'U'"))
      .containsEntry("IDEDBS", String.valueOf(databaseId));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsUpd keeps updating the connection and its site rows when the identifier is a single value (legacy screen)")
  void updateWithTheIdentifierAsAValue() throws Exception {
    ObjectNode parameters = panel(DATABASE + "v", "", "0");
    parameters.put("IdeDbs", databaseId);
    sites(parameters, rowTypes("DELETE", "INSERT", "INSERT"), ids(siteRowId, null, null), ints(MADRID, ONATE, MADRID),
      ints(BASE, TEST, TEST), ints(1, 2, 3));

    assertOk(maintain("DbsUpd", parameters));

    assertThat(database(databaseId)).containsEntry("ALS", DATABASE + "v").containsEntry("DES", "Changed");
    assertThat(sites(databaseId)).containsExactly(ONATE + "/" + TEST + "/2", MADRID + "/" + TEST + "/3");
    assertThat(database(otherId)).containsEntry("ALS", OTHER_DATABASE);
    assertThat(sites(otherId)).containsExactly(ONATE + "/" + BASE + "/1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsUpd keeps the stored password when FlgPwd is 0 and writes the typed one when it is 1")
  void updateThePasswordOnlyWhenTyped() throws Exception {
    ObjectNode kept = panel(DATABASE, "", "0");
    kept.set("IdeDbs", objectMapper.createArrayNode().add(databaseId));
    kept.put("IdeDbs.selected", databaseId);
    assertOk(maintain("DbsUpd", kept));
    assertThat(database(databaseId)).containsEntry("DBSPWD", STORED_PASSWORD);

    ObjectNode changed = panel(DATABASE, "new-secret-793", "1");
    changed.set("IdeDbs", objectMapper.createArrayNode().add(databaseId));
    changed.put("IdeDbs.selected", databaseId);
    assertOk(maintain("DbsUpd", changed));
    // Stored encrypted: neither the previous one nor the typed text
    assertThat(database(databaseId).get("DBSPWD")).isNotNull().isNotEqualTo(STORED_PASSWORD).isNotEqualTo("new-secret-793");
    assertThat(database(otherId)).containsEntry("DBSPWD", STORED_PASSWORD);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsNew creates the connection with every row of the sites grid when no row is selected")
  void createWithItsSites() throws Exception {
    ObjectNode parameters = panel(NEW_DATABASE, "typed-793", "1");
    parameters.set("IdeDbs", objectMapper.createArrayNode());
    parameters.put("Act", 1);
    sites(parameters, rowTypes("INSERT", "INSERT"), ids(null, null), ints(MADRID, ONATE), ints(BASE, TEST), ints(1, 2));

    assertOk(maintain("DbsNew", parameters));

    Map<String, Object> created = select("SELECT * FROM AweDbs WHERE Als = '" + NEW_DATABASE + "'");
    assertThat(created).containsEntry("DES", "Changed").containsEntry("DCT", "D").containsEntry("TYP", "Pre")
      .containsEntry("ACT", "1");
    assertThat(created.get("DBSPWD")).isNotNull().isNotEqualTo("typed-793");
    int createdId = Integer.parseInt(String.valueOf(created.get("IDEDBS")));
    assertThat(createdId).isNotIn(databaseId, otherId);
    assertThat(sites(createdId)).containsExactly(MADRID + "/" + BASE + "/1", ONATE + "/" + TEST + "/2");
    assertThat(sites(databaseId)).containsExactly(MADRID + "/" + BASE + "/1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsDel deletes the selected connections sent as a list with their site rows")
  void deleteWithTheIdentifiersAsAList() throws Exception {
    assertOk(maintain("DbsDel", "{\"IdeDbs\":[" + databaseId + "," + otherId + "],\"max\":30}"));

    assertThat(database(databaseId)).isEmpty();
    assertThat(database(otherId)).isEmpty();
    assertThat(sites(databaseId)).isEmpty();
    assertThat(sites(otherId)).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsDel deletes only the connection of a one element list")
  void deleteOneDatabase() throws Exception {
    assertOk(maintain("DbsDel", "{\"IdeDbs\":[" + databaseId + "],\"IdeDbs.selected\":" + databaseId + ",\"Als\":\"" + DATABASE
      + "\",\"max\":30}"));

    assertThat(database(databaseId)).isEmpty();
    assertThat(sites(databaseId)).isEmpty();
    assertThat(database(otherId)).containsEntry("ALS", OTHER_DATABASE);
    assertThat(sites(otherId)).containsExactly(ONATE + "/" + BASE + "/1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("DbsDel is a multiple maintain: a single value is rejected and nothing is deleted")
  void deleteWithTheIdentifierAsAValueChangesNothing() throws Exception {
    // Every caller is a grid (the legacy Dbs screen too), which sends a list
    String response = maintain("DbsDel", "{\"IdeDbs\":" + otherId + ",\"max\":30}").andReturn().getResponse()
      .getContentAsString();

    assertThat(JsonPath.<List<String>>read(response, "$[?(@.type == 'message')].parameters.type")).as(response)
      .containsOnly("error");
    assertThat(database(otherId)).containsEntry("ALS", OTHER_DATABASE);
    assertThat(sites(otherId)).containsExactly(ONATE + "/" + BASE + "/1");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The sites of a connection are listed with the identifier of the grid as a list")
  void sitesOfTheSelectedConnection() throws Exception {
    String response = mockMvc.perform(post("/action/data/QryLstSitModByDbs")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"IdeDbs\":[" + databaseId + "],\"IdeDbs.selected\":" + databaseId + "}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<List<Object>>read(response, "$[?(@.type == 'fill')].parameters.datalist.rows[*].IdeSitModDbs"))
      .map(String::valueOf).containsExactly(String.valueOf(siteRowId));
  }

  @Test
  @DisplayName("The databases screen is generated with its grid and its side panel")
  void screenIsGenerated() throws Exception {
    // The option is in the private menu: sign in first
    new SecurityAuditTestSupport(dataSource).copyTestUser(SIGNED_IN_USER);
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/action/login")
      .with(csrf())
      .session(session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"" + SIGNED_IN_USER + "\",\"pwd_usr\":\"test\"}")
      .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

    String response = mockMvc.perform(post("/screen-data/databases")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"report\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<String>read(response, "$.screen.name")).isEqualTo("database-admin");
    List<String> components = JsonPath.read(response, "$.components[*].id");
    assertThat(components).contains("GrdDbsAdm", "CrtDbsTxt", "CrtDbsSta", "PnlMod", "Als", "Dct", "Dbc", "Dbt", "Drv", "Typ",
        "DbsUsr", "DbsPwd", "FlgPwd", "Des", "Act", "SitModDbsLst", "ButSitModDbsLstAdd", "ButSitModDbsLstDel", "ButPnlUpd",
        "ButPnlNew", "ButPnlCnl", "CtxButUpd", "CtxButDel", "ButDel", "ButNew")
      .doesNotContain("IdeDbs", "CrtAls", "CrtDct", "CrtTyp", "CrtAct", "ButSch", "ButRst", "ButViw", "ButUpd");
    // The search and the quick filter of the grid toolbar replace the criteria window
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'GrdDbsAdm')].controller.search"))
      .containsExactly("CrtDbsTxt");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtDbsSta')].controller.targetAction"))
      .containsExactly("DbsStaCnt");
    // Only the identifier of the connections is sent to the maintains
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdDbsAdm')].controller.columnModel[?(@.sendable == true)].name")).containsExactly("IdeDbs");
    // Delete is the danger action of the selection bar
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdDbsAdm')].controller.buttonModel[?(@.selection == true)].id")).containsExactly("ButDel");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButDel')].controller.style"))
      .containsExactly("btn-danger");
    // Main column with the avatar and the description below the alias, environment, type, state and the row buttons; the
    // connection URL is a hidden column (the side panel reads it)
    List<String> columns = JsonPath.read(response, "$.components[?(@.id == 'GrdDbsAdm')].controller.columnModel[*].name");
    assertThat(columns).containsSubsequence("Als", "TypTxt", "DbtTxt", "StaTxt", "EdtBtn", "MorBtn");
    assertThat(JsonPath.<List<Boolean>>read(response,
      "$.components[?(@.id == 'GrdDbsAdm')].controller.columnModel[?(@.name == 'Dbc')].hidden")).containsExactly(true);
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdDbsAdm')].controller.columnModel[?(@.name == 'Als')].secondaryField")).containsExactly("Des");
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdDbsAdm')].controller.columnModel[?(@.name == 'StaTxt')].statusField")).containsExactly("StaTon");
    // New connection is the primary button
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'ButNew')].controller.style"))
      .containsExactly("btn-primary");
    // The password is a password criterion; Active is a switch
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'DbsPwd')].controller.component"))
      .containsExactly("password");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Act')].controller.style"))
      .singleElement().asString().contains("switch");
    // The sites and modules grid is editable and sends its operations
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'SitModDbsLst')].controller.editable"))
      .containsExactly(true);
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'SitModDbsLst')].controller.targetAction"))
      .containsExactly("QryLstSitModByDbs");
    // Identity header of the side panel: the selected connection of the grid with its state tag
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlDbs')].controller.headerGrid"))
      .containsExactly("GrdDbsAdm");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlDbs')].controller.headerTitleField"))
      .containsExactly("Als");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlDbs')].controller.headerSubtitleField"))
      .containsExactly("Dbc");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlDbs')].controller.headerTags[*].field"))
      .containsExactly("StaTxt");
    assertThat(JsonPath.<String>read(response, "$.template")).contains("PnlDbs").contains("awe-form-section-title");
  }

  /**
   * Criteria of the side panel, with the typed password and its flag
   */
  private ObjectNode panel(String alias, String password, String passwordFlag) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("Als", alias);
    parameters.put("Des", "Changed");
    parameters.put("Dct", "D");
    parameters.put("Typ", "Pre");
    parameters.put("Dbt", "h2");
    parameters.put("Dbc", "java:comp/env/jdbc/x");
    parameters.put("Drv", "org.h2.Driver");
    parameters.put("DbsUsr", "usrx");
    parameters.put("DbsPwd", password);
    parameters.put("FlgPwd", passwordFlag);
    parameters.put("Act", 0);
    parameters.put("PnlMod", "edit");
    parameters.put("max", 30);
    // No row operations unless the test adds them
    parameters.set("SitModDbsLst-RowTyp", objectMapper.createArrayNode());
    return parameters;
  }

  /**
   * Rows of the sites and modules grid (send-operations)
   */
  private void sites(ObjectNode parameters, ArrayNode rowTypes, ArrayNode ids, ArrayNode sites, ArrayNode modules,
                     ArrayNode orders) {
    parameters.set("SitModDbsLst-RowTyp", rowTypes);
    parameters.set("IdeSitModDbs", ids);
    parameters.set("IdeSit", sites);
    parameters.set("IdeMod", modules);
    parameters.set("Ord", orders);
  }

  private ArrayNode rowTypes(String... types) {
    ArrayNode list = objectMapper.createArrayNode();
    for (String type : types) {
      list.add(type);
    }
    return list;
  }

  private ArrayNode ids(Integer... values) {
    ArrayNode list = objectMapper.createArrayNode();
    for (Integer value : values) {
      if (value == null) {
        list.addNull();
      } else {
        list.add(value);
      }
    }
    return list;
  }

  private ArrayNode ints(int... values) {
    ArrayNode list = objectMapper.createArrayNode();
    for (int value : values) {
      list.add(value);
    }
    return list;
  }

  private ResultActions maintain(String target, JsonNode parameters) throws Exception {
    return maintain(target, objectMapper.writeValueAsString(parameters));
  }

  private ResultActions maintain(String target, String body) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
      .with(csrf())
      .contentType(MediaType.APPLICATION_JSON)
      .content(body)
      .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

  private static void assertOk(ResultActions result) throws Exception {
    String response = result.andReturn().getResponse().getContentAsString();
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }

  private Map<String, Object> database(int id) throws Exception {
    return select("SELECT IdeDbs, Als, Des, Dct, Typ, Dbt, Dbc, Drv, DbsUsr, DbsPwd, Act FROM AweDbs WHERE IdeDbs = " + id);
  }

  /**
   * Site rows of a connection as site/module/order, by order
   */
  private List<String> sites(int databaseId) throws Exception {
    List<String> rows = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT IdeSit, IdeMod, Ord FROM AweSitModDbs WHERE IdeDbs = " + databaseId + " ORDER BY Ord");
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        rows.add(result.getInt(1) + "/" + result.getInt(2) + "/" + result.getInt(3));
      }
    }
    return rows;
  }

  /**
   * First row of a select, with the column names in upper case and the values as trimmed text
   */
  private Map<String, Object> select(String sql) throws Exception {
    Map<String, Object> row = new HashMap<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {
      if (result.next()) {
        ResultSetMetaData metaData = result.getMetaData();
        for (int column = 1; column <= metaData.getColumnCount(); column++) {
          Object value = result.getObject(column);
          row.put(metaData.getColumnName(column).toUpperCase(), value == null ? null : value.toString().trim());
        }
      }
    }
    return row;
  }

  private int nextId(String column, String table) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(" + column + ") FROM " + table);
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
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
