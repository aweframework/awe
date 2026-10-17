package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
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
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security audit of the account changes made through the generic maintains and of the REST tokens
 */
@Tag("integration")
@DisplayName("Security audit account tests")
class SecurityAuditAccountTest extends AbstractSpringAppIntegrationTest {

  private static final String USER = "audit-adm";

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;
  private int userId;

  @BeforeEach
  void createUser() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    support.deleteUser(USER);
    userId = support.copyTestUser(USER);
  }

  @AfterEach
  void deleteUser() throws Exception {
    support.deleteUser(USER);
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("An administrator locking and unlocking a user is audited, and unlocking clears the failed logins")
  void lockAndUnlockByAdministrator() throws Exception {
    Instant start = Instant.now().minusSeconds(1);
    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
      statement.executeUpdate("UPDATE ope SET NumLog = 2 WHERE l1_nom = '" + USER + "'");
    }

    updateUser(1).andExpect(status().isOk());
    assertThat(support.lockStatus(USER)[0]).isEqualTo(1);
    updateUser(0).andExpect(status().isOk());

    assertThat(support.lockStatus(USER)).containsExactly(0, 0);
    assertThat(support.events(USER, start)).extracting(event -> event.substring(0, event.lastIndexOf('/')))
      .containsExactly("ACCOUNT_LOCKED/SUCCESS/ADMINISTRATOR", "ACCOUNT_UNLOCKED/SUCCESS/ADMINISTRATOR");
  }

  @Test
  @WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
  @DisplayName("Resetting the second factor of users is audited per user")
  void secondFactorResetByAdministrator() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    maintain("reset2fa", "{\"IdeOpe\":[" + userId + "],\"max\":30}").andExpect(status().isOk());

    assertThat(support.events(USER, start)).extracting(event -> event.substring(0, event.lastIndexOf('/')))
      .containsExactly("TOTP_RESET/SUCCESS/ADMINISTRATOR");
  }

  @Test
  @DisplayName("A password change with a wrong current password is audited as a failure and changes nothing")
  void passwordChangeWithWrongCurrentPassword() throws Exception {
    Instant start = Instant.now().minusSeconds(1);
    Object storedPassword = support.user(USER).get("L1_PAS");

    maintain("ChdPwdPub", "{\"Usr\":\"" + USER + "\",\"OldPas\":\"wrong\",\"Pas\":\"newpass1\",\"max\":30}");

    assertThat(support.user(USER).get("L1_PAS")).isEqualTo(storedPassword);
    assertThat(support.events(USER, start)).extracting(event -> event.substring(0, event.lastIndexOf('/')))
      .containsExactly("PASSWORD_CHANGE/FAILURE/BAD_CREDENTIALS");
  }

  @Test
  @DisplayName("A REST login issues a token and a wrong token is rejected")
  void restTokens() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    mockMvc.perform(post("/api/authenticate")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"" + USER + "\",\"password\":\"test\"}"))
      .andExpect(status().isOk());
    mockMvc.perform(post("/api/data/SimpleGetAll")
      .header("Authorization", "Bearer not-a-valid-token")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{}")).andReturn();

    assertThat(support.events(USER, start)).containsExactly("LOGIN/SUCCESS/null/REST", "TOKEN_ISSUED/SUCCESS/null/REST");
    assertThat(support.count("TOKEN_REJECTED", "INVALID_TOKEN", start)).isPositive();
  }

  @Test
  @DisplayName("Opening an option restricted for the user profile is audited as a denied access")
  void restrictedOptionIsDenied() throws Exception {
    String operator = "audit-ope";
    support.deleteUser(operator);
    support.copyTestUser(operator);
    try {
      try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
        // Operator profile: the settings menu (and the security audit option) is restricted
        statement.executeUpdate("UPDATE ope SET IdePro = 3, Res = 'operator' WHERE l1_nom = '" + operator + "'");
      }
      Instant start = Instant.now().minusSeconds(1);
      org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
      mockMvc.perform(post("/action/login")
        .with(csrf())
        .session(session)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"cod_usr\":\"" + operator + "\",\"pwd_usr\":\"test\"}")
        .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

      mockMvc.perform(post("/screen-data/security-audit").with(csrf()).session(session)
        .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

      assertThat(support.events(operator, start)).contains("ACCESS_DENIED/FAILURE/RESTRICTED/WEB");
    } finally {
      support.deleteUser(operator);
    }
  }

  private org.springframework.test.web.servlet.ResultActions updateUser(int locked) throws Exception {
    Map<String, Object> user = support.user(USER);
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("IdeOpe", userId);
    parameters.put("Sta", toInt(user.get("L1_ACT")));
    parameters.put("WebPrn", string(user.get("WEBPRN")));
    parameters.put("PcPrn", string(user.get("PCPRN")));
    parameters.put("Eml", string(user.get("EMLADR")));
    parameters.put("EmlSrv", string(user.get("EMLSRV")));
    parameters.put("Lan", string(user.get("L1_LAN")));
    parameters.put("Nam", string(user.get("OPENAM")));
    parameters.put("Pro", toInt(user.get("IDEPRO")));
    parameters.put("Thm", toInt(user.get("IDETHM")));
    parameters.put("ScrIni", string(user.get("SCRINI")));
    parameters.put("Res", string(user.get("RES")));
    parameters.put("PwdLck", locked);
    parameters.put("FlgPwdUpd", 0);
    parameters.put("max", 30);
    return maintain("UsrUpd", objectMapper.writeValueAsString(parameters));
  }

  private org.springframework.test.web.servlet.ResultActions maintain(String target, String body) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
      .with(csrf())
      .contentType(MediaType.APPLICATION_JSON)
      .content(body)
      .accept(MediaType.APPLICATION_JSON));
  }

  private static String string(Object value) {
    return value == null ? null : value.toString().trim();
  }

  private static Integer toInt(Object value) {
    return value == null ? null : Integer.valueOf(value.toString().trim());
  }
}
