package com.almis.awe.security.authorization;

import com.almis.awe.config.ScreenAccessMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.service.menu.MenuOptionLookup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import java.util.Set;

/**
 * Decides if a maintain target or a query launched by a client can be launched by the session user: a target used by
 * the screens of some menu options can only be launched when the user can open at least one of those options. A target
 * that no screen uses is not checked.
 *
 * <p>The behaviour depends on {@code awe.security.screen-access}: {@code off} does not check, {@code warn} logs and
 * audits the launches without access but allows them, and {@code enforce} refuses them.</p>
 */
@Slf4j
public class ScreenTargetAuthorizer extends ServiceConfig {

  /**
   * Kind of target that is launched
   */
  public enum Kind {
    /**
     * Maintain target
     */
    MAINTAIN,
    /**
     * Query
     */
    QUERY;

    private String label() {
      return name().toLowerCase();
    }
  }

  private final SecurityConfigProperties securityConfigProperties;
  private final ScreenTargetIndex screenTargetIndex;
  private final MenuOptionLookup menuOptionLookup;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties Security configuration properties
   * @param screenTargetIndex        Index of the targets used by each option
   * @param menuOptionLookup         Menu option lookup
   */
  public ScreenTargetAuthorizer(SecurityConfigProperties securityConfigProperties, ScreenTargetIndex screenTargetIndex,
                                MenuOptionLookup menuOptionLookup) {
    this.securityConfigProperties = securityConfigProperties;
    this.screenTargetIndex = screenTargetIndex;
    this.menuOptionLookup = menuOptionLookup;
  }

  /**
   * Check if a client can launch a target with the authorizer of the application, when there is one
   *
   * @param context  Application context
   * @param targetId Maintain or query identifier
   * @param kind     Kind of target
   * @return The launch must go on (false when it must be refused)
   */
  public static boolean isAllowed(ApplicationContext context, String targetId, Kind kind) {
    ObjectProvider<ScreenTargetAuthorizer> provider = context == null ? null
      : context.getBeanProvider(ScreenTargetAuthorizer.class);
    ScreenTargetAuthorizer authorizer = provider == null ? null : provider.getIfAvailable();
    return authorizer == null || authorizer.authorize(targetId, kind);
  }

  /**
   * Check if a client can launch a target. A launch without access is logged and audited, and it is only refused
   * when the mode is {@code enforce}
   *
   * @param targetId Maintain or query identifier
   * @param kind     Kind of target
   * @return The launch must go on (false when it must be refused)
   */
  public boolean authorize(String targetId, Kind kind) {
    ScreenAccessMode mode = securityConfigProperties.getScreenAccess();
    if (mode == null || mode == ScreenAccessMode.OFF) {
      return true;
    }

    Set<String> options = screenTargetIndex.getOptionsUsing(targetId);
    if (options.isEmpty() || canOpenAny(options)) {
      return true;
    }

    // The user cannot open any option whose screen uses the target. The screens are not given away in the logs
    log.warn("Client launch of {} [{}] without access to any screen that uses it{}", kind.label(), targetId,
      mode == ScreenAccessMode.WARN ? " (allowed: screen-access is warn)" : " refused");
    audit(targetId, kind);
    return mode == ScreenAccessMode.WARN;
  }

  private boolean canOpenAny(Set<String> options) {
    boolean authenticated = isAuthenticated();
    for (String option : options) {
      try {
        if (authenticated && menuOptionLookup.isAvailableOption(option, AweConstants.PRIVATE_MENU)
          || menuOptionLookup.isAvailableOption(option, AweConstants.PUBLIC_MENU)) {
          return true;
        }
      } catch (Exception exc) {
        log.warn("Option [{}] could not be checked: {}", option, exc.getMessage());
      }
    }
    return false;
  }

  private boolean isAuthenticated() {
    AweSession session = getSession();
    return session != null && session.isAuthenticated();
  }

  private void audit(String targetId, Kind kind) {
    try {
      SecurityAuditService securityAuditService = getApplicationContext() == null ? null
        : getApplicationContext().getBeanProvider(SecurityAuditService.class).getIfAvailable();
      if (securityAuditService != null && securityAuditService.isEnabled()) {
        securityAuditService.failure(SecurityAuditEventType.ACCESS_DENIED, null, SecurityAuditCause.RESTRICTED,
          kind.label() + ": " + targetId);
      }
    } catch (RuntimeException exc) {
      log.debug("Denied access to {} {} could not be audited", kind.label(), targetId, exc);
    }
  }
}
