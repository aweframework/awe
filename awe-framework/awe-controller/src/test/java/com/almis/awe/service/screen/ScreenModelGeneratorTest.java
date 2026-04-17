package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.almis.awe.model.constant.AweConstants.ACTION_DATA;
import static org.mockito.Mockito.mock;

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
}
