package com.almis.awe.scheduler.builder.cron;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import javax.naming.NamingException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class used for testing CronPatternBuilder test
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class CronPatternBuilderTest {

  private CronPatternBuilder cronPatternBuilder;

  @BeforeEach
  void setUp() {
    cronPatternBuilder = new CronPatternBuilder(new Schedule());
  }

  /**
   * Test context loaded
   */
  @Test
  void contextLoads() {
    assertNotNull(cronPatternBuilder);
  }

  /**
   * Check triggers contains calendars without calendar list
   */
  @Test
  void buildPatternWithError() {
    // Mock
    Schedule schedule = new Schedule();
    schedule.setRepeatType(11);
    cronPatternBuilder.setSchedule(schedule);
    // Call
    assertThrows(AWException.class, () -> cronPatternBuilder.build());
  }

  /**
   * Week day pattern
   *
   * @throws NamingException Test error
   */
  @Test
  void buildWeekDayPattern() throws Exception {
    // Mock
    Schedule schedule = new Schedule();
    schedule.setRepeatType(4);
    schedule.setRepeatNumber(2);
    schedule.setWeekDayList(Arrays.asList("1", "2", "3"));
    cronPatternBuilder.setSchedule(schedule);

    // Call
    CronScheduleBuilder scheduleBuilder = cronPatternBuilder.build();

    // Assert not null
    assertNotNull(scheduleBuilder);
  }

  /**
   * Week pattern
   *
   * @throws NamingException Test error
   */
  @Test
  void buildWeekPattern() throws Exception {
    // Mock
    Schedule schedule = new Schedule();
    schedule.setRepeatType(4);
    schedule.setRepeatNumber(2);
    schedule.setWeekList(Arrays.asList("1", "2", "3"));
    cronPatternBuilder.setSchedule(schedule);

    // Call
    CronScheduleBuilder scheduleBuilder = cronPatternBuilder.build();

    // Assert not null
    assertNotNull(scheduleBuilder);
  }

  /**
   * Day pattern
   *
   * @throws NamingException Test error
   */
  @Test
  void buildDayPattern() throws Exception {
    // Mock
    Schedule schedule = new Schedule();
    schedule.setRepeatType(3);
    schedule.setRepeatNumber(1);
    schedule.setHourList(Arrays.asList("9", "15"));
    schedule.setWeekDayList(Collections.singletonList(""));
    schedule.setWeekList(Collections.singletonList(""));
    cronPatternBuilder.setSchedule(schedule);

    // Call
    CronScheduleBuilder scheduleBuilder = cronPatternBuilder.build();

    // Assert not null
    assertNotNull(scheduleBuilder.build());
  }

  /**
   * Day pattern
   *
   * @throws NamingException Test error
   */
  @Test
  void buildDayPatternEmptyWeek() throws Exception {
    // Mock
    Schedule schedule = new Schedule();
    schedule.setRepeatType(3);
    schedule.setRepeatNumber(1);
    schedule.setHourList(Arrays.asList("9", "15"));
    schedule.setWeekDayList(Collections.emptyList());
    schedule.setWeekList(Collections.emptyList());
    cronPatternBuilder.setSchedule(schedule);

    // Call
    CronScheduleBuilder scheduleBuilder = cronPatternBuilder.build();

    // Assert not null
    assertNotNull(scheduleBuilder.build());
  }

  /**
   * Day pattern
   *
   * @throws NamingException Test error
   */
  @Test
  void buildYearPattern() throws Exception {
    // Mock
    Schedule schedule = new Schedule();
    schedule.setRepeatType(5);
    schedule.setRepeatNumber(1);
    schedule.setDate(new Date());
    schedule.setTime("00:00:01");
    schedule.setHourList(Arrays.asList("9", "15"));
    schedule.setWeekDayList(Collections.emptyList());
    schedule.setWeekList(Collections.emptyList());
    cronPatternBuilder.setSchedule(schedule);

    // Call
    CronScheduleBuilder scheduleBuilder = cronPatternBuilder.build();

    // Assert not null
    assertNotNull(scheduleBuilder.build());
  }

  @Test
  void buildYearPatternKeepsPastDate() throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.set(2000, Calendar.JANUARY, 1, 10, 15, 30);
    calendar.set(Calendar.MILLISECOND, 0);

    Schedule schedule = new Schedule();
    schedule.setRepeatType(5);
    schedule.setRepeatNumber(1);
    schedule.setDate(calendar.getTime());
    schedule.setTime("10:15:30");
    cronPatternBuilder.setSchedule(schedule);

    CronTrigger trigger = TriggerBuilder.newTrigger()
      .withSchedule(cronPatternBuilder.build())
      .build();

    assertEquals("30 15 10 1 1 ? 2000", trigger.getCronExpression());
  }

  @Test
  void buildPatternUsesDoNothingMisfirePolicy() throws Exception {
    Schedule schedule = new Schedule();
    schedule.setRepeatType(6);
    schedule.setDate(new Date());
    schedule.setTime("00:00:01");
    cronPatternBuilder.setSchedule(schedule);

    CronTrigger trigger = TriggerBuilder.newTrigger()
      .withSchedule(cronPatternBuilder.build())
      .build();

    assertEquals(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING, trigger.getMisfireInstruction());
  }
  /**
   * The editor numbers the days of the week from Monday (1) to Saturday (6) and Sunday (0); Quartz from Sunday (1) to
   * Saturday (7): the cron fires on the configured day (#993)
   */
  @Test
  void theDaysOfTheWeekOfTheEditorAreConvertedToQuartzDays() throws Exception {
    assertEquals("0 0 8 ? * 2 *", weekDaysCron(Collections.singletonList("1"), null));
    assertEquals("0 0 8 ? * 1 *", weekDaysCron(Collections.singletonList("0"), null));
    assertEquals("0 0 8 ? * 7,2,4 *", weekDaysCron(Arrays.asList("6", "1", "3"), null));
    assertEquals("0 0 8 ? * 1#1 *", weekDaysCron(Collections.singletonList("0"), Collections.singletonList("#1")));
    assertEquals("0 0 8 ? * * *", weekDaysCron(Collections.singletonList("*"), null));
  }

  /**
   * Only a single digit 0 to 7 is a day of the editor (7 is Sunday as 0): any other number is kept, so Quartz refuses
   * it as before
   */
  @Test
  void onlyTheDigitsZeroToSevenAreConvertedAndAnyOtherNumberIsKept() {
    assertEquals("1", CronPatternBuilder.toQuartzDay("7"));
    assertEquals("1", CronPatternBuilder.toQuartzDay("0"));
    assertEquals("7", CronPatternBuilder.toQuartzDay("6"));
    assertEquals("8", CronPatternBuilder.toQuartzDay("8"));
    assertEquals("9", CronPatternBuilder.toQuartzDay("9"));
    assertEquals("10", CronPatternBuilder.toQuartzDay("10"));
    assertEquals("MON", CronPatternBuilder.toQuartzDay("MON"));
  }

  /**
   * A custom launch on Monday, Wednesday and Sunday of the editor fires on those days of the week (#993)
   */
  @Test
  void aCustomLaunchFiresOnTheWeekDaysChosenInTheEditor() throws Exception {
    Schedule schedule = new Schedule();
    schedule.setRepeatType(7);
    schedule.setHourList(Collections.singletonList("8"));
    schedule.setMinuteList(Collections.singletonList("0"));
    schedule.setSecondList(Collections.singletonList("0"));
    schedule.setWeekDayList(Arrays.asList("1", "3", "0"));
    // Monday 23/11/2026 00:00; the trigger starts then, so the result does not depend on the day the test runs
    Date fire = Date.from(LocalDate.of(2026, 11, 23).atStartOfDay(ZoneId.systemDefault()).toInstant());
    Trigger trigger = TriggerBuilder.newTrigger().startAt(fire).withSchedule(new PatternBuilder(schedule).build()).build();

    List<DayOfWeek> days = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      fire = trigger.getFireTimeAfter(fire);
      days.add(fire.toInstant().atZone(ZoneId.systemDefault()).getDayOfWeek());
    }

    assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY), days);
  }

  private String weekDaysCron(java.util.List<String> weekDays, java.util.List<String> weeks) throws Exception {
    Schedule schedule = new Schedule();
    schedule.setRepeatType(7);
    schedule.setHourList(Collections.singletonList("8"));
    schedule.setMinuteList(Collections.singletonList("0"));
    schedule.setSecondList(Collections.singletonList("0"));
    schedule.setWeekDayList(weekDays);
    schedule.setWeekList(weeks);
    cronPatternBuilder.setSchedule(schedule);
    CronTrigger trigger = TriggerBuilder.newTrigger().withSchedule(cronPatternBuilder.build()).build();
    return trigger.getCronExpression();
  }
}
