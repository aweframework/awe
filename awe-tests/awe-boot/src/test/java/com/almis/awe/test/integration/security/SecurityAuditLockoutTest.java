package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Automatic lockout after consecutive wrong passwords (awe.security.lockout.max-attempts)
 */
@Tag("integration")
@TestPropertySource(properties = "awe.security.lockout.max-attempts=2")
@DisplayName("Security audit automatic lockout tests")
class SecurityAuditLockoutTest extends AbstractSpringAppIntegrationTest {

  private static final String USER = "audit-lock";

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;

  @BeforeEach
  void createUser() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    support.deleteUser(USER);
    support.copyTestUser(USER);
  }

  @AfterEach
  void deleteUser() throws Exception {
    support.deleteUser(USER);
  }

  @Test
  @DisplayName("The failure that reaches the limit locks the account, and then even the right password is refused")
  void lockAfterMaxAttempts() throws Exception {
    Instant start = Instant.now().minusSeconds(1);

    login(USER, "wrong-1");
    assertThat(support.lockStatus(USER)).containsExactly(0, 1);
    login(USER, "wrong-2");
    assertThat(support.lockStatus(USER)).containsExactly(1, 2);
    login(USER, "test");

    assertThat(support.events(USER, start)).containsExactly(
      "LOGIN/FAILURE/BAD_CREDENTIALS/WEB",
      "LOGIN/FAILURE/BAD_CREDENTIALS/WEB",
      "ACCOUNT_LOCKED/SUCCESS/MAX_ATTEMPTS/WEB",
      "LOGIN/FAILURE/LOCKED/WEB");
  }

  @Test
  @DisplayName("A successful login resets the failed attempts")
  void successResetsTheCount() throws Exception {
    login(USER, "wrong-1");
    assertThat(support.lockStatus(USER)).containsExactly(0, 1);

    login(USER, "test");

    assertThat(support.lockStatus(USER)).containsExactly(0, 0);
  }

  private void login(String user, String password) throws Exception {
    mockMvc.perform(post("/action/login")
      .with(csrf())
      .session(new MockHttpSession())
      .contentType(MediaType.APPLICATION_JSON)
      .content("{\"cod_usr\":\"" + user + "\",\"pwd_usr\":\"" + password + "\"}")
      .accept(MediaType.APPLICATION_JSON)).andReturn();
  }
}
