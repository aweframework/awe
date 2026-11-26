package com.almis.awe.scheduler.listener;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.tracker.AweConnectionTracker;
import com.almis.awe.scheduler.bean.event.SchedulerTaskFinishedEvent;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.service.BroadcastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

/**
 * Scheduler task events: a finished task launches the tasks that depend on it. The screens of the scheduler read the
 * state of the executions from their queries, so no live update is pushed to the users (the old task screen that
 * received them was removed, #993).
 */
@Slf4j
public class SchedulerEventListener {

  private final TaskDAO taskDAO;

  /**
   * Autowired constructor
   *
   * @param broadcastService  Broadcast service (not used: kept for the auto-configuration)
   * @param connectionTracker Connection tracker (not used: kept for the auto-configuration)
   * @param taskDAO           Task DAO
   */
  public SchedulerEventListener(BroadcastService broadcastService, AweConnectionTracker connectionTracker, TaskDAO taskDAO) {
    this.taskDAO = taskDAO;
  }

  /**
   * On task finished event: launch the tasks that depend on it
   *
   * @param event Task finished event
   * @throws AWException Error launching the dependencies
   */
  @EventListener
  public void onTaskFinished(SchedulerTaskFinishedEvent event) throws AWException {
    taskDAO.onFinishTask(event.getTask(), event.getTaskExecution());
  }
}
