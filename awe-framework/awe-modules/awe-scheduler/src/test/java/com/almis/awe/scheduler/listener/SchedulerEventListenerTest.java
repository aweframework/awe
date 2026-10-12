package com.almis.awe.scheduler.listener;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.details.ConnectionDetails;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.bean.event.SchedulerTaskProgressEvent;
import com.almis.awe.scheduler.bean.event.SchedulerTaskStartedEvent;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.service.BroadcastService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The scheduler task screen is opened from the menu, so its components live in the "report" view (nested in the "base"
 * home layout). The live updates pushed while a task runs must be addressed to that view (#815).
 */
@ExtendWith(MockitoExtension.class)
class SchedulerEventListenerTest {

  private static final String TASK_SCREEN_COMET = "task-screen-comet";
  private static final String OTHER_SCREEN_COMET = "other-screen-comet";
  private static final String TASK_VIEW = "report";

  @Mock
  private BroadcastService broadcastService;
  @Mock
  private AweConnectionTracker connectionTracker;
  @Mock
  private TaskDAO taskDAO;

  private SchedulerEventListener listener;
  private TaskExecution execution;
  private Task task;

  @BeforeEach
  void setUp() {
    ConcurrentMap<String, ConcurrentMap<String, ConnectionDetails>> connections = new ConcurrentHashMap<>();
    ConcurrentMap<String, ConnectionDetails> userConnections = new ConcurrentHashMap<>();
    userConnections.put(TASK_SCREEN_COMET, new ConnectionDetails().setConnection(TASK_SCREEN_COMET).setScreen("scheduler-tasks"));
    userConnections.put(OTHER_SCREEN_COMET, new ConnectionDetails().setConnection(OTHER_SCREEN_COMET).setScreen("other-screen"));
    connections.put("user", userConnections);
    when(connectionTracker.getAllConnections()).thenReturn(connections);

    execution = new TaskExecution();
    execution.setTaskId(3);
    execution.setExecutionId(7);
    execution.setInitialDate(new Date());
    execution.setExecutionTime(12);
    task = new Task();
    task.setTaskId(3);
    listener = new SchedulerEventListener(broadcastService, connectionTracker, taskDAO);
  }

  @Test
  void startedTaskSelectionIsAddressedToTaskScreenView() {
    listener.onTaskStarted(new SchedulerTaskStartedEvent(this, task));

    assertAllAddressedToTaskScreenView(captureBroadcast(2));
  }

  @Test
  void progressUpdatesAreAddressedToTaskScreenView() {
    when(taskDAO.getProgressNode(anyLong())).thenReturn(JsonNodeFactory.instance.objectNode());

    listener.onTaskProgress(new SchedulerTaskProgressEvent(this, execution, 100));

    List<ClientAction> actions = captureBroadcast(2);
    assertAllAddressedToTaskScreenView(actions);
    assertThat(actions).extracting(action -> action.getAddress().getComponent()).containsOnly("GrdExeLst");
    assertThat(actions).extracting(action -> action.getAddress().getRow()).containsOnly("3-7");
  }

  @Test
  void finishedUpdatesAndExecutionDataReloadAreAddressedToTaskScreenView() throws AWException {
    when(taskDAO.getProgressNode(anyLong())).thenReturn(JsonNodeFactory.instance.objectNode());
    when(taskDAO.getStatusIcon(any())).thenReturn(JsonNodeFactory.instance.objectNode());

    listener.onTaskFinished(new SchedulerTaskFinishedEvent(this, task, execution, 12));

    List<ClientAction> actions = captureBroadcast(4);
    assertAllAddressedToTaskScreenView(actions);
    assertThat(actions).extracting(action -> action.getAddress().getComponent()).contains("reload-execution-data");
  }

  @Test
  void usersOnOtherScreensReceiveNothing() {
    listener.onTaskStarted(new SchedulerTaskStartedEvent(this, task));

    verify(broadcastService, never()).broadcastMessageToUID(eq(OTHER_SCREEN_COMET), any(ClientAction[].class));
    assertThat(captureBroadcast(2)).isNotEmpty();
  }

  private List<ClientAction> captureBroadcast(int expectedActions) {
    List<ClientAction> actions = mockingDetails(broadcastService).getInvocations().stream()
      .filter(invocation -> TASK_SCREEN_COMET.equals(invocation.getRawArguments()[0]))
      .flatMap(invocation -> Arrays.stream((ClientAction[]) invocation.getRawArguments()[1]))
      .collect(Collectors.toList());
    assertThat(actions).hasSize(expectedActions);
    return actions;
  }

  private void assertAllAddressedToTaskScreenView(List<ClientAction> actions) {
    assertThat(actions).extracting(action -> action.getAddress().getView()).containsOnly(TASK_VIEW);
  }
}
