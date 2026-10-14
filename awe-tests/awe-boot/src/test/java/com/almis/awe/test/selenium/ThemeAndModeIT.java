package com.almis.awe.test.selenium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Themes, dark mode and design tokens (#770): the user chooses a theme and a mode in the user menu and the page shows
 * the values that theme and mode give to the design tokens. The values are those of the LESS themes, so the test also
 * catches a theme that stops rendering with its colors. The test user starts with the sunset theme.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ThemeAndModeIT extends AbstractSessionTests {

  private static final String USER_THEME = "sunset";

  /**
   * Set while a test has changed the theme or chosen the dark mode, so the cleanup puts them back even when it fails
   */
  private boolean themeChanged = false;
  private boolean darkChosen = false;

  ThemeAndModeIT() {
    super(Session.LOGGED_IN);
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
   * The default theme shows the tokens of its dark mode and of its light mode. The user menu offers the light mode only
   * after the dark one, so the test chooses both and does not depend on what the operating system of the runner prefers
   */
  @Test
  void t001_darkModeChangesTheTokensOfTheDefaultTheme() {
    setTestTitle("Dark mode changes the tokens of the default theme");

    themeChanged = true;
    selectTheme("default");
    checkThemeApplied("default");

    // Dark mode chosen: the page has the dark class and not the light one
    darkChosen = true;
    switchToDarkMode();
    checkColorMode("dark");
    checkDesignToken("--awe-color-text", "#eee");
    checkDesignToken("--awe-color-background", "#060606");

    // Light mode chosen: the page has the light class and not the dark one (it wins over the operating system)
    switchToLightMode();
    darkChosen = false;
    checkColorMode("light");
    checkDesignToken("--awe-color-text", "#333333");
    checkDesignToken("--awe-color-background", "#f4f6f7");
  }

  /**
   * Another theme shows its own tokens in both modes, and the mode stays when the theme changes
   */
  @Test
  void t002_themeChangesTheTokensInBothModes() {
    setTestTitle("Another theme changes the tokens in both modes");

    // Sunset, dark mode and then light mode
    themeChanged = true;
    selectTheme("sunset");
    checkThemeApplied("sunset");
    darkChosen = true;
    switchToDarkMode();
    checkColorMode("dark");
    checkDesignToken("--awe-color-theme", "#721717");
    switchToLightMode();
    darkChosen = false;
    checkColorMode("light");
    checkDesignToken("--awe-color-theme", "#d05a5a");

    // Another theme in the dark mode
    darkChosen = true;
    switchToDarkMode();
    selectTheme("sky");
    checkThemeApplied("sky");
    checkColorMode("dark");
    checkDesignToken("--awe-color-background", "#060606");
  }
}
