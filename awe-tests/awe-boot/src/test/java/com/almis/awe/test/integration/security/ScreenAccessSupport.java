package com.almis.awe.test.integration.security;

import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Helpers of the screen access integration tests
 */
abstract class ScreenAccessSupport extends AbstractSpringAppIntegrationTest {

  static final String REFUSED = "You do not have permission to perform this action. Contact your administrator if you need it.";
  static final String REFUSED_TITLE = "Access denied";

  @Autowired
  private JWTTokenService jwtTokenService;

  /**
   * The answer of a refused client action is the generic refusal, which never names the refused target
   *
   * @param response Answer of the action
   * @param target   Refused target
   */
  static void assertRefused(String response, String target) {
    assertThat(JsonPath.<List<String>>read(response, "$[?(@.type == 'message')].parameters.title"))
      .as(response).containsExactly(REFUSED_TITLE);
    assertThat(JsonPath.<List<String>>read(response, "$[?(@.type == 'message')].parameters.message"))
      .as(response).singleElement().asString().isEqualTo(REFUSED).doesNotContain(target);
  }

  String actionMaintain(String target, String restriction) throws Exception {
    return mockMvc.perform(post("/action/maintain/" + target)
        .with(csrf())
        .session(session(restriction))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}")
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  String actionData(String query, String restriction) throws Exception {
    return actionData(query, restriction, "{\"max\":30}");
  }

  String actionData(String query, String restriction, String content) throws Exception {
    return mockMvc.perform(post("/action/data/" + query)
        .with(csrf())
        .session(session(restriction))
        .contentType(MediaType.APPLICATION_JSON)
        .content(content)
        .accept(MediaType.APPLICATION_JSON))
      .andReturn().getResponse().getContentAsString();
  }

  MockHttpSession session(String restriction) throws Exception {
    MockHttpSession session = new MockHttpSession();
    for (String[] parameter : new String[][]{{"user", "test"}, {"restriction", restriction}}) {
      mockMvc.perform(post("/session/set/" + parameter[0])
          .with(csrf())
          .param("value", parameter[1])
          .session(session))
        .andReturn();
    }
    return session;
  }

  String token(String user) {
    return JWT.create()
      .withSubject(user)
      .withExpiresAt(new Date(System.currentTimeMillis() + 60000))
      .withIssuer("AWE ISSUER")
      .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
  }
}
