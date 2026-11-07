package com.almis.awe.test.integration.security;

import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;

import javax.sql.DataSource;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The login queries that only the server launches are server-only: a signed-in client that asks for the login data of
 * another user through /action/data is refused and gets neither the password hash nor the 2FA secret. The same holds
 * for the other queries that only Java launches and return credentials or the data of other users (database and JMS
 * connections, users subscribed to a notification, password change date, login attempts, user information and mail
 * server of a user).
 */
@Tag("integration")
@DisplayName("Server-only login queries tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class ServerOnlyLoginQueriesTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String VICTIM = "server-only-query";
  private static final String VICTIM_SECRET = "SECRET";
  private static final String REFUSED_MESSAGE = "You do not have permission to perform this action. Contact your administrator if you need it.";

  @Autowired
  private DataSource dataSource;

  private SecurityAuditTestSupport support;

  @BeforeEach
  void createVictim() throws Exception {
    support = new SecurityAuditTestSupport(dataSource);
    support.deleteUser(VICTIM);
    support.copyTestUser(VICTIM);
  }

  @AfterEach
  void deleteVictim() throws Exception {
    support.deleteUser(VICTIM);
  }

  static Stream<Arguments> forgedReads() {
    return Stream.of(
      Arguments.of("userDetails", "{\"user\":\"" + VICTIM + "\",\"max\":30}"),
      Arguments.of("userDetailsByEmail",
        "{\"email\":\"" + VICTIM + "@test.com\",\"user\":\"" + VICTIM + "\",\"max\":30}"));
  }

  @ParameterizedTest(name = "{0} through /action/data")
  @MethodSource("forgedReads")
  @DisplayName("A signed-in user cannot read the login data of another user through /action/data")
  void actionDataIsRefused(String query, String body) throws Exception {
    String response = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();

    assertThat(response)
      .contains(REFUSED_MESSAGE)
      .doesNotContain("\"password\"")
      .doesNotContain("secret2fa")
      .doesNotContain(VICTIM_SECRET);
  }

  static Stream<Arguments> forgedServerReads() {
    return Stream.of(
      Arguments.of("DatabaseConnections", "{\"max\":30}", "\"password\""),
      Arguments.of("JmsConnections", "{\"max\":30}", "JmsPwd"),
      Arguments.of("get-interested-users", "{\"subscription\":\"" + VICTIM + "\",\"max\":30}", "\"byEmail\""),
      Arguments.of("GetChgPwdUsr", "{\"Usr\":\"" + VICTIM + "\",\"max\":30}", "l1_psd"),
      Arguments.of("AutUsr", "{\"Usr\":\"" + VICTIM + "\",\"max\":30}", "NumLog"),
      Arguments.of("UsrInf", "{\"Usr\":\"" + VICTIM + "\",\"max\":30}", "UsrRes"),
      Arguments.of("GetEmlSrvByOpe", "{\"Usr\":\"" + VICTIM + "\",\"max\":30}", "EmlPwd"));
  }

  @ParameterizedTest(name = "{0} through /action/data")
  @MethodSource("forgedServerReads")
  @DisplayName("A signed-in user cannot launch the other server-only queries through /action/data")
  void serverQueryIsRefused(String query, String body, String leakedField) throws Exception {
    String response = mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();

    assertThat(response)
      .contains(REFUSED_MESSAGE)
      .doesNotContain(leakedField)
      .doesNotContain("\"rows\"");
  }

  @Test
  @DisplayName("The unique user name check of the users screen does not return the password hash")
  void uniqueUserCheckDoesNotReturnThePassword() throws Exception {
    String response = mockMvc.perform(post("/action/data/UsrChkUni")
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"Usr\":\"" + VICTIM + "\",\"max\":30}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();

    assertThat(response)
      .contains(VICTIM)
      .doesNotContain("\"Pwd\"")
      .doesNotContain("5e52fee4");
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
}
