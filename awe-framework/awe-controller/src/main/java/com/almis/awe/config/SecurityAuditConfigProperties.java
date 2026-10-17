package com.almis.awe.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Security audit properties
 */
@Data
@ConfigurationProperties(prefix = "awe.security.audit")
public class SecurityAuditConfigProperties {
  /**
   * Record security events (logins, logouts, locks, password and second factor changes, denied accesses and REST
   * tokens). When disabled no event is recorded by any sink and nothing is purged.
   * Default value true
   */
  private boolean enabled = true;
  /**
   * Anonymize the client address of every event: the last octet of IPv4 addresses and everything after the first 48
   * bits of IPv6 addresses are set to zero.
   * Default value false
   */
  private boolean anonymizeIp = false;
  /**
   * Database sink
   */
  @NestedConfigurationProperty
  private Database database = new Database();
  /**
   * JSON log sink
   */
  @NestedConfigurationProperty
  private Log log = new Log();
  /**
   * Micrometer sink
   */
  @NestedConfigurationProperty
  private Metrics metrics = new Metrics();

  /**
   * Database sink properties
   */
  @Data
  public static class Database {
    /**
     * Store the events in the AweSecAudEvt table.
     * Default value true
     */
    private boolean enabled = true;
    /**
     * Days an event is kept in the AweSecAudEvt table before the purge deletes it. 0 keeps the events forever.
     * Default value 365
     */
    private int retentionDays = 365;
    /**
     * Cron expression (Spring format, server time zone) of the purge of expired events.
     * Default value 0 0 3 * * * (every day at 03:00)
     */
    private String purgeCron = "0 0 3 * * *";
  }

  /**
   * JSON log sink properties
   */
  @Data
  public static class Log {
    /**
     * Write every event as one JSON line on the awe.security.audit logger (INFO level), to be shipped to a SIEM.
     * Default value true
     */
    private boolean enabled = true;
  }

  /**
   * Micrometer sink properties
   */
  @Data
  public static class Metrics {
    /**
     * Count the events on the awe.security.audit.events Micrometer counter (tags type, outcome, channel and cause). Only
     * applies when the observability starter (or another Micrometer registry) is present.
     * Default value true
     */
    private boolean enabled = true;
  }
}
