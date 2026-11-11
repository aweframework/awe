package com.almis.awe.controller;

import com.almis.awe.config.BaseConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Static resources controller: the styles of the application. The theme is on the {@code <html>} attributes since
 * AWE 5 (#885), so the theme variables stylesheet is not served any more
 */
class StaticResourcesControllerTest {

  private BaseConfigProperties baseConfigProperties;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    baseConfigProperties = new BaseConfigProperties();
    mockMvc = MockMvcBuilders.standaloneSetup(new StaticResourcesController(baseConfigProperties)).build();
  }

  @Test
  void stylesTakeTheImagesOfTheApplication() throws Exception {
    var result = mockMvc.perform(get("/css/styles-123.css")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getViewName()).isEqualTo("styles.css");
    assertThat(result.getModelAndView().getModel())
      .containsEntry("startupLogo", baseConfigProperties.getPaths().getImageStartupLogo())
      .containsEntry("startupBackground", baseConfigProperties.getPaths().getImageStartupBackground())
      .containsEntry("navbarLogo", baseConfigProperties.getPaths().getImageNavbarLogo());
  }

  @Test
  void indexTakesTheSplashLogoOfTheApplication() throws Exception {
    baseConfigProperties.getPaths().setImageSplashLogo("images/logo/my-mark.svg");

    var result = mockMvc.perform(get("/")).andReturn();

    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getViewName()).isEqualTo("index");
    assertThat(result.getModelAndView().getModel()).containsEntry("splashLogo", "images/logo/my-mark.svg");
  }

  @Test
  void anEmptySplashLogoKeepsTheBuiltInMark() throws Exception {
    baseConfigProperties.getPaths().setImageSplashLogo(" ");

    var result = mockMvc.perform(get("/")).andReturn();

    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getModel()).containsEntry("splashLogo", null);
  }

  @ParameterizedTest
  @CsvSource(value = {"light,light", "dark,dark", "system,NULL", "'',NULL"}, nullValues = "NULL")
  void indexTakesTheLightOrDarkModeOfTheApplicationForTheSplash(String themeMode, String expected) throws Exception {
    baseConfigProperties.setThemeMode(themeMode);

    var result = mockMvc.perform(get("/")).andReturn();

    assertThat(result.getModelAndView()).isNotNull();
    assertThat(result.getModelAndView().getModel()).containsEntry("themeMode", expected);
  }

  @Test
  void themeVariablesAreNotServedAnyMore() throws Exception {
    var result = mockMvc.perform(get("/css/themeVariables-123.css")).andReturn();

    assertThat(result.getResponse().getStatus()).isEqualTo(404);
  }
}
