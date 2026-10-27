package com.almis.awe.autoconfigure;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.SecurityConfigProperties;
import com.almis.awe.model.settings.WebChartOptions;
import com.almis.awe.model.settings.WebLoginSettings;
import com.almis.awe.model.settings.WebNumberOptions;
import com.almis.awe.model.settings.WebPivotOptions;
import com.almis.awe.model.settings.WebTooltip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Settings published to the client carry the branding of the sign-in page (awe.application.login.*)
 */
class PropertyConfigLoginSettingsTest {

  private static WebLoginSettings login(BaseConfigProperties properties) {
    return new PropertyConfig().webSettings(properties, new SecurityConfigProperties(), new WebNumberOptions(),
      new WebChartOptions(), new WebTooltip(), new WebPivotOptions()).getLogin();
  }

  @Test
  @DisplayName("Without properties the sign-in page keeps the AWE defaults")
  void defaults() {
    WebLoginSettings login = login(new BaseConfigProperties());

    assertThat(login.getLayout()).isEqualTo("split");
    assertThat(login.getLogo()).isNull();
    assertThat(login.getLogoDark()).isNull();
    assertThat(login.getBackground()).isNull();
    assertThat(login.getTitle()).isEqualTo("SCREEN_TEXT_LOGIN_TITLE");
    assertThat(login.getSubtitle()).isEqualTo("SCREEN_TEXT_LOGIN_SUBTITLE");
    assertThat(login.getPanelTitle()).isEqualTo("SCREEN_TEXT_LOGIN_PANEL_TITLE");
    assertThat(login.getPanelSubtitle()).isEqualTo("SCREEN_TEXT_LOGIN_PANEL_SUBTITLE");
    assertThat(login.getFooterText()).isNull();
    assertThat(login.getPoweredBy()).isTrue();
    assertThat(login.getLanguageSelector()).isTrue();
    assertThat(login.getModeSelector()).isTrue();
    assertThat(login.getLanguages()).containsExactly("en-GB", "es-ES", "fr-FR");
    assertThat(login.getBackgroundEffect()).isNull();
  }

  @Test
  @DisplayName("The background effect is any registered name, trimmed; blank or empty is not published (S23)")
  void backgroundEffect() {
    BaseConfigProperties properties = new BaseConfigProperties();

    properties.getLogin().setBackgroundEffect("  brand-wave ");
    assertThat(login(properties).getBackgroundEffect()).isEqualTo("brand-wave");

    properties.getLogin().setBackgroundEffect("Plasma-Wave");
    assertThat(login(properties).getBackgroundEffect()).isEqualTo("Plasma-Wave");

    properties.getLogin().setBackgroundEffect("  ");
    assertThat(login(properties).getBackgroundEffect()).isNull();

    properties.getLogin().setBackgroundEffect("");
    assertThat(login(properties).getBackgroundEffect()).isNull();

    properties.getLogin().setBackgroundEffect(null);
    assertThat(login(properties).getBackgroundEffect()).isNull();
  }

  @Test
  @DisplayName("The background effect name reaches the published JSON")
  void backgroundEffectJson() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getLogin().setBackgroundEffect("brand-wave");

    JsonNode json = new JsonMapper().valueToTree(login(properties));

    assertThat(json.get("backgroundEffect").asString()).isEqualTo("brand-wave");
  }

  @Test
  @DisplayName("The selectors can be hidden and the languages come from language-list")
  void selectors() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getLogin().setLanguageSelector(false);
    properties.getLogin().setModeSelector(false);
    properties.setLanguageList(java.util.List.of("es-ES", "en-GB"));

    WebLoginSettings login = login(properties);

    assertThat(login.getLanguageSelector()).isFalse();
    assertThat(login.getModeSelector()).isFalse();
    assertThat(login.getLanguages()).containsExactly("es-ES", "en-GB");
  }

  @Test
  @DisplayName("Without properties the published JSON holds the layout, the AWE texts, powered by and the selectors")
  void defaultsJson() {
    JsonNode json = new JsonMapper().valueToTree(login(new BaseConfigProperties()));

    assertThat(json.propertyNames()).containsExactlyInAnyOrder("layout", "title", "subtitle", "panelTitle",
      "panelSubtitle", "poweredBy", "languageSelector", "modeSelector", "languages");
  }

  @Test
  @DisplayName("The login.* properties reach the client")
  void loginProperties() {
    BaseConfigProperties properties = new BaseConfigProperties();
    BaseConfigProperties.Login source = properties.getLogin();
    source.setLayout("form-only");
    source.setLogo("images/acme.svg");
    source.setLogoDark("images/acme-dark.svg");
    source.setBackground("https://cdn.acme.com/bg.jpg");
    source.setTitle("Welcome to Acme");
    source.setSubtitle("ACME_LOGIN_SUBTITLE");
    source.setPanelTitle("Operations Hub");
    source.setPanelSubtitle("From the warehouse to the door");
    source.setFooterText("Acme © 2026");
    source.setPoweredBy(false);

    WebLoginSettings login = login(properties);

    assertThat(login.getLayout()).isEqualTo("form-only");
    assertThat(login.getLogo()).isEqualTo("images/acme.svg");
    assertThat(login.getLogoDark()).isEqualTo("images/acme-dark.svg");
    assertThat(login.getBackground()).isEqualTo("https://cdn.acme.com/bg.jpg");
    assertThat(login.getTitle()).isEqualTo("Welcome to Acme");
    assertThat(login.getSubtitle()).isEqualTo("ACME_LOGIN_SUBTITLE");
    assertThat(login.getPanelTitle()).isEqualTo("Operations Hub");
    assertThat(login.getPanelSubtitle()).isEqualTo("From the warehouse to the door");
    assertThat(login.getFooterText()).isEqualTo("Acme © 2026");
    assertThat(login.getPoweredBy()).isFalse();
  }

  @Test
  @DisplayName("The layout is case insensitive and an unknown layout falls back to split")
  void layout() {
    BaseConfigProperties properties = new BaseConfigProperties();

    properties.getLogin().setLayout(" Card ");
    assertThat(login(properties).getLayout()).isEqualTo("card");

    properties.getLogin().setLayout("diagonal");
    assertThat(login(properties).getLayout()).isEqualTo("split");

    properties.getLogin().setLayout("");
    assertThat(login(properties).getLayout()).isEqualTo("split");
  }

  @Test
  @DisplayName("Blank texts and images are not published, so the page does not show them")
  void blankValues() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getLogin().setTitle(" ");
    properties.getLogin().setLogo("");

    WebLoginSettings login = login(properties);

    assertThat(login.getTitle()).isNull();
    assertThat(login.getLogo()).isNull();
  }

  @Test
  @DisplayName("A changed image-startup-logo/background is the deprecated fallback, relative to the application root")
  void startupImagesAreTheFallback() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getPaths().setImageStartupLogo("../images/logo/acme.svg");
    properties.getPaths().setImageStartupBackground("https://cdn.acme.com/bg.jpg");

    WebLoginSettings login = login(properties);

    assertThat(login.getLogo()).isEqualTo("images/logo/acme.svg");
    assertThat(login.getBackground()).isEqualTo("https://cdn.acme.com/bg.jpg");
  }

  @Test
  @DisplayName("login.logo and login.background win over the image-startup-* fallbacks")
  void loginImagesWinOverTheFallback() {
    BaseConfigProperties properties = new BaseConfigProperties();
    properties.getPaths().setImageStartupLogo("../images/logo/old.svg");
    properties.getPaths().setImageStartupBackground("../images/background/old.jpg");
    properties.getLogin().setLogo("images/new.svg");
    properties.getLogin().setBackground("images/new.jpg");

    WebLoginSettings login = login(properties);

    assertThat(login.getLogo()).isEqualTo("images/new.svg");
    assertThat(login.getBackground()).isEqualTo("images/new.jpg");
  }
}
