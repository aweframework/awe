package com.almis.awe.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Multi-tenant configuration using Spring Security's native OAuth2ClientProperties
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
	 * Default tenant
	 */
	private String defaultTenant = "default";

	// Use Spring Security native classes
	private Map<String, OAuth2ClientProperties.Registration> registrations = new HashMap<>();
	private Map<String, OAuth2ClientProperties.Provider> providers = new HashMap<>();

	/**
	 * Gets OAuth2 registration configuration for a tenant
	 *
	 * @param tenant The tenant identifier
	 * @return OAuth2ClientProperties.Registration for the tenant
	 */
	public OAuth2ClientProperties.Registration getRegistration(String tenant) {
		OAuth2ClientProperties.Registration registration = registrations.get(tenant);
		if (registration == null) {
			log.warn("No OAuth2 registration found for tenant: {}. Using default tenant: {}",
					tenant, defaultTenant);
			registration = registrations.get(defaultTenant);
		}
		return registration;
	}

	/**
	 * Gets OAuth2 provider configuration for a tenant
	 *
	 * @param tenant The tenant identifier
	 * @return OAuth2ClientProperties.Provider for the tenant
	 */
	public OAuth2ClientProperties.Provider getProvider(String tenant) {
		OAuth2ClientProperties.Provider provider = providers.get(tenant);
		if (provider == null) {
			log.warn("No OAuth2 provider found for tenant: {}. Using default tenant: {}",
					tenant, defaultTenant);
			provider = providers.get(defaultTenant);
		}
		return provider;
	}

	/**
	 * Checks if a tenant is configured
	 *
	 * @param tenant The tenant identifier
	 * @return true if the tenant has both registration and provider configured
	 */
	public boolean hasTenant(String tenant) {
		return registrations.containsKey(tenant) && providers.containsKey(tenant);
	}

}
