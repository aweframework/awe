package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.task.FlowRules.ExecutionRow;
import com.almis.awe.scheduler.dao.task.FlowRules.JoinRow;
import com.almis.awe.scheduler.dao.task.FlowRules.Outcome;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.enums.TriggerType;
import com.almis.awe.scheduler.factory.TriggerFactory;
import com.almis.awe.scheduler.webhook.WebhookParameterMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.almis.awe.scheduler.constant.JobConstants.TASK;

/**
 * Immediate task launches: manual launches with operator supplied values, dependency launches propagating the
 * parent parameter values, and the end-of-task handling of a flow run: a child with several parents is launched
 * once, when all its parents inside the run have ended and met their condition, and skipped when one of them failed.
 */
@Slf4j
public class TaskImmediateLauncher {

  // locales
  private static final String WARNING_MESSAGE_TASK_DEPENDENCY_ERROR = "WARNING_MESSAGE_TASK_DEPENDENCY_ERROR";

  private final TaskRepository taskRepository;
  private final TaskExecutionRepository executionRepository;
  private final TaskParameterOverrides parameterOverrides;
  private final ServiceConfig serviceConfig;
  private final FlowRunRepository flowRuns;

  /**
   * Constructor
   *
   * @param taskRepository      Task repository
   * @param executionRepository Task execution repository
   * @param parameterOverrides  Task parameter override rules
   * @param serviceConfig       Service configuration giving access to locales
   * @param flowRuns            Flow run repository
   */
  public TaskImmediateLauncher(TaskRepository taskRepository, TaskExecutionRepository executionRepository,
                               TaskParameterOverrides parameterOverrides, ServiceConfig serviceConfig,
                               FlowRunRepository flowRuns) {
    this.taskRepository = taskRepository;
    this.executionRepository = executionRepository;
    this.parameterOverrides = parameterOverrides;
    this.serviceConfig = serviceConfig;
    this.flowRuns = flowRuns;
  }

  /**
   * Launch a task manually, applying the operator supplied values for its parameters
   *
   * @param taskId    Task identifier
   * @param user      Launch user
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @param submitter Scheduling entry point
   * @throws AWException Error executing immediate task
   */
  public void launchNow(Integer taskId, String user, Map<String, String> variables, TaskSubmitter submitter)
    throws AWException {
    // Creates a task that is executed at the moment it is added to the scheduler
    launch(taskId, TriggerType.MANUAL, user, null, task -> variables, submitter);

    // Log launched task
    log.info("Task launched manually: {}", taskId);
  }

  /**
   * Launch a task because a webhook arrived. The data of the webhook gives the values of the variable parameters that
   * have a JSON pointer; the other parameters keep their stored values.
   *
   * @param taskId    Task identifier
   * @param hookKey   Key of the hook that received the webhook
   * @param data      Data of the webhook
   * @param submitter Scheduling entry point
   * @throws AWException Error executing the task
   */
  public void launchWebhook(Integer taskId, String hookKey, JsonNode data, TaskSubmitter submitter)
    throws AWException {
    launch(taskId, TriggerType.WEBHOOK, "webhook:" + hookKey, null,
      task -> WebhookParameterMapper.variables(task.getParameterList(), data), submitter);

    // Log launched task
    log.info("[SCHEDULER][WEBHOOK {}] Task {} launched", hookKey, taskId);
  }

  /**
   * Launch the task as dependency, propagating the given parent parameter values to the child.
   * The child defines the contract: only the child's VARIABLE (source="1") parameters whose name
   * is present in the map are overridden.
   *
   * @param taskId          Task identifier
   * @param parentExecution Parent execution
   * @param variables       Parent parameter values (name -&gt; value) to propagate; empty for none
   * @param submitter       Scheduling entry point
   * @throws AWException Error executing dependency task
   */
  public void launchDependency(Integer taskId, TaskExecution parentExecution, Map<String, String> variables,
                               TaskSubmitter submitter) throws AWException {
    // Creates a task that is executed at the moment it is added to the scheduler
    launch(taskId, TriggerType.DEPENDENCY, "#" + parentExecution.getTaskId(), parentExecution,
      task -> variables, submitter);

    // Log launched task
    log.info("Task #{} launched as dependency from task #{}", taskId, parentExecution.getTaskId());
  }

  /**
   * Property keys of the task that do not resolve against the configuration of this instance,
   * taking the operator supplied values into account. An empty list means every PROPERTY
   * parameter of the task resolves and the task can be launched.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return Property keys that do not resolve, in parameter order
   * @throws AWException Error loading the task
   */
  public List<String> findUnresolvedProperties(Integer taskId, Map<String, String> variables) throws AWException {
    Task task = taskRepository.loadTask(taskId);
    parameterOverrides.apply(task, variables, parameterOverrides.overridableSources(TriggerType.MANUAL));
    return parameterOverrides.unresolvedProperties(task);
  }

  /**
   * Parameters of the task whose operator supplied value is not a value of their type (see
   * {@link TaskParameterOverrides#invalidValues(Task, Map)}). An empty list means every supplied value can be used.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return Names of the parameters with a value that is not valid, in parameter order
   * @throws AWException Error loading the task
   */
  public List<String> findInvalidValues(Integer taskId, Map<String, String> variables) throws AWException {
    if (variables == null || variables.isEmpty()) {
      return List.of();
    }
    return parameterOverrides.invalidValues(taskRepository.loadTask(taskId), variables);
  }

  /**
   * Property parameters of the task whose operator supplied value changes their key (see
   * {@link TaskParameterOverrides#changedPropertyKeys(Task, Map)}). An empty list means no key changes.
   *
   * @param taskId    Task identifier
   * @param variables Operator supplied values (parameter name -&gt; value); null for none
   * @return Names of the property parameters whose key would change, in parameter order
   * @throws AWException Error loading the task
   */
  public List<String> findChangedPropertyKeys(Integer taskId, Map<String, String> variables) throws AWException {
    if (variables == null || variables.isEmpty()) {
      return List.of();
    }
    return parameterOverrides.changedPropertyKeys(taskRepository.loadTask(taskId), variables);
  }

  /**
   * On finish task event: evaluate the join of each child of the task inside its flow run (launch it, skip it or keep
   * it waiting), warn the parents of a failed task when they are configured so, and end the run when nothing is left
   * to do
   *
   * @param task      Finished task
   * @param execution Task execution
   * @param submitter Scheduling entry point for the dependencies
   * @throws AWException Error updating the run or launching the dependencies
   */
  public void onFinishTask(Task task, TaskExecution execution, TaskSubmitter submitter) throws AWException {
    String runId = execution.getRunId();
    Integer root = runId == null ? null : flowRuns.getRootTaskId(runId);
    if (root == null) {
      log.warn("[SCHEDULER][FLOW] Execution {} of task {} has no flow run: its dependencies are not evaluated",
        execution.getExecutionId(), execution.getTaskId());
      return;
    }

    FlowContext flow = new FlowContext(runId, flowGraph(root), task, execution, submitter);
    for (Integer child : flow.graph.children(task.getTaskId())) {
      evaluate(flow, child);
    }
    warnParents(flow);
    finishRun(flow);
  }

  /**
   * Decide the join of a child: skip it when an ended parent failed its condition, launch it when every parent
   * ended and met it, otherwise leave it waiting. Only the caller whose guarded update changes the row acts.
   *
   * @param flow  Run being evaluated
   * @param child Child task
   */
  private void evaluate(FlowContext flow, Integer child) throws AWException {
    if (!flow.graph.scope().contains(child)) {
      return;
    }
    List<JoinRow> joins = flowRuns.getJoins(flow.runId);
    flowRuns.register(flow.runId, child, joins);
    joins = flowRuns.getJoins(flow.runId);
    List<Outcome> outcomes = new ArrayList<>();
    for (Integer parent : flow.graph.parents(child)) {
      outcomes.add(outcome(flow, parent, joins));
    }

    switch (FlowRules.decide(outcomes)) {
      case SKIP -> {
        if (flowRuns.transition(flow.runId, child, FlowRules.WAITING, FlowRules.SKIPPED)) {
          log.info("[SCHEDULER][FLOW] Task #{} skipped in run {}", child, flow.runId);
          for (Integer grandchild : flow.graph.children(child)) {
            evaluate(flow, grandchild);
          }
        }
      }
      case LAUNCH -> {
        if (flowRuns.transition(flow.runId, child, FlowRules.WAITING, FlowRules.LAUNCHED)) {
          // Propagate the parent parameter values (#724): the child decides which of its VARIABLE parameters apply.
          // The run parameters are applied on top by the launch.
          launchDependency(child, flow.execution, parameterOverrides.propagatedValues(flow.task), flow.submitter);
        }
      }
      default -> log.debug("[SCHEDULER][FLOW] Task #{} waits for its parents in run {}", child, flow.runId);
    }
  }

  /**
   * Situation of a parent of the run: the finished execution is the one just received, any other parent is read
   * from the run
   */
  private Outcome outcome(FlowContext flow, Integer parent, List<JoinRow> joins) throws AWException {
    boolean skipped = joins.stream()
      .anyMatch(join -> join.taskId().equals(parent) && FlowRules.SKIPPED.equals(join.status()));
    Integer status = executionOf(flow, parent).map(ExecutionRow::status).orElse(null);
    boolean ended = status != null && status != TaskStatus.JOB_OK.getValue()
      && status != TaskStatus.JOB_RUNNING.getValue() && status != TaskStatus.JOB_QUEUED.getValue();
    Task parentTask = ended ? flow.taskOf(parent) : null;
    return FlowRules.parentOutcome(status, skipped,
      parentTask != null && parentTask.isLaunchDependenciesOnWarning(),
      parentTask != null && parentTask.isLaunchDependenciesOnError());
  }

  /**
   * Latest execution of a task in the run, the finished one when it is the task that just ended
   */
  private Optional<ExecutionRow> executionOf(FlowContext flow, Integer taskId) throws AWException {
    if (taskId.equals(flow.execution.getTaskId())) {
      return Optional.of(new ExecutionRow(taskId, flow.execution.getExecutionId(), flow.execution.getStatus()));
    }
    return flowRuns.getExecutions(flow.runId).stream()
      .filter(execution -> execution.taskId().equals(taskId))
      .max(Comparator.comparing(ExecutionRow::executionId));
  }

  /**
   * Set every parent of a failed task to warning when the parent is configured so
   */
  private void warnParents(FlowContext flow) throws AWException {
    TaskExecution execution = flow.execution;
    if (execution.getStatus() == null || TaskStatus.JOB_ERROR.getValue() != execution.getStatus()) {
      return;
    }
    try {
      for (Integer parent : flow.graph.parents(execution.getTaskId())) {
        Optional<ExecutionRow> parentExecution = executionOf(flow, parent);
        if (parentExecution.isPresent() && flow.taskOf(parent).isSetTaskOnWarningIfDependencyError()) {
          executionRepository.changeStatus(new TaskExecution().setTaskId(parent)
              .setExecutionId(parentExecution.get().executionId()).setRunId(flow.runId), TaskStatus.JOB_WARNING,
            serviceConfig.getLocale(WARNING_MESSAGE_TASK_DEPENDENCY_ERROR,
              execution.getTaskId().toString(), execution.getExecutionId().toString()));
        }
      }
    } catch (Exception exc) {
      throw new AWException("Error setting task status", exc);
    }
  }

  /**
   * End the run when nothing is pending, with the worst status of its executions
   */
  private void finishRun(FlowContext flow) throws AWException {
    List<JoinRow> joins = flowRuns.getJoins(flow.runId);
    Map<Integer, ExecutionRow> executions = new HashMap<>();
    for (ExecutionRow row : flowRuns.getExecutions(flow.runId)) {
      executions.merge(row.taskId(), row, (a, b) -> a.executionId() >= b.executionId() ? a : b);
    }
    // The execution that just ended may not be in the read yet when the database lags behind the event
    executions.put(flow.execution.getTaskId(), new ExecutionRow(flow.execution.getTaskId(),
      flow.execution.getExecutionId(), flow.execution.getStatus()));

    if (!FlowRules.isPending(joins, executions.values())) {
      TaskStatus status = FlowRules.runStatus(executions.values().stream().map(ExecutionRow::status).toList(),
        joins.stream().anyMatch(join -> FlowRules.SKIPPED.equals(join.status())));
      if (flowRuns.endRun(flow.runId, status)) {
        log.info("[SCHEDULER][FLOW] Run {} ended with status {}", flow.runId, status);
      }
    }
  }

  /**
   * Dependency graph reachable from the run root
   */
  private FlowGraph flowGraph(Integer root) throws AWException {
    Map<Integer, List<Integer>> children = new LinkedHashMap<>();
    Map<Integer, List<Integer>> parents = new LinkedHashMap<>();
    for (TaskDependency dependency : flowRuns.getDependencies()) {
      children.computeIfAbsent(dependency.getParentId(), key -> new ArrayList<>()).add(dependency.getTaskId());
      parents.computeIfAbsent(dependency.getTaskId(), key -> new ArrayList<>()).add(dependency.getParentId());
    }
    return new FlowGraph(FlowRules.scope(root, children), children, parents);
  }

  /**
   * Dependency graph of a run: the tasks inside it and the children and parents of each task
   */
  private record FlowGraph(Set<Integer> scope, Map<Integer, List<Integer>> childrenByTask,
                           Map<Integer, List<Integer>> parentsByTask) {

    List<Integer> children(Integer taskId) {
      return childrenByTask.getOrDefault(taskId, List.of());
    }

    List<Integer> parents(Integer taskId) {
      return parentsByTask.getOrDefault(taskId, List.of()).stream().filter(scope::contains).toList();
    }
  }

  /**
   * State of the evaluation of one finished execution: the run, its graph and the tasks loaded so far
   */
  private final class FlowContext {
    private final String runId;
    private final FlowGraph graph;
    private final Task task;
    private final TaskExecution execution;
    private final TaskSubmitter submitter;
    private final Map<Integer, Task> tasks = new HashMap<>();

    private FlowContext(String runId, FlowGraph graph, Task task, TaskExecution execution, TaskSubmitter submitter) {
      this.runId = runId;
      this.graph = graph;
      this.task = task;
      this.execution = execution;
      this.submitter = submitter;
      this.tasks.put(task.getTaskId(), task);
    }

    private Task taskOf(Integer taskId) throws AWException {
      Task loaded = tasks.get(taskId);
      if (loaded == null) {
        loaded = taskRepository.loadTask(taskId);
        tasks.put(taskId, loaded);
      }
      return loaded;
    }
  }

  /**
   * Execute immediately a task
   *
   * @param taskId      Task id
   * @param triggerType Trigger type
   * @param launcher    Task launcher
   * @param parent      Parent execution
   * @param variables   Supplies, from the loaded task, the values for its parameters (null values when not applicable)
   * @param submitter   Scheduling entry point
   * @throws AWException Error executing immediate task
   */
  private void launch(Integer taskId, TriggerType triggerType, String launcher, TaskExecution parent,
                      Function<Task, Map<String, String>> variables, TaskSubmitter submitter) throws AWException {
    try {
      Task task = taskRepository.loadTask(taskId);
      task.setLauncher(launcher);
      task.setParentExecution(parent);

      // Override variable parameters with the operator supplied values
      Map<String, String> values = variables.apply(task);
      parameterOverrides.apply(task, values, parameterOverrides.overridableSources(triggerType));
      if (TriggerType.DEPENDENCY.equals(triggerType)) {
        // A dependency belongs to the run of its parent, whose parameters have the last word
        if (parent != null && parent.getRunId() != null) {
          task.setRunId(parent.getRunId());
          parameterOverrides.apply(task, flowRuns.getParameters(parent.getRunId()),
            parameterOverrides.overridableSources(triggerType));
        }
      } else {
        // The values of a manual or webhook launch become the parameters of its run
        task.setRunParameters(values);
      }

      JobDataMap dataMap = new JobDataMap();
      dataMap.put(TASK, task);

      // Add a new trigger to task
      task.setTrigger(TriggerFactory.getInstance(triggerType, dataMap));

      // Set launcher to job data
      task.setJob(task.getJob()
        .getJobBuilder()
        .withIdentity(task.getTrigger().getKey().getName(), task.getTrigger().getKey().getGroup())
        .build());

      // Schedule task
      submitter.submit(task);
    } catch (Exception exc) {
      throw new AWException("Error launching immediate task: " + taskId, exc);
    }
  }

  /**
   * Set the parent status to warning when this dependency failed and the parent is configured so
   *
   * @param execution Task execution
   */
  private void updateParentStatus(TaskExecution execution) throws AWException {
    if (execution.getParentExecution() != null) {
      try {
        Task parentTask = taskRepository.loadTask(execution.getParentExecution().getTaskId());
        if (TaskStatus.JOB_ERROR.equals(TaskStatus.valueOf(execution.getStatus()))
          && parentTask.isSetTaskOnWarningIfDependencyError()) {
          executionRepository.changeStatus(execution.getParentExecution(), TaskStatus.JOB_WARNING,
            serviceConfig.getLocale(WARNING_MESSAGE_TASK_DEPENDENCY_ERROR,
              execution.getTaskId().toString(), execution.getExecutionId().toString()));
        }
      } catch (Exception exc) {
        throw new AWException("Error setting task status", exc);
      }
    }
  }

  /**
   * Check if the task has to execute dependencies depending on its
   * configuration
   *
   * @param task      Task
   * @param execution Task execution
   * @return boolean
   */
  private boolean checkDependencyExecution(Task task, TaskExecution execution) {
    // Check if the dependencies have to be launched
    return switch (TaskStatus.valueOf(execution.getStatus())) {
      case JOB_OK -> true;
      case JOB_WARNING -> task.isLaunchDependenciesOnWarning();
      case JOB_ERROR -> task.isLaunchDependenciesOnError();
      default -> false;
    };
  }

  /**
   * Executes the dependencies for the given task
   *
   * @param task      Task to execute dependencies
   * @param execution Task execution
   * @param submitter Scheduling entry point
   */
  private void executeDependencies(Task task, TaskExecution execution, TaskSubmitter submitter) throws AWException {
    // Propagate the parent parameter values to each dependent child (#724). The child decides which
    // apply: only the child's VARIABLE parameters whose name matches are overridden.
    // Building the map from all parent parameters keeps the origin (value/property/variable)
    // irrelevant — the contract is defined by the child. Cascading is automatic: the child's mutated
    // task travels in the JobDataMap, so its own dependents inherit the accumulated values.
    Map<String, String> parentValues = parameterOverrides.propagatedValues(task);

    // Iterate dependencies and create a new task for each.
    for (TaskDependency dependency : task.getDependencyList()) {
      // Execute task dependency
      launchDependency(dependency.getTaskId(), execution, parentValues, submitter);
    }
  }
}
