package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.service.totp.AweTotpOperations;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Manage application accesses
 */
@Slf4j
public class TotpService extends ServiceConfig {

  private static final String ERROR_TITLE_ACCESS_DENIED = "ERROR_TITLE_ACCESS_DENIED";
  private static final String ERROR_MESSAGE_FORBIDDEN_ACCESS = "ERROR_MESSAGE_FORBIDDEN_ACCESS";

  // Autowire
  private final AweTotpOperations totpOperations;

  /**
   * Autowired constructor
   * @param totpOperations TOTP operations
   */
  public TotpService(AweTotpOperations totpOperations) {
    this.totpOperations = totpOperations;
  }

  /**
   * Generate the authenticated user's TOTP QR code as PNG bytes.
   * <p>
   * Access policy is enforced by {@link #enforceQrCodeAccess(AweUserDetails)}.
   * Any runtime failure in QR generation (invalid secret, encoding error) is
   * logged at ERROR level and re-thrown so the request handler returns a
   * visible error response.
   *
   * @return QR code PNG bytes
   * @throws AWException when the current session is not allowed to retrieve the QR,
   *                     or when QR generation fails at runtime
   */
  public byte[] getQRCode() throws AWException {
    AweUserDetails userDetails = currentUserDetails();
    enforceQrCodeAccess(userDetails);
    try {
      return totpOperations.generateQrPng(getLocale("APP_NAME"), userDetails.getUsername(), userDetails.getSecret2fa());
    } catch (Exception e) {
      log.error("TOTP QR generation failed for user '{}': {}", userDetails.getUsername(), e.getMessage(), e);
      if (e instanceof AWException aw) {
        throw aw;
      }
      AWException wrapper = new AWException(getLocale("ERROR_TITLE_RETRIEVING_DATA"), getLocale(ERROR_MESSAGE_FORBIDDEN_ACCESS), e);
      wrapper.setType(AnswerType.ERROR);
      throw wrapper;
    }
  }

  /**
   * Enforce that the QR code endpoint is only reachable in allowed flows.
   * <p>
   * Allowed when {@link AweUserDetails#mayFetchQrCode()} returns true:
   * <ul>
   *   <li>Fully authenticated (settings activation flow).</li>
   *   <li>Fresh enrollment: a secret was just generated in this session and the QR has
   *       not yet been presented ({@code freshEnrollment=true}).</li>
   * </ul>
   * Explicitly denied when:
   * <ul>
   *   <li>Pending TOTP enrollment (no secret generated yet) — would cause a null-secret
   *       runtime failure and is not a valid QR access state.</li>
   *   <li>Enrolled but not yet TOTP-verified and not in a fresh-enrollment window —
   *       the re-login case where the user must verify before accessing the QR again.</li>
   * </ul>
   *
   * @param userDetails current principal
   * @throws AWException when access to the QR code is not permitted
   */
  private void enforceQrCodeAccess(AweUserDetails userDetails) throws AWException {
    if (userDetails.isAwaitingTotpEnrollment()) {
      // Pending enrollment: no secret has been generated yet. Deny explicitly rather than
      // letting a null-secret reach the QR generator (which would be a confusing runtime error).
      log.warn("QR code access denied for user '{}': pending TOTP enrollment (no secret generated yet)",
        userDetails.getUsername());
      throw new AWException(
        getLocale(ERROR_TITLE_ACCESS_DENIED),
        getLocale(ERROR_MESSAGE_FORBIDDEN_ACCESS),
        AnswerType.ERROR
      );
    }
    if (!userDetails.mayFetchQrCode()) {
      // Enrolled 2FA user who is not yet TOTP-verified and is not in a fresh-enrollment window.
      // This is the re-login case; the user must verify their existing code first.
      log.warn("QR code access denied for user '{}': enrolled 2FA session not yet verified (fullyAuthenticated={}, freshEnrollment={})",
        userDetails.getUsername(), userDetails.isFullyAuthenticated(), userDetails.isFreshEnrollment());
      throw new AWException(
        getLocale(ERROR_TITLE_ACCESS_DENIED),
        getLocale(ERROR_MESSAGE_FORBIDDEN_ACCESS),
        AnswerType.ERROR
      );
    }
  }

  /**
   * Return the data needed by the TOTP QR screen.
   * <p>
   * When {@code generate=true}, this is the enrollment path. Access is guarded by
   * {@link #enforceGenerateAccess(AweUserDetails)} before any secret generation happens.
   * Allowed only in two explicitly defined states:
   * <ul>
   *   <li>Pending TOTP enrollment ({@link AweUserDetails#isAwaitingTotpEnrollment()}) — FORCE
   *       activation flow where the secret hasn't been generated yet.</li>
   *   <li>Fully authenticated ({@code fullyAuthenticated=true}) — settings
   *       activation flow where the user is already in a private session.</li>
   * </ul>
   * All other sessions (including enrolled users who have not yet passed TOTP verification)
   * are explicitly denied to prevent secret rotation before TOTP verification.
   * <p>
   * When {@code generate=false}, returns data only if 2FA is already enabled.
   *
   * @param generate true to generate a new secret, false to return existing data
   * @return Service data with secret code
   * @throws AWException when the current session is not allowed to generate a new secret
   */
  public ServiceData getQRCodeList(Boolean generate) throws AWException {
    AweUserDetails userDetails = currentUserDetails();
    ServiceData serviceData = new ServiceData();
    if (userDetails.isEnabled2fa() || Boolean.TRUE.equals(generate)) {

      // Generate if forced — guarded: only allowed for pending enrollment or fully authenticated sessions
      if (Boolean.TRUE.equals(generate)) {
        enforceGenerateAccess(userDetails);
        serviceData = generate2faSecret(false);
      }

      // Generate retrieval codes
      DataList qrCode = new DataList();
      DataListUtil.addColumnWithOneRow(qrCode, "secretCode", userDetails.getSecret2fa());
      DataListUtil.addColumnWithOneRow(qrCode, "random", Math.random() * 10000);

      return serviceData.setDataList(qrCode);
    }

    return serviceData.setDataList(new DataList());
  }

  /**
   * Enforce that the secret-generation path ({@code generate=true}) is only reachable
   * in legitimately allowed enrollment states.
   * <p>
   * Allowed when:
   * <ul>
   *   <li>The user is in the FORCE-mode pre-enrollment window
   *       ({@link AweUserDetails#isAwaitingTotpEnrollment()}) — they have authenticated
   *       but do not yet have a TOTP secret; this is the only valid bootstrap state for the
   *       {@code force-qr-code} public query.</li>
   *   <li>The user is fully authenticated ({@code fullyAuthenticated=true}) —
   *       they are in a private settings session and may regenerate their own secret.</li>
   * </ul>
   * Explicitly denied in all other states, most critically when an enrolled user
   * ({@code enabled2fa=true}) is only partially authenticated (has logged in but not yet
   * passed TOTP verification). In that state the session must NOT be allowed to rotate
   * the existing secret, which would bypass the verification requirement.
   *
   * @param userDetails current principal
   * @throws AWException when the current session state is not allowed to generate a new secret
   */
  private void enforceGenerateAccess(AweUserDetails userDetails) throws AWException {
    if (!userDetails.isAwaitingTotpEnrollment() && !userDetails.isFullyAuthenticated()) {
      // Partially authenticated enrolled session: deny secret rotation before TOTP verification.
      log.warn("Secret generation denied for user '{}': session is not in an allowed enrollment state "
          + "(pendingTotpEnrollment={}, fullyAuthenticated={})",
        userDetails.getUsername(), userDetails.isAwaitingTotpEnrollment(), userDetails.isFullyAuthenticated());
      throw new AWException(
        getLocale(ERROR_TITLE_ACCESS_DENIED),
        getLocale(ERROR_MESSAGE_FORBIDDEN_ACCESS),
        AnswerType.ERROR
      );
    }
  }

  /**
   * Generate secret code
   *
   * @return Service data
   */
  public ServiceData generate2faSecret() throws AWException {
    return generate2faSecret(true);
  }

  private ServiceData generate2faSecret(boolean preserveAuthenticatedSession) throws AWException {
    AweUserDetails userDetails = currentUserDetails();
    Authentication authentication = currentAuthentication();
    boolean was2faEnabled = userDetails.isEnabled2fa();
    boolean wasFullyAuthenticated = userDetails.isFullyAuthenticated();
    boolean wasSpringAuthenticated = authentication != null && authentication.isAuthenticated();

    // Generate secret
    String secret = totpOperations.generateSecret();

    // Store secret first, then mirror the persisted state in the current session
    ServiceData serviceData;
    try {
      serviceData = getBean(MaintainService.class).launchPrivateMaintain("store2faSecret", JsonNodeFactory.instance.objectNode()
        .put("user", userDetails.getUsername())
        .put("secret", secret)
      );
    } catch (Exception e) {
      log.error("TOTP secret storage failed for user '{}': {}", userDetails.getUsername(), e.getMessage(), e);
      if (e instanceof AWException aw) {
        throw aw;
      }
      AWException wrapper = new AWException(getLocale("ERROR_TITLE_RETRIEVING_DATA"), getLocale(ERROR_MESSAGE_FORBIDDEN_ACCESS), e);
      wrapper.setType(AnswerType.ERROR);
      throw wrapper;
    }
    userDetails.setEnabled2fa(true);
    userDetails.setSecret2fa(secret);
    // Enrollment is complete: the user now has a secret and may proceed to the QR/verify step.
    // Clear the pre-enrollment gate so AweSession.isAuthenticated() no longer blocks.
    userDetails.setPendingTotpEnrollment(false);
    // Mark that a fresh secret was generated in this session: /access/qr-code is permitted
    // until the user completes TOTP verification (onLoginSuccess clears this flag).
    if (!preserveAuthenticatedSession) {
      userDetails.setFreshEnrollment(true);
    }
    userDetails.setFullyAuthenticated(wasFullyAuthenticated
      || preserveAuthenticatedSession && !was2faEnabled && wasSpringAuthenticated);
    return serviceData;
  }

  /**
   * Update 2fa status
   *
   * @return Service data
   */
  public ServiceData update2faStatus(Integer enabled) {
    AweUserDetails userDetails = currentUserDetails();

    // Update enabled status on user details
    userDetails.setEnabled2fa(enabled == 1);

    return new ServiceData();
  }

  /**
   * Verify if code is valid
   *
   * @param code Code to check
   * @return Code is valid or not
   */
  public boolean verify2faCode(String code) {
    AweUserDetails userDetails = currentUserDetails();
    boolean valid = totpOperations.verifyCode(userDetails.getSecret2fa(), code);
    if (!valid) {
      log.warn("TOTP verification failed for user '{}'", userDetails.getUsername());
    }
    return valid;
  }

  private AweUserDetails currentUserDetails() {
    return (AweUserDetails) currentAuthentication().getPrincipal();
  }

  private Authentication currentAuthentication() {
    return SecurityContextHolder.getContext().getAuthentication();
  }
}
