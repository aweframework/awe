package com.almis.awe.service.totp;

import com.almis.awe.exception.AWERuntimeException;
import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.SecretGenerator;
import com.bastiaanjansen.otp.TOTPGenerator;
import org.apache.commons.codec.binary.Base32;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

public class OtpJavaTotpOperations implements AweTotpOperations {

  private static final int PASSWORD_LENGTH = 6;
  private static final int VERIFICATION_WINDOW = 1;
  private static final Duration PERIOD = Duration.ofSeconds(30);
  private static final String OTP_AUTH_SCHEME = "otpauth://totp/";
  private static final String CODE_PATTERN = "\\d{6}";
  private static final String INVALID_SECRET_MESSAGE = "Invalid Base32 TOTP secret";

  private final Clock clock;
  private final ZxingQrPngGenerator qrPngGenerator;

  public OtpJavaTotpOperations() {
    this(Clock.systemUTC(), new ZxingQrPngGenerator());
  }

  public OtpJavaTotpOperations(Clock clock, ZxingQrPngGenerator qrPngGenerator) {
    this.clock = clock;
    this.qrPngGenerator = qrPngGenerator;
  }

  @Override
  public String generateSecret() {
    return new String(SecretGenerator.generate(), StandardCharsets.US_ASCII);
  }

  @Override
  public boolean verifyCode(String secret, String code) {
    TOTPGenerator generator = buildGenerator(secret);
    String normalizedCode = normalizeCode(code);

    if (normalizedCode == null || !normalizedCode.matches(CODE_PATTERN)) {
      return false;
    }

    try {
      return generator.verify(normalizedCode, VERIFICATION_WINDOW);
    } catch (IllegalArgumentException | IllegalStateException exc) {
      return false;
    }
  }

  @Override
  public URI buildOtpAuthUri(String issuer, String accountName, String secret) {
    String normalizedIssuer = normalizeUriComponent(issuer, "issuer");
    String normalizedAccountName = normalizeUriComponent(accountName, "account name");
    String normalizedSecret = normalizeSecret(secret);

    if (normalizedSecret == null) {
      throw new AWERuntimeException(INVALID_SECRET_MESSAGE);
    }

    validateSecret(normalizedSecret);

    String rawUri = OTP_AUTH_SCHEME + encodeUriComponent(normalizedAccountName)
      + "?secret=" + encodeUriComponent(normalizedSecret)
      + "&issuer=" + encodeUriComponent(normalizedIssuer)
      + "&algorithm=SHA1&digits=" + PASSWORD_LENGTH
      + "&period=" + PERIOD.toSeconds();

    return URI.create(rawUri);
  }

  @Override
  public byte[] generateQrPng(String issuer, String accountName, String secret) {
    return qrPngGenerator.generate(buildOtpAuthUri(issuer, accountName, secret));
  }

  private TOTPGenerator buildGenerator(String secret) {
    String normalizedSecret = normalizeSecret(secret);

    if (normalizedSecret == null) {
      throw new AWERuntimeException(INVALID_SECRET_MESSAGE);
    }

    validateSecret(normalizedSecret);

    try {
      TOTPGenerator generator = new TOTPGenerator.Builder(normalizedSecret)
        .withClock(clock)
        .withHOTPGenerator(builder -> builder
          .withPasswordLength(PASSWORD_LENGTH)
          .withAlgorithm(HMACAlgorithm.SHA1))
        .withPeriod(PERIOD)
        .build();
      generator.now(clock);
      return generator;
    } catch (IllegalArgumentException | IllegalStateException exc) {
      throw new AWERuntimeException(INVALID_SECRET_MESSAGE, exc);
    }
  }

  private String normalizeSecret(String secret) {
    if (secret == null || secret.isBlank()) {
      return null;
    }
    return secret.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    return code.trim();
  }

  private String normalizeUriComponent(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new AWERuntimeException("Invalid OTPAuth " + label);
    }
    return value.trim();
  }

  private String encodeUriComponent(String value) {
    StringBuilder encoded = new StringBuilder();

    for (byte currentByte : value.getBytes(StandardCharsets.UTF_8)) {
      int current = currentByte & 0xFF;
      if (isUnreserved(current)) {
        encoded.append((char) current);
      } else {
        encoded.append('%');
        encoded.append(Character.toUpperCase(Character.forDigit((current >> 4) & 0xF, 16)));
        encoded.append(Character.toUpperCase(Character.forDigit(current & 0xF, 16)));
      }
    }

    return encoded.toString();
  }

  private boolean isUnreserved(int current) {
    return (current >= 'A' && current <= 'Z')
      || (current >= 'a' && current <= 'z')
      || (current >= '0' && current <= '9')
      || current == '-'
      || current == '.'
      || current == '_'
      || current == '~';
  }

  private void validateSecret(String secret) {
    Base32 base32 = new Base32();
    byte[] secretBytes = secret.getBytes(StandardCharsets.US_ASCII);

    if (!base32.isInAlphabet(secretBytes, true)) {
      throw new AWERuntimeException(INVALID_SECRET_MESSAGE);
    }

    byte[] decoded = base32.decode(secretBytes);
    if (decoded.length == 0) {
      throw new AWERuntimeException(INVALID_SECRET_MESSAGE);
    }
  }
}
