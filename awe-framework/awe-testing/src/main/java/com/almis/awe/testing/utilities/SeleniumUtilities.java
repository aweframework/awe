package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.config.TestConfig;
import com.almis.awe.testing.driver.BrowserDriver;
import com.almis.awe.testing.driver.ElementNotFoundException;
import com.almis.awe.testing.driver.ElementRef;
import com.almis.awe.testing.driver.ElementReplacedException;
import com.almis.awe.testing.driver.Key;
import com.almis.awe.testing.driver.Locator;
import com.almis.awe.testing.extensions.FailureEvidence;
import com.almis.awe.testing.extensions.SeleniumExtension;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.IAweInstructions;
import com.almis.awe.testing.selenium.InstructionsFactory;
import com.almis.awe.testing.selenium.TestAttributes;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

import static com.almis.awe.testing.constants.TestingConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Utilities suite for selenium testing
 */
@Slf4j
@ExtendWith({SpringExtension.class, SeleniumExtension.class})
@ContextConfiguration(classes = TestConfig.class, initializers = ConfigDataApplicationContextInitializer.class)
public class SeleniumUtilities implements IAweInstructions {

  // Constants
  private static final Integer RETRY_COUNT = 10;
  private static final int STALE_RETRY_COUNT = 3;
  private static final int EDIT_ROW_ATTEMPTS = 3;
  private static final Duration EDIT_ROW_WAIT = Duration.ofSeconds(2);
  private static final String TEXT_VALUE = " text: '";
  private static final By LOGIN_BUTTON = By.id("ButLogIn");
  private static final By LOGIN_FORM_CONTROLS = By.cssSelector("#ButLogIn, [criterion-id='cod_usr'] input, [criterion-id='pwd_usr'] input");

  private FailureEvidence failureEvidence = new FailureEvidence();

  @Autowired
  private AweTestConfigProperties properties;
  private SeleniumModel seleniumModel;
  private IAweFrontEndInstructions frontEndInstructions;

  /**
   * Get driver
   *
   * @return Get driver
   * @throws UnsupportedOperationException When the tests run with a tool other than Selenium ({@code awe.test.tool})
   * @deprecated Selenium specific: it exposes the Selenium driver and is only available when the tests run with the
   * Selenium tool, so a step that uses it does not work with any other tool. Write the steps of your product with the neutral steps of this class, with a {@link Locator} and with
   * {@link #getBrowser()}. It stays available through the whole 5.x line and is not removed before 6.0
   */
  @Deprecated
  public WebDriver getDriver() {
    return this.seleniumModel.getDriver();
  }

  /**
   * Get base URL
   *
   * @return Get base URL
   */
  public String getBaseUrl() {
    return seleniumModel.getBaseUrl();
  }

  /**
   * Store selenium model
   *
   * @param model Selenium model
   */
  public IAweInstructions setSeleniumModel(SeleniumModel model) {
    seleniumModel = model;
    model.setProperties(properties);

    this.frontEndInstructions = (IAweFrontEndInstructions) InstructionsFactory
      .getInstance(properties.getFrontend())
      .setSeleniumModel(seleniumModel);

    return this.frontEndInstructions;
  }

  /**
   * Get the browser the steps go through. It is the way for a step of a product to query and act on the page without
   * Selenium types: ask it with a {@link Locator} (for instance {@code getBrowser().exists(Locator.css("#id"))}) and
   * build the locators from the identifiers and test hooks of AWE. It is a preview of the tool-neutral
   * {@link BrowserDriver}, which has no compatibility promise yet: prefer the steps of this class when there is one
   *
   * @return Tool-neutral browser
   */
  protected final BrowserDriver getBrowser() {
    return seleniumModel.getBrowser();
  }

  /**
   * Convert a selector of the front end instructions to the neutral locator the browser works with
   *
   * @param selector Selector
   * @return Locator
   */
  private static Locator locator(By selector) {
    return Locator.from(selector);
  }

  /**
   * Condition: something matches the selector
   */
  private static BrowserCondition toBePresent(By selector) {
    return BrowserCondition.present(locator(selector));
  }

  /**
   * Condition: the selector matches a displayed element
   */
  private static BrowserCondition toBeVisible(By selector) {
    return BrowserCondition.visible(locator(selector));
  }

  /**
   * Condition: the selector matches no displayed element
   */
  private static BrowserCondition toBeInvisible(By selector) {
    return BrowserCondition.invisible(locator(selector));
  }

  /**
   * Condition: the selector matches a displayed and enabled element
   */
  private static BrowserCondition toBeClickable(By selector) {
    return BrowserCondition.clickable(locator(selector));
  }

  /**
   * Condition: the text of the selector contains a text
   */
  private static BrowserCondition toContainText(By selector, String text) {
    return BrowserCondition.textContains(locator(selector), text);
  }

  /**
   * Condition: the value of the selector contains a text
   */
  private static BrowserCondition toContainValue(By selector, String text) {
    return BrowserCondition.valueContains(locator(selector), text);
  }

  /**
   * Wait for screen load
   */
  private void waitForLoad() {
    waitUntil(BrowserCondition.of("page to finish loading", BrowserDriver::isPageLoaded));
  }

  /**
   * Wait until a condition over the neutral browser is met, within the configured timeout
   *
   * @param condition Condition
   */
  private void waitUntil(BrowserCondition condition) {
    try {
      BrowserPoll.until(getBrowser(), condition, properties.getTimeout());
      // Assert true on condition
      assertTrue(true, condition.toString());
      log.debug(condition.toString());
    } catch (Exception exc) {
      assertWithScreenshot(condition.toString(), false, exc);
    }
  }

  /**
   * Take a screenshot when an error has occurred
   *
   * @param message   Assert message
   * @param condition Assert condition
   * @param throwable Throwable list
   */
  private void assertWithScreenshot(String message, boolean condition, Throwable... throwable) {
    if (!condition) {
      storeFailureEvidence(message, throwable);
    }

    // Assert false
    assertTrue(condition, message);
  }

  /**
   * Store the screenshot, the page source and the browser console of a failure. It is a help to diagnose the failure, so
   * none of it can fail by itself or replace the failure that is being reported
   *
   * @param message   Assert message
   * @param throwable Throwable list
   */
  private void storeFailureEvidence(String message, Throwable... throwable) {
    try {
      BrowserDriver browser = getBrowser();
      if (browser == null) {
        log.warn("There is no browser to take the evidence of the failed test from");
        return;
      }

      Optional<byte[]> screenshot = takeScreenshot(browser);
      String screenshotName = failureEvidence.buildName(getClass().getSimpleName(),
        seleniumModel.getCurrentOption(), message, true);
      Path path = Paths.get(properties.getScreenshotPath(), screenshotName + ".png");
      log.error(message, (Object) throwable);

      screenshot.ifPresent(image -> {
        log.error("Storing screenshot at: " + path);
        try {
          failureEvidence.storeScreenshot(seleniumModel, image, path);
        } catch (IOException ioExc) {
          log.error("Error trying to store screenshot at: " + path, ioExc);
        }
      });

      // The DOM next to the screenshot helps diagnosing selector failures
      try {
        failureEvidence.storePageSource(path, browser.pageSource());
      } catch (Exception exc) {
        log.warn("Could not read the page source of the failed test", exc);
      }

      // A blank screen caused by a client crash can only be diagnosed with the browser console. Reading it empties it,
      // so it is read once
      try {
        failureEvidence.storeConsoleEntries(path, browser.consoleEntries());
      } catch (Exception exc) {
        log.warn("Could not read the browser console of the failed test", exc);
      }
    } catch (Exception exc) {
      log.warn("Could not store the evidence of the failed test", exc);
    }
  }

  private Optional<byte[]> takeScreenshot(BrowserDriver browser) {
    try {
      Optional<byte[]> screenshot = browser.screenshot();
      if (screenshot.isEmpty()) {
        log.warn("The browser cannot take screenshots");
      }
      return screenshot;
    } catch (Exception exc) {
      log.warn("Could not take the screenshot of the failed test", exc);
      return Optional.empty();
    }
  }

  private boolean isWritable(By selector) {
    try {
      // A search box that exists but is hidden (selectors without search) cannot receive text
      return getBrowser().isVisible(locator(selector));
    } catch (Exception exc) {
      return false;
    }
  }

  /**
   * Checks whether a located element can receive Selenium input.
   *
   * @param selector Element selector
   * @param diagnosticLabel Wait label used for diagnostics
   * @return Condition for element actionability
   */
  private BrowserCondition inputToBeActionable(By selector, String diagnosticLabel) {
    Locator input = locator(selector);
    return BrowserCondition.of(diagnosticLabel + ": input is visible and enabled for selector " + input,
      browser -> browser.isVisible(input) && browser.isEnabled(input));
  }

  /**
   * Checks whether a located element is ready for click actions.
   *
   * @param selector Element selector
   * @param diagnosticLabel Wait label used for diagnostics
   * @return Condition for element clickability
   */
  private BrowserCondition elementToBeActionableForClick(By selector, String diagnosticLabel) {
    Locator element = locator(selector);
    return BrowserCondition.of(diagnosticLabel + ": element is clickable for selector " + element,
      browser -> BrowserCondition.clickable(element).isMet(browser));
  }

  /**
   * Checks whether a selector is visible and contains the expected text.
   *
   * @param selector     Element selector
   * @param expectedText Expected text
   * @param diagnosticLabel Wait label used for diagnostics
   * @return Condition for selector text readiness
   */
  private static BrowserCondition selectorVisibleWithText(By selector, String expectedText, String diagnosticLabel) {
    Locator element = locator(selector);
    return BrowserCondition.of(diagnosticLabel + ": selector " + element + " is visible with expected text: '" + expectedText + "'",
      browser -> {
        try {
          return browser.isVisible(element) && browser.text(element).contains(expectedText);
        } catch (ElementNotFoundException | ElementReplacedException exc) {
          return false;
        }
      });
  }

  /**
   * Checks whether the matched selector has at least one visible control and every visible control is enabled.
   *
   * @param browser  Browser
   * @param selector Control selector
   * @return Control readiness
   */
  private static boolean visibleControlReady(BrowserDriver browser, By selector) {
    try {
      boolean visibleControlFound = false;
      for (ElementRef element : browser.elements(locator(selector))) {
        if (element.isVisible()) {
          visibleControlFound = true;
          if (!element.isEnabled()) {
            return false;
          }
        }
      }
      return visibleControlFound;
    } catch (ElementReplacedException exc) {
      return false;
    }
  }

  /**
   * Checks whether all visible shell controls from the provided selectors are enabled.
   * Selectors with no visible matches are treated as optional for the current shell.
   *
   * @param browser Browser
   * @return Whether every visible shell control is actionable
   */
  private boolean visibleShellControlsReady(BrowserDriver browser) {
    try {
      for (By selector : frontEndInstructions.getRequiredPostLoginShellControls()) {
        if (!visibleControlReady(browser, selector)) {
          return false;
        }
      }

      for (By selector : frontEndInstructions.getOptionalPostLoginShellControls()) {
        if (hasVisibleElement(browser, selector) && !visibleControlReady(browser, selector)) {
          return false;
        }
      }
      return true;
    } catch (ElementReplacedException exc) {
      return false;
    }
  }

  /**
   * Checks whether any visible element matched by the selector exists.
   *
   * @param browser  Browser
   * @param selector Element selector
   * @return Whether a visible element exists
   */
  private static boolean hasVisibleElement(BrowserDriver browser, By selector) {
    try {
      return browser.elements(locator(selector)).stream().anyMatch(ElementRef::isVisible);
    } catch (ElementReplacedException exc) {
      return true;
    }
  }

  /**
   * Waits until a criterion is ready to receive input.
   *
   * @param criterionName Criterion identifier
   */
  protected void waitForInputActionability(String criterionName) {
    By selector = frontEndInstructions.getCriterionInput(frontEndInstructions.getCriterionCss(criterionName));
    waitUntil(inputToBeActionable(selector, "Input actionability [" + criterionName + "]"));
  }

  /**
   * Waits until a button is ready to be clicked.
   *
   * @param buttonName Button identifier
   */
  protected void waitForButtonClickability(String buttonName) {
    waitUntil(elementToBeActionableForClick(frontEndInstructions.getButton(buttonName), "Button clickability [" + buttonName + "]"));
  }

  /**
   * Checks if the expected login result is still displayed in the login form.
   * This preserves wrong-login flows that intentionally assert login-screen alerts.
   *
   * @param resultSelector Login result selector
   * @param expectedText   Expected result text
   * @return Condition for login-screen result readiness
   */
  private BrowserCondition loginFormResultReady(By resultSelector, String expectedText) {
    BrowserCondition resultShown = selectorVisibleWithText(resultSelector, expectedText, "Login form result");
    return BrowserCondition.of("Login form result: selector " + locator(resultSelector)
        + " is visible with expected text: '" + expectedText + "'",
      browser -> hasVisibleElement(browser, LOGIN_BUTTON) && resultShown.isMet(browser));
  }

  /**
   * Waits until the authenticated shell is ready after successful login.
   *
   * @param postLoginSelector Selector expected in the authenticated shell
   * @param expectedText      Expected text inside the selector
   */
  private void waitForAuthenticatedShell(By postLoginSelector, String expectedText) {
    waitUntil(authenticatedShellReady(postLoginSelector, expectedText));
  }

  /**
   * Checks if authenticated shell signals are ready after login.
   *
   * @param postLoginSelector Selector expected in the authenticated shell
   * @param expectedText      Expected text inside the selector
   * @return Authenticated shell readiness condition
   */
  private BrowserCondition authenticatedShellReady(By postLoginSelector, String expectedText) {
    BrowserCondition loadingBarGone = toBeInvisible(frontEndInstructions.getLoadingBar());
    BrowserCondition postLoginShown = selectorVisibleWithText(postLoginSelector, expectedText, "Post-login selector readiness");
    return BrowserCondition.of("Authenticated shell readiness: loading bar invisible, login form hidden, post-login selector "
        + locator(postLoginSelector) + " contains text: '" + expectedText
        + "', and visible frontend shell controls are actionable",
      browser -> {
        try {
          return loadingBarGone.isMet(browser)
            && !hasVisibleElement(browser, LOGIN_FORM_CONTROLS)
            && postLoginShown.isMet(browser)
            && visibleShellControlsReady(browser);
        } catch (ElementNotFoundException | ElementReplacedException exc) {
          return false;
        }
      });
  }

  /**
   * Waits for the login result without over-constraining expected login-screen errors.
   *
   * @param cssSelector Selector to check
   * @param expectedText Expected text inside selector
   */
  private void waitForExpectedSelectorResult(String cssSelector, String expectedText) {
    waitForExpectedSelectorResult(By.cssSelector(cssSelector), expectedText);
  }

  /**
   * Waits for the login result without over-constraining expected login-screen errors.
   *
   * @param resultSelector Selector to check
   * @param expectedText   Expected text inside selector
   */
  private void waitForExpectedSelectorResult(By resultSelector, String expectedText) {
    waitForLoadingBar();
    waitUntil(BrowserCondition.anyOf(loginFormResultReady(resultSelector, expectedText),
      authenticatedShellReady(resultSelector, expectedText)));

    if (!hasVisibleElement(getBrowser(), LOGIN_BUTTON)) {
      waitForAuthenticatedShell(resultSelector, expectedText);
    }
  }

  /**
   * Type keys on a criterion
   *
   * @param selector Criterion selector to type keys
   * @param text     Text to type
   */
  private void sendKeys(By selector, CharSequence... text) {
    sendKeys(locator(selector), text);
  }

  /**
   * Type keys on a criterion
   *
   * @param selector Criterion selector to type keys
   * @param text     Text to type
   */
  private void sendKeys(Locator selector, CharSequence... text) {
    try {
      getBrowser().type(selector, text.length == 1 ? text[0] : String.join("", text));
    } catch (Exception exc) {
      assertWithScreenshot("Sending keys to element: " + selector + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Bring the search box of a suggest to the center of the viewport before typing in it. The suggestions panel is
   * aligned when it opens (below the search box when there is room, above it otherwise) and a scroll after that leaves
   * it misplaced until the next render of the client, which happens when the search box loses the focus on the press of
   * the click on an option: the option moves from under the pointer and the click is lost.
   *
   * @param selector Search box selector
   */
  private void scrollToTheCenter(By selector) {
    try {
      getBrowser().scrollToCenter(locator(selector));
    } catch (Exception exc) {
      // Typing in it scrolls it into view
      log.debug("Could not scroll the search box to the center", exc);
    }
  }

  /**
   * Clear text on criterion
   *
   * @param selector Criterion selector
   */
  private void clearText(By selector) {
    String textToClear = getBrowser().attribute(locator(selector), "value");
    if (textToClear != null && !textToClear.isEmpty()) {
      getBrowser().clear(locator(selector));
      waitForEmptyText(locator(selector), textToClear);
    }
  }

  /**
   * Move over an element once it is displayed
   *
   * @param selector Element selector
   */
  private void moveTo(By selector) {
    // Wait until element is displayed
    waitUntil(toBeVisible(selector));

    try {
      getBrowser().hover(locator(selector));
    } catch (Exception exc) {
      assertWithScreenshot("Moving over element: " + selector + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Click on an element. A client may replace the element between finding and clicking it (a list that is filtered
   * while the text is typed), so a replaced element is looked up again before giving up. The driver reports it as
   * replaced whether the browser says that it is stale (Chrome) or, when the action is already running, that its origin
   * is not displayed (Firefox)
   *
   * @param selector Element selector
   */
  private void click(By selector) {
    ElementReplacedException replacedException = null;
    for (int attempt = 0; attempt < STALE_RETRY_COUNT; attempt++) {
      // Wait until element is clickable (the selector is resolved again on every check)
      waitUntil(toBeClickable(selector));
      try {
        getBrowser().click(locator(selector));
        return;
      } catch (ElementReplacedException exc) {
        replacedException = exc;
        log.debug("The element to click was replaced, looking for it again: {}", selector);
      } catch (Exception exc) {
        assertWithScreenshot("Clicking on element: " + selector + "\n" + exc.getMessage(), false, exc);
        return;
      }
    }
    assertWithScreenshot("Clicking on element: " + selector + "\n" + replacedException.getMessage(), false, replacedException);
  }

  /**
   * Double click on an element
   *
   * @param selector Element selector
   */
  private void doubleClick(By selector) {
    // Wait until element is clickable
    waitUntil(toBeClickable(selector));

    try {
      getBrowser().doubleClick(locator(selector));
    } catch (Exception exc) {
      assertWithScreenshot("Clicking on element: " + selector + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Context menu on element
   *
   * @param selector Element selector
   */
  private void contextMenu(By selector) {
    // Wait until element is clickable
    waitUntil(toBeClickable(selector));

    try {
      getBrowser().contextClick(locator(selector));
    } catch (Exception exc) {
      assertWithScreenshot("Right clicking on element: " + selector + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Select a date in datepicker
   *
   * @param parentSelector Parent selector
   * @param dateValue      Date value
   */
  private void selectDateFromSelector(String parentSelector, CharSequence dateValue) {
    // Click on date
    clickDateFromSelector(parentSelector);

    // Wait until datepicker is visible
    checkVisible(locator(frontEndInstructions.getDatepicker()));

    // Write text on date
    By activeSelector = frontEndInstructions.getActiveDatepicker();
    writeTextFromSelector(frontEndInstructions.getCriterionInput(parentSelector), dateValue, true, activeSelector);

    // Make click twice if datepicker is still visible
    if (frontEndInstructions.datePickerRequiresManualClick() && getBrowser().exists(locator(activeSelector))) {
        clickSelector(activeSelector);
    }
    // Wait for not visible
    checkNotVisible(locator(frontEndInstructions.getDatepicker()));

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Select from datepicker
   *
   * @param parentSelector Datepicker selector
   * @param type           Select type
   * @param search         Search string
   */
  private void selectFromDatepicker(String parentSelector, String type, String search) {
    // Click on date
    clickDateFromSelector(parentSelector);

    // Wait until datepicker is visible
    waitUntil(toBeVisible(frontEndInstructions.getDatepicker()));

    // Click on selector
    click(frontEndInstructions.getCellFromDatepicker(type, search));

    // Wait for not visible
    checkNotVisible(locator(frontEndInstructions.getDatepicker()));
  }

  /**
   * Click on selector
   *
   * @param selector Selector
   */
  private void clickSelector(By selector) {
    // Wait for element visible
    waitUntil(toBeVisible(selector));

    // Move mouse before clicking on selector
    moveMouse();

    // Click on selector
    click(selector);
  }

  /**
   * Click on date criterion input
   *
   * @param parentSelector Parent selector
   */
  private void clickDateFromSelector(String parentSelector) {
    clickSelector(frontEndInstructions.getDateCriterion(parentSelector));
  }

  /**
   * Checks if loader is not visible
   *
   * @return Condition for loader visibility
   */
  private BrowserCondition checkIfLoaderIsNotVisible() {
    return toBeInvisible(frontEndInstructions.getLoaderSelector());
  }

  /**
   * Checks if grid loader is not visible
   *
   * @return Condition for grid visibility
   */
  private BrowserCondition checkIfGridLoaderIsNotVisible() {
    return toBeInvisible(frontEndInstructions.getGridLoaderSelector());
  }

  /**
   * Click on row
   *
   * @param selector Row selector
   */
  private void clickRowFromSelector(By selector) {
    // Wait for element visible
    waitUntil(BrowserCondition.allOf(toBeVisible(selector), checkIfGridLoaderIsNotVisible()));

    // Click button
    click(selector);
  }

  /**
   * Edit row
   *
   * @param selector Row selector
   */
  private void editRowFromSelector(String gridId, By selector) {
    // Wait for element visible
    waitUntil(BrowserCondition.allOf(toBeVisible(selector), checkIfGridLoaderIsNotVisible()));

    // Depending on behavior, do click or double click
    switch (frontEndInstructions.getRowEditBehavior()) {
      case DOUBLE_CLICK:
        // Double click, as many times as needed to start the edition of the row
        doubleClickToEditRow(gridId, selector);
        break;
      case SINGLE_CLICK:
      default:
        // Click button
        click(selector);
        break;
    }
  }

  /**
   * Double click on a cell to edit its row. The browser takes a double click for two clicks when they are separated by
   * more than the double click interval, and on a loaded machine the second click of the action waits for the browser
   * to draw the selection of the row caused by the first one: the row is selected, but never edited. So, when the client
   * tells which row is being edited, the double click is repeated until the row of the cell is being edited. The client
   * can reject the edition on purpose (the row being edited has errors), so after the last attempt the step goes on.
   *
   * @param gridId   Grid identifier (null for any grid)
   * @param selector Cell selector
   */
  private void doubleClickToEditRow(String gridId, By selector) {
    By rowOfCell = frontEndInstructions.getGridRowOfCell();
    String rowId = rowOfCell == null ? null : getRowIdOfCell(selector, rowOfCell);
    if (rowId == null) {
      doubleClick(selector);
      return;
    }

    for (int attempt = 1; attempt <= EDIT_ROW_ATTEMPTS; attempt++) {
      // The cell has been replaced by its editor when the edition started after the last attempt
      if (attempt > 1 && !getBrowser().exists(locator(selector))) {
        return;
      }
      doubleClick(selector);
      if (waitForRowEdition(gridId, rowId)) {
        return;
      }
      log.warn("The row {} did not start being edited after the double click {} of {}", rowId, attempt, EDIT_ROW_ATTEMPTS);
    }
  }

  /**
   * Retrieve the identifier of the row that contains a cell
   *
   * @param selector  Cell selector
   * @param rowOfCell Selector of the row, relative to the cell
   * @return Row identifier, or null if the row cannot be identified
   */
  private String getRowIdOfCell(By selector, By rowOfCell) {
    try {
      return getBrowser().attribute(locator(selector), locator(rowOfCell), "row-id");
    } catch (ElementNotFoundException | ElementReplacedException exc) {
      log.debug("Could not identify the row of the cell", exc);
      return null;
    }
  }

  /**
   * Wait for a row to be edited, or for the time that the client needs to start it
   *
   * @param gridId Grid identifier (null for any grid)
   * @param rowId  Row identifier
   * @return The row is being edited
   */
  private boolean waitForRowEdition(String gridId, String rowId) {
    By editingRow = frontEndInstructions.getGridEditingRow(gridId, rowId);
    if (editingRow == null) {
      return true;
    }
    Duration timeout = properties.getTimeout().compareTo(EDIT_ROW_WAIT) < 0 ? properties.getTimeout() : EDIT_ROW_WAIT;
    try {
      BrowserPoll.until(getBrowser(), toBePresent(editingRow), timeout);
      return true;
    } catch (BrowserPoll.PollTimeoutException exc) {
      return false;
    }
  }

  /**
   * Click on row with a text
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  private void clickRowContentsFromSelector(String gridId, String search) {
    // Leave the row as it is when the client keeps it selected: selecting it again would unselect it. Wait for the grid
    // to be loaded first, so a row of the previous result set is not taken as the selected one
    By selected = frontEndInstructions.findGridSelectedRow(gridId, search);
    if (selected != null) {
      waitUntil(checkIfGridLoaderIsNotVisible());
      if (getBrowser().exists(locator(selected))) {
        return;
      }
    }
    clickRowFromSelector(frontEndInstructions.findGridRowSelection(gridId, search));
  }

  /**
   * Edit row with a text
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  private void editRowContentsFromSelector(String gridId, String search) {
    editRowFromSelector(gridId, frontEndInstructions.findGridCell(gridId, search));
  }

  /**
   * Context menu on row
   *
   * @param selector Selector to apply
   */
  private void contextMenuFromSelector(By selector) {
    // Wait for element visible
    waitUntil(BrowserCondition.allOf(toBeVisible(selector), checkIfGridLoaderIsNotVisible()));

    // Click button
    contextMenu(selector);
  }

  /**
   * Wait for selector to be clickable
   *
   * @param selector Selector to wait for
   */
  private void waitForSelector(By selector) {
    waitForSelector(locator(selector));
  }

  /**
   * Wait for selector to be clickable
   *
   * @param selector Selector to wait for
   */
  private void waitForSelector(Locator selector) {
    // Wait for element visible
    waitUntil(BrowserCondition.visible(selector));

    // Move mouse again
    moveMouse();
  }

  /**
   * Move mouse to avoid help popovers
   */
  private void moveMouse() {
    try {
      Locator popover = locator(frontEndInstructions.getPopover());
      // Safecheck
      int safecheck = 0;

      // Move mouse while help is being displayed
      while (getBrowser().exists(popover) && safecheck < RETRY_COUNT) {
        getBrowser().pause(Duration.ofMillis(100));
        getBrowser().hover(popover);
        getBrowser().moveMouseBy(0, 30);
        safecheck++;
      }
    } catch (Exception exc) {
      // Assert error moving mouse
      assertWithScreenshot("Moving mouse: " + exc.getMessage(), true);
    }
  }

  /**
   * Move mouse out of criterion
   */
  private void moveMouseOutOfCriterion() {
    try {
      // Move mouse out of criterion (up)
      getBrowser().moveMouseBy(0, -30);
      getBrowser().clickAtPointer();
    } catch (Exception exc) {
      // Assert error moving mouse
      assertWithScreenshot("Moving mouse after criterion: " + exc.getMessage(), true);
    }
  }

  /**
   * Write text check clear text
   *
   * @param selector      Element selector
   * @param text          Text
   * @param clearText     Clear text
   * @param clickSelector Selector for click element
   */
  private void writeTextFromSelector(By selector, CharSequence text, boolean clearText, By clickSelector) {
    // Write text from selector
    writeTextFromSelector(selector, text, clearText);

    // Click on click selector
    waitUntil(toBeClickable(clickSelector));
    clickSelector(clickSelector);
  }

  private void writeTextFromSelector(By selector, CharSequence text, boolean clearText) {
    // Wait for element present
    waitUntil(toBePresent(selector));

    // Clear previous text
    if (clearText) {
      clearText(selector);
    }

    // Write text
    sendKeys(selector, text);
  }

  /**
   * Get criterion text
   *
   * @param parentSelector Parent selector
   * @return Text from criterion
   */
  private String getTextFromSelector(String parentSelector) {
    By selector = frontEndInstructions.getCriterionInput(parentSelector);

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Get selector text
    return getBrowser().attribute(locator(selector), "value");
  }

  /**
   * Click on a checkbox or a radio button
   *
   * @param parentSelector parent selector
   */
  private void clickCheckboxFromSelector(String parentSelector) {
    By selector = frontEndInstructions.getCheckbox(parentSelector);

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click on checkbox
    click(selector);
  }

  /**
   * Click on select box
   *
   * @param parentSelector Select box
   */
  private void suggestClick(String parentSelector) {
    By selector = frontEndInstructions.getSuggestChoice(parentSelector);
    By loaderSelector = frontEndInstructions.getSuggestLoader(parentSelector);

    // Wait for loader
    waitUntil(toBeInvisible(loaderSelector));

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Triple click selector
    click(selector);

    // Wait for element present
    waitUntil(toBePresent(frontEndInstructions.getSuggestDropdownList()));
  }

  /**
   * Click on select box
   *
   * @param parentSelector Select box
   */
  private void selectClick(String parentSelector) {
    By selector = frontEndInstructions.getSelectChoice(parentSelector);
    By loaderSelector = frontEndInstructions.getSelectLoader(parentSelector);

    // Wait for loader
    waitUntil(toBeInvisible(loaderSelector));

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click selector
    click(selector);

    // Wait for element present
    waitUntil(toBePresent(frontEndInstructions.getSelectDropdownList()));
  }

  /**
   * Select first value of the select
   *
   * @param parentSelector Parent selector
   */
  private void selectFirstFromSelector(String parentSelector) {
    // Click on selector
    selectClick(parentSelector);

    // Click option
    click(frontEndInstructions.getSelectDropdownListFirstElement());
  }

  /**
   * Select last element
   *
   * @param parentSelector Parent selector
   */
  private void selectLastFromSelector(String parentSelector) {
    // Click on selector
    selectClick(parentSelector);

    // Click option
    click(frontEndInstructions.getSelectDropdownListLastElement());
  }

  /**
   * Select an element which contains a label
   *
   * @param parentSelector Parent selector
   * @param label          Label to search
   */
  private void selectContainFromSelector(String parentSelector, String label) {
    // Click on selector
    selectClick(parentSelector);

    // Select result on list
    selectResult(label);
  }

  /**
   * Suggest element which contains label
   *
   * @param parentSelector Parent selector
   * @param search         Search string
   * @param label          Label to search
   */
  private void suggestFromSelector(String parentSelector, String search, String label) {
    // Wait for element present
    waitUntil(checkIfLoaderIsNotVisible());

    // Click on selector
    suggestClick(parentSelector);

    // Selectors
    By suggestDropdownListInput = frontEndInstructions.getSuggest(parentSelector);

    // Wait for element present
    waitUntil(toBePresent(suggestDropdownListInput));

    // Write text
    if (isWritable(frontEndInstructions.getSuggestInput(parentSelector))) {
      clearText(suggestDropdownListInput);
      sendKeys(suggestDropdownListInput, search);
    }

    // Wait for loading bar
    waitForLoadingBar();

    // Select result on list
    suggestResult(label);
  }

  /**
   * Suggest last element which contains label
   *
   * @param parentSelector Criterion name
   * @param search         Search string
   */
  private void suggestLastFromSelector(String parentSelector, String search) {
    By selector = frontEndInstructions.getSuggestDropdownListLastElement();
    By suggestDropdownListInput = frontEndInstructions.getSuggestInput(parentSelector);

    // Wait for element present
    waitUntil(checkIfLoaderIsNotVisible());

    // Click on selector
    suggestClick(parentSelector);

    // Wait for element present
    waitUntil(toBePresent(suggestDropdownListInput));

    // Write username
    sendKeys(suggestDropdownListInput, search);

    // Wait for loading bar
    waitForLoadingBar();

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click option
    click(selector);
  }

  /**
   * Suggest or select multiple
   *
   * @param parentSelector Parent selector
   * @param search         Text to search
   * @param label          Text to find in label
   */
  private void suggestMultipleFromSelector(String parentSelector, boolean clear, String search, String label) {
    // Safecheck
    int safecheck = 0;
    By searchBox = frontEndInstructions.getSuggestMultipleInput(parentSelector);
    boolean panelBased = frontEndInstructions.multipleChoiceUsesPanel();

    // Wait for element present
    waitUntil(checkIfLoaderIsNotVisible());

    // Wait for element present. A panel based multiple choice shows its search box only once the panel is open
    if (!panelBased) {
      waitUntil(toBePresent(searchBox));
    }

    // Clear selector
    if (clear) {
      By clearSelector = frontEndInstructions.getSuggestMultipleChoiceClose(parentSelector);
      while (getBrowser().exists(locator(clearSelector)) && safecheck < RETRY_COUNT) {
        click(clearSelector);
        safecheck++;
      }
    }

    // Open the panel when the search box lives inside it (a multiple select). A multiple suggest keeps its search box in
    // the criterion, so nothing is opened and the panel steps below are skipped
    boolean openedPanel = panelBased && !getBrowser().exists(locator(searchBox));
    if (openedPanel) {
      selectClick(parentSelector);
    }
    waitUntil(toBePresent(searchBox));

    // Write search text (the panel keeps the text of the previous search)
    if (openedPanel) {
      clearText(searchBox);
    }
    if (!panelBased) {
      scrollToTheCenter(searchBox);
    }
    sendKeys(searchBox, search);

    // Wait for loading bar
    waitForLoadingBar();

    // Select result on list
    suggestResult(label);

    // The panel this step opened stays open after choosing: close it so it does not cover the next steps
    if (openedPanel) {
      getBrowser().press(Key.ESCAPE);
      // The panel leaves with an exit animation: the next item must find it closed, or it would take it for an open one
      waitUntil(toBeInvisible(searchBox));
    }
  }

  /**
   * Click on save row and wait
   *
   * @param selector Save row selector
   */
  private void saveRowFromSelector(By selector) {
    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click option
    click(selector);
  }

  /**
   * Check text inside selector
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkText(By selector, String text) {
    checkText(locator(selector), text);
  }

  /**
   * Check text inside selector
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkText(Locator selector, String text) {
    String nodeText = getBrowser().text(selector);
    String message = selector.toString() + TEXT_VALUE + nodeText + "' isn't equal to " + text;

    // Assert element is not located
    assertWithScreenshot(message, nodeText.equalsIgnoreCase(text));
  }

  /**
   * Check if selector contains text
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkTextContains(By selector, String text) {
    checkTextContains(locator(selector), text);
  }

  /**
   * Check if selector contains text
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkTextContains(Locator selector, String text) {
    String nodeText = getBrowser().text(selector);
    String message = selector.toString() + TEXT_VALUE + nodeText + "' doesn't contain " + text;

    // Assert element is not located
    assertWithScreenshot(message, nodeText.contains(text));
  }

  /**
   * Check if selector multiple contains text
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkTextMultipleContains(By selector, String text) {
    List<String> nodeValues = getBrowser().texts(locator(selector));
    String message = selector.toString() + " list doesn't contain " + text;

    // Assert element is not located
    assertWithScreenshot(message, nodeValues.stream().anyMatch(t -> t.contains(text)));
  }

  /**
   * Check if selector doesn't contain a text
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkTextNotContains(By selector, String text) {
    checkTextNotContains(locator(selector), text);
  }

  /**
   * Check if selector doesn't contain a text
   *
   * @param selector Selector to check
   * @param text     Text to compare
   */
  private void checkTextNotContains(Locator selector, String text) {
    String nodeText = getBrowser().text(selector);
    String message = selector.toString() + TEXT_VALUE + nodeText + "' contains " + text;

    // Assert element is not located
    assertWithScreenshot(message, !nodeText.contains(text));
  }

  /**
   * Check if a criterion contains text
   *
   * @param selector Criterion selector
   * @param text     Text to compare
   */
  private void checkCriterionContains(By selector, String text) {
    // An asynchronous action may still be changing the value: wait for it, then assert (with evidence) what is shown
    Locator input = locator(selector);
    try {
      BrowserPoll.until(getBrowser(), BrowserCondition.of("the value of " + input + " to contain '" + text + "'",
        browser -> String.valueOf(browser.attribute(input, "value")).contains(text)), properties.getTimeout());
    } catch (Exception exc) {
      log.debug("The value of {} does not contain '{}' yet", selector, text);
    }

    String nodeText = getBrowser().attribute(input, "value");
    String message = selector.toString() + TEXT_VALUE + nodeText + "' doesn't contain " + text;

    // Assert element is not located
    assertWithScreenshot(message, nodeText.contains(text));
  }

  // ===================================================================================================================
  // Public API
  // ===================================================================================================================

  /**
   * Set test title
   *
   * @param title Test title
   */
  protected void setTestTitle(String title) {
    // Info
    log.info("======================================================================================");
    log.info("| " + title);
    log.info("======================================================================================");
    seleniumModel.setTestTitle(title);
  }

  /**
   * Go to a screen defined on the menu
   *
   * @param menuOptions Menu options to navigate to
   */
  protected void gotoScreen(String... menuOptions) {

    int optionNumber = 1;
    for (String option : menuOptions) {
      // Wait for text in selector
      waitUntil(toBeVisible(frontEndInstructions.getMenuOption(option)));

      switch (frontEndInstructions.getMenuBehavior()) {
        case CLICK_ALL:
          clickAllOptions(optionNumber, option, menuOptions);
          break;
        case CLICK_FIRST_AND_OPTION:
          clickFirstAndOption(optionNumber, option, menuOptions);
          break;
        case CLICK_OPTION:
        default:
          clickOption(optionNumber, option, menuOptions);
      }

      seleniumModel.setCurrentOption(option);
      optionNumber++;
    }

    // Wait for the click to take effect
    waitForMenuOption(menuOptions[menuOptions.length - 1]);

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Wait until the click on a menu option has taken effect: the menu dropdown is closed or, in a client whose menu
   * stays open, the screen of the option is the current one
   *
   * @param option Last option clicked
   */
  protected void waitForMenuOption(String option) {
    By activeOption = frontEndInstructions.getMenuActiveOption(option);
    if (activeOption != null) {
      waitUntil(toBeVisible(activeOption));
    } else {
      // Wait for element not visible
      waitUntil(toBeInvisible(frontEndInstructions.getMenuDropdown()));
    }
  }

  private void clickOption(int optionNumber, String option, String[] options) {
    if (optionNumber == options.length) {
      // Click on screen
      click(frontEndInstructions.getMenuOption(option));
    } else {
      moveTo(frontEndInstructions.getMenuOption(option));
    }
  }

  private void clickFirstAndOption(int optionNumber, String option, String[] options) {
    if (optionNumber == 1 || optionNumber == options.length) {
      // Click on screen
      click(frontEndInstructions.getMenuOption(option));
    } else {
      moveTo(frontEndInstructions.getMenuOption(option));
    }
  }

  private void clickAllOptions(int optionNumber, String option, String[] options) {
    // If it is not the last option, check if it is already opened
    boolean hasOpenedChildren = getBrowser().exists(locator(frontEndInstructions.getMenuOpenedChildren(option)));

    if (optionNumber == options.length || !hasOpenedChildren) {
      // Click on screen
      click(frontEndInstructions.getMenuOption(option));
    }
  }

  /**
   * Wait for css selector
   *
   * @param cssSelector CSS Selector
   * @return Locator of the selector
   */
  protected Locator waitForCssLocator(String cssSelector) {
    Locator selector = Locator.css(cssSelector);

    // Wait for selector
    waitForSelector(selector);

    // Return selector
    return selector;
  }

  /**
   * Wait for css selector
   *
   * @param cssSelector CSS Selector
   * @return Selenium locator of the selector
   * @deprecated It returns a Selenium type. Use {@link #waitForCssLocator(String)}, which returns a {@link Locator}.
   * It stays available through the whole 5.x line and is not removed before 6.0
   */
  @Deprecated
  protected By waitForCssSelector(String cssSelector) {
    return waitForCssLocator(cssSelector).toBy();
  }

  /**
   * Wait for loading bar to hide
   */
  protected void waitForLoadingBar() {
    waitUntil(toBeInvisible(frontEndInstructions.getLoadingBar()));
  }

  /**
   * Wait for loading grid to hide
   */
  protected void waitForLoadingGrid() {
    // Wait for element not visible
    waitUntil(checkIfGridLoaderIsNotVisible());

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Wait for button to be clickable
   *
   * @param buttonName Button name
   */
  protected void waitForButton(String buttonName) {
    waitForSelector(frontEndInstructions.getButton(buttonName));
  }

  /**
   * Wait for tab to be clickable
   *
   * @param tabCriterionName Tab criterion name
   */
  protected void waitForTab(String tabCriterionName) {
    waitForSelector(frontEndInstructions.getTab(tabCriterionName));
  }

  /**
   * Wait for context button to be clickable
   *
   * @param buttonName Button name
   */
  protected void waitForContextButton(String buttonName) {
    // Wait some milliseconds
    pause(100);

    // Wait for context button
    waitForSelector(frontEndInstructions.getContextButton(buttonName));
  }

  /**
   * Wait for text inside a tag with a CSS class
   *
   * @param clazz    CSS class
   * @param contains Text to check
   */
  protected void waitForText(String clazz, String contains) {
    By selector = frontEndInstructions.containsText(clazz, contains);

    // Wait for element visible
    waitUntil(toBeVisible(selector));
  }

  /**
   * Wait for text inside an element
   *
   * @param selector Locator of the element
   * @param contains Text to check
   */
  protected void waitForText(Locator selector, String contains) {
    // Wait for element visible
    waitUntil(BrowserCondition.textContains(selector, contains));
  }

  /**
   * Wait for text inside a tag with a CSS class
   *
   * @param selector Selector
   * @param contains Text to check
   * @deprecated Selenium specific. Use {@link #waitForText(Locator, String)}. It stays available through the whole 5.x
   * line and is not removed before 6.0
   */
  @Deprecated
  protected void waitForText(By selector, String contains) {
    waitForText(locator(selector), contains);
  }

  /**
   * Wait for text inside the value of an input
   *
   * @param selector Locator of the input
   * @param contains Text to check
   */
  protected void waitForValue(Locator selector, String contains) {
    // Wait for element visible
    waitUntil(BrowserCondition.valueContains(selector, contains));
  }

  /**
   * Wait for text inside a tag with a CSS class
   *
   * @param selector Selector
   * @param contains Text to check
   * @deprecated Selenium specific. Use {@link #waitForValue(Locator, String)}. It stays available through the whole 5.x
   * line and is not removed before 6.0
   */
  @Deprecated
  protected void waitForValue(By selector, String contains) {
    waitForValue(locator(selector), contains);
  }

  /**
   * Wait for no text in the value of an input
   *
   * @param selector Locator of the input
   * @param text     Text to check
   */
  protected void waitForEmptyText(Locator selector, String text) {
    // Wait for element visible
    waitUntil(BrowserCondition.not(BrowserCondition.valueContains(selector, text)));
  }

  /**
   * Wait for no text in selector
   *
   * @param selector Selector
   * @param text     Text to check
   * @deprecated Selenium specific. Use {@link #waitForEmptyText(Locator, String)}. It stays available through the whole
   * 5.x line and is not removed before 6.0
   */
  @Deprecated
  protected void waitForEmptyText(By selector, String text) {
    waitForEmptyText(locator(selector), text);
  }

  /**
   * Pause
   *
   * @param time Milliseconds
   */
  protected void pause(Integer time) {
    getBrowser().pause(Duration.ofMillis(time));
  }

  /**
   * Click on an element
   *
   * @param cssSelector Input selector
   */
  protected void click(String cssSelector) {
    click(By.cssSelector(cssSelector));
  }

  /**
   * Click on a button
   *
   * @param buttonName Button name
   */
  protected void clickButton(String buttonName) {
    clickButton(buttonName, false);
  }

  /**
   * Click on a button
   *
   * @param buttonName        Button name
   * @param waitForLoadingBar Wait for loading bar after clicking
   */
  protected void clickButton(String buttonName, boolean waitForLoadingBar) {
    // Wait for element visible
    waitForButton(buttonName);

    // Click button
    By selector = frontEndInstructions.getButton(buttonName);
    clickSelector(selector);

    if (waitForLoadingBar) {
      // Wait for loading bar
      waitForLoadingBar();
    }

    // Move mouse
    moveMouse();
  }

  /**
   * Click on a context button
   *
   * @param contextButtonOptionList Context button option list
   */
  protected void clickContextButton(String... contextButtonOptionList) {
    By contextButtonSelector = null;
    for (String contextButtonOption : contextButtonOptionList) {
      // Set context button name
      contextButtonSelector = frontEndInstructions.getContextButton(contextButtonOption);

      // Wait for context button
      waitForContextButton(contextButtonOption);

      // Mouse over context button. The pointer jumps: a pointer that travels from an option to its nested option
      // (Firefox moves it step by step) crosses the options that lie between them and the menu closes the nested ones
      getBrowser().hoverInstantly(locator(contextButtonSelector));
    }

    // Click on last option
    if (contextButtonSelector != null) {
      // Click button
      clickSelector(contextButtonSelector);
    }
  }

  /**
   * Click on tab
   *
   * @param tabName  Tab name
   * @param tabLabel Tab label local
   */
  protected void clickTab(String tabName, String tabLabel) {
    // Wait for tab not disabled
    waitForTab(tabName);

    // Get tab selector
    By tabSelector = frontEndInstructions.getTab(tabName, tabLabel);

    // If tab is visible, click on tab
    if (getBrowser().isVisible(locator(tabSelector))) {
      // Tab selector
      clickSelector(tabSelector);

      // Wait for tab active
      waitUntil(toBeVisible(frontEndInstructions.getTabActive(tabName, tabLabel)));
    } else {
      // If not visible, click on tab menu, wait for dropdown and click on dropdown option
      clickSelector(frontEndInstructions.getTabMenu(tabName));

      // Wait for tab label
      clickSelector(frontEndInstructions.getTabMenuDropdownOption(tabName, tabLabel));

      // Wait for dropdown not visible
      waitUntil(toBeInvisible(frontEndInstructions.getTabMenuDropdown(tabName)));
    }
  }

  /**
   * Click on info button
   *
   * @param infoButtonName Button name
   */
  protected void clickInfoButton(String infoButtonName) {
    clickSelector(frontEndInstructions.getInfoButton(infoButtonName));
  }

  /**
   * Click on tree button
   *
   * @param gridId Grid id
   * @param rowId  Row id
   */
  protected void clickTreeButton(String gridId, String rowId) {

    // Wait until visible
    waitUntil(toBeVisible(frontEndInstructions.getTreeButton(gridId, rowId)));

    // Click on tree button
    clickSelector(frontEndInstructions.getTreeButton(gridId, rowId));

    // Check loader is not visible
    checkNotVisible(locator(frontEndInstructions.getTreeButtonLoader()));

    // Pause to wait tree leaf to open
    pause(250);
  }

  /**
   * Click on datepicker
   *
   * @param criterionName Datepicker name
   */
  protected void clickDate(String criterionName) {
    clickDateFromSelector(frontEndInstructions.getCriterionCss(criterionName));
  }

  /**
   * Click on datepicker on grid
   *
   * @param gridId   Grid id
   * @param columnId Column id
   */
  protected void clickDate(String gridId, String columnId) {
    clickDateFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId));
  }

  /**
   * Click on datepicker on grid
   *
   * @param gridId   Grid id
   * @param rowId    row id
   * @param columnId Column id
   */
  protected void clickDate(String gridId, String rowId, String columnId) {
    clickDateFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId));
  }

  /**
   * Select a date in datepicker
   *
   * @param dateName  Datepicker name
   * @param dateValue Date to select
   */
  protected void selectDate(String dateName, CharSequence dateValue) {
    selectDateFromSelector(frontEndInstructions.getCriterionCss(dateName), dateValue);
  }

  /**
   * Select a date in a grid
   *
   * @param gridId    Grid id
   * @param columnId  Column id
   * @param dateValue Date to select
   */
  protected void selectDate(String gridId, String columnId, CharSequence dateValue) {
    // Select date with parent selector
    selectDateFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId), dateValue);
  }

  /**
   * Select a date in a grid
   *
   * @param gridId    Grid id
   * @param rowId     Row id
   * @param columnId  Column id
   * @param dateValue Date to select
   */
  protected void selectDate(String gridId, String rowId, String columnId, CharSequence dateValue) {
    // Select date with parent selector
    selectDateFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId), dateValue);
  }

  /**
   * Select a day in datepicker (current month)
   *
   * @param dateName Datepicker name
   * @param day      Day to select
   */
  protected void selectDay(String dateName, @Nonnull Integer day) {
    selectFromDatepicker(frontEndInstructions.getCriterionCss(dateName), DAY, day.toString());
  }

  /**
   * Select a day in datepicker (current month) in a grid
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param day      Day to select
   */
  protected void selectDay(String gridId, String columnId, @Nonnull Integer day) {
    // Select date with parent selector
    selectFromDatepicker(frontEndInstructions.getEditingParentCss(gridId, columnId), DAY, day.toString());
  }

  /**
   * Select a day in datepicker (current month) in a grid
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param day      Day to select
   */
  protected void selectDay(String gridId, String rowId, String columnId, @Nonnull Integer day) {
    // Select date with parent selector
    selectFromDatepicker(frontEndInstructions.getParentCss(gridId, rowId, columnId), DAY, day.toString());
  }

  /**
   * Retrieve today day of month
   *
   * @return Day of month as string
   */
  protected Integer getTodayDay() {
    return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
  }

  /**
   * Retrieve today day of month
   *
   * @return Day of month as string
   */
  protected Integer getTomorrowDay() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_MONTH, 1);
    return calendar.get(Calendar.DAY_OF_MONTH);
  }

  /**
   * Select a month in datepicker
   *
   * @param dateName Datepicker name
   * @param month    Month to select
   */
  protected void selectMonth(String dateName, String month) {
    selectFromDatepicker(frontEndInstructions.getCriterionCss(dateName), MONTH, month);
  }

  /**
   * Select a month in datepicker in a grid
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param month    Month to select
   */
  protected void selectMonth(String gridId, String columnId, String month) {
    // Select date with parent selector
    selectFromDatepicker(frontEndInstructions.getEditingParentCss(gridId, columnId), MONTH, month);
  }

  /**
   * Select a month in datepicker in a grid
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param month    Month to select
   */
  protected void selectMonth(String gridId, String rowId, String columnId, String month) {
    // Select date with parent selector
    selectFromDatepicker(frontEndInstructions.getParentCss(gridId, rowId, columnId), MONTH, month);
  }

  /**
   * Select a year in datepicker
   *
   * @param dateName Datepicker name
   * @param year     Year to select
   */
  protected void selectYear(String dateName, @Nonnull Integer year) {
    selectFromDatepicker(frontEndInstructions.getCriterionCss(dateName), YEAR, year.toString());
  }

  /**
   * Select a year in datepicker in a grid
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param year     Year to select
   */
  protected void selectYear(String gridId, String columnId, @Nonnull Integer year) {
    // Select date with parent selector
    selectFromDatepicker(frontEndInstructions.getEditingParentCss(gridId, columnId), YEAR, year.toString());
  }

  /**
   * Select a year in datepicker in a grid
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param year     Year to select
   */
  protected void selectYear(String gridId, String rowId, String columnId, @Nonnull Integer year) {
    // Select date with parent selector
    selectFromDatepicker(frontEndInstructions.getParentCss(gridId, rowId, columnId), YEAR, year.toString());
  }

  /**
   * Click on a checkbox or a radio button
   *
   * @param criterionName Criterion name
   */
  protected void clickCheckbox(String criterionName) {
    clickCheckboxFromSelector(frontEndInstructions.getCriterionCss(criterionName));
  }

  /**
   * Click on an option of a group of buttons (a button checkbox or a button radio rendered as one group)
   *
   * @param criterionName Criterion (group) name
   * @param optionId      Value of the option
   */
  protected void clickCheckboxOption(String criterionName, String optionId) {
    By selector = frontEndInstructions.getCheckboxOption(criterionName, optionId);

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click on the option
    click(selector);
  }

  /**
   * Click on a checkbox or a radio button
   *
   * @param gridId   Grid id
   * @param columnId Column id
   */
  protected void clickCheckbox(String gridId, String columnId) {
    clickCheckboxFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId));
  }

  /**
   * Click on a checkbox or a radio button
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   */
  protected void clickCheckbox(String gridId, String rowId, String columnId) {
    clickCheckboxFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId));
  }

  /**
   * Click on row with a text
   *
   * @param search Text to search
   */
  protected void clickRowContents(String search) {
    clickRowContentsFromSelector(null, search);
  }

  /**
   * Click on row with a text
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  protected void clickRowContents(String gridId, String search) {
    clickRowContentsFromSelector(gridId, search);
  }

  /**
   * Click on a row with a text even if it is already selected, to toggle its selection: unlike
   * {@link #clickRowContents(String, String)} it never leaves a selected row as it is
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  protected void toggleRowContents(String gridId, String search) {
    clickRowFromSelector(frontEndInstructions.findGridRowSelection(gridId, search));
  }

  /**
   * Click on row with a text
   *
   * @param search Text to search
   */
  protected void editRow(String search) {
    editRowContentsFromSelector(null, search);
  }

  /**
   * Click on row with a text
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  protected void editRow(String gridId, String search) {
    editRowContentsFromSelector(gridId, search);
  }

  /**
   * Click on row
   *
   * @param gridId   Grid to search in
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void editRow(String gridId, String rowId, String columnId) {
    editRowFromSelector(gridId, frontEndInstructions.getGridCell(gridId, rowId, columnId));
  }

  /**
   * Click on a cell on selected row
   *
   * @param gridId   Grid id
   * @param columnId Column id
   */
  protected void clickCell(String gridId, String columnId) {
    clickRowFromSelector(frontEndInstructions.getGridCell(gridId, null, columnId));
  }

  /**
   * Click on a grid cell
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   */
  protected void clickCell(String gridId, String rowId, String columnId) {
    clickRowFromSelector(frontEndInstructions.getGridCell(gridId, rowId, columnId));
  }

  /**
   * Context menu on row
   *
   * @param search Text to search
   */
  protected void contextMenuRowContents(String search) {
    contextMenuRowContents(null, search);
  }

  /**
   * Context menu on row
   *
   * @param gridId Grid identifier
   * @param search Text to search
   */
  protected void contextMenuRowContents(String gridId, String search) {
    contextMenuFromSelector(frontEndInstructions.findGridCell(gridId, search));
  }


  /**
   * Context menu on a grid
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   */
  protected void contextMenu(String gridId, String rowId, String columnId) {
    contextMenuFromSelector(frontEndInstructions.getGridCell(gridId, rowId, columnId));
  }

  /**
   * Type keys on a criterion
   *
   * @param selector Criterion selector to type keys
   * @param text     Text to type
   */
  protected void writeTextOnDriver(Locator selector, CharSequence... text) {
    // Wait for element present
    waitUntil(BrowserCondition.present(selector));

    // Write text
    getBrowser().sendKeys(selector, text);
  }

  /**
   * Type keys on a criterion
   *
   * @param selector Criterion selector to type keys
   * @param text     Text to type
   * @deprecated Selenium specific. Use {@link #writeTextOnDriver(Locator, CharSequence...)}. It stays available through
   * the whole 5.x line and is not removed before 6.0
   */
  @Deprecated
  protected void writeTextOnDriver(By selector, CharSequence... text) {
    writeTextOnDriver(locator(selector), text);
  }

  /**
   * Write text on selector
   *
   * @param selector Locator of the element
   * @param text     Text
   */
  protected void writeText(Locator selector, CharSequence text) {
    // Wait for element present
    waitUntil(BrowserCondition.present(selector));

    // Write text
    sendKeys(selector, text);
  }

  /**
   * Write text on selector
   *
   * @param selector Selector
   * @param text     Text
   * @deprecated Selenium specific. Use {@link #writeText(Locator, CharSequence)}. It stays available through the whole
   * 5.x line and is not removed before 6.0
   */
  @Deprecated
  protected void writeText(By selector, CharSequence text) {
    writeText(locator(selector), text);
  }

  /**
   * Write text on criterion
   *
   * @param criterionName Criterion name
   * @param text          Text
   */
  protected void writeText(String criterionName, CharSequence text) {
    writeText(criterionName, text, true);
  }

  /**
   * Write text check clear text
   *
   * @param criterionName Criterion name
   * @param text          Text
   * @param clearText     Clear text
   */
  protected void writeText(String criterionName, CharSequence text, boolean clearText) {
    By criterionSelector = frontEndInstructions.getCriterionInput(frontEndInstructions.getCriterionCss(criterionName));
    waitUntil(toBeVisible(criterionSelector));
    writeTextFromSelector(criterionSelector, text, clearText);
    moveMouseOutOfCriterion();
  }

  /**
   * Write text check clear text
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param text     Text to write
   */
  protected void writeText(String gridId, String columnId, CharSequence text) {
    // Write text on grid
    writeText(gridId, null, columnId, text, true);
  }


  /**
   * Write text check clear text
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param text     Text to write
   */
  protected void writeText(String gridId, String rowId, String columnId, CharSequence text) {
    // Write text on grid
    writeText(gridId, rowId, columnId, text, true);
  }

  /**
   * Write text check clear text
   *
   * @param gridId    Grid id
   * @param rowId     Row id
   * @param columnId  Column id
   * @param text      Text to write
   * @param clearText Clear previous text
   */
  protected void writeText(String gridId, String rowId, String columnId, CharSequence text, boolean clearText) {
    // Write text on grid
    String parentCss = rowId == null ? frontEndInstructions.getEditingParentCss(gridId, columnId)
      : frontEndInstructions.getParentCss(gridId, rowId, columnId);
    By selector = frontEndInstructions.getCriterionInput(parentCss);
    writeTextFromSelector(selector, text, clearText, selector);
  }

  /**
   * Clear text on input selector
   *
   * @param cssSelector Input selector
   */
  protected void clearText(String cssSelector) {
    clearText(By.cssSelector(cssSelector));
  }

  /**
   * Get criterion text
   *
   * @param criterionName Criterion name
   * @return Text from criterion
   */
  protected String getText(String criterionName) {
    return getTextFromSelector(frontEndInstructions.getCriterionCss(criterionName));
  }

  /**
   * Get the text of the editor of a cell of the row being edited (the selected one, in the clients that edit the row
   * the user selects)
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @return Cell text
   */
  protected String getText(String gridId, String columnId) {
    // The text of a cell is read from its editor, so the row is the one being edited
    return getTextFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId));
  }

  /**
   * Get grid cell text
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @return Cell text
   */
  protected String getText(String gridId, String rowId, String columnId) {
    return getTextFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId));
  }

  /**
   * Select first value of the select
   *
   * @param criterionName Criterion name
   */
  protected void selectFirst(String criterionName) {
    selectFirstFromSelector(frontEndInstructions.getCriterionCss(criterionName));
  }

  /**
   * Select first value of the select
   *
   * @param gridId   Grid id
   * @param columnId Column id
   */
  protected void selectFirst(String gridId, String columnId) {
    selectFirstFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId));
  }

  /**
   * Select first value of the select
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   */
  protected void selectFirst(String gridId, String rowId, String columnId) {
    selectFirstFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId));
  }

  /**
   * Select first value of the select
   *
   * @param criterionName Criterion name
   */
  protected void selectLast(String criterionName) {
    selectLastFromSelector(frontEndInstructions.getCriterionCss(criterionName));
  }

  /**
   * Select first value of the select
   *
   * @param gridId   Grid id
   * @param columnId Column id
   */
  protected void selectLast(String gridId, String columnId) {
    selectLastFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId));
  }

  /**
   * Select first value of the select
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   */
  protected void selectLast(String gridId, String rowId, String columnId) {
    selectLastFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId));
  }

  /**
   * Select value on the selector
   *
   * @param criterionName Criterion name
   * @param label         Label to search
   */
  protected void selectContain(String criterionName, String label) {
    selectContainFromSelector(frontEndInstructions.getCriterionCss(criterionName), label);
  }

  /**
   * Select value on the selector
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param label    Label to search
   */
  protected void selectContain(String gridId, String columnId, String label) {
    selectContainFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId), label);
  }

  /**
   * Select value on the selector
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param label    Label to search
   */
  protected void selectContain(String gridId, String rowId, String columnId, String label) {
    selectContainFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId), label);
  }

  /**
   * Select all rows of grid
   *
   * @param gridId Grid id
   */
  protected void selectAllRowsOfGrid(String gridId) {
    String parentSelector = frontEndInstructions.getParentCss(gridId, null, null);

    By selector = By.cssSelector(parentSelector);

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click on checkbox
    click(selector);

  }

  /**
   * Select result on select list
   *
   * @param match Match label
   */
  protected void selectResult(String match) {
    By selector = frontEndInstructions.getSelectResult(match);

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click option
    click(selector);
  }

  /**
   * Select suggest result on suggest list
   *
   * @param match Match label
   */
  protected void suggestResult(String match) {
    By selector = frontEndInstructions.getSuggestResult(match);

    // Wait for element present
    waitUntil(toBePresent(selector));

    // Click option
    click(selector);
  }

  /**
   * Suggest element which contains label
   *
   * @param criterionName Criterion name
   * @param search        Search string
   * @param label         Label to search
   */
  protected void suggest(String criterionName, String search, String label) {
    suggestFromSelector(frontEndInstructions.getCriterionCss(criterionName), search, label);
  }

  /**
   * Suggest element which contains label
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param search   Search string
   * @param label    Label to search
   */
  protected void suggest(String gridId, String columnId, String search, String label) {
    suggestFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId), search, label);
  }

  /**
   * Suggest element which contains label
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param search   Search string
   * @param label    Label to search
   */
  protected void suggest(String gridId, String rowId, String columnId, String search, String label) {
    suggestFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId), search, label);
  }

  /**
   * Suggest element which contains label
   *
   * @param criterionName Criterion name
   * @param search        Search string
   */
  protected void suggestLast(String criterionName, String search) {
    suggestLastFromSelector(frontEndInstructions.getCriterionCss(criterionName), search);
  }

  /**
   * Suggest element which contains label
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param search   Search string
   */
  protected void suggestLast(String gridId, String columnId, String search) {
    suggestLastFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId), search);
  }

  /**
   * Suggest element which contains label
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param search   Search string
   */
  protected void suggestLast(String gridId, String rowId, String columnId, String search) {
    suggestLastFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId), search);
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param criterionName Criterion name
   * @param items         Items to add and search for
   */
  protected void suggestMultipleList(String criterionName, String... items) {
    boolean clear = true;
    for (String item : items) {
      suggestMultiple(criterionName, clear, item, item);
      clear = false;
    }
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param criterionName Criterion name
   * @param search        Search string
   * @param label         Text to find in label
   */
  protected void suggestMultiple(String criterionName, String search, String label) {
    suggestMultiple(criterionName, true, search, label);
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param search   Search string
   * @param label    Text to find in label
   */
  protected void suggestMultiple(String gridId, String columnId, String search, String label) {
    suggestMultiple(gridId, null, columnId, true, search, label);
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param search   Search string
   * @param label    Text to find in label
   */
  protected void suggestMultiple(String gridId, String rowId, String columnId, String search, String label) {
    suggestMultiple(gridId, rowId, columnId, true, search, label);
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param criterionName Criterion name
   * @param search        Search string
   * @param label         Text to find in label
   */
  protected void suggestMultiple(String criterionName, boolean clear, String search, String label) {
    suggestMultipleFromSelector(frontEndInstructions.getCriterionCss(criterionName), clear, search, label);
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @param search   Search string
   * @param label    Text to find in label
   */
  protected void suggestMultiple(String gridId, String columnId, boolean clear, String search, String label) {
    suggestMultipleFromSelector(frontEndInstructions.getEditingParentCss(gridId, columnId), clear, search, label);
  }

  /**
   * Suggest or select multiple element which contains label
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param search   Search string
   * @param label    Text to find in label
   */
  protected void suggestMultiple(String gridId, String rowId, String columnId, boolean clear, String search, String label) {
    suggestMultipleFromSelector(frontEndInstructions.getParentCss(gridId, rowId, columnId), clear, search, label);
  }

  /**
   * Click on search button (ButSch) and wait the grid to load
   */
  protected void searchAndWait() {
    searchAndWait("ButSch");
  }

  /**
   * Click on search button and wait the grid to load
   *
   * @param buttonName Button name
   */
  protected void searchAndWait(String buttonName) {
    clickButton(buttonName, false);

    // Wait for loading bar
    waitForLoadingGrid();

    // Move mouse
    moveMouse();
  }

  /**
   * Click on save row and wait
   */
  protected void saveRow() {
    saveRowFromSelector(frontEndInstructions.getGridSaveButton());
  }

  /**
   * Click on save row and wait
   *
   * @param gridId Grid with the save button
   */
  protected void saveRow(String gridId) {
    saveRowFromSelector(frontEndInstructions.getGridSaveButton(gridId));
  }


  /**
   * Scroll grid
   *
   * @param gridId     Grid identifier
   * @param horizontal Horizontal scroll in pixels
   * @param vertical   Vertical scroll in pixels
   */
  protected void scrollGrid(String gridId, int horizontal, int vertical) {
    getBrowser().scrollTo(locator(frontEndInstructions.getGridScrollZone(gridId)), horizontal, vertical);
  }

  /**
   * Show mouse
   */
  protected void showMouse() {
    getBrowser().executeScript("let seleniumFollowerImg=document.createElement(\"span\");" +
      "seleniumFollowerImg.setAttribute('id', 'selenium_mouse');" +
      "seleniumFollowerImg.setAttribute('style', 'position: absolute; z-index: 99999999999; pointer-events: none; transition: all .1s ease, text-shadow .1s linear; -moz-transition: all .01s linear, text-shadow .1s linear; color: white;-webkit-text-stroke-width: 2px;-webkit-text-stroke-color: #000;');" +
      // Visual aid for recordings: the pointer icon is injected, it is not a locator and no test looks for it
      "seleniumFollowerImg.classList.add('fa', 'fa-mouse-pointer', 'fa-2x');" +
      "document.body.appendChild(seleniumFollowerImg);" +
      "document.addEventListener('mousemove', function(e) {" +
      "let seleniumMouse=document.getElementById(\"selenium_mouse\");" +
      "seleniumMouse.style.left=e.pageX + 'px';" +
      "seleniumMouse.style.top=e.pageY + 'px';" +
      "});" +
      "document.addEventListener('click', function(e) {" +
      "let seleniumMouse=document.getElementById(\"selenium_mouse\");" +
      "seleniumMouse.style.textShadow='0 0 20px blue';" +
      "seleniumMouse.style.color='blue';" +
      "setTimeout(function() {seleniumMouse.style.textShadow='0 0 0px blue';seleniumMouse.style.color='white';}, 100);" +
      "});");
  }

  /**
   * Click on column header
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void sortGrid(String gridId, String columnId) {
    // Click on header
    clickRowFromSelector(frontEndInstructions.getGridHeader(gridId, columnId));

    // Wait for loading bar
    waitForLoadingGrid();
  }

  /**
   * Accept confirm window and wait for it to disappear
   */
  protected void acceptConfirm() {
    // Pause 300 ms
    pause(300);

    // Click on button
    clickButton("confirm-accept");

    // Wait for element not present
    waitUntil(toBeInvisible(By.id("confirm-accept")));
  }

  /**
   * Check a message box and close it
   *
   * @param messageType Message type (success (default), info, warning, danger)
   */
  protected void checkAndCloseMessage(String messageType) {
    By messageSelector = frontEndInstructions.getMessage(messageType);

    // Wait for message selector
    waitUntil(toBeClickable(messageSelector));

    // Click on message selector
    click(messageSelector);

    // Wait for element not present
    waitUntil(toBeInvisible(messageSelector));

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Close the messages of a stack one by one. A screen that shows several messages (one per failed query) keeps its
   * controls blocked until the user has closed all of them
   *
   * @param messageType Message type (success, info, warning, danger)
   * @param count       Number of messages to close
   */
  protected void closeMessages(String messageType, int count) {
    By messageSelector = frontEndInstructions.getMessage(messageType);

    for (int closed = 0; closed < count; closed++) {
      // Wait for a message to close
      waitUntil(toBeClickable(messageSelector));
      Locator messages = locator(messageSelector);
      List<ElementRef> shown = getBrowser().elements(messages);
      if (shown.isEmpty()) {
        throw new ElementNotFoundException(messages, null);
      }
      ElementRef message = shown.get(0);

      // Close it and wait for it to leave (the client may show the next message of the stack in its place)
      click(messageSelector);
      waitUntil(BrowserCondition.of("message " + messages + " to leave the page", browser -> !browser.elements(messages).contains(message)));
    }
  }

  /**
   * Click on confirm button, accept confirmation and accept message
   *
   * @param button Button name
   */
  protected void clickButtonAndConfirm(String button) {
    clickButtonAndConfirm(button, "success");
  }

  /**
   * Click on confirm button, accept confirmation and accept message
   *
   * @param button      Button name
   * @param messageType Message type (info, warning, success, danger)
   */
  protected void clickButtonAndConfirm(String button, String messageType) {
    // Click on button
    clickButton(button);

    // Accept confirm
    acceptConfirm();

    // Accept message
    checkAndCloseMessage(messageType);
  }

  /**
   * Check text inside css selector
   *
   * @param cssSelector Selector to check
   * @param text        Text to compare
   */
  protected void checkText(String cssSelector, String text) {
    // Check selector text
    checkText(waitForCssLocator(cssSelector), text);
  }

  /**
   * Check text inside css selector
   *
   * @param cssSelector Selector to check
   * @param text        Text to compare
   */
  protected void checkTextContains(String cssSelector, String text) {
    // Check selector text
    checkTextContains(waitForCssLocator(cssSelector), text);
  }

  /**
   * heck if selector doesn't contain a text
   *
   * @param cssSelector Selector to check
   * @param text        Text to compare
   */
  protected void checkTextNotContains(String cssSelector, String text) {
    checkTextNotContains(waitForCssLocator(cssSelector), text);
  }

  /**
   * Check if grid contains some texts
   *
   * @param searchList Texts to search for in the grid
   */
  protected void checkRowContents(String... searchList) {
    checkRowContentsGrid(null, searchList);
  }

  /**
   * Check if grid contains some texts
   *
   * @param gridId     Grid Identifier
   * @param searchList Texts to search for in the grid
   */
  protected void checkRowContentsGrid(String gridId, String... searchList) {
    for (String search : searchList) {
      By selector = frontEndInstructions.findGridCell(gridId, search);

      // Wait for element visible
      waitUntil(BrowserCondition.allOf(toBeVisible(selector), checkIfGridLoaderIsNotVisible()));

      // Check text
      checkTextContains(selector, search);
    }
  }

  /**
   * Check cell contents
   *
   * @param gridId   Grid id
   * @param rowId    Row id
   * @param columnId Column id
   * @param search   Search value
   */
  protected void checkCellContents(String gridId, String rowId, String columnId, String search) {
    By selector = frontEndInstructions.getGridCellText(gridId, rowId, columnId, search);

    // Wait for element visible
    waitUntil(BrowserCondition.allOf(toBeVisible(selector), checkIfGridLoaderIsNotVisible()));

    // Check text
    checkTextContains(selector, search);
  }

  /**
   * Check if grid doesn't contain some texts
   *
   * @param search Texts to search for in the grid
   */
  protected void checkRowNotContains(String search) {
    By selector = frontEndInstructions.findGridCell(null, search);

    BrowserCondition condition = BrowserCondition.allOf(toBeInvisible(selector), checkIfGridLoaderIsNotVisible());

    // Assert element is not located
    assertWithScreenshot(condition.toString(), condition.isMet(getBrowser()));
  }

  /**
   * Assert if a criterion contains a text
   *
   * @param criterionName Criterion name
   * @param search        Text to check
   */
  protected void checkCriterionContents(String criterionName, String search) {
    By selector = frontEndInstructions.getCriterionInput(frontEndInstructions.getCriterionCss(criterionName));

    // Wait for element visible
    waitUntil(toBePresent(selector));

    // Check text
    checkCriterionContains(selector, search);
  }

  /**
   * Assert if some criteria are checked or not
   *
   * @param isChecked     Flag to check
   * @param criteriaNames Elements to check
   */
  protected void checkCheckboxRadio(boolean isChecked, String... criteriaNames) {
    // Wait for element visible
    Arrays.stream(criteriaNames)
      .forEach(criterionName -> waitUntil(toBePresent(frontEndInstructions.getCheckboxChecked(criterionName, isChecked))));
  }

  /**
   * Assert if a selector contains a text
   *
   * @param criterionName Selector name
   * @param search        Text to check
   */
  protected void checkSelectContents(String criterionName, String search) {
    By selector = frontEndInstructions.getSelectChosen(criterionName);

    // Wait for element visible
    waitUntil(toBeVisible(selector));

    // Check text
    checkTextContains(selector, search);
  }

  /**
   * Assert if a suggest contains a text
   *
   * @param criterionName Selector name
   * @param search        Text to check
   */
  protected void checkSuggestContents(String criterionName, String search) {
    By selector = frontEndInstructions.getSuggestChosen(criterionName);

    // Wait for element visible
    waitUntil(BrowserCondition.allOf(
      toBeVisible(selector),
      toBeInvisible(frontEndInstructions.getSuggestLoader(frontEndInstructions.getCriterionCss(criterionName)))));

    switch (frontEndInstructions.getSuggestBehavior()) {
      case TEXT:
        // Check text
        checkTextContains(selector, search);
        break;
      case INPUT:
      default:
        // Check input
        checkCriterionContains(selector, search);
        break;
    }

  }

  /**
   * Assert if a selector contains a number of results
   *
   * @param criterionName Selector name
   * @param number        Number of results to check
   */
  protected void checkSelectNumberOfResults(String criterionName, Integer number) {
    selectClick(frontEndInstructions.getCriterionCss(criterionName));

    // Assert element is not located
    assertWithScreenshot("Number of elements doesn't match", number == getBrowser().count(locator(frontEndInstructions.getSelectDropdownListElements())));
  }

  /**
   * Assert if a selector contains a text
   *
   * @param criterionName Selector name
   * @param search        Text to check
   */
  protected void checkMultipleSelectorContents(String criterionName, String search) {
    By selector = frontEndInstructions.getSelectMultipleTextContainer(criterionName);

    // Wait for element visible
    waitUntil(toBeVisible(selector));

    // Check text
    checkTextMultipleContains(selector, search);
  }

  /**
   * Check if message is missing
   *
   * @param messageType Message type
   */
  protected void checkMessageMissing(String messageType) {
    By messageSelector = frontEndInstructions.getMessage(messageType);

    // Wait 1 second
    pause(1000);
    int messages = getBrowser().count(locator(messageSelector));

    // Check there are no messages of messageType
    assertEquals(0, messages);
  }

  /**
   * Check element is present
   *
   * @param cssSelector CSS selector
   */
  protected void checkPresence(String cssSelector) {
    By selector = By.cssSelector(cssSelector);

    // Wait until visible
    waitUntil(toBePresent(selector));
  }

  /**
   * Check element is visible
   *
   * @param cssSelector CSS selector
   */
  protected void checkVisible(String cssSelector) {
    checkVisible(Locator.css(cssSelector));
  }

  /**
   * Check element is visible
   *
   * @param selector Selector
   */
  protected void checkVisible(Locator selector) {
    // Wait until visible
    waitUntil(BrowserCondition.visible(selector));
  }

  /**
   * Check element is visible
   *
   * @param selector Selector
   * @deprecated Selenium specific. Use {@link #checkVisible(Locator)}. It stays available through the whole 5.x line and
   * is not removed before 6.0
   */
  @Deprecated
  protected void checkVisible(By selector) {
    checkVisible(locator(selector));
  }

  /**
   * Check element is visible
   *
   * @param cssSelector CSS selector
   * @param search      Search string
   */
  protected void checkVisibleAndContains(String cssSelector, String search) {
    // Check if it is visible
    checkVisible(cssSelector);

    // Check text contains
    checkTextContains(By.cssSelector(cssSelector), search);
  }

  /**
   * Check element is not visible
   *
   * @param cssSelector CSS selector
   */
  protected void checkNotVisible(String cssSelector) {
    checkNotVisible(Locator.css(cssSelector));
  }

  /**
   * Check element is not visible
   *
   * @param selector selector
   */
  protected void checkNotVisible(Locator selector) {
    // Wait until not visible
    waitUntil(BrowserCondition.invisible(selector));
  }

  /**
   * Check element is not visible
   *
   * @param selector selector
   * @deprecated Selenium specific. Use {@link #checkNotVisible(Locator)}. It stays available through the whole 5.x line
   * and is not removed before 6.0
   */
  @Deprecated
  protected void checkNotVisible(By selector) {
    checkNotVisible(locator(selector));
  }

  /**
   * Starting point; Go to a determined url
   *
   * @param url Start url
   */
  protected void goToUrl(String url) {
    assertNotNull(getBrowser());
    seleniumModel.setCurrentOption("login");

    log.info("Launching tests with '{}' browser: {}'", properties.getBrowser(), seleniumModel.getBaseUrl());

    // Set driver timeout
    getBrowser().setScriptTimeout(properties.getTimeout());

    // Open page in different browsers
    getBrowser().open(url);

    // Show mouse if defined
    if (properties.isShowMouse()) {
      showMouse();
    }

    // Wait for load
    waitForLoad();
  }

  /**
   * Log into the application
   *
   * @param username    User name
   * @param password    Password
   * @param cssSelector Selector to check
   * @param checkText   Text to check inside selector
   */
  protected void checkLogin(String username, String password, String cssSelector, String checkText) {
    // Fill the login form and submit it
    submitLogin(username, password);

    // Wait for login result or authenticated shell readiness
    waitForExpectedSelectorResult(cssSelector, checkText);

    // Assertion
    checkText(cssSelector, checkText);
  }

  /**
   * Log into the application and check the name of the logged user
   *
   * @param username Login of the user
   * @param password Password
   * @param userName Name that the application shows for the logged user
   */
  protected void checkLogin(String username, String password, String userName) {
    By loggedUser = frontEndInstructions.getLoggedUser();

    // Fill the login form and submit it
    submitLogin(username, password);

    // Wait for the authenticated shell
    waitForExpectedSelectorResult(loggedUser, userName);

    // Assertion
    checkText(loggedUser, userName);
  }

  /**
   * Try to log into the application with credentials that the application rejects, and check the message
   *
   * @param username    Login of the user
   * @param password    Password
   * @param messageType Type of the message that the application shows (success, info, warning, danger)
   * @param title       Title of the message
   * @param message     Text of the message
   */
  protected void checkLoginRejected(String username, String password, String messageType, String title, String message) {
    By messageText = frontEndInstructions.getMessageText(messageType);

    // Fill the login form and submit it
    submitLogin(username, password);

    // Wait for the message of the login screen
    waitForExpectedSelectorResult(messageText, message);

    // Assertions
    checkText(messageText, message);
    checkMessageTitle(messageType, title);
  }

  /**
   * Check that a field of the login form holds the text that was typed in it
   *
   * @param criterionName Criterion of the field
   * @param expected      Typed text
   * @return The field holds the text, or it is not there any more
   */
  private boolean loginFieldHolds(String criterionName, String expected) {
    Locator input = locator(frontEndInstructions.getCriterionInput(frontEndInstructions.getCriterionCss(criterionName)));
    try {
      return expected.equals(getBrowser().attribute(input, "value"));
    } catch (ElementNotFoundException exc) {
      // A field that is not there is not filled again: the login has moved on
      return true;
    } catch (ElementReplacedException exc) {
      // The form was drawn again while it was read, and a new form comes back empty
      return false;
    }
  }

  /**
   * Fill the login form and submit it
   *
   * @param username Login of the user
   * @param password Password
   */
  private void submitLogin(String username, String password) {
    // Go to base URL
    goToUrl(seleniumModel.getBaseUrl());

    // Test title
    setTestTitle("Login test: Log into the application");

    // Wait for login form inputs to be actionable
    waitForInputActionability("cod_usr");
    waitForInputActionability("pwd_usr");

    // Write username
    writeText("cod_usr", username);

    // Write password
    writeText("pwd_usr", password);

    // A login form that is initialized again while it is being filled (right after a logout) loses what was typed
    if (!loginFieldHolds("cod_usr", username) || !loginFieldHolds("pwd_usr", password)) {
      log.warn("The login form was cleared while it was being filled, filling it again");
      writeText("cod_usr", username);
      writeText("pwd_usr", password);
    }

    // Wait for login button to be clickable
    waitForButtonClickability("ButLogIn");

    // Click button
    clickButton("ButLogIn", true);
  }

  /**
   * Log out the application
   *
   * @param cssSelector Selector to check
   * @param checkText   Text to check inside selector
   */
  protected void checkLogout(String cssSelector, String checkText) {
    // Test title
    setTestTitle("Logout test: Log out the application");

    // Wait for element not visible
    waitForLoadingBar();

    // Wait for element present
    clickButton("ButLogOut", true);

    // Wait for text in selector
    checkText(cssSelector, checkText);
  }

  /**
   * Log out the application and check that the login screen is shown
   */
  protected void checkLogout() {
    logout(false);
  }

  /**
   * Log out of an application that asks for a confirmation first, and check that the login screen
   * is shown
   */
  protected void checkLogoutWithConfirmation() {
    logout(true);
  }

  /**
   * Log out the application and check that the login screen is shown
   *
   * @param confirm Accept the confirmation that the application asks for
   */
  private void logout(boolean confirm) {
    By marker = frontEndInstructions.getLoginScreenMarker();

    // Test title
    setTestTitle("Logout test: Log out the application");

    // Wait for element not visible
    waitForLoadingBar();

    // Open the user menu when the logout button is inside it
    Optional.ofNullable(frontEndInstructions.getUserMenuButtonId()).ifPresent(this::clickInfoButton);

    // Click on logout
    clickButton("ButLogOut", true);

    // Accept the confirmation
    if (confirm) {
      acceptConfirm();
    }

    // Wait for the login screen
    waitForSelector(marker);
    checkText(marker, frontEndInstructions.getLoginScreenText());
  }

  /**
   * Select module in module list
   *
   * @param moduleName Module name
   */
  protected void selectModule(String moduleName) {
    // Click on info button
    clickInfoButton("ButSetTog");

    // Suggest
    selectContain("module", moduleName);

    // Wait for loading bar
    waitForLoadingBar();
  }

  /**
   * Broadcast a message to a user
   *
   * @param user User name
   * @param text Text to send
   */
  protected void broadcastMessageToUser(String user, String text) {
    // Go to broadcast screen
    gotoScreen("tools", "broadcast-messages");

    // Suggest
    suggest("MsgTar", user, user);

    // Write on criterion
    writeText("MsgDes", text);

    // Search and wait
    clickButton("ButSnd");

    // Accept message
    checkAndCloseMessage("success");

    // Accept message
    checkAndCloseMessage("info");
  }
  /*
  =================================
  SEMANTIC STEPS

  Steps that tests can express without selectors or automation types: the front end instructions locate what they ask
  for (see IAweFrontEndInstructions), so the same step runs on every client.
  =================================
  */

  /**
   * Check the title of a message
   *
   * @param messageType Message type (success, info, warning, danger)
   * @param text        Expected title
   */
  protected void checkMessageTitle(String messageType, String text) {
    By selector = frontEndInstructions.getMessageTitle(messageType);
    waitForSelector(selector);
    checkText(selector, text);
  }

  /**
   * Check the text of a message
   *
   * @param messageType Message type (success, info, warning, danger)
   * @param text        Expected text
   */
  protected void checkMessageText(String messageType, String text) {
    By selector = frontEndInstructions.getMessageText(messageType);
    waitForSelector(selector);
    checkText(selector, text);
  }

  /**
   * Check that an option of the application menu is visible and contains a text
   *
   * @param option Option name
   * @param text   Text that the option contains
   */
  protected void checkMenuOption(String option, String text) {
    By selector = frontEndInstructions.getMenuOptionItem(option);
    checkVisible(locator(selector));
    waitForText(locator(selector), text);
  }

  /**
   * Check the label of a criterion
   *
   * @param criterionName Criterion name
   * @param text          Expected label
   */
  protected void checkCriterionLabel(String criterionName, String text) {
    By selector = frontEndInstructions.getCriterionLabel(criterionName);
    waitForSelector(selector);
    checkText(selector, text);
  }

  /**
   * Check the unit addon of a criterion
   *
   * @param criterionName Criterion name
   * @param text          Expected unit
   */
  protected void checkCriterionUnit(String criterionName, String text) {
    By selector = frontEndInstructions.getCriterionUnit(criterionName);
    waitForSelector(selector);
    checkText(selector, text);
  }

  /**
   * Check that the screen shows validation errors
   */
  protected void checkValidationErrorVisible() {
    checkVisible(locator(frontEndInstructions.getValidationError()));
  }

  /**
   * Click on a day of the open datepicker that can be picked
   */
  protected void clickEnabledDatepickerDay() {
    click(frontEndInstructions.getEnabledDatepickerDay());
  }

  /**
   * Check the number of the active step of a wizard
   *
   * @param number Expected number of the step
   */
  protected void checkActiveWizardStep(String number) {
    checkVisible(locator(frontEndInstructions.getActiveWizardStep(number)));
  }

  /**
   * Check that a tag list contains a text
   *
   * @param tagListId Tag list identifier
   * @param text      Text that the tag list contains
   */
  protected void checkTagListContains(String tagListId, String text) {
    By selector = frontEndInstructions.getTagList(tagListId);
    checkVisible(locator(selector));
    checkTextContains(selector, text);
  }

  /**
   * Check that a chart is displayed
   *
   * @param chartId Chart identifier
   */
  protected void checkChartVisible(String chartId) {
    checkVisible(locator(frontEndInstructions.getChart(chartId)));
  }

  /**
   * Wait until the log viewer shows a text
   *
   * @param text Text that the log contains
   */
  protected void checkLogViewerContains(String text) {
    By selector = frontEndInstructions.getLogViewer();
    waitForText(locator(selector), text);
    checkTextContains(selector, text);
  }

  /**
   * Check a text inside the frame that embeds an external application. The frame is the only one of the screen, and the
   * content is the one of the external application, so the selector of the element to read is supplied by the caller
   *
   * @param contentCssSelector CSS selector of the element to read, inside the embedded application
   * @param text               Expected text
   */
  protected void checkTextInEmbeddedFrame(String contentCssSelector, String text) {
    By frameSelector = frontEndInstructions.getEmbeddedFrame();

    // Wait for the frame
    waitForSelector(frameSelector);

    // Check inside the frame, and return to the page afterwards
    getBrowser().inFrame(locator(frameSelector), () -> checkText(contentCssSelector, text));
  }

  /**
   * Invalidate the session of the user from another window, as if it had been closed on the server
   */
  protected void invalidateSession() {
    getBrowser().executeScript("var winNew = window.open('" + getBaseUrl()
      + "session/invalidate','_blank', 'width=1, height=1');setTimeout(function(){ winNew.close();}, 1000);");
  }

  /**
   * Check that a button is displayed, whatever its state
   *
   * @param buttonId Button identifier
   */
  protected void checkButtonVisible(String buttonId) {
    checkVisible(locator(frontEndInstructions.getAnyButton(buttonId)));
  }

  /**
   * Check that a button is not displayed
   *
   * @param buttonId Button identifier
   */
  protected void checkButtonNotVisible(String buttonId) {
    checkNotVisible(locator(frontEndInstructions.getAnyButton(buttonId)));
  }

  /**
   * Check that a button is displayed and disabled
   *
   * @param buttonId Button identifier
   */
  protected void checkButtonDisabled(String buttonId) {
    checkVisible(locator(frontEndInstructions.getDisabledButton(buttonId)));
  }

  /**
   * Check that a grid exists in the screen, even if it is hidden
   *
   * @param gridId Grid identifier
   */
  protected void checkGridPresent(String gridId) {
    waitUntil(toBePresent(frontEndInstructions.getGrid(gridId)));
  }

  /**
   * Check that a grid is not displayed
   *
   * @param gridId Grid identifier
   */
  protected void checkGridNotVisible(String gridId) {
    checkNotVisible(locator(frontEndInstructions.getGrid(gridId)));
  }

  /**
   * Check that every row of a grid is selected (the checkbox of its header is checked)
   *
   * @param gridId Grid identifier
   */
  protected void checkAllRowsSelected(String gridId) {
    waitUntil(toBePresent(frontEndInstructions.getGridHeaderCheckboxSelected(gridId)));
  }

  /**
   * Click on the area of a grid that shows its rows (outside any row)
   *
   * @param gridId Grid identifier
   */
  protected void clickGridViewport(String gridId) {
    click(frontEndInstructions.getGridScrollZone(gridId));
  }

  /**
   * Check the number of rows that a grid shows in each page
   *
   * @param size Expected page size
   */
  protected void checkGridPageSize(String size) {
    By selector = frontEndInstructions.getGridPageSize();
    waitForSelector(selector);
    BrowserDriver browser = getBrowser();
    Locator pageSize = locator(selector);

    // A native selector shows all its options: the page size is the selected one. A control whose text repeats the
    // page size (a dropdown holds a hidden selector too) exposes it as a value
    String value = browser.attribute(pageSize, TestAttributes.VALUE);
    String shown;
    if (value != null && !value.isEmpty()) {
      shown = value;
    } else if ("select".equalsIgnoreCase(browser.tagName(pageSize))) {
      shown = browser.selectedOptionText(pageSize);
    } else {
      shown = browser.text(pageSize);
    }
    assertWithScreenshot(selector + TEXT_VALUE + shown + "' isn't equal to " + size, shown.equals(size));
  }

  /**
   * Check that the icon that a column of a grid shows is displayed
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   * @param icon     Name of the icon, without the prefix of the icon library (for instance {@code plus})
   */
  protected void checkGridIconVisible(String gridId, String columnId, String icon) {
    checkVisible(locator(frontEndInstructions.getGridIcon(gridId, columnId, icon)));
  }

  /**
   * Check that a column of a grid shows a success icon
   *
   * @param columnId Column identifier
   */
  protected void checkColumnSuccessIcon(String columnId) {
    checkVisible(locator(frontEndInstructions.getColumnSuccessIcon(columnId)));
  }

  /**
   * Check that a row of a tree grid is displayed
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   */
  protected void checkTreeRowVisible(String gridId, String rowId) {
    checkVisible(locator(frontEndInstructions.getTreeRow(gridId, rowId)));
  }

  /**
   * Check that a row of a tree grid is not displayed
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   */
  protected void checkTreeRowNotVisible(String gridId, String rowId) {
    checkNotVisible(locator(frontEndInstructions.getTreeRow(gridId, rowId)));
  }

  /**
   * Check that a row of a tree grid is displayed as deleted
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   */
  protected void checkTreeRowDeleted(String gridId, String rowId) {
    checkVisible(locator(frontEndInstructions.getDeletedTreeRow(gridId, rowId)));
  }

  /**
   * Check that the icon to expand or collapse a row of a tree grid is displayed
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   */
  protected void checkTreeIconVisible(String gridId, String rowId) {
    checkVisible(locator(frontEndInstructions.getTreeRowIcon(gridId, rowId)));
  }

  /**
   * Check that the icon to expand or collapse a row of a tree grid is not displayed
   *
   * @param gridId Tree grid identifier
   * @param rowId  Row identifier
   */
  protected void checkTreeIconNotVisible(String gridId, String rowId) {
    checkNotVisible(locator(frontEndInstructions.getTreeRowIcon(gridId, rowId)));
  }

  /**
   * Close the open context menu without choosing an option
   */
  protected void closeContextMenu() {
    By mask = frontEndInstructions.getContextMenuMask();
    if (mask != null) {
      click(mask);
    } else {
      getBrowser().press(Key.ESCAPE);
    }
  }

  /**
   * Check that no context menu is displayed
   */
  protected void checkContextMenuNotVisible() {
    checkNotVisible(locator(frontEndInstructions.getContextMenu()));
  }

  /**
   * Check that a modal dialog has been closed
   *
   * @param dialogId Dialog identifier
   */
  protected void checkDialogClosed(String dialogId) {
    checkNotVisible(locator(frontEndInstructions.getOpenDialog(dialogId)));
  }

  /**
   * Open the list of suggestions of a criterion
   *
   * @param criterionName Criterion name
   */
  protected void openSuggest(String criterionName) {
    click(frontEndInstructions.getSuggestChoice(frontEndInstructions.getCriterionCss(criterionName)));
  }

  /**
   * Write text in the search box of the open suggest list of a criterion
   *
   * @param criterionName Criterion name
   * @param text          Text to search
   */
  protected void writeSuggestSearch(String criterionName, CharSequence text) {
    writeText(locator(frontEndInstructions.getSuggestInput(frontEndInstructions.getCriterionCss(criterionName))), text);
  }

  /**
   * Check the number of results that the open select or suggest list shows
   *
   * @param expected Expected number of results
   */
  protected void checkSuggestResultCount(int expected) {
    if (expected > 0) {
      checkVisible(locator(frontEndInstructions.getSelectOption(expected)));
    }
    checkNotVisible(locator(frontEndInstructions.getSelectOption(expected + 1)));
  }

  /**
   * Suggest an element after a first search that is replaced by a second one while the first is still loading
   *
   * @param criterionName Criterion name
   * @param search1       First search
   * @param search2       Second search
   * @param match         Label of the result to pick
   * @param pause         Milliseconds to wait between the searches
   */
  protected void suggestReplacingSearch(String criterionName, String search1, String search2, String match, Integer pause) {
    openSuggest(criterionName);
    delayedSearch(frontEndInstructions.getSuggestInput(frontEndInstructions.getCriterionCss(criterionName)),
      search1, search2, match, pause);
  }

  /**
   * Suggest an element of a multiple suggest after a first search that is replaced by a second one while the first is
   * still loading
   *
   * @param criterionName Criterion name
   * @param search1       First search
   * @param search2       Second search
   * @param match         Label of the result to pick
   * @param pause         Milliseconds to wait between the searches
   */
  protected void suggestMultipleReplacingSearch(String criterionName, String search1, String search2, String match, Integer pause) {
    delayedSearch(frontEndInstructions.getSuggestMultipleInput(frontEndInstructions.getCriterionCss(criterionName)),
      search1, search2, match, pause);
  }

  /**
   * Search, wait, search again and pick a result
   *
   * @param searchBox Search box
   * @param search1   First search
   * @param search2   Second search
   * @param match     Label of the result to pick
   * @param pause     Milliseconds to wait between the searches
   */
  private void delayedSearch(By searchBox, String search1, String search2, String match, Integer pause) {
    // Write text
    waitUntil(toBePresent(searchBox));
    scrollToTheCenter(searchBox);
    writeText(locator(searchBox), search1);

    // Pause
    pause(pause);

    // Clear text
    clearText(searchBox);

    // Write select
    writeTextOnDriver(locator(searchBox), search2);

    // Click selector
    selectResult(match);
  }
}
