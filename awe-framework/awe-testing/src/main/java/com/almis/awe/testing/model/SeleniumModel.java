package com.almis.awe.testing.model;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.SeleniumBrowserDriver;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.WebDriver;

import java.net.InetAddress;
import java.util.Optional;

@Data
@Accessors(chain = true)
public class SeleniumModel {

  // Properties
  private AweTestConfigProperties properties;
  // Selenium driver: wiring of the Selenium tool and source of truth of the browser. Tests do not use it directly
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
