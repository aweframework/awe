package com.almis.awe.service.observation;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Call;
import com.almis.awe.model.entities.maintain.Insert;
import com.almis.awe.model.entities.maintain.MaintainQuery;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.ActionService;
import com.almis.awe.service.LauncherService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.service.data.connector.query.SQLQueryConnector;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Each execution tells the observer which answer type it ended with, so the observer can tell a real failure from
 * a business message, also for actions, whose result is a list of client actions.
 */
class ExecutionOutcomeTest {

  /**
   * Observer that keeps the answer type of every result
   */
  static class AnswerTypeObserver implements ExecutionObserver {
    final List<AnswerType> answerTypes = new ArrayList<>();

    @Override
    public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException {
      return execution.execute();
    }

    @Override
    public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution,
                         Function<? super T, AnswerType> answerType) throws AWException {
      T result = execution.execute();
      answerTypes.add(answerType.apply(result));
      return result;
    }
  }

  @Test
  @DisplayName("The default five-argument method of an observer that only knows the old one still runs the execution")
  void defaultDelegates() throws AWException {
    ExecutionObserver old = new ExecutionObserver() {
      @Override
      public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException {
        return execution.execute();
      }
    };

    assertThat(old.observe(ExecutionObserver.ExecutionType.QUERY, "Q", Map.of(), () -> "ok", result -> AnswerType.OK)).isEqualTo("ok");
  }

  @Test
  @DisplayName("A query tells the answer type of its data")
  void queryAnswerType() throws AWException {
    ApplicationContext context = mock(ApplicationContext.class);
    SQLQueryConnector connector = mock(SQLQueryConnector.class);
    when(connector.launch(any(), any())).thenReturn(new ServiceData().setType(AnswerType.WARNING));
    when(context.getBean(SQLQueryConnector.class)).thenReturn(connector);
    QueryLauncher launcher = new QueryLauncher();
    launcher.setApplicationContext(context);
    AnswerTypeObserver observer = new AnswerTypeObserver();
    launcher.setExecutionObserver(observer);

    launcher.launchQuery(new Query().setId("Q1"), null);

    assertThat(observer.answerTypes).containsExactly(AnswerType.WARNING);
  }

  private MaintainService maintainService(MaintainLauncher launcher, AnswerTypeObserver observer) {
    MaintainService service = new MaintainService(launcher, null, mock(DatabaseConfigProperties.class));
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    service.setApplicationContext(context);
    service.setExecutionObserver(observer);
    return service;
  }

  @Test
  @DisplayName("A maintain tells the answer type of its output: OK for an empty target")
  void maintainAnswerType() throws AWException {
    AnswerTypeObserver observer = new AnswerTypeObserver();
    MaintainService service = maintainService(mock(MaintainLauncher.class), observer);
    Target target = new Target();
    target.setName("M1");
    target.setQueryList(Collections.emptyList());

    ServiceData result = service.launchMaintain(target, JsonNodeFactory.instance.objectNode(), null, false);

    assertThat(result.getType()).isEqualTo(AnswerType.OK);
    assertThat(observer.answerTypes).containsExactly(AnswerType.OK);
  }

  @Test
  @DisplayName("A maintain whose query answers an error tells ERROR")
  void maintainErrorAnswerType() throws AWException {
    MaintainLauncher launcher = mock(MaintainLauncher.class);
    when(launcher.launchMaintain(any(MaintainQuery.class), any(), any())).thenReturn(new ServiceData().setType(AnswerType.ERROR));
    AnswerTypeObserver observer = new AnswerTypeObserver();
    MaintainService service = maintainService(launcher, observer);
    Insert insert = new Insert();
    insert.setVariableDefinitionList(Collections.emptyList());
    Target target = new Target();
    target.setName("M1");
    target.setQueryList(List.of(insert));

    ServiceData result = service.launchMaintain(target, JsonNodeFactory.instance.objectNode(), null, false);

    assertThat(result.getType()).isEqualTo(AnswerType.ERROR);
    assertThat(observer.answerTypes).containsExactly(AnswerType.ERROR);
  }

  @Test
  @DisplayName("An action tells the answer type its service ended with, though it returns client actions")
  void actionAnswerType() throws AWException {
    LauncherService launcherService = mock(LauncherService.class);
    when(launcherService.callService(any(), any())).thenReturn(new ServiceData().setType(AnswerType.ERROR));
    ActionService service = new ActionService(launcherService, new BaseConfigProperties());
    service.setApplicationContext(mock(ApplicationContext.class));
    AnswerTypeObserver observer = new AnswerTypeObserver();
    service.setExecutionObserver(observer);

    service.launchAction(Action.builder().id("A1").call(new Call().setService("S")).build());

    assertThat(observer.answerTypes).containsExactly(AnswerType.ERROR);
  }

  @Test
  @DisplayName("An action whose service answers nothing has no answer type")
  void actionWithoutServiceData() throws AWException {
    LauncherService launcherService = mock(LauncherService.class);
    when(launcherService.callService(any(), any())).thenReturn(null);
    ActionService service = new ActionService(launcherService, new BaseConfigProperties());
    service.setApplicationContext(mock(ApplicationContext.class));
    AnswerTypeObserver observer = new AnswerTypeObserver();
    service.setExecutionObserver(observer);

    service.launchAction(Action.builder().id("A1").call(new Call().setService("S")).build());

    assertThat(observer.answerTypes).containsExactly((AnswerType) null);
  }
}
