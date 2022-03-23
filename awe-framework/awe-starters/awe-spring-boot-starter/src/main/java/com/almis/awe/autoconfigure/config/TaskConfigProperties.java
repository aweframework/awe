package com.almis.awe.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Executor task config properties
 */
@ConfigurationProperties(prefix = "awe.application.task.pool")
@Data
public class TaskConfigProperties {
  /**
   * Task executor pool size.
   * Default value 20
   */
  private Integer size = 20;
  /**
   * Task executor pool max elements.
   * Default value 50
   */
  private Integer maxSize = 50;
  /**
   * Task executor queue size.
   * Default value 1000
   */
  private Integer queueSize = 1000;
  /**
   * Task executor await termination in seconds.
   * Default value 120s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration awaitTermination = Duration.ofSeconds(120);
  /**
   * AWE Pool executor threads prefix.
   * Default value AweThread-
   */
  private String threadPrefix = "AweThread-";
  /**
   * AWE Help Pool executor threads prefix.
   * Default value AweHelpThread-
   */
  private String helpThreadPrefix = "AweHelpThread-";
  /**
   * AWE context less Pool executor threads prefix.
   * Default value AweInitThread-
   */
  private String contextlessThreadPrefix = "AweInitThread-";
}
