package com.almis.awe.service.report;

import com.almis.ade.api.enumerate.ChipTone;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Colors of the printed grids. A report has no CSS variables, so the values of the AWE 5 design tokens are copied here
 * (source: website/static/design-system/prototype/tokens.css, light mode).
 */
final class PrintPalette {

  private static final String NEUTRAL = "neutral";
  private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
  private static final Pattern SHORT_HEX_COLOR = Pattern.compile("#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])");

  // --awe-palette-<name>-500: the palettes a grid column may name (same list as the swatch of the clients)
  private static final Map<String, String> PALETTES = Map.ofEntries(
    Map.entry("red", "#ef4444"), Map.entry("orange", "#f97316"), Map.entry("amber", "#f59e0b"),
    Map.entry("yellow", "#eab308"), Map.entry("lime", "#84cc16"), Map.entry("green", "#22c55e"),
    Map.entry("emerald", "#10b981"), Map.entry("teal", "#14b8a6"), Map.entry("cyan", "#06b6d4"),
    Map.entry("sky", "#0ea5e9"), Map.entry("blue", "#3b82f6"), Map.entry("indigo", "#6366f1"),
    Map.entry("violet", "#8b5cf6"), Map.entry("purple", "#a855f7"), Map.entry("fuchsia", "#d946ef"),
    Map.entry("pink", "#ec4899"), Map.entry("rose", "#f43f5e"), Map.entry("slate", "#64748b"),
    Map.entry("gray", "#6b7280"), Map.entry("zinc", "#71717a"), Map.entry(NEUTRAL, "#737373"),
    Map.entry("stone", "#78716c"));

  // Tones of a status (--awe-color-success, --awe-color-warning, --awe-color-danger, --awe-color-info: light mode)
  private static final Map<String, String> TONES = Map.of(
    "success", "#15803d", "warning", "#c2410c", "danger", "#dc2626", "info", "#0369a1", NEUTRAL, "#64748b");

  // Background of the group rows, from the surface tokens: --awe-surface-200 / -100 / -50 (slate, light mode)
  static final String GROUP_BACKGROUND_LEVEL_1 = "#e2e8f0";
  static final String GROUP_BACKGROUND_LEVEL_2 = "#f1f5f9";
  static final String GROUP_BACKGROUND_DEEPER = "#f8fafc";

  private PrintPalette() {
  }

  /**
   * Tone of a status value as ADE knows it
   *
   * @param tone Tone value of the row (any case, may have spaces)
   * @return Normalized tone name: one of the known tones, neutral otherwise
   */
  static String toneName(String tone) {
    String name = tone == null ? "" : tone.trim().toLowerCase(Locale.ROOT);
    return TONES.containsKey(name) ? name : NEUTRAL;
  }

  /**
   * Tones of ADE by the name used as chip value
   *
   * @return Chip tones by normalized tone name
   */
  static Map<String, ChipTone> chipTones() {
    return Map.of("success", ChipTone.SUCCESS, "warning", ChipTone.WARNING, "danger", ChipTone.DANGER,
      "info", ChipTone.INFO, NEUTRAL, ChipTone.NEUTRAL);
  }

  /**
   * Color for a sparkline: a #RRGGBB hex as it is, a #RGB hex expanded, or the name of a status tone or of a palette
   * of the design tokens
   *
   * @param color Color declared in the column
   * @return Color as #RRGGBB, null when it is not valid (ADE uses its default color)
   */
  static String hexColor(String color) {
    String value = color == null ? "" : color.trim();
    if (HEX_COLOR.matcher(value).matches()) {
      return value;
    }
    var shortHex = SHORT_HEX_COLOR.matcher(value);
    if (shortHex.matches()) {
      return "#" + shortHex.group(1).repeat(2) + shortHex.group(2).repeat(2) + shortHex.group(3).repeat(2);
    }
    String name = value.toLowerCase(Locale.ROOT);
    return PALETTES.containsKey(name) ? PALETTES.get(name) : TONES.get(name);
  }
}
