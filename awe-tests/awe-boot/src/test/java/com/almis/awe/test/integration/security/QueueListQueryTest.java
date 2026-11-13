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
 * The queue list (QueLst) returns, for the message queues screen (queue-admin), the alias, the destination and the state
 * (active or inactive) of each queue with its label and its tone. The search of the grid (CrtAls) finds queues by alias
 * ignoring case, the quick filter (CrtQueSta) by state, and the options query (QueStaCnt, with QueStaCntAct and
 * QueStaCntIna) counts the queues of each option with the same search.
 */
@Tag("integration")
@DisplayName("Queue list query tests")
class QueueListQueryTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "quelst";
  private static final String ACTIVE = PREFIX + " active";
  private static final String INACTIVE = PREFIX + " inactive";
  private static final String SEARCH = "\"CrtAls\":\"" + PREFIX + "\"";

  @Autowired
  private DataSource dataSource;

  @BeforeEach
  void createQueues() throws Exception {
    deleteQueues();
    int id = nextQueueId();
    insertQueue(id, ACTIVE, 1);
    insertQueue(id + 1, INACTIVE, 0);
  }

  @AfterEach
  void deleteQueues() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("DELETE FROM AweQue WHERE Als LIKE ?")) {
      statement.setString(1, PREFIX + "%");
      statement.executeUpdate();
      commit(connection);
    }
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void queueListReturnsDestinationAndState() throws Exception {
    List<Map<String, Object>> rows = rows("QueLst", "{" + SEARCH + ",\"lang\":\"en-GB\"}");

    assertThat(names(rows)).containsExactlyInAnyOrder(ACTIVE, INACTIVE);
    assertThat(row(rows, ACTIVE))
      .containsEntry("DstNam", "quelst.destination")
      .containsEntry("StaCod", "active")
      .containsEntry("StaTxt", "Active")
      .containsEntry("StaTon", "success");
    assertThat(row(rows, INACTIVE))
      .containsEntry("StaCod", "inactive")
      .containsEntry("StaTxt", "Inactive")
      .containsEntry("StaTon", "neutral");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void queueListSearchesByAliasIgnoringCase() throws Exception {
    assertThat(names(rows("QueLst", "{\"CrtAls\":\"" + INACTIVE.toUpperCase() + "\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("QueLst", "{\"CrtAls\":\"" + PREFIX.toUpperCase() + "\"}")))
      .containsExactlyInAnyOrder(ACTIVE, INACTIVE);
    assertThat(rows("QueLst", "{\"CrtAls\":\"nobody-matches-qzx-wqv\"}")).isEmpty();
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void queueListFiltersByState() throws Exception {
    assertThat(names(rows("QueLst", "{" + SEARCH + ",\"CrtQueSta\":\"active\"}"))).containsExactly(ACTIVE);
    assertThat(names(rows("QueLst", "{" + SEARCH + ",\"CrtQueSta\":\"inactive\"}"))).containsExactly(INACTIVE);
    assertThat(names(rows("QueLst", "{" + SEARCH + ",\"CrtQueSta\":\"\"}"))).containsExactlyInAnyOrder(ACTIVE, INACTIVE);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void countsListTheOptionsInOrder() throws Exception {
    assertThat(rows("QueStaCnt", "{" + SEARCH + "}"))
      .extracting(row -> String.valueOf(row.get("value")), row -> row.get("label"), QueueListQueryTest::count)
      .containsExactly(
        tuple("", "SCREEN_TEXT_QUEUE_FILTER_ALL", 2),
        tuple("active", "SCREEN_TEXT_QUEUE_FILTER_ACTIVE", 1),
        tuple("inactive", "SCREEN_TEXT_QUEUE_FILTER_INACTIVE", 1));
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  void countsFollowTheSearchAndNotTheChosenOption() throws Exception {
    // The chosen option is sent with the counts too: it must not change them
    assertThat(rows("QueStaCnt", "{\"CrtAls\":\"" + INACTIVE.toUpperCase() + "\",\"CrtQueSta\":\"active\"}"))
      .extracting(row -> String.valueOf(row.get("value")), QueueListQueryTest::count)
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

  private static Map<String, Object> row(List<Map<String, Object>> rows, String alias) {
    return rows.stream()
      .filter(row -> alias.equals(String.valueOf(row.get("Als")).trim()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("No row for " + alias));
  }

  private static List<String> names(List<Map<String, Object>> rows) {
    return rows.stream().map(row -> String.valueOf(row.get("Als")).trim()).toList();
  }

  private int nextQueueId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeAweQue) FROM AweQue");
         ResultSet result = statement.executeQuery()) {
      result.next();
      return result.getInt(1) + 1000;
    }
  }

  private void insertQueue(int id, String alias, int active) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "INSERT INTO AweQue (IdeAweQue, Als, QueTyp, ConTyp, DstNam, Act) VALUES (?, ?, 'QUEUE', 'J', ?, ?)")) {
      statement.setInt(1, id);
      statement.setString(2, alias);
      statement.setString(3, "quelst.destination");
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
