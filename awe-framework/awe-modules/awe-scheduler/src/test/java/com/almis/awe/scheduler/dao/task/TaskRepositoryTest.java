package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.calendar.Calendar;
import com.almis.awe.scheduler.bean.file.Server;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.ServerDAO;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.almis.awe.scheduler.constant.MaintainConstants.FILE_DELETE_MODIFICATION_QUERY;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_FILE_MODIFICATIONS_QUERY;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_PATH;
import static com.almis.awe.scheduler.constant.TaskConstants.FILE_TRACKING_GROUP;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_IDE_TASK;
import static com.almis.awe.scheduler.constant.TaskConstants.TASK_LAUNCH_TYPE;
import static com.almis.awe.scheduler.constant.TaskConstants.UPDATE_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link TaskRepository}
 */
@ExtendWith(MockitoExtension.class)
class TaskRepositoryTest {

  @Mock
  private Scheduler scheduler;

  @Mock
  private QueryService queryService;

  @Mock
  private MaintainService maintainService;

  @Mock
  private QueryUtil queryUtil;

  @Mock
  private CalendarDAO calendarDAO;

  @Mock
  private ServerDAO serverDAO;

  @Mock
  private ServiceConfig serviceConfig;

  @Mock
  private AweRequest aweRequest;

  private TaskRepository repository;

  @BeforeEach
  void initRepository() {
    repository = new TaskRepository(scheduler, queryService, maintainService, queryUtil, calendarDAO, serverDAO,
      serviceConfig);
  }

  @Test
  void fileTrackingTaskLoadsItsServerCalendarAndFileModifications() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(fileTrackingTaskData()));
    Date modified = new Date(1_000L);
    given(queryService.launchPrivateQuery(eq(SCHEDULER_FILE_MODIFICATIONS_QUERY), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(modificationData("/in/a.txt", modified)));
    Server server = new Server();
    given(serverDAO.findServer(3)).willReturn(server);
    Calendar calendar = new Calendar();
    given(calendarDAO.getCalendar(4)).willReturn(calendar);

    Task task = repository.loadTask(7);

    assertEquals(7, task.getTaskId());
    assertEquals(FILE_TRACKING_GROUP, task.getGroup());
    assertSame(server, task.getFile().getServer());
    assertSame(calendar, task.getCalendar());
    assertEquals(Map.of("/in/a.txt", modified), task.getFile().getFileModifications());
  }

  @Test
  void missingTaskDataFails() throws Exception {
    given(queryUtil.getParameters(null, "1", "0")).willReturn(JsonNodeFactory.instance.objectNode());
    given(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class)))
      .willReturn(new ServiceData().setDataList(new DataList()));

    AWException exception = assertThrows(AWException.class, () -> repository.loadTask(7));

    assertEquals("No task data found", exception.getMessage());
  }

  @Test
  void clearFileModificationTableSetsTheTaskInTheRequest() throws Exception {
    given(serviceConfig.getRequest()).willReturn(aweRequest);

    repository.clearFileModificationTable(7);

    verify(aweRequest).setParameter(TASK_IDE_TASK, JsonNodeFactory.instance.numberNode(7));
    verify(maintainService).launchPrivateMaintain(FILE_DELETE_MODIFICATION_QUERY);
  }

  private DataList fileTrackingTaskData() {
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(TASK_LAUNCH_TYPE, new CellData(TaskLaunchType.FILE_TRACKING.getValue()));
    row.put(TASK_IDE, new CellData(7));
    row.put("executionType", new CellData(1));
    row.put("repeatType", new CellData(2));
    row.put("repeatNumber", new CellData(2));
    row.put("calendarId", new CellData(4));
    row.put("fileServerId", new CellData(3));
    row.put(FILE_PATH, new CellData("/in"));
    dataList.addRow(row);
    return dataList;
  }

  private DataList modificationData(String path, Date date) {
    DataList dataList = new DataList();
    Map<String, CellData> row = new HashMap<>();
    row.put(FILE_PATH, new CellData(path));
    row.put(UPDATE_DATE, new CellData(date));
    dataList.addRow(row);
    return dataList;
  }
}
