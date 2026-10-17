package com.almis.awe.security.audit;

import com.almis.awe.config.SecurityAuditConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Clock;
import java.time.temporal.ChronoUnit;

/**
 * Deletes the security audit events older than {@code awe.security.audit.database.retention-days} on the
 * {@code awe.security.audit.database.purge-cron} schedule. It runs on its own thread, so applications do not need
 * scheduling enabled; with several nodes every node runs the same idempotent deletion.
 */
@Slf4j
public class SecurityAuditPurgeScheduler implements SmartLifecycle {

  private final DatabaseSecurityAuditSink sink;
  private final SecurityAuditConfigProperties properties;
  private final Clock clock;
  private ThreadPoolTaskScheduler scheduler;

  /**
   * Constructor
   *
   * @param sink       Database sink
   * @param properties Security audit properties
   * @param clock      Clock
   */
  public SecurityAuditPurgeScheduler(DatabaseSecurityAuditSink sink, SecurityAuditConfigProperties properties, Clock clock) {
    this.sink = sink;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Delete the expired events now
   *
   * @return Deleted events
   */
  public int purgeExpiredEvents() {
    int retentionDays = properties.getDatabase().getRetentionDays();
    if (retentionDays <= 0) {
      return 0;
    }
    try {
      int deleted = sink.purge(clock.instant().minus(retentionDays, ChronoUnit.DAYS));
      log.info("Security audit purge: {} events older than {} days deleted", deleted, retentionDays);
      return deleted;
    } catch (RuntimeException exc) {
      log.warn("Security audit purge failed", exc);
      return 0;
    }
  }

  @Override
  public synchronized void start() {
    if (scheduler != null || properties.getDatabase().getRetentionDays() <= 0) {
      return;
    }
    scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("AweSecurityAuditPurge-");
    scheduler.setDaemon(true);
    scheduler.initialize();
    scheduler.schedule(this::purgeExpiredEvents, new CronTrigger(properties.getDatabase().getPurgeCron()));
  }

  @Override
  public synchronized void stop() {
    if (scheduler != null) {
      scheduler.shutdown();
      scheduler = null;
    }
  }

  @Override
  public synchronized boolean isRunning() {
    return scheduler != null;
  }
}
