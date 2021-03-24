package com.almis.awe.scheduler.builder.cron;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import lombok.extern.log4j.Log4j2;
import org.quartz.ScheduleBuilder;
import org.quartz.Trigger;

/**
 * Default pattern builder
 */
@Log4j2
public class PatternBuilder {
  private final Schedule schedule;

  /**
   * Constructor
   *
   * @param schedule Schedule
   */
  public PatternBuilder(Schedule schedule) {
    this.schedule = schedule;
  }

  /**
   * Build schedule builder
   *
   * @return Scheduler builder
   * @throws AWException Error retrieving scheduler builder
   */
  public <T extends Trigger> ScheduleBuilder<T> build() throws AWException {
    switch (schedule.getRepeatType()) {
      case 0:
      case 1:
      case 2:
        return (ScheduleBuilder<T>) new SimplePatternBuilder(schedule).build();
      case 3:
      case 4:
      case 5:
      case 6:
      case 7:
        return (ScheduleBuilder<T>) new CronPatternBuilder(schedule).build();
      default:
        log.error("[SCHEDULER][PATTERN] The selected type of pattern to load is not valid: {}", schedule.getRepeatType());
        throw new AWException("The selected option is not valid");
    }
  }
}
