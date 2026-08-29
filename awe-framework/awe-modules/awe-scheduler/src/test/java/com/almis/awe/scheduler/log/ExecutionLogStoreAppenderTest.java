package com.almis.awe.scheduler.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import java.nio.charset.StandardCharsets;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ExecutionLogStoreAppender}: the same MDC-keyed admission rule as the file-mode
 * {@code SCHEDULER_EXECUTION} sifting appender (reused evaluator/filter, not re-implemented),
 * origin-tagging via the {@code executionLogOrigin} MDC key (defaulting to {@code SCHEDULER} when
 * absent, per D4), event-timestamp propagation, physical-line splitting for multi-line events, and
 * the defensive per-line character cap.
 */
@ExtendWith(MockitoExtension.class)
class ExecutionLogStoreAppenderTest {

  private static final int MAX_LINE_LENGTH = 20;

  @Mock
  private ExecutionLogStore store;

  private LoggerContext loggerContext;
  private PatternLayoutEncoder encoder;
  private ExecutionLogStoreAppender appender;

  @BeforeEach
  void setUp() {
    loggerContext = new LoggerContext();
    loggerContext.start();

    encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%msg");
    encoder.setCharset(StandardCharsets.UTF_8);
    encoder.start();

    appender = new ExecutionLogStoreAppender(encoder, store, MAX_LINE_LENGTH);
    appender.setContext(loggerContext);
    appender.start();
  }

  @AfterEach
  void tearDown() {
    appender.stop();
    encoder.stop();
    loggerContext.stop();
  }

  @Test
  void mdcAbsentIssuesNoAppendCall() {
    LoggingEvent event = loggingEvent("a line", null, null);

    appender.doAppend(event);

    verifyNoInteractions(store);
  }

  @Test
  void malformedMdcKeyIssuesNoAppendCall() {
    LoggingEvent event = loggingEvent("a line", "not-a-valid-key", null);

    appender.doAppend(event);

    verifyNoInteractions(store);
  }

  @Test
  void evaluatorFilterDeniesNonExecutionEventsExactlyLikeTheSiftingAppenderDoesToday() {
    LoggingEvent event = loggingEvent("unrelated log line", null, null);

    appender.doAppend(event);

    verifyNoInteractions(store);
  }

  @Test
  void matchingExecutionEventWithNoOriginMdcKeyAppendsTheRenderedLineWithTheSchedulerOriginAndTheEventTimestamp() {
    LoggingEvent event = loggingEvent("hello world", "12-34", null);

    appender.doAppend(event);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(12, 34), ExecutionLogOrigin.SCHEDULER, event.getTimeStamp(), "hello world")));
  }

  @Test
  void matchingExecutionEventWithTheApplicationOriginMdcKeyAppendsTheApplicationOrigin() {
    LoggingEvent event = loggingEvent("hello world", "12-34", ExecutionLogOrigin.APPLICATION.code());

    appender.doAppend(event);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(12, 34), ExecutionLogOrigin.APPLICATION, event.getTimeStamp(), "hello world")));
  }

  @Test
  void anUnrecognizedOriginMdcValueDefaultsToScheduler() {
    LoggingEvent event = loggingEvent("hello world", "12-34", "not-a-real-origin");

    appender.doAppend(event);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(12, 34), ExecutionLogOrigin.SCHEDULER, event.getTimeStamp(), "hello world")));
  }

  @Test
  void multiLineEventProducesOneAppendCallPerPhysicalLine() {
    LoggingEvent event = loggingEvent("first line\nsecond line\nthird line", "1-1", null);

    appender.doAppend(event);

    ArgumentCaptor<ExecutionLogLine> captor = ArgumentCaptor.forClass(ExecutionLogLine.class);
    verify(store, times(3)).append(captor.capture());
    List<ExecutionLogLine> capturedLines = captor.getAllValues();
    assertEquals(List.of("first line", "second line", "third line"), capturedLines.stream().map(ExecutionLogLine::text).toList());
    assertTrue(capturedLines.stream().allMatch(capturedLine -> capturedLine.key().equals(new ExecutionKey(1, 1))
      && capturedLine.origin() == ExecutionLogOrigin.SCHEDULER
      && capturedLine.timestampMillis() == event.getTimeStamp()));
  }

  @Test
  void patternLayoutEncoderRendersTheSameAsTheFileModePattern() {
    PatternLayoutEncoder customEncoder = new PatternLayoutEncoder();
    customEncoder.setContext(loggerContext);
    customEncoder.setPattern("[%level] %msg");
    customEncoder.setCharset(StandardCharsets.UTF_8);
    customEncoder.start();
    ExecutionLogStoreAppender customAppender = new ExecutionLogStoreAppender(customEncoder, store, MAX_LINE_LENGTH);
    customAppender.setContext(loggerContext);
    customAppender.start();

    LoggingEvent event = loggingEvent("short", "5-6", Level.WARN, null);

    customAppender.doAppend(event);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(5, 6), ExecutionLogOrigin.SCHEDULER, event.getTimeStamp(), "[WARN] short")));
    customAppender.stop();
    customEncoder.stop();
  }

  @Test
  void lineExceedingTheMaxLengthIsCappedBeforeReachingStoreAppendWithAnEllipsisMarker() {
    String longLine = "a".repeat(MAX_LINE_LENGTH + 15);
    LoggingEvent event = loggingEvent(longLine, "2-9", null);

    appender.doAppend(event);

    ArgumentCaptor<ExecutionLogLine> captor = ArgumentCaptor.forClass(ExecutionLogLine.class);
    verify(store).append(captor.capture());
    String captured = captor.getValue().text();
    assertEquals(MAX_LINE_LENGTH, captured.length());
    assertTrue(captured.endsWith("…"), "Capped line must end with an ellipsis marker: " + captured);
    assertEquals(longLine.substring(0, MAX_LINE_LENGTH - 1), captured.substring(0, MAX_LINE_LENGTH - 1));
  }

  @Test
  void lineWithinTheMaxLengthIsNotAltered() {
    String shortLine = "well within the cap";
    LoggingEvent event = loggingEvent(shortLine, "3-3", null);

    appender.doAppend(event);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(3, 3), ExecutionLogOrigin.SCHEDULER, event.getTimeStamp(), shortLine)));
  }

  /**
   * A cap boundary landing in the middle of a surrogate pair must back off by one character
   * instead of emitting a lone high surrogate, which would corrupt the stored/rendered text; the
   * ellipsis marker is then appended after the back-off point.
   */
  @Test
  void capNeverSplitsASurrogatePair() {
    // U+1F600 (grinning face) is a surrogate pair; place it so the ellipsis cut boundary
    // (maxLength - 1) would otherwise land in the middle of it.
    String emoji = "😀";
    String padding = "x".repeat(MAX_LINE_LENGTH - 2);
    String line = padding + emoji + "z"; // forces truncation while the pair spans the cut boundary
    LoggingEvent event = loggingEvent(line, "4-4", null);

    appender.doAppend(event);

    ArgumentCaptor<ExecutionLogLine> captor = ArgumentCaptor.forClass(ExecutionLogLine.class);
    verify(store).append(captor.capture());
    String captured = captor.getValue().text();
    assertEquals(padding + "…", captured);
    assertTrue(captured.chars().noneMatch(codeUnit -> Character.isSurrogate((char) codeUnit)));
  }

  private LoggingEvent loggingEvent(String message, String mdcExecutionKey, String mdcOrigin) {
    return loggingEvent(message, mdcExecutionKey, Level.INFO, mdcOrigin);
  }

  private LoggingEvent loggingEvent(String message, String mdcExecutionKey, Level level, String mdcOrigin) {
    LoggingEvent event = new LoggingEvent();
    event.setLoggerName("com.almis.awe.scheduler.test");
    event.setLevel(level);
    event.setMessage(message);
    event.setTimeStamp(System.currentTimeMillis());
    Map<String, String> mdc = new HashMap<>();
    if (mdcExecutionKey != null) {
      mdc.put(TaskConstants.LOG_BY_TASK_EXECUTION, mdcExecutionKey);
    }
    if (mdcOrigin != null) {
      mdc.put(TaskConstants.EXECUTION_LOG_ORIGIN, mdcOrigin);
    }
    event.setMDCPropertyMap(mdc);
    return event;
  }
}
