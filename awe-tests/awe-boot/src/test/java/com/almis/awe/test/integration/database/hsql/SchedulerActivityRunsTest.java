package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SQL of the executions of the activity screen grouped by flow run (#993, phase 2b) on a real database, through the
 * action controller as the screen launches it: the top-level rows (flow runs with a matching execution and matching
 * executions without run) are paged and sorted in the database, newest first, and each run brings every execution
 * of the run and the tasks it skipped as its children. Three tasks of their own, far from the ids of the task sequence,
 * are stored with their executions and runs and removed after the test; the task name filter leaves the rows of other
 * tests out.
 */
@Tag("integration")
class SchedulerActivityRunsTest extends AbstractSchedulerMaintainTest {

  private static final int ROOT = 9111;
  private static final int CHILD = 9112;
  private static final int SKIPPED = 9113;
  private static final int OK = 0;
  private static final int ERROR = 1;
  private static final String QUERY = "getActivityRunList";
  private static final String NAME_FILTER = "runs probe";
  private static final String OLD_RUN = "run-runs-probe-old";
  private static final String NEW_RUN = "run-runs-probe-new";

  @Autowired
  private DataSource dataSource;

  private JdbcTemplate jdbc;

  @BeforeEach
  void storeRuns() {
    jdbc = new JdbcTemplate(dataSource);
    deleteRuns();
    task(ROOT, "Runs Probe Root");
    task(CHILD, "Runs Probe Child");
    task(SKIPPED, "Runs Probe Report");
    // Old run: the root OK, the child with error, the report skipped
    run(OLD_RUN, 1, LocalDateTime.of(2019, 3, 1, 10, 0), ERROR);
    execution(ROOT, 1, "FILE_TRACKING_TASK", OK, LocalDateTime.of(2019, 3, 1, 10, 0), OLD_RUN);
    execution(CHILD, 1, "DEPENDANT_TASK", ERROR, LocalDateTime.of(2019, 3, 1, 10, 1), OLD_RUN);
    jdbc.update("INSERT INTO AweSchRunTsk (IdeRun, IdeTsk, Sta, ExeTsk, UpdDat) VALUES (?, ?, 'SKIPPED', NULL, ?)",
      OLD_RUN, SKIPPED, Timestamp.valueOf(LocalDateTime.of(2019, 3, 1, 10, 2)));
    // Execution without run between both runs
    execution(CHILD, 2, "MANUAL_TASK", OK, LocalDateTime.of(2019, 3, 2, 10, 0), null);
    // New run: the root and the child OK
    run(NEW_RUN, 2, LocalDateTime.of(2019, 3, 3, 10, 0), OK);
    execution(ROOT, 2, "FILE_TRACKING_TASK", OK, LocalDateTime.of(2019, 3, 3, 10, 0), NEW_RUN);
    execution(CHILD, 3, "DEPENDANT_TASK", OK, LocalDateTime.of(2019, 3, 3, 10, 1), NEW_RUN);
  }

  @AfterEach
  void deleteRuns() {
    jdbc.update("DELETE FROM AweSchRunTsk WHERE IdeRun IN (?, ?)", OLD_RUN, NEW_RUN);
    jdbc.update("DELETE FROM AweSchRun WHERE IdeRun IN (?, ?)", OLD_RUN, NEW_RUN);
    jdbc.update("DELETE FROM AweSchExe WHERE IdeTsk IN (?, ?, ?)", ROOT, CHILD, SKIPPED);
    jdbc.update("DELETE FROM AweSchTsk WHERE Ide IN (?, ?, ?)", ROOT, CHILD, SKIPPED);
  }

  @Test
  void theRunsAndTheExecutionsWithoutRunAreTheTopLevelRowsAndARunMatchesByAnyOfItsExecutions() throws Exception {
    JsonNode page = dataList(params().put("CrtRunTsk", NAME_FILTER));
    List<JsonNode> rows = rows(page);

    // Newest first: the new run with its two executions, the execution without run, the old run with its two
    // executions and its skipped task
    assertThat(page.get("records").asInt()).isEqualTo(3);
    assertThat(column(rows, "id")).containsExactly(NEW_RUN, ROOT + "-2", CHILD + "-3", CHILD + "-2", OLD_RUN,
      ROOT + "-1", CHILD + "-1", OLD_RUN + "/" + SKIPPED);
    assertThat(column(rows, "RunPar")).containsExactly("", NEW_RUN, NEW_RUN, "", "", OLD_RUN, OLD_RUN, OLD_RUN);
    assertThat(column(rows, "RunLeaf")).containsExactly("0", "1", "1", "1", "0", "1", "1", "1");
    JsonNode oldRun = rows.get(4);
    assertThat(oldRun.get("TskNam").asString()).isEqualTo("Runs Probe Root #1");
    assertThat(oldRun.get("Sta").asString()).isEqualTo(String.valueOf(ERROR));
    assertThat(oldRun.get("RunExeCnt").asInt()).isEqualTo(2);
    assertThat(oldRun.get("ExeOri").asString()).isEqualTo("file");
    assertThat(oldRun.get("ExeRun").get("value").asString()).isEqualTo(OLD_RUN);
    // From the first start to the last end: 10:00 to 10:02
    assertThat(oldRun.get("ExeEla").asInt()).isEqualTo(120);
    assertThat(rows.get(7).get("TskNam").asString()).isEqualTo("Runs Probe Report");

    // An error filter keeps the run with an execution in error, with every execution of the run
    List<JsonNode> withError = rows(dataList(params().put("CrtRunTsk", NAME_FILTER).put("CrtRunQck", "error")));
    assertThat(column(withError, "id")).containsExactly(OLD_RUN, ROOT + "-1", CHILD + "-1", OLD_RUN + "/" + SKIPPED);
  }

  @Test
  void theTopLevelRowsAreSortedInTheDatabase() throws Exception {
    List<JsonNode> oldestFirst = rows(dataList(params().put("CrtRunTsk", NAME_FILTER)
      .set("sort", objectMapper.createArrayNode().add(objectMapper.createObjectNode().put("id", "ExeLstTim")
        .put("direction", "asc")))));

    assertThat(column(oldestFirst, "RunPar").stream().filter(String::isEmpty).count()).isEqualTo(3);
    assertThat(oldestFirst.stream().filter(row -> row.get("RunPar").asString().isEmpty())
      .map(row -> row.get("id").asString()).toList()).containsExactly(OLD_RUN, CHILD + "-2", NEW_RUN);
  }

  private void task(int id, String name) {
    jdbc.update("INSERT INTO AweSchTsk (Ide, Nam, TypExe, CmdExe, TypLch, Act) VALUES (?, ?, 0, 'none', 0, 0)", id, name);
  }

  private void run(String runId, int rootExecution, LocalDateTime start, int status) {
    jdbc.update("INSERT INTO AweSchRun (IdeRun, IdeTskRoo, ExeTskRoo, LchBy, IniDat, EndDat, Sta) "
      + "VALUES (?, ?, ?, 'test', ?, ?, ?)", runId, ROOT, rootExecution, Timestamp.valueOf(start),
      Timestamp.valueOf(start.plusMinutes(2)), status);
  }

  private void execution(int taskId, int executionId, String group, int status, LocalDateTime start, String runId) {
    jdbc.update("INSERT INTO AweSchExe (IdeTsk, GrpTsk, ExeTsk, IniDat, EndDat, ExeTim, Sta, LchBy, IdeRun) "
        + "VALUES (?, ?, ?, ?, ?, 60000, ?, 'test', ?)", taskId, group, executionId, Timestamp.valueOf(start),
      Timestamp.valueOf(start.plusMinutes(1)), status, runId);
  }

  private List<JsonNode> rows(JsonNode dataList) {
    List<JsonNode> rows = new ArrayList<>();
    dataList.get("rows").forEach(rows::add);
    return rows;
  }

  private JsonNode dataList(ObjectNode parameters) throws Exception {
    String response = mockMvc.perform(post("/action/data/" + QUERY)
        .with(csrf())
        .content(objectMapper.writeValueAsString(parameters))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();
    for (JsonNode action : objectMapper.readTree(response)) {
      if ("fill".equals(action.get("type").asString())) {
        return action.get("parameters").get("datalist");
      }
    }
    throw new AssertionError("No fill action: " + response);
  }
}
