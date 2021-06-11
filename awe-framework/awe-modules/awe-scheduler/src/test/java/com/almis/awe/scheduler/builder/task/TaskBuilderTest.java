package com.almis.awe.scheduler.builder.task;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.file.File;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Class used for testing TaskBuilder class
 */
@Log4j2
@ExtendWith(MockitoExtension.class)
class TaskBuilderTest {

  /**
   * Generate task test with a null datalist
   */
  @Test
  void generateFileTask() throws AWException {
    Task task = new FileTaskBuilder(getTask().setLaunchType(TaskLaunchType.FILE_TRACKING.getValue()), new File()).build();
    assertNotNull(task);
    assertNotNull(task.getJob());
    assertNotNull(task.getTrigger());
  }

  /**
   * Generate task test with an empty datalist
   */
  @Test
  void generateScheduledTask() throws AWException {
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
