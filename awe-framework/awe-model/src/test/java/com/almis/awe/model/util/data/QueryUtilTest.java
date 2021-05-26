package com.almis.awe.model.util.data;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Query Util class
 *
 * @author pgarcia
 */
class QueryUtilTest {

  @InjectMocks
  private QueryUtil queryUtil;

  /**
   * Test null get parameters
   */
  @Test
  void testNullGetParameters() {
    assertThrows(NullPointerException.class, () -> queryUtil.getParameters(null, null, null, null));
  }

  /**
   * Test null get parameters
   */
  @Test
  void testNullVariableIsList() {
    assertThrows(NullPointerException.class, () -> queryUtil.variableIsList(null, null));
  }
}