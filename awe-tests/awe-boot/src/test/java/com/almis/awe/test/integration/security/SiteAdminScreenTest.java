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
 * The sites screen (site-admin) saves a site and the module rows of its panel in one action (SitNew, SitUpd), sends the
 * identifier of the sites from its grid, as a list, to SitUpd and SitDel, loads the module rows of the selected site with
 * the site of each row and its order (QryLstModDbsBySitAdm) and refuses a new site whose name is taken (SitNamUni).
 */
@Tag("integration")
@DisplayName("Site admin screen tests")
class SiteAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "sitadm793";
  private static final String SITE = PREFIX + " one";
  private static final String OTHER_SITE = PREFIX + " two";
  private static final String NEW_SITE = PREFIX + " new";
  private static final String SIGNED_IN_USER = "sitadm793usr";
  // Modules and databases of the test data
  private static final int BASE = 28;
  private static final int TEST = 916;
  private static final int DATABASE_ONE = 6;
  private static final int DATABASE_TWO = 7;
  private static final int DATABASE_THREE = 8;

  @Autowired
  private DataSource dataSource;

  private int siteId;
  private int otherId;
  private int moduleRowId;

  @BeforeEach
  void createSites() throws Exception {
    deleteSites();
    siteId = nextId("IdeSit", "AweSit");
    otherId = siteId + 1;
    moduleRowId = nextId("IdeSitModDbs", "AweSitModDbs");
    execute("INSERT INTO AweSit (IdeSit, Nam, Ord, Act) VALUES (" + siteId + ", '" + SITE + "', 3, 0)");
    execute("INSERT INTO AweSit (IdeSit, Nam, Ord, Act) VALUES (" + otherId + ", '" + OTHER_SITE + "', 4, 1)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + moduleRowId + ", " + siteId
      + ", " + BASE + ", " + DATABASE_ONE + ", 1)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + (moduleRowId + 1) + ", " + otherId
      + ", " + BASE + ", " + DATABASE_TWO + ", 1)");
  }

  @AfterEach
  void deleteSites() throws Exception {
    execute("DELETE FROM AweSitModDbs WHERE IdeSit IN (SELECT IdeSit FROM AweSit WHERE Nam LIKE '" + PREFIX + "%')");
    execute("DELETE FROM AweSit WHERE Nam LIKE '" + PREFIX + "%'");
    execute("DELETE FROM HISAweSit WHERE Nam LIKE '" + PREFIX + "%'");
    new SecurityAuditTestSupport(dataSource).deleteUser(SIGNED_IN_USER);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("SitNew creates the site with the order of the panel and the module rows of its grid in one save")
  void createWithItsModules() throws Exception {
    ObjectNode parameters = panel(NEW_SITE, 7, 1);
    // No selected row: the grid sends no site
    parameters.set("IdeSit", objectMapper.createArrayNode());
    modules(parameters, list("INSERT", "INSERT"), ids(null, null), ids(null, null), ints(BASE, TEST),
      ints(DATABASE_THREE, DATABASE_ONE), ints(1, 2));

    assertOk(maintain("SitNew", parameters));

    Map<String, Object> created = select("SELECT IdeSit, Nam, Ord, Act FROM AweSit WHERE Nam = '" + NEW_SITE + "'");
    assertThat(created).containsEntry("ORD", "7").containsEntry("ACT", "1");
    int createdId = Integer.parseInt(String.valueOf(created.get("IDESIT")));
    assertThat(createdId).isNotIn(siteId, otherId);
    assertThat(modules(createdId)).containsExactlyInAnyOrder(BASE + "/" + DATABASE_THREE, TEST + "/" + DATABASE_ONE);
    assertThat(modules(siteId)).containsExactly(BASE + "/" + DATABASE_ONE);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("SitUpd updates only the site of the one element list and the database of its module row")
  void updateTheSiteAndItsModules() throws Exception {
    ObjectNode parameters = panel(SITE + " changed", 5, 1);
    parameters.set("IdeSit", objectMapper.createArrayNode().add(siteId));
    parameters.put("IdeSit.selected", siteId);
    modules(parameters, list("UPDATE"), ids(moduleRowId), ids(siteId), ints(BASE), ints(DATABASE_THREE), ints(1));

    assertOk(maintain("SitUpd", parameters));

    assertThat(site(siteId)).containsEntry("NAM", SITE + " changed").containsEntry("ORD", "5").containsEntry("ACT", "1");
    assertThat(modules(siteId)).containsExactly(BASE + "/" + DATABASE_THREE);
    assertThat(site(otherId)).containsEntry("NAM", OTHER_SITE).containsEntry("ORD", "4").containsEntry("ACT", "1");
    assertThat(modules(otherId)).containsExactly(BASE + "/" + DATABASE_TWO);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("SitDel deletes the selected sites sent as a list with their module rows")
  void deleteWithTheIdentifiersAsAList() throws Exception {
    assertOk(maintain("SitDel", "{\"IdeSit\":[" + siteId + "," + otherId + "],\"max\":30}"));

    assertThat(site(siteId)).isEmpty();
    assertThat(site(otherId)).isEmpty();
    assertThat(modules(siteId)).isEmpty();
    assertThat(modules(otherId)).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The module rows of the selected site come with the site of each row and their order")
  void modulesOfTheSelectedSite() throws Exception {
    String response = data("QryLstModDbsBySitAdm", "{\"IdeSit\":[" + siteId + "],\"IdeSit.selected\":" + siteId + "}");

    List<Map<String, Object>> rows = JsonPath.read(response, "$[?(@.type == 'fill')].parameters.datalist.rows[*]");
    assertThat(rows).singleElement().satisfies(row -> assertThat(row)
      .containsEntry("IdeSitModDbs", moduleRowId)
      .containsEntry("IdeSitRow", siteId)
      .containsEntry("IdeMod", BASE)
      .containsEntry("IdeDbs", DATABASE_ONE)
      .containsEntry("SitModOrd", 1));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The name check of the creation finds a site name in use and lets a free one pass")
  void nameCheckFindsTheNameInUse() throws Exception {
    assertThat(JsonPath.<List<Object>>read(data("SitNamUni", "{\"Nam\":\"" + OTHER_SITE + "\"}"),
      "$[?(@.type == 'fill')].parameters.datalist.rows[*].Nam")).map(String::valueOf).map(String::trim)
      .containsExactly(OTHER_SITE);
    assertThat(JsonPath.<List<Object>>read(data("SitNamUni", "{\"Nam\":\"" + NEW_SITE + "\"}"),
      "$[?(@.type == 'fill')].parameters.datalist.rows[*]")).isEmpty();
  }

  @Test
  @DisplayName("The sites screen is generated with its grid and its side panel")
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

    String response = mockMvc.perform(post("/screen-data/sites")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"view\":\"report\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(JsonPath.<String>read(response, "$.screen.name")).isEqualTo("site-admin");
    List<String> components = JsonPath.read(response, "$.components[*].id");
    assertThat(components).contains("GrdSitAdm", "CrtSitTxt", "CrtSitSta", "PnlMod", "Nam", "OrdCrt", "Act", "SitModDbsLst",
        "ButSitModAdd", "ButSitModDel", "ButPnlUpd", "ButPnlNew", "ButPnlCnl", "CtxButUpd", "CtxButDup", "CtxButDel", "ButDel",
        "ButNew");
    // The search and the quick filter of the grid toolbar
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'GrdSitAdm')].controller.search"))
      .containsExactly("CrtSitTxt");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'CrtSitSta')].controller.targetAction"))
      .containsExactly("SitStaCnt");
    // Only the identifier of the sites is sent to the maintains
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdSitAdm')].controller.columnModel[?(@.sendable == true)].name")).containsExactly("IdeSit");
    // Delete is the danger action of the selection bar
    assertThat(JsonPath.<List<String>>read(response,
      "$.components[?(@.id == 'GrdSitAdm')].controller.buttonModel[?(@.selection == true)].id")).containsExactly("ButDel");
    // A new site starts active; Active is a switch
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'Act')].model.selected[*]"))
      .map(String::valueOf).containsExactly("1");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'Act')].controller.style"))
      .singleElement().asString().contains("switch");
    // The modules grid of the panel is editable, sends its operations and loads the rows of the selected site
    assertThat(JsonPath.<List<Object>>read(response, "$.components[?(@.id == 'SitModDbsLst')].controller.editable"))
      .containsExactly(true);
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'SitModDbsLst')].controller.targetAction"))
      .containsExactly("QryLstModDbsBySitAdm");
    // Identity header of the side panel: the selected site of the grid with its state tag
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlSit')].controller.headerGrid"))
      .containsExactly("GrdSitAdm");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlSit')].controller.headerTitleField"))
      .containsExactly("Nam");
    assertThat(JsonPath.<List<String>>read(response, "$.components[?(@.id == 'PnlSit')].controller.headerTags[*].field"))
      .containsExactly("StaTxt");
  }

  /**
   * Criteria of the side panel, without module rows unless the test adds them
   */
  private ObjectNode panel(String name, int order, int active) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("Nam", name);
    parameters.put("OrdCrt", order);
    parameters.put("Act", active);
    parameters.put("PnlMod", "edit");
    parameters.put("max", 30);
    parameters.set("SitModDbsLst-RowTyp", objectMapper.createArrayNode());
    return parameters;
  }

  /**
   * Rows of the modules grid of the panel (send-operations)
   */
  private void modules(ObjectNode parameters, ArrayNode rowTypes, ArrayNode ids, ArrayNode sites, ArrayNode modules,
                       ArrayNode databases, ArrayNode orders) {
    parameters.set("SitModDbsLst-RowTyp", rowTypes);
    parameters.set("IdeSitModDbs", ids);
    parameters.set("IdeSitRow", sites);
    parameters.set("IdeMod", modules);
    parameters.set("IdeDbs", databases);
    parameters.set("SitModOrd", orders);
  }

  private ArrayNode list(String... values) {
    ArrayNode list = objectMapper.createArrayNode();
    for (String value : values) {
      list.add(value);
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

  private String data(String query, String body) throws Exception {
    return mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
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

  private Map<String, Object> site(int id) throws Exception {
    return select("SELECT IdeSit, Nam, Ord, Act FROM AweSit WHERE IdeSit = " + id);
  }

  /**
   * Module rows of a site as module/database, by order
   */
  private List<String> modules(int siteId) throws Exception {
    List<String> rows = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT IdeMod, IdeDbs FROM AweSitModDbs WHERE IdeSit = " + siteId + " ORDER BY Ord");
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        rows.add(result.getInt(1) + "/" + result.getInt(2));
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
