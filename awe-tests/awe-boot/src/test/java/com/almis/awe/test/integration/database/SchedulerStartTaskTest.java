package com.almis.awe.test.integration.database;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.Test;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starting an execution inserts its row with the next execution id of the task on every database (#991: MySQL rejects
 * an insert with a subquery on its own table)
 */
@WithMockUser(username = "test", password = "test")
public abstract class SchedulerStartTaskTest extends AbstractSpringAppIntegrationTest {

  private static final int TASK_ID = 99991;

  @Autowired
  private TaskDAO taskDAO;

  @Autowired
  private DataSource dataSource;

  @Test
  void startingATaskInsertsTheExecutionWithTheNextId() throws Exception {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    try {
      TaskExecution first = taskDAO.startTask(task());
      TaskExecution second = taskDAO.startTask(task());

      assertThat(first.getExecutionId()).isEqualTo(1);
      assertThat(second.getExecutionId()).isEqualTo(2);
    } finally {
      jdbc.update("DELETE FROM AweSchRun WHERE IdeTskRoo = ?", TASK_ID);
      jdbc.update("DELETE FROM AweSchExe WHERE IdeTsk = ?", TASK_ID);
    }
  }

  private Task task() {
    Task task = new Task().setTaskId(TASK_ID).setLauncher("test").setGroup("MANUAL_TASK").setStoredExecutions(10);
    task.setTrigger(TriggerBuilder.newTrigger().withIdentity(TASK_ID + "-test", "MANUAL_TASK").build());
    return task;
  }
}
