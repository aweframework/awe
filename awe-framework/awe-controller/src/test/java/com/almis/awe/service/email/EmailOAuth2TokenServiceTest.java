package com.almis.awe.service.email;

import com.almis.awe.exception.AWException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOAuth2TokenServiceTest {

  private static final ClientRegistration REGISTRATION = ClientRegistration.withRegistrationId("office365")
    .clientId("client-id")
    .clientSecret("client-secret")
    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
    .tokenUri("https://provider.com/oauth/token")
    .build();

  private final EmailOAuth2RegistrationService registrationService =
    new EmailOAuth2RegistrationService(new InMemoryClientRegistrationRepository(REGISTRATION));
  private final OAuth2AuthorizedClientManager manager = mock(OAuth2AuthorizedClientManager.class);
  private final OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);
  private final EmailOAuth2TokenService tokenService = new EmailOAuth2TokenService(registrationService, manager, authorizedClientService);

  @Test
  void accessTokenIsAuthorizedForTheRegistrationWithTheServerAsPrincipal() throws AWException {
    when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(REGISTRATION, "Mail",
      new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plusSeconds(3600))));

    assertThat(tokenService.getAccessToken("Mail", "office365")).isEqualTo("access-token");

    ArgumentCaptor<OAuth2AuthorizeRequest> request = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
    verify(manager).authorize(request.capture());
    assertThat(request.getValue().getClientRegistrationId()).isEqualTo("office365");
    assertThat(request.getValue().getPrincipal().getName()).isEqualTo("Mail");
  }

  @Test
  void tokenErrorNamesServerAndRegistrationOnly() {
    when(manager.authorize(any())).thenThrow(new ClientAuthorizationException(
      new OAuth2Error("invalid_client", "Invalid client secret client-secret", null), "office365"));

    assertThatThrownBy(() -> tokenService.getAccessToken("Mail", "office365"))
      .isInstanceOf(AWException.class)
      .hasMessageContainingAll("'Mail'", "'office365'", "invalid_client")
      .hasNoCause()
      .satisfies(exc -> assertThat(exc.getMessage()).doesNotContain("client-secret"));
  }

  @Test
  void withoutAuthorizedClientManagerTheTokenCannotBeObtained() {
    EmailOAuth2TokenService withoutManager = new EmailOAuth2TokenService(new EmailOAuth2RegistrationService(null), null, null);

    assertThatThrownBy(() -> withoutManager.getAccessToken("Mail", "office365"))
      .isInstanceOf(AWException.class)
      .hasMessageContainingAll("'Mail'", "'office365'");
  }

  @Test
  void removedAccessTokenIsRemovedFromTheAuthorizedClientsOfTheServer() {
    tokenService.removeAccessToken("Mail", "office365");

    verify(authorizedClientService).removeAuthorizedClient("office365", "Mail");
  }

  @Test
  @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void tokenErrorOfTheIssuerKeepsItsOAuth2ErrorCode() throws Exception {
    HttpServer tokenEndpoint = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    tokenEndpoint.createContext("/oauth/token", exchange -> {
      byte[] body = "{\"error\":\"invalid_client\",\"error_description\":\"Invalid client or Invalid client credentials\"}"
        .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(401, body.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(body);
      }
    });
    tokenEndpoint.start();
    try {
      ClientRegistration registration = ClientRegistration.withRegistrationId("mailtest")
        .clientId("client-id")
        .clientSecret("wrong-client-secret")
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenUri("http://localhost:" + tokenEndpoint.getAddress().getPort() + "/oauth/token")
        .build();
      InMemoryClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(registration);
      EmailOAuth2TokenService withTokenEndpoint = EmailOAuth2TokenService.create(new EmailOAuth2RegistrationService(repository), repository);

      assertThatThrownBy(() -> withTokenEndpoint.getAccessToken("Mail", "mailtest"))
        .isInstanceOf(AWException.class)
        .hasMessageContainingAll("'Mail'", "'mailtest'", "invalid_client")
        .satisfies(exc -> assertThat(exc.getMessage()).doesNotContain("wrong-client-secret"));
    } finally {
      tokenEndpoint.stop(0);
    }
  }

  @Test
  @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void tokenEndpointThatDoesNotAnswerFailsAfterTheReadTimeout() throws Exception {
    // The socket accepts the connection (backlog) but never answers the token request
    try (ServerSocket silentTokenEndpoint = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      ClientRegistration registration = ClientRegistration.withRegistrationId("office365")
        .clientId("client-id")
        .clientSecret("client-secret")
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenUri("http://localhost:" + silentTokenEndpoint.getLocalPort() + "/oauth/token")
        .build();
      InMemoryClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(registration);
      EmailOAuth2TokenService withTimeouts = EmailOAuth2TokenService.create(new EmailOAuth2RegistrationService(repository),
        repository, Duration.ofSeconds(1), Duration.ofMillis(200));

      assertThatThrownBy(() -> withTimeouts.getAccessToken("Mail", "office365"))
        .isInstanceOf(AWException.class)
        .hasMessageContainingAll("'Mail'", "'office365'");
    }
  }
}
