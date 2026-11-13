package com.almis.awe.scheduler.dao.task;

import com.almis.awe.scheduler.bean.task.Task;
import com.almis.awe.scheduler.bean.task.TaskParameter;
import com.almis.awe.scheduler.constant.ParameterConstants;
import com.almis.awe.scheduler.enums.TriggerType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TaskParameterOverrides}
 */
class TaskParameterOverridesTest {

  private static final String VALUE = String.valueOf(ParameterConstants.VALUE);
  private static final String VARIABLE = String.valueOf(ParameterConstants.VARIABLE);
  private static final String PROPERTY = String.valueOf(ParameterConstants.PROPERTY);

  private final TaskParameterOverrides overrides = new TaskParameterOverrides(
    key -> Map.of("present.key", "EU").get(key));

  @Test
  void manualLaunchesOverrideEverySourceAndTheRestOnlyVariables() {
    assertEquals(Set.of(VALUE, VARIABLE, PROPERTY), overrides.overridableSources(TriggerType.MANUAL));
    assertEquals(Set.of(VARIABLE), overrides.overridableSources(TriggerType.DEPENDENCY));
    assertEquals(Set.of(VARIABLE), overrides.overridableSources(TriggerType.TASK));
    assertEquals(Set.of(VARIABLE), TaskParameterOverrides.VARIABLE_SOURCES);
  }

  @Test
  void applyOnlyOverridesParametersOfTheGivenSources() {
    TaskParameter value = parameter("mode", VALUE, "smoke");
    TaskParameter variable = parameter("date", VARIABLE, "STORED");
    Task task = new Task().setParameterList(List.of(value, variable));

    overrides.apply(task, Map.of("mode", "OTHER", "date", "2026-10-15"), TaskParameterOverrides.VARIABLE_SOURCES);

    assertEquals("smoke", value.getValue());
    assertEquals("2026-10-15", variable.getValue());
  }

  @Test
  void applyWithoutParametersIsANoOp() {
    Task task = new Task();

    overrides.apply(task, Map.of("date", "2026-10-15"), TaskParameterOverrides.VARIABLE_SOURCES);

    assertEquals(null, task.getParameterList());
  }

  @Test
  void unresolvedPropertiesListsMissingBlankAndNullKeysInOrder() {
    Task task = new Task().setParameterList(Arrays.asList(
      parameter("a", PROPERTY, "missing.key"),
      parameter("b", PROPERTY, "present.key"),
      parameter("c", PROPERTY, null),
      parameter("d", PROPERTY, "  "),
      parameter("e", VALUE, "missing.key")));

    assertEquals(List.of("missing.key", "", "  "), overrides.unresolvedProperties(task));
  }

  @Test
  void unresolvedPropertiesOfATaskWithoutParametersIsEmpty() {
    assertTrue(overrides.unresolvedProperties(new Task()).isEmpty());
  }

  @Test
  void propagatedValuesResolvePropertiesAndSkipMissingValuesKeepingOrder() {
    Task task = new Task().setParameterList(Arrays.asList(
      parameter("region", PROPERTY, "present.key"),
      parameter("lost", PROPERTY, "missing.key"),
      parameter(null, VALUE, "noName"),
      parameter("empty", VALUE, null),
      parameter("date", VARIABLE, "2026-10-15"),
      parameter("mode", VALUE, "smoke")));

    Map<String, String> values = overrides.propagatedValues(task);

    assertEquals(List.of("region", "date", "mode"), List.copyOf(values.keySet()));
    assertEquals("EU", values.get("region"));
    assertEquals("2026-10-15", values.get("date"));
    assertEquals("smoke", values.get("mode"));
  }

  @Test
  void propagatedValuesOfATaskWithoutParametersIsEmpty() {
    assertTrue(overrides.propagatedValues(new Task()).isEmpty());
  }

  private TaskParameter parameter(String name, String source, String value) {
    return new TaskParameter().setName(name).setSource(source).setValue(value);
  }
}
