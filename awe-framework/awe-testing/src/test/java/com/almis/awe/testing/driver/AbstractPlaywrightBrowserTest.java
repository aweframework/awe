package com.almis.awe.testing.driver;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base of the tests of the Playwright adapter: a real headless browser with a fresh page per test. It is Chromium, which
 * is what CI installs; {@code -Dawe.test.playwright.engine=firefox} runs the same tests on Firefox, to check the adapter
 * there where Firefox is installed. Add {@code -Dawe.test.playwright.required=true} to fail instead of skipping when the
 * browser is not installed.
 */
@RequiresPlaywrightBrowser
abstract class AbstractPlaywrightBrowserTest {

  protected BrowserContext context;
  protected Page page;
  protected PlaywrightBrowserDriver browser;

  @BeforeEach
  void openPage() {
    context = PlaywrightTestBrowser.newContext(800, 600);
    page = PlaywrightTestBrowser.newPage(context);
    browser = new PlaywrightBrowserDriver(page);
  }

  @AfterEach
  void closePage() {
    context.close();
  }

  /**
   * Show an in-memory page
   *
   * @param body Content of the body
   */
  protected void show(String body) {
    page.setContent("<!DOCTYPE html><html><body style='margin:0'>" + body + "</body></html>");
  }

  /**
   * Run a script in the page and read what it returns
   */
  protected Object eval(String expression) {
    return page.evaluate(expression);
  }
}
