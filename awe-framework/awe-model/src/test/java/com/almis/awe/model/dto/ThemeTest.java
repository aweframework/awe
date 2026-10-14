package com.almis.awe.model.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Theme DTO tests: the CSS the database themes generate
 */
class ThemeTest {

  @Test
  void lightThemeDeclaresItsVariablesOnTheThemeClass() {
    Theme theme = new Theme().setName("ocean").setPrimaryColor("#123456").setPanelBackgroundColor("#fff");

    assertThat(theme).hasToString("""
      .theme-ocean {
        --primary-color: #123456;
        --panel-background-color: #fff;
      }
      """);
  }

  @Test
  void darkThemeAlsoAppliesWhenTheOperatingSystemPrefersDarkAndTheUserDidNotChoose() {
    Theme theme = new Theme().setName("ocean").setDark(true).setPrimaryColor("#abcdef");

    assertThat(theme).hasToString("""
      .theme-ocean.dark {
        --primary-color: #abcdef;
      }

      @media (prefers-color-scheme: dark) {
        .theme-ocean:not(.light):not(.dark) {
          --primary-color: #abcdef;
        }
      }
      """);
  }

  @Test
  void darkThemeDoesNotFollowTheOperatingSystemWhenTheApplicationDisablesIt() {
    Theme theme = new Theme().setName("ocean").setDark(true).setPrimaryColor("#abcdef");

    assertThat(theme.toCss(false)).isEqualTo("""
      .theme-ocean.dark {
        --primary-color: #abcdef;
      }
      """);
    assertThat(theme.toCss(true)).isEqualTo(theme.toString());
  }

  @Test
  void lightThemeIsTheSameWhetherOrNotTheApplicationFollowsTheOperatingSystem() {
    Theme theme = new Theme().setName("ocean").setPrimaryColor("#123456");

    assertThat(theme.toCss(false)).isEqualTo(theme.toString());
  }
}
