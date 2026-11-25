package com.almis.awe.scheduler.dao.task;

import com.almis.awe.builder.client.SelectActionBuilder;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.model.util.data.date.LegacyDateConverter;
import com.almis.awe.scheduler.bean.calendar.Schedule;
import com.almis.awe.scheduler.bean.file.ServerTestResult;
import com.almis.awe.scheduler.bean.task.TaskLaunch;
import com.almis.awe.scheduler.dao.task.TaskCatalogPresenter.TaskSummary;
import com.almis.awe.scheduler.enums.TaskLaunchType;
import com.almis.awe.scheduler.enums.TaskStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.util.CalendarDatesImport;
import com.almis.awe.scheduler.util.CalendarRunDays;
import com.almis.awe.scheduler.util.TaskWhenSummary;
import com.almis.awe.service.QueryService;
import org.apache.commons.text.StringEscapeUtils;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.text.MessageFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.almis.awe.model.constant.AweConstants.JSON_LABEL_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_TITLE_PARAMETER;
import static com.almis.awe.model.constant.AweConstants.JSON_VALUE_PARAMETER;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_CALENDAR_DATES;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_CALENDAR_ROWS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_CALENDAR_TASKS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_CALENDAR_USAGE;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_SERVER;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_SERVER_ROWS;
import static com.almis.awe.scheduler.constant.QueryConstants.SCHEDULER_CONFIGURATION_SERVER_USAGE;

/**
 * Servers and calendars of the scheduler configuration screen: every server with its connection and the tasks that use
 * it (the tasks that run on it and the tasks that check their files in it), and every calendar with its dates, its next
 * date from today and the tasks that use it; each list searched, filtered by its active state and with the counts of
 * that filter. The servers and the calendars are few, so they are read at once and filtered and sorted (by name) here.
 * The secrets of a server (passwords and private key) are never read: its side panel only learns whether each one is
 * saved
 */
public class SchedulerConfigurationPresenter {

  /**
   * Tabs of the configuration screen: their values are the identifiers of their containers in the screen
   */
  public static final String TAB_SERVERS = "CfgSrv";
  public static final String TAB_CALENDARS = "CfgCal";
  public static final String TAB_ENGINE = "CfgEng";

  private static final String CALENDAR_PANEL_NAME = "CalPnlNom";
  private static final String CALENDAR_PANEL_DESCRIPTION = "CalPnlDes";
  private static final String CALENDAR_PANEL_ACTIVE = "CalPnlAct";
  private static final String CALENDAR_ID = "IdeCal";
  private static final String KEY_PHRASE_STORED = "SrvKpwSav";
  private static final String KEY_STORED = "SrvKeySav";
  private static final String LOGIN_SECRET_STORED = "SrvPwdSav";
  private static final String SSH_USER = "SshUsr";
  private static final String SERVER_ID = "IdeSrv";
  private static final String TEST_STATE = "SrvTstSta";
  private static final String TEST_TEXT = "SrvTstTxt";
  private static final String TEST_SUBTITLE = "SrvTstSub";

  /**
   * Criteria of the server side panel that its loader fills, in the order they are filled (the connection type first,
   * so the fields of the type are shown before their values arrive): the connection, the empty secret fields (a saved
   * secret is never sent back), whether each secret is saved (SrvPwdSav, SrvKeySav, SrvKpwSav: the screen makes its
   * field read-only, with a placeholder that tells it is saved) and the removal flag of the private key
   */
  public static final List<String> SERVER_PANEL_FIELDS = List.of("Pro", "Nom", "Hst", "Prt", "Act", SSH_USER, "SshPwd",
    "SshKey", "SshKeyPass", LOGIN_SECRET_STORED, KEY_STORED, KEY_PHRASE_STORED, "FlgSshKeyDel");

  /**
   * Criteria of the calendar side panel that its loader fills: the name, the description, the active switch and how
   * many dates the calendar has. They have names of their own: the servers tab of the same screen has Nom and Act
   */
  public static final List<String> CALENDAR_PANEL_FIELDS = List.of(CALENDAR_PANEL_NAME, CALENDAR_PANEL_DESCRIPTION,
    CALENDAR_PANEL_ACTIVE, "CalPnlCnt");

  private static final String ID = "id";
  private static final String NAME = "Nom";
  private static final String ACTIVE = "Act";
  private static final String TASK_ID = "IdeTsk";
  private static final String TASK_NAME = "Nam";
  private static final String TEXT = "SCREEN_TEXT_SCHEDULER_";
  private static final String NO_VALUE = "—";
  private static final int MOST_DELETE_DAYS = 3;
  private static final String SEPARATOR = " · ";
  private static final String DEFAULT_SSH_PORT = "22";
  private static final String NEUTRAL_TONE = "neutral";
  private static final String SSH = "ssh";
  private static final String FTP = "ftp";
  private static final String FOLDER = "folder";
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
  private static final DateTimeFormatter FULL_DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DAY_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
  private static final Comparator<Task> TASK_ORDER = Comparator
    .comparing((Task task) -> task.name.toLowerCase(Locale.ROOT)).thenComparing(Task::id);

  private final QueryService queryService;
  private final QueryUtil queryUtil;
  private final ServiceConfig serviceConfig;
  private final Clock clock;

  /**
   * Constructor
   *
   * @param queryService  Query service
   * @param queryUtil     Query utilities
   * @param serviceConfig Service configuration giving access to the locales
   * @param clock         Clock of the next dates of the calendars
   */
  public SchedulerConfigurationPresenter(QueryService queryService, QueryUtil queryUtil, ServiceConfig serviceConfig,
                                         Clock clock) {
    this.queryService = queryService;
    this.queryUtil = queryUtil;
    this.serviceConfig = serviceConfig;
    this.clock = clock;
  }

  /**
   * Retrieve the tabs of the configuration screen: «Servidores 4», «Calendarios 3» and the engine with its state
   * («Motor · En marcha»), or only its name when the state of the engine is not known
   *
   * @param engineStatus Locale key of the state of the engine, null when the engine cannot be read
   * @return One row per tab: value (its container), label and icon
   * @throws AWException The servers or the calendars cannot be read
   */
  public ServiceData getTabs(String engineStatus) throws AWException {
    DataList dataList = new DataList();
    dataList.addRow(getTab(TAB_SERVERS, text(TEXT + "CONFIGURATION_TAB_SERVERS",
      String.valueOf(getServerRows(null, null).size())), "lucide:server"));
    dataList.addRow(getTab(TAB_CALENDARS, text(TEXT + "CONFIGURATION_TAB_CALENDARS",
      String.valueOf(getCalendarRows(null, null).size())), "lucide:calendar"));
    dataList.addRow(getTab(TAB_ENGINE, isBlank(engineStatus) ? text(TEXT + "CONFIGURATION_TAB_ENGINE")
      : text(TEXT + "CONFIGURATION_TAB_ENGINE_STATUS", text(engineStatus)), "lucide:cpu"));
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, CellData> getTab(String value, String label, String icon) {
    Map<String, CellData> row = new HashMap<>();
    row.put(JSON_VALUE_PARAMETER, new CellData(value));
    row.put(JSON_LABEL_PARAMETER, new CellData(label));
    row.put("icon", new CellData(icon));
    return row;
  }

  /**
   * Retrieve the servers: name, host (the application server for a folder), port, connection type with its icon and
   * its use, active state and the tasks that use it (how many and the first one by name)
   *
   * @param search Text contained in the name or in the host, case insensitive; every server when blank
   * @param active 1 active servers, 0 inactive servers; every server when blank
   * @return Servers sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the servers cannot be read
   */
  public ServiceData getServers(String search, String active) throws AWException {
    return getServers(search, active, Map.of());
  }

  /**
   * Retrieve the servers as {@link #getServers(String, String)} does, each one with its last connection test of the
   * session of the user («OK · 0,42 s» and when, «Error» with when and the reason, or untested)
   *
   * @param search  Text contained in the name or in the host, case insensitive; every server when blank
   * @param active  1 active servers, 0 inactive servers; every server when blank
   * @param results Last connection test of each server tested in the session
   * @return Servers sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the servers cannot be read
   */
  public ServiceData getServers(String search, String active, Map<Integer, ServerTestResult> results)
    throws AWException {
    validateActive(active);
    Map<Integer, Usage> usage = getServerUsage();
    LocalDate today = LocalDate.now(clock);
    DataList dataList = new DataList();
    for (Map<String, CellData> server : getServerRows(search, active)) {
      Integer id = integer(server, SERVER_ID);
      Map<String, CellData> row = getServerRow(server, usage.getOrDefault(id, new Usage()));
      putTestResult(row, results.get(id), today);
      dataList.addRow(row);
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Tasks of the catalog read by their identifiers (their launch, next fire time and last execution)
   */
  @FunctionalInterface
  public interface TaskSummaries {
    /**
     * Read the summary of some tasks
     *
     * @param taskIds Task identifiers
     * @return Summary of each task that exists, by its identifier
     * @throws AWException The tasks cannot be read
     */
    Map<Integer, TaskSummary> read(Collection<Integer> taskIds) throws AWException;
  }

  /**
   * Server for its side panel, as one row and as the select actions that fill the criteria of the panel
   * ({@link #SERVER_PANEL_FIELDS}): its connection, whether its password, its private key and the passphrase of the key
   * are saved (1 or 0, with «Guardada, no se muestra» for a saved one) and its secret fields empty. The secrets are
   * never read nor sent: a blank secret keeps the saved one when the panel saves (UpdSchSrv). Without a server, a new
   * one: an active SSH server on port 22 with nothing saved
   *
   * @param serverId Server, null for a new server
   * @return One row with the fields of the panel and one silent select action per field
   * @throws AWException The server no longer exists or cannot be read
   */
  public ServiceData loadServer(Integer serverId) throws AWException {
    Map<String, Object> values = new LinkedHashMap<>();
    if (serverId == null) {
      values.put("Pro", SSH);
      values.put(NAME, "");
      values.put("Hst", "");
      values.put("Prt", DEFAULT_SSH_PORT);
      values.put(ACTIVE, 1);
      values.put(SSH_USER, "");
      putSecrets(values, false, false, false);
    } else {
      Map<String, CellData> server = getStoredServer(serverId);
      values.put("Pro", Optional.ofNullable(text(server, "Pro")).map(value -> value.toLowerCase(Locale.ROOT)).orElse(""));
      values.put(NAME, Optional.ofNullable(text(server, NAME)).orElse(""));
      values.put("Hst", Optional.ofNullable(text(server, "Hst")).orElse(""));
      values.put("Prt", Optional.ofNullable(text(server, "Prt")).orElse(""));
      values.put(ACTIVE, flag(server) ? 1 : 0);
      values.put(SSH_USER, Optional.ofNullable(text(server, SSH_USER)).orElse(""));
      putSecrets(values, saved(server, LOGIN_SECRET_STORED), saved(server, KEY_STORED), saved(server, KEY_PHRASE_STORED));
    }
    Map<String, CellData> row = new LinkedHashMap<>();
    ServiceData serviceData = new ServiceData();
    for (String field : SERVER_PANEL_FIELDS) {
      Object value = values.get(field);
      row.put(field, new CellData(value));
      serviceData.addClientAction(new SelectActionBuilder(field, value).setSilent(true).build());
    }
    DataList dataList = new DataList();
    dataList.addRow(row);
    dataList.setRecords(1);
    return serviceData.setDataList(dataList);
  }

  /**
   * The secret fields of the panel: always empty, whether each secret is saved, and the key not removed
   */
  private void putSecrets(Map<String, Object> values, boolean password, boolean key, boolean passphrase) {
    values.put("SshPwd", "");
    values.put("SshKey", "");
    values.put("SshKeyPass", "");
    values.put(LOGIN_SECRET_STORED, password ? 1 : 0);
    values.put(KEY_STORED, key ? 1 : 0);
    values.put(KEY_PHRASE_STORED, passphrase ? 1 : 0);
    values.put("FlgSshKeyDel", 0);
  }

  /**
   * Stored server without its secrets, with whether each one is saved
   */
  private Map<String, CellData> getStoredServer(Integer serverId) throws AWException {
    ObjectNode parameters = getParameters();
    parameters.put("serverId", serverId);
    List<Map<String, CellData>> rows = queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_SERVER, parameters)
      .getDataList().getRows();
    if (rows.isEmpty()) {
      throw new AWException(text("SCHEDULER_SERVER_NOT_FOUND_TITLE"), text("SCHEDULER_SERVER_TEST_SERVER_NOT_FOUND"));
    }
    return rows.get(0);
  }

  private static boolean saved(Map<String, CellData> row, String field) {
    return Integer.valueOf(1).equals(integer(row, field));
  }

  /**
   * «En uso por» of the server side panel: the tasks that use the server (the ones that run on it and the ones that
   * check their files in it, as the servers list counts them), sorted by name, each one with a link to its task page,
   * when it is launched («When» summary, its detail and its next execution) and its last status as a tag («OK hoy
   * 08:05»)
   *
   * @param serverId  Server, null for a new server (no task)
   * @param summaries Reader of the summaries of the tasks
   * @return One row per task: id, IdeTsk, TskNam (link), TskUseSub, TskStaTxt, TskStaIcn and TskStaTon
   * @throws AWException The tasks cannot be read
   */
  public ServiceData getServerTasks(Integer serverId, TaskSummaries summaries) throws AWException {
    DataList dataList = new DataList();
    Usage usage = serverId == null ? null : getServerUsage().get(serverId);
    if (usage != null) {
      List<Task> tasks = usage.tasks.values().stream().sorted(TASK_ORDER).toList();
      Map<Integer, TaskSummary> found = summaries.read(tasks.stream().map(Task::id).toList());
      LocalDateTime now = LocalDateTime.now(clock);
      TaskWhenSummary words = new TaskWhenSummary(this::text);
      for (Task task : tasks) {
        dataList.addRow(getServerTaskRow(task, found.get(task.id()), words, now));
      }
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Row of a task of the server side panel
   */
  private Map<String, CellData> getServerTaskRow(Task task, TaskSummary summary, TaskWhenSummary words,
                                                 LocalDateTime now) throws AWException {
    Map<String, CellData> row = new HashMap<>();
    row.put(ID, new CellData(task.id()));
    row.put(TASK_ID, new CellData(task.id()));
    ObjectNode link = JsonNodeFactory.instance.objectNode();
    link.put(JSON_VALUE_PARAMETER, task.name());
    link.put(JSON_LABEL_PARAMETER, task.name());
    link.put(JSON_TITLE_PARAMETER, summary == null || isBlank(summary.description()) ? task.name()
      : summary.description());
    row.put("TskNam", new CellData(link));
    List<String> launch = new ArrayList<>();
    if (summary != null) {
      launch.add(summary.when().text());
      launch.add(summary.when().detail());
      if (summary.nextFire() != null) {
        launch.add(text(TEXT + "SERVER_TASK_NEXT", lowerFirst(summary.next().text())));
      }
    }
    row.put("TskUseSub", new CellData(String.join(SEPARATOR, launch.stream().filter(part -> !isBlank(part)).toList())));
    if (summary == null || summary.lastStatus() == null) {
      row.put("TskStaTxt", new CellData(text("SCREEN_TEXT_TASK_CATALOG_NO_EXECUTIONS")));
      row.put("TskStaIcn", new CellData("lucide:circle-dashed"));
      row.put("TskStaTon", new CellData(NEUTRAL_TONE));
    } else {
      String code = summary.waiting() && summary.lastStatus() == TaskStatus.JOB_RUNNING.getValue() ? "deferred"
        : String.valueOf(summary.lastStatus());
      Date lastDate = summary.lastDate();
      String label = serviceConfig.getLocale(queryService.findLabel("ActStaTyp", code));
      row.put("TskStaTxt", new CellData(lastDate == null ? label : text(TEXT + "SERVER_TASK_STATUS", label,
        lowerFirst(words.relative(LegacyDateConverter.asLocalDateTime(lastDate), now)))));
      row.put("TskStaIcn", new CellData(queryService.findLabel("ActStaIco", code)));
      row.put("TskStaTon", new CellData(queryService.findLabel("ActStaTon", code)));
    }
    return row;
  }

  /**
   * Choice cards of the connection type of the server side panel: SSH, FTP and folder, each with its icon and what it
   * is for (locale keys, translated by the client)
   *
   * @return One row per type: value, label, icon and description
   */
  public ServiceData getServerTypes() {
    DataList dataList = new DataList();
    for (String[] type : List.of(new String[]{SSH, "lucide:terminal"}, new String[]{FTP, "lucide:upload"},
      new String[]{FOLDER, "lucide:folder"})) {
      String key = TEXT + "SERVER_TYPE_" + type[0].toUpperCase(Locale.ROOT);
      Map<String, CellData> row = new HashMap<>();
      row.put(JSON_VALUE_PARAMETER, new CellData(type[0]));
      row.put(JSON_LABEL_PARAMETER, new CellData(key));
      row.put("icon", new CellData(type[1]));
      row.put("description", new CellData(key + "_HELP"));
      dataList.addRow(row);
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Calendar for its side panel, as the select actions that fill the criteria of the panel
   * ({@link #CALENDAR_PANEL_FIELDS}): its name, its description, whether it is active and how many dates it has
   * («10 fechas»). Without a calendar, a new one: active, with no dates
   *
   * @param calendarId Calendar, null for a new calendar
   * @return One silent select action per field
   * @throws AWException The calendar no longer exists or cannot be read
   */
  public ServiceData loadCalendar(Integer calendarId) throws AWException {
    Map<String, Object> values = new LinkedHashMap<>();
    int dates = 0;
    if (calendarId == null) {
      values.put(CALENDAR_PANEL_NAME, "");
      values.put(CALENDAR_PANEL_DESCRIPTION, "");
      values.put(CALENDAR_PANEL_ACTIVE, 1);
    } else {
      Map<String, CellData> calendar = getCalendarRows(null, null).stream()
        .filter(row -> calendarId.equals(integer(row, CALENDAR_ID))).findFirst()
        .orElseThrow(() -> new AWException(text("SCHEDULER_CALENDAR_NOT_FOUND_TITLE"),
          text("SCHEDULER_CALENDAR_NOT_FOUND_MESSAGE")));
      values.put(CALENDAR_PANEL_NAME, Optional.ofNullable(text(calendar, NAME)).orElse(""));
      values.put(CALENDAR_PANEL_DESCRIPTION, Optional.ofNullable(text(calendar, "Des")).orElse(""));
      values.put(CALENDAR_PANEL_ACTIVE, flag(calendar) ? 1 : 0);
      dates = getCalendarDateRows(calendarId).size();
    }
    values.put("CalPnlCnt", switch (dates) {
      case 0 -> text(TEXT + "CALENDAR_PANEL_DATES_NONE");
      case 1 -> text(TEXT + "CALENDAR_PANEL_DATES_ONE");
      default -> text(TEXT + "CALENDAR_PANEL_DATES_MANY", String.valueOf(dates));
    });
    ServiceData serviceData = new ServiceData();
    for (String field : CALENDAR_PANEL_FIELDS) {
      serviceData.addClientAction(new SelectActionBuilder(field, values.get(field)).setSilent(true).build());
    }
    return serviceData;
  }

  /**
   * Dates of the calendar side panel: the dates of the calendar in order, each one with its weekday («dom», «vie», as
   * the task catalog names them)
   *
   * @param calendarId Calendar, null for a new calendar (no dates)
   * @return One row per date: id, IdeDat, Dat (dd/MM/yyyy), Nam and DatWek
   * @throws AWException The dates cannot be read
   */
  public ServiceData getCalendarDates(Integer calendarId) throws AWException {
    DataList dataList = new DataList();
    if (calendarId != null) {
      for (Map<String, CellData> date : getCalendarDateRows(calendarId)) {
        LocalDate day = LegacyDateConverter.asLocalDate(date.get("Dat").getDateValue());
        Integer id = integer(date, "IdeDat");
        Map<String, CellData> row = new HashMap<>();
        row.put(ID, new CellData(id));
        row.put("IdeDat", new CellData(id));
        row.put("Dat", new CellData(FULL_DAY.format(day)));
        row.put(TASK_NAME, new CellData(Optional.ofNullable(text(date, TASK_NAME)).orElse("")));
        row.put("DatWek", new CellData(text(CalendarDatesImport.weekdayKey(day))));
        dataList.addRow(row);
      }
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Stored dates of a calendar, in order
   */
  private List<Map<String, CellData>> getCalendarDateRows(Integer calendarId) throws AWException {
    return queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_DATES, getParameters()).getDataList()
      .getRows().stream()
      .filter(row -> calendarId.equals(integer(row, CALENDAR_ID)) && row.get("Dat") != null
        && row.get("Dat").getObjectValue() != null)
      .sorted(Comparator.comparing((Map<String, CellData> row) -> row.get("Dat").getDateValue()))
      .toList();
  }

  /**
   * A text that goes inside a line («próxima hoy 10:50», «OK hoy 08:05») starts in lower case
   */
  private static String lowerFirst(String text) {
    return isBlank(text) ? "" : text.substring(0, 1).toLowerCase(Locale.ROOT) + text.substring(1);
  }

  /**
   * Refuse to delete a server that a task uses (it runs on it or checks its files in it): the refusal names the tasks,
   * so they can be changed first
   *
   * @param serverIds Servers to delete, as numbers or as text
   * @throws AWException A server is used by a task, an identifier is not a number (refused before anything is read),
   *                     or the tasks cannot be read
   */
  public void validateServersNotInUse(List<?> serverIds) throws AWException {
    Set<Integer> servers = new HashSet<>();
    for (Object serverId : serverIds == null ? List.of() : serverIds) {
      if (serverId != null && !isBlank(String.valueOf(serverId))) {
        servers.add(identifier(serverId));
      }
    }
    Usage used = new Usage();
    getServerUsage().forEach((server, usage) -> {
      if (servers.contains(server)) {
        usage.tasks.values().forEach(task -> used.tasks.putIfAbsent(task.id, task));
      }
    });
    // The maintain message is HTML: the names of the tasks are escaped
    List<String> names = used.tasks.values().stream().sorted(TASK_ORDER)
      .map(task -> StringEscapeUtils.escapeHtml4(task.name)).toList();
    if (!names.isEmpty()) {
      throw new AWException(text("SCHEDULER_SERVER_IN_USE_TITLE"), names.size() == 1
        ? text("SCHEDULER_SERVER_IN_USE_ONE", names.get(0))
        : text("SCHEDULER_SERVER_IN_USE_MANY", String.valueOf(names.size()), String.join(", ", names)));
    }
  }

  /**
   * Identifier sent by the caller: a number, or a validation refusal that does not repeat the value
   */
  private Integer identifier(Object value) throws AWException {
    try {
      return Integer.valueOf(String.valueOf(value).trim());
    } catch (NumberFormatException exc) {
      throw new AWException(text("SCHEDULER_INVALID_IDENTIFIER_TITLE"), text("SCHEDULER_INVALID_IDENTIFIER_MESSAGE"));
    }
  }

  /**
   * Retrieve the options of the active filter of the servers with their counts (every server, the active ones and the
   * inactive ones), counted with the search
   *
   * @param search Text contained in the name or in the host
   * @return One row per option: value, label (locale key), count and tone
   * @throws AWException The servers cannot be read
   */
  public ServiceData getServerActiveCounts(String search) throws AWException {
    return getActiveCounts(getServerRows(search, null));
  }

  /**
   * Retrieve the calendars: name, description, active state, how many dates they have, their next date from today and
   * the tasks that use them (how many and the first one by name)
   *
   * @param search Text contained in the name, case insensitive; every calendar when blank
   * @param active 1 active calendars, 0 inactive calendars; every calendar when blank
   * @return Calendars sorted by name
   * @throws AWException The active filter is not valid (InvalidExecutionFilterException) or the calendars cannot be
   *                     read
   */
  public ServiceData getCalendars(String search, String active) throws AWException {
    validateActive(active);
    Map<Integer, List<LocalDate>> dates = new HashMap<>();
    for (Map<String, CellData> row : queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_DATES,
      getParameters()).getDataList().getRows()) {
      CellData date = row.get("Dat");
      if (date != null && date.getObjectValue() != null) {
        dates.computeIfAbsent(integer(row, CALENDAR_ID), key -> new ArrayList<>())
          .add(LegacyDateConverter.asLocalDate(date.getDateValue()));
      }
    }
    Map<Integer, Usage> usage = new HashMap<>();
    for (Map<String, CellData> row : queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_USAGE,
      getParameters()).getDataList().getRows()) {
      usage.computeIfAbsent(integer(row, CALENDAR_ID), key -> new Usage()).add(row);
    }
    LocalDate today = LocalDate.now(clock);
    DataList dataList = new DataList();
    for (Map<String, CellData> calendar : getCalendarRows(search, active)) {
      Integer id = integer(calendar, CALENDAR_ID);
      dataList.addRow(getCalendarRow(calendar, dates.getOrDefault(id, List.of()), usage.getOrDefault(id, new Usage()),
        today));
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Before deleting a calendar: the tasks that use it (the launch of the task has it, as the calendars list counts
   * them), sorted by name, each one with its «When» summary and the next days (from now, at most
   * {@value #MOST_DELETE_DAYS}) it would run on without the calendar, the days the calendar excludes today. The days
   * are the ones its repetition fires on; when the repetition cannot tell them, the next days of the calendar (said
   * so). A task launched on demand or inactive runs no day by itself, and an inactive calendar excludes no day
   *
   * @param calendarId Calendar to delete
   * @return One row per task: id, IdeTsk, Nam, CalDelWhn («When» summary), CalDelDat («would run on 08/12 and
   * 25/12», empty when no day changes), CalDelDatCnt (how many days), CalDelSub (the line under the task in the delete
   * dialog: when it runs and those days) and the heading of the dialog, the same in every row: title («Delete
   * «Holidays»?») and description (how many tasks use it and what happens to them)
   * @throws AWException The tasks or the calendars cannot be read
   */
  public ServiceData getCalendarDeleteCheck(Integer calendarId) throws AWException {
    DataList dataList = new DataList();
    if (calendarId != null) {
      Optional<Map<String, CellData>> calendar = queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_ROWS,
          getParameters()).getDataList().getRows().stream()
        .filter(row -> calendarId.equals(integer(row, CALENDAR_ID))).findFirst();
      boolean active = calendar.map(SchedulerConfigurationPresenter::flag).orElse(false);
      String title = text(TEXT + "CALENDAR_DELETE_TITLE", calendar.map(row -> text(row, NAME)).orElse(""));
      List<LocalDate> days = new ArrayList<>();
      for (Map<String, CellData> row : queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_DATES,
        getParameters()).getDataList().getRows()) {
        CellData date = row.get("Dat");
        if (calendarId.equals(integer(row, CALENDAR_ID)) && date != null && date.getObjectValue() != null) {
          days.add(LegacyDateConverter.asLocalDate(date.getDateValue()));
        }
      }
      ObjectNode parameters = getParameters();
      parameters.put("calendarId", calendarId);
      LocalDateTime now = LocalDateTime.now(clock);
      TaskWhenSummary summary = new TaskWhenSummary(this::text);
      List<Map<String, CellData>> tasks = queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_TASKS,
          parameters).getDataList().getRows().stream()
        .sorted(Comparator.comparing((Map<String, CellData> row) -> Optional.ofNullable(text(row, TASK_NAME))
          .orElse("").toLowerCase(Locale.ROOT)).thenComparing(row -> integer(row, TASK_ID)))
        .toList();
      String description = tasks.size() == 1 ? text(TEXT + "CALENDAR_DELETE_TEXT_ONE")
        : text(TEXT + "CALENDAR_DELETE_TEXT_MANY", String.valueOf(tasks.size()));
      for (Map<String, CellData> task : tasks) {
        Map<String, CellData> row = getCalendarDeleteRow(task, active ? days : List.of(), now, summary);
        row.put("title", new CellData(title));
        row.put("description", new CellData(description));
        dataList.addRow(row);
      }
    }
    dataList.setRecords(dataList.getRows().size());
    return new ServiceData().setDataList(dataList);
  }

  /**
   * Row of a task of the delete check of a calendar: its «When» summary and the days it would run on without it
   */
  private Map<String, CellData> getCalendarDeleteRow(Map<String, CellData> task, List<LocalDate> days,
                                                     LocalDateTime now, TaskWhenSummary summary) {
    Integer taskId = integer(task, TASK_ID);
    Integer launchType = integer(task, "TypLch");
    Schedule schedule = getSchedule(task);
    DeleteRuns deleteRuns = getDeleteRuns(task, launchType, schedule, days, now);
    String key = deleteRuns.key();
    List<LocalDate> runs = deleteRuns.runs();
    boolean fired = deleteRuns.fired();
    Map<String, CellData> row = new HashMap<>();
    row.put(ID, new CellData(taskId));
    row.put(TASK_ID, new CellData(taskId));
    row.put(TASK_NAME, new CellData(Optional.ofNullable(text(task, TASK_NAME)).orElse("")));
    String when = summary.when(getLaunch(launchType, schedule)).text();
    String changes = runs.isEmpty() ? "" : text(key, join(runs.stream()
      .map(day -> text(TEXT + "CALENDAR_DELETE_DAY", formatDay(day, now)))
      .toList()));
    row.put("CalDelWhn", new CellData(when));
    row.put("CalDelDat", new CellData(changes));
    row.put("CalDelSub", new CellData(changes.isEmpty() ? when : when + SEPARATOR + changes));
    row.put("CalDelDatCnt", new CellData(runs.size()));
    // «En uso por» of the side panel: the days of the calendar its repetition fires on, so it will not run on them
    boolean skips = fired && !runs.isEmpty();
    row.put("CalSkpTxt", new CellData(skips ? text(TEXT + "CALENDAR_SKIPS", join(runs.stream()
      .map(day -> formatDay(day, now)).toList())) : ""));
    row.put("CalSkpTon", new CellData(skips ? "info" : ""));
    row.put("CalSkpIcn", new CellData(skips ? "lucide:calendar-x" : ""));
    return row;
  }

  /**
   * Days of the delete check of a calendar for a task: the days its repetition fires on (fired), the next dates of the
   * calendar when its repetition cannot tell, or none. Only an active task with a trigger of its repetition (scheduled,
   * or checking its files) runs by itself
   */
  private static DeleteRuns getDeleteRuns(Map<String, CellData> task, Integer launchType, Schedule schedule,
                                          List<LocalDate> days, LocalDateTime now) {
    boolean runsByItself = TaskLaunchType.SCHEDULED.getValue().equals(launchType)
      || TaskLaunchType.FILE_TRACKING.getValue().equals(launchType);
    if (days.isEmpty() || !flag(task) || !runsByItself) {
      return new DeleteRuns(TEXT + "CALENDAR_DELETE_RUNS", List.of(), false);
    }
    Optional<List<LocalDate>> fires = CalendarRunDays.runDays(schedule, days, now, MOST_DELETE_DAYS);
    if (fires.isPresent()) {
      return new DeleteRuns(TEXT + "CALENDAR_DELETE_RUNS", fires.get(), true);
    }
    return new DeleteRuns(TEXT + "CALENDAR_DELETE_DATES", days.stream().distinct()
      .filter(day -> !day.isBefore(now.toLocalDate())).sorted().limit(MOST_DELETE_DAYS).toList(), false);
  }

  /**
   * A day of the delete check: without its year when it is in the current year
   */
  private static String formatDay(LocalDate day, LocalDateTime now) {
    return (day.getYear() == now.getYear() ? DAY : FULL_DAY).format(day);
  }

  /**
   * Stored repetition of the launch of a task
   */
  private static Schedule getSchedule(Map<String, CellData> row) {
    return new Schedule()
      .setRepeatType(integer(row, "RptTyp"))
      .setRepeatNumber(integer(row, "RptNum"))
      .setInitialDate(date(row, "IniDat"))
      .setInitialTime(text(row, "IniTim"))
      .setEndDate(date(row, "EndDat"))
      .setEndTime(text(row, "EndTim"))
      .setDate(date(row, "ExeDte"))
      .setTime(text(row, "ExeTim"))
      .setYearList(list(row, "ExeYrs"))
      .setMonthList(list(row, "ExeMth"))
      .setWeekList(list(row, "ExeWek"))
      .setDayList(list(row, "ExeDay"))
      .setWeekDayList(list(row, "WeekDays"))
      .setHourList(list(row, "ExeHrs"))
      .setMinuteList(list(row, "ExeMin"))
      .setSecondList(list(row, "ExeSec"));
  }

  /**
   * Launch of a task as its «When» summary reads it, without its calendar
   */
  private static TaskLaunch getLaunch(Integer launchType, Schedule schedule) {
    return new TaskLaunch()
      .setLaunchType(launchType)
      .setRepeatType(schedule.getRepeatType())
      .setRepeatNumber(schedule.getRepeatNumber())
      .setWeekDays(schedule.getWeekDayList())
      .setDays(schedule.getDayList())
      .setHours(schedule.getHourList())
      .setMinutes(schedule.getMinuteList())
      .setSeconds(schedule.getSecondList())
      .setDate(schedule.getDate() == null ? null : LegacyDateConverter.asLocalDate(schedule.getDate()))
      .setTime(schedule.getTime());
  }

  /**
   * Items of a list in a sentence: «a, b and c»
   */
  private String join(List<String> items) {
    if (items.size() <= 1) {
      return items.isEmpty() ? "" : items.get(0);
    }
    return text("SCREEN_TEXT_TASK_CATALOG_AND", String.join(", ", items.subList(0, items.size() - 1)),
      items.get(items.size() - 1));
  }

  /**
   * Retrieve the options of the active filter of the calendars with their counts (every calendar, the active ones and
   * the inactive ones), counted with the search
   *
   * @param search Text contained in the name
   * @return One row per option: value, label (locale key), count and tone
   * @throws AWException The calendars cannot be read
   */
  public ServiceData getCalendarActiveCounts(String search) throws AWException {
    return getActiveCounts(getCalendarRows(search, null));
  }

  /**
   * Servers that match the search (name or host shown) and the active filter, sorted by name
   */
  private List<Map<String, CellData>> getServerRows(String search, String active) throws AWException {
    String text = isBlank(search) ? null : search.trim().toLowerCase(Locale.ROOT);
    Predicate<Map<String, CellData>> matches = server -> text == null
      || contains(text(server, NAME), text) || (!isFolder(server) && contains(text(server, "Hst"), text));
    return filter(queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_SERVER_ROWS, getParameters()).getDataList()
      .getRows(), matches, active);
  }

  /**
   * Calendars that match the search (name) and the active filter, sorted by name
   */
  private List<Map<String, CellData>> getCalendarRows(String search, String active) throws AWException {
    String text = isBlank(search) ? null : search.trim().toLowerCase(Locale.ROOT);
    return filter(queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_CALENDAR_ROWS, getParameters())
      .getDataList().getRows(), row -> text == null || contains(text(row, NAME), text), active);
  }

  /**
   * Tasks that use each server: the ones that run on it and the ones that check their files in it, each task once
   */
  private Map<Integer, Usage> getServerUsage() throws AWException {
    Map<Integer, Usage> usage = new HashMap<>();
    for (Map<String, CellData> row : queryService.launchPrivateQuery(SCHEDULER_CONFIGURATION_SERVER_USAGE,
      getParameters()).getDataList().getRows()) {
      Integer executionServer = integer(row, "ExeSrv");
      Integer fileServer = integer(row, "FilSrv");
      if (executionServer != null) {
        usage.computeIfAbsent(executionServer, key -> new Usage()).add(row).commands = true;
      }
      if (fileServer != null) {
        usage.computeIfAbsent(fileServer, key -> new Usage()).add(row).files = true;
      }
    }
    return usage;
  }

  /**
   * Row of a server as the list shows it
   */
  private Map<String, CellData> getServerRow(Map<String, CellData> server, Usage usage) {
    Map<String, CellData> row = new HashMap<>();
    Integer id = integer(server, SERVER_ID);
    String type = Optional.ofNullable(text(server, "Pro")).map(value -> value.toLowerCase(Locale.ROOT)).orElse("");
    boolean folder = FOLDER.equals(type);
    row.put(ID, new CellData(id));
    row.put(SERVER_ID, new CellData(id));
    row.put(NAME, new CellData(Optional.ofNullable(text(server, NAME)).orElse("")));
    row.put("Pro", new CellData(type));
    row.put(ACTIVE, new CellData(flag(server) ? 1 : 0));
    // A folder is in the application server: no host nor port
    row.put("SrvHst", new CellData(folder ? text(TEXT + "SERVER_APPLICATION")
      : Optional.ofNullable(text(server, "Hst")).orElse("")));
    row.put("SrvHstApp", new CellData(folder ? 1 : 0));
    String port = text(server, "Prt");
    row.put("SrvPrt", new CellData(folder || port == null ? NO_VALUE : port));
    String typeText = switch (type) {
      case SSH, FTP, FOLDER -> text(TEXT + "SERVER_TYPE_" + type.toUpperCase(Locale.ROOT));
      default -> type.toUpperCase(Locale.ROOT);
    };
    row.put("SrvTypTxt", new CellData(typeText));
    row.put("SrvTypIcn", new CellData(switch (type) {
      case SSH -> "lucide:terminal";
      case FTP -> "lucide:upload";
      case FOLDER -> "lucide:folder";
      default -> "lucide:server";
    }));
    row.put("SrvTypSub", new CellData(getServerUse(type, usage)));
    putUsage(row, "Srv", usage);
    // Subtitle of the side panel: «Servidor externo · SSH · lo usa 1 tarea»
    row.put("SrvPnlSub", new CellData(text(TEXT + (folder ? "SERVER_PANEL_LOCAL" : "SERVER_PANEL_EXTERNAL"), typeText)
      + SEPARATOR + getUsedBy(usage)));
    return row;
  }

  /**
   * How many tasks use a server or a calendar, for the subtitle of its side panel: «lo usa 1 tarea»
   */
  private String getUsedBy(Usage usage) {
    int count = usage.tasks.size();
    return switch (count) {
      case 0 -> text(TEXT + "SERVER_PANEL_USED_NONE");
      case 1 -> text(TEXT + "SERVER_PANEL_USED_ONE");
      default -> text(TEXT + "SERVER_PANEL_USED_MANY", String.valueOf(count));
    };
  }

  /**
   * Last connection test of a server in the session: its state (ok, error, none), its text («OK · 0,42 s», «Error»,
   * «Sin probar») and the line below (when, and the reason of a failure)
   */
  private void putTestResult(Map<String, CellData> row, ServerTestResult result, LocalDate today) {
    if (result == null) {
      row.put(TEST_STATE, new CellData("none"));
      row.put(TEST_TEXT, new CellData(text(TEXT + "SERVER_TEST_NONE")));
      row.put(TEST_SUBTITLE, new CellData(""));
      return;
    }
    LocalDateTime testedAt = result.testedAt();
    String when = testedAt.toLocalDate().equals(today)
      ? text("SCREEN_TEXT_TASK_CATALOG_TODAY", TIME.format(testedAt)) : DAY_TIME.format(testedAt);
    if (result.ok()) {
      row.put(TEST_STATE, new CellData("ok"));
      row.put(TEST_TEXT, new CellData(text(TEXT + "SERVER_TEST_OK", text(TEXT + "SERVER_TEST_SECONDS",
        result.seconds()))));
      row.put(TEST_SUBTITLE, new CellData(when));
    } else {
      row.put(TEST_STATE, new CellData("error"));
      row.put(TEST_TEXT, new CellData(text(TEXT + "SERVER_TEST_ERROR")));
      row.put(TEST_SUBTITLE, new CellData(text(TEXT + "SERVER_TEST_ERROR_SUB", when,
        text(TEXT + "SERVER_TEST_REASON_" + result.reason()))));
    }
  }

  /**
   * What a server is used for: an SSH server runs remote commands and serves files over SFTP, as its tasks use it
   */
  private String getServerUse(String type, Usage usage) {
    String use = switch (type) {
      case SSH -> {
        if (usage.commands && usage.files) {
          yield "COMMANDS_AND_FILES";
        } else if (usage.commands) {
          yield "COMMANDS";
        } else {
          yield usage.files ? "FILES" : "SSH";
        }
      }
      case FTP -> "FTP";
      case FOLDER -> "FOLDER";
      default -> null;
    };
    return use == null ? "" : text(TEXT + "SERVER_USE_" + use);
  }

  /**
   * Row of a calendar as the list shows it
   */
  private Map<String, CellData> getCalendarRow(Map<String, CellData> calendar, List<LocalDate> dates, Usage usage,
                                               LocalDate today) {
    Map<String, CellData> row = new HashMap<>();
    Integer id = integer(calendar, CALENDAR_ID);
    row.put(ID, new CellData(id));
    row.put(CALENDAR_ID, new CellData(id));
    row.put(NAME, new CellData(Optional.ofNullable(text(calendar, NAME)).orElse("")));
    row.put("Des", new CellData(Optional.ofNullable(text(calendar, "Des")).orElse("")));
    row.put(ACTIVE, new CellData(flag(calendar) ? 1 : 0));
    row.put("CalDatCnt", new CellData(dates.size()));
    // The next date from today: of this year, its day; of a later year, the calendar starts then
    Optional<LocalDate> next = dates.stream().filter(date -> !date.isBefore(today)).min(Comparator.naturalOrder());
    row.put("CalNxtDat", new CellData(next.map(DAY::format).orElse("")));
    row.put("CalDatSub", new CellData(next
      .map(date -> date.getYear() == today.getYear() ? text(TEXT + "CALENDAR_NEXT", DAY.format(date))
        : text(TEXT + "CALENDAR_FROM", FULL_DAY.format(date)))
      .orElseGet(() -> text(TEXT + "CALENDAR_NO_NEXT"))));
    putUsage(row, "Cal", usage);
    // Header of the side panel: its icon and «Calendario · lo usa 1 tarea»
    row.put("CalPnlIcn", new CellData("lucide:calendar-x"));
    row.put("CalPnlSub", new CellData(text(TEXT + "CALENDAR_PANEL_KIND") + SEPARATOR + getUsedBy(usage)));
    return row;
  }

  /**
   * Tasks that use a server or a calendar: how many, the text of the list and the first one by name
   */
  private void putUsage(Map<String, CellData> row, String prefix, Usage usage) {
    int count = usage.tasks.size();
    row.put(prefix + "UseCnt", new CellData(count));
    row.put(prefix + "UseTxt", new CellData(switch (count) {
      case 0 -> text(TEXT + "USED_BY_NONE");
      case 1 -> text(TEXT + "USED_BY_ONE");
      default -> text(TEXT + "USED_BY_MANY", String.valueOf(count));
    }));
    row.put(prefix + "UseSub", new CellData(usage.tasks.values().stream().min(TASK_ORDER)
      .map(task -> task.name).orElse("")));
  }

  private ServiceData getActiveCounts(List<Map<String, CellData>> rows) {
    long active = rows.stream().filter(SchedulerConfigurationPresenter::flag).count();
    DataList dataList = new DataList();
    dataList.addRow(getCount("", TEXT + "CONFIGURATION_ALL", rows.size()));
    dataList.addRow(getCount("1", TEXT + "CONFIGURATION_ACTIVE", active));
    dataList.addRow(getCount("0", TEXT + "CONFIGURATION_INACTIVE", rows.size() - active));
    return new ServiceData().setDataList(dataList);
  }

  private static Map<String, CellData> getCount(String value, String label, long count) {
    Map<String, CellData> row = new HashMap<>();
    row.put(JSON_VALUE_PARAMETER, new CellData(value));
    row.put(JSON_LABEL_PARAMETER, new CellData(label));
    row.put("count", new CellData(count));
    row.put("tone", new CellData(""));
    return row;
  }

  private static List<Map<String, CellData>> filter(List<Map<String, CellData>> rows,
                                                    Predicate<Map<String, CellData>> matches, String active) {
    String state = isBlank(active) ? null : active.trim();
    return rows.stream()
      .filter(matches)
      .filter(row -> state == null || state.equals(flag(row) ? "1" : "0"))
      .sorted(Comparator.comparing((Map<String, CellData> row) -> Optional.ofNullable(text(row, NAME)).orElse("")
        .toLowerCase(Locale.ROOT)))
      .toList();
  }

  /**
   * Refuse an active filter that is not valid, never ignore it: the list is never wider than the filter asks for
   */
  private void validateActive(String active) throws InvalidExecutionFilterException {
    if (!isBlank(active) && !Set.of("0", "1").contains(active.trim())) {
      throw new InvalidExecutionFilterException(serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_TITLE"),
        serviceConfig.getLocale("SCHEDULER_INVALID_FILTER_MESSAGE", "active", active));
    }
  }

  /**
   * Parameters of the queries: every row, in the order of the query
   */
  private ObjectNode getParameters() {
    ObjectNode parameters = queryUtil.getParameters(null, "1", "0");
    parameters.putArray("sort");
    return parameters;
  }

  /**
   * Text of a locale with its parameters. The parameters are not escaped: the grid shows the texts as text
   */
  private String text(String key, String... parameters) {
    String template = serviceConfig.getLocale(key);
    return parameters.length == 0 ? template : MessageFormat.format(template, (Object[]) parameters);
  }

  private static boolean isFolder(Map<String, CellData> server) {
    return FOLDER.equalsIgnoreCase(text(server, "Pro"));
  }

  private static boolean contains(String value, String text) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(text);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String text(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    String value = cell == null ? null : cell.getStringValue();
    return isBlank(value) ? null : value.trim();
  }

  private static Integer integer(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getObjectValue() == null || isBlank(cell.getStringValue()) ? null
      : cell.getIntegerValue();
  }

  private static java.util.Date date(Map<String, CellData> row, String field) {
    CellData cell = row.get(field);
    return cell == null || cell.getObjectValue() == null ? null : cell.getDateValue();
  }

  /**
   * Values of a list stored as text separated by commas
   */
  private static List<String> list(Map<String, CellData> row, String field) {
    String value = text(row, field);
    return value == null ? List.of() : Arrays.stream(value.split(",")).map(String::trim)
      .filter(item -> !item.isEmpty()).toList();
  }

  private static boolean flag(Map<String, CellData> row) {
    return Integer.valueOf(1).equals(integer(row, ACTIVE));
  }

  /**
   * Tasks that use a server or a calendar, each one once, and how a server is used
   */
  private static final class Usage {
    private final Map<Integer, Task> tasks = new LinkedHashMap<>();
    private boolean commands;
    private boolean files;

    private Usage add(Map<String, CellData> row) {
      Integer id = integer(row, TASK_ID);
      tasks.putIfAbsent(id, new Task(id, Optional.ofNullable(text(row, TASK_NAME)).orElse("")));
      return this;
    }
  }

  private record DeleteRuns(String key, List<LocalDate> runs, boolean fired) {
  }

  private record Task(Integer id, String name) {
  }
}
