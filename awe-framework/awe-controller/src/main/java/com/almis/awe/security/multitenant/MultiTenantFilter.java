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
 * Filter that intercepts requests to determine the tenant based on subdomain
 */
@Component
@Order(1)
@Slf4j
public class MultiTenantFilter extends OncePerRequestFilter {

	private static final Pattern SUBDOMAIN_PATTERN = Pattern.compile("^([a-zA-Z0-9-]+)\\.(.+)$");

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

		String tenant = extractTenantFromRequest(request);

		// Set tenant in context
		TenantContext.setCurrentTenant(tenant);

		// Also set as request attribute for fallback scenarios
		request.setAttribute("currentTenant", tenant);

		log.debug("Processing request for tenant: {} - URI: {}", tenant, request.getRequestURI());

		try {
			filterChain.doFilter(request, response);
		} finally {
			// Only clear if this filter set it
			if (tenant.equals(TenantContext.getCurrentTenant())) {
				TenantContext.clear();
			}
		}
	}

	/**
	 * Extracts tenant from request subdomain
	 *
	 * @param request The HTTP request
	 * @return The tenant identifier
	 */
	private String extractTenantFromRequest(HttpServletRequest request) {
		String serverName = request.getServerName();
		String requestURI = request.getRequestURI();
		log.debug("Extracting tenant from server: {}. Request URI: {}", serverName, requestURI);

		// Handle null or empty server name
		if (serverName == null || serverName.isEmpty()) {
			String defaultTenant = multiTenantConfig.getDefaultTenant();
			log.debug("Server name is null or empty, using default tenant: {}", defaultTenant);
			return defaultTenant;
		}

		// Extract subdomain
		Matcher matcher = SUBDOMAIN_PATTERN.matcher(serverName);
		if (matcher.matches()) {
			String subdomain = matcher.group(1);
			log.debug("Subdomain detected: {}", subdomain);

			// Check if the tenant is configured
			if (multiTenantConfig.hasTenant(subdomain)) {
				log.debug("Valid tenant found: {}", subdomain);
				return subdomain;
			} else {
				log.debug("Subdomain {} is not a configured tenant", subdomain);
			}
		}

		// Use default tenant if not found
		String defaultTenant = multiTenantConfig.getDefaultTenant();
		log.debug("Using default tenant: {}", defaultTenant);
		return defaultTenant;
	}
}
