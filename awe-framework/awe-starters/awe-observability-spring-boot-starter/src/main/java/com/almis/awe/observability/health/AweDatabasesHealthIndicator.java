package com.almis.awe.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Health of the databases AWE connects to by alias (multidatabase mode).
 *
 * <p>The default datasource is already checked by the Spring Boot {@code db} indicator; this one adds a check per
 * alias, reported as one detail each, and is {@code DOWN} when any of them is.</p>
 */
@Slf4j
public class AweDatabasesHealthIndicator extends AbstractHealthIndicator {

  private final Supplier<Map<Object, Object>> dataSources;
  private final Duration timeout;
  private final TimeBoundedCall bounded = new TimeBoundedCall();

  /**
   * Autowired constructor
   *
   * @param dataSources Supplier of the datasources by alias
   * @param timeout     Maximum time of the check of each database, from getting a connection to validating it
   */
  public AweDatabasesHealthIndicator(Supplier<Map<Object, Object>> dataSources, Duration timeout) {
    super("AWE databases health check failed");
    this.dataSources = dataSources;
    this.timeout = timeout;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    Map<Object, Object> current = dataSources.get();
    if (current == null || current.isEmpty()) {
      builder.unknown().withDetail("databases", "No database loaded yet");
      return;
    }

    boolean allUp = true;
    for (Map.Entry<Object, Object> entry : current.entrySet()) {
      String result = check(entry.getKey(), entry.getValue());
      allUp &= "UP".equals(result);
      builder.withDetail(String.valueOf(entry.getKey()), result);
    }
    builder.status(allUp ? "UP" : "DOWN");
  }

  private String check(Object alias, Object candidate) {
    if (!(candidate instanceof DataSource dataSource)) {
      return "DOWN: not a datasource";
    }
    try {
      boolean valid = bounded.run(alias, () -> {
        try (Connection connection = dataSource.getConnection()) {
          return connection.isValid((int) Math.max(1, timeout.toSeconds()));
        }
      }, timeout);
      return valid ? "UP" : "DOWN: connection is not valid";
    } catch (TimeBoundedCall.StillRunningException exc) {
      return "DOWN: check still running";
    } catch (TimeoutException exc) {
      return "DOWN: timed out after " + timeout;
    } catch (Exception exc) {
      log.debug("Database health check failed", exc);
      return "DOWN: " + exc.getMessage();
    }
  }
}
