package com.almis.awe.service;

import com.almis.awe.builder.enumerates.InitialLoad;
import com.almis.awe.builder.screen.DialogBuilder;
import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.builder.screen.TagBuilder;
import com.almis.awe.builder.screen.criteria.SelectCriteriaBuilder;
import com.almis.awe.builder.screen.criteria.TextCriteriaBuilder;
import com.almis.awe.exception.AWENotFoundException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.model.type.AnswerType;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class used for testing ScreenService class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class ScreenServiceTest {

  private static final String QUERY_FAILED_TITLE = "Query failed";
  private static final String QUERY_FAILED_MESSAGE = "The query of Options failed";
  private static final String MESSAGE_ACTION = "message";
  private static final String DEFERRED_FAILED_ACTION = "deferred-failed";

  @InjectMocks
  private ScreenService screenService;

  @Mock
  private MenuService menuService;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweClientTracker aweClientTracker;

  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  @Mock
  private ScreenComponentGenerator screenComponentGenerator;

  @Mock
  private TemplateService templateService;

  /**
   * Initializes json mapper for tests
   */
  @BeforeEach
  void setUp() {
    screenService.setApplicationContext(applicationContext);
  }

  /**
   * Check null screen
   */
  @Test
  void getScreenElementListWithNull() {
    assertThrows(NullPointerException.class, () -> screenService.getScreenElementList(null, null));
  }

  /**
   * Check null screen
   */
  @Test
  void getScreenDataForReact() throws Exception {
    when(menuService.getMenu()).thenReturn(new Menu());
    when(menuService.getAvailableOptionScreen(eq("MatTst"), any(Menu.class))).thenReturn(new ScreenBuilder().setId("test-screen").build());
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweSession.getUser()).thenReturn("testUser");
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(applicationContext.getBean(AweClientTracker.class)).thenReturn(aweClientTracker);
    when(aweRequest.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());

    ScreenData screenData = screenService.getScreenData("MatTst", false);
    assertNotNull(screenData);
  }

  /**
   * Check null screen
   */
  @Test
  void getScreenDataForAngular() throws Exception {
    when(menuService.getMenu()).thenReturn(new Menu());
    when(menuService.getDefaultScreen(any(Menu.class))).thenReturn(new ScreenBuilder().setId("default-screen").build());
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweSession.getUser()).thenReturn("testUser");
    when(aweSession.isAuthenticated()).thenReturn(true);
    when(applicationContext.getBean(AweClientTracker.class)).thenReturn(aweClientTracker);
    when(aweRequest.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());

    ScreenData screenData = screenService.getScreenData(null, true);

    assertNotNull(screenData);
  }

  private Screen buildDeferredScreen() throws Exception {
    return new ScreenBuilder().setId("deferred-screen").addTag(new TagBuilder().setSource("center").setType("div")
      .addDialog(new DialogBuilder().setId("Dia").setDeferred(true)
        .addCriteria(new SelectCriteriaBuilder().setId("Options").setInitialLoad(InitialLoad.QUERY).setTargetAction("OptionsQuery"))
        .addCriteria(new TextCriteriaBuilder().setId("Plain")))
      .addDialog(new DialogBuilder().setId("Eager"))).build();
  }

  private void prepareDeferredRequest(String containerId, Map<String, JsonNode> parameters) throws Exception {
    lenient().when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    lenient().when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    lenient().when(aweRequest.getTargetAction()).thenReturn(containerId);
    lenient().when(aweRequest.getParameterAsString("option")).thenReturn("MatTst");
    lenient().when(aweRequest.getParameter(any())).thenAnswer(invocation -> parameters.get((String) invocation.getArgument(0)));
    lenient().when(aweRequest.getParameterList()).thenReturn(JsonNodeFactory.instance.objectNode());
    lenient().when(menuService.getMenu()).thenReturn(new Menu());
    lenient().when(menuService.getAvailableOptionScreen(eq("MatTst"), any(Menu.class))).thenReturn(buildDeferredScreen());
  }

  private static ScreenComponent loadedComponent(String id, int rows, List<CellData> selected) throws Exception {
    ComponentModel model = new ComponentModel().setPage(1L).setTotal(1L).setRecords((long) rows).setSelected(selected);
    List<Map<String, CellData>> values = new ArrayList<>();
    for (int i = 0; i < rows; i++) {
      values.add(Map.of("value", new CellData(i), "label", new CellData("L" + i)));
    }
    model.setValues(values);
    return new ScreenComponent().setId(id).setModel(model)
      .setController(new SelectCriteriaBuilder().setId(id).setInitialLoad(InitialLoad.QUERY).setTargetAction("Q").build());
  }

  /**
   * The first opening of a deferred container returns the model of its components as initial-model actions (#824)
   */
  @Test
  void getDeferredDataReturnsTheInitialModelOfTheComponents() throws Exception {
    prepareDeferredRequest("Dia", Map.of());
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenReturn(Map.of("Options", loadedComponent("Options", 2, List.of(new CellData("1")))));

    ServiceData serviceData = screenService.getDeferredDataAction();

    assertEquals(1, serviceData.getClientActionList().size());
    ClientAction action = serviceData.getClientActionList().get(0);
    assertEquals("initial-model", action.getType());
    assertEquals("Options", action.getTarget());
    DataList datalist = (DataList) ((CellData) action.getParameters().get("datalist")).getObjectValue();
    assertEquals(2, datalist.getRows().size());
    assertEquals(2L, datalist.getRecords());
    assertTrue(action.getParameters().containsKey("selected"));
    assertTrue(action.getParameters().containsKey("defaultValues"));
    assertTrue(serviceData.getClientActionList().stream().noneMatch(a -> "deferred-failed".equals(a.getType())));
  }

  /**
   * Components which already have a value are not given the initial one (#824)
   */
  @Test
  void getDeferredDataKeepsTheValuesAlreadySet() throws Exception {
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    prepareDeferredRequest("Dia", Map.of("deferredValued", parameters.arrayNode().add("Options"),
      "deferredLoaded", parameters.arrayNode().add("Plain")));
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenReturn(Map.of("Options", loadedComponent("Options", 2, List.of(new CellData("1")))));

    ServiceData serviceData = screenService.getDeferredDataAction();

    ClientAction action = serviceData.getClientActionList().get(0);
    assertTrue(action.getParameters().containsKey("datalist"));
    assertFalse(action.getParameters().containsKey("selected"));
    assertFalse(action.getParameters().containsKey("defaultValues"));
    ArgumentCaptor<Set<String>> loaded = ArgumentCaptor.forClass(Set.class);
    verify(screenComponentGenerator).generateDeferredComponentMap(any(), any(), any(), any(), any(), loaded.capture());
    assertEquals(Set.of("Plain"), loaded.getValue());
  }

  /**
   * A failing initial load of a deferred container reaches the client as a message, like it does with the screen load (#824)
   */
  @Test
  void getDeferredDataReturnsTheErrorsOfTheInitialLoads() throws Exception {
    prepareDeferredRequest("Dia", Map.of());
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        ScreenData data = invocation.getArgument(2);
        data.addError(new AWException(QUERY_FAILED_TITLE, QUERY_FAILED_MESSAGE));
        return Map.of();
      });

    ServiceData serviceData = screenService.getDeferredDataAction();

    ClientAction message = serviceData.getClientActionList().stream()
      .filter(action -> MESSAGE_ACTION.equals(action.getType())).findFirst().orElseThrow();
    assertEquals(QUERY_FAILED_TITLE, ((CellData) message.getParameters().get("title")).getStringValue());
    assertEquals(QUERY_FAILED_MESSAGE, ((CellData) message.getParameters().get(MESSAGE_ACTION)).getStringValue());
  }

  /**
   * A failed initial load tells the client the container was not loaded, so it is requested again on the next opening (#824)
   */
  @Test
  void getDeferredDataSignalsTheFailureOfTheContainer() throws Exception {
    prepareDeferredRequest("Dia", Map.of());
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        ScreenData data = invocation.getArgument(2);
        data.addError(new AWException(QUERY_FAILED_TITLE, QUERY_FAILED_MESSAGE));
        return Map.of();
      });

    ServiceData serviceData = screenService.getDeferredDataAction();

    ClientAction failed = serviceData.getClientActionList().stream()
      .filter(action -> DEFERRED_FAILED_ACTION.equals(action.getType())).findFirst().orElseThrow();
    assertEquals("Dia", failed.getTarget());
    assertTrue(serviceData.getClientActionList().stream().anyMatch(action -> MESSAGE_ACTION.equals(action.getType())));
  }

  /**
   * A warning or an information message of an initial load is not a failure: the container stays loaded (#824)
   */
  @Test
  void getDeferredDataDoesNotSignalAFailureForAWarning() throws Exception {
    prepareDeferredRequest("Dia", Map.of());
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        ScreenData data = invocation.getArgument(2);
        data.addError(new AWException(QUERY_FAILED_TITLE, QUERY_FAILED_MESSAGE, AnswerType.WARNING));
        return Map.of();
      });

    ServiceData serviceData = screenService.getDeferredDataAction();

    assertTrue(serviceData.getClientActionList().stream().anyMatch(action -> MESSAGE_ACTION.equals(action.getType())));
    assertTrue(serviceData.getClientActionList().stream().noneMatch(action -> DEFERRED_FAILED_ACTION.equals(action.getType())));
  }

  /**
   * A message without a type is not known to be harmless: it counts as a failure (#824)
   */
  @Test
  void getDeferredDataSignalsAFailureForAMessageWithoutType() throws Exception {
    prepareDeferredRequest("Dia", Map.of());
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        ScreenData data = invocation.getArgument(2);
        data.addAction(new ClientAction(MESSAGE_ACTION).addParameter(MESSAGE_ACTION, QUERY_FAILED_MESSAGE));
        return Map.of();
      });

    ServiceData serviceData = screenService.getDeferredDataAction();

    assertTrue(serviceData.getClientActionList().stream().anyMatch(action -> DEFERRED_FAILED_ACTION.equals(action.getType())));
  }

  /**
   * A message whose type is a plain text value is read as well: a warning is not a failure (#824)
   */
  @Test
  void getDeferredDataReadsAMessageTypeStoredAsText() throws Exception {
    prepareDeferredRequest("Dia", Map.of());
    when(screenComponentGenerator.generateDeferredComponentMap(any(), any(), any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        ScreenData data = invocation.getArgument(2);
        ClientAction warning = new ClientAction(MESSAGE_ACTION);
        warning.getParameters().put(AweConstants.ACTION_MESSAGE_TYPE_ATTRIBUTE, "warning");
        data.addAction(warning);
        return Map.of();
      });

    ServiceData serviceData = screenService.getDeferredDataAction();

    assertTrue(serviceData.getClientActionList().stream().noneMatch(action -> DEFERRED_FAILED_ACTION.equals(action.getType())));
  }

  /**
   * Only deferred containers can be loaded on demand (#824)
   */
  @Test
  void getDeferredDataRejectsContainersWhichAreNotDeferred() throws Exception {
    prepareDeferredRequest("Eager", Map.of());
    AweElements elements = mock(AweElements.class);
    lenient().when(applicationContext.getBean(AweElements.class)).thenReturn(elements);
    lenient().when(elements.getLocaleWithLanguage(any(), any(), any(Object[].class))).thenReturn("not found");

    assertThrows(AWENotFoundException.class, () -> screenService.getDeferredDataAction());
  }
}
