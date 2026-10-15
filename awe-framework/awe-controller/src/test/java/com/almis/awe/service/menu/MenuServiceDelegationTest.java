package com.almis.awe.service.menu;

import com.almis.awe.exception.AWException;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MenuService facade wiring: delegation to the injected collaborators and the reflection contract of Services.xml
 */
@ExtendWith(MockitoExtension.class)
class MenuServiceDelegationTest {

  @Mock
  private MenuProvider menuProvider;
  @Mock
  private MenuScreenResolver menuScreenResolver;
  @Mock
  private MenuOptionLookup menuOptionLookup;
  @Mock
  private MenuSuggestionLists menuSuggestionLists;
  @Mock
  private MenuRestrictionTreeService menuRestrictionTreeService;

  private MenuService menuService;

  @BeforeEach
  void setUp() {
    menuService = new MenuService(menuProvider, menuScreenResolver, menuOptionLookup, menuSuggestionLists,
      menuRestrictionTreeService);
  }

  @Test
  void delegatesToTheInjectedCollaborators() throws AWException {
    Menu menu = new Menu();
    Screen screen = new Screen();
    Option option = new Option();
    ServiceData suggestions = new ServiceData();
    ServiceData restrictions = new ServiceData();
    ServiceData tree = new ServiceData();
    when(menuProvider.getMenu()).thenReturn(menu);
    when(menuScreenResolver.getOptionScreen("opt", menu)).thenReturn(screen);
    when(menuOptionLookup.getOptionByName("opt")).thenReturn(option);
    when(menuOptionLookup.checkOptionAddress("screen/public/opt")).thenReturn(true);
    when(menuSuggestionLists.getNameOptionList("su")).thenReturn(suggestions);
    when(menuRestrictionTreeService.getScreenRestrictions("general")).thenReturn(restrictions);
    when(menuRestrictionTreeService.getMenuOptionTreeByModule(1, 2, "module")).thenReturn(tree);

    assertThat(menuService.getMenu()).isSameAs(menu);
    assertThat(menuService.getOptionScreen("opt", menu)).isSameAs(screen);
    assertThat(menuService.getOptionByName("opt")).isSameAs(option);
    assertThat(menuService.checkOptionAddress("screen/public/opt")).isTrue();
    assertThat(menuService.getNameOptionList("su")).isSameAs(suggestions);
    assertThat(menuService.getScreenRestrictions("general")).isSameAs(restrictions);
    assertThat(menuService.getMenuOptionTreeByModule(1, 2, "module")).isSameAs(tree);
  }

  @Test
  void injectedCollaboratorsKeepTheirOwnApplicationContext() {
    menuService.setApplicationContext(mock(ApplicationContext.class));

    verifyNoInteractions(menuProvider, menuScreenResolver, menuOptionLookup, menuSuggestionLists,
      menuRestrictionTreeService);
  }

  @Test
  void servicesXmlMethodsKeepTheirReflectionSignatures() throws NoSuchMethodException {
    // Services.xml (awe-generic-screens) resolves these with getClass().getMethod(name, parameterClasses)
    List<Method> methods = List.of(
      MenuService.class.getMethod("getNameOptionList", String.class),
      MenuService.class.getMethod("getAvailableScreenList", String.class),
      MenuService.class.getMethod("getAllScreenList", String.class),
      MenuService.class.getMethod("getModuleList"),
      MenuService.class.getMethod("getScreenRestrictions", String.class),
      MenuService.class.getMethod("getMenuOptionTree"),
      MenuService.class.getMethod("getMenuOptionTreeByModule", Integer.class, Integer.class, String.class),
      MenuService.class.getMethod("updateMenuRestrictionTree", String.class),
      MenuService.class.getMethod("allowMenuOption", Integer.class, Integer.class, String.class),
      MenuService.class.getMethod("restrictMenuOption", Integer.class, Integer.class, String.class),
      MenuService.class.getMethod("removeRestriction", Integer.class, Integer.class, String.class),
      MenuService.class.getMethod("refreshMenu"));

    assertThat(methods).allSatisfy(method -> {
      assertThat(method.getDeclaringClass()).isEqualTo(MenuService.class);
      assertThat(method.getReturnType()).isEqualTo(ServiceData.class);
    });
  }
}
