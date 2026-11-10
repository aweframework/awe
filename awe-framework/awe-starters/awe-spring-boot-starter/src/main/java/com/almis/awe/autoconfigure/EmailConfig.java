package com.almis.awe.autoconfigure;

import com.almis.awe.autoconfigure.config.EmailConfigProperties;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.exception.AWException;
import com.almis.awe.factory.MailSenderFactory;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EmailService;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.data.connector.maintain.EmailMaintainConnector;
import com.almis.awe.service.email.EmailOAuth2RegistrationService;
import com.almis.awe.service.email.EmailOAuth2TokenService;
import com.almis.awe.service.email.XOAuth2MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Properties;

/**
 * Email configuration
 * @author dfuentes
 * Created by dfuentes on 25/04/2017.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(value = EmailConfigProperties.class)
@ConditionalOnProperty(name = "awe.mail.enabled", havingValue = "true")
public class EmailConfig {

  // Name of the default sender in OAuth2 messages and in the token cache of the email servers
  private static final String DEFAULT_SENDER_NAME = "awe.mail";

  // Email config properties
  private final EmailConfigProperties emailConfigProperties;

  /**
   * EmailConfig constructor
   * @param emailConfigProperties email config properties
   */
  public EmailConfig (EmailConfigProperties emailConfigProperties) {
    this.emailConfigProperties = emailConfigProperties;
  }

  /**
   * Default JavaMail configuration. With awe.mail.oauth2-registration the default sender authenticates with XOAUTH2
   * (awe.mail.user = sender mailbox, access token of the registration on each connection).
   *
   * @param emailOAuth2TokenService Email OAuth2 token service (only used with awe.mail.oauth2-registration)
   * @return Mail sender
   */
  @Bean
  @ConditionalOnMissingBean
  public JavaMailSender defaultMail(ObjectProvider<EmailOAuth2TokenService> emailOAuth2TokenService) {
    JavaMailSenderImpl javaMailSender;
    String registrationId = emailConfigProperties.getOauth2Registration();
    boolean oauth2 = registrationId != null && !registrationId.isBlank();

    // Create JavaMailSender
    if (oauth2) {
      javaMailSender = createOAuth2MailSender(registrationId, emailOAuth2TokenService.getIfAvailable());
    } else {
      javaMailSender = new JavaMailSenderImpl();
    }
    javaMailSender.setHost(emailConfigProperties.getHost());
    javaMailSender.setPort(emailConfigProperties.getPort());

    // Add authentication
    if (oauth2) {
      javaMailSender.setUsername(emailConfigProperties.getUser());
    } else if (emailConfigProperties.isAuth()) {
      javaMailSender.setUsername(emailConfigProperties.getUser());
      javaMailSender.setPassword(emailConfigProperties.getPass());
    }

    // Generate smtp properties
    Properties properties = new Properties();
    properties.put("mail.smtp.localhost", emailConfigProperties.getLocalhost());
    properties.put("mail.debug", emailConfigProperties.isDebug());
    properties.put("mail.smtp.starttls.enable", emailConfigProperties.isTls());
    properties.put("mail.smtp.ssl.enable", emailConfigProperties.isSsl());
    if (emailConfigProperties.isSsl()){
      properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      properties.put("mail.smtp.ssl.checkserveridentity", true);
    }
    if (oauth2) {
      // The access token is only sent through SSL or a required STARTTLS (string values: the sender checks them)
      properties.setProperty("mail.smtp.starttls.enable", String.valueOf(emailConfigProperties.isTls()));
      properties.setProperty("mail.smtp.starttls.required", String.valueOf(emailConfigProperties.isTls()));
      properties.setProperty("mail.smtp.ssl.enable", String.valueOf(emailConfigProperties.isSsl()));
    }

    // Add properties (keeping the XOAUTH2 properties of the OAuth2 sender)
    javaMailSender.getJavaMailProperties().putAll(properties);
    return javaMailSender;
  }

  /**
   * Create the OAuth2 default sender. A misconfiguration is logged and keeps the OAuth2 sender, so every message fails
   * instead of leaving without OAuth2. Logs name the property and the registration only.
   *
   * @param registrationId          Spring Security client registration identifier
   * @param emailOAuth2TokenService Email OAuth2 token service
   * @return OAuth2 mail sender
   */
  private JavaMailSenderImpl createOAuth2MailSender(String registrationId, EmailOAuth2TokenService emailOAuth2TokenService) {
    XOAuth2MailSender mailSender = new XOAuth2MailSender(DEFAULT_SENDER_NAME, registrationId, emailOAuth2TokenService);
    mailSender.setProtocol("smtp");
    if (!emailConfigProperties.isTls() && !emailConfigProperties.isSsl()) {
      log.error("awe.mail.oauth2-registration '{}' needs awe.mail.tls=true or awe.mail.ssl=true: every email of the default sender fails", registrationId);
    }
    if (emailConfigProperties.getUser() == null || emailConfigProperties.getUser().isBlank()) {
      log.error("awe.mail.oauth2-registration '{}' needs awe.mail.user (sender mailbox): every email of the default sender fails", registrationId);
    }
    if (emailOAuth2TokenService == null) {
      log.error("awe.mail.oauth2-registration '{}' cannot be used: OAuth2 is not available", registrationId);
    } else {
      try {
        emailOAuth2TokenService.checkRegistration(DEFAULT_SENDER_NAME, registrationId);
      } catch (AWException exc) {
        log.error("awe.mail.oauth2-registration '{}' cannot be used: {}", registrationId, exc.getMessage());
      }
    }
    return mailSender;
  }

  /**
   * Java Mail sender map
   *
   * @return Mail sender
   */
  @Bean
  @ConditionalOnMissingBean
  @DependsOnDatabaseInitialization
  public MailSenderFactory mailSenderFactory(JavaMailSender defaultMailSender, QueryService queryService,
                                             EncodeService encodeService, EmailOAuth2TokenService emailOAuth2TokenService) {
    return new MailSenderFactory(defaultMailSender, queryService, encodeService, emailOAuth2TokenService);
  }

  /**
   * OAuth2 access tokens of email servers (client_credentials registrations).
   * The authorized client manager works outside HTTP requests, calls the token endpoint with timeouts and is only
   * created when the application has OAuth2 client registrations. It is kept private to the email servers, so it does
   * not replace the authorized client manager Spring Security creates for the web requests.
   *
   * @param emailOAuth2RegistrationService Email OAuth2 registration service
   * @param clientRegistrationRepository   Client registration repository (only when OAuth2 registrations exist)
   * @return Email OAuth2 token service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EmailOAuth2TokenService emailOAuth2TokenService(EmailOAuth2RegistrationService emailOAuth2RegistrationService,
                                                         ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) {
    ClientRegistrationRepository registrationRepository = clientRegistrationRepository.getIfAvailable();
    if (registrationRepository == null) {
      return new EmailOAuth2TokenService(emailOAuth2RegistrationService, null, null);
    }
    return EmailOAuth2TokenService.create(emailOAuth2RegistrationService, registrationRepository);
  }

  /////////////////////////////////////////////
  // SERVICES
  /////////////////////////////////////////////
  /**
   * Email service
   * @param mailSenderFactory Mail sender factory
   * @param baseConfigProperties Base configuration properties
   * @param queryService Query Service
   * @param queryUtil Query utilities
   * @param userDAO  User DAO
   * @return Email service bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EmailService emailService(MailSenderFactory mailSenderFactory, BaseConfigProperties baseConfigProperties,
                                   QueryService queryService, QueryUtil queryUtil, UserDAOImpl userDAO) {
    return new EmailService(mailSenderFactory, baseConfigProperties, queryService, queryUtil, userDAO);
  }

  /////////////////////////////////////////////
  // CONNECTORS
  /////////////////////////////////////////////

  /**
   * Email Maintain connector
   * @param emailService Email service
   * @return Email Maintain connector bean
   */
  @Bean
  @ConditionalOnMissingBean
  public EmailMaintainConnector emailMaintainConnector(EmailService emailService) {
    return new EmailMaintainConnector(emailService);
  }
}
