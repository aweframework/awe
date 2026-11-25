package com.almis.awe.scheduler.listener;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.details.ConnectionDetails;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.service.BroadcastService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * The old task screen (scheduler-tasks), which received the live updates of a running task, was removed (#993): the
 * listener pushes nothing to the users, even to a connection still on that screen, and a finished task still launches
 * the tasks that depend on it.
 */
@ExtendWith(MockitoExtension.class)
class SchedulerEventListenerTest {

  private static final String OLD_TASK_SCREEN_COMET = "old-task-screen-comet";

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
    userConnections.put(OLD_TASK_SCREEN_COMET,
      new ConnectionDetails().setConnection(OLD_TASK_SCREEN_COMET).setScreen("scheduler-tasks"));
    connections.put("user", userConnections);
    lenient().when(connectionTracker.getAllConnections()).thenReturn(connections);

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
  void finishingATaskLaunchesItsDependenciesAndPushesNothingToTheOldTaskScreen() throws AWException {
    listener.onTaskFinished(new SchedulerTaskFinishedEvent(this, task, execution, 12));

    verify(taskDAO).onFinishTask(task, execution);
    verifyNoInteractions(broadcastService);
  }
}
