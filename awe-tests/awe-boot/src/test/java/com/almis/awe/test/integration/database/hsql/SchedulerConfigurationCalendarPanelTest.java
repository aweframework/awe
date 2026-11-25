package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.Calendar;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calendar side panel of the scheduler configuration screen (#993, mock-up 06 artboard 2A) on a real database, through
 * the action controller as the screen launches it: «Nuevo calendario» saves the calendar with the dates of its grid and
 * adds it to the engine; saving an edited calendar (the calendar of the selected row, IdeCal.selected) changes its
 * fields, adds and removes its dates and reloads it in the engine; and its Activo switch reschedules the tasks that use
 * it, as the row menu does. The loader fills the panel and the dates grid with the stored calendar. The calendars and
 * the task of each test are removed after it.
 */
@Tag("integration")
class SchedulerConfigurationCalendarPanelTest extends AbstractSchedulerMaintainTest {

  private static final LocalDate CHRISTMAS = LocalDate.of(2098, 12, 25);
  private static final LocalDate BOXING_DAY = LocalDate.of(2098, 12, 26);
  private static final LocalDate NEW_YEAR = LocalDate.of(2099, 1, 1);

  @Autowired
  private DataSource dataSource;
  @Autowired
  private Scheduler scheduler;

  private JdbcTemplate jdbc;
  private String name;
  private final List<Integer> tasks = new ArrayList<>();

  @BeforeEach
  void setUp() {
    jdbc = new JdbcTemplate(dataSource);
    name = unique("Panel calendar");
  }

  @AfterEach
  void deleteCalendar() throws Exception {
    if (!tasks.isEmpty()) {
      deleteTasks(tasks.toArray(Integer[]::new));
    }
    for (Integer calendar : jdbc.queryForList("SELECT Ide FROM AweSchCal WHERE Nom LIKE 'Panel calendar%'",
      Integer.class)) {
      maintain("DeleteConfigurationCalendar", params().<ObjectNode>set("IdeCal", ints(calendar))
        .put("IdeCal.selected", calendar));
    }
  }

  @Test
  void aNewCalendarIsSavedWithTheDatesOfItsGridAndAddedToTheEngine() throws Exception {
    int calendar = createCalendar();

    assertThat(jdbc.queryForObject("SELECT Des FROM AweSchCal WHERE Ide = ?", String.class, calendar))
      .isEqualTo("Bank holidays");
    assertThat(jdbc.queryForObject("SELECT Act FROM AweSchCal WHERE Ide = ?", Integer.class, calendar)).isEqualTo(1);
    assertThat(storedDates(calendar)).containsExactly("2098-12-25 Christmas", "2098-12-26 Boxing day");
    Calendar engine = scheduler.getCalendar(String.valueOf(calendar));
    assertThat(engine).isNotNull();
    assertThat(engine.isTimeIncluded(noon(CHRISTMAS))).isFalse();
    assertThat(engine.isTimeIncluded(noon(NEW_YEAR))).isTrue();
  }

  @Test
  void theLoaderFillsThePanelAndTheDatesGridWithTheCalendarOfTheSelectedRow() throws Exception {
    int calendar = createCalendar();

    JsonNode actions = perform("maintain", "loadConfigurationCalendar", selected(calendar));
    List<JsonNode> dates = rows("getConfigurationCalendarPanelDates", selected(calendar));

    assertThat(selectValues(actions)).containsExactly("CalPnlNom=" + name, "CalPnlDes=Bank holidays", "CalPnlAct=1",
      "CalPnlCnt=2 dates");
    assertThat(column(dates, "Dat")).containsExactly("25/12/2098", "26/12/2098");
    assertThat(column(dates, "Nam")).containsExactly("Christmas", "Boxing day");
    assertThat(column(dates, "DatWek")).containsExactly("Thu", "Fri");
  }

  @Test
  void theDayOfADateAddedOrChangedInTheGridIsFilledByTheServer() throws Exception {
    JsonNode actions = perform("maintain-silent", "getConfigurationCalendarDateWeekday", params()
      .put("CalDatWekDat", "25/12/2098").put("CalDatWekRow", "new-row-1"));

    JsonNode update = null;
    for (JsonNode action : actions) {
      if ("update-row".equals(action.get("type").asString())) {
        update = action;
      }
    }
    assertThat(update).as("update-row in %s", actions).isNotNull();
    assertThat(update.get("target").asString()).isEqualTo("GrdDatLst");
    assertThat(update.get("parameters").get("rowId").asString()).isEqualTo("new-row-1");
    assertThat(update.get("parameters").get("row").get("DatWek").asString()).isEqualTo("Thu");
  }

  @Test
  void savingAnEditedCalendarChangesItsFieldsAddsAndRemovesItsDatesAndReloadsItInTheEngine() throws Exception {
    int calendar = createCalendar();
    List<JsonNode> dates = rows("getConfigurationCalendarPanelDates", selected(calendar));
    int christmas = dates.get(0).get("IdeDat").asInt();

    // Christmas is removed and New Year is added; Boxing day is not sent (the grid sends only its changed rows)
    ObjectNode parameters = panel(name + " v2", "Holidays", 1, selected(calendar));
    parameters.set("GrdDatLst-RowTyp", strings("DELETE", "INSERT"));
    parameters.set("IdeDat", strings(String.valueOf(christmas), ""));
    parameters.set("Dat", strings("25/12/2098", "01/01/2099"));
    parameters.set("Nam", strings("Christmas", "New Year"));
    maintain("UpdConfigurationCalendar", parameters);

    assertThat(jdbc.queryForObject("SELECT Nom FROM AweSchCal WHERE Ide = ?", String.class, calendar))
      .isEqualTo(name + " v2");
    assertThat(jdbc.queryForObject("SELECT Des FROM AweSchCal WHERE Ide = ?", String.class, calendar))
      .isEqualTo("Holidays");
    assertThat(storedDates(calendar)).containsExactly("2098-12-26 Boxing day", "2099-01-01 New Year");
    Calendar engine = scheduler.getCalendar(String.valueOf(calendar));
    assertThat(engine.isTimeIncluded(noon(CHRISTMAS))).isTrue();
    assertThat(engine.isTimeIncluded(noon(BOXING_DAY))).isFalse();
    assertThat(engine.isTimeIncluded(noon(NEW_YEAR))).isFalse();
  }

  @Test
  void theActiveSwitchOfThePanelReschedulesTheTasksThatUseTheCalendarAsTheRowMenuDoes() throws Exception {
    int calendar = createCalendar();
    int task = createTask(scheduledTask(unique("Panel calendar task"), 6).put("IdeCal", calendar));
    tasks.add(task);
    assertThat(trigger(task).getCalendarName()).isEqualTo(String.valueOf(calendar));

    maintain("UpdConfigurationCalendar", panel(name, "Bank holidays", 0, selected(calendar)));
    assertThat(jdbc.queryForObject("SELECT Act FROM AweSchCal WHERE Ide = ?", Integer.class, calendar)).isZero();
    assertThat(trigger(task).getCalendarName()).isNull();

    maintain("UpdConfigurationCalendar", panel(name, "Bank holidays", 1, selected(calendar)));
    assertThat(trigger(task).getCalendarName()).isEqualTo(String.valueOf(calendar));
  }

  /**
   * «Nuevo calendario»: no selected row, the panel and two new rows of the dates grid
   */
  private int createCalendar() throws Exception {
    ObjectNode parameters = panel(name, "Bank holidays", 1, params().set("IdeCal", ints()));
    parameters.set("GrdDatLst-RowTyp", strings("INSERT", "INSERT"));
    parameters.set("IdeDat", strings("", ""));
    parameters.set("Dat", strings("25/12/2098", "26/12/2098"));
    parameters.set("Nam", strings("Christmas", "Boxing day"));
    maintain("NewConfigurationCalendar", parameters);
    return jdbc.queryForObject("SELECT Ide FROM AweSchCal WHERE Nom = ?", Integer.class, name);
  }

  /**
   * The criteria of the panel, with an empty dates grid unless the caller sets its rows
   */
  private ObjectNode panel(String calendarName, String description, int active, ObjectNode parameters) {
    parameters.put("CalPnlNom", calendarName).put("CalPnlDes", description).put("CalPnlAct", active);
    parameters.set("GrdDatLst-RowTyp", strings());
    parameters.set("IdeDat", strings());
    parameters.set("Dat", strings());
    parameters.set("Nam", strings());
    return parameters;
  }

  /**
   * The calendar column of the calendars grid with one selected row
   */
  private ObjectNode selected(int calendar) {
    return params().<ObjectNode>set("IdeCal", ints(calendar)).put("IdeCal.selected", calendar);
  }

  private List<String> storedDates(int calendar) {
    return jdbc.queryForList("SELECT Dat, Nom FROM AweSchCalDat WHERE IdeCal = ? ORDER BY Dat", calendar).stream()
      .map(row -> ((java.util.Date) row.get("DAT")).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() + " "
        + row.get("NOM"))
      .toList();
  }

  private static List<String> selectValues(JsonNode actions) {
    List<String> values = new ArrayList<>();
    for (JsonNode action : actions) {
      if ("select".equals(action.get("type").asString())) {
        JsonNode value = action.get("parameters").get("values");
        values.add(action.get("target").asString() + "=" + (value.isArray() ? value.get(0) : value).asString());
      }
    }
    return values;
  }

  private Trigger trigger(int task) throws Exception {
    for (TriggerKey key : scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup())) {
      if (key.getName().equals(String.valueOf(task))) {
        return scheduler.getTrigger(key);
      }
    }
    throw new AssertionError("Task " + task + " has no trigger");
  }

  private static long noon(LocalDate day) {
    return day.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }
}
