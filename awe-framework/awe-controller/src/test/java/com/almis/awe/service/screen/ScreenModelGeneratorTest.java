package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.service.GridLayoutService;
import com.almis.awe.service.WindowStateService;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.almis.awe.model.constant.AweConstants.ACTION_DATA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScreenModelGeneratorTest {

  @Test
  void shouldKeepSingleSelectDefaultValueWhenInitialLoadIsValue() throws Exception {
    ScreenModelGenerator generator = new ScreenModelGenerator(mock(InitialLoadDao.class), mock(BaseConfigProperties.class));
    ScreenComponent screenComponent = buildScreenComponent("select", "value", "1");
    Future<ServiceData> futureData = CompletableFuture.completedFuture(buildServiceData(List.of("2", "3", "4")));

    invokeStoreDataInComponent(generator, futureData, screenComponent);

    assertEquals(List.of(new CellData("2")), screenComponent.getModel().getSelected());
    assertEquals(List.of(new CellData("2")), screenComponent.getModel().getDefaultValues());
    assertEquals(3, screenComponent.getModel().getValues().size());
  }

  @Test
  void shouldPopulateMultipleSelectFromReturnedValuesWhenInitialLoadIsValue() throws Exception {
    ScreenModelGenerator generator = new ScreenModelGenerator(mock(InitialLoadDao.class), mock(BaseConfigProperties.class));
    ScreenComponent screenComponent = buildScreenComponent("select-multiple", "value", null);
    Future<ServiceData> futureData = CompletableFuture.completedFuture(buildServiceData(List.of("2", "3", "4")));

    invokeStoreDataInComponent(generator, futureData, screenComponent);

    assertEquals(List.of(new CellData("2"), new CellData("3"), new CellData("4")), screenComponent.getModel().getSelected());
    assertEquals(List.of(new CellData("2"), new CellData("3"), new CellData("4")), screenComponent.getModel().getDefaultValues());
  }

  @Test
  void shouldUseCheckTargetToHydrateMissingSuggestLabels() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(Map.of("screen", "test-screen")));
    ScreenComponent screenComponent = buildSuggestComponent("ComponentSuggestCheckInitial", List.of("3"), Collections.emptyList(), "CheckSuggest", "FallbackSuggest");

    invokeHydratePendingSuggestLabels(generator, Map.of(screenComponent.getId(), screenComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    assertEquals("CheckSuggest", captor.getValue().getTarget());
    assertEquals("3", captor.getValue().getParameters().get("suggest").asString());
    assertEquals("test-screen", captor.getValue().getParameters().get("screen").asString());
    assertEquals(List.of(new CellData("3")), screenComponent.getModel().getSelected());
    assertEquals(1, screenComponent.getModel().getValues().size());
    assertEquals("3", screenComponent.getModel().getValues().get(0).get("value").getStringValue());
    assertEquals("Label 3", screenComponent.getModel().getValues().get(0).get("label").getStringValue());
  }

  @Test
  void shouldMergeOnlyMissingSuggestLabelsWithoutOverwritingExistingRows() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("2"))));
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(Map.of()));
    List<Map<String, CellData>> existingRows = new ArrayList<>();
    existingRows.add(Map.of("value", new CellData("1"), "label", new CellData("Existing 1")));
    ScreenComponent screenComponent = buildSuggestComponent("ComponentSuggestMulti", List.of("1", "2"), existingRows, "CheckSuggest", null);
    List<CellData> initialSelected = new ArrayList<>(screenComponent.getModel().getSelected());

    invokeHydratePendingSuggestLabels(generator, Map.of(screenComponent.getId(), screenComponent));

    assertEquals(initialSelected, screenComponent.getModel().getSelected());
    assertEquals(2, screenComponent.getModel().getValues().size());
    assertEquals("Existing 1", screenComponent.getModel().getValues().get(0).get("label").getStringValue());
    assertEquals("2", screenComponent.getModel().getValues().get(1).get("value").getStringValue());
    assertEquals("Label 2", screenComponent.getModel().getValues().get(1).get("label").getStringValue());
    verify(initialLoadDao, times(1)).launchInitialLoad(any());
  }

  @Test
  void shouldSkipHydrationWhenSelectedSuggestLabelsAreAlreadyPresent() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(Map.of()));
    ScreenComponent screenComponent = buildSuggestComponent(
      "ComponentSuggestResolved",
      List.of("3"),
      List.of(Map.of("value", new CellData("3"), "label", new CellData("Already resolved"))),
      "CheckSuggest",
      null);

    invokeHydratePendingSuggestLabels(generator, Map.of(screenComponent.getId(), screenComponent));

    verify(initialLoadDao, never()).launchInitialLoad(any());
    assertEquals("Already resolved", screenComponent.getModel().getValues().get(0).get("label").getStringValue());
  }

  @Test
  void shouldFallbackToTargetActionWhenCheckTargetIsMissing() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("7"))));
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(Map.of()));
    ScreenComponent screenComponent = buildSuggestComponent("ComponentSuggestFallback", List.of("7"), Collections.emptyList(), null, "FallbackSuggest");

    invokeHydratePendingSuggestLabels(generator, Map.of(screenComponent.getId(), screenComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    assertEquals("FallbackSuggest", captor.getValue().getTarget());
    assertEquals("7", screenComponent.getModel().getValues().get(0).get("value").getStringValue());
  }

  @Test
  void shouldOverrideCollidingRequestParameterWithCriterionSelectedValue() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.putArray("Als").add("A1").add("A2");
    requestParameters.put("OtherParam", "raw-value");
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(requestParameters));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent criterionComponent = buildCriterionComponent("Als", "text", List.of("criterion-value"));

    invokeHydratePendingSuggestLabels(generator, Map.of(suggestComponent.getId(), suggestComponent, criterionComponent.getId(), criterionComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    var parameters = captor.getValue().getParameters();
    assertTrue(parameters.get("Als").isString(), "Colliding parameter should be overridden with the criterion scalar value");
    assertEquals("criterion-value", parameters.get("Als").asString());
    assertEquals("raw-value", parameters.get("OtherParam").asString());
    assertEquals("3", parameters.get("suggest").asString());
  }

  @Test
  void shouldShadowCollidingRequestParameterWhenCriterionIsEmpty() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.putArray("Prd").add("P1").add("P2");
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(requestParameters));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent criterionComponent = buildCriterionComponent("Prd", "text", Collections.emptyList());

    invokeHydratePendingSuggestLabels(generator, Map.of(suggestComponent.getId(), suggestComponent, criterionComponent.getId(), criterionComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    var parameters = captor.getValue().getParameters();
    assertTrue(parameters.get("Prd").isString(), "Empty criterion should shadow the colliding request parameter");
    assertEquals("", parameters.get("Prd").asString());
  }

  @Test
  void shouldOverrideVariableKeyWhenCriterionVariableDiffersFromId() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.put("SugNumReq", "raw-id-value");
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(requestParameters));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent criterionComponent = buildCriterionComponent("SugNumReq", "SugNum", "text", List.of("criterion-value"));

    invokeHydratePendingSuggestLabels(generator, Map.of(suggestComponent.getId(), suggestComponent, criterionComponent.getId(), criterionComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    var parameters = captor.getValue().getParameters();
    assertTrue(parameters.hasNonNull("SugNum"), "Override should land on the criterion variable key");
    assertEquals("criterion-value", parameters.get("SugNum").asString());
    assertEquals("raw-id-value", parameters.get("SugNumReq").asString(), "Criterion id key should keep its original request value");
  }

  @Test
  void shouldNotInjectEmptyValueWhenCriterionKeyIsAbsentFromRequest() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(Map.of("screen", "test-screen")));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent criterionComponent = buildCriterionComponent("Opt", "text", Collections.emptyList());

    invokeHydratePendingSuggestLabels(generator, Map.of(suggestComponent.getId(), suggestComponent, criterionComponent.getId(), criterionComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    var parameters = captor.getValue().getParameters();
    assertFalse(parameters.has("Opt"), "Empty criterion absent from the request should not be injected");
  }

  @Test
  void shouldKeepNonEmptySiblingValueWhenEmptyCriterionWithSameVariableIteratesLater() throws Exception {
    ObjectNode parameters = captureHydrationParametersForSharedVariableSiblings(true);

    assertEquals("v", parameters.get("SugNum").asString(), "Empty sibling should not wipe the value written by a non-empty sibling");
  }

  @Test
  void shouldKeepNonEmptySiblingValueWhenEmptyCriterionWithSameVariableIteratesFirst() throws Exception {
    ObjectNode parameters = captureHydrationParametersForSharedVariableSiblings(false);

    assertEquals("v", parameters.get("SugNum").asString(), "Non-empty sibling should win over an earlier empty sibling");
  }

  private ObjectNode captureHydrationParametersForSharedVariableSiblings(boolean nonEmptyFirst) throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.put("SugNum", "raw-value");
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(requestParameters));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent nonEmptySibling = buildCriterionComponent("SugNumReq", "SugNum", "text", List.of("v"));
    ScreenComponent emptySibling = buildCriterionComponent("SugNumRea", "SugNum", "text", Collections.emptyList());

    Map<String, ScreenComponent> componentMap = new LinkedHashMap<>();
    componentMap.put(suggestComponent.getId(), suggestComponent);
    ScreenComponent first = nonEmptyFirst ? nonEmptySibling : emptySibling;
    ScreenComponent second = nonEmptyFirst ? emptySibling : nonEmptySibling;
    componentMap.put(first.getId(), first);
    componentMap.put(second.getId(), second);

    invokeHydratePendingSuggestLabels(generator, componentMap);

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    return (ObjectNode) captor.getValue().getParameters();
  }

  @Test
  void shouldOverrideRequestParameterWithArrayForMultipleSelectionCriterion() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.put("MultiSelect", "raw-value");
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(requestParameters));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent criterionComponent = buildCriterionComponent("MultiSelect", "select-multiple", List.of("1", "2"));

    invokeHydratePendingSuggestLabels(generator, Map.of(suggestComponent.getId(), suggestComponent, criterionComponent.getId(), criterionComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    var parameters = captor.getValue().getParameters();
    assertTrue(parameters.get("MultiSelect").isArray(), "Multiple selection criterion should be overridden with an array value");
    assertEquals(2, parameters.get("MultiSelect").size());
    assertEquals("1", parameters.get("MultiSelect").get(0).asString());
    assertEquals("2", parameters.get("MultiSelect").get(1).asString());
  }

  @Test
  void shouldOverrideRequestParameterWithArrayForDateRangeCriterionWithASingleDate() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(CompletableFuture.completedFuture(buildServiceData(List.of("3"))));
    ObjectNode requestParameters = JsonNodeFactory.instance.objectNode();
    requestParameters.put("Period", "raw-value");
    TestScreenModelGenerator generator = new TestScreenModelGenerator(initialLoadDao, buildRequest(requestParameters));
    ScreenComponent suggestComponent = buildSuggestComponent("ComponentSuggest", List.of("3"), Collections.emptyList(), "CheckSuggest", null);
    ScreenComponent criterionComponent = buildCriterionComponent("Period", "date-range", List.of("01/01/2026"));

    invokeHydratePendingSuggestLabels(generator, Map.of(suggestComponent.getId(), suggestComponent, criterionComponent.getId(), criterionComponent));

    ArgumentCaptor<AweThreadInitialization> captor = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao).launchInitialLoad(captor.capture());
    var parameters = captor.getValue().getParameters();
    assertTrue(parameters.get("Period").isArray(), "A date range criterion should be overridden with an array value");
    assertEquals(1, parameters.get("Period").size());
    assertEquals("01/01/2026", parameters.get("Period").get(0).asString());
  }

  @Test
  void shouldSplitDateRangeDefaultValueIntoStartAndEnd() {
    TestScreenModelGenerator generator = new TestScreenModelGenerator(mock(InitialLoadDao.class), buildRequest(JsonNodeFactory.instance.objectNode()));
    Criteria controller = Criteria.builder().id("Period").componentType("date-range").value("01/01/2026, 31/01/2026").build();
    ScreenComponent screenComponent = new ScreenComponent().setId("Period").setController(controller).setModel(new ComponentModel());

    generator.generateScreenCriterionModel(controller, screenComponent, JsonNodeFactory.instance.objectNode());

    assertEquals(List.of(new CellData("01/01/2026"), new CellData("31/01/2026")), screenComponent.getModel().getSelected());
    assertEquals(List.of(new CellData("01/01/2026"), new CellData("31/01/2026")), screenComponent.getModel().getDefaultValues());
  }

  @Test
  void shouldKeepAnOpenStartOfDateRangeDefaultValue() {
    TestScreenModelGenerator generator = new TestScreenModelGenerator(mock(InitialLoadDao.class), buildRequest(JsonNodeFactory.instance.objectNode()));
    Criteria controller = Criteria.builder().id("Period").componentType("date-range").value(",31/01/2026").build();
    ScreenComponent screenComponent = new ScreenComponent().setId("Period").setController(controller).setModel(new ComponentModel());

    generator.generateScreenCriterionModel(controller, screenComponent, JsonNodeFactory.instance.objectNode());

    assertEquals(List.of(new CellData(""), new CellData("31/01/2026")), screenComponent.getModel().getSelected());
  }

  @Test
  void shouldGenerateTheQuickFilterOfAGridWithItsValueAndItsOptions() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    DataList options = new DataList();
    options.getRows().add(Map.of("value", new CellData(""), "label", new CellData("All"), "count", new CellData(128)));
    options.getRows().add(Map.of("value", new CellData("LCK"), "label", new CellData("Locked"), "count", new CellData(3), "tone", new CellData("danger")));
    when(initialLoadDao.launchInitialLoad(any())).thenAnswer(invocation -> CompletableFuture.completedFuture(
      "UsrStaCnt".equals(((AweThreadInitialization) invocation.getArgument(0)).getTarget()) ?
        new ServiceData().addVariable(ACTION_DATA, new CellData(options)) : new ServiceData().addVariable(ACTION_DATA, new CellData(new DataList()))));
    AweRequest request = buildRequest(JsonNodeFactory.instance.objectNode());
    BaseConfigProperties properties = mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS);
    ScreenComponentGenerator generator = new ScreenComponentGenerator(request, new TestScreenModelGenerator(initialLoadDao, request, properties),
      initialLoadDao, mock(AweElementsDao.class), properties, mock(ScreenConfigurationGenerator.class), mock(GridLayoutService.class),
      mock(WindowStateService.class));
    // A generation error is reported with its locale key
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweElements.class)).thenReturn(mock(AweElements.class, invocation ->
      "getLocaleWithLanguage".equals(invocation.getMethod().getName()) ? String.valueOf((Object) invocation.getArgument(0)) : null));
    generator.setApplicationContext(context);
    XStream xstream = new XStream(new DomDriver());
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Screen.class);
    Screen screen = (Screen) xstream.fromXML("<screen template=\"window\"><tag source=\"center\">" +
      "<grid id=\"GrdUsr\" server-action=\"data\" target-action=\"UsrLst\" initial-load=\"query\">" +
      "<quick-filter id=\"CrtUsrSta\" initial-load=\"query\" target-action=\"UsrStaCnt\" value=\"\"/>" +
      "<column name=\"usr\" label=\"USER\"/></grid></tag></screen>");
    screen.setId("users");
    ScreenData data = new ScreenData();

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, data, new Menu(), JsonNodeFactory.instance.objectNode());

    assertEquals(List.of(), data.getActions().stream().map(action -> action.getParameters().toString()).toList());
    ScreenComponent quickFilter = componentMap.get("CrtUsrSta");
    assertEquals(List.of(new CellData("")), quickFilter.getModel().getSelected());
    assertEquals(List.of("", "LCK"), quickFilter.getModel().getValues().stream().map(row -> row.get("value").getStringValue()).toList());
    assertEquals(3, quickFilter.getModel().getValues().get(1).get("count").getIntegerValue());
    assertEquals("danger", quickFilter.getModel().getValues().get(1).get("tone").getStringValue());
  }

  @Test
  void shouldGenerateTheSearchOfAGridWithItsValueAndNoQuery() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenAnswer(invocation -> CompletableFuture.completedFuture(
      new ServiceData().addVariable(ACTION_DATA, new CellData(new DataList()))));
    AweRequest request = buildRequest(JsonNodeFactory.instance.objectNode());
    BaseConfigProperties properties = mock(BaseConfigProperties.class, RETURNS_DEEP_STUBS);
    ScreenComponentGenerator generator = new ScreenComponentGenerator(request, new TestScreenModelGenerator(initialLoadDao, request, properties),
      initialLoadDao, mock(AweElementsDao.class), properties, mock(ScreenConfigurationGenerator.class), mock(GridLayoutService.class),
      mock(WindowStateService.class));
    // A generation error is reported with its locale key
    ApplicationContext context = mock(ApplicationContext.class);
    when(context.getBean(AweElements.class)).thenReturn(mock(AweElements.class, invocation ->
      "getLocaleWithLanguage".equals(invocation.getMethod().getName()) ? String.valueOf((Object) invocation.getArgument(0)) : null));
    generator.setApplicationContext(context);
    XStream xstream = new XStream(new DomDriver());
    xstream.allowTypesByWildcard(new String[]{"com.almis.awe.**"});
    xstream.processAnnotations(Screen.class);
    Screen screen = (Screen) xstream.fromXML("<screen template=\"window\"><tag source=\"center\">" +
      "<grid id=\"GrdUsr\" server-action=\"data\" target-action=\"UsrLst\" initial-load=\"query\">" +
      "<search id=\"CrtUsrTxt\" placeholder=\"SCREEN_TEXT_USER_SEARCH\" value=\"adm\"/>" +
      "<column name=\"usr\" label=\"USER\"/></grid></tag></screen>");
    screen.setId("users");
    ScreenData data = new ScreenData();

    Map<String, ScreenComponent> componentMap = generator.generateComponentMap(screen, data, new Menu(), JsonNodeFactory.instance.objectNode());

    assertEquals(List.of(), data.getActions().stream().map(action -> action.getParameters().toString()).toList());
    assertEquals(Set.of("GrdUsr", "CrtUsrTxt"), componentMap.keySet());
    ScreenComponent search = componentMap.get("CrtUsrTxt");
    assertEquals("grid-search", search.getController().getComponentType());
    assertEquals(List.of(new CellData("adm")), search.getModel().getSelected());
    // The search has no query of its own: only the grid (and the screen configuration) load data
    ArgumentCaptor<AweThreadInitialization> loads = ArgumentCaptor.forClass(AweThreadInitialization.class);
    verify(initialLoadDao, atLeast(0)).launchInitialLoad(loads.capture());
    assertEquals(List.of("UsrLst"), loads.getAllValues().stream().map(AweThreadInitialization::getTarget)
      .filter(target -> !"getScreenConfiguration".equals(target)).toList());
  }

  private ScreenComponent buildScreenComponent(String componentType, String initialLoad, String value) {
    Criteria controller = Criteria.builder()
      .componentType(componentType)
      .initialLoad(initialLoad)
      .value(value)
      .build();
    ComponentModel model = new ComponentModel();
    if (value != null) {
      model.setSelected(List.of(new CellData(value)));
      model.setDefaultValues(List.of(new CellData(value)));
    }
    return new ScreenComponent().setController(controller).setModel(model);
  }

  private ScreenComponent buildSuggestComponent(String id, List<String> selectedValues,
                                               List<Map<String, CellData>> values, String checkTarget,
                                               String targetAction) {
    Criteria controller = Criteria.builder()
      .id(id)
      .componentType(values.size() > 1 || selectedValues.size() > 1 ? "suggest-multiple" : "suggest")
      .checkTarget(checkTarget)
      .targetAction(targetAction)
      .build();
    ComponentModel model = new ComponentModel()
      .setSelected(selectedValues.stream().map(CellData::new).toList())
      .setDefaultValues(selectedValues.stream().map(CellData::new).toList())
      .setValues(new ArrayList<>(values));
    return new ScreenComponent().setId(id).setController(controller).setModel(model);
  }

  private ScreenComponent buildCriterionComponent(String id, String componentType, List<String> selectedValues) {
    return buildCriterionComponent(id, null, componentType, selectedValues);
  }

  private ScreenComponent buildCriterionComponent(String id, String variable, String componentType, List<String> selectedValues) {
    Criteria controller = Criteria.builder()
      .id(id)
      .variable(variable)
      .componentType(componentType)
      .build();
    ComponentModel model = new ComponentModel()
      .setSelected(selectedValues.stream().map(CellData::new).toList());
    return new ScreenComponent().setId(id).setController(controller).setModel(model);
  }

  private AweRequest buildRequest(Map<String, String> parameters) {
    ObjectNode parameterList = JsonNodeFactory.instance.objectNode();
    parameters.forEach(parameterList::put);
    return buildRequest(parameterList);
  }

  private AweRequest buildRequest(ObjectNode parameters) {
    AweRequest request = new AweRequest(mock(jakarta.servlet.http.HttpServletRequest.class), mock(jakarta.servlet.http.HttpServletResponse.class), new JsonMapper());
    request.setParameterList(parameters);
    return request;
  }

  private ServiceData buildServiceData(List<String> selectedValues) {
    DataList dataList = new DataList();
    for (String selectedValue : selectedValues) {
      dataList.getRows().add(Map.of("value", new CellData(selectedValue), "label", new CellData("Label " + selectedValue)));
    }
    return new ServiceData().addVariable(ACTION_DATA, new CellData(dataList));
  }

  private void invokeStoreDataInComponent(ScreenModelGenerator generator, Future<ServiceData> futureData,
                                          ScreenComponent screenComponent) throws Exception {
    Method method = ScreenModelGenerator.class.getDeclaredMethod("storeDataInComponent", Future.class, ScreenComponent.class, ScreenData.class);
    method.setAccessible(true);
    method.invoke(generator, futureData, screenComponent, new ScreenData());
  }

  private void invokeHydratePendingSuggestLabels(ScreenModelGenerator generator,
                                                 Map<String, ScreenComponent> componentMap) throws Exception {
    Method method = ScreenModelGenerator.class.getDeclaredMethod("hydratePendingSuggestLabels", Map.class, ScreenData.class);
    method.setAccessible(true);
    method.invoke(generator, componentMap, new ScreenData());
  }

  private static class TestScreenModelGenerator extends ScreenModelGenerator {
    private final AweRequest request;

    TestScreenModelGenerator(InitialLoadDao initialLoadDao, AweRequest request) {
      this(initialLoadDao, request, mock(BaseConfigProperties.class));
    }

    TestScreenModelGenerator(InitialLoadDao initialLoadDao, AweRequest request, BaseConfigProperties baseConfigProperties) {
      super(initialLoadDao, baseConfigProperties);
      this.request = request;
    }

    @Override
    public AweRequest getRequest() {
      return request;
    }
  }
}
