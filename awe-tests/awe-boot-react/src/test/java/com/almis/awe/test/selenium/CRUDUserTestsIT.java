package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Users: add, update, try to add twice and delete a user. The class creates its own profile, which the user has, and deletes it after the user. The user and its profile have names that other classes do not use (the module tests search the user that is called test).
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDUserTestsIT extends AbstractCrudTests {

  private static final String PROFILE_ACRONYM = "PRU";
  private static final String PROFILE = "Profile user";
  private static final String USER = "crud user";
  private static final String USER_SEARCH = "crud use";

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
   * Add a new user
   */
  private void addNewUser() {
    // Go for new screen
    addNew(TOOLS, USERS);

    // Insert text
    writeText("Usr", USER);

    // Select on selector
    selectContain("Sta", "Yes");

    // Insert text
    writeText("Pas", "1234");

    // Insert text
    writeText("RetPas", "1234");

    // Insert text
    writeText("Nam", "crud");

    // Suggest on selector
    suggest("Pro", PROFILE_ACRONYM, PROFILE_ACRONYM);

    // Suggest on selector
    suggest("Thm", "grass", "grass");

    // Insert text
    writeText("Eml", "crud@almis.com");
  }

  /**
   * Add the profile that the user has
   */
  @Test
  void t001_newProfile() {
    // Title
    setTestTitle("Add the profile of the user");

    // Create the profile
    createProfile(PROFILE_ACRONYM, PROFILE);

    // Check that it is in the list
    verifyListed(PROFILE_ACRONYM, TOOLS, PROFILES);
  }

  /**
   * Add the user with the profile that it has, unless they are there. A step that is run again finds them as the run left them:
   * the failed attempt may have saved the user (the new one would be the duplicate), and the later steps of the class (which a
   * rerun comes after) have deleted it
   */
  private void createUser() {
    // The profile of the user
    createProfile(PROFILE_ACRONYM, PROFILE);

    if (!exists(USER_SEARCH, TOOLS, USERS)) {
      // Add a new user
      addNewUser();

      // Check new user added
      checkCriterionContents("Usr", USER);

      // Store and confirm
      clickButtonAndConfirm(CONFIRM_BUTTON);
    }
  }

  /**
   * Add a new user
   */
  @Test
  void t002_newUser() {
    // Title
    setTestTitle("Add a new user");

    // The user is added unless an attempt that saved it is still there (a rerun)
    createUser();

    // Verify
    verifyView("CrtUsr", USER_SEARCH);

    // Check contents
    checkCriterionContents("Nom", USER);

    // Check contents
    checkSelectContents("Sta", "Yes");

    // Check contents
    checkCriterionContents("Nam", "crud");

    // Check contents
    checkCriterionContents("Eml", "crud@almis.com");

    // Check contents
    checkSuggestContents("Thm", "grass");

    // Check contents
    checkSuggestContents("Pro", PROFILE_ACRONYM + " - " + PROFILE);
  }

  /**
   * Update a user
   */
  @Test
  void t004_updateUser() {
    // Title
    setTestTitle("Update a user");

    // The user that the update needs (a rerun comes after the deletion of the user)
    createUser();

    // Go to update
    update("CrtUsr", USER_SEARCH, TOOLS, USERS);

    // Insert text
    writeText("Eml", "crudUpd@almis.com");

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Verify
    verifyView("CrtUsr", USER_SEARCH);

    // Check contents
    checkCriterionContents("Eml", "crudUpd@almis.com");
  }

  /**
   * Try to add a duplicated user
   */
  @Test
  void t005_newDuplicatedUser() {
    // Title
    setTestTitle("Try to add a duplicated user");

    // The user to duplicate (a rerun comes after the deletion of the user)
    createUser();

    // Check user
    addNewUser();

    // Check text
    checkCriterionContents("Usr", USER);

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON, "warning");
  }

  /**
   * Delete the user
   */
  @Test
  void t006_deleteUser() {
    // Title
    setTestTitle("Delete a user");

    // Delete the user, if it is still there
    verifyRecordDeleted("CrtUsr", USER_SEARCH, TOOLS, USERS);
  }

  /**
   * Delete the profile of the user, once the user is deleted
   */
  @Test
  void t008_deleteProfile() {
    // Title
    setTestTitle("Delete the profile of the user");

    // Delete the profile
    verifyProfileDeleted(PROFILE_ACRONYM);
  }
}
