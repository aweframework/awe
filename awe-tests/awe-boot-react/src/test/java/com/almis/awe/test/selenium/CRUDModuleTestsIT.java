package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Modules screen (module-admin): the smoke flow of its side panel with tabs and nested editable grids, which only the
 * browser sees. A new module is created from the panel with a profile row in its Access tab, its row is listed and it is
 * deleted from the selection bar. The list, its search, quick filters and counts, the unique name, the maintains of the
 * panel and the actions of its buttons are proven by ModuleListQueryTest and ModuleAdminScreenTest (awe-boot) and by the
 * descriptor tests of awe-generic-screens.
 *
 * <p>A module takes its name from the modules that the menu of the application declares, so the name is fixed: Inf, which
 * no other module has. The search by Inf lists only what this class (or a previous run) created.</p>
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDModuleTestsIT extends AbstractCrudTests {

  private static final String GRID = "GrdModAdm";
  private static final String SEARCH = "CrtModTxt";
  private static final String TABS = "ModTab";
  private static final String PROFILES_GRID = "MdlPrfLst";
  // The React tab criterion matches its tabs by the text of their label
  private static final String ACCESS_TAB = "Access";
  private static final String MODULE = "Inf";
  private static final String PROFILE = "TST";

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
   * Open the modules screen and search the modules of the class
   */
  private void searchModulesOfTheClass() {
    openScreen(TOOLS, MODULES);
    searchGrid(SEARCH, MODULE);
  }

  /**
   * Delete every module that the search of the class lists, from the selection bar
   */
  private void deleteModulesOfTheClass() {
    searchModulesOfTheClass();
    if (hasRowContents(MODULE)) {
      selectAllRowsOfGrid(GRID);
      clickGridSelectionButton(GRID, "ButDel");
      acceptConfirm();
      checkAndCloseMessage("success");
    }
    checkRowGone(GRID, MODULE);
  }

  /**
   * Create a module from the side panel with a profile in its Access tab, check that the grid lists it and delete it. A
   * module that a previous run left is deleted first
   */
  @Test
  void t001_createAndDeleteAModuleFromTheSidePanel() {
    setTestTitle("Create a module with a profile from the side panel and delete it");

    deleteModulesOfTheClass();

    clickButton("ButNew");
    selectContain("Nam", MODULE);
    writeText("Ord", "3");
    clickTab(TABS, ACCESS_TAB);
    clickButton("ButMdlPrfLstAdd");
    // In React the select of a grid cell shows its search only when it is open: choose the option by its text
    selectContain(PROFILES_GRID, "IdePro", PROFILE);
    saveRow(PROFILES_GRID);
    // One save for the module and the rows of its grids
    clickButtonAndConfirm("ButPnlNew");

    searchModulesOfTheClass();
    checkRowContentsGrid(GRID, MODULE, "Active");

    deleteModulesOfTheClass();
  }
}
