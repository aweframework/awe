package com.almis.awe.security.authentication;

import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.security.audit.LoginAuditor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Security audit of the logins that go through the local login entry points
 */
class LocalLoginAuthenticationManagerAuditTest {

  private final AuthenticationManager delegate = mock(AuthenticationManager.class);
  private final LoginAuditor auditor = mock(LoginAuditor.class);
  private final SecurityConfigProperties properties = new SecurityConfigProperties();
  private LocalLoginAuthenticationManager manager;

  @BeforeEach
  void setUp() {
    manager = new LocalLoginAuthenticationManager(delegate, new LocalLoginPolicy(properties));
    manager.setLoginAuditor(auditor);
  }

  @Test
  void aSuccessfulLoginIsRecordedWithTheAuthenticatedName() {
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken("jdoe", "secret");
    Authentication result = UsernamePasswordAuthenticationToken.authenticated("JDOE", null, List.of());
    when(delegate.authenticate(token)).thenReturn(result);

    assertThat(manager.authenticate(token)).isSameAs(result);
    verify(auditor).onSuccess("JDOE");
    verify(auditor, never()).onFailure(any(), any());
  }

  @Test
  void aFailedLoginIsRecordedWithTheTypedNameAndRethrown() {
    BadCredentialsException failure = new BadCredentialsException("bad");
    when(delegate.authenticate(any())).thenThrow(failure);

    assertThatThrownBy(() -> manager.authenticate(new UsernamePasswordAuthenticationToken(" jdoe ", "wrong")))
      .isSameAs(failure);
    verify(auditor).onFailure("jdoe", failure);
    verify(auditor, never()).onSuccess(any());
  }

  @Test
  void aLoginRefusedByTheLocalLoginPolicyIsRecorded() {
    properties.getSso().setEnabled(true);
    properties.getSso().setLocalLogin(false);
    when(delegate.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(() -> manager.authenticate(new UsernamePasswordAuthenticationToken("jdoe", "secret")))
      .isInstanceOf(LocalLoginDisabledException.class);
    verify(auditor).onFailure(eq("jdoe"), isA(LocalLoginDisabledException.class));
  }

  @Test
  void withoutAuditorNothingIsRecorded() {
    LocalLoginAuthenticationManager plain = new LocalLoginAuthenticationManager(delegate, new LocalLoginPolicy(properties));
    when(delegate.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(() -> plain.authenticate(new UsernamePasswordAuthenticationToken("jdoe", "wrong")))
      .isInstanceOf(BadCredentialsException.class);
  }
}
