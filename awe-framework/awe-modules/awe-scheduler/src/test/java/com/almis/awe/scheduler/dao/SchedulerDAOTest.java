package com.almis.awe.scheduler.dao;

import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.listener.SchedulerJobListener;
import com.almis.awe.scheduler.listener.SchedulerTriggerListener;
import com.almis.awe.scheduler.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;
import org.quartz.ListenerManager;
import org.quartz.impl.matchers.GroupMatcher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_VISIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

  @Mock
  private ListenerManager listenerManager;

  private static Clock fixedClock(long time) {
    return Clock.fixed(Instant.ofEpochMilli(time), ZoneOffset.UTC);
  }

  @BeforeEach
  void setUp() throws Exception {
    schedulerDAO = new SchedulerDAO(scheduler, true, calendarDAO, taskService, schedulerTriggerListener, schedulerJobListener);
    lenient().when(scheduler.getListenerManager()).thenReturn(listenerManager);
    lenient().when(listenerManager.getTriggerListeners()).thenReturn(Collections.emptyList());
    lenient().when(listenerManager.getJobListeners()).thenReturn(Collections.emptyList());
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

  @Test
  void startNoQuartzLoadsDataBeforeStartingScheduler() throws Exception {
    given(scheduler.isStarted()).willReturn(false);

    schedulerDAO.startNoQuartz();

    InOrder inOrder = inOrder(calendarDAO, taskService, scheduler);
    inOrder.verify(calendarDAO).loadSchedulerCalendar();
    inOrder.verify(taskService).updateInterruptedTasks();
    inOrder.verify(taskService).loadSchedulerTasks();
    inOrder.verify(scheduler).start();
  }

  @Test
  void startNoQuartzKeepsRunningSchedulerInStandbyWhileLoading() throws Exception {
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.isInStandbyMode()).willReturn(false);

    schedulerDAO.startNoQuartz();

    InOrder inOrder = inOrder(scheduler, calendarDAO, taskService);
    inOrder.verify(scheduler).standby();
    inOrder.verify(calendarDAO).loadSchedulerCalendar();
    inOrder.verify(taskService).updateInterruptedTasks();
    inOrder.verify(taskService).loadSchedulerTasks();
    inOrder.verify(scheduler).start();
  }

  @Test
  void startNoQuartzDefersDueScheduledTriggersBeforeStartingScheduler() throws Exception {
    Date originalStart = new Date(System.currentTimeMillis() - 1_000L);
    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity("1", "SCHEDULED_TASK")
      .startAt(originalStart)
      .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(5).repeatForever())
      .build();

    given(scheduler.isStarted()).willReturn(false);
    given(scheduler.getTriggerKeys(any())).willReturn(Collections.singleton(trigger.getKey()), Collections.emptySet());
    given(scheduler.getTrigger(trigger.getKey())).willReturn(trigger);

    schedulerDAO.startNoQuartz();

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    InOrder inOrder = inOrder(calendarDAO, taskService, scheduler);
    inOrder.verify(calendarDAO).loadSchedulerCalendar();
    inOrder.verify(taskService).updateInterruptedTasks();
    inOrder.verify(taskService).loadSchedulerTasks();
    inOrder.verify(scheduler).rescheduleJob(eq(trigger.getKey()), triggerCaptor.capture());
    inOrder.verify(scheduler).start();
    verify(scheduler, times(2)).getTriggerKeys(any());

    Trigger adjustedTrigger = triggerCaptor.getValue();
    assertTrue(adjustedTrigger.getStartTime().after(originalStart));
    assertTrue(adjustedTrigger.getStartTime().after(new Date(System.currentTimeMillis() - 500L)));
  }

  @Test
  void startNoQuartzKeepsPausedPlannedTriggersPausedDuringStartupHardening() throws Exception {
    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity("1", "SCHEDULED_TASK")
      .startAt(new Date(System.currentTimeMillis() - 1_000L))
      .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(5).repeatForever())
      .build();

    given(scheduler.isStarted()).willReturn(false);
    given(scheduler.getTriggerKeys(any())).willReturn(Collections.singleton(trigger.getKey()), Collections.emptySet());
    given(scheduler.getTrigger(trigger.getKey())).willReturn(trigger);
    given(scheduler.getTriggerState(trigger.getKey())).willReturn(Trigger.TriggerState.PAUSED);

    schedulerDAO.startNoQuartz();

    verify(scheduler, never()).rescheduleJob(eq(trigger.getKey()), any(Trigger.class));
    verify(scheduler, never()).deleteJob(trigger.getJobKey());
    verify(scheduler).start();
  }

  @Test
  void startNoQuartzDefersTriggersInsideSweepToStartGuardWindow() throws Exception {
    long startupTime = System.currentTimeMillis();
    SchedulerDAO guardedSchedulerDAO = new SchedulerDAO(scheduler, true, calendarDAO, taskService,
      schedulerTriggerListener, schedulerJobListener,
      new SchedulerDAO.StartupHardeningConfig(fixedClock(startupTime), 1_000L));
    Date originalStart = new Date(startupTime + 500L);
    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity("1", "SCHEDULED_TASK")
      .startAt(originalStart)
      .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(5).repeatForever())
      .build();

    given(scheduler.isStarted()).willReturn(false);
    given(scheduler.getTrigger(trigger.getKey())).willReturn(trigger);
    given(scheduler.getTriggerState(trigger.getKey())).willReturn(Trigger.TriggerState.NORMAL);
    given(scheduler.getTriggerKeys(any())).willAnswer(invocation -> {
      GroupMatcher<TriggerKey> matcher = invocation.getArgument(0);
      return "SCHEDULED_TASK".equals(matcher.getCompareToValue())
        ? Collections.singleton(trigger.getKey())
        : Collections.emptySet();
    });

    guardedSchedulerDAO.startNoQuartz();

    ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler).rescheduleJob(eq(trigger.getKey()), triggerCaptor.capture());
    verify(scheduler).start();

    Trigger adjustedTrigger = triggerCaptor.getValue();
    assertTrue(adjustedTrigger.getStartTime().after(new Date(startupTime + 1_000L)));
  }

  @Test
  void startNoQuartzRemovesExpiredOneShotScheduledTriggersBeforeStartingScheduler() throws Exception {
    Trigger trigger = TriggerBuilder.newTrigger()
      .withIdentity("1", "SCHEDULED_TASK")
      .startAt(new Date(System.currentTimeMillis() - 1_000L))
      .build();

    given(scheduler.isStarted()).willReturn(false);
    given(scheduler.getTriggerKeys(any())).willReturn(Collections.singleton(trigger.getKey()), Collections.emptySet());
    given(scheduler.getTrigger(trigger.getKey())).willReturn(trigger);

    schedulerDAO.startNoQuartz();

    InOrder inOrder = inOrder(calendarDAO, taskService, scheduler);
    inOrder.verify(calendarDAO).loadSchedulerCalendar();
    inOrder.verify(taskService).updateInterruptedTasks();
    inOrder.verify(taskService).loadSchedulerTasks();
    inOrder.verify(scheduler).deleteJob(trigger.getJobKey());
    inOrder.verify(scheduler).start();
    verify(scheduler, times(2)).getTriggerKeys(any());
    verify(scheduler, never()).rescheduleJob(eq(trigger.getKey()), any(Trigger.class));
  }
}
