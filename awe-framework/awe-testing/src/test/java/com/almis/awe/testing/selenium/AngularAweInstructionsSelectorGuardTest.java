package com.almis.awe.testing.selenium;

import java.util.Map;

/**
 * Selector guard for {@link AngularAweInstructions}: no selector can depend on select2, bootstrap-datepicker, tabdrop,
 * angular-ui-grid, Bootstrap or font-awesome internals.
 */
class AngularAweInstructionsSelectorGuardTest extends AbstractFrontEndSelectorGuardTest {

  @Override
  protected Map<String, String> allowedFragments() {
    return Map.of(".help.popover:not(.ng-hide)", "The help popover of the awe-help directive has no test hook yet and "
      + "must still be located to move the mouse away from it. Remove this entry when the AngularJS client hooks it");
  }

  @Override
  protected String clientVocabularyFile() {
    return TestIdsVocabularyTest.ANGULAR_VOCABULARY_FILE;
  }

  @Override
  protected IAweFrontEndInstructions instructions() {
    return new AngularAweInstructions();
  }
}
