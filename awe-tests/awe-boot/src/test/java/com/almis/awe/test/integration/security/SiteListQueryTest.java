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
 * The site list (SitLst) returns, for the sites screen (site-admin), the name and the tone of the avatar of the site and
 * its state (active or inactive) with its label and its tone. The search of the grid (CrtSitTxt) finds sites by name, the
 * quick filter (CrtSitSta) by state, and the options query (SitStaCnt) counts the sites of each option with the same
 * search.
 */
@Tag("integration")
@DisplayName("Site list query tests")
class SiteListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "sitlst793";
  private static final String ACTIVE = PREFIX + " active";
  private static final String INACTIVE = PREFIX + " inactive";
  private static final String SEARCH = "\"CrtSitTxt\":\"" + PREFIX + "\"";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createSites() throws Exception {
    deleteSites();
    int id = nextSiteId();
    insertSite(id, ACTIVE, 3, 1);
    insertSite(id + 1, INACTIVE, 4, 0);
  }

  @AfterEach
  void deleteSites() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweSit WHERE Nam LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void siteListReturnsAvatarAndState() throws Exception {
    List<Map<String, Object>> rows = rows("SitLst", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(names(rows)).containsExactlyInAnyOrder(ACTIVE, INACTIVE);
    assertThat(row(rows, ACTIVE))
      .containsEntry("Ini", ACTIVE)
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success")
      .containsKeys("id", "IdeSit", "Ord", "Act", "IniTon");
    assertThat(row(rows, INACTIVE))
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void siteListSearchesByNameIgnoringCase() throws Exception {
    assertThat(names(rows("SitLst", "{\"CrtSitTxt\":\"" + INACTIVE.toUpperCase() + "\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("SitLst", "{\"CrtSitTxt\":\"" + PREFIX.toUpperCase() + "\"}")))
      .containsExactlyInAnyOrder(ACTIVE, INACTIVE);
    assertThat(rows("SitLst", "{\"CrtSitTxt\":\"nobody-matches-qzx793-wqv793\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void siteListFiltersByState() throws Exception {
    assertThat(names(rows("SitLst", "{" + SEARCH + ",\"CrtSitSta\":\"active\"}"))).containsExactly(ACTIVE);
    assertThat(names(rows("SitLst", "{" + SEARCH + ",\"CrtSitSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("SitLst", "{" + SEARCH + ",\"CrtSitSta\":\"\"}"))).containsExactlyInAnyOrder(ACTIVE, INACTIVE);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("SitStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), SiteListQueryTest::count)
      .containsExactly(
        tuple("", "SCREEN_TEXT_SITE_FILTER_ALL", 2),
        tuple("active", "SCREEN_TEXT_SITE_FILTER_ACTIVE", 1),
        tuple("inactive", "SCREEN_TEXT_SITE_FILTER_INACTIVE", 1));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void countsFollowTheSearchAndNotTheChosenOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    assertThat(rows("SitStaCnt", "{\"CrtSitTxt\":\"" + INACTIVE + "\",\"CrtSitSta\":\"active\"}"))
      .extracting(row -> String.valueOf(row.get("value")), SiteListQueryTest::count)
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
      .filter(row -> name.equals(String.valueOf(row.get("Nam")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + name));
  }

  private static List<String> names(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("Nam")).trim()).toList();
  }

  private int nextSiteId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeSit) FROM AweSit");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void insertSite(int id, String name, int order, int active) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweSit (IdeSit, Nam, Ord, Act) VALUES (?, ?, ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, name);
      statement.setInt(3, order);
      statement.setInt(4, active);
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
