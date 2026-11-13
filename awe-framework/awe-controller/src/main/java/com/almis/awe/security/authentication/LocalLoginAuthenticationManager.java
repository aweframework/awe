package com.almis.awe.security.authentication;

import com.almis.awe.security.audit.LoginAuditor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.UUID;

/**
 * Authentication manager decorator that applies the {@link LocalLoginPolicy} to local username and password logins.
 *
 * <p>It is only wired to the login entry points (standard form login, {@code /action/login} and
 * {@code /api/authenticate}), never to the shared authentication manager that internal flows use.</p>
 *
 * <ul>
 *   <li>When the policy is inactive, or the authentication is not a user name and password login with a text user
 *   name, it is passed to the delegate untouched.</li>
 *   <li>A break-glass user is authenticated with the normalised (trimmed) name that was checked; the details are kept.</li>
 *   <li>A refused user still goes through the delegate, but with a throw-away password, so a refusal costs the same as
 *   a wrong password (the password hash is computed), it can never succeed and the delegate publishes the same bad
 *   credentials failure event as for a wrong password. The same bad credentials failure is then thrown.</li>
 * </ul>
 */
public class LocalLoginAuthenticationManager implements AuthenticationManager {

  private final AuthenticationManager delegate;
  private final LocalLoginPolicy localLoginPolicy;
  private LoginAuditor loginAuditor;

  /**
   * Create the decorator
   *
   * @param delegate         Real authentication manager
   * @param localLoginPolicy Local login policy
   */
  public LocalLoginAuthenticationManager(AuthenticationManager delegate, LocalLoginPolicy localLoginPolicy) {
    this.delegate = delegate;
    this.localLoginPolicy = localLoginPolicy;
  }

  /**
   * Get the policy used by this manager
   *
   * @return Local login policy
   */
  public LocalLoginPolicy getLocalLoginPolicy() {
    return localLoginPolicy;
  }

  /**
   * Record the logins of the entry points in the security audit (and apply the automatic lockout)
   *
   * @param loginAuditor Login auditor, null to record nothing
   */
  public void setLoginAuditor(LoginAuditor loginAuditor) {
    this.loginAuditor = loginAuditor;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    if (loginAuditor == null) {
      return authenticateWithPolicy(authentication);
    }
    String username = authentication.getPrincipal() instanceof String name ? name.trim() : authentication.getName();
    Authentication result;
    try {
      result = authenticateWithPolicy(authentication);
    } catch (AuthenticationException failure) {
      loginAuditor.onFailure(username, failure);
      throw failure;
    }
    loginAuditor.onSuccess(result.getName());
    return result;
  }

  private Authentication authenticateWithPolicy(Authentication authentication) {
    if (!localLoginPolicy.isActive()
      || !(authentication instanceof UsernamePasswordAuthenticationToken)
      || !(authentication.getPrincipal() instanceof String username)) {
      return delegate.authenticate(authentication);
    }

    try {
      String checked = localLoginPolicy.check(username);
      return delegate.authenticate(checked.equals(username) ? authentication : normalise(authentication, checked));
    } catch (LocalLoginDisabledException refused) {
      return failLikeBadCredentials(authentication, refused);
    }
  }

  private Authentication failLikeBadCredentials(Authentication authentication, LocalLoginDisabledException refused) {
    try {
      // Same work as for a wrong password (password hash included); it cannot succeed with a throw-away password
      delegate.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(authentication.getPrincipal(), UUID.randomUUID().toString()));
    } catch (AuthenticationException ignored) {
      // Expected: the delegate reports (and publishes) a failure, like with a wrong password
    }
    throw refused;
  }

  private Authentication normalise(Authentication authentication, String checked) {
    UsernamePasswordAuthenticationToken normalised = UsernamePasswordAuthenticationToken.unauthenticated(checked, authentication.getCredentials());
    normalised.setDetails(authentication.getDetails());
    return normalised;
  }
}
