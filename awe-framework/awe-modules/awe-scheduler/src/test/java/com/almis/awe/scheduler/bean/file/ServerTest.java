package com.almis.awe.scheduler.bean.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies SSH credential fields on the {@link Server} bean never leak through
 * {@code toString()}/{@code equals()}/{@code hashCode()} (secret masking guard).
 */
class ServerTest {

  private static final String SECRET_USER = "root";
  private static final String SECRET_PASSWORD = "s3cr3t-P@ss";
  private static final String SECRET_KEY = "-----BEGIN PRIVATE KEY-----super-secret-key-material-----END PRIVATE KEY-----";
  private static final String SECRET_KEY_PASS = "k3y-p@ssphrase";

  private Server buildServerWithCredentials() {
    return new Server()
        .setServerId(1)
        .setName("ssh-server")
        .setTypeOfConnection("ssh")
        .setPort(22)
        .setUser(SECRET_USER)
        .setPassword(SECRET_PASSWORD)
        .setKey(SECRET_KEY)
        .setKeyPassphrase(SECRET_KEY_PASS);
  }

  @Test
  void toStringDoesNotExposeCredentials() {
    String rendered = buildServerWithCredentials().toString();

    assertFalse(rendered.contains(SECRET_USER), "toString() must not contain the SSH user");
    assertFalse(rendered.contains(SECRET_PASSWORD), "toString() must not contain the SSH password");
    assertFalse(rendered.contains(SECRET_KEY), "toString() must not contain the SSH key");
    assertFalse(rendered.contains(SECRET_KEY_PASS), "toString() must not contain the SSH key passphrase");
  }

  @Test
  void equalsIgnoresCredentialFields() {
    Server withCredentials = buildServerWithCredentials();
    Server withoutCredentials = new Server()
        .setServerId(1)
        .setName("ssh-server")
        .setTypeOfConnection("ssh")
        .setPort(22);

    assertEquals(withoutCredentials, withCredentials,
        "equals() must ignore user/password/key so credential-only differences do not affect equality");
  }

  @Test
  void hashCodeIgnoresCredentialFields() {
    Server withCredentials = buildServerWithCredentials();
    Server withoutCredentials = new Server()
        .setServerId(1)
        .setName("ssh-server")
        .setTypeOfConnection("ssh")
        .setPort(22);

    assertEquals(withoutCredentials.hashCode(), withCredentials.hashCode(),
        "hashCode() must ignore user/password/key");
  }

  @Test
  void equalsDetectsDifferenceInNonCredentialFields() {
    Server serverA = new Server().setServerId(1).setName("ssh-server").setTypeOfConnection("ssh").setPort(22);
    Server serverB = new Server().setServerId(2).setName("ssh-server").setTypeOfConnection("ssh").setPort(22);

    assertTrue(!serverA.equals(serverB), "equals() must still compare non-credential fields");
  }
}
