package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SQL and wiring of the servers and calendars of the scheduler configuration screen (#993) on a real database, through
 * the action controller as the screen launches them: who uses each server (the tasks that run on it and the tasks that
 * check their files in it) without returning its secrets, the search by name or host, the dates of each calendar with
 * the next one from today and the tasks that use it, the counts of their active filters, the task catalog filtered by a
 * server or a calendar, and the tasks of a calendar to delete with the days they would run on without it. Servers,
 * calendars and tasks of their own, far from the ids of the sequences and outside Quartz, are stored and removed after
 * each test; the rows of other tests are left out by the search.
 */
@Tag("integration")
class SchedulerConfigurationQueriesTest extends AbstractSchedulerMaintainTest {

  private static final int SFTP = 9401;
  private static final int BATCH = 9402;
  private static final int FOLDER = 9403;
  private static final int SPARE = 9404;
  private static final int[] SERVERS = {SFTP, BATCH, FOLDER, SPARE};
  private static final int HOLIDAYS = 9401;
  private static final int NEXT_YEAR = 9402;
  private static final int[] CALENDARS = {HOLIDAYS, NEXT_YEAR};
  private static final int IMPORT = 9401;
  private static final int REPORT = 9402;
  private static final int CLOSE = 9403;
  private static final int[] TASKS = {IMPORT, REPORT, CLOSE};
  private static final String SEARCH = "config probe";
  private static final String SECRET = "probe-secret";
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
  private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbc;

  @BeforeEach
  void storeConfiguration() {
    jdbc = new JdbcTemplate(dataSource);
    deleteConfiguration();
    server(SFTP, "config probe sftp", "ssh", "sftp.banks.local", 1);
    server(BATCH, "config probe batch", "ssh", "batch.probe.local", 1);
    server(FOLDER, "config probe folder", "folder", "localhost", 1);
    server(SPARE, "config probe spare", "ssh", "spare.probe.local", 0);
    jdbc.update("INSERT INTO AweSchCal (Ide, Des, Act, Nom) VALUES (?, 'Probe holidays', 1, 'config probe holidays')",
      HOLIDAYS);
    jdbc.update("INSERT INTO AweSchCal (Ide, Des, Act, Nom) VALUES (?, 'Ready for next year', 0, "
      + "'config probe next year')", NEXT_YEAR);
    LocalDate today = LocalDate.now();
    calendarDate(HOLIDAYS, 1, today.minusDays(1));
    calendarDate(HOLIDAYS, 2, today);
    calendarDate(HOLIDAYS, 3, today.plusDays(1));
    calendarDate(NEXT_YEAR, 4, LocalDate.of(today.getYear() + 1, 1, 1));
    // The import checks its files in the SFTP server and runs on the batch server; the report runs on the batch server;
    // the closing is scheduled with the holidays
    task(IMPORT, "Config Probe Import", 2, BATCH);
    task(REPORT, "Config Probe Report", 0, BATCH);
    task(CLOSE, "Config Probe Close", 1, null);
    jdbc.update("INSERT INTO AweSchTskLch (Ide, IdeTsk, RptNum, RptTyp, IdSrv, Pth, Pat) VALUES (?, ?, 5, 1, ?, '/in', "
      + "'*.n43')", IMPORT, IMPORT, SFTP);
    jdbc.update("INSERT INTO AweSchTskLch (Ide, IdeTsk, RptTyp, IdeCal, WeekDays, ExeHrs, ExeMin, ExeSec) "
      + "VALUES (?, ?, 7, ?, '1,2,3,4,5', '22', '30', '0')", CLOSE, CLOSE, HOLIDAYS);
  }

  @AfterEach
  void deleteConfiguration() {
    for (int task : TASKS) {
      jdbc.update("DELETE FROM AweSchTskLch WHERE IdeTsk = ?", task);
      jdbc.update("DELETE FROM AweSchTsk WHERE Ide = ?", task);
    }
    for (int calendar : CALENDARS) {
      jdbc.update("DELETE FROM AweSchCalDat WHERE IdeCal = ?", calendar);
      jdbc.update("DELETE FROM AweSchCal WHERE Ide = ?", calendar);
    }
    for (int server : SERVERS) {
      jdbc.update("DELETE FROM AweSchSrv WHERE Ide = ?", server);
    }
  }

  @Test
  void everyServerTellsWhoUsesItAndNeverReturnsItsSecrets() throws Exception {
    List<JsonNode> rows = rows("getConfigurationServers", params().put("CrtSrvSch", SEARCH));

    assertThat(column(rows, "Nom")).containsExactly("config probe batch", "config probe folder", "config probe sftp",
      "config probe spare");
    assertThat(column(rows, "SrvHst")).containsExactly("batch.probe.local", "Application server", "sftp.banks.local",
      "spare.probe.local");
    assertThat(column(rows, "SrvPrt")).containsExactly("22", "—", "22", "22");
    assertThat(column(rows, "SrvTypTxt")).containsExactly("SSH", "Folder", "SSH", "SSH");
    assertThat(column(rows, "SrvTypSub")).containsExactly("Remote commands", "Local folder", "Files over SFTP",
      "Remote commands or files over SFTP");
    assertThat(column(rows, "SrvUseTxt")).containsExactly("2 tasks", "No task", "1 task", "No task");
    assertThat(column(rows, "SrvUseSub")).containsExactly("Config Probe Import", "", "Config Probe Import", "");
    assertThat(rows.toString()).doesNotContain(SECRET);
  }

  @Test
  void everyServerCarriesItsRowIdentifierItsHostStyleAndItsActiveAndTestPills() throws Exception {
    List<JsonNode> rows = rows("getConfigurationServers", params().put("CrtSrvSch", SEARCH));

    assertThat(column(rows, "Ide")).containsExactly(String.valueOf(BATCH), String.valueOf(FOLDER), String.valueOf(SFTP),
      String.valueOf(SPARE));
    assertThat(rows.stream().map(row -> row.get("SrvHstTxt").get("style").asString()).toList())
      .containsExactly("awe-grid-mono", "text-muted", "awe-grid-mono", "awe-grid-mono");
    assertThat(column(rows, "SrvActTxt")).containsExactly("Active", "Active", "Active", "Inactive");
    assertThat(column(rows, "SrvActTon")).containsExactly("success", "success", "success", "neutral");
    assertThat(column(rows, "SrvActIcn")).containsExactly("lucide:power", "lucide:power", "lucide:power",
      "lucide:power-off");
    // Untested in this session
    assertThat(column(rows, "SrvTstTon")).containsOnly("neutral");
    assertThat(column(rows, "SrvTstIcn")).containsOnly("lucide:minus");
  }

  @Test
  void theSidePanelLoadsAServerWithoutItsSecretsAndTellsWhichOnesAreSaved() throws Exception {
    // The batch server keeps its password only: no private key (NULL) and an empty passphrase
    jdbc.update("UPDATE AweSchSrv SET SshKey = NULL, SshKeyPass = '' WHERE Ide = ?", BATCH);

    JsonNode sftpAnswer = perform("maintain", "loadConfigurationServer", params().put("IdeSrv.selected", SFTP));
    Map<String, String> sftp = selected(sftpAnswer);
    Map<String, String> batch = selected(perform("maintain", "loadConfigurationServer",
      params().put("IdeSrv.selected", BATCH)));

    assertThat(sftp).containsEntry("Nom", "config probe sftp").containsEntry("Pro", "ssh")
      .containsEntry("Hst", "sftp.banks.local").containsEntry("Prt", "22").containsEntry("Act", "1")
      .containsEntry("SshUsr", "probe").containsEntry("SrvPwdSav", "1").containsEntry("SrvKeySav", "1")
      .containsEntry("SrvKpwSav", "1").doesNotContainKeys("SrvPwdViw", "SrvKeyViw", "SrvKpwViw")
      .containsEntry("SshPwd", "").containsEntry("SshKey", "").containsEntry("SshKeyPass", "")
      .containsEntry("FlgSshKeyDel", "0");
    assertThat(batch).containsEntry("SrvPwdSav", "1").containsEntry("SrvKeySav", "0").containsEntry("SrvKpwSav", "0");
    assertThat(sftpAnswer.toString()).doesNotContain(SECRET);
  }

  @Test
  void theSidePanelOfANewServerStartsAsAnActiveSshServerOnPort22() throws Exception {
    Map<String, String> created = selected(perform("maintain", "loadConfigurationServer", params()));

    assertThat(created).containsEntry("Nom", "").containsEntry("Pro", "ssh").containsEntry("Prt", "22")
      .containsEntry("Act", "1").containsEntry("SrvPwdSav", "0").containsEntry("SrvKeySav", "0")
      .containsEntry("SrvKpwSav", "0");
  }

  @Test
  void theTasksOfAServerAreTheOnesThatRunOnItOrCheckTheirFilesInItSortedByName() throws Exception {
    List<JsonNode> batch = rows("getConfigurationServerTasks", params().put("IdeSrv.selected", BATCH));
    List<JsonNode> sftp = rows("getConfigurationServerTasks", params().put("IdeSrv.selected", SFTP));
    List<JsonNode> folder = rows("getConfigurationServerTasks", params().put("IdeSrv.selected", FOLDER));

    assertThat(column(batch, "IdeTsk")).containsExactly(String.valueOf(IMPORT), String.valueOf(REPORT));
    assertThat(batch.stream().map(row -> row.get("TskNam").get("label").asString()).toList())
      .containsExactly("Config Probe Import", "Config Probe Report");
    assertThat(column(batch, "TskStaTxt")).containsOnly("No executions");
    assertThat(column(sftp, "IdeTsk")).containsExactly(String.valueOf(IMPORT));
    // When it is launched; outside Quartz it has no next check
    assertThat(column(sftp, "TskUseSub")).containsExactly(
      "When a file arrives in config probe sftp:/in · *.n43 · checks every 5 min");
    assertThat(folder).isEmpty();
  }

  @Test
  void theSidePanelSubtitleAndTheConnectionTypeCardsOfAServer() throws Exception {
    List<JsonNode> rows = rows("getConfigurationServers", params().put("CrtSrvSch", SEARCH));
    List<JsonNode> types = rows("getConfigurationServerTypes", params());

    assertThat(column(rows, "SrvPnlSub")).containsExactly("External server · SSH · used by 2 tasks",
      "Application server · Folder · no task uses it", "External server · SSH · used by 1 task",
      "External server · SSH · no task uses it");
    assertThat(column(types, "value")).containsExactly("ssh", "ftp", "folder");
    assertThat(column(types, "icon")).containsExactly("lucide:terminal", "lucide:upload", "lucide:folder");
  }

  @Test
  void theTabsCountEveryServerAndEveryCalendarAndNameTheEngine() throws Exception {
    int servers = jdbc.queryForObject("SELECT COUNT(*) FROM AweSchSrv", Integer.class);
    int calendars = jdbc.queryForObject("SELECT COUNT(*) FROM AweSchCal", Integer.class);

    List<JsonNode> tabs = rows("getConfigurationTabs", params());

    assertThat(column(tabs, "value")).containsExactly("CfgSrv", "CfgCal", "CfgEng");
    assertThat(column(tabs, "label").subList(0, 2)).containsExactly("Servers " + servers, "Calendars " + calendars);
    assertThat(column(tabs, "label").get(2)).startsWith("Engine");
    assertThat(column(tabs, "icon")).containsExactly("lucide:server", "lucide:calendar", "lucide:cpu");
  }

  @Test
  void theServerSearchFindsANameOrAHostAndTheActiveFilterCountsWithIt() throws Exception {
    List<JsonNode> byHost = rows("getConfigurationServers", params().put("CrtSrvSch", "PROBE.local"));
    List<JsonNode> inactive = rows("getConfigurationServers", params().put("CrtSrvSch", SEARCH).put("CrtSrvAct", "0"));
    List<JsonNode> counts = rows("getConfigurationServerActiveCounts", params().put("CrtSrvSch", SEARCH)
      .put("CrtSrvAct", "0"));

    assertThat(column(byHost, "Nom")).containsExactly("config probe batch", "config probe spare");
    assertThat(column(inactive, "Nom")).containsExactly("config probe spare");
    assertThat(column(counts, "value")).containsExactly("", "1", "0");
    assertThat(counts.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(4, 3, 1);
  }

  @Test
  void everyCalendarTellsItsDatesItsNextDateFromTodayAndWhoUsesIt() throws Exception {
    List<JsonNode> rows = rows("getConfigurationCalendars", params().put("CrtCalSch", SEARCH));
    int nextYear = LocalDate.now().getYear() + 1;

    assertThat(column(rows, "Nom")).containsExactly("config probe holidays", "config probe next year");
    assertThat(column(rows, "Des")).containsExactly("Probe holidays", "Ready for next year");
    assertThat(column(rows, "CalDatCnt")).containsExactly("3", "1");
    assertThat(column(rows, "CalNxtDat")).containsExactly(LocalDate.now().format(DAY), "01/01");
    assertThat(column(rows, "CalDatSub")).containsExactly("next " + LocalDate.now().format(DAY),
      "from 01/01/" + nextYear);
    assertThat(column(rows, "CalUseTxt")).containsExactly("1 task", "No task");
    assertThat(column(rows, "CalUseSub")).containsExactly("Config Probe Close", "");
  }

  @Test
  void theCalendarSearchFindsANameAndTheActiveFilterCountsWithIt() throws Exception {
    List<JsonNode> byName = rows("getConfigurationCalendars", params().put("CrtCalSch", "PROBE NEXT"));
    List<JsonNode> active = rows("getConfigurationCalendars", params().put("CrtCalSch", SEARCH).put("CrtCalAct", "1"));
    List<JsonNode> counts = rows("getConfigurationCalendarActiveCounts", params().put("CrtCalSch", SEARCH));

    assertThat(column(byName, "Nom")).containsExactly("config probe next year");
    assertThat(column(active, "Nom")).containsExactly("config probe holidays");
    assertThat(counts.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(2, 1, 1);
  }

  @Test
  void theTaskCatalogKeepsTheTasksOfAServerOrOfACalendar() throws Exception {
    List<JsonNode> batch = rows("getTaskCatalog", params().put("CrtTskSch", SEARCH).put("CrtTskSrv", BATCH));
    List<JsonNode> sftp = rows("getTaskCatalog", params().put("CrtTskSch", SEARCH).put("CrtTskSrv", SFTP));
    List<JsonNode> holidays = rows("getTaskCatalog", params().put("CrtTskSch", SEARCH).put("CrtTskCal", HOLIDAYS));
    List<JsonNode> counts = rows("getTaskCatalogActiveCounts", params().put("CrtTskSch", SEARCH)
      .put("CrtTskSrv", BATCH));

    assertThat(column(batch, "Nam")).containsExactly("Config Probe Import", "Config Probe Report");
    assertThat(column(sftp, "Nam")).containsExactly("Config Probe Import");
    assertThat(column(holidays, "Nam")).containsExactly("Config Probe Close");
    assertThat(counts.stream().map(row -> row.get("count").asInt()).toList()).containsExactly(2, 2, 0);
  }

  @Test
  void deletingACalendarTellsItsTasksAndTheWeekdaysTheyWouldRunOnWithoutIt() throws Exception {
    // Only a Sunday and the Monday after it, from next week: the closing runs on weekdays at 22:30
    jdbc.update("DELETE FROM AweSchCalDat WHERE IdeCal = ?", HOLIDAYS);
    LocalDate sunday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    calendarDate(HOLIDAYS, 5, sunday);
    calendarDate(HOLIDAYS, 6, sunday.plusDays(1));
    DateTimeFormatter day = sunday.plusDays(1).getYear() == LocalDate.now().getYear() ? DAY : FULL_DAY;

    List<JsonNode> rows = rows("getConfigurationCalendarDeleteCheck", params().put("IdeCal", HOLIDAYS));

    assertThat(column(rows, "Nam")).containsExactly("Config Probe Close");
    assertThat(column(rows, "CalDelWhn")).containsExactly("Weekdays at 22:30");
    assertThat(column(rows, "CalDelDat")).containsExactly("would run on " + sunday.plusDays(1).format(day));
  }

  /**
   * Values the select actions of an answer give to each criterion of the screen (the first value of each)
   */
  private static Map<String, String> selected(JsonNode actions) {
    Map<String, String> values = new LinkedHashMap<>();
    for (JsonNode action : actions) {
      if ("select".equals(action.get("type").asString())) {
        JsonNode value = action.get("parameters").get("values");
        values.put(action.get("target").asString(), value.isArray() ? value.get(0).asString() : value.asString());
      }
    }
    return values;
  }

  private void server(int id, String name, String type, String host, int active) {
    jdbc.update("INSERT INTO AweSchSrv (Ide, Nom, Pro, Hst, Prt, Act, SshUsr, SshPwd, SshKey, SshKeyPass) "
      + "VALUES (?, ?, ?, ?, '22', ?, 'probe', ?, ?, ?)", id, name, type, host, active, SECRET, SECRET, SECRET);
  }

  private void calendarDate(int calendarId, int id, LocalDate date) {
    jdbc.update("INSERT INTO AweSchCalDat (Ide, IdeCal, Nom, Dat) VALUES (?, ?, 'probe', ?)", 9400 + id, calendarId,
      Date.valueOf(date));
  }

  private void task(int id, String name, int launchType, Integer executionServer) {
    jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, Des, TypExe, IdeSrvExe, CmdExe, TypLch, Act) "
      + "VALUES (?, ?, 'probe', 0, ?, 'probe', ?, 1)", id, name, executionServer, launchType);
  }
}
