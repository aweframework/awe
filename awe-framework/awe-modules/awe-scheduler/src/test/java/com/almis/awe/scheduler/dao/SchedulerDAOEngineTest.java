package com.almis.awe.scheduler.dao;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.Engine;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineStatus;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineTrigger;
import com.almis.awe.scheduler.listener.SchedulerJobListener;
import com.almis.awe.scheduler.listener.SchedulerTriggerListener;
import com.almis.awe.scheduler.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.context.ApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * What the scheduler tells of its engine for the «Motor» tab of the configuration screen (#993): its status (running,
 * paused, stopped), its threads in use, whether its last start failed, and the schedules of the tasks it holds (the
 * triggers of the scheduled and file tracking tasks, not the ones of a launch on demand nor the internal ones)
 */
class SchedulerDAOEngineTest {

  private static final Instant NOW = Instant.parse("2026-11-17T10:45:00Z");

  private Scheduler scheduler;
  private SchedulerDAO schedulerDAO;

  @BeforeEach
  void init() throws Exception {
    scheduler = mock(Scheduler.class);
    schedulerDAO = new SchedulerDAO(scheduler, false, mock(CalendarDAO.class), mock(TaskService.class),
      mock(SchedulerTriggerListener.class), mock(SchedulerJobListener.class),
      new SchedulerDAO.StartupHardeningConfig(Clock.fixed(NOW, ZoneOffset.UTC), 1_000L));
    given(scheduler.getListenerManager()).willReturn(mock(ListenerManager.class));
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    given(context.getBean(AweElements.class)).willReturn(elements);
    given(elements.getLocaleWithLanguage(anyString(), any())).willAnswer(call -> call.getArgument(0));
    schedulerDAO.setApplicationContext(context);
  }

  @Test
  void theEngineTellsItsStatusAndItsThreadsInUse() throws Exception {
    givenMetadata(true, false, false);
    given(scheduler.getCurrentlyExecutingJobs()).willReturn(List.of(mock(JobExecutionContext.class)));

    Engine running = schedulerDAO.getEngine();

    assertThat(running.status()).isEqualTo(EngineStatus.RUNNING);
    assertThat(running.threadsInUse()).isEqualTo(1);
    assertThat(running.poolSize()).isEqualTo(10);
    assertThat(running.instance()).isEqualTo("AweScheduler");
    assertThat(running.lastStartFailed()).isFalse();
    givenMetadata(true, true, false);
    assertThat(schedulerDAO.getEngine().status()).isEqualTo(EngineStatus.PAUSED);
    givenMetadata(false, true, false);
    assertThat(schedulerDAO.getEngine().status()).isEqualTo(EngineStatus.STOPPED);
    givenMetadata(true, true, true);
    assertThat(schedulerDAO.getEngine().status()).isEqualTo(EngineStatus.STOPPED);
  }

  @Test
  void aFailedStartIsToldUntilTheEngineStartsAgain() throws Exception {
    givenMetadata(false, true, false);
    willThrow(new SchedulerException("boom")).given(scheduler).start();

    assertThrows(AWException.class, () -> schedulerDAO.startNoQuartz());
    assertThat(schedulerDAO.getEngine().lastStartFailed()).isTrue();

    willThrow(new IllegalStateException("boom")).willAnswer(call -> null).given(scheduler).start();
    assertThrows(IllegalStateException.class, () -> schedulerDAO.startNoQuartz());
    assertThat(schedulerDAO.getEngine().lastStartFailed()).isTrue();
    schedulerDAO.startNoQuartz();
    assertThat(schedulerDAO.getEngine().lastStartFailed()).isFalse();
  }

  @Test
  void theSchedulesAreTheTriggersOfTheScheduledAndFileTrackingTasks() throws Exception {
    Trigger scheduled = trigger("10", "SCHEDULED_TASK");
    Trigger file = trigger("20", "FILE_TRACKING_TASK");
    given(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("SCHEDULED_TASK")))
      .willReturn(Set.of(scheduled.getKey()));
    given(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("FILE_TRACKING_TASK")))
      .willReturn(Set.of(file.getKey(), new TriggerKey("gone", "FILE_TRACKING_TASK")));
    given(scheduler.getTrigger(scheduled.getKey())).willReturn(scheduled);
    given(scheduler.getTrigger(file.getKey())).willReturn(file);
    given(scheduler.getTriggerState(scheduled.getKey())).willReturn(Trigger.TriggerState.NORMAL);
    given(scheduler.getTriggerState(file.getKey())).willReturn(Trigger.TriggerState.PAUSED);
    given(scheduler.getTriggerGroupNames()).willReturn(List.of("SCHEDULED_TASK", "FILE_TRACKING_TASK",
      "MANUAL_TASK", "TIMEOUT"));

    List<EngineTrigger> triggers = schedulerDAO.getEngineTriggers();

    assertThat(triggers).extracting(EngineTrigger::taskId).containsExactlyInAnyOrder(10, 20);
    assertThat(triggers).extracting(EngineTrigger::state).containsExactlyInAnyOrder("NORMAL", "PAUSED");
    assertThat(triggers).extracting(EngineTrigger::calendar).containsOnly("3");
  }

  @Test
  void oneTriggerThatCannotBeReadIsSkippedAndTheOthersAreStillRead() throws Exception {
    Trigger readable = trigger("10", "SCHEDULED_TASK");
    TriggerKey broken = new TriggerKey("20", "SCHEDULED_TASK");
    TriggerKey brokenState = new TriggerKey("30", "SCHEDULED_TASK");
    Trigger stateless = trigger("30", "SCHEDULED_TASK");
    given(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("SCHEDULED_TASK")))
      .willReturn(Set.of(readable.getKey(), broken, brokenState));
    given(scheduler.getTriggerKeys(GroupMatcher.triggerGroupEquals("FILE_TRACKING_TASK"))).willReturn(Set.of());
    given(scheduler.getTrigger(readable.getKey())).willReturn(readable);
    given(scheduler.getTrigger(broken)).willThrow(new SchedulerException("cannot deserialize the trigger"));
    given(scheduler.getTrigger(brokenState)).willReturn(stateless);
    given(scheduler.getTriggerState(readable.getKey())).willReturn(Trigger.TriggerState.NORMAL);
    given(scheduler.getTriggerState(brokenState)).willThrow(new IllegalStateException("store unavailable"));

    List<EngineTrigger> triggers = schedulerDAO.getEngineTriggers();

    assertThat(triggers).extracting(EngineTrigger::taskId).containsExactly(10);
  }

  private void givenMetadata(boolean started, boolean standby, boolean shutdown) throws Exception {
    SchedulerMetaData metadata = mock(SchedulerMetaData.class);
    given(metadata.isStarted()).willReturn(started);
    given(metadata.isInStandbyMode()).willReturn(standby);
    given(metadata.isShutdown()).willReturn(shutdown);
    given(metadata.getSchedulerInstanceId()).willReturn("NON_CLUSTERED");
    given(metadata.getSchedulerName()).willReturn("AweScheduler");
    given(metadata.getThreadPoolSize()).willReturn(10);
    given(metadata.getVersion()).willReturn("2.5.2");
    given(metadata.getJobStoreClass()).willAnswer(call -> Object.class);
    given(metadata.getThreadPoolClass()).willAnswer(call -> Object.class);
    given(scheduler.getMetaData()).willReturn(metadata);
    given(scheduler.isStarted()).willReturn(started);
    given(scheduler.isInStandbyMode()).willReturn(standby);
  }

  private static Trigger trigger(String name, String group) {
    return TriggerBuilder.newTrigger().withIdentity(name, group).modifiedByCalendar("3")
      .startAt(Date.from(NOW.plusSeconds(60))).build();
  }
}
