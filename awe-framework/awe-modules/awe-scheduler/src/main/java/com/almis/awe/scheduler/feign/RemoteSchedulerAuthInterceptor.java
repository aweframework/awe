package com.almis.awe.scheduler.feign;

import com.almis.awe.rest.dto.LoginRequest;
import com.almis.awe.rest.dto.LoginResponse;
import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Adds the bearer token of the remote scheduler instance to the calls of {@link RemoteScheduler}.
 *
 * <p>The token comes from {@code POST /api/authenticate} on the instance and is reused until it is about to
 * expire: the expiration of the login answer, else the {@code exp} claim of the token. When neither gives one the
 * token is not cached and every call logs in again.
 */
@Slf4j
public class RemoteSchedulerAuthInterceptor implements RequestInterceptor {

  private static final String API_SUFFIX = "/scheduler/api/v1";
  private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);
  // shortcut: fixed login timeouts, make them properties if a deployment needs others
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

  private final String user;
  private final String password;
  private final String loginUrl;
  private final Clock clock;
  private final RestClient restClient;
  private String token;
  private Instant expiresAt;
  private boolean warned = false;

  /**
   * Create the interceptor
   *
   * @param schedulerUrl Remote scheduler API url (ending in /scheduler/api/v1)
   * @param user         User to log in with
   * @param password     Password to log in with
   * @param clock        Clock used to check the token expiration
   * (the login has a 2 s connect and a 10 s read timeout, so a stalled login cannot block the calls forever)
   */
  public RemoteSchedulerAuthInterceptor(String schedulerUrl, String user, String password, Clock clock) {
    this(schedulerUrl, user, password, clock, CONNECT_TIMEOUT, READ_TIMEOUT);
  }

  RemoteSchedulerAuthInterceptor(String schedulerUrl, String user, String password, Clock clock,
                                 Duration connectTimeout, Duration readTimeout) {
    String base = schedulerUrl.endsWith("/") ? schedulerUrl.substring(0, schedulerUrl.length() - 1) : schedulerUrl;
    this.loginUrl = (base.endsWith(API_SUFFIX) ? base.substring(0, base.length() - API_SUFFIX.length()) : base)
      + "/api/authenticate";
    this.user = user;
    this.password = password;
    this.clock = clock;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public void apply(RequestTemplate template) {
    String bearer = token();
    if (bearer != null) {
      template.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
    }
  }

  private synchronized String token() {
    if (!StringUtils.hasText(user)) {
      if (!warned) {
        warned = true;
        log.warn("Remote scheduler calls are sent without credentials: awe.scheduler.remote-scheduler-user is empty");
      }
      return null;
    }
    if (token == null || expiresAt == null || !clock.instant().isBefore(expiresAt.minus(REFRESH_MARGIN))) {
      login();
    }
    return token;
  }

  /**
   * Expiration of the token: the one of the login answer, else the {@code exp} claim of the token (the login filter
   * of the instance answers without it). The claim is only read to know when to log in again, never trusted
   *
   * @param response Login answer
   * @return Expiration, or null when unknown (the token is not cached)
   */
  private static Instant expiration(LoginResponse response) {
    if (response.getExpiresAt() != null) {
      return response.getExpiresAt().toInstant();
    }
    try {
      Date claim = response.getToken() == null ? null : JWT.decode(response.getToken()).getExpiresAt();
      return claim == null ? null : claim.toInstant();
    } catch (JWTDecodeException exception) {
      return null;
    }
  }

  private void login() {
    try {
      LoginResponse response = restClient.post().uri(loginUrl)
        .body(new LoginRequest().setUsername(user).setPassword(password))
        .retrieve().body(LoginResponse.class);
      token = response == null ? null : response.getToken();
      expiresAt = response == null ? null : expiration(response);
    } catch (RestClientException exception) {
      token = null;
      // The cause is not chained: its message may echo the request
      throw new IllegalStateException("Login of user '" + user + "' at the remote scheduler failed: "
        + exception.getClass().getSimpleName());
    }
  }
}
