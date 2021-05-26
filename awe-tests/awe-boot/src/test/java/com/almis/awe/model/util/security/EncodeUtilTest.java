package com.almis.awe.model.util.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Encode util test class
 *
 * @author pgarcia
 */
@ExtendWith(MockitoExtension.class)
class EncodeUtilTest {

  @BeforeEach
  public void setUp() {
    EncodeUtil.init(null);
  }

  /**
   * Test of hash
   *
   * @throws Exception Test error
   */
  @Test
  void testHash() throws Exception {
    // Prepare
    assertEquals("655e786674d9d3e77bc05ed1de37b4b6bc89f788829f9f3c679e7687b410c89b", EncodeUtil.hash(EncodeUtil.HashingAlgorithms.SHA_256, "prueba"));
  }
}