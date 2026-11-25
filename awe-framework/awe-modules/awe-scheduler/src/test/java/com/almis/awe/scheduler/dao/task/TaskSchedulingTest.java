package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.filechecker.FileChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerUtils;
import org.quartz.spi.OperableTrigger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Refresh of an edited task in the scheduler (#993, S8): the new trigger and job are built and checked before the old
 * job is removed, only the job of the task groups is removed, the old job and trigger come back from their next fire
 * when the new ones cannot be added (a paused one does not), an inactive task or one that never fires again is left
 * without trigger until it is activated, and nothing else of the task changes (its executions, its logs and
 * the files it has seen are kept)
 */
@ExtendWith(MockitoExtension.class)
class TaskSchedulingTest {

  private static final Instant NOW = Instant.parse("2026-11-19T10:00:00Z");
  private static final String SCHEDULED = "SCHEDULED_TASK";
  private static final String FILE = "FILE_TRACKING_TASK";

  @Mock
  private Scheduler scheduler;
  @Mock
  private FileChecker fileChecker;
  @Mock
  private TaskRepository taskRepository;
  @Mock
  private TaskExecutionRepository executionRepository;
  @Mock
  private ServiceConfig serviceConfig;

  private TaskScheduling scheduling;

  @BeforeEach
  void setUp() {
    scheduling = new TaskScheduling(scheduler, fileChecker, taskRepository, executionRepository, serviceConfig,
      Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void refreshingAnEditedTaskReplacesItsJobInTheTaskGroupsWithoutPurgingItsExecutionsNorItsFiles() throws Exception {
    // Scheduled before, a file task now (another group), active
    TriggerKey oldKey = new TriggerKey("7", SCHEDULED);
    Trigger oldTrigger = TriggerBuilder.newTrigger().withIdentity(oldKey).forJob("job-7", "COMMAND").build();
    Trigger trigger = everyHourFrom("7", FILE, NOW.plus(Duration.ofHours(1)));
    JobDetail job = job("job-7-new");
    given(taskRepository.loadTask(7)).willReturn(new Task().setTaskId(7).setLaunchType(2).setActive(true)
      .setTrigger(trigger).setJob(job));
    given(scheduler.checkExists(oldKey)).willReturn(true);
    given(scheduler.getTrigger(oldKey)).willReturn(oldTrigger);

    scheduling.refreshTask(7);

    verify(scheduler).deleteJob(oldTrigger.getJobKey());
    verify(scheduler).scheduleJob(job, trigger);
    verify(scheduler, never()).pauseTrigger(any());
    // Only the groups of a task: the triggers of the other groups (timeouts, reports...) are not touched
    verify(scheduler, never()).getTriggerGroupNames();
    verifyNoInteractions(executionRepository);
    verify(taskRepository, never()).clearFileModificationTable(any());
  }

  @Test
  void refreshingATaskThatIsManualNowRemovesItsTrigger() throws Exception {
    TriggerKey oldKey = new TriggerKey("8", SCHEDULED);
    Trigger oldTrigger = TriggerBuilder.newTrigger().withIdentity(oldKey).forJob("job-8", "COMMAND").build();
    given(taskRepository.loadTask(8)).willReturn(new Task().setTaskId(8).setLaunchType(0).setActive(true));
    given(scheduler.checkExists(oldKey)).willReturn(true);
    given(scheduler.getTrigger(oldKey)).willReturn(oldTrigger);

    scheduling.refreshTask(8);

    verify(scheduler).deleteJob(oldTrigger.getJobKey());
    verify(scheduler, never()).scheduleJob(any(), any());
    verifyNoInteractions(executionRepository);
  }

  @Test
  void aTaskThatCannotBeLoadedOrWhoseCalendarIsMissingKeepsItsJob() throws Exception {
    given(taskRepository.loadTask(7)).willThrow(new AWException("No task data found"));
    Trigger withCalendar = TriggerBuilder.newTrigger().withIdentity("9", SCHEDULED).startAt(Date.from(NOW))
      .modifiedByCalendar("Holidays").withSchedule(SimpleScheduleBuilder.repeatHourlyForever()).build();
    given(taskRepository.loadTask(9)).willReturn(new Task().setTaskId(9).setLaunchType(1).setActive(true)
      .setTrigger(withCalendar).setJob(job("job-9")));

    assertThrows(AWException.class, () -> scheduling.refreshTask(7));
    assertThrows(AWException.class, () -> scheduling.refreshTask(9));

    verify(scheduler, never()).deleteJob(any());
    verify(scheduler, never()).scheduleJob(any(), any());
  }

  @Test
  void anActiveTaskWithoutAnyFireAfterNowIsLeftWithoutTrigger() throws Exception {
    // Once an hour ago, and a cron of a year that has passed: added, Quartz would fire the first one as a misfire
    TriggerKey oldKey = new TriggerKey("7", SCHEDULED);
    Trigger oldTrigger = TriggerBuilder.newTrigger().withIdentity(oldKey).forJob("job-7", "COMMAND").build();
    Trigger past = TriggerBuilder.newTrigger().withIdentity("7", SCHEDULED)
      .startAt(Date.from(NOW.minus(Duration.ofHours(1)))).build();
    given(taskRepository.loadTask(7)).willReturn(new Task().setTaskId(7).setLaunchType(1).setActive(true)
      .setTrigger(past).setJob(job("job-7-new")));
    Trigger never = TriggerBuilder.newTrigger().withIdentity("9", SCHEDULED).startAt(Date.from(NOW))
      .withSchedule(CronScheduleBuilder.cronSchedule("0 0 3 1 1 ? 2000")).build();
    given(taskRepository.loadTask(9)).willReturn(new Task().setTaskId(9).setLaunchType(1).setActive(true)
      .setTrigger(never).setJob(job("job-9")));
    given(scheduler.checkExists(any(TriggerKey.class))).willAnswer(call -> oldKey.equals(call.getArgument(0)));
    given(scheduler.getTrigger(oldKey)).willReturn(oldTrigger);

    scheduling.refreshTask(7);
    scheduling.refreshTask(9);

    verify(scheduler).deleteJob(oldTrigger.getJobKey());
    verify(scheduler, never()).scheduleJob(any(), any());
    verify(scheduler, never()).pauseTrigger(any());
  }

  @Test
  void whenTheNewJobCannotBeAddedAPreviouslyPausedTriggerIsNotAddedAgain() throws Exception {
    TriggerKey oldKey = new TriggerKey("7", SCHEDULED);
    Trigger oldTrigger = TriggerBuilder.newTrigger().withIdentity(oldKey).forJob("job-7", "COMMAND").build();
    JobDetail oldJob = job("job-7");
    Trigger trigger = everyHourFrom("7", SCHEDULED, NOW.plus(Duration.ofHours(1)));
    JobDetail job = job("job-7-new");
    given(taskRepository.loadTask(7)).willReturn(new Task().setTaskId(7).setLaunchType(1).setActive(true)
      .setTrigger(trigger).setJob(job));
    given(scheduler.checkExists(oldKey)).willReturn(true);
    given(scheduler.getTrigger(oldKey)).willReturn(oldTrigger);
    given(scheduler.getJobDetail(oldTrigger.getJobKey())).willReturn(oldJob);
    given(scheduler.getTriggerState(oldKey)).willReturn(Trigger.TriggerState.PAUSED);
    willThrow(new SchedulerException("store failure")).given(scheduler).scheduleJob(job, trigger);

    assertThrows(AWException.class, () -> scheduling.refreshTask(7));

    InOrder order = inOrder(scheduler);
    order.verify(scheduler).deleteJob(oldTrigger.getJobKey());
    order.verify(scheduler).scheduleJob(job, trigger);
    // Left without trigger, as an inactive task: activating it adds it from the stored task
    verify(scheduler, never()).scheduleJob(org.mockito.ArgumentMatchers.eq(oldJob), any());
    verify(scheduler, never()).pauseTrigger(any());
  }

  @Test
  void whenTheNewJobCannotBeAddedAPreviouslyActiveTriggerComesBackFromItsNextFireWithoutAnyPastFire()
    throws Exception {
    // Every hour since 09:30 of the day before: its next fire was 10:30, and 09:30 had misfired
    Instant start = NOW.minus(Duration.ofHours(24)).minus(Duration.ofMinutes(30));
    Instant next = NOW.plus(Duration.ofMinutes(30));
    Trigger ahead = storedEveryHour("7", start, next);
    Trigger misfired = storedEveryHour("8", start, NOW.minus(Duration.ofMinutes(30)));
    for (Trigger old : List.of(ahead, misfired)) {
      int taskId = Integer.parseInt(old.getKey().getName());
      Trigger trigger = everyHourFrom(old.getKey().getName(), FILE, NOW.plus(Duration.ofHours(1)));
      given(taskRepository.loadTask(taskId)).willReturn(new Task().setTaskId(taskId).setLaunchType(2).setActive(true)
        .setTrigger(trigger).setJob(job("job-" + taskId + "-new")));
      given(scheduler.getTrigger(old.getKey())).willReturn(old);
      given(scheduler.getJobDetail(old.getJobKey())).willReturn(job(old.getJobKey().getName()));
      given(scheduler.getTriggerState(old.getKey())).willReturn(Trigger.TriggerState.NORMAL);
    }
    given(scheduler.checkExists(any(TriggerKey.class)))
      .willAnswer(call -> SCHEDULED.equals(((TriggerKey) call.getArgument(0)).getGroup()));
    // The new file trigger cannot be stored
    given(scheduler.scheduleJob(any(), any())).willAnswer(call -> {
      if (FILE.equals(((Trigger) call.getArgument(1)).getKey().getGroup())) {
        throw new SchedulerException("store failure");
      }
      return null;
    });

    assertThrows(AWException.class, () -> scheduling.refreshTask(7));
    assertThrows(AWException.class, () -> scheduling.refreshTask(8));

    ArgumentCaptor<Trigger> restored = ArgumentCaptor.forClass(Trigger.class);
    verify(scheduler, never()).pauseTrigger(any());
    verify(scheduler, org.mockito.Mockito.times(4)).scheduleJob(any(), restored.capture());
    List<Trigger> back = restored.getAllValues().stream()
      .filter(added -> SCHEDULED.equals(added.getKey().getGroup())).toList();
    assertEquals(List.of(ahead.getKey(), misfired.getKey()), back.stream().map(Trigger::getKey).toList());
    for (Trigger added : back) {
      // The first fire Quartz computes when it stores it is the next one after now, in the phase of the old trigger
      List<Date> fires = TriggerUtils.computeFireTimes((OperableTrigger) added, null, 2);
      assertEquals(List.of(Date.from(next), Date.from(next.plus(Duration.ofHours(1)))), fires);
      assertEquals(ahead.getJobKey().getGroup(), added.getJobKey().getGroup());
    }
  }

  @Test
  void anInactiveTaskIsLeftWithoutTriggerWhateverItsNextFire() throws Exception {
    // Its first fire is an hour ahead, and one every minute: neither is ever live
    Trigger later = everyHourFrom("7", SCHEDULED, NOW.plus(Duration.ofHours(1)));
    given(taskRepository.loadTask(7)).willReturn(new Task().setTaskId(7).setLaunchType(1).setActive(false)
      .setTrigger(later).setJob(job("job-7")));
    TriggerKey oldKey = new TriggerKey("8", SCHEDULED);
    Trigger oldTrigger = TriggerBuilder.newTrigger().withIdentity(oldKey).forJob("job-8", "COMMAND").build();
    Trigger everyMinute = TriggerBuilder.newTrigger().withIdentity("8", SCHEDULED).startAt(Date.from(NOW))
      .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever()).build();
    given(taskRepository.loadTask(8)).willReturn(new Task().setTaskId(8).setLaunchType(1).setActive(false)
      .setTrigger(everyMinute).setJob(job("job-8-new")));
    given(scheduler.checkExists(any(TriggerKey.class))).willAnswer(call -> oldKey.equals(call.getArgument(0)));
    given(scheduler.getTrigger(oldKey)).willReturn(oldTrigger);

    scheduling.refreshTask(7);
    scheduling.refreshTask(8);

    verify(scheduler).deleteJob(oldTrigger.getJobKey());
    verify(scheduler, never()).scheduleJob(any(), any());
    verify(scheduler, never()).pauseTrigger(any());
  }

  @Test
  void activatingATaskWithoutTriggerAddsItFromTheStoredTask() throws Exception {
    Trigger trigger = everyHourFrom("7", SCHEDULED, NOW.plus(Duration.ofHours(1)));
    JobDetail job = job("job-7");
    given(scheduler.checkExists(trigger.getKey())).willReturn(false);

    scheduling.resumeTask(new Task().setTaskId(7).setLaunchType(1).setActive(true).setTrigger(trigger).setJob(job));

    verify(scheduler).scheduleJob(job, trigger);
    verify(scheduler, never()).rescheduleJob(any(), any());
  }

  private static Trigger everyHourFrom(String name, String group, Instant start) {
    return TriggerBuilder.newTrigger().withIdentity(name, group).startAt(Date.from(start))
      .withSchedule(SimpleScheduleBuilder.repeatHourlyForever()).build();
  }

  /**
   * Trigger as the scheduler stores it: every hour from its start, with the next fire it had computed
   */
  private static Trigger storedEveryHour(String name, Instant start, Instant nextFire) {
    OperableTrigger trigger = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity(name, SCHEDULED)
      .forJob("job-" + name, "COMMAND").startAt(Date.from(start))
      .withSchedule(SimpleScheduleBuilder.repeatHourlyForever()).build();
    trigger.setNextFireTime(Date.from(nextFire));
    return trigger;
  }

  private static JobDetail job(String name) {
    return JobBuilder.newJob(Job.class).withIdentity(name, "COMMAND").build();
  }
}
