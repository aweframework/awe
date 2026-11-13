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
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.type.SecondFactorStatusType;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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

import static com.almis.awe.model.constant.AweConstants.CHANGE_LANGUAGE;
import static com.almis.awe.model.constant.AweConstants.JSON_SCREEN;
import static com.almis.awe.model.constant.AweConstants.SESSION_LANGUAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

/**
 * The language chosen on the sign-in page is the language of the session once the login (local, second factor or
 * SSO) completes, and it is saved as the language of the user
 */
@ExtendWith(MockitoExtension.class)
class AccessServiceSigninLanguageTest {

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
  BaseConfigProperties baseConfigProperties;
  @Mock
  OAuth2UrlService oAuth2UrlService;

  private AccessService accessService;
  private AweUserDetails userDetails;
  private AweSession session;

  @BeforeEach
  void setUp() throws AWException {
    accessService = new AccessService(sessionDetails, menuService, mock(EncodeService.class), totpService,
      baseConfigProperties, securityConfigProperties, totpConfigProperties, userDetailService,
      mock(MaintainService.class));
    accessService.setApplicationContext(applicationContext);
    accessService.setUserService(userService);

    userDetails = new AweUserDetails().setUsername("jdoe").setInitialScreen("home").setLanguage("en-GB");
    session = new AweSession();
    lenient().when(baseConfigProperties.getLanguageList()).thenReturn(List.of("en-GB", "es-ES", "fr-FR"));
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(userDetails);
    SecurityContextHolder.setContext(securityContext);
    lenient().when(applicationContext.getBean(AweRequest.class)).thenReturn(request);
    // The login also reads the colour mode chosen on the sign-in page, not sent here
    lenient().when(request.getParameterAsString(anyString())).thenReturn(null);
    lenient().when(applicationContext.getBean(AweSession.class)).thenReturn(session);
    Menu menu = new Menu();
    menu.setScreenContext("private");
    lenient().when(menuService.getMenu()).thenReturn(menu);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private ServiceData loginWithSigninLanguage(String language, SecondFactorStatusType secondFactor) throws AWException {
    when(request.getParameterAsString(AccessService.SIGNIN_LANGUAGE_PARAMETER)).thenReturn(language);
    when(totpConfigProperties.getEnabled()).thenReturn(secondFactor);
    return accessService.login();
  }

  private String changeLanguageOf(ServiceData serviceData) {
    ClientAction changeLanguage = serviceData.getClientActionList().stream()
      .filter(action -> CHANGE_LANGUAGE.equals(action.getType())).findFirst()
      .orElseThrow(() -> new AssertionError("No change language action"));
    return ((CellData) changeLanguage.getParameters().get(SESSION_LANGUAGE)).getStringValue();
  }

  @Test
  void theLanguageChosenOnTheSigninPageIsTheLanguageOfTheSessionAndOfTheUser() throws AWException {
    ServiceData serviceData = loginWithSigninLanguage("es-ES", SecondFactorStatusType.DISABLED);

    // Set before the session details are stored, so the session language and /settings carry it
    InOrder order = inOrder(userService, sessionDetails);
    order.verify(userService).changeUserLanguage("jdoe", "es-ES");
    order.verify(sessionDetails).onLoginSuccess(userDetails);
    assertThat(userDetails.getLanguage()).isEqualTo("es-ES");
    assertThat(changeLanguageOf(serviceData)).isEqualTo("es-ES");
    assertThat(session.getParameter(AccessService.SIGNIN_LANGUAGE_PARAMETER)).isNull();
  }

  @Test
  void theSecondFactorScreenShowsTheLanguageChosenOnTheSigninPageAndTheLoginSavesItOnceVerified() throws AWException {
    userDetails.setEnabled2fa(true).setSecret2fa("SECRET");
    ServiceData secondFactor = loginWithSigninLanguage("fr-FR", SecondFactorStatusType.OPTIONAL);

    assertThat(changeLanguageOf(secondFactor)).isEqualTo("fr-FR");
    verify(userService, never()).changeUserLanguage(anyString(), anyString());

    when(totpService.verify2faCode("123456")).thenReturn(true);
    ServiceData home = accessService.verify2faCode("123456");

    verify(userService).changeUserLanguage("jdoe", "fr-FR");
    assertThat(changeLanguageOf(home)).isEqualTo("fr-FR");
  }

  @Test
  void theSecondFactorActivationScreenShowsTheLanguageChosenOnTheSigninPage() throws AWException {
    ServiceData activation = loginWithSigninLanguage("es-ES", SecondFactorStatusType.FORCE);

    assertThat(changeLanguageOf(activation)).isEqualTo("es-ES");
    verify(userService, never()).changeUserLanguage(anyString(), anyString());
  }

  @Test
  void withoutALanguageChosenOnTheSigninPageTheLanguageOfTheUserWins() throws AWException {
    ServiceData serviceData = loginWithSigninLanguage(null, SecondFactorStatusType.DISABLED);

    verify(userService, never()).changeUserLanguage(anyString(), anyString());
    assertThat(changeLanguageOf(serviceData)).isEqualTo("en-GB");
    assertThat(userDetails.getLanguage()).isEqualTo("en-GB");
  }

  @Test
  void ignoresALanguageThatIsNotALanguageOfTheApplication() throws AWException {
    ServiceData serviceData = loginWithSigninLanguage("de-DE", SecondFactorStatusType.DISABLED);

    verify(userService, never()).changeUserLanguage(anyString(), anyString());
    assertThat(changeLanguageOf(serviceData)).isEqualTo("en-GB");
  }

  @Test
  void aLoginWithoutALanguageDropsTheLanguageLeftByAnUnfinishedLogin() throws AWException {
    session.setParameter(AccessService.SIGNIN_LANGUAGE_PARAMETER, "es-ES");

    loginWithSigninLanguage(null, SecondFactorStatusType.DISABLED);

    verify(userService, never()).changeUserLanguage(anyString(), anyString());
    assertThat(userDetails.getLanguage()).isEqualTo("en-GB");
  }

  @Test
  void completesTheLoginWhenTheLanguageCannotBeSaved() throws AWException {
    when(userService.changeUserLanguage("jdoe", "es-ES")).thenThrow(new AWException("Error", "Database down"));

    ServiceData serviceData = loginWithSigninLanguage("es-ES", SecondFactorStatusType.DISABLED);

    assertThat(((CellData) serviceData.getClientActionList().get(0).getParameters().get(JSON_SCREEN)).getStringValue())
      .isEqualTo("/private/home");
    assertThat(changeLanguageOf(serviceData)).isEqualTo("es-ES");
    verify(sessionDetails).onLoginSuccess(userDetails);
  }

  @Test
  void keepsTheLanguageChosenOnTheSigninPageWhenTheSsoLoginStarts() {
    when(request.getParameterAsString(AccessService.SIGNIN_LANGUAGE_PARAMETER)).thenReturn("es-ES");
    MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    when(request.getHttpRequest()).thenReturn(httpRequest);
    when(applicationContext.getBean(OAuth2UrlService.class)).thenReturn(oAuth2UrlService);
    when(oAuth2UrlService.getOAuth2AuthorizationUrl(httpRequest)).thenReturn("/oauth2/authorization/provider");

    accessService.loginWithSSO();

    assertThat(session.getParameter(AccessService.SIGNIN_LANGUAGE_PARAMETER)).isEqualTo("es-ES");
  }

  @Test
  void keepsTheLanguageChosenOnTheSigninPageWhenTheAzureLoginStarts() {
    when(request.getParameterAsString(AccessService.SIGNIN_LANGUAGE_PARAMETER)).thenReturn("fr-FR");
    when(request.getHttpRequest()).thenReturn(new MockHttpServletRequest());

    accessService.loginWithAzureEntraID();

    assertThat(session.getParameter(AccessService.SIGNIN_LANGUAGE_PARAMETER)).isEqualTo("fr-FR");
  }

  @Test
  void theLanguageChosenOnTheSigninPageIsTheLanguageOfTheUserWhenTheSsoProviderReturns() throws AWException {
    session.setParameter(AccessService.SIGNIN_LANGUAGE_PARAMETER, "es-ES");
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

    InOrder order = inOrder(userService, sessionDetails);
    order.verify(userService).changeUserLanguage("jdoe", "es-ES");
    order.verify(sessionDetails).onLoginSuccess(userDetails);
    assertThat(userDetails.getLanguage()).isEqualTo("es-ES");
    assertThat(initialUrl).isEqualTo("/private/home");
    assertThat(session.getParameter(AccessService.SIGNIN_LANGUAGE_PARAMETER)).isNull();
  }
}
