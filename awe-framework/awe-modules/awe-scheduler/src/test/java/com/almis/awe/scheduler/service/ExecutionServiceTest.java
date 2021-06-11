package com.almis.awe.scheduler.service;

import com.almis.awe.scheduler.bean.task.TaskExecution;
import com.almis.awe.scheduler.enums.TaskStatus;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Class used for testing ExecutionService class
 */
@Log4j2
@ExtendWith(MockitoExtension.class)
class ExecutionServiceTest {

  @InjectMocks
  private ExecutionService executionService;

  @Mock
  private Scheduler scheduler;

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    // Check that controller are active
    assertNotNull(executionService);
  }

  /**
   * Start progress job 1 second
   */
  @Test
  void startProgressJobOneSecond() throws Exception {
    // Mock and spy
    TaskExecution execution = new TaskExecution();
    execution.setStatus(TaskStatus.JOB_OK.getValue());
    execution.setDescription("AllRight");

    // Run method
    executionService.startProgressJob(execution, 0);

    // Assert
    verify(scheduler, times(1)).scheduleJob(any(), any());
  }

  /**
   * Start progress job n seconds
   */
  @Test
  void startProgressJobAverageTime() throws Exception {
    // Mock and spy
    TaskExecution execution = new TaskExecution();
    execution.setStatus(TaskStatus.JOB_OK.getValue());
    execution.setDescription("AllRight");

    // Run method
    executionService.startProgressJob(execution, 12311);

    // Assert
    verify(scheduler, times(1)).scheduleJob(any(), any());
  }
}
