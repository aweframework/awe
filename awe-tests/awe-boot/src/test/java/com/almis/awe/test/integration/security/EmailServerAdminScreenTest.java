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
 * The side panel of the mail servers screen (email-server-admin) never loads the stored password of a server: EmlSrvUpd
 * writes the password only when one is typed (FlgPwdUpd is 1) and keeps the stored encrypted one otherwise.
 */
@Tag("integration")
@DisplayName("Mail server admin screen tests")
class EmailServerAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "emlsrvadm";
  private static final String SERVER = PREFIX + " one";
  private static final String STORED_PASSWORD = "stored-encrypted-password";
  private static final String TYPED_PASSWORD = "typed-secret";

  @Autowired
  private DataSource dataSource;

  private int serverId;

  @BeforeEach
  void createServer() throws Exception {
    deleteServers();
    serverId = nextServerId();
    execute("INSERT INTO AweEmlSrv (IdeAweEmlSrv, SrvNam, Hst, Ath, EmlUsr, EmlPwd, Act) VALUES (" + serverId + ", '"
      + SERVER + "', 'smtp.emlsrvadm.test', 1, 'emluser', '" + STORED_PASSWORD + "', 1)");
  }

  @AfterEach
  void deleteServers() throws Exception {
    execute("DELETE FROM AweEmlSrv WHERE SrvNam LIKE '" + PREFIX + "%'");
    execute("DELETE FROM HISAweEmlSrv WHERE SrvNam LIKE '" + PREFIX + "%'");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("EmlSrvUpd keeps the stored password when none is typed")
  void updateKeepsThePasswordWhenNoneIsTyped() throws Exception {
    assertOk(maintain("EmlSrvUpd", panel(SERVER + " changed", "", 0)));

    assertThat(column("SrvNam")).isEqualTo(SERVER + " changed");
    assertThat(column("EmlPwd")).isEqualTo(STORED_PASSWORD);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("EmlSrvUpd writes the typed password encrypted")
  void updateWritesTheTypedPasswordEncrypted() throws Exception {
    assertOk(maintain("EmlSrvUpd", panel(SERVER, TYPED_PASSWORD, 1)));

    assertThat(column("EmlPwd")).isNotBlank().isNotIn(STORED_PASSWORD, TYPED_PASSWORD);
  }

  /**
   * Criteria of the side panel with the selected server of the grid
   */
  private ObjectNode panel(String name, String password, int passwordTyped) {
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.set("IdeAweEmlSrv", objectMapper.createArrayNode().add(serverId));
    parameters.put("IdeAweEmlSrv.selected", serverId);
    parameters.put("SrvNam", name);
    parameters.put("Hst", "smtp.emlsrvadm.test");
    parameters.put("Ath", 1);
    parameters.put("EmlUsr", "emluser");
    parameters.put("EmlPwd", password);
    parameters.put("FlgPwdUpd", passwordTyped);
    parameters.put("Act", 1);
    parameters.put("max", 30);
    return parameters;
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

  private static void assertOk(String response) {
    List<String> types = JsonPath.read(response, "$[?(@.type == 'message')].parameters.type");
    assertThat(types).as(response).containsOnly("ok");
  }

  private String column(String column) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT " + column + " FROM AweEmlSrv WHERE IdeAweEmlSrv = " + serverId);
         ResultSet result = statement.executeQuery()) {
      assertThat(result.next()).as("server %s", serverId).isTrue();
      String value = result.getString(1);
      return value == null ? null : value.trim();
    }
  }

  private int nextServerId() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT MAX(IdeAweEmlSrv) FROM AweEmlSrv");
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
