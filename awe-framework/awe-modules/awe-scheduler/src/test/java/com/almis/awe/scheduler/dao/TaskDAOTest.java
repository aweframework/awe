package com.almis.awe.scheduler.dao;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.filechecker.FileChecker;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.constant.QueryConstants.*;
import static com.almis.awe.scheduler.constant.TaskConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Class used for testing Task DAO class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class TaskDAOTest {

  private TaskDAO taskDAO;

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryService queryService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private Scheduler scheduler;

  @Mock
  private ApplicationContext context;

  @Mock
  private AweElements aweElements;

  @Mock
  private CalendarDAO calendarDAO;

  @Mock
  private ServerDAO serverDAO;

  @Mock
  private FileChecker fileChecker;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  void initBeans() {
    taskDAO = new TaskDAO(scheduler, 5, "", queryService, maintainService, queryUtil, calendarDAO, serverDAO, fileChecker);
    taskDAO.setApplicationContext(context);
  }

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(taskDAO);
  }

  /**
   * Change task test
   */
  @Test
  void changeTask() throws Exception {
    // Mock and spy
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    TaskExecution execution = new TaskExecution();
    execution.setStatus(TaskStatus.JOB_OK.getValue());
    execution.setDescription("Allright");

    // Run method
    taskDAO.changeStatus(new Task(), execution, TaskStatus.JOB_WARNING, "Because reasons");

    // Assert
    assertSame(TaskStatus.JOB_WARNING.getValue(), execution.getStatus());
    assertSame("Because reasons", execution.getDescription());
    verify(maintainService, times(1)).launchPrivateMaintain(anyString(), any(ObjectNode.class));
  }

  /**
   * Pause task test
   */
  @Test
  void pauseTask() throws Exception {
    // Mock and spy
    Task task = new Task();
    task.setTaskId(1);
    task.setGroup("TASK_GROUP");
    given(scheduler.checkExists(any(TriggerKey.class))).willReturn(true);

    // Run method
    taskDAO.pauseTask(task);

    // Assert
    verify(scheduler, times(1)).pauseTrigger(any(TriggerKey.class));
  }

  /**
   * Pause task without trigger checked
   */
  @Test
  void pauseTaskNoTrigger() throws Exception {
    // Mock and spy
    Task task = new Task();
    task.setTaskId(1);
    task.setGroup("TASK_GROUP");
    given(scheduler.checkExists(any(TriggerKey.class))).willReturn(false);

    // Run method
    taskDAO.pauseTask(task);

    // Assert
    verify(scheduler, times(0)).pauseTrigger(any(TriggerKey.class));
  }

  /**
   * Resume task test
   */
  @Test
  void resumeTask() throws Exception {
    // Mock and spy
    Task task = new Task();
    task.setLaunchType(1);
    task.setTaskId(1);
    task.setGroup("TASK_GROUP");
    task.setJob(null);
    task.setTrigger(TriggerBuilder.newTrigger().build());
    given(scheduler.checkExists(any(TriggerKey.class))).willReturn(true);
    given(scheduler.getTrigger(any(TriggerKey.class))).willReturn(TriggerBuilder.newTrigger().build());

    // Run method
    taskDAO.resumeTask(task);

    // Assert
    verify(scheduler, times(1)).rescheduleJob(any(TriggerKey.class), any(Trigger.class));
  }

  /**
   * Resume new task test
   */
  @Test
  void resumeNewTask() throws Exception {
    // Mock and spy
    Task task = new Task();
    task.setLaunchType(1);
    task.setTaskId(1);
    task.setGroup("TASK_GROUP");
    task.setJob(null);
    task.setTrigger(TriggerBuilder.newTrigger().build());

    // Run method
    taskDAO.resumeTask(task);

    // Assert
    verify(scheduler, times(1)).scheduleJob(eq(null), any(Trigger.class));
  }

  /**
   * Resume manual task test
   */
  @Test
  void resumeManualTask() throws Exception {
    // Mock and spy
    Task task = new Task();
    task.setLaunchType(0);
    task.setTaskId(1);
    task.setGroup("TASK_GROUP");
    task.setJob(null);
    task.setTrigger(TriggerBuilder.newTrigger().build());

    // Run method
    taskDAO.resumeTask(task);

    // Assert
    verify(scheduler, times(0)).scheduleJob(eq(null), any(Trigger.class));
  }

  /**
   * Get task execution from trigger
   */
  @Test
  void getTaskExecutionFromTrigger() {
    // Mock and spy
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity("1-121", "TASK_GROUP").build();

    // Run method
    TaskExecution execution = taskDAO.getTaskExecution(trigger);

    // Assert
    assertSame(1, execution.getTaskId());
    assertSame(121, execution.getExecutionId());
    assertSame("TASK_GROUP", execution.getGroupId());
  }

  /**
   * Get task execution preserves the task name (D7 regression - see #685).
   * The AweSchExe-only query keeps the execution row even when the matching AweSchTsk row is
   * missing; name is preserved via the getTaskName scalar subquery (NULL-tolerant). This test
   * verifies the DAO maps the returned {@code name} field onto {@code TaskExecution.name}, so
   * the execution-screen selector (refreshExecutionScreen) still receives a name.
   */
  @Test
  void getTaskExecutionPreservesName() throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_IDE, new CellData(1));
    row.put("name", new CellData("MyTaskName"));
    row.put("executionId", new CellData(1));
    row.put("status", new CellData(TaskStatus.JOB_OK.getValue()));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(dataList));

    TaskExecution execution = taskDAO.getTaskExecution(1, 1);

    assertNotNull(execution);
    assertEquals("MyTaskName", execution.getName());
  }

  /**
   * Check task finish ok
   */
  @Test
  void checkTaskFinishOk() throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    DataList taskDataList = getTaskDataList(true);
    Task task = mockTask();
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(
      new ServiceData().setDataList(taskDataList));
    TaskExecution parentExecution = new TaskExecution().setTaskId(2).setExecutionId(11).setStatus(TaskStatus.JOB_OK.getValue());
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(12).setStatus(TaskStatus.JOB_OK.getValue()).setParentExecution(parentExecution);

    // Finish task
    taskDAO.onFinishTask(task, execution);

    // Assert
    assertSame(TaskStatus.JOB_OK.getValue(), parentExecution.getStatus());
  }

  /**
   * Check task finish error
   */
  @Test
  void checkTaskFinishError() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), eq(null), any(Object[].class))).willReturn("LOCALE");
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());

    DataList taskDataList = getTaskDataList(true);
    Task task = mockTask();
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(
      new ServiceData().setDataList(taskDataList));
    TaskExecution parentExecution = new TaskExecution().setTaskId(2).setExecutionId(11).setStatus(TaskStatus.JOB_OK.getValue());
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(12).setStatus(TaskStatus.JOB_ERROR.getValue()).setParentExecution(parentExecution);

    // Finish task
    taskDAO.onFinishTask(task, execution);

    // Assert
    assertSame(TaskStatus.JOB_WARNING.getValue(), parentExecution.getStatus());
  }

  /**
   * Check task finish error
   */
  @Test
  void checkTaskFinishErrorParentOk() throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    DataList taskDataList = getTaskDataList(false);
    Task task = mockTask(false);
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(
      new ServiceData().setDataList(taskDataList));

    TaskExecution parentExecution = new TaskExecution().setTaskId(2).setExecutionId(11).setStatus(TaskStatus.JOB_OK.getValue());
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(12).setStatus(TaskStatus.JOB_ERROR.getValue()).setParentExecution(parentExecution);

    // Finish task
    taskDAO.onFinishTask(task, execution);

    // Assert
    assertSame(TaskStatus.JOB_OK.getValue(), parentExecution.getStatus());
  }

  /**
   * Check task finish error
   */
  @Test
  void checkTaskFinishWarning() throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    DataList taskDataList = getTaskDataList(true);
    Task task = mockTask();
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(
      new ServiceData().setDataList(taskDataList));
    TaskExecution parentExecution = new TaskExecution().setTaskId(2).setExecutionId(11).setStatus(TaskStatus.JOB_OK.getValue());
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(12).setStatus(TaskStatus.JOB_WARNING.getValue()).setParentExecution(parentExecution);

    // Finish task
    taskDAO.onFinishTask(task, execution);

    // Assert
    assertSame(TaskStatus.JOB_OK.getValue(), parentExecution.getStatus());
  }

  /**
   * Check task finish error
   */
  @Test
  void checkTaskFinishInterrupted() throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    DataList taskDataList = getTaskDataList(true);
    Task task = mockTask();
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(
      new ServiceData().setDataList(taskDataList));

    TaskExecution parentExecution = new TaskExecution().setTaskId(2).setExecutionId(11).setStatus(TaskStatus.JOB_OK.getValue());
    TaskExecution execution = new TaskExecution().setTaskId(1).setExecutionId(12).setStatus(TaskStatus.JOB_INTERRUPTED.getValue()).setParentExecution(parentExecution);

    // Finish task
    taskDAO.onFinishTask(task, execution);

    // Assert
    assertSame(TaskStatus.JOB_OK.getValue(), parentExecution.getStatus());
  }

  /**
   * Check if task execution is allowed
   */
  @Test
  void isTaskExecutionAllowed() {
    Task task = mockTask();
    task.setLaunchType(TaskLaunchType.FILE_TRACKING.getValue());
    given(fileChecker.checkFile(task)).willReturn("File");

    // Finish task
    boolean allowed = taskDAO.isTaskExecutionAllowed(task);

    // Assert
    assertTrue(allowed);
  }

  /**
   * Test new task without execution
   */
  @Test
  void startTask() throws Exception {
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryUtil.getParameters()).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(new DataList()));
    Task task = new Task();
    task.setTrigger(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    task.setParentExecution(new TaskExecution());

    // Finish task
    TaskExecution execution = taskDAO.startTask(task);

    // Assert
    assertNull(execution);
  }

  /**
   * Load execution screen
   */
  @Test
  void loadExecutionScreen() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), eq(null))).willReturn("LOCALE");
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    ObjectNode address = JsonNodeFactory.instance.objectNode();
    address.put("row", "1" + TASK_SEPARATOR + "1");
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_IDE, new CellData(1));
    row.put("executionId", new CellData(1));
    row.put("initialDate", new CellData(new Date()));
    row.put("status", new CellData(TaskStatus.JOB_INTERRUPTED.getValue()));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(dataList));
    given(queryService.findLabel(anyString(), anyString())).willReturn("Label");

    // Finish task
    ServiceData serviceData = taskDAO.loadExecutionScreen("lala", address);

    // Assert
    assertEquals(16, serviceData.getClientActionList().size());
  }

  /**
   * Reload execution screen
   */
  @Test
  void reloadExecutionScreen() throws Exception {
    doReturn(aweElements).when(context).getBean(any(Class.class));
    given(aweElements.getLocaleWithLanguage(anyString(), eq(null))).willReturn("LOCALE");
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());

    ObjectNode address = JsonNodeFactory.instance.objectNode();
    address.put("row", "1" + TASK_SEPARATOR + "1");
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_IDE, new CellData(1));
    row.put("executionId", new CellData(1));
    row.put("initialDate", new CellData(new Date()));
    row.put("endDate", new CellData(new Date()));
    row.put("status", new CellData(TaskStatus.JOB_WARNING.getValue()));
    row.put("executionTime", new CellData(1231));
    dataList.addRow(row);
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(dataList));
    given(queryService.findLabel(anyString(), anyString())).willReturn("Label");

    // Finish task
    ServiceData serviceData = taskDAO.reloadExecutionScreen(1, 1);

    // Assert
    assertEquals(13, serviceData.getClientActionList().size());
  }

  /**
   * Operator supplied values must override the stored value of VARIABLE (source="1") parameters
   * present in the map, while leaving VALUE (source="0") and PROPERTY (source="2") parameters,
   * as well as VARIABLE parameters not present in the map, untouched.
   */
  @Test
  void applyOperatorValuesOverridesVariableParameters() {
    TaskParameter variableInMap = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.VARIABLE)).setValue("2020-01-01");
    TaskParameter variableNotInMap = new TaskParameter().setName("other").setSource(String.valueOf(ParameterConstants.VARIABLE)).setValue("keepMe");
    TaskParameter valueParameter = new TaskParameter().setName("value").setSource(String.valueOf(ParameterConstants.VALUE)).setValue("staticValue");
    TaskParameter propertyParameter = new TaskParameter().setName("property").setSource(String.valueOf(ParameterConstants.PROPERTY)).setValue("staticProperty");

    Task task = new Task().setParameterList(Arrays.asList(variableInMap, variableNotInMap, valueParameter, propertyParameter));

    Map<String, String> variables = new HashMap<>();
    variables.put("date", "2026-07-06");
    // These entries must be ignored because the matching parameters are not VARIABLE parameters
    variables.put("value", "hackedValue");
    variables.put("property", "hackedProperty");

    // Run method
    taskDAO.applyOperatorValues(task, variables);

    // Assert
    assertEquals("2026-07-06", variableInMap.getValue());
    assertEquals("keepMe", variableNotInMap.getValue());
    assertEquals("staticValue", valueParameter.getValue());
    assertEquals("staticProperty", propertyParameter.getValue());
  }

  /**
   * A null operator values map must be a no-op and leave every parameter value untouched.
   */
  @Test
  void applyOperatorValuesNullMapIsNoOp() {
    TaskParameter variableParameter = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.VARIABLE)).setValue("2020-01-01");
    Task task = new Task().setParameterList(List.of(variableParameter));

    // Run method
    taskDAO.applyOperatorValues(task, null);

    // Assert
    assertEquals("2020-01-01", variableParameter.getValue());
  }

  /**
   * A non-null but empty operator values map must be a no-op (short-circuits before iterating).
   */
  @Test
  void applyOperatorValuesEmptyMapIsNoOp() {
    TaskParameter variableParameter = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.VARIABLE)).setValue("2020-01-01");
    Task task = new Task().setParameterList(List.of(variableParameter));

    // Run method
    taskDAO.applyOperatorValues(task, new HashMap<>());

    // Assert
    assertEquals("2020-01-01", variableParameter.getValue());
  }

  /**
   * A variable parameter with a null name must not raise a NPE and must keep its stored value.
   */
  @Test
  void applyOperatorValuesNullParameterNameIsNotOverridden() {
    TaskParameter nullNameParameter = new TaskParameter().setName(null).setSource(String.valueOf(ParameterConstants.VARIABLE)).setValue("keepMe");
    Task task = new Task().setParameterList(List.of(nullNameParameter));

    Map<String, String> variables = new HashMap<>();
    variables.put("date", "2026-07-06");

    // Run method
    assertDoesNotThrow(() -> taskDAO.applyOperatorValues(task, variables));

    // Assert
    assertEquals("keepMe", nullNameParameter.getValue());
  }

  /**
   * Issue #724: when a parent task launches its dependencies, the parent's parameter values must be
   * propagated to each dependent child. The child defines the contract: only the child's VARIABLE
   * (source="1") parameters whose name matches a parent parameter are overridden with the parent's
   * value; VARIABLE parameters the parent does not supply keep their stored default.
   */
  @Test
  void executeDependenciesPropagatesParentParametersToChild() throws Exception {
    // Parent finished OK carrying a value at execution time (source is irrelevant: origin does not matter)
    TaskParameter parentParam = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.VALUE)).setValue("2026-07-06");
    Task parentTask = new Task()
      .setTaskId(2)
      .setLaunchDependenciesOnError(true)
      .setLaunchDependenciesOnWarning(true)
      .setParameterList(List.of(parentParam))
      .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(5).setParentId(2)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20).setStatus(TaskStatus.JOB_OK.getValue());

    // Child (task 5) declares two VARIABLE parameters: 'date' (parent supplies it) and 'region' (parent does not)
    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(getTaskDataList(true)));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_PARAMETERS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(getVariableParameterDataList()));

    // Capture the child task that would be scheduled
    TaskDAO spyDao = spy(taskDAO);
    doNothing().when(spyDao).addTaskToScheduler(any(Task.class));
    ArgumentCaptor<Task> childCaptor = ArgumentCaptor.forClass(Task.class);

    // Parent finishes -> dependencies are launched
    spyDao.onFinishTask(parentTask, execution);

    // The child was scheduled with the parent's value applied to the matching VARIABLE parameter,
    // while the VARIABLE parameter the parent did not supply keeps its stored default.
    verify(spyDao).addTaskToScheduler(childCaptor.capture());
    Task child = childCaptor.getValue();
    assertEquals("2026-07-06", findParameter(child, "date").getValue());
    assertEquals("EU", findParameter(child, "region").getValue());
  }

  /**
   * Issue #724: a parent with no parameters launches its dependents exactly as before (no regression):
   * the child keeps every stored parameter value.
   */
  @Test
  void executeDependenciesWithoutParentParametersKeepsChildDefaults() throws Exception {
    Task parentTask = new Task()
      .setTaskId(2)
      .setLaunchDependenciesOnError(true)
      .setLaunchDependenciesOnWarning(true)
      .setParameterList(Collections.emptyList())
      .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(5).setParentId(2)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20).setStatus(TaskStatus.JOB_OK.getValue());

    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(getTaskDataList(true)));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_PARAMETERS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(getVariableParameterDataList()));

    TaskDAO spyDao = spy(taskDAO);
    doNothing().when(spyDao).addTaskToScheduler(any(Task.class));
    ArgumentCaptor<Task> childCaptor = ArgumentCaptor.forClass(Task.class);

    spyDao.onFinishTask(parentTask, execution);

    verify(spyDao).addTaskToScheduler(childCaptor.capture());
    Task child = childCaptor.getValue();
    assertEquals("STORED_DEFAULT", findParameter(child, "date").getValue());
    assertEquals("EU", findParameter(child, "region").getValue());
  }

  /**
   * Issue #724: a parent parameter with a null value is treated as "not supplied" and must not wipe
   * the matching child VARIABLE parameter's stored default.
   */
  @Test
  void executeDependenciesNullParentValueKeepsChildDefault() throws Exception {
    TaskParameter parentParam = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.VALUE)).setValue(null);
    Task parentTask = new Task()
      .setTaskId(2)
      .setLaunchDependenciesOnError(true)
      .setLaunchDependenciesOnWarning(true)
      .setParameterList(List.of(parentParam))
      .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(5).setParentId(2)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20).setStatus(TaskStatus.JOB_OK.getValue());

    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(getTaskDataList(true)));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_PARAMETERS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(getVariableParameterDataList()));

    TaskDAO spyDao = spy(taskDAO);
    doNothing().when(spyDao).addTaskToScheduler(any(Task.class));
    ArgumentCaptor<Task> childCaptor = ArgumentCaptor.forClass(Task.class);

    spyDao.onFinishTask(parentTask, execution);

    verify(spyDao).addTaskToScheduler(childCaptor.capture());
    Task child = childCaptor.getValue();
    assertEquals("STORED_DEFAULT", findParameter(child, "date").getValue());
  }

  /**
   * Issue #724: propagation cascades along the whole chain. The child's mutated task carries the
   * inherited value, so when the child finishes and launches its own dependency, the grandchild
   * inherits the value accumulated from the parent through the child.
   */
  @Test
  void executeDependenciesCascadesPropagationToGrandchild() throws Exception {
    TaskParameter parentParam = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.VALUE)).setValue("2026-07-06");
    Task parentTask = new Task()
      .setTaskId(2)
      .setLaunchDependenciesOnError(true)
      .setLaunchDependenciesOnWarning(true)
      .setParameterList(List.of(parentParam))
      .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(5).setParentId(2)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20).setStatus(TaskStatus.JOB_OK.getValue());

    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(getTaskDataList(true)));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_PARAMETERS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(getVariableParameterDataList()));

    TaskDAO spyDao = spy(taskDAO);
    doNothing().when(spyDao).addTaskToScheduler(any(Task.class));
    ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);

    // Hop 1: parent finishes and launches the child, which inherits date=2026-07-06
    spyDao.onFinishTask(parentTask, execution);
    verify(spyDao).addTaskToScheduler(captor.capture());
    Task child = captor.getValue();
    assertEquals("2026-07-06", findParameter(child, "date").getValue());

    // Hop 2: the mutated child finishes and launches its own dependency (grandchild)
    child.setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(9).setParentId(5)));
    TaskExecution childExecution = new TaskExecution().setTaskId(5).setExecutionId(21).setStatus(TaskStatus.JOB_OK.getValue());
    spyDao.onFinishTask(child, childExecution);

    // The grandchild inherits the value accumulated through the child
    verify(spyDao, times(2)).addTaskToScheduler(captor.capture());
    Task grandchild = captor.getValue();
    assertEquals("2026-07-06", findParameter(grandchild, "date").getValue());
  }

  /**
   * Issue #724: a parent PROPERTY-source (source="2") parameter stores the property KEY, not the value.
   * When propagated to a dependent child, the child's matching VARIABLE parameter must receive the
   * RESOLVED property value, not the raw property key.
   */
  @Test
  void executeDependenciesPropagatesResolvedPropertyValueToChild() throws Exception {
    // Parent supplies a PROPERTY-source parameter 'date' holding the property key
    TaskParameter parentParam = new TaskParameter().setName("date").setSource(String.valueOf(ParameterConstants.PROPERTY)).setValue("some.property.key");
    Task parentTask = new Task()
      .setTaskId(2)
      .setLaunchDependenciesOnError(true)
      .setLaunchDependenciesOnWarning(true)
      .setParameterList(List.of(parentParam))
      .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(5).setParentId(2)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20).setStatus(TaskStatus.JOB_OK.getValue());

    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(getTaskDataList(true)));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_PARAMETERS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(getVariableParameterDataList()));

    // The property key resolves to an actual value via getProperty (getElements().getProperty)
    doReturn(aweElements).when(context).getBean(AweElements.class);
    given(aweElements.getProperty("some.property.key")).willReturn("2026-07-06");

    TaskDAO spyDao = spy(taskDAO);
    doNothing().when(spyDao).addTaskToScheduler(any(Task.class));
    ArgumentCaptor<Task> childCaptor = ArgumentCaptor.forClass(Task.class);

    spyDao.onFinishTask(parentTask, execution);

    // The child receives the RESOLVED property value, not the property key
    verify(spyDao).addTaskToScheduler(childCaptor.capture());
    Task child = childCaptor.getValue();
    assertEquals("2026-07-06", findParameter(child, "date").getValue());
  }

  /**
   * Issue #724: a child's non-VARIABLE parameter (here source="0"/VALUE) whose name matches a parent
   * parameter is never overridden; only VARIABLE parameters accept propagated parent values.
   */
  @Test
  void executeDependenciesDoesNotOverrideChildNonVariableParameter() throws Exception {
    // Parent supplies 'database'='prod'; the child declares 'database' as a VALUE parameter that must be kept
    TaskParameter parentParam = new TaskParameter().setName("database").setSource(String.valueOf(ParameterConstants.VALUE)).setValue("prod");
    Task parentTask = new Task()
      .setTaskId(2)
      .setLaunchDependenciesOnError(true)
      .setLaunchDependenciesOnWarning(true)
      .setParameterList(List.of(parentParam))
      .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(5).setParentId(2)));
    TaskExecution execution = new TaskExecution().setTaskId(2).setExecutionId(20).setStatus(TaskStatus.JOB_OK.getValue());

    given(queryUtil.getParameters(any(), any(), any())).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).willReturn(new ServiceData().setDataList(getTaskDataList(true)));
    given(queryService.launchPrivateQuery(eq(SCHEDULER_TASK_PARAMETERS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(getNonVariableParameterDataList()));

    TaskDAO spyDao = spy(taskDAO);
    doNothing().when(spyDao).addTaskToScheduler(any(Task.class));
    ArgumentCaptor<Task> childCaptor = ArgumentCaptor.forClass(Task.class);

    spyDao.onFinishTask(parentTask, execution);

    // The child's VALUE-source 'database' parameter keeps its stored value; the parent value is ignored
    verify(spyDao).addTaskToScheduler(childCaptor.capture());
    Task child = childCaptor.getValue();
    assertEquals("test", findParameter(child, "database").getValue());
  }

  /**
   * Build a data list with a single VALUE (source="0") child parameter and its stored default.
   *
   * @return DataList mimicking the taskParameters query result for the child task
   */
  private DataList getNonVariableParameterDataList() {
    DataList parameterDataList = new DataList();
    Map<String, CellData> databaseRow = new HashMap<>();
    databaseRow.put("name", new CellData("database"));
    databaseRow.put("source", new CellData(String.valueOf(ParameterConstants.VALUE)));
    databaseRow.put("value", new CellData("test"));
    parameterDataList.addRow(databaseRow);
    return parameterDataList;
  }

  /**
   * Build a data list with two VARIABLE (source="1") child parameters and their stored defaults.
   *
   * @return DataList mimicking the taskParameters query result for the child task
   */
  private DataList getVariableParameterDataList() {
    DataList parameterDataList = new DataList();
    Map<String, CellData> dateRow = new HashMap<>();
    dateRow.put("name", new CellData("date"));
    dateRow.put("source", new CellData(String.valueOf(ParameterConstants.VARIABLE)));
    dateRow.put("value", new CellData("STORED_DEFAULT"));
    parameterDataList.addRow(dateRow);
    Map<String, CellData> regionRow = new HashMap<>();
    regionRow.put("name", new CellData("region"));
    regionRow.put("source", new CellData(String.valueOf(ParameterConstants.VARIABLE)));
    regionRow.put("value", new CellData("EU"));
    parameterDataList.addRow(regionRow);
    return parameterDataList;
  }

  /**
   * Find a parameter by name in a task parameter list.
   *
   * @param task Task holding the parameters
   * @param name Parameter name to find
   * @return Matching task parameter
   */
  private TaskParameter findParameter(Task task, String name) {
    return task.getParameterList().stream()
      .filter(parameter -> name.equals(parameter.getName()))
      .findFirst()
      .orElseThrow(() -> new AssertionError("Parameter not found: " + name));
  }

  /**
   * Mock task
   *
   * @return Task task
   */
  private Task mockTask() {
    return mockTask(true);
  }

  /**
   * Mock a task
   *
   * @return Task mocked
   */
  private Task mockTask(boolean setTaskOnWarning) {
    return new Task()
            .setTaskId(1)
            .setExecutionType(1)
            .setLaunchType(TaskLaunchType.SCHEDULED.getValue())
            .setLaunchDependenciesOnError(true)
            .setLaunchDependenciesOnWarning(true)
            .setSetTaskOnWarningIfDependencyError(setTaskOnWarning)
            .setSchedule(new Schedule().setRepeatNumber(2).setRepeatType(2))
            .setDependencyList(Collections.singletonList(new TaskDependency().setTaskId(1).setParentId(2)));
  }

  private DataList getTaskDataList(boolean setTaskOnWarning) {
    // Mock
    DataList taskDataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_LAUNCH_TYPE, new CellData(1));
    row.put(TASK_IDE, new CellData(2));
    row.put("repeatType", new CellData(2));
    row.put("repeatNumber", new CellData(2));
    row.put("executionType", new CellData(1));
    row.put("parentId", new CellData(1));
    row.put("launchDependenciesOnError", new CellData(true));
    row.put("launchDependenciesOnWarning", new CellData(true));
    row.put("setTaskOnWarningIfDependencyError", new CellData(setTaskOnWarning));
    row.put(FILE_PATH, new CellData("file/path"));
    row.put(UPDATE_DATE, new CellData(new Date()));
    taskDataList.addRow(row);

    return taskDataList;
  }
}
