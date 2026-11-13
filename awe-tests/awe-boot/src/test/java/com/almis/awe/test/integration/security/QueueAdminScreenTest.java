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
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The side panel of the message queues screen (queue-admin) refuses a new queue whose alias is taken (QueAlsUni, checked
 * with control-unique-cancel before QueNew) and never loads the stored password of a queue: QueUpd writes the password
 * only when one is typed (FlgPwdUpd is 1) and keeps the stored encrypted one otherwise.
 */
@Tag("integration")
@DisplayName("Queue admin screen tests")
class QueueAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "queadm";
  private static final String QUEUE = PREFIX + " one";
  private static final String NEW_QUEUE = PREFIX + " new";
  private static final String STORED_PASSWORD = "stored-encrypted-password";
  private static final String TYPED_PASSWORD = "typed-secret";

  @Autowired
  private DataSource dataSource;

  private int queueId;

  @BeforeEach
  void createQueue() throws Exception {
    deleteQueues();
    queueId = nextQueueId();
    execute("INSERT INTO AweQue (IdeAweQue, Als, Des, QueTyp, ConTyp, JmsBrk, JmsUsr, JmsPwd, DstNam, Act) VALUES ("
      + queueId + ", '" + QUEUE + "', 'Queue of the tests', 'QUEUE', 'R', 'tcp://broker.queadm.test:61616', 'jmsuser', '"
      + STORED_PASSWORD + "', 'queadm.destination', 1)");
  }

  @AfterEach
  void deleteQueues() throws Exception {
    execute("DELETE FROM AweQue WHERE Als LIKE '" + PREFIX + "%'");
    execute("DELETE FROM HISAweQue WHERE Als LIKE '" + PREFIX + "%'");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("The alias check of the creation cancels a queue whose alias is taken and lets a free one pass")
  void aliasCheckRefusesTheAliasInUse() throws Exception {
    assertThat(responseTypes(uniqueCheck(QUEUE))).contains("message", "cancel");
    assertThat(responseTypes(uniqueCheck(NEW_QUEUE))).doesNotContain("message", "cancel");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("QueUpd keeps the stored password when none is typed")
  void updateKeepsThePasswordWhenNoneIsTyped() throws Exception {
    assertOk(maintain("QueUpd", panel(QUEUE + " changed", "", 0)));

    assertThat(column("Als")).isEqualTo(QUEUE + " changed");
    assertThat(column("JmsPwd")).isEqualTo(STORED_PASSWORD);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("QueUpd writes the typed password encrypted")
  void updateWritesTheTypedPasswordEncrypted() throws Exception {
    assertOk(maintain("QueUpd", panel(QUEUE, TYPED_PASSWORD, 1)));

    assertThat(column("JmsPwd")).isNotBlank().isNotIn(STORED_PASSWORD, TYPED_PASSWORD);
  }

  /**
   * Criteria of the side panel with the selected queue of the grid
   */
  private ObjectNode panel(String alias, String password, int passwordTyped) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.set("IdeAweQue", objectMapper.createArrayNode().add(queueId));
    parameters.put("IdeAweQue.selected", queueId);
    parameters.put("Als", alias);
    parameters.put("Des", "Queue of the tests");
    parameters.put("QueTyp", "QUEUE");
    parameters.put("ConTyp", "R");
    parameters.put("JmsBrk", "tcp://broker.queadm.test:61616");
    parameters.put("JmsUsr", "jmsuser");
    parameters.put("JmsPwd", password);
    parameters.put("FlgPwdUpd", passwordTyped);
    parameters.put("DstNam", "queadm.destination");
    parameters.put("Act", 1);
    parameters.put("max", 30);
    return parameters;
  }

  private String uniqueCheck(String alias) throws Exception {
    return mockMvc.perform(post("/action/control-unique-cancel/QueAlsUni")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"Als\":\"" + alias + "\",\"target\":\"UniMsg\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  private String maintain(String target, ObjectNode parameters) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(parameters))
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
  }

  private static List<String> responseTypes(String response) {
    return JsonPath.read(response, "$[*].type");
  }

  private static void assertOk(String response) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }

  private String column(String column) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT " + column + " FROM AweQue WHERE IdeAweQue = " + queueId);
         ResultSet result = statement.executeQuery()) {
      assertThat(result.next()).as("queue %s", queueId).isTrue();
      String value = result.getString(1);
      return value == null ? null : value.trim();
    }
  }

  private int nextQueueId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeAweQue) FROM AweQue");
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
