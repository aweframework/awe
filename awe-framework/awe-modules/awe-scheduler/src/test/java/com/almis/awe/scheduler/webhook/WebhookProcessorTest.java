package com.almis.awe.scheduler.webhook;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.dao.TaskDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Processing of the inbox: only one node claims a message, the type filter and the active flags are honoured, and a
 * failure is retried with a growing wait
 */
class WebhookProcessorTest {

  private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");

  private final WebhookInboxRepository repository = mock(WebhookInboxRepository.class);
  private final TaskDAO taskDAO = mock(TaskDAO.class);
  private WebhookProcessor processor;

  @BeforeEach
  void setUp() throws Exception {
    processor = new WebhookProcessor(repository, taskDAO, Duration.ofSeconds(10), Duration.ofDays(30), () -> true,
      Clock.fixed(NOW, ZoneOffset.UTC));
    given(repository.findHook("market")).willReturn(Optional.of(
      new WebhookHook().setTaskId(7).setHookKey("market").setActive(1).setTaskActive(1)));
    given(repository.claim(any(), any())).willReturn(true);
  }

  private static WebhookMessage message(int attempts, String type) {
    return new WebhookMessage().setHookKey("market").setMessageId("m-1").setType(type)
      .setPayload("{\"type\":\"" + type + "\",\"data\":{\"date\":\"2026-11-12\"}}").setAttempts(attempts);
  }

  private void whenDue(WebhookMessage... messages) throws Exception {
    given(repository.findDue(any(), eq(3), anyInt())).willReturn(List.of(messages), List.of());
  }

  @Test
  void aDueMessageLaunchesTheTaskWithItsDataAndIsProcessed() throws Exception {
    WebhookMessage message = message(0, "loaded");
    whenDue(message);

    processor.sweep();

    verify(taskDAO).launchWebhook(eq(7), eq("market"), any(JsonNode.class));
    verify(repository).markProcessed(message, 7, NOW);
    verify(repository).claim(message, NOW.plus(Duration.ofMinutes(2)));
  }

  @Test
  void aMessageClaimedByAnotherNodeIsNotLaunched() throws Exception {
    whenDue(message(0, "loaded"));
    given(repository.claim(any(), any())).willReturn(false);

    processor.sweep();

    verifyNoInteractions(taskDAO);
    verify(repository, never()).markProcessed(any(), any(), any());
  }

  @Test
  void aMessageOfAnotherTypeIsIgnored() throws Exception {
    WebhookMessage message = message(0, "other");
    whenDue(message);
    given(repository.findHook("market")).willReturn(Optional.of(
      new WebhookHook().setTaskId(7).setHookKey("market").setActive(1).setTaskActive(1).setTypeFilter("loaded")));

    processor.sweep();

    verifyNoInteractions(taskDAO);
    verify(repository).markIgnored(eq(message), anyString(), eq(NOW));
  }

  @Test
  void aMessageIsIgnoredWhenTheHookOrItsTaskIsNoLongerActive() throws Exception {
    WebhookMessage message = message(0, "loaded");
    whenDue(message);
    given(repository.findHook("market")).willReturn(Optional.of(
      new WebhookHook().setTaskId(7).setHookKey("market").setActive(1).setTaskActive(0)));

    processor.sweep();

    verifyNoInteractions(taskDAO);
    verify(repository).markIgnored(eq(message), anyString(), eq(NOW));
  }

  @Test
  void aFailedLaunchIsRetriedWithAGrowingWait() throws Exception {
    WebhookMessage first = message(0, "loaded");
    WebhookMessage second = message(1, "loaded");
    whenDue(first);
    doThrow(new AWException("scheduler down")).when(taskDAO).launchWebhook(any(), any(), any());

    processor.sweep();
    verify(repository).markFailed(first, "scheduler down", NOW.plus(Duration.ofSeconds(30)));

    whenDue(second);
    processor.sweep();
    verify(repository).markFailed(second, "scheduler down", NOW.plus(Duration.ofMinutes(2)));
    verify(repository, never()).markProcessed(any(), any(), any());
  }

  @Test
  void onlyTheMessagesBelowTheAttemptLimitAreDue() throws Exception {
    processor.sweep();

    verify(repository).findDue(NOW, 3, 20);
  }

  @Test
  void theInboxIsPurgedAtMostOnceAnHour() throws Exception {
    processor.sweep();
    processor.sweep();

    verify(repository).purge(NOW.minus(Duration.ofDays(30)));
  }

  @Test
  void aSweepNeverThrowsAndANodeThatDoesNotLaunchLeavesTheInboxAlone() throws Exception {
    given(repository.findDue(any(), anyInt(), anyInt())).willThrow(new AWException("database down"));
    processor.sweep();

    WebhookProcessor disabled = new WebhookProcessor(repository, taskDAO, Duration.ofSeconds(10),
      Duration.ofDays(30), () -> false, Clock.fixed(NOW, ZoneOffset.UTC));
    disabled.sweep();

    verify(repository).findDue(any(), anyInt(), anyInt());
  }

  @Test
  void aStoppedSchedulerLeavesTheMessagesInTheInboxUntilItRunsAgain() throws Exception {
    // A claimed message would be launched into a scheduler in standby and run only when it starts again
    AtomicBoolean running = new AtomicBoolean(false);
    WebhookProcessor stopped = new WebhookProcessor(repository, taskDAO, Duration.ofSeconds(10), Duration.ofDays(30),
      running::get, Clock.fixed(NOW, ZoneOffset.UTC));
    whenDue(message(0, "loaded"));

    stopped.sweep();
    verifyNoInteractions(taskDAO);
    verify(repository, never()).claim(any(), any());

    running.set(true);
    stopped.sweep();
    verify(taskDAO).launchWebhook(eq(7), eq("market"), any(JsonNode.class));
  }
}
