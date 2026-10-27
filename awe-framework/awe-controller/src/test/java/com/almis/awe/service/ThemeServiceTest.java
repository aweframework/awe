package com.almis.awe.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweElements;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.theme.PaletteGenerator;
import com.almis.awe.model.util.theme.ThemeAppearance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.almis.awe.model.constant.AweConstants.SESSION_DENSITY;
import static com.almis.awe.model.constant.AweConstants.SESSION_THEME;
import static com.almis.awe.model.constant.AweConstants.SESSION_THEME_MODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Resolution of the AWE 5 theme appearance (#885, S5, S6, S9): the AweThm row of the theme name or its preset, then the
 * application properties, then the mode and density of the user; invalid or empty values fall back to the next source
 * and finally to emerald, auto, system and comfortable. The theme screens compose the stored primary and surface from a
 * palette select and a colour picker (S12)
 */
@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

  private static final String QUERY = "getThemeAppearance";

  @Mock
  private QueryService queryService;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private AweSession aweSession;

  private BaseConfigProperties properties;
  private ThemeService themeService;
  private final Logger logger = (Logger) LoggerFactory.getLogger(ThemeService.class);
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    properties = new BaseConfigProperties();
    themeService = new ThemeService(queryService, properties);
    themeService.setApplicationContext(applicationContext);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  @Test
  void takesTheAweThmRowOfTheTheme() throws Exception {
    givenRow("violet", "zinc", "dark");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("violet", "zinc", "dark", "comfortable", Map.of()));
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq(QUERY), parameters.capture());
    assertThat(parameters.getValue().get("theme").asText()).isEqualTo("sky");
    assertThat(appender.list).isEmpty();
  }

  @Test
  void takesThePresetOfAClassicNameWithoutRow() throws Exception {
    givenNoRow();

    ThemeAppearance appearance = themeService.resolve("amazonia", null, null);

    assertThat(appearance)
      .isEqualTo(new ThemeAppearance("#948787", "stone", "system", "comfortable", PaletteGenerator.primary("#948787")));
    assertThat(appender.list).isEmpty();
  }

  @Test
  void takesTheDefaultsForAnUnknownNameWithoutRow() throws Exception {
    givenNoRow();

    assertThat(themeService.resolve("custom", null, null))
      .isEqualTo(new ThemeAppearance("emerald", "auto", "system", "comfortable", Map.of()));
    assertThat(appender.list).isEmpty();
  }

  @Test
  void takesThePresetForTheNullColumnsOfAClassicRow() throws Exception {
    givenRow(null, "slate", null);

    assertThat(themeService.resolve("sunset", null, null))
      .isEqualTo(new ThemeAppearance("red", "slate", "system", "comfortable", Map.of()));
    assertThat(appender.list).isEmpty();
  }

  @Test
  void takesTheDefaultsForTheNullColumnsOfACustomRow() throws Exception {
    givenRow(null, null, null);

    assertThat(themeService.resolve("custom", null, null))
      .isEqualTo(new ThemeAppearance("emerald", "auto", "system", "comfortable", Map.of()));
  }

  @Test
  void ignoresTheInvalidValuesOfTheRowWithOneWarningEachThatDoesNotShowThem() throws Exception {
    givenRow("brown-secret", "white-secret", "dim-secret");

    assertThat(themeService.resolve("sea", null, null))
      .isEqualTo(new ThemeAppearance("blue", "slate", "system", "comfortable", Map.of()));
    assertThat(appender.list).hasSize(3).allSatisfy(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.WARN);
      assertThat(event.getFormattedMessage()).doesNotContain("secret");
    });
    assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
      .anySatisfy(message -> assertThat(message).contains("primary"))
      .anySatisfy(message -> assertThat(message).contains("surface"))
      .anySatisfy(message -> assertThat(message).contains("mode"));
  }

  @Test
  void treatsAFailingQueryAsNoRow() throws Exception {
    when(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class))).thenThrow(new AWException("title", "message"));

    assertThat(themeService.resolve("frost", null, null))
      .isEqualTo(new ThemeAppearance("cyan", "slate", "system", "comfortable", Map.of()));
    assertThat(appender.list).hasSize(1).allSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
  }

  @Test
  void thePrimaryPropertyAloneOverridesTheTheme() throws Exception {
    givenRow("violet", "zinc", "dark");
    properties.setThemePrimary("Teal");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("teal", "zinc", "dark", "comfortable", Map.of()));
  }

  @Test
  void theSurfacePropertyAloneOverridesTheTheme() throws Exception {
    givenRow("violet", "zinc", "dark");
    properties.setThemeSurface("stone");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("violet", "stone", "dark", "comfortable", Map.of()));
  }

  @Test
  void theModePropertyAloneOverridesTheTheme() throws Exception {
    givenRow("violet", "zinc", "dark");
    properties.setThemeMode("light");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("violet", "zinc", "light", "comfortable", Map.of()));
  }

  @Test
  void theDensityPropertySetsTheDensity() throws Exception {
    givenNoRow();
    properties.setDensity("compact");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("blue", "auto", "system", "compact", Map.of()));
  }

  @Test
  void invalidPropertiesFallBackToTheThemeWithOneWarningEach() throws Exception {
    givenRow("violet", "zinc", "dark");
    properties.setThemePrimary("brown-secret");
    properties.setThemeSurface("#12");
    properties.setThemeMode("dim-secret");
    properties.setDensity("cozy-secret");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("violet", "zinc", "dark", "comfortable", Map.of()));
    assertThat(appender.list).hasSize(4).allSatisfy(event -> {
      assertThat(event.getLevel()).isEqualTo(Level.WARN);
      assertThat(event.getFormattedMessage()).doesNotContain("secret").contains("awe.application.");
    });
  }

  @Test
  void theUserModeAndDensityOverrideThePropertiesAndTheTheme() throws Exception {
    givenRow("violet", "zinc", "dark");
    properties.setThemeMode("system");
    properties.setDensity("comfortable");

    assertThat(themeService.resolve("sky", "light", "compact"))
      .isEqualTo(new ThemeAppearance("violet", "zinc", "light", "compact", Map.of()));
  }

  @Test
  void anEmptyUserModeOrDensityKeepsTheOtherSourcesSilently() throws Exception {
    givenRow("violet", "zinc", "dark");

    assertThat(themeService.resolve("sky", "", " "))
      .isEqualTo(new ThemeAppearance("violet", "zinc", "dark", "comfortable", Map.of()));
    assertThat(appender.list).isEmpty();
  }

  @Test
  void anInvalidUserModeOrDensityFallsBackWithAWarningThatDoesNotShowIt() throws Exception {
    givenRow("violet", "zinc", "dark");

    assertThat(themeService.resolve("sky", "dim-secret", "cozy-secret"))
      .isEqualTo(new ThemeAppearance("violet", "zinc", "dark", "comfortable", Map.of()));
    assertThat(appender.list).hasSize(2)
      .allSatisfy(event -> assertThat(event.getFormattedMessage()).doesNotContain("secret").contains("user"));
  }

  @Test
  void aHexPrimaryGeneratesItsPalette() throws Exception {
    givenRow("#1A73E8", "slate", "system");

    assertThat(themeService.resolve("custom", null, null))
      .isEqualTo(new ThemeAppearance("#1A73E8", "slate", "system", "comfortable", PaletteGenerator.primary("#1a73e8")));
  }

  @Test
  void aLowerCaseOrShortHexOfTheRowIsExposedAsUpperCaseRrggbb() throws Exception {
    givenRow("#abc", "#78716c", null);

    ThemeAppearance appearance = themeService.resolve("custom", null, null);

    assertThat(appearance.primary()).isEqualTo("#AABBCC");
    assertThat(appearance.surface()).isEqualTo("#78716C");
    // The generated palette keeps the output of the prototype generator
    Map<String, String> palette = new LinkedHashMap<>(PaletteGenerator.primary("#abc"));
    palette.putAll(PaletteGenerator.surface("#78716c"));
    assertThat(appearance.palette()).isEqualTo(palette);
  }

  @Test
  void aLowerCaseHexPropertyIsExposedAsUpperCaseRrggbb() throws Exception {
    givenRow("violet", "zinc", "dark");
    properties.setThemePrimary(" #3c2e60 ");
    properties.setThemeSurface("#f4a");

    Map<String, String> palette = new LinkedHashMap<>(PaletteGenerator.primary("#3c2e60"));
    palette.putAll(PaletteGenerator.surface("#ff44aa"));
    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("#3C2E60", "#FF44AA", "dark", "comfortable", palette));
  }

  @Test
  void aHexSurfaceGeneratesItsPalette() throws Exception {
    givenNoRow();
    properties.setThemeSurface("#475569");

    assertThat(themeService.resolve("sky", null, null))
      .isEqualTo(new ThemeAppearance("blue", "#475569", "system", "comfortable", PaletteGenerator.surface("#475569")));
  }

  @Test
  void aHexPrimaryAndSurfaceGenerateBothPalettes() throws Exception {
    givenRow("#948787", "#948787", null);

    Map<String, String> palette = new LinkedHashMap<>(PaletteGenerator.primary("#948787"));
    palette.putAll(PaletteGenerator.surface("#948787"));
    assertThat(themeService.resolve("custom", null, null))
      .isEqualTo(new ThemeAppearance("#948787", "#948787", "system", "comfortable", palette));
  }

  @Test
  void resolvesTheSessionThemeModeAndDensity() throws Exception {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    when(aweSession.getParameter(String.class, SESSION_THEME)).thenReturn("sunset");
    when(aweSession.getParameter(String.class, SESSION_THEME_MODE)).thenReturn("dark");
    when(aweSession.getParameter(String.class, SESSION_DENSITY)).thenReturn("compact");
    givenNoRow();

    assertThat(themeService.getAppearance())
      .isEqualTo(new ThemeAppearance("red", "auto", "dark", "compact", Map.of()));
    ArgumentCaptor<ObjectNode> parameters = ArgumentCaptor.forClass(ObjectNode.class);
    verify(queryService).launchPrivateQuery(eq(QUERY), parameters.capture());
    assertThat(parameters.getValue().get("theme").asText()).isEqualTo("sunset");
  }

  @Test
  void resolvesTheApplicationThemeWithoutSessionTheme() throws Exception {
    when(applicationContext.getBean(AweSession.class)).thenReturn(aweSession);
    properties.setTheme("frost");
    givenNoRow();

    assertThat(themeService.getAppearance())
      .isEqualTo(new ThemeAppearance("cyan", "slate", "system", "comfortable", Map.of()));
  }

  @Test
  void resolvesTheApplicationThemeWithoutSession() throws Exception {
    when(applicationContext.getBean(AweSession.class)).thenThrow(new IllegalStateException("No session"));
    givenNoRow();

    assertThat(themeService.getAppearance())
      .isEqualTo(new ThemeAppearance("blue", "auto", "system", "comfortable", Map.of()));
  }

  @Test
  void composesThePalettesOfTheSelects() throws Exception {
    assertThat(colors(themeService.composeColors(" Violet ", "#123456", "ZINC", "#654321")))
      .containsExactly("violet", "zinc");
    assertThat(colors(themeService.composeColors("lime", null, "auto", null))).containsExactly("lime", "auto");
  }

  @Test
  void composesTheHexesOfTheCustomPickers() throws Exception {
    assertThat(colors(themeService.composeColors("custom", " #1A73E8 ", "Custom", "#334155")))
      .containsExactly("#1A73E8", "#334155");
  }

  @Test
  void composesALowerCaseHexInUpperCase() throws Exception {
    assertThat(colors(themeService.composeColors("custom", "#3c2e60", "custom", "#78716c")))
      .containsExactly("#3C2E60", "#78716C");
  }

  @Test
  void composesAShortHexExpanded() throws Exception {
    assertThat(colors(themeService.composeColors("custom", "#abc", "custom", " #F0a ")))
      .containsExactly("#AABBCC", "#FF00AA");
  }

  @Test
  void rejectsAnInvalidPrimary() {
    givenLocales();
    for (String[] primary : new String[][]{{"custom", "#12"}, {"custom", null}, {"custom", "blue"}, {"chartreuse", null},
      {"slate", null}, {null, "#1a73e8"}}) {
      assertThatThrownBy(() -> themeService.composeColors(primary[0], primary[1], "auto", null))
        .isInstanceOf(AWException.class)
        .hasFieldOrPropertyWithValue("title", "ERROR_TITLE_INVALID_THEME_COLOR")
        .hasMessage("ERROR_MESSAGE_INVALID_THEME_PRIMARY");
    }
  }

  @Test
  void rejectsAnInvalidSurface() {
    givenLocales();
    for (String[] surface : new String[][]{{"custom", "#1234567"}, {"custom", ""}, {"emerald", null}, {"custom", "auto"},
      {"", null}}) {
      assertThatThrownBy(() -> themeService.composeColors("emerald", null, surface[0], surface[1]))
        .isInstanceOf(AWException.class)
        .hasFieldOrPropertyWithValue("title", "ERROR_TITLE_INVALID_THEME_COLOR")
        .hasMessage("ERROR_MESSAGE_INVALID_THEME_SURFACE");
    }
  }

  private void givenLocales() {
    AweElements elements = mock(AweElements.class);
    when(applicationContext.getBean(AweElements.class)).thenReturn(elements);
    when(elements.getLanguage()).thenReturn("en-GB");
    when(elements.getLocaleWithLanguage(anyString(), eq("en-GB"))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  private static List<String> colors(ServiceData serviceData) {
    DataList dataList = serviceData.getDataList();
    assertThat(dataList.getRows()).hasSize(1);
    return List.of(dataList.getRows().get(0).get("Pri").getStringValue(),
      dataList.getRows().get(0).get("Srf").getStringValue());
  }

  private void givenRow(String primary, String surface, String mode) throws AWException {
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "Pri", primary);
    DataListUtil.addColumnWithOneRow(dataList, "Srf", surface);
    DataListUtil.addColumnWithOneRow(dataList, "Mde", mode);
    when(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(dataList));
  }

  private void givenNoRow() throws AWException {
    when(queryService.launchPrivateQuery(eq(QUERY), any(ObjectNode.class))).thenReturn(new ServiceData().setDataList(new DataList()));
  }
}
