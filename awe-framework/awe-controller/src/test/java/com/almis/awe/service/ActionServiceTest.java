package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWEUnauthorizedException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.Action;
import com.almis.awe.model.entities.actions.Answer;
import com.almis.awe.model.entities.actions.Call;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.actions.Parameter;
import com.almis.awe.model.entities.services.Service;
import com.almis.awe.security.authorization.TargetKind;
import com.almis.awe.security.authorization.TargetLaunchAuthorizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionServiceTest {

  @Mock
  private LauncherService launcherService;

  @Mock
  private TargetLaunchAuthorizer authorizer;

  private AweRequest request;
  private TestableActionService actionService;

  @BeforeEach
  void setUp() {
    request = new AweRequest(mock(HttpServletRequest.class), mock(HttpServletResponse.class), new JsonMapper());
    actionService = new TestableActionService(launcherService, new BaseConfigProperties(), authorizer);
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

  private static Action action(String serviceId) {
    return Action.builder().id("A1").call(new Call().setService(serviceId)).build();
  }

  private AweElements givenService(Service service) throws AWException {
    ApplicationContext context = mock(ApplicationContext.class);
    AweElements elements = mock(AweElements.class);
    when(context.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getService(service.getId())).thenReturn(service);
    actionService.setApplicationContext(context);
    return elements;
  }

  @Test
  void aServiceRefusedByTheAuthorizersIsNeverCalled() throws Exception {
    AweElements elements = givenService(Service.builder().id("UsrExp").permission("users.read").build());
    when(elements.getLocaleWithLanguage(anyString(), any())).thenAnswer(call -> call.getArgument(0));
    when(authorizer.authorize("UsrExp", TargetKind.SERVICE)).thenReturn(false);

    assertThatThrownBy(() -> actionService.launchAction(action("UsrExp")))
      .isInstanceOf(AWEUnauthorizedException.class)
      .hasMessage(AWEUnauthorizedException.MESSAGE_KEY);
    verify(launcherService, never()).callService(any(), any());
  }

  @Test
  void aServiceAllowedByTheAuthorizersIsCalled() throws Exception {
    givenService(Service.builder().id("UsrExp").permission("users.read").build());
    when(authorizer.authorize("UsrExp", TargetKind.SERVICE)).thenReturn(true);
    when(launcherService.callService("UsrExp", null)).thenReturn(new ServiceData());

    actionService.launchAction(action("UsrExp"));

    verify(launcherService).callService("UsrExp", null);
  }

  @Test
  void aServiceAuthorizedByItsTargetIsNotCheckedItself() throws Exception {
    givenService(Service.builder().id("launchQuery").authorization("target").build());
    when(launcherService.callService("launchQuery", null)).thenReturn(new ServiceData());

    actionService.launchAction(action("launchQuery"));

    verifyNoInteractions(authorizer);
    verify(launcherService).callService("launchQuery", null);
  }

  @Test
  void shouldAnswerAnAuthorizationRefusalWithItsMessageInsteadOfTheErrorAnswerOfTheAction() {
    actionService.setDefaultError(defaultErrorWithMessage());

    List<ClientAction> actionList = actionService.launchError(actionWithOwnErrorAnswer(),
      new AWEUnauthorizedException("Refused title", "Refused message"));

    assertThat(actionList).extracting(ClientAction::getType).containsExactly("end-load", "message");
    assertThat(actionList.get(1).getParameters()).extractingByKeys("type", "title", "message")
      .map(value -> ((CellData) value).getStringValue())
      .containsExactly("error", "Refused title", "Refused message");
  }

  @Test
  void shouldAnswerAnyOtherErrorWithTheErrorAnswerOfTheAction() {
    actionService.setDefaultError(defaultErrorWithMessage());

    List<ClientAction> actionList = actionService.launchError(actionWithOwnErrorAnswer(),
      new AWException("Title", "Message"));

    assertThat(actionList).extracting(ClientAction::getType).containsExactly("validate");
  }

  private static Action actionWithOwnErrorAnswer() {
    return Action.builder().id("control-unique-cancel")
      .answers(List.of(Answer.builder().type("error")
        .responseList(List.of(ClientAction.builder().type("validate").build())).build()))
      .build();
  }

  private static Action defaultErrorWithMessage() {
    ClientAction message = ClientAction.builder().type("message").parameterList(List.of(
      Parameter.builder().name("type").variable("MESSAGE_TYPE").build(),
      Parameter.builder().name("title").variable("MESSAGE_TITLE").build(),
      Parameter.builder().name("message").variable("MESSAGE_DESCRIPTION").build())).build();
    return Action.builder().id("DEFAULT_ERROR")
      .answers(List.of(Answer.builder().type("error")
        .responseList(List.of(ClientAction.builder().type("end-load").build(), message)).build()))
      .build();
  }

  static class TestableActionService extends ActionService {
    private AweRequest testRequest;
    private Action defaultError;

    TestableActionService(LauncherService launcherService, BaseConfigProperties baseConfigProperties,
                          TargetLaunchAuthorizer authorizer) {
      super(launcherService, baseConfigProperties, authorizer);
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
