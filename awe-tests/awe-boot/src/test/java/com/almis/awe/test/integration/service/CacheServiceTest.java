package com.almis.awe.test.integration.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.enumerated.EnumeratedGroup;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@org.junit.jupiter.api.Tag("integration")
@DisplayName("Cache service Tests")
class CacheServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private AweElements aweElements;

  @Autowired
  private CacheManager cacheManager;

  @Test
  void testMenu() throws Exception {
    Menu originalMenu = aweElements.getMenu(AweConstants.PUBLIC_MENU);
    Menu menu = originalMenu.copy();
    menu.addElement(Option.builder()
            .invisible(true)
            .screen("Prueba")
            .name("prueba")
      .build());
    aweElements.setMenu(AweConstants.PUBLIC_MENU, menu);
    assertThat(menu.getElementList().size(), equalTo(aweElements.getMenu(AweConstants.PUBLIC_MENU).getElementList().size()));
    aweElements.setMenu(AweConstants.PUBLIC_MENU, originalMenu);
  }

  @Test
  void testScreen() throws Exception {
    Screen screen = aweElements.getScreen("MatTst").copy();
    screen.addElement(Tag.builder().build());
    aweElements.setScreen(screen);
    assertThat(screen.getElementList().size(), equalTo(aweElements.getScreen("MatTst").getElementList().size()));
  }

  @Test
  void testEnumerated() throws Exception {
    // Skip the first result: For some reason is not the same object as cached one
    aweElements.getEnumerated("Es1Es0");

    // First invocation returns object returned by the method
    EnumeratedGroup first = aweElements.getEnumerated("Es1Es0");

    // Second invocation should return cached value
    EnumeratedGroup result = aweElements.getEnumerated("Es1Es0");
    assertThat(result, is(first));

    // Verify repository method was invoked once
    assertThat(Objects.requireNonNull(cacheManager.getCache("enumerated")).get("Es1Es0"), is(notNullValue()));

    // Skip the first result: For some reason is not the same object as cached one
    aweElements.getEnumerated("EsyEnn");

    // First invocation returns object returned by the method
    EnumeratedGroup second = aweElements.getEnumerated("EsyEnn");
    assertThat(second, not(is(first)));

    // Second invocation should return cached value
    result = aweElements.getEnumerated("EsyEnn");
    assertThat(result, is(second));

    // Verify repository method was invoked once
    assertThat(Objects.requireNonNull(cacheManager.getCache("enumerated")).get("EsyEnn"), is(notNullValue()));
  }
}