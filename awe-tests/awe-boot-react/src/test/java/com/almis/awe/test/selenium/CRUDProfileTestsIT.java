package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Profiles: add, update and delete a profile. It does not need any other record or class.
 */
@Tag("CRUDCriteriaMatrixIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class CRUDProfileTestsIT extends AbstractCrudTests {

  private static final String ACRONYM = "PRA";
  private static final String PROFILE = "Profile alpha";
  private static final String PROFILE_CHANGED = "Profile alpha changed";

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
   * Add a new profile
   */
  @Test
  void t001_newProfile() {
    // Title
    setTestTitle("Add a new profile");

    // A rerun after an attempt that saved the profile does not add it again, and checks it as saved
    if (!exists(ACRONYM, TOOLS, PROFILES)) {
      // Go for new screen
      addNew(TOOLS, PROFILES);

      // Insert text
      writeText("Acr", ACRONYM);

      // Insert text
      writeText("Nam", PROFILE);

      // Select on selector
      selectContain("Act", "Yes");

      // Suggest on  selector
      suggest("IdeThm", "sunse", "sunse");

      // Suggest on  selector
      suggest("ScrIni", "Modules", "Modules");

      // Verify criterion
      checkSuggestContents("IdeThm", "sunset");

      // Store and confirm
      clickButtonAndConfirm(CONFIRM_BUTTON);
    }

    // Verify
    verifyView("CrtPro", ACRONYM);

    // Check contents
    checkCriterionContents("Acr", ACRONYM);

    // Check contents
    checkCriterionContents("Nam", PROFILE);

    // Check contents
    checkSelectContents("Act", "Yes");
  }

  /**
   * Update a profile
   */
  @Test
  void t003_updateProfile() {
    // Title
    setTestTitle("Update a profile");

    // The profile that the update needs (a rerun comes after the deletion of the profile)
    createProfile(ACRONYM, PROFILE);

    // Go to update
    update("CrtPro", ACRONYM, TOOLS, PROFILES);

    // Insert text
    writeText("Nam", PROFILE_CHANGED);

    // Verify criterion
    checkCriterionContents("Nam", PROFILE_CHANGED);

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Verify
    verifyView("CrtPro", ACRONYM);

    // Check contents
    checkCriterionContents("Acr", ACRONYM);

    // Check criterion
    checkCriterionContents("Nam", PROFILE_CHANGED);

    // Check contents
    checkSelectContents("Act", "Yes");
  }

  /**
   * Delete a profile
   */
  @Test
  void t005_deleteProfile() {
    // Title
    setTestTitle("Delete a profile");

    // Delete the profile
    verifyProfileDeleted(ACRONYM);
  }
}
