package com.almis.awe.service.totp;

import com.almis.awe.exception.AWERuntimeException;
import com.bastiaanjansen.otp.HMACAlgorithm;
import com.bastiaanjansen.otp.TOTPGenerator;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpJavaTotpOperationsTest {

  private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

  @ParameterizedTest
  @MethodSource("rfc6238Vectors")
  void generateCodeUsesConfiguredTotpSemantics(long epochSecond, String expectedCode) {
    AweTotpOperations operations = operationsAt(epochSecond);

    assertThat(operations.verifyCode(RFC_SECRET, expectedCode)).isTrue();
  }

  @Test
  void verifyCodeAcceptsAdjacentWindowsOnly() {
    ZxingQrPngGenerator qrGenerator = new ZxingQrPngGenerator();
    Instant currentInstant = Instant.ofEpochSecond(1_111_111_111L);
    AweTotpOperations verifier = new OtpJavaTotpOperations(Clock.fixed(currentInstant, ZoneOffset.UTC), qrGenerator);

    String previousCode = generateCodeAt(currentInstant.minusSeconds(30).getEpochSecond(), RFC_SECRET);
    String currentCode = generateCodeAt(currentInstant.getEpochSecond(), RFC_SECRET);
    String nextCode = generateCodeAt(currentInstant.plusSeconds(30).getEpochSecond(), RFC_SECRET);
    String outOfWindowCode = generateCodeAt(currentInstant.plusSeconds(60).getEpochSecond(), RFC_SECRET);

    assertThat(verifier.verifyCode(RFC_SECRET, previousCode)).isTrue();
    assertThat(verifier.verifyCode(RFC_SECRET, currentCode)).isTrue();
    assertThat(verifier.verifyCode(RFC_SECRET, nextCode)).isTrue();
    assertThat(verifier.verifyCode(RFC_SECRET, outOfWindowCode)).isFalse();
  }

  @Test
  void generateSecretReturnsBase32EncodedValue() {
    AweTotpOperations operations = operationsAt(59L);

    String secret = operations.generateSecret();

    assertThat(secret).matches("[A-Z2-7]+=*");
    assertThat(new Base32().decode(secret)).isNotEmpty();
  }

  @Test
  void buildOtpAuthUriPreservesCompatibilitySafeLabelAndIssuerEncoding() {
    AweTotpOperations operations = operationsAt(59L);

    URI uri = operations.buildOtpAuthUri("Almis Web Engine", "john.doe+qa@example.com", RFC_SECRET);

    assertThat(uri.getScheme()).isEqualTo("otpauth");
    assertThat(uri.getHost()).isEqualTo("totp");
    assertThat(uri.getRawPath()).isEqualTo("/john.doe%2Bqa%40example.com");
    assertThat(uri.getRawPath()).doesNotContain("Almis");
    assertThat(uri.getRawQuery())
      .contains("issuer=Almis%20Web%20Engine")
      .contains("secret=" + RFC_SECRET)
      .contains("algorithm=SHA1")
      .contains("digits=6")
      .contains("period=30");
  }

  @Test
  void invalidSecretsFailFastInVerification() {
    AweTotpOperations operations = operationsAt(59L);

    assertThatThrownBy(() -> operations.verifyCode("not-base32", "123456"))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("Base32");
  }

  @Test
  void invalidSecretsFailFastInProvisioningPath() {
    // Provisioning (buildOtpAuthUri) must also fail fast on an invalid secret,
    // not silently emit a broken QR payload.
    AweTotpOperations operations = operationsAt(59L);

    assertThatThrownBy(() -> operations.buildOtpAuthUri("Issuer", "user@example.com", "not-base32!"))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("Base32");
  }

  @Test
  void nullOrBlankSecretFailsFastInProvisioningPath() {
    AweTotpOperations operations = operationsAt(59L);

    assertThatThrownBy(() -> operations.buildOtpAuthUri("Issuer", "user@example.com", null))
      .isInstanceOf(AWERuntimeException.class);

    assertThatThrownBy(() -> operations.buildOtpAuthUri("Issuer", "user@example.com", "   "))
      .isInstanceOf(AWERuntimeException.class);
  }

  @Test
  void invalidSecretsFailFastInQrGeneration() {
    AweTotpOperations operations = operationsAt(59L);

    assertThatThrownBy(() -> operations.generateQrPng("Issuer", "user@example.com", "not-base32!"))
      .isInstanceOf(AWERuntimeException.class)
      .hasMessageContaining("Base32");
  }

  @Test
  void storedSecretsAreNormalizedBeforeVerification() {
    String currentCode = generateCodeAt(1_111_111_111L, RFC_SECRET);

    AweTotpOperations operations = operationsAt(1_111_111_111L);

    assertThat(operations.verifyCode("  gezdgnbvgy3tqojqgezdgnbvgy3tqojq  ", currentCode)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "12345", "1234567", "12ab56", "12 456"})
  void malformedCodesFailClosed(String malformedCode) {
    AweTotpOperations operations = operationsAt(1_111_111_111L);

    assertThat(operations.verifyCode(RFC_SECRET, malformedCode)).isFalse();
  }

  private static AweTotpOperations operationsAt(long epochSecond) {
    return new OtpJavaTotpOperations(Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC), new ZxingQrPngGenerator());
  }

  private static String generateCodeAt(long epochSecond, String secret) {
    TOTPGenerator generator = new TOTPGenerator.Builder(secret)
      .withClock(Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC))
      .withHOTPGenerator(builder -> builder
        .withPasswordLength(6)
        .withAlgorithm(HMACAlgorithm.SHA1))
      .withPeriod(Duration.ofSeconds(30))
      .build();
    return generator.now(Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC));
  }

  private static Stream<Arguments> rfc6238Vectors() {
    return Stream.of(
      Arguments.of(59L, "287082"),
      Arguments.of(1_111_111_109L, "081804"),
      Arguments.of(1_234_567_890L, "005924")
    );
  }
}
