package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Screen personalization (#792), the one flow no lower layer observes: an administrator opens the editor, chooses the
 * Users screen, clicks its "new user" button in the canvas, changes its label for everyone and saves; the history lists
 * the change and Undo takes it back. The rules, the validation, the audit and the history query are covered by the unit,
 * descriptor and Spring tests.
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ScreenPersonalizationTestsIT extends AbstractScreenPersonalizationTests {

  private static final String USERS_SCREEN = "Users";
  private static final String NEW_USER_BUTTON = "ButNew";
  private static final String NEW_LABEL = "Add person";

  /**
   * Change the label of a button of the Users screen for everyone and save it: the history lists the change
   */
  @Test
  void t001_changeALabelAndSeeItInTheHistory() {
    setTestTitle("Personalize the label of a button and see the change in the history");

    openEditorOn(USERS_SCREEN);
    selectElement(NEW_USER_BUTTON);
    changeLabel(NEW_LABEL);
    saveChanges();

    openHistory();
    checkHistoryHas(NEW_LABEL);
  }

  /**
   * Undo the change from the history: the rule is deleted and the deletion is recorded
   */
  @Test
  void t002_undoTheChange() {
    setTestTitle("Undo the personalization from the history");

    openEditorOn(USERS_SCREEN);
    openHistory();
    undoLatestChange();

    checkHistoryHas("Deleted");
  }
}
