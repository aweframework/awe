package com.almis.awe.test.integration.database;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cacheable queries whose result depends on session values must never share a cache entry between users
 */
@DisplayName("Query cache keyed by session values")
@Tag("integration")
@WithMockUser
@TestPropertySource(properties = "spring.cache.type=simple")
class QueryCacheSessionTest extends AbstractSpringAppIntegrationTest {

  private static final int RESTRICTION_ID = 990001;
  private static final int DONALD_ID = 2;
  private static final String RESTRICTED_OPTION = "cache-session-leak-option";

  @Autowired
  private CacheManager cacheManager;

  @Autowired
  private DataSource dataSource;

  @Autowired
  private AweElements aweElements;

  @Autowired
  private QueryLauncher queryLauncher;

  private MockHttpSession session;

  @BeforeEach
  void setUpSession() {
    session = new MockHttpSession();
    Cache queryData = cacheManager.getCache("queryData");
    if (queryData != null) {
      queryData.clear();
    }
  }

  @Test
  void connectedUserIsNotSharedBetweenUsers() throws Exception {
    setSessionParameter("user", "test");
    assertThat(column(launchQuery("connectedUser"), "value")).containsExactly("test");

    setSessionParameter("user", "donald");
    assertThat(column(launchQuery("connectedUser"), "value")).containsExactly("donald");
  }

  @Test
  void screenRestrictionsAreNotSharedBetweenUsers() throws Exception {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.update("insert into AweScrRes (IdeAweScrRes, IdeOpe, IdePro, IdeMod, Opt, AccMod, Act) values (?, ?, null, null, ?, 'R', 1)",
      RESTRICTION_ID, DONALD_ID, RESTRICTED_OPTION);
    try {
      setSessionParameter("user", "test");
      assertThat(column(launchQuery("getScreenRestrictions"), "option")).doesNotContain(RESTRICTED_OPTION);

      setSessionParameter("user", "donald");
      assertThat(column(launchQuery("getScreenRestrictions"), "option")).contains(RESTRICTED_OPTION);
    } finally {
      jdbcTemplate.update("delete from AweScrRes where IdeAweScrRes = ?", RESTRICTION_ID);
    }
  }

  @Test
  void translatedResultsAreNotSharedBetweenLanguages() throws Exception {
    setSessionParameter("user", "test");
    setSessionParameter("language", "en-GB");
    assertThat(activeLabels(launchQuery("ModLst"))).contains("Yes").doesNotContain("Si");

    setSessionParameter("language", "es-ES");
    assertThat(activeLabels(launchQuery("ModLst"))).contains("Si").doesNotContain("Yes");
  }

  @Test
  void cacheKeyDependsOnTargetDatabase() throws Exception {
    Query query = aweElements.getQuery("ModLst");
    ObjectNode firstDatabase = JsonNodeFactory.instance.objectNode().put("_database_", "awedb");
    ObjectNode secondDatabase = JsonNodeFactory.instance.objectNode().put("_database_", "awedb2");

    assertThat(queryLauncher.getCacheKey(query, firstDatabase))
      .isNotEqualTo(queryLauncher.getCacheKey(query, secondDatabase))
      .isEqualTo(queryLauncher.getCacheKey(query, firstDatabase.deepCopy()));
  }

  private List<String> activeLabels(JsonNode rows) {
    List<String> labels = new ArrayList<>();
    rows.forEach(row -> labels.add(row.get("ActIco").get("label").asString()));
    return labels;
  }

  private void setSessionParameter(String name, String value) throws Exception {
    mockMvc.perform(post(String.format("/session/set/%s", name))
        .with(csrf())
        .param("value", value)
        .session(session))
      .andExpect(status().isOk());
  }

  private JsonNode launchQuery(String queryName) throws Exception {
    String response = mockMvc.perform(post("/action/data/" + queryName)
        .with(csrf())
        .session(session)
        .content("{}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response).get(0).get("parameters").get("datalist").get("rows");
  }

  private List<String> column(JsonNode rows, String field) {
    List<String> values = new ArrayList<>();
    rows.forEach(row -> values.add(row.get(field).asString()));
    return values;
  }
}
