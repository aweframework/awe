package com.almis.awe.scheduler.dao.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Whether a text is a value of a task parameter type (the types of the SchParTyp enumerated). The text types (STRING,
 * OBJECT) and an unknown type accept any text, blank included; every other type needs a value of its own: a whole
 * number in range (INTEGER, LONG), a decimal number with a dot (FLOAT, DOUBLE), a date dd/MM/yyyy or yyyy-MM-dd (DATE)
 * and a time HH:mm or HH:mm:ss (TIME). A value of a type that is not text is read without its surrounding blanks
 * ({@link #normalize(String, String)}).
 */
final class ParameterValues {

  private static final Set<String> TEXT_TYPES = Set.of("STRING", "OBJECT");
  private static final Set<String> TYPED = Set.of("INTEGER", "LONG", "FLOAT", "DOUBLE", "DATE", "TIME");
  // A whole part, a fraction or both: 3, 0.5, .5
  private static final Pattern DECIMAL = Pattern.compile("[-+]?(\\d+(\\.\\d+)?|\\.\\d+)");
  private static final List<DateTimeFormatter> DATES = List.of(
    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
    DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT));
  private static final List<DateTimeFormatter> TIMES = List.of(
    DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT),
    DateTimeFormatter.ofPattern("HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));

  private ParameterValues() {
  }

  /**
   * Value of a parameter as it is checked and used: a type that is not text (numbers, dates, times) without the blanks
   * around it; a text type or an unknown one as it is
   *
   * @param type  Parameter type (SchParTyp value); null for none
   * @param value Text; null for none
   * @return Value to check and use
   */
  static String normalize(String type, String value) {
    String kind = type == null ? "" : type.toUpperCase(Locale.ROOT);
    return value == null || TEXT_TYPES.contains(kind) || !TYPED.contains(kind) ? value : value.strip();
  }

  /**
   * Whether the text is a value of the type
   *
   * @param type  Parameter type (SchParTyp value); null for none
   * @param value Text
   * @return The text is a value of the type
   */
  static boolean isValid(String type, String value) {
    String kind = type == null ? "" : type.toUpperCase(Locale.ROOT);
    if (TEXT_TYPES.contains(kind)) {
      return true;
    }
    boolean blank = value == null || value.isBlank();
    return switch (kind) {
      case "INTEGER" -> !blank && parses(() -> Integer.parseInt(value));
      case "LONG" -> !blank && parses(() -> Long.parseLong(value));
      case "FLOAT", "DOUBLE" -> !blank && DECIMAL.matcher(value).matches();
      case "DATE" -> !blank && DATES.stream().anyMatch(format -> parses(() -> LocalDate.parse(value, format)));
      case "TIME" -> !blank && TIMES.stream().anyMatch(format -> parses(() -> LocalTime.parse(value, format)));
      default -> true;
    };
  }

  private static boolean parses(Runnable parse) {
    try {
      parse.run();
      return true;
    } catch (NumberFormatException | DateTimeParseException exc) {
      return false;
    }
  }
}
