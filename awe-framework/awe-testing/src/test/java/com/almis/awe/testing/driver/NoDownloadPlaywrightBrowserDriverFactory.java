package com.almis.awe.testing.driver;

import com.microsoft.playwright.Playwright;

/**
 * The factory of the Playwright tool for the tests: it uses the browsers that are installed and never downloads one
 */
class NoDownloadPlaywrightBrowserDriverFactory extends PlaywrightBrowserDriverFactory {

  @Override
  protected Playwright createPlaywright() {
    return PlaywrightTestBrowser.createPlaywright();
  }
}
