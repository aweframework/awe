package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.rest.service.JWTTokenService;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.service.DeferredExecutionWatcher;
import com.almis.awe.service.DummyService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tasks whose maintain answers deferred (accepted, finishes later) on a real database: the launch that keeps the
 * execution running with its external state, the completion API, the deadline, the manual stop, the restart skip, the
 * task list and the security of the scheduler API. It proves the wiring and the SQL of the deferred state, which unit
 * tests cannot. The periodic watcher is not waited for: its pass is called directly and its due dates are moved in the
 * database. It shares the application context of the other scheduler tests.
 */
@Tag("integration")
class SchedulerDeferredExecutionTest extends AbstractSchedulerMaintainTest {

  private static final int WARNING = 2;
  private static final int STOPPED = 3;
  private static final int RUNNING = 4;
  private static final int INTERRUPTED = 6;
  private static final long WAIT_MILLIS = 20_000;

  @Autowired
  private DataSource dataSource;
  @Autowired
  private Scheduler scheduler;
  @Autowired
  private TaskDAO taskDAO;
  @Autowired
  private DeferredExecutionWatcher watcher;
  @Autowired
  private DummyService dummyService;
  @Autowired
  private JWTTokenService jwtTokenService;

  @Test
  void aDeferredAnswerKeepsTheExecutionRunningWithItsExternalStateAndLaunchesNoDependent() throws Exception {
    int taskId = create(deferredTask(unique("Deferred launch")));
    int dependentId = create(dependentOf(taskId));
    try {
      launch(taskId);

      Map<String, Object> execution = execution(taskId);
      assertThat(execution).containsEntry("STA", RUNNING);
      assertThat(execution).containsEntry("DES", "Waiting for the external system");
      Map<String, Object> state = single("SELECT ExtRef, DfrDat, DueDat FROM AweSchExeExt WHERE IdeTsk = ?", taskId);
      assertThat(state).containsEntry("EXTREF", "external-ref-1");
      assertThat(state.get("DUEDAT")).isNotNull();
      assertThat(received(taskId)).containsExactly(new int[]{taskId, 1});
      assertThat(count("SELECT COUNT(*) FROM AweSchExe WHERE IdeTsk = ?", dependentId)).isZero();
      // The task list shows the waiting task as running
      assertThat(taskListRow(taskId).orElseThrow().get("Sta").asString()).isEqualTo(String.valueOf(RUNNING));
    } finally {
      stopAndDelete(taskId);
      deleteTasks(dependentId);
    }
  }

  @Test
  void theCompletionApiClosesTheExecutionOnceAndLaunchesTheDependentTask() throws Exception {
    int taskId = create(deferredTask(unique("Deferred complete")));
    int dependentId = create(dependentOf(taskId));
    int plainId = create(manualTask(unique("Deferred plain")));
    try {
      launch(taskId);
      new JdbcTemplate(dataSource).update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, Sta, LchBy) "
        + "VALUES (?, 'sample', 1, CURRENT_TIMESTAMP, 4, 'test')", plainId);

      complete(taskId, 1, "{\"status\":\"WARNING\",\"description\":\"Partial result\"}", true)
        .andExpect(status().isOk());

      assertThat(execution(taskId)).containsEntry("STA", WARNING);
      assertThat(execution(taskId)).containsEntry("DES", "Partial result");
      Map<String, Object> trace = single("SELECT CmpSrc, CmpDat, CncSta FROM AweSchExeExt WHERE IdeTsk = ?", taskId);
      assertThat(trace).containsEntry("CMPSRC", "API").containsEntry("CNCSTA", null);
      assertThat(trace.get("CMPDAT")).isNotNull();
      await(() -> count("SELECT COUNT(*) FROM AweSchExe WHERE IdeTsk = ?", dependentId) == 1,
        "the dependent task to be launched");

      String again = complete(taskId, 1, "{\"status\":\"OK\"}", true).andExpect(status().isConflict())
        .andReturn().getResponse().getContentAsString();
      assertThat(objectMapper.readTree(again).get("type").asString()).isEqualTo("execution-not-running");
      assertThat(execution(taskId)).containsEntry("STA", WARNING);

      String notDeferred = complete(plainId, 1, "{\"status\":\"OK\"}", true).andExpect(status().isConflict())
        .andReturn().getResponse().getContentAsString();
      assertThat(objectMapper.readTree(notDeferred).get("type").asString()).isEqualTo("execution-not-deferred");
      complete(taskId, 99, "{\"status\":\"OK\"}", true).andExpect(status().isNotFound());
      complete(taskId, 1, "{\"status\":\"DONE\"}", true).andExpect(status().isBadRequest());
      complete(taskId, 1, "{\"status\":\"OK\"}", false).andExpect(status().isUnauthorized());

      await(() -> executionStatus(dependentId) != RUNNING, "the dependent task to end");
    } finally {
      deleteTasks(dependentId);
      deleteTasks(taskId);
      deleteTasks(plainId);
      forget(dependentId, taskId, plainId);
    }
  }

  @Test
  void anExecutionPastItsDueDateIsInterruptedByTheWatcherAndCannotBeCompletedLater() throws Exception {
    int overdueId = create(deferredTask(unique("Deferred overdue")));
    int onTimeId = create(deferredTask(unique("Deferred on time")));
    try {
      launch(overdueId);
      launch(onTimeId);
      new JdbcTemplate(dataSource).update(
        "UPDATE AweSchExeExt SET DueDat = TIMESTAMP '2000-01-01 00:00:00' WHERE IdeTsk = ?", overdueId);

      watcher.sweep();

      assertThat(executionStatus(overdueId)).isEqualTo(INTERRUPTED);
      assertThat(executionStatus(onTimeId)).isEqualTo(RUNNING);
      complete(overdueId, 1, "{\"status\":\"OK\"}", true).andExpect(status().isConflict());
    } finally {
      stopAndDelete(onTimeId);
      deleteTasks(overdueId);
      forget(overdueId);
    }
  }

  @Test
  void stoppingATaskClosesItsDeferredExecutionAsStoppedAndNotifiesTheCancellationOnce() throws Exception {
    int taskId = create(deferredTask(unique("Deferred stop")).put("CncMnt", "deferredTestCancel"));
    try {
      launch(taskId);
      // The activity counter of the executions waiting for their confirmation
      assertThat(counter("getActivityWaitingCount")).isPositive();

      maintain("StpTsk", params().put("IdeTsk.selected", taskId));
      maintain("StpTsk", params().put("IdeTsk.selected", taskId));

      assertThat(executionStatus(taskId)).isEqualTo(STOPPED);
      assertThat(dummyService.getDeferredCancellations()).containsOnlyOnce(taskId + "-1-external-ref-1");
      Map<String, Object> trace = single("SELECT CmpSrc, CncSta, CncErr FROM AweSchExeExt WHERE IdeTsk = ?", taskId);
      assertThat(trace).containsEntry("CMPSRC", "USER").containsEntry("CNCSTA", "SENT");
      assertThat((String) trace.get("CNCERR")).isNullOrEmpty();
    } finally {
      deleteTasks(taskId);
      forget(taskId);
    }
  }

  @Test
  void theStartupInterruptionSkipsTheDeferredExecutionsAndInterruptsThePlainOnes() throws Exception {
    int deferredId = create(deferredTask(unique("Deferred restart")));
    int plainId = create(manualTask(unique("Plain restart")));
    try {
      launch(deferredId);
      new JdbcTemplate(dataSource).update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, Sta, LchBy) "
        + "VALUES (?, 'sample', 1, CURRENT_TIMESTAMP, 4, 'test')", plainId);

      taskDAO.updateInterruptedTasks();

      assertThat(executionStatus(plainId)).isEqualTo(INTERRUPTED);
      assertThat(executionStatus(deferredId)).isEqualTo(RUNNING);
    } finally {
      stopAndDelete(deferredId);
      deleteTasks(plainId);
      forget(plainId);
    }
  }

  /**
   * Parameters of a manual task whose maintain answers deferred. It has no task parameters: the execution key is reserved
   */
  private ObjectNode deferredTask(String name) {
    ObjectNode parameters = manualTask(name).put("CmdExe", "deferredTestLaunch");
    parameters.set("ParNam", strings());
    parameters.set("ParSrc", ints());
    parameters.set("ParTyp", strings());
    parameters.set("ParVal", strings());
    return parameters;
  }

  /**
   * Parameters of a manual task that runs after a parent (TskPrn)
   */
  private ObjectNode dependentOf(int parentId) {
    ObjectNode parameters = manualTask(unique("Deferred dependent"));
    parameters.set("TskPrn", ints(parentId));
    return parameters;
  }

  /**
   * Launch a task from the task list and wait until no scheduler thread holds the execution
   */
  private void launch(int taskId) throws Exception {
    maintain("LchTsk", params().put("IdeTsk.selected", taskId));
    await(() -> count("SELECT COUNT(*) FROM AweSchExe WHERE IdeTsk = ?", taskId) == 1 && jobsIdle(),
      "the job to end");
  }

  /**
   * Create a task. The identifiers of the deleted tasks are reused, so the executions they left are removed: the
   * execution of the test is always the first
   */
  private int create(ObjectNode parameters) throws Exception {
    int taskId = createTask(parameters);
    forget(taskId);
    return taskId;
  }

  private void forget(int... taskIds) {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    for (int taskId : taskIds) {
      jdbc.update("DELETE FROM AweSchExeExt WHERE IdeTsk = ?", taskId);
      jdbc.update("DELETE FROM AweSchExe WHERE IdeTsk = ?", taskId);
    }
  }

  private void stopAndDelete(int taskId) throws Exception {
    try {
      maintain("StpTsk", params().put("IdeTsk.selected", taskId));
    } finally {
      deleteTasks(taskId);
      forget(taskId);
    }
  }

  private int counter(String query) throws Exception {
    return rows(query, params()).get(0).get("value").asInt();
  }

  private boolean jobsIdle() {
    try {
      return scheduler.getCurrentlyExecutingJobs().isEmpty();
    } catch (SchedulerException exc) {
      throw new IllegalStateException(exc);
    }
  }

  /**
   * Execution keys (task, execution) that the maintain received for a task
   */
  private List<int[]> received(int taskId) {
    return dummyService.getDeferredLaunches().stream().filter(key -> key[0] == taskId).toList();
  }

  private ResultActions complete(int taskId, int executionId, String body, boolean withBearer) throws Exception {
    var request = post("/scheduler/api/v1/tasks/" + taskId + "/executions/" + executionId + "/complete")
      .with(anonymous()).contentType(MediaType.APPLICATION_JSON).content(body);
    return mockMvc.perform(withBearer ? request.header("Authorization", "Bearer " + bearer()) : request);
  }

  private String bearer() {
    return JWT.create()
      .withSubject("test")
      .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
      .withIssuer("AWE ISSUER")
      .sign(Algorithm.HMAC512(jwtTokenService.getSecret().getBytes()));
  }

  private int executionStatus(int taskId) {
    return (Integer) execution(taskId).get("STA");
  }

  private Map<String, Object> execution(int taskId) {
    return single("SELECT Sta, Des FROM AweSchExe WHERE IdeTsk = ?", taskId);
  }

  private Map<String, Object> single(String sql, Object... arguments) {
    List<Map<String, Object>> rows = new JdbcTemplate(dataSource).queryForList(sql, arguments);
    assertThat(rows).as(sql).hasSize(1);
    return rows.get(0);
  }

  private long count(String sql, Object... arguments) {
    Long count = new JdbcTemplate(dataSource).queryForObject(sql, Long.class, arguments);
    return count == null ? 0 : count;
  }

  private void await(BooleanSupplier condition, String description) {
    Awaitility.await("Waiting for " + description).atMost(Duration.ofMillis(WAIT_MILLIS))
      .pollInterval(Duration.ofMillis(50)).until(condition::getAsBoolean);
  }
}
