package com.almis.awe.security.multitenant;

import com.almis.awe.config.SecurityConfigProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom authentication entry point that handles auto-launch in multi-tenant environments
 */
@Slf4j
@Component
public class MultiTenantOAuth2AuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityConfigProperties securityConfigProperties;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public MultiTenantOAuth2AuthenticationEntryPoint(SecurityConfigProperties securityConfigProperties) {
        this.securityConfigProperties = securityConfigProperties;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        if (securityConfigProperties.getSso().isAutoLaunch()) {
            
            // Redirect directly to OAuth2 authorization endpoint
            String redirectUrl = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/keycloak";
            log.debug("Auto-launching OAuth2 flow, redirecting to: {}", redirectUrl);
            redirectStrategy.sendRedirect(request, response, redirectUrl);
        } else {
            // Default behavior - redirect to the login page
            redirectStrategy.sendRedirect(request, response, "/");
        }
    }
}
