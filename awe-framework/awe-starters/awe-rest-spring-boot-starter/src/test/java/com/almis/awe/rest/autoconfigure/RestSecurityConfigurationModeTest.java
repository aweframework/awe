package com.almis.awe.rest.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.rest.security.JWTAuthenticationFilter;
import com.almis.awe.rest.security.JWTAuthorizationFilter;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestSecurityConfigurationModeTest {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class,
      OAuth2ResourceServerAutoConfiguration.class,
      WebMvcAutoConfiguration.class
    ))
    .withUserConfiguration(RestSecurityConfiguration.class, TestConfig.class);

  @Test
  void shouldKeepJwtFiltersAndAuthenticateEndpointInLocalModeByDefault() {
    contextRunner
      .withPropertyValues(
        "awe.security.master.key=test-master-key",
        "awe.application.module-list[0]=awe"
      )
      .run(context -> {
        assertThat(context).hasNotFailed();

        FilterChainProxy filterChainProxy = context.getBean(FilterChainProxy.class);
        List<jakarta.servlet.Filter> filters = filterChainProxy.getFilters("/api/data/test");

        assertThat(filters).anyMatch(JWTAuthenticationFilter.class::isInstance);
        assertThat(filters).anyMatch(JWTAuthorizationFilter.class::isInstance);

        MockMvc mockMvc = buildMockMvc(context);
        mockMvc.perform(post("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"demo\",\"password\":\"demo\"}"))
          .andExpect(status().isOk())
          .andExpect(header().exists("Authorization"));

        mockMvc.perform(get("/api/public/data/test"))
          .andExpect(status().isOk());

        mockMvc.perform(get("/api/maintain/test"))
          .andExpect(status().isUnauthorized());
      });
  }

  @Test
  void shouldDisableJwtFiltersAndAuthenticateEndpointInOauth2ResourceServerMode() {
    contextRunner
      .withPropertyValues(
        "awe.application.module-list[0]=awe",
        "awe.rest.api.auth.mode=oauth2-resource-server",
        "awe.rest.api.oauth2-resource-server.jwt.jwk-set-uri=https://issuer.example.com/jwks",
        "awe.rest.api.oauth2-resource-server.jwt.audiences[0]=api://awe-rest"
      )
      .run(context -> {
        assertThat(context).hasNotFailed();

        FilterChainProxy filterChainProxy = context.getBean(FilterChainProxy.class);
        List<jakarta.servlet.Filter> filters = filterChainProxy.getFilters("/api/data/test");

        assertThat(filters).noneMatch(JWTAuthenticationFilter.class::isInstance);
        assertThat(filters).noneMatch(JWTAuthorizationFilter.class::isInstance);

        MockMvc mockMvc = buildMockMvc(context);
        mockMvc.perform(get("/api/data/test"))
          .andExpect(status().isUnauthorized())
          .andExpect(header().doesNotExist("Location"));

        mockMvc.perform(get("/api/maintain/test"))
          .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/maintain/async/test"))
          .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/public/data/test"))
          .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/maintain/test"))
          .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
          .andExpect(status().isNotFound());

        mockMvc.perform(get("/swagger-ui/index.html"))
          .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"demo\",\"password\":\"demo\"}"))
          .andExpect(status().isUnauthorized());
      });
  }

  private MockMvc buildMockMvc(org.springframework.boot.test.context.assertj.AssertableWebApplicationContext context) {
    return MockMvcBuilders.webAppContextSetup((WebApplicationContext) context.getSourceApplicationContext())
      .addFilters(context.getBean(FilterChainProxy.class))
      .build();
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    AuthenticationManager authenticationManager() {
      return authentication -> {
        String username = String.valueOf(authentication.getPrincipal());
        return new UsernamePasswordAuthenticationToken(
          org.springframework.security.core.userdetails.User.withUsername(username).password("N/A").authorities(List.of()).build(),
          null,
          List.of()
        );
      };
    }

    @Bean
    UserDetailsService userDetailsService() {
      return username -> new AweUserDetails().setUsername(username).setAuthorities(List.of());
    }

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    PublicQueryMaintainAuthorization publicQueryMaintainAuthorization() {
      PublicQueryMaintainAuthorization authorization = mock(PublicQueryMaintainAuthorization.class);
      when(authorization.check(any(), any())).thenReturn(new AuthorizationDecision(true));
      return authorization;
    }

    @Bean
    BaseConfigProperties baseConfigProperties() {
      return new BaseConfigProperties();
    }

    @Bean
    SecurityConfigProperties securityConfigProperties() {
      return new SecurityConfigProperties();
    }

    @RestController
    @RequestMapping("/api")
    static class TestRestController {
      @GetMapping("/data/test")
      String securedData() {
        return "ok";
      }

      @GetMapping("/maintain/test")
      String securedMaintain() {
        return "ok";
      }

      @GetMapping("/maintain/async/test")
      String securedMaintainAsync() {
        return "ok";
      }

      @GetMapping("/public/data/test")
      String publicData() {
        return "ok";
      }

      @GetMapping("/public/maintain/test")
      String publicMaintain() {
        return "ok";
      }
    }
  }
}
