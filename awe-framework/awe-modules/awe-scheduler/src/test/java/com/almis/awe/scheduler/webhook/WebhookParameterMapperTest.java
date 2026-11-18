package com.almis.awe.scheduler.webhook;

import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which values of the webhook data reach the task: only the variable parameters with a pointer
 */
class WebhookParameterMapperTest {

  private static final JsonNode DATA = JsonMapper.builder().build()
    .readTree("{\"date\":\"2026-11-12\",\"entity\":\"0049\",\"count\":7,\"tags\":[\"a\",\"b\"],\"none\":null}");

  private static TaskParameter parameter(String name, int source, String pointer) {
    return new TaskParameter().setName(name).setSource(String.valueOf(source)).setPointer(pointer);
  }

  @Test
  void aMappedVariableArrivesAsText() {
    List<TaskParameter> parameters = List.of(
      parameter("day", ParameterConstants.VARIABLE, "/date"),
      parameter("total", ParameterConstants.VARIABLE, "/count"),
      parameter("tags", ParameterConstants.VARIABLE, "/tags"),
      parameter("second", ParameterConstants.VARIABLE, "/tags/1"));

    assertThat(WebhookParameterMapper.variables(parameters, DATA)).containsOnly(
      Map.entry("day", "2026-11-12"), Map.entry("total", "7"), Map.entry("tags", "[\"a\",\"b\"]"),
      Map.entry("second", "b"));
  }

  @Test
  void anUnmappedParameterDoesNotArrive() {
    List<TaskParameter> parameters = List.of(
      parameter("entity", ParameterConstants.VARIABLE, null),
      parameter("other", ParameterConstants.VARIABLE, " "),
      parameter("day", ParameterConstants.VARIABLE, "/date"));

    assertThat(WebhookParameterMapper.variables(parameters, DATA)).containsOnlyKeys("day");
  }

  @Test
  void aValueOrPropertyParameterIsNeverOverridden() {
    List<TaskParameter> parameters = List.of(
      parameter("fixed", ParameterConstants.VALUE, "/date"),
      parameter("key", ParameterConstants.PROPERTY, "/entity"));

    assertThat(WebhookParameterMapper.variables(parameters, DATA)).isEmpty();
  }

  @Test
  void aPointerWithoutValueIsSkipped() {
    List<TaskParameter> parameters = List.of(
      parameter("missing", ParameterConstants.VARIABLE, "/nothing/here"),
      parameter("null", ParameterConstants.VARIABLE, "/none"),
      parameter("bad", ParameterConstants.VARIABLE, "date"),
      parameter("day", ParameterConstants.VARIABLE, "/date"));

    assertThat(WebhookParameterMapper.variables(parameters, DATA)).containsOnlyKeys("day");
  }

  @Test
  void aTaskWithoutParametersGetsNothing() {
    assertThat(WebhookParameterMapper.variables(null, DATA)).isEmpty();
  }
}
