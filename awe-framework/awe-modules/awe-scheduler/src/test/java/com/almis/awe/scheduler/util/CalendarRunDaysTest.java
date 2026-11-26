package com.almis.awe.scheduler.util;

import com.almis.awe.scheduler.bean.calendar.Schedule;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Days of a calendar on which a task would run without it (#993, delete a calendar in use): only the days its
 * repetition fires, from now on, up to its end and at most the number asked; unknown when the repetition cannot be
 * built
 */
class CalendarRunDaysTest {

  private static final LocalDate SUNDAY = LocalDate.of(2026, 12, 6);
  private static final LocalDate TUESDAY = LocalDate.of(2026, 12, 8);
  private static final LocalDate FRIDAY = LocalDate.of(2026, 12, 25);
  private static final LocalDate MONDAY = LocalDate.of(2026, 12, 28);
  private static final LocalDate THURSDAY = LocalDate.of(2026, 12, 31);

  /**
   * Weekdays at 22:30, as the editor stores them (Monday 1 to Friday 5)
   */
  private static Schedule workdays() {
    return new Schedule().setRepeatType(7).setWeekDayList(List.of("1", "2", "3", "4", "5"))
      .setHourList(List.of("22")).setMinuteList(List.of("30")).setSecondList(List.of("0"));
  }

  @Test
  void onlyTheDaysItsRepetitionFiresCountAndAtMostTheOnesAsked() {
    Optional<List<LocalDate>> days = CalendarRunDays.runDays(workdays(),
      List.of(THURSDAY, MONDAY, SUNDAY, FRIDAY, TUESDAY), LocalDateTime.of(2026, 11, 24, 10, 0), 3);

    assertThat(days).contains(List.of(TUESDAY, FRIDAY, MONDAY));
  }

  @Test
  void aDayCountsFromNowOn() {
    Optional<List<LocalDate>> days = CalendarRunDays.runDays(workdays(),
      List.of(LocalDate.of(2026, 12, 1), TUESDAY, TUESDAY.plusDays(1)), TUESDAY.atTime(23, 0), 3);

    assertThat(days).contains(List.of(TUESDAY.plusDays(1)));
  }

  @Test
  void aDayAfterTheEndOfTheRepetitionDoesNotCount() {
    Schedule schedule = workdays().setEndDate(Date.valueOf(LocalDate.of(2026, 12, 10))).setEndTime("00:00:00");

    Optional<List<LocalDate>> days = CalendarRunDays.runDays(schedule, List.of(TUESDAY, FRIDAY),
      LocalDateTime.of(2026, 11, 24, 10, 0), 3);

    assertThat(days).contains(List.of(TUESDAY));
  }

  @Test
  void aRepetitionThatCannotBeBuiltCannotTellItsDays() {
    Optional<List<LocalDate>> days = CalendarRunDays.runDays(new Schedule().setRepeatType(99), List.of(TUESDAY),
      LocalDateTime.of(2026, 11, 24, 10, 0), 3);

    assertThat(days).isEmpty();
  }
}
