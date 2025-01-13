package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.type.SecondFactorStatusType;
import com.almis.awe.service.user.AweUserDetailService;
import com.almis.awe.session.AweSessionDetails;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.constant.AweConstants.AZURE_OAUTH2_AUTHORIZATION_URL;
import static com.almis.awe.model.constant.AweConstants.SESSION_INITIAL_URL;
import static com.almis.awe.service.AccessService.PROVISIONING_NEW_USER;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

  @InjectMocks
  AccessService accessService;

  @Mock
  TotpConfigProperties totpConfigProperties;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  AweSessionDetails aweSessionDetails;

  @Mock
  BaseConfigProperties baseConfigProperties;

  @Mock
  SecurityConfigProperties securityConfigProperties;

  @Mock
  EncodeService encodeService;

  @Mock
  MenuService menuService;

  @Mock
  ApplicationContext applicationContext;

  @Mock
  TotpService totpService;

  @Mock
  AweSession aweSession;

  @Mock
  AweElements aweElements;

  @Mock
  AweRequest aweRequest;

  @Mock
  AweUserDetailService aweUserDetailService;

  @Mock
  MaintainService maintainService;

  private AweUserDetails aweUserDetails;

  @BeforeEach
  void setUp() {
    aweUserDetails = new AweUserDetails();
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getPrincipal()).thenReturn(aweUserDetails);
    SecurityContextHolder.setContext(securityContext);
    accessService.setApplicationContext(applicationContext);
  }

  @ParameterizedTest
  @EnumSource(SecondFactorStatusType.class)
  void loginUser2FA(SecondFactorStatusType statusType) throws Exception {
    aweUserDetails.setEnabled2fa(true).setSecret2fa("SECRET");
    lenient().when(menuService.getMenu()).thenReturn(new Menu());
    lenient().when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(totpConfigProperties.getEnabled()).thenReturn(statusType);
    ServiceData serviceData = accessService.login();
    assertEquals(3, serviceData.getClientActionList().size());
  }

  @ParameterizedTest
  @EnumSource(SecondFactorStatusType.class)
  void loginUserNot2FA(SecondFactorStatusType statusType) throws Exception {
    lenient().when(menuService.getMenu()).thenReturn(new Menu());
    lenient().when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(totpConfigProperties.getEnabled()).thenReturn(statusType);
    ServiceData serviceData = accessService.login();
    assertEquals(3, serviceData.getClientActionList().size());
  }

  @Test
  void verify2faCodeOk() throws Exception {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(totpService.verify2faCode(anyString())).thenReturn(true);
    when(menuService.getMenu()).thenReturn(new Menu());
    ServiceData serviceData = accessService.verify2faCode("code");
    assertEquals(3, serviceData.getClientActionList().size());
  }

  @Test
  void verify2faCodeNotValid() {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");
    when(totpService.verify2faCode(anyString())).thenReturn(false);
    assertThrows(AWException.class, () -> accessService.verify2faCode("code"));
  }

  @Test
  void logout() {
    ServiceData serviceData = accessService.logout();
    assertEquals(3, serviceData.getClientActionList().size());
  }

  @Test
  void isAuthenticated() throws Exception {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    assertFalse(accessService.isAuthenticated());
  }

  @Test
  void getProfileNameFileList() {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProfileList()).thenReturn(new HashSet<>(Arrays.asList("profile1", "profile2")));
    ServiceData serviceData = accessService.getProfileNameFileList();
    assertEquals(2, serviceData.getDataList().getRows().size());
  }

  @Test
  void encryptText() throws Exception {
    ServiceData serviceData = accessService.encryptText("test", "4W3M42T3RK3Y%$ED");
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  @Test
  void encryptProperty() {
    ReflectionTestUtils.setField(accessService, "jasyptPoolSize", 1);
    ServiceData serviceData = accessService.encryptProperty("test", "4W3M42T3RK3Y%$ED");
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  @Test
  void encryptPropertyWithoutKey() {
    ReflectionTestUtils.setField(accessService, "jasyptPoolSize", 1);
    when(securityConfigProperties.getMasterKey()).thenReturn("4W3M42T3RK3Y%$ED");
    ServiceData serviceData = accessService.encryptProperty("test", null);
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  @Test
  void loginWithAzureEntraID() {
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweRequest.getHttpRequest()).thenReturn(new MockHttpServletRequest());
    ServiceData serviceData = accessService.loginWithAzureEntraID();
    assertEquals(1, serviceData.getClientActionList().size());
    assertEquals(AZURE_OAUTH2_AUTHORIZATION_URL, serviceData.getClientActionList().get(0).getTarget());
  }

  @Test
  void givenOauth2Info_onAuthenticationSuccess_userAlreadyInDB() throws AWException {
    // Given
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "foo@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new OAuth2UserAuthority(attributeMap));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    Menu mockMenu = new Menu();
    mockMenu.setScreenContext("dummy");
    // When
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweUserDetailService.loadUserByEmail(anyString())).thenReturn(new AweUserDetails());
    when(menuService.getMenu()).thenReturn(mockMenu);
    // Then
    String initialUrl = accessService.onAuthenticationSuccess(oAuth2User);
    //Asserts
    verify(aweSession, times(1)).setParameter(eq(SESSION_INITIAL_URL), any());
    assertNotNull(initialUrl);
  }

  @Test
  void givenOauth2InfoWithRole_onAuthenticationSuccess_provisionNewUser() throws AWException {
    // Given
    Map<String, Object> attributeMap = Map.of(PREFERRED_USERNAME, "foo@acme.com");
    List<GrantedAuthority> grantedAuthorities = List.of(new OAuth2UserAuthority(attributeMap));
    DefaultOAuth2User oAuth2User = new DefaultOAuth2User(grantedAuthorities, attributeMap, PREFERRED_USERNAME);
    aweUserDetails.setEmail("foo@acme.com");
    Menu mockMenu = new Menu();
    mockMenu.setScreenContext("dummy");

    // When
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweUserDetailService.loadUserByEmail(anyString())).thenReturn(null);
    when(aweUserDetailService.loadUserByRole(oAuth2User)).thenReturn(aweUserDetails);
    when(securityConfigProperties.isAutoProvisionUser()).thenReturn(true);
    when(menuService.getMenu()).thenReturn(mockMenu);
    when(baseConfigProperties.getLanguageDefault()).thenReturn("es-ES");
    // Then
    String initialUrl = accessService.onAuthenticationSuccess(oAuth2User);
    //Asserts
    verify(maintainService, times(1)).launchPrivateMaintain(eq(PROVISIONING_NEW_USER), any(ObjectNode.class));
    verify(aweSession, times(1)).setParameter(eq(SESSION_INITIAL_URL), any());
    assertNotNull(initialUrl);
  }
}