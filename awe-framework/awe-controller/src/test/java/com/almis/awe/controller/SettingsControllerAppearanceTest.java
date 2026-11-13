package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.util.theme.PaletteGenerator;
import com.almis.awe.model.util.theme.ThemeAppearance;
import com.almis.awe.service.InitService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.ThemeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The appearance endpoint returns only the AWE 5 theme appearance of the session (#885, S8), without the side effects of
 * the settings request
 */
@ExtendWith(MockitoExtension.class)
class SettingsControllerAppearanceTest {

  @Mock
  private MenuService menuService;

  @Mock
  private InitService initService;

  @Mock
  private ThemeService themeService;

  @Mock
  private ApplicationContext applicationContext;

  private SettingsController settingsController;

  @BeforeEach
  void setUp() {
    settingsController = new SettingsController(menuService, initService, new BaseConfigProperties(), themeService);
    settingsController.setApplicationContext(applicationContext);
  }

  @Test
  void returnsTheFiveAppearanceFieldsOfTheSession() {
    Map<String, String> palette = PaletteGenerator.primary("#1a73e8");
    when(themeService.getAppearance()).thenReturn(new ThemeAppearance("#1a73e8", "stone", "dark", "compact", palette));

    Map<String, Object> appearance = settingsController.getAppearance();

    assertThat(appearance).containsExactly(
      Map.entry("themePrimary", "#1a73e8"),
      Map.entry("themeSurface", "stone"),
      Map.entry("themeMode", "dark"),
      Map.entry("density", "compact"),
      Map.entry("themePalette", palette));
  }

  @Test
  void serializesAsTheAppearanceFieldsOfTheSettings() {
    when(themeService.getAppearance()).thenReturn(new ThemeAppearance("blue", "auto", "system", "comfortable", Map.of()));

    JsonNode json = new ObjectMapper().valueToTree(settingsController.getAppearance());

    assertThat(json.propertyNames()).containsExactly("themePrimary", "themeSurface", "themeMode", "density", "themePalette");
    assertThat(json.get("themePrimary").asText()).isEqualTo("blue");
    assertThat(json.get("themeSurface").asText()).isEqualTo("auto");
    assertThat(json.get("themeMode").asText()).isEqualTo("system");
    assertThat(json.get("density").asText()).isEqualTo("comfortable");
    assertThat(json.get("themePalette").isObject()).isTrue();
    assertThat(json.get("themePalette").size()).isZero();
  }

  @Test
  void doesNotLaunchTheClientStartServicesNorReadTheSettings() {
    when(themeService.getAppearance()).thenReturn(new ThemeAppearance("blue", "auto", "system", "comfortable", Map.of()));

    settingsController.getAppearance();

    verifyNoInteractions(initService, menuService, applicationContext);
  }
}
