package com.almis.awe.scheduler.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.CommandDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.scheduler.service.scheduled.CommandJobService;
import com.almis.awe.scheduler.service.scheduled.JobService;
import com.almis.awe.service.MaintainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.quartz.JobDataMap;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.almis.awe.scheduler.constant.TaskConstants.LOG_BY_TASK_EXECUTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Parameter handling of command tasks.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommandJobServiceTest {

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
  private CommandDAO commandDAO;
  @Mock
  private ExecutionLogStore executionLogStore;
  @Mock
  private AweElements aweElements;
  @Mock
  private ApplicationContext context;

  /**
   * Build the service under test wired to the mocked collaborators.
   *
   * @return Service under test
   */
  private CommandJobService service() {
    CommandJobService service = new CommandJobService(executionService, maintainService, queryUtil, taskDAO,
      eventPublisher, commandDAO, executionLogStore, Duration.ofSeconds(5));
    service.setApplicationContext(context);
    doReturn(aweElements).when(context).getBean(AweElements.class);
    return service;
  }

  /**
   * Build a command task carrying the given parameters.
   *
   * @param parameters Task parameters
   * @return Command task
   */
  private Task commandTask(TaskParameter... parameters) {
    Trigger trigger = mock(Trigger.class);
    when(trigger.getKey()).thenReturn(new TriggerKey("DummyTrigger"));
    return new Task()
      .setTaskId(1)
      .setAction("script.sh")
      .setTrigger(trigger)
      .setParameterList(new ArrayList<>(Arrays.asList(parameters)));
  }

  /**
   * Build a parameter with the given source.
   *
   * @param name   Parameter name
   * @param value  Stored value
   * @param source Parameter source
   * @return Task parameter
   */
  private TaskParameter parameter(String name, String value, Integer source) {
    return new TaskParameter().setName(name).setValue(value).setSource(String.valueOf(source));
  }

  /**
   * Run the given task and return the environment array handed to the command.
   *
   * @param task Task to run
   * @return Environment array received by CommandDAO
   */
  private String[] runAndCaptureEnvironment(Task task) throws Exception {
    given(commandDAO.runCommand(any(Task.class), any(String[].class), anyLong())).willReturn(0);

    service().executeJob(task, new TaskExecution().setTaskId(1).setExecutionId(1), new JobDataMap()).get();

    ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
    verify(commandDAO).runCommand(any(Task.class), captor.capture(), anyLong());
    return captor.getValue();
  }

  /**
   * A PROPERTY parameter must reach the command as its resolved property value, the same way
   * MaintainJobService resolves it, instead of the stored property key.
   */
  @Test
  void propertyParametersAreResolvedBeforeRunningTheCommand() throws Exception {
    given(aweElements.getProperty("my.region")).willReturn("EU");
    Task task = commandTask(parameter("region", "my.region", ParameterConstants.PROPERTY));

    String[] environment = runAndCaptureEnvironment(task);

    assertEquals(List.of("EU"), Arrays.asList(environment));
    assertEquals("EU", task.getParameterList().get(0).getValue(),
      "The resolved value must also be visible to the executor building the command line");
  }

  /**
   * VALUE and VARIABLE parameters are passed through untouched, in their declared order.
   */
  @Test
  void valueAndVariableParametersArePassedThroughUnchanged() throws Exception {
    Task task = commandTask(
      parameter("first", "literal", ParameterConstants.VALUE),
      parameter("second", "operatorSupplied", ParameterConstants.VARIABLE));

    String[] environment = runAndCaptureEnvironment(task);

    assertEquals(List.of("literal", "operatorSupplied"), Arrays.asList(environment));
  }

  /**
   * A task mixing the three sources resolves only the PROPERTY one and keeps the declared order.
   */
  @Test
  void mixedSourcesResolveOnlyPropertyParametersKeepingOrder() throws Exception {
    given(aweElements.getProperty("my.region")).willReturn("EU");
    Task task = commandTask(
      parameter("mode", "smoke", ParameterConstants.VALUE),
      parameter("region", "my.region", ParameterConstants.PROPERTY),
      parameter("date", "2026-01-31", ParameterConstants.VARIABLE));

    String[] environment = runAndCaptureEnvironment(task);

    assertEquals(List.of("smoke", "EU", "2026-01-31"), Arrays.asList(environment));
  }

  /**
   * A task without parameters runs with an empty environment.
   */
  @Test
  void taskWithoutParametersRunsWithEmptyEnvironment() throws Exception {
    assertEquals(0, runAndCaptureEnvironment(commandTask()).length);
  }

  /**
   * D1 corrective / pre-existing MDC leak fix: an unchecked exception from the job body must still
   * run {@code endLogging} - MDC cleared, store completion signalled - before propagating, instead
   * of leaking the MDC onto the pooled job thread.
   *
   * <p>Also pins the C1 gate fix: {@code result} must stay {@code null} until an outcome from
   * {@code execute(...)} is actually known, so a crashed command renders the abnormal-completion
   * summary line instead of a misleading "finished with result OK" one (the default
   * {@link com.almis.awe.model.dto.ServiceData} answer type).</p>
   */
  @Test
  void unexpectedExceptionDuringCommandExecutionStillRunsEndLoggingAndPropagates() {
    given(commandDAO.runCommand(any(Task.class), any(String[].class), anyLong()))
      .willThrow(new IllegalStateException("boom"));
    Task task = commandTask();
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(1).setInitialDate(new Date());

    CommandJobService service = service();
    given(aweElements.getLocaleWithLanguage(anyString(), any(), any(Object[].class)))
      .willAnswer(invocation -> {
        Object[] tokens = Arrays.copyOfRange(invocation.getArguments(), 2, invocation.getArguments().length);
        return invocation.getArgument(0) + "|" + Arrays.toString(tokens);
      });

    ListAppender<ILoggingEvent> logAppender = attachListAppender();
    try {
      assertThrows(IllegalStateException.class, () -> service.executeJob(task, execution, new JobDataMap()));
    } finally {
      detachListAppender(logAppender);
    }

    verify(executionLogStore).complete(any(), any());
    assertNull(MDC.get(LOG_BY_TASK_EXECUTION));

    ILoggingEvent summaryEvent = logAppender.list.stream()
      .filter(event -> event.getLevel() == Level.INFO)
      .findFirst()
      .orElseThrow(() -> new AssertionError("No summary line was logged"));
    assertTrue(summaryEvent.getFormattedMessage().startsWith("SCHEDULER_EXECUTION_LOG_COMPLETED_ABNORMALLY|"),
      summaryEvent.getFormattedMessage());
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
}
