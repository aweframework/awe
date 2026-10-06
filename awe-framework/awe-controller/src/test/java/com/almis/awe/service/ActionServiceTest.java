package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Answer;
import com.almis.awe.model.entities.actions.ClientAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

  @Mock
  private LauncherService launcherService;

  private AweRequest request;
  private TestableActionService actionService;

  @BeforeEach
  void setUp() {
    request = new AweRequest(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new ObjectMapper());
    actionService = new TestableActionService(launcherService, new BaseConfigProperties());
    actionService.setTestRequest(request);
  }

  @Test
  void shouldFormatScalarStringsWithQuotesAndPreserveJsonStructures() {
    request.setParameter("selectedIds", "10", "20");
    request.setParameter("pwd_usr", "secret");
    request.setParameter("description", "contains secret value");
    request.setParameter("status", "active");
    request.setParameter("empty", "");
    request.setParameter("attempts", 3);

    String formattedParameters = ReflectionTestUtils.invokeMethod(actionService, "getParameterListAsString");

    assertThat(formattedParameters)
      .contains("selectedIds=[\"10\",\"20\"]")
      .contains("pwd_usr=*****")
      .contains("description=\"contains ***** value\"")
      .contains("status=\"active\"")
      .contains("empty=\"\"")
      .contains("attempts=3");
  }

  @Test
  void shouldLaunchTheDefaultErrorWhenTheActionHasNoAnswerForTheErrorType() {
    actionService.setDefaultError(Action.builder().id("DEFAULT_ERROR")
      .answers(List.of(Answer.builder().type("error")
        .responseList(List.of(ClientAction.builder().type("end-load").build())).build()))
      .build());

    List<ClientAction> actionList = actionService.launchError(new Action(), new AWException("Title", "Message"));

    assertThat(actionList).extracting(ClientAction::getType).containsExactly("end-load");
  }

  @Test
  void shouldReturnEmptyActionListWhenTheDefaultErrorHasNoAnswerForTheErrorType() {
    Action defaultError = Action.builder().id("DEFAULT_ERROR").build();
    actionService.setDefaultError(defaultError);

    List<ClientAction> actionList = actionService.launchError(defaultError, new AWException("Title", "Message"));

    assertThat(actionList).isEmpty();
  }

  static class TestableActionService extends ActionService {
    private AweRequest testRequest;
    private Action defaultError;

    TestableActionService(LauncherService launcherService, BaseConfigProperties baseConfigProperties) {
      super(launcherService, baseConfigProperties);
    }

    void setTestRequest(AweRequest testRequest) {
      this.testRequest = testRequest;
    }

    void setDefaultError(Action defaultError) {
      this.defaultError = defaultError;
    }

    @Override
    public Action getAction(String actionId) {
      return defaultError;
    }

    @Override
    public AweRequest getRequest() {
      return testRequest;
    }
  }
}
