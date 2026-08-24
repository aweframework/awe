package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.CommandDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.scheduled.CommandJobService;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
      eventPublisher, commandDAO, Duration.ofSeconds(5));
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
}
