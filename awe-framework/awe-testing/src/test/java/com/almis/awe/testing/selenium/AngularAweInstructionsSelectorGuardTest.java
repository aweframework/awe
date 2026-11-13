package com.almis.awe.testing.selenium;

import java.util.Map;

/**
 * Selector guard for {@link AngularAweInstructions}: no selector can depend on select2, bootstrap-datepicker, tabdrop,
 * angular-ui-grid, Bootstrap or font-awesome internals.
 */
class AngularAweInstructionsSelectorGuardTest extends AbstractFrontEndSelectorGuardTest {

  @Override
  protected String clientVocabularyFile() {
    return TestIdsVocabularyTest.ANGULAR_VOCABULARY_FILE;
  }

  @Override
  protected IAweFrontEndInstructions instructions() {
    return new AngularAweInstructions();
  }

  @Override
  protected Map<String, String> allowedFragments() {
    return Map.of("[data-icon~='fa-", "The icon column of the AngularJS client renders the font-awesome class of the icon "
      + "in its hook (data-icon), so the icon is searched by that class; the tests name the icon without its prefix");
  }
}
