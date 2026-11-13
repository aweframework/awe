package com.almis.awe.security.audit;

import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.security.authentication.LocalLoginDisabledException;
import com.almis.awe.security.authentication.SsoRoleNotMappedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * Maps authentication failures to security audit causes
 */
public final class SecurityAuditCauses {

  private SecurityAuditCauses() {
  }

  /**
   * Cause of an authentication failure
   *
   * @param exception Authentication failure
   * @return Cause
   */
  public static SecurityAuditCause of(AuthenticationException exception) {
    return switch (exception) {
      case LocalLoginDisabledException ignored -> SecurityAuditCause.LOCAL_LOGIN_DISABLED;
      case BadCredentialsException ignored -> SecurityAuditCause.BAD_CREDENTIALS;
      case UsernameNotFoundException ignored -> SecurityAuditCause.BAD_CREDENTIALS;
      case LockedException ignored -> SecurityAuditCause.LOCKED;
      case DisabledException ignored -> SecurityAuditCause.DISABLED;
      case AccountExpiredException ignored -> SecurityAuditCause.ACCOUNT_EXPIRED;
      case CredentialsExpiredException ignored -> SecurityAuditCause.CREDENTIALS_EXPIRED;
      case SsoRoleNotMappedException ignored -> SecurityAuditCause.ROLE_NOT_MAPPED;
      case OAuth2AuthenticationException ignored -> SecurityAuditCause.PROVIDER_ERROR;
      case null, default -> SecurityAuditCause.ERROR;
    };
  }
}
