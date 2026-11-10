package com.almis.awe.service.email;

import com.almis.awe.exception.AWException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import lombok.Getter;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Optional;

/**
 * Mail sender that authenticates with SASL XOAUTH2: user = sender mailbox, password = OAuth2 access token.
 * The token is requested right before each connection and is never stored in the sender.
 */
@Getter
public class XOAuth2MailSender extends JavaMailSenderImpl {

  private final String serverName;
  private final String registrationId;
  private final EmailOAuth2TokenService tokenService;

  /**
   * Constructor
   *
   * @param serverName     Email server name
   * @param registrationId Spring Security client registration identifier
   * @param tokenService   Email OAuth2 token service
   */
  public XOAuth2MailSender(String serverName, String registrationId, EmailOAuth2TokenService tokenService) {
    this.serverName = serverName;
    this.registrationId = registrationId;
    this.tokenService = tokenService;
    getJavaMailProperties().setProperty("mail.smtp.auth", "true");
    getJavaMailProperties().setProperty("mail.smtp.auth.mechanisms", "XOAUTH2");
  }

  @Override
  protected Transport connectTransport() throws MessagingException {
    // The access token is a credential: it is only sent through STARTTLS or SSL
    if (!isTlsRequired()) {
      throw new AuthenticationFailedException(String.format("Email server '%s' uses OAuth2 authentication without STARTTLS or SSL", serverName));
    }
    // XOAUTH2 authenticates the sender mailbox: without it no token is requested
    if (getUsername() == null || getUsername().isBlank()) {
      throw new AuthenticationFailedException(String.format("Email server '%s' uses OAuth2 authentication without a sender mailbox", serverName));
    }

    String accessToken = getAccessToken();
    Transport transport = getTransport(getSession());
    try {
      transport.connect(getHost(), getPort(), getUsername(), accessToken);
    } catch (AuthenticationFailedException exc) {
      // The server rejected the token: the next connection asks for a new one
      tokenService.removeAccessToken(serverName, registrationId);
      throw exc;
    }
    return transport;
  }

  private boolean isTlsRequired() {
    String protocol = Optional.ofNullable(getProtocol()).orElse(DEFAULT_PROTOCOL);
    String prefix = "mail." + protocol + ".";
    return "smtps".equals(protocol)
      || Boolean.parseBoolean(getJavaMailProperties().getProperty(prefix + "ssl.enable"))
      || Boolean.parseBoolean(getJavaMailProperties().getProperty(prefix + "starttls.required"));
  }

  private String getAccessToken() throws AuthenticationFailedException {
    if (tokenService == null) {
      throw new AuthenticationFailedException(String.format("Email server '%s' uses OAuth2 registration '%s' but OAuth2 is not available", serverName, registrationId));
    }
    try {
      return tokenService.getAccessToken(serverName, registrationId);
    } catch (AWException exc) {
      throw new AuthenticationFailedException(exc.getMessage());
    }
  }
}
