package com.almis.awe.scheduler.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Verifies the signature of a Standard Webhooks message: HMAC-SHA256, base64 encoded, of
 * {@code {webhook-id}.{webhook-timestamp}.{raw body}}, with a secret of the form {@code whsec_<base64 key>}.
 */
public class WebhookSignatureVerifier {

  private static final String SECRET_PREFIX = "whsec_";
  private static final String VERSION_PREFIX = "v1,";
  private static final String ALGORITHM = "HmacSHA256";

  private final Clock clock;
  private final Duration tolerance;

  /**
   * Constructor
   *
   * @param clock     Clock that tells the current time
   * @param tolerance Maximum difference between the message timestamp and the current time
   */
  public WebhookSignatureVerifier(Clock clock, Duration tolerance) {
    this.clock = clock;
    this.tolerance = tolerance;
  }

  /**
   * Check a message
   *
   * @param secrets         Secrets separated by spaces or commas; a match with any of them is valid (rotation)
   * @param id              Value of the webhook-id header
   * @param timestamp       Value of the webhook-timestamp header (epoch seconds)
   * @param signatureHeader Value of the webhook-signature header: space separated {@code v1,<signature>} entries
   * @param body            Raw body, as received
   * @return True when the timestamp is within the tolerance and a signature matches
   */
  public boolean verify(String secrets, String id, String timestamp, String signatureHeader, byte[] body) {
    if (isBlank(secrets) || isBlank(id) || isBlank(timestamp) || isBlank(signatureHeader) || body == null
      || !isTimestampValid(timestamp)) {
      return false;
    }

    List<byte[]> received = receivedSignatures(signatureHeader);
    byte[] prefix = (id + "." + timestamp + ".").getBytes(StandardCharsets.UTF_8);
    byte[] signed = new byte[prefix.length + body.length];
    System.arraycopy(prefix, 0, signed, 0, prefix.length);
    System.arraycopy(body, 0, signed, prefix.length, body.length);

    boolean valid = false;
    for (String secret : secrets.trim().split("[\s,]+")) {
      byte[] expected = sign(secret, signed);
      if (expected != null) {
        // Every candidate is compared, so the time does not tell which one matched
        for (byte[] signature : received) {
          valid |= MessageDigest.isEqual(expected, signature);
        }
      }
    }
    return valid;
  }

  private boolean isTimestampValid(String timestamp) {
    try {
      long difference = Math.abs(clock.instant().getEpochSecond() - Long.parseLong(timestamp.trim()));
      return difference <= tolerance.toSeconds();
    } catch (NumberFormatException exc) {
      return false;
    }
  }

  private List<byte[]> receivedSignatures(String header) {
    List<byte[]> signatures = new ArrayList<>();
    for (String entry : header.trim().split("\s+")) {
      if (entry.startsWith(VERSION_PREFIX)) {
        try {
          signatures.add(Base64.getDecoder().decode(entry.substring(VERSION_PREFIX.length())));
        } catch (IllegalArgumentException exc) {
          // Not base64: it cannot match
        }
      }
    }
    return signatures;
  }

  private byte[] sign(String secret, byte[] signed) {
    try {
      String encoded = secret.startsWith(SECRET_PREFIX) ? secret.substring(SECRET_PREFIX.length()) : secret;
      byte[] key = Base64.getDecoder().decode(encoded);
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key, ALGORITHM));
      return mac.doFinal(signed);
    } catch (IllegalArgumentException | GeneralSecurityException exc) {
      // A secret that is not valid base64, or an empty key, never validates anything
      return null;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
