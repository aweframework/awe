package com.almis.awe.test.integration.database;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Screen personalization against the real SQL: the save is audited and the history reads it back newest first with the
 * value before and after, a save that mixes a new rule and a changed one keeps each on its own rule, and the logins of
 * an import are resolved to users
 */
@DisplayName("Screen personalization")
@Tag("integration")
@WithMockUser(username = "test", password = "test")
class ScreenPersonalizationTest extends AbstractSpringAppIntegrationTest {

  private static final String SCREEN = "CrtTstLeft";
  private static final String COMPONENT = "ButBck";

  @Autowired
  private DataSource dataSource;

  private MockHttpSession session;

  @BeforeEach
  void openSession() throws Exception {
    session = new MockHttpSession();
    mockMvc.perform(post("/session/set/user").with(csrf()).param("value", "test").session(session)).andExpect(status().isOk());
    mockMvc.perform(post("/session/set/restriction").with(csrf()).param("value", "administrator").session(session))
      .andExpect(status().isOk());
    deleteRules();
  }

  @AfterEach
  void deleteRules() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    jdbc.update("delete from AweScrCnf where Scr = ?", SCREEN);
    jdbc.update("delete from HISAweScrCnf where Scr = ?", SCREEN);
  }

  @Test
  @DisplayName("A save is audited, mixing a new rule and a changed one keeps each on its own rule")
  void saveIsAuditedAndKeepsEachRule() throws Exception {
    save("[\"INSERT\"]", "[null]", "[\"label\"]", "[\"First\"]");
    int ruleId = new JdbcTemplate(dataSource).queryForObject("select IdeAweScrCnf from AweScrCnf where Scr = ?", Integer.class, SCREEN);

    // The insert goes first: the sequence of its row must not be written over the key of the update
    save("[\"INSERT\",\"UPDATE\"]", "[null," + ruleId + "]", "[\"visible\",\"label\"]", "[\"false\",\"Second\"]");

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    assertThat(jdbc.queryForObject("select count(*) from AweScrCnf where Scr = ?", Integer.class, SCREEN)).isEqualTo(2);
    assertThat(jdbc.queryForObject("select Val from AweScrCnf where IdeAweScrCnf = ?", String.class, ruleId)).isEqualTo("Second");

    List<JsonNode> history = rows(query("getScreenConfigurationHistory", "{\"screen\":\"" + SCREEN + "\"}"));
    assertThat(history).hasSize(3);
    assertThat(history.get(0).path("ruleId").asString()).isEqualTo(String.valueOf(ruleId));
    assertThat(history.get(0).path("valueBefore").asString()).isEqualTo("First");
    assertThat(history.get(0).path("valueAfter").asString()).isEqualTo("Second");
    assertThat(history.get(2).path("action").asString()).isEqualTo("created");
  }

  @Test
  @DisplayName("The logins of an import are resolved to the ids of their users")
  void importLoginsAreResolvedToUsers() throws Exception {
    List<String> logins = rows(query("getScreenImportUsers", "{\"logins\":[\"test\",\"donald\",\"nobody\"]}")).stream()
      .map(row -> row.path("login").asString().trim()).toList();

    assertThat(logins).containsExactlyInAnyOrder("test", "donald");
  }

  private void save(String rowTypes, String ids, String attributes, String values) throws Exception {
    int rows = objectMapper.readTree(rowTypes).size();
    mockMvc.perform(post("/action/maintain/updateScreenConfiguration")
        .session(session)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"GrdScrCnf-RowTyp\":" + rowTypes + ",\"IdeAweScrCnf\":" + ids + ",\"IdeOpe\":" + repeat("null", rows)
          + ",\"IdePro\":" + repeat("null", rows) + ",\"Scr\":" + repeat("\"" + SCREEN + "\"", rows)
          + ",\"Nam\":" + repeat("\"" + COMPONENT + "\"", rows) + ",\"Atr\":" + attributes + ",\"Val\":" + values
          + ",\"Act\":" + repeat("1", rows) + ",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private JsonNode query(String name, String variables) throws Exception {
    String response = mockMvc.perform(post("/action/data/" + name)
        .session(session)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(variables.substring(0, variables.length() - 1) + ",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response);
  }

  private List<JsonNode> rows(JsonNode response) {
    List<JsonNode> rows = new ArrayList<>();
    response.findValue("rows").forEach(rows::add);
    return rows;
  }

  private static String repeat(String value, int times) {
    return "[" + String.join(",", java.util.Collections.nCopies(times, value)) + "]";
  }
}
