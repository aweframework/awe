package com.almis.awe.security.authorization;

import com.almis.awe.config.AuthorizationMode;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.permissions.PermissionCatalog;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.model.security.audit.SecurityAuditCause;
import com.almis.awe.model.security.audit.SecurityAuditEventType;
import com.almis.awe.security.audit.SecurityAuditService;
import com.almis.awe.service.user.AweUserDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides if a maintain target, a query or a service launched by a client can be launched by the session user: a
 * target that declares a {@code permission} can only be launched when the user has every permission it lists, as a
 * {@code PERM_<id>} authority loaded at login. A target without {@code permission} is not checked, unless
 * {@code awe.security.permissions.require-undeclared} is enabled: then a private query requires {@code default.read}
 * and a private maintain target or a service requires {@code default.write}. Public and server-only targets and the
 * services authorized by their target never take a default permission.
 *
 * <p>It runs after the access by screen ({@link ScreenTargetAuthorizer}): the permission makes the check stricter, it
 * never opens what the access by screen closes. The launches of the REST API skip the access by screen, so there the
 * permission is the only check. The behaviour depends on {@code awe.security.authorization.mode}:
 * {@code off} does not check, {@code warn} logs and audits the launches without the permissions but allows them, and
 * {@code enforce} refuses them.</p>
 */
@Slf4j
public class PermissionTargetAuthorizer extends ServiceConfig implements TargetLaunchAuthorizer, Ordered {

  /**
   * Order of the authorizer in the chain: after the access by screen
   */
  public static final int ORDER = ScreenTargetAuthorizer.ORDER + 100;

  private static final String DEFAULT_READ = PermissionCatalog.DEFAULT_RESOURCE + PermissionCatalog.SEPARATOR + "read";
  private static final String DEFAULT_WRITE = PermissionCatalog.DEFAULT_RESOURCE + PermissionCatalog.SEPARATOR
    + PermissionCatalog.WRITE_VERB;

  private final SecurityConfigProperties securityConfigProperties;

  /**
   * Autowired constructor
   *
   * @param securityConfigProperties Security configuration properties
   */
  public PermissionTargetAuthorizer(SecurityConfigProperties securityConfigProperties) {
    this.securityConfigProperties = securityConfigProperties;
  }

  /**
   * Check if a client can launch a target. A launch without the permissions is logged and audited, and it is only
   * refused when the mode is {@code enforce}
   *
   * @param targetId Maintain, query or service identifier
   * @param kind     Kind of target
   * @return The launch must go on (false when it must be refused)
   */
  @Override
  public boolean authorize(String targetId, TargetKind kind) {
    AuthorizationMode mode = securityConfigProperties.getAuthorization().getMode();
    if (mode == null || mode == AuthorizationMode.OFF) {
      return true;
    }

    if (canLaunch(targetId, kind)) {
      return true;
    }

    // The permissions of the user are never logged nor audited
    log.warn("Client launch of {} [{}] without the permissions it requires{}", kind.label(), targetId,
      mode == AuthorizationMode.WARN ? " (allowed: authorization mode is warn)" : " refused");
    audit(targetId, kind, mode);
    return mode == AuthorizationMode.WARN;
  }

  /**
   * Check if the session user holds the permissions a target requires, whatever the authorization mode and without
   * logging nor auditing: a target that requires no permission, or unknown, can always be launched
   *
   * @param targetId Maintain, query or service identifier
   * @param kind     Kind of target
   * @return The user holds every permission of the target
   */
  public boolean canLaunch(String targetId, TargetKind kind) {
    String permission = getRequiredPermission(targetId, kind);
    return permission == null || hasAll(permission);
  }

  /**
   * Check if the session user holds every permission of a {@code permission} attribute, without logging nor auditing
   *
   * @param permission Permission identifiers separated by spaces
   * @return The user holds all of them
   */
  public boolean hasPermissions(String permission) {
    return hasAll(permission);
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  /**
   * Retrieve the permissions a target requires: its permission attribute or, when it declares none, the default
   * permission of its kind if require-undeclared is enabled. An unknown target is left to the services, which refuse it
   *
   * @return Permission identifiers separated by spaces, or null when the target requires none
   */
  private String getRequiredPermission(String targetId, TargetKind kind) {
    try {
      AweElements elements = getElements();
      return switch (kind) {
        case QUERY -> {
          Query query = elements.getQuery(targetId);
          yield query == null ? null : required(query.getPermission(),
            !query.isPublic() && !query.isServerOnly(), DEFAULT_READ);
        }
        case MAINTAIN -> {
          Target target = elements.getMaintain(targetId);
          yield target == null ? null : required(target.getPermission(),
            !target.isPublic() && !target.isServerOnly(), DEFAULT_WRITE);
        }
        case SERVICE -> {
          Service service = elements.getService(targetId);
          yield service == null ? null : required(service.getPermission(), !service.isAuthorizedByTarget(),
            DEFAULT_WRITE);
        }
      };
    } catch (AWException exc) {
      log.debug("{} {} not found", kind.label(), targetId, exc);
      return null;
    }
  }

  /**
   * The declared permission, or the default one when none is declared, the target can take it and require-undeclared
   * is enabled
   */
  private String required(String permission, boolean takesDefault, String defaultPermission) {
    if (permission != null && !permission.isBlank()) {
      return permission;
    }
    return takesDefault && securityConfigProperties.getPermissions().isRequireUndeclared() ? defaultPermission : null;
  }

  /**
   * Whether the user holds every permission of the attribute: a permission the catalog grants always needs no
   * authority, so it also holds for an authentication AWE did not build at login, but only for an authenticated user
   */
  private boolean hasAll(String permission) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Set<String> authorities = authentication == null ? Set.of()
      : authentication.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.toSet());
    Set<String> grantedAlways = isAuthenticatedUser(authentication)
      ? getElements().getPermissionCatalog().getGrantedAlways() : Set.of();
    return Arrays.stream(permission.trim().split("\\s+"))
      .allMatch(id -> grantedAlways.contains(id)
        || authorities.contains(AweUserDetailService.PERMISSION_AUTHORITY_PREFIX + id));
  }

  private static boolean isAuthenticatedUser(Authentication authentication) {
    return authentication != null && authentication.isAuthenticated()
      && !(authentication instanceof AnonymousAuthenticationToken);
  }

  /**
   * Audit the launch without access: the detail of a launch allowed by the warn mode says it, so the audit tells it
   * from a refusal
   */
  private void audit(String targetId, TargetKind kind, AuthorizationMode mode) {
    try {
      SecurityAuditService securityAuditService = getApplicationContext() == null ? null
        : getApplicationContext().getBeanProvider(SecurityAuditService.class).getIfAvailable();
      if (securityAuditService != null && securityAuditService.isEnabled()) {
        securityAuditService.failure(SecurityAuditEventType.ACCESS_DENIED, null, SecurityAuditCause.PERMISSION,
          kind.label() + ": " + targetId + (mode == AuthorizationMode.WARN ? " (allowed: warn)" : ""));
      }
    } catch (RuntimeException exc) {
      log.debug("Denied access to {} {} could not be audited", kind.label(), targetId, exc);
    }
  }
}
