package com.almis.awe.service.totp;

import java.net.URI;

public interface AweTotpOperations {

  String generateSecret();

  boolean verifyCode(String secret, String code);

  URI buildOtpAuthUri(String issuer, String accountName, String secret);

  byte[] generateQrPng(String issuer, String accountName, String secret);
}
