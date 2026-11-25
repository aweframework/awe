package com.almis.awe.scheduler.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dates of a calendar read from an .ics or a .csv file (#993, «Importar…»): every day of each event or line with its
 * name, in order and each day once; the lines that cannot be read are told by their number; a file over the bound is
 * not read
 */
class CalendarDatesImportTest {

  @Test
  void anIcsFileGivesTheDaysOfItsEventsWithTheirSummary() {
    String ics = String.join("\r\n",
      "BEGIN:VCALENDAR",
      "BEGIN:VEVENT",
      "DTSTART;VALUE=DATE:20261208",
      "SUMMARY:Inmaculada\\, Concepción",
      "END:VEVENT",
      "BEGIN:VEVENT",
      "DTSTART:20261224T090000",
      "DTEND:20261226T000000",
      "SUMMARY:Navi",
      " dad",
      "END:VEVENT",
      "BEGIN:VEVENT",
      "DTSTART;VALUE=DATE:20261208",
      "SUMMARY:Repeated",
      "END:VEVENT",
      "BEGIN:VEVENT",
      "DTSTART;VALUE=DATE:2026-13-01",
      "SUMMARY:Broken",
      "END:VEVENT",
      "BEGIN:VEVENT",
      "SUMMARY:No start",
      "END:VEVENT",
      "END:VCALENDAR");

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.ICS", ics);

    assertThat(result.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Inmaculada, Concepción"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 24), "Navidad"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 25), "Navidad"));
    assertThat(result.invalidLines()).containsExactly(17, 20);
    assertThat(result.tooLarge()).isFalse();
  }

  @Test
  void aCsvFileGivesItsDatesInBothFormatsWithAnOptionalNameAndToleratesAHeader() {
    String csv = String.join("\n",
      "Fecha;Nombre",
      "08/12/2026;Inmaculada",
      "\"2026-12-25\";\"Navidad; día\"",
      "",
      "06/12/2026",
      "25/12/2026;Repeated",
      "31/02/2026;Wrong day",
      "tomorrow");

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.csv", csv);

    assertThat(result.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 6), ""),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Inmaculada"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 25), "Navidad; día"));
    assertThat(result.invalidLines()).containsExactly(7, 8);
  }

  @Test
  void aFileWithMoreLinesThanTheBoundIsNotRead() {
    String csv = "08/12/2026\n".repeat(CalendarDatesImport.MOST_LINES + 1);

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.csv", csv);

    assertThat(result.tooLarge()).isTrue();
    assertThat(result.dates()).isEmpty();
  }

  @Test
  void aFileThatIsNeitherIcsNorCsvIsNotSupported() {
    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.xlsx", "08/12/2026");

    assertThat(result.unsupported()).isTrue();
    assertThat(result.dates()).isEmpty();
  }

  @Test
  void aFileWithMoreDatesThanTheBoundIsNotRead() {
    LocalDate first = LocalDate.of(2020, 1, 1);
    StringBuilder csv = new StringBuilder();
    for (int day = 0; day <= CalendarDatesImport.MOST_DATES; day++) {
      csv.append(first.plusDays(day)).append('\n');
    }

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.csv", csv.toString());

    assertThat(CalendarDatesImport.MOST_DATES).isEqualTo(3660);
    assertThat(result.tooLarge()).isTrue();
    assertThat(result.dates()).isEmpty();
  }

  @Test
  void eachLineOfACsvFileHasItsOwnSeparator() {
    String csv = String.join("\n", "08/12/2026,Inmaculada", "25/12/2026;Navidad, día", "06/12/2026,\"Constitución; día\"");

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.csv", csv);

    assertThat(result.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 6), "Constitución; día"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Inmaculada"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 25), "Navidad, día"));
    assertThat(result.invalidLines()).isEmpty();
  }

  @Test
  void onlyTheFirstLineWithTextMayBeAHeaderAndAWrongDateThereIsReported() {
    CalendarDatesImport.Result header = CalendarDatesImport.parse("holidays.csv",
      String.join("\n", "", "Fecha;Nombre", "08/12/2026;Inmaculada", "Fecha;Nombre"));
    CalendarDatesImport.Result wrongDate = CalendarDatesImport.parse("holidays.csv",
      String.join("\n", "31/02/2026;Wrong day", "08/12/2026;Inmaculada"));

    assertThat(header.dates()).containsExactly(new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Inmaculada"));
    assertThat(header.invalidLines()).containsExactly(4);
    assertThat(wrongDate.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Inmaculada"));
    assertThat(wrongDate.invalidLines()).containsExactly(1);
  }

  @Test
  void aRepeatedEventIsReportedAndADurationInDaysGivesItsDays() {
    String ics = String.join("\n",
      "BEGIN:VEVENT", "DTSTART;VALUE=DATE:20261208", "RRULE:FREQ=YEARLY", "SUMMARY:Every year", "END:VEVENT",
      "BEGIN:VEVENT", "DTSTART;VALUE=DATE:20261224", "DURATION:P2D", "SUMMARY:Navidad", "END:VEVENT",
      "BEGIN:VEVENT", "DTSTART:20261231T100000", "DURATION:PT2H", "SUMMARY:Hours", "END:VEVENT");

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.ics", ics);

    assertThat(result.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 24), "Navidad"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 25), "Navidad"));
    assertThat(result.invalidLines()).containsExactly(3, 13);
  }

  @Test
  void theSummaryOfAnAlarmIsNotTheNameOfItsEvent() {
    String ics = String.join("\n", "BEGIN:VEVENT", "DTSTART;VALUE=DATE:20261208", "SUMMARY:Inmaculada",
      "BEGIN:VALARM", "ACTION:DISPLAY", "SUMMARY:Reminder", "DTSTART:20990101T000000", "END:VALARM", "END:VEVENT");

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.ics", ics);

    assertThat(result.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Inmaculada"));
    assertThat(result.invalidLines()).isEmpty();
  }

  @Test
  void aTimeInUtcOrInAnotherZoneIsTheDayOfTheServerZoneAndAnEndAtMidnightIsNotIncluded() {
    String ics = String.join("\n",
      "BEGIN:VEVENT", "DTSTART:20261207T230000Z", "DTEND:20261208T230000Z", "SUMMARY:Utc", "END:VEVENT",
      "BEGIN:VEVENT", "DTSTART;TZID=America/New_York:20261224T200000", "SUMMARY:New York", "END:VEVENT",
      "BEGIN:VEVENT", "DTSTART;TZID=Nowhere/Unknown:20261231T200000", "SUMMARY:Unknown", "END:VEVENT");

    CalendarDatesImport.Result result = CalendarDatesImport.parse("holidays.ics", ics, ZoneId.of("Europe/Madrid"));

    assertThat(result.dates()).containsExactly(
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 8), "Utc"),
      new CalendarDatesImport.Day(LocalDate.of(2026, 12, 25), "New York"));
    assertThat(result.invalidLines()).containsExactly(11);
  }

  @Test
  void theDaysOfALongEventAreBounded() {
    String ics = String.join("\n", "BEGIN:VEVENT", "DTSTART;VALUE=DATE:20260101", "DTEND;VALUE=DATE:20300101",
      "END:VEVENT");

    List<CalendarDatesImport.Day> dates = CalendarDatesImport.parse("long.ics", ics).dates();

    assertThat(dates).hasSize(CalendarDatesImport.MOST_EVENT_DAYS);
  }

  @Test
  void theWeekdayOfADateOfTheGridIsTheOneOfTheSavedDatesAndNoneWhenItIsNotADate() {
    // 25/12/2026 is a Friday (6, from Sunday 1), 27/12/2026 a Sunday (1); the grid sends dd/MM/yyyy
    assertThat(CalendarDatesImport.weekdayKeyOf("25/12/2026")).contains("SCREEN_TEXT_TASK_CATALOG_WEEKDAY_6");
    assertThat(CalendarDatesImport.weekdayKeyOf(" 27/12/2026 ")).contains("SCREEN_TEXT_TASK_CATALOG_WEEKDAY_1");
    assertThat(CalendarDatesImport.weekdayKeyOf("2026-12-28")).contains("SCREEN_TEXT_TASK_CATALOG_WEEKDAY_2");
    assertThat(CalendarDatesImport.weekdayKeyOf("31/02/2026")).isEmpty();
    assertThat(CalendarDatesImport.weekdayKeyOf("")).isEmpty();
    assertThat(CalendarDatesImport.weekdayKeyOf(null)).isEmpty();
  }
}
