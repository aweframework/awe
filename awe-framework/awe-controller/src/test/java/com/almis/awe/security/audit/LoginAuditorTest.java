package com.almis.awe.security.audit;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import com.almis.awe.security.authentication.LocalLoginDisabledException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginAuditorTest {

  private final RecordingSecurityAudit audit = new RecordingSecurityAudit();
  private final LoginAttemptService attempts = mock(LoginAttemptService.class);
  private final LoginAuditor auditor = new LoginAuditor(audit.service(), attempts);

  @Test
  void successIsRecordedAndResetsTheFailures() throws AWException {
    auditor.onSuccess("jdoe");

    assertThat(audit.events()).extracting("type", "outcome", "username", "cause")
      .containsExactly(tuple(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.SUCCESS, "jdoe", null));
    verify(attempts).registerSuccess("jdoe");
  }

  @Test
  void wrongPasswordIsRecordedAndCounted() throws AWException {
    auditor.onFailure("jdoe", new BadCredentialsException("bad"));

    assertThat(audit.events()).extracting("type", "outcome", "cause")
      .containsExactly(tuple(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS));
    verify(attempts).registerFailure("jdoe");
  }

  @Test
  void theFailureThatReachesTheLimitLocksTheAccount() throws AWException {
    when(attempts.registerFailure("jdoe")).thenReturn(true);

    auditor.onFailure("jdoe", new BadCredentialsException("bad"));

    assertThat(audit.events()).extracting("type", "outcome", "cause", "username").containsExactly(
      tuple(SecurityAuditEventType.LOGIN, SecurityAuditOutcome.FAILURE, SecurityAuditCause.BAD_CREDENTIALS, "jdoe"),
      tuple(SecurityAuditEventType.ACCOUNT_LOCKED, SecurityAuditOutcome.SUCCESS, SecurityAuditCause.MAX_ATTEMPTS, "jdoe"));
  }

  @Test
  void otherFailuresAreRecordedWithTheirCauseAndNotCounted() throws AWException {
    auditor.onFailure("a", new LockedException("locked"));
    auditor.onFailure("b", new DisabledException("disabled"));
    auditor.onFailure("c", new CredentialsExpiredException("expired"));
    auditor.onFailure("d", new LocalLoginDisabledException());

    assertThat(audit.events()).extracting("cause").containsExactly(SecurityAuditCause.LOCKED, SecurityAuditCause.DISABLED,
      SecurityAuditCause.CREDENTIALS_EXPIRED, SecurityAuditCause.LOCAL_LOGIN_DISABLED);
    verify(attempts, never()).registerFailure(anyString());
  }

  @Test
  void lockoutErrorsDoNotChangeTheLogin() throws AWException {
    when(attempts.registerFailure("jdoe")).thenThrow(new AWException("db", "down"));
    doThrow(new AWException("db", "down")).when(attempts).registerSuccess("jdoe");

    auditor.onFailure("jdoe", new BadCredentialsException("bad"));
    auditor.onSuccess("jdoe");

    assertThat(audit.events()).hasSize(2);
  }
}
