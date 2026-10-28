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
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.almis.awe.testing.constants.TestingConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Utilities suite for selenium testing
 */
@Slf4j
// SpringExtension must be registered first: it injects the properties that SeleniumExtension needs in setSeleniumModel
@ExtendWith({SpringExtension.class, SeleniumExtension.class})
@ContextConfiguration(classes = TestConfig.class, initializers = ConfigDataApplicationContextInitializer.class)
public class SeleniumUtilities implements IAweInstructions {

  // Constants
  private static final Integer RETRY_COUNT = 10;
  private static final int STALE_RETRY_COUNT = 3;
  // Turn of the mouse wheel that zooms a chart in (negative: up)
  private static final int CHART_ZOOM_WHEEL = -600;
  // How often, and for how long at most, the options of a list are looked at to see that they stopped changing
  private static final Duration OPTIONS_SETTLE_INTERVAL = Duration.ofMillis(200);
  private static final Duration OPTIONS_SETTLE_TIMEOUT = Duration.ofSeconds(3);
  // How often, and for how long at most, the avatar of the logged user is looked at to see that its name is filled
  private static final Duration LOGGED_USER_NAME_INTERVAL = Duration.ofMillis(100);
  private static final Duration LOGGED_USER_NAME_TIMEOUT = Duration.ofSeconds(3);
  private static final Duration MENU_CLICK_EFFECT_TIMEOUT = Duration.ofSeconds(5);
  private static final int MENU_CLICK_RETRIES = 2;
  // Where a dragged row lands on the row it is dropped on, as a share of its height
  private static final double DROP_UPPER_HALF = 0.25;
  private static final double DROP_LOWER_HALF = 0.75;
  private static final int EDIT_ROW_ATTEMPTS = 3;
  private static final Duration EDIT_ROW_WAIT = Duration.ofSeconds(2);
  private static final int CLOSE_COLUMNS_ATTEMPTS = 3;
  private static final String TEXT_VALUE = " text: '";
  // Tells whether the element that has the focus is inside the element that the selector (first argument) finds
  private static final String FOCUS_INSIDE_SCRIPT = "var active = document.activeElement;"
    + " return !!active && !!active.closest(arguments[0]);";
  // Drops a file (name, media type and base64 content are the arguments after the drop zone) the way a browser does
  private static final String DROP_FILE_SCRIPT = "var zone = arguments[0];"
    + " var bytes = Uint8Array.from(atob(arguments[3]), function (c) { return c.charCodeAt(0); });"
    + " var transfer = new DataTransfer();"
    + " transfer.items.add(new File([bytes], arguments[1], {type: arguments[2]}));"
    + " ['dragenter', 'dragover', 'drop'].forEach(function (type) {"
    + " zone.dispatchEvent(new DragEvent(type, {bubbles: true, cancelable: true, dataTransfer: transfer})); });";
  private static final String SPARKLINE_SUMMARY_SCRIPT = "return arguments[0].getAttribute('data-type') + '|' + "
    + "Array.prototype.map.call(arguments[0].querySelectorAll('title'), function (title) { return title.textContent; }).join(',');";
  private static final String SPARKLINE_FOCUS_SCRIPT = "return arguments[0].getAttribute('focusable') + '|' + "
    + "arguments[0].querySelectorAll('[tabindex]').length;";
  private static final int DIALOG_WIDTH_TOLERANCE = 2;
  // Width of the element (first argument)
  private static final String WIDTH_SCRIPT = "return arguments[0].getBoundingClientRect().width;";
  // Distance from the left edge of a criterion where the click that leaves it lands: inside the gutter of its column,
  // which is the criterion itself and not its control
  private static final int CRITERION_EDGE_OFFSET = 2;
  // Where a side panel (first argument) is: its gaps to the left and to the right edges of the viewport, its width and
  // its maximum width (second argument, a css length that the browser resolves) capped by the width of the viewport
  private static final String SIDE_PANEL_PLACEMENT_SCRIPT = "var panel = arguments[0], rect = panel.getBoundingClientRect();"
    + " var viewport = document.documentElement.clientWidth, probe = document.createElement('div');"
    + " probe.style.cssText = 'position:absolute;visibility:hidden;width:' + arguments[1];"
    + " document.body.appendChild(probe); var maximum = Math.min(probe.offsetWidth, viewport); probe.remove();"
    + " return rect.left + '|' + (viewport - rect.right) + '|' + panel.offsetWidth + '|' + maximum;";
  private static final int SIDE_PANEL_TOLERANCE = 2;
  // Edges of a popover (first argument) and of its anchor (second argument, a component id found as the clients find
  // it: by the attribute that carries the id of a criterion, a grid, a tree grid or a window, or by the element id), as
  // top|right|bottom|left of the popover and then of the anchor. Nothing when the anchor is not found
  private static final String POPOVER_PLACEMENT_SCRIPT = "var id = arguments[1], value = CSS.escape(id), anchor = null;"
    + " ['criterion-id', 'grid-id', 'tree-grid-id', 'window-id'].forEach(function (name) {"
    + " anchor = anchor || document.querySelector('[' + name + '=\"' + value + '\"]'); });"
    + " anchor = anchor || document.getElementById(id); if (!anchor) { return null; }"
    + " return [arguments[0].getBoundingClientRect(), anchor.getBoundingClientRect()].map(function (rect) {"
    + " return rect.top + '|' + rect.right + '|' + rect.bottom + '|' + rect.left; }).join('|');";
  private static final List<String> POPOVER_POSITIONS = List.of("top", "right", "bottom", "left");
  // A popover opens on its side of the anchor, at most this gap away from it, and alongside it
  private static final int POPOVER_MAX_GAP = 16;
  private static final int POPOVER_TOLERANCE = 2;
  private static final int GRID_TOP_TOLERANCE = 2;
  // Box of an element as top|bottom|width
  private static final String BOX_SCRIPT = "var rect = arguments[0].getBoundingClientRect();"
    + " return rect.top + '|' + rect.bottom + '|' + rect.width;";
  // The detail of a row starts at most this gap under the row, and is at least this part of the width of the row
  private static final int ROW_DETAIL_MAX_GAP = 20;
  private static final double ROW_DETAIL_MIN_WIDTH = 0.9;
  // Edges of an element as top|bottom|left|right
  private static final String EDGES_SCRIPT = "var rect = arguments[0].getBoundingClientRect();"
    + " return rect.top + '|' + rect.bottom + '|' + rect.left + '|' + rect.right;";
  // Bottom of the last element located by the css selector of the first argument, nothing when there is none
  private static final String LAST_BOTTOM_SCRIPT = "var all = document.querySelectorAll(arguments[0]);"
    + " return all.length ? all[all.length - 1].getBoundingClientRect().bottom : null;";
  // Width of the content of an element, without its scroll bar
  private static final String CLIENT_WIDTH_SCRIPT = "return arguments[0].clientWidth;";
  // Pixels that a nested grid may go past the edges of the detail of its row (rounding of the boxes)
  private static final int ROW_DETAIL_TOLERANCE = 1;
  // Number of toggles of row details (located by the css selector of the first argument) that say that their detail is open
  private static final String OPEN_ROW_DETAIL_TOGGLES_SCRIPT = "return Array.prototype.filter.call(document.querySelectorAll(arguments[0]),"
    + " function (toggle) { return toggle.getAttribute('aria-expanded') === 'true'; }).length;";
  // The page records every download it fetches when it starts, and its answer when it arrives: the name of the file, or
  // "error" when the file does not arrive
  private static final String RECORD_DOWNLOADS_SCRIPT = "if (!window.aweDownloads) {"
    + " window.aweDownloads = {started: 0, files: []}; const pageFetch = window.fetch;"
    + " window.fetch = function (url) { const answer = pageFetch.apply(this, arguments);"
    + " if (String(url).indexOf('/file/download/') >= 0) { const downloads = window.aweDownloads; downloads.started++;"
    + " answer.then(function (response) { downloads.files.push(response.ok ? response.headers.get('Filename') : 'error'); },"
    + " function () { downloads.files.push('error'); }); }"
    + " return answer; }; }"
    + " window.aweDownloads.started = 0; window.aweDownloads.files = [];";
  // Number of files downloaded once every download started has arrived, or -1 while one is on its way or did not arrive
  private static final String DOWNLOADED_FILES_SCRIPT = "const downloads = window.aweDownloads || {started: 0, files: []};"
    + " const arrived = downloads.files.length === downloads.started && downloads.files.indexOf('error') < 0;"
    + " return String(arrived ? downloads.files.length : -1);";
  private static final int GROUP_HEADER_TOLERANCE = 3;
  private static final double MAXIMIZED_MIN_FILL = 0.9;
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
   * Wait until the options of the open list stop changing: two looks in a row (about 200 milliseconds apart) give the same
   * non-empty options. A list that is filtered while the text is typed shows some options first and others after it, and an
   * option that was found among the first is not where it was when the pointer arrives. It is a help, not a requirement:
   * if the options keep changing, or there are none, the step goes on and the wait for the option it needs says what is
   * wrong
   *
   * @param options Selector of the options
   */
  private void waitForTheOptionsToSettle(By options) {
    Locator locator = locator(options);
    AtomicReference<List<String>> previous = new AtomicReference<>();
    BrowserCondition settled = BrowserCondition.of("options located by " + locator + " to stop changing", browser -> {
      try {
        List<String> current = browser.texts(locator);
        return !current.isEmpty() && current.equals(previous.getAndSet(current));
      } catch (ElementReplacedException exc) {
        previous.set(null);
        return false;
      }
    });
    Duration timeout = properties.getTimeout().compareTo(OPTIONS_SETTLE_TIMEOUT) < 0 ? properties.getTimeout() : OPTIONS_SETTLE_TIMEOUT;
    try {
      BrowserPoll.until(getBrowser(), settled, timeout, OPTIONS_SETTLE_INTERVAL, Clock.systemUTC(),
        duration -> Thread.sleep(duration.toMillis()));
    } catch (BrowserPoll.PollTimeoutException exc) {
      log.debug("The options did not settle: {}", exc.getMessage());
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
      Path path = Path.of(properties.getScreenshotPath(), screenshotName + ".png");
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
    // A menu opened before may still be closing (its mask, its fade out): while it does, a menu that overflows the
    // viewport keeps the page scrolled and the layout moves under the pointer when it is gone
    // (every match counts, not only the first one; React has no mask nor ng-animate, so nothing matches and it passes)
    waitUntil(BrowserCondition.noneVisible(locator(By.cssSelector(".component-mask"))));
    waitUntil(BrowserCondition.noneVisible(locator(By.cssSelector("[data-testid='context-menu'].ng-animate"))));

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
   * Take the focus out of a criterion with a click on its left edge, out of its control. A click above it would land on
   * whatever the page shows there when the criterion has no label row (the step bar of a wizard, a tab, a button)
   *
   * @param criterionCss Criterion selector
   */
  private void moveMouseOutOfCriterion(String criterionCss) {
    try {
      Locator criterion = Locator.css(criterionCss);
      double width = ((Number) getBrowser().executeScriptOn(criterion, WIDTH_SCRIPT)).doubleValue();
      getBrowser().hover(criterion);
      getBrowser().moveMouseBy(CRITERION_EDGE_OFFSET - (int) (width / 2), 0);
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

    // Let the filtered list settle: the option is chosen where it is when the pointer arrives
    waitForTheOptionsToSettle(frontEndInstructions.getSelectOptions());

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
   * Give the page time to show the text that a check expects. A screen that is still appearing, or a value that an
   * asynchronous action is still loading, is not a failure yet: the check that follows asserts what is shown, as it always
   * did, and reports it (with evidence) if it is not the expected text
   *
   * @param condition Text that is expected
   */
  private void waitForExpectedText(BrowserCondition condition) {
    try {
      BrowserPoll.until(getBrowser(), condition, properties.getTimeout());
    } catch (BrowserPoll.PollTimeoutException exc) {
      log.debug("Not yet: {}", condition);
    }
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
    waitForExpectedText(BrowserCondition.textEquals(selector, text));
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
    waitForExpectedText(BrowserCondition.textContains(selector, text));
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
    waitForExpectedText(BrowserCondition.anyTextContains(locator(selector), text));
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

    // The previous step may still have server tasks pending (a restore that loads its suggests): the client drops the
    // actions that are waiting when an option is chosen, so the screen would not change
    waitForLoadingBar();

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

    // Wait for the click to take effect, clicking again when the browser ignored it
    String lastOption = menuOptions[menuOptions.length - 1];
    retryMenuClickWhenIgnored(lastOption);
    waitForMenuOption(lastOption);

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
    waitUntil(menuOptionTookEffect(option));
  }

  /**
   * Condition: the click on a menu option has taken effect (the screen of the option is the current one or, in a client
   * that collapses its menu, the dropdown is closed)
   */
  private BrowserCondition menuOptionTookEffect(String option) {
    By activeOption = frontEndInstructions.getMenuActiveOption(option);
    return activeOption != null ? toBeVisible(activeOption) : toBeInvisible(frontEndInstructions.getMenuDropdown());
  }

  /**
   * A browser may ignore a click on a menu option that it reports as done (the option gets the focus but the screen does
   * not change). Give the click a short time to take effect and, if it did not, click the option again (at most
   * {@value #MENU_CLICK_RETRIES} more times). A click that took effect is never repeated, and a click that never takes
   * effect is reported by the wait that follows, as always
   *
   * @param option Last option clicked
   */
  private void retryMenuClickWhenIgnored(String option) {
    Duration timeout = properties.getTimeout().compareTo(MENU_CLICK_EFFECT_TIMEOUT) < 0 ? properties.getTimeout() : MENU_CLICK_EFFECT_TIMEOUT;
    BrowserCondition tookEffect = menuOptionTookEffect(option);
    for (int retry = 0; retry <= MENU_CLICK_RETRIES; retry++) {
      try {
        BrowserPoll.until(getBrowser(), tookEffect, timeout);
        return;
      } catch (BrowserPoll.PollTimeoutException exc) {
        if (retry == MENU_CLICK_RETRIES) {
          return;
        }
        log.warn("The click on the menu option '{}' had no effect after {} ms, clicking it again", option, timeout.toMillis());
        click(frontEndInstructions.getMenuOption(option));
      }
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
   * Open the user menu unless it is already open (its theme selector is shown), so a menu that is already open is not
   * closed
   */
  private void openUserMenuIfClosed() {
    if (!isVisibleNow(locator(By.cssSelector(frontEndInstructions.getCriterionCss("theme"))))) {
      openUserMenu();
    }
  }

  /**
   * Open or close the user menu (the avatar of the logged user), where the theme, the color mode and the language are
   */
  protected void openUserMenu() {
    clickSelector(frontEndInstructions.getLoggedUser());
  }

  /**
   * Click an option of the user menu, which has to be open
   *
   * @param optionId Option identifier (dark-mode, light-mode)
   */
  protected void clickUserMenuOption(String optionId) {
    clickSelector(frontEndInstructions.getUserMenuOption(optionId));
  }

  /**
   * Choose the dark mode in the user menu, as a user does
   */
  protected void switchToDarkMode() {
    openUserMenuIfClosed();
    clickUserMenuOption("dark-mode");
  }

  /**
   * Choose the light mode in the user menu, as a user does
   */
  protected void switchToLightMode() {
    openUserMenuIfClosed();
    clickUserMenuOption("light-mode");
  }

  /**
   * Choose the light mode in the user menu when the menu offers it (the user is in the dark mode) and wait until the page
   * is light, so a test starts in the light mode whatever mode the session starts in. Otherwise the mode is kept
   */
  protected void switchToLightModeIfDark() {
    openUserMenuIfClosed();
    if (isVisibleNow(locator(frontEndInstructions.getUserMenuOption("light-mode")))) {
      clickUserMenuOption("light-mode");
      checkColorMode("light");
    }
  }

  /**
   * Choose a theme in the user menu, as a user does
   *
   * @param themeName Theme name
   */
  protected void selectTheme(String themeName) {
    openUserMenuIfClosed();
    selectContain("theme", themeName);
    waitForLoadingBar();
  }

  /**
   * Check that a theme is applied to the page
   *
   * @param themeName Theme name
   */
  protected void checkThemeApplied(String themeName) {
    waitUntil(BrowserCondition.present(locator(frontEndInstructions.getThemedPage(themeName))));
  }

  /**
   * Check that the user chose the light mode or the dark mode (the page carries the mode that the user chose, which wins over the operating system)
   *
   * @param mode Color mode: dark or light
   */
  protected void checkColorMode(String mode) {
    if (!"dark".equals(mode) && !"light".equals(mode)) {
      throw new IllegalArgumentException("The color mode is dark or light, not " + mode);
    }
    waitUntil(BrowserCondition.present(locator(frontEndInstructions.getColorModePage(mode))));
  }

  /**
   * Check the theme appearance on the page root, the same in both engines (#885): the {@code <html>} element carries the
   * class of the mode (and not the other one) and the primary palette of the theme in {@code data-awe-primary}
   *
   * @param mode    Color mode: dark or light
   * @param primary Primary palette (red, blue, sky...)
   */
  protected void checkThemeAppearance(String mode, String primary) {
    if (!"dark".equals(mode) && !"light".equals(mode)) {
      throw new IllegalArgumentException("The color mode is dark or light, not " + mode);
    }
    String otherMode = "dark".equals(mode) ? "light" : "dark";
    waitUntil(BrowserCondition.present(locator(By.cssSelector(
      String.format("html.%s:not(.%s)[data-awe-primary='%s']", mode, otherMode, primary)))));
  }

  /**
   * Check the computed value of a CSS property of the first element that matches a selector (rgb(...) for the colors).
   * The case and the spaces around the value are ignored
   *
   * @param cssSelector CSS selector of the element
   * @param property    CSS property (background-color)
   * @param value       Expected computed value (rgb(248, 250, 252))
   */
  protected void checkComputedStyle(String cssSelector, String property, String value) {
    waitUntil(BrowserCondition.of("computed " + property + " of " + cssSelector + " to be " + value, browser -> {
      Object computed = browser.executeScript(
        "const element = document.querySelector(arguments[0]);"
          + "return element ? getComputedStyle(element).getPropertyValue(arguments[1]).trim() : null;", cssSelector, property);
      return computed != null && value.trim().equalsIgnoreCase(String.valueOf(computed).trim());
    }));
  }

  /**
   * Check the value a design token (a --awe-* CSS custom property) has on the page, which is the value the theme and
   * the mode that are applied give it. The case and the spaces around the value are ignored
   *
   * @param token Design token name, as in the design tokens guide (--awe-color-primary)
   * @param value Expected value (#1d89cf)
   */
  protected void checkDesignToken(String token, String value) {
    if (token == null || !token.startsWith("--awe-")) {
      throw new IllegalArgumentException("A design token starts with --awe-, not " + token);
    }
    waitUntil(BrowserCondition.of("design token " + token + " to be " + value, browser ->
      value.trim().equalsIgnoreCase(String.valueOf(browser.executeScript(
        "return getComputedStyle(document.body).getPropertyValue(arguments[0]).trim();", token)).trim())));
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
   * Click the button of a grid cell
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void clickGridCellButton(String gridId, String rowId, String columnId) {
    By button = frontEndInstructions.getGridCellButton(gridId, rowId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(button), checkIfGridLoaderIsNotVisible()));
    clickSelector(button);
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
   * Start recording the files that the page downloads (the downloads it fetches from the file service), forgetting the
   * ones recorded before
   */
  protected void startRecordingDownloads() {
    getBrowser().executeScript(RECORD_DOWNLOADS_SCRIPT);
  }

  /**
   * Check the number of files downloaded since the recording started: every download started has arrived
   *
   * @param count Number of files downloaded
   */
  protected void checkFilesDownloaded(int count) {
    waitUntil(BrowserCondition.of(count + " files downloaded", browser ->
      String.valueOf(count).equals(String.valueOf(browser.executeScript(DOWNLOADED_FILES_SCRIPT)))));
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
   * Right click on a button to open its context menu
   *
   * @param buttonId Button id
   */
  protected void contextMenuOnButton(String buttonId) {
    waitForButton(buttonId);
    contextMenu(frontEndInstructions.getButton(buttonId));
  }

  /**
   * Right click on a criterion to open its context menu
   *
   * @param criterionId Criterion id
   */
  protected void contextMenuOnCriterion(String criterionId) {
    contextMenu(By.cssSelector(frontEndInstructions.getCriterionCss(criterionId)));
  }

  /**
   * Right click on the tab list of a tab criterion to open its context menu
   *
   * @param tabId Tab criterion id
   */
  protected void contextMenuOnTab(String tabId) {
    waitForTab(tabId);
    contextMenu(frontEndInstructions.getTab(tabId));
  }

  /**
   * Right click on the active step of a wizard to open its context menu
   *
   * @param wizardId Wizard criterion id
   */
  protected void contextMenuOnWizard(String wizardId) {
    contextMenu(frontEndInstructions.getActiveWizardStepOf(wizardId));
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
    moveMouseOutOfCriterion(frontEndInstructions.getCriterionCss(criterionName));
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
   * Check a message box and close it.
   *
   * <p>The message has to be shown: if it never is, the step fails. Once it has been seen, it is allowed to leave on
   * its own before or while it is clicked. The messages close themselves after a few seconds by design (the
   * {@code messageTimeout} of each type, 2 seconds for the success ones), so on a slow runner a message can be gone by
   * the time the test reaches it, and the click would wait for an element that does not exist any more until the
   * timeout. A message that is gone is the normal end of a self-closing message, as a closed one is, so the step waits
   * until the message is closed by the click or has left by itself, and then for the loading bar.</p>
   *
   * @param messageType Message type (success (default), info, warning, danger)
   */
  protected void checkAndCloseMessage(String messageType) {
    By messageSelector = frontEndInstructions.getMessage(messageType);
    Locator message = locator(messageSelector);

    // Wait for message selector: it has to be shown at least once
    waitUntil(toBeClickable(messageSelector));

    // Click on it, unless it left by itself or does while it is clicked (it is not waited for again)
    waitUntil(BrowserCondition.of("message " + message + " to be closed or to close itself", browser -> {
      if (!browser.exists(message)) {
        return true;
      }
      try {
        browser.click(message);
        return true;
      } catch (ElementReplacedException | ElementNotFoundException exc) {
        log.debug("The message left while it was clicked, checking again: {}", messageSelector);
        return false;
      }
    }));

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
   * Tell whether a grid shows a text, once it has loaded, without failing when it does not. A step that creates or deletes a
   * record uses it to find out whether the record is already there, which is the state that a failed attempt of the same
   * step leaves when the step is run again
   *
   * @param search Text to search for in the grid
   * @return The grid shows the text
   */
  protected boolean hasRowContents(String search) {
    return hasRowContentsGrid(null, search);
  }

  /**
   * Tell whether a grid shows a text, once it has loaded, without failing when it does not
   *
   * @param gridId Grid Identifier
   * @param search Text to search for in the grid
   * @return The grid shows the text
   */
  protected boolean hasRowContentsGrid(String gridId, String search) {
    // The rows of a search that is still loading are not there yet
    waitUntil(checkIfGridLoaderIsNotVisible());
    return hasVisibleElement(getBrowser(), frontEndInstructions.findGridCell(gridId, search));
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
   * Make sure that the user is logged in: it does nothing when the application already shows that user, logs in when
   * nobody is logged in, and logs the other user out first when another user is. It lets a test class start from
   * whatever state the browser has (a new browser, or the page that an earlier test left), so a test does not depend on a
   * login done by another one. A test that checks the login itself uses {@link #checkLogin(String, String, String)}.
   *
   * @param username Login of the user
   * @param password Password
   * @param userName Name that the application shows for the logged user (the whole name, not a part of it)
   */
  protected void ensureLoggedIn(String username, String password, String userName) {
    String shown = shownLoggedUser();
    if (userName.equals(shown)) {
      return;
    }
    if (shown != null) {
      logOut();
    }
    checkLogin(username, password, userName);
  }

  /**
   * Make sure that nobody is logged in: it does nothing when no user is shown (the login screen, or a page that is not the
   * application), and logs out when one is, with the logout of the application (it accepts the confirmation when the
   * application asks for it). It lets a test class that checks the login start from the login screen whatever the browser
   * showed before. A test that checks the logout itself uses {@link #checkLogout()}.
   */
  protected void ensureLoggedOut() {
    if (shownLoggedUser() != null) {
      logOut();
    }
  }

  /**
   * Log out with the logout that the application uses
   */
  private void logOut() {
    if (frontEndInstructions.logoutNeedsConfirmation()) {
      checkLogoutWithConfirmation();
    } else {
      checkLogout();
    }
  }

  /**
   * Read the name that the application shows for the logged user. It does not wait for the user to appear, but it waits a
   * little for a name that is not filled yet (the avatar is drawn before its name)
   *
   * @return Name of the logged user, or null when none is shown or none could be read
   */
  private String shownLoggedUser() {
    Locator loggedUser = locator(frontEndInstructions.getLoggedUser());
    try {
      if (!getBrowser().isVisible(loggedUser)) {
        return null;
      }
      String shown = getBrowser().text(loggedUser).trim();
      return shown.isEmpty() ? waitForLoggedUserName(loggedUser) : shown;
    } catch (ElementNotFoundException | ElementReplacedException exc) {
      // The page changed while it was read: there is no logged user to rely on
      return null;
    }
  }

  /**
   * Wait, for a short time, until the avatar of the logged user shows a name
   *
   * @param loggedUser Locator of the logged user
   * @return The name, or null when it did not arrive: the user is not recognised, neither as the expected one nor as another
   */
  private String waitForLoggedUserName(Locator loggedUser) {
    AtomicReference<String> shown = new AtomicReference<>();
    BrowserCondition named = BrowserCondition.of("the name of the logged user to be shown", browser -> {
      try {
        shown.set(browser.isVisible(loggedUser) ? browser.text(loggedUser).trim() : "");
      } catch (ElementNotFoundException | ElementReplacedException exc) {
        shown.set("");
      }
      return !shown.get().isEmpty();
    });
    Duration timeout = properties.getTimeout().compareTo(LOGGED_USER_NAME_TIMEOUT) < 0 ? properties.getTimeout() : LOGGED_USER_NAME_TIMEOUT;
    try {
      BrowserPoll.until(getBrowser(), named, timeout, LOGGED_USER_NAME_INTERVAL, Clock.systemUTC(),
        duration -> Thread.sleep(duration.toMillis()));
      return shown.get();
    } catch (BrowserPoll.PollTimeoutException exc) {
      log.debug("The logged user showed no name: {}", exc.getMessage());
      return null;
    }
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
   * Ask to log out, close the confirmation with the Escape key and check that the application stays logged in
   */
  protected void checkLogoutCancelledWithEscape() {
    By confirmAccept = By.id("confirm-accept");

    // Test title
    setTestTitle("Logout cancelled: close the confirmation with the Escape key");

    // Ask to log out
    clickLogoutButton();
    waitUntil(toBeVisible(confirmAccept));

    // Pause 300 ms
    pause(300);

    // Close the confirmation with the Escape key
    getBrowser().press(Key.ESCAPE);
    waitUntil(toBeInvisible(confirmAccept));

    // The logout did not run: the login screen is not shown
    waitForLoadingBar();
    waitUntil(toBeInvisible(frontEndInstructions.getLoginScreenMarker()));

    // Close the user menu that holds the logout button, so the next step finds it closed
    if (frontEndInstructions.getUserMenuButtonId() != null) {
      getBrowser().press(Key.ESCAPE);
      waitUntil(toBeInvisible(frontEndInstructions.getButton("ButLogOut")));
    }
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

    // Click on logout
    clickLogoutButton();

    // Accept the confirmation
    if (confirm) {
      acceptConfirm();
    }

    // Wait for the login screen
    waitForSelector(marker);
    checkText(marker, frontEndInstructions.getLoginScreenText());
  }

  /**
   * Click on the logout button, opening the user menu first when the button is inside it
   */
  private void clickLogoutButton() {
    // Wait for element not visible
    waitForLoadingBar();

    // Open the user menu when the logout button is inside it
    Optional.ofNullable(frontEndInstructions.getUserMenuButtonId()).ifPresent(this::clickInfoButton);

    // Click on logout
    clickButton("ButLogOut", true);
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
   * Make sure that a module is selected: it does nothing when the menu of the module is already shown, and selects the
   * module otherwise. It lets a test class choose the module it needs without depending on another test having done it.
   * A test that checks the module selection itself uses {@link #selectModule(String)}.
   *
   * @param moduleName Module name
   * @param menuOption Menu option that only the module shows
   */
  protected void ensureModule(String moduleName, String menuOption) {
    Locator option = locator(frontEndInstructions.getMenuOptionItem(menuOption));
    if (isVisibleNow(option)) {
      return;
    }
    selectModule(moduleName);
    checkVisible(option);
  }

  /**
   * Check, without waiting, that an element is visible; an element that changes or goes away while it is read is not
   *
   * @param selector Locator of the element
   * @return The element is visible
   */
  private boolean isVisibleNow(Locator selector) {
    try {
      return getBrowser().isVisible(selector);
    } catch (ElementNotFoundException | ElementReplacedException exc) {
      return false;
    }
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
   * Check the popover of a target message shown over a component
   *
   * @param component Id of the component the message points at
   * @param title     Expected title
   * @param text      Expected text
   */
  protected void checkMessagePopover(String component, String title, String text) {
    waitForSelector(frontEndInstructions.getMessagePopover(component));
    checkText(frontEndInstructions.getMessagePopoverTitle(component), title);
    checkText(frontEndInstructions.getMessagePopoverText(component), text);
  }

  /**
   * Wait for the popover of a target message to hide (after the timeout of its type, or when it is dismissed)
   *
   * @param component Id of the component the message points at
   */
  protected void waitForMessagePopoverToHide(String component) {
    waitUntil(toBeInvisible(frontEndInstructions.getMessagePopover(component)));
  }

  /**
   * Dismiss the popover of a target message before its timeout, as a user does, and wait for it to hide
   *
   * @param component Id of the component the message points at
   */
  protected void dismissMessagePopover(String component) {
    By mask = frontEndInstructions.getMessagePopoverMask();
    if (mask != null) {
      click(mask);
    } else {
      getBrowser().press(Key.ESCAPE);
    }
    waitForMessagePopoverToHide(component);
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
   * Check the description (secondary line) that a card of a launcher page shows
   *
   * @param option Option name of the card
   * @param text   Expected description
   */
  protected void checkLauncherCardDescription(String option, String text) {
    By selector = frontEndInstructions.getLauncherCardDescription(option);
    waitForSelector(selector);
    checkText(selector, text);
  }

  /**
   * Check that a card of a launcher page has no description
   *
   * @param option Option name of the card
   */
  protected void checkLauncherCardWithoutDescription(String option) {
    checkNotVisible(locator(frontEndInstructions.getLauncherCardDescription(option)));
  }

  /**
   * Check that the breadcrumb of the page is visible and contains a text
   *
   * @param text Text that the breadcrumb contains
   */
  protected void checkBreadcrumbContains(String text) {
    By selector = frontEndInstructions.getBreadcrumb();
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
   * Check that a pivot table is drawn and displayed
   *
   * @param pivotTableId Pivot table identifier
   */
  protected void checkPivotTableVisible(String pivotTableId) {
    checkVisible(locator(frontEndInstructions.getPivotTable(pivotTableId)));
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
   * Check that a map is displayed with its markers drawn (React client only)
   *
   * @param mapId Map identifier
   */
  protected void checkMapVisible(String mapId) {
    checkVisible(locator(frontEndInstructions.getMap(mapId)));
  }

  /**
   * Check how many markers a map shows (React client only)
   *
   * @param mapId Map identifier
   * @param count Expected number of markers
   */
  protected void checkMapMarkerCount(String mapId, int count) {
    Locator markers = locator(frontEndInstructions.getMapMarkers(mapId));
    waitUntil(BrowserCondition.of("map " + mapId + " to show " + count + " markers",
      browser -> browser.count(markers) == count));
  }

  /**
   * Click on the marker of a row of a map (React client only)
   *
   * @param mapId Map identifier
   * @param rowId Row identifier (the value of the marker)
   */
  protected void clickMapMarker(String mapId, String rowId) {
    click(frontEndInstructions.getMapMarker(mapId, rowId));
  }

  /**
   * Check that the marker of a row of a map is selected (React client only)
   *
   * @param mapId Map identifier
   * @param rowId Row identifier (the value of the marker)
   */
  protected void checkMapMarkerSelected(String mapId, String rowId) {
    checkVisible(locator(frontEndInstructions.getMapMarkerSelected(mapId, rowId)));
  }

  /**
   * Check that a map draws a route (React client only)
   *
   * @param mapId Map identifier
   */
  protected void checkMapRouteVisible(String mapId) {
    checkVisible(locator(frontEndInstructions.getMapRoute(mapId)));
  }

  /**
   * Zoom a chart in with the mouse wheel over its center
   *
   * @param chartId Chart identifier
   */
  protected void zoomChart(String chartId) {
    By selector = frontEndInstructions.getChart(chartId);
    checkVisible(locator(selector));
    try {
      getBrowser().wheel(locator(selector), CHART_ZOOM_WHEEL);
    } catch (Exception exc) {
      assertWithScreenshot("Zooming the chart: " + chartId + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Check that a chart is zoomed (it shows a part of its range)
   *
   * @param chartId Chart identifier
   */
  protected void checkChartZoomed(String chartId) {
    checkVisible(locator(frontEndInstructions.getChartZoomed(chartId)));
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
   * Wait until the file viewer shows a text
   *
   * @param text Text that the file contains
   */
  protected void checkFileViewerContains(String text) {
    By selector = frontEndInstructions.getFileViewer();
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
   * Check that a grid is not in the page at all, neither shown nor hidden (for instance the nested grid of a row detail
   * once the row closes or its grid reloads)
   *
   * @param gridId Grid identifier
   */
  protected void checkGridNotPresent(String gridId) {
    waitUntil(BrowserCondition.not(toBePresent(frontEndInstructions.getGrid(gridId))));
  }

  /**
   * Check that the cells of a grid show text and nothing active: no script, frame, image, form or event handler, and no
   * link to a script url. The grid has to show at least one cell
   *
   * @param gridId Grid identifier
   */
  protected void checkGridCellsHaveNoActiveContent(String gridId) {
    waitUntil(BrowserCondition.allOf(toBePresent(frontEndInstructions.getGridCells(gridId)), checkIfGridLoaderIsNotVisible()));
    By selector = frontEndInstructions.getGridCellsActiveContent(gridId);
    assertWithScreenshot("The cells of the grid " + gridId + " have active content (" + selector + ")",
      !toBePresent(selector).isMet(getBrowser()));
  }

  /**
   * Check that the rows of a grid are in a given order, reading the values of one of its columns
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   * @param expected Values of the column, in the order the rows are expected to be shown
   */
  protected void checkGridColumnOrder(String gridId, String columnId, String... expected) {
    Locator cells = locator(frontEndInstructions.getGridColumnCells(gridId, columnId));
    List<String> expectedValues = List.of(expected);
    BrowserCondition inOrder = BrowserCondition.of("the column " + columnId + " of the grid " + gridId + " to show " + expectedValues,
      browser -> {
        try {
          return browser.texts(cells).stream().map(String::trim).toList().equals(expectedValues);
        } catch (ElementReplacedException exc) {
          return false;
        }
      });
    waitUntil(BrowserCondition.allOf(inOrder, checkIfGridLoaderIsNotVisible()));
  }

  /**
   * Check that a row of a grid with move-rows shows the grip that drags it
   *
   * @param gridId    Grid identifier
   * @param rowNumber Position of the row, starting at 1
   */
  protected void checkGridRowGripShown(String gridId, int rowNumber) {
    waitUntil(BrowserCondition.visible(locator(frontEndInstructions.getGridRowGrip(gridId, rowNumber))));
  }

  /**
   * Drag a row of a grid with move-rows by its grip and drop it on the upper half of another row, so it lands just above it
   *
   * @param gridId          Grid identifier
   * @param rowNumber       Position of the row that is dragged, starting at 1
   * @param targetRowNumber Position of the row it is dropped on, starting at 1
   */
  protected void dragGridRowBefore(String gridId, int rowNumber, int targetRowNumber) {
    dragGridRow(gridId, rowNumber, targetRowNumber, DROP_UPPER_HALF);
  }

  /**
   * Drag a row of a grid with move-rows by its grip and drop it on the lower half of another row, so it lands just below it
   *
   * @param gridId          Grid identifier
   * @param rowNumber       Position of the row that is dragged, starting at 1
   * @param targetRowNumber Position of the row it is dropped on, starting at 1
   */
  protected void dragGridRowAfter(String gridId, int rowNumber, int targetRowNumber) {
    dragGridRow(gridId, rowNumber, targetRowNumber, DROP_LOWER_HALF);
  }

  private void dragGridRow(String gridId, int rowNumber, int targetRowNumber, double targetYRatio) {
    Locator grip = locator(frontEndInstructions.getGridRowGrip(gridId, rowNumber));
    Locator target = locator(frontEndInstructions.getGridRowFirstCell(gridId, targetRowNumber));
    waitUntil(BrowserCondition.visible(grip));
    getBrowser().dragAndDrop(grip, target, targetYRatio);
    waitUntil(checkIfGridLoaderIsNotVisible());
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
   * Check the text of the paginator of a grid that tells the range of rows shown and the total (for instance
   * {@code View 1 - 10 of 25})
   *
   * @param text Expected text
   */
  protected void checkGridPaginationInfo(String text) {
    checkText(frontEndInstructions.getGridPaginationInfo(), text);
  }

  /**
   * Go to the next page of a grid with its paginator and wait for the grid to load. The AngularJS client has a hook for
   * it in its compact pager only
   */
  protected void clickGridNextPage() {
    click(frontEndInstructions.getGridPageNext());
    waitForLoadingGrid();
  }

  /**
   * Go to a page of a grid typing it on the go to page input of its paginator, which shows the current page, and
   * pressing Enter, and wait for the grid to load. In the AngularJS client the input is in the wide pager only, when the
   * grid has more than one page
   *
   * @param page Page to go to (the first one is 1)
   */
  protected void goToGridPage(int page) {
    Locator input = locator(frontEndInstructions.getGridGotoPage());
    waitUntil(BrowserCondition.present(input));
    getBrowser().clear(input);
    writeText(input, String.valueOf(page));
    getBrowser().press(input, Key.ENTER);
    waitForLoadingGrid();
  }

  /**
   * Check that an option of the radio of a cell of a grid (a radio or a button-radio column) is the chosen one
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param optionId Value of the option
   */
  protected void checkGridRadioChoice(String gridId, String rowId, String columnId, String optionId) {
    waitUntil(toBePresent(frontEndInstructions.getGridRadioChecked(gridId, rowId, columnId, optionId, true)));
  }

  /**
   * Check that no option of the radio of a cell of a grid is chosen
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void checkGridRadioNoChoice(String gridId, String rowId, String columnId) {
    waitUntil(BrowserCondition.allOf(
      toBePresent(frontEndInstructions.getGridRadioInput(gridId, rowId, columnId)),
      BrowserCondition.not(toBePresent(frontEndInstructions.getGridRadioChecked(gridId, rowId, columnId, null, true)))));
  }

  /**
   * Choose an option of the radio of a cell of a grid (a radio or a button-radio column) with a click
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param optionId Value of the option
   */
  protected void selectGridRadioOption(String gridId, String rowId, String columnId, String optionId) {
    By selector = frontEndInstructions.getGridRadioOption(gridId, rowId, columnId, optionId);
    waitUntil(toBePresent(selector));
    click(selector);
  }

  /**
   * Move the choice of a grid cell to the next option with the right arrow key, from the chosen option of the cell
   * (or from its first one when there is none). It works the same in a radio column and in a button-radio column
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void moveGridRadioChoiceNext(String gridId, String rowId, String columnId) {
    pressKeyOnGridRadio(gridId, rowId, columnId, Key.ARROW_RIGHT);
  }

  /**
   * Move the choice of a grid cell to the previous option with the left arrow key, from the chosen option of the cell
   * (or from its first one when there is none). It works the same in a radio column and in a button-radio column
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void moveGridRadioChoicePrevious(String gridId, String rowId, String columnId) {
    pressKeyOnGridRadio(gridId, rowId, columnId, Key.ARROW_LEFT);
  }

  /**
   * Press a key on the radio of a cell of a grid: the chosen option (or the first one) gets the focus, as when the user
   * tabs to it, and the key goes to it. A click would not do, because the input of a radio is hidden behind its own look
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param key      Key to press
   */
  private void pressKeyOnGridRadio(String gridId, String rowId, String columnId, Key key) {
    By chosen = frontEndInstructions.getGridRadioChecked(gridId, rowId, columnId, null, true);
    By selector = getBrowser().count(locator(chosen)) > 0 ? chosen : frontEndInstructions.getGridRadioInput(gridId, rowId, columnId);
    waitUntil(toBePresent(selector));
    getBrowser().executeScriptOn(locator(selector), "arguments[0].focus();");
    getBrowser().press(key);
  }

  /**
   * Check that a grid without rows shows its empty state with a title and a text
   *
   * @param title Text that the title contains
   * @param text  Text that the text contains
   */
  protected void checkGridEmptyState(String title, String text) {
    checkVisible(locator(frontEndInstructions.getGridEmpty()));
    checkTextContains(frontEndInstructions.getGridEmptyTitle(), title);
    checkTextContains(frontEndInstructions.getGridEmptyText(), text);
  }

  /**
   * Check that no grid shows its empty state
   */
  protected void checkGridEmptyStateNotVisible() {
    checkNotVisible(locator(frontEndInstructions.getGridEmpty()));
  }

  /**
   * Click the button that the empty state of a grid offers
   */
  protected void clickGridEmptyAction() {
    click(frontEndInstructions.getGridEmptyAction());
  }

  /**
   * Click the toggle of the detail of a row of a grid, which opens the detail when it is closed and closes it when it is
   * open
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void clickGridRowDetailToggle(String gridId, String rowId) {
    click(frontEndInstructions.getGridRowDetailToggle(gridId, rowId));
  }

  /**
   * Press Enter on the toggle of the detail of a row of a grid. The toggle gets the focus as when the user tabs to it: a
   * click would toggle the detail too
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void pressEnterOnGridRowDetailToggle(String gridId, String rowId) {
    focusGridRowDetailToggle(gridId, rowId);
    getBrowser().press(Key.ENTER);
  }

  /**
   * Press Space on the toggle of the detail of a row of a grid. The toggle gets the focus as when the user tabs to it: a
   * click would toggle the detail too
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void pressSpaceOnGridRowDetailToggle(String gridId, String rowId) {
    getBrowser().sendKeys(focusGridRowDetailToggle(gridId, rowId), " ");
  }

  /**
   * Give the focus to the toggle of the detail of a row of a grid, without clicking it
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   * @return Toggle locator
   */
  private Locator focusGridRowDetailToggle(String gridId, String rowId) {
    By toggle = frontEndInstructions.getGridRowDetailToggle(gridId, rowId);
    waitUntil(toBeVisible(toggle));
    getBrowser().executeScriptOn(locator(toggle), "arguments[0].focus();");
    return locator(toggle);
  }

  /**
   * Check that the detail of a row of a grid is open with a text, and that its toggle says so ({@code aria-expanded}) and
   * points to it ({@code aria-controls} is the id of the detail)
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   * @param text   Text that the detail contains, with its line breaks
   */
  protected void checkGridRowDetailOpen(String gridId, String rowId, String text) {
    Locator toggle = locator(frontEndInstructions.getGridRowDetailToggle(gridId, rowId));
    Locator detail = locator(frontEndInstructions.getGridRowDetail(gridId, rowId));
    waitUntil(BrowserCondition.allOf(toSayThatTheDetailIs(toggle, true), BrowserCondition.visible(detail),
      BrowserCondition.textContains(detail, text)));
    String controls = getBrowser().attribute(toggle, "aria-controls");
    String detailId = getBrowser().attribute(detail, "id");
    assertWithScreenshot("The toggle of the row " + rowId + " of " + gridId + " controls '" + controls + "', not its detail '" + detailId + "'",
      controls != null && controls.equals(detailId));
  }

  /**
   * Check the label that the toggle of the detail of a row of a grid gives to assistive technologies ({@code aria-label}),
   * which tells whether it shows or hides the detail
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   * @param label  Expected label, in the language of the user
   */
  protected void checkGridRowDetailToggleLabel(String gridId, String rowId, String label) {
    Locator toggle = locator(frontEndInstructions.getGridRowDetailToggle(gridId, rowId));
    waitUntil(BrowserCondition.of("the toggle of the detail of the row " + rowId + " of " + gridId + " to be labelled '" + label + "'",
      browser -> {
        try {
          return label.equals(browser.attribute(toggle, "aria-label"));
        } catch (ElementReplacedException exc) {
          return false;
        }
      }));
  }

  /**
   * Check that the detail of a row of a grid is closed, and that its toggle says so ({@code aria-expanded})
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void checkGridRowDetailClosed(String gridId, String rowId) {
    waitUntil(toSayThatTheDetailIs(locator(frontEndInstructions.getGridRowDetailToggle(gridId, rowId)), false));
    checkNotVisible(locator(frontEndInstructions.getGridRowDetail(gridId, rowId)));
  }

  /**
   * Check that no row of a grid has its detail open: no detail is shown and no toggle says that its detail is open
   *
   * @param gridId Grid identifier
   */
  protected void checkNoGridRowDetailOpen(String gridId) {
    String toggles = locator(frontEndInstructions.getGridRowDetailToggle(gridId, null)).expression();
    waitUntil(BrowserCondition.allOf(
      BrowserCondition.noneVisible(locator(frontEndInstructions.getGridRowDetail(gridId, null))),
      BrowserCondition.of("no toggle of the details of the rows of " + gridId + " to say that its detail is open", browser ->
        browser.executeScript(OPEN_ROW_DETAIL_TOGGLES_SCRIPT, toggles) instanceof Number open && open.intValue() == 0)));
  }

  /**
   * Condition: the toggle of the detail of a row is shown and says that the detail is open or closed
   * ({@code aria-expanded}), which is what assistive technologies announce
   *
   * @param toggle Toggle locator
   * @param isOpen True for an open detail, false for a closed one
   * @return Condition
   */
  private static BrowserCondition toSayThatTheDetailIs(Locator toggle, boolean isOpen) {
    String expanded = String.valueOf(isOpen);
    return BrowserCondition.of("toggle located by " + toggle + " to have aria-expanded " + expanded, browser -> {
      try {
        return browser.isVisible(toggle) && expanded.equals(browser.attribute(toggle, "aria-expanded"));
      } catch (ElementReplacedException exc) {
        return false;
      }
    });
  }

  /**
   * Check that the detail of a row of a grid has some style classes, as the {@code style} of the row detail of the grid
   * assigns them
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param cssClass Style classes, separated by spaces
   */
  protected void checkGridRowDetailHasClass(String gridId, String rowId, String cssClass) {
    waitUntil(toBeVisible(frontEndInstructions.getGridRowDetailWithClass(gridId, rowId, cssClass)));
  }

  /**
   * Check the value that the browser computes for a style property of the detail of a row of a grid, which proves that
   * the style rules of its classes reach the page
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param property Css property ({@code border-left-width})
   * @param value    Expected computed value ({@code 4px})
   */
  protected void checkGridRowDetailComputedStyle(String gridId, String rowId, String property, String value) {
    Locator detail = locator(frontEndInstructions.getGridRowDetail(gridId, rowId));
    waitUntil(BrowserCondition.of("the " + property + " of the detail of the row " + rowId + " of " + gridId + " to be " + value,
      browser -> browser.isVisible(detail) && value.equals(String.valueOf(
        browser.executeScriptOn(detail, "return getComputedStyle(arguments[0]).getPropertyValue(arguments[1]);", property)))));
  }

  /**
   * Check that the detail of a row of a grid starts right under its row (a few pixels at most) and is about as wide as
   * the row (90% at least)
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void checkGridRowDetailUnderItsRow(String gridId, String rowId) {
    By row = frontEndInstructions.getGridRow(gridId, rowId);
    By detail = frontEndInstructions.getGridRowDetail(gridId, rowId);
    waitUntil(BrowserCondition.allOf(toBeVisible(row), toBeVisible(detail)));
    String rowBox = String.valueOf(getBrowser().executeScriptOn(locator(row), BOX_SCRIPT));
    String detailBox = String.valueOf(getBrowser().executeScriptOn(locator(detail), BOX_SCRIPT));
    double[] rowEdges = parseBox(rowBox);
    double[] detailEdges = parseBox(detailBox);
    double gap = detailEdges[0] - rowEdges[1];
    assertWithScreenshot("The detail of the row " + rowId + " of " + gridId + " (" + detailBox + ") is not under its row (" + rowBox
        + "): top|bottom|width", gap >= -1 && gap <= ROW_DETAIL_MAX_GAP && detailEdges[2] >= rowEdges[2] * ROW_DETAIL_MIN_WIDTH);
  }

  /**
   * Check that the detail of a row of a grid is about as wide as the visible part of the grid (90% at least), even when
   * the columns of the grid are narrower
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void checkGridRowDetailAcrossGrid(String gridId, String rowId) {
    By detail = frontEndInstructions.getGridRowDetail(gridId, rowId);
    By visiblePart = frontEndInstructions.getGridScrollZone(gridId);
    waitUntil(BrowserCondition.allOf(toBeVisible(detail), toBeVisible(visiblePart)));
    String detailBox = String.valueOf(getBrowser().executeScriptOn(locator(detail), BOX_SCRIPT));
    Object visibleWidth = getBrowser().executeScriptOn(locator(visiblePart), CLIENT_WIDTH_SCRIPT);
    double width = visibleWidth instanceof Number number ? number.doubleValue() : Double.NaN;
    assertWithScreenshot("The detail of the row " + rowId + " of " + gridId + " (" + detailBox + ": top|bottom|width) is not as wide"
      + " as the visible part of the grid (" + visibleWidth + ")", width > 0 && parseBox(detailBox)[2] >= width * ROW_DETAIL_MIN_WIDTH);
  }

  /**
   * Check that the detail of a row of a grid shows a nested grid (a row detail with components)
   *
   * @param gridId       Grid identifier
   * @param rowId        Row identifier
   * @param nestedGridId Identifier of the nested grid, as the client renders it for the row ({@code GrdOrders-7})
   */
  protected void checkGridRowDetailHasNestedGrid(String gridId, String rowId, String nestedGridId) {
    waitUntil(toBeVisible(frontEndInstructions.getGridRowDetailNestedGrid(gridId, rowId, nestedGridId)));
  }

  /**
   * Check that the detail of a row of a grid holds the whole nested grid that it shows, down to its last row and across
   * its width: the detail grows with the nested grid and does not cut it
   *
   * @param gridId       Grid identifier
   * @param rowId        Row identifier
   * @param nestedGridId Identifier of the nested grid, as the client renders it for the row ({@code GrdOrders-7})
   */
  protected void checkGridRowDetailHoldsNestedGrid(String gridId, String rowId, String nestedGridId) {
    By detail = frontEndInstructions.getGridRowDetail(gridId, rowId);
    By nested = frontEndInstructions.getGridRowDetailNestedGrid(gridId, rowId, nestedGridId);
    By nestedRows = frontEndInstructions.getGridRow(nestedGridId, null);
    waitUntil(BrowserCondition.allOf(toBeVisible(detail), toBeVisible(nested), toBePresent(nestedRows),
      checkIfGridLoaderIsNotVisible()));
    String detailBox = String.valueOf(getBrowser().executeScriptOn(locator(detail), EDGES_SCRIPT));
    String nestedBox = String.valueOf(getBrowser().executeScriptOn(locator(nested), EDGES_SCRIPT));
    Object lastRowBottom = getBrowser().executeScript(LAST_BOTTOM_SCRIPT, locator(nestedRows).expression());
    double[] outer = parseNumbers(detailBox, 4);
    double[] inner = parseNumbers(nestedBox, 4);
    double lastBottom = lastRowBottom instanceof Number number ? number.doubleValue() : Double.NaN;
    assertWithScreenshot("The detail of the row " + rowId + " of " + gridId + " (" + detailBox + ") does not hold the nested grid "
        + nestedGridId + " (" + nestedBox + ") down to its last row (" + lastRowBottom + "): top|bottom|left|right",
      inner[1] > inner[0] && inner[0] >= outer[0] - ROW_DETAIL_TOLERANCE && inner[1] <= outer[1] + ROW_DETAIL_TOLERANCE
        && inner[2] >= outer[2] - ROW_DETAIL_TOLERANCE && inner[3] <= outer[3] + ROW_DETAIL_TOLERANCE
        && lastBottom <= inner[1] + ROW_DETAIL_TOLERANCE);
  }

  /**
   * Read the box of an element that {@link #BOX_SCRIPT} returns
   *
   * @param box Box as top|bottom|width
   * @return Top, bottom and width (not numbers when the box is not valid)
   */
  private static double[] parseBox(String box) {
    return parseNumbers(box, 3);
  }

  /**
   * Read the numbers, separated by {@code |}, that a script returns
   *
   * @param numbers Numbers separated by {@code |}
   * @param count   Number of values to read
   * @return Values (not numbers when a value is missing or not valid)
   */
  private static double[] parseNumbers(String numbers, int count) {
    String[] edges = numbers.split("\\|");
    double[] values = new double[count];
    Arrays.fill(values, Double.NaN);
    for (int index = 0; index < Math.min(edges.length, values.length); index++) {
      try {
        values[index] = Double.parseDouble(edges[index]);
      } catch (NumberFormatException exc) {
        values[index] = Double.NaN;
      }
    }
    return values;
  }

  /**
   * Check that a row of a grid is shown and not selected
   *
   * @param gridId Grid identifier
   * @param rowId  Row identifier
   */
  protected void checkGridRowNotSelected(String gridId, String rowId) {
    waitUntil(toBeVisible(frontEndInstructions.getGridRowSelected(gridId, rowId, false)));
  }

  /**
   * Open the Columns dialog of the toolbar of a grid
   *
   * @param gridId Grid identifier
   */
  protected void openGridColumns(String gridId) {
    click(frontEndInstructions.getGridColumnsButton(gridId));
    waitUntil(toBeVisible(frontEndInstructions.getGridColumnsDialog(gridId)));
  }

  /**
   * Check that the Columns of a grid are open as a popover anchored to the Columns button, as a modal dialog or as a
   * side panel (grid attribute columns-panel)
   *
   * @param gridId Grid identifier
   * @param panel  popover, dialog or side-panel
   */
  protected void checkGridColumnsPanel(String gridId, String panel) {
    waitUntil(toBeVisible(frontEndInstructions.getGridColumnsPanel(gridId, panel)));
  }

  /**
   * Close the Columns dialog of a grid
   *
   * @param gridId Grid identifier
   */
  protected void closeGridColumns(String gridId) {
    // A reset or a save shows its outcome in the dialog when the server answers, and the centered dialog grows: a click
    // on its way at that moment misses the Close button, so it is clicked again while the dialog and its button stay.
    // Once the button is gone the click worked, and the dialog is only waited for
    By dialog = frontEndInstructions.getGridColumnsDialog(gridId);
    By close = frontEndInstructions.getGridColumnsClose(gridId);
    Duration wait = properties.getTimeout().compareTo(EDIT_ROW_WAIT) < 0 ? properties.getTimeout() : EDIT_ROW_WAIT;
    for (int attempt = 1; attempt <= CLOSE_COLUMNS_ATTEMPTS && getBrowser().exists(locator(close)); attempt++) {
      if (attempt > 1) {
        log.warn("The Columns dialog of {} is still open: click {} of {} on Close", gridId, attempt, CLOSE_COLUMNS_ATTEMPTS);
      }
      click(close);
      try {
        BrowserPoll.until(getBrowser(), BrowserCondition.not(toBePresent(dialog)), wait);
        return;
      } catch (BrowserPoll.PollTimeoutException exc) {
        log.debug("The Columns dialog of {} did not close within {}", gridId, wait, exc);
      }
    }
    waitUntil(BrowserCondition.not(toBePresent(dialog)));
  }

  /**
   * Check the counter of the Columns dialog of a grid: how many columns are shown of the ones it lists
   *
   * @param gridId  Grid identifier
   * @param visible Columns shown
   * @param total   Columns listed
   */
  protected void checkGridColumnsCounter(String gridId, int visible, int total) {
    waitUntil(toBePresent(frontEndInstructions.getGridColumnsCounter(gridId, String.valueOf(visible), String.valueOf(total))));
  }

  /**
   * Check that the Columns dialog of a grid lists a column. The columns that the screen hides never appear in it
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridColumnsListed(String gridId, String columnId) {
    waitUntil(toBePresent(frontEndInstructions.getGridColumnsItem(gridId, columnId)));
  }

  /**
   * Check that the Columns dialog of a grid does not list a column
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridColumnsNotListed(String gridId, String columnId) {
    waitUntil(BrowserCondition.not(toBePresent(frontEndInstructions.getGridColumnsItem(gridId, columnId))));
  }

  /**
   * Hide a column of a grid unchecking it in the Columns dialog (that has to be open)
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void hideGridColumn(String gridId, String columnId) {
    setGridColumnChecked(gridId, columnId, false);
  }

  /**
   * Show a column of a grid checking it in the Columns dialog (that has to be open)
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void showGridColumn(String gridId, String columnId) {
    setGridColumnChecked(gridId, columnId, true);
  }

  private void setGridColumnChecked(String gridId, String columnId, boolean checked) {
    By checkbox = frontEndInstructions.getGridColumnsCheckbox(gridId, columnId);
    waitUntil(toBePresent(checkbox));
    Object current = getBrowser().executeScriptOn(locator(checkbox), "return arguments[0].checked;");
    if (!Boolean.valueOf(checked).equals(current)) {
      click(checkbox);
    }
  }

  /**
   * Move a column of a grid one position up with its arrow in the Columns dialog (that has to be open)
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void moveGridColumnUp(String gridId, String columnId) {
    clickGridColumnArrow(gridId, columnId, frontEndInstructions.getGridColumnsMoveUp(gridId, columnId));
  }

  /**
   * Move a column of a grid one position down with its arrow in the Columns dialog (that has to be open)
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void moveGridColumnDown(String gridId, String columnId) {
    clickGridColumnArrow(gridId, columnId, frontEndInstructions.getGridColumnsMoveDown(gridId, columnId));
  }

  // The arrows of a column only show on the row under the pointer (or with the focus), as a user sees them: the row
  // is hovered before its arrow is clicked
  private void clickGridColumnArrow(String gridId, String columnId, By arrow) {
    By item = frontEndInstructions.getGridColumnsItem(gridId, columnId);
    waitUntil(toBeVisible(item));
    getBrowser().hover(locator(item));
    click(arrow);
  }

  /**
   * Check that a column cannot move up in the Columns dialog of a grid: it is the first one of its group header
   * (or the first column without group), because a column never leaves its group
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridColumnCannotMoveUp(String gridId, String columnId) {
    checkGridColumnArrowDisabled(frontEndInstructions.getGridColumnsMoveUp(gridId, columnId));
  }

  /**
   * Check that a column cannot move down in the Columns dialog of a grid: it is the last one of its group header
   * (or the last column without group), because a column never leaves its group
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridColumnCannotMoveDown(String gridId, String columnId) {
    checkGridColumnArrowDisabled(frontEndInstructions.getGridColumnsMoveDown(gridId, columnId));
  }

  private void checkGridColumnArrowDisabled(By arrow) {
    waitUntil(toBePresent(arrow));
    Object disabled = getBrowser().executeScriptOn(locator(arrow), "return arguments[0].disabled === true;");
    assertWithScreenshot("The arrow " + arrow + " is enabled", Boolean.TRUE.equals(disabled));
  }

  /**
   * Show all the columns of a grid with the action of its Columns dialog
   *
   * @param gridId Grid identifier
   * @deprecated The Columns no longer have a Show all action: check each hidden column instead with
   * {@link #showGridColumn(String, String)}. It stays available through the whole 5.x line and is not removed before 6.0
   */
  @Deprecated
  protected void clickGridColumnsShowAll(String gridId) {
    click(frontEndInstructions.getGridColumnsShowAll(gridId));
  }

  /**
   * Go back to the definition of the screen with the action of the Columns dialog of a grid, which also removes the layout
   * that the user saved. It is silent (no message when it works): the step waits until the server answers, when the
   * Columns are no longer {@code aria-busy}. A warning or an error of the server is still shown as a message
   *
   * @param gridId Grid identifier
   */
  protected void clickGridColumnsReset(String gridId) {
    click(frontEndInstructions.getGridColumnsReset(gridId));
    waitForGridColumnsCall(gridId);
  }

  /**
   * Save the layout of a grid as the default of the user with the action of its Columns dialog. It is silent (no message
   * when it works) and closes the Columns (popover, dialog or side panel) when the server answers that the layout was
   * saved: the step waits until they are closed. When the layout is not saved the Columns stay open (a warning or an
   * error of the server is still shown as a message) and the step fails
   *
   * @param gridId Grid identifier
   */
  protected void clickGridColumnsSave(String gridId) {
    click(frontEndInstructions.getGridColumnsSave(gridId));
    waitUntil(BrowserCondition.not(toBePresent(frontEndInstructions.getGridColumnsDialog(gridId))));
  }

  private void waitForGridColumnsCall(String gridId) {
    waitUntil(BrowserCondition.not(toBePresent(frontEndInstructions.getGridColumnsBusy(gridId))));
  }

  /**
   * Undo the changes made in the Columns dialog of a grid since it was opened (or since the last reset) with
   * its Cancel action, which also closes it
   *
   * @param gridId Grid identifier
   */
  protected void clickGridColumnsCancel(String gridId) {
    click(frontEndInstructions.getGridColumnsCancel(gridId));
    waitUntil(BrowserCondition.not(toBePresent(frontEndInstructions.getGridColumnsDialog(gridId))));
  }

  /**
   * Check the order of the visible columns of a grid, from left to right. The columns have to be shown
   *
   * @param gridId    Grid identifier
   * @param columnIds Columns in the order they are expected
   */
  protected void checkGridColumnsLeftToRight(String gridId, String... columnIds) {
    String script = "return arguments[0].getBoundingClientRect().left;";
    double previous = Double.NEGATIVE_INFINITY;
    for (String columnId : columnIds) {
      By header = frontEndInstructions.getGridHeader(gridId, columnId);
      waitUntil(toBeVisible(header));
      Object measured = getBrowser().executeScriptOn(locator(header), script);
      double left = measured instanceof Number number ? number.doubleValue() : Double.NaN;
      assertWithScreenshot("The column " + columnId + " of the grid " + gridId + " is not after the previous one: " + left + " <= " + previous,
        left > previous);
      previous = left;
    }
  }

  /**
   * Check that a column of a grid is not shown
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridColumnNotShown(String gridId, String columnId) {
    waitUntil(BrowserCondition.not(toBeVisible(frontEndInstructions.getGridHeader(gridId, columnId))));
  }

  /**
   * Check that a group header of a grid spans exactly over its first and its last visible columns, so hiding any column of
   * the group keeps the header aligned with the ones that are shown
   *
   * @param gridId        Grid identifier
   * @param title         Title of the group header
   * @param firstColumnId First visible column of the group
   * @param lastColumnId  Last visible column of the group
   */
  protected void checkGridGroupHeaderOver(String gridId, String title, String firstColumnId, String lastColumnId) {
    By group = frontEndInstructions.getGridGroupHeader(gridId, title);
    By first = frontEndInstructions.getGridHeader(gridId, firstColumnId);
    By last = frontEndInstructions.getGridHeader(gridId, lastColumnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(group), toBeVisible(first), toBeVisible(last)));
    Object groupLeft = getBrowser().executeScriptOn(locator(group), "return arguments[0].getBoundingClientRect().left;");
    Object groupRight = getBrowser().executeScriptOn(locator(group), "return arguments[0].getBoundingClientRect().right;");
    Object firstLeft = getBrowser().executeScriptOn(locator(first), "return arguments[0].getBoundingClientRect().left;");
    Object lastRight = getBrowser().executeScriptOn(locator(last), "return arguments[0].getBoundingClientRect().right;");
    assertWithScreenshot("The group header " + title + " of the grid " + gridId + " does not span from " + firstColumnId + " to " + lastColumnId
        + " (" + groupLeft + "-" + groupRight + " against " + firstLeft + "-" + lastRight + ")",
      isNear(groupLeft, firstLeft) && isNear(groupRight, lastRight));
  }

  /**
   * Check that a grid does not show a group header: none of its columns is shown
   *
   * @param gridId Grid identifier
   * @param title  Title of the group header
   */
  protected void checkGridGroupHeaderNotShown(String gridId, String title) {
    waitUntil(BrowserCondition.not(toBeVisible(frontEndInstructions.getGridGroupHeader(gridId, title))));
  }

  private boolean isNear(Object first, Object second) {
    return first instanceof Number a && second instanceof Number b && Math.abs(a.doubleValue() - b.doubleValue()) <= GROUP_HEADER_TOLERANCE;
  }

  /**
   * Maximize a grid with the button of its toolbar
   *
   * @param gridId Grid identifier
   */
  protected void maximizeGrid(String gridId) {
    click(frontEndInstructions.getGridMaximizeButton(gridId));
  }

  /**
   * Restore a maximized grid with the button of its toolbar
   *
   * @param gridId Grid identifier
   */
  protected void restoreGrid(String gridId) {
    click(frontEndInstructions.getGridMaximizeButton(gridId));
  }

  /**
   * Check that a grid fills the content area of the screen (its maximize target), like a maximized window: its toolbar says so,
   * the grid takes the size of the content area and the page keeps something above it (the header of the application)
   *
   * @param gridId Grid identifier
   */
  protected void checkGridMaximized(String gridId) {
    waitUntil(toBePresent(frontEndInstructions.getGridToolbarMaximized(gridId, true)));
    checkFillsMaximizeTarget("grid " + gridId, frontEndInstructions.getGrid(gridId));
  }

  /**
   * Check that an element fills its maximize target (the content area) and leaves the header of the application above it
   *
   * @param description Description of the element for the failure message
   * @param element     Element selector
   */
  private void checkFillsMaximizeTarget(String description, By element) {
    waitUntil(toBeVisible(element));
    // Smallest ratio between the sizes of the element and of its maximize target (0 when it has none)
    Object fill = getBrowser().executeScriptOn(locator(element), "const target = arguments[0].closest('.maximize-target'); if (!target) { return 0; } "
      + "const g = arguments[0].getBoundingClientRect(); const t = target.getBoundingClientRect(); "
      + "return Math.min(g.width / t.width, t.width / g.width, g.height / t.height, t.height / g.height);");
    Object top = getBrowser().executeScriptOn(locator(element), "return arguments[0].getBoundingClientRect().top;");
    assertWithScreenshot("The " + description + " does not fill the content area (" + fill + " of its size) or covers the header (" + top + "px from the top)",
      fill instanceof Number f && top instanceof Number t && f.doubleValue() >= MAXIMIZED_MIN_FILL && t.doubleValue() > 0);
  }

  /**
   * Check that a grid does not fill the window
   *
   * @param gridId Grid identifier
   */
  protected void checkGridNotMaximized(String gridId) {
    waitUntil(toBePresent(frontEndInstructions.getGridToolbarMaximized(gridId, false)));
    checkNotFixed("grid " + gridId, frontEndInstructions.getGrid(gridId));
  }

  /**
   * Wait until an element that was maximized is back in the layout of the page (it is no longer fixed over the content area)
   *
   * @param description Description of the element for the failure message
   * @param element     Element selector
   */
  private void checkNotFixed(String description, By element) {
    waitUntil(BrowserCondition.of("the " + description + " to stop being fixed over the content area",
      browser -> !"fixed".equals(browser.executeScriptOn(locator(element), "return window.getComputedStyle(arguments[0]).position;"))));
  }

  /**
   * Maximize a window with its Maximize button
   *
   * @param windowId Window identifier
   */
  protected void maximizeWindow(String windowId) {
    click(frontEndInstructions.getWindowMaximizeButton(windowId));
  }

  /**
   * Restore a maximized window with its Restore button
   *
   * @param windowId Window identifier
   */
  protected void restoreWindow(String windowId) {
    click(frontEndInstructions.getWindowMaximizeButton(windowId));
  }

  /**
   * Check that a window fills the content area of the screen (its maximize target) and keeps the header of the application above it
   *
   * @param windowId Window identifier
   */
  protected void checkWindowMaximized(String windowId) {
    waitUntil(toBePresent(frontEndInstructions.getWindow(windowId, true)));
    checkFillsMaximizeTarget("window " + windowId, frontEndInstructions.getWindow(windowId, true));
  }

  /**
   * Check that a window is not maximized
   *
   * @param windowId Window identifier
   */
  protected void checkWindowNotMaximized(String windowId) {
    waitUntil(toBePresent(frontEndInstructions.getWindow(windowId, false)));
    checkNotFixed("window " + windowId, frontEndInstructions.getWindow(windowId, false));
  }

  /**
   * Maximize a tab component (tab list and content) with its Maximize button
   *
   * @param tabId Tab identifier
   */
  protected void maximizeTab(String tabId) {
    click(frontEndInstructions.getTabMaximizeButton(tabId));
  }

  /**
   * Restore a maximized tab component with its Restore button
   *
   * @param tabId Tab identifier
   */
  protected void restoreTab(String tabId) {
    click(frontEndInstructions.getTabMaximizeButton(tabId));
  }

  /**
   * Check that a tab component fills the content area of the screen (its maximize target) and keeps the header of the application above it
   *
   * @param tabId Tab identifier
   */
  protected void checkTabMaximized(String tabId) {
    waitUntil(toBePresent(frontEndInstructions.getTabComponent(tabId, true)));
    checkFillsMaximizeTarget("tab " + tabId, frontEndInstructions.getTabComponent(tabId, true));
  }

  /**
   * Check that a tab component is not maximized
   *
   * @param tabId Tab identifier
   */
  protected void checkTabNotMaximized(String tabId) {
    waitUntil(toBePresent(frontEndInstructions.getTabComponent(tabId, false)));
    checkNotFixed("tab " + tabId, frontEndInstructions.getTabComponent(tabId, false));
  }

  /**
   * Check that the header of a column of a grid is displayed
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridHeaderVisible(String gridId, String columnId) {
    checkVisible(locator(frontEndInstructions.getGridHeader(gridId, columnId)));
  }

  /**
   * Check that the header of a column of a grid is not displayed (a grid with the {@code grid-no-header} class)
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   */
  protected void checkGridHeaderNotVisible(String gridId, String columnId) {
    checkNotVisible(locator(frontEndInstructions.getGridHeader(gridId, columnId)));
  }

  /**
   * Check that the first row of a grid starts right under the top of the grid, so a grid without header
   * (the {@code grid-no-header} class) leaves no empty space where the header was, give or take two pixels
   *
   * @param gridId   Grid identifier
   * @param rowId    Identifier of the first row
   * @param columnId Identifier of any column of that row
   */
  protected void checkGridFirstRowAtTop(String gridId, String rowId, String columnId) {
    By cell = frontEndInstructions.getGridCell(gridId, rowId, columnId);
    waitUntil(toBeVisible(cell));
    String script = "return arguments[0].getBoundingClientRect().top;";
    Object cellTop = getBrowser().executeScriptOn(locator(cell), script);
    Object gridTop = getBrowser().executeScriptOn(locator(frontEndInstructions.getGrid(gridId)), script);
    double gap = cellTop instanceof Number c && gridTop instanceof Number g ? c.doubleValue() - g.doubleValue() : Double.NaN;
    Object measured = gap;
    assertWithScreenshot("The first row of the grid " + gridId + " starts " + measured + "px under its top, not at it",
      Math.abs(gap) <= GRID_TOP_TOLERANCE);
  }

  /**
   * Check the sparkline that a sparkline column draws in a cell of a grid: the type of the drawing (line, bar or area)
   * and the value of every point, which is its tooltip, in order
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param type     Expected type
   * @param values   Expected values of the points, separated by commas
   */
  protected void checkGridSparkline(String gridId, String rowId, String columnId, String type, String values) {
    By sparkline = frontEndInstructions.getGridSparkline(gridId, rowId, columnId);
    waitUntil(toBePresent(sparkline));
    Object shown = getBrowser().executeScriptOn(locator(sparkline), SPARKLINE_SUMMARY_SCRIPT);
    assertWithScreenshot("The sparkline of " + gridId + "/" + rowId + "/" + columnId + " is '" + shown + "', not '" + type + "|" + values + "'",
      (type + "|" + values).equals(shown));
  }

  /**
   * Check that a cell of a grid has no sparkline because its value is not a valid series of numbers, while the cell is
   * still shown
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void checkGridSparklineEmpty(String gridId, String rowId, String columnId) {
    // The row can be drawn after the grid loads: an empty cell only proves something once the grid has finished loading
    waitForLoadingGrid();
    waitUntil(BrowserCondition.allOf(
      toBePresent(frontEndInstructions.getGridCell(gridId, rowId, columnId)),
      BrowserCondition.not(toBePresent(frontEndInstructions.getGridSparkline(gridId, rowId, columnId)))));
  }

  /**
   * Check how many markers (dots of the lowest, highest and last points) the sparkline of a cell of a grid has
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param markers  Expected number of markers
   */
  protected void checkGridSparklineMarkers(String gridId, String rowId, String columnId, int markers) {
    By sparkline = frontEndInstructions.getGridSparkline(gridId, rowId, columnId);
    waitUntil(toBePresent(sparkline));
    Object shown = getBrowser().executeScriptOn(locator(sparkline), "return arguments[0].querySelectorAll('circle').length;");
    assertWithScreenshot("The sparkline of " + gridId + "/" + rowId + "/" + columnId + " has " + shown + " markers, not " + markers,
      shown instanceof Number number && number.intValue() == markers);
  }

  /**
   * Check that the sparkline of a cell of a grid does not take the keyboard focus (it is not focusable and nothing in it
   * is a tab stop), so it does not get in the way of the keyboard navigation of the grid
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   */
  protected void checkGridSparklineNotFocusable(String gridId, String rowId, String columnId) {
    By sparkline = frontEndInstructions.getGridSparkline(gridId, rowId, columnId);
    waitUntil(toBePresent(sparkline));
    Object shown = getBrowser().executeScriptOn(locator(sparkline), SPARKLINE_FOCUS_SCRIPT);
    assertWithScreenshot("The sparkline of " + gridId + "/" + rowId + "/" + columnId + " takes the focus: '" + shown + "'",
      "false|0".equals(shown));
  }

  /**
   * Check that a cell of a grid has a style class (for instance the {@code scale-seq-3} that a query assigns to colour it)
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param cssClass Style class
   */
  protected void checkCellHasClass(String gridId, String rowId, String columnId, String cssClass) {
    waitUntil(toBePresent(frontEndInstructions.getGridCellWithClass(gridId, rowId, columnId, cssClass)));
  }

  /**
   * Script that finds the text block of a cell of a grid that wraps over several lines (the cell or the text it holds)
   */
  private static final String WRAPPED_TEXT_OF_CELL = "const cell = arguments[0];"
    + " const text = cell.matches('.p-cell-wrap, .cell-wrap') ? cell : (cell.querySelector('.p-cell-wrap, .cell-wrap') || cell);";

  /**
   * Read a number of a cell of a grid with a script that starts with the text block of the cell
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param script   Script that returns the number, once it has the text block in {@code text}
   * @return Number
   */
  private double readWrappedCell(String gridId, String rowId, String columnId, String script) {
    By cell = frontEndInstructions.getGridCell(gridId, rowId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(cell), checkIfGridLoaderIsNotVisible()));
    Object value = getBrowser().executeScriptOn(locator(cell), WRAPPED_TEXT_OF_CELL + script);
    return value instanceof Number number ? number.doubleValue() : Double.NaN;
  }

  /**
   * Check the number of lines that the text of a cell of a grid shows (a column that wraps, with or without a maximum)
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param lines    Expected number of lines
   */
  protected void checkGridCellLines(String gridId, String rowId, String columnId, int lines) {
    double shown = readCellLines(gridId, rowId, columnId);
    assertWithScreenshot("The cell " + columnId + " of the row " + rowId + " of the grid " + gridId + " shows " + shown
      + " lines, not " + lines, shown == lines);
  }

  /**
   * Check that the text of a cell of a grid wraps over at least some lines
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param lines    Minimum number of lines
   */
  protected void checkGridCellLinesAtLeast(String gridId, String rowId, String columnId, int lines) {
    double shown = readCellLines(gridId, rowId, columnId);
    assertWithScreenshot("The cell " + columnId + " of the row " + rowId + " of the grid " + gridId + " shows " + shown
      + " lines, fewer than " + lines, shown >= lines);
  }

  private double readCellLines(String gridId, String rowId, String columnId) {
    return readWrappedCell(gridId, rowId, columnId, " const style = getComputedStyle(text);"
      + " return Math.round((text.clientHeight - parseFloat(style.paddingTop) - parseFloat(style.paddingBottom))"
      + " / parseFloat(style.lineHeight));");
  }

  /**
   * Check that the text of a cell of a grid is shown whole, or that it is cut (a column with a maximum number of lines
   * whose text is longer)
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param cut      The text is expected to be cut
   */
  protected void checkGridCellTextCut(String gridId, String rowId, String columnId, boolean cut) {
    double hidden = readWrappedCell(gridId, rowId, columnId, " return text.scrollHeight - text.clientHeight;");
    assertWithScreenshot("The text of the cell " + columnId + " of the row " + rowId + " of the grid " + gridId
      + (cut ? " is not cut" : " is cut (" + hidden + "px hidden)"), (hidden > 1) == cut);
  }

  /**
   * Check that the row of a grid is taller than another one
   *
   * @param gridId   Grid identifier
   * @param rowId    Row that is expected to be taller
   * @param otherId  Other row
   * @param columnId Identifier of any column
   */
  protected void checkGridRowTaller(String gridId, String rowId, String otherId, String columnId) {
    String script = "return arguments[0].closest('[data-testid=\"grid-row\"]').getBoundingClientRect().height;";
    By cell = frontEndInstructions.getGridCell(gridId, rowId, columnId);
    By otherCell = frontEndInstructions.getGridCell(gridId, otherId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(cell), toBeVisible(otherCell), checkIfGridLoaderIsNotVisible()));
    Object height = getBrowser().executeScriptOn(locator(cell), script);
    Object otherHeight = getBrowser().executeScriptOn(locator(otherCell), script);
    assertWithScreenshot("The row " + rowId + " (" + height + "px) of the grid " + gridId + " is not taller than the row "
      + otherId + " (" + otherHeight + "px)",
      height instanceof Number h && otherHeight instanceof Number o && h.doubleValue() > o.doubleValue() + 1);
  }

  /**
   * Check that the row of a grid is tall enough for all its cells: no cell of the row has text that overflows it
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Identifier of any column
   */
  protected void checkGridRowFitsItsCells(String gridId, String rowId, String columnId) {
    By cell = frontEndInstructions.getGridCell(gridId, rowId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(cell), checkIfGridLoaderIsNotVisible()));
    Object overflow = getBrowser().executeScriptOn(locator(cell), "const row = arguments[0].closest('[data-testid=\"grid-row\"]');"
      + " const bottom = row.getBoundingClientRect().bottom + 1;"
      + " return Array.from(row.querySelectorAll('.p-cell-wrap, .cell-wrap')).filter(e => e.getBoundingClientRect().bottom > bottom).length;");
    assertWithScreenshot("The row " + rowId + " of the grid " + gridId + " is shorter than the text of " + overflow + " of its cells",
      overflow instanceof Number n && n.intValue() == 0);
  }

  /**
   * Check that the whole value of a cell of a grid is exposed as the tooltip of its text (when the text is cut by the
   * width of the column)
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param text     Text that the title contains
   */
  protected void checkGridCellTitle(String gridId, String rowId, String columnId, String text) {
    By cell = frontEndInstructions.getGridCell(gridId, rowId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(cell), checkIfGridLoaderIsNotVisible()));
    Object title = getBrowser().executeScriptOn(locator(cell), "const e = arguments[0].matches('[title]') ? arguments[0]"
      + " : arguments[0].querySelector('[title]'); return e ? e.getAttribute('title') : '';");
    assertWithScreenshot("The title of the cell " + columnId + " of the row " + rowId + " of the grid " + gridId + " is '" + title
      + "', it does not contain '" + text + "'", title instanceof String s && s.contains(text));
  }

  /**
   * Check that the cut text of a cell of a grid shows its whole value in a tooltip on hover and on keyboard focus, and
   * that the tooltip goes away when the pointer or the focus leaves
   *
   * @param gridId   Grid identifier
   * @param rowId    Row identifier
   * @param columnId Column identifier
   * @param text     Text that the tooltip contains
   */
  protected void checkGridCellFullTextTooltip(String gridId, String rowId, String columnId, String text) {
    By cell = frontEndInstructions.getGridCell(gridId, rowId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(cell), checkIfGridLoaderIsNotVisible()));
    String tooltip = "const t = document.querySelector('.grid-cell-tooltip'); return t ? t.textContent : '';";
    String textOfCell = WRAPPED_TEXT_OF_CELL + " return text;";

    // On hover
    getBrowser().hover(locator(cell));
    waitUntil(BrowserCondition.of("the tooltip of the cell " + columnId + " to show '" + text + "' on hover",
      browser -> browser.executeScript(tooltip) instanceof String shown && shown.contains(text)));

    // The pointer leaves
    getBrowser().hover(locator(frontEndInstructions.getGridHeader(gridId, columnId)));
    waitUntil(BrowserCondition.of("the tooltip to go away when the pointer leaves",
      browser -> browser.executeScript(tooltip) instanceof String shown && shown.isEmpty()));

    // On keyboard focus: the cut text is reachable with the keyboard
    getBrowser().executeScriptOn(locator(cell), WRAPPED_TEXT_OF_CELL + " text.focus();");
    waitUntil(BrowserCondition.of("the tooltip of the cell " + columnId + " to show '" + text + "' on focus",
      browser -> browser.executeScript(tooltip) instanceof String shown && shown.contains(text)));
    assertWithScreenshot("The cut text of the cell " + columnId + " is not reachable with the keyboard",
      getBrowser().executeScriptOn(locator(cell), WRAPPED_TEXT_OF_CELL + " return text.tabIndex >= 0 && document.activeElement === text;")
        instanceof Boolean reachable && reachable);

    // The focus leaves
    getBrowser().executeScriptOn(locator(cell), WRAPPED_TEXT_OF_CELL + " text.blur();");
    waitUntil(BrowserCondition.of("the tooltip to go away when the focus leaves",
      browser -> browser.executeScript(tooltip) instanceof String shown && shown.isEmpty()));
  }

  /**
   * Check that a row of a grid is selected
   *
   * @param gridId Grid identifier
   * @param search Text of the row
   */
  protected void checkRowSelected(String gridId, String search) {
    waitUntil(toBePresent(frontEndInstructions.findSelectedGridRow(gridId, search)));
  }

  /**
   * Sort a client-side grid with a click on the header of a column and measure how long it takes to render its rows:
   * from the click to the last change of the rows, with the layout of each change included
   *
   * @param gridId   Grid identifier
   * @param columnId Column identifier
   * @return Milliseconds
   */
  protected double measureGridSortRender(String gridId, String columnId) {
    By header = frontEndInstructions.getGridHeader(gridId, columnId);
    waitUntil(BrowserCondition.allOf(toBeVisible(header), checkIfGridLoaderIsNotVisible()));

    // The time starts with the click event and ends with the last change of the rows, once the page is quiet for 250 ms.
    // The click is a real one on the header, as the one of a user: the sort of some clients listens to an element inside it
    getBrowser().executeScriptOn(locator(header), "const root = document.querySelector('[grid-id=\"' + arguments[1] + '\"]');"
      + " const state = window.__gridRender = {done: false, ms: 0, start: null, quiet: null};"
      + " document.addEventListener('click', function () { state.start = performance.now(); }, {capture: true, once: true});"
      + " new MutationObserver(function (changes, observer) {"
      + "   if (state.start === null) { return; }"
      + "   void root.offsetHeight; state.ms = performance.now() - state.start;"
      + "   clearTimeout(state.quiet);"
      + "   state.quiet = setTimeout(function () { observer.disconnect(); state.done = true; }, 250);"
      + " }).observe(root, {childList: true, subtree: true, attributes: true, characterData: true});", gridId);
    click(header);
    waitUntil(BrowserCondition.of("the grid " + gridId + " to render the sort",
      browser -> browser.executeScript("return window.__gridRender.done;") instanceof Boolean done && done));
    Object ms = getBrowser().executeScript("return window.__gridRender.ms;");
    return ms instanceof Number number ? number.doubleValue() : Double.NaN;
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
   * Close a modal dialog with the close icon of its header, as a user dismissing it without using its buttons
   *
   * @param dialogId Dialog identifier
   */
  protected void closeDialog(String dialogId) {
    click(frontEndInstructions.getDialogClose(dialogId));
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
   * Drag the handle of a side of the closest resizable around an element, along the axis of that side
   *
   * @param elementId Identifier of an element inside the resizable
   * @param direction Side of the handle: top, right, bottom or left
   * @param offset    Pixels to drag: positive to the right or down, negative to the left or up
   */
  protected void dragResizable(String elementId, String direction, int offset) {
    boolean horizontal = "right".equals(direction) || "left".equals(direction);
    dragBy(frontEndInstructions.getResizableHandle(elementId, direction), horizontal ? offset : 0, horizontal ? 0 : offset);
  }

  /**
   * Get the box of the closest resizable around an element
   *
   * @param elementId Identifier of an element inside the resizable
   * @param direction Side of the handle of the resizable: top, right, bottom or left
   * @return Left, top, width and height in pixels
   */
  protected double[] getResizableBounds(String elementId, String direction) {
    return getBounds(frontEndInstructions.getResizable(elementId, direction));
  }

  /**
   * Get the box of the element next to the closest resizable around an element, on the side of its handle: the one that
   * gives or takes the size when the handle is dragged
   *
   * @param elementId Identifier of an element inside the resizable
   * @param direction Side of the handle of the resizable: top, right, bottom or left
   * @return Left, top, width and height in pixels
   */
  protected double[] getResizableNeighbourBounds(String elementId, String direction) {
    return getBounds(frontEndInstructions.getResizableNeighbour(elementId, direction, true));
  }

  /**
   * Get the box of the element next to the closest resizable around an element, on the side opposite to its handle
   *
   * @param elementId Identifier of an element inside the resizable
   * @param direction Side of the handle of the resizable: top, right, bottom or left
   * @return Left, top, width and height in pixels
   */
  protected double[] getResizableOppositeNeighbourBounds(String elementId, String direction) {
    return getBounds(frontEndInstructions.getResizableNeighbour(elementId, direction, false));
  }

  /**
   * Drag an element: press on its center, move the pointer by an offset and release it
   */
  private void dragBy(By selector, int dx, int dy) {
    waitUntil(toBeVisible(selector));
    getBrowser().drag(locator(selector), dx, dy);
  }

  /**
   * Get the box of an element in the viewport, as left, top, width and height in pixels
   */
  private double[] getBounds(By selector) {
    waitUntil(toBeVisible(selector));
    String box = String.valueOf(getBrowser().executeScriptOn(locator(selector), "var rect = arguments[0].getBoundingClientRect();"
      + " return rect.left + '|' + rect.top + '|' + rect.width + '|' + rect.height;"));
    return parseNumbers(box, 4);
  }

  /**
   * Check the width of an open dialog, give or take two pixels. It is the layout width, so the transition that opens the
   * dialog (it grows from a smaller scale) does not change it
   *
   * @param dialogId Dialog identifier
   * @param width    Expected width in pixels
   */
  protected void checkDialogWidth(String dialogId, int width) {
    By selector = frontEndInstructions.getDialogPanel(dialogId);
    waitUntil(toBeVisible(selector));
    Object measured = getBrowser().executeScriptOn(locator(selector), "return arguments[0].offsetWidth;");
    double shown = measured instanceof Number number ? number.doubleValue() : Double.NaN;
    assertWithScreenshot("The width of the dialog " + dialogId + " is " + measured + "px, not " + width + "px",
      Math.abs(shown - width) <= DIALOG_WIDTH_TOLERANCE);
  }

  /**
   * Close a side panel with the close button of its header
   *
   * @param panelId Side panel identifier
   */
  protected void closeSidePanel(String panelId) {
    click(frontEndInstructions.getSidePanelClose(panelId));
  }

  /**
   * Close an open side panel pressing Esc, and check that it has been closed
   *
   * @param panelId Side panel identifier
   */
  protected void closeSidePanelWithEscape(String panelId) {
    waitUntil(toBeVisible(frontEndInstructions.getOpenSidePanel(panelId)));
    getBrowser().press(Key.ESCAPE);
    checkSidePanelClosed(panelId);
  }

  /**
   * Check that a side panel has been closed
   *
   * @param panelId Side panel identifier
   */
  protected void checkSidePanelClosed(String panelId) {
    checkNotVisible(locator(frontEndInstructions.getOpenSidePanel(panelId)));
  }

  /**
   * Check that a side panel is open against an edge of the screen and as wide as its maximum width (or the screen, when
   * the screen is narrower), give or take two pixels. The browser resolves the maximum width, so a length in rem does
   * not depend on the font size of the theme. It waits for the panel to finish sliding in
   *
   * @param panelId  Side panel identifier
   * @param position Edge of the screen: left or right
   * @param maxWidth Maximum width of the side panel (a css length, as in its max-width attribute)
   */
  protected void checkSidePanelOpen(String panelId, String position, String maxWidth) {
    boolean left = "left".equals(position);
    if (!left && !"right".equals(position)) {
      throw new IllegalArgumentException("The position of a side panel is left or right, not " + position);
    }
    By selector = frontEndInstructions.getSidePanelPanel(panelId);
    waitUntil(toBeVisible(selector));
    waitUntil(BrowserCondition.of("the side panel " + panelId + " to be open on the " + position + " edge, " + maxWidth + " wide",
      browser -> isSidePanelPlaced(browser.executeScriptOn(locator(selector), SIDE_PANEL_PLACEMENT_SCRIPT, maxWidth), left)));
  }

  private static boolean isSidePanelPlaced(Object placement, boolean left) {
    if (!(placement instanceof String text)) {
      return false;
    }
    String[] values = text.split("\\|");
    try {
      double gap = Double.parseDouble(values[left ? 0 : 1]);
      double width = Double.parseDouble(values[2]);
      double maximum = Double.parseDouble(values[3]);
      return Math.abs(gap) <= SIDE_PANEL_TOLERANCE && Math.abs(width - maximum) <= SIDE_PANEL_TOLERANCE;
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException exc) {
      return false;
    }
  }

  /**
   * Press Tab several times on an open side panel and check that the focus never leaves it: after the last field it goes
   * back to the first one
   *
   * @param panelId Side panel identifier
   * @param presses Times to press Tab (more than the fields of the panel to go round it)
   */
  protected void checkSidePanelKeepsTheFocus(String panelId, int presses) {
    String panelCss = frontEndInstructions.getSidePanelCss(panelId);
    checkFocusInside(panelCss, "the side panel " + panelId);
    for (int press = 1; press <= presses; press++) {
      getBrowser().press(Key.TAB);
      checkFocusInside(panelCss, "the side panel " + panelId + " after " + press + " Tab");
    }
  }

  /**
   * Close a popover anchored to a control with the close button of its header
   *
   * @param popoverId Popover identifier
   */
  protected void closePopover(String popoverId) {
    click(frontEndInstructions.getPopoverClose(popoverId));
  }

  /**
   * Close an open popover pressing Esc, and check that it has been closed
   *
   * @param popoverId Popover identifier
   */
  protected void closePopoverWithEscape(String popoverId) {
    waitUntil(toBeVisible(frontEndInstructions.getOpenPopover(popoverId)));
    getBrowser().press(Key.ESCAPE);
    checkPopoverClosed(popoverId);
  }

  /**
   * Check that a popover anchored to a control has been closed
   *
   * @param popoverId Popover identifier
   */
  protected void checkPopoverClosed(String popoverId) {
    checkNotVisible(locator(frontEndInstructions.getOpenPopover(popoverId)));
  }

  /**
   * Check that a popover is open next to its anchor: on its side of the anchor (top, right, bottom or left), at most 16
   * pixels away from it and alongside it (they overlap on the other axis)
   *
   * @param popoverId Popover identifier
   * @param anchorId  Identifier of the control the popover is anchored to (its anchor attribute)
   * @param position  Side of the anchor: top, right, bottom or left (its position attribute)
   */
  protected void checkPopoverOpen(String popoverId, String anchorId, String position) {
    int side = POPOVER_POSITIONS.indexOf(position);
    if (side < 0) {
      throw new IllegalArgumentException("The position of a popover is top, right, bottom or left, not " + position);
    }
    By selector = frontEndInstructions.getOpenPopover(popoverId);
    waitUntil(toBeVisible(selector));
    waitUntil(BrowserCondition.of("the popover " + popoverId + " to be open at the " + position + " of " + anchorId,
      browser -> isPopoverPlaced(browser.executeScriptOn(locator(selector), POPOVER_PLACEMENT_SCRIPT, anchorId), side)));
  }

  private static boolean isPopoverPlaced(Object placement, int side) {
    if (!(placement instanceof String text)) {
      return false;
    }
    try {
      double[] edges = Arrays.stream(text.split("\\|")).mapToDouble(Double::parseDouble).toArray();
      // Edges: 0 top, 1 right, 2 bottom, 3 left of the popover and 4 to 7 of the anchor
      double gap = switch (side) {
        case 0 -> edges[4] - edges[2];
        case 1 -> edges[3] - edges[5];
        case 2 -> edges[0] - edges[6];
        default -> edges[7] - edges[1];
      };
      boolean vertical = side % 2 == 0;
      boolean alongside = vertical
        ? edges[3] < edges[5] && edges[1] > edges[7]
        : edges[0] < edges[6] && edges[2] > edges[4];
      return gap >= -POPOVER_TOLERANCE && gap <= POPOVER_MAX_GAP && alongside;
    } catch (NumberFormatException | ArrayIndexOutOfBoundsException exc) {
      return false;
    }
  }

  /**
   * Press Tab several times on an open popover and check that the focus never leaves it: after the last field it goes
   * back to the first one
   *
   * @param popoverId Popover identifier
   * @param presses   Times to press Tab (more than the fields of the popover to go round it)
   */
  protected void checkPopoverKeepsTheFocus(String popoverId, int presses) {
    String popoverCss = frontEndInstructions.getPopoverCss(popoverId);
    checkFocusInside(popoverCss, "the popover " + popoverId);
    for (int press = 1; press <= presses; press++) {
      getBrowser().press(Key.TAB);
      checkFocusInside(popoverCss, "the popover " + popoverId + " after " + press + " Tab");
    }
  }

  /**
   * Check the value of a card: its number and its unit, as the user reads them. The spaces between them do not count,
   * because the number and the unit are separate elements and each tool reads the space between them its own way
   *
   * @param cardId       Card identifier
   * @param expectedText Expected value, for instance {@code 412,600 €}
   */
  protected void checkCardValue(String cardId, String expectedText) {
    Locator value = locator(frontEndInstructions.getCardValue(cardId));
    String expected = withoutSpaces(expectedText);
    waitUntil(BrowserCondition.of("the value of the card " + cardId + " to be '" + expectedText + "'", browser -> {
      try {
        return browser.isVisible(value) && expected.equals(withoutSpaces(browser.text(value)));
      } catch (ElementNotFoundException | ElementReplacedException exc) {
        return false;
      }
    }));
  }

  /**
   * Check the trend delta of a card: its text (the number and its unit, for instance {@code +12.5%} or {@code −0.18 pt}
   * with the minus sign U+2212), its direction and its tone
   *
   * @param cardId       Card identifier
   * @param expectedText Expected text of the delta
   * @param trend        Expected direction: up, down or flat
   * @param tone         Expected tone: success, warning, danger, info or neutral
   */
  protected void checkCardDelta(String cardId, String expectedText, String trend, String tone) {
    Locator delta = locator(frontEndInstructions.getCardDelta(cardId));
    Locator text = locator(frontEndInstructions.getCardDeltaText(cardId));
    waitUntil(BrowserCondition.of("the delta of the card " + cardId + " to be '" + expectedText + "', " + trend + " and " + tone,
      browser -> {
        try {
          return browser.isVisible(delta) && expectedText.equals(browser.text(text).strip())
            && trend.equals(browser.attribute(delta, "data-trend")) && tone.equals(browser.attribute(delta, "data-tone"));
        } catch (ElementNotFoundException | ElementReplacedException exc) {
          return false;
        }
      }));
  }

  /**
   * Check the progress of a card towards its target: the percent its progress bar announces ({@code aria-valuenow})
   *
   * @param cardId  Card identifier
   * @param percent Expected percent (0 to 100)
   */
  protected void checkCardProgress(String cardId, int percent) {
    Locator progress = locator(frontEndInstructions.getCardProgress(cardId));
    String expected = String.valueOf(percent);
    waitUntil(BrowserCondition.of("the progress of the card " + cardId + " to be " + percent + " %", browser -> {
      try {
        return browser.isVisible(progress) && expected.equals(browser.attribute(progress, "aria-valuenow"));
      } catch (ElementNotFoundException | ElementReplacedException exc) {
        return false;
      }
    }));
  }

  /**
   * Check that a component (a criterion, a grid or a chart) is shown in the body of a card
   *
   * @param cardId      Card identifier
   * @param componentId Component identifier
   */
  protected void checkCardBodyComponent(String cardId, String componentId) {
    checkVisible(locator(frontEndInstructions.getCardBodyComponent(cardId, componentId)));
  }

  private static String withoutSpaces(String text) {
    return text == null ? "" : text.replaceAll("[\\s\\u00A0\\u202F]", "");
  }

  /**
   * Check that a button has the keyboard focus, for instance the one that opened a dialog once the dialog is closed
   *
   * @param buttonId Button identifier
   */
  protected void checkButtonFocused(String buttonId) {
    checkFocusInside(frontEndInstructions.getButtonCss(buttonId), "button " + buttonId);
  }

  private void checkFocusInside(String css, String description) {
    waitUntil(BrowserCondition.of("focus to be on " + description,
      browser -> Boolean.TRUE.equals(browser.executeScript(FOCUS_INSIDE_SCRIPT, css))));
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

  /**
   * Check that a tab criterion marks a tab as holding fields that failed the validation, or that it marks none
   *
   * @param tabId   Tab criterion identifier
   * @param invalid Expected state: true when a tab is marked invalid, false when none is
   */
  protected void checkTabInvalid(String tabId, boolean invalid) {
    checkMarkedInvalid(frontEndInstructions.getInvalidTab(tabId), invalid);
  }

  /**
   * Check that a wizard marks a step as holding fields that failed the validation, or that it marks none
   *
   * @param wizardId Wizard criterion identifier
   * @param invalid  Expected state: true when a step is marked invalid, false when none is
   */
  protected void checkWizardStepInvalid(String wizardId, boolean invalid) {
    checkMarkedInvalid(frontEndInstructions.getInvalidWizardStep(wizardId), invalid);
  }

  private void checkMarkedInvalid(By marked, boolean invalid) {
    BrowserCondition isMarked = BrowserCondition.present(locator(marked));
    waitUntil(invalid ? isMarked : BrowserCondition.not(isMarked));
  }

  /**
   * Check that the focus is on a criterion (the keyboard focus is on the control of the criterion)
   *
   * @param criterionName Criterion identifier
   */
  protected void checkCriterionFocused(String criterionName) {
    String criterionCss = frontEndInstructions.getCriterionCss(criterionName);
    waitUntil(BrowserCondition.of("focus to be on criterion " + criterionName,
      browser -> Boolean.TRUE.equals(browser.executeScript(FOCUS_INSIDE_SCRIPT, criterionCss))));
  }

  /**
   * Check the dates of a date range criterion
   *
   * @param criterionName Criterion identifier
   * @param from          Expected start date (empty when there is none)
   * @param to            Expected end date (empty when there is none)
   */
  protected void checkDateRange(String criterionName, String from, String to) {
    Locator fromInput = locator(frontEndInstructions.getDateRangeFrom(criterionName));
    Locator toInput = locator(frontEndInstructions.getDateRangeTo(criterionName));
    waitUntil(BrowserCondition.of("date range " + criterionName + " to be " + from + " - " + to,
      browser -> from.equals(valueOf(browser, fromInput)) && to.equals(valueOf(browser, toInput))));
  }

  private static String valueOf(BrowserDriver browser, Locator input) {
    String value = browser.attribute(input, "value");
    return value == null ? "" : value;
  }

  /**
   * Check that a date range criterion cannot be edited: its two dates are disabled
   *
   * @param criterionName Criterion identifier
   */
  protected void checkDateRangeDisabled(String criterionName) {
    Locator fromInput = locator(frontEndInstructions.getDateRangeFrom(criterionName));
    Locator toInput = locator(frontEndInstructions.getDateRangeTo(criterionName));
    waitUntil(BrowserCondition.of("date range " + criterionName + " to be disabled",
      browser -> browser.exists(fromInput) && !browser.isEnabled(fromInput) && browser.exists(toInput) && !browser.isEnabled(toInput)));
  }

  /**
   * Check how many quick presets a date range criterion offers
   *
   * @param criterionName Criterion identifier
   * @param count         Expected number of presets
   */
  protected void checkDateRangePresetCount(String criterionName, int count) {
    Locator presets = locator(frontEndInstructions.getDateRangePresets(criterionName));
    waitUntil(BrowserCondition.of("date range " + criterionName + " to offer " + count + " presets",
      browser -> browser.count(presets) == count));
  }

  /**
   * Click on a quick preset of a date range criterion
   *
   * @param criterionName Criterion identifier
   * @param presetCode    Preset code, such as 1M
   */
  protected void clickDateRangePreset(String criterionName, String presetCode) {
    clickSelector(frontEndInstructions.getDateRangePreset(criterionName, presetCode));
  }

  /**
   * Check that a quick preset of a date range criterion is the active one (the dates are the ones it set)
   *
   * @param criterionName Criterion identifier
   * @param presetCode    Preset code, such as 1M
   */
  protected void checkDateRangePresetActive(String criterionName, String presetCode) {
    checkVisible(locator(frontEndInstructions.getDateRangePresetActive(criterionName, presetCode)));
  }

  /**
   * Type the start date of a date range criterion and press Enter on it, which submits the screen
   *
   * @param criterionName Criterion identifier
   * @param date          Start date, in the format of the criterion
   */
  protected void submitDateRangeFrom(String criterionName, String date) {
    submitDate(locator(frontEndInstructions.getDateRangeFrom(criterionName)), date);
  }

  /**
   * Type the end date of a date range criterion and press Enter on it, which submits the screen
   *
   * @param criterionName Criterion identifier
   * @param date          End date, in the format of the criterion
   */
  protected void submitDateRangeTo(String criterionName, String date) {
    submitDate(locator(frontEndInstructions.getDateRangeTo(criterionName)), date);
  }

  /**
   * Replace the date of an input with a typed one and press Enter on it, without leaving the input first
   *
   * @param input Date input
   * @param date  Date to type
   */
  private void submitDate(Locator input, String date) {
    waitUntil(BrowserCondition.visible(input));
    getBrowser().clear(input);
    writeText(input, date);
    getBrowser().press(input, Key.ENTER);
  }

  /**
   * Clear the start date of a date range criterion
   *
   * @param criterionName Criterion identifier
   */
  protected void clearDateRangeFrom(String criterionName) {
    By fromInput = frontEndInstructions.getDateRangeFrom(criterionName);
    waitUntil(toBeVisible(fromInput));
    clearText(fromInput);
    moveMouseOutOfCriterion(frontEndInstructions.getCriterionCss(criterionName));
  }

  /**
   * Drop a file on the drop zone of an uploader criterion, as a user does when dragging it from the desktop
   *
   * @param criterionName Criterion identifier
   * @param fileName      Name of the file
   * @param mimeType      Media type of the file, such as image/png
   * @param base64Content Content of the file, base64 encoded
   */
  protected void dropFile(String criterionName, String fileName, String mimeType, String base64Content) {
    Locator dropZone = locator(frontEndInstructions.getUploaderDropZone(criterionName));
    waitUntil(BrowserCondition.visible(dropZone));
    getBrowser().executeScriptOn(dropZone, DROP_FILE_SCRIPT, fileName, mimeType, base64Content);
  }

  /**
   * Check that an uploader criterion offers the button that opens the camera, or that it does not
   *
   * @param criterionName Criterion identifier
   * @param shown         Expected state: true when the camera button is displayed, false when it is not
   */
  protected void checkUploadCaptureButton(String criterionName, boolean shown) {
    Locator capture = locator(frontEndInstructions.getUploaderCapture(criterionName));
    waitUntil(shown ? BrowserCondition.visible(capture) : BrowserCondition.invisible(capture));
  }

  /**
   * Choose a file with the camera input of an uploader criterion (the input that captures an image). Only the React
   * client renders that input: AngularJS creates it when the camera button is clicked
   *
   * @param criterionName Criterion identifier
   * @param filePath      Path of the file that the camera returns
   */
  protected void chooseCapturedFile(String criterionName, String filePath) {
    Locator captureInput = locator(frontEndInstructions.getUploaderCaptureInput(criterionName));
    waitUntil(BrowserCondition.present(captureInput));
    getBrowser().sendKeys(captureInput, filePath);
  }

  /**
   * Check the name of the file that an uploader criterion holds
   *
   * @param criterionName Criterion identifier
   * @param fileName      Expected name of the file (it may be part of what the criterion shows)
   */
  protected void checkUploadedFile(String criterionName, String fileName) {
    Locator filename = locator(frontEndInstructions.getUploaderFilename(criterionName));
    waitUntil(BrowserCondition.of("uploader " + criterionName + " to hold the file " + fileName,
      browser -> browser.exists(filename) && (String.valueOf(browser.text(filename)).contains(fileName)
        || valueOf(browser, filename).contains(fileName))));
  }

  /**
   * Check that an uploader criterion holds no file
   *
   * @param criterionName Criterion identifier
   */
  protected void checkNoUploadedFile(String criterionName) {
    Locator filename = locator(frontEndInstructions.getUploaderFilename(criterionName));
    waitUntil(BrowserCondition.of("uploader " + criterionName + " to hold no file",
      browser -> !browser.exists(filename) || (String.valueOf(browser.text(filename)).isBlank()
        && valueOf(browser, filename).isBlank())));
  }

  /**
   * Check that an uploader criterion shows the preview of the image that it holds (a blob address)
   *
   * @param criterionName Criterion identifier
   */
  protected void checkUploadPreview(String criterionName) {
    Locator preview = locator(frontEndInstructions.getUploaderPreview(criterionName));
    waitUntil(BrowserCondition.of("uploader " + criterionName + " to show an image preview",
      browser -> browser.isVisible(preview) && String.valueOf(browser.attribute(preview, "src")).startsWith("blob:")));
  }

  /**
   * Check that an uploader criterion shows no image preview
   *
   * @param criterionName Criterion identifier
   */
  protected void checkNoUploadPreview(String criterionName) {
    checkNotVisible(locator(frontEndInstructions.getUploaderPreview(criterionName)));
  }

  /**
   * Click on the button that clears the file of an uploader criterion
   *
   * @param criterionName Criterion identifier
   */
  protected void clearUpload(String criterionName) {
    clickSelector(frontEndInstructions.getUploaderClear(criterionName));
  }
}
