package com.almis.awe.test.integration.security;

import com.almis.awe.config.PasswordEncoderType;
import com.almis.awe.security.authentication.encoder.AwePasswordEncoder;
import com.almis.awe.service.EncodeService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
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
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A legacy RIPEMD-160 password hash keeps validating on the real login, and it is rehashed with BCrypt (the configured
 * encoder) after a successful login. Hash values are compared with {@code isTrue()/isFalse()} so that a failure never
 * prints them.
 */
@Tag("integration")
@WithAnonymousUser
@DisplayName("Password rehash on login tests")
class PasswordRehashLoginTest extends AbstractSpringAppIntegrationTest {

  // Throw-away copy of the user test, deleted after each test
  private static final String USER = "rehash-usr";
  private static final String PASSWORD = "legacy-password-778";
  private static final AwePasswordEncoder PASSWORD_ENCODER = new AwePasswordEncoder(PasswordEncoderType.BCRYPT);

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;

  @BeforeEach
  void createUser() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    support.deleteUser(USER);
    support.copyTestUser(USER);
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET Enable2fa = 0, Secret2fa = NULL WHERE l1_nom = ?")) {
      statement.setString(1, USER);
      statement.executeUpdate();
    }
  }

  @AfterEach
  void deleteUser() throws Exception {
    support.deleteUser(USER);
  }

  @ParameterizedTest(name = "upper case hash: {0}")
  @ValueSource(booleans = {false, true})
  @DisplayName("A legacy hash signs in, is rehashed with BCrypt, and a second login keeps the new hash")
  void legacyHash_isRehashedOnTheFirstSuccessfulLogin(boolean upperCase) throws Exception {
    String legacyHash = EncodeService.encodeRipEmd160(PASSWORD);
    storePassword(upperCase ? legacyHash.toUpperCase(Locale.ROOT) : legacyHash);

    login(PASSWORD).andExpect(jsonPath("$[1].parameters.token", is(notNullValue())));

    String rehashed = storedPassword();
    assertThat(rehashed.startsWith("{bcrypt}")).isTrue();
    assertThat(PASSWORD_ENCODER.matches(PASSWORD, rehashed)).isTrue();

    login(PASSWORD).andExpect(jsonPath("$[1].parameters.token", is(notNullValue())));

    assertThat(storedPassword().equals(rehashed)).isTrue();
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  @DisplayName("The rehash statement is logged with the new hash masked")
  void rehash_logsTheStatementWithoutTheHash(CapturedOutput output) throws Exception {
    String legacyHash = EncodeService.encodeRipEmd160(PASSWORD);
    storePassword(legacyHash);

    login(PASSWORD).andExpect(jsonPath("$[1].parameters.token", is(notNullValue())));

    String rehashed = storedPassword();
    assertThat(rehashed.startsWith("{bcrypt}")).isTrue();
    assertThat(output.getOut()).contains("UsrRehashPwd", "'********'");
    assertThat(output.getOut().contains(rehashed)).isFalse();
    assertThat(output.getOut().contains("{bcrypt}")).isFalse();
    assertThat(output.getOut().contains(legacyHash)).isFalse();
    assertThat(output.getOut().contains(PASSWORD)).isFalse();
  }

  @Test
  @DisplayName("A failed login keeps the legacy hash")
  void wrongPassword_keepsTheLegacyHash() throws Exception {
    String legacyHash = EncodeService.encodeRipEmd160(PASSWORD);
    storePassword(legacyHash);

    login("wrong-password")
      .andExpect(jsonPath("$[?(@.type=='message')].parameters.title", hasItem("Invalid credentials")));

    assertThat(storedPassword().equals(legacyHash)).isTrue();
  }

  private ResultActions login(String password) throws Exception {
    return mockMvc.perform(post("/action/login")
        .with(csrf())
        .session(new MockHttpSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"cod_usr\":\"" + USER + "\",\"pwd_usr\":\"" + password + "\"}")
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private void storePassword(String hash) throws Exception {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement("UPDATE ope SET l1_pas = ? WHERE l1_nom = ?")) {
      statement.setString(1, hash);
      statement.setString(2, USER);
      statement.executeUpdate();
    }
  }

  private String storedPassword() throws Exception {
    return String.valueOf(support.user(USER).get("L1_PAS")).trim();
  }
}
