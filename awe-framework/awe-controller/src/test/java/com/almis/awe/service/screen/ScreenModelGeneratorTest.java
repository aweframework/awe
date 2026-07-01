package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    assertEquals("3", captor.getValue().getParameters().get("suggest").asText());
    assertEquals("test-screen", captor.getValue().getParameters().get("screen").asText());
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

  private AweRequest buildRequest(Map<String, String> parameters) {
    AweRequest request = new AweRequest(mock(jakarta.servlet.http.HttpServletRequest.class), mock(jakarta.servlet.http.HttpServletResponse.class), new ObjectMapper());
    request.setParameterList(JsonNodeFactory.instance.objectNode());
    parameters.forEach(request::setParameter);
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
      super(initialLoadDao, mock(BaseConfigProperties.class));
      this.request = request;
    }

    @Override
    public AweRequest getRequest() {
      return request;
    }
  }
}
