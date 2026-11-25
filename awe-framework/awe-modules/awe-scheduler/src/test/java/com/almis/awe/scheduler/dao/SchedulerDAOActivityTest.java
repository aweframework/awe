package com.almis.awe.scheduler.dao;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.scheduler.listener.SchedulerJobListener;
import com.almis.awe.scheduler.listener.SchedulerTriggerListener;
import com.almis.awe.scheduler.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerMetaData;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.spi.OperableTrigger;
import org.springframework.context.ApplicationContext;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.almis.awe.scheduler.constant.JobConstants.TASK_VISIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests of the scheduler data of the activity screen: upcoming fire times of the next hours, the language
 * independent engine status code and the line of the engine under the status (instance and running time).
 */
class SchedulerDAOActivityTest {

  private static final Instant NOW = Instant.parse("2026-10-15T10:00:00Z");
  private static final String GROUP = "SCHEDULED_TASK";
  // Texts of the engine line as the Spanish locale has them
  private static final Map<String, String> LOCALES = Map.of(
    "SCREEN_TEXT_ACTIVITY_ENGINE_LINE_STARTED", "Instancia Quartz {0} · en marcha desde hace {1}",
    "SCREEN_TEXT_ACTIVITY_ENGINE_LINE_STANDBY", "Instancia Quartz {0} · en pausa",
    "SCREEN_TEXT_ACTIVITY_ENGINE_LINE_STOPPED", "Instancia Quartz {0} · detenido",
    "SCREEN_TEXT_ACTIVITY_DURATION_DAYS", "{0} d {1} h",
    "SCREEN_TEXT_ACTIVITY_DURATION_HOURS", "{0} h {1} min",
    "SCREEN_TEXT_ACTIVITY_DURATION_MINUTES", "{0} min",
    "SCREEN_TEXT_ACTIVITY_DURATION_SECONDS", "{0} s");

  private Scheduler scheduler;
  private SchedulerDAO schedulerDAO;

  @BeforeEach
  void init() {
    scheduler = mock(Scheduler.class);
    schedulerDAO = newSchedulerDAO(Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private SchedulerDAO newSchedulerDAO(Clock clock) {
    SchedulerDAO dao = new SchedulerDAO(scheduler, false, mock(CalendarDAO.class), mock(TaskService.class),
      mock(SchedulerTriggerListener.class), mock(SchedulerJobListener.class),
      new SchedulerDAO.StartupHardeningConfig(clock, 1_000L));
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    given(context.getBean(AweElements.class)).willReturn(elements);
    given(elements.getLocaleWithLanguage(anyString(), any(), any(Object[].class))).willAnswer(call -> {
      Object[] arguments = call.getArguments();
      Object[] tokens = java.util.Arrays.copyOfRange(arguments, 2, arguments.length);
      return MessageFormat.format(LOCALES.getOrDefault((String) arguments[0], (String) arguments[0]), tokens);
    });
    dao.setApplicationContext(context);
    return dao;
  }

  @Test
  void upcomingCountsTheFireTimesOfTheVisibleActiveTriggersInTheWindowSkippingTheirCalendar() throws Exception {
    // Every 6 hours (00, 06, 12, 18 UTC): 4 fire times in the 24 hours after 10:00
    Trigger visible = cron("visible", true, null);
    // Same schedule with a calendar that excludes 12:00: 3 fire times
    BaseCalendar noNoon = new BaseCalendar(TimeZone.getTimeZone("UTC")) {
      @Override
      public boolean isTimeIncluded(long timeStamp) {
        return Instant.ofEpochMilli(timeStamp).atZone(ZoneOffset.UTC).getHour() != 12;
      }
    };
    Trigger withCalendar = cron("calendar", true, noNoon);
    Trigger hidden = cron("hidden", false, null);
    Trigger paused = cron("paused", true, null);
    Trigger finished = TriggerBuilder.newTrigger().withIdentity("finished", GROUP).build();
    finished.getJobDataMap().put(TASK_VISIBLE, true);
    givenTriggers(Map.of(visible, Trigger.TriggerState.NORMAL, withCalendar, Trigger.TriggerState.NORMAL,
      hidden, Trigger.TriggerState.NORMAL, paused, Trigger.TriggerState.PAUSED,
      finished, Trigger.TriggerState.COMPLETE));
    given(scheduler.getCalendar("noNoon")).willReturn(noNoon);

    assertEquals(7L, schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24)));
  }

  @Test
  void upcomingLeavesOutTheTriggersInErrorAndCountsTheBlockedOnes() throws Exception {
    // A blocked trigger waits for the running execution of its job to end and fires after it: it is counted. A
    // trigger paused while blocked cannot fire and Quartz reports it as PAUSED (left out by the first test)
    givenTriggers(Map.of(cron("normal", true, null), Trigger.TriggerState.NORMAL,
      cron("blocked", true, null), Trigger.TriggerState.BLOCKED,
      cron("error", true, null), Trigger.TriggerState.ERROR));

    assertEquals(8L, schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24)));
  }

  @Test
  void upcomingCountsAtMost1440FireTimesOfATrigger() throws Exception {
    // Every second: 86400 fire times in 24 hours, counted up to the cap of the trigger
    OperableTrigger everySecond = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity("everySecond", GROUP)
      .startAt(java.util.Date.from(NOW.minusSeconds(3600)))
      .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever()).build();
    everySecond.getJobDataMap().put(TASK_VISIBLE, true);
    everySecond.computeFirstFireTime(null);
    everySecond.setNextFireTime(everySecond.getFireTimeAfter(java.util.Date.from(NOW)));
    givenTriggers(Map.of(everySecond, Trigger.TriggerState.NORMAL, cron("visible", true, null),
      Trigger.TriggerState.NORMAL));

    assertEquals(1440L + 4L, schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24)));
  }

  @Test
  void theCapCountsIncludedFireTimesAfterTheOnesItsCalendarExcludes() throws Exception {
    // Every second with a calendar that excludes the first hour of the window: its 3600 excluded fire times are
    // visited within their own budget and the trigger still counts the cap of included ones
    BaseCalendar notFirstHour = new BaseCalendar(TimeZone.getTimeZone("UTC")) {
      @Override
      public boolean isTimeIncluded(long timeStamp) {
        return !Instant.ofEpochMilli(timeStamp).isBefore(NOW.plusSeconds(3600));
      }
    };
    OperableTrigger everySecond = (OperableTrigger) TriggerBuilder.newTrigger().withIdentity("everySecond", GROUP)
      .startAt(java.util.Date.from(NOW.minusSeconds(3600))).modifiedByCalendar("notFirstHour")
      .withSchedule(SimpleScheduleBuilder.repeatSecondlyForever()).build();
    everySecond.getJobDataMap().put(TASK_VISIBLE, true);
    everySecond.computeFirstFireTime(null);
    everySecond.setNextFireTime(java.util.Date.from(NOW.plusSeconds(3600)));
    givenTriggers(Map.of(everySecond, Trigger.TriggerState.NORMAL));
    given(scheduler.getCalendar("notFirstHour")).willReturn(notFirstHour);

    assertEquals(SchedulerDAO.UPCOMING_FIRE_TIMES_CAP, schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24)));
  }

  @Test
  void aVisibilityThatIsNotABooleanDoesNotBreakTheCount() throws Exception {
    Trigger text = cron("text", false, null);
    text.getJobDataMap().put(TASK_VISIBLE, "true");
    Trigger number = cron("number", false, null);
    number.getJobDataMap().put(TASK_VISIBLE, 1);
    givenTriggers(Map.of(text, Trigger.TriggerState.NORMAL, number, Trigger.TriggerState.NORMAL));

    assertEquals(4L, schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24)));
  }

  @Test
  void upcomingIsZeroWhenTheSchedulerIsNotRunning() throws Exception {
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.isInStandbyMode()).willReturn(true);

    assertEquals(0L, schedulerDAO.countUpcomingFireTimes(Duration.ofHours(24)));
  }

  @Test
  void metadataCarriesTheEngineStatusCodeNextToItsText() throws Exception {
    assertEquals(List.of("Stopped", "stopped"), status(false, false));
    assertEquals(List.of("Standby", "standby"), status(true, true));
    assertEquals(List.of("Started and running", "started"), status(true, false));
  }

  @Test
  void metadataTellsTheInstanceAndHowLongTheRunningEngineHasBeenRunning() throws Exception {
    assertEquals(List.of("6 d 4 h", "Instancia Quartz quartz-tesoreria-01 · en marcha desde hace 6 d 4 h"),
      engineLine(true, false, false, "quartz-tesoreria-01", NOW.minus(Duration.ofDays(6).plusHours(4).plusMinutes(30))));
    assertEquals(List.of("3 h 12 min", "Instancia Quartz quartz-tesoreria-01 · en marcha desde hace 3 h 12 min"),
      engineLine(true, false, false, "quartz-tesoreria-01", NOW.minus(Duration.ofHours(3).plusMinutes(12).plusSeconds(40))));
    assertEquals(List.of("7 min", "Instancia Quartz quartz-tesoreria-01 · en marcha desde hace 7 min"),
      engineLine(true, false, false, "quartz-tesoreria-01", NOW.minus(Duration.ofMinutes(7).plusSeconds(5))));
    assertEquals(List.of("45 s", "Instancia Quartz quartz-tesoreria-01 · en marcha desde hace 45 s"),
      engineLine(true, false, false, "quartz-tesoreria-01", NOW.minusSeconds(45)));
  }

  @Test
  void metadataTellsThePausedAndTheStoppedEngineWithoutARunningTime() throws Exception {
    assertEquals(List.of("", "Instancia Quartz quartz-tesoreria-01 · en pausa"),
      engineLine(true, true, false, "quartz-tesoreria-01", NOW.minusSeconds(45)));
    assertEquals(List.of("", "Instancia Quartz quartz-tesoreria-01 · detenido"),
      engineLine(false, false, true, "quartz-tesoreria-01", null));
  }

  @Test
  void theRunningTimeStartsAgainWhenTheEngineIsResumedOrRestartedNotWhenQuartzFirstStarted() throws Exception {
    // Quartz keeps the instant of its first start through standby and start: the line counts from the last start
    MutableClock clock = new MutableClock(NOW.minus(Duration.ofMinutes(10)));
    schedulerDAO = newSchedulerDAO(clock);
    given(scheduler.getListenerManager()).willReturn(mock(org.quartz.ListenerManager.class));
    Instant quartzStart = NOW.minus(Duration.ofDays(6));

    schedulerDAO.stop();
    schedulerDAO.start();
    clock.now = NOW;
    assertEquals("10 min", engineLine(true, false, false, "quartz-tesoreria-01", quartzStart).get(0));

    schedulerDAO.restart();
    clock.now = NOW.plusSeconds(45);
    assertEquals("45 s", engineLine(true, false, false, "quartz-tesoreria-01", quartzStart).get(0));
  }

  @Test
  void metadataNamesTheEngineByItsSchedulerNameWhenItIsNotClustered() throws Exception {
    // Quartz gives every scheduler that is not clustered the same instance id
    assertEquals("Instancia Quartz AweScheduler · en marcha desde hace 45 s",
      engineLine(true, false, false, "NON_CLUSTERED", NOW.minusSeconds(45)).get(1));
  }

  private List<String> status(boolean started, boolean standby) throws Exception {
    Map<String, CellData> row = metadataRow(started, standby, false, "quartz-tesoreria-01", null);
    return List.of(row.get("schedulerStatus").getStringValue(), row.get("schedulerStatusCode").getStringValue());
  }

  private List<String> engineLine(boolean started, boolean standby, boolean shutdown, String instanceId,
                                  Instant runningSince) throws Exception {
    Map<String, CellData> row = metadataRow(started, standby, shutdown, instanceId, runningSince);
    return List.of(row.get("schedulerRunningFor").getStringValue(), row.get("schedulerEngineLine").getStringValue());
  }

  private Map<String, CellData> metadataRow(boolean started, boolean standby, boolean shutdown, String instanceId,
                                            Instant runningSince) throws Exception {
    SchedulerMetaData metadata = mock(SchedulerMetaData.class);
    given(metadata.isStarted()).willReturn(started);
    given(metadata.isInStandbyMode()).willReturn(standby);
    given(metadata.isShutdown()).willReturn(shutdown);
    given(metadata.getSchedulerInstanceId()).willReturn(instanceId);
    given(metadata.getSchedulerName()).willReturn("AweScheduler");
    given(metadata.getRunningSince()).willReturn(runningSince == null ? null : java.util.Date.from(runningSince));
    given(metadata.getJobStoreClass()).willAnswer(call -> Object.class);
    given(metadata.getThreadPoolClass()).willAnswer(call -> Object.class);
    given(scheduler.getMetaData()).willReturn(metadata);
    return schedulerDAO.getSchedulerMetadata().getDataList().getRows().get(0);
  }

  /**
   * Clock whose instant the test moves
   */
  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private Trigger cron(String name, boolean visible, BaseCalendar calendar) {
    TriggerBuilder<?> builder = TriggerBuilder.newTrigger().withIdentity(name, GROUP)
      .startAt(java.util.Date.from(NOW.minusSeconds(3600)))
      .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0/6 * * ?").inTimeZone(TimeZone.getTimeZone("UTC")));
    if (calendar != null) {
      builder.modifiedByCalendar("noNoon");
    }
    OperableTrigger trigger = (OperableTrigger) builder.build();
    if (visible) {
      trigger.getJobDataMap().put(TASK_VISIBLE, true);
    }
    // As the scheduler does when the trigger is scheduled
    trigger.computeFirstFireTime(calendar);
    trigger.setNextFireTime(trigger.getFireTimeAfter(java.util.Date.from(NOW)));
    while (calendar != null && !calendar.isTimeIncluded(trigger.getNextFireTime().getTime())) {
      trigger.setNextFireTime(trigger.getFireTimeAfter(trigger.getNextFireTime()));
    }
    return trigger;
  }

  private void givenTriggers(Map<Trigger, Trigger.TriggerState> triggers) throws Exception {
    given(scheduler.isStarted()).willReturn(true);
    given(scheduler.isInStandbyMode()).willReturn(false);
    given(scheduler.getTriggerGroupNames()).willReturn(List.of(GROUP));
    List<TriggerKey> keys = new ArrayList<>();
    for (Map.Entry<Trigger, Trigger.TriggerState> entry : triggers.entrySet()) {
      keys.add(entry.getKey().getKey());
      given(scheduler.getTrigger(entry.getKey().getKey())).willReturn(entry.getKey());
      given(scheduler.getTriggerState(entry.getKey().getKey())).willReturn(entry.getValue());
    }
    given(scheduler.getTriggerKeys(any())).willReturn(Set.copyOf(keys));
  }
}
