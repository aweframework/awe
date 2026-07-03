package com.almis.awe.scheduler.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link SshHostKeyPolicy}
 */
class SshHostKeyPolicyTest {

  /**
   * The enum must define exactly the three policies the SSH executor
   * config maps to a MINA ServerKeyVerifier: accept-on-first-use (default),
   * strict (known_hosts only) and accept-all (insecure/dev only).
   */
  @Test
  void definesExpectedPolicies() {
    SshHostKeyPolicy[] values = SshHostKeyPolicy.values();

    assertEquals(3, values.length);
    assertNotNull(SshHostKeyPolicy.valueOf("ACCEPT_ON_FIRST_USE"));
    assertNotNull(SshHostKeyPolicy.valueOf("STRICT"));
    assertNotNull(SshHostKeyPolicy.valueOf("ACCEPT_ALL"));
  }
}
