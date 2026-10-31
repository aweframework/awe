package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Themes screen (theme-admin, #793): the side panel creates two themes with the defaults of a new theme (emerald,
 * automatic surface, system mode) or other palettes, the pencil of a row edits its theme in the side panel (whose header
 * shows the theme: name, primary · surface and state) and its Active switch deactivates it, the quick filters count the
 * themes of the search (All, Active, Inactive) and filter the grid, the search finds them by name or primary colour, the
 * row menu edits them and the selection bar and the row menu delete them. The class creates its own two themes and
 * deletes them at the end.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ThemeAdminTestsIT extends AbstractCrudTests {

  private static final String GRID = "GrdThmAdm";
  private static final String QUICK_FILTER = "CrtThmSta";
  private static final String SEARCH = "CrtThmTxt";
  private static final String PANEL = "PnlThm";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String MORE_BUTTON = "MorBtn";
  // Words of the themes of the class only: the search by them lists just these two
  private static final String PREFIX = "Thmpaneltest";
  private static final String ROSE_THEME = PREFIX + " rose";
  private static final String DEFAULT_THEME = PREFIX + " default";
  private static final String DEFAULT_THEME_CHANGED = PREFIX + " teal";

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogoutWithConfirmation();
  }

  /**
   * Open the themes screen and search the themes of the class
   */
  private void searchThemesOfTheClass() {
    openScreen(TOOLS, THEMES);
    searchGrid(SEARCH, PREFIX);
  }

  /**
   * Tell whether the themes screen lists a theme of the class (a rerun finds what a failed attempt saved or deleted)
   *
   * @param name Theme name
   * @return The grid shows the theme
   */
  private boolean listed(String name) {
    searchThemesOfTheClass();
    return hasRowContents(name);
  }

  /**
   * Open the side panel of a new theme: no selected theme, so the header shows the label of the panel, and the colours
   * and the mode start with the defaults of a new theme
   */
  private void openNewThemePanel() {
    openScreen(TOOLS, THEMES);
    clickButton("ButNew");
    checkSidePanelTitle(PANEL, "Theme");
    checkSelectContents("PriPal", "Emerald");
    checkSelectContents("SrfPal", "Automatic");
    checkSelectContents("Mde", "System");
  }

  /**
   * Create a theme with the rose primary, the slate surface and the dark mode, unless it is there (a rerun)
   */
  @Test
  void t001_newThemeWithPalettesFromThePanel() {
    setTestTitle("Add a theme with other palettes from the side panel");

    if (!listed(ROSE_THEME)) {
      openNewThemePanel();
      writeText("Nam", ROSE_THEME);
      selectContain("PriPal", "Rose");
      selectContain("SrfPal", "Slate");
      selectContain("Mde", "Dark");
      clickButtonAndConfirm("ButPnlNew");
    }

    searchThemesOfTheClass();
    checkRowContentsGrid(GRID, ROSE_THEME, "Rose · Slate", "Dark", "Active");
  }

  /**
   * Create a theme with the defaults of a new theme, unless it is there (a rerun): it starts active
   */
  @Test
  void t002_newThemeWithTheDefaultsFromThePanel() {
    setTestTitle("Add a theme with the defaults from the side panel");

    if (!listed(DEFAULT_THEME) && !listed(DEFAULT_THEME_CHANGED)) {
      openNewThemePanel();
      writeText("Nam", DEFAULT_THEME);
      clickButtonAndConfirm("ButPnlNew");

      searchThemesOfTheClass();
      checkRowContentsGrid(GRID, DEFAULT_THEME, "Emerald · Automatic", "System", "Active");
    }
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the theme of its row, its header shows the theme
   * (name, primary · surface and its state), and the Active switch deactivates it while its primary changes. Help is an
   * icon-only button named by its label
   */
  @Test
  void t003_editThemeFromThePencil() {
    setTestTitle("Edit a theme from the pencil of its row");

    if (!listed(DEFAULT_THEME_CHANGED)) {
      searchThemesOfTheClass();
      checkButtonIconOnly("ButHlp", "Help");

      clickGridRowButton(GRID, DEFAULT_THEME, EDIT_BUTTON);

      checkCriterionContents("Nam", DEFAULT_THEME);
      checkSelectContents("PriPal", "Emerald");
      checkSidePanelTitle(PANEL, DEFAULT_THEME);
      checkSidePanelSubtitle(PANEL, "Emerald · Automatic");
      checkSidePanelTag(PANEL, "Active");

      writeText("Nam", DEFAULT_THEME_CHANGED);
      selectContain("PriPal", "Teal");
      checkCheckboxRadio(true, "Act");
      clickCheckbox("Act");
      checkCheckboxRadio(false, "Act");
      clickButtonAndConfirm("ButPnlUpd");
    }

    searchThemesOfTheClass();
    checkRowContentsGrid(GRID, DEFAULT_THEME_CHANGED, "Teal · Automatic", "Inactive");
  }

  /**
   * The counts of the quick filters follow the search: the themes of the class
   */
  @Test
  void t004_quickFilterCounts() {
    setTestTitle("Quick filter counts of the themes of the search");

    searchThemesOfTheClass();

    checkGridQuickFilterOption(QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 1");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
  }

  /**
   * Each quick filter shows the themes of its option, and All shows them all again
   */
  @Test
  void t005_quickFilterChoices() {
    setTestTitle("Quick filters filter the grid");

    searchThemesOfTheClass();
    checkRowContentsGrid(GRID, ROSE_THEME, DEFAULT_THEME_CHANGED);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, ROSE_THEME);
    checkRowContentsGrid(GRID, DEFAULT_THEME_CHANGED);

    clickGridQuickFilterOption(QUICK_FILTER, "active");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "active");
    checkRowGone(GRID, DEFAULT_THEME_CHANGED);
    checkRowContentsGrid(GRID, ROSE_THEME);

    // The counts stay the ones of the search, whatever the option chosen
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, ROSE_THEME, DEFAULT_THEME_CHANGED);
  }

  /**
   * The search finds the themes by name (typing, with no Enter) and by primary colour, ignoring case, the counts follow
   * it, and its clear button shows every theme again
   */
  @Test
  void t006_searchByNameOrPrimary() {
    setTestTitle("Search themes by name or primary colour");

    searchThemesOfTheClass();
    checkRowContentsGrid(GRID, ROSE_THEME, DEFAULT_THEME_CHANGED);

    typeGridSearch(SEARCH, DEFAULT_THEME_CHANGED.toUpperCase());
    checkRowGone(GRID, ROSE_THEME);
    checkRowContentsGrid(GRID, DEFAULT_THEME_CHANGED);
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 0");

    searchGrid(SEARCH, "ROSE");
    checkRowGone(GRID, DEFAULT_THEME_CHANGED);
    checkRowContentsGrid(GRID, ROSE_THEME);

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, ROSE_THEME, DEFAULT_THEME_CHANGED);
  }

  /**
   * The ellipsis of a row opens the row menu of its theme, whose Edit opens the side panel filled with it
   */
  @Test
  void t007_editThemeFromTheRowMenuButton() {
    setTestTitle("Edit a theme from the row menu of its ellipsis");

    searchThemesOfTheClass();
    clickGridRowButton(GRID, ROSE_THEME, MORE_BUTTON);
    clickContextButton("CtxButUpd");

    checkCriterionContents("Nam", ROSE_THEME);
    checkSelectContents("PriPal", "Rose");
    checkSelectContents("SrfPal", "Slate");
    checkSelectContents("Mde", "Dark");
    checkCheckboxRadio(true, "Act");
    checkSidePanelTitle(PANEL, ROSE_THEME);
    checkSidePanelSubtitle(PANEL, "Rose · Slate");
    checkSidePanelTag(PANEL, "Active");
    clickButton("ButPnlCnl");
  }

  /**
   * Delete a theme from the row menu (the ellipsis selects only its row) and, once it is gone and nothing is selected,
   * the other one from the selection bar
   */
  @Test
  void t008_deleteThemes() {
    setTestTitle("Delete the themes from the row menu and the selection bar");

    if (listed(DEFAULT_THEME_CHANGED)) {
      clickGridRowButton(GRID, DEFAULT_THEME_CHANGED, MORE_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, DEFAULT_THEME_CHANGED);
    }

    if (listed(ROSE_THEME)) {
      toggleRowContents(GRID, ROSE_THEME);
      checkGridSelectionBar(GRID, "1 selected");
      clickGridSelectionButton(GRID, "ButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, ROSE_THEME);
    }

    searchThemesOfTheClass();
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 0");
  }
}
