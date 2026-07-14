package com.almis.awe.service.hotreload;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static com.almis.awe.service.hotreload.XmlReloadHandler.ReloadResult.SKIPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * XmlReloadHandler SPI tests
 */
class XmlReloadHandlerTest {

  /**
   * Test that the default order places an handler without an explicit order first among
   * handlers with a positive order, so a handler that does not override it participates in
   * the dispatch on equal footing with the lowest priority
   */
  @Test
  void defaultOrderIsZero() {
    XmlReloadHandler handler = new XmlReloadHandler() {
      @Override
      public boolean supports(Path moduleRoot, Path changedFile) {
        return false;
      }

      @Override
      public ReloadResult reload(Path moduleRoot, Path changedFile) {
        return SKIPPED;
      }
    };

    assertEquals(0, handler.order());
  }
}
