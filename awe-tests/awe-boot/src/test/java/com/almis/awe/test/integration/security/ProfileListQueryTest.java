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
 * The profile list (ProLst) returns, for the profiles screen (profile-admin), a secondary line with the acronym and the
 * theme, the name and the tone of the avatar of the profile and its state (active or inactive) with its label and its
 * tone, and keeps every previous field. The search of the grid (CrtProTxt) finds profiles by name, acronym, theme or
 * initial screen, the quick filter (CrtProSta) by state, and the options query (ProStaCnt) counts the profiles of each
 * option with the same search.
 */
@Tag("integration")
@DisplayName("Profile list query tests")
class ProfileListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String PREFIX = "prflst793";
  // Active, with the sunset theme and an initial screen
  private static final String THEMED = PREFIX + " themed";
  private static final String THEMED_ACRONYM = "Q7A";
  // Active, without theme
  private static final String PLAIN = PREFIX + " plain";
  private static final String PLAIN_ACRONYM = "Q7B";
  // Inactive, with the sky theme
  private static final String INACTIVE = PREFIX + " inactive";
  private static final String INACTIVE_ACRONYM = "Q7C";
  private static final String SCREEN = "screen-qzx793";
  private static final String SEARCH = "\"CrtProTxt\":\"" + PREFIX + "\"";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createProfiles() throws Exception {
    deleteProfiles();
    int id = nextProfileId();
    insertProfile(id, THEMED_ACRONYM, THEMED, "sunset", SCREEN, 1);
    insertProfile(id + 1, PLAIN_ACRONYM, PLAIN, null, null, 1);
    insertProfile(id + 2, INACTIVE_ACRONYM, INACTIVE, "sky", null, 0);
  }

  @AfterEach
  void deleteProfiles() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AwePro WHERE Nam LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void profileListReturnsSecondaryLineAvatarAndState() throws Exception {
    List<Map<String, Object>> rows = rows("ProLst", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(names(rows)).containsExactlyInAnyOrder(THEMED, PLAIN, INACTIVE);
    // The secondary line is the acronym and the theme, or just the acronym without a theme
    assertThat(row(rows, THEMED))
      .containsEntry("ProSec", THEMED_ACRONYM + " · sunset")
      .containsEntry("Ini", THEMED)
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");
    assertThat(row(rows, PLAIN))
      .containsEntry("ProSec", PLAIN_ACRONYM)
      .containsEntry("StaCod", "active");
    assertThat(row(rows, INACTIVE))
      .containsEntry("ProSec", INACTIVE_ACRONYM + " · sky")
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");

    // The tone of the avatar depends only on the identifier, so a profile always gets the same colour
    List<String> tones = List.of("primary", "info", "success", "warning", "danger", "neutral");
    rows.forEach(row -> assertThat(row).containsEntry("IniTon",
      tones.get(Integer.parseInt(String.valueOf(row.get("IdePro"))) % tones.size())));

    // The previous fields stay
    assertThat(row(rows, THEMED)).containsKeys("id", "IdePro", "Acr", "Nam", "ThmTxt", "IdeThm", "ScrIni", "Res", "Act", "ActIco")
      .containsEntry("ThmTxt", "sunset").containsEntry("ScrIni", SCREEN);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void profileListSearchesByNameAcronymThemeOrScreenIgnoringCase() throws Exception {
    assertThat(names(rows("ProLst", "{\"CrtProTxt\":\"" + PREFIX.toUpperCase() + " PLAIN\"}"))).containsExactly(PLAIN);
    assertThat(names(rows("ProLst", "{\"CrtProTxt\":\"q7c\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("ProLst", "{\"CrtProTxt\":\"SKY\"}"))).contains(INACTIVE).doesNotContain(THEMED, PLAIN);
    assertThat(names(rows("ProLst", "{\"CrtProTxt\":\"QZX793\"}"))).containsExactly(THEMED);
    assertThat(rows("ProLst", "{\"CrtProTxt\":\"nobody-matches-qzx793-wqv793\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void profileListWithAnEmptySearchShowsEveryProfile() throws Exception {
    List<String> all = names(rows("ProLst", "{}"));

    assertThat(names(rows("ProLst", "{\"CrtProTxt\":\"\"}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(names(rows("ProLst", "{\"CrtProTxt\":null}"))).containsExactlyInAnyOrderElementsOf(all);
    assertThat(all).contains("administrator", THEMED, PLAIN, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void profileListFiltersByState() throws Exception {
    assertThat(names(rows("ProLst", "{" + SEARCH + ",\"CrtProSta\":\"active\"}"))).containsExactlyInAnyOrder(THEMED, PLAIN);
    assertThat(names(rows("ProLst", "{" + SEARCH + ",\"CrtProSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("ProLst", "{" + SEARCH + ",\"CrtProSta\":\"\"}"))).containsExactlyInAnyOrder(THEMED, PLAIN, INACTIVE);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void profileListKeepsItsPreviousFilters() throws Exception {
    // The criteria of the deprecated Prf screen still filter the list
    assertThat(names(rows("ProLst", "{" + SEARCH + ",\"CrtAct\":0}"))).containsExactly(INACTIVE);
    assertThat(names(rows("ProLst", "{" + SEARCH + ",\"CrtThm\":1}"))).containsExactly(THEMED);
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("ProStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), ProfileListQueryTest::count,
        row -> String.valueOf(row.get("tone")))
      .containsExactly(
        tuple("", "SCREEN_TEXT_PROFILE_FILTER_ALL", 3, ""),
        tuple("active", "SCREEN_TEXT_PROFILE_FILTER_ACTIVE", 2, ""),
        tuple("inactive", "SCREEN_TEXT_PROFILE_FILTER_INACTIVE", 1, ""));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void countsFollowTheSearchAndTheOtherFilters() throws Exception {
    assertThat(rows("ProStaCnt", "{\"CrtProTxt\":\"SKY\"}"))
      .extracting(row -> String.valueOf(row.get("value")), ProfileListQueryTest::count)
      .contains(tuple("inactive", 1));
    assertThat(rows("ProStaCnt", "{\"CrtProTxt\":\"" + PLAIN_ACRONYM.toLowerCase() + "\"}"))
      .extracting(row -> String.valueOf(row.get("value")), ProfileListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 1), tuple("inactive", 0));
    assertThat(rows("ProStaCnt", "{" + SEARCH + ",\"CrtThm\":2}"))
      .extracting(row -> String.valueOf(row.get("value")), ProfileListQueryTest::count)
      .containsExactly(tuple("", 1), tuple("active", 0), tuple("inactive", 1));
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void eachCountIsTheNumberOfRowsOfItsOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    for (Map<String, Object> option : rows("ProStaCnt", "{" + SEARCH + ",\"CrtProSta\":\"inactive\"}")) {
      String value = String.valueOf(option.get("value"));
      assertThat(rows("ProLst", "{" + SEARCH + ",\"CrtProSta\":\"" + value + "\"}"))
        .as("rows of option '%s'", value).hasSize(count(option));
    }
    // Without search, All is every profile
    List<Map<String, Object>> counts = rows("ProStaCnt", "{}");
    assertThat(count(counts.getFirst())).isEqualTo(rows("ProLst", "{}").size());
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
      .filter(row -> name.equals(String.valueOf(row.get("Nam")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + name));
  }

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

  private int nextProfileId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdePro) FROM AwePro");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  /**
   * Insert a profile with a theme by its name (null: no theme)
   */
  private void insertProfile(int id, String acronym, String name, String theme, String screen, int active) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AwePro (IdePro, Acr, Nam, IdeThm, ScrIni, Res, Act) "
             + "VALUES (?, ?, ?, (SELECT IdeThm FROM AweThm WHERE Nam = ?), ?, ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, acronym);
      statement.setString(3, name);
      if (theme == null) {
        statement.setNull(4, Types.VARCHAR);
      } else {
        statement.setString(4, theme);
      }
      statement.setString(5, screen);
      statement.setNull(6, Types.VARCHAR);
      statement.setInt(7, active);
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
