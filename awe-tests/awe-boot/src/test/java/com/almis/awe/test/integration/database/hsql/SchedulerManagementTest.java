package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Actions of the scheduler management screen (stop, start, clear and stop, restart), checked through the status and the
 * configured jobs that the screen shows. The scheduler is shared by the whole context, so each test restarts it in a
 * finally block when it leaves it stopped, and deletes the task it creates.
 * <p>
 * Replaces the browser flow {@code SchedulerManagementTestsIT#t001_manageScheduler}: the buttons of the screen only send
 * these maintains and reload this status.
 */
@Tag("integration")
class SchedulerManagementTest extends AbstractSchedulerMaintainTest {

  private static final String SCHEDULER_STOPPED = "Standby";

  @Autowired
  private Scheduler scheduler;

  @Test
  void stopKeepsTheJobsAndStartRunsTheSchedulerAgain() throws Exception {
    int taskId = createTask(scheduledTask(unique("Management stop"), 6));
    try {
      assertThat(schedulerStatus()).isEqualTo(SCHEDULER_RUNNING);
      assertThat(configuredJobs()).contains(String.valueOf(taskId));

      maintain("stopScheduler", params());
      assertThat(schedulerStatus()).isEqualTo(SCHEDULER_STOPPED);
      assertThat(triggerNames()).contains(String.valueOf(taskId));

      maintain("startScheduler", params());
      assertThat(schedulerStatus()).isEqualTo(SCHEDULER_RUNNING);
      assertThat(configuredJobs()).contains(String.valueOf(taskId));
    } finally {
      // The task is removed even when the scheduler could not be restarted
      try {
        restoreScheduler();
      } finally {
        deleteTasks(taskId);
      }
    }
  }

  @Test
  void clearAndStopRemovesTheJobsAndRestartLoadsThemAgain() throws Exception {
    int taskId = createTask(scheduledTask(unique("Management clear"), 6));
    try {
      maintain("clearAndStop", params());
      assertThat(schedulerStatus()).isEqualTo(SCHEDULER_STOPPED);
      assertThat(triggerNames()).isEmpty();

      // The restart loads the tasks again from the database
      maintain("restartScheduler", params());
      assertThat(schedulerStatus()).isEqualTo(SCHEDULER_RUNNING);
      assertThat(configuredJobs()).contains(String.valueOf(taskId));
    } finally {
      // The task is removed even when the scheduler could not be restarted
      try {
        restoreScheduler();
      } finally {
        deleteTasks(taskId);
      }
    }
  }

  /**
   * Leave the scheduler running for the next tests when a test failed with it stopped
   */
  private void restoreScheduler() throws Exception {
    if (!SCHEDULER_RUNNING.equals(schedulerStatus())) {
      maintain("restartScheduler", params());
    }
  }

  /**
   * Names of the triggers that the scheduler holds (the task identifiers), whatever its state: the management screen lists
   * them only while it runs
   */
  private Set<String> triggerNames() throws Exception {
    return scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup()).stream().map(TriggerKey::getName)
      .collect(Collectors.toSet());
  }
}
