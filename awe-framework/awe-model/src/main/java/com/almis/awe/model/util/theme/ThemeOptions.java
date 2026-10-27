package com.almis.awe.model.util.theme;

import java.util.List;
import java.util.Locale;

/**
 * Values of the AWE 5 theme appearance: the primary colour (a palette of the token file or a hex), the surface (a
 * neutral palette, auto or a hex), the mode and the density, with the defaults used when no source gives a valid value
 */
public final class ThemeOptions {

  public static final String DEFAULT_PRIMARY = "emerald";
  public static final String DEFAULT_SURFACE = "auto";
  public static final String DEFAULT_MODE = "system";
  public static final String DEFAULT_DENSITY = "comfortable";

  public static final List<String> PRIMARIES = List.of(DEFAULT_PRIMARY, "green", "teal", "cyan", "sky", "blue", "indigo",
    "violet", "purple", "fuchsia", "pink", "rose", "red", "orange", "amber", "yellow", "lime");
  public static final List<String> SURFACES = List.of("slate", "gray", "zinc", "neutral", "stone", DEFAULT_SURFACE);
  public static final List<String> MODES = List.of("light", "dark", DEFAULT_MODE);
  public static final List<String> DENSITIES = List.of(DEFAULT_DENSITY, "compact");

  private ThemeOptions() {
  }

  /**
   * @param value Value
   * @return The value trimmed: a hex colour as {@link #normalizeHex(String)}, any other value in lower case; null
   * when it is empty
   */
  public static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String hex = normalizeHex(value);
    return hex != null ? hex : value.trim().toLowerCase(Locale.ROOT);
  }

  /**
   * Format every theme hex the same way: {@code #RRGGBB} in upper case, a short {@code #RGB} expanded
   * ({@code #abc} -> {@code #AABBCC})
   *
   * @param value Value
   * @return The hex trimmed, expanded and in upper case, null when the value is not a {@code #RGB} or {@code #RRGGBB}
   * colour
   */
  public static String normalizeHex(String value) {
    if (!PaletteGenerator.isHex(value)) {
      return null;
    }
    String digits = value.trim().substring(1).toUpperCase(Locale.ROOT);
    if (digits.length() == 3) {
      StringBuilder expanded = new StringBuilder();
      for (char digit : digits.toCharArray()) {
        expanded.append(digit).append(digit);
      }
      digits = expanded.toString();
    }
    return "#" + digits;
  }

  private static boolean isOneOf(List<String> values, String value) {
    String normalized = normalize(value);
    return normalized != null && values.contains(normalized);
  }

  /**
   * @param value Value
   * @return Whether the value is a primary palette or a hex colour
   */
  public static boolean isPrimary(String value) {
    return isOneOf(PRIMARIES, value) || PaletteGenerator.isHex(value);
  }

  /**
   * @param value Value
   * @return Whether the value is a surface palette, auto or a hex colour
   */
  public static boolean isSurface(String value) {
    return isOneOf(SURFACES, value) || PaletteGenerator.isHex(value);
  }

  /**
   * @param value Value
   * @return Whether the value is light, dark or system
   */
  public static boolean isMode(String value) {
    return isOneOf(MODES, value);
  }

  /**
   * @param value Value
   * @return Whether the value is comfortable or compact
   */
  public static boolean isDensity(String value) {
    return isOneOf(DENSITIES, value);
  }
}
