package com.almis.awe.scheduler.dao;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.listener.SchedulerJobListener;
import com.almis.awe.scheduler.listener.SchedulerTriggerListener;
import com.almis.awe.scheduler.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.util.*;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_VISIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class used for testing SchedulerDAO class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class SchedulerDAOTest {

  private SchedulerDAO schedulerDAO;

  @Mock
  private Scheduler scheduler;

  @Mock
  private CalendarDAO calendarDAO;

  @Mock
  private TaskService taskService;

  @Mock
  private SchedulerTriggerListener schedulerTriggerListener;

  @Mock
  private SchedulerJobListener schedulerJobListener;

  @Mock
  private JobExecutionContext jobExecutionContext;

  @BeforeEach
  void setUp() {
    schedulerDAO = new SchedulerDAO(scheduler, true, calendarDAO, taskService, schedulerTriggerListener, schedulerJobListener);
  }

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    assertNotNull(schedulerDAO);
  }

  /**
   * Get executing jobs without scheduler configured
   */
  @Test
  void getCurrentlyExecutingJobsNoScheduler() throws Exception {
    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();
    // Assert
    assertNotNull(serviceData);
  }

  /**
   * Get executing jobs without scheduler configured
   */
  @Test
  void getCurrentlyExecutingJobsStandby() throws Exception {
    // Mock and spy
    Trigger trigger = mock(Trigger.class);
    when(jobExecutionContext.getTrigger()).thenReturn(trigger);
    when(trigger.getKey()).thenReturn(new TriggerKey("test", "test"));
    List<JobExecutionContext> jobExecutionContextList = Collections.singletonList(jobExecutionContext);
    when(scheduler.getTriggerState(any())).thenReturn(Trigger.TriggerState.COMPLETE);
    given(scheduler.getCurrentlyExecutingJobs()).willReturn(jobExecutionContextList);
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(true);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertEquals(1, serviceData.getDataList().getRecords());
  }

  /**
   * Get executing jobs without scheduler configured
   */
  @Test
  void getCurrentlyExecutingJobsNoStarted() throws Exception {
    // Mock and spy
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(false);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertEquals(0, serviceData.getDataList().getRecords());
  }

  /**
   * Get executing jobs with scheduler configured
   */
  @Test
  void getCurrentlyExecutingJobsWithScheduler() throws Exception {
    // Mock and spy
    JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    given(jobExecutionContext.getTrigger()).willReturn(TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build());
    List<JobExecutionContext> jobExecutionContextList = Collections.singletonList(jobExecutionContext);
    given(scheduler.getCurrentlyExecutingJobs()).willReturn(jobExecutionContextList);
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.getTriggerState(any())).willReturn(Trigger.TriggerState.NORMAL);

    // Run method
    ServiceData serviceData = schedulerDAO.getCurrentlyExecutingJobs();

    // Assert
    assertEquals(1, serviceData.getDataList().getRecords());
  }

  /**
   * Get configured jobs with scheduler configured
   */
  @Test
  void getConfiguredJobsWithScheduler() throws Exception {
    // Mock and spy
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.getTriggerGroupNames()).willReturn(Arrays.asList("1", "2", "3", "TEST_GROUP"));
    Set<TriggerKey> triggerKeySet = new HashSet<>();
    triggerKeySet.add(new TriggerKey("1", "TEST_GROUP"));
    given(scheduler.getTriggerKeys(any())).willReturn(triggerKeySet);
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity("1", "TEST_GROUP").build();
    trigger.getJobDataMap().put(TASK_VISIBLE, true);
    given(scheduler.getTrigger(any())).willReturn(trigger);

    // Run method
    ServiceData serviceData = schedulerDAO.getConfiguredJobs();

    // Assert
    assertEquals(4, serviceData.getDataList().getRecords());
  }
}
