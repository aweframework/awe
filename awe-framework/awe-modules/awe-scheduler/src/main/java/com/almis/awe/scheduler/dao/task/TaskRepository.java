package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.report.Report;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskDependency;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.builder.task.TaskBuilder;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.factory.TaskFactory;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_ID;
import static com.almis.awe.scheduler.constant.MaintainConstants.FILE_DELETE_MODIFICATION_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_FILE_MODIFICATIONS_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_DEPENDENCIES_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_PARAMETERS_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_TASK_QUERY;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_PATH;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE_TASK;
import static com.almis.awe.scheduler.constant.TaskConstants.UPDATE_DATE;

/**
 * Task persistence: loads a task definition (parameters, dependencies, calendar, file and file modifications)
 * from the database and maintains its file modification table.
 */
@Slf4j
public class TaskRepository {

  private final Scheduler scheduler;
  private final QueryService queryService;
  private final MaintainService maintainService;
  private final QueryUtil queryUtil;
  private final CalendarDAO calendarDAO;
  private final ServerDAO serverDAO;
  private final ServiceConfig serviceConfig;

  /**
   * Constructor
   *
   * @param scheduler       Scheduler
   * @param queryService    Query service
   * @param maintainService Maintain service
   * @param queryUtil       Query utilities
   * @param calendarDAO     Calendar DAO
   * @param serverDAO       Server DAO
   * @param serviceConfig   Service configuration giving access to the current request
   */
  public TaskRepository(Scheduler scheduler, QueryService queryService, MaintainService maintainService,
                        QueryUtil queryUtil, CalendarDAO calendarDAO, ServerDAO serverDAO,
                        ServiceConfig serviceConfig) {
    this.scheduler = scheduler;
    this.queryService = queryService;
    this.maintainService = maintainService;
    this.queryUtil = queryUtil;
    this.calendarDAO = calendarDAO;
    this.serverDAO = serverDAO;
    this.serviceConfig = serviceConfig;
  }

  /**
   * Load task from task id
   *
   * @param taskId Task id
   * @return Generated task
   * @throws AWException Error retrieving task
   */
  public Task loadTask(Integer taskId) throws AWException {

    // Retrieve task data
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_ID, taskId);
    ServiceData taskParameters = queryService.launchPrivateQuery(SCHEDULER_TASK_QUERY, parameters);
    TaskBuilder taskBuilder = TaskFactory.getInstance(generateTask(taskParameters), generateFile(taskParameters),
      scheduler);

    // Set task parameters
    DataList taskParameterList = queryService.launchPrivateQuery(SCHEDULER_TASK_PARAMETERS_QUERY, parameters)
      .getDataList();
    taskBuilder.setParameters(DataListUtil.asBeanList(taskParameterList, TaskParameter.class));

    // Retrieve task dependencies
    DataList dependencyList = queryService.launchPrivateQuery(SCHEDULER_TASK_DEPENDENCIES_QUERY, parameters)
      .getDataList();
    taskBuilder.setDependencies(DataListUtil.asBeanList(dependencyList, TaskDependency.class));

    // Retrieve task calendar
    if (taskBuilder.getCalendarId() != null) {
      taskBuilder.setCalendar(calendarDAO.getCalendar(taskBuilder.getCalendarId()));
    }

    // Fill file if defined
    if (taskBuilder.getFile() != null) {
      // Generate server if file is defined
      taskBuilder.setFileServer(serverDAO.findServer(taskBuilder.getFile().getFileServerId()));

      // Generate file modifications if defined
      setFileModificationsFromDb(taskBuilder.getTask());
    }

    // Retrieve task
    return taskBuilder.build();
  }

  /**
   * Clear the file modification table from database
   *
   * @param taskId Task identifier
   * @throws AWException Error clearing the table
   */
  public void clearFileModificationTable(Integer taskId) throws AWException {
    serviceConfig.getRequest().setParameter(TASK_IDE_TASK, JsonNodeFactory.instance.numberNode(taskId));
    maintainService.launchPrivateMaintain(FILE_DELETE_MODIFICATION_QUERY);
  }

  private Task generateTask(ServiceData taskParameters) throws AWException {
    return DataListUtil.asBeanList(taskParameters.getDataList(), Task.class)
      .stream().findFirst()
      .orElseThrow(() -> new AWException("No task data found"))
      .setSchedule(DataListUtil.asBeanList(taskParameters.getDataList(), Schedule.class)
        .stream().findFirst().orElse(new Schedule()))
      .setReport(DataListUtil.asBeanList(taskParameters.getDataList(), Report.class)
        .stream().findFirst().orElse(new Report()));
  }

  private File generateFile(ServiceData taskParameters) {
    return DataListUtil.asBeanList(taskParameters.getDataList(), File.class).stream().findFirst().orElse(null);
  }

  /**
   * load file modification hashmap from database
   *
   * @param task Task
   * @throws AWException Error loading modification hashmap
   */
  private void setFileModificationsFromDb(Task task) throws AWException {
    // Set server ip to the context
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.put(TASK_IDE, task.getTaskId());
    DataList modificationsDataList = queryService.launchPrivateQuery(SCHEDULER_FILE_MODIFICATIONS_QUERY, parameters)
      .getDataList();
    log.debug("[File] Last modification files loaded from database on task #{}", task.getTaskId());

    // java.util.Date is the type of the File bean modification map (bean boundary)
    Map<String, Date> modifications = new HashMap<>();

    // Fill modifications hashmap
    for (Map<String, CellData> row : modificationsDataList.getRows()) {
      modifications.put(row.get(FILE_PATH).getStringValue(), row.get(UPDATE_DATE).getDateValue());
    }

    task.getFile().setFileModifications(modifications);
  }
}
