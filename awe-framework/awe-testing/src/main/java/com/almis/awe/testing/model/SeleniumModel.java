package com.almis.awe.testing.model;

import com.almis.awe.testing.config.AweTestConfigProperties;
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
  // Drivers
  private WebDriver driver;
  private WebDriverManager webDriverManager;
  // Local data
  private String currentOption;
  private String testTitle;

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
