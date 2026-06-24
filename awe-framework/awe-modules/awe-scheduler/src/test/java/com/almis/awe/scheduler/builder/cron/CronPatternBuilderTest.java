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
import org.quartz.TriggerBuilder;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

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
}
