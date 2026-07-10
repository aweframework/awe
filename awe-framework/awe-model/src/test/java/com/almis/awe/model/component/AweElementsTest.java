package com.almis.awe.model.component;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.dao.AweElementsDao;
import com.almis.awe.model.entities.menu.Menu;
import com.almis.awe.model.entities.screen.Screen;
import com.almis.awe.model.util.XmlSerializerTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AweElements initialization and reload tests
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AweElementsTest {

  private static final String LANGUAGE = "en-GB";

  @Mock
  private WebApplicationContext context;

  @Mock
  private Environment environment;

  private BaseConfigProperties baseConfigProperties;
  private AweElements aweElements;

  @BeforeEach
  void setUp() {
    when(context.getEnvironment()).thenReturn(environment);
    baseConfigProperties = new BaseConfigProperties();
    baseConfigProperties.setModuleList(new String[]{"module-one", "module-two"});
    baseConfigProperties.setLanguageList(List.of(LANGUAGE));
    AweElementsDao elementsDao = new AweElementsDao(XmlSerializerTestUtil.buildSerializer(), baseConfigProperties);
    aweElements = new AweElements(context, baseConfigProperties, elementsDao);
    aweElements.init();
  }

  /**
   * Test that queries are loaded on startup honoring module precedence
   */
  @Test
  void initLoadsQueriesWithModulePrecedence() throws AWException {
    assertEquals("serviceModuleOne", aweElements.getQuery("sharedQuery").getService());
    assertNotNull(aweElements.getQuery("moduleOneQuery"));
    assertNotNull(aweElements.getQuery("moduleTwoQuery"));
  }

  /**
   * Test that screens are always preloaded on startup, including subdirectories
   */
  @Test
  void initAlwaysPreloadsScreens() {
    assertTrue(aweElements.getScreenList().contains("flat-screen"));
    assertTrue(aweElements.getScreenList().contains("sub-screen"));
    assertTrue(aweElements.getScreenList().contains("shared-screen"));
  }

  /**
   * Test that profiles are loaded on startup
   */
  @Test
  void initLoadsProfiles() {
    assertTrue(aweElements.getProfileList().contains("general"));
  }

  /**
   * Test that reloading queries rebuilds the whole map from the current module list
   */
  @Test
  void reloadQueriesRebuildsMap() throws AWException {
    // Change the module list and reload: module-two values must win now
    baseConfigProperties.setModuleList(new String[]{"module-two"});
    aweElements.reloadQueries();

    assertEquals("serviceModuleTwo", aweElements.getQuery("sharedQuery").getService());
    assertNull(aweElements.getQuery("moduleOneQuery"));

    // Restore the module list and reload: module precedence must be back
    baseConfigProperties.setModuleList(new String[]{"module-one", "module-two"});
    aweElements.reloadQueries();

    assertEquals("serviceModuleOne", aweElements.getQuery("sharedQuery").getService());
    assertNotNull(aweElements.getQuery("moduleOneQuery"));
  }

  /**
   * Test that reloading screens discards poisoned entries and preserves module precedence
   */
  @Test
  void reloadScreensDiscardsPoisonedEntries() throws AWException {
    // Poison the screen map
    aweElements.setScreen((Screen) new Screen().setId("poisoned-screen"));
    assertTrue(aweElements.getScreenList().contains("poisoned-screen"));

    // Reload
    aweElements.reloadScreens();

    // Poisoned entry is gone and legit entries are back
    assertFalse(aweElements.getScreenList().contains("poisoned-screen"));
    assertTrue(aweElements.getScreenList().contains("flat-screen"));
    assertTrue(aweElements.getScreenList().contains("sub-screen"));
    assertEquals("SHARED_SCREEN_MODULE_ONE", aweElements.getScreen("shared-screen").getLabel());
  }

  /**
   * Test that reloading locales discards poisoned entries and preserves merge semantics
   */
  @Test
  void reloadLocalesDiscardsPoisonedEntries() {
    // Poison the locale map
    aweElements.getLocales().get(LANGUAGE).put("POISONED_KEY", "poisoned");
    assertEquals("poisoned", aweElements.getLocaleWithLanguage("POISONED_KEY", LANGUAGE));

    // Reload
    aweElements.reloadLocales();

    // Poisoned entry is gone (missing locales resolve to their own identifier)
    assertEquals("POISONED_KEY", aweElements.getLocaleWithLanguage("POISONED_KEY", LANGUAGE));

    // Legit entries are back with module precedence per language
    assertEquals("Module one value", aweElements.getLocaleWithLanguage("SHARED_KEY", LANGUAGE));
    assertEquals("Module one key", aweElements.getLocaleWithLanguage("MODULE_ONE_KEY", LANGUAGE));
    assertEquals("Module two key", aweElements.getLocaleWithLanguage("MODULE_TWO_KEY", LANGUAGE));
  }

  /**
   * Test that reloading menus discards poisoned entries
   */
  @Test
  void reloadMenusDiscardsPoisonedEntries() throws AWException {
    // Poison the public menu
    Menu poisonedMenu = new Menu();
    poisonedMenu.setScreen("poisoned");
    aweElements.setMenu("public", poisonedMenu);
    assertEquals("poisoned", aweElements.getMenu("public").getScreen());

    // Reload
    aweElements.reloadMenus();

    // Legit menus are back
    assertEquals("signin", aweElements.getMenu("public").getScreen());
    assertEquals("home", aweElements.getMenu("private").getScreen());
  }

  /**
   * Test that reloading profiles rebuilds the profile map
   */
  @Test
  void reloadProfilesRebuildsMap() {
    aweElements.reloadProfiles();

    assertTrue(aweElements.getProfileList().contains("general"));
  }

  /**
   * Test that reloading everything rebuilds all element maps
   */
  @Test
  void reloadAllRebuildsEveryMap() throws AWException {
    // Poison screens, menus and locales
    aweElements.setScreen((Screen) new Screen().setId("poisoned-screen"));
    Menu poisonedMenu = new Menu();
    poisonedMenu.setScreen("poisoned");
    aweElements.setMenu("public", poisonedMenu);
    aweElements.getLocales().get(LANGUAGE).put("POISONED_KEY", "poisoned");

    // Reload everything
    aweElements.reloadAll();

    // All poisoned entries are gone and legit entries are back
    assertFalse(aweElements.getScreenList().contains("poisoned-screen"));
    assertEquals("signin", aweElements.getMenu("public").getScreen());
    assertEquals("POISONED_KEY", aweElements.getLocaleWithLanguage("POISONED_KEY", LANGUAGE));
    assertEquals("serviceModuleOne", aweElements.getQuery("sharedQuery").getService());
    assertTrue(aweElements.getProfileList().contains("general"));
  }

  /**
   * Test that the remaining per-type reload methods rebuild their maps without errors
   */
  @Test
  void reloadRemainingTypesDoesNotFail() {
    assertDoesNotThrow(() -> {
      aweElements.reloadEnumerated();
      aweElements.reloadQueues();
      aweElements.reloadMaintains();
      aweElements.reloadEmails();
      aweElements.reloadServices();
      aweElements.reloadActions();
    });
  }

  /**
   * Test that a reload producing an empty query map keeps the previous definitions
   */
  @Test
  void reloadQueriesKeepsPreviousDefinitionsOnEmptyRebuild() throws AWException {
    baseConfigProperties.setModuleList(new String[]{"missing-module"});

    aweElements.reloadQueries();

    assertEquals("serviceModuleOne", aweElements.getQuery("sharedQuery").getService());
    assertNotNull(aweElements.getQuery("moduleOneQuery"));
  }

  /**
   * Test that a reload producing an empty screen map keeps the previous definitions
   */
  @Test
  void reloadScreensKeepsPreviousDefinitionsOnEmptyRebuild() {
    baseConfigProperties.setModuleList(new String[]{"missing-module"});

    aweElements.reloadScreens();

    assertTrue(aweElements.getScreenList().contains("flat-screen"));
    assertTrue(aweElements.getScreenList().contains("sub-screen"));
  }

  /**
   * Test that a reload producing empty locale maps keeps the previous definitions
   */
  @Test
  void reloadLocalesKeepsPreviousDefinitionsOnEmptyRebuild() {
    baseConfigProperties.setModuleList(new String[]{"missing-module"});

    aweElements.reloadLocales();

    assertEquals("Module one value", aweElements.getLocaleWithLanguage("SHARED_KEY", LANGUAGE));
  }

  /**
   * Test that a menu reload which cannot read the menu files keeps the previous menus
   */
  @Test
  void reloadMenusKeepsPreviousDefinitionsWhenMenusCannotBeRead() throws AWException {
    baseConfigProperties.setModuleList(new String[]{"missing-module"});

    assertDoesNotThrow(() -> aweElements.reloadMenus());

    assertEquals("signin", aweElements.getMenu("public").getScreen());
    assertEquals("home", aweElements.getMenu("private").getScreen());
  }

  /**
   * Test that a failed future does not poison the calling thread interrupt flag
   */
  @Test
  void waitForMapDoesNotInterruptOnExecutionFailure() {
    try {
      Map<String, String> result = aweElements.waitForMap(
        CompletableFuture.failedFuture(new IllegalStateException("Parse failure")), "test");

      assertTrue(result.isEmpty());
      assertFalse(Thread.currentThread().isInterrupted());
    } finally {
      // Clear the interrupt flag to avoid poisoning other tests if the assertion fails
      Thread.interrupted();
    }
  }
}
