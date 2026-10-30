package com.almis.awe.test.integration.security;

import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The maintain targets that only the server launches (issue #886) cannot be driven by a signed-in client: a user that
 * launches them against another user through /action/maintain or /api/maintain is refused and the row of the other user
 * does not change.
 */
@Tag("integration")
@DisplayName("Server-only user targets tests")
@WithMockUser(username = "test", password = "test", roles = {"ADMIN", "USER"})
class ServerOnlyUserTargetsTest extends AbstractSpringAppIntegrationTest {

  private static final String SIGNED_IN_USER = "test";
  private static final String VICTIM = "server-only-886";
  private static final String REFUSED_MESSAGE = "without authorization";

  @Autowired
  private DataSource dataSource;

  @Autowired
  private JWTTokenService jwtTokenService;

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

  static Stream<Arguments> forgedLaunches() {
    return Stream.of(
      Arguments.of("store2faSecret", "{\"user\":\"" + VICTIM + "\",\"secret\":\"ATTACKER-SECRET\"}"),
      Arguments.of("UpdPwdLck", "{\"Usr\":\"" + VICTIM + "\",\"PwdLck\":1}"),
      Arguments.of("UpdCntLog", "{\"Usr\":\"" + VICTIM + "\",\"UpdCntLog\":99}"),
      Arguments.of("UpdateOauthUserProfile", "{\"userName\":\"" + VICTIM + "\",\"profile\":\"ADM\"}"));
  }

  @ParameterizedTest(name = "{0} through /action/maintain")
  @MethodSource("forgedLaunches")
  @DisplayName("A signed-in user cannot launch a server-only target against another user through /action/maintain")
  void actionMaintainIsRefused(String target, String body) throws Exception {
    Map<String, Object> before = support.user(VICTIM);

    MvcResult result = mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .session(signedInSession())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .accept(MediaType.APPLICATION_JSON))
      .andReturn();

    assertThat(result.getResponse().getContentAsString()).contains(REFUSED_MESSAGE);
    assertThat(support.user(VICTIM)).isEqualTo(before);
  }

  @ParameterizedTest(name = "{0} through /api/maintain")
  @MethodSource("forgedLaunches")
  @DisplayName("A signed-in user cannot launch a server-only target against another user through /api/maintain")
  void apiMaintainIsRefused(String target, String body) throws Exception {
    Map<String, Object> before = support.user(VICTIM);

    MvcResult result = mockMvc.perform(post("/api/maintain/" + target)
        .header("Authorization", "Bearer " + token())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"parameters\":" + body + "}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn();

    assertThat(result.getResponse().getContentAsString()).contains(REFUSED_MESSAGE);
    assertThat(support.user(VICTIM)).isEqualTo(before);
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

  private String token() {
    return JWT.create()
      .withSubject(SIGNED_IN_USER)
      .withExpiresAt(new Date(System.currentTimeMillis() + 60000))
      .withIssuer("AWE ISSUER")
      .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
  }
}
