package com.almis.awe.observability.health;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Health of the JMS broker AWE queues use: it is UP when a connection can be opened.
 */
public class AweQueueHealthIndicator extends AbstractHealthIndicator {

  private final ConnectionFactory connectionFactory;
  private final Duration timeout;
  private final TimeBoundedCall bounded = new TimeBoundedCall();

  /**
   * Autowired constructor
   *
   * @param connectionFactory JMS connection factory
   * @param timeout           Maximum time to wait for the connection
   */
  public AweQueueHealthIndicator(ConnectionFactory connectionFactory, Duration timeout) {
    super("AWE queue health check failed");
    this.connectionFactory = connectionFactory;
    this.timeout = timeout;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    try {
      String provider = bounded.run("queue", () -> {
        try (Connection connection = connectionFactory.createConnection()) {
          return connection.getMetaData().getJMSProviderName();
        }
      }, timeout);
      builder.up().withDetail("provider", provider);
    } catch (TimeBoundedCall.StillRunningException exc) {
      builder.down().withDetail("error", "Connection check still running");
    } catch (TimeoutException exc) {
      builder.down().withDetail("error", "Connection timed out after " + timeout);
    }
  }
}
