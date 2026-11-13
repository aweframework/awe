package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.UserDAOImpl;
import com.almis.awe.factory.MailSenderFactory;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.email.EmailOAuth2RegistrationService;
import com.almis.awe.service.email.EmailOAuth2TokenService;
import com.almis.awe.service.email.XOAuth2MailSender;
import jakarta.mail.AuthenticationFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Test for EmailConfig class
 */
@ExtendWith(OutputCaptureExtension.class)
class EmailConfigTest {

  private static final ClientRegistrationRepository REGISTRATIONS = new InMemoryClientRegistrationRepository(
    ClientRegistration.withRegistrationId("office365")
      .clientId("client-id")
      .clientSecret("client-secret")
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .tokenUri("https://provider.com/oauth/token")
      .build(),
    ClientRegistration.withRegistrationId("login")
      .clientId("client-id")
      .clientSecret("client-secret")
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
      .authorizationUri("https://provider.com/oauth/authorize")
      .tokenUri("https://provider.com/oauth/token")
      .build());

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(EmailConfig.class))
    .withPropertyValues("awe.mail.enabled=true", "awe.application.module-list=awe")
    .withBean(BaseConfigProperties.class)
    .withBean(QueryService.class, () -> mock(QueryService.class))
    .withBean(QueryUtil.class, () -> mock(QueryUtil.class))
    .withBean(UserDAOImpl.class, () -> mock(UserDAOImpl.class))
    .withBean(EncodeService.class, () -> mock(EncodeService.class));

  @Test
  void authorizedClientManagerIsOnlyCreatedWhenTheApplicationHasClientRegistrations() {
    ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("office365")
      .clientId("client-id")
      .clientSecret("client-secret")
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .tokenUri("https://provider.com/oauth/token")
      .build());

    contextRunner
      .withBean(ClientRegistrationRepository.class, () -> repository)
      .withBean(EmailOAuth2RegistrationService.class, () -> new EmailOAuth2RegistrationService(repository))
      .run(context -> assertThatCode(() -> context.getBean(EmailOAuth2TokenService.class).checkRegistration("Mail", "office365"))
        .doesNotThrowAnyException());

    // Without a ClientRegistrationRepository bean, mail starts without authorized client manager
    contextRunner
      .withBean(EmailOAuth2RegistrationService.class, () -> new EmailOAuth2RegistrationService(repository))
      .run(context -> {
        assertThat(context).hasNotFailed().hasSingleBean(MailSenderFactory.class);
        assertThatThrownBy(() -> context.getBean(EmailOAuth2TokenService.class).checkRegistration("Mail", "office365"))
          .hasMessageContaining("no authorized client manager");
      });
  }

  @Test
  void emailAuthorizedClientManagerIsNotExposedAsBean() {
    // A OAuth2AuthorizedClientManager bean would replace the one Spring Security creates for the web requests
    ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("office365")
      .clientId("client-id")
      .clientSecret("client-secret")
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .tokenUri("https://provider.com/oauth/token")
      .build());

    contextRunner
      .withBean(ClientRegistrationRepository.class, () -> repository)
      .withBean(EmailOAuth2RegistrationService.class, () -> new EmailOAuth2RegistrationService(repository))
      .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(OAuth2AuthorizedClientManager.class));
  }

  @Test
  void defaultSenderWithoutOAuth2RegistrationIsUnchanged() {
    contextRunner
      .withPropertyValues("awe.mail.host=smtp.almis.com", "awe.mail.port=587", "awe.mail.auth=true", "awe.mail.user=user",
        "awe.mail.pass=pass", "awe.mail.tls=true")
      .withBean(EmailOAuth2RegistrationService.class, () -> new EmailOAuth2RegistrationService(null))
      .run(context -> assertThat(context.getBean(JavaMailSender.class)).isExactlyInstanceOf(JavaMailSenderImpl.class)
        .satisfies(sender -> {
          JavaMailSenderImpl mailSender = (JavaMailSenderImpl) sender;
          assertThat(mailSender.getHost()).isEqualTo("smtp.almis.com");
          assertThat(mailSender.getPort()).isEqualTo(587);
          assertThat(mailSender.getUsername()).isEqualTo("user");
          assertThat("pass".equals(mailSender.getPassword())).as("password is set").isTrue();
          assertThat(mailSender.getJavaMailProperties()).isEqualTo(properties(Map.of("mail.smtp.localhost", "localhost",
            "mail.debug", false, "mail.smtp.starttls.enable", true, "mail.smtp.ssl.enable", false)));
        }));
  }

  @ParameterizedTest
  @CsvSource({"awe.mail.tls=true,mail.smtp.starttls.required", "awe.mail.ssl=true,mail.smtp.ssl.enable"})
  void defaultSenderWithOAuth2RegistrationSendsWithXOAuth2AsTheSenderMailbox(String tlsProperty, String tlsMailProperty) {
    contextRunner
      .withPropertyValues("awe.mail.oauth2-registration=office365", "awe.mail.host=smtp.office365.com", "awe.mail.port=587",
        "awe.mail.user=sender@almis.com", "awe.mail.pass=unused", tlsProperty)
      .withBean(ClientRegistrationRepository.class, () -> REGISTRATIONS)
      .withBean(EmailOAuth2RegistrationService.class, () -> new EmailOAuth2RegistrationService(REGISTRATIONS))
      .run(context -> assertThat(context.getBean(JavaMailSender.class)).isInstanceOfSatisfying(XOAuth2MailSender.class, sender -> {
        assertThat(sender.getServerName()).isEqualTo("awe.mail");
        assertThat(sender.getRegistrationId()).isEqualTo("office365");
        assertThat(sender.getTokenService()).isSameAs(context.getBean(EmailOAuth2TokenService.class));
        assertThat(sender.getHost()).isEqualTo("smtp.office365.com");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getUsername()).isEqualTo("sender@almis.com");
        assertThat(sender.getPassword()).as("awe.mail.pass is not used").isNull();
        assertThat(sender.getJavaMailProperties())
          .containsEntry("mail.smtp.auth", "true")
          .containsEntry("mail.smtp.auth.mechanisms", "XOAUTH2")
          .containsEntry(tlsMailProperty, "true");
      }));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
    "office365|awe.mail.tls=false|awe.mail.user=sender@almis.com|true|uses OAuth2 authentication without STARTTLS or SSL",
    "office365|awe.mail.tls=true|awe.mail.user=|true|uses OAuth2 authentication without a sender mailbox",
    "missing|awe.mail.tls=true|awe.mail.user=sender@almis.com|true|uses an unknown OAuth2 client registration 'missing'",
    "login|awe.mail.tls=true|awe.mail.user=sender@almis.com|true|uses OAuth2 client registration 'login', which is not client_credentials",
    "office365|awe.mail.tls=true|awe.mail.user=sender@almis.com|false|no authorized client manager"})
  void misconfiguredOAuth2DefaultSenderStartsAndEverySendFailsWithoutAToken(String registration, String tlsProperty, String userProperty,
                                                                          boolean registrationRepository, String error, CapturedOutput output) {
    ApplicationContextRunner runner = contextRunner
      .withPropertyValues("awe.mail.oauth2-registration=" + registration, "awe.mail.host=smtp.office365.com", tlsProperty, userProperty)
      .withBean(EmailOAuth2RegistrationService.class, () -> new EmailOAuth2RegistrationService(REGISTRATIONS));
    if (registrationRepository) {
      runner = runner.withBean(ClientRegistrationRepository.class, () -> REGISTRATIONS);
    }

    // Every message fails before asking for a token: no fallback to a sender without OAuth2
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThatThrownBy(((JavaMailSenderImpl) context.getBean(JavaMailSender.class))::testConnection)
        .isInstanceOf(AuthenticationFailedException.class)
        .hasMessageContaining(error);
    });
    assertThat(output).contains("ERROR").contains("awe.mail.oauth2-registration").contains("'" + registration + "'")
      .doesNotContain("client-secret");
  }

  private static Properties properties(Map<String, Object> values) {
    Properties properties = new Properties();
    properties.putAll(values);
    return properties;
  }
}
