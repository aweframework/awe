package com.almis.awe.scheduler.autoconfigure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Schedule Task Pool config properties
 */
@Data
@ConfigurationProperties(prefix = "awe.scheduler.task-pool")
public class SchedulerTaskConfigProperties {
  /**
   * Schedule Task pool size
   * The Default value is 20
   */
  private Integer size = 20;
  /**
   * Schedule Task pool max size
   * The Default value is 50
   */
  private Integer maxSize = 50;
  /**
   * Schedule Task pool queue capacity
   * The Default value is 1000
   */
  private Integer queueSize = 1000;
  /**
   * Schedule Task pool termination.
   * You can use Duration format (1ms, 1s, 1m, 1h, 1d, ...) See {@link Duration}
   * The Default value is 120s
   */
  private Duration termination = Duration.ofSeconds(120);
}
