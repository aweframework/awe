package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Databases screen (database-admin, #793): the side panel creates two connections, one of them with two sites and modules
 * added in the wide dialog that opens over the panel (its editable grid) and listed read-only in the panel once the dialog
 * is closed, the pencil of a row edits its connection in the side panel (whose header shows the connection: alias,
 * connection and state) with its sites and modules, and a third site row added in the dialog is listed in the panel
 * before saving and kept with the other two after saving,
 * a new connection starts without sites, the quick filters count the connections of the search (All, Active, Inactive)
 * and filter the grid, the search finds them by alias or user, the row menu edits them and the selection bar and the row
 * menu delete them. The class creates its own two connections, on the sites and modules of the test data, and deletes
 * them at the end.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class DatabaseAdminTestsIT extends AbstractCrudTests {

  private static final String GRID = "GrdDbsAdm";
  private static final String SITES_GRID = "SitModDbsLst";
  private static final String SITES_LIST = "SitModDbsViw";
  private static final String SITES_DIALOG = "DlgSitMod";
  // Width of a modal-lg dialog in both engines
  private static final int SITES_DIALOG_WIDTH = 900;
  private static final String QUICK_FILTER = "CrtDbsSta";
  private static final String SEARCH = "CrtDbsTxt";
  private static final String PANEL = "PnlDbs";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String MORE_BUTTON = "MorBtn";
  // Words of the connections of the class only: the search by them lists just these two. The alias of a connection
  // takes 15 characters at most
  private static final String PREFIX = "Dbsadmtest";
  private static final String SITES_DATABASE = PREFIX + " one";
  private static final String SITES_DATABASE_CHANGED = PREFIX + " upd";
  private static final String INACTIVE_DATABASE = PREFIX + " two";
  // The connection with sites is a real one (the database of the application)
  private static final String CONNECTION = "jdbc:hsqldb:file:target/db/awe-react";
  private static final String INACTIVE_CONNECTION = "jdbc:hsqldb:mem:dbsadmtest";
  private static final String USER = "dbsadmtestusr";
  // Sites and modules of the test data
  private static final String MADRID = "Madrid";
  private static final String ONATE = "Onate";
  private static final String BASE = "Base";
  private static final String TEST = "Test";

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
   * Open the databases screen and search the connections of the class
   */
  private void searchDatabasesOfTheClass() {
    openScreen(TOOLS, DATABASES);
    searchGrid(SEARCH, PREFIX);
  }

  /**
   * Tell whether the databases screen lists a connection of the class (a rerun finds what a failed attempt saved or
   * deleted)
   *
   * @param alias Connection alias
   * @return The grid shows the connection
   */
  private boolean listed(String alias) {
    searchDatabasesOfTheClass();
    return hasRowContents(alias);
  }

  /**
   * Open the side panel of a new connection: no selected connection, so the header shows the label of the panel, and it
   * starts active and without sites and modules
   */
  private void openNewDatabasePanel() {
    openScreen(TOOLS, DATABASES);
    clickButton("ButNew");
    checkSidePanelOpen(PANEL, "right", "48rem");
    checkSidePanelTitle(PANEL, "Database connection");
    checkCheckboxRadio(true, "Act");
  }

  /**
   * Fill the connection of the side panel
   *
   * @param alias      Alias
   * @param connection Connection
   */
  private void fillConnection(String alias, String connection) {
    writeText("Als", alias);
    selectContain("Dct", "Jdbc");
    selectContain("Typ", "Development");
    writeText("Dbc", connection);
    selectContain("Dbt", "HSQL");
  }

  /**
   * Open the dialog of the sites and modules over the side panel: its buttons are clicked, so it is on top of the panel
   */
  private void openSitesDialog() {
    clickButton("ButSitModEdt");
    checkDialogWidth(SITES_DIALOG, SITES_DIALOG_WIDTH);
  }

  /**
   * Close the dialog of the sites and modules with Done, back to the side panel
   */
  private void closeSitesDialog() {
    clickButton("ButSitModDon");
    checkDialogClosed(SITES_DIALOG);
  }

  /**
   * Add a row to the sites and modules grid of the dialog
   *
   * @param site   Site
   * @param module Module
   * @param order  Order
   */
  private void addSite(String site, String module, String order) {
    clickButton("ButSitModDbsLstAdd");
    // In React the select of a grid cell shows its search only when it is open: choose the option by its text
    selectContain(SITES_GRID, "IdeSit", site);
    selectContain(SITES_GRID, "IdeMod", module);
    writeText(SITES_GRID, "Ord", order);
    saveRow(SITES_GRID);
  }

  /**
   * Create a connection with two sites and modules, unless it is there (a rerun)
   */
  @Test
  void t001_newDatabaseWithSitesFromThePanel() {
    setTestTitle("Add a connection with its sites and modules from the side panel");

    if (!listed(SITES_DATABASE) && !listed(SITES_DATABASE_CHANGED)) {
      openNewDatabasePanel();
      fillConnection(SITES_DATABASE, CONNECTION);
      writeText("DbsUsr", USER);
      writeText("DbsPwd", "dbsadmtest-secret");
      writeText("Des", "Connection of the database admin test");
      openSitesDialog();
      addSite(MADRID, BASE, "1");
      addSite(ONATE, TEST, "2");
      checkRowContentsGrid(SITES_GRID, MADRID, ONATE);
      closeSitesDialog();
      // The panel lists the rows of the dialog, not saved yet
      checkRowContentsGrid(SITES_LIST, MADRID, BASE, ONATE, TEST);
      clickButtonAndConfirm("ButPnlNew");

      searchDatabasesOfTheClass();
      checkRowContentsGrid(GRID, SITES_DATABASE, "Development", "HSQL", "Active");
    }
  }

  /**
   * Create an inactive connection without sites and modules, unless it is there (a rerun)
   */
  @Test
  void t002_newInactiveDatabaseFromThePanel() {
    setTestTitle("Add an inactive connection from the side panel");

    if (!listed(INACTIVE_DATABASE)) {
      openNewDatabasePanel();
      fillConnection(INACTIVE_DATABASE, INACTIVE_CONNECTION);
      clickCheckbox("Act");
      checkCheckboxRadio(false, "Act");
      clickButtonAndConfirm("ButPnlNew");
    }

    searchDatabasesOfTheClass();
    checkRowContentsGrid(GRID, INACTIVE_DATABASE, "Inactive");
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the connection of its row and its sites and
   * modules, and its header shows the connection (alias, connection and state). A third site row
   * is saved with the alias, and the three rows keep the connection. Help is an icon-only button named by its label
   */
  @Test
  void t003_editDatabaseFromThePencil() {
    setTestTitle("Edit a connection and its sites from the pencil of its row");

    if (!listed(SITES_DATABASE_CHANGED)) {
      searchDatabasesOfTheClass();
      checkButtonIconOnly("ButHlp", "Help");

      clickGridRowButton(GRID, SITES_DATABASE, EDIT_BUTTON);

      checkCriterionContents("Als", SITES_DATABASE);
      checkCriterionContents("Dbc", CONNECTION);
      checkCriterionContents("DbsUsr", USER);
      checkSelectContents("Dbt", "HSQL");
      checkSidePanelTitle(PANEL, SITES_DATABASE);
      checkSidePanelSubtitle(PANEL, CONNECTION);
      checkSidePanelTag(PANEL, "Active");
      checkRowContentsGrid(SITES_LIST, MADRID, ONATE);

      writeText("Als", SITES_DATABASE_CHANGED);
      openSitesDialog();
      checkRowContentsGrid(SITES_GRID, MADRID, ONATE);
      addSite(MADRID, TEST, "3");
      closeSitesDialog();
      checkRowContentsGrid(SITES_LIST, "3");
      clickButtonAndConfirm("ButPnlUpd");
    }

    searchDatabasesOfTheClass();
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED, "Active");
    // Every site row is still on the connection
    clickGridRowButton(GRID, SITES_DATABASE_CHANGED, EDIT_BUTTON);
    checkRowContentsGrid(SITES_LIST, MADRID, ONATE, TEST, "3");
    clickButton("ButPnlCnl");
  }

  /**
   * After editing a connection, a new connection starts without its sites and modules
   */
  @Test
  void t004_newDatabaseStartsEmpty() {
    setTestTitle("A new connection starts without the sites of the edited one");

    searchDatabasesOfTheClass();
    clickGridRowButton(GRID, SITES_DATABASE_CHANGED, EDIT_BUTTON);
    checkRowContentsGrid(SITES_LIST, MADRID, ONATE);
    clickButton("ButPnlCnl");

    clickButton("ButNew");
    checkSidePanelTitle(PANEL, "Database connection");
    checkRowGone(SITES_LIST, MADRID);
    checkRowGone(SITES_LIST, ONATE);
    openSitesDialog();
    checkRowGone(SITES_GRID, MADRID);
    closeSitesDialog();
    clickButton("ButPnlCnl");
  }

  /**
   * The counts of the quick filters follow the search: the connections of the class
   */
  @Test
  void t005_quickFilterCounts() {
    setTestTitle("Quick filter counts of the connections of the search");

    searchDatabasesOfTheClass();

    checkGridQuickFilterOption(QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 1");
    checkGridQuickFilterOption(QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
  }

  /**
   * Each quick filter shows the connections of its option, and All shows them all again
   */
  @Test
  void t006_quickFilterChoices() {
    setTestTitle("Quick filters filter the grid");

    searchDatabasesOfTheClass();
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED, INACTIVE_DATABASE);

    clickGridQuickFilterOption(QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "inactive");
    checkRowGone(GRID, SITES_DATABASE_CHANGED);
    checkRowContentsGrid(GRID, INACTIVE_DATABASE);

    clickGridQuickFilterOption(QUICK_FILTER, "active");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "active");
    checkRowGone(GRID, INACTIVE_DATABASE);
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED);

    clickGridQuickFilterOption(QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(QUICK_FILTER, "");
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED, INACTIVE_DATABASE);
  }

  /**
   * The search finds the connections by alias (typing, with no Enter) and by user, ignoring case, the counts follow it,
   * and its clear button shows every connection again
   */
  @Test
  void t007_searchByAliasOrUser() {
    setTestTitle("Search connections by alias or user");

    searchDatabasesOfTheClass();
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED, INACTIVE_DATABASE);

    typeGridSearch(SEARCH, INACTIVE_DATABASE.toUpperCase());
    checkRowGone(GRID, SITES_DATABASE_CHANGED);
    checkRowContentsGrid(GRID, INACTIVE_DATABASE);
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(QUICK_FILTER, "active", "Active 0");

    searchGrid(SEARCH, USER.toUpperCase());
    checkRowGone(GRID, INACTIVE_DATABASE);
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED);

    clearGridSearch(SEARCH);
    checkRowContentsGrid(GRID, SITES_DATABASE_CHANGED, INACTIVE_DATABASE);
  }

  /**
   * The ellipsis of a row opens the row menu of its connection, whose Edit opens the side panel filled with it
   */
  @Test
  void t008_editDatabaseFromTheRowMenuButton() {
    setTestTitle("Edit a connection from the row menu of its ellipsis");

    searchDatabasesOfTheClass();
    clickGridRowButton(GRID, INACTIVE_DATABASE, MORE_BUTTON);
    clickContextButton("CtxButUpd");

    checkCriterionContents("Als", INACTIVE_DATABASE);
    checkCriterionContents("Dbc", INACTIVE_CONNECTION);
    checkSelectContents("Typ", "Development");
    checkCheckboxRadio(false, "Act");
    checkSidePanelTitle(PANEL, INACTIVE_DATABASE);
    checkSidePanelSubtitle(PANEL, INACTIVE_CONNECTION);
    checkSidePanelTag(PANEL, "Inactive");
    clickButton("ButPnlCnl");
  }

  /**
   * Delete a connection from the row menu (the ellipsis selects only its row) and, once it is gone and nothing is
   * selected, the other one from the selection bar
   */
  @Test
  void t009_deleteDatabases() {
    setTestTitle("Delete the connections from the row menu and the selection bar");

    if (listed(INACTIVE_DATABASE)) {
      clickGridRowButton(GRID, INACTIVE_DATABASE, MORE_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
      checkRowGone(GRID, INACTIVE_DATABASE);
    }

    for (String alias : new String[]{SITES_DATABASE, SITES_DATABASE_CHANGED}) {
      if (listed(alias)) {
        toggleRowContents(GRID, alias);
        checkGridSelectionBar(GRID, "1 selected");
        clickGridSelectionButton(GRID, "ButDel");
        acceptConfirm();
        checkAndCloseMessage("success");
        checkRowGone(GRID, alias);
      }
    }

    searchDatabasesOfTheClass();
    checkGridQuickFilterOption(QUICK_FILTER, "", "All 0");
  }
}
