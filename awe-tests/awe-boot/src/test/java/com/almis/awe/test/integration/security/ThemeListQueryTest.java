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
 * The theme list (ThmLst) returns, for the themes screen (theme-admin), a secondary line with the primary colour and the
 * surface (the translated palette or the hex of a custom colour), the translated mode, the name and the tone of the
 * avatar of the theme and its state (active or inactive) with its label and its tone, and keeps every previous field. The
 * search of the grid (CrtThmTxt) finds themes by name, primary colour or surface, the quick filter (CrtThmSta) by state,
 * and the options query (ThmStaCnt) counts the themes of each option with the same search.
 */
@Tag("integration")
@DisplayName("Theme list query tests")
class ThemeListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String PREFIX = "thmlst793";
  // Active, with palettes
  private static final String PALETTE = PREFIX + " palette";
  // Active, with custom colours
  private static final String CUSTOM = PREFIX + " custom";
  // Inactive
  private static final String INACTIVE = PREFIX + " inactive";
  private static final String SEARCH = "\"CrtThmTxt\":\"" + PREFIX + "\"";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createThemes() throws Exception {
    deleteThemes();
    int id = nextThemeId();
    insertTheme(id, PALETTE, "rose", "slate", "dark", 1);
    insertTheme(id + 1, CUSTOM, "#1A73E8", "#3F4A5C", "light", 1);
    insertTheme(id + 2, INACTIVE, "teal", "auto", "system", 0);
  }

  @AfterEach
  void deleteThemes() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweThm WHERE Nam LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void themeListReturnsColoursModeAvatarAndState() throws Exception {
    List<Map<String, Object>> rows = rows("ThmLst", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(names(rows)).containsExactlyInAnyOrder(PALETTE, CUSTOM, INACTIVE);
    // The secondary line is the primary colour and the surface: the translated palette or the hex of a custom colour
    assertThat(row(rows, PALETTE))
      .containsEntry("ThmSec", "Rose · Slate")
      .containsEntry("MdeTxt", "Dark")
      .containsEntry("Ini", PALETTE)
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");
    assertThat(row(rows, CUSTOM))
      .containsEntry("ThmSec", "#1A73E8 · #3F4A5C")
      .containsEntry("MdeTxt", "Light")
      .containsEntry("StaCod", "active");
    assertThat(row(rows, INACTIVE))
      .containsEntry("ThmSec", "Teal · Automatic")
      .containsEntry("MdeTxt", "System")
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");

    // The tone of the avatar depends only on the identifier, so a theme always gets the same colour
    List<String> tones = List.of("primary", "info", "success", "warning", "danger", "neutral");
    rows.forEach(row -> assertThat(row).containsEntry("IniTon",
      tones.get(Integer.parseInt(String.valueOf(row.get("IdeThm"))) % tones.size())));

    // The previous fields stay: the update and view screens and the side panel load them from the selected row
    assertThat(row(rows, CUSTOM)).containsKeys("id", "IdeThm", "NamVal", "Nam", "Act", "ActIco", "Pri", "Srf", "PriPal",
        "PriHex", "SrfPal", "SrfHex", "Mde")
      .containsEntry("NamVal", CUSTOM).containsEntry("Pri", "#1A73E8").containsEntry("PriPal", "custom")
      .containsEntry("PriHex", "#1A73E8").containsEntry("SrfPal", "custom").containsEntry("SrfHex", "#3F4A5C")
      .containsEntry("Mde", "light");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void theSecondaryLineAndTheModeAreTranslated() throws Exception {
    assertThat(row(rows("ThmLst", "{" + SEARCH + ",\"lang\":\"es-ES\"}"), INACTIVE))
      .containsEntry("ThmSec", "Verde azulado · Automática").containsEntry("MdeTxt", "Sistema")
      .containsEntry("StaTxt", "Inactivo");
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void themeListSearchesByNamePrimaryOrSurfaceIgnoringCase() throws Exception {
    assertThat(names(rows("ThmLst", "{\"CrtThmTxt\":\"" + PREFIX.toUpperCase() + " PALETTE\"}"))).containsExactly(PALETTE);
    assertThat(names(rows("ThmLst", "{\"CrtThmTxt\":\"#1a73e8\"}"))).containsExactly(CUSTOM);
    assertThat(names(rows("ThmLst", "{\"CrtThmTxt\":\"3f4a5c\"}"))).containsExactly(CUSTOM);
    assertThat(names(rows("ThmLst", "{\"CrtThmTxt\":\"ROSE\"}"))).contains(PALETTE).doesNotContain(CUSTOM, INACTIVE);
    assertThat(rows("ThmLst", "{\"CrtThmTxt\":\"nobody-matches-qzx793-wqv793\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void themeListWithAnEmptySearchShowsEveryTheme() throws Exception {
    List<String> all = names(rows("ThmLst", "{}"));

    assertThat(names(rows("ThmLst", "{\"CrtThmTxt\":\"\"}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(names(rows("ThmLst", "{\"CrtThmTxt\":null}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(all).contains("sunset", PALETTE, CUSTOM, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void themeListFiltersByState() throws Exception {
    assertThat(names(rows("ThmLst", "{" + SEARCH + ",\"CrtThmSta\":\"active\"}"))).containsExactlyInAnyOrder(PALETTE, CUSTOM);
    assertThat(names(rows("ThmLst", "{" + SEARCH + ",\"CrtThmSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("ThmLst", "{" + SEARCH + ",\"CrtThmSta\":\"\"}"))).containsExactlyInAnyOrder(PALETTE, CUSTOM, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void themeListKeepsItsPreviousFilters() throws Exception {
    // The criteria of the deprecated Thm screen still filter the list
    assertThat(names(rows("ThmLst", "{" + SEARCH + ",\"CrtAct\":0}"))).containsExactly(INACTIVE);
    int id = Integer.parseInt(String.valueOf(row(rows("ThmLst", "{" + SEARCH + "}"), CUSTOM).get("IdeThm")));
    assertThat(names(rows("ThmLst", "{\"CrtNam\":" + id + "}"))).containsExactly(CUSTOM);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("ThmStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), ThemeListQueryTest::count,
        row -> String.valueOf(row.get("tone")))
      .containsExactly(
        tuple("", "SCREEN_TEXT_THEME_FILTER_ALL", 3, ""),
        tuple("active", "SCREEN_TEXT_THEME_FILTER_ACTIVE", 2, ""),
        tuple("inactive", "SCREEN_TEXT_THEME_FILTER_INACTIVE", 1, ""));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsFollowTheSearchAndTheOtherFilters() throws Exception {
    assertThat(rows("ThmStaCnt", "{\"CrtThmTxt\":\"3F4A5C\"}"))
      .extracting(row -> String.valueOf(row.get("value")), ThemeListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 1), tuple("inactive", 0));
    assertThat(rows("ThmStaCnt", "{" + SEARCH + ",\"CrtAct\":0}"))
      .extracting(row -> String.valueOf(row.get("value")), ThemeListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 0), tuple("inactive", 1));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfRowsOfItsOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    for (Map<String, Object> option : rows("ThmStaCnt", "{" + SEARCH + ",\"CrtThmSta\":\"inactive\"}")) {
      String value = String.valueOf(option.get("value"));
      assertThat(rows("ThmLst", "{" + SEARCH + ",\"CrtThmSta\":\"" + value + "\"}"))
        .as("rows of option '%s'", value).hasSize(count(option));
    }
    // Without search, All is every theme
    List<Map<String, Object>> counts = rows("ThmStaCnt", "{}");
    assertThat(count(counts.getFirst())).isEqualTo(rows("ThmLst", "{}").size());
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

  private static Map<String, Object> row(List<Map<String, Object>> rows, String name) {
    return rows.stream()
      .filter(row -> name.equals(String.valueOf(row.get("NamVal")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + name));
  }

  private static List<String> names(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("NamVal")).trim()).toList();
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

  private int nextThemeId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeThm) FROM AweThm");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void insertTheme(int id, String name, String primary, String surface, String mode, int active) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweThm (IdeThm, Nam, Act, Pri, Srf, Mde) VALUES (?, ?, ?, ?, ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, name);
      statement.setInt(3, active);
      statement.setString(4, primary);
      statement.setString(5, surface);
      statement.setString(6, mode);
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
