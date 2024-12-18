package com.almis.awe.service.screen;

import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.model.type.LoadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Screen Model generator tests
 */
class ScreenModelGeneratorTest {

  @InjectMocks
  private ScreenModelGenerator screenModelGenerator;

  @Mock
  private InitialLoadDao initialLoadDao;

  @Mock
  ApplicationContext applicationContext;

  @Mock
  AweElements aweElements;

  @BeforeEach
  void setUp() {
    screenModelGenerator.setApplicationContext(applicationContext);
  }

  /**
   * Initialize beans
   */
  @BeforeEach
  public void initBeans() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void generateComponentModelFromDataList() {
    ScreenComponent screenComponent = new ScreenComponent().setModel(new ComponentModel());
    screenModelGenerator.generateComponentModelFromDataList(new DataList(), screenComponent);
    assertEquals(Long.valueOf(0), screenComponent.getModel().getRecords());
  }

  @Test
  void givenInterruptedFuture_launchInitialLoadList() throws AWException {
    // Given
    CompletableFuture<ServiceData> interruptedRunnerTask = CompletableFuture.failedFuture(new InterruptedException());
    List<AweThreadInitialization> aweThreadInitializations = List.of(new AweThreadInitialization().setInitialLoadType(LoadType.VALUE).setTarget("dummy").setComponentId("dummyId"));
    HashMap<String, ScreenComponent> componentMap = new HashMap<>();
    componentMap.put("dummyId", new ScreenComponent()
        .setId("dummyId")
        .setController(new Criteria()));
    // When
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(interruptedRunnerTask);
    when(applicationContext.getBean(AweElements.class)).thenReturn(aweElements);
    // Then
    screenModelGenerator.launchInitialLoadList(aweThreadInitializations, componentMap, new ScreenData());
    // Asserts
    verify(aweElements, times(1)).getLocaleWithLanguage(anyString(), eq(null));
  }
}
