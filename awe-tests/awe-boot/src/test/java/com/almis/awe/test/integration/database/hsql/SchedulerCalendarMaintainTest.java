package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maintains of the scheduler calendar screens (new, update, deactivate, activate and delete a calendar with its dates),
 * checked through the queries that the calendar screens load. Each test creates its own calendar and deletes it.
 * <p>
 * Replaces the browser flow {@code SchedulerCalendarTestsIT} (t001 new, t002 update with dates, t003 deactivate and
 * activate, t901 delete): the screens only send these maintains and show these queries.
 */
@Tag("integration")
class SchedulerCalendarMaintainTest extends AbstractSchedulerMaintainTest {

  private static final String INSERT = "INSERT";

  @Test
  void newCalendarStoresItsDataAndDates() throws Exception {
    String name = unique("Calendar new");
    int calendarId = createCalendar(name);
    try {
      JsonNode row = calendarListRow(calendarId);
      assertThat(row.get("Nom").asString()).isEqualTo(name);
      assertThat(row.get("Des").asString()).isEqualTo(name + " description");
      assertThat(row.get("Act").asString()).isEqualTo("1");

      // The update screen loads the calendar and its dates, ordered by date
      JsonNode calendar = rows("SchCal", params().put("Ide.selected", calendarId)).get(0);
      assertThat(calendar.get("Nom").asString()).isEqualTo(name);
      assertThat(calendar.get("Act").asString()).isEqualTo("1");
      List<JsonNode> dates = calendarDates(calendarId);
      assertThat(column(dates, "Nam")).containsExactly("First day", "Third day");
      assertThat(column(dates, "Dat")).containsExactly("01/12/2098", "03/12/2098");
    } finally {
      deleteCalendar(calendarId);
    }
  }

  @Test
  void updateCalendarChangesItsDataAndDates() throws Exception {
    String name = unique("Calendar update");
    int calendarId = createCalendar(name);
    try {
      List<JsonNode> dates = calendarDates(calendarId);
      int firstDate = dates.get(0).get("IdeDat").asInt();
      int thirdDate = dates.get(1).get("IdeDat").asInt();

      // The grid of dates sends its operations: change the first date, delete the third one and add a new one
      ObjectNode parameters = params()
        .put("CalIde", calendarId)
        .put("Nom", name + " updated")
        .put("Des", "Description updated")
        .put("Act", 1);
      parameters.set("GrdDatLst-RowTyp", strings("UPDATE", "DELETE", INSERT));
      parameters.set("IdeDat", ints(firstDate, thirdDate, null));
      parameters.set("Nam", strings("First day updated", "Third day", "Fifth day"));
      parameters.set("Dat", strings("02/12/2098", "03/12/2098", "05/12/2098"));
      maintain("UpdSchCal", parameters);

      JsonNode calendar = rows("SchCal", params().put("Ide.selected", calendarId)).get(0);
      assertThat(calendar.get("Nom").asString()).isEqualTo(name + " updated");
      assertThat(calendar.get("Des").asString()).isEqualTo("Description updated");
      List<JsonNode> updated = calendarDates(calendarId);
      assertThat(column(updated, "Nam")).containsExactly("First day updated", "Fifth day");
      assertThat(column(updated, "Dat")).containsExactly("02/12/2098", "05/12/2098");
      assertThat(updated.get(0).get("IdeDat").asInt()).isEqualTo(firstDate);
    } finally {
      deleteCalendar(calendarId);
    }
  }

  @Test
  void deactivateAndActivateCalendarChangeItsState() throws Exception {
    String name = unique("Calendar state");
    int calendarId = createCalendar(name);
    try {
      maintain("DeaSchCal", params().set("Ide", ints(calendarId)));
      assertThat(calendarListRow(calendarId).get("Act").asString()).isEqualTo("0");
      assertThat(calendarIds("0")).contains(calendarId);
      assertThat(calendarIds("1")).doesNotContain(calendarId);

      maintain("ActSchCal", params().set("Ide", ints(calendarId)));
      assertThat(calendarListRow(calendarId).get("Act").asString()).isEqualTo("1");
      assertThat(calendarIds("1")).contains(calendarId);
      assertThat(calendarIds("0")).doesNotContain(calendarId);
    } finally {
      deleteCalendar(calendarId);
    }
  }

  @Test
  void deleteCalendarRemovesItAndItsDates() throws Exception {
    int calendarId = createCalendar(unique("Calendar delete"));

    deleteCalendar(calendarId);

    assertThat(find(rows("SchCalLst", params()), "Ide", String.valueOf(calendarId))).isEmpty();
    assertThat(calendarDates(calendarId)).isEmpty();
  }

  /**
   * Create an active calendar with two dates, as the new calendar screen does
   *
   * @param name Calendar name
   * @return Calendar identifier
   */
  private int createCalendar(String name) throws Exception {
    ObjectNode parameters = params()
      .put("Nom", name)
      .put("Des", name + " description")
      .put("Act", 1);
    parameters.set("GrdDatLst-RowTyp", strings(INSERT, INSERT));
    parameters.set("IdeDat", ints(null, null));
    parameters.set("Nam", strings("Third day", "First day"));
    parameters.set("Dat", strings("03/12/2098", "01/12/2098"));
    maintain("NewSchedulerCalendar", parameters);
    return find(rows("SchCalLst", params()), "Nom", name)
      .orElseThrow(() -> new AssertionError("Calendar " + name + " not in the calendar list"))
      .get("Ide").asInt();
  }

  private void deleteCalendar(int calendarId) throws Exception {
    maintain("DeleteSchedulerCalendar", params().set("Ide", ints(calendarId)));
  }

  private JsonNode calendarListRow(int calendarId) throws Exception {
    return find(rows("SchCalLst", params()), "Ide", String.valueOf(calendarId))
      .orElseThrow(() -> new AssertionError("Calendar " + calendarId + " not in the calendar list"));
  }

  /**
   * Calendars that the calendar list shows with the active filter of the screen
   */
  private List<Integer> calendarIds(String active) throws Exception {
    return rows("SchCalLst", params().put("CrtAct", active)).stream().map(row -> row.get("Ide").asInt()).toList();
  }

  private List<JsonNode> calendarDates(int calendarId) throws Exception {
    return rows("SchCalDatLst", params().put("CalIde", calendarId));
  }
}
