package com.almis.awe.scheduler.dao.task;

import com.almis.awe.builder.client.ClientActionBuilder;
import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.builder.client.UpdateControllerActionBuilder;
import com.almis.awe.builder.client.css.AddCssClassActionBuilder;
import com.almis.awe.builder.client.css.RemoveCssClassActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.TimeUtil;
import com.almis.awe.model.util.data.date.DateTimeFormatting;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.log.ExecutionKey;
import com.almis.awe.scheduler.log.ExecutionLogPage;
import com.almis.awe.scheduler.log.ExecutionLogStore;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static com.almis.awe.model.constant.AweConstants.JSON_ICON_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_STYLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.TaskConstants.DEPENDENCY_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_ID;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_LOG_PATH;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_PROGRESS;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_EXECUTION_TIME;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCHED_BY_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE_LIST;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_SEPARATOR;

/**
 * Data of the task execution screens: the execution list with progress and elapsed time, the execution detail
 * screen and its log window, and the progress and status widgets pushed while a task runs.
 */
@Slf4j
public class TaskExecutionPresenter {

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final ExecutionLogStore executionLogStore;
  private final TaskExecutionRepository executionRepository;
  private final ServiceConfig serviceConfig;
  private final Clock clock;

  /**
   * Constructor
   *
   * @param queryService        Query service
   * @param queryUtil           Query utilities
   * @param executionLogStore   Execution log store
   * @param executionRepository Task execution repository
   * @param serviceConfig       Service configuration giving access to locales
   */
  public TaskExecutionPresenter(QueryService queryService, QueryUtil queryUtil, ExecutionLogStore executionLogStore,
                                TaskExecutionRepository executionRepository, ServiceConfig serviceConfig) {
    this(queryService, queryUtil, executionLogStore, executionRepository, serviceConfig, Clock.systemDefaultZone());
  }

  /**
   * Constructor with an explicit clock for the progress and elapsed time computations
   */
  TaskExecutionPresenter(QueryService queryService, QueryUtil queryUtil, ExecutionLogStore executionLogStore,
                         TaskExecutionRepository executionRepository, ServiceConfig serviceConfig, Clock clock) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.executionLogStore = executionLogStore;
    this.executionRepository = executionRepository;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
  }

  /**
   * Retrieve the execution list of a task, with progress, time, launcher and log locator of each execution
   *
   * @param taskId Task id
   * @return Task execution list
   * @throws AWException Error retrieving task execution list
   */
  public ServiceData getTaskExecutionList(Integer taskId) throws AWException {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    ServiceData serviceData = queryService.launchPrivateQuery("getTaskExecutionList", parameters);
    long averageExecution = executionRepository.getAverageTime(taskId);

    DataList dataList = serviceData.getDataList();
    for (Map<String, CellData> row : dataList.getRows()) {
      CellData progress = new CellData();
      CellData averageTime = new CellData();
      Integer executionId = row.get(TASK_EXECUTION_ID).getIntegerValue();

      // Calculate progress
      CellData status = row.get("Sta");
      CellData executionTime = row.get(TASK_EXECUTION_TIME);
      CellData launchType = row.get(TASK_LAUNCH_TYPE_LIST);
      CellData launchedBy = new CellData(getLaunchedByText(launchType.getStringValue(),
        row.get(TASK_LAUNCHED_BY_LIST).getStringValue()));
      CellData executionLogPathCell = new CellData(executionLogStore.locatorNode(
        new ExecutionKey(taskId, executionId)));
      Instant initialDate = LegacyDateConverter.asInstant(row.get("ExeLstTim").getDateValue());

      if (TaskStatus.JOB_RUNNING.equals(TaskStatus.valueOf(status.getIntegerValue()))) {
        progress = new CellData(getProgressNode(getProgress(initialDate, averageExecution)));
        averageTime = new CellData(TimeUtil.formatTime(getElapsedTime(initialDate), false));
      } else if (executionTime != null && executionTime.getIntegerValue() != null) {
        averageTime = new CellData(TimeUtil.formatTime(executionTime.getIntegerValue()));
      }

      row.put(TASK_EXECUTION_PROGRESS, progress);
      row.put(TASK_EXECUTION_TIME, averageTime);
      row.put(TASK_LAUNCHED_BY_LIST, launchedBy);
      row.put(TASK_EXECUTION_LOG_PATH, executionLogPathCell);
    }

    return serviceData;
  }

  /**
   * Retrieve progress node
   *
   * @param percentage Percentage
   * @return Progress node
   */
  public ObjectNode getProgressNode(long percentage) {
    ObjectNode progressBar = JsonNodeFactory.instance.objectNode();
    if (percentage > 100L) {
      progressBar.put(JSON_VALUE_PARAMETER, 100);
      progressBar.put(JSON_STYLE_PARAMETER, "progress-bar-danger");
      progressBar.put(JSON_LABEL_PARAMETER, "");
    } else {
      progressBar.put(JSON_VALUE_PARAMETER, percentage);
      progressBar.put(JSON_LABEL_PARAMETER, percentage + "%");
    }
    return progressBar;
  }

  /**
   * Generate status icon
   *
   * @param execution Execution
   * @return Status icon
   */
  public ObjectNode getStatusIcon(TaskExecution execution) {
    ObjectNode statusIcon = JsonNodeFactory.instance.objectNode();
    statusIcon.put(JSON_VALUE_PARAMETER, execution.getStatus());
    try {
      statusIcon.put(JSON_LABEL_PARAMETER, queryService.findLabel("StaTyp", execution.getStatus().toString()));
      statusIcon.put(JSON_ICON_PARAMETER, queryService.findLabel("StaIco", execution.getStatus().toString()));
      statusIcon.put(JSON_STYLE_PARAMETER, queryService.findLabel("StaSty", execution.getStatus().toString()));
    } catch (AWException exc) {
      log.error("Error finding icon labels", exc);
    }
    return statusIcon;
  }

  /**
   * Retrieve the progress of a running execution as a percentage of the average execution time
   *
   * @param initialDate      Execution start
   * @param averageExecution Average execution time in milliseconds
   * @return Progress percentage, 101 when there is no average execution time
   */
  public long getProgress(Instant initialDate, long averageExecution) {
    long currentExecution = clock.millis() - initialDate.toEpochMilli();
    return averageExecution == 0 ? 101L : (currentExecution * 100) / averageExecution;
  }

  /**
   * Retrieve elapsed time from a start instant
   *
   * @param startDate Start instant
   * @return Elapsed time in milliseconds
   */
  public Integer getElapsedTime(Instant startDate) {
    return (int) (clock.millis() - startDate.toEpochMilli());
  }

  /**
   * Load execution screen parameters
   *
   * @param path    Log path
   * @param address Button address
   * @return ServiceData
   * @throws AWException Error loading the execution screen
   */
  public ServiceData loadExecutionScreen(String path, ObjectNode address) throws AWException {
    ServiceData serviceData = new ServiceData();

    // Refresh execution screen
    String[] executionAddress = address.get("row").asString("").split(TASK_SEPARATOR);
    ExecutionKey key = new ExecutionKey(Integer.parseInt(executionAddress[0]), Integer.parseInt(executionAddress[1]));
    executionLogStore.applyViewerSelection(path, key, serviceData);

    TaskExecution taskExecution = executionRepository.getTaskExecution(key.taskId(), key.executionId());
    if (taskExecution != null) {
      refreshExecutionScreen(taskExecution, serviceData);
    }

    return serviceData;
  }

  /**
   * Retrieve the database-mode execution log window from a line offset (get-execution-log action).
   * Unreachable in file mode: the widget polls get-log-file, served by FileService, unchanged.
   * <p>
   * Single-response delivery: the store's lines always land in {@code LOG_CONTENT}, paired with
   * {@code LOG_CONTENT_REPLACE} telling the widget whether to append them or atomically replace
   * its current content, never a separate reset/filter round trip.
   *
   * @param taskId        Task identifier
   * @param executionId   Execution identifier
   * @param offset        Line offset to read from
   * @param clientVersion Window version the caller currently holds, as previously returned in
   *                      {@code LOG_CONTENT_VERSION}; {@code null} when absent
   * @return ServiceData
   * @throws AWException Error reading the execution log
   */
  public ServiceData getExecutionLog(Integer taskId, Integer executionId, Integer offset, String clientVersion)
    throws AWException {
    ServiceData serviceData = new ServiceData();
    ExecutionLogPage page = executionLogStore.read(new ExecutionKey(taskId, executionId), offset, clientVersion);

    serviceData.addVariable("LOG_CONTENT", new CellData(page.lines()));
    serviceData.addVariable("LOG_CONTENT_REPLACE", new CellData(page.replace()));
    serviceData.addVariable("LOG_CONTENT_VERSION", new CellData(page.version()));

    return serviceData;
  }

  /**
   * Reload execution task parameters
   *
   * @param taskId      Task identifier
   * @param executionId Execution identifier
   * @return Service Data
   * @throws AWException Error reloading execution screen
   */
  public ServiceData reloadExecutionScreen(Integer taskId, Integer executionId) throws AWException {
    ServiceData serviceData = new ServiceData();

    // Refresh execution screen
    if (taskId != null && executionId != null) {
      TaskExecution taskExecution = executionRepository.getTaskExecution(taskId, executionId);
      if (taskExecution != null) {
        refreshExecutionScreen(taskExecution, serviceData);
      }
    }

    return serviceData;
  }

  /**
   * Refresh execution screen data
   *
   * @param taskExecution Task execution
   * @param serviceData   Service data
   */
  private void refreshExecutionScreen(TaskExecution taskExecution, ServiceData serviceData) throws AWException {
    String status = taskExecution.getStatus().toString();
    String group = taskExecution.getGroupId();
    String startTime = formatTimestamp(taskExecution.getInitialDate());
    String endTime = taskExecution.getEndDate() == null ? "" : formatTimestamp(taskExecution.getEndDate());
    String executionTime = taskExecution.getExecutionTime() == null
      ? ""
      : TimeUtil.formatTime(taskExecution.getExecutionTime());
    addSilentAction(serviceData, new SelectActionBuilder("task-id", taskExecution.getName()));
    addSilentAction(serviceData, new SelectActionBuilder("execution-id", taskExecution.getExecutionId()));
    addSilentAction(serviceData, new SelectActionBuilder("execution-start-time", startTime));
    addSilentAction(serviceData, new SelectActionBuilder("execution-end-time", endTime));
    addSilentAction(serviceData, new SelectActionBuilder("execution-time", executionTime));
    addSilentAction(serviceData, new SelectActionBuilder("execution-information", taskExecution.getDescription()));
    addSilentAction(serviceData, new SelectActionBuilder("execution-title",
      serviceConfig.getLocale(queryService.findLabel("StaTit", status))));
    addSilentAction(serviceData, new SelectActionBuilder("execution-launched-by",
      getLaunchedByText(group, taskExecution.getLaunchedBy())));
    addSilentAction(serviceData, new UpdateControllerActionBuilder("execution-launched-by", "icon",
      queryService.findLabel("LchTxtIcoNam", group)));
    addSilentAction(serviceData, new RemoveCssClassActionBuilder("#execution-header,#execution-icon,"
      + "[criterion-id='execution-launched-by'] .text-icon,[criterion-id='execution-information'] .input",
      "bg-success", "bg-danger", "bg-warning", "bg-info",
      "fa-check", "fa-times", "fa-exclamation-triangle", "fa-chain-broken", "fa-refresh", "fa-spin",
      "text-danger", "text-info", "text-success", "text-light-gray", "input-sm"));
    addSilentAction(serviceData, new AddCssClassActionBuilder("#execution-header",
      queryService.findLabel("StaBg", status)));
    addSilentAction(serviceData, new AddCssClassActionBuilder("#execution-icon",
      queryService.findLabel("StaIco", status)));
    addSilentAction(serviceData, new AddCssClassActionBuilder("[criterion-id='execution-launched-by'] .text-icon",
      queryService.findLabel("LchTxtSty", group)));
  }

  /**
   * Add an asynchronous and silent client action
   *
   * @param serviceData Service data
   * @param builder     Client action builder
   */
  private void addSilentAction(ServiceData serviceData, ClientActionBuilder<?> builder) {
    builder.setAsync(true);
    builder.setSilent(true);
    serviceData.addClientAction(builder.build());
  }

  /**
   * Format an execution date as web timestamp with milliseconds
   *
   * @param date Execution date (TaskExecution bean boundary)
   * @return Web timestamp with milliseconds
   */
  private String formatTimestamp(Date date) {
    return DateTimeFormatting.formatWebTimestampWithMs(LegacyDateConverter.asLocalDateTime(date));
  }

  /**
   * Retrieve launchedBy text
   *
   * @param launchType Launch type
   * @param launchedBy Launched by
   * @return Launched by text
   */
  private String getLaunchedByText(String launchType, String launchedBy) {
    return DEPENDENCY_GROUP.equalsIgnoreCase(launchType)
      ? serviceConfig.getLocale("PARAMETER_TASK") + " " + launchedBy
      : launchedBy;
  }
}
