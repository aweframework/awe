package com.almis.awe.scheduler.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Standard Webhooks verification, with the test vector of the integration spec (3.4)
 */
class WebhookSignatureVerifierTest {

  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
  private static final String OTHER_SECRET = "whsec_Zm9vYmFyZm9vYmFyZm9vYmFyZm9vYmFyZm9vYmFyMTI=";
  private static final String ID = "0193f2a4-6c1e-7b3a-9d2e-5f8a1c4e7b20";
  private static final String TIMESTAMP = "1763029530";
  private static final String SIGNATURE = "v1,A4hcS6QMtMJGUshESWRCI0SK2l7hfSW/LXYbcYD8Ef0=";
  private static final byte[] BODY = ("{\"type\":\"job.finished\",\"timestamp\":\"2025-11-13T10:25:30Z\",\"data\":"
    + "{\"jobId\":\"0193f2a0-1111-7aaa-8bbb-000000000001\",\"status\":\"COMPLETED\"}}")
    .getBytes(StandardCharsets.UTF_8);

  private final WebhookSignatureVerifier verifier = verifierAt(Instant.ofEpochSecond(1763029530L));

  private static WebhookSignatureVerifier verifierAt(Instant now) {
    return new WebhookSignatureVerifier(Clock.fixed(now, ZoneOffset.UTC), Duration.ofMinutes(5));
  }

  @Test
  void theSpecVectorIsValid() {
    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, SIGNATURE, BODY)).isTrue();
  }

  @Test
  void aTamperedBodyIsRejected() {
    byte[] tampered = new String(BODY, StandardCharsets.UTF_8).replace("COMPLETED", "FAILED")
      .getBytes(StandardCharsets.UTF_8);

    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, SIGNATURE, tampered)).isFalse();
  }

  @Test
  void aTamperedSignatureIsRejected() {
    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, "v1,B4hcS6QMtMJGUshESWRCI0SK2l7hfSW/LXYbcYD8Ef0=", BODY))
      .isFalse();
  }

  @Test
  void aTamperedIdOrTimestampIsRejected() {
    assertThat(verifier.verify(SECRET, ID + "x", TIMESTAMP, SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify(SECRET, ID, "1763029531", SIGNATURE, BODY)).isFalse();
  }

  @Test
  void aTimestampOutOfToleranceIsRejectedInBothDirections() {
    assertThat(verifierAt(Instant.ofEpochSecond(1763029530L + 240)).verify(SECRET, ID, TIMESTAMP, SIGNATURE, BODY))
      .isTrue();
    assertThat(verifierAt(Instant.ofEpochSecond(1763029530L + 600)).verify(SECRET, ID, TIMESTAMP, SIGNATURE, BODY))
      .isFalse();
    assertThat(verifierAt(Instant.ofEpochSecond(1763029530L - 600)).verify(SECRET, ID, TIMESTAMP, SIGNATURE, BODY))
      .isFalse();
  }

  @Test
  void anyConfiguredSecretValidatesDuringARotation() {
    assertThat(verifier.verify(OTHER_SECRET + " " + SECRET, ID, TIMESTAMP, SIGNATURE, BODY)).isTrue();
    assertThat(verifier.verify(SECRET + "," + OTHER_SECRET, ID, TIMESTAMP, SIGNATURE, BODY)).isTrue();
    assertThat(verifier.verify(OTHER_SECRET, ID, TIMESTAMP, SIGNATURE, BODY)).isFalse();
  }

  @Test
  void theHeaderMayHoldSeveralSignaturesAndOnlyV1Counts() {
    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, "v1,AAAA v2,zzzz " + SIGNATURE, BODY)).isTrue();
    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, SIGNATURE.replace("v1,", "v2,"), BODY)).isFalse();
  }

  @Test
  void missingOrMalformedInputIsRejectedWithoutThrowing() {
    assertThat(verifier.verify(null, ID, TIMESTAMP, SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify(" ", ID, TIMESTAMP, SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify("whsec_%%%", ID, TIMESTAMP, SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify(SECRET, null, TIMESTAMP, SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify(SECRET, ID, "abc", SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify(SECRET, ID, null, SIGNATURE, BODY)).isFalse();
    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, null, BODY)).isFalse();
    assertThat(verifier.verify(SECRET, ID, TIMESTAMP, "v1,%%%", BODY)).isFalse();
  }
}
