package com.almis.awe.scheduler.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ExecutionLogAppenderInstaller}: attaches the capture appender to the root
 * logger on start, detaches and stops it on stop, then drains the writer -- all against a
 * throwaway {@link LoggerContext} so no test ever touches the real application logging config.
 */
@ExtendWith(MockitoExtension.class)
class ExecutionLogAppenderInstallerTest {

  @Mock
  private ExecutionLogStore store;

  @Mock
  private ExecutionLogWriter writer;

  private LoggerContext loggerContext;
  private ExecutionLogAppenderInstaller installer;

  @BeforeEach
  void setUp() {
    loggerContext = new LoggerContext();
    loggerContext.start();
    installer = new ExecutionLogAppenderInstaller(store, writer, "%msg", 1000, loggerContext);
  }

  @AfterEach
  void tearDown() {
    loggerContext.stop();
  }

  @Test
  void startAttachesTheAppenderToTheRootLoggerAndMarksItRunning() {
    installer.start();

    assertTrue(installer.isRunning());
    Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    List<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>> attached = attachedAppenders(root);
    assertEquals(1, attached.size());
    assertTrue(attached.get(0) instanceof ExecutionLogStoreAppender);
  }

  @Test
  void theInstalledAppenderCapturesNonAsciiContentIntactOnAnyPlatformCharset() {
    installer.start();

    LoggingEvent multiByteEvent = loggingEvent("acentuación … 😀", "1-1");
    Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    root.callAppenders(multiByteEvent);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(1, 1), ExecutionLogOrigin.SCHEDULER, multiByteEvent.getTimeStamp(), "acentuación … 😀")));
  }

  @Test
  void theInstalledAppenderCapturesExecutionEventsAndDeniesNonExecutionEvents() {
    installer.start();

    LoggingEvent executionEvent = loggingEvent("captured line", "1-1");
    LoggingEvent unrelatedEvent = loggingEvent("unrelated line", null);

    Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    root.callAppenders(executionEvent);
    root.callAppenders(unrelatedEvent);

    verify(store).append(eq(new ExecutionLogLine(new ExecutionKey(1, 1), ExecutionLogOrigin.SCHEDULER, executionEvent.getTimeStamp(), "captured line")));
    verify(store, never()).append(eq(new ExecutionLogLine(new ExecutionKey(1, 1), ExecutionLogOrigin.SCHEDULER, executionEvent.getTimeStamp(), "unrelated line")));
  }

  @Test
  void stopDetachesTheAppenderStopsItThenDrainsTheWriter() {
    installer.start();

    installer.stop();

    assertFalse(installer.isRunning());
    Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    assertEquals(0, attachedAppenders(root).size());
    verify(writer).stop();
  }

  @Test
  void stopBeforeStartNeverThrowsAndNeverTouchesTheWriter() {
    installer.stop();

    assertFalse(installer.isRunning());
    verifyNoInteractions(writer);
  }

  private List<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>> attachedAppenders(Logger logger) {
    List<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenders = new java.util.ArrayList<>();
    Iterator<ch.qos.logback.core.Appender<ch.qos.logback.classic.spi.ILoggingEvent>> iterator = logger.iteratorForAppenders();
    iterator.forEachRemaining(appenders::add);
    return appenders;
  }

  private LoggingEvent loggingEvent(String message, String mdcExecutionKey) {
    LoggingEvent event = new LoggingEvent();
    event.setLoggerName("com.almis.awe.scheduler.test");
    event.setLevel(Level.INFO);
    event.setMessage(message);
    event.setTimeStamp(System.currentTimeMillis());
    java.util.Map<String, String> mdc = new java.util.HashMap<>();
    if (mdcExecutionKey != null) {
      mdc.put(TaskConstants.LOG_BY_TASK_EXECUTION, mdcExecutionKey);
    }
    event.setMDCPropertyMap(mdc);
    return event;
  }
}
