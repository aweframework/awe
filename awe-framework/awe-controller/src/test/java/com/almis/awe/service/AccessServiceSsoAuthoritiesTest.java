package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

/**
 * A successful single sign-on web login replaces the session authentication with one that keeps the provider
 * authorities and adds the roles and permissions (PERM_) of the AWE user
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccessServiceSsoAuthoritiesTest {

  @InjectMocks
  AccessService accessService;
  @Mock
  AweSessionDetails aweSessionDetails;
  @Mock
  MenuService menuService;
  @Mock
  EncodeService encodeService;
  @Mock
  TotpService totpService;
  @Mock
  BaseConfigProperties baseConfigProperties;
  @Mock
  SecurityConfigProperties securityConfigProperties;
  @Mock
  TotpConfigProperties totpConfigProperties;
  @Mock
  AweUserDetailService aweUserDetailService;
  @Mock
  MaintainService maintainService;
  @Mock
  ApplicationContext applicationContext;
  @Mock
  AweSession aweSession;
  @Mock
  AweElements aweElements;

  private OAuth2AuthenticationToken token;
  private AweUserDetails userDetails;

  @BeforeEach
  void setUp() throws Exception {
    accessService.setApplicationContext(applicationContext);
    Map<String, Object> attributes = Map.of(PREFERRED_USERNAME, "jane@acme.com");
    List<GrantedAuthority> providerAuthorities = List.of(new SimpleGrantedAuthority("AWE_ADMIN"));
    token = new OAuth2AuthenticationToken(new DefaultOAuth2User(providerAuthorities, attributes, PREFERRED_USERNAME),
      providerAuthorities, "acme");
    token.setDetails("request details");
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(token);
    SecurityContextHolder.setContext(context);

    SecurityConfigProperties.Sso sso = new SecurityConfigProperties.Sso();
    sso.setUserNameAttribute(PREFERRED_USERNAME);
    when(securityConfigProperties.getSso()).thenReturn(sso);
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    Menu menu = new Menu();
    menu.setScreenContext("screen");
    when(menuService.getMenu()).thenReturn(menu);
    userDetails = new AweUserDetails().setUsername("jane").setProfileName("administrator").setEnabled(true)
      .setAccountNonLocked(true).setInitialScreen("home")
      .setAuthorities(List.of(new SimpleGrantedAuthority("ROLE_administrator"),
        new SimpleGrantedAuthority("PERM_users.read")));
    when(aweUserDetailService.loadUserByUsername(anyString())).thenReturn(userDetails);
    when(aweUserDetailService.mapGrantedAuthorityProfile(any())).thenReturn(Optional.of("administrator"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void successfulLoginAddsTheRolesAndPermissionsOfTheUserToTheProviderAuthorities() throws Exception {
    accessService.onAuthenticationSuccess(token);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).isInstanceOf(OAuth2AuthenticationToken.class);
    OAuth2AuthenticationToken replaced = (OAuth2AuthenticationToken) authentication;
    assertThat(replaced.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactly("AWE_ADMIN", "ROLE_administrator", "PERM_users.read");
    assertThat(replaced.getPrincipal()).isSameAs(token.getPrincipal());
    assertThat(replaced.getAuthorizedClientRegistrationId()).isEqualTo("acme");
    assertThat(replaced.getDetails()).isEqualTo("request details");
    assertThat(replaced.isAuthenticated()).isTrue();
  }

  @Test
  void successfulLoginDropsThePermissionsSentByTheProvider() throws Exception {
    List<GrantedAuthority> providerAuthorities = List.of(new SimpleGrantedAuthority("PERM_users.delete"),
      new SimpleGrantedAuthority("SCOPE_openid"));
    OAuth2AuthenticationToken providerToken = new OAuth2AuthenticationToken(
      new DefaultOAuth2User(providerAuthorities, Map.of(PREFERRED_USERNAME, "jane@acme.com"), PREFERRED_USERNAME),
      providerAuthorities, "acme");
    SecurityContextHolder.getContext().setAuthentication(providerToken);

    accessService.onAuthenticationSuccess(providerToken);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
      .extracting(GrantedAuthority::getAuthority)
      .containsExactly("SCOPE_openid", "ROLE_administrator", "PERM_users.read");
  }

  @Test
  void refusedLoginKeepsTheProviderAuthentication() {
    userDetails.setAccountNonLocked(false);

    assertThatThrownBy(() -> accessService.onAuthenticationSuccess(token)).isInstanceOf(LockedException.class);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(token);
  }
}
