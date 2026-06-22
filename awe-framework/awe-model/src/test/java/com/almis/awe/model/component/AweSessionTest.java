package com.almis.awe.model.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AweSession#isAuthenticated()} 2FA authorization policy.
 *
 * <p>Policy under test:
 * <ul>
 *   <li>FORCE pre-enrollment ({@code pendingTotpEnrollment=true}) → always denied.</li>
 *   <li>Enrolled 2FA with TOTP verified ({@code enabled2fa=true, fullyAuthenticated=true}) → allowed.</li>
 *   <li>Enrolled 2FA without TOTP verification ({@code enabled2fa=true, fullyAuthenticated=false}) → denied.</li>
 *   <li>No 2FA ({@code enabled2fa=false, pendingTotpEnrollment=false}) → delegates to Spring.</li>
 *   <li>Non-AweUserDetails principal → delegates to Spring.</li>
 *   <li>Anonymous authentication → denied.</li>
 *   <li>Null authentication → denied.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AweSessionTest {

  @Mock
  private SecurityContext securityContext;

  private AweSession aweSession;

  @BeforeEach
  void setUp() {
    aweSession = new AweSession();
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // --- FORCE pre-enrollment ---

  @Test
  void forceMode_pendingEnrollment_isNotAuthenticated() {
    AweUserDetails principal = pendingEnrollmentPrincipal();
    Authentication auth = springAuthenticated(principal);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertFalse(aweSession.isAuthenticated(),
      "FORCE pre-enrollment user must not pass the isAuthenticated gate");
  }

  @Test
  void forceMode_pendingEnrollment_withEnabled2fa_isNotAuthenticated() {
    // Edge: pendingTotpEnrollment=true takes priority even if enabled2fa is also true
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(true)
      .setFullyAuthenticated(false)
      .setPendingTotpEnrollment(true);
    Authentication auth = springAuthenticated(principal);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertFalse(aweSession.isAuthenticated(),
      "pendingTotpEnrollment=true must override any other flag and deny access");
  }

  // --- Enrolled 2FA ---

  @Test
  void enrolled2fa_totpVerified_isAuthenticated() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(true)
      .setFullyAuthenticated(true)
      .setPendingTotpEnrollment(false);
    Authentication auth = springAuthenticated(principal);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertTrue(aweSession.isAuthenticated(),
      "Enrolled 2FA user who has completed TOTP verification must be authenticated");
  }

  @Test
  void enrolled2fa_totpNotYetVerified_isNotAuthenticated() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(true)
      .setFullyAuthenticated(false)
      .setPendingTotpEnrollment(false);
    Authentication auth = springAuthenticated(principal);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertFalse(aweSession.isAuthenticated(),
      "Enrolled 2FA user who has not yet passed TOTP verification must be denied");
  }

  // --- No 2FA (DISABLED or OPTIONAL without enrollment) ---

  @Test
  void no2fa_springAuthenticated_isAuthenticated() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(false)
      .setFullyAuthenticated(false)
      .setPendingTotpEnrollment(false);
    Authentication auth = springAuthenticated(principal);
    when(securityContext.getAuthentication()).thenReturn(auth);

    assertTrue(aweSession.isAuthenticated(),
      "User without 2FA in DISABLED or OPTIONAL mode must be authenticated via Spring flag");
  }

  @Test
  void no2fa_springNotAuthenticated_isNotAuthenticated() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(false)
      .setFullyAuthenticated(false)
      .setPendingTotpEnrollment(false);
    Authentication auth = mock(Authentication.class);
    when(auth).thenReturn(auth);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn(principal);
    when(auth.isAuthenticated()).thenReturn(false);

    assertFalse(aweSession.isAuthenticated(),
      "User without 2FA where Spring reports not-authenticated must be denied");
  }

  // --- Non-AweUserDetails principal ---

  @Test
  void nonAweUserDetails_springAuthenticated_isAuthenticated() {
    Authentication auth = mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn("some-non-awe-principal");
    when(auth.isAuthenticated()).thenReturn(true);

    assertTrue(aweSession.isAuthenticated(),
      "Non-AweUserDetails principal should delegate to Spring's isAuthenticated()");
  }

  @Test
  void nonAweUserDetails_springNotAuthenticated_isNotAuthenticated() {
    Authentication auth = mock(Authentication.class);
    when(securityContext.getAuthentication()).thenReturn(auth);
    when(auth.getPrincipal()).thenReturn("some-non-awe-principal");
    when(auth.isAuthenticated()).thenReturn(false);

    assertFalse(aweSession.isAuthenticated(),
      "Non-AweUserDetails principal where Spring reports not-authenticated must be denied");
  }

  // --- Anonymous and null ---

  @Test
  void anonymousAuthentication_isNotAuthenticated() {
    AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken(
      "anon", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    when(securityContext.getAuthentication()).thenReturn(anon);

    assertFalse(aweSession.isAuthenticated(),
      "Anonymous authentication must always be denied");
  }

  @Test
  void nullAuthentication_isNotAuthenticated() {
    when(securityContext.getAuthentication()).thenReturn(null);

    assertFalse(aweSession.isAuthenticated(),
      "Null authentication must be denied");
  }

  // --- Named predicate coverage ---

  @Test
  void isAwaitingTotpEnrollment_true_whenPendingEnrollment() {
    AweUserDetails principal = pendingEnrollmentPrincipal();
    assertTrue(principal.isAwaitingTotpEnrollment(),
      "isAwaitingTotpEnrollment() must return true when pendingTotpEnrollment=true");
  }

  @Test
  void isAwaitingTotpEnrollment_false_whenNotPending() {
    AweUserDetails principal = new AweUserDetails().setPendingTotpEnrollment(false);
    assertFalse(principal.isAwaitingTotpEnrollment(),
      "isAwaitingTotpEnrollment() must return false when pendingTotpEnrollment=false");
  }

  @Test
  void hasTotpVerified_true_whenEnrolledAndFullyAuthenticated() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(true)
      .setFullyAuthenticated(true);
    assertTrue(principal.hasTotpVerified(),
      "hasTotpVerified() must return true when enrolled and fullyAuthenticated");
  }

  @Test
  void hasTotpVerified_false_whenEnrolledButNotVerified() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(true)
      .setFullyAuthenticated(false);
    assertFalse(principal.hasTotpVerified(),
      "hasTotpVerified() must return false when enrolled but not TOTP-verified");
  }

  @Test
  void hasTotpVerified_false_whenNotEnrolled() {
    AweUserDetails principal = new AweUserDetails()
      .setEnabled2fa(false)
      .setFullyAuthenticated(true);
    assertFalse(principal.hasTotpVerified(),
      "hasTotpVerified() must return false when 2FA is not enabled, even if fullyAuthenticated=true");
  }

  @Test
  void mayFetchQrCode_true_whenFullyAuthenticated() {
    AweUserDetails principal = new AweUserDetails()
      .setFullyAuthenticated(true)
      .setFreshEnrollment(false);
    assertTrue(principal.mayFetchQrCode(),
      "mayFetchQrCode() must return true for a fully authenticated user (settings flow)");
  }

  @Test
  void mayFetchQrCode_true_whenFreshEnrollment() {
    AweUserDetails principal = new AweUserDetails()
      .setFullyAuthenticated(false)
      .setFreshEnrollment(true);
    assertTrue(principal.mayFetchQrCode(),
      "mayFetchQrCode() must return true in a fresh-enrollment window");
  }

  @Test
  void mayFetchQrCode_false_whenNeitherFullyAuthNorFreshEnrollment() {
    AweUserDetails principal = new AweUserDetails()
      .setFullyAuthenticated(false)
      .setFreshEnrollment(false);
    assertFalse(principal.mayFetchQrCode(),
      "mayFetchQrCode() must return false when neither fully authenticated nor in fresh-enrollment window");
  }

  // --- Helpers ---

  private AweUserDetails pendingEnrollmentPrincipal() {
    return new AweUserDetails()
      .setEnabled2fa(false)
      .setFullyAuthenticated(false)
      .setPendingTotpEnrollment(true);
  }

  private Authentication springAuthenticated(AweUserDetails principal) {
    return new UsernamePasswordAuthenticationToken(
      principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }
}
