package com.almis.awe.scheduler.autoconfigure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * S12a: an application that uses a remote scheduler with the file execution log store logs one WARN at
 * startup, because the activity log viewer reads the application's own store.
 */
class ExecutionLogStoreRemoteWarningTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ExecutionLogStoreConfig.class))
    .withBean(QueryService.class, () -> mock(QueryService.class))
    .withBean(MaintainService.class, () -> mock(MaintainService.class))
    .withBean(QueryUtil.class, () -> mock(QueryUtil.class));

  private final Logger logger = (Logger) LoggerFactory.getLogger(ExecutionLogStoreConfig.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @BeforeEach
  void attachAppender() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
  }

  @Test
  void warnsOnceWhenTheApplicationUsesARemoteSchedulerWithTheFileStore() {
    contextRunner
      .withPropertyValues("awe.scheduler.remote-enabled=true", "awe.scheduler.execution-log-store=file")
      .run(context -> assertThat(context).hasNotFailed());

    List<ILoggingEvent> warnings = warnings();
    assertThat(warnings).hasSize(1);
    assertThat(warnings.getFirst().getFormattedMessage())
      .contains("activity log viewer reads this application's own store")
      .contains("remote scheduler will not show their logs")
      .contains("awe.scheduler.execution-log-store=database")
      .contains("both instances")
      .contains("awe.scheduler.remote-callback-user");
  }

  @Test
  void doesNotWarnWhenTheRemoteSchedulerIsDisabled() {
    contextRunner
      .withPropertyValues("awe.scheduler.remote-enabled=false", "awe.scheduler.execution-log-store=file")
      .run(context -> assertThat(context).hasNotFailed());

    assertThat(warnings()).isEmpty();
  }

  @Test
  void doesNotWarnWhenTheStoreIsDatabase() {
    contextRunner
      .withPropertyValues("awe.scheduler.remote-enabled=true", "awe.scheduler.execution-log-store=database")
      .run(context -> assertThat(context).hasNotFailed());

    assertThat(warnings()).isEmpty();
  }

  @Test
  void doesNotWarnOnTheSchedulerInstance() {
    contextRunner
      .withPropertyValues("awe.scheduler.remote-enabled=true", "awe.scheduler.scheduler-instance=true",
        "awe.scheduler.execution-log-store=file")
      .run(context -> assertThat(context).hasNotFailed());

    assertThat(warnings()).isEmpty();
  }

  private List<ILoggingEvent> warnings() {
    return appender.list.stream().filter(event -> Level.WARN.equals(event.getLevel())).toList();
  }
}
