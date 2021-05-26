package com.almis.awe.model.util.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Time Util tests
 *
 * @author pgarcia
 */
class TimeUtilTest {

  /**
   * Test hours
   */
  @Test
  void testExactHoursWithMs() {
    // Test and assert
    assertEquals("10h", TimeUtil.formatTime(36000000));
  }

  /**
   * Test hours
   */
  @Test
  void testExactHoursWithoutMs() {
    // Test and assert
    assertEquals("6h 40m", TimeUtil.formatTime(24000000, false));
  }

  /**
   * Test hours
   */
  @Test
  void testNotExactHoursWithMs() {
    // Test and assert
    assertEquals("3h 7m 11s 231ms", TimeUtil.formatTime(11231231));
  }

  /**
   * Test hours
   */
  @Test
  void testNotExactHoursWithoutMs() {
    // Test and assert
    assertEquals("6h 30m 23s", TimeUtil.formatTime(23423423, false));
    assertEquals("1h 5s", TimeUtil.formatTime(3605000, false));
  }

  /**
   * Test exact minutes with ms
   */
  @Test
  void testExactMinutesWithMs() {
    // Test and assert
    assertEquals("6m", TimeUtil.formatTime(360000));
  }

  /**
   * Test minutes
   */
  @Test
  void testExactMinutesWithoutMs() {
    // Test and assert
    assertEquals("4m", TimeUtil.formatTime(240000, false));
  }

  /**
   * Test minutes
   */
  @Test
  void testMinutesWithMs() {
    // Test and assert
    assertEquals("6m 1s 231ms", TimeUtil.formatTime(361231, true));
  }

  /**
   * Test minutes
   */
  @Test
  void testMinutesWithoutMs() {
    // Test and assert
    assertEquals("7m 32s", TimeUtil.formatTime(452324, false));
  }
}