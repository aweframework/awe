package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.file.ServerTestResult;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.enums.TaskLaunchKind;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Servers and calendars of the scheduler configuration screen (#993): how a server tells its connection (type, the use
 * of an SSH server, the folder of the application server), who uses it (every task once, the first by name), when the
 * next date of a calendar is (from today, with its year when it is not this one), and the refusal of an active filter
 * that is not valid before anything is read
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchedulerConfigurationPresenterTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private ServiceConfig serviceConfig;

  private SchedulerConfigurationPresenter presenter;

  @BeforeEach
  void setUp() throws Exception {
    presenter = new SchedulerConfigurationPresenter(queryService, queryUtil, serviceConfig,
      Clock.fixed(TODAY.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    given(queryUtil.getParameters(any(), anyString(), anyString()))
      .willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(serviceConfig.getLocale(anyString())).willAnswer(invocation -> invocation.getArgument(0) + "[{0}]");
    given(queryService.launchPrivateQuery(eq("getConfigurationServerRows"), any(ObjectNode.class)))
      .willReturn(data(List.of(
        server(1, "sftp-banks", "ssh", "sftp.banks.local", "22"),
        server(2, "batch-01", "ssh", "batch-01.local", "22"),
        server(3, "both", "ssh", "both.local", "2222"),
        server(4, "spare", "ssh", "spare.local", "22"),
        server(5, "ftp-remit", "ftp", "ftp.remit.local", "21"),
        server(6, "input-folder", "folder", "localhost", "0"))));
    // Zeta runs on batch-01 and checks its files in sftp-banks; Alpha and Beta use «both» in both ways
    given(queryService.launchPrivateQuery(eq("getConfigurationServerUsage"), any(ObjectNode.class)))
      .willReturn(data(List.of(
        usage(30, "Zeta", 2, 1),
        usage(20, "Beta", 3, 3),
        usage(10, "Alpha", 3, 3))));
  }

  @Test
  void aServerTellsItsConnectionByTypeAndTheUseOfItsTasks() throws Exception {
    List<Map<String, CellData>> rows = presenter.getServers(null, null).getDataList().getRows();

    assertThat(column(rows, "Nom")).containsExactly("batch-01", "both", "ftp-remit", "input-folder", "sftp-banks",
      "spare");
    assertThat(column(rows, "SrvTypTxt")).containsExactly("SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]", "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FTP[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FOLDER[{0}]", "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]");
    assertThat(column(rows, "SrvTypSub")).containsExactly("SCREEN_TEXT_SCHEDULER_SERVER_USE_COMMANDS[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_USE_COMMANDS_AND_FILES[{0}]", "SCREEN_TEXT_SCHEDULER_SERVER_USE_FTP[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_USE_FOLDER[{0}]", "SCREEN_TEXT_SCHEDULER_SERVER_USE_FILES[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_USE_SSH[{0}]");
    assertThat(column(rows, "SrvTypIcn")).containsExactly("lucide:terminal", "lucide:terminal", "lucide:upload",
      "lucide:folder", "lucide:terminal", "lucide:terminal");
    // The folder is in the application server: no host nor port
    assertThat(column(rows, "SrvHst")).containsExactly("batch-01.local", "both.local", "ftp.remit.local",
      "SCREEN_TEXT_SCHEDULER_SERVER_APPLICATION[{0}]", "sftp.banks.local", "spare.local");
    assertThat(column(rows, "SrvHstApp")).containsExactly("0", "0", "0", "1", "0", "0");
    assertThat(column(rows, "SrvPrt")).containsExactly("22", "2222", "21", "—", "22", "22");
  }

  @Test
  void aServerIsUsedOnceByEachTaskThatReferencesItAndShowsTheFirstOneByName() throws Exception {
    List<Map<String, CellData>> rows = presenter.getServers(null, null).getDataList().getRows();

    assertThat(column(rows, "SrvUseCnt")).containsExactly("1", "2", "0", "0", "1", "0");
    assertThat(column(rows, "SrvUseTxt")).containsExactly("SCREEN_TEXT_SCHEDULER_USED_BY_ONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_USED_BY_MANY[2]", "SCREEN_TEXT_SCHEDULER_USED_BY_NONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_USED_BY_NONE[{0}]", "SCREEN_TEXT_SCHEDULER_USED_BY_ONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_USED_BY_NONE[{0}]");
    assertThat(column(rows, "SrvUseSub")).containsExactly("Zeta", "Alpha", "", "", "Zeta", "");
  }

  @Test
  void aServerShowsTheLastConnectionTestOfThisSessionOrThatItIsUntested() throws Exception {
    given(serviceConfig.getLocale("SCREEN_TEXT_SCHEDULER_SERVER_TEST_SECONDS")).willReturn("{0},{1} s");
    given(serviceConfig.getLocale("SCREEN_TEXT_SCHEDULER_SERVER_TEST_OK")).willReturn("OK · {0}");
    given(serviceConfig.getLocale("SCREEN_TEXT_SCHEDULER_SERVER_TEST_ERROR_SUB")).willReturn("{0} · {1}");
    given(serviceConfig.getLocale("SCREEN_TEXT_SCHEDULER_SERVER_TEST_REASON_CONNECTION_ERROR")).willReturn("timed out");
    given(serviceConfig.getLocale("SCREEN_TEXT_TASK_CATALOG_TODAY")).willReturn("Today {0}");
    // batch-01 passed today; ftp-remit failed yesterday; the others were not tested in this session
    Map<Integer, ServerTestResult> results = Map.of(
      2, ServerTestResult.success(420, TODAY.atTime(10, 44)),
      5, ServerTestResult.failure("CONNECTION_ERROR", 3000, TODAY.minusDays(1).atTime(18, 5)));

    List<Map<String, CellData>> rows = presenter.getServers(null, null, results).getDataList().getRows();

    assertThat(column(rows, "Nom")).containsExactly("batch-01", "both", "ftp-remit", "input-folder", "sftp-banks",
      "spare");
    assertThat(column(rows, "SrvTstSta")).containsExactly("ok", "none", "error", "none", "none", "none");
    assertThat(column(rows, "SrvTstTxt")).containsExactly("OK · 0,42 s",
      "SCREEN_TEXT_SCHEDULER_SERVER_TEST_NONE[{0}]", "SCREEN_TEXT_SCHEDULER_SERVER_TEST_ERROR[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_TEST_NONE[{0}]", "SCREEN_TEXT_SCHEDULER_SERVER_TEST_NONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_TEST_NONE[{0}]");
    assertThat(column(rows, "SrvTstSub")).containsExactly("Today 10:44", "", "23/11 18:05 · timed out", "", "", "");
  }

  @Test
  void aServerUsedByATaskCannotBeDeletedAndTheRefusalNamesItsTasks() throws Exception {
    given(serviceConfig.getLocale("SCHEDULER_SERVER_IN_USE_ONE")).willReturn("used by 1 task ({0})");
    given(serviceConfig.getLocale("SCHEDULER_SERVER_IN_USE_MANY")).willReturn("used by {0} tasks ({1})");

    // spare (4) and ftp-remit (5) are free; batch-01 (2) is used by Zeta; «both» (3) by Alpha and Beta
    presenter.validateServersNotInUse(List.of(4, "5"));
    AWException one = assertThrows(AWException.class, () -> presenter.validateServersNotInUse(List.of(4, 2)));
    AWException many = assertThrows(AWException.class, () -> presenter.validateServersNotInUse(List.of("3")));

    assertThat(one.getTitle()).isEqualTo("SCHEDULER_SERVER_IN_USE_TITLE[{0}]");
    assertThat(one.getMessage()).isEqualTo("used by 1 task (Zeta)");
    assertThat(many.getMessage()).isEqualTo("used by 2 tasks (Alpha, Beta)");
  }

  @Test
  void aServerToDeleteThatIsNotANumberIsRefusedAsNotValidBeforeAnythingIsRead() throws Exception {
    AWException refusal = assertThrows(AWException.class,
      () -> presenter.validateServersNotInUse(List.of(4, "4; DROP")));

    assertThat(refusal.getTitle()).isEqualTo("SCHEDULER_INVALID_IDENTIFIER_TITLE[{0}]");
    assertThat(refusal.getMessage()).isEqualTo("SCHEDULER_INVALID_IDENTIFIER_MESSAGE[{0}]");
    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void theNextDateOfACalendarIsFromTodayAndCarriesItsYearWhenItIsNotThisOne() throws Exception {
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarRows"), any(ObjectNode.class)))
      .willReturn(data(List.of(calendar(1, "Holidays 2026"), calendar(2, "Holidays 2027"), calendar(3, "Past"),
        calendar(4, "Empty"))));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarDates"), any(ObjectNode.class)))
      .willReturn(data(List.of(date(1, TODAY.minusDays(1)), date(1, TODAY.plusDays(12)), date(1, TODAY),
        date(2, LocalDate.of(2027, 1, 6)), date(2, LocalDate.of(2027, 1, 1)), date(3, LocalDate.of(2025, 12, 25)))));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarUsage"), any(ObjectNode.class)))
      .willReturn(data(List.of()));

    List<Map<String, CellData>> rows = presenter.getCalendars(null, null).getDataList().getRows();

    assertThat(column(rows, "Nom")).containsExactly("Empty", "Holidays 2026", "Holidays 2027", "Past");
    assertThat(column(rows, "CalDatCnt")).containsExactly("0", "3", "2", "1");
    assertThat(column(rows, "CalNxtDat")).containsExactly("", "24/11", "01/01", "");
    assertThat(column(rows, "CalDatSub")).containsExactly("SCREEN_TEXT_SCHEDULER_CALENDAR_NO_NEXT[{0}]",
      "SCREEN_TEXT_SCHEDULER_CALENDAR_NEXT[24/11]", "SCREEN_TEXT_SCHEDULER_CALENDAR_FROM[01/01/2027]",
      "SCREEN_TEXT_SCHEDULER_CALENDAR_NO_NEXT[{0}]");
    assertThat(column(rows, "CalUseTxt")).containsOnly("SCREEN_TEXT_SCHEDULER_USED_BY_NONE[{0}]");
    // The subtitle of the side panel: «Calendario · no lo usa ninguna tarea»
    assertThat(column(rows, "CalPnlSub")).containsOnly("SCREEN_TEXT_SCHEDULER_CALENDAR_PANEL_KIND[{0}] · "
      + "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_NONE[{0}]");
  }

  @Test
  void aCalendarLoadedForTheSidePanelFillsItsFieldsAndTellsHowManyDatesItHas() throws Exception {
    givenCalendarTasks(1, List.of());

    ServiceData stored = presenter.loadCalendar(1);
    ServiceData created = presenter.loadCalendar(null);

    assertThat(selected(stored)).containsExactly("CalPnlNom=Holidays", "CalPnlDes=Holidays description",
      "CalPnlAct=1", "CalPnlCnt=SCREEN_TEXT_SCHEDULER_CALENDAR_PANEL_DATES_MANY[6]");
    // A new calendar is active and has no dates
    assertThat(selected(created)).containsExactly("CalPnlNom=", "CalPnlDes=", "CalPnlAct=1",
      "CalPnlCnt=SCREEN_TEXT_SCHEDULER_CALENDAR_PANEL_DATES_NONE[{0}]");
    assertThrows(AWException.class, () -> presenter.loadCalendar(9));
  }

  @Test
  void theDatesOfTheSidePanelAreTheOnesOfTheCalendarInOrderWithTheirWeekday() throws Exception {
    Map<String, CellData> christmas = date(1, LocalDate.of(2026, 12, 25));
    christmas.put("IdeDat", new CellData(7));
    christmas.put("Nam", new CellData("Navidad"));
    Map<String, CellData> constitution = date(1, LocalDate.of(2026, 12, 6));
    constitution.put("IdeDat", new CellData(8));
    constitution.put("Nam", new CellData("Constitución"));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarDates"), any(ObjectNode.class)))
      .willReturn(data(List.of(christmas, date(2, LocalDate.of(2026, 12, 1)), constitution)));

    List<Map<String, CellData>> rows = presenter.getCalendarDates(1).getDataList().getRows();

    assertThat(column(rows, "IdeDat")).containsExactly("8", "7");
    assertThat(column(rows, "Dat")).containsExactly("06/12/2026", "25/12/2026");
    assertThat(column(rows, "Nam")).containsExactly("Constitución", "Navidad");
    // Sunday and Friday, as the catalog numbers the weekdays (Sunday 1)
    assertThat(column(rows, "DatWek")).containsExactly("SCREEN_TEXT_TASK_CATALOG_WEEKDAY_1[{0}]",
      "SCREEN_TEXT_TASK_CATALOG_WEEKDAY_6[{0}]");
    assertThat(presenter.getCalendarDates(null).getDataList().getRows()).isEmpty();
  }

  @Test
  void theTasksOfTheSidePanelTellTheDaysOfTheCalendarTheyWillNotRunOn() throws Exception {
    givenCalendarTasks(1, List.of(launch(10, "Close", 1, 7, "1,2,3,4,5", "22", "30"),
      launch(30, "Broken", 1, 99, null, null, null)));
    given(serviceConfig.getLocale("SCREEN_TEXT_TASK_CATALOG_AND")).willReturn("{0} y {1}");

    List<Map<String, CellData>> rows = presenter.getCalendarDeleteCheck(1).getDataList().getRows();

    // Only the days its repetition fires on: a repetition that cannot tell them says nothing
    assertThat(column(rows, "Nam")).containsExactly("Broken", "Close");
    assertThat(column(rows, "CalSkpTxt")).containsExactly("",
      "SCREEN_TEXT_SCHEDULER_CALENDAR_SKIPS[08/12, 25/12 y 28/12]");
    assertThat(column(rows, "CalSkpTon")).containsExactly("", "info");
    assertThat(column(rows, "CalSkpIcn")).containsExactly("", "lucide:calendar-x");
  }

  @Test
  void deletingACalendarTellsItsTasksAndTheDaysTheyWouldNowRunOn() throws Exception {
    givenCalendarTasks(1, List.of(
      // Weekdays at 22:30 (Monday 1 to Friday 5)
      launch(10, "Close", 1, 7, "1,2,3,4,5", "22", "30"),
      // A manual task keeps the calendar of a launch it no longer has: it does not run by itself
      launch(20, "Archive", 0, null, null, null, null),
      // A repetition that cannot be built: the days of the calendar
      launch(30, "Broken", 1, 99, null, null, null)));
    given(serviceConfig.getLocale("SCREEN_TEXT_TASK_CATALOG_AND")).willReturn("{0} y {1}");

    List<Map<String, CellData>> rows = presenter.getCalendarDeleteCheck(1).getDataList().getRows();

    assertThat(column(rows, "Nam")).containsExactly("Archive", "Broken", "Close");
    assertThat(column(rows, "IdeTsk")).containsExactly("20", "30", "10");
    // Sunday 06/12 is not a weekday; the most are three days
    assertThat(column(rows, "CalDelDat")).containsExactly("",
      "SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DATES[SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DAY[06/12], "
        + "SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DAY[08/12] y SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DAY[25/12]]",
      "SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_RUNS[SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DAY[08/12], "
        + "SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DAY[25/12] y SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_DAY[28/12]]");
    assertThat(column(rows, "CalDelDatCnt")).containsExactly("0", "3", "3");
    assertThat(column(rows, "CalDelWhn").get(2)).isEqualTo("SCREEN_TEXT_TASK_WHEN_WORKDAYS[22:30]");
    // The line under each task of the dialog: when it runs and, when some day changes, the days
    assertThat(column(rows, "CalDelSub").get(0)).isEqualTo(column(rows, "CalDelWhn").get(0));
    assertThat(column(rows, "CalDelSub").get(2)).isEqualTo("SCREEN_TEXT_TASK_WHEN_WORKDAYS[22:30] · "
      + column(rows, "CalDelDat").get(2));
    // The heading of the dialog names the calendar and how many tasks use it
    assertThat(column(rows, "title")).containsOnly("SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_TITLE[Holidays]");
    assertThat(column(rows, "description")).containsOnly("SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_TEXT_MANY[3]");
  }

  @Test
  void deletingAnInactiveCalendarChangesNoDay() throws Exception {
    givenCalendarTasks(0, List.of(launch(10, "Close", 1, 7, "1,2,3,4,5", "22", "30")));

    List<Map<String, CellData>> rows = presenter.getCalendarDeleteCheck(1).getDataList().getRows();

    assertThat(column(rows, "Nam")).containsExactly("Close");
    assertThat(column(rows, "CalDelDat")).containsExactly("");
    assertThat(column(rows, "CalDelDatCnt")).containsExactly("0");
    assertThat(column(rows, "description")).containsExactly("SCREEN_TEXT_SCHEDULER_CALENDAR_DELETE_TEXT_ONE[{0}]");
  }

  @Test
  void anInactiveTaskThatUsesTheCalendarWouldRunOnNoDay() throws Exception {
    Map<String, CellData> inactive = launch(10, "Close", 1, 7, "1,2,3,4,5", "22", "30");
    inactive.put("Act", new CellData(0));
    givenCalendarTasks(1, List.of(inactive));

    List<Map<String, CellData>> rows = presenter.getCalendarDeleteCheck(1).getDataList().getRows();

    assertThat(column(rows, "Nam")).containsExactly("Close");
    assertThat(column(rows, "CalDelDat")).containsExactly("");
    assertThat(column(rows, "CalDelDatCnt")).containsExactly("0");
  }

  @Test
  void theTabsTellHowManyServersAndCalendarsThereAreAndTheStateOfTheEngine() throws Exception {
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarRows"), any(ObjectNode.class)))
      .willReturn(data(List.of(calendar(1, "Holidays"), calendar(2, "Other"))));
    given(serviceConfig.getLocale("SCREEN_TEXT_SCHEDULER_ENGINE_STATUS_RUNNING")).willReturn("Running");

    List<Map<String, CellData>> rows = presenter.getTabs("SCREEN_TEXT_SCHEDULER_ENGINE_STATUS_RUNNING")
      .getDataList().getRows();

    assertThat(column(rows, "value")).containsExactly("CfgSrv", "CfgCal", "CfgEng");
    assertThat(column(rows, "label")).containsExactly("SCREEN_TEXT_SCHEDULER_CONFIGURATION_TAB_SERVERS[6]",
      "SCREEN_TEXT_SCHEDULER_CONFIGURATION_TAB_CALENDARS[2]", "SCREEN_TEXT_SCHEDULER_CONFIGURATION_TAB_ENGINE_STATUS[Running]");
    assertThat(column(rows, "icon")).containsExactly("lucide:server", "lucide:calendar", "lucide:cpu");
  }

  @Test
  void theEngineTabWithoutTheStateOfTheEngineIsOnlyItsName() throws Exception {
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarRows"), any(ObjectNode.class)))
      .willReturn(data(List.of()));

    List<Map<String, CellData>> rows = presenter.getTabs(null).getDataList().getRows();

    assertThat(column(rows, "label")).containsExactly("SCREEN_TEXT_SCHEDULER_CONFIGURATION_TAB_SERVERS[6]",
      "SCREEN_TEXT_SCHEDULER_CONFIGURATION_TAB_CALENDARS[0]", "SCREEN_TEXT_SCHEDULER_CONFIGURATION_TAB_ENGINE[{0}]");
  }

  @Test
  void theSidePanelSubtitleOfAServerTellsWhereItIsItsTypeAndHowManyTasksUseIt() throws Exception {
    List<Map<String, CellData>> rows = presenter.getServers(null, null).getDataList().getRows();

    assertThat(column(rows, "Nom")).containsExactly("batch-01", "both", "ftp-remit", "input-folder", "sftp-banks",
      "spare");
    assertThat(column(rows, "SrvPnlSub")).containsExactly(
      "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_EXTERNAL[SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]]"
        + " · SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_ONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_EXTERNAL[SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]]"
        + " · SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_MANY[2]",
      "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_EXTERNAL[SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FTP[{0}]]"
        + " · SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_NONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_LOCAL[SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FOLDER[{0}]]"
        + " · SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_NONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_EXTERNAL[SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]]"
        + " · SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_ONE[{0}]",
      "SCREEN_TEXT_SCHEDULER_SERVER_PANEL_EXTERNAL[SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH[{0}]]"
        + " · SCREEN_TEXT_SCHEDULER_SERVER_PANEL_USED_NONE[{0}]");
  }

  @Test
  void aServerLoadedForTheSidePanelNeverCarriesItsSecretsOnlyWhetherEachOneIsSaved() throws Exception {
    Map<String, CellData> stored = server(1, "sftp-banks", "ssh", "sftp.banks.local", "22");
    stored.put("SshUsr", new CellData("treasury"));
    stored.put("SrvPwdSav", new CellData(1));
    stored.put("SrvKeySav", new CellData(0));
    stored.put("SrvKpwSav", new CellData(1));
    // Whatever the query may carry, the secrets are never copied
    stored.put("SshPwd", new CellData("stored-password"));
    stored.put("SshKey", new CellData("stored-key"));
    stored.put("SshKeyPass", new CellData("stored-passphrase"));
    given(queryService.launchPrivateQuery(eq("getConfigurationServer"), any(ObjectNode.class)))
      .willReturn(data(List.of(stored)));

    ServiceData loaded = presenter.loadServer(1);

    Map<String, CellData> row = loaded.getDataList().getRows().get(0);
    assertThat(row.get("Nom").getStringValue()).isEqualTo("sftp-banks");
    assertThat(row.get("Pro").getStringValue()).isEqualTo("ssh");
    assertThat(row.get("Hst").getStringValue()).isEqualTo("sftp.banks.local");
    assertThat(row.get("Prt").getStringValue()).isEqualTo("22");
    assertThat(row.get("Act").getStringValue()).isEqualTo("1");
    assertThat(row.get("SshUsr").getStringValue()).isEqualTo("treasury");
    assertThat(row.get("SrvPwdSav").getStringValue()).isEqualTo("1");
    assertThat(row.get("SrvKeySav").getStringValue()).isEqualTo("0");
    assertThat(row.get("SrvKpwSav").getStringValue()).isEqualTo("1");
    // The read-only field of a saved secret tells it is saved (its placeholder): no text is sent for it
    assertThat(row).doesNotContainKeys("SrvPwdViw", "SrvKeyViw", "SrvKpwViw");
    assertThat(row.get("FlgSshKeyDel").getStringValue()).isEqualTo("0");
    // The secret fields of the form are emptied, never filled
    assertThat(row.get("SshPwd").getStringValue()).isEmpty();
    assertThat(row.get("SshKey").getStringValue()).isEmpty();
    assertThat(row.get("SshKeyPass").getStringValue()).isEmpty();
    assertThat(loaded.toString()).doesNotContain("stored-password", "stored-key", "stored-passphrase");
    // One select action per field of the panel, the connection type first
    assertThat(loaded.getClientActionList()).hasSize(row.size());
    assertThat(loaded.getClientActionList().get(0).getTarget()).isEqualTo("Pro");
    assertThat(loaded.getClientActionList()).allMatch(action -> "select".equals(action.getType()));
  }

  @Test
  void aNewServerStartsAsAnActiveSshServerOnPort22WithNothingSaved() throws Exception {
    ServiceData loaded = presenter.loadServer(null);

    Map<String, CellData> row = loaded.getDataList().getRows().get(0);
    assertThat(row.get("Nom").getStringValue()).isEmpty();
    assertThat(row.get("Pro").getStringValue()).isEqualTo("ssh");
    assertThat(row.get("Prt").getStringValue()).isEqualTo("22");
    assertThat(row.get("Act").getStringValue()).isEqualTo("1");
    assertThat(row.get("SrvPwdSav").getStringValue()).isEqualTo("0");
    assertThat(row.get("SrvKeySav").getStringValue()).isEqualTo("0");
    assertThat(row.get("SrvKpwSav").getStringValue()).isEqualTo("0");
    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void aServerThatNoLongerExistsIsNotLoaded() throws Exception {
    given(queryService.launchPrivateQuery(eq("getConfigurationServer"), any(ObjectNode.class)))
      .willReturn(data(List.of()));

    AWException refusal = assertThrows(AWException.class, () -> presenter.loadServer(99));

    assertThat(refusal.getMessage()).isEqualTo("SCHEDULER_SERVER_TEST_SERVER_NOT_FOUND[{0}]");
  }

  @Test
  void theTasksOfAServerAreTheOnesOfItsUseSortedByNameWithTheirLaunchAndTheirLastStatus() throws Exception {
    given(serviceConfig.getLocale("SCREEN_TEXT_TASK_CATALOG_TODAY")).willReturn("Today {0}");
    given(serviceConfig.getLocale("SCREEN_TEXT_SCHEDULER_SERVER_TASK_STATUS")).willReturn("{0} {1}");
    given(serviceConfig.getLocale("ENUM_STATUS_OK")).willReturn("OK");
    given(queryService.findLabel("ActStaTyp", "3")).willReturn("ENUM_STATUS_OK");
    given(queryService.findLabel("ActStaIco", "3")).willReturn("lucide:circle-check");
    given(queryService.findLabel("ActStaTon", "3")).willReturn("success");
    TaskWhenSummary.When file = new TaskWhenSummary.When(TaskLaunchKind.FILE, "lucide:file-down", "On a file in /in",
      "checks every 5 min");
    TaskWhenSummary.When manual = new TaskWhenSummary.When(TaskLaunchKind.MANUAL, "lucide:hand", "Manual", "");
    Map<Integer, TaskCatalogPresenter.TaskSummary> summaries = Map.of(
      10, summary(10, "Alpha", manual, null, null, null),
      20, summary(20, "Beta", file, TODAY.atTime(10, 50), 3, Timestamp.valueOf(TODAY.atTime(8, 5))));
    List<List<Integer>> asked = new ArrayList<>();

    // «both» (3) is used by Alpha and Beta
    List<Map<String, CellData>> rows = presenter.getServerTasks(3, ids -> {
      asked.add(List.copyOf(ids));
      return summaries;
    }).getDataList().getRows();

    assertThat(asked).containsExactly(List.of(10, 20));
    assertThat(column(rows, "IdeTsk")).containsExactly("10", "20");
    assertThat(rows.stream().map(row -> row.get("TskNam").getObjectValue().toString()).toList())
      .allMatch(link -> link.contains("\"value\""));
    assertThat(column(rows, "TskUseSub")).containsExactly("Manual",
      "On a file in /in · checks every 5 min · SCREEN_TEXT_SCHEDULER_SERVER_TASK_NEXT[today 10:50]");
    assertThat(column(rows, "TskStaTxt")).containsExactly("SCREEN_TEXT_TASK_CATALOG_NO_EXECUTIONS[{0}]",
      "OK today 08:05");
    assertThat(column(rows, "TskStaTon")).containsExactly("neutral", "success");
    assertThat(column(rows, "TskStaIcn")).containsExactly("lucide:circle-dashed", "lucide:circle-check");
  }

  @Test
  void aServerThatNoTaskUsesAndANewServerHaveNoTasks() throws Exception {
    assertThat(presenter.getServerTasks(4, ids -> Map.of()).getDataList().getRows()).isEmpty();
    assertThat(presenter.getServerTasks(null, ids -> Map.of()).getDataList().getRows()).isEmpty();
  }

  @Test
  void theConnectionTypesAreTheChoiceCardsOfThePanelInTheOrderOfTheMockUp() {
    List<Map<String, CellData>> rows = presenter.getServerTypes().getDataList().getRows();

    assertThat(column(rows, "value")).containsExactly("ssh", "ftp", "folder");
    assertThat(column(rows, "label")).containsExactly("SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH",
      "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FTP", "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FOLDER");
    assertThat(column(rows, "icon")).containsExactly("lucide:terminal", "lucide:upload", "lucide:folder");
    assertThat(column(rows, "description")).containsExactly("SCREEN_TEXT_SCHEDULER_SERVER_TYPE_SSH_HELP",
      "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FTP_HELP", "SCREEN_TEXT_SCHEDULER_SERVER_TYPE_FOLDER_HELP");
  }

  @Test
  void anActiveFilterThatIsNotValidIsRefusedBeforeAnythingIsRead() throws Exception {
    assertThrows(InvalidExecutionFilterException.class, () -> presenter.getServers(null, "2"));
    assertThrows(InvalidExecutionFilterException.class, () -> presenter.getCalendars(null, "yes"));

    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  /**
   * Calendar 1 (active or not) with its past date, Sunday 06/12, Tuesday 08/12, Friday 25/12, Monday 28/12 and
   * Thursday 31/12, used by the launches of some tasks; calendar 2 has a date of its own
   */
  private void givenCalendarTasks(int active, List<Map<String, CellData>> launches) throws Exception {
    Map<String, CellData> calendar = calendar(1, "Holidays");
    calendar.put("Act", new CellData(active));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarRows"), any(ObjectNode.class)))
      .willReturn(data(List.of(calendar, calendar(2, "Other"))));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarDates"), any(ObjectNode.class)))
      .willReturn(data(List.of(date(1, TODAY.minusDays(1)), date(1, LocalDate.of(2026, 12, 31)),
        date(1, LocalDate.of(2026, 12, 6)), date(1, LocalDate.of(2026, 12, 8)), date(1, LocalDate.of(2026, 12, 25)),
        date(1, LocalDate.of(2026, 12, 28)), date(2, LocalDate.of(2026, 12, 1)))));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarTasks"), any(ObjectNode.class)))
      .willReturn(data(launches));
  }

  private static Map<String, CellData> launch(int taskId, String name, int launchType, Integer repeatType,
                                              String weekDays, String hours, String minutes) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", new CellData(taskId));
    row.put("Nam", new CellData(name));
    row.put("TypLch", new CellData(launchType));
    row.put("Act", new CellData(1));
    row.put("RptTyp", repeatType == null ? new CellData() : new CellData(repeatType));
    row.put("WeekDays", weekDays == null ? new CellData() : new CellData(weekDays));
    row.put("ExeHrs", hours == null ? new CellData() : new CellData(hours));
    row.put("ExeMin", minutes == null ? new CellData() : new CellData(minutes));
    row.put("ExeSec", new CellData("0"));
    return row;
  }

  private static TaskCatalogPresenter.TaskSummary summary(int id, String name, TaskWhenSummary.When when,
                                                          LocalDateTime nextFire, Integer lastStatus,
                                                          java.util.Date lastDate) {
    return new TaskCatalogPresenter.TaskSummary(id, name, null, null, true, when.kind(), new TaskLaunch(), when,
      new TaskWhenSummary.Line(nextFire == null ? "—" : "Today 10:50", ""), nextFire, null, lastStatus, false,
      lastDate);
  }

  /**
   * The criteria a loader fills with its select actions, in order
   */
  private static List<String> selected(ServiceData serviceData) {
    return serviceData.getClientActionList().stream()
      .map(action -> action.getTarget() + "=" + ((Object[]) action.getParameters().get("values"))[0]).toList();
  }

  private static List<String> column(List<Map<String, CellData>> rows, String column) {
    return rows.stream().map(row -> row.get(column).getStringValue()).toList();
  }

  private static ServiceData data(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, CellData> server(int id, String name, String type, String host, String port) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeSrv", new CellData(id));
    row.put("Nom", new CellData(name));
    row.put("Pro", new CellData(type));
    row.put("Hst", new CellData(host));
    row.put("Prt", new CellData(port));
    row.put("Act", new CellData(1));
    return row;
  }

  private static Map<String, CellData> usage(int taskId, String name, Integer executionServer, Integer fileServer) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeTsk", new CellData(taskId));
    row.put("Nam", new CellData(name));
    row.put("ExeSrv", executionServer == null ? new CellData() : new CellData(executionServer));
    row.put("FilSrv", fileServer == null ? new CellData() : new CellData(fileServer));
    return row;
  }

  private static Map<String, CellData> calendar(int id, String name) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeCal", new CellData(id));
    row.put("Nom", new CellData(name));
    row.put("Des", new CellData(name + " description"));
    row.put("Act", new CellData(1));
    return row;
  }

  private static Map<String, CellData> date(int calendarId, LocalDate date) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeCal", new CellData(calendarId));
    row.put("Dat", new CellData(Date.valueOf(date)));
    return row;
  }
}
