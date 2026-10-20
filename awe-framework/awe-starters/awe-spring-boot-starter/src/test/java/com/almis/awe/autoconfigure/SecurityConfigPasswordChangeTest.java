package com.almis.awe.autoconfigure;

import com.almis.awe.component.AweHttpServletRequestWrapper;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.MultiTenantOAuth2Config;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.security.multitenant.MultiTenantFilter;
import com.almis.awe.service.AccessService;
import com.almis.awe.service.ActionService;
import com.almis.awe.service.ErrorPageService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.TotpService;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.service.user.PasswordChangeService;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Password change service configuration tests")
class SecurityConfigPasswordChangeTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(SecurityConfig.class))
    .withBean(AuthenticationManagerBuilder.class, () -> mock(AuthenticationManagerBuilder.class))
    .withBean(AccessService.class, () -> mock(AccessService.class))
    .withBean(TotpService.class, () -> mock(TotpService.class))
    .withBean(MaintainService.class, () -> mock(MaintainService.class))
    .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
    .withUserConfiguration(BaseConfigProperties.class, SecurityConfigProperties.class)
    .withPropertyValues("awe.application.module-list=awe", "awe.security.auth-mode=bbdd",
      "awe.security.master-key=test-master-key");

  @Test
  @DisplayName("A custom user details service starts the context without the expired password change service")
  void customUserDetailsService_hasNoPasswordChangeService() {
    UserDetailsService custom = username -> User.withUsername(username).password("x").build();

    runner.withBean(UserDetailsService.class, () -> custom).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).doesNotHaveBean(PasswordChangeService.class);
    });
  }

  @Test
  @DisplayName("AWE's user details service enables the expired password change service")
  void aweUserDetailsService_hasPasswordChangeService() {
    runner.withBean(AweUserDetailService.class, () -> mock(AweUserDetailService.class)).run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).hasSingleBean(PasswordChangeService.class);
      assertThat(ReflectionTestUtils.getField(context.getBean(PasswordChangeService.class), "passwordEncoder"))
        .isSameAs(context.getBean(PasswordEncoder.class));
    });
  }

  @Test
  @DisplayName("Without the service the expired password login answers the localized message only")
  void expiredPasswordFailure_withoutTheService_answersTheMessageOnly() throws Exception {
    ApplicationContext context = mock(ApplicationContext.class);
    @SuppressWarnings("unchecked")
    ObjectProvider<PasswordChangeService> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    when(context.getBeanProvider(PasswordChangeService.class)).thenReturn(provider);
    AweRequest aweRequest = mock(AweRequest.class);
    when(aweRequest.getParameterAsString(anyString())).thenReturn("jorgito");
    when(context.getBean(AweRequest.class)).thenReturn(aweRequest);
    AweElements elements = mock(AweElements.class);
    ActionService actionService = mock(ActionService.class);
    ClientAction message = new ClientAction("message");
    when(actionService.launchError(anyString(), any(AWException.class))).thenReturn(List.of(message));
    ObjectMapper objectMapper = new JsonMapper();

    AweWebSecurityConfig config = new AweWebSecurityConfig(context, new BaseConfigProperties(), new SecurityConfigProperties(),
      mock(AweSessionDetails.class), elements, actionService, objectMapper, mock(AccessService.class),
      mock(ErrorPageService.class), new MultiTenantOAuth2Config(), mock(MultiTenantFilter.class));
    AuthenticationFailureHandler handler = (AuthenticationFailureHandler) ReflectionTestUtils.getField(
      config.jsonAuthenticationFilter(new BaseConfigProperties(), elements, actionService, objectMapper), "failureHandler");

    AweHttpServletRequestWrapper request = mock(AweHttpServletRequestWrapper.class);
    when(request.getBody()).thenReturn("{\"cod_usr\":\"jorgito\"}");
    MockHttpServletResponse response = new MockHttpServletResponse();
    assertThat(handler).isNotNull();
    handler.onAuthenticationFailure(request, response, new CredentialsExpiredException("expired"));

    verify(elements).getLocale("ERROR_TITLE_EXPIRED_PASSWORD");
    assertThat(response.getContentAsString()).contains("message").doesNotContain("screen");
  }
}
