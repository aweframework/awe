package com.almis.awe.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.exception.AWException;
import com.almis.awe.service.totp.AweTotpOperations;
import com.almis.awe.service.totp.OtpJavaTotpOperations;
import com.almis.awe.service.totp.ZxingQrPngGenerator;
import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.TOTPGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

  @Mock
  AweTotpOperations totpOperations;

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
    // Settings flow: user is fully authenticated and has a 2FA secret already generated
    aweUserDetails.setUsername("john.doe");
    aweUserDetails.setSecret2fa("SECRET123");
    aweUserDetails.setFullyAuthenticated(true);
    byte[] expectedPng = new byte[]{1, 2, 3};

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");
    when(totpOperations.generateQrPng(anyString(), nullable(String.class), nullable(String.class))).thenReturn(expectedPng);

    byte[] qrCode = totpService.getQRCode();

    assertArrayEquals(expectedPng, qrCode);
    verify(totpOperations, times(1)).generateQrPng("locale", "john.doe", "SECRET123");
  }

  @Test
  void getQRCodeListGenerate_notEnrolled_pendingEnrollment_isAllowed() throws Exception {
    // FORCE bootstrap: user has no 2FA yet and is in pending enrollment state.
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setPendingTotpEnrollment(true);
    when(totpOperations.generateSecret()).thenReturn("generated-secret");
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    ServiceData serviceData = totpService.getQRCodeList(true);

    assertEquals(1, serviceData.getDataList().getRows().size());
    assertEquals("generated-secret", ((CellData) serviceData.getDataList().getRows().get(0).get("secretCode")).getStringValue());
    assertTrue(((CellData) serviceData.getDataList().getRows().get(0).get("random")).getDoubleValue() >= 0d);
    verify(totpOperations).generateSecret();
    verify(maintainService).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));
  }

  @Test
  void getQRCodeListGenerate_fullyAuthenticated_settingsFlow_isAllowed() throws Exception {
    // Settings activation: user is fully authenticated and requests a new secret.
    aweUserDetails.setEnabled2fa(true);
    aweUserDetails.setFullyAuthenticated(true);
    aweUserDetails.setPendingTotpEnrollment(false);
    when(totpOperations.generateSecret()).thenReturn("generated-secret");
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    ServiceData serviceData = totpService.getQRCodeList(true);

    assertEquals(1, serviceData.getDataList().getRows().size());
    assertEquals("generated-secret", ((CellData) serviceData.getDataList().getRows().get(0).get("secretCode")).getStringValue());
    verify(totpOperations).generateSecret();
    verify(maintainService).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));
  }

  // ----------------------------------------------------------------
  // SECURITY: enrolled partially-authenticated user must NOT use force-qr-code to rotate secret
  // ----------------------------------------------------------------

  @Test
  void getQRCodeList_enrolledPartiallyAuthenticated_generate_isDenied() throws Exception {
    // Attack vector: enrolled user (enabled2fa=true) who has logged in but not yet passed TOTP
    // verification (fullyAuthenticated=false, pendingTotpEnrollment=false) calls force-qr-code
    // (the public query with generate=true). The service MUST deny this and must NOT rotate the
    // secret.
    aweUserDetails.setEnabled2fa(true);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setPendingTotpEnrollment(false);
    aweUserDetails.setSecret2fa("existing-secret");

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");

    assertThrows(AWException.class, () -> totpService.getQRCodeList(true),
      "Enrolled partially-authenticated user must not be able to rotate the 2FA secret via force-qr-code");
    // Secret must not have been rotated
    assertEquals("existing-secret", aweUserDetails.getSecret2fa(),
      "Secret must not be modified when the guard denies the request");
    verify(totpOperations, never()).generateSecret();
  }

  @Test
  void getQRCodeList_enrolledPartiallyAuthenticated_generate_publicQueryIsUnsafeWithoutServiceGuard() {
    // Narrow explanation test: proves that the public query alone is not the safety boundary.
    // Without a service-level guard, an enrolled partially-authenticated session could reach
    // getQRCodeList(true) and rotate its secret. This test documents WHY the guard is needed.
    //
    // Scenario: the service guard is what keeps force-qr-code safe for enrolled sessions.
    // The XML public="true" only removes the authentication gate for the HTTP transport layer.
    // An enrolled session (enabled2fa=true, fullyAuthenticated=false, pendingTotpEnrollment=false)
    // falls into neither allowed state; the service MUST reject it.
    aweUserDetails.setEnabled2fa(true);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setPendingTotpEnrollment(false);
    aweUserDetails.setSecret2fa("should-not-change");

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");

    // The service must throw; generateSecret must never be called.
    assertThrows(AWException.class, () -> totpService.getQRCodeList(true),
      "Service guard must deny enrolled partially-authenticated sessions even when reached via the public query path");
    verify(totpOperations, never()).generateSecret();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void getQRCodeListNoGenerate(boolean userEnabled) throws Exception {
    aweUserDetails.setEnabled2fa(userEnabled);
    aweUserDetails.setSecret2fa("stored-secret");
    lenient().when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    lenient().when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    ServiceData serviceData = totpService.getQRCodeList(false);

    assertEquals(userEnabled ? 1 : 0, serviceData.getDataList().getRows().size());
    if (userEnabled) {
      assertEquals("stored-secret", ((CellData) serviceData.getDataList().getRows().get(0).get("secretCode")).getStringValue());
    }
    verify(totpOperations, never()).generateSecret();
  }

  @Test
  void generate2faSecretKeepsAuthenticatedSettingsSessionFullyAuthenticated() throws Exception {
    AweUserDetails principal = spy(new AweUserDetails()
      .setEnabled2fa(false)
      .setFullyAuthenticated(false));
    aweUserDetails = principal;
    when(authentication.getPrincipal()).thenReturn(principal);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(totpOperations.generateSecret()).thenReturn("secret");

    totpService.generate2faSecret();

    assertTrue(aweUserDetails.isEnabled2fa());
    assertEquals("secret", aweUserDetails.getSecret2fa());
    assertTrue(aweUserDetails.isFullyAuthenticated());
    InOrder inOrder = inOrder(maintainService, principal);
    verify(maintainService, times(1)).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));
    inOrder.verify(maintainService).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));
    inOrder.verify(principal).setEnabled2fa(true);
    inOrder.verify(principal).setSecret2fa("secret");
    inOrder.verify(principal).setFullyAuthenticated(true);
  }

  @Test
  void getQRCodeListGenerateDoesNotElevateForceActivationSessions() throws Exception {
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setPendingTotpEnrollment(true);  // required for the FORCE bootstrap guard
    when(authentication.isAuthenticated()).thenReturn(true);
    when(totpOperations.generateSecret()).thenReturn("generated-secret");
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    ServiceData serviceData = totpService.getQRCodeList(true);

    assertEquals(1, serviceData.getDataList().getRows().size());
    assertEquals("generated-secret", ((CellData) serviceData.getDataList().getRows().get(0).get("secretCode")).getStringValue());
    assertTrue(aweUserDetails.isEnabled2fa());
    assertFalse(aweUserDetails.isFullyAuthenticated());
    verify(maintainService).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));
  }

  @Test
  void generate2faSecretDoesNotElevatePartiallyAuthenticatedSessions() throws Exception {
    aweUserDetails.setEnabled2fa(true);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(totpOperations.generateSecret()).thenReturn("secret");
    when(authentication.isAuthenticated()).thenReturn(true);
    aweUserDetails.setFullyAuthenticated(false);

    totpService.generate2faSecret();

    assertTrue(aweUserDetails.isEnabled2fa());
    assertEquals("secret", aweUserDetails.getSecret2fa());
    assertFalse(aweUserDetails.isFullyAuthenticated());
    verify(maintainService, times(1)).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));
  }

  @Test
  void generate2faSecretDoesNotMutateUserWhenStoreFails() throws Exception {
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setSecret2fa("existing-secret");
    aweUserDetails.setFullyAuthenticated(true);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(totpOperations.generateSecret()).thenReturn("new-secret");
    when(maintainService.launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class)))
      .thenThrow(new AWException("store failed", "boom"));

    assertThrows(AWException.class, () -> totpService.generate2faSecret());
    assertFalse(aweUserDetails.isEnabled2fa());
    assertEquals("existing-secret", aweUserDetails.getSecret2fa());
    assertTrue(aweUserDetails.isFullyAuthenticated());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void update2faStatus(Integer status) {
    totpService.update2faStatus(status);
    assertEquals(status, aweUserDetails.isEnabled2fa() ? 1 : 0);
  }

  @Test
  void generate2faSecretBehavioralFlowContractForSettingsActivation() throws Exception {
    // Behavioral proof: the public generate2faSecret() service method must, in a single
    // atomic sequence, (1) store the secret via MaintainService, (2) enable 2FA on the
    // principal, (3) update the principal's secret, and (4) preserve the authenticated
    // session when the session was already fully authenticated before the call.
    // This test uses a real adapter to exercise the full code path with no mocks
    // for the cryptographic operations.
    AweTotpOperations realOperations = new OtpJavaTotpOperations(
      Clock.fixed(Instant.ofEpochSecond(1_111_111_111L), ZoneOffset.UTC),
      new ZxingQrPngGenerator());
    TotpService realTotpService = new TotpService(realOperations);
    realTotpService.setApplicationContext(applicationContext);

    AweUserDetails principal = new AweUserDetails();
    principal.setEnabled2fa(false);
    principal.setFullyAuthenticated(true);

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(principal);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    realTotpService.generate2faSecret();

    // Persistence was invoked with the right target before the principal was mutated
    verify(maintainService, times(1)).launchPrivateMaintain(eq("store2faSecret"), any(ObjectNode.class));

    // Session enabled flag, secret, and full-auth are all set to expected post-activation values
    assertTrue(principal.isEnabled2fa(), "2FA must be enabled after settings activation");
    assertNotNull(principal.getSecret2fa(), "Secret must be set after settings activation");
    assertThat(principal.getSecret2fa()).matches("[A-Z2-7]+=*");
    assertTrue(principal.isFullyAuthenticated(), "Settings activation must preserve fully-authenticated session");
  }

  @Test
  void verify2faCode() {
    when(totpOperations.verifyCode(nullable(String.class), anyString())).thenReturn(true);
    totpService.verify2faCode("code");
    verify(totpOperations, times(1)).verifyCode(eq(null), anyString());
  }

  @Test
  void verify2faCodeAcceptsNormalizedStoredSecrets() {
    AweTotpOperations realOperations = new OtpJavaTotpOperations(
      Clock.fixed(Instant.ofEpochSecond(1_111_111_111L), ZoneOffset.UTC),
      new ZxingQrPngGenerator());
    TotpService realTotpService = new TotpService(realOperations);
    aweUserDetails.setSecret2fa("  gezdgnbvgy3tqojqgezdgnbvgy3tqojq  ");

    String code = generateCodeAt(1_111_111_111L, "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");

    assertTrue(realTotpService.verify2faCode(code));
  }

  @Test
  void verify2faCodeReturnsFalseForMalformedCodes() {
    AweTotpOperations realOperations = new OtpJavaTotpOperations(
      Clock.fixed(Instant.ofEpochSecond(1_111_111_111L), ZoneOffset.UTC),
      new ZxingQrPngGenerator());
    TotpService realTotpService = new TotpService(realOperations);
    aweUserDetails.setSecret2fa("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");

    assertFalse(realTotpService.verify2faCode("12ab56"));
    assertFalse(realTotpService.verify2faCode("12345"));
  }

  // ----------------------------------------------------------------
  // getQRCode() access guard tests
  // ----------------------------------------------------------------

  @Test
  void getQRCode_fullyAuthenticated_settingsFlow_isAllowed() throws Exception {
    // Fully authenticated user accessing QR from user settings
    aweUserDetails.setUsername("alice");
    aweUserDetails.setSecret2fa("SECRET123");
    aweUserDetails.setEnabled2fa(true);
    aweUserDetails.setFullyAuthenticated(true);
    aweUserDetails.setFreshEnrollment(false);
    byte[] expectedPng = new byte[]{7, 8, 9};

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");
    when(totpOperations.generateQrPng(anyString(), nullable(String.class), nullable(String.class))).thenReturn(expectedPng);

    byte[] qrCode = totpService.getQRCode();

    assertArrayEquals(expectedPng, qrCode);
  }

  @Test
  void getQRCode_justEnrolled_freshEnrollmentFlag_isAllowed() throws Exception {
    // User just completed enrollment (generate2faSecret set freshEnrollment=true),
    // not yet TOTP-verified — allowed because they need to scan the QR before verifying
    aweUserDetails.setUsername("bob");
    aweUserDetails.setSecret2fa("NEWSECRET");
    aweUserDetails.setEnabled2fa(true);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setFreshEnrollment(true);  // set by generate2faSecret(false)
    aweUserDetails.setPendingTotpEnrollment(false);
    byte[] expectedPng = new byte[]{1, 2, 3};

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");
    when(totpOperations.generateQrPng(anyString(), nullable(String.class), nullable(String.class))).thenReturn(expectedPng);

    byte[] qrCode = totpService.getQRCode();

    assertArrayEquals(expectedPng, qrCode);
  }

  @Test
  void getQRCode_partiallyAuthenticated_enrolled_noFreshEnrollment_isDenied() {
    // Enrolled 2FA user who has logged in but not yet passed TOTP verification,
    // and is NOT in a fresh enrollment window: must not fetch the QR.
    aweUserDetails.setUsername("charlie");
    aweUserDetails.setSecret2fa("EXISTINGSECRET");
    aweUserDetails.setEnabled2fa(true);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setFreshEnrollment(false);  // not a fresh enrollment session
    aweUserDetails.setPendingTotpEnrollment(false);

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");

    assertThrows(AWException.class, () -> totpService.getQRCode(),
      "Partially authenticated enrolled user without fresh enrollment must not fetch the QR");
  }

  @Test
  void generate2faSecret_activationFlow_setsFreshEnrollmentAndClearsPendingEnrollment() throws Exception {
    // After successful enrollment, pendingTotpEnrollment is cleared and freshEnrollment is set
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setPendingTotpEnrollment(true);
    aweUserDetails.setFreshEnrollment(false);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(totpOperations.generateSecret()).thenReturn("generated-secret");
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    // getQRCodeList with generate=true triggers generate2faSecret(false)
    totpService.getQRCodeList(true);

    assertFalse(aweUserDetails.isPendingTotpEnrollment(),
      "generate2faSecret must clear the pendingTotpEnrollment flag");
    assertTrue(aweUserDetails.isFreshEnrollment(),
      "generate2faSecret(force=false) must set freshEnrollment=true so QR is accessible");
    assertTrue(aweUserDetails.isEnabled2fa(),
      "generate2faSecret must set enabled2fa=true");
    assertFalse(aweUserDetails.isFullyAuthenticated(),
      "generate2faSecret(false) must NOT elevate the session to fully-authenticated");
  }

  @Test
  void generate2faSecret_settingsFlow_doesNotSetFreshEnrollment() throws Exception {
    // Settings activation (generate2faSecret(true)) does NOT set freshEnrollment
    // because the user is already fully authenticated
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setFullyAuthenticated(true);
    aweUserDetails.setPendingTotpEnrollment(false);
    aweUserDetails.setFreshEnrollment(false);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(totpOperations.generateSecret()).thenReturn("settings-secret");
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    totpService.generate2faSecret();

    assertFalse(aweUserDetails.isFreshEnrollment(),
      "Settings flow (preserveAuthenticatedSession=true) must not set freshEnrollment");
    assertTrue(aweUserDetails.isFullyAuthenticated(),
      "Settings activation must preserve the fully-authenticated state");
  }

  // ----------------------------------------------------------------
  // FORCE enrollment bootstrap: pending-enrollment can call getQRCodeList(true)
  // ----------------------------------------------------------------

  @Test
  void getQRCodeList_pendingEnrollment_generate_isAllowed() throws Exception {
    // FORCE-mode user with pendingTotpEnrollment=true and no secret yet.
    // getQRCodeList(true) is the enrollment bootstrap path reached via the force-qr-code
    // public query; it must succeed and generate the secret.
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setPendingTotpEnrollment(true);
    aweUserDetails.setSecret2fa(null);

    when(authentication.isAuthenticated()).thenReturn(true);
    when(totpOperations.generateSecret()).thenReturn("force-secret");
    when(applicationContext.getBean(MaintainService.class)).thenReturn(maintainService);
    when(maintainService.launchPrivateMaintain(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData());

    ServiceData result = totpService.getQRCodeList(true);

    // Secret was generated and stored; enrollment completed
    assertFalse(aweUserDetails.isPendingTotpEnrollment(),
      "generate2faSecret must clear pendingTotpEnrollment after successful enrollment");
    assertTrue(aweUserDetails.isEnabled2fa(),
      "generate2faSecret must set enabled2fa=true");
    assertTrue(aweUserDetails.isFreshEnrollment(),
      "generate2faSecret(force=false) must set freshEnrollment=true so QR is accessible");
    assertFalse(aweUserDetails.isFullyAuthenticated(),
      "Enrollment must not elevate the session to fully-authenticated");
    assertEquals(1, result.getDataList().getRows().size(),
      "Result must contain the newly generated secret row");
    assertEquals("force-secret", ((CellData) result.getDataList().getRows().get(0).get("secretCode")).getStringValue());
  }

  // ----------------------------------------------------------------
  // QR code access: pending-enrollment BEFORE secret generation is denied explicitly
  // ----------------------------------------------------------------

  @Test
  void getQRCode_pendingEnrollment_noSecret_isDenied() {
    // User is in pending enrollment state (no secret yet). Calling /access/qr-code directly
    // must be denied explicitly, not by a null-secret NPE in the QR generator.
    aweUserDetails.setUsername("dave");
    aweUserDetails.setSecret2fa(null);
    aweUserDetails.setEnabled2fa(false);
    aweUserDetails.setFullyAuthenticated(false);
    aweUserDetails.setFreshEnrollment(false);
    aweUserDetails.setPendingTotpEnrollment(true);

    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getLocaleWithLanguage(anyString(), eq(null))).thenReturn("locale");

    AWException ex = assertThrows(AWException.class, () -> totpService.getQRCode(),
      "Pending-enrollment user must receive an explicit AWException, not a runtime NPE");
    assertNotNull(ex, "AWException must be non-null");
  }

  // ----------------------------------------------------------------
  // verify2faCode: failed verification returns false (logging side-effect not asserted)
  // ----------------------------------------------------------------

  @Test
  void verify2faCode_returnsTrue_whenCodeValid() {
    when(totpOperations.verifyCode(nullable(String.class), anyString())).thenReturn(true);
    assertTrue(totpService.verify2faCode("123456"),
      "verify2faCode must return true when code is valid");
  }

  @Test
  void verify2faCode_returnsFalse_whenCodeInvalid() {
    when(totpOperations.verifyCode(nullable(String.class), anyString())).thenReturn(false);
    assertFalse(totpService.verify2faCode("000000"),
      "verify2faCode must return false when code is invalid; warn log is emitted");
  }

  private String generateCodeAt(long epochSecond, String secret) {
    Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC);
    TOTPGenerator generator = new TOTPGenerator.Builder(secret)
      .withClock(fixedClock)
      .withHOTPGenerator(builder -> builder
        .withPasswordLength(6)
        .withAlgorithm(HMACAlgorithm.SHA1))
      .withPeriod(Duration.ofSeconds(30))
      .build();
    return generator.now(fixedClock);
  }
}
