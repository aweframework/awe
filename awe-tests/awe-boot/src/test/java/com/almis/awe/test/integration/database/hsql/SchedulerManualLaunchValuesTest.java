package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.service.DummyService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A manual launch from the launch dialog on a real database: the values the operator types replace the stored ones (a
 * value, a variable) in the execution, are stored as the parameters of its run and shown by the activity side panel; a
 * property keeps its key. A value that is not of its type, or a changed property key, is rejected before anything is
 * launched: a valid launch made after it is the only one that runs. The launch is awaited on the database, never with
 * a sleep. It shares the application context of the other scheduler tests.
 */
@Tag("integration")
class SchedulerManualLaunchValuesTest extends AbstractSchedulerMaintainTest {

  private static final String STORED_KEY = "awe.test.launch.entity.stored";
  private static final String OTHER_KEY = "awe.test.launch.entity.other";
  private static final long WAIT_MILLIS = 20_000;

  @Autowired
  private DataSource dataSource;
  @Autowired
  private DummyService dummyService;

  private int taskId;

  @BeforeEach
  void createTheTask() throws Exception {
    System.setProperty(STORED_KEY, "stored-entity");
    System.setProperty(OTHER_KEY, "0182");
    // A manual task with a value, a property and a typed value; its maintain records the day and the entity it receives
    ObjectNode parameters = manualTask(unique("Manual values")).put("CmdExe", "webhookTestLaunch");
    parameters.set("ParNam", strings("day", "entity", "count"));
    parameters.set("ParSrc", ints(0, 2, 0));
    parameters.set("ParTyp", strings("STRING", "STRING", "INTEGER"));
    parameters.set("ParVal", strings("none", STORED_KEY, "1"));
    taskId = createTask(parameters);
    jdbc().update("DELETE FROM AweSchExe WHERE IdeTsk = ?", taskId);
    dummyService.getWebhookLaunches().clear();
  }

  @AfterEach
  void deleteTheTask() throws Exception {
    System.clearProperty(STORED_KEY);
    System.clearProperty(OTHER_KEY);
    deleteTasks(taskId);
    // The identifiers of the deleted tasks are reused: remove what the test leaves for the next task with the same one
    jdbc().update("DELETE FROM AweSchExe WHERE IdeTsk = ?", taskId);
    dummyService.getWebhookLaunches().clear();
  }

  @Test
  void theValuesTypedForAValueReachTheExecutionWithoutBlanksAndThePropertyKeepsItsKey() throws Exception {
    ObjectNode values = dialog("2026-11-19", STORED_KEY, " 7 ");

    maintain("VldTskPar", values);
    maintain("LchTskVar", values);

    await(() -> !dummyService.getWebhookLaunches().isEmpty(), "the task to receive its values");
    // The value replaces the stored one and the property key is resolved at execution time
    assertThat(dummyService.getWebhookLaunches()).containsExactly(Map.of("day", "2026-11-19", "entity", "stored-entity"));
    String runId = jdbc().queryForObject("SELECT IdeRun FROM AweSchExe WHERE IdeTsk = ?", String.class, taskId);
    List<String> shown = new ArrayList<>();
    for (JsonNode row : rows("getActivityExecutionParameters", params().put("ExeRun.selected", runId))) {
      shown.add(row.get("ParNam").asString() + "=" + row.get("ParVal").asString());
    }
    assertThat(shown).containsExactlyInAnyOrder("day=2026-11-19", "entity=" + STORED_KEY, "count=7");
  }

  @Test
  void aValueThatIsNotOfItsTypeIsRejectedAndNothingIsLaunched() throws Exception {
    assertRejectedAndNothingLaunched(dialog("2026-11-19", STORED_KEY, "seven"));
  }

  @Test
  void aChangedPropertyKeyIsRejectedAndNothingIsLaunched() throws Exception {
    assertRejectedAndNothingLaunched(dialog("2026-11-19", OTHER_KEY, "7"));
  }

  /**
   * The check and the launch of the dialog answer an error for the values, and nothing runs with them: a valid launch
   * made afterwards is awaited and is the only execution of the task (no fixed wait for something that never comes)
   */
  private void assertRejectedAndNothingLaunched(ObjectNode values) throws Exception {
    assertThat(messageTypes(perform("maintain", "VldTskPar", values))).containsExactly("error");
    assertThat(messageTypes(perform("maintain", "LchTskVar", values))).containsExactly("error");

    maintain("LchTskVar", dialog("2026-12-01", STORED_KEY, "1"));
    await(() -> !dummyService.getWebhookLaunches().isEmpty(), "the valid launch");
    assertThat(dummyService.getWebhookLaunches()).containsExactly(Map.of("day", "2026-12-01", "entity", "stored-entity"));
    assertThat(jdbc().queryForObject("SELECT COUNT(*) FROM AweSchExe WHERE IdeTsk = ?", Long.class, taskId)).isOne();
  }

  /**
   * What the launch dialog sends: the task and the name and value columns of its grid
   */
  private ObjectNode dialog(String day, String entity, String count) {
    ObjectNode values = params().put("IdeTsk.selected", taskId);
    values.set("name", strings("day", "entity", "count"));
    values.set("value", strings(day, entity, count));
    return values;
  }

  private List<String> messageTypes(JsonNode actions) {
    List<String> types = new ArrayList<>();
    for (JsonNode action : actions) {
      if ("message".equals(action.get("type").asString())) {
        types.add(action.get("parameters").get("type").asString());
      }
    }
    return types;
  }

  private JdbcTemplate jdbc() {
    return new JdbcTemplate(dataSource);
  }

  private void await(BooleanSupplier condition, String description) {
    Awaitility.await("Waiting for " + description).atMost(Duration.ofMillis(WAIT_MILLIS))
      .pollInterval(Duration.ofMillis(50)).until(condition::getAsBoolean);
  }
}
