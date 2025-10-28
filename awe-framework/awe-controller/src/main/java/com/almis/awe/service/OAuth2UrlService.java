package com.almis.awe.service;

import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.security.multitenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Service for building multi-tenant OAuth2 URLs
 */

@Slf4j
public class OAuth2UrlService {

	private static final String OAUTH2_AUTHORIZATION_BASE_URI = "/oauth2/authorization/";

	private final MultiTenantOAuth2Config multiTenantConfig;
	private final ClientRegistrationRepository clientRegistrationRepository;

	@Autowired
	public OAuth2UrlService(
			@Autowired(required = false) MultiTenantOAuth2Config multiTenantConfig,
			ClientRegistrationRepository clientRegistrationRepository) {
		this.multiTenantConfig = multiTenantConfig;
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	/**
	 * Gets the OAuth2 authorization URL for the current tenant
	 *
	 * @param request The HTTP request
	 * @return The OAuth2 authorization URL
	 */
	public String getOAuth2AuthorizationUrl(HttpServletRequest request) {
		String registrationId = resolveRegistrationId(request);
		String contextPath = request.getContextPath();
		String authorizationUrl = contextPath + OAUTH2_AUTHORIZATION_BASE_URI + registrationId;

		log.debug("Generated OAuth2 authorization URL: {} for registrationId: {}", authorizationUrl, registrationId);
		return authorizationUrl;
	}

	/**
	 * Gets the registration ID for the current tenant from the request
	 *
	 * @param request The HTTP request
	 * @return The registration ID
	 */
	private String resolveRegistrationId(HttpServletRequest request) {
		// First, try to get from the request attribute (set by MultiTenantFilter)
		String registrationId = (String) request.getAttribute("oauth2.registration.id");

		if (registrationId != null && !registrationId.isEmpty()) {
			log.debug("Found registration ID in request attribute: {}", registrationId);
			return validateAndReturnRegistrationId(registrationId);
		}

		// Fallback to TenantContext
		return resolveRegistrationIdFromContext();
	}

	/**
	 * Gets the registration ID from TenantContext
	 *
	 * @return The registration ID
	 */
	private String resolveRegistrationIdFromContext() {
		// Try to get from TenantContext
		String tenant = TenantContext.getCurrentTenant();

		if (tenant != null && !tenant.isEmpty() && multiTenantConfig != null) {
			String registrationId = multiTenantConfig.getRegistrationId(tenant);
			log.debug("Resolved registration ID from TenantContext. Tenant: {}, Registration ID: {}",
					tenant, registrationId);
			return validateAndReturnRegistrationId(registrationId);
		}

		// Fallback to default
		return getDefaultRegistrationId();
	}

	/**
	 * Validates that a registration ID exists in Spring Security
	 *
	 * @param registrationId The registration ID to validate
	 * @return The registration ID if valid, or the default if not
	 */
	private String validateAndReturnRegistrationId(String registrationId) {
		try {
			ClientRegistration clientRegistration =	clientRegistrationRepository.findByRegistrationId(registrationId);

			if (clientRegistration != null) {
				log.debug("Validated registration ID: {}", registrationId);
				return registrationId;
			} else {
				log.warn("Registration ID '{}' not found in ClientRegistrationRepository, using default", registrationId);
				return getDefaultRegistrationId();
			}
		} catch (Exception e) {
			log.error("Error validating registration ID '{}': {}", registrationId, e.getMessage());
			return getDefaultRegistrationId();
		}
	}

	/**
	 * Gets the default registration ID
	 *
	 * @return The default registration ID
	 */
	private String getDefaultRegistrationId() {
		if (multiTenantConfig != null && multiTenantConfig.isEnabled()) {
			String defaultTenant = multiTenantConfig.getDefaultTenant();
			String defaultRegistrationId = multiTenantConfig.getRegistrationId(defaultTenant);
			log.debug("Using default registration ID: {}", defaultRegistrationId);
			return defaultRegistrationId;
		}

		// Get first client registration if available
		if (clientRegistrationRepository != null && clientRegistrationRepository instanceof InMemoryClientRegistrationRepository inMemoryClientRegistrationRepository) {
			return inMemoryClientRegistrationRepository.iterator().next().getRegistrationId();
		} else
			throw new IllegalStateException("No OAuth2 client registrations found");
	}
}