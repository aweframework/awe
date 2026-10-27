package com.almis.awe.service;

import com.almis.awe.config.BaseConfigProperties;
import com.almis.awe.config.ServiceConfig;
import com.almis.awe.exception.AWException;
import com.almis.awe.model.component.AweSession;
import com.almis.awe.model.dto.CellData;
import com.almis.awe.model.dto.DataList;
import com.almis.awe.model.dto.ServiceData;
import com.almis.awe.model.util.data.DataListUtil;
import com.almis.awe.model.util.theme.PaletteGenerator;
import com.almis.awe.model.util.theme.ThemeAppearance;
import com.almis.awe.model.util.theme.ThemeOptions;
import com.almis.awe.model.util.theme.ThemePresets;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.almis.awe.model.constant.AweConstants.SESSION_DENSITY;
import static com.almis.awe.model.constant.AweConstants.SESSION_THEME;
import static com.almis.awe.model.constant.AweConstants.SESSION_THEME_MODE;

/**
 * Resolves the AWE 5 appearance of a theme: primary colour, surface, mode, density and the palette generated from a hex.
 * <p>
 * Each value comes from the first valid source: the mode and the density chosen by the user, the application properties
 * (awe.application.theme-primary, theme-surface, theme-mode and density), the AweThm row of the theme name, the preset of
 * the classic theme name and finally emerald, auto, system and comfortable. An invalid value is skipped with a warning
 * that names the source but not the value. A hex primary or surface is exposed as #RRGGBB in upper case.
 * </p>
 */
@Slf4j
public class ThemeService extends ServiceConfig {

  private static final String PRESET_SOURCE = "preset";

  private static final String THEME_QUERY = "getThemeAppearance";
  private static final String USER_SOURCE = "the user settings";
  private static final String CUSTOM = "custom";
  private static final String INVALID_COLOR_TITLE = "ERROR_TITLE_INVALID_THEME_COLOR";

  private final QueryService queryService;
  private final BaseConfigProperties baseConfigProperties;

  /**
   * Theme service constructor
   *
   * @param queryService         Query service
   * @param baseConfigProperties Base configuration properties
   */
  public ThemeService(QueryService queryService, BaseConfigProperties baseConfigProperties) {
    this.queryService = queryService;
    this.baseConfigProperties = baseConfigProperties;
  }

  /**
   * Resolve the appearance of the session theme (the application theme without session theme) with the mode and the
   * density of the session user
   *
   * @return Theme appearance
   */
  public ThemeAppearance getAppearance() {
    AweSession session = getSession();
    String theme = Optional.ofNullable(session).map(s -> s.getParameter(String.class, SESSION_THEME))
      .orElse(baseConfigProperties.getTheme());
    String userMode = Optional.ofNullable(session).map(s -> s.getParameter(String.class, SESSION_THEME_MODE)).orElse(null);
    String userDensity = Optional.ofNullable(session).map(s -> s.getParameter(String.class, SESSION_DENSITY)).orElse(null);
    return resolve(theme, userMode, userDensity);
  }

  /**
   * Resolve the appearance of a theme
   *
   * @param themeName   Theme name
   * @param userMode    Mode chosen by the user (null when the user did not choose one)
   * @param userDensity Density chosen by the user (null when the user did not choose one)
   * @return Theme appearance
   */
  public ThemeAppearance resolve(String themeName, String userMode, String userDensity) {
    Map<String, String> row = themeRow(themeName);
    Optional<ThemePresets.Preset> preset = ThemePresets.of(themeName);
    String themeSource = "theme '" + themeName + "'";

    String primary = pick("primary", ThemeOptions::isPrimary, ThemeOptions.DEFAULT_PRIMARY,
      new Source("awe.application.theme-primary", baseConfigProperties.getThemePrimary()),
      new Source(themeSource, row.get("Pri")),
      new Source(PRESET_SOURCE, preset.map(ThemePresets.Preset::primary).orElse(null)));
    String surface = pick("surface", ThemeOptions::isSurface, ThemeOptions.DEFAULT_SURFACE,
      new Source("awe.application.theme-surface", baseConfigProperties.getThemeSurface()),
      new Source(themeSource, row.get("Srf")),
      new Source(PRESET_SOURCE, preset.map(ThemePresets.Preset::surface).orElse(null)));
    String mode = pick("mode", ThemeOptions::isMode, ThemeOptions.DEFAULT_MODE,
      new Source(USER_SOURCE, userMode),
      new Source("awe.application.theme-mode", baseConfigProperties.getThemeMode()),
      new Source(themeSource, row.get("Mde")),
      new Source(PRESET_SOURCE, preset.map(ThemePresets.Preset::mode).orElse(null)));
    String density = pick("density", ThemeOptions::isDensity, ThemeOptions.DEFAULT_DENSITY,
      new Source(USER_SOURCE, userDensity),
      new Source("awe.application.density", baseConfigProperties.getDensity()));

    Map<String, String> palette = new LinkedHashMap<>();
    if (PaletteGenerator.isHex(primary)) {
      palette.putAll(PaletteGenerator.primary(primary));
    }
    if (PaletteGenerator.isHex(surface)) {
      palette.putAll(PaletteGenerator.surface(surface));
    }
    return new ThemeAppearance(primary, surface, mode, density, Collections.unmodifiableMap(palette));
  }

  /**
   * Compose the primary colour and the surface a theme stores from the palette selects and the colour pickers of the
   * theme screens: the palette name, or the hex of the picker when the palette is custom (the picker is ignored
   * otherwise). Both values are trimmed: a palette name in lower case, a hex as #RRGGBB in upper case.
   *
   * @param primaryPalette Primary palette or custom
   * @param primaryHex     Primary hex, used when the primary palette is custom
   * @param surfacePalette Surface palette, auto or custom
   * @param surfaceHex     Surface hex, used when the surface palette is custom
   * @return One row with the Pri and Srf columns
   * @throws AWException The palette is not one of the theme or the custom hex is not a #RGB or #RRGGBB colour
   */
  public ServiceData composeColors(String primaryPalette, String primaryHex, String surfacePalette, String surfaceHex)
    throws AWException {
    String primary = composeColor(ThemeOptions.PRIMARIES, primaryPalette, primaryHex, "ERROR_MESSAGE_INVALID_THEME_PRIMARY");
    String surface = composeColor(ThemeOptions.SURFACES, surfacePalette, surfaceHex, "ERROR_MESSAGE_INVALID_THEME_SURFACE");
    DataList dataList = new DataList();
    DataListUtil.addColumnWithOneRow(dataList, "Pri", primary);
    DataListUtil.addColumnWithOneRow(dataList, "Srf", surface);
    return new ServiceData().setDataList(dataList);
  }

  private String composeColor(List<String> palettes, String palette, String hex, String invalidMessage)
    throws AWException {
    String value = ThemeOptions.normalize(palette);
    if (CUSTOM.equals(value)) {
      value = ThemeOptions.normalize(hex);
      if (PaletteGenerator.isHex(value)) {
        return value;
      }
    } else if (value != null && palettes.contains(value)) {
      return value;
    }
    throw new AWException(getLocale(INVALID_COLOR_TITLE), getLocale(invalidMessage));
  }

  /**
   * Retrieve the primary colour, surface and mode of the AweThm row of a theme name
   *
   * @param themeName Theme name
   * @return Pri, Srf and Mde of the row, empty when the theme has no row
   */
  private Map<String, String> themeRow(String themeName) {
    if (themeName == null) {
      return Map.of();
    }
    ObjectNode parameters = JsonNodeFactory.instance.objectNode();
    parameters.put("theme", themeName);
    try {
      DataList dataList = queryService.launchPrivateQuery(THEME_QUERY, parameters).getDataList();
      if (dataList == null || dataList.getRows().isEmpty()) {
        return Map.of();
      }
      Map<String, String> row = new LinkedHashMap<>();
      dataList.getRows().get(0).forEach((column, cell) ->
        row.put(column, Optional.ofNullable(cell).map(CellData::getStringValue).orElse(null)));
      return row;
    } catch (AWException exc) {
      log.warn("The appearance of the theme '{}' could not be read, using its preset", themeName, exc);
      return Map.of();
    }
  }

  /**
   * Pick the first valid value of the sources
   *
   * @param field        Field name (for the log)
   * @param valid        Validation of a value
   * @param defaultValue Value when no source has a valid one
   * @param sources      Sources by priority
   * @return Normalized value
   */
  private String pick(String field, Predicate<String> valid, String defaultValue, Source... sources) {
    for (Source source : sources) {
      String value = ThemeOptions.normalize(source.value());
      if (value != null) {
        if (valid.test(value)) {
          return value;
        }
        log.warn("Ignoring the invalid theme {} of {}", field, source.name());
      }
    }
    return defaultValue;
  }

  private record Source(String name, String value) {
  }
}
