package com.almis.awe.security.multitenant;

import com.almis.awe.config.MultiTenantOAuth2Config;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter that extracts tenant from subdomain and maps it to OAuth2 registration ID
 * <p>
 * Responsibilities:
 * 1. Extract subdomain from request
 * 2. Map subdomain to registration ID (via convention or explicit mapping)
 * 3. Set registration ID as a request attribute
 * 4. Spring Security's ClientRegistrationRepository handles the rest
 */
@Order(1)
@Slf4j
public class MultiTenantFilter extends OncePerRequestFilter {

	private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("^([a-zA-Z0-9-]+)\\.(.+)$");
	private static final String TENANT_ATTRIBUTE = "currentTenant";
	private static final String REGISTRATION_ID_ATTRIBUTE = "oauth2.registration.id";

	private final MultiTenantOAuth2Config multiTenantConfig;

	public MultiTenantFilter(MultiTenantOAuth2Config multiTenantConfig) {
		this.multiTenantConfig = multiTenantConfig;
	}

	@Override
	protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
																	@NotNull FilterChain filterChain) throws ServletException, IOException {

		if (!multiTenantConfig.isEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}

		// Extract tenant from subdomain
		String tenant = extractTenantFromRequest(request);

		// Map tenant to registration ID
		String registrationId = multiTenantConfig.getRegistrationId(tenant);

		// Set tenant and registration ID in context
		TenantContext.setCurrentTenant(tenant);

		// Set as request attributes for OAuth2 flow
		request.setAttribute(TENANT_ATTRIBUTE, tenant);
		request.setAttribute(REGISTRATION_ID_ATTRIBUTE, registrationId);

		log.debug("Multi-tenant filter - Tenant: '{}', Registration ID: '{}', URI: {}",
				tenant, registrationId, request.getRequestURI());

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Clean up context
			TenantContext.clear();
		}
	}

	/**
	 * Extracts tenant from request subdomain
	 * <p>
	 * Examples:
	 *   - tenant1.example.com -> "tenant1"
	 *   - public.example.com -> "public"
	 *   - example.com -> defaultTenant
	 *   - localhost -> defaultTenant
	 *
	 * @param request The HTTP request
	 * @return The tenant identifier
	 */
	private String extractTenantFromRequest(HttpServletRequest request) {
		String serverName = request.getServerName();

		if (serverName == null || serverName.isEmpty()) {
			log.debug("Server name is null/empty, using default tenant: {}", multiTenantConfig.getDefaultTenant());
			return multiTenantConfig.getDefaultTenant();
		}

		log.debug("Extracting tenant from server name: {}", serverName);

		// Extract subdomain using regex
		Matcher matcher = SUBDOMAIN_PATTERN.matcher(serverName);
		if (matcher.matches()) {
			String subdomain = matcher.group(1);
			log.debug("Subdomain detected: '{}'", subdomain);

			// Validate if this is a valid tenant
			if (multiTenantConfig.isValidTenant(subdomain)) {
				log.debug("Valid tenant found: '{}'", subdomain);
				return subdomain;
			} else {
				log.debug("Subdomain '{}' is not a configured tenant, using default", subdomain);
			}
		} else {
			log.debug("No subdomain pattern matched in '{}'", serverName);
		}

		// No valid subdomain found, use default
		String defaultTenant = multiTenantConfig.getDefaultTenant();
		log.debug("Using default tenant: '{}'", defaultTenant);
		return defaultTenant;
	}
}