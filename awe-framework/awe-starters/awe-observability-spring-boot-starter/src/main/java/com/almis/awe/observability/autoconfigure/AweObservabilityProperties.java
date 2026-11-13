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

  /**
   * Maximum number of distinct values of the id and screen tags of each AWE meter. Values beyond it are reported as
   * other, so a forged or unexpected identifier never adds series.
   */
  private int maxTagValues = 1000;

  /**
   * Usage metrics: screens opened, exports, files and connected users.
   */
  private Usage usage = new Usage();

  /**
   * Usage metric families. Each one can be switched off on its own.
   */
  @Data
  public static class Usage {

    /**
     * Screens opened from the menu (awe.screen).
     */
    private Screens screens = new Screens();

    /**
     * Prints and exports of the screens (awe.export).
     */
    private Exports exports = new Exports();

    /**
     * Files uploaded and downloaded (awe.files).
     */
    private Files files = new Files();

    /**
     * Users connected right now (awe.users.connected and awe.connections.active).
     */
    private Users users = new Users();
  }

  /**
   * Screens family
   */
  @Data
  public static class Screens {

    /**
     * Count and time the screens built for a menu option (awe.screen timer, tagged by option and screen).
     */
    private boolean enabled = true;
  }

  /**
   * Exports family
   */
  @Data
  public static class Exports {

    /**
     * Count and time the prints and exports of the screens (awe.export timer, tagged by screen and format).
     */
    private boolean enabled = true;
  }

  /**
   * Files family
   */
  @Data
  public static class Files {

    /**
     * Count the files uploaded and downloaded and sum their bytes (awe.files summary, tagged by direction).
     */
    private boolean enabled = true;
  }

  /**
   * Users family
   */
  @Data
  public static class Users {

    /**
     * Publish the users connected and the connections open right now (awe.users.connected and
     * awe.connections.active gauges, per instance).
     */
    private boolean enabled = true;
  }
}
