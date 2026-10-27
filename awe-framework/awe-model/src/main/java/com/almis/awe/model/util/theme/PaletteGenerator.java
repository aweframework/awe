package com.almis.awe.model.util.theme;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Generates the AWE 5 palette of a hex colour: the {@code --awe-primary-*} or the {@code --awe-surface-*} custom
 * properties a theme renders on {@code <html>} when its primary or its surface is a hex instead of a palette name.
 * <p>
 * The hex is converted to OKLCH; its hue and chroma are kept and every step gets a fixed lightness, so the contrast
 * pairs of the semantic tokens hold for any hue. The chroma is clamped into the sRGB gamut per step; the surface chroma
 * is capped so a saturated hex only tints the neutrals. The primary fill is chosen by measured contrast. The output is
 * the one of the generator of the design system prototype (website/static/design-system/prototype/prototype.js).
 * </p>
 */
public final class PaletteGenerator {

  private static final List<String> STEPS = List.of("50", "100", "200", "300", "400", "500", "600", "700", "800",
    "900", "950");
  private static final Map<String, Double> PRIMARY_LIGHTNESS = steps(0.97, 0.935, 0.885, 0.81, 0.72, 0.64, 0.56, 0.49,
    0.43, 0.38, 0.28);
  // Very light steps keep a pastel chroma, as the named palettes do
  private static final Map<String, Double> PRIMARY_CHROMA_CAP = Map.of("50", 0.035, "100", 0.07);
  private static final Map<String, Double> SURFACE_LIGHTNESS = steps(0.985, 0.968, 0.929, 0.869, 0.711, 0.554, 0.446,
    0.372, 0.279, 0.208, 0.129);
  private static final double SURFACE_CHROMA_MAX = 0.03;
  private static final Map<String, Double> SURFACE_CHROMA_PROFILE = steps(0.35, 0.45, 0.6, 0.75, 0.9, 1, 1, 1, 0.95,
    0.9, 0.8);
  // Lightest dark-mode background a primary text or fill sits on (surface 900 with the highlight mix)
  private static final double DARK_REFERENCE_LIGHTNESS = 0.3;
  private static final double AA = 4.5;
  private static final String WHITE = "#ffffff";
  private static final Pattern HEX = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
  private static final String PRIMARY_PREFIX = "--awe-primary-";
  private static final String SURFACE_PREFIX = "--awe-surface-";

  private PaletteGenerator() {
  }

  /**
   * @param value Text
   * @return Whether the text is a hex colour ({@code #RGB} or {@code #RRGGBB})
   */
  public static boolean isHex(String value) {
    return value != null && HEX.matcher(value.trim()).matches();
  }

  /**
   * Generate the primary palette of a hex
   *
   * @param hex Hex colour ({@code #RGB} or {@code #RRGGBB})
   * @return {@code --awe-primary-50} to {@code -950} and the fill tokens, in that order
   */
  public static Map<String, String> primary(String hex) {
    Oklch base = toOklch(normalize(hex));
    Map<String, Oklch> scale = scale(base, PRIMARY_LIGHTNESS,
      step -> Math.min(base.chroma(), PRIMARY_CHROMA_CAP.getOrDefault(step, Double.POSITIVE_INFINITY)));
    String darkReference = toHex(new Oklch(DARK_REFERENCE_LIGHTNESS, Math.min(base.chroma(), 0.04), base.hue()));

    // Light mode: primary text (700) on white, on the highlight (50) and on the subtle background (100)
    ensure(scale, "700", colour -> contrast(colour, WHITE) >= AA && contrast(colour, toHex(scale.get("50"))) >= AA
      && contrast(colour, toHex(scale.get("100"))) >= AA, -1);
    ensure(scale, "800", colour -> contrast(colour, toHex(scale.get("50"))) >= AA
      && contrast(colour, toHex(scale.get("100"))) >= AA, -1);
    // Dark mode: primary text and fill (400) and the subtle text (300) on the dark surfaces
    ensure(scale, "400", colour -> contrast(colour, darkReference) >= AA, 1);
    ensure(scale, "300", colour -> contrast(colour, darkReference) >= AA, 1);

    Map<String, String> hexes = new LinkedHashMap<>();
    STEPS.forEach(step -> hexes.put(step, toHex(scale.get(step))));
    Fill fill = fill(base, scale, hexes);

    Map<String, String> palette = new LinkedHashMap<>();
    STEPS.forEach(step -> palette.put(PRIMARY_PREFIX + step, hexes.get(step)));
    palette.put(PRIMARY_PREFIX + "fill", hexes.get(fill.step()));
    palette.put(PRIMARY_PREFIX + "fill-hover", hexes.get(fill.hover()));
    palette.put(PRIMARY_PREFIX + "fill-active", hexes.get(fill.active()));
    palette.put(PRIMARY_PREFIX + "on-fill", fill.on());
    return palette;
  }

  /**
   * Generate the surface palette of a hex
   *
   * @param hex Hex colour ({@code #RGB} or {@code #RRGGBB})
   * @return {@code --awe-surface-0} to {@code -950}, in that order
   */
  public static Map<String, String> surface(String hex) {
    Oklch base = toOklch(normalize(hex));
    double chroma = Math.min(base.chroma(), SURFACE_CHROMA_MAX);
    Map<String, Oklch> scale = scale(base, SURFACE_LIGHTNESS, step -> chroma * SURFACE_CHROMA_PROFILE.get(step));

    // Light: muted text (500) on white and on the page (50). Dark: muted text (400) on the dark surfaces
    ensure(scale, "500", colour -> contrast(colour, WHITE) >= AA && contrast(colour, toHex(scale.get("50"))) >= AA
      && contrast(colour, toHex(scale.get("100"))) >= AA, -1);
    ensure(scale, "400", colour -> contrast(colour, toHex(scale.get("800"))) >= AA
      && contrast(colour, toHex(scale.get("900"))) >= AA, 1);

    Map<String, String> palette = new LinkedHashMap<>();
    palette.put(SURFACE_PREFIX + "0", WHITE);
    STEPS.forEach(step -> palette.put(SURFACE_PREFIX + step, toHex(scale.get(step))));
    return palette;
  }

  // The fill: the step closest to the brand lightness whose label (white or the 950 step) reaches AA
  private static Fill fill(Oklch base, Map<String, Oklch> scale, Map<String, String> hexes) {
    String dark = hexes.get("950");
    List<Fill> candidates = new ArrayList<>();
    for (String step : List.of("500", "600", "700", "800")) {
      int index = STEPS.indexOf(step);
      String hover = STEPS.get(index + 1);
      String active = STEPS.get(Math.min(index + 2, STEPS.size() - 1));
      if (List.of(step, hover, active).stream().allMatch(s -> contrast(hexes.get(s), WHITE) >= AA)) {
        candidates.add(new Fill(step, hover, active, WHITE, Math.abs(scale.get(step).lightness() - base.lightness())));
      }
    }
    for (String step : List.of("300", "400", "500", "600")) {
      int index = STEPS.indexOf(step);
      String hover = STEPS.get(index - 1);
      String active = List.of(STEPS.get(index + 1), STEPS.get(index - 2)).stream()
        .filter(s -> contrast(hexes.get(s), dark) >= AA).findFirst().orElse(null);
      if (active != null && List.of(step, hover).stream().allMatch(s -> contrast(hexes.get(s), dark) >= AA)) {
        candidates.add(new Fill(step, hover, active, dark,
          Math.abs(scale.get(step).lightness() - base.lightness()) + 0.001));
      }
    }
    return candidates.stream().min(Comparator.comparingDouble(Fill::distance))
      .orElse(new Fill("800", "900", "950", WHITE, 0));
  }

  private static Map<String, Double> steps(double... values) {
    Map<String, Double> steps = new LinkedHashMap<>();
    for (int index = 0; index < values.length; index++) {
      steps.put(STEPS.get(index), values[index]);
    }
    return steps;
  }

  private static Map<String, Oklch> scale(Oklch base, Map<String, Double> lightness,
                                          java.util.function.ToDoubleFunction<String> chromaAt) {
    Map<String, Oklch> scale = new LinkedHashMap<>();
    STEPS.forEach(step -> scale.put(step, new Oklch(lightness.get(step), chromaAt.applyAsDouble(step), base.hue())));
    return scale;
  }

  // Move one step's lightness (by 0.005) until the check passes; keeps the hue and chroma
  private static void ensure(Map<String, Oklch> scale, String step, java.util.function.Predicate<String> check,
                             int direction) {
    for (int attempt = 0; attempt < 60 && !check.test(toHex(scale.get(step))); attempt++) {
      Oklch colour = scale.get(step);
      scale.put(step, new Oklch(colour.lightness() + 0.005 * direction, colour.chroma(), colour.hue()));
    }
  }

  private static String normalize(String value) {
    String hex = ThemeOptions.normalizeHex(value);
    if (hex == null) {
      throw new IllegalArgumentException("Not a hex colour (#RGB or #RRGGBB): " + value);
    }
    return hex.toLowerCase(Locale.ROOT);
  }

  private static double toLinear(double channel) {
    return channel <= 0.04045 ? channel / 12.92 : StrictMath.pow((channel + 0.055) / 1.055, 2.4);
  }

  private static double fromLinear(double channel) {
    return channel <= 0.0031308 ? 12.92 * channel : 1.055 * StrictMath.pow(channel, 1 / 2.4) - 0.055;
  }

  private static double[] toRgb(String hex) {
    return new double[]{Integer.parseInt(hex.substring(1, 3), 16) / 255.0,
      Integer.parseInt(hex.substring(3, 5), 16) / 255.0, Integer.parseInt(hex.substring(5, 7), 16) / 255.0};
  }

  private static Oklch toOklch(String hex) {
    double[] rgb = toRgb(hex);
    double r = toLinear(rgb[0]);
    double g = toLinear(rgb[1]);
    double b = toLinear(rgb[2]);
    double l = StrictMath.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
    double m = StrictMath.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
    double s = StrictMath.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
    double lightness = 0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s;
    double a = 1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s;
    double bAxis = 0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s;
    return new Oklch(lightness, StrictMath.hypot(a, bAxis), StrictMath.atan2(bAxis, a));
  }

  // Linear sRGB of an OKLCH colour (may be out of gamut)
  private static double[] toLinearRgb(double lightness, double chroma, double hue) {
    double a = chroma * StrictMath.cos(hue);
    double b = chroma * StrictMath.sin(hue);
    double l = StrictMath.pow(lightness + 0.3963377774 * a + 0.2158037573 * b, 3);
    double m = StrictMath.pow(lightness - 0.1055613458 * a - 0.0638541728 * b, 3);
    double s = StrictMath.pow(lightness - 0.0894841775 * a - 1.291485548 * b, 3);
    return new double[]{4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
      -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
      -0.0041960863 * l - 0.7034186147 * m + 1.7076949378 * s};
  }

  private static boolean inGamut(double[] linear) {
    for (double channel : linear) {
      if (channel < -1e-4 || channel > 1 + 1e-4) {
        return false;
      }
    }
    return true;
  }

  // Keep the lightness and the hue, reduce the chroma until the colour fits in sRGB (binary search)
  private static String toHex(Oklch colour) {
    double chroma = colour.chroma();
    if (!inGamut(toLinearRgb(colour.lightness(), chroma, colour.hue()))) {
      double low = 0;
      double high = chroma;
      for (int iteration = 0; iteration < 24; iteration++) {
        double middle = (low + high) / 2;
        if (inGamut(toLinearRgb(colour.lightness(), middle, colour.hue()))) {
          low = middle;
        } else {
          high = middle;
        }
      }
      chroma = low;
    }
    StringBuilder hex = new StringBuilder("#");
    for (double channel : toLinearRgb(colour.lightness(), chroma, colour.hue())) {
      long value = Math.round(Math.min(1, Math.max(0, fromLinear(channel))) * 255);
      hex.append(String.format("%02x", value));
    }
    return hex.toString();
  }

  private static double luminance(String hex) {
    double[] rgb = toRgb(hex);
    return 0.2126 * toLinear(rgb[0]) + 0.7152 * toLinear(rgb[1]) + 0.0722 * toLinear(rgb[2]);
  }

  private static double contrast(String first, String second) {
    double a = luminance(first);
    double b = luminance(second);
    return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
  }

  private record Oklch(double lightness, double chroma, double hue) {
  }

  private record Fill(String step, String hover, String active, String on, double distance) {
  }
}
