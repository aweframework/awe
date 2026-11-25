package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.scheduler.bean.task.ExecutionListCriteria;
import com.almis.awe.scheduler.bean.task.TaskCatalogCriteria;
import com.almis.awe.scheduler.bean.task.TaskVariable;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    return new RemoteSchedulerService(schedulerService, remoteScheduler, new JsonMapper(), remote);
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

  /**
   * The execution list of all tasks is read from the remote scheduler when it is remote, and locally otherwise.
   */
  @Test
  void executionListFollowsTheRemoteFlag() throws AWException {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setTaskName("nightly");
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    ServiceData remoteData = new ServiceData();
    ServiceData localData = new ServiceData();
    when(remoteScheduler.getExecutionList(criteria)).thenReturn(remoteData);
    when(local.getExecutionList(criteria)).thenReturn(localData);

    assertSame(remoteData, service(local, remoteScheduler, true).getExecutionList(criteria));
    assertSame(localData, service(local, remoteScheduler, false).getExecutionList(criteria));
  }

  /**
   * The activity counters are read from the remote scheduler when it is remote, and locally otherwise.
   */
  @Test
  void activityCountersFollowTheRemoteFlag() throws AWException {
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    ServiceData remoteData = new ServiceData();
    ServiceData localData = new ServiceData();
    when(remoteScheduler.getActivityCounters()).thenReturn(remoteData);
    when(local.getActivityCounters()).thenReturn(localData);

    assertSame(remoteData, service(local, remoteScheduler, true).getActivityCounters());
    assertSame(localData, service(local, remoteScheduler, false).getActivityCounters());
  }

  /**
   * The counts of the state quick filter are read from the remote scheduler when it is remote, after validating the
   * filters on this instance, and locally otherwise.
   */
  @Test
  void stateCountsFollowTheRemoteFlagAndValidateTheFiltersBeforeCallingTheRemoteScheduler() throws AWException {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setTaskName("nightly");
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    ServiceData remoteData = new ServiceData();
    ServiceData localData = new ServiceData();
    when(remoteScheduler.getExecutionStateCounts(criteria)).thenReturn(remoteData);
    when(local.getExecutionStateCounts(criteria)).thenReturn(localData);

    assertSame(remoteData, service(local, remoteScheduler, true).getExecutionStateCounts(criteria));
    assertSame(localData, service(local, remoteScheduler, false).getExecutionStateCounts(criteria));
    verify(local).validateExecutionListCriteria(criteria);

    ExecutionListCriteria invalid = new ExecutionListCriteria().setState("api");
    org.mockito.Mockito.doThrow(new com.almis.awe.scheduler.exception.InvalidExecutionFilterException("t", "m"))
      .when(local).validateExecutionListCriteria(invalid);
    assertThrows(com.almis.awe.scheduler.exception.InvalidExecutionFilterException.class,
      () -> service(local, remoteScheduler, true).getExecutionStateCounts(invalid));
    verify(remoteScheduler, org.mockito.Mockito.never()).getExecutionStateCounts(invalid);
  }

  /**
   * With a remote scheduler, the filters are validated on this instance first: an invalid value never reaches the
   * remote scheduler and raises the AWE validation error.
   */
  @Test
  void remoteExecutionListValidatesTheFiltersBeforeCallingTheRemoteScheduler() throws AWException {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setOrigins(List.of("api"));
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    org.mockito.Mockito.doThrow(new com.almis.awe.scheduler.exception.InvalidExecutionFilterException("t", "m"))
      .when(local).validateExecutionListCriteria(criteria);

    assertThrows(com.almis.awe.scheduler.exception.InvalidExecutionFilterException.class,
      () -> service(local, remoteScheduler, true).getExecutionList(criteria));
    org.mockito.Mockito.verifyNoInteractions(remoteScheduler);
  }

  /**
   * The executions grouped by flow run are read from the remote scheduler when it is remote (its filters validated
   * here first), and locally otherwise.
   */
  @Test
  void runListFollowsTheRemoteFlagAndValidatesTheFiltersBeforeCallingTheRemoteScheduler() throws AWException {
    ExecutionListCriteria criteria = new ExecutionListCriteria().setTaskName("nightly");
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    ServiceData remoteData = new ServiceData();
    ServiceData localData = new ServiceData();
    when(remoteScheduler.getRunList(criteria)).thenReturn(remoteData);
    when(local.getRunList(criteria)).thenReturn(localData);

    assertSame(remoteData, service(local, remoteScheduler, true).getRunList(criteria));
    assertSame(localData, service(local, remoteScheduler, false).getRunList(criteria));
    verify(local).validateExecutionListCriteria(criteria);

    ExecutionListCriteria invalid = new ExecutionListCriteria().setMax(501);
    org.mockito.Mockito.doThrow(new com.almis.awe.scheduler.exception.InvalidExecutionFilterException("t", "m"))
      .when(local).validateExecutionListCriteria(invalid);
    assertThrows(com.almis.awe.scheduler.exception.InvalidExecutionFilterException.class,
      () -> service(local, remoteScheduler, true).getRunList(invalid));
    verify(remoteScheduler, org.mockito.Mockito.never()).getRunList(invalid);
  }
  /**
   * The summary and the graph of a flow run are read from the remote scheduler when it is remote, and locally
   * otherwise; without run there is nothing to read
   */
  @Test
  void flowRunPageFollowsTheRemoteFlagAndReadsNothingWithoutRun() throws AWException {
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    String runId = "0f8fad5b-d9cb-469f-a165-70867728950e";
    ServiceData remoteSummary = new ServiceData();
    ServiceData localSummary = new ServiceData();
    ServiceData remoteGraph = new ServiceData();
    ServiceData localGraph = new ServiceData();
    when(remoteScheduler.getFlowRunSummary(runId)).thenReturn(remoteSummary);
    when(local.getFlowRunSummary(runId)).thenReturn(localSummary);
    when(remoteScheduler.getFlowRunGraph(runId)).thenReturn(remoteGraph);
    when(local.getFlowRunGraph(runId)).thenReturn(localGraph);

    assertSame(remoteSummary, service(local, remoteScheduler, true).getFlowRunSummary(runId));
    assertSame(localSummary, service(local, remoteScheduler, false).getFlowRunSummary(runId));
    assertSame(remoteGraph, service(local, remoteScheduler, true).getFlowRunGraph(runId));
    assertSame(localGraph, service(local, remoteScheduler, false).getFlowRunGraph(runId));
    assertTrue(service(local, remoteScheduler, true).getFlowRunGraph(null).getDataList().getRows().isEmpty());
    assertTrue(service(local, remoteScheduler, true).getFlowRunSummary(" ").getDataList().getRows().isEmpty());
    verify(remoteScheduler, org.mockito.Mockito.never()).getFlowRunGraph(null);
  }

  /**
   * A flow run id that is not the shape of the ids of AweSchRun (a UUID) is not a run: it never reaches the URL of the
   * remote scheduler nor the local one, and there is nothing to read
   */
  @Test
  void aFlowRunIdThatIsNotAUuidReadsNothingAndNeverReachesTheRemoteScheduler() throws AWException {
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);

    for (String invalid : List.of("run-1", "../tasks", "0f8fad5b-d9cb-469f-a165-70867728950e/graph")) {
      assertTrue(service(local, remoteScheduler, true).getFlowRunSummary(invalid).getDataList().getRows().isEmpty());
      assertTrue(service(local, remoteScheduler, true).getFlowRunGraph(invalid).getDataList().getRows().isEmpty());
      assertTrue(service(local, remoteScheduler, false).getFlowRunGraph(invalid).getDataList().getRows().isEmpty());
    }
    org.mockito.Mockito.verifyNoInteractions(remoteScheduler, local);
  }

  /**
   * The task catalog and the counts of its active filter are read from the remote scheduler when it is remote, after
   * validating the filters on this instance (an invalid value never reaches it), and locally otherwise.
   */
  @Test
  void taskCatalogFollowsTheRemoteFlagAndValidatesTheFiltersBeforeCallingTheRemoteScheduler() throws AWException {
    TaskCatalogCriteria criteria = new TaskCatalogCriteria().setSearch("nightly");
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    ServiceData remoteData = new ServiceData();
    ServiceData localData = new ServiceData();
    ServiceData remoteCounts = new ServiceData();
    ServiceData localCounts = new ServiceData();
    when(remoteScheduler.getTaskCatalog(criteria)).thenReturn(remoteData);
    when(local.getTaskCatalog(criteria)).thenReturn(localData);
    when(remoteScheduler.getTaskCatalogActiveCounts(criteria)).thenReturn(remoteCounts);
    when(local.getTaskCatalogActiveCounts(criteria)).thenReturn(localCounts);

    assertSame(remoteData, service(local, remoteScheduler, true).getTaskCatalog(criteria));
    assertSame(localData, service(local, remoteScheduler, false).getTaskCatalog(criteria));
    assertSame(remoteCounts, service(local, remoteScheduler, true).getTaskCatalogActiveCounts(criteria));
    assertSame(localCounts, service(local, remoteScheduler, false).getTaskCatalogActiveCounts(criteria));
    verify(local, org.mockito.Mockito.times(2)).validateTaskCatalogCriteria(criteria);

    TaskCatalogCriteria invalid = new TaskCatalogCriteria().setLaunchType("api");
    org.mockito.Mockito.doThrow(new com.almis.awe.scheduler.exception.InvalidExecutionFilterException("t", "m"))
      .when(local).validateTaskCatalogCriteria(invalid);
    assertThrows(com.almis.awe.scheduler.exception.InvalidExecutionFilterException.class,
      () -> service(local, remoteScheduler, true).getTaskCatalog(invalid));
    verify(remoteScheduler, org.mockito.Mockito.never()).getTaskCatalog(invalid);
  }

  /**
   * A task saved from the task editor is refreshed (without purging its executions) in the remote scheduler when it is
   * remote, and locally otherwise
   */
  @Test
  void theEditedTaskIsRefreshedInTheSchedulerThatFollowsTheRemoteFlag() throws AWException {
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);

    service(local, remoteScheduler, true).refreshSchedulerTask(5);
    service(local, remoteScheduler, false).refreshSchedulerTask(6);

    verify(remoteScheduler).refreshTask(5);
    verify(local).refreshSchedulerTask(6);
    verify(local, org.mockito.Mockito.never()).updateSchedulerTask(any());
  }

  /**
   * Saving a calendar from the side panel of the configuration screen reloads it in the engine and then reschedules the
   * tasks that use it with it (active) or without it (inactive), as the row menu does, in the scheduler that follows
   * the remote flag
   */
  @Test
  void aCalendarSavedFromThePanelIsReloadedAndItsTasksRescheduledWithItOrWithoutIt() throws AWException {
    SchedulerService local = mock(SchedulerService.class);
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    org.mockito.InOrder order = org.mockito.Mockito.inOrder(local, remoteScheduler);

    service(local, remoteScheduler, false).updateConfigurationCalendar(5, 1);
    service(local, remoteScheduler, false).updateConfigurationCalendar(6, 0);
    service(local, remoteScheduler, true).updateConfigurationCalendar(7, 0);

    order.verify(local).updateSchedulerCalendar(5);
    order.verify(local).activateCalendar(List.of(5));
    order.verify(local).updateSchedulerCalendar(6);
    order.verify(local).deactivateCalendar(List.of(6));
    order.verify(remoteScheduler).updateCalendar(7);
    order.verify(remoteScheduler).deactivateCalendar(List.of(7));
    verify(local, org.mockito.Mockito.never()).deactivateCalendar(List.of(5));
  }
}
