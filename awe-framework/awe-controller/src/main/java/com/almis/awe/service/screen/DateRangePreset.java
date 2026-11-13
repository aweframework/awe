package com.almis.awe.service.screen;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quick presets of the date range criterion used as its default value (value="YTD"). Same codes and dates as the
 * clients (utilities/dates.ts in React, services/dateRange.js in AngularJS): TODAY, YTD, ALL and nD, nW, nM, nY
 */
final class DateRangePreset {
  private static final Pattern PERIOD = Pattern.compile("^(\\d+)([DWMY])$");
  private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private DateRangePreset() {
  }

  /**
   * Resolve a preset code into its start and end dates
   *
   * @param code  Preset code
   * @param today Current date
   * @return Start and end as dd/mm/yyyy (empty list for ALL), or empty when the code is not a preset
   */
  static Optional<List<String>> resolve(String code, LocalDate today) {
    String preset = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    Matcher period = PERIOD.matcher(preset);
    return switch (preset) {
      case "ALL" -> Optional.of(List.of());
      case "TODAY" -> Optional.of(range(today, today));
      case "YTD" -> Optional.of(range(today.withDayOfYear(1), today));
      default -> period.matches() ? Optional.of(range(subtract(today, Integer.parseInt(period.group(1)), period.group(2)), today)) : Optional.empty();
    };
  }

  private static LocalDate subtract(LocalDate date, int amount, String unit) {
    // minusMonths and minusYears keep the last day of the month when the day does not exist, as the clients do
    return switch (unit) {
      case "D" -> date.minusDays(amount);
      case "W" -> date.minusWeeks(amount);
      case "M" -> date.minusMonths(amount);
      default -> date.minusYears(amount);
    };
  }

  private static List<String> range(LocalDate from, LocalDate to) {
    return List.of(from.format(FORMAT), to.format(FORMAT));
  }
}
