package com.almis.awe.testing.model;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.SystemUtils;
import org.openqa.selenium.WebDriver;

import java.net.InetAddress;

@Data
@Accessors(chain = true)
public class SeleniumModel {

  // Drivers
  private WebDriver driver;
  private WebDriverManager webDriverManager;
  // Browser data
  private String browser;
  private Integer browserWidth;
  private Integer browserHeight;
  private String screenshotPath;

  // Services data
  private String browserHost;
  private Integer browserDisplay;
  private Integer browserPort;
  private String recorderUrl;
  private boolean remoteBrowser;

  // Tests data
  private String startUrl;
  private Integer serverPort;
  private String contextPath;
  private Integer timeout;

  // Local data
  private String currentOption;
  private String testTitle;

  // Video recording data
  private String videoSave;
  private String videoFormat;
  private boolean showMouse;
  private boolean allowedRecording = true;

  /**
   * Get current base url
   *
   * @return Start url
   */
  public String getBaseUrl() {
    if (remoteBrowser) {
      try {
        return String.format("http://%s:%d%s",
          SystemUtils.IS_OS_LINUX ? InetAddress.getLocalHost().getHostAddress() : "host.docker.internal",
          getServerPort(),
          getContextPath());
      } catch (Exception exc) {
        return getStartUrl();
      }
    } else {
      return getStartUrl();
    }
  }
}
