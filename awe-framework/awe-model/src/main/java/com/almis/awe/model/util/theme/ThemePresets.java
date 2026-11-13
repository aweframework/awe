package com.almis.awe.model.util.theme;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Presets of the classic theme names: each name maps to the AWE 5 palette nearest to its brand colour (a grey brand
 * keeps its hex), the surface is auto unless the classic background was tinted, and the mode follows the system.
 * <p>
 * They apply when a theme has no row in AweThm (as {@code amazonia}, which only exists in LESS) or its row has no valid
 * primary colour, surface or mode. The V1.5.0 migration sets the same values on the classic rows.
 * </p>
 */
public final class ThemePresets {

  private static final String STONE = "stone";

  private static final Map<String, Preset> PRESETS = presets();

  private ThemePresets() {
  }

  /**
   * Primary colour, surface and mode of a classic theme
   *
   * @param primary Primary palette or hex colour
   * @param surface Surface palette, auto or hex colour
   * @param mode    Mode
   */
  public record Preset(String primary, String surface, String mode) {
  }

  /**
   * @param themeName Theme name
   * @return Preset of the theme name, empty when it is not a classic name
   */
  public static Optional<Preset> of(String themeName) {
    return Optional.ofNullable(themeName).map(PRESETS::get);
  }

  /**
   * @return Presets of the classic names, in the order of the classic themes
   */
  public static Map<String, Preset> all() {
    return PRESETS;
  }

  private static Map<String, Preset> presets() {
    Map<String, Preset> presets = new LinkedHashMap<>();
    presets.put("default", preset("sky", "auto"));
    presets.put("asphalt", preset("teal", "auto"));
    presets.put("purple-hills", preset("violet", "auto"));
    presets.put("dust", preset("#948787", STONE));
    presets.put("frost", preset("cyan", "slate"));
    presets.put("fresh", preset("lime", "auto"));
    presets.put("silver", preset("cyan", "auto"));
    presets.put("clean", preset("emerald", "auto"));
    presets.put("white", preset("cyan", "auto"));
    presets.put("adminflare", preset("blue", "auto"));
    presets.put("orange", preset("red", STONE));
    presets.put("sunset", preset("red", "auto"));
    presets.put("grass", preset("cyan", "auto"));
    presets.put("sky", preset("blue", "auto"));
    presets.put("eclipse", preset("sky", "auto"));
    presets.put("sunny", preset("yellow", "auto"));
    presets.put("sea", preset("blue", "slate"));
    presets.put("ferrari", preset("red", "auto"));
    presets.put("amazonia", preset("#948787", STONE));
    return Collections.unmodifiableMap(presets);
  }

  private static Preset preset(String primary, String surface) {
    return new Preset(primary, surface, ThemeOptions.DEFAULT_MODE);
  }
}
