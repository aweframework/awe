package com.almis.awe.testing.selenium;

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
}
