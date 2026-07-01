package com.almis.awe.scheduler.builder.cron;

import com.almis.awe.scheduler.bean.calendar.Schedule;
import org.junit.jupiter.api.Test;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimplePatternBuilderTest {

  @Test
  void buildPatternUsesNextMisfirePolicy() throws Exception {
    Schedule schedule = new Schedule();
    schedule.setRepeatType(0);
    schedule.setRepeatNumber(5);

    SimpleTrigger trigger = TriggerBuilder.newTrigger()
      .withSchedule(new SimplePatternBuilder(schedule).build())
      .build();

    assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT,
      trigger.getMisfireInstruction());
  }
}
