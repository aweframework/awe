package com.almis.awe.security.multitenant;

import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;

/**
 * Custom authentication entry point that handles auto-launch in multi-tenant environments
 */
@Slf4j
public class MultiTenantOAuth2AuthenticationEntryPoint implements AuthenticationEntryPoint {

	// Autowired services
	private final SecurityConfigProperties securityConfigProperties;
	private final MultiTenantOAuth2Config multiTenantConfig;
	private final ClientRegistrationRepository clientRegistrationRepository;

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	public MultiTenantOAuth2AuthenticationEntryPoint(
			SecurityConfigProperties securityConfigProperties,
			MultiTenantOAuth2Config multiTenantConfig,
			ClientRegistrationRepository clientRegistrationRepository) {
		this.securityConfigProperties = securityConfigProperties;
		this.multiTenantConfig = multiTenantConfig;
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {

			// Get the registration ID from the request attribute (set by MultiTenantFilter)
			String registrationId = (String) request.getAttribute("oauth2.registration.id");

			// Validate and fallback to default if necessary
			registrationId = getValidRegistrationId(registrationId);

        if (securityConfigProperties.getSso().isAutoLaunch()) {
            // Redirect directly to OAuth2 authorization endpoint
            String redirectUrl = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/" + registrationId;
            log.debug("Auto-launching OAuth2 flow, redirecting to: {}", redirectUrl);
            redirectStrategy.sendRedirect(request, response, redirectUrl);
        } else {
            // Default behavior - redirect to the login page
            redirectStrategy.sendRedirect(request, response, "/");
        }
    }

	/**
	 * Validates the registration ID and returns a valid one or the default
	 *
	 * @param registrationId The registration ID to validate
	 * @return A valid registration ID
	 */
	private String getValidRegistrationId(String registrationId) {
		// If null or empty, use default
		if (registrationId == null || registrationId.isEmpty()) {
			log.warn("No registration ID provided, using default tenant configuration");
			return getDefaultRegistrationId();
		}

		// Try to find the registration in Spring Security
		try {
			ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
			if (clientRegistration != null) {
				log.debug("Valid registration ID found: {}", registrationId);
				return registrationId;
			}
		} catch (Exception e) {
			log.warn("Error finding registration ID '{}': {}", registrationId, e.getMessage());
		}

		// Registration isn't found, use default
		log.warn("Registration ID '{}' not found in ClientRegistrationRepository, using default", registrationId);
		return getDefaultRegistrationId();
	}

	/**
	 * Gets the default registration ID from configuration
	 *
	 * @return The default registration ID
	 */
	private String getDefaultRegistrationId() {
		String defaultTenant = multiTenantConfig.getDefaultTenant();
		String defaultRegistrationId = multiTenantConfig.getRegistrationId(defaultTenant);
		log.debug("Using default registration ID from multi-tenant config: {}", defaultRegistrationId);
		return defaultRegistrationId;
	}
}
