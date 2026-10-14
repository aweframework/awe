package com.almis.awe.observability.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Observability starter configuration properties
 */
@ConfigurationProperties(prefix = "awe.observability")
@Data
public class AweObservabilityProperties {

  /**
   * Enable the observability starter (health checks, metrics and traces). Set it to false to switch
   * everything it contributes off without removing the dependency.
   */
  private boolean enabled = true;

  /**
   * Maximum time a health check waits for a database or queue connection to answer before it reports the
   * connector as down.
   */
  private Duration healthTimeout = Duration.ofSeconds(2);
}
