package com.almis.awe.config;

import com.almis.awe.model.rest.ServiceAuth;
import com.almis.awe.model.rest.ServiceDetails;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * AWE Rest connector config properties
 */
@ConfigurationProperties(prefix = "awe.rest")
@Validated
@Data
public class RestConfigProperties {

  /**
   * Enable AWE application as Eureka Client.
   * Default value false
   */
  private boolean clientEnabled = false;

  /**
   * Rest connection timeout in millis
   */
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration connectionTimeout = Duration.ofMillis(5000);
  /**
   * Timeout for waiting for a connection from the connection manager.
   * Default value 5 seconds
   */
  @DurationUnit(ChronoUnit.MILLIS)
  private Duration connectionRequestTimeout = Duration.ofMillis(5000);

  /**
   * Response timeout in millis that covers the entire request-response cycle
   */
  @DurationUnit(ChronoUnit.MILLIS)
  public Duration responseTimeout = Duration.ofMillis(5000);

  /**
   * Map with rest service info used by AWE microservice and REST connector.
   * [Service name, Object with service details]
   */
  private Map<String, ServiceDetails> services = new HashMap<>();

  /**
   * Service authentication
   */
  private ServiceAuth authentication;
}
