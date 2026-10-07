package com.almis.awe.testing.driver;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.BrowserType;
import com.almis.awe.testing.model.types.EvidenceMode;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Factory of the Playwright tool (pilot): launches the browser that the {@code awe.test.browser} property asks for and
 * publishes the neutral browser in the {@link SeleniumModel}. The Selenium driver of the model stays empty.
 *
 * <p>{@code chrome} and {@code headless-chrome} run the Chromium that Playwright installs; {@code firefox} and
 * {@code headless-firefox} the Firefox it installs. The other browser types, and every remote or service browser, are
 * Selenium infrastructure (a grid or a Docker image) that Playwright does not use, so they fail at once with a message
 * instead of starting something else.</p>
 */
@Slf4j
public class PlaywrightBrowserDriverFactory implements BrowserDriverFactory {

  private static final String SUPPORTED = "chrome, headless-chrome, firefox, headless-firefox";

  /**
   * Browser engine of Playwright
   */
  enum Engine {
    /** Chromium */
    CHROMIUM,
    /** Firefox */
    FIREFOX
  }

  /**
   * Get the engine that runs a browser type
   *
   * @param type Browser type of the configuration
   * @return Engine
   * @throws UnsupportedOperationException If Playwright does not run that browser type
   */
  static Engine engineOf(BrowserType type) {
    return switch (type) {
      case CHROME, HEADLESS_CHROME -> Engine.CHROMIUM;
      case FIREFOX, HEADLESS_FIREFOX -> Engine.FIREFOX;
      default -> throw new UnsupportedOperationException(String.format("Browser type '%s' is not supported by the Playwright pilot "
        + "(awe.test.tool=playwright). Supported browser types: %s. Remote and service browsers are not supported yet: "
        + "use awe.test.tool=selenium for them", type.getName(), SUPPORTED));
    };
  }

  /**
   * Check whether a browser type runs without a window
   *
   * @param type Browser type of the configuration
   * @return true for the headless types
   */
  static boolean isHeadless(BrowserType type) {
    return type == BrowserType.HEADLESS_CHROME || type == BrowserType.HEADLESS_FIREFOX;
  }

  /**
   * Get the arguments of Chromium. The shared memory of a container is too small for it, so it never uses it (harmless
   * elsewhere). Its sandbox needs privileges that a container or the root user do not give, so the sandbox is removed
   * when it runs headless (the run of CI) or in such a session, unless the configuration says otherwise
   *
   * @param headless        Whether it runs without a window
   * @param noSandbox       The configured choice ({@code awe.test.playwright.no-sandbox}), or null for the automatic rule
   * @param rootOrContainer Whether the process is run by root or inside a container
   * @return Arguments
   */
  static List<String> chromiumArguments(boolean headless, Boolean noSandbox, boolean rootOrContainer) {
    boolean withoutSandbox = noSandbox != null ? noSandbox : headless || rootOrContainer;
    return withoutSandbox ? List.of("--disable-dev-shm-usage", "--no-sandbox") : List.of("--disable-dev-shm-usage");
  }

  /**
   * Check whether the process runs as root or inside a container: Docker and Podman leave a marker file in it and
   * Kubernetes sets the address of its API server
   *
   * @param user        User of the process
   * @param exists      Tells whether a file exists
   * @param environment Reads an environment variable
   * @return true for root or a container
   */
  static boolean isRootOrContainer(String user, Predicate<String> exists, UnaryOperator<String> environment) {
    String kubernetes = environment.apply("KUBERNETES_SERVICE_HOST");
    return "root".equals(user) || exists.test("/.dockerenv") || exists.test("/run/.containerenv")
      || kubernetes != null && !kubernetes.isBlank();
  }

  @Override
  public BrowserSession create(SeleniumModel model, String testName) {
    AweTestConfigProperties properties = model.getProperties();
    // Before anything is started
    Engine engine = engineOf(properties.getBrowser());
    log.info("Selected browser is {} (Playwright {}), window size: {}x{}", properties.getBrowser(), engine,
      properties.getBrowserWidth(), properties.getBrowserHeight());

    Playwright playwright = createPlaywright();
    Browser browser = null;
    BrowserContext context = null;
    Path videoDir = null;
    try {
      browser = launch(playwright, engine, isHeadless(properties.getBrowser()), properties.getPlaywright().getNoSandbox());
      Path evidenceDir = Path.of(properties.getScreenshotPath());
      videoDir = properties.getPlaywright().getVideo() == EvidenceMode.OFF ? null : createVideoDirectory(evidenceDir);
      Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
        .setViewportSize(properties.getBrowserWidth(), properties.getBrowserHeight())
        // As the Firefox profile of the Selenium tool does: the environments under test have self signed certificates
        .setIgnoreHTTPSErrors(true);
      if (videoDir != null) {
        // The context lasts the whole test class, so that is the video: it is kept (or not) when the context is closed. It
        // is recorded at half the window size: encoding the full size made the Chromium suites about 50% slower in CI
        contextOptions.setRecordVideoDir(videoDir)
          .setRecordVideoSize(properties.getBrowserWidth() / 2, properties.getBrowserHeight() / 2);
      }
      context = browser.newContext(contextOptions);
      context.setDefaultTimeout(properties.getTimeout().toMillis());
      context.setDefaultNavigationTimeout(properties.getTimeout().toMillis());
      // The video starts with the page
      long recordingStart = System.nanoTime();
      Page page = context.newPage();
      PlaywrightEvidence evidence = new PlaywrightEvidence(context, videoDir == null ? null : page.video(), videoDir,
        evidenceDir, properties.getPlaywright().getTrace(), properties.getPlaywright().isTraceSnapshots(), properties.getPlaywright().getVideo(), System::nanoTime, recordingStart);
      evidence.startTracing();
      PlaywrightBrowserDriver driver = new PlaywrightBrowserDriver(page);
      model.setBrowser(driver);
      return new PlaywrightBrowserSession(playwright, browser, context, driver, evidence);
    } catch (RuntimeException exc) {
      new PlaywrightBrowserSession(playwright, browser, context, null).closeQuietly();
      // Nothing was recorded in it yet: it must not stay among the evidence
      deleteQuietly(videoDir);
      throw exc;
    }
  }

  // A folder of its own inside the evidence one, which is on the same file system and is removed when the video is resolved
  private static void deleteQuietly(Path videoDir) {
    if (videoDir == null) {
      return;
    }
    try (java.util.stream.Stream<Path> files = Files.walk(videoDir)) {
      files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
    } catch (IOException | RuntimeException exc) {
      log.debug("The folder of the Playwright video {} could not be removed", videoDir, exc);
    }
  }

  private static Path createVideoDirectory(Path evidenceDir) {
    try {
      Files.createDirectories(evidenceDir);
      return Files.createTempDirectory(evidenceDir, "playwright-video-");
    } catch (IOException exc) {
      throw new UncheckedIOException("Could not create the folder of the Playwright video in " + evidenceDir, exc);
    }
  }

  /**
   * Start the Playwright driver. Playwright downloads the browsers that are not installed the first time it starts
   * (unless {@code PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD} is set): that is expected when a test run chooses this tool
   *
   * @return Playwright
   */
  protected Playwright createPlaywright() {
    return Playwright.create();
  }

  /**
   * Launch a browser
   *
   * @param playwright Playwright
   * @param engine     Engine
   * @param headless   Whether it runs without a window
   * @param noSandbox  Configured choice of running Chromium without its sandbox, or null for the automatic rule
   * @return Browser
   */
  protected Browser launch(Playwright playwright, Engine engine, boolean headless, Boolean noSandbox) {
    com.microsoft.playwright.BrowserType.LaunchOptions options = new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(headless);
    if (engine == Engine.CHROMIUM) {
      options.setArgs(chromiumArguments(headless, noSandbox,
        isRootOrContainer(System.getProperty("user.name"), path -> Files.exists(Path.of(path)), System::getenv)));
      return playwright.chromium().launch(options);
    }
    return playwright.firefox().launch(options);
  }

  /**
   * Playwright browser of a test run: the context, the browser and the Playwright driver that run it
   */
  static class PlaywrightBrowserSession implements BrowserSession {
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final BrowserDriver driver;
    private final PlaywrightEvidence evidence;

    PlaywrightBrowserSession(Playwright playwright, Browser browser, BrowserContext context, BrowserDriver driver) {
      this(playwright, browser, context, driver, null);
    }

    PlaywrightBrowserSession(Playwright playwright, Browser browser, BrowserContext context, BrowserDriver driver,
                             PlaywrightEvidence evidence) {
      this.playwright = playwright;
      this.browser = browser;
      this.context = context;
      this.driver = driver;
      this.evidence = evidence;
    }

    @Override
    public void testStarted(String testClass, String testName) {
      if (evidence != null) {
        evidence.testStarted(testClass, testName);
      }
    }

    @Override
    public void testFinished(String testName, String evidenceName, boolean failed) {
      if (evidence != null) {
        evidence.testFinished(testName, evidenceName, failed);
      }
    }

    @Override
    public void onEvidence(EvidenceListener listener) {
      if (evidence != null) {
        evidence.setListener(listener);
      }
    }

    @Override
    public BrowserDriver browser() {
      return driver;
    }

    @Override
    public void close() {
      log.info("Disposing Playwright browser...");
      RuntimeException failure = null;
      // The video is complete when its context is closed, and that is when it is kept or deleted
      for (AutoCloseable resource : new AutoCloseable[]{context == null ? null : context::close,
        evidence == null ? null : evidence::finishVideo,
        browser == null ? null : browser::close, playwright == null ? null : playwright::close}) {
        failure = closeResource(resource, failure);
      }
      if (failure != null) {
        throw failure;
      }
      log.info("Playwright browser disposed");
    }

    /**
     * Dispose of the session after a failure to open it, without hiding that failure
     */
    void closeQuietly() {
      try {
        close();
      } catch (RuntimeException exc) {
        log.debug("Could not dispose of the Playwright browser that failed to open", exc);
      }
    }

    // Each resource is closed whatever happened to the ones before: the first failure is thrown at the end
    private static RuntimeException closeResource(AutoCloseable resource, RuntimeException failure) {
      if (resource == null) {
        return failure;
      }
      try {
        resource.close();
        return failure;
      } catch (Exception exc) {
        RuntimeException current = exc instanceof RuntimeException runtime ? runtime : new IllegalStateException(exc);
        if (failure == null) {
          return current;
        }
        failure.addSuppressed(current);
        return failure;
      }
    }
  }
}
