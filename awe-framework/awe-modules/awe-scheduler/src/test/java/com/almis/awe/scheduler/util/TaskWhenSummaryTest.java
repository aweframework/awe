package com.almis.awe.scheduler.util;

import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * «When» summary of the task catalog (#993): a natural-language text derived from the launch type, the repetition, the
 * calendar, the file server and the parents of a task, with the English texts of the module, and the next execution
 * of a task in the same words
 */
class TaskWhenSummaryTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 10, 0);
  private static Map<String, String> locales;
  private static TaskWhenSummary summary;

  @BeforeAll
  static void load() throws Exception {
    locales = new HashMap<>();
    try (InputStream stream = TaskWhenSummaryTest.class.getClassLoader()
      .getResourceAsStream("application/awe-scheduler/locale/Locale-en-GB.xml")) {
      assertNotNull(stream);
      NodeList entries = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream)
        .getElementsByTagName("locale");
      for (int index = 0; index < entries.getLength(); index++) {
        Element entry = (Element) entries.item(index);
        locales.put(entry.getAttribute("name"), entry.getAttribute("value"));
      }
    }
    summary = new TaskWhenSummary((key, parameters) -> {
      String template = locales.get(key);
      assertNotNull(template, "missing locale " + key);
      return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
    });
  }

  @Test
  void aManualTaskIsLaunchedByHand() {
    assertWhen(new TaskLaunch().setLaunchType(0), TaskLaunchKind.MANUAL, "lucide:hand", "Launched by hand",
      "no schedule");
  }

  @Test
  void aWebhookTaskIsLaunchedWhenAWebhookArrives() {
    assertWhen(new TaskLaunch().setLaunchType(4), TaskLaunchKind.WEBHOOK, "lucide:webhook", "When a webhook arrives",
      "signed call from outside");
  }

  @Test
  void aFileTaskNamesItsServerPathPatternAndCheckInterval() {
    TaskLaunch launch = new TaskLaunch().setLaunchType(2).setServer("sftp-banks").setPath("/in/n43")
      .setPattern("*.n43").setRepeatType(1).setRepeatNumber(5);

    assertWhen(launch, TaskLaunchKind.FILE, "lucide:file-down", "When a file arrives in sftp-banks:/in/n43",
      "*.n43 · checks every 5 min");
    assertEquals("When a file arrives in /in", summary.when(new TaskLaunch().setLaunchType(2).setPath("/in")).text());
  }

  @Test
  void aTaskLaunchedAfterItsParentsNamesThemAndWhenTheyLaunchIt() {
    TaskLaunch one = new TaskLaunch().setLaunchType(0)
      .setParents(List.of(new TaskLaunch.Parent("Import statements", true, false)));
    TaskLaunch two = new TaskLaunch().setLaunchType(0).setParents(List.of(
      new TaskLaunch.Parent("Reconcile", false, false), new TaskLaunch.Parent("Update positions", false, false)));
    TaskLaunch four = new TaskLaunch().setLaunchType(0).setParents(List.of(new TaskLaunch.Parent("A", false, false),
      new TaskLaunch.Parent("B", false, false), new TaskLaunch.Parent("C", false, false),
      new TaskLaunch.Parent("D", false, false)));

    assertWhen(one, TaskLaunchKind.DEPENDENCY, "lucide:corner-down-right", "After Import statements",
      "if it ends OK or with a warning");
    assertEquals("if it ends OK", detail(parent(false, false)));
    assertEquals("if it ends OK or with an error", detail(parent(false, true)));
    assertEquals("whatever it ends with", detail(parent(true, true)));
    assertWhen(two, TaskLaunchKind.DEPENDENCY, "lucide:corner-down-right", "After Reconcile and Update positions",
      "when all 2 have ended");
    assertEquals("After A, B and 2 more", summary.when(four).text());
  }

  @Test
  void aTaskWithItsOwnLaunchAndParentsTellsItsLaunchAndItsParents() {
    TaskLaunch launch = new TaskLaunch().setLaunchType(1).setRepeatType(2).setRepeatNumber(1)
      .setParents(List.of(new TaskLaunch.Parent("Close the day", false, false)));

    assertWhen(launch, TaskLaunchKind.SCHEDULED, "lucide:clock", "Every hour", "also after Close the day");
  }

  @Test
  void aScheduledTaskWithAnIntervalRepeatsEveryInterval() {
    assertEquals("Every 30 seconds", interval(0, 30));
    assertEquals("Every minute", interval(1, 1));
    assertEquals("Every 15 minutes", interval(1, 15));
    assertEquals("Every 2 hours", interval(2, 2));
    assertWhen(new TaskLaunch().setLaunchType(1).setRepeatType(1).setRepeatNumber(15), TaskLaunchKind.SCHEDULED,
      "lucide:clock", "Every 15 minutes", "scheduled");
  }

  @Test
  void aScheduledTaskOnDaysTellsItsDaysAndTimes() {
    assertEquals("Every day at 03:00", days(1, null, List.of("3"), List.of("0")));
    assertEquals("Every 2 days at 03:00", days(2, null, List.of("3"), List.of("0")));
    assertEquals("Every day at 00:00", days(1, null, null, null));
    assertEquals("Every day at 08:00, 08:30, 20:00 and 20:30", days(1, null, List.of("8", "20"), List.of("30", "0")));
    // The editor numbers the days of the week from Monday (1) to Saturday (6) and Sunday (0), as the cron is built
    assertEquals("Weekdays at 22:30", custom(List.of("1", "2", "3", "4", "5"), List.of("22"), List.of("30")));
    assertEquals("Weekends at 10:00", custom(List.of("6", "0"), List.of("10"), List.of("0")));
    assertEquals("Mon, Wed and Fri at 08:00", custom(List.of("5", "1", "3"), List.of("8"), List.of("0")));
    assertEquals("Sun at 08:00", custom(List.of("0"), List.of("8"), List.of("0")));
    // Too many times to list: the schedule is custom
    assertEquals("Custom schedule", custom(null, List.of("*"), List.of("0")));
  }

  @Test
  void aScheduledTaskOnDaysOfTheMonthOrOnceTellsItsDates() {
    assertEquals("Monthly on day 1 and 15 at 06:00", summary.when(new TaskLaunch().setLaunchType(1).setRepeatType(7)
      .setDays(List.of("1", "15")).setHours(List.of("6")).setMinutes(List.of("0"))).text());
    assertEquals("Every 3 months on day 1 at 06:00", summary.when(new TaskLaunch().setLaunchType(1).setRepeatType(4)
      .setRepeatNumber(3).setHours(List.of("6")).setMinutes(List.of("0"))).text());
    assertEquals("Once, on 31/12/2026 at 23:00", summary.when(new TaskLaunch().setLaunchType(1).setRepeatType(6)
      .setDate(LocalDate.of(2026, 12, 31)).setTime("23:00:00")).text());
  }

  @Test
  void aScheduledTaskWithACalendarNamesItsCalendar() {
    TaskLaunch launch = new TaskLaunch().setLaunchType(1).setRepeatType(7).setWeekDays(List.of("1", "2", "3", "4", "5"))
      .setHours(List.of("22")).setMinutes(List.of("30")).setCalendar("Holidays ES 2026");

    assertWhen(launch, TaskLaunchKind.SCHEDULED, "lucide:calendar-days", "Weekdays at 22:30",
      "calendar Holidays ES 2026");
  }

  @Test
  void aDateIsToldRelativeToToday() {
    assertEquals("Today 08:05", summary.relative(LocalDateTime.of(2026, 11, 18, 8, 5), NOW));
    assertEquals("Yesterday 22:30", summary.relative(LocalDateTime.of(2026, 11, 17, 22, 30), NOW));
    assertEquals("Tomorrow 03:00", summary.relative(LocalDateTime.of(2026, 11, 19, 3, 0), NOW));
    assertEquals("02/10 08:00", summary.relative(LocalDateTime.of(2026, 10, 2, 8, 0), NOW));
    assertEquals("02/10/2025 08:00", summary.relative(LocalDateTime.of(2025, 10, 2, 8, 0), NOW));
  }

  @Test
  void theNextExecutionIsTheNextFireTimeOrWhatLaunchesTheTask() {
    TaskLaunch scheduled = new TaskLaunch().setLaunchType(1).setRepeatType(1).setRepeatNumber(5);
    TaskLaunch file = new TaskLaunch().setLaunchType(2).setServer("sftp-banks").setPath("/in");
    TaskLaunch child = new TaskLaunch().setLaunchType(0)
      .setParents(List.of(new TaskLaunch.Parent("Import statements", false, false)));
    LocalDateTime tomorrow = LocalDateTime.of(2026, 11, 19, 3, 0);

    assertEquals(new TaskWhenSummary.Line("Tomorrow 03:00", "Thu 19/11/2026"),
      summary.next(scheduled, true, tomorrow, NOW));
    assertEquals(new TaskWhenSummary.Line("Today 10:25", "next check of sftp-banks"),
      summary.next(file, true, LocalDateTime.of(2026, 11, 18, 10, 25), NOW));
    assertEquals(new TaskWhenSummary.Line("When Import statements ends", ""), summary.next(child, true, null, NOW));
    assertEquals(new TaskWhenSummary.Line("When a webhook arrives", ""),
      summary.next(new TaskLaunch().setLaunchType(4), true, null, NOW));
    assertEquals(new TaskWhenSummary.Line("—", "on demand"),
      summary.next(new TaskLaunch().setLaunchType(0), true, null, NOW));
    assertEquals(new TaskWhenSummary.Line("—", "not scheduled"), summary.next(scheduled, true, null, NOW));
    assertEquals(new TaskWhenSummary.Line("—", "task inactive"), summary.next(scheduled, false, tomorrow, NOW));
  }

  @Test
  void aDurationShowsMillisecondsOnlyUnderOneSecond() {
    assertEquals("6m 35s", TaskWhenSummary.duration(395_600L));
    assertEquals("1h 2m", TaskWhenSummary.duration(3_720_400L));
    assertEquals("8s", TaskWhenSummary.duration(8_500L));
    assertEquals("40m", TaskWhenSummary.duration(2_400_000L));
    assertEquals("850ms", TaskWhenSummary.duration(850L));
    assertEquals("0ms", TaskWhenSummary.duration(0L));
  }

  private static TaskLaunch parent(boolean onWarning, boolean onError) {
    return new TaskLaunch().setLaunchType(0).setParents(List.of(new TaskLaunch.Parent("P", onWarning, onError)));
  }

  private static String detail(TaskLaunch launch) {
    return summary.when(launch).detail();
  }

  private static String interval(int repeatType, int repeatNumber) {
    return summary.when(new TaskLaunch().setLaunchType(1).setRepeatType(repeatType).setRepeatNumber(repeatNumber))
      .text();
  }

  private static String days(int every, List<String> weekDays, List<String> hours, List<String> minutes) {
    return summary.when(new TaskLaunch().setLaunchType(1).setRepeatType(3).setRepeatNumber(every)
      .setWeekDays(weekDays).setHours(hours).setMinutes(minutes)).text();
  }

  private static String custom(List<String> weekDays, List<String> hours, List<String> minutes) {
    return summary.when(new TaskLaunch().setLaunchType(1).setRepeatType(7).setWeekDays(weekDays).setHours(hours)
      .setMinutes(minutes)).text();
  }

  private static void assertWhen(TaskLaunch launch, TaskLaunchKind kind, String icon, String text, String detail) {
    TaskWhenSummary.When when = summary.when(launch);
    assertEquals(kind, when.kind());
    assertEquals(icon, when.icon());
    assertEquals(text, when.text());
    assertEquals(detail, when.detail());
  }
}
