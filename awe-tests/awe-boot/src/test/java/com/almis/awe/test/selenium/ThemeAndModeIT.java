package com.almis.awe.test.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Themes and dark mode with the AWE 5 tokens (#770, #885): the user chooses a theme and a mode in the user menu, the
 * page root carries the mode class and the primary palette of the theme preset, and the page shows the colors the
 * tokens give them. The test user starts with the sunset theme; each test starts in the light mode, whatever mode the
 * session starts in (another session of the same user may have left the dark one).
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ThemeAndModeIT extends AbstractSessionTests {

  private static final String USER_THEME = "sunset";

  // Elements whose computed colors the tests check
  private static final String BODY = "body";
  private static final String PRIMARY_BUTTON = ".btn-awe.btn-primary";
  private static final String BACKGROUND_COLOR = "background-color";
  private static final String COLOR = "color";

  // Token values: surface auto is slate in light and zinc in dark
  private static final String LIGHT_BACKGROUND = "rgb(248, 250, 252)";
  private static final String LIGHT_TEXT = "rgb(51, 65, 85)";
  private static final String DARK_BACKGROUND = "rgb(9, 9, 11)";
  private static final String DARK_TEXT = "rgb(255, 255, 255)";

  // Red primary: the fill (600) in light, the 400 shade in dark
  private static final String RED_PRIMARY_LIGHT = "rgb(220, 38, 38)";
  private static final String RED_PRIMARY_DARK = "rgb(248, 113, 113)";

  /**
   * Set while a test has changed the theme or chosen the dark mode, so the cleanup puts them back even when it fails
   */
  private boolean themeChanged = false;
  private boolean darkChosen = false;

  ThemeAndModeIT() {
    super(Session.LOGGED_IN);
  }

  /**
   * Start in the light mode: the user menu offers the dark mode only to a user who is not in it
   */
  @BeforeEach
  void startInLightMode() {
    switchToLightModeIfDark();
  }

  /**
   * Put the theme of the test user and the light mode back, as the next tests expect them
   */
  @AfterEach
  void restoreThemeAndMode() {
    // Each step restores one thing, so a failure of the first does not leave the second changed
    try {
      if (darkChosen) {
        darkChosen = false;
        switchToLightMode();
      }
    } finally {
      if (themeChanged) {
        themeChanged = false;
        selectTheme(USER_THEME);
      }
    }
  }

  /**
   * The default theme (sky preset) shows the colors of its dark mode and of its light mode. The user menu offers the
   * light mode only after the dark one, so the test chooses both and does not depend on what the operating system of
   * the runner prefers or on the mode the session starts in
   */
  @Test
  void t001_darkModeChangesTheColorsOfTheDefaultTheme() {
    setTestTitle("Dark mode changes the colors of the default theme");

    themeChanged = true;
    selectTheme("default");
    checkThemeApplied("default");

    // Dark mode chosen: the page has the dark class and not the light one
    darkChosen = true;
    switchToDarkMode();
    checkColorMode("dark");
    checkThemeAppearance("dark", "sky");
    checkComputedStyle(BODY, BACKGROUND_COLOR, DARK_BACKGROUND);
    checkComputedStyle(BODY, COLOR, DARK_TEXT);

    // Light mode chosen: the page has the light class and not the dark one (it wins over the operating system)
    switchToLightMode();
    darkChosen = false;
    checkColorMode("light");
    checkThemeAppearance("light", "sky");
    checkComputedStyle(BODY, BACKGROUND_COLOR, LIGHT_BACKGROUND);
    checkComputedStyle(BODY, COLOR, LIGHT_TEXT);
  }

  /**
   * Another theme shows its own primary in both modes, and the mode stays when the theme changes
   */
  @Test
  void t002_themeChangesThePrimaryInBothModes() {
    setTestTitle("Another theme changes the primary in both modes");

    // Sunset (red preset), dark mode and then light mode
    themeChanged = true;
    selectTheme("sunset");
    checkThemeApplied("sunset");
    darkChosen = true;
    switchToDarkMode();
    checkColorMode("dark");
    checkThemeAppearance("dark", "red");
    checkComputedStyle(PRIMARY_BUTTON, BACKGROUND_COLOR, RED_PRIMARY_DARK);
    switchToLightMode();
    darkChosen = false;
    checkColorMode("light");
    checkThemeAppearance("light", "red");
    checkComputedStyle(PRIMARY_BUTTON, BACKGROUND_COLOR, RED_PRIMARY_LIGHT);

    // Another theme (blue preset) in the dark mode
    darkChosen = true;
    switchToDarkMode();
    selectTheme("sky");
    checkThemeApplied("sky");
    checkColorMode("dark");
    checkThemeAppearance("dark", "blue");
    checkComputedStyle(BODY, BACKGROUND_COLOR, DARK_BACKGROUND);
  }
}
