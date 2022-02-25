package com.almis.awe.service;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweUserDetails;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Manage application accesses
 */
public class TotpService extends ServiceConfig {

  // Autowire
  private final SecretGenerator secretGenerator;
  private final QrDataFactory qrDataFactory;
  private final QrGenerator qrGenerator;
  private final CodeVerifier codeVerifier;

  /**
   * Autowired constructor
   * @param secretGenerator Secret generator
   * @param qrDataFactory QR Data factory
   * @param qrGenerator QR Code generator
   * @param codeVerifier TOTP Code verifier
   */
  public TotpService(SecretGenerator secretGenerator, QrDataFactory qrDataFactory, QrGenerator qrGenerator,
                     CodeVerifier codeVerifier) {
    this.secretGenerator = secretGenerator;
    this.qrDataFactory = qrDataFactory;
    this.qrGenerator = qrGenerator;
    this.codeVerifier = codeVerifier;
  }

  /**
   * Retrieve QR Code in PNG format as String
   *
   * @return QR Code as string
   */
  public byte[] getQRCode() throws QrGenerationException {
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    QrData qrData = qrDataFactory.newBuilder()
      .label(userDetails.getUsername())
      .issuer(getLocale("APP_NAME"))
      .secret(userDetails.getSecret2fa())
      .build();
    return qrGenerator.generate(qrData);
  }

  /**
   * Generate QR code
   *
   * @return Service data
   */
  public ServiceData getQRCodeList(Boolean generate) throws AWException {
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    ServiceData serviceData = new ServiceData();
    if (userDetails.isEnabled2fa() || Boolean.TRUE.equals(generate)) {

      // Generate if forced
      if (Boolean.TRUE.equals(generate)) {
        serviceData = generate2faSecret();
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
   * Generate secret code
   *
   * @return Service data
   */
  public ServiceData generate2faSecret() throws AWException {
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

    // Generate secret
    String secret = secretGenerator.generate();

    // Store secret into database and authentication
    userDetails.setEnabled2fa(true);
    userDetails.setSecret2fa(secret);
    return getBean(MaintainService.class).launchPrivateMaintain("store2faSecret", JsonNodeFactory.instance.objectNode()
      .put("user", userDetails.getUsername())
      .put("secret", secret)
    );
  }

  /**
   * Update 2fa status
   *
   * @return Service data
   */
  public ServiceData update2faStatus(Integer enabled) {
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());

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
    AweUserDetails userDetails = ((AweUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    return codeVerifier.isValidCode(userDetails.getSecret2fa(), code);
  }
}
