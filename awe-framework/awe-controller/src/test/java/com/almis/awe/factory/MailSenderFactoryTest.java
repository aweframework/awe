package com.almis.awe.factory;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.EmailServer;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.EmailSecurityMode;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.EncodeService;
import com.almis.awe.service.QueryService;
import com.almis.awe.service.email.EmailOAuth2TokenService;
import com.almis.awe.service.email.XOAuth2MailSender;
import jakarta.mail.AuthenticationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSenderFactoryTest {

  private static final String SERVER_NAME = "EmailServer";

  private MailSenderFactory mailSenderFactory;

  @Mock
  private QueryService queryService;

  @Mock
  private EncodeService encodeService;

  @Mock
  private JavaMailSender defaultSender;

  @Mock
  private EmailOAuth2TokenService tokenService;

  @BeforeEach
  void createFactory() throws AWException {
    mailSenderFactory = new MailSenderFactory(defaultSender, queryService, encodeService, tokenService);
    lenient().when(encodeService.decryptRipEmd160("encrypted-pass")).thenReturn("pass");
  }

  @Test
  void init() throws Exception {
    givenServer(new EmailServer().setAuthenticated(true).setUser("user").setPass("encrypted-pass").setHost("localhost"));

    assertEquals(defaultSender, mailSenderFactory.getMailSender());
    assertNotEquals(defaultSender, mailSenderFactory.getMailSender(SERVER_NAME));
  }

  @Test
  void initException() throws Exception {
    when(queryService.launchPrivateQuery(anyString())).thenThrow(AWException.class);

    mailSenderFactory.init();

    assertEquals(defaultSender, mailSenderFactory.getMailSender());
    assertEquals(defaultSender, mailSenderFactory.getMailSender(SERVER_NAME));
  }

  @Test
  void existingAuthenticatedServerKeepsPort25WithoutTlsAndUsesItsDecryptedCredentials() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setAuthentication(1).setUser("user").setPass("encrypted-pass"));

    assertThat(sender.getHost()).isEqualTo("localhost");
    assertThat(sender.getPort()).isEqualTo(25);
    assertThat(sender.getJavaMailProperties()).doesNotContainKeys("mail.smtp.starttls.enable", "mail.smtp.ssl.enable");
    assertThat(sender.getJavaMailProperties()).containsEntry("mail.smtp.auth", "true");
    assertThat(sender.getUsername()).isEqualTo("user");
    assertThat("pass".equals(sender.getPassword())).as("password is decrypted").isTrue();
  }

  @Test
  void serverWithoutAuthenticationHasNoCredentials() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setAuthentication(0).setUser("user").setPass("encrypted-pass"));

    assertThat(sender.getJavaMailProperties()).doesNotContainKey("mail.smtp.auth");
    assertThat(sender.getUsername()).isNull();
    assertThat(sender.getPassword()).isNull();
  }

  @Test
  void passwordThatCannotBeDecryptedIsUsedAsStored() throws Exception {
    when(encodeService.decryptRipEmd160("plain-pass")).thenThrow(new AWException("Decrypt error"));

    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setAuthentication(1).setUser("user").setPass("plain-pass"));

    assertThat("plain-pass".equals(sender.getPassword())).as("stored password is kept").isTrue();
  }

  @Test
  void serverWithUnknownAuthenticationIsLeftOutAndTheOthersAreKept() throws Exception {
    DataList dataList = DataListUtil.fromBeanList(List.of(
      new EmailServer().setName("Unknown").setHost("localhost").setAuthentication(9),
      new EmailServer().setName(SERVER_NAME).setHost("localhost")));
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));

    mailSenderFactory.init();

    assertEquals(defaultSender, mailSenderFactory.getMailSender("Unknown"));
    assertNotEquals(defaultSender, mailSenderFactory.getMailSender(SERVER_NAME));
  }

  @Test
  void serverRowThatCannotBeReadIsLeftOutAndTheOthersAreKept() throws Exception {
    DataList dataList = DataList.builder().build();
    dataList.addRow(Map.of("name", new CellData("Bad"), "host", new CellData("localhost"), "securityMode", new CellData("TLS")));
    dataList.addRow(Map.of("name", new CellData(SERVER_NAME), "host", new CellData("localhost"), "securityMode", new CellData("STARTTLS")));
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));

    mailSenderFactory.init();

    assertEquals(defaultSender, mailSenderFactory.getMailSender("Bad"));
    assertThat(((JavaMailSenderImpl) mailSenderFactory.getMailSender(SERVER_NAME)).getPort()).isEqualTo(587);
  }

  @ParameterizedTest
  @CsvSource({"NONE,25", "STARTTLS,587", "SSL,465"})
  void serverWithoutPortUsesTheSecurityModeDefaultPort(EmailSecurityMode securityMode, int expectedPort) throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setSecurityMode(securityMode));

    assertThat(sender.getPort()).isEqualTo(expectedPort);
  }

  @Test
  void serverPortIsApplied() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setSecurityMode(EmailSecurityMode.STARTTLS).setPort(2525));

    assertThat(sender.getPort()).isEqualTo(2525);
  }

  @Test
  void startTlsServerRequiresStartTls() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setSecurityMode(EmailSecurityMode.STARTTLS));

    assertThat(sender.getJavaMailProperties())
      .containsEntry("mail.smtp.starttls.enable", "true")
      .containsEntry("mail.smtp.starttls.required", "true")
      .doesNotContainKey("mail.smtp.ssl.enable");
  }

  @Test
  void sslServerUsesImplicitTls() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setSecurityMode(EmailSecurityMode.SSL));

    assertThat(sender.getJavaMailProperties())
      .containsEntry("mail.smtp.ssl.enable", "true")
      .doesNotContainKey("mail.smtp.starttls.enable");
  }

  @Test
  void oauth2ServerSendsWithXOAuth2AsTheSenderMailboxThroughItsRegistration() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("smtp.office365.com").setSecurityMode(EmailSecurityMode.STARTTLS)
      .setAuthentication(2).setUser("sender@almis.com").setPass("encrypted-pass").setOauthRegistration("office365"));

    assertThat(sender).isInstanceOfSatisfying(XOAuth2MailSender.class, xoauth2Sender -> {
      assertThat(xoauth2Sender.getServerName()).isEqualTo(SERVER_NAME);
      assertThat(xoauth2Sender.getRegistrationId()).isEqualTo("office365");
    });
    assertThat(sender.getJavaMailProperties())
      .containsEntry("mail.smtp.auth", "true")
      .containsEntry("mail.smtp.auth.mechanisms", "XOAUTH2")
      .containsEntry("mail.smtp.starttls.required", "true");
    assertThat(sender.getUsername()).isEqualTo("sender@almis.com");
    assertThat(sender.getPassword()).as("stored password is not used").isNull();
  }

  @Test
  void oauth2ServerWithInvalidRegistrationIsNotReplacedByTheDefaultSender() throws Exception {
    doThrow(new AWException("Email server 'EmailServer' uses an unknown OAuth2 client registration 'missing'"))
      .when(tokenService).checkRegistration(SERVER_NAME, "missing");

    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setAuthentication(2).setUser("sender@almis.com").setOauthRegistration("missing"));

    assertThat(sender).isInstanceOf(XOAuth2MailSender.class);
  }

  @Test
  void oauth2ServerWithoutTlsKeepsItsSenderAndNeverSendsAToken() throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setSecurityMode(EmailSecurityMode.NONE)
      .setAuthentication(2).setUser("sender@almis.com").setOauthRegistration("office365"));

    assertThat(sender).isInstanceOf(XOAuth2MailSender.class);
    assertThatThrownBy(sender::testConnection)
      .isInstanceOf(AuthenticationFailedException.class)
      .hasMessage("Email server 'EmailServer' uses OAuth2 authentication without STARTTLS or SSL");
    verify(tokenService, never()).getAccessToken(anyString(), anyString());
  }

  @ParameterizedTest
  @CsvSource({"0", "1", "2"})
  void sslServerPropertiesMatchTheProtocolOfItsSender(int authentication) throws Exception {
    JavaMailSenderImpl sender = givenServer(new EmailServer().setHost("localhost").setSecurityMode(EmailSecurityMode.SSL)
      .setAuthentication(authentication).setUser("sender@almis.com").setPass("encrypted-pass").setOauthRegistration("office365"));

    // SSL is implicit TLS on the smtp protocol (mail.smtp.ssl.enable), so every property uses the mail.smtp prefix
    assertThat(sender.getProtocol()).isEqualTo("smtp");
    assertThat(sender.getJavaMailProperties().stringPropertyNames()).isNotEmpty().allMatch(key -> key.startsWith("mail.smtp."));
  }

  private JavaMailSenderImpl givenServer(EmailServer emailServer) throws AWException {
    DataList dataList = DataListUtil.fromBeanList(Collections.singletonList(emailServer.setName(SERVER_NAME)));
    when(queryService.launchPrivateQuery(anyString())).thenReturn(new ServiceData().setDataList(dataList));
    mailSenderFactory.init();
    return (JavaMailSenderImpl) mailSenderFactory.getMailSender(SERVER_NAME);
  }
}
