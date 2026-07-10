package com.almis.awe.scheduler.autoconfigure.config;

import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler module properties
 */
@Data
@ConfigurationProperties(prefix = "awe.scheduler")
public class SchedulerConfigProperties {
  /**
   * Default sender address used for scheduler report emails.
   */
  public static final String DEFAULT_REPORT_EMAIL_FROM_VALUE = "scheduler@almis.com";
  /**
   * Fully-qualified property key (relaxed-binding canonical form) for the
   * scheduler report email sender address.
   */
  public static final String REPORT_EMAIL_FROM_VALUE_PROPERTY = "awe.scheduler.report-email-from-value";

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
  private String reportEmailFromValue = DEFAULT_REPORT_EMAIL_FROM_VALUE;
  /**
   * Scheduler execution log path
   */
  @Value("${logging.file.path:${java.io.tmpdir}}/scheduler}")
  private String executionLogPath;
  /**
   * Scheduler execution log pattern
   * Default value %d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx
   */
  private String executionLogPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx";

  /**
   * Scheduler tasks default timeout in seconds
   * Default value 1800s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration taskTimeout = Duration.ofSeconds(1800);

  /**
   * Flag which indicates if scheduler is in a separate instance
   */
  private boolean remoteEnabled = false;

  /**
   * URL to call to remote scheduler instance
   */
  private URI remoteSchedulerUrl;

  /**
   * Flag which indicates if instance is a separate scheduler instance
   */
  private boolean schedulerInstance = false;

  /**
   * URL to call from remote scheduler instance
   */
  private URI remoteCallbackUrl;
  /**
   * Remote call is secured (Auth is required)
   */
  private boolean remoteCallbackSecureEnabled = true;
  /**
   * Remote user name
   */
  private String remoteCallbackUser;
  /**
   * Remote password
   */
  private String remoteCallbackPassword;

  /**
   * SSH host-key verification policy for remote command execution.
   * Default value ACCEPT_ON_FIRST_USE (trust-on-first-use)
   */
  private SshHostKeyPolicy sshHostKeyPolicy = SshHostKeyPolicy.ACCEPT_ON_FIRST_USE;

  /**
   * Path to the known_hosts file used to persist/read trusted SSH host keys.
   * Default value ${user.home}/.ssh/known_hosts
   */
  private String sshKnownHostsPath = Paths.get(System.getProperty("user.home"), ".ssh", "known_hosts").toString();

  /**
   * SSH connect and authentication timeout in seconds
   * Default value 30s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration sshConnectTimeout = Duration.ofSeconds(30);
}