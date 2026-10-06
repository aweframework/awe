package com.almis.awe.testing.driver;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.model.types.BrowserType;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SeleniumBrowserDriverFactoryTest {

  private AweTestConfigProperties properties;
  private SeleniumModel model;
  private WebDriver webDriver;
  private final List<DriverManagerType> setUps = new ArrayList<>();
  private ChromeOptions chromeOptions;
  private FirefoxOptions firefoxOptions;
  private SeleniumBrowserDriverFactory factory;

  @BeforeEach
  void setUp() {
    properties = new AweTestConfigProperties();
    properties.setBrowserWidth(800);
    properties.setBrowserHeight(600);
    properties.setScreenshotPath("target/tests/selenium/screenshots/");
    model = new SeleniumModel().setProperties(properties);
    webDriver = mock(WebDriver.class, RETURNS_DEEP_STUBS);
    factory = new SeleniumBrowserDriverFactory() {
      @Override
      protected void setUpDriverBinary(WebDriverManager manager) {
        setUps.add(manager.getDriverManagerType());
      }

      @Override
      protected WebDriver launchChrome(ChromeOptions options) {
        chromeOptions = options;
        return webDriver;
      }

      @Override
      protected WebDriver launchFirefox(FirefoxOptions options) {
        firefoxOptions = options;
        return webDriver;
      }
    };
  }

  @Test
  void firefoxSetsUpGeckodriverAndNotChromedriver() throws Exception {
    properties.setBrowser(BrowserType.FIREFOX);

    factory.create(model, "test");

    assertThat(setUps).containsExactly(DriverManagerType.FIREFOX);
    assertThat(firefoxOptions).isNotNull();
    assertThat(chromeOptions).isNull();
  }

  @Test
  void headlessFirefoxSetsUpGeckodriverAndRunsHeadless() throws Exception {
    properties.setBrowser(BrowserType.HEADLESS_FIREFOX);

    factory.create(model, "test");

    assertThat(setUps).containsExactly(DriverManagerType.FIREFOX);
    assertThat(arguments(firefoxOptions.asMap().get("moz:firefoxOptions"))).contains("--headless", "--window-size=800,600");
  }

  @Test
  void chromeSetsUpChromedriver() throws Exception {
    properties.setBrowser(BrowserType.CHROME);

    factory.create(model, "test");

    assertThat(setUps).containsExactly(DriverManagerType.CHROME);
    assertThat(arguments(chromeOptions.asMap().get("goog:chromeOptions"))).contains("--window-size=800,600").doesNotContain("--headless=new");
  }

  @Test
  void headlessChromeRunsHeadless() throws Exception {
    properties.setBrowser(BrowserType.HEADLESS_CHROME);

    factory.create(model, "test");

    assertThat(arguments(chromeOptions.asMap().get("goog:chromeOptions"))).contains("--headless=new");
  }

  @Test
  void theSessionPublishesTheSeleniumDriverInTheModelAndSizesTheWindow() throws Exception {
    properties.setBrowser(BrowserType.HEADLESS_CHROME);

    BrowserSession session = factory.create(model, "test");

    assertThat(session.browser()).isInstanceOf(SeleniumBrowserDriver.class);
    assertThat(model.getBrowser()).isInstanceOf(SeleniumBrowserDriver.class);
    @SuppressWarnings("deprecation") WebDriver published = model.getDriver();
    assertThat(published).isSameAs(webDriver);
    verify(webDriver.manage().window()).setSize(new Dimension(800, 600));
  }

  @Test
  void closingTheSessionQuitsTheDriver() throws Exception {
    properties.setBrowser(BrowserType.HEADLESS_CHROME);
    BrowserSession session = factory.create(model, "test");

    session.close();

    verify(webDriver).quit();
  }

  @Test
  void closingTheSessionStopsTheBrowserContainerEvenWhenTheDriverCannotQuit() {
    WebDriverManager manager = mock(WebDriverManager.class);
    doThrow(new WebDriverException("the browser is gone")).when(webDriver).quit();
    BrowserSession session = new SeleniumBrowserDriverFactory.SeleniumBrowserSession(webDriver, manager);

    assertThatThrownBy(session::close).isInstanceOf(WebDriverException.class);

    // The container of a Docker browser is not left running
    verify(manager).quit();
  }

  @SuppressWarnings("unchecked")
  private static List<String> arguments(Object vendorOptions) {
    return (List<String>) ((Map<String, Object>) vendorOptions).get("args");
  }
}
