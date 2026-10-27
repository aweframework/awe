package com.almis.awe.model.util.theme;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Presets of the classic theme names (#885, S6): the AWE 5 palette nearest to the brand colour of each name
 */
class ThemePresetsTest {

  @Test
  void mapsTheNineteenClassicNames() {
    assertThat(ThemePresets.all()).containsExactly(
      Map.entry("default", new ThemePresets.Preset("sky", "auto", "system")),
      Map.entry("asphalt", new ThemePresets.Preset("teal", "auto", "system")),
      Map.entry("purple-hills", new ThemePresets.Preset("violet", "auto", "system")),
      Map.entry("dust", new ThemePresets.Preset("#948787", "stone", "system")),
      Map.entry("frost", new ThemePresets.Preset("cyan", "slate", "system")),
      Map.entry("fresh", new ThemePresets.Preset("lime", "auto", "system")),
      Map.entry("silver", new ThemePresets.Preset("cyan", "auto", "system")),
      Map.entry("clean", new ThemePresets.Preset("emerald", "auto", "system")),
      Map.entry("white", new ThemePresets.Preset("cyan", "auto", "system")),
      Map.entry("adminflare", new ThemePresets.Preset("blue", "auto", "system")),
      Map.entry("orange", new ThemePresets.Preset("red", "stone", "system")),
      Map.entry("sunset", new ThemePresets.Preset("red", "auto", "system")),
      Map.entry("grass", new ThemePresets.Preset("cyan", "auto", "system")),
      Map.entry("sky", new ThemePresets.Preset("blue", "auto", "system")),
      Map.entry("eclipse", new ThemePresets.Preset("sky", "auto", "system")),
      Map.entry("sunny", new ThemePresets.Preset("yellow", "auto", "system")),
      Map.entry("sea", new ThemePresets.Preset("blue", "slate", "system")),
      Map.entry("ferrari", new ThemePresets.Preset("red", "auto", "system")),
      Map.entry("amazonia", new ThemePresets.Preset("#948787", "stone", "system")));
  }

  @Test
  void everyPresetHoldsValidValues() {
    ThemePresets.all().forEach((name, preset) -> {
      assertThat(ThemeOptions.isPrimary(preset.primary())).as(name).isTrue();
      assertThat(ThemeOptions.isSurface(preset.surface())).as(name).isTrue();
      assertThat(ThemeOptions.isMode(preset.mode())).as(name).isTrue();
    });
  }

  @Test
  void findsAPresetByItsName() {
    assertThat(ThemePresets.of("amazonia")).contains(new ThemePresets.Preset("#948787", "stone", "system"));
    assertThat(ThemePresets.of("custom-theme")).isEmpty();
    assertThat(ThemePresets.of(null)).isEmpty();
  }
}
