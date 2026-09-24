package com.almis.awe.scheduler.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ExecutionKey}
 */
class ExecutionKeyTest {

  @Test
  void mdcKeyRendersTaskIdSeparatorExecutionId() {
    ExecutionKey key = new ExecutionKey(12, 34);

    assertEquals("12-34", key.mdcKey());
  }

  @Test
  void mdcKeyRendersDifferentIdsCorrectly() {
    ExecutionKey key = new ExecutionKey(7, 210);

    assertEquals("7-210", key.mdcKey());
  }

  @Test
  void ofRoundTripsAValidMdcKey() {
    ExecutionKey key = new ExecutionKey(7, 21);

    assertEquals(key, ExecutionKey.of(key.mdcKey()));
  }

  @Test
  void ofReturnsNullForNullInput() {
    assertNull(ExecutionKey.of(null));
  }

  @Test
  void ofReturnsNullForEmptyInput() {
    assertNull(ExecutionKey.of(""));
  }

  @Test
  void ofReturnsNullWhenSeparatorIsMissing() {
    assertNull(ExecutionKey.of("12"));
  }

  @Test
  void ofReturnsNullForNonNumericParts() {
    assertNull(ExecutionKey.of("12-abc"));
  }

  @Test
  void ofReturnsNullForTooManyParts() {
    assertNull(ExecutionKey.of("1-2-3"));
  }

  @Test
  void constructorRejectsNullTaskId() {
    assertThrows(NullPointerException.class, () -> new ExecutionKey(null, 1));
  }

  @Test
  void constructorRejectsNullExecutionId() {
    assertThrows(NullPointerException.class, () -> new ExecutionKey(1, null));
  }
}
