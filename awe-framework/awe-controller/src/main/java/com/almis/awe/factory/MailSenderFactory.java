package com.almis.awe.factory;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.EmailServer;
import com.almis.awe.model.type.EmailAuthenticationType;
import com.almis.awe.model.type.EmailSecurityMode;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.email.EmailOAuth2TokenService;
import com.almis.awe.service.email.XOAuth2MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import tools.jackson.core.JacksonException;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MailSenderFactory extends ServiceConfig {

  private static final String SMTP_PROTOCOL = "smtp";

  private final JavaMailSender defaultSender;
  private final QueryService queryService;
  private final EncodeService encodeService;
  private final EmailOAuth2TokenService tokenService;
  private Map<String, JavaMailSender> mailSenderMap;

  public MailSenderFactory(JavaMailSender defaultSender, QueryService queryService) {
    this(defaultSender, queryService, null);
  }

  public MailSenderFactory(JavaMailSender defaultSender, QueryService queryService, EncodeService encodeService) {
    this(defaultSender, queryService, encodeService, null);
  }

  public MailSenderFactory(JavaMailSender defaultSender, QueryService queryService, EncodeService encodeService,
                           EmailOAuth2TokenService tokenService) {
    this.defaultSender = defaultSender;
    this.queryService = queryService;
    this.encodeService = encodeService;
    this.tokenService = tokenService;
  }

  /**
   * Initialize Mail Sender Factory
   */
  public MailSenderFactory init() {
    try {
      // Retrieve server list
      DataList dataList = queryService.launchPrivateQuery("EmlSrvSndLst").getDataList();

      Map<String, JavaMailSender> senders = new ConcurrentHashMap<>();
      // Each row is read on its own, so a server that cannot be read is left out and the others are kept
      for (Map<String, CellData> row : dataList.getRows()) {
        String serverName = Optional.ofNullable(row.get("name")).map(CellData::getStringValue).orElse(null);
        try {
          EmailServer emailServer = DataListUtil.getMapper().convertValue(row, EmailServer.class);
          senders.put(emailServer.getName(), generateMailSender(emailServer));
        } catch (JacksonException exc) {
          // The conversion message may quote row values (password included), so only the server name is logged
          log.error("Error reading email server '{}': {}", serverName, exc.getClass().getSimpleName());
        } catch (IllegalArgumentException exc) {
          log.error("Error generating mail sender of email server '{}': {}", serverName, exc.getMessage());
        }
      }
      mailSenderMap = senders;
    } catch (AWException | IllegalArgumentException exc) {
      log.error("Error retrieving email server list", exc);
    }

    return this;
  }

  /**
   * Retrieve mail server
   * @return Mail server
   */
  public JavaMailSender getMailSender() {
    return getMailSender(null);
  }

  /**
   * Retrieve mail server
   * @param mailServer Mail server
   * @return Mail server
   */
  public JavaMailSender getMailSender(String mailServer) {
    if (mailSenderMap == null) {
      this.init();
    }

    if (mailServer != null && mailSenderMap != null) {
      return Optional.ofNullable(mailSenderMap.get(mailServer)).orElse(defaultSender);
    }

    return defaultSender;
  }

  /**
   * Generate each mail server
   * @param emailServer Mail server
   * @return Mail sender from mail server
   */
  private JavaMailSender generateMailSender(EmailServer emailServer) {
    Properties properties = new Properties();

    // Create JavaMailSender
    JavaMailSenderImpl javaMailSender = createMailSender(emailServer);
    // SSL is implicit TLS on the smtp protocol (mail.smtp.ssl.enable), so every property uses the mail.smtp prefix
    javaMailSender.setProtocol(SMTP_PROTOCOL);
    javaMailSender.setHost(emailServer.getHost());
    javaMailSender.setPort(emailServer.getPortOrDefault());

    // Add connection security
    switch (emailServer.getSecurityModeOrDefault()) {
      case STARTTLS -> {
        properties.setProperty("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.smtp.starttls.required", "true");
      }
      case SSL -> properties.setProperty("mail.smtp.ssl.enable", "true");
      default -> {
        // Plain connection
      }
    }

    // Add authentication
    switch (emailServer.getAuthenticationType()) {
      case BASIC -> {
        properties.setProperty("mail.smtp.auth", "true");
        javaMailSender.setUsername(emailServer.getUser());
        javaMailSender.setPassword(decrypt(emailServer.getName(), emailServer.getPass()));
      }
      // XOAUTH2: the sender mailbox authenticates with an access token requested on each connection
      case OAUTH2 -> javaMailSender.setUsername(emailServer.getUser());
      default -> {
        // No authentication
      }
    }

    javaMailSender.getJavaMailProperties().putAll(properties);
    return javaMailSender;
  }

  /**
   * Create the mail sender of the server authentication type. A misconfigured OAuth2 server (unknown registration or
   * no STARTTLS/SSL) keeps its own sender, so its messages fail instead of leaving through the default server.
   * @param emailServer Mail server
   * @return Mail sender
   */
  private JavaMailSenderImpl createMailSender(EmailServer emailServer) {
    if (emailServer.getAuthenticationType() != EmailAuthenticationType.OAUTH2) {
      return new JavaMailSenderImpl();
    }

    if (emailServer.getSecurityModeOrDefault() == EmailSecurityMode.NONE) {
      log.error("Email server '{}' uses OAuth2 authentication without STARTTLS or SSL", emailServer.getName());
    }
    if (tokenService == null) {
      log.error("Email server '{}' uses OAuth2 registration '{}' but OAuth2 is not available", emailServer.getName(), emailServer.getOauthRegistration());
    } else {
      try {
        tokenService.checkRegistration(emailServer.getName(), emailServer.getOauthRegistration());
      } catch (AWException exc) {
        log.error(exc.getMessage());
      }
    }
    return new XOAuth2MailSender(emailServer.getName(), emailServer.getOauthRegistration(), tokenService);
  }

  /**
   * Decrypt a secret stored by the email server maintain. A secret that cannot be decrypted is used as stored.
   * @param serverName Mail server name
   * @param secret Stored secret
   * @return Decrypted secret
   */
  private String decrypt(String serverName, String secret) {
    if (secret == null || secret.isEmpty()) {
      return secret;
    }

    try {
      return Optional.ofNullable(encodeService).orElseGet(() -> getBean(EncodeService.class)).decryptRipEmd160(secret);
    } catch (AWException exc) {
      log.warn("Email server '{}' has a secret that cannot be decrypted: it is used as stored", serverName);
      return secret;
    }
  }
}
