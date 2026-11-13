package com.almis.awe.security.audit;

/**
 * Resolves the tenant stored on each security audit event
 */
@FunctionalInterface
public interface SecurityAuditTenantResolver {

  /**
   * Current tenant
   *
   * @return Tenant, or null when unknown
   */
  String resolveTenant();
}
