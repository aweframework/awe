package com.almis.awe.service.screen;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.actions.ClientAction;
import com.almis.awe.model.entities.screen.component.MenuContainer;
import com.almis.awe.model.entities.screen.component.criteria.Criteria;
import com.almis.awe.model.entities.screen.data.AweThreadInitialization;
import com.almis.awe.model.entities.screen.data.ComponentModel;
import com.almis.awe.model.entities.screen.data.ScreenComponent;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.model.type.LoadType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A failed initial load must add an error to the screen data without interrupting the request thread (#894):
 * Tomcat 11 refuses to write the response on an interrupted thread.
 */
class ScreenModelGeneratorInitialLoadFailureTest {

  private static final String CRITERION = "Criterion";
  private static final String COMPONENT_TARGET = "ComponentTarget";

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  @Test
  void shouldNotInterruptRequestThreadWhenScreenTargetLoadFails() throws Exception {
    AweThreadInitialization initialization = new AweThreadInitialization()
      .setInitialLoadType(LoadType.SCREEN)
      .setTarget("ScreenTarget");

    ScreenData data = launch(failingDao(), List.of(initialization), Map.of());

    assertErrorWithoutInterrupt(data);
  }

  @Test
  void shouldNotInterruptRequestThreadWhenMenuRestrictionLoadFails() throws Exception {
    AweThreadInitialization initialization = new AweThreadInitialization()
      .setInitialLoadType(LoadType.MENU)
      .setComponentId("Menu");
    ScreenComponent menu = new ScreenComponent().setId("Menu").setController(new MenuContainer());

    ScreenData data = launch(failingDao(), List.of(initialization), Map.of("Menu", menu));

    assertErrorWithoutInterrupt(data);
  }

  @Test
  void shouldNotInterruptRequestThreadWhenComponentLoadFails() throws Exception {
    AweThreadInitialization initialization = new AweThreadInitialization()
      .setTarget(COMPONENT_TARGET)
      .setComponentId(CRITERION);

    ScreenData data = launch(failingDao(), List.of(initialization), Map.of(CRITERION, buildCriterion()));

    assertErrorWithoutInterrupt(data);
  }

  @Test
  void shouldNotInterruptRequestThreadWhenSuggestHydrationFails() throws Exception {
    ScreenComponent suggest = buildSuggest();

    ScreenData data = launch(failingDao(), List.of(), Map.of(suggest.getId(), suggest));

    assertErrorWithoutInterrupt(data);
  }

  @Test
  void shouldRestoreInterruptFlagWhenInitialLoadIsInterrupted() throws Exception {
    @SuppressWarnings("unchecked")
    Future<ServiceData> interrupted = mock(Future.class);
    when(interrupted.get()).thenThrow(new InterruptedException("interrupted"));
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any())).thenReturn(interrupted);
    AweThreadInitialization initialization = new AweThreadInitialization()
      .setTarget(COMPONENT_TARGET)
      .setComponentId(CRITERION);

    ScreenData data = launch(initialLoadDao, List.of(initialization), Map.of(CRITERION, buildCriterion()));

    assertThat(data.getActions()).extracting(ClientAction::getType).contains("message");
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }

  private ScreenData launch(InitialLoadDao initialLoadDao, List<AweThreadInitialization> initializations,
                            Map<String, ScreenComponent> componentMap) throws Exception {
    ScreenData data = new ScreenData().addScreenProperty("option", "test-screen");
    new LocaleFreeScreenModelGenerator(initialLoadDao).launchInitialLoadList(initializations, componentMap, data);
    return data;
  }

  private void assertErrorWithoutInterrupt(ScreenData data) {
    assertThat(data.getActions()).extracting(ClientAction::getType).contains("message");
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  private InitialLoadDao failingDao() throws Exception {
    InitialLoadDao initialLoadDao = mock(InitialLoadDao.class);
    when(initialLoadDao.launchInitialLoad(any()))
      .thenAnswer(invocation -> CompletableFuture.failedFuture(new IllegalStateException("Connection refused")));
    return initialLoadDao;
  }

  private ScreenComponent buildCriterion() {
    Criteria controller = Criteria.builder().id(CRITERION).componentType("select").initialLoad("query")
      .targetAction(COMPONENT_TARGET).build();
    return new ScreenComponent().setId(CRITERION).setController(controller).setModel(new ComponentModel());
  }

  private ScreenComponent buildSuggest() {
    Criteria controller = Criteria.builder().id("Suggest").componentType("suggest").checkTarget("CheckSuggest").build();
    ComponentModel model = new ComponentModel()
      .setSelected(List.of(new CellData("3")))
      .setDefaultValues(List.of(new CellData("3")))
      .setValues(new ArrayList<>(Collections.emptyList()));
    return new ScreenComponent().setId("Suggest").setController(controller).setModel(model);
  }

  private static class LocaleFreeScreenModelGenerator extends ScreenModelGenerator {
    private final AweRequest request;

    LocaleFreeScreenModelGenerator(InitialLoadDao initialLoadDao) {
      super(initialLoadDao, mock(BaseConfigProperties.class));
      request = new AweRequest(mock(jakarta.servlet.http.HttpServletRequest.class),
        mock(jakarta.servlet.http.HttpServletResponse.class), new JsonMapper());
      request.setParameterList(JsonNodeFactory.instance.objectNode());
    }

    @Override
    public AweRequest getRequest() {
      return request;
    }

    @Override
    public String getLocale(String locale) {
      return locale;
    }

    @Override
    public String getLocale(String locale, String... parameters) {
      return locale;
    }
  }
}
