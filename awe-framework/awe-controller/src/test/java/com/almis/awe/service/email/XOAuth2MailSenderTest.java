package com.almis.awe.service.email;

import com.almis.awe.exception.AWException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XOAuth2MailSenderTest {

  private final EmailOAuth2TokenService tokenService = mock(EmailOAuth2TokenService.class);
  private final Transport transport = mock(Transport.class);
  private final XOAuth2MailSender sender = new XOAuth2MailSender("Mail", "office365", tokenService) {
    @Override
    protected Transport getTransport(Session session) {
      return transport;
    }
  };

  @Test
  void usesXOAuth2WithTheSenderMailbox() {
    sender.setUsername("sender@almis.com");

    assertThat(sender.getJavaMailProperties())
      .containsEntry("mail.smtp.auth", "true")
      .containsEntry("mail.smtp.auth.mechanisms", "XOAUTH2");
    assertThat(sender.getUsername()).isEqualTo("sender@almis.com");
    assertThat(sender.getPassword()).isNull();
  }

  @Test
  void eachConnectionUsesAFreshAccessTokenAsPassword() throws Exception {
    sender.getJavaMailProperties().setProperty("mail.smtp.starttls.required", "true");
    sender.setHost("smtp.office365.com");
    sender.setPort(587);
    sender.setUsername("sender@almis.com");
    when(tokenService.getAccessToken("Mail", "office365")).thenReturn("token-1", "token-2");

    sender.connectTransport();
    sender.connectTransport();

    verify(transport).connect("smtp.office365.com", 587, "sender@almis.com", "token-1");
    verify(transport).connect("smtp.office365.com", 587, "sender@almis.com", "token-2");
  }

  @Test
  void tokenRejectedByTheServerIsRemovedFromTheCacheAndTheErrorIsKept() throws Exception {
    sender.getJavaMailProperties().setProperty("mail.smtp.ssl.enable", "true");
    sender.setHost("smtp.office365.com");
    sender.setPort(465);
    sender.setUsername("sender@almis.com");
    when(tokenService.getAccessToken("Mail", "office365")).thenReturn("rejected-token");
    AuthenticationFailedException rejected = new AuthenticationFailedException("535 5.7.3 Authentication unsuccessful");
    doThrow(rejected).when(transport).connect("smtp.office365.com", 465, "sender@almis.com", "rejected-token");

    assertThatThrownBy(sender::connectTransport).isSameAs(rejected);
    verify(tokenService).removeAccessToken("Mail", "office365");
    verify(tokenService).getAccessToken("Mail", "office365");
  }

  @Test
  void tokenErrorFailsTheConnectionAsAuthenticationError() throws Exception {
    sender.getJavaMailProperties().setProperty("mail.smtp.starttls.required", "true");
    sender.setUsername("sender@almis.com");
    when(tokenService.getAccessToken("Mail", "office365")).thenThrow(new AWException("Email server 'Mail' token error"));

    assertThatThrownBy(sender::connectTransport)
      .isInstanceOf(AuthenticationFailedException.class)
      .hasMessage("Email server 'Mail' token error");
    verify(transport, never()).connect(anyString(), anyInt(), anyString(), anyString());
  }

  @Test
  void senderWithoutMailboxNeverRequestsAToken() throws Exception {
    sender.getJavaMailProperties().setProperty("mail.smtp.starttls.required", "true");

    assertThatThrownBy(sender::connectTransport)
      .isInstanceOf(AuthenticationFailedException.class)
      .hasMessage("Email server 'Mail' uses OAuth2 authentication without a sender mailbox");
    verify(tokenService, never()).getAccessToken(anyString(), anyString());
  }
}
