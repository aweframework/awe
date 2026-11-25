package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calendars tab of the scheduler configuration screen (#993) on a real database, through the action controller as the
 * screen launches it: the row menu activates, deactivates and deletes the calendar of the calendars grid (IdeCal, never
 * the Ide of the servers grid); deleting a calendar in use deletes its dates and takes it out of the launch of its
 * tasks, also of a task that has no trigger in the engine; the delete dialog names the calendar and how many tasks use
 * it. Calendars and tasks of their own, far from the ids of the sequences and outside Quartz, are stored and removed
 * after each test.
 */
@Tag("integration")
class SchedulerConfigurationCalendarTest extends AbstractSchedulerMaintainTest {

  private static final int USED = 9421;
  private static final int FREE = 9422;
  private static final int TASK = 9421;

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbc;

  @BeforeEach
  void storeCalendars() {
    jdbc = new JdbcTemplate(dataSource);
    deleteCalendars();
    jdbc.update("INSERT INTO AweSchCal (Ide, Des, Act, Nom) VALUES (?, 'probe', 1, 'Calendar Probe Used')", USED);
    jdbc.update("INSERT INTO AweSchCal (Ide, Des, Act, Nom) VALUES (?, 'probe', 1, 'Calendar Probe Free')", FREE);
    LocalDate day = LocalDate.of(2098, 12, 25);
    jdbc.update("INSERT INTO AweSchCalDat (Ide, IdeCal, Nom, Dat) VALUES (9421, ?, 'Christmas', ?)", USED,
      Date.valueOf(day));
    jdbc.update("INSERT INTO AweSchCalDat (Ide, IdeCal, Nom, Dat) VALUES (9422, ?, 'Boxing day', ?)", USED,
      Date.valueOf(day.plusDays(1)));
    // An inactive scheduled task: it uses the calendar but has no trigger in the engine
    jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, Des, TypExe, CmdExe, TypLch, Act) "
      + "VALUES (?, 'Calendar Probe Close', 'probe', 0, 'probe', 1, 0)", TASK);
    jdbc.update("INSERT INTO AweSchTskLch (Ide, IdeTsk, RptTyp, IdeCal, WeekDays, ExeHrs, ExeMin, ExeSec) "
      + "VALUES (?, ?, 7, ?, '1,2,3,4,5', '22', '30', '0')", TASK, TASK, USED);
  }

  @AfterEach
  void deleteCalendars() {
    jdbc.update("DELETE FROM AweSchTskLch WHERE IdeTsk = ?", TASK);
    jdbc.update("DELETE FROM AweSchTsk WHERE Ide = ?", TASK);
    jdbc.update("DELETE FROM AweSchCalDat WHERE IdeCal IN (?, ?)", USED, FREE);
    jdbc.update("DELETE FROM AweSchCal WHERE Ide IN (?, ?)", USED, FREE);
  }

  @Test
  void theRowMenuDeactivatesAndActivatesTheCalendarOfTheCalendarsGridOnly() throws Exception {
    // The servers grid sends its own Ide: the calendar targets never read it
    maintain("DeaConfigurationCalendar", selectedRow(FREE).set("Ide", ints(USED)));
    assertThat(active()).containsExactly(1, 0);

    maintain("ActConfigurationCalendar", selectedRow(FREE).set("Ide", ints(USED)));
    assertThat(active()).containsExactly(1, 1);
  }

  @Test
  void deletingACalendarInUseDeletesItsDatesAndTakesItOutOfItsTasks() throws Exception {
    maintain("DeleteConfigurationCalendar", selectedRow(USED).set("Ide", ints(FREE)));

    assertThat(jdbc.queryForList("SELECT Ide FROM AweSchCal WHERE Ide IN (?, ?)", Integer.class, USED, FREE))
      .containsExactly(FREE);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AweSchCalDat WHERE IdeCal = ?", Integer.class, USED))
      .isZero();
    assertThat(jdbc.queryForObject("SELECT IdeCal FROM AweSchTskLch WHERE IdeTsk = ?", Integer.class, TASK)).isNull();
  }

  @Test
  void theDeleteDialogNamesTheCalendarHowManyTasksUseItAndWhenEachOneRuns() throws Exception {
    List<JsonNode> summary = rows("getConfigurationCalendarDeleteSummary", params().set("IdeCal", ints(USED)));
    List<JsonNode> tasks = rows("getConfigurationCalendarDeleteCheck", params().set("IdeCal", ints(USED)));

    assertThat(column(summary, "title")).containsExactly("Delete «Calendar Probe Used»?");
    assertThat(column(summary, "description")).containsExactly("This calendar is used by 1 task. If you delete "
      + "it, the task is left without a calendar and also runs on these days.");
    assertThat(column(tasks, "CalDelTsk")).containsExactly("Calendar Probe Close");
    // An inactive task runs no day by itself: only when it runs
    assertThat(column(tasks, "CalDelSub")).containsExactly("Weekdays at 22:30");
  }

  /**
   * The calendar column of the calendars grid as the grid sends it with one selected row: the values of the selected
   * rows only, and .selected the value of the selected row
   */
  private ObjectNode selectedRow(int calendar) {
    return params().<ObjectNode>set("IdeCal", ints(calendar)).put("IdeCal.selected", calendar);
  }

  private List<Integer> active() {
    return jdbc.queryForList("SELECT Act FROM AweSchCal WHERE Ide IN (?, ?) ORDER BY Ide", Integer.class, USED, FREE);
  }
}
