package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Sites screen (site-admin): the side panel creates and edits a site with the database of each of its modules inside the
 * panel (one save for the site and its rows), its header shows the site with its state, the quick filters count and
 * filter the sites of the search, the search finds them by name, the row menu duplicates one and the row menu and the
 * selection bar delete them. The class creates its own sites and deletes them at the end.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDSiteTestsIT extends AbstractCrudTests {

  private static final String GRID = "GrdSitAdm";
  private static final String QUICK_FILTER = "CrtSitSta";
  private static final String SEARCH = "CrtSitTxt";
  private static final String PANEL = "PnlSit";
  private static final String MODULES_GRID = "SitModDbsLst";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String ROW_MENU_BUTTON = "MorBtn";
  // Words of the sites of the class only: the search by them lists just these
  private static final String PREFIX = "Paneltest site";
  private static final String ACTIVE_SITE = PREFIX + " active";
  private static final String INACTIVE_SITE = PREFIX + " inactive";
  private static final String INACTIVE_SITE_CHANGED = PREFIX + " reactivated";
  private static final String COPIED_SITE = PREFIX + " copy";
  private static final String BASE = "Base";
  private static final String DATABASE_ONE = "awedb1";
  private static final String DATABASE_TWO = "awedb2";
  private static final String DATABASE_THREE = "awedb3";

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
    // The application asks for a confirmation before logging out
    checkLogoutWithConfirmation();
  }

  /**
   * Open the sites screen and search the sites of the class
   */
  private void searchSitesOfTheClass() {
    openScreen(TOOLS, SITES);
    searchGrid(SEARCH, PREFIX);
  }

  /**
   * Tell whether the sites screen lists a site of the class (a rerun finds what a failed attempt saved or deleted)
   *
   * @param name Site name
   * @return The grid shows the site
   */
  private boolean listed(String name) {
    searchSitesOfTheClass();
    return hasRowContents(name);
  }

  /**
   * Create a site from the side panel, with the Base module on a database
   *
   * @param name     Site name
   * @param database Database alias
   * @param active   Leave the Active switch on
   */
  private void createFromThePanel(String name, String database, boolean active) {
    openScreen(TOOLS, SITES);
    clickButton("ButNew");
    // No selected site: the header of the panel shows its label. Active is a switch, on for a new site
    checkSidePanelTitle(PANEL, "Site");
    writeText("Nam", name);
    writeText("OrdCrt", "3");
    if (!active) {
      clickCheckbox("Act");
      checkCheckboxRadio(false, "Act");
    }
    // The modules and databases of the site, edited inside the panel
    clickButton("ButSitModAdd");
    // In React the select of a grid cell shows its search only when it is open: choose the option by its text
    selectContain(MODULES_GRID, "IdeMod", BASE);
    selectContain(MODULES_GRID, "IdeDbs", database);
    writeText(MODULES_GRID, "SitModOrd", "1");
    saveRow(MODULES_GRID);
    // One save for the site and its rows
    clickButtonAndConfirm("ButPnlNew");
  }

  /**
   * Create the active site from the side panel, unless it is there (a rerun)
   */
  @Test
  void t001_newActiveSiteFromThePanel() {
    setTestTitle("Add an active site with its modules from the side panel");

    if (!listed(ACTIVE_SITE)) {
      createFromThePanel(ACTIVE_SITE, DATABASE_ONE, true);
    }

    searchSitesOfTheClass();
    checkRowContentsGrid(GRID, ACTIVE_SITE, "Active");
  }

  /**
   * Create the inactive site from the side panel, unless it is there (a rerun)
   */
  @Test
  void t002_newInactiveSiteFromThePanel() {
    setTestTitle("Add an inactive site with its modules from the side panel");

    if (!listed(INACTIVE_SITE)) {
      createFromThePanel(INACTIVE_SITE, DATABASE_TWO, false);
    }

    searchSitesOfTheClass();
    checkRowContentsGrid(GRID, INACTIVE_SITE, "Inactive");
  }

  /**
   * The counts of the quick filters follow the search: the sites of the class
   */
  @Test
  void t003_quickFilterCounts() {
    setTestTitle("Quick filter counts of the sites of the search");

    searchSitesOfTheClass();

    checkGridQuickFilterOption(QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 1");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
  }

  /**
   * Each quick filter shows the sites of its option, and All shows them all again
   */
  @Test
  void t004_quickFilterChoices() {
    setTestTitle("Quick filters filter the grid");

    searchSitesOfTheClass();
    checkRowContentsGrid(GRID, ACTIVE_SITE, INACTIVE_SITE);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, ACTIVE_SITE);
    checkRowContentsGrid(GRID, INACTIVE_SITE);

    clickGridQuickFilterOption(QUICK_FILTER, "active");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "active");
    checkRowGone(GRID, INACTIVE_SITE);
    checkRowContentsGrid(GRID, ACTIVE_SITE);

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, ACTIVE_SITE, INACTIVE_SITE);
  }

  /**
   * The search finds the sites by name, ignoring case, the counts follow it and its clear button shows every site again
   */
  @Test
  void t005_searchByName() {
    setTestTitle("Search sites by name");

    searchSitesOfTheClass();
    checkRowContentsGrid(GRID, ACTIVE_SITE, INACTIVE_SITE);

    typeGridSearch(SEARCH, INACTIVE_SITE.toLowerCase());
    checkRowGone(GRID, ACTIVE_SITE);
    checkRowContentsGrid(GRID, INACTIVE_SITE);
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 0");

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, ACTIVE_SITE, INACTIVE_SITE);
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the site of its row, whose header shows the site
   * and its state; its modules grid shows the rows of the site and the panel saves the site and its rows in one action
   */
  @Test
  void t006_editSiteAndItsModulesFromThePencil() {
    setTestTitle("Edit a site and its modules from the pencil of its row");

    if (!listed(INACTIVE_SITE_CHANGED)) {
      searchSitesOfTheClass();
      clickGridRowButton(GRID, INACTIVE_SITE, EDIT_BUTTON);

      checkCriterionContents("Nam", INACTIVE_SITE);
      checkCheckboxRadio(false, "Act");
      checkSidePanelTitle(PANEL, INACTIVE_SITE);
      checkSidePanelTag(PANEL, "Inactive");
      // The modules of the site, loaded in the panel
      checkRowContentsGrid(MODULES_GRID, BASE, DATABASE_TWO);

      writeText("Nam", INACTIVE_SITE_CHANGED);
      clickCheckbox("Act");
      checkCheckboxRadio(true, "Act");

      // The database of the module row, edited in the same panel
      editRow(MODULES_GRID, BASE);
      selectContain(MODULES_GRID, "IdeDbs", DATABASE_THREE);
      saveRow(MODULES_GRID);

      clickButtonAndConfirm("ButPnlUpd");
    }

    searchSitesOfTheClass();
    checkRowContentsGrid(GRID, INACTIVE_SITE_CHANGED, "Active");

    // The module row keeps the database chosen in the panel
    clickGridRowButton(GRID, INACTIVE_SITE_CHANGED, EDIT_BUTTON);
    checkRowContentsGrid(MODULES_GRID, BASE, DATABASE_THREE);
    clickButton("ButPnlCnl");
  }

  /**
   * The row menu duplicates a site: the panel opens as a new site with its values and without its module rows, and the
   * copy is created with the modules of the copy
   */
  @Test
  void t007_duplicateSiteFromTheRowMenu() {
    setTestTitle("Duplicate a site from the row menu");

    if (!listed(COPIED_SITE)) {
      searchSitesOfTheClass();
      clickGridRowButton(GRID, ACTIVE_SITE, ROW_MENU_BUTTON);
      clickContextButton("CtxButDup");

      // The copy starts with the values of the original site and no module rows
      checkCriterionContents("Nam", ACTIVE_SITE);
      checkSidePanelTitle(PANEL, ACTIVE_SITE);
      checkGridEmptyState("No modules yet", "Add the database each module uses.");

      writeText("Nam", COPIED_SITE);
      clickButton("ButSitModAdd");
      // In React the select of a grid cell shows its search only when it is open: choose the option by its text
      selectContain(MODULES_GRID, "IdeMod", BASE);
      selectContain(MODULES_GRID, "IdeDbs", DATABASE_THREE);
      writeText(MODULES_GRID, "SitModOrd", "1");
      saveRow(MODULES_GRID);
      clickButtonAndConfirm("ButPnlNew");
    }

    searchSitesOfTheClass();
    checkRowContentsGrid(GRID, COPIED_SITE);
  }

  /**
   * Delete the sites of the class: one from the row menu, the rest from the selection bar of the grid
   */
  @Test
  void t008_deleteSites() {
    setTestTitle("Delete the sites from the row menu and the selection bar");

    if (listed(COPIED_SITE)) {
      clickGridRowButton(GRID, COPIED_SITE, ROW_MENU_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, COPIED_SITE);
    }

    if (listed(ACTIVE_SITE)) {
      toggleRowContents(GRID, ACTIVE_SITE);
      clickGridSelectionButton(GRID, "ButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, ACTIVE_SITE);
    }

    if (listed(INACTIVE_SITE_CHANGED)) {
      toggleRowContents(GRID, INACTIVE_SITE_CHANGED);
      clickGridSelectionButton(GRID, "ButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, INACTIVE_SITE_CHANGED);
    }

    searchSitesOfTheClass();
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 0");
  }
}
