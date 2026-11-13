package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maintains of the scheduler task screens (new, update, activate, deactivate and delete a manual, scheduled or file
 * task), checked through the queries that the task screens load and the jobs that the management screen shows. Each test
 * creates its own records and deletes them. The scheduled launches start in {@link #FUTURE_DATE}, so no task runs.
 * <p>
 * Replaces the browser flows {@code SchedulerManualTaskTestsIT} (t001, t021, t901), {@code SchedulerScheduledTaskTestsIT}
 * (t003, t031, t036, t037, t039, t901) and {@code SchedulerFileTaskTestsIT} (t002, t051, t901): the wizard and the task
 * list only send these maintains and show these queries.
 */
@Tag("integration")
class SchedulerTaskMaintainTest extends AbstractSchedulerMaintainTest {

  private static final String NO_NEXT_EXECUTION = "-";

  @Test
  void newManualTaskStoresItsConfigurationParametersAndReport() throws Exception {
    String name = unique("Manual task new");
    int taskId = createTask(manualTask(name));
    try {
      JsonNode listRow = taskListRow(taskId).orElseThrow();
      assertThat(listRow.get("Nam").asString()).isEqualTo(name);
      assertThat(listRow.get("TypLch").asString()).isEqualTo("0");
      assertThat(listRow.get("Act").asString()).isEqualTo("1");
      assertThat(listRow.get("NxtTim").asString()).isEqualTo(NO_NEXT_EXECUTION);

      JsonNode task = taskData(taskId);
      assertThat(task.get("Des").asString()).isEqualTo(name + " description");
      assertThat(task.get("NumStoExe").asString()).isEqualTo("45");
      assertThat(task.get("TimOutExe").asString()).isEqualTo("59");
      assertThat(task.get("TypExe").asString()).isEqualTo("1");
      assertThat(task.get("maintain").asString()).isEqualTo("waitSomeSeconds");
      assertThat(task.get("LchDepWrn").asString()).isEqualTo("1");
      assertThat(task.get("LchSetWrn").asString()).isEqualTo("0");
      assertThat(task.get("RepTyp").asString()).isEqualTo("2");
      assertThat(task.get("RepMsg").asString()).isEqualTo("Broadcast message");
      assertThat(strings(task.get("RepSndSta"))).containsExactly("1", "2");
      assertThat(strings(task.get("RepUsrDst"))).containsExactly("test");

      List<JsonNode> parameters = rows("taskParameters", params().put("IdeTsk.selected", taskId));
      assertThat(column(parameters, "ParNam")).containsExactly("secondsToWait");
      assertThat(column(parameters, "ParVal")).containsExactly("3");

      // A manual task has no trigger in the scheduler
      assertThat(configuredJobs()).doesNotContain(String.valueOf(taskId));
    } finally {
      deleteTasks(taskId);
    }
  }

  @Test
  void updateManualTaskChangesItsConfigurationParametersAndReport() throws Exception {
    String name = unique("Manual task update");
    int taskId = createTask(manualTask(name));
    try {
      ObjectNode parameters = manualTask(name + " updated")
        .put("Ide", taskId)
        .put("Des", "Description updated")
        .put("NumStoExe", 60)
        .put("TimOutExe", 120)
        .put("LchSetWrn", 1)
        .put("RepTyp", 1)
        .put("RepTit", "Email title")
        .put("RepMsg", "Email message");
      parameters.set("RepSndSta", strings("1"));
      parameters.set("RepEmaDst", strings("test"));
      parameters.set("RepUsrDst", strings());
      parameters.set("ParVal", strings("33"));
      maintain("UpdateSchedulerTask", parameters);

      assertThat(taskListRow(taskId).orElseThrow().get("Nam").asString()).isEqualTo(name + " updated");
      JsonNode task = taskData(taskId);
      assertThat(task.get("Des").asString()).isEqualTo("Description updated");
      assertThat(task.get("NumStoExe").asString()).isEqualTo("60");
      assertThat(task.get("TimOutExe").asString()).isEqualTo("120");
      assertThat(task.get("LchSetWrn").asString()).isEqualTo("1");
      assertThat(task.get("RepTyp").asString()).isEqualTo("1");
      assertThat(task.get("RepTit").asString()).isEqualTo("Email title");
      assertThat(strings(task.get("RepSndSta"))).containsExactly("1");
      assertThat(strings(task.get("RepEmaDst"))).containsExactly("test");
      List<JsonNode> taskParameters = rows("taskParameters", params().put("IdeTsk.selected", taskId));
      assertThat(column(taskParameters, "ParNam")).containsExactly("secondsToWait");
      assertThat(column(taskParameters, "ParVal")).containsExactly("33");
    } finally {
      deleteTasks(taskId);
    }
  }

  @Test
  void scheduledTaskStoresEachRepeatTypeAndIsScheduled() throws Exception {
    String calendarName = unique("Launch calendar");
    int calendarId = createLaunchCalendar(calendarName);
    String name = unique("Scheduled task");
    Integer taskId = null;
    try {
      taskId = createTask(scheduledTask(name, 1200).put("RptTyp", 0));
      JsonNode listRow = taskListRow(taskId).orElseThrow();
      assertThat(listRow.get("TypLch").asString()).isEqualTo("1");
      assertThat(listRow.get("NxtTim").asString()).startsWith(FUTURE_DATE);
      assertThat(configuredJobs()).contains(String.valueOf(taskId));
      JsonNode task = taskData(taskId);
      assertThat(task.get("RptTyp").asString()).isEqualTo("0");
      assertThat(task.get("RptNum").asString()).isEqualTo("1200");
      assertThat(task.get("IniDat").asString()).isEqualTo(FUTURE_DATE);

      // Every some hours
      maintain("UpdateSchedulerTask", scheduledTask(name + " hours", 6).put("Ide", taskId));
      task = taskData(taskId);
      assertThat(task.get("Nam").asString()).isEqualTo(name + " hours");
      assertThat(task.get("RptTyp").asString()).isEqualTo("2");
      assertThat(task.get("RptNum").asString()).isEqualTo("6");
      assertThat(taskListRow(taskId).orElseThrow().get("NxtTim").asString()).startsWith(FUTURE_DATE);

      // Once, on a date and time
      maintain("UpdateSchedulerTask", scheduledTask(name + " once", 1).put("Ide", taskId)
        .put("RptTyp", 6).put("schExeDate", "02/01/2099").put("schExeTime", "23:59:59"));
      task = taskData(taskId);
      assertThat(task.get("RptTyp").asString()).isEqualTo("6");
      assertThat(task.get("schExeDate").asString()).isEqualTo("02/01/2099");
      assertThat(task.get("schExeTime").asString()).isEqualTo("23:59:59");
      assertThat(taskListRow(taskId).orElseThrow().get("NxtTim").asString()).isEqualTo("02/01/2099 23:59:59");

      // Custom, on a calendar
      ObjectNode custom = scheduledTask(name + " custom", 1).put("Ide", taskId).put("RptTyp", 7).put("IdeCal", calendarId);
      custom.set("years", strings("2099", "2100"));
      custom.set("months", strings("10", "11"));
      custom.set("days", strings("1", "2"));
      custom.set("hours", strings("0", "1"));
      custom.set("minutes", strings("0"));
      custom.set("seconds", strings("0"));
      maintain("UpdateSchedulerTask", custom);
      task = taskData(taskId);
      assertThat(task.get("RptTyp").asString()).isEqualTo("7");
      assertThat(task.get("IdeCal").asInt()).isEqualTo(calendarId);
      assertThat(strings(task.get("years"))).containsExactly("2099", "2100");
      assertThat(strings(task.get("months"))).containsExactly("10", "11");
      assertThat(strings(task.get("hours"))).containsExactly("0", "1");
      assertThat(taskListRow(taskId).orElseThrow().get("NxtTim").asString()).isEqualTo("01/10/2099 00:00:00");
      assertThat(configuredJobs()).contains(String.valueOf(taskId));
    } finally {
      // The calendar is removed even when the task could not be created or removed
      try {
        if (taskId != null) {
          deleteTasks(taskId);
        }
      } finally {
        maintain("DeleteSchedulerCalendar", params().set("Ide", ints(calendarId)));
      }
    }
  }

  @Test
  void updateTaskStoresItsDependencies() throws Exception {
    String prerequisiteName = unique("Prerequisite task");
    int prerequisiteId = createTask(manualTask(prerequisiteName));
    String name = unique("Dependent task");
    int taskId = createTask(scheduledTask(name, 6));
    try {
      ObjectNode parameters = scheduledTask(name, 6).put("Ide", taskId);
      parameters.set("DepTskIde", ints(prerequisiteId));
      maintain("UpdateSchedulerTask", parameters);

      List<JsonNode> dependencies = rows("taskDependencies", params().put("IdeTsk.selected", taskId));
      assertThat(dependencies).hasSize(1);
      assertThat(dependencies.get(0).get("taskId").asInt()).isEqualTo(prerequisiteId);
      assertThat(dependencies.get(0).get("Nam").asString()).isEqualTo(prerequisiteName);
    } finally {
      deleteTasks(taskId);
      deleteTasks(prerequisiteId);
    }
  }

  @Test
  void deactivateAndActivateTaskPauseAndResumeIt() throws Exception {
    int taskId = createTask(scheduledTask(unique("Scheduled task state"), 6));
    try {
      maintain("DeaSchTsk", params().put("IdeTsk.selected", taskId));
      JsonNode paused = taskListRow(taskId).orElseThrow();
      assertThat(paused.get("Act").asString()).isEqualTo("0");
      assertThat(paused.get("NxtTim").asString()).isEqualTo(NO_NEXT_EXECUTION);

      maintain("ActSchTsk", params().put("IdeTsk.selected", taskId));
      JsonNode resumed = taskListRow(taskId).orElseThrow();
      assertThat(resumed.get("Act").asString()).isEqualTo("1");
      assertThat(resumed.get("NxtTim").asString()).isEqualTo(FUTURE_DATE + " 00:00:00");
    } finally {
      deleteTasks(taskId);
    }
  }

  @Test
  void inactiveFileTaskIsStoredPausedAndActivationSchedulesIt() throws Exception {
    String serverName = unique("Folder server");
    maintain("NewSchedulerServer", params().put("Nom", serverName).put("Pro", "folder").put("Hst", "127.0.0.1")
      .put("Prt", 21212).put("Act", 1));
    int serverId = find(rows("SchSrvLst", params().put("CrtSrv", serverName)), "Nom", serverName).orElseThrow()
      .get("Ide").asInt();
    Integer taskId = null;
    try {
      String name = unique("File task");
      taskId = createTask(scheduledTask(name, 1).put("Act", 0).put("TypLch", 2).put("LchSrv", serverId)
        .put("LchPth", "/test").put("LchPat", ".*\\.txt"));

      JsonNode task = taskData(taskId);
      assertThat(task.get("TypLch").asString()).isEqualTo("2");
      assertThat(task.get("LchSrv").asInt()).isEqualTo(serverId);
      assertThat(task.get("LchPth").asString()).isEqualTo("/test");
      assertThat(task.get("LchPat").asString()).isEqualTo(".*\\.txt");
      JsonNode inactive = taskListRow(taskId).orElseThrow();
      assertThat(inactive.get("Act").asString()).isEqualTo("0");
      assertThat(inactive.get("NxtTim").asString()).isEqualTo(NO_NEXT_EXECUTION);

      maintain("ActSchTsk", params().put("IdeTsk.selected", taskId));
      JsonNode active = taskListRow(taskId).orElseThrow();
      assertThat(active.get("Act").asString()).isEqualTo("1");
      assertThat(active.get("NxtTim").asString()).isEqualTo(FUTURE_DATE + " 00:00:00");
    } finally {
      if (taskId != null) {
        deleteTasks(taskId);
      }
      maintain("DeleteSchedulerServer", params().set("Ide", ints(serverId)));
    }
  }

  @Test
  void deleteTaskRemovesItsRowsAndItsJob() throws Exception {
    int taskId = createTask(scheduledTask(unique("Scheduled task delete"), 6));
    assertThat(configuredJobs()).contains(String.valueOf(taskId));

    deleteTasks(taskId);

    assertThat(taskListRow(taskId)).isEmpty();
    assertThat(rows("taskData", params().put("IdeTsk.selected", taskId))).isEmpty();
    assertThat(rows("taskParameters", params().put("IdeTsk.selected", taskId))).isEmpty();
    assertThat(configuredJobs()).doesNotContain(String.valueOf(taskId));
  }

  /**
   * Create an active calendar without dates for the custom launch
   */
  private int createLaunchCalendar(String name) throws Exception {
    ObjectNode parameters = params().put("Nom", name).put("Des", name).put("Act", 1);
    parameters.set("GrdDatLst-RowTyp", strings());
    maintain("NewSchedulerCalendar", parameters);
    return find(rows("SchCalLst", params()), "Nom", name).orElseThrow().get("Ide").asInt();
  }

  private List<String> strings(JsonNode list) {
    List<String> values = new ArrayList<>();
    list.forEach(value -> values.add(value.asString()));
    return values;
  }
}
