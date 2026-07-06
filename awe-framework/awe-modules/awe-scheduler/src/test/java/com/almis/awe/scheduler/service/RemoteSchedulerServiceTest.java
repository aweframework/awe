package com.almis.awe.scheduler.service;

import com.almis.awe.scheduler.bean.task.TaskVariable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the operator-values adapter used by the manual launch modal.
 *
 * <p>The List-to-Map conversion is isolated in {@link RemoteSchedulerService#toVariableMap(List)}
 * so it can be tested without touching Quartz. The full executeTaskNow(Integer, String, List)
 * overload only zips the rows and delegates to the existing Map overload.
 */
class RemoteSchedulerServiceTest {

  /**
   * A normal set of rows is preserved in order, name mapped to value.
   */
  @Test
  void toVariableMapZipsRowsPreservingOrder() {
    List<TaskVariable> variables = List.of(
      new TaskVariable().setName("first").setValue("1"),
      new TaskVariable().setName("second").setValue("2"),
      new TaskVariable().setName("third").setValue("3"));

    Map<String, String> result = RemoteSchedulerService.toVariableMap(variables);

    assertEquals(3, result.size());
    assertEquals("1", result.get("first"));
    assertEquals("2", result.get("second"));
    assertEquals("3", result.get("third"));
    assertEquals(List.of("first", "second", "third"), new ArrayList<>(result.keySet()));
  }

  /**
   * A null list yields an empty map (task still launches with no operator values).
   */
  @Test
  void toVariableMapNullListYieldsEmptyMap() {
    assertTrue(RemoteSchedulerService.toVariableMap(null).isEmpty());
  }

  /**
   * An empty list yields an empty map.
   */
  @Test
  void toVariableMapEmptyListYieldsEmptyMap() {
    assertTrue(RemoteSchedulerService.toVariableMap(List.of()).isEmpty());
  }

  /**
   * Rows with a null or blank name, and null rows, are skipped.
   */
  @Test
  void toVariableMapSkipsNullOrBlankNames() {
    List<TaskVariable> variables = new ArrayList<>();
    variables.add(new TaskVariable().setName("kept").setValue("ok"));
    variables.add(new TaskVariable().setName(null).setValue("dropped-null-name"));
    variables.add(new TaskVariable().setName("   ").setValue("dropped-blank-name"));
    variables.add(null);

    Map<String, String> result = RemoteSchedulerService.toVariableMap(variables);

    assertEquals(1, result.size());
    assertEquals("ok", result.get("kept"));
  }

  /**
   * On a duplicate name the last value wins.
   */
  @Test
  void toVariableMapLastValueWinsOnDuplicateName() {
    List<TaskVariable> variables = List.of(
      new TaskVariable().setName("dup").setValue("old"),
      new TaskVariable().setName("dup").setValue("new"));

    Map<String, String> result = RemoteSchedulerService.toVariableMap(variables);

    assertEquals(1, result.size());
    assertEquals("new", result.get("dup"));
  }
}
