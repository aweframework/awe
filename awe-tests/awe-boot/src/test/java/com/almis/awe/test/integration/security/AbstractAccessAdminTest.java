package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Data and requests of the access window tests (access-admin) on the embedded database: the subjects of the test data
 * (profiles ADM and OPE, user donald), the rules they insert (removed before and after each test, with the rules of
 * the test options) and the data and maintain requests of a signed-in user.
 */
abstract class AbstractAccessAdminTest extends AbstractSpringAppIntegrationTest {

  static final String SIGNED_IN_USER = "test";
  static final int ADM = 1;
  static final int OPE = 3;
  static final int DONALD = 2;
  // Rows inserted by the tests start here, far from the rows of the maintains (ScrResKey)
  static final int FIRST_ROW = 991000;
  // The subject as the cards, the maintains and the grids of the panels send it: without max (the engine pages the
  // queries of the request with gridRowsPerPage)
  static final String OPE_SUBJECT = "\"subject-type\":\"profile\",\"IdePro\":" + OPE;
  static final String DONALD_SUBJECT = "\"subject-type\":\"user\",\"IdeOpe\":" + DONALD + ",\"IdePro\":" + ADM;
  // The tree is a treegrid with load-all: no page (max 0)
  static final String OPE_TREE = "\"max\":0," + OPE_SUBJECT;
  static final String DONALD_TREE = "\"max\":0," + DONALD_SUBJECT;
  static final List<String> OPTIONS = List.of("tools", "sites", "users", "databases", "themes",
    "encrypt-tools");

  @Autowired
  DataSource dataSource;

  @Autowired
  CacheManager cacheManager;

  @BeforeEach
  @AfterEach
  void deleteRules() throws Exception {
    execute("DELETE FROM AweScrRes WHERE IdeAweScrRes >= " + FIRST_ROW + " OR Opt IN ('" + String.join("','", OPTIONS)
      + "')");
    // The rules are read with cacheable queries; the maintains clear the cache, the inserts of the test do not
    Cache queryData = cacheManager.getCache("queryData");
    if (queryData != null) {
      queryData.clear();
    }
  }

  /**
   * Rules of the test options: user|profile|option|rule
   */
  List<String> rules() throws Exception {
    List<String> rules = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT IdeOpe, IdePro, Opt, AccMod FROM AweScrRes "
           + "WHERE Opt IN ('" + String.join("','", OPTIONS) + "') AND Act = 1");
         ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        rules.add(result.getObject(1) + "|" + result.getObject(2) + "|" + result.getString(3).trim() + "|"
          + result.getString(4).trim());
      }
    }
    return rules;
  }

  void insertRule(int id, Integer user, Integer profile, String option, String rule) throws Exception {
    execute("INSERT INTO AweScrRes (IdeAweScrRes, IdeOpe, IdePro, Opt, AccMod, Act) VALUES (" + id + ", " + user + ", "
      + profile + ", '" + option + "', '" + rule + "', 1)");
  }

  String maintain(String target, String body) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  static void assertOk(String response) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }

  List<Map<String, Object>> rows(String query, String parameters) throws Exception {
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

  static Map<String, Object> row(List<Map<String, Object>> rows, String option) {
    return rows.stream().filter(row -> option.equals(row.get("option-id"))).findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + option));
  }

  static int number(Object value) {
    return Integer.parseInt(String.valueOf(value));
  }

  /**
   * Text of a field, empty when the field is null
   */
  static String text(Map<String, Object> row, String field) {
    Object value = row.get(field);
    return value == null ? "" : String.valueOf(value);
  }

  MockHttpSession signedInSession() throws Exception {
    MockHttpSession session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", SIGNED_IN_USER).session(session)).andReturn();
    mockMvc.perform(post("/session/set/language").with(csrf()).param("value", "en-GB").session(session)).andReturn();
    return session;
  }

  int count(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1);
    }
  }

  void execute(String sql) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.executeUpdate();
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }
}
