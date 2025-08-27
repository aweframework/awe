package com.almis.awe.autoconfigure;

import com.almis.awe.security.handler.AweOauth2AuthenticationFailureHandler;
import com.almis.awe.security.handler.AweOauth2AuthenticationSuccessHandler;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ErrorPageService;
import com.azure.spring.cloud.autoconfigure.implementation.aad.security.AadWebApplicationHttpSecurityConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static com.almis.awe.model.constant.AweConstants.AZURE_OAUTH2_AUTHORIZATION_URL;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Configuration class to loading azure oauth2 EntraID service
 */
@Configuration
@ConditionalOnClass(AadWebApplicationHttpSecurityConfigurer.class)
public class AzureOAuthConfig {

  private final AccessService accessService;
	private final ErrorPageService errorPageService;


	@Value("${spring.cloud.azure.active-directory.redirect-uri-template:/login/oauth2/code/}")
  private String azureRedirectUriTemplate;

  public AzureOAuthConfig(AccessService accessService, ErrorPageService errorPageService) {
    this.accessService = accessService;
		this.errorPageService = errorPageService;
	}

  @Bean
  @ConditionalOnProperty(prefix = "spring.cloud.azure.active-directory", name = "enabled", havingValue = "true")
  public SecurityFilterChain azureOauth2Filter(HttpSecurity httpSecurity) throws Exception {
    // Configure oauth2
    return httpSecurity.securityMatcher(
            AZURE_OAUTH2_AUTHORIZATION_URL,
            azureRedirectUriTemplate)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                antMatcher(azureRedirectUriTemplate),
                antMatcher(AZURE_OAUTH2_AUTHORIZATION_URL)).permitAll()
        )
        .with(AadWebApplicationHttpSecurityConfigurer.aadWebApplication(), withDefaults())
        .oauth2Login(oauth2LoginConfigurer -> oauth2LoginConfigurer
            .loginPage(AZURE_OAUTH2_AUTHORIZATION_URL)
            .loginProcessingUrl(azureRedirectUriTemplate)
            .successHandler(authSuccessHandler())
						.failureHandler(authFailureHandler())
        ).build();
  }

  @Bean
  @ConditionalOnMissingBean
  public AweOauth2AuthenticationSuccessHandler authSuccessHandler() {
    return new AweOauth2AuthenticationSuccessHandler(accessService);
  }

	/**
	 * Creates an instance of AweOauth2AuthenticationFailureHandler, which is responsible
	 * for handling authentication failures during an OAuth2 login process.
	 *
	 * @return an instance of AweOauth2AuthenticationFailureHandler to manage OAuth2 authentication failures
	 */
	@Bean
	@ConditionalOnMissingBean
	public AweOauth2AuthenticationFailureHandler authFailureHandler() {
		return new AweOauth2AuthenticationFailureHandler(errorPageService);
	}
}
