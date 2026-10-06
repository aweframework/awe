package com.almis.awe.testing.model;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.SeleniumBrowserDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.WebDriver;

import java.net.InetAddress;
import java.util.Optional;

@Data
// The Selenium driver getter fails when another tool runs the browser: printing or comparing the model must not ask for it
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters = true)
@Accessors(chain = true)
public class SeleniumModel {

  // Properties
  private AweTestConfigProperties properties;
  // Selenium driver: wiring of the Selenium tool and source of truth of the browser. Tests do not use it directly. It is
  // only set when the Selenium tool opened the browser; another tool sets the browser and leaves the driver empty
  private WebDriver driver;
  // Tool-neutral browser (preview). When none was set it is the Selenium adapter of the driver
  private BrowserDriver browser;
  private WebDriverManager webDriverManager;
  // Local data
  private String currentOption;
  private String testTitle;
  // Whether a screenshot has already been stored for the test being run
  private boolean screenshotTaken;

  /**
   * Get the Selenium driver
   *
   * @return Selenium driver, or null when no browser has been opened yet
   * @throws UnsupportedOperationException When the browser is driven by another tool: there is no Selenium driver behind it
   * @deprecated Selenium specific: it exposes the Selenium driver and is only available when the tests run with the
   * Selenium tool ({@code awe.test.tool}). Use {@link #getBrowser()}. It stays available through the whole 5.x line and
   * is not removed before 6.0
   */
  @Deprecated
  public WebDriver getDriver() {
    if (driver == null && browser != null) {
      throw new UnsupportedOperationException("getDriver() is only available with the Selenium tool (awe.test.tool=selenium), "
        + "and this browser is driven by another tool. Write the step with the neutral steps of SeleniumUtilities "
        + "or with getBrowser() and a Locator");
    }
    return driver;
  }

  /**
   * Get the tool-neutral browser: the one that was set or, when none was, the Selenium adapter of the current driver
   *
   * @return Browser, or null if there is neither a browser nor a driver
   */
  public BrowserDriver getBrowser() {
    if (browser != null) {
      return browser;
    }
    return driver == null ? null : new SeleniumBrowserDriver(driver);
  }

  /**
   * Get current base url
   *
   * @return Start url
   */
  public String getBaseUrl() {
    if (properties.isRemoteBrowser()) {
      try {
        return String.format("http://%s:%d%s",
          Optional.ofNullable(properties.getServerHost()).orElse(SystemUtils.IS_OS_LINUX ? InetAddress.getLocalHost().getHostAddress() : "host.docker.internal"),
          properties.getServerPort(),
          properties.getContextPath());
      } catch (Exception exc) {
        return properties.getStartUrl();
      }
    } else {
      return properties.getStartUrl();
    }
  }
}
