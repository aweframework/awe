package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEvent;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.model.security.audit.SecurityAuditOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;

/**
 * Records local logins (form, {@code /action/login} and {@code /api/authenticate}) and applies the automatic lockout
 * to wrong passwords. A failure of the lockout never changes the result of the login.
 */
@Slf4j
public class LoginAuditor {

  private final SecurityAuditService securityAuditService;
  private final LoginAttemptService loginAttemptService;

  /**
   * Constructor
   *
   * @param securityAuditService Security audit service
   * @param loginAttemptService  Lockout service
   */
  public LoginAuditor(SecurityAuditService securityAuditService, LoginAttemptService loginAttemptService) {
    this.securityAuditService = securityAuditService;
    this.loginAttemptService = loginAttemptService;
  }

  /**
   * A local login succeeded
   *
   * @param username User name
   */
  public void onSuccess(String username) {
    securityAuditService.success(SecurityAuditEventType.LOGIN, username, null);
    try {
      loginAttemptService.registerSuccess(username);
    } catch (Exception exc) {
      log.warn("Failed login count of user {} could not be reset", sanitize(username), exc);
    }
  }

  /**
   * A local login failed
   *
   * @param username  User name as typed
   * @param exception Failure
   */
  public void onFailure(String username, AuthenticationException exception) {
    SecurityAuditCause cause = SecurityAuditCauses.of(exception);
    securityAuditService.failure(SecurityAuditEventType.LOGIN, username, cause, null);
    if (cause != SecurityAuditCause.BAD_CREDENTIALS) {
      return;
    }
    try {
      if (loginAttemptService.registerFailure(username)) {
        securityAuditService.publish(SecurityAuditEvent.builder()
          .type(SecurityAuditEventType.ACCOUNT_LOCKED)
          .outcome(SecurityAuditOutcome.SUCCESS)
          .cause(SecurityAuditCause.MAX_ATTEMPTS)
          .username(username)
          .build());
      }
    } catch (Exception exc) {
      log.warn("Failed login of user {} could not be counted for the automatic lockout", sanitize(username), exc);
    }
  }

  private static String sanitize(String value) {
    return value == null ? null : value.replaceAll("\\p{Cntrl}", "_");
  }
}
