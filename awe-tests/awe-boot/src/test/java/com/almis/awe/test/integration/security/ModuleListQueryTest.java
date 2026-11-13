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
 * The module list of the modules screen (ModLstAdm, module-admin) returns, for each module, the initial screen (the
 * secondary line), the name and the tone of its avatar, how many profiles and users can enter it, in how many sites it
 * has a database and its state (active or inactive) with its label and its tone. The search of the grid (CrtModTxt)
 * finds modules by name, the quick filter (CrtModSta) by state, and the options query (ModStaCnt) counts the modules of
 * each option with the same search. ModLst, the list of the deprecated Mod screen, is not changed.
 */
@Tag("integration")
@DisplayName("Module list query tests")
class ModuleListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String PREFIX = "modlst";
  // Active, with two profiles, one user and two rows in one site
  private static final String FULL = PREFIX + " full";
  // Active, without access nor sites
  private static final String EMPTY = PREFIX + " empty";
  // Inactive, with one user and two sites
  private static final String INACTIVE = PREFIX + " inactive";
  private static final String SEARCH = "\"CrtModTxt\":\"" + PREFIX + "\"";
  private static final int MADRID = 10;
  private static final int ONATE = 17;

  @Autowired
  private DataSource dataSource;

  private int fullId;

  @BeforeEach
  void createModules() throws Exception {
    deleteModules();
    fullId = nextId("IdeMod", "AweMod");
    int proRow = nextId("IdeModPro", "AweModPro");
    int opeRow = nextId("IdeModOpe", "AweModOpe");
    int sitRow = nextId("IdeSitModDbs", "AweSitModDbs");
    execute("INSERT INTO AweMod (IdeMod, Nam, ScrIni, Act, Ord) VALUES (" + fullId + ", '" + FULL + "', 'Sit', 1, 1)");
    execute("INSERT INTO AweMod (IdeMod, Nam, ScrIni, Act, Ord) VALUES (" + (fullId + 1) + ", '" + EMPTY + "', NULL, 1, 2)");
    execute("INSERT INTO AweMod (IdeMod, Nam, ScrIni, Act, Ord) VALUES (" + (fullId + 2) + ", '" + INACTIVE + "', 'Dbs', 0, 3)");
    execute("INSERT INTO AweModPro (IdeModPro, IdeMod, IdePro) VALUES (" + proRow + ", " + fullId + ", 1)");
    execute("INSERT INTO AweModPro (IdeModPro, IdeMod, IdePro) VALUES (" + (proRow + 1) + ", " + fullId + ", 2)");
    execute("INSERT INTO AweModOpe (IdeModOpe, IdeMod, IdeOpe) VALUES (" + opeRow + ", " + fullId + ", 1)");
    execute("INSERT INTO AweModOpe (IdeModOpe, IdeMod, IdeOpe) VALUES (" + (opeRow + 1) + ", " + (fullId + 2) + ", 2)");
    // Two databases of the full module in the same site count as one site
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + sitRow + ", " + MADRID + ", "
      + fullId + ", 8, 1)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + (sitRow + 1) + ", " + MADRID + ", "
      + fullId + ", 6, 2)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + (sitRow + 2) + ", " + MADRID + ", "
      + (fullId + 2) + ", 8, 1)");
    execute("INSERT INTO AweSitModDbs (IdeSitModDbs, IdeSit, IdeMod, IdeDbs, Ord) VALUES (" + (sitRow + 3) + ", " + ONATE + ", "
      + (fullId + 2) + ", 8, 2)");
  }

  @AfterEach
  void deleteModules() throws Exception {
    String modules = "(SELECT IdeMod FROM AweMod WHERE Nam LIKE '" + PREFIX + "%')";
    for (String table : List.of("AweModOpe", "AweModPro", "AweSitModDbs")) {
      execute("DELETE FROM " + table + " WHERE IdeMod IN " + modules);
    }
    execute("DELETE FROM AweMod WHERE Nam LIKE '" + PREFIX + "%'");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void moduleListReturnsCountsAvatarAndState() throws Exception {
    List<Map<String, Object>> rows = rows("ModLstAdm", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(names(rows)).containsExactly(FULL, EMPTY, INACTIVE);
    assertThat(row(rows, FULL))
      .containsEntry("ScrIni", "Sit")
      .containsEntry("Ini", FULL)
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");
    assertThat(counts(row(rows, FULL))).containsExactly(2, 1, 1);
    assertThat(counts(row(rows, EMPTY))).containsExactly(0, 0, 0);
    assertThat(counts(row(rows, INACTIVE))).containsExactly(0, 1, 2);
    assertThat(row(rows, INACTIVE))
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");

    // The tone of the avatar depends only on the identifier, so a module always gets the same colour
    List<String> tones = List.of("primary", "info", "success", "warning", "danger", "neutral");
    rows.forEach(row -> assertThat(row).containsEntry("IniTon",
      tones.get(Integer.parseInt(String.valueOf(row.get("IdeMod"))) % tones.size())));
    // The side panel and the selection read these fields of the row; there is no theme
    assertThat(row(rows, FULL)).containsKeys("id", "IdeMod", "IdeModSel", "Nam", "ScrIni", "Ord", "Act")
      .doesNotContainKeys("IdeThm", "ThmTxt")
      .containsEntry("IdeModSel", row(rows, FULL).get("IdeMod"));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theStateIsTranslated() throws Exception {
    assertThat(row(rows("ModLstAdm", "{" + SEARCH + ",\"lang\":\"es-ES\"}"), INACTIVE)).containsEntry("StaTxt", "Inactivo");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void moduleListSearchesByNameIgnoringCase() throws Exception {
    assertThat(names(rows("ModLstAdm", "{\"CrtModTxt\":\"" + PREFIX.toUpperCase() + " FULL\"}"))).containsExactly(FULL);
    assertThat(names(rows("ModLstAdm", "{\"CrtModTxt\":\"" + PREFIX + " in\"}"))).containsExactly(INACTIVE);
    assertThat(rows("ModLstAdm", "{\"CrtModTxt\":\"nobody-matches-qzx-modlst\"}")).isEmpty();
    // An empty search shows every module, the ones of the test data too
    assertThat(names(rows("ModLstAdm", "{\"CrtModTxt\":\"\"}"))).contains("Base", "Test", FULL, EMPTY, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void moduleListFiltersByState() throws Exception {
    assertThat(names(rows("ModLstAdm", "{" + SEARCH + ",\"CrtModSta\":\"active\"}"))).containsExactly(FULL, EMPTY);
    assertThat(names(rows("ModLstAdm", "{" + SEARCH + ",\"CrtModSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("ModLstAdm", "{" + SEARCH + ",\"CrtModSta\":\"\"}"))).containsExactly(FULL, EMPTY, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void moduleListIgnoresTheModuleOfThePanel() throws Exception {
    // The hidden IdeMod criterion of the side panel is sent with every reload of the grid
    assertThat(names(rows("ModLstAdm", "{" + SEARCH + ",\"IdeMod\":" + fullId + "}"))).containsExactly(FULL, EMPTY, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("ModStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> text(row, "value"), row -> row.get("label"), ModuleListQueryTest::count,
        row -> text(row, "tone"))
      .containsExactly(
        tuple("", "SCREEN_TEXT_MODULE_FILTER_ALL", 3, ""),
        tuple("active", "SCREEN_TEXT_MODULE_FILTER_ACTIVE", 2, ""),
        tuple("inactive", "SCREEN_TEXT_MODULE_FILTER_INACTIVE", 1, ""));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfRowsOfItsOption() throws Exception {
    assertThat(rows("ModStaCnt", "{\"CrtModTxt\":\"" + PREFIX + " in\"}"))
      .extracting(row -> text(row, "value"), ModuleListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 0), tuple("inactive", 1));
    // The chosen option is sent with the counts too: it must not change them
    for (Map<String, Object> option : rows("ModStaCnt", "{" + SEARCH + ",\"CrtModSta\":\"inactive\"}")) {
      String value = text(option, "value");
      assertThat(rows("ModLstAdm", "{" + SEARCH + ",\"CrtModSta\":\"" + value + "\"}"))
        .as("rows of option '%s'", value).hasSize(count(option));
    }
    // Without search, All is every module
    assertThat(count(rows("ModStaCnt", "{}").getFirst())).isEqualTo(rows("ModLstAdm", "{}").size());
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void deprecatedModuleListKeepsItsTheme() throws Exception {
    Map<String, Object> base = rows("ModLst", "{}").stream()
      .filter(row -> "Base".equals(String.valueOf(row.get("Nam")).trim())).findFirst().orElseThrow();
    assertThat(base).containsKeys("IdeThm", "ThmTxt", "ActIco").containsEntry("ThmTxt", "sky");
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

  /**
   * Profiles, users and sites of a module
   */
  private static List<Integer> counts(Map<String, Object> row) {
    return List.of(number(row.get("ProCnt")), number(row.get("UsrCnt")), number(row.get("SitCnt")));
  }

  private static int number(Object value) {
    return Integer.parseInt(String.valueOf(value));
  }

  /**
   * Text of a field, empty when the field is null: Oracle stores an empty string as NULL, so the empty value of the All
   * option comes as null there (the clients treat both the same way)
   */
  private static String text(Map<String, Object> row, String field) {
    Object value = row.get(field);
    return value == null ? "" : String.valueOf(value);
  }

  private static int count(Map<String, Object> row) {
    return number(row.get("count"));
  }

  private static Map<String, Object> row(List<Map<String, Object>> rows, String name) {
    return rows.stream()
      .filter(row -> name.equals(String.valueOf(row.get("Nam")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + name));
  }

  /**
   * Names of the rows of the test, in the order of the list
   */
  private static List<String> names(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("Nam")).trim()).toList();
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
