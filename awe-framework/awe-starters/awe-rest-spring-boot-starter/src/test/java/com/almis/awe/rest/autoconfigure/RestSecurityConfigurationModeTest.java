package com.almis.awe.rest.autoconfigure;

import com.almis.awe.autoconfigure.LocalLoginConfig;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.rest.security.JWTAuthenticationFilter;
import com.almis.awe.rest.security.JWTAuthorizationFilter;
import com.almis.awe.security.authorization.PublicQueryMaintainAuthorization;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
      ServletWebSecurityAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class,
      OAuth2ResourceServerAutoConfiguration.class,
      OAuth2ResourceServerWebSecurityAutoConfiguration.class,
      WebMvcAutoConfiguration.class
    ))
    .withUserConfiguration(RestSecurityConfiguration.class, LocalLoginConfig.class, TestConfig.class);

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

  @Test
  void shouldAnswerARefusedLocalLoginExactlyLikeABadPassword() {
    contextRunner
      .withPropertyValues("awe.application.module-list[0]=awe")
      .run(context -> {
        SecurityConfigProperties.Sso sso = context.getBean(SecurityConfigProperties.class).getSso();
        sso.setEnabled(true);
        sso.setLocalLogin(false);
        sso.setBreakGlassUsers(List.of("admin"));

        MockMvc mockMvc = buildMockMvc(context);
        MvcResult wrongPassword = mockMvc.perform(post("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
          .andExpect(status().isUnauthorized())
          .andReturn();
        MvcResult refused = mockMvc.perform(post("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"demo\",\"password\":\"x\"}"))
          .andExpect(status().isUnauthorized())
          .andExpect(header().doesNotExist("Authorization"))
          .andReturn();

        assertThat(refused.getResponse().getContentAsString()).isEqualTo(wrongPassword.getResponse().getContentAsString());
        assertThat(refused.getResponse().getContentType()).isEqualTo(wrongPassword.getResponse().getContentType());
      });
  }

  @Test
  void shouldKeepAuthenticateEndpointOpenForBreakGlassUsersWhenLocalLoginIsDisabled() {
    contextRunner
      .withPropertyValues("awe.application.module-list[0]=awe")
      .run(context -> {
        SecurityConfigProperties.Sso sso = context.getBean(SecurityConfigProperties.class).getSso();
        sso.setEnabled(true);
        sso.setLocalLogin(false);
        sso.setBreakGlassUsers(List.of("admin"));

        buildMockMvc(context).perform(post("/api/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin\",\"password\":\"x\"}"))
          .andExpect(status().isOk())
          .andExpect(header().exists("Authorization"));
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
    @Primary
    AuthenticationManager authenticationManager() {
      // Plays the role of the real providers. The decorated manager comes from the real LocalLoginConfig
      return authentication -> {
        if ("wrong".equals(authentication.getCredentials())) {
          throw new BadCredentialsException("Bad credentials");
        }
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
      return new JsonMapper();
    }

    @Bean
    PublicQueryMaintainAuthorization publicQueryMaintainAuthorization() {
      PublicQueryMaintainAuthorization authorization = mock(PublicQueryMaintainAuthorization.class);
      when(authorization.authorize(any(), any())).thenReturn(new AuthorizationDecision(true));
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
