package com.almis.awe.scheduler.builder.cron;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimplePatternBuilderTest {

  private static SimpleTrigger triggerOf(int repeatType, int repeatNumber) throws AWException {
    Schedule schedule = new Schedule();
    schedule.setRepeatType(repeatType);
    schedule.setRepeatNumber(repeatNumber);

    return TriggerBuilder.newTrigger()
      .withSchedule(new SimplePatternBuilder(schedule).build())
      .build();
  }

  @Test
  void buildPatternUsesNextMisfirePolicy() throws Exception {
    assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
      triggerOf(0, 5).getMisfireInstruction());
  }

  /**
   * The "Every" launch types of the scheduled task screen: the repeat number is a count of seconds (0), minutes (1) or
   * hours (2), and the task repeats until it is removed
   */
  @ParameterizedTest(name = "repeat type {0} every {1}: {2} ms")
  @CsvSource({
    "0, 5, 5000",
    "1, 5, 300000",
    "2, 3, 10800000"
  })
  void buildPatternRepeatsForeverAtTheIntervalOfTheRepeatType(int repeatType, int repeatNumber, long intervalMillis)
    throws Exception {
    SimpleTrigger trigger = triggerOf(repeatType, repeatNumber);

    assertThat(trigger.getRepeatInterval()).isEqualTo(intervalMillis);
    assertThat(trigger.getRepeatCount()).isEqualTo(SimpleTrigger.REPEAT_INDEFINITELY);
  }

  @Test
  void buildPatternRejectsARepeatTypeWithoutAnInterval() {
    Schedule schedule = new Schedule();
    schedule.setRepeatType(3);
    schedule.setRepeatNumber(1);
    SimplePatternBuilder builder = new SimplePatternBuilder(schedule);

    assertThatThrownBy(builder::build)
      .isInstanceOf(AWException.class)
      .hasMessageContaining("The selected option is not valid");
  }
}
