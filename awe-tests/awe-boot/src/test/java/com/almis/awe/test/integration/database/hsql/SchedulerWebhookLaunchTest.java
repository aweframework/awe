package com.almis.awe.test.integration.database.hsql;

import com.almis.awe.scheduler.webhook.WebhookProcessor;
import com.almis.awe.service.DummyService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tasks launched by a signed webhook on a real database: the endpoint (anonymous, the signature is the authentication),
 * the inbox with its deduplication, the processor that launches the task with the mapped values, and the SQL of its
 * states. The processor sweep is called directly and the launch is awaited on the database, never with a sleep. It
 * shares the application context of the other scheduler tests.
 */
@Tag("integration")
class SchedulerWebhookLaunchTest extends AbstractSchedulerMaintainTest {

  private static final String SECRET_PROPERTY = "awe.test.webhook.secret";
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
  private static final long WAIT_MILLIS = 20_000;

  @Autowired
  private DataSource dataSource;
  @Autowired
  private WebhookProcessor processor;
  @Autowired
  private DummyService dummyService;

  private int taskId;
  private String hookKey;

  @BeforeEach
  void createTheWebhookTask() throws Exception {
    System.setProperty(SECRET_PROPERTY, SECRET);
    // A webhook task with a variable parameter that the webhook maps and one that it does not
    hookKey = "hook-" + System.nanoTime();
    taskId = createTask(webhookTask(unique("Webhook launch"), hookKey));
    jdbc().update("DELETE FROM AweSchExe WHERE IdeTsk = ?", taskId);
  }

  /**
   * Parameters of a webhook task as the task editor sends them: the hook fields and a pointer on one parameter
   */
  private ObjectNode webhookTask(String name, String key) {
    ObjectNode parameters = manualTask(name).put("CmdExe", "webhookTestLaunch").put("TypLch", 4)
      .put("HokKey", key).put("HokAct", 1).put("HokSec", SECRET_PROPERTY).put("HokTyp", "loaded");
    parameters.set("ParNam", strings("day", "entity"));
    parameters.set("ParSrc", ints(1, 1));
    parameters.set("ParTyp", strings("STRING", "STRING"));
    parameters.set("ParVal", strings("none", "stored"));
    parameters.set("ParPtr", strings("/day", ""));
    return parameters;
  }

  @AfterEach
  void deleteTheWebhookTask() throws Exception {
    System.clearProperty(SECRET_PROPERTY);
    deleteTasks(taskId);
    forget();
  }

  @Test
  void aSignedWebhookIsStoredOnceAndLaunchesTheTaskWithTheMappedValuesOnly() throws Exception {
    String body = envelope("loaded", "{\"day\":\"2026-11-12\",\"entity\":\"0049\"}");

    webhook(hookKey, "msg-1", body, true).andExpect(status().isAccepted()).andExpect(jsonPath("$.messageId")
      .value("msg-1"));
    webhook(hookKey, "msg-1", body, true).andExpect(status().isAccepted());
    assertThat(count("SELECT COUNT(*) FROM AweSchHokMsg WHERE Src = ?", hookKey)).isEqualTo(1);

    processor.sweep();

    await(() -> count("SELECT COUNT(*) FROM AweSchHokMsg WHERE Src = ? AND Sta = 'PROCESSED'", hookKey) == 1,
      "the message to be processed");
    await(() -> count("SELECT COUNT(*) FROM AweSchExe WHERE IdeTsk = ?", taskId) == 1, "the task to be launched");
    Map<String, Object> execution = single("SELECT GrpTsk, LchBy FROM AweSchExe WHERE IdeTsk = ?", taskId);
    assertThat(execution).containsEntry("LCHBY", "webhook:" + hookKey).containsEntry("GRPTSK", "WEBHOOK_TASK");
    await(() -> !launches().isEmpty(), "the task to receive its values");
    // The mapped parameter takes the value of the webhook; the other keeps its stored value
    assertThat(launches()).containsExactly(Map.of("day", "2026-11-12", "entity", "stored"));
    Map<String, Object> message = single("SELECT IdeTsk, PrcDat, Att FROM AweSchHokMsg WHERE Src = ?", hookKey);
    assertThat(message).containsEntry("IDETSK", taskId).containsEntry("ATT", 1);
    assertThat(message.get("PRCDAT")).isNotNull();
  }

  @Test
  void theTaskFormSavesUpdatesAndDropsTheHookAndLoadsItBack() throws Exception {
    // New: the hook row and the pointer are stored, and the task data loads them with the URL of the key
    assertThat(single("SELECT HokKey, Act, SecPrp, TypFlt FROM AweSchTskHok WHERE IdeTsk = ?", taskId))
      .containsEntry("HOKKEY", hookKey).containsEntry("ACT", 1).containsEntry("SECPRP", SECRET_PROPERTY)
      .containsEntry("TYPFLT", "loaded");
    assertThat(column(rows("taskParameters", params().put("IdeTsk.selected", taskId)), "ParPtr"))
      .containsExactly("/day", "");
    JsonNode task = taskData(taskId);
    assertThat(task.get("HokKey").asString()).isEqualTo(hookKey);
    assertThat(task.get("HokSec").asString()).isEqualTo(SECRET_PROPERTY);
    assertThat(rows("webhookUrl", params().put("HokKey", hookKey)).get(0).get("value").asString())
      .endsWith("/scheduler/api/v1/webhooks/triggers/" + hookKey);

    // Update: the same row changes, it is not duplicated, and keeping its own key is allowed
    maintain("UpdateSchedulerTaskEditor", webhookTask(unique("Webhook launch"), hookKey + "-b").put("Ide", taskId)
      .put("HokAct", 0).put("HokTyp", ""));
    assertThat(single("SELECT HokKey, Act FROM AweSchTskHok WHERE IdeTsk = ?", taskId))
      .containsEntry("HOKKEY", hookKey + "-b").containsEntry("ACT", 0);

    // A task that stops being a webhook task loses its hook
    maintain("UpdateSchedulerTaskEditor", manualTask(unique("Webhook launch")).put("Ide", taskId));
    assertThat(count("SELECT COUNT(*) FROM AweSchTskHok WHERE IdeTsk = ?", taskId)).isZero();
  }

  @Test
  void aKeyOfAnotherTaskOrOutsideTheAlphabetIsRejectedWithAMessageAndSavesNothing() throws Exception {
    String name = unique("Webhook duplicate");

    for (String key : new String[]{hookKey, "Not A Key"}) {
      JsonNode actions = perform("maintain", "NewSchedulerTaskEditor", webhookTask(name, key));
      assertThat(actions.toString()).contains("\"type\":\"error\"");
    }
    assertThat(count("SELECT COUNT(*) FROM AweSchTsk WHERE Nam = ?", name)).isZero();
    assertThat(count("SELECT COUNT(*) FROM AweSchTskHok WHERE HokKey = ?", hookKey)).isEqualTo(1);
  }

  @Test
  void aWebhookThatIsNotValidIsRejectedAndOnlyTheWebhookPathIsAnonymous() throws Exception {
    String body = envelope("loaded", "{\"day\":\"2026-11-12\"}");

    webhook(hookKey, "msg-2", body, false).andExpect(status().isUnauthorized());
    webhook("no-such-hook", "msg-2", body, true).andExpect(status().isNotFound());
    // The scheduler api keeps asking for its bearer
    mockMvc.perform(post("/scheduler/api/v1/tasks/" + taskId + "/executions/1/complete").with(anonymous())
      .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OK\"}")).andExpect(status().isUnauthorized());
    assertThat(count("SELECT COUNT(*) FROM AweSchHokMsg WHERE Src = ?", hookKey)).isZero();
  }

  @Test
  void aMessageThatTheHookDoesNotWantIsIgnoredAndAFailingOneIsKeptFailedWithItsError() throws Exception {
    // The type filter of the hook is "loaded"
    webhook(hookKey, "msg-3", envelope("other", "{\"day\":\"2026-11-12\"}"), true).andExpect(status().isAccepted());
    // A message whose payload cannot be read fails its launch
    jdbc().update("INSERT INTO AweSchHokMsg (Src, MsgId, RcvDat, Typ, Pay, Sta, Att, NxtDat) "
      + "VALUES (?, 'msg-4', CURRENT_TIMESTAMP, 'loaded', 'not json', 'RECEIVED', 0, CURRENT_TIMESTAMP)", hookKey);

    processor.sweep();

    await(() -> count("SELECT COUNT(*) FROM AweSchHokMsg WHERE Src = ? AND Sta <> 'RECEIVED'", hookKey) == 2,
      "both messages to be processed");
    Map<String, Object> ignored = single("SELECT Sta, Err, IdeTsk, PrcDat FROM AweSchHokMsg WHERE MsgId = 'msg-3'");
    assertThat(ignored).containsEntry("STA", "IGNORED");
    assertThat(ignored.get("ERR")).isNotNull();
    assertThat(ignored.get("IDETSK")).isNull();
    Map<String, Object> failed = single("SELECT Sta, Err, Att, PrcDat, NxtDat FROM AweSchHokMsg WHERE MsgId = 'msg-4'");
    assertThat(failed).containsEntry("STA", "FAILED").containsEntry("ATT", 1);
    assertThat(failed.get("ERR")).isNotNull();
    assertThat(failed.get("PRCDAT")).isNull();
    assertThat(count("SELECT COUNT(*) FROM AweSchExe WHERE IdeTsk = ?", taskId)).isZero();
  }

  private String envelope(String type, String data) {
    return "{\"type\":\"" + type + "\",\"timestamp\":\"2026-11-16T06:00:02Z\",\"data\":" + data + "}";
  }

  private ResultActions webhook(String key, String id, String body, boolean validSignature) throws Exception {
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String signature = validSignature ? sign(id, timestamp, body) : "v1,AAAA";
    return mockMvc.perform(post("/scheduler/api/v1/webhooks/triggers/" + key).with(anonymous())
      .contentType(MediaType.APPLICATION_JSON).header("webhook-id", id).header("webhook-timestamp", timestamp)
      .header("webhook-signature", signature).content(body));
  }

  private String sign(String id, String timestamp, String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(Base64.getDecoder().decode(SECRET.substring("whsec_".length())), "HmacSHA256"));
    return "v1," + Base64.getEncoder().encodeToString(
      mac.doFinal((id + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
  }

  private List<Map<String, String>> launches() {
    return dummyService.getWebhookLaunches();
  }

  /**
   * The identifiers of the deleted tasks are reused: remove what the test leaves for the next task with the same one
   */
  private void forget() {
    jdbc().update("DELETE FROM AweSchHokMsg WHERE Src = ?", hookKey);
    jdbc().update("DELETE FROM AweSchTskHok WHERE IdeTsk = ?", taskId);
    jdbc().update("DELETE FROM AweSchExeExt WHERE IdeTsk = ?", taskId);
    jdbc().update("DELETE FROM AweSchExe WHERE IdeTsk = ?", taskId);
    dummyService.getWebhookLaunches().clear();
  }

  private JdbcTemplate jdbc() {
    return new JdbcTemplate(dataSource);
  }

  private Map<String, Object> single(String sql, Object... arguments) {
    List<Map<String, Object>> rows = jdbc().queryForList(sql, arguments);
    assertThat(rows).as(sql).hasSize(1);
    return rows.get(0);
  }

  private long count(String sql, Object... arguments) {
    Long count = jdbc().queryForObject(sql, Long.class, arguments);
    return count == null ? 0 : count;
  }

  private void await(BooleanSupplier condition, String description) {
    Awaitility.await("Waiting for " + description).atMost(Duration.ofMillis(WAIT_MILLIS))
      .pollInterval(Duration.ofMillis(50)).until(condition::getAsBoolean);
  }
}
