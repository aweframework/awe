package com.almis.awe.scheduler.builder.cron;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.CronScheduleBuilder;

import javax.naming.NamingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Class used for testing CronPatternBuilder test
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class CronPatternBuilderTest {

  @InjectMocks
  private CronPatternBuilder cronPatternBuilder;

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
}
