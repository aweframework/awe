package com.almis.awe.scheduler.util;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dates of a calendar read from a file (#993, «Importar…» of the calendar side panel), never saved here: an .ics file
 * gives every day of each event (VEVENT, from DTSTART, a date or a date and time, to DTEND or DTSTART plus a DURATION
 * in days or weeks, whose date is not included when it is a date or a midnight) with its SUMMARY as the name (not the
 * one of a component inside it, such as an alarm); a time in UTC (Z) or in a zone (TZID) is read in the zone of the
 * server before taking its day; a repeated event (RRULE) or a duration in hours is told as a line that cannot be read,
 * never reduced to one day. A .csv file gives one date per line (dd/MM/yyyy or yyyy-MM-dd) and an optional name,
 * separated by a semicolon or a comma (each line its own), with a header tolerated only on the first line with text and
 * only when it has no digit (a wrong date there is told). Each day is kept once (its first name), in order; the lines
 * that cannot be read are told by their number. A file of more than {@value #MOST_LINES} lines or more than
 * {@value #MOST_DATES} dates is not read, and an event gives at most {@value #MOST_EVENT_DAYS} days
 */
public final class CalendarDatesImport {

  /**
   * Most lines of a file
   */
  public static final int MOST_LINES = 10_000;
  /**
   * Most days of an event
   */
  public static final int MOST_EVENT_DAYS = 366;
  /**
   * Most dates of a file
   */
  public static final int MOST_DATES = 3660;
  /**
   * Most characters of a name, as the dates of a calendar store it
   */
  private static final int MOST_NAME = 40;
  private static final DateTimeFormatter ICS_DATE = DateTimeFormatter.ofPattern("uuuuMMdd")
    .withResolverStyle(ResolverStyle.STRICT);
  private static final List<DateTimeFormatter> CSV_DATES = List.of(
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
    DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));
  private static final Pattern ICS_VALUE = Pattern.compile("(\\d{8})(?:T(\\d{2})(\\d{2})(\\d{2})(Z?))?");
  private static final Pattern ICS_DAYS = Pattern.compile("\\+?P(?:(\\d{1,4})W|(\\d{1,5})D)");
  private static final Pattern ICS_TZID = Pattern.compile(";\\s*TZID=\"?([^;:\"]+)\"?", Pattern.CASE_INSENSITIVE);

  private CalendarDatesImport() {
  }

  /**
   * Locale key of the weekday of a date of the calendar side panel («dom», «vie»), as the task catalog names the
   * weekdays: from Sunday (1) to Saturday (7). The saved dates and the imported ones show the same weekday
   *
   * @param date Date
   * @return Locale key of its weekday
   */
  public static String weekdayKey(LocalDate date) {
    return "SCREEN_TEXT_TASK_CATALOG_WEEKDAY_" + (date.getDayOfWeek().getValue() % 7 + 1);
  }

  /**
   * Locale key of the weekday of a date of the dates grid of the calendar side panel, as the grid sends it (dd/MM/yyyy,
   * or yyyy-MM-dd as a .csv file may have it)
   *
   * @param date Date as text
   * @return Locale key of its weekday, none when it is empty or not a date
   */
  public static Optional<String> weekdayKeyOf(String date) {
    return date == null || date.isBlank() ? Optional.empty() : csvDate(date).map(CalendarDatesImport::weekdayKey);
  }

  /**
   * A day with its name
   *
   * @param date Day
   * @param name Name, empty when it has none
   */
  public record Day(LocalDate date, String name) {
  }

  /**
   * What a file gives
   *
   * @param dates        Days, in order, each one once
   * @param invalidLines Numbers of the lines that cannot be read, in order
   * @param tooLarge     The file has more lines or more dates than the bounds: it was not read
   * @param unsupported  The file is neither an .ics nor a .csv file: it was not read
   */
  public record Result(List<Day> dates, List<Integer> invalidLines, boolean tooLarge, boolean unsupported) {
  }

  /**
   * Read the dates of a file, the times of an .ics file in the zone of the server
   *
   * @param fileName Name of the file, whose extension tells its format
   * @param content  Content of the file
   * @return Its dates and the lines that cannot be read
   */
  public static Result parse(String fileName, String content) {
    return parse(fileName, content, ZoneId.systemDefault());
  }

  /**
   * Read the dates of a file
   *
   * @param fileName Name of the file, whose extension tells its format
   * @param content  Content of the file
   * @param zone     Zone of the server: the day of a time in UTC or in another zone is the one of this zone
   * @return Its dates and the lines that cannot be read
   */
  public static Result parse(String fileName, String content, ZoneId zone) {
    String name = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    boolean ics = name.endsWith(".ics");
    if (!ics && !name.endsWith(".csv")) {
      return new Result(List.of(), List.of(), false, true);
    }
    String text = content == null ? "" : content;
    if (text.startsWith("\uFEFF")) {
      text = text.substring(1);
    }
    String[] lines = text.split("\\R");
    if (lines.length > MOST_LINES) {
      return new Result(List.of(), List.of(), true, false);
    }
    Map<LocalDate, String> days = new LinkedHashMap<>();
    List<Integer> invalid = new ArrayList<>();
    if (ics) {
      readIcs(lines, days, invalid, zone);
    } else {
      readCsv(lines, days, invalid);
    }
    if (days.size() > MOST_DATES) {
      return new Result(List.of(), List.of(), true, false);
    }
    List<Day> dates = days.entrySet().stream().sorted(Map.Entry.comparingByKey())
      .map(day -> new Day(day.getKey(), day.getValue())).toList();
    return new Result(dates, invalid.stream().sorted(Comparator.naturalOrder()).toList(), false, false);
  }

  /**
   * Events of an .ics file: its lines unfolded (a line that starts with a space or a tab goes on with the one before)
   */
  private static void readIcs(String[] lines, Map<LocalDate, String> days, List<Integer> invalid, ZoneId zone) {
    List<String> logical = new ArrayList<>();
    List<Integer> numbers = new ArrayList<>();
    unfold(lines, logical, numbers);
    Event event = null;
    // Components inside the event (an alarm): their properties are not the ones of the event
    int nested = 0;
    for (int index = 0; index < logical.size(); index++) {
      String line = logical.get(index).trim().toUpperCase(Locale.ROOT);
      int number = numbers.get(index);
      if ("BEGIN:VEVENT".equals(line)) {
        event = new Event(number, zone);
        nested = 0;
      } else if (event != null && nested == 0 && "END:VEVENT".equals(line)) {
        event.addTo(days, invalid);
        event = null;
      } else if (event != null && line.startsWith("BEGIN:")) {
        nested++;
      } else if (event != null && nested > 0 && line.startsWith("END:")) {
        nested--;
      } else if (event != null && nested == 0) {
        event.read(logical.get(index), number);
      }
    }
  }

  /**
   * Unfold the lines of an .ics file: a line that starts with a space or a tab goes on with the one before. Each logical
   * line keeps the number of its first line
   */
  private static void unfold(String[] lines, List<String> logical, List<Integer> numbers) {
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      if (!logical.isEmpty() && (line.startsWith(" ") || line.startsWith("\t"))) {
        logical.set(logical.size() - 1, logical.get(logical.size() - 1) + line.substring(1));
      } else {
        logical.add(line);
        numbers.add(index + 1);
      }
    }
  }

  /**
   * Lines of a .csv file: a date and an optional name, separated by a semicolon or a comma (the first one out of double
   * quotes in each line). The first line with text may be a header when its first field has no digit
   */
  private static void readCsv(String[] lines, Map<LocalDate, String> days, List<Integer> invalid) {
    boolean first = true;
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index];
      if (line.isBlank()) {
        continue;
      }
      List<String> fields = fields(line, separator(line));
      Optional<LocalDate> date = csvDate(fields.get(0));
      if (date.isPresent()) {
        days.putIfAbsent(date.get(), name(fields.size() > 1 ? fields.get(1) : ""));
      } else if (!first || fields.get(0).chars().anyMatch(Character::isDigit)) {
        invalid.add(index + 1);
      }
      first = false;
    }
  }

  /**
   * Separator of a line of a .csv file: the first semicolon or comma out of double quotes, a comma when it has none
   */
  private static char separator(String line) {
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char character = line.charAt(index);
      if (character == '"') {
        quoted = !quoted;
      } else if (!quoted && (character == ';' || character == ',')) {
        return character;
      }
    }
    return ',';
  }

  /**
   * Fields of a line of a .csv file, a field between double quotes may have the separator (two double quotes are one)
   */
  private static List<String> fields(String line, char separator) {
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < line.length(); index++) {
      char character = line.charAt(index);
      if (character == '"' && quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
        field.append('"');
        index++;
      } else if (character == '"') {
        quoted = !quoted;
      } else if (character == separator && !quoted) {
        fields.add(field.toString().trim());
        field.setLength(0);
      } else {
        field.append(character);
      }
    }
    fields.add(field.toString().trim());
    return fields;
  }

  private static Optional<LocalDate> csvDate(String value) {
    for (DateTimeFormatter format : CSV_DATES) {
      try {
        return Optional.of(LocalDate.parse(value.trim(), format));
      } catch (DateTimeParseException exc) {
        // Try the next format
      }
    }
    return Optional.empty();
  }

  private static String name(String value) {
    String name = value == null ? "" : value.trim();
    return name.length() > MOST_NAME ? name.substring(0, MOST_NAME).trim() : name;
  }

  /**
   * An event of an .ics file: where it starts and ends (the day, and whether its end is a midnight) and its summary
   */
  private static final class Event {
    private final int begin;
    private final ZoneId zone;
    private LocalDate start;
    private Integer startLine;
    private LocalDate end;
    private boolean endIncluded;
    private Integer durationDays;
    private Integer invalidLine;
    private String summary = "";

    private Event(int begin, ZoneId zone) {
      this.begin = begin;
      this.zone = zone;
    }

    private void read(String line, int number) {
      int colon = valueStart(line);
      if (colon < 0) {
        return;
      }
      String name = line.substring(0, colon);
      String property = name.split(";", 2)[0].trim().toUpperCase(Locale.ROOT);
      String value = line.substring(colon + 1).trim();
      switch (property) {
        case "DTSTART" -> {
          startLine = number;
          start = dateTime(name, value).map(LocalDateTime::toLocalDate).orElse(null);
        }
        case "DTEND" -> {
          Optional<LocalDateTime> dateTime = dateTime(name, value);
          end = dateTime.map(LocalDateTime::toLocalDate).orElse(null);
          endIncluded = dateTime.map(time -> !LocalTime.MIDNIGHT.equals(time.toLocalTime())).orElse(false);
          if (end == null) {
            invalid(number);
          }
        }
        case "DURATION" -> {
          Matcher matcher = ICS_DAYS.matcher(value.toUpperCase(Locale.ROOT));
          if (matcher.matches()) {
            durationDays = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) * 7 : Integer.parseInt(matcher.group(2));
          } else {
            invalid(number);
          }
        }
        // A repeated event is not reduced to its first day: it is told
        case "RRULE", "RDATE" -> invalid(number);
        case "SUMMARY" -> summary = name(unescape(value));
        default -> {
          // Other properties are not needed
        }
      }
    }

    private void invalid(int number) {
      if (invalidLine == null) {
        invalidLine = number;
      }
    }

    /**
     * Date and time of a DTSTART or a DTEND (midnight when it is a date) in the zone of the server: a time in UTC or in
     * a zone (TZID) is moved to it, a floating time is kept; empty when it cannot be read or its zone is not known
     */
    private Optional<LocalDateTime> dateTime(String name, String value) {
      Matcher matcher = ICS_VALUE.matcher(value);
      LocalDate date = matcher.matches() ? icsDate(matcher.group(1)) : null;
      if (date == null) {
        return Optional.empty();
      }
      if (matcher.group(2) == null) {
        return Optional.of(date.atStartOfDay());
      }
      try {
        LocalDateTime local = date.atTime(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
          Integer.parseInt(matcher.group(4)));
        Matcher tzid = ICS_TZID.matcher(name);
        if (!matcher.group(5).isEmpty()) {
          return Optional.of(local.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDateTime());
        } else if (tzid.find()) {
          return Optional.of(local.atZone(ZoneId.of(tzid.group(1).trim())).withZoneSameInstant(zone)
            .toLocalDateTime());
        }
        return Optional.of(local);
      } catch (DateTimeException exc) {
        return Optional.empty();
      }
    }

    /**
     * Every day of the event, or the line that cannot be read
     */
    private void addTo(Map<LocalDate, String> days, List<Integer> invalid) {
      if (start == null) {
        invalid.add(startLine == null ? begin : startLine);
        return;
      }
      if (invalidLine != null) {
        invalid.add(invalidLine);
        return;
      }
      LocalDate last;
      if (end != null) {
        last = endIncluded ? end : end.minusDays(1);
      } else {
        last = durationDays == null || durationDays == 0 ? start : start.plusDays(durationDays - 1L);
      }
      LocalDate day = start;
      for (int count = 0; count < MOST_EVENT_DAYS && !day.isAfter(last.isBefore(start) ? start : last); count++) {
        days.putIfAbsent(day, summary);
        day = day.plusDays(1);
      }
    }

    /**
     * Position of the colon that starts the value: the first one out of double quotes (a parameter may have one)
     */
    private static int valueStart(String line) {
      boolean quoted = false;
      for (int index = 0; index < line.length(); index++) {
        char character = line.charAt(index);
        if (character == '"') {
          quoted = !quoted;
        } else if (character == ':' && !quoted) {
          return index;
        }
      }
      return -1;
    }

    private static LocalDate icsDate(String value) {
      try {
        return LocalDate.parse(value, ICS_DATE);
      } catch (DateTimeParseException exc) {
        return null;
      }
    }

    private static String unescape(String value) {
      return value.replace("\\n", " ").replace("\\N", " ").replace("\\,", ",").replace("\\;", ";")
        .replace("\\\\", "\\");
    }
  }
}
