package com.almis.awe.scheduler.executor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.scheduler.bean.task.Task;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.TriggerBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class used for testing CommandStreamLogger, extracted verbatim from CommandDAO's
 * former private logHandler method so both LocalCommandExecutor and (future)
 * SshCommandExecutor emit stdout/stderr lines in the exact same format.
 */
class CommandStreamLoggerTest {

  private final CommandStreamLogger commandStreamLogger = new CommandStreamLogger();
  private ListAppender<ILoggingEvent> listAppender;
  private Logger logbackLogger;

  @BeforeEach
  void setUp() {
    logbackLogger = (Logger) LoggerFactory.getLogger(CommandStreamLogger.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logbackLogger.addAppender(listAppender);
  }

  @AfterEach
  void tearDown() {
    logbackLogger.detachAppender(listAppender);
  }

  @Test
  void logsEachLineWithTriggerKeyAndType() {
    Task task = generateTask();
    InputStream inputStream = IOUtils.toInputStream("line one\nline two", StandardCharsets.UTF_8);

    commandStreamLogger.log(task, inputStream, "OUTPUT");

    assertEquals(2, listAppender.list.size());
    assertEquals("[" + task.getTrigger().getKey() + "] [OUTPUT] line one", listAppender.list.get(0).getFormattedMessage());
    assertEquals("[" + task.getTrigger().getKey() + "] [OUTPUT] line two", listAppender.list.get(1).getFormattedMessage());
  }

  @Test
  void logsNothingForEmptyStream() {
    Task task = generateTask();
    InputStream inputStream = IOUtils.toInputStream("", StandardCharsets.UTF_8);

    commandStreamLogger.log(task, inputStream, "ERROR");

    assertTrue(listAppender.list.isEmpty());
  }

  @Test
  void doesNotPropagateExceptionWhenStreamFails() {
    Task task = generateTask();
    InputStream brokenStream = new InputStream() {
      @Override
      public int read() throws IOException {
        throw new IOException("broken stream");
      }
    };

    commandStreamLogger.log(task, brokenStream, "ERROR");

    assertTrue(listAppender.list.stream()
      .anyMatch(event -> event.getFormattedMessage().contains("Failed to collect the command output")));
  }

  private Task generateTask() {
    Task task = new Task();
    task.setTrigger(TriggerBuilder.newTrigger().build());
    return task;
  }
}
