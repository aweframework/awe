package com.almis.awe.scheduler.autoconfigure.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler module properties
 */
@Data
@ConfigurationProperties(prefix = "awe.scheduler")
public class SchedulerConfigProperties {
  /**
   * Flag to load tasks on start application
   * Default value true
   */
  private boolean tasksLoadOnStart = true;
  /**
   * Number of executions to stored in log
   * Default value 5
   */
  private int storedExecutions = 5;
  /**
   * Scheduler report email
   */
  private String reportEmailFromValue = "scheduler@almis.com";
  /**
   * Scheduler execution log path
   */
  @Value("${logging.file.path:${java.io.tmpdir}}/scheduler")
  private String executionLogPath;
  /**
   * Scheduler execution log path
   * Default value %d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx
   */
  private String executionLogPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx";

  /**
   * Scheduler tasks default timeout in seconds
   * Default value 1800s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration taskTimeout = Duration.ofSeconds(1800);
}