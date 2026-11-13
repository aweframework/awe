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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The side panel of the mail servers screen (email-server-admin) never loads the stored password of a server: EmlSrvUpd
 * writes the password only when one is typed (FlgPwdUpd is 1) and keeps the stored encrypted one otherwise. EmlSrvNew and
 * EmlSrvUpd store the port, the security mode, the authentication type (2 is OAuth2) and the OAuth2 client registration
 * of the panel, and audit them in HISAweEmlSrv.
 */
@Tag("integration")
@DisplayName("Mail server admin screen tests")
class EmailServerAdminScreenTest extends AbstractSpringAppIntegrationTest {

  private static final String PREFIX = "emlsrvadm";
  private static final String SERVER = PREFIX + " one";
  private static final String STORED_PASSWORD = "stored-encrypted-password";
  private static final String TYPED_PASSWORD = "typed-secret";
  private static final String OAUTH2_SERVER = PREFIX + " oauth2";
  private static final String SENDER = "noreply@emlsrvadm.test";
  private static final String REGISTRATION = "emlsrvadm-mail";

  @Autowired
  private DataSource dataSource;

  private int serverId;

  @BeforeEach
  void createServer() throws Exception {
    deleteServers();
    serverId = nextServerId();
    execute("INSERT INTO AweEmlSrv (IdeAweEmlSrv, SrvNam, Hst, Prt, SecMod, Ath, EmlUsr, EmlPwd, Act) VALUES (" + serverId
      + ", '" + SERVER + "', 'smtp.emlsrvadm.test', 2525, 'SSL', 1, 'emluser', '" + STORED_PASSWORD + "', 1)");
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

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("EmlSrvNew stores and audits the port, the security mode and the OAuth2 registration")
  void newServerStoresPortSecurityAndOAuth2Registration() throws Exception {
    ObjectNode parameters = oauth2(panel(OAUTH2_SERVER, "", 0), 587, "STARTTLS");
    parameters.remove(List.of("IdeAweEmlSrv", "IdeAweEmlSrv.selected"));

    assertOk(maintain("EmlSrvNew", parameters));

    String server = "SrvNam = '" + OAUTH2_SERVER + "'";
    assertThat(columns("AweEmlSrv", server)).startsWith("587", "STARTTLS", "2", SENDER, REGISTRATION);
    assertThat(columns("HISAweEmlSrv", server + " AND HISact = 'I'")).startsWith("587", "STARTTLS", "2", SENDER, REGISTRATION);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("EmlSrvUpd stores and audits the port, the security mode and the OAuth2 registration")
  void updateStoresPortSecurityAndOAuth2Registration() throws Exception {
    assertOk(maintain("EmlSrvUpd", oauth2(panel(SERVER, "", 0), 465, "SSL")));

    assertThat(columns("AweEmlSrv", "IdeAweEmlSrv = " + serverId))
      .containsExactly("465", "SSL", "2", SENDER, REGISTRATION, STORED_PASSWORD);
    // The update audits the values it writes, without the identifier of the server
    assertThat(columns("HISAweEmlSrv", "SrvNam = '" + SERVER + "' AND HISact = 'U'"))
      .startsWith("465", "SSL", "2", SENDER, REGISTRATION);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("EmlSrvUpd stores an empty port as no port, so the security mode gives the default one")
  void updateStoresAnEmptyPortAsNoPort() throws Exception {
    ObjectNode parameters = panel(SERVER, "", 0);
    parameters.put("Prt", "");
    parameters.put("SecMod", "NONE");

    assertOk(maintain("EmlSrvUpd", parameters));

    assertThat(columns("AweEmlSrv", "IdeAweEmlSrv = " + serverId)).startsWith(null, "NONE", "1");
  }

  /**
   * An OAuth2 server of the panel: the sender mailbox as user, the client registration, the port and the security mode
   */
  private ObjectNode oauth2(ObjectNode parameters, int port, String securityMode) {
    parameters.put("Prt", port);
    parameters.put("SecMod", securityMode);
    parameters.put("Ath", 2);
    parameters.put("EmlUsr", SENDER);
    parameters.put("OauReg", REGISTRATION);
    return parameters;
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

  /**
   * Port, security mode, authentication, user, OAuth2 registration and password of the only server row of a table
   */
  private List<String> columns(String table, String where) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(
           "SELECT Prt, SecMod, Ath, EmlUsr, OauReg, EmlPwd FROM " + table + " WHERE " + where);
         ResultSet result = statement.executeQuery()) {
      assertThat(result.next()).as("%s %s", table, where).isTrue();
      List<String> values = new ArrayList<>();
      for (int column = 1; column <= 6; column++) {
        String value = result.getString(column);
        values.add(value == null ? null : value.trim());
      }
      assertThat(result.next()).as("one row of %s %s", table, where).isFalse();
      return values;
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
