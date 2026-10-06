package com.almis.awe.testing.driver;

import com.almis.awe.testing.model.types.BrowserTool;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserDriverFactoryTest {

  @Test
  void theSeleniumToolIsRunByTheSeleniumFactory() {
    assertThat(BrowserDriverFactory.forTool(BrowserTool.SELENIUM)).isInstanceOf(SeleniumBrowserDriverFactory.class);
  }

  @Test
  void everyToolHasAFactory() {
    for (BrowserTool tool : BrowserTool.values()) {
      assertThat(BrowserDriverFactory.forTool(tool)).as("Factory of %s", tool).isNotNull();
    }
  }
}
