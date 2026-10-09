package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Database connections: add, update and delete a database connection. The class creates its own site, which the connection is assigned to, and deletes it after the connection.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDDatabaseTestsIT extends AbstractCrudTests {

  private static final String SITE = "Site gamma";
  // The alias of a database connection takes 15 characters at most
  private static final String DATABASE = "DBS gamma";
  private static final String DATABASE_CHANGED = "DBS gamma upd";

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
    checkLogout();
  }

  /**
   * Add the site that the database connection is assigned to
   */
  @Test
  void t001_newSite() {
    // Title
    setTestTitle("Add the site of the database connection");

    // Create the site
    createSite(SITE);

    // Check that it is in the list
    verifyListed(SITE, TOOLS, SITES);
  }

  /**
   * Add the database connection with the site that it is assigned to, unless they are there. A step that is run again finds
   * them as the run left them: the failed attempt may have saved the connection, and the later steps of the class (which a
   * rerun comes after) have deleted it
   */
  private void createDatabase() {
    // The site of the connection
    createSite(SITE);

    if (!exists(DATABASE, TOOLS, DATABASES)) {
      // Go for new screen
      addNew(TOOLS, DATABASES);

      // Insert text
      writeText("Als", DATABASE);

      // Select on selector
      selectContain("Dct", "Jdbc");

      // Insert text
      writeText("Dbc", "jdbc:hsqldb:file:awe-tests/awe-boot/target/db/awe-boot");

      // Select on selector
      selectContain("Typ", "Development");

      // Insert text
      writeText("Des", "This is a test case of new DataBase");

      // Select on selector
      selectContain("Dbt", "HSQL");

      // Click on button
      clickButton("ButSitModDbsLstAdd");

      // Suggest on column selector
      suggest("SitModDbsLst", "IdeSit", SITE, SITE);

      // Suggest on column selector
      suggest("SitModDbsLst", "IdeMod", "Base", "Base");

      // Insert text
      writeText("SitModDbsLst", "Ord", "5");

      // Save line
      saveRow();

      // Check row
      checkRowContents(SITE, "Base", "5");

      // Store and confirm
      clickButtonAndConfirm(CONFIRM_BUTTON);
    }
  }

  /**
   * Add a new database connection
   */
  @Test
  void t002_newDatabase() {
    // Title
    setTestTitle("Add a new database connection");

    // The connection is added unless an attempt that saved it is still there (a rerun)
    createDatabase();

    // Verify
    verifyView("CrtAls", DATABASE);

    // Check contents
    checkCriterionContents("Als", DATABASE);

    // Check row contents
    checkRowContents(SITE, "Base", "5");
  }

  /**
   * Update a database connection
   */
  @Test
  void t004_updateDatabase() {
    // Title
    setTestTitle("Update a database connection");

    // The connection that the update needs (a rerun comes after the deletion of the connection)
    createDatabase();

    // Go to update
    update("CrtAls", DATABASE, TOOLS, DATABASES);

    // Insert text
    writeText("Als", DATABASE_CHANGED);

    // Select on selector
    selectContain("Dct", "Jdbc");

    // Insert text
    writeText("Dbc", "Test");

    // Insert text
    writeText("Des", "This is a tes case of modify DB");

    // Click on row
    editRow(SITE);

    // Suggest on column selector
    suggest("SitModDbsLst", "IdeMod", "Test", "Test");

    // Save line
    saveRow();

    // Check row
    checkRowContents("Test");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Verify
    verifyView("CrtAls", DATABASE_CHANGED);

    // Check contents
    checkCriterionContents("Als", DATABASE_CHANGED);

    // Check row contents
    checkRowContents("Test");
  }

  /**
   * Delete the database connection
   */
  @Test
  void t006_deleteDatabase() {
    // Title
    setTestTitle("Delete a database connection");

    // Delete the database, if it is still there
    verifyRecordDeleted("CrtAls", DATABASE, TOOLS, DATABASES);
  }

  /**
   * Delete the site of the database connection, once the connection is deleted
   */
  @Test
  void t008_deleteSite() {
    // Title
    setTestTitle("Delete the site of the database connection");

    // Delete the site
    verifySiteDeleted(SITE);
  }
}
