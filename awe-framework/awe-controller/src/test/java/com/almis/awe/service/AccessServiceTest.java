package com.almis.awe.service;

import com.almis.awe.config.TotpConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.type.SecondFactorStatusType;
import com.almis.awe.session.AweSessionDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
  Environment environment;

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
  void getProfileNameFileList() throws Exception {
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getProfileList()).thenReturn(new HashSet<>(Arrays.asList("profile1", "profile2")));
    ServiceData serviceData = accessService.getProfileNameFileList();
    assertEquals(2, serviceData.getDataList().getRows().size());
  }

  @Test
  void encryptText() throws Exception {
    when(environment.getProperty(eq("application.encoding"), anyString())).thenReturn("UTF-8");
    ServiceData serviceData = accessService.encryptText("test", "4W3M42T3RK3Y%$ED");
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  @Test
  void encryptProperty() {
    when(environment.getProperty(eq("application.encoding"), anyString())).thenReturn("UTF-8");
    ReflectionTestUtils.setField(accessService, "jasyptPoolSize", 1);
    ServiceData serviceData = accessService.encryptProperty("test", "4W3M42T3RK3Y%$ED");
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  @Test
  void encryptPropertyWithoutKey() {
    when(environment.getProperty(eq("application.encoding"), anyString())).thenReturn("UTF-8");
    ReflectionTestUtils.setField(accessService, "jasyptPoolSize", 1);
    ReflectionTestUtils.setField(accessService, "masterKey", "master");
    ServiceData serviceData = accessService.encryptProperty("test", null);
    assertEquals(1, serviceData.getDataList().getRows().size());
  }
}