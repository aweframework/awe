package com.almis.awe.service.menu;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWEUnauthorizedException;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.component.panelable.Panelable;
import com.almis.awe.service.LauncherService;
import com.almis.awe.service.QueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MenuScreenResolver resolves options through the injected MenuOptionLookup, so a replaced lookup bean
 * also applies to the screen resolution
 */
@ExtendWith(MockitoExtension.class)
class MenuScreenResolverTest {

  @Mock
  private MenuProvider menuProvider;
  @Mock
  private MenuOptionLookup menuOptionLookup;
  @Mock
  private InitialLoadDao initialLoadDao;
  @Mock
  private LauncherService launcherService;
  @Mock
  private BaseConfigProperties baseConfigProperties;
  @Mock
  private ApplicationContext context;
  @Mock
  private AweElements aweElements;

  private MenuScreenResolver menuScreenResolver;

  @BeforeEach
  void setUp() {
    menuScreenResolver = new MenuScreenResolver(menuProvider, menuOptionLookup, initialLoadDao, launcherService,
      baseConfigProperties);
    menuScreenResolver.setApplicationContext(context);
    lenient().when(baseConfigProperties.getScreen()).thenReturn(new BaseConfigProperties.Screen());
  }

  @AfterEach
  void clearInterruptFlag() {
    Thread.interrupted();
  }

  private Screen initializedScreen(String id) {
    Screen screen = new Screen();
    screen.setId(id);
    screen.setInitialized(true);
    return screen;
  }

  @Test
  void optionScreenUsesTheInjectedOptionLookup() throws AWException {
    Menu menu = new Menu();
    Option custom = new Option();
    custom.setName("custom");
    custom.setScreen("custom-screen");
    when(menuOptionLookup.getOptionByName("opt", menu)).thenReturn(custom);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen("custom-screen")).thenReturn(initializedScreen("custom-screen"));

    assertThat(menuScreenResolver.getOptionScreen("opt", menu).getId()).isEqualTo("custom-screen");
    verifyNoInteractions(menuProvider, initialLoadDao, launcherService);
  }

  @Test
  void availableOptionScreenOfTheUserMenuUsesTheInjectedProviderAndLookup() throws AWException {
    Menu menu = new Menu();
    Option custom = new Option();
    custom.setName("custom");
    custom.setScreen("custom-screen");
    when(menuProvider.getMenu()).thenReturn(menu);
    when(menuOptionLookup.getAvailableOptionByName("opt", menu)).thenReturn(custom);
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen("custom-screen")).thenReturn(initializedScreen("custom-screen"));

    assertThat(menuScreenResolver.getAvailableOptionScreen("opt").getId()).isEqualTo("custom-screen");
    verify(menuOptionLookup).getAvailableOptionByName("opt", menu);
  }

  @Test
  void aFailedPanelableLoadKeepsTheRequestThreadUninterrupted() throws AWException {
    Panelable tabs = mock(Panelable.class);
    when(tabs.getInitialLoad()).thenReturn("query");
    when(tabs.getTargetAction()).thenReturn("TabTarget");
    Screen stored = mock(Screen.class);
    Screen copy = mock(Screen.class);
    when(stored.copy()).thenReturn(copy);
    when(copy.copy()).thenReturn(copy);
    when(copy.getElementsByType(Panelable.class)).thenReturn(List.of(tabs));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen("tabs-screen")).thenReturn(stored);
    when(initialLoadDao.launchServerInitialLoad(any()))
      .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Connection refused")));

    assertThat(menuScreenResolver.getScreen("tabs-screen")).isSameAs(copy);
    assertThat(Thread.currentThread().isInterrupted()).isFalse();
  }

  @Test
  void anInterruptedPanelableLoadRestoresTheInterruptFlag() throws Exception {
    @SuppressWarnings("unchecked")
    Future<ServiceData> interrupted = mock(Future.class);
    when(interrupted.get()).thenThrow(new InterruptedException("interrupted"));
    Panelable tabs = mock(Panelable.class);
    when(tabs.getInitialLoad()).thenReturn("query");
    when(tabs.getTargetAction()).thenReturn("TabTarget");
    Screen stored = mock(Screen.class);
    Screen copy = mock(Screen.class);
    when(stored.copy()).thenReturn(copy);
    when(copy.copy()).thenReturn(copy);
    when(copy.getElementsByType(Panelable.class)).thenReturn(List.of(tabs));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen("tabs-screen")).thenReturn(stored);
    when(initialLoadDao.launchServerInitialLoad(any())).thenReturn(interrupted);

    menuScreenResolver.getScreen("tabs-screen");

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
  }

  @Test
  void panelableValuesAreLoadedForTheSharedScreenWithoutTheClientCheck() throws Exception {
    // The values are stored once in the screen definition shared by every user, so whoever opens the screen first
    // (a user without access to the query, an anonymous user on a public screen) must not empty them for everyone
    QueryService queryService = mock(QueryService.class);
    // The client check would refuse the query
    lenient().doThrow(new AWEUnauthorizedException("Title", "Refused")).when(queryService).authorizeClientLaunch(anyString());
    DataList values = new DataList();
    values.addRow(Map.of(AweConstants.JSON_VALUE_PARAMETER, new CellData("1"),
      AweConstants.JSON_LABEL_PARAMETER, new CellData("Step one")));
    when(queryService.launchPrivateQuery(anyString(), (ObjectNode) any())).thenReturn(
      new ServiceData().addVariable(AweConstants.ACTION_DATA, new CellData(values)));
    MenuScreenResolver resolver = new MenuScreenResolver(menuProvider, menuOptionLookup,
      new InitialLoadDao(queryService), launcherService, baseConfigProperties);
    resolver.setApplicationContext(context);
    Panelable tabs = mock(Panelable.class);
    when(tabs.getInitialLoad()).thenReturn("query");
    when(tabs.getTargetAction()).thenReturn("TabTarget");
    Screen stored = mock(Screen.class);
    Screen copy = mock(Screen.class);
    when(stored.copy()).thenReturn(copy);
    when(copy.copy()).thenReturn(copy);
    when(copy.getElementsByType(Panelable.class)).thenReturn(List.of(tabs));
    when(context.getBean(AweElements.class)).thenReturn(aweElements);
    when(aweElements.getScreen("tabs-screen")).thenReturn(stored);

    resolver.getScreen("tabs-screen");

    verify(tabs).setTabValues(Map.of("1", "Step one"));
    verify(queryService, never()).authorizeClientLaunch(anyString());
  }
}
