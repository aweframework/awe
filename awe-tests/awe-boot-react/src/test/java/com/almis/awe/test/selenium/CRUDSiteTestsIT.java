package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Sites: add, update and delete a site. It does not need any other record or class.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDSiteTestsIT extends AbstractCrudTests {

  private static final String SITE = "Site alpha";
  private static final String SITE_CHANGED = "Site alpha changed";

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
   * Add a new site
   */
  @Test
  void t001_newSite() {
    // Title
    setTestTitle("Add a new site");

    // A rerun after an attempt that saved the site does not add it again, and checks it as saved
    if (!exists(SITE, TOOLS, SITES)) {
      // Go for new screen
      addNew(TOOLS, SITES);

      // Write on criterion
      writeText("Nam", SITE);

      // Select on selector
      selectLast("Act");

      // Write on criterion
      writeText("Ord", "3");

      // Click on button
      clickButton("ButGrdAdd");

      // Suggest on column selector
      selectContain("SitModDbsLst", "IdeMod", "Base");

      // Suggest on column selector
      selectContain("SitModDbsLst", "IdeDbs", "awedb");

      // Write on criterion
      writeText("SitModDbsLst", "Order", "3");

      // Save line
      saveRow();

      // Check row values
      checkRowContents("Base", "awedb", "3");

      // Store and confirm
      clickButtonAndConfirm(CONFIRM_BUTTON);
    }

    // Verify
    verifyView("CrtSit", SITE);

    // Check row contents
    checkRowContents("Base", "awedb");
  }

  /**
   * Update a site
   */
  @Test
  void t003_updateSite() {
    // Title
    setTestTitle("Update a site");

    // The site that the update needs (a rerun comes after the deletion of the site)
    createSite(SITE);

    // Go to update
    update("CrtSit", SITE, TOOLS, SITES);

    // Write on criterion
    writeText("Nam", SITE_CHANGED);

    // Edit row
    editRow("Base");

    // Suggest on column selector
    selectContain("SitModDbsLst", "IdeDbs", "awedb2");

    // Save line
    saveRow();

    // Check row values
    checkRowContents("Base", "awedb2", "3");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Verify
    verifyView("CrtSit", SITE_CHANGED);

    // Check row contents
    checkRowContents("Base", "awedb2");
  }

  /**
   * Delete a site
   */
  @Test
  void t005_deleteSite() {
    // Title
    setTestTitle("Delete a site");

    // Delete the site
    verifySiteDeleted(SITE);
  }
}
