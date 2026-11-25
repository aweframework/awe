package com.almis.awe.scheduler.dao.task;

import com.almis.awe.config.ServiceConfig;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.QueryUtil;
import com.almis.awe.scheduler.bean.task.DeferredExecution;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.Engine;
import static com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineStatus;
import static com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineTrigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * «Motor» tab of the scheduler configuration screen (#993): the card of the engine (its status and the sentence that
 * tells it, its data and its technical details, the execution that waits for a confirmation) and the schedules that
 * the engine holds (one row per trigger of a task, filtered by its state with counts)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SchedulerEnginePresenterTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 17, 10, 45);
  private static final Map<String, String> LOCALES = new HashMap<>();

  static {
    String engine = "SCREEN_TEXT_SCHEDULER_ENGINE_";
    LOCALES.put(engine + "SUMMARY_RUNNING", "El motor {0} está en marcha desde hace {1} y lanza las tareas a su hora. "
      + "Tiene {2} y ahora mismo {3} ({4} de {5}).");
    LOCALES.put(engine + "SUMMARY_PAUSED", "El motor {0} está pausado: conserva {1}.");
    LOCALES.put(engine + "SUMMARY_PAUSED_EMPTY", "El motor {0} está pausado y no tiene nada programado.");
    LOCALES.put(engine + "SUMMARY_STOPPED", "El motor {0} está detenido.");
    LOCALES.put(engine + "SUMMARY_UNAVAILABLE", "No se puede leer el estado del motor.");
    LOCALES.put(engine + "SCHEDULES_ONE", "1 programación");
    LOCALES.put(engine + "SCHEDULES_MANY", "{0} programaciones");
    LOCALES.put(engine + "THREADS_NONE", "no ocupa ningún hilo");
    LOCALES.put(engine + "THREADS_ONE", "ocupa 1 hilo");
    LOCALES.put(engine + "THREADS_MANY", "ocupa {0} hilos");
    LOCALES.put(engine + "THREADS_OF", "{0} de {1}");
    LOCALES.put(engine + "AGO", "hace {0}");
    LOCALES.put(engine + "IN", "dentro de {0}");
    LOCALES.put(engine + "VERSION", "Quartz {0}");
    LOCALES.put(engine + "INSTANCE_LOCAL", "En este servidor (no remoto)");
    LOCALES.put(engine + "STORE_MEMORY", "En memoria");
    LOCALES.put(engine + "STORE_NOT_CLUSTERED", "Sin clúster");
    LOCALES.put(engine + "STORE_LOADS_TASKS", "al arrancar carga las tareas activas de la base de datos");
    LOCALES.put(engine + "TECHNICAL", "Almacén {0} · hilos {1} · persistente: {2} · clúster: {3} · remoto: {4}");
    LOCALES.put(engine + "YES", "sí");
    LOCALES.put(engine + "NO", "no");
    LOCALES.put(engine + "DEFERRED", "Ahora hay {0} esperando confirmación ({1}, límite {2}).");
    LOCALES.put(engine + "RUNS_MAINTAIN", "Maintain {0}");
    LOCALES.put(engine + "RUNS_COMMAND", "Comando {0}");
    LOCALES.put(engine + "NO_CALENDAR", "Sin calendario");
    LOCALES.put(engine + "CALENDAR_EXCLUDES", "excluye {0}");
    LOCALES.put(engine + "TASK_UNKNOWN", "Tarea {0}");
    LOCALES.put(engine + "FACT_INSTANCE", "Instancia");
    LOCALES.put(engine + "FACT_VERSION", "Versión");
    LOCALES.put(engine + "FACT_STORE", "Almacén de trabajos");
    LOCALES.put(engine + "FACT_THREADS", "Hilos en uso");
    LOCALES.put(engine + "FACT_SINCE", "En marcha desde");
    LOCALES.put(engine + "FACT_STATUS", "Estado");
    LOCALES.put(engine + "TILE_PAUSED", "En pausa");
    LOCALES.put(engine + "STATUS_STOPPED", "Detenido");
    LOCALES.put(engine + "STARTED_ON", "arrancó el {0}");
    LOCALES.put(engine + "FACT_EXECUTED", "Trabajos ejecutados");
    LOCALES.put(engine + "EXECUTED_SINCE_START", "desde el arranque");
    LOCALES.put(engine + "SCHEDULES_TITLE", "Programadas en el motor · {0}");
    LOCALES.put("SCHEDULER_INVALID_FILTER_VALUE_MESSAGE", "El valor del filtro {0} no es válido");
    LOCALES.put("SCREEN_TEXT_TASK_CATALOG_AND", "{0} y {1}");
    LOCALES.put("SCREEN_TEXT_TASK_CATALOG_TODAY", "Hoy {0}");
    LOCALES.put("SCREEN_TEXT_TASK_CATALOG_YESTERDAY", "Ayer {0}");
    LOCALES.put("SCREEN_TEXT_TASK_WHEN_WORKDAYS", "Laborables a las {0}");
    LOCALES.put("SCREEN_TEXT_ACTIVITY_DURATION_DAYS", "{0} d {1} h");
    LOCALES.put("SCREEN_TEXT_ACTIVITY_DURATION_HOURS", "{0} h {1} min");
    LOCALES.put("SCREEN_TEXT_ACTIVITY_DURATION_MINUTES", "{0} min");
  }

  @Mock
  private QueryService queryService;
  @Mock
  private QueryUtil queryUtil;
  @Mock
  private ServiceConfig serviceConfig;

  private final List<DeferredExecution> deferred = new ArrayList<>();
  private SchedulerEnginePresenter presenter;

  @BeforeEach
  void setUp() throws Exception {
    presenter = new SchedulerEnginePresenter(queryService, queryUtil, serviceConfig,
      Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()), () -> deferred);
    given(queryUtil.getParameters(any(), anyString(), anyString()))
      .willAnswer(invocation -> JsonNodeFactory.instance.objectNode());
    given(serviceConfig.getLocale(anyString()))
      .willAnswer(invocation -> LOCALES.getOrDefault(invocation.getArgument(0), invocation.getArgument(0)));
    given(queryService.launchPrivateQuery(eq("getConfigurationEngineTasks"), any(ObjectNode.class)))
      .willReturn(data(List.of(
        task(10, "Cierre contable diario", 1, "DailyAccountingClose"),
        task(20, "Importar extractos bancarios", 0, "import.sh"),
        task(30, "Envío de remesas SEPA", 1, "SendSepa"))));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarRows"), any(ObjectNode.class)))
      .willReturn(data(List.of(row("IdeCal", 1, "Nom", "Festivos ES 2026"))));
    given(queryService.launchPrivateQuery(eq("getConfigurationCalendarDates"), any(ObjectNode.class)))
      .willReturn(data(List.of(calendarDate(1, LocalDate.of(2026, 11, 1)), calendarDate(1, LocalDate.of(2026, 12, 8)),
        calendarDate(1, LocalDate.of(2026, 12, 25)))));
  }

  @Test
  void aRunningEngineTellsHowLongItRunsItsSchedulesAndItsThreadsWithItsData() throws Exception {
    Map<String, CellData> card = card(running(0), List.of(scheduled(10, "NORMAL"), file(20, "NORMAL")));

    assertThat(text(card, "EngSta")).isEqualTo("running");
    assertThat(text(card, "EngTxt")).isEqualTo("El motor quartz-tesoreria-01 está en marcha desde hace 6 d 4 h y lanza "
      + "las tareas a su hora. Tiene 2 programaciones y ahora mismo no ocupa ningún hilo (0 de 10).");
    assertThat(text(card, "EngIns")).isEqualTo("quartz-tesoreria-01");
    assertThat(text(card, "EngInsTxt")).isEqualTo("En este servidor (no remoto)");
    assertThat(text(card, "EngVer")).isEqualTo("Quartz 2.5.2");
    assertThat(text(card, "EngStoTxt")).isEqualTo("En memoria");
    assertThat(text(card, "EngStoDet")).isEqualTo("Sin clúster · al arrancar carga las tareas activas de la base de datos");
    assertThat(text(card, "EngThrTxt")).isEqualTo("0 de 10");
    assertThat(text(card, "EngThrPct")).isEqualTo("0");
    assertThat(text(card, "EngSin")).isEqualTo("11/11/2026 06:45");
    assertThat(text(card, "EngSinAgo")).isEqualTo("hace 6 d 4 h");
    assertThat(text(card, "EngExe")).isEqualTo("1284");
    assertThat(text(card, "EngSch")).isEqualTo("2");
    assertThat(text(card, "EngErr")).isEmpty();
    assertThat(text(card, "EngTec")).isEqualTo("Almacén org.quartz.simpl.RAMJobStore · hilos "
      + "org.quartz.simpl.SimpleThreadPool · persistente: no · clúster: no · remoto: no · al arrancar carga las tareas "
      + "activas de la base de datos");
  }

  @Test
  void theTimeTileOfTheCardFollowsTheStatusOfTheEngine() throws Exception {
    // Quartz does not tell since when the engine is paused: a paused engine tells its pause and its last start under
    // it, never «En marcha desde»
    JsonNode running = new JsonMapper().readTree(text(card(running(0), List.of(scheduled(10, "NORMAL"))), "EngTil"));
    Map<String, CellData> pausedCard = card(engine(EngineStatus.PAUSED, 0, false), List.of(scheduled(10, "PAUSED")));
    JsonNode paused = new JsonMapper().readTree(text(pausedCard, "EngTil"));
    JsonNode emptied = new JsonMapper().readTree(text(card(engine(EngineStatus.PAUSED, 0, false), List.of()), "EngTil"));
    JsonNode stopped = new JsonMapper().readTree(text(card(engine(EngineStatus.STOPPED, 0, false), List.of()), "EngTil"));

    assertThat(List.of(running.get(4).get("label").asString(), running.get(4).get("value").asString(),
      running.get(4).get("hint").asString())).containsExactly("En marcha desde", "11/11/2026 06:45", "hace 6 d 4 h");
    for (JsonNode tiles : List.of(paused, emptied)) {
      assertThat(List.of(tiles.get(4).get("label").asString(), tiles.get(4).get("value").asString(),
        tiles.get(4).get("hint").asString())).containsExactly("Estado", "En pausa", "arrancó el 11/11/2026 06:45");
    }
    assertThat(List.of(stopped.get(4).get("label").asString(), stopped.get(4).get("value").asString()))
      .containsExactly("Estado", "Detenido");
    assertThat(stopped.get(4).has("hint")).isFalse();
    // The chip, the sentence and the tab (EngStaTxt) of the paused engine follow its status too
    assertThat(text(pausedCard, "EngStaTxt")).isEqualTo("SCREEN_TEXT_SCHEDULER_ENGINE_STATUS_PAUSED");
    assertThat(text(pausedCard, "EngTxt")).isEqualTo("El motor quartz-tesoreria-01 está pausado: conserva 1 programación.");
  }

  @Test
  void theThreadsInUseAreToldInOneOrMany() throws Exception {
    assertThat(text(card(running(1), List.of(scheduled(10, "NORMAL"))), "EngTxt"))
      .endsWith("Tiene 1 programación y ahora mismo ocupa 1 hilo (1 de 10).");
    assertThat(text(card(running(4), List.of()), "EngTxt"))
      .endsWith("Tiene 0 programaciones y ahora mismo ocupa 4 hilos (4 de 10).");
  }

  @Test
  void aPausedAStoppedAndAnUnreadableEngineAreToldAsSuchWithTheLastStartError() throws Exception {
    Map<String, CellData> paused = card(engine(EngineStatus.PAUSED, 0, false), List.of(scheduled(10, "PAUSED")));
    Map<String, CellData> emptied = card(engine(EngineStatus.PAUSED, 0, false), List.of());
    Map<String, CellData> stopped = card(engine(EngineStatus.STOPPED, 0, true), List.of());
    Map<String, CellData> unreadable = presenter.getEngine(null, List.of()).getDataList().getRows().get(0);

    assertThat(List.of(text(paused, "EngSta"), text(emptied, "EngSta"), text(stopped, "EngSta"),
      text(unreadable, "EngSta"))).containsExactly("paused", "paused-empty", "stopped", "unavailable");
    assertThat(text(paused, "EngTxt")).isEqualTo("El motor quartz-tesoreria-01 está pausado: conserva 1 programación.");
    assertThat(text(emptied, "EngTxt")).isEqualTo("El motor quartz-tesoreria-01 está pausado y no tiene nada programado.");
    assertThat(text(stopped, "EngTxt")).isEqualTo("El motor quartz-tesoreria-01 está detenido.");
    assertThat(text(stopped, "EngErr")).isEqualTo("SCREEN_TEXT_SCHEDULER_ENGINE_START_ERROR");
    assertThat(text(stopped, "EngSinAgo")).isEmpty();
    assertThat(text(paused, "EngErr")).isEmpty();
    assertThat(text(unreadable, "EngTxt")).isEqualTo("No se puede leer el estado del motor.");
  }

  @Test
  void theExecutionThatWaitsForAConfirmationIsToldWithTheNearestDeadline() throws Exception {
    deferred.add(deferredExecution(20, NOW.withHour(14).withMinute(0)));
    deferred.add(deferredExecution(30, NOW.withHour(12).withMinute(15)));

    Map<String, CellData> card = card(running(0), List.of());

    assertThat(text(card, "EngDfrCnt")).isEqualTo("2");
    assertThat(text(card, "EngDfrTxt")).isEqualTo("Ahora hay 2 esperando confirmación (Envío de remesas SEPA, "
      + "límite 12:15).");
    deferred.clear();
    assertThat(text(card(running(0), List.of()), "EngDfrTxt")).isEmpty();
  }

  @Test
  void eachScheduleOfTheEngineTellsItsTaskWhenItsCalendarItsFireTimesAndItsState() throws Exception {
    List<EngineTrigger> triggers = List.of(
      new EngineTrigger(10, "SCHEDULED_TASK", "1", date(NOW.minusDays(1).withHour(22).withMinute(30)),
        date(NOW.withHour(22).withMinute(30)), "NORMAL"),
      new EngineTrigger(20, "FILE_TRACKING_TASK", null, date(NOW), date(NOW.plusMinutes(5)), "PAUSED"),
      new EngineTrigger(99, "SCHEDULED_TASK", null, null, null, "ERROR"));

    List<Map<String, CellData>> rows = presenter.getTriggers(triggers, null).getDataList().getRows();

    assertThat(column(rows, "IdeTsk")).containsExactly("20", "10", "99");
    assertThat(column(rows, "TskNam")).containsExactly("Importar extractos bancarios", "Cierre contable diario",
      "Tarea 99");
    assertThat(column(rows, "TskRun")).containsExactly("Comando import.sh", "Maintain DailyAccountingClose", "");
    assertThat(column(rows, "TrgKnd")).containsExactly("file", "scheduled", "scheduled");
    assertThat(column(rows, "TrgKndTxt")).containsExactly("SCREEN_TEXT_SCHEDULER_ENGINE_KIND_FILE",
      "SCREEN_TEXT_SCHEDULER_ENGINE_KIND_SCHEDULED", "SCREEN_TEXT_SCHEDULER_ENGINE_KIND_SCHEDULED");
    assertThat(column(rows, "TrgWhn").get(1)).isEqualTo("Laborables a las 22:30");
    assertThat(column(rows, "CalNam")).containsExactly("Sin calendario", "Festivos ES 2026", "Sin calendario");
    assertThat(column(rows, "CalExc")).containsExactly("", "excluye 08/12 y 25/12", "");
    assertThat(column(rows, "PrvFir")).containsExactly("Hoy 10:45", "Ayer 22:30", "—");
    assertThat(column(rows, "NxtFir")).containsExactly("Hoy 10:50", "Hoy 22:30", "—");
    assertThat(column(rows, "NxtFirIn")).containsExactly("dentro de 5 min", "dentro de 11 h 45 min", "");
    assertThat(column(rows, "TrgSta")).containsExactly("paused", "normal", "error");
  }

  @Test
  void theSchedulesAreFilteredByTheirStateWithTheCountOfEachFilter() throws Exception {
    List<EngineTrigger> triggers = List.of(scheduled(10, "NORMAL"), file(20, "PAUSED"), scheduled(30, "ERROR"),
      scheduled(40, "BLOCKED"));

    assertThat(column(presenter.getTriggers(triggers, "paused").getDataList().getRows(), "IdeTsk"))
      .containsExactly("20");
    assertThat(column(presenter.getTriggers(triggers, "error").getDataList().getRows(), "IdeTsk"))
      .containsExactly("30");
    List<Map<String, CellData>> counts = presenter.getTriggerCounts(triggers).getDataList().getRows();
    assertThat(column(counts, "value")).containsExactly("", "paused", "error");
    assertThat(column(counts, "count")).containsExactly("4", "1", "1");
  }

  @Test
  void aFilterThatIsNotValidIsRefusedBeforeAnythingIsRead() throws Exception {
    assertThrows(InvalidExecutionFilterException.class, () -> presenter.validateFilter("blocked"));

    verify(queryService, never()).launchPrivateQuery(anyString(), any(ObjectNode.class));
  }

  @Test
  void theRefusalOfAFilterNamesTheFilterButNeverEchoesItsValue() {
    InvalidExecutionFilterException refusal = assertThrows(InvalidExecutionFilterException.class,
      () -> presenter.validateFilter("<img src=x onerror=alert(1)>"));

    assertThat(refusal.getMessage()).isEqualTo("El valor del filtro state no es válido");
  }

  @Test
  void theFactsOfTheCardAreItsTilesWithTheirHints() throws Exception {
    Map<String, CellData> card = card(running(3), List.of(scheduled(10, "NORMAL")));

    JsonNode tiles = new JsonMapper().readTree(text(card, "EngTil"));
    assertThat(tiles).hasSize(6);
    assertThat(tiles.findValuesAsString("label")).containsExactly("Instancia", "Versión", "Almacén de trabajos",
      "Hilos en uso", "En marcha desde", "Trabajos ejecutados");
    assertThat(tiles.findValuesAsString("value")).containsExactly("quartz-tesoreria-01", "Quartz 2.5.2", "En memoria",
      "3 de 10", "11/11/2026 06:45", "1284");
    assertThat(tiles.get(0).get("hint").asString()).isEqualTo("En este servidor (no remoto)");
    // The long description of the job store lives in the technical details: the tiles stay short
    assertThat(tiles.get(2).has("hint")).isFalse();
    assertThat(tiles.get(4).get("hint").asString()).isEqualTo("hace 6 d 4 h");
    assertThat(tiles.get(5).get("hint").asString()).isEqualTo("desde el arranque");
    assertThat(tiles.get(1).has("hint")).isFalse();
    assertThat(text(card, "EngSchTit")).isEqualTo("Programadas en el motor · 1");
  }

  @Test
  void anEngineThatCannotBeReadHasNoFacts() throws Exception {
    Map<String, CellData> unreadable = presenter.getEngine(null, List.of()).getDataList().getRows().get(0);

    assertThat(text(unreadable, "EngTil")).isEqualTo("[]");
    assertThat(text(unreadable, "EngSchTit")).isEqualTo("Programadas en el motor · 0");
  }

  private Map<String, CellData> card(Engine engine, List<EngineTrigger> triggers) throws Exception {
    return presenter.getEngine(engine, triggers).getDataList().getRows().get(0);
  }

  private static Engine running(int threadsInUse) {
    return engine(EngineStatus.RUNNING, threadsInUse, false);
  }

  private static Engine engine(EngineStatus status, int threadsInUse, boolean lastStartFailed) {
    return new Engine("quartz-tesoreria-01", "2.5.2", "org.quartz.simpl.RAMJobStore", false, false,
      "org.quartz.simpl.SimpleThreadPool", 10, threadsInUse,
      status == EngineStatus.STOPPED ? null : date(NOW.minusDays(6).minusHours(4)), 1284, status, lastStartFailed,
      false);
  }

  private static EngineTrigger scheduled(int taskId, String state) {
    return new EngineTrigger(taskId, "SCHEDULED_TASK", null, null, date(NOW.plusHours(taskId)), state);
  }

  private static EngineTrigger file(int taskId, String state) {
    return new EngineTrigger(taskId, "FILE_TRACKING_TASK", null, null, date(NOW.plusHours(taskId)), state);
  }

  private static DeferredExecution deferredExecution(int taskId, LocalDateTime due) {
    DeferredExecution execution = new DeferredExecution();
    execution.setTaskId(taskId);
    execution.setExecutionId(1);
    execution.setDueDate(date(due));
    return execution;
  }

  private static Date date(LocalDateTime dateTime) {
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private static Map<String, CellData> task(int id, String name, int executionType, String command) {
    Map<String, CellData> row = row("IdeTsk", id, "Nam", name);
    row.put("TypExe", new CellData(executionType));
    row.put("CmdExe", new CellData(command));
    row.put("TypLch", new CellData(id == 20 ? 2 : 1));
    row.put("RptTyp", new CellData(7));
    row.put("WeekDays", new CellData("1,2,3,4,5"));
    row.put("ExeHrs", new CellData("22"));
    row.put("ExeMin", new CellData("30"));
    return row;
  }

  private static Map<String, CellData> calendarDate(int calendarId, LocalDate date) {
    Map<String, CellData> row = new HashMap<>();
    row.put("IdeCal", new CellData(calendarId));
    row.put("Dat", new CellData(java.sql.Date.valueOf(date)));
    return row;
  }

  private static Map<String, CellData> row(String key, Object value, String otherKey, Object otherValue) {
    Map<String, CellData> row = new HashMap<>();
    row.put(key, new CellData(value));
    row.put(otherKey, new CellData(otherValue));
    return row;
  }

  private static String text(Map<String, CellData> row, String column) {
    return row.get(column).getStringValue();
  }

  private static List<String> column(List<Map<String, CellData>> rows, String column) {
    return rows.stream().map(row -> row.get(column).getStringValue()).toList();
  }

  private static ServiceData data(List<Map<String, CellData>> rows) {
    DataList dataList = new DataList();
    rows.forEach(dataList::addRow);
    return new ServiceData().setDataList(dataList);
  }
}
