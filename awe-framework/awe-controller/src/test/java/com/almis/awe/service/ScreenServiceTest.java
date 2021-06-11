package com.almis.awe.service;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class used for testing ScreenService class
 */
@Log4j2
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
