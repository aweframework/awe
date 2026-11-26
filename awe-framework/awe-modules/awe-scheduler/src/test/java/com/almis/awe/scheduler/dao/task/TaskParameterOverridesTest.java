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
  void manualLaunchesOverrideValuesAndVariablesAndTheRestOnlyVariables() {
    assertEquals(Set.of(VALUE, VARIABLE), overrides.overridableSources(TriggerType.MANUAL));
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

  @Test
  void invalidValuesListsTheChangedValuesThatAreNotOfTheParameterTypeInOrder() {
    Task task = new Task().setParameterList(List.of(
      typed("count", VALUE, "INTEGER", "5"),
      typed("size", VARIABLE, "LONG", "5"),
      typed("rate", VALUE, "DOUBLE", "0.5"),
      typed("ratio", VALUE, "FLOAT", "0.5"),
      typed("day", VARIABLE, "DATE", "17/11/2026"),
      typed("at", VALUE, "TIME", "08:00:00"),
      typed("mode", VALUE, "STRING", "FULL")));

    assertEquals(List.of("count", "size", "rate", "day", "at"), overrides.invalidValues(task, Map.of(
      "count", "five", "size", "1.5", "rate", "1,5", "ratio", "-2.25", "day", "31/02/2026", "at", "25:00",
      "mode", "any text")));
    assertEquals(List.of(), overrides.invalidValues(task, Map.of(
      "count", "-12", "size", "9000000000", "rate", "3", "day", "2026-11-18", "at", "08:05", "mode", "")));
  }

  @Test
  void aBlankValueIsOnlyValidForTextParameters() {
    Task task = new Task().setParameterList(List.of(
      typed("count", VALUE, "INTEGER", "5"),
      typed("note", VALUE, "STRING", "x"),
      typed("data", VARIABLE, "OBJECT", "x")));

    assertEquals(List.of("count"), overrides.invalidValues(task, Map.of("count", " ", "note", "", "data", "")));
  }

  @Test
  void invalidValuesIgnoresUnchangedValuesPropertyKeysAndUnknownNames() {
    // A stored value is not checked again: the launch keeps working with what the task already had
    Task task = new Task().setParameterList(Arrays.asList(
      typed("count", VALUE, "INTEGER", "not a number"),
      typed("empty", VARIABLE, "INTEGER", null),
      typed("region", PROPERTY, "INTEGER", "present.key")));

    assertEquals(List.of(), overrides.invalidValues(task, Map.of("count", "not a number", "empty", "",
      "region", "other.key", "unknown", "x")));
    assertEquals(List.of(), overrides.invalidValues(task, null));
    assertEquals(List.of(), overrides.invalidValues(new Task(), Map.of("count", "x")));
  }

  @Test
  void invalidValuesOfATaskThatDoesNotExistIsEmpty() {
    // The launch of a task that does not exist answers it does not exist, as it did before the values were checked
    assertEquals(List.of(), overrides.invalidValues(null, Map.of("count", "five")));
    assertEquals(List.of(), overrides.changedPropertyKeys(null, Map.of("region", "other.key")));
  }

  @Test
  void typedValuesAreCheckedAndAppliedWithoutTheirSurroundingBlanks() {
    TaskParameter count = typed("count", VALUE, "INTEGER", "5");
    TaskParameter rate = typed("rate", VARIABLE, "DOUBLE", "1");
    TaskParameter note = typed("note", VALUE, "STRING", "x");
    Task task = new Task().setParameterList(List.of(count, rate, note));
    Map<String, String> values = Map.of("count", " 7 ", "rate", " .5", "note", " spaced ");

    assertEquals(List.of(), overrides.invalidValues(task, values));
    overrides.apply(task, values, overrides.overridableSources(TriggerType.MANUAL));

    assertEquals("7", count.getValue());
    assertEquals(".5", rate.getValue());
    assertEquals(" spaced ", note.getValue());
  }

  @Test
  void aManualLaunchCannotChangeTheKeyOfAProperty() {
    TaskParameter region = typed("region", PROPERTY, "STRING", "present.key");
    TaskParameter zone = typed("zone", PROPERTY, "STRING", "other.key");
    Task task = new Task().setParameterList(List.of(region, zone));

    assertEquals(List.of("zone"), overrides.changedPropertyKeys(task, Map.of("region", " present.key ", "zone", "x.key")));
    assertEquals(List.of(), overrides.changedPropertyKeys(task, Map.of("region", "present.key")));
    overrides.apply(task, Map.of("region", "another.key"), overrides.overridableSources(TriggerType.MANUAL));
    assertEquals("present.key", region.getValue());
  }

  private TaskParameter typed(String name, String source, String type, String value) {
    return parameter(name, source, value).setType(type);
  }

  private TaskParameter parameter(String name, String source, String value) {
    return new TaskParameter().setName(name).setSource(source).setValue(value);
  }
}
