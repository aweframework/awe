package com.almis.awe.scheduler.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.nio.charset.StandardCharsets;

/**
 * Installs the database-mode {@link ExecutionLogStoreAppender} onto the root logger at runtime
 * (ADR-1) and manages its lifecycle. Left out of {@code scheduler-log.xml} deliberately: the
 * shipped Logback include stays untouched (zero regression risk for file mode), the appender is
 * only ever created when {@code awe.scheduler.execution-log-store=database} selects this bean,
 * and no static holder is needed to bridge the Logback-instantiated appender to the Spring-managed
 * writer -- this installer owns the appender and injects the store directly through the
 * constructor.
 */
public class ExecutionLogAppenderInstaller implements SmartLifecycle {

  private final ExecutionLogStore store;
  private final ExecutionLogWriter writer;
  private final String pattern;
  private final int maxLineLength;
  private final LoggerContext loggerContext;

  private volatile boolean running;
  private ExecutionLogStoreAppender appender;
  private PatternLayoutEncoder encoder;

  /**
   * Constructor
   *
   * @param store         Execution log store the appender feeds
   * @param writer        Execution log writer, stopped (drained) when this installer stops
   * @param pattern       Encoder pattern, from {@code awe.scheduler.execution-log-pattern}
   * @param maxLineLength Defensive per-line character cap
   */
  public ExecutionLogAppenderInstaller(ExecutionLogStore store, ExecutionLogWriter writer, String pattern, int maxLineLength) {
    this(store, writer, pattern, maxLineLength, (LoggerContext) LoggerFactory.getILoggerFactory());
  }

  ExecutionLogAppenderInstaller(ExecutionLogStore store, ExecutionLogWriter writer, String pattern, int maxLineLength,
                                 LoggerContext loggerContext) {
    this.store = store;
    this.writer = writer;
    this.pattern = pattern;
    this.maxLineLength = maxLineLength;
    this.loggerContext = loggerContext;
  }

  @Override
  public void start() {
    encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern(pattern);
    // The appender decodes the rendered bytes as UTF-8; the encoder must not fall back to the
    // platform default charset or non-ASCII content is corrupted on non-UTF-8 JVMs.
    encoder.setCharset(StandardCharsets.UTF_8);
    encoder.start();

    appender = new ExecutionLogStoreAppender(encoder, store, maxLineLength);
    appender.setContext(loggerContext);
    appender.start();

    loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(appender);
    running = true;
  }

  @Override
  public void stop() {
    if (!running) {
      return;
    }

    Logger root = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    if (appender != null) {
      root.detachAppender(appender);
      appender.stop();
      appender = null;
    }
    if (encoder != null) {
      encoder.stop();
      encoder = null;
    }
    writer.stop();
    running = false;
  }

  @Override
  public boolean isRunning() {
    return running;
  }
}
