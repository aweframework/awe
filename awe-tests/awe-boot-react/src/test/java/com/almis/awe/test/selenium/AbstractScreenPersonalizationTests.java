package com.almis.awe.test.selenium;

import com.almis.awe.testing.driver.Locator;

/**
 * Steps of the screen personalization editor (Settings, Personalize screen): the locators of its toolbar, its inspect
 * canvas, its side panel and its history live here, so the tests only describe what the administrator does
 */
abstract class AbstractScreenPersonalizationTests extends AbstractApplicationTests {

  private static final Locator SCREEN_PICKER = Locator.css(".scr-per-toolbar .p-autocomplete-input");
  private static final Locator SIDE_PANEL = Locator.css(".scr-per-side");
  private static final Locator LABEL_FIELD = Locator.css("#scr-per-label");
  private static final Locator STATUS = Locator.css(".scr-per-status");
  private static final Locator HISTORY = Locator.css(".scr-per-history-rows");
  private static final Locator EDIT_MODE_OFF = Locator.xpath("//*[contains(@class,'p-selectbutton')]"
    + "//*[contains(@class,'p-button')][normalize-space(.)='Edit'][@aria-pressed='false']");

  /**
   * Open the editor and choose the screen to personalize
   *
   * @param screenLabel Label of the screen in the picker
   */
  void openEditorOn(String screenLabel) {
    gotoScreen("settings", "screen-personalization");
    // The editor may be open already (the menu does not open the current screen again): it keeps the mode and the screen
    // chosen before, so it goes back to Edit and the text of its picker is replaced, not appended to
    checkVisible(SCREEN_PICKER);
    if (getBrowser().isVisible(EDIT_MODE_OFF)) {
      getBrowser().click(EDIT_MODE_OFF);
    }
    getBrowser().clear(SCREEN_PICKER);
    writeText(SCREEN_PICKER, screenLabel);
    // A suggestion reads "label (option id)"
    Locator suggestion = Locator.xpath("//li[contains(@class,'p-autocomplete-item')][starts-with(normalize-space(.),'" + screenLabel + " (')]");
    checkVisible(suggestion);
    getBrowser().click(suggestion);
    // The inspect node is display: contents (no box of its own): wait for the elements it wraps
    checkVisible(Locator.css(".scr-per-canvas .awe-inspect-node > *"));
  }

  /**
   * Select an element of the screen in the canvas: the click selects it instead of acting and opens its side panel
   *
   * @param elementId Identifier of the element
   */
  void selectElement(String elementId) {
    click(".scr-per-canvas .awe-inspect-node #" + elementId);
    checkVisible(SIDE_PANEL);
  }

  /**
   * Type a new label for the selected element, in the scope the editor is on (everyone by default)
   *
   * @param label New label
   */
  void changeLabel(String label) {
    getBrowser().clear(LABEL_FIELD);
    writeText(LABEL_FIELD, label);
    waitForText(STATUS, "1 change");
    // Below 1440px the side panel floats over the toolbar: close it as the administrator does before saving
    getBrowser().click(Locator.xpath("//*[contains(@class,'scr-per-side')]//button[normalize-space(.)='Done']"));
    checkNotVisible(SIDE_PANEL);
  }

  /**
   * Save the pending changes: the status of the toolbar is empty again when the server answered
   */
  void saveChanges() {
    getBrowser().click(Locator.xpath("//button[normalize-space(.)='Save changes']"));
    checkNotVisible(Locator.xpath("//*[contains(@class,'scr-per-status')][contains(.,'change')]"));
    waitForLoadingBar();
  }

  /**
   * Switch the editor to its history
   */
  void openHistory() {
    getBrowser().click(Locator.xpath("//*[contains(@class,'p-selectbutton')]//*[contains(@class,'p-button')]"
      + "[normalize-space(.)='History']"));
    checkVisible(HISTORY);
  }

  /**
   * Check that the history lists a text, newest first
   *
   * @param text Text of a change
   */
  void checkHistoryHas(String text) {
    waitForText(HISTORY, text);
  }

  /**
   * Undo the latest change of the history (its first row)
   */
  void undoLatestChange() {
    click(".scr-per-history-row:first-child .p-button");
    waitForLoadingBar();
  }
}
