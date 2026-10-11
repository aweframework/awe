package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringFixedEnvironmentIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Local login of a user whose password has expired (the test data sets PwdExp and these users never changed the
 * password): the user is guided to change it without getting an authenticated session.
 */
@Tag("integration")
@WithAnonymousUser
@DisplayName("Expired password change tests")
class ExpiredPasswordChangeTest extends AbstractSpringFixedEnvironmentIT {

  // The user whose password the tests change: the test database is shared with other tests, so it is restored
  private static final String CHANGED_USER = "juanito";

  @Autowired
  private DataSource dataSource;

  private String originalPassword;
  private String originalOpePassword;
  private Timestamp originalPasswordDate;

  @BeforeEach
  void keepTheUserToChange() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT l1_pas, OpePas, l1_psd FROM ope WHERE l1_nom = ?")) {
      statement.setString(1, CHANGED_USER);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertTrue(resultSet.next());
        originalPassword = resultSet.getString(1);
        originalOpePassword = resultSet.getString(2);
        originalPasswordDate = resultSet.getTimestamp(3);
      }
    }
  }

  @AfterEach
  void restoreTheUserToChange() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET l1_pas = ?, OpePas = ?, l1_psd = ? WHERE l1_nom = ?")) {
      statement.setString(1, originalPassword);
      statement.setString(2, originalOpePassword);
      statement.setTimestamp(3, originalPasswordDate);
      statement.setString(4, CHANGED_USER);
      statement.executeUpdate();
    }
  }

  private ResultActions login(MockHttpSession session, String user, String password) throws Exception {
    return mockMvc.perform(post("/action/login")
      .with(csrf())
      .session(session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"" + user + "\",\"pwd_usr\":\"" + password + "\"}")
      .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions changePassword(MockHttpSession session, String password) throws Exception {
    return mockMvc.perform(post("/action/maintain/ChdPwdExpired")
      .with(csrf())
      .session(session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"Pas\":\"" + password + "\",\"max\":30}")
      .accept(MediaType.APPLICATION_JSON));
  }

  private ResultActions privateRequest(MockHttpSession session) throws Exception {
    return mockMvc.perform(post("/action/maintain/saveUserAvatar")
      .with(csrf())
      .session(session)
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"CrtAvatar\":\"whatever\",\"max\":30}")
      .accept(MediaType.APPLICATION_JSON));
  }

  @Test
  @DisplayName("An expired password answers a localized message, guides to the change screen and grants no session")
  void expiredPasswordLogin_guidesToTheChangeScreenWithoutSession() throws Exception {
    MockHttpSession session = new MockHttpSession();

    login(session, "jorgito", "test")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.title", hasItem("Password expired")))
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.message",
        hasItem("Your password has expired. Set a new password to continue.")))
      .andExpect(jsonPath("$[?(@.type=='screen')].context", hasItem("screen")))
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")))
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.token").doesNotExist());

    // The user is not authenticated
    privateRequest(session).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("A wrong password does not reveal the expiration nor opens the change")
  void wrongPassword_isAnInvalidLoginAndAllowsNoChange() throws Exception {
    MockHttpSession session = new MockHttpSession();

    login(session, "jorgito", "wrong")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.title", hasItem("Invalid credentials")))
      .andExpect(jsonPath("$[?(@.type=='screen')]").isEmpty());

    changePassword(session, "newpass1")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("warning")));
    login(new MockHttpSession(), "jorgito", "newpass1")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.title", hasItem("Invalid credentials")));
  }

  @Test
  @DisplayName("The password is changed after verifying the expired one, and then the regular login works")
  void changeExpiredPassword_thenRegularLogin() throws Exception {
    MockHttpSession session = new MockHttpSession();
    login(session, CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));

    changePassword(session, "newpass1")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("ok")))
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.message",
        hasItem("Password changed. Sign in with the new password.")))
      .andExpect(jsonPath("$[?(@.type=='screen')].target", hasItem("/")));

    // Still no session, and the request cannot be used twice
    privateRequest(session).andExpect(status().isUnauthorized());
    changePassword(session, "another1")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("warning")));

    // The old password is not valid anymore and the new one is not expired
    login(new MockHttpSession(), CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.title", hasItem("Invalid credentials")));
    login(new MockHttpSession(), CHANGED_USER, "newpass1")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[1].parameters.token", is(notNullValue())));
  }

  @Test
  @DisplayName("The password cannot be changed without having verified the expired one in the same session")
  void changeWithoutVerifiedPassword_isRejected() throws Exception {
    MockHttpSession session = new MockHttpSession();

    changePassword(session, "newpass1")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("warning")))
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.message", not(hasItem("Password changed. Sign in with the new password."))));

    // The password of the user has not changed: it is still expired
    login(new MockHttpSession(), "jaimito", "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));
  }

  @Test
  @DisplayName("A password that breaks the policy spends the request and tells the user to sign in again")
  void policyError_spendsTheRequest() throws Exception {
    MockHttpSession session = new MockHttpSession();
    login(session, CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));

    changePassword(session, "a")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.message",
        hasItem("The new password does not meet the password policy. Sign in again to set a new password.")));

    changePassword(session, "newpass1")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("warning")));
    login(new MockHttpSession(), CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));
  }

  @Test
  @DisplayName("A pending change does not survive a successful login of another user in the same session")
  void pendingChange_isDiscardedByAnotherLogin() throws Exception {
    MockHttpSession session = new MockHttpSession();
    login(session, CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));

    login(session, "test", "test").andExpect(status().isOk());

    // The user of the session has a language that is not configured: the refusal is still localized (default language)
    changePassword(session, "newpass1")
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.type", hasItem("warning")))
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.message",
        hasItem("The password change is not available or has expired. Sign in again to change the password.")));
    // The password of the first user has not changed: it is still expired
    assertPasswordUnchanged();
  }

  @Test
  @DisplayName("A failed login discards the pending change")
  void pendingChange_isDiscardedByAFailedLogin() throws Exception {
    MockHttpSession session = new MockHttpSession();
    login(session, CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));

    login(session, "test", "wrong").andExpect(status().isOk());

    changePassword(session, "newpass1")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.message",
        hasItem("The password change is not available or has expired. Sign in again to change the password.")));
    assertPasswordUnchanged();
  }

  private void assertPasswordUnchanged() throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("SELECT l1_pas FROM ope WHERE l1_nom = ?")) {
      statement.setString(1, CHANGED_USER);
      try (ResultSet resultSet = statement.executeQuery()) {
        assertTrue(resultSet.next());
        assertEquals(originalPassword, resultSet.getString(1));
      }
    }
    login(new MockHttpSession(), CHANGED_USER, "test")
      .andExpect(jsonPath("$[?(@.type=='screen')].parameters.screen", hasItem("change-expired-password")));
  }

  @Test
  @DisplayName("The change maintain used before by anonymous users is not public anymore")
  void legacyChangePasswordMaintain_isNotPublic() throws Exception {
    mockMvc.perform(post("/action/maintain/ChdPwd")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"Usr\":\"test\",\"Pas\":\"hacked\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnauthorized());
  }
}
