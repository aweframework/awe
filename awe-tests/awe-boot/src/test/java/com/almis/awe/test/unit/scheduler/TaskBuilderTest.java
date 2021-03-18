package com.almis.awe.test.unit.scheduler;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.builder.task.FileTaskBuilder;
import com.almis.awe.scheduler.builder.task.ScheduledTaskBuilder;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.test.unit.TestUtil;
import lombok.extern.log4j.Log4j2;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertNotNull;

/**
 * Class used for testing queries through ActionController
 *
 * @author jbellon
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Log4j2
public class TaskBuilderTest extends TestUtil {

  /**
   * Generate task test with a null datalist
   */
  @Test
  public void generateFileTask() throws AWException {
    Task task = new FileTaskBuilder(getTask().setLaunchType(TaskLaunchType.FILE_TRACKING.getValue()), new File()).build();
    assertNotNull(task);
    assertNotNull(task.getJob());
    assertNotNull(task.getTrigger());
  }

  /**
   * Generate task test with an empty datalist
   */
  @Test
  public void generateScheduledTask() throws AWException {
    Task task = new ScheduledTaskBuilder(getTask().setLaunchType(TaskLaunchType.SCHEDULED.getValue())).build();
    assertNotNull(task);
    assertNotNull(task.getJob());
    assertNotNull(task.getTrigger());
  }

  private Task getTask() {
    return new Task()
      .setTaskId(1)
      .setExecutionType(1)
      .setSchedule(new Schedule().setRepeatType(1).setRepeatNumber(1));
  }
}
