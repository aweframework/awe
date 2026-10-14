package com.almis.awe.service.observation;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.DatabaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Answer;
import com.almis.awe.model.entities.actions.Call;
import com.almis.awe.model.entities.maintain.Target;
import com.almis.awe.model.entities.queries.Query;
import com.almis.awe.service.ActionService;
import com.almis.awe.service.LauncherService;
import com.almis.awe.service.MaintainService;
import com.almis.awe.service.data.connector.maintain.MaintainLauncher;
import com.almis.awe.service.data.connector.query.EnumQueryConnector;
import com.almis.awe.service.data.connector.query.QueryConnector;
import com.almis.awe.service.data.connector.query.QueryLauncher;
import com.almis.awe.service.data.connector.query.QueueQueryConnector;
import com.almis.awe.service.data.connector.query.SQLQueryConnector;
import com.almis.awe.service.data.connector.query.ServiceQueryConnector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The execution points of queries, maintains and actions report to the {@link ExecutionObserver}, and behave
 * exactly as before when there is none.
 */
class ExecutionObserverTest {

  private ApplicationContext context;
  private RecordingObserver observer;

  /**
   * Observer that records what it is told and runs the execution
   */
  static class RecordingObserver implements ExecutionObserver {
    record Call(ExecutionObserver.ExecutionType type, String id, Map<String, String> tags) {
    }

    final List<Call> calls = new ArrayList<>();

    @Override
    public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException {
      calls.add(new Call(type, id, tags));
      return execution.execute();
    }
  }

  @BeforeEach
  void setUp() {
    context = mock(ApplicationContext.class);
    observer = new RecordingObserver();
  }

  private QueryLauncher queryLauncher() {
    QueryLauncher launcher = new QueryLauncher();
    launcher.setApplicationContext(context);
    return launcher;
  }

  private <T extends QueryConnector> void connector(Class<T> type) throws AWException {
    T connector = mock(type);
    when(connector.launch(any(), any())).thenReturn(new ServiceData());
    when(context.getBean(type)).thenReturn(connector);
  }

  @Test
  @DisplayName("The default observer just runs the execution and lets its exception through")
  void noneRunsTheExecution() throws AWException {
    assertThat(ExecutionObserver.NONE.observe(ExecutionObserver.ExecutionType.QUERY, "Q1", Map.of(), () -> "result"))
      .isEqualTo("result");

    AWException failure = new AWException("title", "message");
    assertThatThrownBy(() -> ExecutionObserver.NONE.observe(ExecutionObserver.ExecutionType.QUERY, "Q1", Map.of(), () -> {
      throw failure;
    })).isSameAs(failure);
  }

  @Test
  @DisplayName("A query is reported with its id and the connector that runs it")
  void queriesAreObserved() throws AWException {
    connector(SQLQueryConnector.class);
    connector(ServiceQueryConnector.class);
    connector(EnumQueryConnector.class);
    connector(QueueQueryConnector.class);
    QueryLauncher launcher = queryLauncher();
    launcher.setExecutionObserver(observer);

    launcher.launchQuery(new Query().setId("Q1"), null);
    launcher.launchQuery(new Query().setId("Q2").setService("S"), null);
    launcher.launchQuery(new Query().setId("Q3").setEnumerated("E"), null);
    launcher.launchQuery(new Query().setId("Q4").setQueue("Q"), null);

    assertThat(observer.calls).containsExactly(
      new RecordingObserver.Call(ExecutionObserver.ExecutionType.QUERY, "Q1", Map.of("connector", "sql")),
      new RecordingObserver.Call(ExecutionObserver.ExecutionType.QUERY, "Q2", Map.of("connector", "service")),
      new RecordingObserver.Call(ExecutionObserver.ExecutionType.QUERY, "Q3", Map.of("connector", "enum")),
      new RecordingObserver.Call(ExecutionObserver.ExecutionType.QUERY, "Q4", Map.of("connector", "queue")));
  }

  @Test
  @DisplayName("A query without observer runs as before")
  void queriesWithoutObserver() throws AWException {
    connector(SQLQueryConnector.class);

    assertThat(queryLauncher().launchQuery(new Query().setId("Q1"), null)).isNotNull();
  }

  @Test
  @DisplayName("A query that fails reaches the observer and the caller")
  void failingQuery() throws AWException {
    AWException failure = new AWException("title", "message");
    SQLQueryConnector sql = mock(SQLQueryConnector.class);
    when(sql.launch(any(), any())).thenThrow(failure);
    when(context.getBean(SQLQueryConnector.class)).thenReturn(sql);
    QueryLauncher launcher = queryLauncher();
    launcher.setExecutionObserver(observer);

    assertThatThrownBy(() -> launcher.launchQuery(new Query().setId("Q1"), null)).isSameAs(failure);
    assertThat(observer.calls).hasSize(1);
  }

  private MaintainService maintainService() throws AWException {
    MaintainService service = new MaintainService(mock(MaintainLauncher.class), null, mock(DatabaseConfigProperties.class));
    service.setApplicationContext(context);
    AweElements elements = mock(AweElements.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getLocaleWithLanguage(anyString(), any())).thenReturn("msg");
    return service;
  }

  @Test
  @DisplayName("A maintain is reported once, with the name of its target, whichever overload started it")
  void maintainsAreObserved() throws AWException {
    MaintainService service = maintainService();
    service.setExecutionObserver(observer);
    Target target = new Target();
    target.setName("M1");
    target.setQueryList(Collections.emptyList());

    service.launchMaintain(target, JsonNodeFactory.instance.objectNode(), null, false);

    assertThat(observer.calls).containsExactly(
      new RecordingObserver.Call(ExecutionObserver.ExecutionType.MAINTAIN, "M1", Map.of()));
  }

  @Test
  @DisplayName("A maintain without observer runs as before")
  void maintainsWithoutObserver() throws AWException {
    Target target = new Target();
    target.setName("M1");
    target.setQueryList(Collections.emptyList());

    assertThat(maintainService().launchMaintain(target, JsonNodeFactory.instance.objectNode(), null, false)).isNotNull();
  }

  private ActionService actionService(LauncherService launcherService) {
    ActionService service = new ActionService(launcherService, new BaseConfigProperties());
    service.setApplicationContext(context);
    return service;
  }

  @Test
  @DisplayName("An action is reported with its id, also when it is launched by id")
  void actionsAreObserved() throws AWException {
    LauncherService launcherService = mock(LauncherService.class);
    when(launcherService.callService(any(), any())).thenReturn(new ServiceData());
    ActionService service = actionService(launcherService);
    service.setExecutionObserver(observer);
    Action action = Action.builder().id("A1").call(new Call().setService("S")).build();

    service.launchAction(action);

    assertThat(observer.calls).containsExactly(
      new RecordingObserver.Call(ExecutionObserver.ExecutionType.ACTION, "A1", Map.of()));
  }

  @Test
  @DisplayName("An action that fails is reported with its error even if the launch by id turns it into an error answer")
  void failingActionLaunchedById() throws AWException {
    LauncherService launcherService = mock(LauncherService.class);
    AWException failure = new AWException("title", "message");
    when(launcherService.callService(any(), any())).thenThrow(failure);
    List<Throwable> seen = new ArrayList<>();
    ExecutionObserver failingObserver = new ExecutionObserver() {
      @Override
      public <T> T observe(ExecutionType type, String id, Map<String, String> tags, Execution<T> execution) throws AWException {
        try {
          return execution.execute();
        } catch (AWException exc) {
          seen.add(exc);
          throw exc;
        }
      }
    };
    Action action = Action.builder().id("A1").call(new Call().setService("S"))
      .answers(List.of(Answer.builder().type("error").responseList(List.of()).build())).build();
    AweRequest request = new AweRequest(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new ObjectMapper());
    ActionService service = new ActionService(launcherService, new BaseConfigProperties()) {
      @Override
      public Action getAction(String actionId) {
        return action;
      }

      @Override
      public AweRequest getRequest() {
        return request;
      }
    };
    service.setApplicationContext(context);
    service.setExecutionObserver(failingObserver);

    service.launchAction("A1");

    assertThat(seen).containsExactly(failure);
  }
}
