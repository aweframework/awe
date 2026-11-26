package com.almis.awe.scheduler.service;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.scheduler.dao.CalendarDAO;
import com.almis.awe.scheduler.dao.SchedulerDAO;
import com.almis.awe.scheduler.dao.TaskDAO;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.Engine;
import com.almis.awe.scheduler.dao.task.SchedulerEnginePresenter.EngineStatus;
import com.almis.awe.scheduler.exception.InvalidExecutionFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * «Motor» tab of the configuration screen read where the engine runs (#993): the card, the schedules and the counts of
 * their state filter degrade when the engine cannot be read (the card tells it, the schedules are empty and the counts
 * are zero) instead of failing, and the card is not blanked when only its schedules cannot be read
 */
@ExtendWith(MockitoExtension.class)
class SchedulerServiceEngineTest {

  @Mock
  private TaskDAO taskDAO;
  @Mock
  private SchedulerDAO schedulerDAO;
  @Mock
  private CalendarDAO calendarDAO;
  @Mock
  private SchedulerExecutionCompletionService completionService;

  private SchedulerService schedulerService;

  @BeforeEach
  void initService() {
    schedulerService = new SchedulerService(taskDAO, schedulerDAO, calendarDAO, completionService, false, false);
  }

  @Test
  void theCardTellsTheEngineWhenOnlyItsSchedulesCannotBeRead() throws Exception {
    Engine engine = new Engine("quartz", "2.5.2", "store", false, false, "pool", 10, 0, null, 0, EngineStatus.RUNNING,
      false, false);
    given(schedulerDAO.getEngine()).willReturn(engine);
    given(schedulerDAO.getEngineTriggers()).willThrow(new AWException("schedules unreadable"));
    ServiceData card = new ServiceData();
    given(taskDAO.getConfigurationEngine(engine, List.of())).willReturn(card);

    assertThat(schedulerService.getConfigurationEngine()).isSameAs(card);
  }

  @Test
  void theCardOfAnEngineThatCannotBeReadSaysSo() throws Exception {
    given(schedulerDAO.getEngine()).willThrow(new AWException("engine unreadable"));
    ServiceData card = new ServiceData();
    given(taskDAO.getConfigurationEngine(null, List.of())).willReturn(card);

    assertThat(schedulerService.getConfigurationEngine()).isSameAs(card);
    assertThat(schedulerService.getConfigurationEngineUnavailable()).isSameAs(card);
  }

  @Test
  void theSchedulesOfAnEngineThatCannotBeReadAreEmptyAndTheirCountsZero() throws Exception {
    given(schedulerDAO.getEngineTriggers()).willThrow(new IllegalStateException("engine unreadable"));
    ServiceData rows = new ServiceData();
    ServiceData counts = new ServiceData();
    given(taskDAO.getConfigurationEngineTriggers(List.of(), "paused")).willReturn(rows);
    given(taskDAO.getConfigurationEngineTriggerCounts(List.of())).willReturn(counts);

    assertThat(schedulerService.getConfigurationEngineTriggers("paused")).isSameAs(rows);
    assertThat(schedulerService.getConfigurationEngineTriggerCounts()).isSameAs(counts);
    assertThat(schedulerService.getConfigurationEngineEmptyTriggerCounts()).isSameAs(counts);
  }

  @Test
  void aFilterThatIsNotValidIsStillRefusedBeforeTheEngineIsRead() throws Exception {
    willThrow(new InvalidExecutionFilterException("title", "message")).given(taskDAO)
      .validateConfigurationEngineFilter("blocked");

    assertThrows(InvalidExecutionFilterException.class,
      () -> schedulerService.getConfigurationEngineTriggers("blocked"));

    verify(schedulerDAO, never()).getEngineTriggers();
  }
}
