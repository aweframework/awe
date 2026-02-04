package com.almis.awe.service;

import com.almis.awe.builder.screen.ScreenBuilder;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.data.ScreenData;
import com.almis.awe.model.tracker.AweClientTracker;
import com.almis.awe.service.screen.ScreenComponentGenerator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Class used for testing ScreenService class
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class ScreenServiceTest {

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
}
