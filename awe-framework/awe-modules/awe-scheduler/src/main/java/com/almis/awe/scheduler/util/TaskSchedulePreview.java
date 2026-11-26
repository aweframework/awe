package com.almis.awe.scheduler.util;

import com.almis.awe.model.dto.CellData;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Live preview of the «When» of a scheduled task in the task editor (#993): the repetition in a sentence whose key parts
 * are bold (what it repeats, the calendar; a time tells its seconds when they are not 0), its next runs as tiles (the day and the date, the time and a hint: today,
 * tomorrow, the days or the holiday it skips), the first holiday of its calendar it skips and how its stored fields are
 * saved. Pure: the texts come from the locales through {@link TaskWhenSummary.Texts}, the runs from the next fire
 * times of the scheduler and the holidays from its calendar
 */
public final class TaskSchedulePreview {

  private static final String SCHEDULE = "SCREEN_TEXT_TASK_EDITOR_SCHEDULE_";
  private static final String CATALOG = "SCREEN_TEXT_TASK_CATALOG_";
  private static final String WEEKDAY = CATALOG + "WEEKDAY_";
  private static final String WHEN = "SCREEN_TEXT_TASK_WHEN_";
  private static final String PRESET = "ENUM_TASK_EDITOR_PRESET_";
  private static final DateTimeFormatter RUN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter TIME_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d)}");
  private static final Pattern NUMBER = Pattern.compile("\\d{1,2}");
  private static final int REPEAT_ONCE = 6;
  /**
   * Most days a run can skip and still tell them (a weekend and a holiday next to it)
   */
  private static final int MOST_SKIPPED_DAYS = 3;
  /**
   * Keys of the «When» summary that read better after «Runs» in the editor
   */
  private static final Map<String, String> EDITOR_WORDS = Map.of(WHEN + "WORKDAYS", SCHEDULE + "WORKDAYS");
  private static final String TITLE = "title";
  private static final String TEXT = "text";

  /**
   * What the preview is built from
   *
   * @param schedule Stored fields of the repetition (with the preset applied)
   * @param preset   Preset of the «Repetition» of the screen
   * @param calendar Name of its calendar, null without one
   * @param holidays Holidays of its calendar
   * @param next     Next runs (nextDate dd/MM/yyyy and nextTime HH:mm:ss), as the next fire times are computed
   * @param complete The next runs could be computed (the repetition is complete)
   */
  public record Input(Schedule schedule, String preset, String calendar, List<LocalDate> holidays,
                      List<Map<String, CellData>> next, boolean complete) {
  }

  /**
   * A part of the sentence, bold or not
   *
   * @param text   Text
   * @param strong Bold
   */
  private record Segment(String text, boolean strong) {
  }

  /**
   * A field of the stored repetition, with its label
   */
  private record Field(String label, List<String> values) {
  }

  private final TaskWhenSummary.Texts texts;
  private final TaskWhenSummary summary;

  /**
   * Constructor
   *
   * @param texts Texts of the preview
   */
  public TaskSchedulePreview(TaskWhenSummary.Texts texts) {
    this.texts = texts;
    this.summary = new TaskWhenSummary((key, parameters) -> texts.text(EDITOR_WORDS.getOrDefault(key, key), parameters),
      true);
  }

  /**
   * Row of the preview card: title (a JSON array of segments, a plain text while it is incomplete), tiles (a JSON array
   * of label, value and hint) with their label, description (the first holiday it skips or why there are no runs) and
   * the stored fields as details with their label
   *
   * @param input What the preview is built from
   * @param now   Current date
   * @return Row of the card
   */
  public Map<String, CellData> build(Input input, LocalDateTime now) {
    Map<String, CellData> row = new HashMap<>();
    ArrayNode tiles = JsonNodeFactory.instance.arrayNode();
    String description = "";
    String details = "";
    if (!input.complete()) {
      row.put(TITLE, new CellData(text(SCHEDULE + "INCOMPLETE")));
    } else {
      row.put(TITLE, new CellData(sentence(input).toString()));
      List<LocalDateTime> runs = input.next().stream().map(run -> LocalDateTime.parse(run.get("nextDate").getStringValue()
        + " " + run.get("nextTime").getStringValue(), RUN)).toList();
      for (int index = 0; index < runs.size(); index++) {
        tiles.add(tile(runs.get(index), index == 0 ? null : runs.get(index - 1), input.holidays(), now));
      }
      description = runs.isEmpty() ? withoutRuns(input.schedule(), now) : firstHoliday(input, now);
      details = stored(input.schedule(), input.calendar());
    }
    row.put("tiles", new CellData(tiles.toString()));
    row.put("tilesLabel", new CellData(text(SCHEDULE + "NEXT")));
    row.put("description", new CellData(description));
    row.put("details", new CellData(details));
    row.put("detailsLabel", new CellData(text(SCHEDULE + "STORED")));
    return row;
  }

  /**
   * The repetition in a sentence: what it repeats (bold), the calendar it skips (its name bold), its range and, for a
   * custom repetition, the preset it matches
   */
  private ArrayNode sentence(Input input) {
    Schedule schedule = input.schedule();
    List<Segment> segments = new ArrayList<>();
    template(segments, text(SCHEDULE + "LEAD"), lowerFirst(summary.when(launch(schedule)).text()));
    if (input.calendar() != null && !input.calendar().isBlank()) {
      segments.add(new Segment(", ", false));
      template(segments, text(SCHEDULE + "CALENDAR"), input.calendar());
    }
    if (!Integer.valueOf(REPEAT_ONCE).equals(schedule.getRepeatType())) {
      Optional<String> from = rangeDate(schedule.getInitialDate(), schedule.getInitialTime());
      Optional<String> to = rangeDate(schedule.getEndDate(), schedule.getEndTime());
      if (from.isPresent()) {
        segments.add(new Segment(", " + text(SCHEDULE + "FROM", from.get()) + " "
          + to.map(date -> text(SCHEDULE + "TO", date)).orElseGet(() -> text(SCHEDULE + "NO_END")), false));
      } else {
        to.ifPresent(date -> segments.add(new Segment(", " + text(SCHEDULE + "TO", date), false)));
      }
    }
    segments.add(new Segment(".", false));
    String matched = TaskSchedulePreset.detect(schedule).getPreset();
    if (TaskSchedulePreset.CUSTOM.equals(input.preset()) && !TaskSchedulePreset.CUSTOM.equals(matched)) {
      segments.add(new Segment(" " + text(SCHEDULE + "MATCHES", text(PRESET + matched)), false));
    }
    ArrayNode array = JsonNodeFactory.instance.arrayNode();
    StringBuilder plain = new StringBuilder();
    for (Segment segment : segments) {
      if (segment.strong()) {
        flush(array, plain);
        array.addObject().put(TEXT, segment.text()).put("strong", true);
      } else {
        plain.append(segment.text());
      }
    }
    flush(array, plain);
    return array;
  }

  /**
   * Segments of a locale whose parameters are bold: the text around each parameter is plain
   */
  private static void template(List<Segment> segments, String template, String... strong) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    int start = 0;
    while (matcher.find()) {
      segments.add(new Segment(template.substring(start, matcher.start()), false));
      int index = Integer.parseInt(matcher.group(1));
      segments.add(new Segment(index < strong.length ? strong[index] : "", true));
      start = matcher.end();
    }
    segments.add(new Segment(template.substring(start), false));
  }

  private static void flush(ArrayNode array, StringBuilder plain) {
    if (!plain.isEmpty()) {
      array.addObject().put(TEXT, plain.toString());
      plain.setLength(0);
    }
  }

  /**
   * Tile of a run: its day of the week and its date, its time (with the seconds only when it has them) and a hint:
   * today, tomorrow, or the holiday or the days it skips since the run before it
   */
  private ObjectNode tile(LocalDateTime run, LocalDateTime previous, List<LocalDate> holidays, LocalDateTime now) {
    LocalDate day = run.toLocalDate();
    ObjectNode tile = JsonNodeFactory.instance.objectNode()
      .put("label", weekDay(day) + " " + (day.getYear() == now.getYear() ? DAY_MONTH : DATE).format(day))
      .put("value", (run.getSecond() == 0 ? TIME : TIME_SECONDS).format(run));
    hint(day, previous, holidays, now).ifPresent(hint -> tile.put("hint", hint));
    return tile;
  }

  private Optional<String> hint(LocalDate day, LocalDateTime previous, List<LocalDate> holidays, LocalDateTime now) {
    if (day.equals(now.toLocalDate())) {
      return Optional.of(text(SCHEDULE + "TODAY"));
    } else if (day.equals(now.toLocalDate().plusDays(1))) {
      return Optional.of(text(SCHEDULE + "TOMORROW"));
    } else if (previous == null) {
      return Optional.empty();
    }
    long gap = ChronoUnit.DAYS.between(previous.toLocalDate(), day);
    if (gap < 2 || gap > MOST_SKIPPED_DAYS + 1) {
      return Optional.empty();
    }
    List<LocalDate> skipped = previous.toLocalDate().plusDays(1).datesUntil(day).toList();
    if (skipped.stream().anyMatch(holidays::contains)) {
      return Optional.of(text(SCHEDULE + "SKIPS_HOLIDAY"));
    }
    return Optional.of(text(SCHEDULE + "SKIPS", join(skipped.stream().map(this::weekDay).toList())));
  }

  /**
   * Why a repetition has no next runs: its end has passed (it never runs again), or its range leaves none
   */
  private String withoutRuns(Schedule schedule, LocalDateTime now) {
    LocalDate endDay = toLocalDate(schedule.getEndDate());
    if (endDay != null) {
      String[] parts = schedule.getEndTime() == null || schedule.getEndTime().isBlank() ? new String[]{"0"}
        : schedule.getEndTime().trim().split(":");
      LocalDateTime end = endDay.atTime(Integer.parseInt(parts[0]), parts.length > 1 ? Integer.parseInt(parts[1]) : 0,
        parts.length > 2 ? Integer.parseInt(parts[2]) : 0);
      if (end.isBefore(now)) {
        return text(SCHEDULE + "ENDED", rangeDate(schedule.getEndDate(), schedule.getEndTime()).orElse(""));
      }
    }
    return text(SCHEDULE + "NONE");
  }

  /**
   * The first holiday of the calendar the repetition skips: the first one from today (and from its start) on a day it
   * would run (its days of the week and of the month), up to its end
   */
  private String firstHoliday(Input input, LocalDateTime now) {
    if (input.calendar() == null || input.calendar().isBlank()) {
      return "";
    }
    Schedule schedule = input.schedule();
    LocalDate from = Optional.ofNullable(toLocalDate(schedule.getInitialDate())).filter(date -> date.isAfter(now.toLocalDate()))
      .orElse(now.toLocalDate());
    LocalDate to = toLocalDate(schedule.getEndDate());
    List<String> weekDays = clean(schedule.getWeekDayList());
    List<String> days = clean(schedule.getDayList());
    return input.holidays().stream().sorted()
      .filter(date -> !date.isBefore(from) && (to == null || !date.isAfter(to)))
      .filter(date -> weekDays.isEmpty() || weekDays.contains(String.valueOf(date.getDayOfWeek().getValue() % 7)))
      .filter(date -> days.isEmpty() || days.contains(String.valueOf(date.getDayOfMonth())))
      .findFirst()
      .map(date -> text(SCHEDULE + "FIRST_HOLIDAY", weekDay(date) + " " + DATE.format(date), input.calendar()))
      .orElse("");
  }

  /**
   * How the repetition is saved: its stored fields with their values, the empty lists together (all), its calendar and
   * what each preset writes
   */
  private String stored(Schedule schedule, String calendar) {
    List<String> parts = new ArrayList<>();
    int repeatType = Optional.ofNullable(schedule.getRepeatType()).orElse(-1);
    String field = SCHEDULE + "STORED_FIELD";
    if (repeatType >= 0 && repeatType <= 2) {
      String unit = List.of("SECONDS", "MINUTES", "HOURS").get(repeatType);
      parts.add(text(field, text("PARAMETER_REPEAT_EACH"), text(CATALOG + unit,
        String.valueOf(Optional.ofNullable(schedule.getRepeatNumber()).orElse(0)))));
    } else if (repeatType == REPEAT_ONCE) {
      parts.add(text(field, text("PARAMETER_TASK_EDITOR_DATE"), Optional.ofNullable(toLocalDate(schedule.getDate()))
        .map(DATE::format).orElse(TaskWhenSummary.EMPTY_VALUE)));
      parts.add(text(field, text("PARAMETER_TASK_EDITOR_TIME"), Optional.ofNullable(schedule.getTime())
        .orElse(TaskWhenSummary.EMPTY_VALUE)));
    } else {
      List<Field> fields = List.of(
        new Field(text("PARAMETER_DAYS_OF_WEEK"), clean(schedule.getWeekDayList()).stream().map(this::editorWeekDay).toList()),
        new Field(text("PARAMETER_DAYS"), clean(schedule.getDayList())),
        new Field(text("PARAMETER_HOURS"), clean(schedule.getHourList())),
        new Field(text("PARAMETER_MINUTES"), clean(schedule.getMinuteList())),
        new Field(text("PARAMETER_SECONDS"), clean(schedule.getSecondList())),
        new Field(text("PARAMETER_YEARS"), clean(schedule.getYearList())),
        new Field(text("PARAMETER_MONTHS"), clean(schedule.getMonthList())),
        new Field(text("PARAMETER_WEEKS"), clean(schedule.getWeekList())));
      fields.stream().filter(item -> !item.values().isEmpty())
        .forEach(item -> parts.add(text(field, item.label(), join(item.values()))));
      List<String> all = List.of(5, 6, 7, 1, 0, 2, 3, 4).stream().map(fields::get)
        .filter(item -> item.values().isEmpty()).map(Field::label).toList();
      if (!all.isEmpty()) {
        parts.add(text(SCHEDULE + "STORED_ALL", join(all)));
      }
    }
    if (calendar != null && !calendar.isBlank()) {
      parts.add(text(field, text("PARAMETER_CALENDAR"), calendar));
    }
    return String.join(" · ", parts) + ". " + text(SCHEDULE + "STORED_PRESETS");
  }

  /**
   * Launch of the «When» summary for the stored fields of a scheduled task
   */
  private static TaskLaunch launch(Schedule schedule) {
    return new TaskLaunch()
      .setLaunchType(TaskLaunchType.SCHEDULED.getValue())
      .setRepeatType(schedule.getRepeatType())
      .setRepeatNumber(schedule.getRepeatNumber())
      .setWeekDays(schedule.getWeekDayList())
      .setDays(schedule.getDayList())
      .setHours(schedule.getHourList())
      .setMinutes(schedule.getMinuteList())
      .setSeconds(schedule.getSecondList())
      .setDate(toLocalDate(schedule.getDate()))
      .setTime(schedule.getTime());
  }

  /**
   * The summary starts a sentence: it goes after «Runs» in lower case, unless it starts with the name of a day of the
   * week, which keeps its case
   */
  private String lowerFirst(String text) {
    boolean dayName = java.util.stream.IntStream.rangeClosed(1, 7).mapToObj(day -> text(WEEKDAY + day))
      .anyMatch(name -> !name.isEmpty() && text.startsWith(name));
    return text.isEmpty() || dayName ? text : text.substring(0, 1).toLowerCase(java.util.Locale.ROOT) + text.substring(1);
  }

  private Optional<String> rangeDate(java.util.Date date, String time) {
    LocalDate day = toLocalDate(date);
    if (day == null) {
      return Optional.empty();
    }
    String[] parts = time == null || time.isBlank() ? new String[]{"0"} : time.trim().split(":");
    int hour = Integer.parseInt(parts[0]);
    int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
    return Optional.of(hour == 0 && minute == 0 ? DATE.format(day) : DATE.format(day) + " " + TIME.format(day.atTime(hour, minute)));
  }

  private String weekDay(LocalDate date) {
    return text(WEEKDAY + (date.getDayOfWeek().getValue() % 7 + 1));
  }

  /**
   * Name of a day of the week as the editor numbers it (Sunday 0), the value itself when it is not one
   */
  private String editorWeekDay(String day) {
    return NUMBER.matcher(day).matches() && Integer.parseInt(day) <= 7
      ? text(WEEKDAY + (Integer.parseInt(day) % 7 + 1)) : day;
  }

  private String join(List<String> items) {
    if (items.size() <= 1) {
      return items.isEmpty() ? "" : items.get(0);
    }
    return text(CATALOG + "AND", String.join(", ", items.subList(0, items.size() - 1)), items.get(items.size() - 1));
  }

  private static LocalDate toLocalDate(java.util.Date date) {
    if (date == null) {
      return null;
    }
    return date instanceof java.sql.Date sqlDate ? sqlDate.toLocalDate()
      : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private static List<String> clean(List<String> values) {
    return values == null ? List.of() : values.stream().filter(value -> value != null && !value.isBlank())
      .map(String::trim).toList();
  }

  private String text(String key, String... parameters) {
    return texts.text(key, parameters);
  }
}
