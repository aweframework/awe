package com.almis.awe.scheduler.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import com.almis.awe.scheduler.feign.RemoteScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * «Motor» tab of the configuration screen in remote mode (#993): the engine is read where it runs, through the remote
 * scheduler, and it is told as a remote one; a filter that is not valid is refused before it reaches it
 */
class RemoteSchedulerServiceEngineTest {

  @Test
  void aRemoteEngineIsReadThroughTheRemoteSchedulerAndToldAsRemote() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    Map<String, CellData> row = new HashMap<>();
    row.put("EngInsTxt", new CellData("SCREEN_TEXT_SCHEDULER_ENGINE_INSTANCE_LOCAL"));
    DataList dataList = new DataList();
    dataList.addRow(row);
    given(remoteScheduler.getConfigurationEngine()).willReturn(new ServiceData().setDataList(dataList));
    SchedulerService schedulerService = mock(SchedulerService.class);

    ServiceData engine = service(schedulerService, remoteScheduler).getConfigurationEngine();

    assertThat(engine.getDataList().getRows().get(0).get("EngInsTxt").getStringValue())
      .isEqualTo("SCREEN_TEXT_SCHEDULER_ENGINE_INSTANCE_REMOTE");
    verify(schedulerService, never()).getConfigurationEngine();
  }

  @Test
  void theFactsOfARemoteEngineTellWhereItRuns() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    Map<String, CellData> row = new HashMap<>();
    row.put("EngSta", new CellData("running"));
    row.put("EngIns", new CellData("quartz-remote"));
    row.put("EngInsTxt", new CellData("En este servidor (no remoto)"));
    row.put("EngVer", new CellData("Quartz 2.5.2"));
    row.put("EngTil", new CellData("[]"));
    DataList dataList = new DataList();
    dataList.addRow(row);
    given(remoteScheduler.getConfigurationEngine()).willReturn(new ServiceData().setDataList(dataList));

    ServiceData engine = service(mock(SchedulerService.class), remoteScheduler).getConfigurationEngine();

    JsonNode tiles = new JsonMapper().readTree(engine.getDataList().getRows().get(0).get("EngTil").getStringValue());
    assertThat(tiles.get(0).get("value").asString()).isEqualTo("quartz-remote");
    assertThat(tiles.get(0).get("hint").asString()).isEqualTo("SCREEN_TEXT_SCHEDULER_ENGINE_INSTANCE_REMOTE");
  }

  @Test
  void aRemoteEngineThatCannotBeReachedIsToldAsNotAvailable() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    given(remoteScheduler.getConfigurationEngine()).willThrow(new IllegalStateException("unreachable"));
    SchedulerService schedulerService = mock(SchedulerService.class);
    ServiceData unavailable = new ServiceData();
    given(schedulerService.getConfigurationEngineUnavailable()).willReturn(unavailable);

    assertThat(service(schedulerService, remoteScheduler).getConfigurationEngine()).isSameAs(unavailable);
    verify(schedulerService, never()).getConfigurationEngine();
  }

  @Test
  void theSchedulesOfARemoteEngineThatCannotBeReachedAreEmptyAndTheirCountsZero() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    given(remoteScheduler.getConfigurationEngineTriggers("paused")).willThrow(new IllegalStateException("unreachable"));
    given(remoteScheduler.getConfigurationEngineTriggerCounts()).willThrow(new IllegalStateException("unreachable"));
    SchedulerService schedulerService = mock(SchedulerService.class);
    ServiceData counts = new ServiceData();
    given(schedulerService.getConfigurationEngineEmptyTriggerCounts()).willReturn(counts);
    RemoteSchedulerService service = service(schedulerService, remoteScheduler);

    ServiceData rows = service.getConfigurationEngineTriggers("paused");

    assertThat(rows.getDataList().getRows()).isEmpty();
    assertThat(rows.getDataList().getRecords()).isZero();
    assertThat(service.getConfigurationEngineTriggerCounts()).isSameAs(counts);
    verify(schedulerService, never()).getConfigurationEngineTriggers("paused");
    verify(schedulerService, never()).getConfigurationEngineTriggerCounts();
  }

  @Test
  void aFilterThatIsNotValidIsRefusedBeforeItReachesTheRemoteScheduler() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    SchedulerService schedulerService = mock(SchedulerService.class);
    willThrow(new InvalidExecutionFilterException("title", "message")).given(schedulerService)
      .validateConfigurationEngineFilter("blocked");

    assertThrows(InvalidExecutionFilterException.class,
      () -> service(schedulerService, remoteScheduler).getConfigurationEngineTriggers("blocked"));

    verify(remoteScheduler, never()).getConfigurationEngineTriggers(anyString());
  }

  @Test
  void theTabsOfTheConfigurationScreenCarryTheStateOfTheEngine() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    Map<String, CellData> row = new HashMap<>();
    row.put("EngStaTxt", new CellData("SCREEN_TEXT_SCHEDULER_ENGINE_STATUS_PAUSED"));
    DataList dataList = new DataList();
    dataList.addRow(row);
    given(remoteScheduler.getConfigurationEngine()).willReturn(new ServiceData().setDataList(dataList));
    SchedulerService schedulerService = mock(SchedulerService.class);
    ServiceData tabs = new ServiceData();
    given(schedulerService.getConfigurationTabs("SCREEN_TEXT_SCHEDULER_ENGINE_STATUS_PAUSED")).willReturn(tabs);

    assertThat(service(schedulerService, remoteScheduler).getConfigurationTabs()).isSameAs(tabs);
  }

  @Test
  void anEngineThatCannotBeReadLeavesTheTabsWithoutItsState() throws Exception {
    RemoteScheduler remoteScheduler = mock(RemoteScheduler.class);
    given(remoteScheduler.getConfigurationEngine()).willThrow(new IllegalStateException("unreachable"));
    SchedulerService schedulerService = mock(SchedulerService.class);
    ServiceData tabs = new ServiceData();
    given(schedulerService.getConfigurationTabs(null)).willReturn(tabs);

    assertThat(service(schedulerService, remoteScheduler).getConfigurationTabs()).isSameAs(tabs);
  }

  private static RemoteSchedulerService service(SchedulerService schedulerService, RemoteScheduler remoteScheduler) {
    RemoteSchedulerService service = new RemoteSchedulerService(schedulerService, remoteScheduler, new JsonMapper(),
      true);
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    given(context.getBean(AweElements.class)).willReturn(elements);
    given(elements.getLanguage()).willReturn("es");
    given(elements.getLocaleWithLanguage(anyString(), anyString())).willAnswer(call -> call.getArgument(0));
    service.setApplicationContext(context);
    return service;
  }
}
