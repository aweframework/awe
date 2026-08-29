package com.almis.awe.scheduler.service.scheduled;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.constant.TaskConstants;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.enums.ExecutionLogOrigin;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.ExecutionService;
import com.almis.awe.service.MaintainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.quartz.JobDataMap;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link JobService}'s execution-log lifecycle hooks: {@code startLogging} sets the MDC
 * discriminator, {@code endLogging} clears it and signals completion to the active
 * {@link ExecutionLogStore} exactly once, after the MDC is already cleared.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobServiceTest {

  @Mock
  private ExecutionService executionService;
  @Mock
  private MaintainService maintainService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private TaskDAO taskDAO;
  @Mock
  private ApplicationEventPublisher eventPublisher;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private AweElements aweElements;
  @Mock
  private ApplicationContext context;

  private TestJobService service() {
    TestJobService jobService = new TestJobService(executionService, maintainService, queryUtil, taskDAO,
      eventPublisher, executionLogStore, Duration.ofSeconds(5));
    jobService.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    return jobService;
  }

  @Test
  void startLoggingSetsTheMdcDiscriminatorToTheExecutionKey() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(11).setExecutionId(22);

    jobService.callStartLogging(execution);

    assertEquals("11-22", MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
    MDC.clear();
  }

  @Test
  void endLoggingClearsMdcBeforeSignallingCompletion() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(3).setExecutionId(4);
    MDC.put(TaskConstants.LOG_BY_TASK_EXECUTION, execution.getKey());
    MDC.put(TaskConstants.EXECUTION, execution.getKey());

    doAnswer(invocation -> {
      assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION), "MDC must be cleared before complete() is called");
      assertNull(MDC.get(TaskConstants.EXECUTION), "MDC must be cleared before complete() is called");
      return null;
    }).when(executionLogStore).complete(eq(new ExecutionKey(3, 4)), eq(ExecutionLogOrigin.SCHEDULER));

    jobService.callEndLogging(execution);

    verify(executionLogStore).complete(eq(new ExecutionKey(3, 4)), eq(ExecutionLogOrigin.SCHEDULER));
    assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
    assertNull(MDC.get(TaskConstants.EXECUTION));
  }

  @Test
  void endLoggingSignalsCompletionExactlyOncePerExecution() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(5).setExecutionId(6);

    jobService.callEndLogging(execution);

    verify(executionLogStore).complete(eq(new ExecutionKey(5, 6)), eq(ExecutionLogOrigin.SCHEDULER));
  }

  @Test
  void endLoggingSkipsStoreCompleteWhenTaskIdIsAbsent() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(null).setExecutionId(6);

    assertDoesNotThrow(() -> jobService.callEndLogging(execution));

    verifyNoInteractions(executionLogStore);
  }

  @Test
  void endLoggingSkipsStoreCompleteWhenExecutionIdIsAbsent() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(5).setExecutionId(null);

    assertDoesNotThrow(() -> jobService.callEndLogging(execution));

    verifyNoInteractions(executionLogStore);
  }

  /**
   * Pins the pre-existing leak fix at its narrowest point: {@code endLogging} already removes the
   * MDC before calling {@code store.complete}, so even a misbehaving store implementation that
   * throws leaves no MDC residue on the pooled job thread.
   */
  @Test
  void endLoggingRemovesMdcEvenWhenStoreCompleteThrows() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(7).setExecutionId(8);
    MDC.put(TaskConstants.LOG_BY_TASK_EXECUTION, execution.getKey());
    MDC.put(TaskConstants.EXECUTION, execution.getKey());
    doThrow(new RuntimeException("store unavailable")).when(executionLogStore).complete(eq(new ExecutionKey(7, 8)), eq(ExecutionLogOrigin.SCHEDULER));

    assertThrows(RuntimeException.class, () -> jobService.callEndLogging(execution));

    assertNull(MDC.get(TaskConstants.LOG_BY_TASK_EXECUTION));
    assertNull(MDC.get(TaskConstants.EXECUTION));
    MDC.clear();
  }

  private ListAppender<ILoggingEvent> attachListAppender() {
    Logger logger = (Logger) LoggerFactory.getLogger(JobService.class);
    ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    logAppender.start();
    logger.addAppender(logAppender);
    return logAppender;
  }

  private void detachListAppender(ListAppender<ILoggingEvent> logAppender) {
    Logger logger = (Logger) LoggerFactory.getLogger(JobService.class);
    logger.detachAppender(logAppender);
  }

  @Test
  void logExecutionSummaryLogsOneInfoLineInsideTheMdcForANormalOkResult() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(11).setExecutionId(22)
      .setInitialDate(new Date(System.currentTimeMillis() - 1500));
    ServiceData result = new ServiceData().setType(AnswerType.OK).setMessage("All good");

    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      jobService.callStartLogging(execution);
      jobService.callLogExecutionSummary(execution, result);
    } finally {
      detachListAppender(logAppender);
      MDC.clear();
    }

    assertEquals(1, logAppender.list.size());
    ILoggingEvent event = logAppender.list.get(0);
    assertEquals(Level.INFO, event.getLevel());
    assertEquals("11-22", event.getMDCPropertyMap().get(TaskConstants.LOG_BY_TASK_EXECUTION));
    String formatted = event.getFormattedMessage();
    assertTrue(formatted.startsWith("SCHEDULER_EXECUTION_LOG_COMPLETED|"), formatted);
    assertTrue(formatted.contains("ok"), formatted);
    assertTrue(formatted.contains("1s"), "elapsed must be rendered and non-negative: " + formatted);
    assertTrue(formatted.contains("All good"), formatted);
  }

  @Test
  void logExecutionSummaryLogsOneInfoLineInsideTheMdcForAWarningOrErrorResult() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(11).setExecutionId(23)
      .setInitialDate(new Date(System.currentTimeMillis() - 1500));
    ServiceData result = new ServiceData().setType(AnswerType.ERROR).setMessage("boom");

    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      jobService.callStartLogging(execution);
      jobService.callLogExecutionSummary(execution, result);
    } finally {
      detachListAppender(logAppender);
      MDC.clear();
    }

    assertEquals(1, logAppender.list.size());
    ILoggingEvent event = logAppender.list.get(0);
    assertEquals(Level.INFO, event.getLevel());
    assertEquals("11-23", event.getMDCPropertyMap().get(TaskConstants.LOG_BY_TASK_EXECUTION));
    String formatted = event.getFormattedMessage();
    assertTrue(formatted.startsWith("SCHEDULER_EXECUTION_LOG_COMPLETED|"), formatted);
    assertTrue(formatted.contains("error"), formatted);
    assertTrue(formatted.contains("boom"), formatted);
  }

  /**
   * D1 corrective: the unexpected-exception path (only {@code AWException} is caught by either job
   * service today) leaves {@code result} null/unassigned. The hook must never dereference it and
   * must instead render a distinct, locale'd "finished abnormally" line.
   */
  @Test
  void logExecutionSummaryRendersAnAbnormalCompletionLineWhenResultIsNull() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(9).setExecutionId(1)
      .setInitialDate(new Date(System.currentTimeMillis() - 100));

    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      jobService.callStartLogging(execution);
      assertDoesNotThrow(() -> jobService.callLogExecutionSummary(execution, null));
    } finally {
      detachListAppender(logAppender);
      MDC.clear();
    }

    assertEquals(1, logAppender.list.size());
    ILoggingEvent event = logAppender.list.get(0);
    assertEquals(Level.INFO, event.getLevel());
    assertEquals("9-1", event.getMDCPropertyMap().get(TaskConstants.LOG_BY_TASK_EXECUTION));
    assertTrue(event.getFormattedMessage().startsWith("SCHEDULER_EXECUTION_LOG_COMPLETED_ABNORMALLY|"),
      event.getFormattedMessage());
  }

  /**
   * Elapsed-time computation can itself throw (e.g. a null {@code initialDate}); the hook must
   * never let that escape as an NPE. No line is emitted in that case rather than a malformed one.
   */
  @Test
  void logExecutionSummaryNeverThrowsWhenElapsedComputationFails() {
    TestJobService jobService = service();
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(1).setInitialDate(null);
    ServiceData result = new ServiceData().setType(AnswerType.OK).setMessage("ok");

    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      assertDoesNotThrow(() -> jobService.callLogExecutionSummary(execution, result));
    } finally {
      detachListAppender(logAppender);
      MDC.clear();
    }

    assertTrue(logAppender.list.stream().noneMatch(event -> event.getLevel() == Level.INFO));
  }

  /**
   * Minimal concrete {@link JobService} exposing the package-private logging hooks for direct
   * testing, without going through a real job's {@code executeJob} chain.
   */
  private static final class TestJobService extends JobService {

    TestJobService(ExecutionService executionService, MaintainService maintainService, QueryUtil queryUtil,
                   TaskDAO taskDAO, ApplicationEventPublisher eventPublisher, ExecutionLogStore executionLogStore,
                   Duration defaultTimeout) {
      super(executionService, maintainService, queryUtil, taskDAO, eventPublisher, executionLogStore, defaultTimeout);
    }

    void callStartLogging(TaskExecution execution) {
      startLogging(execution);
    }

    void callEndLogging(TaskExecution execution) {
      endLogging(execution);
    }

    void callLogExecutionSummary(TaskExecution execution, ServiceData result) {
      logExecutionSummary(execution, result);
    }

    /**
     * Echoes the locale key and its parameters instead of resolving real locale files, so
     * assertions can pin exactly what {@code logExecutionSummary} renders.
     */
    @Override
    public String getLocale(String locale, String... parameters) {
      return locale + "|" + Arrays.toString(parameters);
    }

    @Override
    public Future<ServiceData> executeJob(Task task, TaskExecution execution, JobDataMap dataMap) {
      throw new UnsupportedOperationException("Not needed for this test");
    }
  }
}
