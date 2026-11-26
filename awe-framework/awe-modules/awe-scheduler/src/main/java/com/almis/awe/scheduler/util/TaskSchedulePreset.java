package com.almis.awe.scheduler.util;

import com.almis.awe.scheduler.bean.calendar.Schedule;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Presets of the «Repetition» of a scheduled task in the task editor (#993): every N minutes, every N hours, daily,
 * workdays, weekly, monthly, once and custom. They are only a way of filling the stored fields of a scheduled task
 * (repeat type and number, days of the week as the editor numbers them, days of the month, hours, minutes, seconds,
 * date and time): {@link #apply(Values, Schedule)} writes the fields of a preset and {@link #detect(Schedule)} finds the
 * preset that stored fields match, the custom one when none does. Pure: no new data, no cron of its own (the trigger is
 * built from the stored fields as before, PatternBuilder)
 */
public final class TaskSchedulePreset {

  public static final String MINUTES = "MIN";
  public static final String HOURS = "HOU";
  public static final String DAILY = "DAY";
  public static final String WORKDAYS = "WRK";
  public static final String WEEKLY = "WEK";
  public static final String MONTHLY = "MON";
  public static final String ONCE = "ONC";
  public static final String CUSTOM = "CUS";
  /**
   * Repeat types of the stored fields: an interval of minutes or hours, days, months, once and custom (the lists)
   */
  private static final int REPEAT_MINUTES = 1;
  private static final int REPEAT_HOURS = 2;
  private static final int REPEAT_DAYS = 3;
  private static final int REPEAT_MONTHS = 4;
  private static final int REPEAT_ONCE = 6;
  private static final int REPEAT_CUSTOM = 7;
  /**
   * Monday to Friday as the task editor numbers the days of the week (Sunday 0)
   */
  private static final Set<Integer> WORKDAY_SET = Set.of(1, 2, 3, 4, 5);
  /**
   * Days of the week of the day toggles of the daily and workdays presets, from Monday to Sunday as the editor numbers
   * them
   */
  private static final List<String> EVERY_DAY = List.of("1", "2", "3", "4", "5", "6", "0");
  private static final List<String> WORKDAY_LIST = List.of("1", "2", "3", "4", "5");
  private static final Pattern NUMBER = Pattern.compile("\\d{1,2}");

  /**
   * Fields of a preset: the preset, the interval (every N minutes or hours), the time of the day (HH:mm or HH:mm:ss),
   * the days of the week (as the editor numbers them) and the day of the month. A launch once uses the stored date and
   * time
   */
  @Data
  @Accessors(chain = true)
  public static class Values {
    private String preset;
    private Integer every;
    private String time;
    private List<String> weekDays = new ArrayList<>();
    private Integer monthDay;
  }

  /**
   * Change of the day toggles of the editor: the preset, its days of the week and the preset the days were filled for
   *
   * @param preset     Preset
   * @param weekDays   Days of the week, as the editor numbers them
   * @param daysPreset Preset the days were filled for
   */
  public record DaysSync(String preset, List<String> weekDays, String daysPreset) {
  }

  private TaskSchedulePreset() {
  }

  /**
   * Days of the week the day toggles show for a preset: every day for a daily one, Monday to Friday for the workdays
   * one, none for the others (weekly has the days chosen)
   *
   * @param preset Preset
   * @return Days of the week, as the editor numbers them
   */
  public static List<String> presetDays(String preset) {
    if (DAILY.equals(preset)) {
      return EVERY_DAY;
    }
    return WORKDAYS.equals(preset) ? WORKDAY_LIST : List.of();
  }

  /**
   * The day toggles of the editor and the presets with days (daily, workdays, weekly). A daily or workdays preset just
   * chosen (its days were filled for another preset) fills its days; a change of those days makes the repetition weekly
   * on the days chosen; a weekly one keeps the days it has. The preset the days were filled for tells a change of
   * preset from a change of days
   *
   * @param preset     Preset of the screen
   * @param daysPreset Preset the days of the screen were filled for
   * @param weekDays   Days of the week of the screen
   * @return The change to make, empty when there is none
   */
  public static Optional<DaysSync> syncDays(String preset, String daysPreset, List<String> weekDays) {
    List<String> days = clean(weekDays);
    if (WEEKLY.equals(preset)) {
      return WEEKLY.equals(daysPreset) ? Optional.empty() : Optional.of(new DaysSync(WEEKLY, days, WEEKLY));
    } else if (!DAILY.equals(preset) && !WORKDAYS.equals(preset)) {
      return Optional.empty();
    }
    List<String> expected = presetDays(preset);
    if (!preset.equals(daysPreset)) {
      return Optional.of(new DaysSync(preset, expected, preset));
    }
    return new TreeSet<>(days).equals(new TreeSet<>(expected)) ? Optional.empty()
      : Optional.of(new DaysSync(WEEKLY, days, WEEKLY));
  }

  /**
   * Stored fields of a scheduled task with a preset: the fields of the preset over the stored ones, which keep their
   * calendar and their range (from, to). The custom preset, an unknown one or none leave the stored fields as they are
   *
   * @param values Preset and its fields
   * @param stored Stored fields (as the screen sends them)
   * @return Stored fields with the preset applied
   */
  public static Schedule apply(Values values, Schedule stored) {
    String preset = Optional.ofNullable(values.getPreset()).orElse(CUSTOM);
    if (!List.of(MINUTES, HOURS, DAILY, WORKDAYS, WEEKLY, MONTHLY, ONCE).contains(preset)) {
      return stored;
    }
    Schedule schedule = new Schedule()
      .setCalendarId(stored.getCalendarId())
      .setInitialDate(stored.getInitialDate())
      .setInitialTime(stored.getInitialTime())
      .setEndDate(stored.getEndDate())
      .setEndTime(stored.getEndTime())
      .setYearList(new ArrayList<>())
      .setMonthList(new ArrayList<>())
      .setWeekList(new ArrayList<>())
      .setDayList(new ArrayList<>())
      .setWeekDayList(new ArrayList<>())
      .setHourList(new ArrayList<>())
      .setMinuteList(new ArrayList<>())
      .setSecondList(new ArrayList<>());
    switch (preset) {
      case MINUTES, HOURS -> schedule.setRepeatType(MINUTES.equals(preset) ? REPEAT_MINUTES : REPEAT_HOURS)
        .setRepeatNumber(values.getEvery());
      case ONCE -> schedule.setRepeatType(REPEAT_ONCE).setDate(stored.getDate()).setTime(stored.getTime());
      default -> {
        // Daily, workdays, weekly and monthly: the custom repetition (a cron of the lists) at a time of the day
        schedule.setRepeatType(REPEAT_CUSTOM);
        setTime(schedule, values.getTime());
        if (WORKDAYS.equals(preset)) {
          schedule.setWeekDayList(new ArrayList<>(List.of("1", "2", "3", "4", "5")));
        } else if (WEEKLY.equals(preset)) {
          schedule.setWeekDayList(clean(values.getWeekDays()));
        } else if (MONTHLY.equals(preset) && values.getMonthDay() != null) {
          schedule.setDayList(new ArrayList<>(List.of(String.valueOf(values.getMonthDay()))));
        }
      }
    }
    return schedule;
  }

  /**
   * The fields of a preset are filled: the interval of every N minutes or hours, the time of a daily, workdays, weekly or
   * monthly one, the days of a weekly one, the day of a monthly one, and the stored date and time of a launch once. The
   * custom preset (and an unknown one) keeps the stored fields as they are, so it is complete once they say how the task
   * repeats (without a repeat type its runs cannot be computed)
   *
   * @param values Preset and its fields
   * @param stored Stored fields (as the screen sends them)
   * @return The preset can be applied
   */
  public static boolean isComplete(Values values, Schedule stored) {
    String preset = Optional.ofNullable(values.getPreset()).orElse(CUSTOM);
    boolean hasTime = values.getTime() != null && !values.getTime().isBlank();
    return switch (preset) {
      case MINUTES, HOURS -> values.getEvery() != null && values.getEvery() > 0;
      case DAILY, WORKDAYS -> hasTime;
      case WEEKLY -> hasTime && !clean(values.getWeekDays()).isEmpty();
      case MONTHLY -> hasTime && values.getMonthDay() != null;
      case ONCE -> stored.getDate() != null && stored.getTime() != null && !stored.getTime().isBlank();
      default -> stored.getRepeatType() != null;
    };
  }

  /**
   * Preset of stored fields, with its fields: the preset they match, else the custom one (every N seconds, every N
   * days or months, yearly, several times a day, a list of years, months or weeks…)
   *
   * @param schedule Stored fields
   * @return Preset and its fields
   */
  public static Values detect(Schedule schedule) {
    int repeatType = Optional.ofNullable(schedule.getRepeatType()).orElse(-1);
    Values values = new Values().setPreset(CUSTOM);
    if (repeatType == REPEAT_MINUTES || repeatType == REPEAT_HOURS) {
      return values.setPreset(repeatType == REPEAT_MINUTES ? MINUTES : HOURS).setEvery(schedule.getRepeatNumber());
    } else if (repeatType == REPEAT_ONCE) {
      return values.setPreset(ONCE);
    }
    Optional<String> time = time(schedule);
    if (time.isEmpty() || hasDateOrLists(schedule)) {
      return values;
    }
    return detectDays(values, schedule, repeatType, time.get());
  }

  /**
   * Whether the stored fields hold a date, or a list of years, months or weeks: no preset but the custom one has them
   */
  private static boolean hasDateOrLists(Schedule schedule) {
    return schedule.getDate() != null || !isEmpty(schedule.getYearList()) || !isEmpty(schedule.getMonthList())
      || !isEmpty(schedule.getWeekList());
  }

  /**
   * Preset of stored fields with a single time of the day: daily, on workdays, weekly or monthly, else the custom one
   */
  private static Values detectDays(Values values, Schedule schedule, int repeatType, String time) {
    int every = Optional.ofNullable(schedule.getRepeatNumber()).orElse(1);
    boolean daily = repeatType == REPEAT_CUSTOM || (repeatType == REPEAT_DAYS && every == 1);
    boolean monthly = repeatType == REPEAT_CUSTOM || (repeatType == REPEAT_MONTHS && every == 1);
    List<String> weekDays = clean(schedule.getWeekDayList());
    List<String> days = clean(schedule.getDayList());
    if (days.isEmpty() && weekDays.isEmpty() && daily) {
      return values.setPreset(DAILY).setTime(time);
    } else if (days.isEmpty() && daily && repeatType == REPEAT_CUSTOM
      && weekDays.stream().allMatch(TaskSchedulePreset::isWeekDay)) {
      Set<Integer> set = new TreeSet<>(weekDays.stream().map(Integer::parseInt).map(day -> day % 7).toList());
      return values.setPreset(WORKDAY_SET.equals(set) ? WORKDAYS : WEEKLY).setTime(time).setWeekDays(weekDays);
    } else if (weekDays.isEmpty() && days.size() == 1 && monthly && NUMBER.matcher(days.get(0)).matches()) {
      return values.setPreset(MONTHLY).setTime(time).setMonthDay(Integer.parseInt(days.get(0)));
    }
    return values;
  }

  /**
   * Hour, minute and second of a time of the day (HH:mm or HH:mm:ss), without leading zeros as the lists keep them
   */
  private static void setTime(Schedule schedule, String time) {
    if (time == null || time.isBlank()) {
      return;
    }
    String[] parts = time.trim().split(":");
    schedule.setHourList(new ArrayList<>(List.of(number(parts[0]))));
    schedule.setMinuteList(new ArrayList<>(List.of(parts.length > 1 ? number(parts[1]) : "0")));
    schedule.setSecondList(new ArrayList<>(List.of(parts.length > 2 ? number(parts[2]) : "0")));
  }

  /**
   * Time of the day of the stored fields (HH:mm:ss, as the time criteria of the screen hold it), empty when they hold
   * more than one hour, minute or second, or a value that is not a number
   */
  private static Optional<String> time(Schedule schedule) {
    List<String> hours = clean(schedule.getHourList());
    List<String> minutes = clean(schedule.getMinuteList());
    List<String> seconds = clean(schedule.getSecondList());
    if (hours.size() != 1 || minutes.size() > 1 || seconds.size() > 1
      || !NUMBER.matcher(hours.get(0)).matches()
      || minutes.stream().anyMatch(value -> !NUMBER.matcher(value).matches())
      || seconds.stream().anyMatch(value -> !NUMBER.matcher(value).matches())) {
      return Optional.empty();
    }
    return Optional.of(pad(hours.get(0)) + ":" + pad(minutes.isEmpty() ? "0" : minutes.get(0)) + ":"
      + pad(seconds.isEmpty() ? "0" : seconds.get(0)));
  }

  private static boolean isWeekDay(String day) {
    return day.matches("[0-7]");
  }

  private static String number(String value) {
    String trimmed = value.trim();
    return NUMBER.matcher(trimmed).matches() ? String.valueOf(Integer.parseInt(trimmed)) : trimmed;
  }

  private static String pad(String value) {
    return value.length() == 1 ? "0" + value : value;
  }

  private static boolean isEmpty(List<String> values) {
    return clean(values).isEmpty();
  }

  private static List<String> clean(List<String> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values.stream()
      .filter(value -> value != null && !value.isBlank()).map(String::trim).toList());
  }
}
