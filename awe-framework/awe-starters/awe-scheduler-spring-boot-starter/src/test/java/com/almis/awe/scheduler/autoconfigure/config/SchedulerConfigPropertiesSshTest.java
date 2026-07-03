package com.almis.awe.scheduler.autoconfigure.config;

import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the SSH-related fields added to {@link SchedulerConfigProperties}
 */
class SchedulerConfigPropertiesSshTest {

  /**
   * Default host-key policy must be accept-on-first-use (TOFU), the
   * convenient default for controlled/internal environments.
   */
  @Test
  void sshHostKeyPolicyDefaultsToAcceptOnFirstUse() {
    SchedulerConfigProperties properties = new SchedulerConfigProperties();

    assertEquals(SshHostKeyPolicy.ACCEPT_ON_FIRST_USE, properties.getSshHostKeyPolicy());
  }

  /**
   * Default known_hosts path must resolve under the current user's home
   * directory so accept-on-first-use / strict policies have somewhere to
   * persist/read trusted host keys without extra configuration.
   */
  @Test
  void sshKnownHostsPathDefaultsUnderUserHome() {
    SchedulerConfigProperties properties = new SchedulerConfigProperties();

    String expected = Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts").toString();
    assertEquals(expected, properties.getSshKnownHostsPath());
  }

  /**
   * Default SSH connect/auth timeout must be 30 seconds, expressed as a
   * Duration bound with @DurationUnit(SECONDS) so externalized bare values
   * (e.g. "30") are not silently interpreted as milliseconds.
   */
  @Test
  void sshConnectTimeoutDefaultsTo30Seconds() {
    SchedulerConfigProperties properties = new SchedulerConfigProperties();

    assertEquals(Duration.ofSeconds(30), properties.getSshConnectTimeout());
  }

  /**
   * Fields must be mutable (Lombok @Data) so Spring can bind externalized
   * awe.scheduler.ssh-* properties onto them.
   */
  @Test
  void sshFieldsAreSettable() {
    SchedulerConfigProperties properties = new SchedulerConfigProperties();

    properties.setSshHostKeyPolicy(SshHostKeyPolicy.STRICT);
    properties.setSshKnownHostsPath("/etc/awe/known_hosts");
    properties.setSshConnectTimeout(Duration.ofSeconds(45));

    assertEquals(SshHostKeyPolicy.STRICT, properties.getSshHostKeyPolicy());
    assertEquals("/etc/awe/known_hosts", properties.getSshKnownHostsPath());
    assertEquals(Duration.ofSeconds(45), properties.getSshConnectTimeout());
  }
}
