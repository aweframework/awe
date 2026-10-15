package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link TaskImmediateLauncher}: every launch goes through the supplied {@link TaskSubmitter}.
 */
@ExtendWith(MockitoExtension.class)
class TaskImmediateLauncherTest {

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private TaskExecutionRepository executionRepository;

  @Mock
  private ServiceConfig serviceConfig;

  private TaskImmediateLauncher launcher;

  private final List<Task> submitted = new ArrayList<>();

  private final TaskSubmitter submitter = submitted::add;

  @BeforeEach
  void initLauncher() {
    launcher = new TaskImmediateLauncher(taskRepository, executionRepository, new TaskParameterOverrides(key -> null),
      serviceConfig);
  }

  @Test
  void launchNowSubmitsAManualTaskWithTheOperatorValues() throws Exception {
    given(taskRepository.loadTask(5)).willReturn(task(5, parameter("mode", ParameterConstants.VALUE, "smoke")));

    launcher.launchNow(5, "admin", Map.of("mode", "OPERATOR"), submitter);

    assertEquals(1, submitted.size());
    Task launched = submitted.get(0);
    assertEquals("admin", launched.getLauncher());
    assertEquals(MANUAL_GROUP, launched.getTrigger().getKey().getGroup());
    assertEquals(launched.getTrigger().getKey().getName(), launched.getJob().getKey().getName());
    assertEquals(launched.getTrigger().getKey().getGroup(), launched.getJob().getKey().getGroup());
    assertEquals("OPERATOR", launched.getParameterList().get(0).getValue());
  }

  @Test
  void launchDependencyOnlyOverridesVariableParameters() throws Exception {
    given(taskRepository.loadTask(5)).willReturn(task(5,
      parameter("mode", ParameterConstants.VALUE, "smoke"),
      parameter("date", ParameterConstants.VARIABLE, "STORED")));
    TaskExecution parent = new TaskExecution().setTaskId(2).setExecutionId(20);

    launcher.launchDependency(5, parent, Map.of("mode", "PARENT", "date", "2026-10-15"), submitter);

    Task launched = submitted.get(0);
    assertEquals("#2", launched.getLauncher());
    assertSame(parent, launched.getParentExecution());
    assertEquals(DEPENDENCY_GROUP, launched.getTrigger().getKey().getGroup());
    assertEquals("smoke", launched.getParameterList().get(0).getValue());
    assertEquals("2026-10-15", launched.getParameterList().get(1).getValue());
  }

  @Test
  void submitterFailuresAreWrappedWithTheTaskIdentifier() throws Exception {
    given(taskRepository.loadTask(5)).willReturn(task(5));
    AWException cause = new AWException("scheduler down");

    AWException exception = assertThrows(AWException.class, () -> launcher.launchNow(5, "admin", null, task -> {
      throw cause;
    }));

    assertEquals("Error launching immediate task: 5", exception.getMessage());
    assertSame(cause, exception.getCause());
  }

  @Test
  void finishedTaskLaunchesEveryDependencyThroughTheSubmitter() throws Exception {
    given(taskRepository.loadTask(5)).willReturn(task(5, parameter("date", ParameterConstants.VARIABLE, "STORED")));
    given(taskRepository.loadTask(6)).willReturn(task(6));
    Task parent = new Task().setTaskId(2)
      .setParameterList(List.of(parameter("date", ParameterConstants.VALUE, "2026-10-15")))
      .setDependencyList(List.of(new TaskDependency().setTaskId(5), new TaskDependency().setTaskId(6)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20)
      .setStatus(TaskStatus.JOB_OK.getValue());

    launcher.onFinishTask(parent, execution, submitter);

    assertEquals(2, submitted.size());
    assertEquals("2026-10-15", submitted.get(0).getParameterList().get(0).getValue());
    assertSame(execution, submitted.get(1).getParentExecution());
    verifyNoInteractions(executionRepository);
  }

  @Test
  void failedDependencyWarnsItsParentAndSkipsDependenciesWhenNotAllowed() throws Exception {
    given(taskRepository.loadTask(2)).willReturn(task(2).setSetTaskOnWarningIfDependencyError(true));
    given(serviceConfig.getLocale("WARNING_MESSAGE_TASK_DEPENDENCY_ERROR", "5", "21")).willReturn("dependency failed");
    TaskExecution parentExecution = new TaskExecution().setTaskId(2).setExecutionId(20);
    TaskExecution execution = new TaskExecution().setTaskId(5).setExecutionId(21)
      .setStatus(TaskStatus.JOB_ERROR.getValue()).setParentExecution(parentExecution);
    Task finished = new Task().setTaskId(5).setLaunchDependenciesOnError(false)
      .setDependencyList(List.of(new TaskDependency().setTaskId(9)));

    launcher.onFinishTask(finished, execution, submitter);

    verify(executionRepository).changeStatus(parentExecution, TaskStatus.JOB_WARNING, "dependency failed");
    verify(taskRepository, never()).loadTask(9);
    assertTrue(submitted.isEmpty());
  }

  @Test
  void parentStatusFailuresAreWrapped() throws Exception {
    AWException cause = new AWException("no parent");
    given(taskRepository.loadTask(2)).willThrow(cause);
    TaskExecution execution = new TaskExecution().setTaskId(5).setExecutionId(21)
      .setStatus(TaskStatus.JOB_OK.getValue()).setParentExecution(new TaskExecution().setTaskId(2));

    AWException exception = assertThrows(AWException.class,
      () -> launcher.onFinishTask(new Task(), execution, submitter));

    assertEquals("Error setting task status", exception.getMessage());
    assertSame(cause, exception.getCause());
    verify(executionRepository, never()).changeStatus(any(), any(), any());
  }

  @Test
  void unresolvedPropertiesTakeTheOperatorValuesIntoAccount() throws Exception {
    given(taskRepository.loadTask(5))
      .willReturn(task(5, parameter("region", ParameterConstants.PROPERTY, "stored.key")));

    assertEquals(List.of("operator.key"), launcher.findUnresolvedProperties(5, Map.of("region", "operator.key")));
  }

  private Task task(int taskId, TaskParameter... parameters) {
    return new Task()
      .setTaskId(taskId)
      .setParameterList(List.of(parameters))
      .setJob(JobBuilder.newJob(NoOpJob.class).withIdentity(String.valueOf(taskId)).build());
  }

  private TaskParameter parameter(String name, int source, String value) {
    return new TaskParameter().setName(name).setSource(String.valueOf(source)).setValue(value);
  }

  /**
   * Job doing nothing, only needed to build job details
   */
  public static class NoOpJob implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      // Nothing to do
    }
  }
}
