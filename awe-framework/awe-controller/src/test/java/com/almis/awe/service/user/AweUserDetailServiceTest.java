package com.almis.awe.service.user;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.dao.UserDAO;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

@ExtendWith(MockitoExtension.class)
class AweUserDetailServiceTest {

  @InjectMocks
  AweUserDetailService userDetailsService;
  @Mock
  BaseConfigProperties baseConfigProperties;
  @Mock
  SecurityConfigProperties securityConfigProperties;
  @Mock
  ApplicationContext context;
  @Mock
  AweElements aweElements;
  @Mock
  UserDAO userDAO;

  @BeforeEach
  void setUp() {
    userDetailsService.setApplicationContext(context);
  }

  @Test
  void giveSomeUser_willReturnUserDetails() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLastChangedPasswordDate(new Date())
      .setLocked(true));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertAll(
      () -> assertNotNull(details),
      () -> assertFalse(details.isCredentialsNonExpired()),
      () -> assertFalse(details.isAccountNonLocked())
    );
  }

  private void mockProperties() {
    when(baseConfigProperties.getTheme()).thenReturn("sky");
    when(baseConfigProperties.getLanguageDefault()).thenReturn("es-ES");
    when(baseConfigProperties.getScreen()).thenReturn(new BaseConfigProperties.Screen());
    when(baseConfigProperties.getDefaultRole()).thenReturn("operator");
    when(securityConfigProperties.getDefaultRestriction()).thenReturn("manager");
  }

  @Test
  void giveSomeUserWithCredentialExpired_willReturnUserDetails() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByUserName(anyString())).willReturn(new User()
      .setUsername("test")
      .setPassword("test")
      .setEnabled(true)
      .setProfile("ADM")
      .setLocked(false));
    UserDetails details = userDetailsService.loadUserByUsername("test");
    assertAll(
      () -> assertNotNull(details),
      () -> assertFalse(details.isCredentialsNonExpired()),
      () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @Test
  void testGetAuthorities() {
    GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_DUMMY");
    List<GrantedAuthority> expectedValue = Collections.singletonList(grantedAuthority);
    assertEquals(expectedValue, userDetailsService.getAuthorities("DUMMY"));
  }

  @Test
  void testMapGrantedAuthorityProfile_withAuthoritiesAndPrefix() {
    SecurityConfigProperties.Sso ssoProperties = new SecurityConfigProperties.Sso();
    ssoProperties.setFilterAuthorityPrefix("ROLE_");
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MANAGER"));
    when(securityConfigProperties.getSso()).thenReturn(ssoProperties);
    String result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals("MANAGER", result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withEmptyAuthorities_returnDefaultRole() {
    List<GrantedAuthority> authorities = Collections.emptyList();
    when(baseConfigProperties.getDefaultRole()).thenReturn("operator");
    String result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals("operator", result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withNullPrefixFilter_returnFullRole() {
    SecurityConfigProperties.Sso ssoConfig = new SecurityConfigProperties.Sso();
    ssoConfig.setFilterAuthorityPrefix(null);
    when(securityConfigProperties.getSso()).thenReturn(ssoConfig);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    String result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals("ROLE_ADMIN", result);
  }

  @Test
  void testMapGrantedAuthorityProfile_withHighPrefixFilter_returnDefaultRole() {
    SecurityConfigProperties.Sso ssoConfig = new SecurityConfigProperties.Sso();
    when(baseConfigProperties.getDefaultRole()).thenReturn("operator");
    ssoConfig.setFilterAuthorityPrefix("ROLE_ADMINS");
    when(securityConfigProperties.getSso()).thenReturn(ssoConfig);
    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    String result = userDetailsService.mapGrantedAuthorityProfile(authorities);
    assertEquals("operator", result);
  }


  @Test
  void loadUserByEmail() {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProperty("PwdExp")).thenReturn("1");
    given(userDAO.findByEmail(anyString())).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEnabled(true)
        .setProfile("ADM")
        .setLocked(false));
    UserDetails details = userDetailsService.loadUserByEmail("foo@dummy.com");
    assertAll(
        () -> assertNotNull(details),
        () -> assertFalse(details.isCredentialsNonExpired()),
        () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void givenRole_loadUserByRole(boolean existRole) throws AWException {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(securityConfigProperties.getSso()).thenReturn(new SecurityConfigProperties.Sso());
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "test@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new OAuth2UserAuthority(attributeMap));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(oAuth2User, grantedAuthorities, "clientRegId");
    given(userDAO.findByRole(anyString())).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEmail("test@acme.com")
        .setEnabled(true)
        .setProfile("ADM")
        .setLocked(false));
    given(userDAO.existRole(anyString())).willReturn(existRole);
    UserDetails details = userDetailsService.loadUserByRole(oAuth2AuthenticationToken);
    assertAll(
        () -> assertNotNull(details),
        () -> assertTrue(details.isCredentialsNonExpired()),
        () -> assertTrue(details.isAccountNonLocked())
    );
  }

  @Test
  void givenNullRole_loadDefaultUserRole() throws AWException {
    mockProperties();
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(securityConfigProperties.getSso()).thenReturn(new SecurityConfigProperties.Sso());
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "test@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new OAuth2UserAuthority(attributeMap));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(oAuth2User, grantedAuthorities, "clientRegId");
    given(userDAO.findByRole(anyString())).willReturn(new User()
        .setUsername("test")
        .setPassword("test")
        .setEmail("test@acme.com")
        .setEnabled(true)
        .setProfile("ADM")
        .setLocked(false));
    given(userDAO.existRole(anyString())).willReturn(true);
    UserDetails details = userDetailsService.loadUserByRole(oAuth2AuthenticationToken);
    assertAll(
        () -> assertNotNull(details),
        () -> assertTrue(details.isCredentialsNonExpired()),
        () -> assertTrue(details.isAccountNonLocked())
    );
  }
}