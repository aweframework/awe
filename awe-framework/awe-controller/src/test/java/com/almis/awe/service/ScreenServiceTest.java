package com.almis.awe.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class used for testing ScreenService class
 */
@Slf4j
class ScreenServiceTest {

  @InjectMocks
  private ScreenService screenService;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  public void initBeans() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Check null screen
   */
  @Test
  void getScreenElementListWithNull() {
    assertThrows(NullPointerException.class, () -> screenService.getScreenElementList(null, null));
  }
}
