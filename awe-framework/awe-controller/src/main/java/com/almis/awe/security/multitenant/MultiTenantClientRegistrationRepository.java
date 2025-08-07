package com.almis.awe.security.multitenant;

import com.almis.awe.config.MultiTenantOAuth2Config;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Multi-tenant client registration repository using Spring Security native configurations
 * Only active when multi-tenant is enabled
 */
@Component
@ConditionalOnProperty(prefix = "awe.security.sso.multitenant", name = "enabled", havingValue = "true")
@Slf4j
public class MultiTenantClientRegistrationRepository implements ClientRegistrationRepository {

	// Constants
	public static final String BASE_URL = "{baseUrl}";
	public static final String HTTP_PROTOCOL = "http://";
	public static final String HTTPS_PROTOCOL = "https://";

	private final MultiTenantOAuth2Config multiTenantConfig;
	private final Map<String, ClientRegistration> registrations = new HashMap<>();

	public MultiTenantClientRegistrationRepository(MultiTenantOAuth2Config multiTenantConfig) {
		this.multiTenantConfig = multiTenantConfig;
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		// Try to get a tenant from TenantContext first
		String currentTenant = TenantContext.getCurrentTenant();
		
		// If null, try to extract from the current HTTP request
		if (currentTenant == null) {
			currentTenant = extractTenantFromCurrentRequest();
		}
		
		// Fallback to default tenant
		if (currentTenant == null) {
			currentTenant = multiTenantConfig.getDefaultTenant();
		}

		log.debug("Resolving client registration for tenant: {} and registrationId: {}", currentTenant, registrationId);

		String key = currentTenant + "-" + registrationId;

		// Check cache first - but don't cache when tenant resolution failed
		if (!currentTenant.equals(multiTenantConfig.getDefaultTenant()) || TenantContext.getCurrentTenant() != null) {
			ClientRegistration registration = registrations.get(key);
			if (registration != null) {
				log.debug("Found cached registration for key: {}", key);
				return registration;
			}
		}

		// Create registration using Spring native classes
		ClientRegistration registration = createClientRegistrationFromSpringConfig(currentTenant, key);
		if (registration != null && (!currentTenant.equals(multiTenantConfig.getDefaultTenant()))) {
			registrations.put(key, registration);
		}

		return registration;
	}

	/**
	 * Extracts tenant from current HTTP request if available
	 * @return tenant name or null if not available
	 */
	private String extractTenantFromCurrentRequest() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				String serverName = request.getServerName();
				
				log.debug("Extracting tenant from current request server: {}", serverName);
				
				// Apply the same logic as MultiTenantFilter
				return extractTenantFromServerName(serverName);
			}
		} catch (Exception e) {
			log.debug("Could not extract tenant from current request: {}", e.getMessage());
		}
		return null;
	}

	/**
	 * Extracts tenant from server name using same logic as MultiTenantFilter
	 * @param serverName the server name
	 * @return tenant name or null
	 */
	private String extractTenantFromServerName(String serverName) {
		// Same pattern as in MultiTenantFilter
		final java.util.regex.Pattern domainPattern = java.util.regex.Pattern.compile("^([a-zA-Z0-9-]+)\\.(.+)$");
		
		java.util.regex.Matcher matcher = domainPattern.matcher(serverName);
		if (matcher.matches()) {
			String subdomain = matcher.group(1);
			log.debug("Subdomain detected: {}", subdomain);

			// Check if the tenant is configured
			if (multiTenantConfig.hasTenant(subdomain)) {
				return subdomain;
			}
		}
		return null;
	}

	/**
	 * Creates ClientRegistration using native OAuth2ClientProperties
	 *
	 * @param tenant The tenant identifier
	 * @param registrationId The registration identifier
	 * @return ClientRegistration instance
	 */
	private ClientRegistration createClientRegistrationFromSpringConfig(String tenant, String registrationId) {
		OAuth2ClientProperties.Registration regConfig = multiTenantConfig.getRegistration(tenant);
		OAuth2ClientProperties.Provider providerConfig = multiTenantConfig.getProvider(tenant);

		if (regConfig == null || providerConfig == null) {
			log.error("Could not find OAuth2 configuration for tenant: {}", tenant);
			return null;
		}

 		log.debug("Creating client registration for tenant: {} with registrationId: {}", tenant, registrationId);

		// Resolve redirect URI dynamically
		String redirectUri = resolveRedirectUri(regConfig.getRedirectUri());
		log.debug("Resolved redirect URI for tenant {}: {}", tenant, redirectUri);

		// Configure provider URLs
		if (providerConfig.getIssuerUri() != null) {
			return ClientRegistrations
					.fromIssuerLocation(providerConfig.getIssuerUri())
					.registrationId(registrationId)
					.clientId(regConfig.getClientId())
					.clientSecret(regConfig.getClientSecret())
					.redirectUri(redirectUri)
					.scope(regConfig.getScope())
					.clientName(regConfig.getClientName())
					.userNameAttributeName(providerConfig.getUserNameAttribute())
					.clientAuthenticationMethod(getClientAuthenticationMethod(regConfig.getClientAuthenticationMethod()))
					.authorizationGrantType(getAuthorizationGrantType(regConfig.getAuthorizationGrantType()))
					.build();

		} else {
			// Manual configuration if no issuer-uri
			ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId)
					.clientId(regConfig.getClientId())
					.clientSecret(regConfig.getClientSecret())
					.clientAuthenticationMethod(getClientAuthenticationMethod(regConfig.getClientAuthenticationMethod()))
					.authorizationGrantType(getAuthorizationGrantType(regConfig.getAuthorizationGrantType()))
					.redirectUri(redirectUri)
					.scope(regConfig.getScope())
					.clientName(regConfig.getClientName())
					.authorizationUri(providerConfig.getAuthorizationUri())
					.tokenUri(providerConfig.getTokenUri())
					.userInfoUri(providerConfig.getUserInfoUri())
					.userNameAttributeName(providerConfig.getUserNameAttribute())
					.jwkSetUri(providerConfig.getJwkSetUri());

			return builder.build();
		}
	}

	/**
	 * Resolves the redirect URI by replacing placeholders
	 *
	 * @param configuredRedirectUri The configured redirect URI with placeholders
	 * @return The resolved redirect URI
	 */
	private String resolveRedirectUri(String configuredRedirectUri) {
		if (configuredRedirectUri == null) {
			return "http://localhost:8080/login/oauth2/code/keycloak"; // Default fallback
		}

		// If it's already an absolute URL, return as is
		if (configuredRedirectUri.startsWith(HTTP_PROTOCOL) || configuredRedirectUri.startsWith(HTTPS_PROTOCOL)) {
			return configuredRedirectUri;
		}

		// Try to get the current request to build base URL
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
			if (attributes != null) {
				HttpServletRequest request = attributes.getRequest();
				String scheme = request.getScheme();
				String serverName = request.getServerName();
				int serverPort = request.getServerPort();
				String contextPath = request.getContextPath();

				String baseUrl = scheme + "://" + serverName;
				if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
					baseUrl += ":" + serverPort;
				}
				baseUrl += contextPath;

				// Replace {baseUrl} placeholder
				return configuredRedirectUri.replace(BASE_URL, baseUrl);
			}
		} catch (Exception e) {
			log.warn("Could not resolve redirect URI from request context: {}", e.getMessage());
		}
		// Fallback: replace {baseUrl} with localhost for development
		if (configuredRedirectUri.contains(BASE_URL)) {
			return configuredRedirectUri.replace(BASE_URL, "http://localhost:8080");
		}

		return configuredRedirectUri;
	}

	/**
	 * Converts string to ClientAuthenticationMethod
	 *
	 * @param method The authentication method as string
	 * @return ClientAuthenticationMethod enum value
	 */
	private ClientAuthenticationMethod getClientAuthenticationMethod(String method) {
		if (method == null) return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
		return switch (method.toLowerCase()) {
			case "client_secret_post" -> ClientAuthenticationMethod.CLIENT_SECRET_POST;
			case "client_secret_jwt" -> ClientAuthenticationMethod.CLIENT_SECRET_JWT;
			case "private_key_jwt" -> ClientAuthenticationMethod.PRIVATE_KEY_JWT;
			case "none" -> ClientAuthenticationMethod.NONE;
			default -> ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
		};
	}

	/**
	 * Converts string to AuthorizationGrantType
	 *
	 * @param grantType The grant type as string
	 * @return AuthorizationGrantType enum value
	 */
	private AuthorizationGrantType getAuthorizationGrantType(String grantType) {
		if (grantType == null) return AuthorizationGrantType.AUTHORIZATION_CODE;
		return switch (grantType.toLowerCase()) {
			case "client_credentials" -> AuthorizationGrantType.CLIENT_CREDENTIALS;
			case "refresh_token" -> AuthorizationGrantType.REFRESH_TOKEN;
			default -> AuthorizationGrantType.AUTHORIZATION_CODE;
		};
	}
}