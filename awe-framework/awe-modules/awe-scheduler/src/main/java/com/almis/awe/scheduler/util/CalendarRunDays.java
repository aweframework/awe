package com.almis.awe.scheduler.util;

import com.almis.awe.exception.AWException;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.builder.cron.PatternBuilder;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Days of a calendar on which a task would run if it had no calendar (#993, delete a calendar in use): the days of the
 * calendar, from now on, on which the repetition of the task fires, as the trigger the scheduler builds from it (the
 * same pattern builders, its start and its end) reads it without a calendar. Pure: no scheduler is needed
 */
@Slf4j
public final class CalendarRunDays {

  private CalendarRunDays() {
  }

  /**
   * Days of the calendar on which the repetition fires, from now on (today only after now), in order
   *
   * @param schedule Stored repetition of the task
   * @param days     Days of the calendar, in any order
   * @param now      Current date
   * @param most     Most days to return
   * @return The days, at most the ones asked; empty when the repetition cannot be built
   */
  public static Optional<List<LocalDate>> runDays(Schedule schedule, Collection<LocalDate> days, LocalDateTime now,
                                                  int most) {
    Trigger trigger;
    try {
      trigger = trigger(schedule, now);
    } catch (AWException | RuntimeException exc) {
      log.debug("[CALENDAR RUN DAYS] The repetition cannot be built", exc);
      return Optional.empty();
    }
    List<LocalDate> runs = new ArrayList<>();
    LocalDate today = now.toLocalDate();
    for (LocalDate day : days.stream().distinct().filter(day -> !day.isBefore(today)).sorted().toList()) {
      if (runs.size() >= most) {
        break;
      }
      // A cron fires on whole seconds: one second before the day starts finds a fire time at midnight too
      LocalDateTime from = day.equals(today) ? now : day.atStartOfDay().minusSeconds(1);
      Date fire = trigger.getFireTimeAfter(date(from));
      if (fire != null && fire.before(date(day.plusDays(1).atStartOfDay()))) {
        runs.add(day);
      }
    }
    return Optional.of(runs);
  }

  /**
   * Trigger of the repetition without a calendar: from its start (today when it has none) to its end
   */
  private static Trigger trigger(Schedule schedule, LocalDateTime now) throws AWException {
    TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger();
    Date start = schedule.getInitialDateTime();
    builder.startAt(start == null ? date(now.toLocalDate().atStartOfDay()) : start);
    if (schedule.getEndDateTime() != null) {
      builder.endAt(schedule.getEndDateTime());
    }
    return builder.withSchedule(new PatternBuilder(schedule).build()).build();
  }

  private static Date date(LocalDateTime dateTime) {
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }
}
