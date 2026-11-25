package com.almis.awe.scheduler.util;

import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.enums.TaskLaunchKind;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * «When» summary of the task catalog: when a task is launched, in plain words derived from its launch type, its
 * repetition, its calendar, the file it waits for and the tasks it runs after (no field of its own), and its next
 * execution in the same words. Pure: the texts come from the locales through {@link Texts}, so the user language is
 * applied by the caller
 */
public final class TaskWhenSummary {

  /**
   * Text of an empty value, as the other views of the scheduler show it
   */
  public static final String EMPTY_VALUE = "—";
  private static final String WHEN = "SCREEN_TEXT_TASK_WHEN_";
  private static final String CATALOG = "SCREEN_TEXT_TASK_CATALOG_";
  private static final String CALENDAR_ICON = "lucide:calendar-days";
  private static final int MOST_TIMES = 4;
  private static final int MOST_PARENTS = 3;
  private static final Pattern NUMBER = Pattern.compile("\\d{1,2}");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
  /**
   * Quartz names of the days of the week, with their numbers (1 Sunday to 7 Saturday)
   */
  private static final Map<String, Integer> DAY_NAMES = Map.of("SUN", 1, "MON", 2, "TUE", 3, "WED", 4, "THU", 5,
    "FRI", 6, "SAT", 7);
  private static final List<Integer> WEEK_ORDER = List.of(2, 3, 4, 5, 6, 7, 1);
  private static final List<Integer> WORKDAYS = List.of(2, 3, 4, 5, 6);
  private static final List<Integer> WEEKENDS = List.of(7, 1);

  /**
   * Texts of the summary: the locale of a key with its parameters
   */
  @FunctionalInterface
  public interface Texts {
    /**
     * Text of a locale key
     *
     * @param key        Locale key
     * @param parameters Parameters of the locale
     * @return Text
     */
    String text(String key, String... parameters);
  }

  /**
   * «When» summary of a task: its kind, its icon, the main text and the detail under it
   *
   * @param kind   Kind of launch
   * @param icon   Icon
   * @param text   Main text
   * @param detail Detail
   */
  public record When(TaskLaunchKind kind, String icon, String text, String detail) {
  }

  /**
   * A text with its detail under it
   *
   * @param text   Main text
   * @param detail Detail
   */
  public record Line(String text, String detail) {
  }

  private final Texts texts;
  private final boolean withSeconds;

  /**
   * Constructor
   *
   * @param texts Texts of the summary
   */
  public TaskWhenSummary(Texts texts) {
    this(texts, false);
  }

  /**
   * Constructor
   *
   * @param texts       Texts of the summary
   * @param withSeconds The times of the day tell their second when it is not 0 (the task editor)
   */
  public TaskWhenSummary(Texts texts, boolean withSeconds) {
    this.texts = texts;
    this.withSeconds = withSeconds;
  }

  /**
   * When a task is launched
   *
   * @param launch Launch of the task
   * @return «When» summary
   */
  public When when(TaskLaunch launch) {
    List<TaskLaunch.Parent> parents = Optional.ofNullable(launch.getParents()).orElse(List.of());
    TaskLaunchKind kind = TaskLaunchKind.of(launch.getLaunchType(), !parents.isEmpty());
    String alsoAfter = parents.isEmpty() ? null : text(WHEN + "ALSO_AFTER", parentNames(parents));
    return switch (kind) {
      case DEPENDENCY -> new When(kind, kind.getIcon(), text(WHEN + "AFTER", parentNames(parents)), condition(parents));
      case WEBHOOK -> new When(kind, kind.getIcon(), text(WHEN + "WEBHOOK"),
        Optional.ofNullable(alsoAfter).orElse(text(WHEN + "WEBHOOK_DETAIL")));
      case FILE -> new When(kind, kind.getIcon(), text(WHEN + "FILE", fileLocation(launch)),
        Optional.ofNullable(alsoAfter).orElse(fileDetail(launch)));
      case SCHEDULED -> scheduled(launch, alsoAfter);
      default -> new When(kind, kind.getIcon(), text(WHEN + "MANUAL"), text(WHEN + "MANUAL_DETAIL"));
    };
  }

  /**
   * Next execution of a task: its next fire time, or what launches it when it has none
   *
   * @param launch   Launch of the task
   * @param active   The task is active
   * @param nextFire Next fire time of the task in the scheduler, null when it has none
   * @param now      Current date
   * @return Next execution and its detail
   */
  public Line next(TaskLaunch launch, boolean active, LocalDateTime nextFire, LocalDateTime now) {
    if (!active) {
      return new Line(EMPTY_VALUE, text(CATALOG + "NEXT_INACTIVE"));
    }
    List<TaskLaunch.Parent> parents = Optional.ofNullable(launch.getParents()).orElse(List.of());
    TaskLaunchKind kind = TaskLaunchKind.of(launch.getLaunchType(), !parents.isEmpty());
    if (nextFire != null) {
      String detail = kind == TaskLaunchKind.FILE
        ? text(CATALOG + "NEXT_FILE_CHECK", Optional.ofNullable(launch.getServer()).orElse(fileLocation(launch)))
        : weekDay(nextFire.getDayOfWeek().getValue() % 7 + 1) + " " + DATE.format(nextFire);
      return new Line(relative(nextFire, now), detail);
    }
    return switch (kind) {
      case DEPENDENCY -> new Line(text(CATALOG + "NEXT_AFTER", parentNames(parents)), "");
      case WEBHOOK -> new Line(text(WHEN + "WEBHOOK"), "");
      case MANUAL -> new Line(EMPTY_VALUE, text(CATALOG + "NEXT_ON_DEMAND"));
      default -> new Line(EMPTY_VALUE, text(CATALOG + "NEXT_NOT_SCHEDULED"));
    };
  }

  /**
   * Date relative to now: today, yesterday or tomorrow with its time, else the day and the time (dd/MM HH:mm) in this
   * year and the full date (dd/MM/yyyy HH:mm) in another year
   *
   * @param date Date
   * @param now  Current date
   * @return Relative date
   */
  public String relative(LocalDateTime date, LocalDateTime now) {
    LocalDate day = date.toLocalDate();
    LocalDate today = now.toLocalDate();
    String time = TIME.format(date);
    if (day.equals(today)) {
      return text(CATALOG + "TODAY", time);
    } else if (day.equals(today.minusDays(1))) {
      return text(CATALOG + "YESTERDAY", time);
    } else if (day.equals(today.plusDays(1))) {
      return text(CATALOG + "TOMORROW", time);
    }
    return (day.getYear() == today.getYear() ? DAY_TIME : DATE_TIME).format(date);
  }

  /**
   * Duration in the words of the catalog and the Activity screen: hours, minutes and seconds («6m 35s», «8s»), and
   * milliseconds only under one second («850ms»)
   *
   * @param millis Duration in milliseconds
   * @return Duration text
   */
  public static String duration(long millis) {
    if (millis < 1000) {
      return Math.max(millis, 0) + "ms";
    }
    long seconds = millis / 1000;
    List<String> parts = new ArrayList<>();
    if (seconds >= 3600) {
      parts.add(seconds / 3600 + "h");
    }
    if (seconds % 3600 >= 60) {
      parts.add(seconds % 3600 / 60 + "m");
    }
    if (seconds % 60 > 0) {
      parts.add(seconds % 60 + "s");
    }
    return String.join(" ", parts);
  }

  /**
   * «When» summary of a scheduled task: its repetition, with its calendar (or the tasks it also runs after) under it
   */
  private When scheduled(TaskLaunch launch, String alsoAfter) {
    TaskLaunchKind kind = TaskLaunchKind.SCHEDULED;
    String icon = launch.getCalendar() == null || launch.getCalendar().isBlank() ? kind.getIcon() : CALENDAR_ICON;
    String detail = icon.equals(CALENDAR_ICON) ? text(WHEN + "CALENDAR", launch.getCalendar())
      : text(WHEN + "SCHEDULED_DETAIL");
    return new When(kind, icon, schedule(launch).orElseGet(() -> text(WHEN + "CUSTOM")),
      Optional.ofNullable(alsoAfter).orElse(detail));
  }

  /**
   * Repetition of a scheduled task, empty when it cannot be told in a few words
   */
  private Optional<String> schedule(TaskLaunch launch) {
    int repeatType = Optional.ofNullable(launch.getRepeatType()).orElse(-1);
    int every = Math.max(1, Optional.ofNullable(launch.getRepeatNumber()).orElse(1));
    return switch (repeatType) {
      case 0 -> Optional.of(every == 1 ? text(WHEN + "EVERY_SECOND") : text(WHEN + "EVERY_SECONDS", str(every)));
      case 1 -> Optional.of(every == 1 ? text(WHEN + "EVERY_MINUTE") : text(WHEN + "EVERY_MINUTES", str(every)));
      case 2 -> Optional.of(every == 1 ? text(WHEN + "EVERY_HOUR") : text(WHEN + "EVERY_HOURS", str(every)));
      case 3 -> times(launch).flatMap(times -> days(launch, every, times));
      case 4 -> times(launch).flatMap(times -> monthDays(launch).map(days -> every == 1
        ? text(WHEN + "MONTHLY", days, times) : text(WHEN + "EVERY_MONTHS", str(every), days, times)));
      case 5 -> Optional.ofNullable(launch.getDate()).flatMap(date -> times(launch)
        .map(times -> text(WHEN + "YEARLY", DAY_MONTH.format(date), times)));
      case 6 -> Optional.ofNullable(launch.getDate()).flatMap(date -> times(launch)
        .map(times -> text(WHEN + "ONCE", DATE.format(date), times)));
      case 7 -> times(launch).flatMap(times -> isEmpty(launch.getWeekDays()) && !isEmpty(launch.getDays())
        ? monthDays(launch).map(days -> text(WHEN + "MONTHLY", days, times))
        : days(launch, 1, times));
      default -> Optional.empty();
    };
  }

  /**
   * Days of a repetition by days: the days of the week, else every day or every some days
   */
  private Optional<String> days(TaskLaunch launch, int every, String times) {
    if (isEmpty(launch.getWeekDays())) {
      return Optional.of(every == 1 ? text(WHEN + "DAILY", times) : text(WHEN + "EVERY_DAYS", str(every), times));
    }
    Optional<TreeSet<Integer>> weekDays = weekDays(launch.getWeekDays());
    if (weekDays.isEmpty()) {
      return Optional.empty();
    }
    TreeSet<Integer> set = weekDays.get();
    if (set.size() == 7) {
      return Optional.of(text(WHEN + "DAILY", times));
    } else if (set.equals(new TreeSet<>(WORKDAYS))) {
      return Optional.of(text(WHEN + "WORKDAYS", times));
    } else if (set.equals(new TreeSet<>(WEEKENDS))) {
      return Optional.of(text(WHEN + "WEEKENDS", times));
    }
    List<String> names = WEEK_ORDER.stream().filter(set::contains).map(this::weekDay).toList();
    return Optional.of(capitalize(text(WHEN + "WEEKDAYS", join(names), times)));
  }

  /**
   * Days of the week of the repetition (stored as the task editor numbers them) as Quartz numbers them, empty when one
   * of them is not a day
   */
  private static Optional<TreeSet<Integer>> weekDays(List<String> values) {
    TreeSet<Integer> days = new TreeSet<>();
    for (String value : values) {
      String day = value.trim().toUpperCase(Locale.ROOT);
      if (day.isEmpty()) {
        continue;
      }
      if ("*".equals(day)) {
        days.addAll(WEEK_ORDER);
      } else if (DAY_NAMES.containsKey(day)) {
        days.add(DAY_NAMES.get(day));
      } else if (NUMBER.matcher(day).matches() && Integer.parseInt(day) <= 7) {
        // The task editor numbers the days from Monday (1) to Saturday (6) and Sunday (0), and the cron is built with
        // the Quartz day (CronPatternBuilder)
        days.add(Integer.parseInt(day) % 7 + 1);
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(days);
  }

  /**
   * Days of the month of the repetition (the first day without them), empty when one of them is not a day
   */
  private Optional<String> monthDays(TaskLaunch launch) {
    if (!isEmpty(launch.getWeekDays())) {
      return Optional.empty();
    }
    List<String> days = isEmpty(launch.getDays()) ? List.of("1") : clean(launch.getDays());
    if (days.stream().anyMatch(day -> !NUMBER.matcher(day).matches())) {
      return Optional.empty();
    }
    return Optional.of(join(days.stream().map(Integer::parseInt).distinct().sorted().map(String::valueOf).toList()));
  }

  /**
   * Times of the day of the repetition (HH:mm, at most four), empty when they cannot be listed
   */
  private Optional<String> times(TaskLaunch launch) {
    if (launch.getDate() != null && launch.getTime() != null && !launch.getTime().isBlank()) {
      String[] time = launch.getTime().trim().split(":");
      return Optional.of(pad(time[0]) + ":" + (time.length > 1 ? pad(time[1]) : "00")
        + second(time.length > 2 ? List.of(time[2]) : List.of()));
    }
    List<String> hours = isEmpty(launch.getHours()) ? List.of("0") : clean(launch.getHours());
    List<String> minutes = isEmpty(launch.getMinutes()) ? List.of("0") : clean(launch.getMinutes());
    if (anyNotNumber(hours) || anyNotNumber(minutes)) {
      return Optional.empty();
    }
    String second = second(isEmpty(launch.getSeconds()) ? List.of() : clean(launch.getSeconds()));
    TreeSet<String> times = new TreeSet<>();
    for (String hour : hours) {
      for (String minute : minutes) {
        times.add(pad(hour) + ":" + pad(minute) + second);
      }
    }
    return times.size() > MOST_TIMES ? Optional.empty() : Optional.of(join(List.copyOf(times)));
  }

  /**
   * Second of the times of the day («:15»), only with seconds told, one second that is a number and not 0
   */
  private String second(List<String> seconds) {
    if (!withSeconds || seconds.size() != 1 || !NUMBER.matcher(seconds.get(0).trim()).matches()
      || Integer.parseInt(seconds.get(0).trim()) == 0) {
      return "";
    }
    return ":" + pad(seconds.get(0));
  }

  /**
   * Condition of the parents of a task: what the parent has to end with, or that all of them have to end
   */
  private String condition(List<TaskLaunch.Parent> parents) {
    if (parents.size() > 1) {
      return text(WHEN + "AFTER_ALL", str(parents.size()));
    }
    TaskLaunch.Parent parent = parents.get(0);
    if (parent.onWarning() && parent.onError()) {
      return text(WHEN + "AFTER_ANY");
    } else if (parent.onWarning()) {
      return text(WHEN + "AFTER_IF_OK_WARNING");
    } else if (parent.onError()) {
      return text(WHEN + "AFTER_IF_OK_ERROR");
    }
    return text(WHEN + "AFTER_IF_OK");
  }

  /**
   * Names of the parents: every name up to three, else the first two and how many more
   */
  private String parentNames(List<TaskLaunch.Parent> parents) {
    List<String> names = new ArrayList<>(parents.stream().map(TaskLaunch.Parent::name).toList());
    if (names.size() > MOST_PARENTS) {
      int more = names.size() - 2;
      names = new ArrayList<>(names.subList(0, 2));
      names.add(text(WHEN + "MORE", str(more)));
    }
    return join(names);
  }

  private String fileLocation(TaskLaunch launch) {
    String server = Optional.ofNullable(launch.getServer()).filter(value -> !value.isBlank()).orElse(null);
    String path = Optional.ofNullable(launch.getPath()).filter(value -> !value.isBlank()).orElse(null);
    if (server != null && path != null) {
      return server + ":" + path;
    }
    return Optional.ofNullable(server).or(() -> Optional.ofNullable(path)).orElse(EMPTY_VALUE);
  }

  /**
   * Detail of a file task: the pattern of its files and how often it checks the folder
   */
  private String fileDetail(TaskLaunch launch) {
    List<String> parts = new ArrayList<>();
    if (launch.getPattern() != null && !launch.getPattern().isBlank()) {
      parts.add(launch.getPattern());
    }
    int every = Optional.ofNullable(launch.getRepeatNumber()).orElse(0);
    String unit = switch (Optional.ofNullable(launch.getRepeatType()).orElse(-1)) {
      case 0 -> "SECONDS";
      case 1 -> "MINUTES";
      case 2 -> "HOURS";
      default -> null;
    };
    if (unit != null && every > 0) {
      parts.add(text(WHEN + "FILE_CHECK", text(CATALOG + unit, str(every))));
    }
    return String.join(" · ", parts);
  }

  private String weekDay(int quartzDay) {
    return text(CATALOG + "WEEKDAY_" + quartzDay);
  }

  /**
   * Join the items of a list: a, b and c
   */
  private String join(List<String> items) {
    if (items.size() <= 1) {
      return items.isEmpty() ? "" : items.get(0);
    }
    return text(CATALOG + "AND", String.join(", ", items.subList(0, items.size() - 1)), items.get(items.size() - 1));
  }

  private String text(String key, String... parameters) {
    return texts.text(key, parameters);
  }

  private static String capitalize(String text) {
    return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
  }

  private static boolean isEmpty(List<String> values) {
    return values == null || values.stream().allMatch(value -> value == null || value.isBlank());
  }

  private static List<String> clean(List<String> values) {
    return values.stream().filter(value -> value != null && !value.isBlank()).map(String::trim).toList();
  }

  private static String pad(String number) {
    String value = number.trim();
    return value.length() == 1 ? "0" + value : value;
  }

  private static String str(int number) {
    return String.valueOf(number);
  }

  private static boolean anyNotNumber(List<String> values) {
    return values.stream().anyMatch(value -> !NUMBER.matcher(value).matches());
  }
}
