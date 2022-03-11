package com.almis.awe.model.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * WebTooltip options
 * 
 * @author pgarcia
 */
@Data
@ConfigurationProperties(prefix = "awe.application.component.tooltip")
public class WebTooltip {
  /**
   *  Timeout for ok messages (in milliseconds).
   *  Default value 2000ms
   */
  @DurationUnit(ChronoUnit.MILLIS)
  @JsonProperty("ok")
  private Duration okTimeout = Duration.ofMillis(2000);

  /**
   *  Timeout for wrong messages (in milliseconds).
   *  Default value 0ms
   */
  @JsonProperty("wrong")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration wrongTimeout = Duration.ofMillis(0);
  /**
   *  Timeout for error messages (in milliseconds).
   *  Default value 0ms
   */
  @JsonProperty("error")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration errorTimeout = Duration.ofMillis(0);
  /**
   *  Timeout for warning messages (in milliseconds).
   *  Default value 4000ms
   */
  @JsonProperty("warning")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration warningTimeout = Duration.ofMillis(4000);
  /**
   *  Timeout for info messages (in milliseconds).
   *  Default value 0ms
   */
  @JsonProperty("info")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration infoTimeout = Duration.ofMillis(0);
  /**
   *  Timeout for validation messages (in milliseconds).
   *  Default value 2000ms
   */
  @JsonProperty("validate")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration validationTimeout = Duration.ofMillis(2000);
  /**
   *  Timeout for help messages (in milliseconds).
   *  Default value 5000ms
   */
  @JsonProperty("help")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration helpTimeout = Duration.ofMillis(5000);
  /**
   *  Timeout for chat messages (in milliseconds).
   *  Default value 0ms
   */
  @JsonProperty("chat")
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration chatTimeout = Duration.ofMillis(0);


  /**
   * Get ok message Timeout in milliseconds
   * @return ok Timeout in milliseconds
   */
  public long getOkTimeout() {
    return okTimeout.toMillis();
  }

  /**
   * Get wrong message Timeout in milliseconds
   * @return wrong Timeout in milliseconds
   */
  public long getWrongTimeout() {
    return wrongTimeout.toMillis();
  }

  /**
   * Get error message Timeout in milliseconds
   * @return error Timeout in milliseconds
   */
  public long getErrorTimeout() {
    return errorTimeout.toMillis();
  }

  /**
   * Get warning message Timeout in milliseconds
   * @return warning Timeout in milliseconds
   */
  public long getWarningTimeout() {
    return warningTimeout.toMillis();
  }

  /**
   * Get info message Timeout in milliseconds
   * @return info Timeout in milliseconds
   */
  public long getInfoTimeout() {
    return infoTimeout.toMillis();
  }

  /**
   * Get validation message Timeout in milliseconds
   * @return validation Timeout in milliseconds
   */
  public long getValidationTimeout() {
    return validationTimeout.toMillis();
  }

  /**
   * Get help message Timeout in milliseconds
   * @return help Timeout in milliseconds
   */
  public long getHelpTimeout() {
    return helpTimeout.toMillis();
  }

  /**
   * Get chat message Timeout in milliseconds
   * @return chat Timeout in milliseconds
   */
  public long getChatTimeout() {
    return chatTimeout.toMillis();
  }
}
