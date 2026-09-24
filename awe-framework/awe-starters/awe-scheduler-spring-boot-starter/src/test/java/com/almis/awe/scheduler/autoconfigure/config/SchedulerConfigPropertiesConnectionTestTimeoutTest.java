package com.almis.awe.scheduler.autoconfigure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the connection-test timeout field of {@link SchedulerConfigProperties}
 */
class SchedulerConfigPropertiesConnectionTestTimeoutTest {

  /**
   * The interactive connection test must not wait the batch-oriented SSH
   * connect timeout (30s): its own default is 10 seconds, expressed as a
   * Duration bound with @DurationUnit(SECONDS) so externalized bare values
   * (e.g. "10") are not silently interpreted as milliseconds.
   */
  @Test
  void connectionTestTimeoutDefaultsTo10Seconds() {
    SchedulerConfigProperties properties = new SchedulerConfigProperties();

    assertEquals(Duration.ofSeconds(10), properties.getConnectionTestTimeout());
  }

  /**
   * The field must be mutable (Lombok @Data) so Spring can bind the
   * externalized awe.scheduler.connection-test-timeout property onto it.
   */
  @Test
  void connectionTestTimeoutIsSettable() {
    SchedulerConfigProperties properties = new SchedulerConfigProperties();

    properties.setConnectionTestTimeout(Duration.ofSeconds(5));

    assertEquals(Duration.ofSeconds(5), properties.getConnectionTestTimeout());
  }
}
