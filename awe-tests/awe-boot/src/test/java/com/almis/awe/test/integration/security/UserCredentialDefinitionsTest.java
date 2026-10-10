package com.almis.awe.test.integration.security;

import com.almis.awe.service.EncodeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password change and user information definitions must not be usable without a session, must only
 * change the password of the signed-in user, and must never return credentials.
 * Hash values are compared with {@code isTrue()/isFalse()} so that a failure never prints them.
 */
@Tag("integration")
@DisplayName("User credential definitions tests")
class UserCredentialDefinitionsTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String OTHER_USER = "donald";
  private static final String OLD_PASSWORD = "old-password-852";
  private static final String NEW_PASSWORD = "new-password-852";

  @Autowired
  private DataSource dataSource;

  private StoredCredentials signedInUserCredentials;
  private StoredCredentials otherUserCredentials;

  @BeforeEach
  void storeCredentials() throws Exception {
    signedInUserCredentials = readCredentials(SIGNED_IN_USER);
    otherUserCredentials = readCredentials(OTHER_USER);
  }

  @AfterEach
  void restoreCredentials() throws Exception {
    writeCredentials(SIGNED_IN_USER, signedInUserCredentials);
    writeCredentials(OTHER_USER, otherUserCredentials);
  }

  @Test
  @WithAnonymousUser
  void anonymousChangePasswordIsRejectedAndKeepsThePassword() throws Exception {
    launchMaintain("ChdPwd", null, "{\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isUnauthorized());

    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void signedInUserChangesOnlyTheirOwnPassword() throws Exception {
    MockHttpSession session = signedInSession();

    // The request names another user: the session user must be the one changed
    launchMaintain("ChdPwd", session, "{\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk());

    assertThat(readCredentials(SIGNED_IN_USER).hasPassword(NEW_PASSWORD)).isTrue();
    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void publicChangePasswordWithAWrongCurrentPasswordKeepsThePassword() throws Exception {
    setPassword(OTHER_USER, OLD_PASSWORD);
    StoredCredentials before = readCredentials(OTHER_USER);

    launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"OldPas\":\"wrong-password\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk());
    launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk());

    assertThat(readCredentials(OTHER_USER).sameAs(before)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void publicChangePasswordWithTheCurrentPasswordChangesIt() throws Exception {
    setPassword(OTHER_USER, OLD_PASSWORD);

    launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"OldPas\":\"" + OLD_PASSWORD + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk());

    assertThat(readCredentials(OTHER_USER).hasPassword(NEW_PASSWORD)).isTrue();
    assertThat(readCredentials(SIGNED_IN_USER).sameAs(signedInUserCredentials)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"UsrInf", "AutUsr", "GetEmlSrvByOpe", "JmsConnections"})
  @WithAnonymousUser
  void anonymousCredentialQueriesAreRejected(String queryName) throws Exception {
    launchQuery(queryName, null)
      .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"UpdCntLog", "UpdPwdLck"})
  @WithAnonymousUser
  void anonymousUserSecurityUpdatesAreRejected(String targetName) throws Exception {
    launchMaintain(targetName, null, "{\"Usr\":\"" + OTHER_USER + "\",\"UpdCntLog\":0,\"PwdLck\":0}")
      .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(strings = {"UsrInf", "AutUsr"})
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userInformationQueriesDoNotReturnPasswords(String queryName) throws Exception {
    String result = launchQuery(queryName, signedInSession())
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(result.contains("\"" + SIGNED_IN_USER + "\"")).isTrue();
    assertThat(result.contains(String.valueOf(signedInUserCredentials.password()))).isFalse();
    if (signedInUserCredentials.newPassword() != null) {
      assertThat(result.contains(signedInUserCredentials.newPassword())).isFalse();
    }
    assertThat(result.contains("\"Pwd\"") || result.contains("\"NewPwd\"")).isFalse();
  }

  private ResultActions launchMaintain(String target, MockHttpSession session, String content) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
      .with(csrf())
      .session(session == null ? new MockHttpSession() : session)
      .contentType(MediaType.APPLICATION_JSON)
      .content(content)
      .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions launchQuery(String queryName, MockHttpSession session) throws Exception {
    return mockMvc.perform(post("/action/data/" + queryName)
      .with(csrf())
      .session(session == null ? new MockHttpSession() : session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"Usr\":\"" + SIGNED_IN_USER + "\"}")
      .accept(MediaType.APPLICATION_JSON));
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

  private StoredCredentials readCredentials(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT l1_pas, OpePas, l1_psd FROM ope WHERE l1_nom = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return new StoredCredentials(resultSet.getString(1), resultSet.getString(2), resultSet.getTimestamp(3));
      }
    }
  }

  private void writeCredentials(String user, StoredCredentials credentials) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET l1_pas = ?, OpePas = ?, l1_psd = ? WHERE l1_nom = ?")) {
      statement.setString(1, credentials.password());
      statement.setString(2, credentials.newPassword());
      statement.setTimestamp(3, credentials.changeDate());
      statement.setString(4, user);
      statement.executeUpdate();
    }
  }

  private void setPassword(String user, String password) throws Exception {
    StoredCredentials current = readCredentials(user);
    writeCredentials(user, new StoredCredentials(EncodeService.encodeRipEmd160(password), current.newPassword(), current.changeDate()));
  }

  private record StoredCredentials(String password, String newPassword, Timestamp changeDate) {
    boolean sameAs(StoredCredentials other) {
      return String.valueOf(password).equals(String.valueOf(other.password))
        && String.valueOf(newPassword).equals(String.valueOf(other.newPassword));
    }

    boolean hasPassword(String plainPassword) {
      return EncodeService.encodeRipEmd160(plainPassword).equalsIgnoreCase(password);
    }
  }
}
