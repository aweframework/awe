package com.almis.awe.model.util.theme;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The values a theme accepts (#885, S9): a primary is one of the 17 palettes or a hex, a surface one of the 5 neutral
 * palettes, auto or a hex, a mode light, dark or system and a density comfortable or compact
 */
class ThemeOptionsTest {

  @Test
  void listsThePalettesOfTheTokenFile() {
    assertThat(ThemeOptions.PRIMARIES).containsExactly("emerald", "green", "teal", "cyan", "sky", "blue", "indigo",
      "violet", "purple", "fuchsia", "pink", "rose", "red", "orange", "amber", "yellow", "lime");
    assertThat(ThemeOptions.SURFACES).containsExactly("slate", "gray", "zinc", "neutral", "stone", "auto");
    assertThat(ThemeOptions.MODES).containsExactly("light", "dark", "system");
    assertThat(ThemeOptions.DENSITIES).containsExactly("comfortable", "compact");
  }

  @Test
  void defaultsToEmeraldAutoSystemAndComfortable() {
    assertThat(ThemeOptions.DEFAULT_PRIMARY).isEqualTo("emerald");
    assertThat(ThemeOptions.DEFAULT_SURFACE).isEqualTo("auto");
    assertThat(ThemeOptions.DEFAULT_MODE).isEqualTo("system");
    assertThat(ThemeOptions.DEFAULT_DENSITY).isEqualTo("comfortable");
  }

  @ParameterizedTest
  @ValueSource(strings = {"emerald", "lime", "Violet", " sky ", "#abc", "#1A73E8"})
  void acceptsPaletteNamesAndHexesAsPrimary(String value) {
    assertThat(ThemeOptions.isPrimary(value)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"auto", "slate", "brown", "#12", "#12345g", "1a73e8", "#1a73e8ff"})
  void rejectsOtherPrimaries(String value) {
    assertThat(ThemeOptions.isPrimary(value)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"slate", "gray", "zinc", "neutral", "stone", "auto", "STONE", "#948787"})
  void acceptsNeutralPalettesAutoAndHexesAsSurface(String value) {
    assertThat(ThemeOptions.isSurface(value)).isTrue();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"emerald", "white", "#xyz"})
  void rejectsOtherSurfaces(String value) {
    assertThat(ThemeOptions.isSurface(value)).isFalse();
  }

  @Test
  void acceptsTheModesAndTheDensities() {
    assertThat(ThemeOptions.isMode("light")).isTrue();
    assertThat(ThemeOptions.isMode("Dark")).isTrue();
    assertThat(ThemeOptions.isMode("system")).isTrue();
    assertThat(ThemeOptions.isMode("auto")).isFalse();
    assertThat(ThemeOptions.isMode(null)).isFalse();
    assertThat(ThemeOptions.isDensity("comfortable")).isTrue();
    assertThat(ThemeOptions.isDensity("compact")).isTrue();
    assertThat(ThemeOptions.isDensity("cozy")).isFalse();
    assertThat(ThemeOptions.isDensity("")).isFalse();
  }

  @Test
  void normalizesNamesToTrimmedLowerCase() {
    assertThat(ThemeOptions.normalize(" Violet ")).isEqualTo("violet");
    assertThat(ThemeOptions.normalize("DARK")).isEqualTo("dark");
    assertThat(ThemeOptions.normalize("#12")).isEqualTo("#12");
    assertThat(ThemeOptions.normalize("  ")).isNull();
    assertThat(ThemeOptions.normalize(null)).isNull();
  }

  /**
   * Every theme hex is #RRGGBB in upper case (#885, S13)
   */
  @Test
  void normalizesHexesToUpperCaseRrggbb() {
    assertThat(ThemeOptions.normalize("#3C2E60")).isEqualTo("#3C2E60");
    assertThat(ThemeOptions.normalize(" #1a73e8 ")).isEqualTo("#1A73E8");
    assertThat(ThemeOptions.normalize("#abc")).isEqualTo("#AABBCC");
    assertThat(ThemeOptions.normalize("#AbC")).isEqualTo("#AABBCC");
  }

  @Test
  void normalizeHexExpandsAndUpperCasesOnlyHexes() {
    assertThat(ThemeOptions.normalizeHex("#3c2e60")).isEqualTo("#3C2E60");
    assertThat(ThemeOptions.normalizeHex(" #f0a ")).isEqualTo("#FF00AA");
    assertThat(ThemeOptions.normalizeHex("#12")).isNull();
    assertThat(ThemeOptions.normalizeHex("#1a73e8ff")).isNull();
    assertThat(ThemeOptions.normalizeHex("violet")).isNull();
    assertThat(ThemeOptions.normalizeHex(null)).isNull();
  }
}
