package com.almis.awe.test.integration.security;

import com.almis.awe.config.PasswordEncoderType;
import com.almis.awe.security.authentication.encoder.AwePasswordEncoder;
import com.almis.awe.service.EncodeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password change and user information definitions must not be usable without a session, must only
 * change the password of the signed-in user, and must never return credentials.
 * Hash values are compared with {@code isTrue()/isFalse()} so that a failure never prints them.
 * The current password is checked in Java (the hashes are salted): a wrong one updates nothing and answers like a right one.
 */
@Tag("integration")
@DisplayName("User credential definitions tests")
class UserCredentialDefinitionsTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String OTHER_USER = "donald";
  private static final String OLD_PASSWORD = "old-password-852";
  private static final String NEW_PASSWORD = "new-password-852";
  private static final AwePasswordEncoder PASSWORD_ENCODER = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);

  @Autowired
  private DataSource dataSource;

  private StoredCredentials signedInUserCredentials;
  private StoredCredentials otherUserCredentials;
  private int otherUserActive;

  @BeforeEach
  void storeCredentials() throws Exception {
    signedInUserCredentials = readCredentials(SIGNED_IN_USER);
    otherUserCredentials = readCredentials(OTHER_USER);
    otherUserActive = readActive(OTHER_USER);
  }

  @AfterEach
  void restoreCredentials() throws Exception {
    writeCredentials(SIGNED_IN_USER, signedInUserCredentials);
    writeCredentials(OTHER_USER, otherUserCredentials);
    writeActive(OTHER_USER, otherUserActive);
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
    assertThat(readCredentials(SIGNED_IN_USER).keepsTheOldHash(signedInUserCredentials)).isTrue();
    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void anonymousExpiredPasswordUpdateIsRejectedAndKeepsThePassword() throws Exception {
    launchMaintain("ChdPwdExpiredUpdate", null, "{\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isUnauthorized());

    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void signedInUserCannotDriveTheExpiredPasswordUpdateOfAnotherUser() throws Exception {
    // The update of an expired password is only meant for the server-side change request: the user is never a parameter
    launchMaintain("ChdPwdExpiredUpdate", signedInSession(), "{\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}");

    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
    assertThat(readCredentials(SIGNED_IN_USER).sameAs(signedInUserCredentials)).isTrue();
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
    StoredCredentials before = readCredentials(OTHER_USER);

    launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"OldPas\":\"" + OLD_PASSWORD + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk());

    assertThat(readCredentials(OTHER_USER).hasPassword(NEW_PASSWORD)).isTrue();
    assertThat(readCredentials(OTHER_USER).keepsTheOldHash(before)).isTrue();
    assertThat(readCredentials(SIGNED_IN_USER).sameAs(signedInUserCredentials)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void publicChangePasswordAnswersTheSameWithAWrongAndARightCurrentPassword() throws Exception {
    setPassword(OTHER_USER, OLD_PASSWORD);

    String wrong = launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"OldPas\":\"wrong-password\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("ok")))
      .andReturn().getResponse().getContentAsString();
    String right = launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"OldPas\":\"" + OLD_PASSWORD + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("ok")))
      .andReturn().getResponse().getContentAsString();

    assertThat(messages(wrong)).isEqualTo(messages(right));
    assertThat(readCredentials(OTHER_USER).hasPassword(NEW_PASSWORD)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void publicChangePasswordOfAnInactiveUserKeepsThePassword() throws Exception {
    setPassword(OTHER_USER, OLD_PASSWORD);
    writeActive(OTHER_USER, 0);
    StoredCredentials before = readCredentials(OTHER_USER);

    launchMaintain("ChdPwdPub", null, "{\"Usr\":\"" + OTHER_USER + "\",\"OldPas\":\"" + OLD_PASSWORD + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk());

    assertThat(readCredentials(OTHER_USER).sameAs(before)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void anonymousPasswordUpdatesAreRejected() throws Exception {
    for (String target : new String[]{"ChdPwdPubUpdate", "UsrUpdPwdUpdate", "UsrUpdPwd"}) {
      launchMaintain(target, null, "{\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
        .andExpect(status().isUnauthorized());
    }

    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void signedInUserCannotDriveTheVerifiedPasswordUpdates() throws Exception {
    // The updates are only meant for the server once it has verified the current password: the user is never a parameter
    MockHttpSession session = signedInSession();
    for (String target : new String[]{"ChdPwdPubUpdate", "UsrUpdPwdUpdate"}) {
      launchMaintain(target, session, "{\"Usr\":\"" + OTHER_USER + "\",\"user\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}");
    }

    assertThat(readCredentials(OTHER_USER).sameAs(otherUserCredentials)).isTrue();
    assertThat(readCredentials(SIGNED_IN_USER).sameAs(signedInUserCredentials)).isTrue();
  }

  @Test
  @WithAnonymousUser
  void anonymousRehashIsRejectedAndKeepsEveryPassword() throws Exception {
    Map<String, String> before = readAllPasswords();

    launchMaintain("UsrRehashPwd", null, rehashRequest())
      .andExpect(status().isUnauthorized());

    assertThat(readAllPasswords().equals(before)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void signedInUserCannotDriveTheRehashOfAnyUser() throws Exception {
    // The rehash target reads the user and the hash only from session parameters that the server sets while it runs
    Map<String, String> before = readAllPasswords();

    launchMaintain("UsrRehashPwd", signedInSession(), rehashRequest());

    assertThat(readAllPasswords().equals(before)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void settingsPasswordChangeWithAWrongCurrentPasswordKeepsThePassword() throws Exception {
    setPassword(SIGNED_IN_USER, OLD_PASSWORD);
    StoredCredentials before = readCredentials(SIGNED_IN_USER);

    launchMaintain("UsrUpdPwd", signedInSession(), "{\"OldPas\":\"wrong-password\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("ok")));

    assertThat(readCredentials(SIGNED_IN_USER).sameAs(before)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void settingsPasswordChangeWithTheCurrentPasswordChangesOnlyTheSessionUser() throws Exception {
    setPassword(SIGNED_IN_USER, OLD_PASSWORD);
    setPassword(OTHER_USER, OLD_PASSWORD);
    StoredCredentials before = readCredentials(SIGNED_IN_USER);
    StoredCredentials otherBefore = readCredentials(OTHER_USER);

    // The request names another user: the session user is the one changed
    launchMaintain("UsrUpdPwd", signedInSession(), "{\"Usr\":\"" + OTHER_USER + "\",\"user\":\"" + OTHER_USER + "\",\"OldPas\":\"" + OLD_PASSWORD + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("ok")));

    assertThat(readCredentials(SIGNED_IN_USER).hasPassword(NEW_PASSWORD)).isTrue();
    assertThat(readCredentials(SIGNED_IN_USER).keepsTheOldHash(before)).isTrue();
    assertThat(readCredentials(OTHER_USER).sameAs(otherBefore)).isTrue();
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void settingsPasswordChangeLogsTheStatementWithoutTheHashNorThePassword(CapturedOutput output) throws Exception {
    setPassword(SIGNED_IN_USER, OLD_PASSWORD);

    launchMaintain("UsrUpdPwd", signedInSession(), "{\"OldPas\":\"" + OLD_PASSWORD + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("ok")));

    String stored = readCredentials(SIGNED_IN_USER).password();
    assertThat(readCredentials(SIGNED_IN_USER).hasPassword(NEW_PASSWORD)).isTrue();
    assertThat(output.getOut()).contains("update ope", "'********'");
    assertThat(output.getOut().contains(stored.trim())).isFalse();
    assertThat(output.getOut().contains("{bcrypt}")).isFalse();
    assertThat(output.getOut().contains(NEW_PASSWORD)).isFalse();
    assertThat(output.getOut().contains(OLD_PASSWORD)).isFalse();
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

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void userSettingsDataDoesNotReturnThePasswordHash() throws Exception {
    StoredCredentials credentials = readCredentials(SIGNED_IN_USER);
    String result = launchQuery("getUserData", signedInSession())
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    assertThat(result.contains("oldPassword")).isFalse();
    assertThat(result.contains(credentials.password().trim())).isFalse();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void encryptUtilReturnsAPasswordHashOfTheConfiguredAlgorithm() throws Exception {
    String result = mockMvc.perform(post("/action/data/GetPwdHas")
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"CrtTxt\":\"" + NEW_PASSWORD + "\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    String hash = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows[0].value").toString()
      .replaceAll("^\\[\"|\"]$", "").replace("\\/", "/");
    assertThat(hash.startsWith("{bcrypt}")).isTrue();
    assertThat(PASSWORD_ENCODER.matches(NEW_PASSWORD, hash)).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void encryptUtilReturnsTheAwe4HashWithTheLegacyAlgorithm() throws Exception {
    String result = mockMvc.perform(post("/action/data/GetPwdHas")
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"CrtTxt\":\"" + NEW_PASSWORD + "\",\"CrtPwdAlg\":\"ripemd160\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    String hash = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows[0].value").toString()
      .replaceAll("^\\[\"|\"]$", "");
    assertThat(hash.equals(EncodeService.encodeRipEmd160(NEW_PASSWORD))).isTrue();
  }

  @Test
  @WithMockUser(username = SIGNED_IN_USER, password = SIGNED_IN_USER, roles = {"ADMIN", "USER"})
  void encryptUtilListsTheConfiguredPasswordAlgorithmFirst() throws Exception {
    String result = launchQuery("GetPwdAlg", signedInSession())
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    List<String> values = JsonPath.read(result, "$[?(@.type == 'fill')].parameters.datalist.rows[*].value");
    assertThat(values).containsExactly("bcrypt", "ripemd160");
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

  /**
   * Request that names another user and a known hash in every parameter name the rehash target could read
   */
  private static String rehashRequest() {
    String hash = PASSWORD_ENCODER.encode(NEW_PASSWORD);
    return "{\"l1_nom\":\"" + OTHER_USER + "\",\"l1_pas\":\"" + hash + "\",\"passwordRehashUser\":\"" + OTHER_USER
      + "\",\"passwordRehashValue\":\"" + hash + "\",\"Usr\":\"" + OTHER_USER + "\",\"Pas\":\"" + NEW_PASSWORD + "\"}";
  }

  /**
   * Stored password hash of every user, by user name
   */
  private Map<String, String> readAllPasswords() throws Exception {
    Map<String, String> passwords = new HashMap<>();
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT l1_nom, l1_pas FROM ope");
         ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        passwords.put(resultSet.getString(1).trim(), String.valueOf(resultSet.getString(2)));
      }
    }
    assertThat(passwords.containsKey(OTHER_USER)).isTrue();
    return passwords;
  }

  private int readActive(String user) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT l1_act FROM ope WHERE l1_nom = ?")) {
      statement.setString(1, user);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertThat(resultSet.next()).isTrue();
        return resultSet.getInt(1);
      }
    }
  }

  private void writeActive(String user, int active) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET l1_act = ? WHERE l1_nom = ?")) {
      statement.setInt(1, active);
      statement.setString(2, user);
      statement.executeUpdate();
    }
  }

  /**
   * Message actions of a maintain answer (type, title and message)
   */
  private static Object messages(String response) {
    return JsonPath.read(response, "$[?(@.type=='message')].parameters['type','title','message']");
  }

  /**
   * Store a password with the legacy RIPEMD-160 hash: it keeps validating as the current password
   */
  private void setPassword(String user, String password) throws Exception {
    StoredCredentials current = readCredentials(user);
    writeCredentials(user, new StoredCredentials(EncodeService.encodeRipEmd160(password), current.newPassword(), current.changeDate()));
  }

  private record StoredCredentials(String password, String newPassword, Timestamp changeDate) {
    boolean sameAs(StoredCredentials other) {
      return String.valueOf(password).equals(String.valueOf(other.password))
        && String.valueOf(newPassword).equals(String.valueOf(other.newPassword));
    }

    /**
     * The password is stored with BCrypt, the configured encoder
     */
    boolean hasPassword(String plainPassword) {
      return password != null && password.startsWith("{bcrypt}") && PASSWORD_ENCODER.matches(plainPassword, password);
    }

    /**
     * The old unsalted copy of the password (OpePas) is not written anymore
     */
    boolean keepsTheOldHash(StoredCredentials other) {
      return String.valueOf(newPassword).equals(String.valueOf(other.newPassword));
    }
  }
}
