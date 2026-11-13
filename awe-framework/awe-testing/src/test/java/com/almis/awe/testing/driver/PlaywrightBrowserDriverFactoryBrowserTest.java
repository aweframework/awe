package com.almis.awe.testing.driver;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.BrowserTool;
import com.almis.awe.testing.model.types.BrowserType;
import com.microsoft.playwright.PlaywrightException;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The browser that the Playwright factory opens, on a real headless Chromium
 */
@RequiresPlaywrightBrowser
class PlaywrightBrowserDriverFactoryBrowserTest {

  @Test
  void theFactoryOpensAChromiumOfTheConfiguredSizeAndPublishesItInTheModel() throws IOException {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setTool(BrowserTool.PLAYWRIGHT);
    properties.setBrowser(BrowserType.HEADLESS_CHROME);
    properties.setBrowserWidth(640);
    properties.setBrowserHeight(480);
    SeleniumModel model = new SeleniumModel().setProperties(properties);

    BrowserSession session = new NoDownloadPlaywrightBrowserDriverFactory().create(model, "test");
    try {
      assertThat(session.browser()).isInstanceOf(PlaywrightBrowserDriver.class).isSameAs(model.getBrowser());
      assertThat(session.browser()).isSameAs(session.browser());
      session.browser().open("data:text/html,<p>size</p>");
      assertThat(session.browser().executeScript("return window.innerWidth + 'x' + window.innerHeight")).isEqualTo("640x480");
      assertThat(session.browser().executeScript("return navigator.userAgent")).asString().contains("Chrome");
      assertThatThrownBy(model::getDriver).isInstanceOf(UnsupportedOperationException.class);
    } finally {
      session.close();
    }
  }

  @Test
  void theFactoryOpensAFirefoxToo() throws IOException {
    AweTestConfigProperties properties = new AweTestConfigProperties();
    properties.setBrowser(BrowserType.HEADLESS_FIREFOX);
    SeleniumModel model = new SeleniumModel().setProperties(properties);

    BrowserSession session;
    try {
      session = new NoDownloadPlaywrightBrowserDriverFactory().create(model, "test");
    } catch (PlaywrightException exc) {
      // Chromium is the browser that CI installs and requires. Firefox is checked where it is installed: a Firefox that is
      // not there skips the test, but one that is there and cannot be launched fails it, even when the run requires browsers
      if (!PlaywrightTestBrowser.isNotInstalled(exc)) {
        throw exc;
      }
      assumeTrue(false, "Playwright Firefox is not installed: " + exc.getMessage());
      return;
    }
    try {
      session.browser().open("data:text/html,<button id='go' onclick=\"document.title='clicked'\">Go</button>");
      session.browser().click(Locator.css("#go"));
      assertThat(session.browser().executeScript("return document.title")).isEqualTo("clicked");
      assertThat(session.browser().executeScript("return navigator.userAgent")).asString().contains("Firefox");
    } finally {
      session.close();
    }
  }
}
