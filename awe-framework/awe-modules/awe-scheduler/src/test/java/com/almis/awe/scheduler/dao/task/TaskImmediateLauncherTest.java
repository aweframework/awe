package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.dao.task.FlowRules.ExecutionRow;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.MANUAL_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.WEBHOOK_GROUP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

  @Mock
  private FlowRunRepository flowRuns;

  private TaskImmediateLauncher launcher;

  private final List<Task> submitted = new ArrayList<>();

  private final TaskSubmitter submitter = submitted::add;

  @BeforeEach
  void initLauncher() {
    launcher = new TaskImmediateLauncher(taskRepository, executionRepository, new TaskParameterOverrides(key -> null),
      serviceConfig, flowRuns);
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
    assertEquals(Map.of("mode", "OPERATOR"), launched.getRunParameters());
    assertNull(launched.getRunId());
  }

  @Test
  void launchWebhookMapsTheDataToTheMappedVariablesOnly() throws Exception {
    given(taskRepository.loadTask(5)).willReturn(task(5,
      parameter("mode", ParameterConstants.VALUE, "smoke"),
      parameter("date", ParameterConstants.VARIABLE, "STORED").setPointer("/date"),
      parameter("entity", ParameterConstants.VARIABLE, "STORED")));

    launcher.launchWebhook(5, "market-data",
      JsonMapper.builder().build().readTree("{\"date\":\"2026-11-12\",\"entity\":\"0049\",\"mode\":\"x\"}"),
      submitter);

    Task launched = submitted.get(0);
    assertEquals("webhook:market-data", launched.getLauncher());
    assertEquals(WEBHOOK_GROUP, launched.getTrigger().getKey().getGroup());
    assertEquals("smoke", launched.getParameterList().get(0).getValue());
    assertEquals("2026-11-12", launched.getParameterList().get(1).getValue());
    assertEquals("STORED", launched.getParameterList().get(2).getValue());
    // The mapped values become the parameters of the run, so they reach every step of the flow (AWE-8)
    assertEquals(Map.of("date", "2026-11-12"), launched.getRunParameters());
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
  void launchDependencyInheritsTheRunAndLetsRunParametersOverridePropagatedValues() throws Exception {
    given(taskRepository.loadTask(5)).willReturn(task(5, parameter("date", ParameterConstants.VARIABLE, "STORED")));
    given(flowRuns.getParameters("r")).willReturn(Map.of("date", "RUN"));
    TaskExecution parent = new TaskExecution().setTaskId(2).setExecutionId(20).setRunId("r");

    launcher.launchDependency(5, parent, Map.of("date", "PARENT"), submitter);

    assertEquals("r", submitted.get(0).getRunId());
    assertEquals("RUN", submitted.get(0).getParameterList().get(0).getValue());
  }

  @Test
  void finishedTaskLaunchesEveryChildInTheRunThroughTheSubmitter() throws Exception {
    givenRun(2, dependency(5, 2), dependency(6, 2));
    given(taskRepository.loadTask(5)).willReturn(task(5, parameter("date", ParameterConstants.VARIABLE, "STORED")));
    given(taskRepository.loadTask(6)).willReturn(task(6));
    given(flowRuns.transition("r", 5, "WAITING", "LAUNCHED")).willReturn(true);
    given(flowRuns.transition("r", 6, "WAITING", "LAUNCHED")).willReturn(true);
    Task parent = new Task().setTaskId(2)
      .setParameterList(List.of(parameter("date", ParameterConstants.VALUE, "2026-10-15")));
    TaskExecution execution = execution(2, 20, TaskStatus.JOB_OK);

    launcher.onFinishTask(parent, execution, submitter);

    assertEquals(2, submitted.size());
    assertEquals("2026-10-15", submitted.get(0).getParameterList().get(0).getValue());
    assertEquals("#2", submitted.get(1).getLauncher());
    assertSame(execution, submitted.get(1).getParentExecution());
    assertEquals("r", submitted.get(1).getRunId());
  }

  @Test
  void diamondJoinLaunchesTheChildOnceAfterBothParentsWithTheRunParameters() throws Exception {
    // A(1) -> B(2), C(3); B -> D(4), C -> D
    givenRun(1, dependency(2, 1), dependency(3, 1), dependency(4, 2), dependency(4, 3));
    given(flowRuns.getParameters("r")).willReturn(Map.of("date", "RUN"));
    given(taskRepository.loadTask(4)).willReturn(task(4, parameter("date", ParameterConstants.VARIABLE, "STORED")));
    given(flowRuns.transition("r", 4, "WAITING", "LAUNCHED")).willReturn(true);

    // B ends while C still runs: D keeps waiting
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 10, 0), new ExecutionRow(3, 12, 4)));
    launcher.onFinishTask(new Task().setTaskId(2), execution(2, 11, TaskStatus.JOB_OK), submitter);
    assertTrue(submitted.isEmpty());
    verify(flowRuns, never()).transition(any(), any(), any(), any());

    // C ends: both parents are done, D launches from C
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 10, 0), new ExecutionRow(2, 11, 0)));
    TaskExecution last = execution(3, 12, TaskStatus.JOB_OK);
    launcher.onFinishTask(new Task().setTaskId(3), last, submitter);

    assertEquals(1, submitted.size());
    Task launched = submitted.get(0);
    assertEquals(4, launched.getTaskId());
    assertEquals("#3", launched.getLauncher());
    assertEquals("RUN", launched.getParameterList().get(0).getValue());
    assertSame(last, launched.getParentExecution());
  }

  @Test
  void aChildLostByAnotherParentIsNotLaunchedTwice() throws Exception {
    givenRun(1, dependency(4, 1));
    given(flowRuns.transition("r", 4, "WAITING", "LAUNCHED")).willReturn(false);

    launcher.onFinishTask(new Task().setTaskId(1), execution(1, 10, TaskStatus.JOB_OK), submitter);

    assertTrue(submitted.isEmpty());
  }

  @Test
  void failedParentSkipsTheChildAndTheRunEndsInError() throws Exception {
    // A(1) -> B(2) -> D(4): B fails and does not launch dependencies on error
    givenRun(1, dependency(2, 1), dependency(4, 2));
    given(taskRepository.loadTask(1)).willReturn(task(1));
    given(flowRuns.transition("r", 4, "WAITING", "SKIPPED")).willReturn(true);
    given(flowRuns.getJoins("r")).willReturn(List.of(new JoinRow(2, "LAUNCHED", 11), new JoinRow(4, "SKIPPED", null)));
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 10, 0), new ExecutionRow(2, 11, 1)));

    launcher.onFinishTask(new Task().setTaskId(2).setLaunchDependenciesOnError(false),
      execution(2, 11, TaskStatus.JOB_ERROR), submitter);

    assertTrue(submitted.isEmpty());
    verify(flowRuns, never()).transition("r", 4, "WAITING", "LAUNCHED");
    verify(flowRuns).endRun("r", TaskStatus.JOB_ERROR);
  }

  @Test
  void skippedChildPropagatesTheSkipToItsOwnChildren() throws Exception {
    // A(1) -> B(2) -> D(4) -> E(5)
    givenRun(1, dependency(2, 1), dependency(4, 2), dependency(5, 4));
    given(flowRuns.transition("r", 4, "WAITING", "SKIPPED")).willReturn(true);
    given(flowRuns.transition("r", 5, "WAITING", "SKIPPED")).willReturn(true);
    given(flowRuns.getJoins("r")).willReturn(List.of(new JoinRow(4, "SKIPPED", null)));

    launcher.onFinishTask(new Task().setTaskId(2), execution(2, 11, TaskStatus.JOB_ERROR), submitter);

    verify(flowRuns).transition("r", 5, "WAITING", "SKIPPED");
  }

  @Test
  void runWithSkippedTaskButNoFailureEndsInWarning() throws Exception {
    givenRun(1, dependency(2, 1));
    given(flowRuns.getJoins("r")).willReturn(List.of(new JoinRow(2, "SKIPPED", null)));
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 10, 0)));
    given(flowRuns.transition("r", 2, "WAITING", "SKIPPED")).willReturn(true);

    launcher.onFinishTask(new Task().setTaskId(1), execution(1, 10, TaskStatus.JOB_WARNING), submitter);

    verify(flowRuns).endRun("r", TaskStatus.JOB_WARNING);
  }

  @Test
  void runStaysOpenWhileATaskIsStillRunning() throws Exception {
    givenRun(1);
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 10, 0), new ExecutionRow(7, 3, 4)));

    launcher.onFinishTask(new Task().setTaskId(1), execution(1, 10, TaskStatus.JOB_OK), submitter);

    verify(flowRuns, never()).endRun(any(), any());
  }

  @Test
  void failedDependencyWarnsEveryParentConfiguredSo() throws Exception {
    // A(1) -> C(5): C fails, A asks to be set to warning
    givenRun(1, dependency(5, 1));
    given(taskRepository.loadTask(1)).willReturn(task(1).setSetTaskOnWarningIfDependencyError(true));
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 20, 0)));
    given(serviceConfig.getLocale("WARNING_MESSAGE_TASK_DEPENDENCY_ERROR", "5", "21")).willReturn("dependency failed");

    launcher.onFinishTask(new Task().setTaskId(5), execution(5, 21, TaskStatus.JOB_ERROR), submitter);

    verify(executionRepository).changeStatus(argThat(parent -> parent.getTaskId() == 1
      && parent.getExecutionId() == 20), eq(TaskStatus.JOB_WARNING), eq("dependency failed"));
  }

  @Test
  void parentIsLeftAloneWhenNotConfiguredOrWhenTheTaskDidNotFail() throws Exception {
    givenRun(1, dependency(5, 1));
    given(taskRepository.loadTask(1)).willReturn(task(1));
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 20, 0)));

    launcher.onFinishTask(new Task().setTaskId(5), execution(5, 21, TaskStatus.JOB_ERROR), submitter);
    launcher.onFinishTask(new Task().setTaskId(5), execution(5, 22, TaskStatus.JOB_WARNING), submitter);

    verify(executionRepository, never()).changeStatus(any(), any(), any());
  }

  @Test
  void parentStatusFailuresAreWrapped() throws Exception {
    givenRun(1, dependency(5, 1));
    AWException cause = new AWException("no parent");
    given(taskRepository.loadTask(1)).willThrow(cause);
    given(flowRuns.getExecutions("r")).willReturn(List.of(new ExecutionRow(1, 20, 0)));

    AWException exception = assertThrows(AWException.class, () -> launcher.onFinishTask(new Task().setTaskId(5),
      execution(5, 21, TaskStatus.JOB_ERROR), submitter));

    assertEquals("Error setting task status", exception.getMessage());
    assertSame(cause, exception.getCause());
    verify(executionRepository, never()).changeStatus(any(), any(), any());
  }

  @Test
  void unresolvedPropertiesCheckTheStoredKeyAndAChangedKeyIsReported() throws Exception {
    // A manual launch keeps the key of a property: the stored one is resolved, the changed one is refused
    given(taskRepository.loadTask(5))
      .willReturn(task(5, parameter("region", ParameterConstants.PROPERTY, "stored.key")));

    assertEquals(List.of("stored.key"), launcher.findUnresolvedProperties(5, Map.of("region", "operator.key")));
    assertEquals(List.of("region"), launcher.findChangedPropertyKeys(5, Map.of("region", "operator.key")));
  }

  private void givenRun(int root, TaskDependency... dependencies) throws AWException {
    given(flowRuns.getRootTaskId("r")).willReturn(root);
    given(flowRuns.getDependencies()).willReturn(List.of(dependencies));
  }

  private TaskDependency dependency(int child, int parent) {
    return new TaskDependency().setTaskId(child).setParentId(parent);
  }

  private TaskExecution execution(int taskId, int executionId, TaskStatus status) {
    return new TaskExecution().setTaskId(taskId).setExecutionId(executionId).setStatus(status.getValue())
      .setRunId("r");
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
