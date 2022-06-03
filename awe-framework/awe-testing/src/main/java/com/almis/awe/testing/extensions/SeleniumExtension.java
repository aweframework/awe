package com.almis.awe.testing.extensions;

import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.recorder.SeleniumRecorderFactory;
import com.almis.awe.testing.utilities.TextUtilities;
import com.automation.remarks.video.recorder.IVideoRecorder;
import com.automation.remarks.video.recorder.VideoRecorder;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.opera.OperaDriver;
import org.openqa.selenium.opera.OperaOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utilities suite for selenium testing
 */
@Slf4j
public class SeleniumExtension implements AfterAllCallback, BeforeEachCallback, AfterEachCallback, TestInstancePostProcessor {

  public static final String VIDEO_SCREEN_SIZE = "video.screen.size";
  public static final String WD_HUB = "/wd/hub";
  private final SeleniumModel seleniumModel = new SeleniumModel();
  private WebDriver driver;
  private WebDriverManager webDriverManager;
  private IVideoRecorder recorder;

  /**
   * Clean driver after a test suite
   */
  @Override
  public void afterAll(ExtensionContext extensionContext) {
    if (driver != null) {
      log.info("Disposing web driver...");
      driver.quit();
      driver = null;
      log.info("Web driver disposed");
    }

    if (webDriverManager != null) {
      log.info("Disposing web driver manager...");
      webDriverManager.quit();
      log.info("Web driver manager disposed");
    }
  }

  /**
   * Initialize test driver
   *
   * @param extensionContext Extension context
   * @throws MalformedURLException Malformed URL
   */
  private void initializeDriver(ExtensionContext extensionContext) throws MalformedURLException {

    // Setup window size
    String windowSize = "--window-size=" + seleniumModel.getBrowserWidth() + "," + seleniumModel.getBrowserHeight();
    log.info("Selected browser is {}, window size: {}x{}", seleniumModel.getBrowser(), seleniumModel.getBrowserWidth(), seleniumModel.getBrowserHeight());
    log.debug("{}", seleniumModel);

    // Setup firefox options
    FirefoxProfile firefoxProfile = new FirefoxProfile();
    firefoxProfile.setPreference("network.proxy.no_proxies_on", "localhost, 127.0.0.1");
    firefoxProfile.setPreference("browser.download.improvements_to_download_panel", false);
    FirefoxOptions firefoxOptions = new FirefoxOptions()
      .setProfile(firefoxProfile)
      .addArguments("--remote-debugging-port=9222")
      .addArguments(windowSize)
      .setLogLevel(FirefoxDriverLogLevel.ERROR);

    // Setup chrome options
    ChromeOptions chromeOptions = new ChromeOptions()
      .addArguments("start-maximized")
      .addArguments("--no-sandbox")
      .addArguments("--disable-dev-shm-usage")
      .addArguments("--disable-gpu")
      .addArguments(windowSize);

    // Setup edge options
    EdgeOptions edgeOptions = new EdgeOptions();
    edgeOptions.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
    Map<String, Object> map = new HashMap<>();
    map.put("args", Arrays.asList("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--whitelisted-ips=", "--allowed-origins='*'"));
    edgeOptions.setCapability("ms:edgeOptions", map);

    // Set video folder property
    System.setProperty("video.folder", seleniumModel.getScreenshotPath());
    System.setProperty("video.file.extension", seleniumModel.getVideoFormat());
    System.setProperty(VIDEO_SCREEN_SIZE, String.format("%dx%d", seleniumModel.getBrowserWidth(), seleniumModel.getBrowserHeight()));

    // Define browser web driver container
    switch (seleniumModel.getBrowser()) {
      case "headless-firefox":
        firefoxOptions.addArguments("--headless");
        driver = getFirefoxDriver(firefoxOptions);
        break;
      case "headless-chrome":
        chromeOptions.setHeadless(true);
        driver = getChromeDriver(chromeOptions);
        break;
      case "remote-firefox":
        webDriverManager = WebDriverManager.firefoxdriver();
        driver = getDockerDriver(webDriverManager, seleniumModel, extensionContext.getDisplayName(), firefoxOptions);
        break;
      case "remote-chrome":
        webDriverManager = WebDriverManager.chromedriver();
        driver = getDockerDriver(webDriverManager, seleniumModel, extensionContext.getDisplayName(), chromeOptions);
        break;
      case "service-firefox":
        driver = getRemoteDriver(seleniumModel, firefoxOptions, WD_HUB);
        break;
      case "service-chrome":
        driver = getRemoteDriver(seleniumModel, chromeOptions, "");
        break;
      case "service-edge":
        driver = getRemoteDriver(seleniumModel, edgeOptions, WD_HUB);
        break;
      case "service-opera":
        driver = getRemoteDriver(seleniumModel, new OperaOptions(), "");
        break;
      case "service-safari":
        driver = getRemoteDriver(seleniumModel, new SafariOptions(), WD_HUB);
        break;
      case "opera":
        WebDriverManager.operadriver().setup();
        driver = new OperaDriver();
        break;
      case "edge":
        WebDriverManager.edgedriver().setup();
        driver = new EdgeDriver(edgeOptions);
        break;
      case "ie":
        WebDriverManager.iedriver().setup();
        driver = new InternetExplorerDriver();
        break;
      case "firefox":
        driver = getFirefoxDriver(firefoxOptions);
        break;
      case "chrome":
      default:
        driver = getChromeDriver(chromeOptions);
        break;
    }

    // Set dimension if defined
    if (seleniumModel.getBrowserWidth() != null && seleniumModel.getBrowserHeight() != null) {
      driver.manage().window().setSize(new Dimension(seleniumModel.getBrowserWidth(), seleniumModel.getBrowserHeight()));
    }

    // Set selenium model
    seleniumModel
      .setDriver(driver)
      .setWebDriverManager(webDriverManager);
  }

  private WebDriver getChromeDriver(ChromeOptions options) {
    WebDriverManager.chromedriver().setup();
    return new ChromeDriver(options);
  }

  private WebDriver getFirefoxDriver(FirefoxOptions options) {
    WebDriverManager.chromedriver().setup();
    return new FirefoxDriver(options);
  }

  private WebDriver getDockerDriver(WebDriverManager driverManager, SeleniumModel model, String name, Capabilities capabilities) {
    driverManager
      .browserInDocker().enableRecording().recordingOutput(model.getScreenshotPath())
      .recordingPrefix(name + "-")
      .capabilities(capabilities)
      .dockerScreenResolution(model.getBrowserWidth() + "x" + model.getBrowserHeight() + "x24");
    model.setRemoteBrowser(true);
    model.setAllowedRecording(false);
    return driverManager.create();
  }

  private RemoteWebDriver getRemoteDriver(SeleniumModel model, Capabilities capabilities, String browserHubPath) throws MalformedURLException {
    System.setProperty("isDocker", model.getRecorderUrl() == null ? "browser" : "browserRecorder");
    URL url = new URL(String.format("http://%s:%d%s", model.getBrowserHost(), model.getBrowserPort(), browserHubPath));
    log.info("{} URL is {}", model.getBrowser(), url);
    RemoteWebDriver webDriver = new RemoteWebDriver(url, capabilities);

    Point position = new Point(0,0);
    Dimension dimension = new Dimension(model.getBrowserWidth(), model.getBrowserHeight());
    webDriver.manage().window().setPosition(position);
    webDriver.manage().window().setSize(dimension);
    System.setProperty("ffmpeg.display", String.format("%s:%d+%d,%d", Optional.ofNullable(model.getBrowserContainer()).filter(StringUtils::isNotBlank).orElse(model.getBrowserHost()), model.getBrowserDisplay(), position.x, position.y));
    System.setProperty("video.recorder.url", model.getRecorderUrl());
    seleniumModel.setRemoteBrowser(true);

    return webDriver;
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {

    // If driver has been initialized, return
    if (driver == null) {
      initializeDriver(extensionContext);
    }

    // Set test title
    seleniumModel.setTestTitle(extensionContext.getDisplayName());

    // Check recording
    if (seleniumModel.isAllowedRecording()) {
      this.recorder = SeleniumRecorderFactory.getRecorder(VideoRecorder.conf().recorderType());
      this.recorder.start();
    }
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    boolean testFailed = extensionContext.getExecutionException().isPresent();

    if (seleniumModel.isAllowedRecording()) {
      log.debug("Storing video recording...");
      String fileName = String.format("%s-%s-%s%s-%s",
        extensionContext.getParent().orElse(extensionContext).getDisplayName(),
        new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss").format(new Date()),
        testFailed ? "[ERR0R]-" : "",
        TextUtilities.sanitizeMessage(seleniumModel.getCurrentOption()),
        TextUtilities.sanitizeMessage(seleniumModel.getTestTitle()));
      File result = this.recorder.stopAndSave(fileName);

      if (testFailed || "ALL".equalsIgnoreCase(seleniumModel.getVideoSave())) {
        log.info("{}Video recording stored at {}", testFailed ? "Test failed. " : "", result.getAbsolutePath());
      } else {
        // Remove video file if test not failed
        result.deleteOnExit();
      }
    }
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext extensionContext) throws Exception {
    testInstance.getClass()
      .getMethod("setSeleniumModel", SeleniumModel.class)
      .invoke(testInstance, seleniumModel);
  }
}
