package com.almis.awe.test.integration.database.hsql;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * «Motor» tab of the scheduler configuration screen (#993) over the embedded scheduler and the HSQL database: the card
 * of the running engine counts the schedules it holds and shows its status as its chip with its facts, and a scheduled
 * task is one of its schedules with its name, its kind, its next fire time and its state (also as the cells of the
 * grid), filtered out by the «paused» filter
 */
@Tag("integration")
class SchedulerConfigurationEngineTest extends AbstractSchedulerMaintainTest {

  @Test
  void aScheduledTaskIsAScheduleOfTheRunningEngine() throws Exception {
    String name = unique("Engine schedule");
    int taskId = createTask(scheduledTask(name, 6));
    try {
      JsonNode engine = rows("getConfigurationEngine", params()).get(0);
      assertThat(engine.get("EngSta").asString()).isEqualTo("running");
      assertThat(engine.get("EngSch").asInt()).isPositive();
      assertThat(engine.get("EngThrSiz").asInt()).isPositive();

      JsonNode schedule = find(rows("getConfigurationEngineTriggers", params()), "IdeTsk", String.valueOf(taskId))
        .orElseThrow();
      assertThat(schedule.get("TskNam").asString()).isEqualTo(name);
      assertThat(schedule.get("TrgKnd").asString()).isEqualTo("scheduled");
      assertThat(schedule.get("TrgSta").asString()).isEqualTo("normal");
      assertThat(schedule.get("NxtFir").asString()).isEqualTo("01/01/2099 00:00");
      // The cells of the grid of the tab: the task, the kind with its icon and the state as a pill
      assertThat(schedule.get("TrgTskNam").asString()).isEqualTo(name);
      assertThat(schedule.get("TrgKndIcn").asString()).isEqualTo("lucide:clock");
      assertThat(schedule.get("TrgStaTon").asString()).isEqualTo("success");
      assertThat(schedule.get("TrgStaIcn").asString()).isEqualTo("lucide:circle-check");

      // The card of the running engine: its status shown once, as the chip with its tone (no icon), the sentence that
      // tells it, its facts as tiles, and the status that enables its actions
      JsonNode card = rows("getConfigurationEngineCard", params()).get(0);
      assertThat(card.get("chip").asString()).isEqualTo("SCREEN_TEXT_SCHEDULER_ENGINE_STATUS_RUNNING");
      assertThat(card.get("chipTone").asString()).isEqualTo("success");
      assertThat(card.has("icon")).isFalse();
      assertThat(card.get("description").asString()).isNotBlank();
      assertThat(card.get("tiles").asString()).startsWith("[{").contains("\"label\"");
      assertThat(rows("getConfigurationEngineStatus", params()).get(0).get("value").asString()).isEqualTo("running");

      List<JsonNode> paused = rows("getConfigurationEngineTriggers", params().put("CrtEngTrg", "paused"));
      assertThat(find(paused, "IdeTsk", String.valueOf(taskId))).isEmpty();
      JsonNode all = rows("getConfigurationEngineTriggerCounts", params()).get(0);
      assertThat(all.get("count").asInt()).isEqualTo(engine.get("EngSch").asInt());
    } finally {
      deleteTasks(taskId);
    }
  }
}
