package com.almis.awe.scheduler.service;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private TaskDAO taskDAO;
  @InjectMocks
  private TaskService taskService;

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  @Test
  void aFailedTaskLoadDoesNotInterruptTheThreadNorStopTheOtherLoads() throws Exception {
    DataList tasks = new DataList();
    tasks.getRows().add(Map.of("taskId", new CellData(1)));
    tasks.getRows().add(Map.of("taskId", new CellData(2)));
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    Task loaded = new Task().setTaskId(2).setLaunchType(1);
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(parameters);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(tasks));
    when(taskDAO.getTask(1)).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broken task")));
    when(taskDAO.getTask(2)).thenReturn(CompletableFuture.completedFuture(loaded));

    taskService.loadSchedulerTasks();

    verify(taskDAO).addTaskToScheduler(loaded);
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  void anInterruptedTaskLoadRestoresTheInterruptFlag() throws Exception {
    DataList tasks = new DataList();
    tasks.getRows().add(Map.of("taskId", new CellData(1)));
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    @SuppressWarnings("unchecked")
    Future<Task> interrupted = mock(Future.class);
    when(interrupted.get()).thenThrow(new InterruptedException("interrupted"));
    when(queryUtil.getParameters(null, "1", "0")).thenReturn(parameters);
    when(queryService.launchPrivateQuery(anyString(), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(tasks));
    when(taskDAO.getTask(1)).thenReturn(interrupted);

    taskService.loadSchedulerTasks();

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }
}
