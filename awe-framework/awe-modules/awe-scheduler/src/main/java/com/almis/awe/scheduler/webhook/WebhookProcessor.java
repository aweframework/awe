package com.almis.awe.scheduler.webhook;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.dao.TaskDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Launches the tasks of the webhooks stored in the inbox, outside the request that received them. It runs on every
 * node and the claim of each message lets only one of them launch it; being based on the database, it keeps working
 * across restarts. A failed message is tried again after a growing wait and, after its last attempt, stays failed with
 * its error.
 */
@Slf4j
public class WebhookProcessor implements ApplicationListener<WebhookStoredEvent> {

  /**
   * Attempts a message has before it stays failed
   */
  static final int MAX_ATTEMPTS = 3;
  private static final int BATCH_SIZE = 20;
  private static final int MAX_BATCHES_PER_SWEEP = 10;
  private static final Duration LEASE = Duration.ofMinutes(2);
  private static final Duration PURGE_INTERVAL = Duration.ofHours(1);
  private static final int MAX_ERROR_LENGTH = 2000;
  private static final List<Duration> BACKOFF = List.of(Duration.ofSeconds(30), Duration.ofMinutes(2),
    Duration.ofMinutes(10));
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private final WebhookInboxRepository repository;
  private final TaskDAO taskDAO;
  private final Duration interval;
  private final Duration retention;
  private final BooleanSupplier enabled;
  private final Clock clock;
  private ScheduledExecutorService executor;
  private Instant lastPurge;

  /**
   * Constructor
   *
   * @param repository Inbox
   * @param taskDAO    Task DAO that launches the tasks
   * @param interval   Time between two checks of the inbox
   * @param retention  Time the messages that are over stay in the inbox
   * @param enabled    Whether this node launches tasks now: false on a node that only receives the webhooks, and while
   *                   its scheduler is not running, so the messages wait in the inbox instead of in a stopped scheduler
   * @param clock      Clock
   */
  public WebhookProcessor(WebhookInboxRepository repository, TaskDAO taskDAO, Duration interval, Duration retention,
                          BooleanSupplier enabled, Clock clock) {
    this.repository = repository;
    this.taskDAO = taskDAO;
    this.interval = interval;
    this.retention = retention;
    this.enabled = enabled;
    this.clock = clock;
  }

  /**
   * Start the periodic check
   */
  public void start() {
    executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "awe-scheduler-webhook-processor");
      thread.setDaemon(true);
      return thread;
    });
    executor.scheduleWithFixedDelay(this::sweep, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Stop the periodic check
   */
  public void stop() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  @Override
  public void onApplicationEvent(WebhookStoredEvent event) {
    // A message arrived: do not wait for the next check. The executor has one thread, so sweeps never overlap
    if (executor != null && !executor.isShutdown()) {
      executor.execute(this::sweep);
    }
  }

  /**
   * Launch the due messages and purge the old ones. It never throws, so a failure cannot cancel the periodic run
   */
  public void sweep() {
    if (!enabled.getAsBoolean()) {
      return;
    }
    try {
      for (int batch = 0; batch < MAX_BATCHES_PER_SWEEP; batch++) {
        List<WebhookMessage> due = repository.findDue(clock.instant(), MAX_ATTEMPTS, BATCH_SIZE);
        if (due.isEmpty()) {
          break;
        }
        due.forEach(this::process);
      }
      purgeIfDue();
    } catch (Exception exc) {
      log.error("[SCHEDULER][WEBHOOK] Error processing the webhook inbox", exc);
    }
  }

  private void process(WebhookMessage message) {
    String hookKey = message.getHookKey();
    try {
      Instant now = clock.instant();
      if (!repository.claim(message, now.plus(LEASE))) {
        // Another node claimed it
        return;
      }

      WebhookHook hook = repository.findHook(hookKey).orElse(null);
      if (hook == null || !hook.isEnabled()) {
        repository.markIgnored(message, "The hook or its task is not active", now);
        log.info("[SCHEDULER][WEBHOOK {}] Message ignored: the hook or its task is not active", hookKey);
      } else if (hook.getTypeFilter() != null && !hook.getTypeFilter().isBlank()
        && !hook.getTypeFilter().equals(message.getType())) {
        repository.markIgnored(message, "The type of the message does not match the filter of the hook", now);
        log.info("[SCHEDULER][WEBHOOK {}] Message ignored: its type does not match the filter", hookKey);
      } else {
        taskDAO.launchWebhook(hook.getTaskId(), hookKey, MAPPER.readTree(message.getPayload()).path("data"));
        repository.markProcessed(message, hook.getTaskId(), now);
      }
    } catch (Exception exc) {
      fail(message, exc);
    }
  }

  private void fail(WebhookMessage message, Exception exc) {
    int attempt = message.getAttempts() + 1;
    boolean lastAttempt = attempt >= MAX_ATTEMPTS;
    log.error("[SCHEDULER][WEBHOOK {}] Attempt {} of {} failed{}", message.getHookKey(), attempt, MAX_ATTEMPTS,
      lastAttempt ? ": no attempts left" : "", exc);
    try {
      String error = String.valueOf(exc.getMessage());
      repository.markFailed(message, error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH)),
        clock.instant().plus(BACKOFF.get(Math.min(attempt, BACKOFF.size()) - 1)));
    } catch (Exception failure) {
      // The lease of the claim makes it due again later
      log.error("[SCHEDULER][WEBHOOK {}] Error recording the failure", message.getHookKey(), failure);
    }
  }

  private void purgeIfDue() throws AWException {
    Instant now = clock.instant();
    if (lastPurge == null || Duration.between(lastPurge, now).compareTo(PURGE_INTERVAL) >= 0) {
      repository.purge(now.minus(retention));
      lastPurge = now;
    }
  }
}
