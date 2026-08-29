package com.almis.awe.scheduler.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link ExecutionLogOrigin}
 */
class ExecutionLogOriginTest {

  @Test
  void schedulerOriginCarriesTheSCode() {
    assertEquals("S", ExecutionLogOrigin.SCHEDULER.code());
  }

  @Test
  void applicationOriginCarriesTheACode() {
    assertEquals("A", ExecutionLogOrigin.APPLICATION.code());
  }

  @Test
  void fromCodeResolvesTheSchedulerCode() {
    assertSame(ExecutionLogOrigin.SCHEDULER, ExecutionLogOrigin.fromCode("S"));
  }

  @Test
  void fromCodeResolvesTheApplicationCode() {
    assertSame(ExecutionLogOrigin.APPLICATION, ExecutionLogOrigin.fromCode("A"));
  }

  @Test
  void fromCodeDefaultsToSchedulerWhenNull() {
    assertSame(ExecutionLogOrigin.SCHEDULER, ExecutionLogOrigin.fromCode(null));
  }

  @Test
  void fromCodeDefaultsToSchedulerWhenUnknown() {
    assertSame(ExecutionLogOrigin.SCHEDULER, ExecutionLogOrigin.fromCode("x"));
  }
}
