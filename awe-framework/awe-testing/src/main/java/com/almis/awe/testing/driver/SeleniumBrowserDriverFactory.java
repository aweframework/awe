package com.almis.awe.testing.driver;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Factory of the Selenium tool: creates the Selenium driver that the {@code awe.test.browser} property asks for (local,
 * headless, Docker or remote service) and publishes it in the {@link SeleniumModel}, where it stays the source of truth of
 * the Selenium wiring ({@code SeleniumModel.getDriver()}) and where the neutral browser is built from
 */
@Slf4j
public class SeleniumBrowserDriverFactory implements BrowserDriverFactory {

  public static final String WD_HUB = "/wd/hub";

  @Override
  public BrowserSession create(SeleniumModel model, String testName) throws MalformedURLException {
    WebDriver driver;
    WebDriverManager webDriverManager = null;

    // Setup window size
    AweTestConfigProperties properties = model.getProperties();
    String windowSize = "--window-size=" + properties.getBrowserWidth() + "," + properties.getBrowserHeight();
    log.info("Selected browser is {}, window size: {}x{}", properties.getBrowser(), properties.getBrowserWidth(), properties.getBrowserHeight());
    log.debug("{}", model);

    // Setup firefox options
    FirefoxProfile firefoxProfile = new FirefoxProfile();
    firefoxProfile.setPreference("network.proxy.no_proxies_on", "localhost, 127.0.0.1");
    // Set profile to accept untrusted certificates
    firefoxProfile.setAcceptUntrustedCertificates(true);

    // Set profile to not assume certificate issuer is untrusted
    firefoxProfile.setAssumeUntrustedCertificateIssuer(false);

    //Set download location and file types
    firefoxProfile.setPreference("browser.download.folderList",2);
    firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
    firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk","text/csv,application/pdf,application/csv,application/vnd.ms-excel");
    firefoxProfile.setPreference("browser.download.start_downloads_in_tmp_dir", true);

    // Set to false so popup not displayed when download finished.
    firefoxProfile.setPreference("browser.download.manager.showAlertOnComplete", false);
    firefoxProfile.setPreference("browser.download.panel.shown", false);
    firefoxProfile.setPreference("browser.download.useToolkitUI", true);

    // Set this to true to disable the pdf opening
    firefoxProfile.setPreference("pdfjs.disabled", true);

    FirefoxOptions firefoxOptions = new FirefoxOptions()
      .setProfile(firefoxProfile)
      .addArguments(windowSize)
      .setLogLevel(FirefoxDriverLogLevel.ERROR);

    // Setup chrome options
    ChromeOptions chromeOptions = new ChromeOptions()
      .addArguments("start-maximized")
      .addArguments("--no-sandbox")
      .addArguments("--disable-dev-shm-usage")
      .addArguments("--disable-gpu")
      .addArguments("--remote-allow-origins=*")
      .addArguments(windowSize);

    // Setup edge options
    EdgeOptions edgeOptions = new EdgeOptions();
    edgeOptions.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
    Map<String, Object> map = new HashMap<>();
    map.put("args", Arrays.asList("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu", "--whitelisted-ips=", "--allowed-origins='*'"));
    edgeOptions.setCapability("ms:edgeOptions", map);

    // Define browser web driver container
    switch (properties.getBrowser()) {
      case HEADLESS_FIREFOX:
        firefoxOptions.addArguments("--headless");
        driver = getFirefoxDriver(firefoxOptions);
        break;
      case HEADLESS_CHROME:
        chromeOptions.addArguments("--headless=new");
        driver = getChromeDriver(chromeOptions);
        break;
      case REMOTE_FIREFOX:
        webDriverManager = WebDriverManager.firefoxdriver();
        driver = getDockerDriver(webDriverManager, model, testName, firefoxOptions);
        break;
      case REMOTE_CHROME:
        webDriverManager = WebDriverManager.chromedriver();
        driver = getDockerDriver(webDriverManager, model, testName, chromeOptions);
        break;
      case SERVICE_FIREFOX:
        driver = getRemoteDriver(model, firefoxOptions, WD_HUB);
        break;
      case SERVICE_CHROME:
        driver = getRemoteDriver(model, chromeOptions, "");
        break;
      case SERVICE_EDGE:
        driver = getRemoteDriver(model, edgeOptions, WD_HUB);
        break;
      case SERVICE_OPERA:
        chromeOptions.setBinary(new File("/path/to/opera"));
        driver = getRemoteDriver(model, chromeOptions, "");
        break;
      case SERVICE_SAFARI:
        driver = getRemoteDriver(model, new SafariOptions(), WD_HUB);
        break;
      case OPERA:
        WebDriverManager.operadriver().setup();
        // The Opera driver does not support w3c syntax, so we recommend using chromedriver to work with Opera
        chromeOptions.setBinary(new File("/path/to/opera"));
        driver = new ChromeDriver(chromeOptions);
        break;
      case EDGE:
        WebDriverManager.edgedriver().setup();
        driver = new EdgeDriver(edgeOptions);
        break;
      case IE:
        WebDriverManager.iedriver().setup();
        driver = new InternetExplorerDriver();
        break;
      case FIREFOX:
        driver = getFirefoxDriver(firefoxOptions);
        break;
      case CHROME:
      default:
        driver = getChromeDriver(chromeOptions);
        break;
    }

    // Set dimension if defined
    driver.manage().window().setSize(new Dimension(
      properties.getBrowserWidth(),
      properties.getBrowserHeight()));

    // Set selenium model
    model
      .setDriver(driver)
      .setWebDriverManager(webDriverManager);

    return new SeleniumBrowserSession(driver, webDriverManager);
  }

  private WebDriver getChromeDriver(ChromeOptions options) {
    setUpDriverBinary(WebDriverManager.chromedriver());
    return launchChrome(options);
  }

  private WebDriver getFirefoxDriver(FirefoxOptions options) {
    setUpDriverBinary(WebDriverManager.firefoxdriver());
    return launchFirefox(options);
  }

  /**
   * Download and set up the binary of a local driver
   *
   * @param manager Driver manager of the browser
   */
  protected void setUpDriverBinary(WebDriverManager manager) {
    manager.setup();
  }

  protected WebDriver launchChrome(ChromeOptions options) {
    return new ChromeDriver(options);
  }

  protected WebDriver launchFirefox(FirefoxOptions options) {
    return new FirefoxDriver(options);
  }

  private WebDriver getDockerDriver(WebDriverManager driverManager, SeleniumModel model, String name, Capabilities capabilities) {
    final AweTestConfigProperties properties = model.getProperties();
    driverManager
      .browserInDocker().enableRecording().dockerRecordingOutput(Paths.get(properties.getScreenshotPath()))
      .dockerRecordingPrefix(name + "-")
      .capabilities(capabilities)
      .dockerScreenResolution(properties.getBrowserWidth() + "x" + properties.getBrowserHeight() + "x24");
    properties.setRemoteBrowser(true);
    properties.setAllowedRecording(false);
    return driverManager.create();
  }

  private RemoteWebDriver getRemoteDriver(SeleniumModel model, Capabilities capabilities, String browserHubPath) throws MalformedURLException {
    final AweTestConfigProperties properties = model.getProperties();
    System.setProperty("isDocker", properties.getRecorderUrl() == null ? "browser" : "browserRecorder");
    URL url = new URL(String.format("http://%s:%d%s", properties.getBrowserHost(), properties.getBrowserPort(), browserHubPath));
    log.info("{} URL is {}", properties.getBrowser(), url);
    RemoteWebDriver webDriver = new RemoteWebDriver(url, capabilities, false);

    Point position = new Point(0, 0);
    Dimension dimension = new Dimension(properties.getBrowserWidth(), properties.getBrowserHeight());
    webDriver.manage().window().setPosition(position);
    webDriver.manage().window().setSize(dimension);
    System.setProperty("ffmpeg.display", String.format("%s:%d+%d,%d", Optional.ofNullable(properties.getBrowserContainer())
      .filter(StringUtils::isNotBlank)
      .orElse(properties.getBrowserHost()), properties.getBrowserDisplay(), position.x, position.y));
    System.setProperty("video.recorder.url", properties.getRecorderUrl());
    properties.setRemoteBrowser(true);

    return webDriver;
  }

  /**
   * Selenium browser of a test run: the driver and, for the Docker browsers, the manager that started the container
   */
  static class SeleniumBrowserSession implements BrowserSession {
    private final WebDriver driver;
    private final WebDriverManager webDriverManager;

    SeleniumBrowserSession(WebDriver driver, WebDriverManager webDriverManager) {
      this.driver = driver;
      this.webDriverManager = webDriverManager;
    }

    @Override
    public BrowserDriver browser() {
      return new SeleniumBrowserDriver(driver);
    }

    @Override
    public void close() {
      try {
        log.info("Disposing web driver...");
        driver.quit();
        log.info("Web driver disposed");
      } finally {
        // A Docker browser that already died must not leave its container running
        if (webDriverManager != null) {
          log.info("Disposing web driver manager...");
          webDriverManager.quit();
          log.info("Web driver manager disposed");
        }
      }
    }
  }
}
