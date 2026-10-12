package com.almis.awe.autoconfigure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.authentication.logout.OidcLogoutToken;
import org.springframework.security.oauth2.client.oidc.session.InMemoryOidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * AWE back-channel logout handler: ends every session of the logout token with an internal request, keeps the
 * sessions that could not be ended and answers the provider with an error
 */
@DisplayName("AWE OIDC back-channel logout handler")
class AweOidcBackChannelLogoutHandlerTest {

  private static final String COOKIE_NAME = "AWESESSION";
  private static final String BASE_URL = "http://awe.example.com/app";
  private static final String PATH = "/app/logout/connect/back-channel/keycloak";
  private static final String DEFAULT_URI = "{baseUrl}/logout/connect/back-channel/{registrationId}";
  private static final String DEFAULT_TARGET = BASE_URL + "/logout/connect/back-channel/keycloak";
  private static final String PROVIDER_SESSION = "provider-sid";
  private static final String TOKEN_VALUE = "signed-logout-token";

  private OidcSessionRegistry registry;
  private RestTemplate restTemplate;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    registry = new InMemoryOidcSessionRegistry();
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
  }

  private AweOidcBackChannelLogoutHandler handler(String logoutUri) {
    return new AweOidcBackChannelLogoutHandler(registry, restTemplate, COOKIE_NAME, logoutUri);
  }

  private static OidcLogoutToken logoutToken() {
    return OidcLogoutToken.withTokenValue(TOKEN_VALUE)
      .claim("iss", "https://idp.example.com")
      .claim("aud", List.of("awe"))
      .claim("iat", Instant.now())
      .claim("jti", "token-id")
      .claim("sid", PROVIDER_SESSION)
      .claim("events", Map.of("http://schemas.openid.net/event/backchannel-logout", Map.of()))
      .build();
  }

  private static Authentication logoutAuthentication() {
    return new TestingAuthenticationToken(logoutToken(), null);
  }

  private void register(String sessionId, Map<String, String> headers) {
    OidcIdToken idToken = new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60),
      Map.of("sub", "user", "iss", "https://idp.example.com", "aud", List.of("awe"), "sid", PROVIDER_SESSION));
    registry.saveSessionInformation(new OidcSessionInformation(sessionId, headers, new DefaultOidcUser(List.of(), idToken)));
  }

  private static MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", PATH);
    request.setScheme("http");
    request.setServerName("awe.example.com");
    request.setServerPort(80);
    request.setContextPath("/app");
    request.setRequestURI(PATH);
    return request;
  }

  @Test
  @DisplayName("Ends every session of the token with its own cookie and the token as form data")
  void callsEverySession() {
    register("one", Map.of());
    register("two", Map.of());
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(method(HttpMethod.POST))
      .andExpect(header("Cookie", COOKIE_NAME + "=one"))
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
      .andExpect(content().formDataContains(Map.of("logout_token", TOKEN_VALUE, "_spring_security_internal_logout", "true")))
      .andRespond(MockRestResponseCreators.withSuccess());
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("Cookie", COOKIE_NAME + "=two"))
      .andRespond(MockRestResponseCreators.withSuccess());
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler(DEFAULT_URI).logout(request(), response, logoutAuthentication());

    server.verify();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(registry.removeSessionInformation(logoutToken())).isEmpty();
  }

  @Test
  @DisplayName("Sends the headers saved with the session, such as the CSRF token")
  void sendsSavedHeaders() {
    register("one", Map.of("X-CSRF-TOKEN", "csrf-value"));
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("X-CSRF-TOKEN", "csrf-value"))
      .andRespond(MockRestResponseCreators.withSuccess());

    handler(DEFAULT_URI).logout(request(), new MockHttpServletResponse(), logoutAuthentication());

    server.verify();
  }

  @Test
  @DisplayName("A subclass can encode the session id of the cookie, as Spring Session's Base64 serializer expects")
  void subclassEncodesTheCookieValue() {
    register("one", Map.of());
    String encoded = Base64.getEncoder().encodeToString("one".getBytes(StandardCharsets.UTF_8));
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("Cookie", COOKIE_NAME + "=" + encoded))
      .andRespond(MockRestResponseCreators.withSuccess());
    AweOidcBackChannelLogoutHandler base64Handler = new AweOidcBackChannelLogoutHandler(registry, restTemplate, COOKIE_NAME,
      DEFAULT_URI) {
      @Override
      protected String sessionCookieValue(String sessionId) {
        return Base64.getEncoder().encodeToString(sessionId.getBytes(StandardCharsets.UTF_8));
      }
    };

    base64Handler.logout(request(), new MockHttpServletResponse(), logoutAuthentication());

    server.verify();
  }

  @Test
  @DisplayName("Does not call anything when the token has no sessions")
  void noSessionsNoCall() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler(DEFAULT_URI).logout(request(), response, logoutAuthentication());

    server.verify();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  @DisplayName("Ignores an authentication that is not a logout token")
  void ignoresOtherAuthentications() {
    register("one", Map.of());

    handler(DEFAULT_URI).logout(request(), new MockHttpServletResponse(), new TestingAuthenticationToken("user", null));
    handler(DEFAULT_URI).logout(request(), new MockHttpServletResponse(), null);

    server.verify();
    assertThat(registry.removeSessionInformation("one")).isNotNull();
  }

  @Test
  @DisplayName("An unexpected error ending a session keeps that session registered and does not stop the others")
  void unexpectedErrorKeepsTheSessionRegistered() {
    register("broken", Map.of());
    register("good", Map.of());
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("Cookie", COOKIE_NAME + "=broken"))
      .andRespond(request -> {
        throw new IllegalStateException("unexpected");
      });
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("Cookie", COOKIE_NAME + "=good"))
      .andRespond(MockRestResponseCreators.withSuccess());
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler(DEFAULT_URI).logout(request(), response, logoutAuthentication());

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(registry.removeSessionInformation("broken")).isNotNull();
    assertThat(registry.removeSessionInformation("good")).isNull();
  }

  @Test
  @DisplayName("A failing session does not stop the others: it stays registered and the provider gets an error")
  void failingSessionDoesNotStopOthers() throws Exception {
    register("bad", Map.of());
    register("good", Map.of());
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("Cookie", COOKIE_NAME + "=bad"))
      .andRespond(MockRestResponseCreators.withServerError());
    server.expect(requestTo(DEFAULT_TARGET)).andExpect(header("Cookie", COOKIE_NAME + "=good"))
      .andRespond(MockRestResponseCreators.withSuccess());
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler(DEFAULT_URI).logout(request(), response, logoutAuthentication());

    server.verify();
    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("partial_logout");
    assertThat(registry.removeSessionInformation("bad")).isNotNull();
    assertThat(registry.removeSessionInformation("good")).isNull();
  }

  @Test
  @DisplayName("Expands the configured logout address with the request and registration placeholders")
  void honoursLogoutUriTemplate() {
    register("one", Map.of());
    server.expect(requestTo("http://localhost:8080/app/logout/connect/back-channel/keycloak?host=awe.example.com&port="))
      .andRespond(MockRestResponseCreators.withSuccess());

    handler("http://localhost:8080{basePath}/logout/connect/back-channel/{registrationId}?host={baseHost}&port={basePort}")
      .logout(request(), new MockHttpServletResponse(), logoutAuthentication());

    server.verify();
  }

  @Test
  @DisplayName("Uses an empty registration id when the request is not on the back-channel path")
  void emptyRegistrationIdOutsideBackChannelPath() {
    register("one", Map.of());
    MockHttpServletRequest other = request();
    other.setRequestURI("/app/other");
    server.expect(requestTo(BASE_URL + "/logout/connect/back-channel/")).andRespond(MockRestResponseCreators.withSuccess());

    handler(DEFAULT_URI).logout(other, new MockHttpServletResponse(), logoutAuthentication());

    server.verify();
  }

  @Test
  @DisplayName("Applies the connect and read timeouts to the internal HTTP client")
  void appliesTimeouts() {
    RestTemplate created = AweOidcBackChannelLogoutHandler.restOperations(Duration.ofMillis(300), Duration.ofMillis(700));

    assertThat(created.getRequestFactory()).hasFieldOrPropertyWithValue("connectTimeout", 300)
      .hasFieldOrPropertyWithValue("readTimeout", 700);
  }
}
