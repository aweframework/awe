package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.model.component.AweRequest;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.settings.WebSettings;
import com.almis.awe.model.util.theme.PaletteGenerator;
import com.almis.awe.model.util.theme.ThemeAppearance;
import com.almis.awe.service.InitService;
import com.almis.awe.service.MenuService;
import com.almis.awe.service.ThemeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static com.almis.awe.model.constant.AweConstants.SESSION_THEME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The settings payload carries the AWE 5 theme appearance (#885, S9) next to the unchanged theme name
 */
@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

  @Mock
  private MenuService menuService;

  @Mock
  private InitService initService;

  @Mock
  private ThemeService themeService;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  @Mock
  private AweRequest aweRequest;

  @Mock
  private HttpServletRequest httpServletRequest;

  private SettingsController settingsController;

  @BeforeEach
  void setUp() {
    BaseConfigProperties properties = new BaseConfigProperties();
    settingsController = new SettingsController(menuService, initService, properties, themeService);
    settingsController.setApplicationContext(applicationContext);
    when(applicationContext.getBean(WebSettings.class)).thenReturn(WebSettings.builder().theme("sky").language("en-GB").build());
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(applicationContext.getBean(AweRequest.class)).thenReturn(aweRequest);
    when(aweRequest.getToken()).thenReturn("token");
  }

  @Test
  void carriesTheThemeAppearanceAndKeepsTheThemeName() throws Exception {
    when(aweSession.getParameter(eq(String.class), anyString()))
      .thenAnswer(invocation -> SESSION_THEME.equals(invocation.getArgument(1)) ? "sunset" : null);
    Map<String, String> palette = PaletteGenerator.primary("#1a73e8");
    when(themeService.getAppearance()).thenReturn(new ThemeAppearance("#1a73e8", "stone", "dark", "compact", palette));

    WebSettings settings = settingsController.getSettings(httpServletRequest);

    assertThat(settings.getTheme()).isEqualTo("sunset");
    assertThat(settings.getThemePrimary()).isEqualTo("#1a73e8");
    assertThat(settings.getThemeSurface()).isEqualTo("stone");
    assertThat(settings.getThemeMode()).isEqualTo("dark");
    assertThat(settings.getDensity()).isEqualTo("compact");
    assertThat(settings.getThemePalette()).isEqualTo(palette);
    assertThat(settings.getCometUID()).isEqualTo("token");
  }

  @Test
  void serializesTheThemeAppearanceAsFlatFieldsAndAPaletteObject() throws Exception {
    when(themeService.getAppearance()).thenReturn(new ThemeAppearance("blue", "auto", "system", "comfortable", Map.of()));

    JsonNode json = new ObjectMapper().valueToTree(settingsController.getSettings(httpServletRequest));

    // Without session theme the theme name is the application one, as before
    assertThat(json.get("theme").asText()).isEqualTo("sky");
    assertThat(json.get("themePrimary").asText()).isEqualTo("blue");
    assertThat(json.get("themeSurface").asText()).isEqualTo("auto");
    assertThat(json.get("themeMode").asText()).isEqualTo("system");
    assertThat(json.get("density").asText()).isEqualTo("comfortable");
    assertThat(json.get("themePalette").isObject()).isTrue();
    assertThat(json.get("themePalette").size()).isZero();
  }
}
