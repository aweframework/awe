package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.task.TaskVariable;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the operator-values adapter used by the manual launch modal.
 *
 * <p>The List-to-Map conversion is isolated in {@link RemoteSchedulerService#toVariableMap(List)}
 * so it can be tested without touching Quartz. The full executeTaskNow(Integer, String, List)
 * overload only zips the rows and delegates to the existing Map overload.
 */
class RemoteSchedulerServiceTest {

  /**
   * Build the service under test with mocked collaborators.
   *
   * @param schedulerService Local scheduler service
   * @param remoteScheduler  Feign client
   * @param remote           Whether the scheduler runs as a separate remote instance
   * @return Service under test
   */
  private RemoteSchedulerService service(SchedulerService schedulerService, RemoteScheduler remoteScheduler, boolean remote) {
    return new RemoteSchedulerService(schedulerService, remoteScheduler, new ObjectMapper(), remote);
  }

  /**
   * Capture the variables map the given Feign client received on executeTaskNow.
   *
   * @param remoteScheduler Feign client mock
   * @return Captured variables map
   */
  @SuppressWarnings("unchecked")
  private Map<String, String> capturedRemoteVariables(RemoteScheduler remoteScheduler) {
    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(remoteScheduler).executeTaskNow(eq(1), eq("user"), captor.capture());
    return captor.getValue();
  }

  /**
   * Manual launch with no operator values reaches the remote scheduler with an empty map.
   */
  @Test
  void executeTaskNowWithoutValuesSendsEmptyMapToRemote() throws AWException {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);

    service(mock(SchedulerService.class), remoteScheduler, true).executeTaskNow(1, "user");

    Map<String, String> sent = capturedRemoteVariables(remoteScheduler);
    assertNotNull(sent, "A null map is rejected by Feign before the request is built");
    assertTrue(sent.isEmpty(), "No operator values were supplied");
  }

  /**
   * A caller reaching the Map overload directly with null gets the same empty map.
   */
  @Test
  void executeTaskNowWithNullMapSendsEmptyMapToRemote() throws AWException {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);

    service(mock(SchedulerService.class), remoteScheduler, true).executeTaskNow(1, "user", (Map<String, String>) null);

    Map<String, String> sent = capturedRemoteVariables(remoteScheduler);
    assertNotNull(sent, "A null map is rejected by Feign before the request is built");
    assertTrue(sent.isEmpty(), "No operator values were supplied");
  }

  /**
   * Operator supplied values are forwarded untouched.
   */
  @Test
  void executeTaskNowForwardsOperatorValuesToRemote() throws AWException {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);

    service(mock(SchedulerService.class), remoteScheduler, true).executeTaskNow(1, "user", Map.of("date", "2026-01-31"));

    assertEquals(Map.of("date", "2026-01-31"), capturedRemoteVariables(remoteScheduler));
  }

  /**
   * The embedded scheduler receives the same empty map.
   */
  @Test
  void executeTaskNowWithoutValuesSendsEmptyMapToLocalScheduler() throws AWException {
    SchedulerService schedulerService = mock(SchedulerService.class);

    service(schedulerService, mock(RemoteScheduler.class), false).executeTaskNow(1, "user");

    ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
    verify(schedulerService).executeTaskNow(eq(1), eq("user"), captor.capture());
    assertNotNull(captor.getValue());
    assertTrue(captor.getValue().isEmpty());
  }

  /**
   * An unresolved property reported by the scheduler becomes an AWException, which aborts the
   * launch chain and is rendered by AWE.
   */
  @Test
  void validateTaskParametersRaisesTheReportedError() {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    when(remoteScheduler.validateTaskParameters(anyInt(), any())).thenReturn(new ServiceData()
      .setType(AnswerType.ERROR)
      .setTitle("Parameter configuration error")
      .setMessage("The property missing.key is not defined"));

    AWException thrown = assertThrows(AWException.class, () ->
      service(mock(SchedulerService.class), remoteScheduler, true).validateTaskParameters(1, List.of()));

    assertEquals("The property missing.key is not defined", thrown.getMessage());
  }

  /**
   * A clean validation does not raise.
   */
  @Test
  void validateTaskParametersPassesWhenEveryPropertyResolves() {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    when(remoteScheduler.validateTaskParameters(anyInt(), any())).thenReturn(new ServiceData());

    assertDoesNotThrow(() ->
      service(mock(SchedulerService.class), remoteScheduler, true).validateTaskParameters(1, List.of()));
  }

  /**
   * The validation runs on the embedded scheduler when the instance is not remote, since that is
   * the JVM whose configuration resolves the properties.
   */
  @Test
  void validateTaskParametersUsesTheEmbeddedSchedulerWhenNotRemote() throws AWException {
    SchedulerService schedulerService = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    when(schedulerService.validateTaskParameters(anyInt(), any())).thenReturn(new ServiceData());

    service(schedulerService, remoteScheduler, false).validateTaskParameters(1, List.of(
      new TaskVariable().setName("region").setValue("my.region")));

    verify(schedulerService).validateTaskParameters(eq(1), eq(Map.of("region", "my.region")));
    verify(remoteScheduler, org.mockito.Mockito.never()).validateTaskParameters(anyInt(), any());
  }

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
