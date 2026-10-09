package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Modules: add, update and delete a module. The class creates its own site, which the module uses, and deletes it after the module.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDModuleTestsIT extends AbstractCrudTests {

  private static final String SITE = "Site beta";
  // A module takes its name from the modules that the menu of the application defines (Inf and Inf Changed), so these names are fixed
  private static final String MODULE = "Inf";
  private static final String MODULE_CHANGED = "Inf Changed";

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
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
   * Add the site that the module uses
   */
  @Test
  void t001_newSite() {
    // Title
    setTestTitle("Add the site of the module");

    // Create the site
    createSite(SITE);

    // Check that it is in the list
    verifyListed(SITE, TOOLS, SITES);
  }

  /**
   * Add the module with the site that it uses, unless they are there. A step that is run again finds them as the run left them:
   * the failed attempt may have saved the module, and the later steps of the class (which a rerun comes after) have deleted it
   */
  private void createModule() {
    // The site of the module
    createSite(SITE);

    if (!exists(MODULE, TOOLS, MODULES)) {
      // Go for new screen
      addNew(TOOLS, MODULES);

      // Select option
      selectContain("Nam", MODULE);

      // Suggest on selector
      suggest("Scr", "Sites", "Sites");

      // Suggest on  selector
      suggest("Thm", "gra", "gra");

      // Select text
      writeText("Ord", "3");

      // Select on selector
      selectContain("Act", "Yes");

      // Click on button
      clickButton("ButMdlUsrLstAdd");

      // Suggest on column selector
      suggest("MdlUsrLst", "IdeOpe", "test", "test");

      // Suggest on column selector
      suggest("MdlUsrLst", "IdeThm", "sky", "sky");

      // Save line
      saveRow("MdlUsrLst");

      // Check row values
      checkRowContentsGrid("MdlUsrLst", "test", "sky");

      // Click on button
      clickButton("ButMdlPrfLstAdd");

      // Suggest on column selector
      suggest("MdlPrfLst", "IdePro", "TST", "TST");

      // Save line
      saveRow("MdlPrfLst");

      // Check row values
      checkRowContentsGrid("MdlPrfLst", "TST");

      // Click on button
      clickButton("ButMdlSitDbsLstAdd");

      // Suggest on column selector
      suggest("MdlSitDbsLst", "IdeSit", SITE, SITE);

      // Suggest on column selector
      suggest("MdlSitDbsLst", "IdeDbs", "awedb", "awedb");

      // Save line
      saveRow("MdlSitDbsLst");

      // Check row values
      checkRowContentsGrid("MdlSitDbsLst", SITE, "awedb");

      // Store and confirm
      clickButtonAndConfirm(CONFIRM_BUTTON);
    }
  }

  /**
   * Add a new module
   */
  @Test
  void t002_newModule() {
    // Title
    setTestTitle("Add a new module");

    // The module is added unless an attempt that saved it is still there (a rerun)
    createModule();

    // Verify
    verifyView("CrtMod", MODULE);

    // Check contents
    checkCriterionContents("Nam", MODULE);

    // Check contents
    checkCriterionContents("Thm", "grass");

    // Check contents
    checkSuggestContents("Scr", "Sites (Sit)");

    // Check row contents
    checkRowContents("test", "TST", SITE);
  }

  /**
   * Update the module
   */
  @Test
  void t004_updateModule() {
    // Title
    setTestTitle("Update the module");

    // The module that the update needs (a rerun comes after the deletion of the module)
    createModule();

    // Go to update
    update("CrtMod", MODULE, TOOLS, MODULES);

    // Write on criterion
    selectContain("Nam", MODULE_CHANGED);

    // Write on criterion
    suggest("Scr", "Usr", "Usr");

    // Click row
    editRow("test");

    // Suggest on column selector
    selectContain("MdlUsrLst", "IdeThm", "grass");

    // Save line
    saveRow("MdlUsrLst");

    // Check row values
    checkRowContentsGrid("MdlUsrLst", "grass");

    // Click row (the profile is the one of the creation, or the one that a failed attempt of this step saved)
    editRow(hasRowContentsGrid("MdlPrfLst", "TST") ? "TST" : "ADM");

    // Suggest on column selector
    selectContain("MdlPrfLst", "IdePro", "ADM - Administrator");

    // Save line
    saveRow("MdlPrfLst");

    // Check row values
    checkRowContentsGrid("MdlPrfLst", "ADM - Administrator");

    // Click row
    editRow(SITE);

    // Suggest on column selector
    selectContain("MdlSitDbsLst", "IdeDbs", "awedb2");

    // Save line
    saveRow("MdlSitDbsLst");

    // Check row values
    checkRowContentsGrid("MdlSitDbsLst", "awedb2");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Verify
    verifyView("CrtMod", MODULE_CHANGED);

    // Check criterion
    checkCriterionContents("Nam", MODULE_CHANGED);

    // Check criterion
    checkCriterionContents("Thm", "grass");

    // Check criterion
    checkSuggestContents("Scr", "Usr");

    // Check row contents
    checkRowContents("test", "ADM", SITE);
  }

  /**
   * Delete the module
   */
  @Test
  void t006_deleteModule() {
    // Title
    setTestTitle("Delete a module");

    // Delete the module, if it is still there
    verifyRecordDeleted("CrtMod", MODULE, TOOLS, MODULES);
  }

  /**
   * Delete the site of the module, once the module that used it is deleted
   */
  @Test
  void t008_deleteSite() {
    // Title
    setTestTitle("Delete the site of the module");

    // Delete the site
    verifySiteDeleted(SITE);
  }
}
