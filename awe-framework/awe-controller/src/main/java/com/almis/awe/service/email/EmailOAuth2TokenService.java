package com.almis.awe.service.email;

import com.almis.awe.exception.AWException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Access tokens of email servers with OAuth2 authentication. The authorized client manager caches the token of each
 * email server (principal = server name) and asks for a new one when it expires.
 * Messages and logs name the server and the registration only, never tokens or secrets.
 */
@Slf4j
public class EmailOAuth2TokenService {

  private static final Duration TOKEN_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration TOKEN_READ_TIMEOUT = Duration.ofSeconds(30);

  private final EmailOAuth2RegistrationService registrationService;
  // Null when the application has no OAuth2 client registration
  private final OAuth2AuthorizedClientManager authorizedClientManager;
  private final OAuth2AuthorizedClientService authorizedClientService;

  /**
   * Constructor
   *
   * @param registrationService     Email OAuth2 registration service
   * @param authorizedClientManager client_credentials authorized client manager (nullable)
   * @param authorizedClientService authorized clients of the manager (nullable)
   */
  public EmailOAuth2TokenService(EmailOAuth2RegistrationService registrationService, OAuth2AuthorizedClientManager authorizedClientManager,
                                 OAuth2AuthorizedClientService authorizedClientService) {
    this.registrationService = registrationService;
    this.authorizedClientManager = authorizedClientManager;
    this.authorizedClientService = authorizedClientService;
  }

  /**
   * Create the token service of the email servers with its own authorized client manager. It works outside HTTP
   * requests and calls the token endpoint with connect and read timeouts.
   *
   * @param registrationService          Email OAuth2 registration service
   * @param clientRegistrationRepository Client registration repository
   * @return Email OAuth2 token service
   */
  public static EmailOAuth2TokenService create(EmailOAuth2RegistrationService registrationService, ClientRegistrationRepository clientRegistrationRepository) {
    return create(registrationService, clientRegistrationRepository, TOKEN_CONNECT_TIMEOUT, TOKEN_READ_TIMEOUT);
  }

  static EmailOAuth2TokenService create(EmailOAuth2RegistrationService registrationService, ClientRegistrationRepository clientRegistrationRepository,
                                        Duration connectTimeout, Duration readTimeout) {
    // java.net.http client: HttpURLConnection loses the body of 401 answers to streamed requests
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
      .connectTimeout(connectTimeout)
      .build());
    requestFactory.setReadTimeout(readTimeout);

    // Same client as the Spring Security default token response client, with timeouts and the OAuth2 error of 4xx answers
    RestClientClientCredentialsTokenResponseClient tokenResponseClient = new RestClientClientCredentialsTokenResponseClient();
    tokenResponseClient.setRestClient(RestClient.builder()
      .requestFactory(requestFactory)
      .messageConverters(messageConverters -> {
        messageConverters.clear();
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
      })
      .defaultStatusHandler(new TokenErrorHandler())
      .build());

    InMemoryOAuth2AuthorizedClientService authorizedClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
      new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
    authorizedClientManager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials(clientCredentials -> clientCredentials.accessTokenResponseClient(tokenResponseClient))
      .build());
    return new EmailOAuth2TokenService(registrationService, authorizedClientManager, authorizedClientService);
  }

  /**
   * Remove the cached access token of the email server, so the next connection asks for a new one
   *
   * @param serverName     Email server name
   * @param registrationId Registration identifier
   */
  public void removeAccessToken(String serverName, String registrationId) {
    if (authorizedClientService != null) {
      authorizedClientService.removeAuthorizedClient(registrationId, serverName);
    }
  }

  /**
   * Check that the email server registration can provide access tokens
   *
   * @param serverName     Email server name
   * @param registrationId Registration identifier
   * @throws AWException Registration not available
   */
  public void checkRegistration(String serverName, String registrationId) throws AWException {
    registrationService.getClientCredentialsRegistration(serverName, registrationId);
    if (authorizedClientManager == null) {
      throw new AWException(String.format("Email server '%s' cannot obtain OAuth2 access tokens from registration '%s': no authorized client manager", serverName, registrationId));
    }
  }

  /**
   * Retrieve a valid access token of the email server
   *
   * @param serverName     Email server name
   * @param registrationId Registration identifier
   * @return Access token
   * @throws AWException Token not available
   */
  public String getAccessToken(String serverName, String registrationId) throws AWException {
    checkRegistration(serverName, registrationId);

    OAuth2AuthorizedClient authorizedClient;
    try {
      authorizedClient = authorizedClientManager.authorize(OAuth2AuthorizeRequest
        .withClientRegistrationId(registrationId)
        .principal(serverName)
        .build());
    } catch (OAuth2AuthorizationException exc) {
      throw tokenError(serverName, registrationId, exc.getError().getErrorCode());
    } catch (RuntimeException exc) {
      throw tokenError(serverName, registrationId, exc.getClass().getSimpleName());
    }

    if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
      throw tokenError(serverName, registrationId, "no access token");
    }
    return authorizedClient.getAccessToken().getTokenValue();
  }

  private static AWException tokenError(String serverName, String registrationId, String reason) {
    String message = String.format("Email server '%s' could not obtain an OAuth2 access token from registration '%s' (%s)", serverName, registrationId, reason);
    log.error(message);
    return new AWException(message);
  }

  /**
   * Token endpoint error handler. Spring Security reads the OAuth2 error of 400 answers only, but issuers answer
   * invalid_client with 401 (RFC 6749 section 5.2): the OAuth2 error of the other 4xx answers is read too, so the error
   * code of the issuer is kept instead of the generic invalid_token_response.
   */
  private static final class TokenErrorHandler implements ResponseErrorHandler {
    private final OAuth2ErrorResponseErrorHandler oauth2ErrorHandler = new OAuth2ErrorResponseErrorHandler();
    private final HttpMessageConverter<OAuth2Error> errorConverter = new OAuth2ErrorHttpMessageConverter();

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
      return oauth2ErrorHandler.hasError(response);
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
      if (response.getStatusCode().is4xxClientError() && response.getStatusCode().value() != HttpStatus.BAD_REQUEST.value()) {
        OAuth2Error error = readError(response);
        if (error != null) {
          throw new OAuth2AuthorizationException(error);
        }
      }
      oauth2ErrorHandler.handleError(url, method, response);
    }

    private OAuth2Error readError(ClientHttpResponse response) throws IOException {
      try {
        return errorConverter.read(OAuth2Error.class, response);
      } catch (HttpMessageNotReadableException exc) {
        // Not an OAuth2 error answer: generic error code
        return null;
      }
    }
  }
}
