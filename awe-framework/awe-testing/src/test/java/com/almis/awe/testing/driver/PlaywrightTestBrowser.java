package com.almis.awe.testing.driver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One headless Chromium for all the Playwright adapter tests: launching a browser per test would dominate their time.
 * Each test gets its own context (cookies, pages and listeners are isolated) and works on in-memory pages: no network
 * server is started and no port is opened.
 *
 * <p>The tests never download a browser (see {@link #driverEnvironment()}): the browser is the one that is installed in
 * {@code PLAYWRIGHT_BROWSERS_PATH} (by default {@code ~/.cache/ms-playwright}). When the browser
 * cannot be launched {@link #unavailableReason()} says why and {@link RequiresPlaywrightBrowser} skips the tests, or fails
 * them when the run requires the browser ({@link #isRequired()}, as CI does).</p>
 */
final class PlaywrightTestBrowser {

  private static final String REQUIRED_PROPERTY = "awe.test.playwright.required";
  private static final String ENGINE_PROPERTY = "awe.test.playwright.engine";
  private static final String NOT_INSTALLED = "Executable doesn't exist";
  private static Playwright playwright;
  private static Browser browser;
  private static String unavailableReason;
  private static boolean launched;

  private PlaywrightTestBrowser() {
  }

  /**
   * Get the engine that the tests run on
   *
   * @return {@code firefox} when {@code -Dawe.test.playwright.engine=firefox} was given, {@code chromium} otherwise
   */
  static String engine() {
    return "firefox".equals(System.getProperty(ENGINE_PROPERTY)) ? "firefox" : "chromium";
  }

  /**
   * Get the name of the engine that the tests run on, for the messages
   *
   * @return Chromium or Firefox
   */
  static String engineName() {
    return "firefox".equals(engine()) ? "Firefox" : "Chromium";
  }

  /**
   * Tell a browser that is not installed from one that is but fails to launch (missing system libraries, a crash...)
   *
   * @param exception Failure of the launch
   * @return true if Playwright says that the executable of the browser is not there
   */
  static boolean isNotInstalled(PlaywrightException exception) {
    return exception.getMessage() != null && exception.getMessage().contains(NOT_INSTALLED);
  }

  /**
   * Check whether the run needs the browser, so that its absence fails the tests instead of skipping them (CI)
   *
   * @return true if {@code -Dawe.test.playwright.required=true} was given
   */
  static boolean isRequired() {
    return Boolean.getBoolean(REQUIRED_PROPERTY);
  }

  /**
   * Get the environment of the Playwright driver: the one of this process, which carries {@code PLAYWRIGHT_BROWSERS_PATH}
   * and the proxies, and the order not to download browsers. Playwright installs the browsers it lacks (Chromium, Firefox
   * and WebKit, about 1 GB) the first time it starts: the tests run with the browsers that are installed, or are skipped
   *
   * @return Environment variables
   */
  static Map<String, String> driverEnvironment() {
    Map<String, String> environment = new HashMap<>(System.getenv());
    environment.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
    return environment;
  }

  /**
   * Start Playwright without letting it download a browser
   *
   * @return Playwright
   */
  static Playwright createPlaywright() {
    return Playwright.create(new Playwright.CreateOptions().setEnv(driverEnvironment()));
  }

  /**
   * Get why the browser cannot be used
   *
   * @return Reason, or null if the browser is available
   */
  static synchronized String unavailableReason() {
    if (!launched) {
      launched = true;
      try {
        playwright = createPlaywright();
        browser = "firefox".equals(engine())
          ? playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true))
          : playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true)
            .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
        Runtime.getRuntime().addShutdownHook(new Thread(PlaywrightTestBrowser::shutdown));
      } catch (Exception exc) {
        unavailableReason = "No Playwright " + engineName() + " is available (install it with `mvn exec:java -D exec.mainClass="
          + "com.microsoft.playwright.CLI -D exec.args=\"install " + engine() + "\"`): " + exc.getMessage();
        shutdown();
      }
    }
    return unavailableReason;
  }

  /**
   * Open a new isolated context with a page of the given size
   *
   * @param width  Viewport width
   * @param height Viewport height
   * @return Context, whose first page is {@link BrowserContext#pages()}
   */
  static BrowserContext newContext(int width, int height) {
    return browser.newContext(new Browser.NewContextOptions().setViewportSize(width, height));
  }

  /**
   * Open a page in a new context
   *
   * @param context Context
   * @return Page
   */
  static Page newPage(BrowserContext context) {
    return context.newPage();
  }

  private static synchronized void shutdown() {
    try {
      if (browser != null) {
        browser.close();
      }
      if (playwright != null) {
        playwright.close();
      }
    } catch (Exception exc) {
      // The JVM is going away
    } finally {
      browser = null;
      playwright = null;
    }
  }
}
