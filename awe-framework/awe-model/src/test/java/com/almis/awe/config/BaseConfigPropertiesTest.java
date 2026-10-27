package com.almis.awe.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Url of the chart server (awe.application.chart-server-url). The Highcharts export server property is gone
 */
class BaseConfigPropertiesTest {

  private static BaseConfigProperties bind(Map<String, String> properties) {
    return new Binder(new MapConfigurationPropertySource(properties))
      .bindOrCreate("awe.application", Bindable.of(BaseConfigProperties.class));
  }

  @Test
  @DisplayName("Without properties the chart server is the local one")
  void defaultChartServerUrl() {
    assertThat(bind(Map.of()).getChartServerUrl()).isEqualTo("http://localhost:7801");
  }

  @Test
  @DisplayName("chart-server-url sets the chart server")
  void chartServerUrl() {
    BaseConfigProperties properties = bind(Map.of("awe.application.chart-server-url", "http://charts:9000"));

    assertThat(properties.getChartServerUrl()).isEqualTo("http://charts:9000");
  }

  @Test
  @DisplayName("The removed highcharts-server-url property no longer sets the chart server")
  void highchartsServerUrlIsIgnored() {
    BaseConfigProperties properties = bind(Map.of("awe.application.highcharts-server-url", "http://legacy:8080"));

    assertThat(properties.getChartServerUrl()).isEqualTo("http://localhost:7801");
  }

  @Test
  @DisplayName("Without properties the map tiles come from OpenStreetMap")
  void defaultMapTileUrl() {
    assertThat(bind(Map.of()).getMap().getTileUrl()).isEqualTo("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
  }

  @Test
  @DisplayName("map.tile-url sets the tile server of the maps")
  void mapTileUrl() {
    BaseConfigProperties properties = bind(Map.of("awe.application.map.tile-url", "https://tiles.example.com/{z}/{x}/{y}.png"));

    assertThat(properties.getMap().getTileUrl()).isEqualTo("https://tiles.example.com/{z}/{x}/{y}.png");
  }

  @Test
  @DisplayName("Without properties the theme appearance comes from the theme name and the density is comfortable")
  void defaultThemeAppearance() {
    BaseConfigProperties properties = bind(Map.of());

    assertThat(properties.getTheme()).isEqualTo("sky");
    assertThat(properties.getThemePrimary()).isNull();
    assertThat(properties.getThemeSurface()).isNull();
    assertThat(properties.getThemeMode()).isNull();
    assertThat(properties.getDensity()).isEqualTo("comfortable");
  }

  @Test
  @DisplayName("theme-primary, theme-surface, theme-mode and density set the theme appearance")
  void themeAppearance() {
    BaseConfigProperties properties = bind(Map.of(
      "awe.application.theme-primary", "#1a73e8",
      "awe.application.theme-surface", "slate",
      "awe.application.theme-mode", "dark",
      "awe.application.density", "compact"));

    assertThat(properties.getThemePrimary()).isEqualTo("#1a73e8");
    assertThat(properties.getThemeSurface()).isEqualTo("slate");
    assertThat(properties.getThemeMode()).isEqualTo("dark");
    assertThat(properties.getDensity()).isEqualTo("compact");
  }

  @Test
  @DisplayName("The default navigation bar logo is the AWE 5 mark (#885), whose wordmark the AWE theme fills")
  void defaultNavbarLogoIsTheAwe5Mark() {
    assertThat(bind(Map.of()).getPaths().getImageNavbarLogo()).isEqualTo("../images/logo/awe-logo-mark.svg");
  }
  @Test
  @DisplayName("Without properties the sign-in page is the split layout with the AWE logo, panel, texts and selectors")
  void defaultLogin() {
    BaseConfigProperties.Login login = bind(Map.of()).getLogin();

    assertThat(login.getLayout()).isEqualTo("split");
    assertThat(login.getLogo()).isNull();
    assertThat(login.getLogoDark()).isNull();
    assertThat(login.getBackground()).isNull();
    assertThat(login.getTitle()).isEqualTo("SCREEN_TEXT_LOGIN_TITLE");
    assertThat(login.getSubtitle()).isEqualTo("SCREEN_TEXT_LOGIN_SUBTITLE");
    assertThat(login.getPanelTitle()).isEqualTo("SCREEN_TEXT_LOGIN_PANEL_TITLE");
    assertThat(login.getPanelSubtitle()).isEqualTo("SCREEN_TEXT_LOGIN_PANEL_SUBTITLE");
    assertThat(login.getFooterText()).isNull();
    assertThat(login.isPoweredBy()).isTrue();
    assertThat(login.isLanguageSelector()).isTrue();
    assertThat(login.isModeSelector()).isTrue();
    assertThat(login.getBackgroundEffect()).isNull();
  }

  @Test
  @DisplayName("background-effect is the name of a canvas effect the application registers, any name (S23)")
  void loginBackgroundEffect() {
    BaseConfigProperties.Login login = bind(Map.of("awe.application.login.background-effect", "brand-wave")).getLogin();

    assertThat(login.getBackgroundEffect()).isEqualTo("brand-wave");
  }

  @Test
  @DisplayName("language-selector and mode-selector hide the selectors of the sign-in footer")
  void loginSelectors() {
    BaseConfigProperties.Login login = bind(Map.of(
      "awe.application.login.language-selector", "false",
      "awe.application.login.mode-selector", "false")).getLogin();

    assertThat(login.isLanguageSelector()).isFalse();
    assertThat(login.isModeSelector()).isFalse();
  }

  @Test
  @DisplayName("The login.* properties brand the sign-in page")
  void login() {
    BaseConfigProperties.Login login = bind(Map.ofEntries(
      Map.entry("awe.application.login.layout", "card"),
      Map.entry("awe.application.login.logo", "images/acme.svg"),
      Map.entry("awe.application.login.logo-dark", "images/acme-dark.svg"),
      Map.entry("awe.application.login.background", "https://cdn.acme.com/bg.jpg"),
      Map.entry("awe.application.login.title", "Welcome to Acme"),
      Map.entry("awe.application.login.subtitle", "ACME_LOGIN_SUBTITLE"),
      Map.entry("awe.application.login.panel-title", "Operations Hub"),
      Map.entry("awe.application.login.panel-subtitle", "From the warehouse to the door"),
      Map.entry("awe.application.login.footer-text", "Acme © 2026"),
      Map.entry("awe.application.login.powered-by", "false"))).getLogin();

    assertThat(login.getLayout()).isEqualTo("card");
    assertThat(login.getLogo()).isEqualTo("images/acme.svg");
    assertThat(login.getLogoDark()).isEqualTo("images/acme-dark.svg");
    assertThat(login.getBackground()).isEqualTo("https://cdn.acme.com/bg.jpg");
    assertThat(login.getTitle()).isEqualTo("Welcome to Acme");
    assertThat(login.getSubtitle()).isEqualTo("ACME_LOGIN_SUBTITLE");
    assertThat(login.getPanelTitle()).isEqualTo("Operations Hub");
    assertThat(login.getPanelSubtitle()).isEqualTo("From the warehouse to the door");
    assertThat(login.getFooterText()).isEqualTo("Acme © 2026");
    assertThat(login.isPoweredBy()).isFalse();
  }
}
