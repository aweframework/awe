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
 * The modules screen (module-admin) sends the module of its side panel as a single value (the hidden IdeMod criterion,
 * filled from the selected row) to the maintains it reuses (ModNew, ModUpd), with the criteria of the General tab and
 * the row operations of its three grids (MdlPrfLst, MdlUsrLst, MdlSitDbsLst). It has no theme: saving a module from it
 * keeps the theme of the module and of its user rows, and a user row added without a theme is still listed. The
 * deprecated ModNew and ModUpd screens still send the theme (Thm and the IdeThm column), and it is still written.
 * ModDel takes the selected modules from the IdeModSel column of the grid, or IdeMod as before (deprecated Mod screen).
 */
@Tag("integration")
@DisplayName("Module admin screen tests")
class ModuleAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "modadm";
  private static final String MODULE = PREFIX + " one";
  private static final String OTHER_MODULE = PREFIX + " two";
  private static final String NEW_MODULE = PREFIX + " new";
  // Test data: themes, users, profiles, sites and databases
  private static final int SUNSET = 1;
  private static final int GRASS = 4;
  private static final int TEST_USER = 1;
  private static final int DONALD = 2;
  private static final int ADMINISTRATOR = 1;
  private static final int OPERATOR = 2;
  private static final int MADRID = 10;
  private static final int ONATE = 17;
  private static final int DATABASE = 8;

  @Autowired
  private DataSource dataSource;

  private int moduleId;
  private int otherId;
  private int userRowId;

  @BeforeEach
  void createModules() throws Exception {
    deleteModules();
    moduleId = nextId("IdeMod", "AweMod");
    otherId = moduleId + 1;
    userRowId = nextId("IdeModOpe", "AweModOpe");
    execute("INSERT INTO AweMod (IdeMod, Nam, ScrIni, IdeThm, Act, Ord) VALUES (" + moduleId + ", '" + MODULE
      + "', 'Sit', " + GRASS + ", 1, 7)");
    execute("INSERT INTO AweMod (IdeMod, Nam, ScrIni, IdeThm, Act, Ord) VALUES (" + otherId + ", '" + OTHER_MODULE
      + "', 'Dbs', " + SUNSET + ", 1, 8)");
    execute("INSERT INTO AweModOpe (IdeModOpe, IdeMod, IdeOpe, IdeThm) VALUES (" + userRowId + ", " + moduleId + ", "
      + TEST_USER + ", " + SUNSET + ")");
    execute("INSERT INTO AweModPro (IdeModPro, IdeMod, IdePro) VALUES (" + nextId("IdeModPro", "AweModPro") + ", "
      + moduleId + ", " + ADMINISTRATOR + ")");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES ("
      + nextId("IdeSitModDbs", "AweSitModDbs") + ", " + MADRID + ", " + moduleId + ", " + DATABASE + ", 1)");
  }

  @AfterEach
  void deleteModules() throws Exception {
    String modules = "(SELECT IdeMod FROM AweMod WHERE Nam LIKE '" + PREFIX + "%')";
    for (String table : List.of("AweModOpe", "AweModPro", "AweSitModDbs", "HISAweModOpe", "HISAweModPro", "HISAweSitModDbs")) {
      execute("DELETE FROM " + table + " WHERE IdeMod IN " + modules);
    }
    execute("DELETE FROM HISAweMod WHERE Nam LIKE '" + PREFIX + "%'");
    execute("DELETE FROM AweMod WHERE Nam LIKE '" + PREFIX + "%'");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModUpd without a theme keeps the theme of the module and of its user rows")
  void updateWithoutThemeKeepsTheThemes() throws Exception {
    ObjectNode parameters = panel(moduleId, MODULE + " changed", "Dbs", 3, 0);
    // The user row is edited (UPDATE) without an IdeThm column: the screen has no theme
    users(parameters, list("UPDATE"), list(userRowId), list(DONALD));

    assertOk(maintain("ModUpd", parameters));

    assertThat(module(moduleId)).containsEntry("NAM", MODULE + " changed").containsEntry("SCRINI", "Dbs")
      .containsEntry("ORD", "3").containsEntry("ACT", "0").containsEntry("IDETHM", String.valueOf(GRASS));
    assertThat(select("SELECT IdeOpe, IdeThm FROM AweModOpe WHERE IdeModOpe = " + userRowId))
      .containsEntry("IDEOPE", String.valueOf(DONALD)).containsEntry("IDETHM", String.valueOf(SUNSET));
    // The other module is not touched
    assertThat(module(otherId)).containsEntry("NAM", OTHER_MODULE).containsEntry("IDETHM", String.valueOf(SUNSET));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModUpd with only the General tab changed keeps the profiles, users and sites of the module")
  void updateOnlyGeneralKeepsTheRows() throws Exception {
    assertOk(maintain("ModUpd", panel(moduleId, MODULE, "Sit", 7, 1)));

    assertThat(count("AweModOpe", moduleId)).isEqualTo(1);
    assertThat(count("AweModPro", moduleId)).isEqualTo(1);
    assertThat(count("AweSitModDbs", moduleId)).isEqualTo(1);
    assertThat(module(moduleId)).containsEntry("IDETHM", String.valueOf(GRASS));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("A user row added through ModUpd without a theme is listed by QryLstUsrByMod")
  void userRowWithoutThemeIsListed() throws Exception {
    ObjectNode parameters = panel(moduleId, MODULE, "Sit", 7, 1);
    users(parameters, list("INSERT"), list((Integer) null), list(DONALD));

    assertOk(maintain("ModUpd", parameters));

    assertThat(select("SELECT IdeThm FROM AweModOpe WHERE IdeMod = " + moduleId + " AND IdeOpe = " + DONALD))
      .containsEntry("IDETHM", null);
    List<Map<String, Object>> rows = rows("QryLstUsrByMod", "{\"IdeMod\":" + moduleId + "}");
    assertThat(rows).extracting(row -> String.valueOf(row.get("IdeOpe")))
      .containsExactlyInAnyOrder(String.valueOf(TEST_USER), String.valueOf(DONALD));
    assertThat(rows).extracting(row -> String.valueOf(row.get("l1_nom")).trim()).contains("donald", "test");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModUpd adds, changes and removes profile and site rows of the module")
  void updateTheAccessAndSitesRows() throws Exception {
    int profileRow = Integer.parseInt(select("SELECT IdeModPro FROM AweModPro WHERE IdeMod = " + moduleId).get("IDEMODPRO").toString());
    int siteRow = Integer.parseInt(select("SELECT IdeSitModDbs FROM AweSitModDbs WHERE IdeMod = " + moduleId)
      .get("IDESITMODDBS").toString());
    ObjectNode parameters = panel(moduleId, MODULE, "Sit", 7, 1);
    parameters.set("MdlPrfLst-RowTyp", list("DELETE", "INSERT"));
    parameters.set("IdeModPro", list(profileRow, null));
    parameters.set("IdePro", list(ADMINISTRATOR, OPERATOR));
    parameters.set("MdlSitDbsLst-RowTyp", list("UPDATE", "INSERT"));
    parameters.set("IdeSitModDbs", list(siteRow, null));
    parameters.set("IdeSit", list(MADRID, ONATE));
    parameters.set("IdeDbs", list(DATABASE, DATABASE));
    parameters.set("SitModOrd", list(5, 6));

    assertOk(maintain("ModUpd", parameters));

    assertThat(column("SELECT IdePro FROM AweModPro WHERE IdeMod = " + moduleId)).containsExactly(String.valueOf(OPERATOR));
    assertThat(column("SELECT IdeSit FROM AweSitModDbs WHERE IdeMod = " + moduleId + " ORDER BY Ord"))
      .containsExactly(String.valueOf(MADRID), String.valueOf(ONATE));
    assertThat(column("SELECT Ord FROM AweSitModDbs WHERE IdeMod = " + moduleId + " ORDER BY Ord")).containsExactly("5", "6");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The deprecated ModUpd screen still writes the theme of the module and of its user rows")
  void deprecatedUpdateStillWritesTheTheme() throws Exception {
    ObjectNode parameters = panel(moduleId, MODULE, "Sit", 7, 1);
    parameters.put("Thm", SUNSET);
    users(parameters, list("UPDATE", "INSERT"), list(userRowId, null), list(TEST_USER, DONALD));
    parameters.set("IdeThm", list(GRASS, SUNSET));

    assertOk(maintain("ModUpd", parameters));

    assertThat(module(moduleId)).containsEntry("IDETHM", String.valueOf(SUNSET));
    assertThat(select("SELECT IdeThm FROM AweModOpe WHERE IdeModOpe = " + userRowId))
      .containsEntry("IDETHM", String.valueOf(GRASS));
    assertThat(select("SELECT IdeThm FROM AweModOpe WHERE IdeMod = " + moduleId + " AND IdeOpe = " + DONALD))
      .containsEntry("IDETHM", String.valueOf(SUNSET));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModNew without a theme creates the module and its rows, with the module of the sequence")
  void createWithoutTheme() throws Exception {
    ObjectNode parameters = panel(null, NEW_MODULE, "Usr", 9, 1);
    parameters.put("PnlMod", "new");
    users(parameters, list("INSERT"), list((Integer) null), list(DONALD));
    parameters.set("MdlPrfLst-RowTyp", list("INSERT"));
    parameters.set("IdeModPro", list((Integer) null));
    parameters.set("IdePro", list(OPERATOR));
    parameters.set("MdlSitDbsLst-RowTyp", list("INSERT"));
    parameters.set("IdeSitModDbs", list((Integer) null));
    parameters.set("IdeSit", list(ONATE));
    parameters.set("IdeDbs", list(DATABASE));
    parameters.set("SitModOrd", list(2));

    assertOk(maintain("ModNew", parameters));

    Map<String, Object> created = select("SELECT IdeMod, ScrIni, IdeThm, Act, Ord FROM AweMod WHERE Nam = '" + NEW_MODULE + "'");
    assertThat(created).containsEntry("SCRINI", "Usr").containsEntry("IDETHM", null).containsEntry("ACT", "1")
      .containsEntry("ORD", "9");
    int createdId = Integer.parseInt(created.get("IDEMOD").toString());
    assertThat(createdId).isNotIn(moduleId, otherId);
    assertThat(column("SELECT IdeOpe FROM AweModOpe WHERE IdeMod = " + createdId)).containsExactly(String.valueOf(DONALD));
    assertThat(column("SELECT IdePro FROM AweModPro WHERE IdeMod = " + createdId)).containsExactly(String.valueOf(OPERATOR));
    assertThat(column("SELECT IdeSit FROM AweSitModDbs WHERE IdeMod = " + createdId)).containsExactly(String.valueOf(ONATE));
    assertThat(rows("QryLstUsrByMod", "{\"IdeMod\":" + createdId + "}")).hasSize(1);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The deprecated ModNew screen still writes the theme of the module and of its user rows")
  void deprecatedCreateStillWritesTheTheme() throws Exception {
    ObjectNode parameters = panel(null, NEW_MODULE, "Usr", 9, 1);
    parameters.put("Thm", GRASS);
    users(parameters, list("INSERT"), list((Integer) null), list(DONALD));
    parameters.set("IdeThm", list(SUNSET));

    assertOk(maintain("ModNew", parameters));

    Map<String, Object> created = select("SELECT IdeMod, IdeThm FROM AweMod WHERE Nam = '" + NEW_MODULE + "'");
    assertThat(created).containsEntry("IDETHM", String.valueOf(GRASS));
    assertThat(select("SELECT IdeThm FROM AweModOpe WHERE IdeMod = " + created.get("IDEMOD")))
      .containsEntry("IDETHM", String.valueOf(SUNSET));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModDel deletes the modules selected in the grid (IdeModSel) with their rows")
  void deleteTheSelectedModules() throws Exception {
    // The hidden IdeMod criterion holds one module; the grid sends every selected one
    assertOk(maintain("ModDel", "{\"IdeMod\":" + moduleId + ",\"IdeModSel\":[" + moduleId + "," + otherId + "],\"Nam\":\""
      + MODULE + "\",\"Act\":1,\"max\":30}"));

    assertThat(module(moduleId)).isEmpty();
    assertThat(module(otherId)).isEmpty();
    assertThat(count("AweModOpe", moduleId)).isZero();
    assertThat(count("AweModPro", moduleId)).isZero();
    assertThat(count("AweSitModDbs", moduleId)).isZero();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModDel still deletes the modules sent as IdeMod (deprecated Mod screen)")
  void deprecatedDeleteStillWorks() throws Exception {
    assertOk(maintain("ModDel", "{\"IdeMod\":[" + otherId + "],\"Nam\":[\"" + OTHER_MODULE + "\"],\"max\":30}"));

    assertThat(module(otherId)).isEmpty();
    assertThat(module(moduleId)).isNotEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("ModNamUni finds a module name already taken")
  void nameUniqueness() throws Exception {
    assertThat(rows("ModNamUni", "{\"Nam\":\"" + MODULE + "\"}")).hasSize(1);
    assertThat(rows("ModNamUni", "{\"Nam\":\"" + NEW_MODULE + "\"}")).isEmpty();
  }

  /**
   * Criteria of the General tab of the side panel, with no row operations in the grids
   */
  private ObjectNode panel(Integer module, String name, String screen, int order, int active) {
    ObjectNode parameters = objectMapper.createObjectNode();
    if (module == null) {
      parameters.put("IdeMod", "");
    } else {
      parameters.put("IdeMod", module);
    }
    parameters.put("Nam", name);
    parameters.put("Scr", screen);
    parameters.put("Ord", order);
    parameters.put("Act", active);
    parameters.put("PnlMod", "edit");
    parameters.put("max", 30);
    for (String grid : List.of("MdlUsrLst", "MdlPrfLst", "MdlSitDbsLst")) {
      parameters.set(grid + "-RowTyp", objectMapper.createArrayNode());
    }
    return parameters;
  }

  /**
   * Rows of the users grid (send-operations): no IdeThm column unless the test adds it
   */
  private void users(ObjectNode parameters, ArrayNode rowTypes, ArrayNode ids, ArrayNode users) {
    parameters.set("MdlUsrLst-RowTyp", rowTypes);
    parameters.set("IdeModOpe", ids);
    parameters.set("IdeOpe", users);
  }

  private ArrayNode list(String... values) {
    ArrayNode list = objectMapper.createArrayNode();
    for (String value : values) {
      list.add(value);
    }
    return list;
  }

  private ArrayNode list(Integer... values) {
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

  private ResultActions maintain(String target, ObjectNode parameters) throws Exception {
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

  private Map<String, Object> module(int id) throws Exception {
    return select("SELECT IdeMod, Nam, ScrIni, IdeThm, Act, Ord FROM AweMod WHERE IdeMod = " + id);
  }

  private int count(String table, int module) throws Exception {
    return Integer.parseInt(select("SELECT COUNT(*) AS Cnt FROM " + table + " WHERE IdeMod = " + module).get("CNT").toString());
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
          row.put(metaData.getColumnLabel(column).toUpperCase(), value == null ? null : value.toString().trim());
        }
      }
    }
    return row;
  }

  /**
   * First column of every row of a select, as trimmed text
   */
  private List<String> column(String sql) throws Exception {
    List<String> values = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        Object value = result.getObject(1);
        values.add(value == null ? null : value.toString().trim());
      }
    }
    return values;
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
