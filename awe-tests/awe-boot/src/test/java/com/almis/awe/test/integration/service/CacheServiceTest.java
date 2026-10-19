package com.almis.awe.test.integration.service;

import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.constant.AweConstants;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.menu.Option;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.entities.screen.Tag;
import com.almis.awe.test.integration.AbstractSpringAppIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@org.junit.jupiter.api.Tag("integration")
@DisplayName("Cache service Tests")
class CacheServiceTest extends AbstractSpringAppIntegrationTest {

  @Autowired
  private AweElements aweElements;

  @Test
  void testMenu() throws Exception {
    Menu originalMenu = aweElements.getMenu(AweConstants.PUBLIC_MENU);
    Menu menu = originalMenu.copy();
    menu.addElement(Option.builder()
            .invisible(true)
            .screen("Prueba")
            .name("prueba")
      .build());
    try {
      aweElements.setMenu(AweConstants.PUBLIC_MENU, menu);
      assertThat(menu.getElementList().size(), equalTo(aweElements.getMenu(AweConstants.PUBLIC_MENU).getElementList().size()));
    } finally {
      // Restore the cached menu: the Spring context is shared with other integration tests
      aweElements.setMenu(AweConstants.PUBLIC_MENU, originalMenu);
    }
  }

  @Test
  void testScreen() throws Exception {
    Screen originalScreen = aweElements.getScreen("MatTst");
    Screen screen = originalScreen.copy();
    screen.addElement(Tag.builder().build());
    try {
      aweElements.setScreen(screen);
      assertThat(screen.getElementList().size(), equalTo(aweElements.getScreen("MatTst").getElementList().size()));
    } finally {
      // Restore the cached screen: a tag without source breaks the template of later screen requests
      aweElements.setScreen(originalScreen);
    }
  }
}
