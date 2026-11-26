package com.almis.awe.scheduler.autoconfigure.config;

import com.almis.awe.scheduler.enums.ExecutionLogStoreType;
import com.almis.awe.scheduler.enums.SshHostKeyPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Scheduler module properties
 */
@Data
@Validated
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
   * Scheduler execution log path. A single placeholder: the property itself, or by default the logging folder (or the
   * temporary folder) followed by /scheduler.
   */
  @Value("${awe.scheduler.execution-log-path:${logging.file.path:${java.io.tmpdir}}/scheduler}")
  private String executionLogPath;
  /**
   * Scheduler execution log pattern
   * Default value %d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx
   */
  private String executionLogPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p : %m%n%wEx";
  /**
   * Execution log store selection: {@code file} (default, on-disk, unchanged behavior) or
   * {@code database} (opt-in, persists to the shared scheduler datasource). An unrecognized
   * value fails application context startup instead of silently falling back.
   */
  @Valid
  private ExecutionLogStoreType executionLogStore = ExecutionLogStoreType.FILE;

  /**
   * Database-mode execution log window size (head + tail lines retained per execution).
   * Default value 1000
   */
  @Min(10)
  private int executionLogMaxLines = 1000;

  /**
   * Database-mode defensive per-line character cap, independent of the line-count window.
   * Default value 1000
   */
  @Min(80)
  private int executionLogMaxLineLength = 1000;

  /**
   * Database-mode bounded async capture queue capacity. Overflow drops lines rather than
   * blocking the task.
   * Default value 10000
   */
  @Min(100)
  private int executionLogQueueCapacity = 10000;

  /**
   * Database-mode dirty-slot count that triggers an eager flush, and the maximum number of
   * queued events drained per writer poll cycle.
   * Default value 200
   */
  @Min(1)
  private int executionLogBatchSize = 200;

  /**
   * Database-mode time-based flush trigger; kept below the log-viewer's 5s autorefresh so a
   * running execution keeps tailing live.
   * Default value 2s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration executionLogFlushInterval = Duration.ofSeconds(2);

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
   * User to log in at the remote scheduler instance. When empty, calls are sent without credentials
   */
  private String remoteSchedulerUser;
  /**
   * Password to log in at the remote scheduler instance
   */
  private String remoteSchedulerPassword;

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
   * Deferred executions: the ones that answered accepted and finish later
   */
  @Valid
  private Deferred deferred = new Deferred();

  /**
   * Webhooks that launch tasks
   */
  @Valid
  private Webhooks webhooks = new Webhooks();

  /**
   * Application-side kill switch for tagging an admitted remote-maintain callback with the
   * propagated execution key (D3, D7). Gates only the callback filter bean; has no effect on the
   * scheduler's own header, which is always sent, nor on the {@code database} execution-log store
   * selection itself.
   * Default value true
   */
  private boolean executionLogCallbackCapture = true;

  /**
   * SSH host-key verification policy for remote command execution.
   * Default value ACCEPT_ON_FIRST_USE (trust-on-first-use)
   */
  @Valid
  private SshHostKeyPolicy sshHostKeyPolicy = SshHostKeyPolicy.ACCEPT_ON_FIRST_USE;

  /**
   * Path to the known_hosts file used to persist/read trusted SSH host keys.
   * Default value ${user.home}/.ssh/known_hosts
   */
  private String sshKnownHostsPath = Path.of(System.getProperty("user.home"), ".ssh", "known_hosts").toString();

  /**
   * SSH connect and authentication timeout in seconds
   * Default value 30s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration sshConnectTimeout = Duration.ofSeconds(30);

  /**
   * Timeout in seconds for the interactive connection test on the server
   * screens, deliberately shorter than the batch-oriented sshConnectTimeout
   * Default value 10s
   */
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration connectionTestTimeout = Duration.ofSeconds(10);

  /**
   * Deferred executions properties
   */
  @Data
  public static class Deferred {
    /**
     * Time a deferred execution may stay running when its task has no timeout; after it the execution is interrupted
     * Default value 24h
     */
    @DurationUnit(ChronoUnit.HOURS)
    private Duration defaultTimeout = Duration.ofHours(24);
    /**
     * Interval between the checks for deferred executions past their due date
     * Default value 60s
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration timeoutCheckInterval = Duration.ofSeconds(60);
  }

  /**
   * Webhook properties
   */
  @Data
  public static class Webhooks {
    /**
     * Maximum size of the body of a webhook; a bigger one is answered with 413
     * Default value 256KB
     */
    private DataSize maxBodySize = DataSize.ofKilobytes(256);
    /**
     * Maximum difference between the timestamp of a webhook and the current time
     * Default value 5m
     */
    @DurationUnit(ChronoUnit.MINUTES)
    private Duration tolerance = Duration.ofMinutes(5);
    /**
     * Time the processed, ignored and definitively failed webhooks stay in the inbox
     * Default value 30d
     */
    @DurationUnit(ChronoUnit.DAYS)
    private Duration inboxRetention = Duration.ofDays(30);
    /**
     * Interval between the checks of the inbox for webhooks to launch
     * Default value 10s
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration processInterval = Duration.ofSeconds(10);
  }
}
