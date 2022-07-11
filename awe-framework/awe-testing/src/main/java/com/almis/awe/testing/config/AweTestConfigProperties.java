package com.almis.awe.testing.config;

import com.almis.awe.testing.model.types.BrowserType;
import com.almis.awe.testing.model.types.FrontendType;
import com.almis.awe.testing.model.types.RecordingSaveType;
import com.almis.awe.testing.model.types.VideoFormatType;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.time.Duration;

/**
 * AWE testing config properties
 */
@ConfigurationProperties(prefix = "awe.test")
@Validated
@Data
public class AweTestConfigProperties {
  /**
   * Selenium browser type
   */
  private BrowserType browser = BrowserType.CHROME;
  /**
   * Selenium browser width size
   */
  private int browserWidth = 1280;
  /**
   * Selenium browser height size
   */
  private int browserHeight = 1024;
  /**
   * Selenium screenshots path report
   */
  private String screenshotPath = "target/tests/selenium/screenshots/";
  /**
   * Video format type
   */
  private VideoFormatType videoFormat = VideoFormatType.WEBM;
  /**
   * Recording save mode
   */
  private RecordingSaveType videoSave = RecordingSaveType.FAILED;
  /**
   * Enable recording test video
   */
  private boolean allowedRecording = true;
  /**
   * Server host of selenium tests
   */
  private String serverHost = "localhost";
  /**
   * Server port of selenium tests
   */
  private int serverPort = 8080;
  /**
   * Context path of selenium tests
   */
  private String contextPath = "/";
  /**
   * Start Url of selenium tests
   */
  @NotNull
  @URL
  private String startUrl;
  /**
   * Selenium timeout
   */
  private Duration timeout = Duration.ofSeconds(60);
  /**
   * Enable capture mouse icon in selenium tests
   */
  private boolean showMouse = true;
  /**
   * Browser host service
   */
  private String browserHost = "browser";
  /**
   * Enable remote browser
   */
  private boolean remoteBrowser;
  /**
   * Browser container service
   */
  private String browserContainer = "browser";
  /**
   * Browser display service
   */
  private int browserDisplay = 99;
  /**
   * Browser port service
   */
  private int browserPort = 4444;
  /**
   * Url of recorder service
   */
  private String recorderUrl = "http://recorder:3000";
  /**
   * Frontend type for tests
   */
  private FrontendType frontend = FrontendType.ANGULAR;
}
