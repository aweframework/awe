package com.almis.awe.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.ServiceData;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

  @Mock
  SecretGenerator secretGenerator;

  @Mock
  QrDataFactory qrDataFactory;

  @Mock
  QrGenerator qrGenerator;

  @Mock
  CodeVerifier codeVerifier;

  @Mock
  SecurityContext securityContext;

  @Mock
  Authentication authentication;

  @Mock
  ApplicationContext applicationContext;

  @Mock
  MaintainService maintainService;

  @Mock
  AweElements aweElements;

  @InjectMocks
  TotpService totpService;

  private AweUserDetails aweUserDetails;

  @BeforeEach
  void setUp() {
    aweUserDetails = new AweUserDetails();
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(aweUserDetails);
    SecurityContextHolder.setContext(securityContext);
    totpService.setApplicationContext(applicationContext);
  }

  @Test
  void getQRCode() throws Exception {
    when(qrDataFactory.newBuilder()).thenReturn(new QrData.Builder());
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");
    totpService.getQRCode();
    verify(qrGenerator, times(1)).generate(any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void getQRCodeListGenerate(boolean userEnabled) throws Exception {
    aweUserDetails.setEnabled2fa(userEnabled);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
    ServiceData serviceData = totpService.getQRCodeList(true);
    assertEquals(1, serviceData.getDataList().getRows().size());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void getQRCodeListNoGenerate(boolean userEnabled) throws Exception {
    aweUserDetails.setEnabled2fa(userEnabled);
    lenient().when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    lenient().when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());
    ServiceData serviceData = totpService.getQRCodeList(false);
    assertEquals(userEnabled ? 1 : 0, serviceData.getDataList().getRows().size());
  }

  @Test
  void generate2faSecret() throws Exception {
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    totpService.generate2faSecret();
    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void update2faStatus(Integer status) {
    totpService.update2faStatus(status);
    assertEquals(status, aweUserDetails.isEnabled2fa() ? 1 : 0);
  }

  @Test
  void verify2faCode() {
    totpService.verify2faCode("code");
    verify(codeVerifier, times(1)).isValidCode(eq(null), anyString());
  }
}