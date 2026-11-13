package com.almis.awe.security.authentication;

import com.almis.awe.config.SecurityConfigProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides whether a local (username and password) login attempt is allowed.
 *
 * <p>Local login is open unless SSO is enabled and {@code awe.security.sso.local-login} is {@code false}. In that case
 * only the users listed in {@code awe.security.sso.break-glass-users} may still sign in locally. User names are
 * trimmed and compared exactly (case-sensitive, the same way the user is later looked up by name). Every break-glass
 * use and every refusal is logged at WARN level (sanitized user name only, never credentials). A refusal is reported
 * to the caller as a plain bad credentials failure.</p>
 */
@Slf4j
public class LocalLoginPolicy {

  private static final int MAX_LOGGED_NAME_LENGTH = 64;

  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Create the policy
   *
   * @param securityConfigProperties Security properties
   */
  public LocalLoginPolicy(SecurityConfigProperties securityConfigProperties) {
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Check if the policy restricts local logins at all (SSO enabled and local login switched off)
   *
   * @return true when local logins are restricted to break-glass users
   */
  public boolean isActive() {
    SecurityConfigProperties.Sso sso = securityConfigProperties.getSso();
    return sso != null && sso.isEnabled() && !sso.isLocalLogin();
  }

  /**
   * Check if a local login is allowed for a user name
   *
   * @param username User name sent in the login request
   * @return The normalised (trimmed) user name that was checked, null when none was sent
   * @throws LocalLoginDisabledException when local login is switched off and the user is not a break-glass user
   */
  public String check(String username) {
    String name = username == null ? null : username.trim();
    SecurityConfigProperties.Sso sso = securityConfigProperties.getSso();
    if (!isActive()) {
      return name;
    }

    if (name != null && !name.isEmpty() && breakGlassUsers(sso).contains(name)) {
      log.warn("[break-glass] Local login attempt by break-glass user {} while local login is disabled", sanitize(name));
      return name;
    }

    log.warn("Local login refused for user {}: local login is disabled (awe.security.sso.local-login=false)", sanitize(name));
    throw new LocalLoginDisabledException();
  }

  private Set<String> breakGlassUsers(SecurityConfigProperties.Sso sso) {
    return sso.getBreakGlassUsers().stream()
      .filter(user -> user != null && !user.isBlank())
      .map(String::trim)
      .collect(Collectors.toSet());
  }

  /**
   * Make a user name safe to log: control characters are replaced and the length is capped
   */
  private String sanitize(String name) {
    if (name == null) {
      return "<none>";
    }
    String clean = name.replaceAll("\\p{Cntrl}", "_");
    return clean.length() > MAX_LOGGED_NAME_LENGTH ? clean.substring(0, MAX_LOGGED_NAME_LENGTH) + "..." : clean;
  }
}
