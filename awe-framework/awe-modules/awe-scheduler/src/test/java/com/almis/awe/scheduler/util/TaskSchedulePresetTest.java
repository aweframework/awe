package com.almis.awe.scheduler.util;

import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.builder.cron.PatternBuilder;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Presets of the «Repetition» of the task editor (#993): each preset writes the same stored fields of a scheduled task
 * (repeat type and number, days of the week as the editor numbers them, days, hours, minutes, seconds, date and time),
 * so the trigger built from them fires as the preset says; and a stored task opens on the preset its fields match,
 * else on the custom one
 */
class TaskSchedulePresetTest {

  private static final Date ONCE_DATE = Date.from(LocalDate.of(2027, 3, 15).atStartOfDay(ZoneId.systemDefault())
    .toInstant());

  @Test
  void everyNMinutesAndEveryNHoursAreIntervals() throws Exception {
    Schedule minutes = TaskSchedulePreset.apply(values("MIN").setEvery(15), new Schedule());
    Schedule hours = TaskSchedulePreset.apply(values("HOU").setEvery(2), new Schedule());

    assertEquals(1, minutes.getRepeatType());
    assertEquals(15, minutes.getRepeatNumber());
    assertEquals(15L * 60 * 1000, ((SimpleTrigger) trigger(minutes)).getRepeatInterval());
    assertEquals(2, hours.getRepeatType());
    assertEquals(2L * 3600 * 1000, ((SimpleTrigger) trigger(hours)).getRepeatInterval());
  }

  @Test
  void dailyWorkdaysWeeklyAndMonthlyBuildTheirCron() throws Exception {
    assertEquals("0 30 22 ? * * *", cron(values("DAY").setTime("22:30")));
    assertEquals("0 30 22 ? * 2,3,4,5,6 *", cron(values("WRK").setTime("22:30:00")));
    // Weekly: the days of the editor (Monday 1 … Saturday 6, Sunday 0) become the Quartz days
    assertEquals("15 5 8 ? * 2,4,1 *", cron(values("WEK").setTime("08:05:15").setWeekDays(List.of("1", "3", "0"))));
    assertEquals("0 0 6 15 * ? *", cron(values("MON").setTime("06:00").setMonthDay(15)));
  }

  @Test
  void aPresetWritesOnlyItsFieldsAndKeepsTheCalendarAndTheRange() {
    Schedule stored = new Schedule().setCalendarId(3).setInitialDate(ONCE_DATE).setInitialTime("00:00:00")
      .setYearList(List.of("2027")).setMonthList(List.of("2")).setHourList(List.of("4", "5")).setDate(ONCE_DATE)
      .setTime("10:00:00");

    Schedule weekly = TaskSchedulePreset.apply(values("WEK").setTime("07:45").setWeekDays(List.of("5")), stored);

    assertEquals(7, weekly.getRepeatType());
    assertNull(weekly.getRepeatNumber());
    assertEquals(List.of("5"), weekly.getWeekDayList());
    assertEquals(List.of("7"), weekly.getHourList());
    assertEquals(List.of("45"), weekly.getMinuteList());
    assertEquals(List.of("0"), weekly.getSecondList());
    assertEquals(List.of(), weekly.getYearList());
    assertEquals(List.of(), weekly.getMonthList());
    assertEquals(List.of(), weekly.getWeekList());
    assertEquals(List.of(), weekly.getDayList());
    assertNull(weekly.getDate());
    assertNull(weekly.getTime());
    assertEquals(3, weekly.getCalendarId());
    assertEquals(ONCE_DATE, weekly.getInitialDate());
    assertEquals("00:00:00", weekly.getInitialTime());
  }

  @Test
  void onceKeepsItsDateAndTimeAndCustomKeepsEveryStoredField() throws Exception {
    Schedule once = TaskSchedulePreset.apply(values("ONC"), new Schedule().setDate(ONCE_DATE).setTime("09:30:00")
      .setHourList(List.of("3")));
    Schedule custom = new Schedule().setRepeatType(0).setRepeatNumber(30).setHourList(List.of("3"));

    assertEquals(6, once.getRepeatType());
    assertEquals(List.of(), once.getHourList());
    assertEquals("00 30 09 15 3 ? 2027", ((CronTrigger) trigger(once)).getCronExpression());
    assertEquals(custom, TaskSchedulePreset.apply(values("CUS"), custom));
    // An unknown or missing preset (an old screen, a request without it) changes nothing
    assertEquals(custom, TaskSchedulePreset.apply(values(null), custom));
  }

  @Test
  void aStoredTaskOpensOnThePresetItsFieldsMatch() {
    assertDetected("MIN", new Schedule().setRepeatType(1).setRepeatNumber(10));
    assertDetected("HOU", new Schedule().setRepeatType(2).setRepeatNumber(4));
    assertDetected("DAY", hourAndMinute(7));
    // The old «days» repetition every day is the same daily launch
    assertDetected("DAY", hourAndMinute(3).setRepeatNumber(1));
    assertDetected("WRK", hourAndMinute(7).setWeekDayList(List.of("5", "1", "2", "3", "4")));
    assertDetected("WEK", hourAndMinute(7).setWeekDayList(List.of("6", "0")));
    assertDetected("MON", hourAndMinute(7).setDayList(List.of("15")));
    assertDetected("MON", hourAndMinute(4).setRepeatNumber(1).setDayList(List.of("1")));
    assertDetected("ONC", new Schedule().setRepeatType(6).setDate(ONCE_DATE).setTime("09:30:00"));

    // The time of a preset keeps its seconds (HH:mm:ss)
    TaskSchedulePreset.Values workdays = TaskSchedulePreset.detect(hourAndMinute(7)
      .setWeekDayList(List.of("1", "2", "3", "4", "5")).setSecondList(List.of("20")));
    assertEquals("WRK", workdays.getPreset());
    assertEquals("22:30:20", workdays.getTime());
    TaskSchedulePreset.Values monthly = TaskSchedulePreset.detect(hourAndMinute(7).setDayList(List.of("15")));
    assertEquals(15, monthly.getMonthDay());
    assertEquals("22:30:00", monthly.getTime());
    assertEquals(List.of("6", "0"),
      TaskSchedulePreset.detect(hourAndMinute(7).setWeekDayList(List.of("6", "0"))).getWeekDays());
  }

  @Test
  void aStoredTaskNoPresetCanSayOpensOnTheCustomOne() {
    // Every N seconds, every N days, every N months, yearly, several hours, a year or a month list, both day lists
    assertDetected("CUS", new Schedule().setRepeatType(0).setRepeatNumber(30));
    assertDetected("CUS", hourAndMinute(3).setRepeatNumber(2));
    assertDetected("CUS", hourAndMinute(4).setRepeatNumber(3).setDayList(List.of("1")));
    assertDetected("CUS", hourAndMinute(5).setRepeatNumber(1));
    assertDetected("CUS", hourAndMinute(7).setHourList(List.of("8", "14")));
    assertDetected("CUS", hourAndMinute(7).setYearList(List.of("2027")));
    assertDetected("CUS", hourAndMinute(7).setDayList(List.of("1")).setWeekDayList(List.of("1")));
    assertDetected("CUS", hourAndMinute(7).setDayList(List.of("1", "15")));
    assertDetected("CUS", new Schedule());
  }

  @Test
  void theDaysOfTheWeekFollowTheDailyAndWorkdaysPresetsAndChangingThemMakesTheRepetitionWeekly() {
    List<String> workdays = List.of("1", "2", "3", "4", "5");
    // Choosing daily or workdays fills their days and remembers the preset they were filled for
    assertEquals(Optional.of(new TaskSchedulePreset.DaysSync("WRK", workdays, "WRK")),
      TaskSchedulePreset.syncDays("WRK", "WEK", List.of("6")));
    assertEquals(Optional.of(new TaskSchedulePreset.DaysSync("DAY", List.of("1", "2", "3", "4", "5", "6", "0"), "DAY")),
      TaskSchedulePreset.syncDays("DAY", null, List.of()));
    // Their own days, in any order: nothing to change
    assertEquals(Optional.empty(), TaskSchedulePreset.syncDays("WRK", "WRK", List.of("5", "4", "3", "2", "1")));
    // A day changed on daily or workdays: weekly, with the days chosen
    assertEquals(Optional.of(new TaskSchedulePreset.DaysSync("WEK", List.of("1", "2", "3", "4"), "WEK")),
      TaskSchedulePreset.syncDays("WRK", "WRK", List.of("1", "2", "3", "4")));
    assertEquals(Optional.of(new TaskSchedulePreset.DaysSync("WEK", List.of("1", "2", "3", "4", "5", "6"), "WEK")),
      TaskSchedulePreset.syncDays("DAY", "DAY", List.of("1", "2", "3", "4", "5", "6")));
    // Weekly keeps the days it has
    assertEquals(Optional.of(new TaskSchedulePreset.DaysSync("WEK", workdays, "WEK")),
      TaskSchedulePreset.syncDays("WEK", "WRK", workdays));
    assertEquals(Optional.empty(), TaskSchedulePreset.syncDays("WEK", "WEK", List.of("1")));
    // The other presets have no days
    assertEquals(Optional.empty(), TaskSchedulePreset.syncDays("MIN", "WRK", List.of()));
    assertEquals(Optional.empty(), TaskSchedulePreset.syncDays(null, null, List.of()));
  }

  @Test
  void aPresetIsCompleteOnlyWithItsFields() {
    assertEquals(false, TaskSchedulePreset.isComplete(values("MIN"), new Schedule()));
    assertEquals(true, TaskSchedulePreset.isComplete(values("HOU").setEvery(2), new Schedule()));
    assertEquals(false, TaskSchedulePreset.isComplete(values("DAY"), new Schedule()));
    assertEquals(true, TaskSchedulePreset.isComplete(values("WRK").setTime("22:30"), new Schedule()));
    assertEquals(false, TaskSchedulePreset.isComplete(values("WEK").setTime("22:30"), new Schedule()));
    assertEquals(true, TaskSchedulePreset.isComplete(values("WEK").setTime("22:30").setWeekDays(List.of("1")),
      new Schedule()));
    assertEquals(false, TaskSchedulePreset.isComplete(values("MON").setTime("22:30"), new Schedule()));
    assertEquals(false, TaskSchedulePreset.isComplete(values("ONC"), new Schedule().setTime("10:00:00")));
    assertEquals(true, TaskSchedulePreset.isComplete(values("ONC"), new Schedule().setDate(ONCE_DATE).setTime("10:00:00")));
    // Custom: its stored fields are what they are, once they say how the task repeats
    assertEquals(false, TaskSchedulePreset.isComplete(values("CUS"), new Schedule()));
    assertEquals(true, TaskSchedulePreset.isComplete(values("CUS"), new Schedule().setRepeatType(7)));
  }

  private static TaskSchedulePreset.Values values(String preset) {
    return new TaskSchedulePreset.Values().setPreset(preset);
  }

  private static Schedule hourAndMinute(int repeatType) {
    return new Schedule().setRepeatType(repeatType).setHourList(List.of("22")).setMinuteList(List.of("30"))
      .setSecondList(List.of("0"));
  }

  private static void assertDetected(String preset, Schedule schedule) {
    assertEquals(preset, TaskSchedulePreset.detect(schedule).getPreset());
  }

  private static String cron(TaskSchedulePreset.Values values) throws Exception {
    return ((CronTrigger) trigger(TaskSchedulePreset.apply(values, new Schedule()))).getCronExpression();
  }

  private static Trigger trigger(Schedule schedule) throws Exception {
    return TriggerBuilder.newTrigger().withSchedule(new PatternBuilder(schedule).build()).build();
  }
}
