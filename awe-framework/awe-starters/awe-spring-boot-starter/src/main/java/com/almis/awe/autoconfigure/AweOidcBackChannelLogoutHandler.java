package com.almis.awe.autoconfigure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.authentication.logout.OidcLogoutToken;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Back-channel logout handler of AWE.
 *
 * <p>It does what the Spring Security handler does, with the HTTP client built by AWE (connect and read timeouts):
 * for each session registered for the logout token it posts to the logout address, with the session cookie and the
 * headers saved with the session (CSRF), so the request runs the same logout handlers as a user logout. A session that
 * cannot be ended stays registered, the rest are still ended, and the provider gets a {@code partial_logout} error.</p>
 *
 * <p>Spring Security does not let callers set the HTTP client of its handler, and its authentication type is not
 * public, so this handler reads the logout token from the public principal and the registration id from the request
 * path of the back-channel endpoint.</p>
 */
@Slf4j
public class AweOidcBackChannelLogoutHandler implements LogoutHandler {

  /**
   * Default internal logout address, the same as Spring Security
   */
  public static final String DEFAULT_LOGOUT_URI = "{baseUrl}/logout/connect/back-channel/{registrationId}";

  private static final String BACK_CHANNEL_PATH = "/logout/connect/back-channel/{registrationId}";
  private static final String REGISTRATION_ID = "registrationId";
  private static final String LOGOUT_TOKEN_PARAMETER = "logout_token";
  private static final String INTERNAL_LOGOUT_PARAMETER = "_spring_security_internal_logout";
  private static final String PARTIAL_LOGOUT = "partial_logout";
  private static final String ERROR_URI = "https://openid.net/specs/openid-connect-backchannel-1_0.html#Validation";

  private final OidcSessionRegistry sessionRegistry;
  private final RestOperations restOperations;
  private final String sessionCookieName;
  private final String logoutUri;
  private final OAuth2ErrorHttpMessageConverter errorConverter = new OAuth2ErrorHttpMessageConverter();
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  /**
   * Create the handler
   *
   * @param sessionRegistry   OIDC session registry
   * @param restOperations    HTTP client used for the internal logout requests
   * @param sessionCookieName name of the session cookie
   * @param logoutUri         internal logout address (may use the placeholders baseUrl, baseScheme, baseHost, basePort,
   *                          basePath and registrationId)
   */
  public AweOidcBackChannelLogoutHandler(OidcSessionRegistry sessionRegistry, RestOperations restOperations,
                                         String sessionCookieName, String logoutUri) {
    this.sessionRegistry = sessionRegistry;
    this.restOperations = restOperations;
    this.sessionCookieName = sessionCookieName;
    this.logoutUri = logoutUri;
  }

  /**
   * Build the HTTP client of the internal logout requests
   *
   * @param connectTimeout connection timeout
   * @param readTimeout    read timeout
   * @return HTTP client with the timeouts applied
   */
  public static RestTemplate restOperations(Duration connectTimeout, Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    return new RestTemplate(requestFactory);
  }

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof OidcLogoutToken logoutToken)) {
      log.debug("OIDC back-channel logout skipped: the authentication is not a logout token");
      return;
    }

    String endpoint = computeLogoutEndpoint(request);
    List<String> errors = new ArrayList<>();
    int total = 0;
    for (OidcSessionInformation session : sessionRegistry.removeSessionInformation(logoutToken)) {
      total++;
      try {
        endSession(endpoint, logoutToken, session);
      } catch (RuntimeException exception) {
        // Any failure (not only HTTP errors) puts the session back: it was removed from the registry above
        log.warn("OIDC back-channel logout failed to end a session: {}", exception.getMessage());
        errors.add(exception.getMessage());
        sessionRegistry.saveSessionInformation(session);
      }
    }
    log.debug("OIDC back-channel logout ended {} out of {} sessions", total - errors.size(), total);

    if (!errors.isEmpty()) {
      writeFailure(response, new OAuth2Error(PARTIAL_LOGOUT, "not all sessions were terminated: " + errors, ERROR_URI));
    }
  }

  private void endSession(String endpoint, OidcLogoutToken logoutToken, OidcSessionInformation session) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, sessionCookieName + "=" + sessionCookieValue(session.getSessionId()));
    session.getAuthorities().forEach(headers::add);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add(LOGOUT_TOKEN_PARAMETER, logoutToken.getTokenValue());
    body.add(INTERNAL_LOGOUT_PARAMETER, "true");
    restOperations.postForEntity(endpoint, new HttpEntity<>(body, headers), Object.class);
  }

  /**
   * Value of the session cookie sent to end a session. The raw session id by default; override it when the session store
   * expects another encoding, such as Spring Session's cookie serializer, which encodes the id in Base64 by default.
   *
   * @param sessionId id of the session to end
   * @return cookie value that identifies the session
   */
  protected String sessionCookieValue(String sessionId) {
    return sessionId;
  }

  private String computeLogoutEndpoint(HttpServletRequest request) {
    UriComponents base = UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
      .replacePath(request.getContextPath()).replaceQuery(null).fragment(null).build();
    Map<String, String> variables = new HashMap<>();
    variables.put("baseScheme", valueOrEmpty(base.getScheme()));
    variables.put("baseUrl", base.toUriString());
    variables.put("baseHost", valueOrEmpty(base.getHost()));
    variables.put("basePath", valueOrEmpty(base.getPath()));
    variables.put("basePort", base.getPort() == -1 ? "" : ":" + base.getPort());
    variables.put(REGISTRATION_ID, registrationId(request));
    return UriComponentsBuilder.fromUriString(logoutUri).buildAndExpand(variables).toUriString();
  }

  private String registrationId(HttpServletRequest request) {
    String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    if (pathMatcher.match(BACK_CHANNEL_PATH, path)) {
      return pathMatcher.extractUriTemplateVariables(BACK_CHANNEL_PATH, path).get(REGISTRATION_ID);
    }
    log.warn("OIDC back-channel logout cannot read the registration id from the request path");
    return "";
  }

  private void writeFailure(HttpServletResponse response, OAuth2Error error) {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    try {
      errorConverter.write(error, null, new ServletServerHttpResponse(response));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static String valueOrEmpty(String value) {
    return value != null ? value : "";
  }
}
