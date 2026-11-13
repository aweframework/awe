package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.type.SecondFactorStatusType;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.almis.awe.model.constant.AweConstants.JSON_SCREEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

/**
 * The colour mode chosen on the sign-in page is saved as the mode of the user once the login (local or SSO) completes
 */
@ExtendWith(MockitoExtension.class)
class AccessServiceSigninModeTest {

  @Mock
  ApplicationContext applicationContext;
  @Mock
  AweSessionDetails sessionDetails;
  @Mock
  MenuService menuService;
  @Mock
  TotpConfigProperties totpConfigProperties;
  @Mock
  TotpService totpService;
  @Mock
  AweRequest request;
  @Mock
  UserService userService;
  @Mock
  SecurityContext securityContext;
  @Mock
  Authentication authentication;
  @Mock
  SecurityConfigProperties securityConfigProperties;
  @Mock
  AweUserDetailService userDetailService;
  @Mock
  OAuth2UrlService oAuth2UrlService;

  private AccessService accessService;
  private AweUserDetails userDetails;
  private AweSession session;

  @BeforeEach
  void setUp() throws AWException {
    accessService = new AccessService(sessionDetails, menuService, mock(EncodeService.class), totpService,
      mock(BaseConfigProperties.class), securityConfigProperties, totpConfigProperties, userDetailService,
      mock(MaintainService.class));
    accessService.setApplicationContext(applicationContext);
    accessService.setUserService(userService);

    userDetails = new AweUserDetails().setUsername("jdoe").setInitialScreen("home");
    session = new AweSession();
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
    SecurityContextHolder.setContext(securityContext);
    lenient().when(applicationContext.getBean(AweRequest.class)).thenReturn(request);
    lenient().when(applicationContext.getBean(AweSession.class)).thenReturn(session);
    Menu menu = new Menu();
    menu.setScreenContext("private");
    lenient().when(menuService.getMenu()).thenReturn(menu);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ServiceData loginWithSigninMode(String mode, SecondFactorStatusType secondFactor) throws AWException {
    when(request.getParameterAsString(AccessService.SIGNIN_THEME_MODE_PARAMETER)).thenReturn(mode);
    when(totpConfigProperties.getEnabled()).thenReturn(secondFactor);
    return accessService.login();
  }

  @Test
  void savesTheModeChosenOnTheSigninPageAsTheModeOfTheUser() throws AWException {
    ServiceData serviceData = loginWithSigninMode("Dark", SecondFactorStatusType.DISABLED);

    verify(userService).changeUserMode("jdoe", "dark");
    assertThat(((CellData) serviceData.getClientActionList().get(0).getParameters().get(JSON_SCREEN)).getStringValue())
      .isEqualTo("/private/home");
    assertThat(session.getParameter(AccessService.SIGNIN_THEME_MODE_PARAMETER)).isNull();
  }

  @Test
  void keepsTheModeChosenOnTheSigninPageUntilTheSecondFactorIsVerified() throws AWException {
    userDetails.setEnabled2fa(true).setSecret2fa("SECRET");
    loginWithSigninMode("light", SecondFactorStatusType.OPTIONAL);

    verify(userService, never()).changeUserMode(anyString(), anyString());

    when(totpService.verify2faCode("123456")).thenReturn(true);
    accessService.verify2faCode("123456");

    verify(userService).changeUserMode("jdoe", "light");
  }

  @Test
  void ignoresAModeThatIsNotLightDarkOrSystem() throws AWException {
    loginWithSigninMode("sepia", SecondFactorStatusType.DISABLED);

    verify(userService, never()).changeUserMode(anyString(), anyString());
  }

  @Test
  void completesTheLoginWhenTheModeCannotBeSaved() throws AWException {
    when(userService.changeUserMode("jdoe", "system")).thenThrow(new AWException("Error", "Database down"));

    ServiceData serviceData = loginWithSigninMode("system", SecondFactorStatusType.DISABLED);

    assertThat(((CellData) serviceData.getClientActionList().get(0).getParameters().get(JSON_SCREEN)).getStringValue())
      .isEqualTo("/private/home");
    verify(sessionDetails).onLoginSuccess(userDetails);
  }

  @Test
  void aLoginWithoutAModeDropsTheModeLeftByAnUnfinishedLogin() throws AWException {
    session.setParameter(AccessService.SIGNIN_THEME_MODE_PARAMETER, "dark");

    loginWithSigninMode(null, SecondFactorStatusType.DISABLED);

    verify(userService, never()).changeUserMode(anyString(), anyString());
  }

  @Test
  void completesTheLoginWhenSavingTheModeFailsUnexpectedly() throws AWException {
    when(userService.changeUserMode("jdoe", "dark")).thenThrow(new IllegalStateException("Unexpected"));

    ServiceData serviceData = loginWithSigninMode("dark", SecondFactorStatusType.DISABLED);

    assertThat(((CellData) serviceData.getClientActionList().get(0).getParameters().get(JSON_SCREEN)).getStringValue())
      .isEqualTo("/private/home");
  }

  @Test
  void keepsTheModeChosenOnTheSigninPageWhenTheSsoLoginStarts() {
    when(request.getParameterAsString(AccessService.SIGNIN_THEME_MODE_PARAMETER)).thenReturn("dark");
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    when(request.getHttpRequest()).thenReturn(httpRequest);
    when(applicationContext.getBean(OAuth2UrlService.class)).thenReturn(oAuth2UrlService);
    when(oAuth2UrlService.getOAuth2AuthorizationUrl(httpRequest)).thenReturn("/oauth2/authorization/provider");

    accessService.loginWithSSO();

    assertThat(session.getParameter(AccessService.SIGNIN_THEME_MODE_PARAMETER)).isEqualTo("dark");
  }

  @Test
  void keepsTheModeChosenOnTheSigninPageWhenTheAzureLoginStarts() {
    when(request.getParameterAsString(AccessService.SIGNIN_THEME_MODE_PARAMETER)).thenReturn("light");
    when(request.getHttpRequest()).thenReturn(new MockHttpServletRequest());

    accessService.loginWithAzureEntraID();

    assertThat(session.getParameter(AccessService.SIGNIN_THEME_MODE_PARAMETER)).isEqualTo("light");
  }

  @Test
  void savesTheModeChosenOnTheSigninPageWhenTheSsoProviderReturns() throws AWException {
    session.setParameter(AccessService.SIGNIN_THEME_MODE_PARAMETER, "dark");
    Map<String, Object> attributes = Map.of(PREFERRED_USERNAME, "jdoe");
    List<GrantedAuthority> authorities = List.of(new OAuth2UserAuthority(attributes), new SimpleGrantedAuthority("USER"));
    OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
      new DefaultOAuth2User(authorities, attributes, PREFERRED_USERNAME), authorities, "provider");
    SecurityConfigProperties.Sso sso = new SecurityConfigProperties.Sso();
    sso.setUserNameAttribute(PREFERRED_USERNAME);
    when(securityConfigProperties.getSso()).thenReturn(sso);
    when(userDetailService.loadUserByUsername(anyString())).thenReturn(userDetails.setProfileName("USER"));
    when(userDetailService.mapGrantedAuthorityProfile(any())).thenReturn(Optional.of("USER"));

    String initialUrl = accessService.onAuthenticationSuccess(token);

    verify(userService).changeUserMode("jdoe", "dark");
    assertThat(initialUrl).isEqualTo("/private/home");
    assertThat(session.getParameter(AccessService.SIGNIN_THEME_MODE_PARAMETER)).isNull();
  }
}
