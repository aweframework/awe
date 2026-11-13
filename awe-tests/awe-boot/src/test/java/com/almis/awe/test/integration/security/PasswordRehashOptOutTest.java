package com.almis.awe.test.integration.security;

import com.almis.awe.service.EncodeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With the RIPEMD-160 opt-out ({@code awe.security.password-encoder=ripemd160}) a legacy hash signs in and is not
 * rehashed
 */
@Tag("integration")
@WithAnonymousUser
@TestPropertySource(properties = "awe.security.password-encoder=ripemd160")
@DisplayName("Password rehash opt-out tests")
class PasswordRehashOptOutTest extends AbstractSpringAppIntegrationTest {

  // Throw-away copy of the user test, deleted after each test
  private static final String USER = "rehash-off";
  private static final String PASSWORD = "legacy-password-778";

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
  @DisplayName("A legacy hash signs in and keeps the legacy hash")
  void legacyHash_isNotRehashed() throws Exception {
    String legacyHash = EncodeService.encodeRipEmd160(PASSWORD);
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET l1_pas = ?, Enable2fa = 0, Secret2fa = NULL WHERE l1_nom = ?")) {
      statement.setString(1, legacyHash);
      statement.setString(2, USER);
      statement.executeUpdate();
    }

    mockMvc.perform(post("/action/login")
        .with(csrf())
        .session(new MockHttpSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"cod_usr\":\"" + USER + "\",\"pwd_usr\":\"" + PASSWORD + "\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[1].parameters.token", is(notNullValue())));

    assertThat(String.valueOf(support.user(USER).get("L1_PAS")).trim().equals(legacyHash)).isTrue();
  }
}
