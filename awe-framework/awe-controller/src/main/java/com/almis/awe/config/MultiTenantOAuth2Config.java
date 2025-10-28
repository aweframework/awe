package com.almis.awe.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Simplified multi-tenant configuration
 * Maps subdomains to OAuth2 registration IDs (provider names)
 */
@ConfigurationProperties(prefix = "awe.security.sso.multitenant")
@Data
@Slf4j
public class MultiTenantOAuth2Config {

	/**
	 * Enable SSO multitenant.
	 */
	private boolean enabled = false;

	/**
	 * Default tenant (used when no subdomain is detected)
	 */
	private String defaultTenant = "default";

	/**
	 * Optional: Explicit mapping from tenant (subdomain) to registration ID
	 * If empty, convention is used: tenant name = registration ID
	 * Example: "acme" -> "keycloak-acme"
	 */
	private Map<String, String> tenantMappings = new HashMap<>();

	/**
	 * Gets OAuth2 registration ID for a tenant
	 *
	 * @param tenant The tenant identifier (subdomain)
	 * @return The registration ID to use for OAuth2 authentication
	 */
	public String getRegistrationId(String tenant) {
		if (tenant == null || tenant.isEmpty()) {
			log.debug("No tenant provided, using default: {}", defaultTenant);
			return defaultTenant;
		}

		// First check explicit mapping
		if (!tenantMappings.isEmpty() && tenantMappings.containsKey(tenant)) {
			String registrationId = tenantMappings.get(tenant);
			log.debug("Using explicit mapping: {} -> {}", tenant, registrationId);
			return registrationId;
		}

		// Fallback to convention: tenant name = registration ID
		log.debug("Using convention mapping: {} -> {}", tenant, tenant);
		return tenant;
	}

	/**
	 * Checks if a tenant is valid
	 * In the new approach, we trust that Spring Security will validate
	 * if the registration ID exists, so we just return true for any tenant
	 */
	public boolean isValidTenant(String tenant) {
		// If we have explicit mappings, validate against them
		if (!tenantMappings.isEmpty()) {
			return tenantMappings.containsKey(tenant);
		}
		// Otherwise, trust Spring Security to validate the registration ID
		// Any subdomain is potentially valid
		return true;
	}
}