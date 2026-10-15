package com.almis.awe.service.menu;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.dao.InitialLoadDao;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.service.LauncherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
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
    when(baseConfigProperties.getScreen()).thenReturn(new BaseConfigProperties.Screen());
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
}
