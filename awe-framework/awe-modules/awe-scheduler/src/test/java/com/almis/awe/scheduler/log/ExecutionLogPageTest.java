package com.almis.awe.scheduler.log;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ExecutionLogPage}
 */
class ExecutionLogPageTest {

  @Test
  void emptyReturnsNoReplaceNoLinesZeroCountsAndNoVersion() {
    ExecutionLogPage page = ExecutionLogPage.empty();

    assertFalse(page.replace());
    assertEquals(0L, page.totalLines());
    assertEquals(0L, page.omittedLines());
    assertTrue(page.lines().isEmpty());
    assertNull(page.version());
  }

  @Test
  void fourArgConstructorDefaultsVersionToNull() {
    ExecutionLogPage page = new ExecutionLogPage(List.of("a", "b"), true, 10L, 3L);

    assertEquals(List.of("a", "b"), page.lines());
    assertTrue(page.replace());
    assertEquals(10L, page.totalLines());
    assertEquals(3L, page.omittedLines());
    assertNull(page.version());
  }

  @Test
  void recordExposesTheSuppliedVersion() {
    ExecutionLogPage page = new ExecutionLogPage(List.of("a", "b"), true, 10L, 3L, "v1");

    assertEquals("v1", page.version());
  }
}
