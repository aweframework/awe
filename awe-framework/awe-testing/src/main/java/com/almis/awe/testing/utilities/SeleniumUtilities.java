package com.almis.awe.testing.utilities;

import com.almis.awe.testing.config.AweTestConfigProperties;
import com.almis.awe.testing.config.TestConfig;
import com.almis.awe.testing.extensions.SeleniumExtension;
import com.almis.awe.testing.model.SeleniumModel;
import com.almis.awe.testing.selenium.IAweFrontEndInstructions;
import com.almis.awe.testing.selenium.IAweInstructions;
import com.almis.awe.testing.selenium.InstructionsFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.almis.awe.testing.constants.TestingConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.openqa.selenium.support.ui.ExpectedConditions.*;

/**
 * Utilities suite for selenium testing
 */
@Slf4j
@ExtendWith({SpringExtension.class, SeleniumExtension.class})
@ContextConfiguration(classes = TestConfig.class, initializers = ConfigDataApplicationContextInitializer.class)
public class SeleniumUtilities implements IAweInstructions {

  // Constants
  private static final Integer RETRY_COUNT = 10;
  private static final String TEXT_VALUE = " text: '";

  @Autowired
  private AweTestConfigProperties properties;
  private SeleniumModel seleniumModel;
  private IAweFrontEndInstructions frontEndInstructions;

  /**
   * Get driver
   *
   * @return Get driver
   */
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
   * Retrieve web element from selector
   *
   * @param selector Selector
   * @return Element found
   */
  private WebElement getElement(By selector) {
    return seleniumModel.getDriver().findElement(selector);
  }

  /**
   * Retrieve web element from selector
   *
   * @param selector Selector
   * @return Element found
   */
  private List<WebElement> getElements(By selector) {
    return seleniumModel.getDriver().findElements(selector);
  }

  /**
   * Wait for screen load
   */
  private void waitForLoad() {
    ExpectedCondition<Boolean> pageLoadCondition = driver1 -> ((JavascriptExecutor) Objects.requireNonNull(driver1)).executeScript("return document.readyState").equals("complete");
    waitUntil(pageLoadCondition);
  }

  /**
   * Wait until an expected condition
   *
   * @param condition Expected condition
   */
  private void waitUntil(ExpectedCondition<?> condition) {
    String message = condition.toString();
    try {
      new WebDriverWait(seleniumModel.getDriver(), properties.getTimeout()).until(condition);
      // Assert true on condition
      assertTrue(true, message);
      log.debug(message);
    } catch (Exception exc) {
      assertWithScreenshot(message, false, exc);
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
      File scrFile = ((TakesScreenshot) seleniumModel.getDriver()).getScreenshotAs(OutputType.FILE);
      String messageSanitized = TextUtilities.sanitizeMessage(message);
      String timestamp = new SimpleDateFormat("dd-MM-yyyy_hh-mm-ss").format(new Date());
      String screenshotName = String.format("%s-%s-[ERROR]-%s-%s", getClass().getSimpleName(), timestamp, seleniumModel.getCurrentOption(), messageSanitized);
      Path path = Paths.get(properties.getScreenshotPath(), screenshotName + ".png");
      log.error(message, (Object) throwable);
      log.error("Storing screenshot at: " + path);

      // Now you can do whatever you need to do with it, for example copy somewhere
      try {
        Files.createDirectories(path.getParent());
        FileUtils.copyFile(scrFile, path.toFile());
      } catch (IOException ioExc) {
        log.error("Error trying to store screenshot at: " + path, ioExc);
      }
    }

    // Assert false
    assertTrue(condition, message);
  }

  private boolean isWritable(By selector) {
    try {
      getElement(selector);
      return true;
    } catch (Exception exc) {
      return false;
    }
  }

  /**
   * Type keys on a criterion
   *
   * @param selector Criterion selector to type keys
   * @param text     Text to type
   */
  private void sendKeys(By selector, CharSequence... text) {
    String conditionMessage = "";
    try {
      WebElement element = getElement(selector);
      new Actions(seleniumModel.getDriver())
        .sendKeys(element, text)
        .pause(200)
        .perform();

      // Assert true on condition
      assertTrue(true, conditionMessage);
    } catch (Exception exc) {
      assertWithScreenshot("Sending keys to element: " + selector.toString() + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Clear text on criterion
   *
   * @param selector Criterion selector
   */
  private void clearText(By selector) {
    String textToClear = getElement(selector).getAttribute("value");
    if (!textToClear.isEmpty()) {
      getElement(selector).clear();
      getElement(selector).sendKeys(IntStream
        .range(-1, textToClear.length())
        .mapToObj(t -> Keys.BACK_SPACE)
        .toArray(CharSequence[]::new));
      waitForEmptyText(selector, textToClear);
    }
  }

  /**
   * Click on an element
   *
   * @param selector Element selector
   */
  private void moveTo(By selector) {
    moveTo(getElement(selector));
  }

  /**
   * Move to an element
   *
   * @param element Element
   */
  private void moveTo(WebElement element) {
    String conditionMessage = "";
    // Wait until element is clickable
    waitUntil(visibilityOf(element));

    // Click on element
    try {
      new Actions(seleniumModel.getDriver())
        .moveToElement(element)
        .pause(100)
        .perform();

      // Assert true on condition
      assertTrue(true, conditionMessage);
    } catch (Exception exc) {
      assertWithScreenshot("Moving over element: " + element.toString() + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Click on an element
   *
   * @param selector Element selector
   */
  private void click(By selector) {
    click(getElement(selector));
  }

  /**
   * Click on an element
   *
   * @param element Element
   */
  private void click(WebElement element) {
    String conditionMessage = "";
    // Wait until element is clickable
    waitUntil(elementToBeClickable(element));

    // Click on element
    try {
      new Actions(seleniumModel.getDriver())
        .moveToElement(element)
        .click(element)
        .pause(100)
        .perform();

      // Assert true on condition
      assertTrue(true, conditionMessage);
    } catch (Exception exc) {
      assertWithScreenshot("Clicking on element: " + element.toString() + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Double click on an element
   *
   * @param selector Element selector
   */
  private void doubleClick(By selector) {
    doubleClick(getElement(selector));
  }

  /**
   * Double click on an element
   *
   * @param element Element
   */
  private void doubleClick(WebElement element) {
    String conditionMessage = "";
    // Wait until element is clickable
    waitUntil(elementToBeClickable(element));

    // Click on element
    try {
      new Actions(seleniumModel.getDriver())
        .moveToElement(element)
        .click(element)
        .pause(50)
        .click(element)
        .pause(100)
        .perform();

      // Assert true on condition
      assertTrue(true, conditionMessage);
    } catch (Exception exc) {
      assertWithScreenshot("Clicking on element: " + element.toString() + "\n" + exc.getMessage(), false, exc);
    }
  }

  /**
   * Context menu on element
   *
   * @param selector Element selector
   */
  private void contextMenu(By selector) {
    String conditionMessage = "";

    // Wait until element is clickable
    waitUntil(elementToBeClickable(selector));

    // Click on element
    try {
      WebElement element = getElement(selector);
      new Actions(seleniumModel.getDriver())
        .moveToElement(element)
        .contextClick(element)
        .pause(100)
        .perform();
      assertTrue(true, conditionMessage);
    } catch (Exception exc) {
      assertWithScreenshot("Right clicking on element: " + selector.toString() + "\n" + exc.getMessage(), false, exc);
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
    checkVisible(frontEndInstructions.getDatepicker());

    // Write text on date
    By activeSelector = frontEndInstructions.getActiveDatepicker();
    writeTextFromSelector(frontEndInstructions.getCriterionInput(parentSelector), dateValue, true, activeSelector);

    // Make click twice if datepicker is still visible
    if (!seleniumModel.getDriver().findElements(activeSelector).isEmpty()) {
      clickSelector(activeSelector);
    }

    // Wait for not visible
    checkNotVisible(frontEndInstructions.getDatepicker());

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
    waitUntil(visibilityOfElementLocated(frontEndInstructions.getDatepicker()));

    // Click on selector
    click(frontEndInstructions.getCellFromDatepicker(type, search));

    // Wait for not visible
    checkNotVisible(frontEndInstructions.getDatepicker());
  }

  /**
   * Click on selector
   *
   * @param selector Selector
   */
  private void clickSelector(By selector) {
    // Wait for element visible
    waitUntil(visibilityOfElementLocated(selector));

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
  private ExpectedCondition<Boolean> checkIfLoaderIsNotVisible() {
    return invisibilityOfElementLocated(frontEndInstructions.getLoaderSelector());
  }

  /**
   * Checks if grid loader is not visible
   *
   * @return Condition for grid visibility
   */
  private ExpectedCondition<Boolean> checkIfGridLoaderIsNotVisible() {
    return invisibilityOfElementLocated(frontEndInstructions.getGridLoaderSelector());
  }

  /**
   * Click on row
   *
   * @param selector Row selector
   */
  private void clickRowFromSelector(By selector) {
    // Wait for element visible
    waitUntil(and(visibilityOfElementLocated(selector), checkIfGridLoaderIsNotVisible()));

    // Click button
    click(selector);
  }

  /**
   * Edit row
   *
   * @param selector Row selector
   */
  private void editRowFromSelector(By selector) {
    // Wait for element visible
    waitUntil(and(visibilityOfElementLocated(selector), checkIfGridLoaderIsNotVisible()));

    // Depending on behavior, do click or double click
    switch (frontEndInstructions.getRowEditBehavior()) {
      case DOUBLE_CLICK:
        // Click button
        doubleClick(selector);
        break;
      case SINGLE_CLICK:
      default:
        // Click button
        click(selector);
        break;
    }
  }

  /**
   * Click on row with a text
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  private void clickRowContentsFromSelector(String gridId, String search) {
    clickRowFromSelector(frontEndInstructions.findGridCell(gridId, search));
  }

  /**
   * Edit row with a text
   *
   * @param gridId Grid to search in
   * @param search Text to search
   */
  private void editRowContentsFromSelector(String gridId, String search) {
    editRowFromSelector(frontEndInstructions.findGridCell(gridId, search));
  }

  /**
   * Context menu on row
   *
   * @param selector Selector to apply
   */
  private void contextMenuFromSelector(By selector) {
    // Wait for element visible
    waitUntil(and(visibilityOfElementLocated(selector), checkIfGridLoaderIsNotVisible()));

    // Click button
    contextMenu(selector);
  }

  /**
   * Wait for selector to be clickable
   *
   * @param selector Selector to wait for
   */
  private void waitForSelector(By selector) {
    // Wait for element visible
    waitUntil(visibilityOfElementLocated(selector));

    // Move mouse again
    moveMouse();
  }

  /**
   * Move mouse to avoid help popovers
   */
  private void moveMouse() {
    By popoverSelector = frontEndInstructions.getPopover();
    try {
      // Safecheck
      int safecheck = 0;

      // Move mouse while help is being displayed
      List<WebElement> popovers = getElements(popoverSelector);
      while (!popovers.isEmpty() && safecheck < RETRY_COUNT) {
        new Actions(seleniumModel.getDriver())
          .pause(100)
          .moveToElement(popovers.get(0))
          .moveByOffset(0, 30)
          .pause(100)
          .build()
          .perform();

        popovers = getElements(popoverSelector);
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
      new Actions(seleniumModel.getDriver())
        .moveByOffset(0, -30)
        .click()
        .pause(100)
        .build()
        .perform();
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
    waitUntil(elementToBeClickable(clickSelector));
    clickSelector(clickSelector);
  }

  private void writeTextFromSelector(By selector, CharSequence text, boolean clearText) {
    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

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
    waitUntil(presenceOfElementLocated(selector));

    // Get selector text
    return getElement(selector).getAttribute("value");
  }

  /**
   * Click on a checkbox or a radio button
   *
   * @param parentSelector parent selector
   */
  private void clickCheckboxFromSelector(String parentSelector) {
    By selector = frontEndInstructions.getCheckbox(parentSelector);

    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

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
    waitUntil(invisibilityOfElementLocated(loaderSelector));

    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

    // Triple click selector
    click(selector);

    // Wait for element present
    waitUntil(presenceOfElementLocated(frontEndInstructions.getSuggestDropdownList()));
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
    waitUntil(invisibilityOfElementLocated(loaderSelector));

    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

    // Click selector
    click(selector);

    // Wait for element present
    waitUntil(presenceOfElementLocated(frontEndInstructions.getSelectDropdownList()));
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
    waitUntil(presenceOfElementLocated(suggestDropdownListInput));

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
    waitUntil(presenceOfElementLocated(suggestDropdownListInput));

    // Write username
    sendKeys(suggestDropdownListInput, search);

    // Wait for loading bar
    waitForLoadingBar();

    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

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

    // Wait for element present
    waitUntil(checkIfLoaderIsNotVisible());

    // Wait for element present
    waitUntil(presenceOfElementLocated(searchBox));

    // Clear selector
    if (clear) {
      By clearSelector = frontEndInstructions.getSuggestMultipleChoiceClose(parentSelector);
      while (!getElements(clearSelector).isEmpty() && safecheck < RETRY_COUNT) {
        click(clearSelector);
        safecheck++;
      }
    }

    // Write search text
    sendKeys(searchBox, search);

    // Wait for loading bar
    waitForLoadingBar();

    // Select result on list
    suggestResult(label);
  }

  /**
   * Click on save row and wait
   *
   * @param selector Save row selector
   */
  private void saveRowFromSelector(By selector) {
    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

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
    String nodeText = getElement(selector).getText();
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
    String nodeText = getElement(selector).getText();
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
    List<String> nodeValues = getElements(selector).stream().map(WebElement::getText).collect(Collectors.toList());
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
    String nodeText = getElement(selector).getText();
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
    String nodeText = getElement(selector).getAttribute("value");
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
      waitUntil(visibilityOfElementLocated(frontEndInstructions.getMenuOption(option)));

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

    // Wait for element not visible
    waitUntil(invisibilityOfElementLocated(frontEndInstructions.getMenuDropdown()));

    // Wait for loading bar
    waitForLoadingBar();
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
    List<WebElement> openedChildren = getElements(frontEndInstructions.getMenuOpenedChildren(option));

    if (optionNumber == options.length || openedChildren.isEmpty()) {
      // Click on screen
      click(frontEndInstructions.getMenuOption(option));
    }
  }

  /**
   * Wait for css selector
   *
   * @param cssSelector CSS Selector
   */
  protected By waitForCssSelector(String cssSelector) {
    By selector = By.cssSelector(cssSelector);

    // Wait for selector
    waitForSelector(selector);

    // Return selector
    return selector;
  }

  /**
   * Wait for loading bar to hide
   */
  protected void waitForLoadingBar() {
    waitUntil(invisibilityOfElementLocated(frontEndInstructions.getLoadingBar()));
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
    waitUntil(visibilityOfElementLocated(selector));
  }

  /**
   * Wait for text inside a tag with a CSS class
   *
   * @param selector Selector
   * @param contains Text to check
   */
  protected void waitForText(By selector, String contains) {
    // Wait for element visible
    waitUntil(textToBePresentInElementLocated(selector, contains));
  }

  /**
   * Wait for text inside a tag with a CSS class
   *
   * @param selector Selector
   * @param contains Text to check
   */
  protected void waitForValue(By selector, String contains) {
    // Wait for element visible
    waitUntil(textToBePresentInElementValue(selector, contains));
  }

  /**
   * Wait for no text in selector
   *
   * @param selector Selector
   * @param text     Text to check
   */
  protected void waitForEmptyText(By selector, String text) {
    // Wait for element visible
    waitUntil(not(textToBePresentInElementValue(selector, text)));
  }

  /**
   * Pause
   *
   * @param time Milliseconds
   */
  protected void pause(Integer time) {
    new Actions(seleniumModel.getDriver())
      .pause(time)
      .build()
      .perform();
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

      // Mouse over context button
      new Actions(seleniumModel.getDriver())
        .moveToElement(seleniumModel.getDriver().findElement(contextButtonSelector))
        .build()
        .perform();
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
    if (getDriver().findElement(tabSelector).isDisplayed()) {
      // Tab selector
      clickSelector(tabSelector);

      // Wait for tab active
      waitUntil(visibilityOfElementLocated(frontEndInstructions.getTabActive(tabName, tabLabel)));
    } else {
      // If not visible, click on tab menu, wait for dropdown and click on dropdown option
      clickSelector(frontEndInstructions.getTabMenu(tabName));

      // Wait for tab label
      clickSelector(frontEndInstructions.getTabMenuDropdownOption(tabName, tabLabel));

      // Wait for dropdown not visible
      waitUntil(invisibilityOfElementLocated(frontEndInstructions.getTabMenuDropdown(tabName)));
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
    waitUntil(visibilityOfElementLocated(frontEndInstructions.getTreeButton(gridId, rowId)));

    // Click on tree button
    clickSelector(frontEndInstructions.getTreeButton(gridId, rowId));

    // Check loader is not visible
    checkNotVisible(frontEndInstructions.getTreeButtonLoader());

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
    clickDateFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId));
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
    selectDateFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId), dateValue);
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
    selectFromDatepicker(frontEndInstructions.getParentCss(gridId, null, columnId), DAY, day.toString());
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
    selectFromDatepicker(frontEndInstructions.getParentCss(gridId, null, columnId), MONTH, month);
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
    selectFromDatepicker(frontEndInstructions.getParentCss(gridId, null, columnId), YEAR, year.toString());
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
    editRowFromSelector(frontEndInstructions.getGridCell(gridId, rowId, columnId));
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
  protected void writeTextOnDriver(By selector, CharSequence... text) {
    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

    // Write text
    getElement(selector).sendKeys(text);
  }

  /**
   * Write text on selector
   *
   * @param selector Selector
   * @param text     Text
   */
  protected void writeText(By selector, CharSequence text) {
    // Wait for element present
    waitUntil(presenceOfElementLocated(selector));

    // Write text
    sendKeys(selector, text);
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
    writeTextFromSelector(frontEndInstructions.getCriterionInput(frontEndInstructions.getCriterionCss(criterionName)), text, clearText);
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
    By selector = frontEndInstructions.getCriterionInput(frontEndInstructions.getParentCss(gridId, rowId, columnId));
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
   * Get selected row cell text
   *
   * @param gridId   Grid id
   * @param columnId Column id
   * @return Cell text
   */
  protected String getText(String gridId, String columnId) {
    return getTextFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId));
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
    selectFirstFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId));
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
    selectLastFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId));
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
    selectContainFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId), label);
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
    waitUntil(presenceOfElementLocated(selector));

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
    waitUntil(presenceOfElementLocated(selector));

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
    waitUntil(presenceOfElementLocated(selector));

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
    suggestFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId), search, label);
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
    suggestLastFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId), search);
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
    suggestMultipleFromSelector(frontEndInstructions.getParentCss(gridId, null, columnId), clear, search, label);
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
    JavascriptExecutor js = ((JavascriptExecutor) seleniumModel.getDriver());
    WebElement grid = seleniumModel.getDriver().findElement(frontEndInstructions.getGridScrollZone(gridId));
    js.executeScript("arguments[0].scrollTo(arguments[1], arguments[2]);", grid, horizontal, vertical);
  }

  /**
   * Show mouse
   */
  protected void showMouse() {
    JavascriptExecutor js = ((JavascriptExecutor) seleniumModel.getDriver());
    js.executeScript("let seleniumFollowerImg=document.createElement(\"span\");" +
      "seleniumFollowerImg.setAttribute('id', 'selenium_mouse');" +
      "seleniumFollowerImg.setAttribute('style', 'position: absolute; z-index: 99999999999; pointer-events: none; transition: all .1s ease, text-shadow .1s linear; -moz-transition: all .01s linear, text-shadow .1s linear; color: white;-webkit-text-stroke-width: 2px;-webkit-text-stroke-color: #000;');" +
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
    waitUntil(invisibilityOfElementLocated(By.id("confirm-accept")));
  }

  /**
   * Check a message box and close it
   *
   * @param messageType Message type (success (default), info, warning, danger)
   */
  protected void checkAndCloseMessage(String messageType) {
    By messageSelector = frontEndInstructions.getMessage(messageType);

    // Wait for message selector
    waitUntil(elementToBeClickable(messageSelector));

    // Click on message selector
    click(messageSelector);

    // Wait for element not present
    waitUntil(invisibilityOfElementLocated(messageSelector));

    // Wait for loading bar
    waitForLoadingBar();
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
    checkText(waitForCssSelector(cssSelector), text);
  }

  /**
   * Check text inside css selector
   *
   * @param cssSelector Selector to check
   * @param text        Text to compare
   */
  protected void checkTextContains(String cssSelector, String text) {
    // Check selector text
    checkTextContains(waitForCssSelector(cssSelector), text);
  }

  /**
   * heck if selector doesn't contain a text
   *
   * @param cssSelector Selector to check
   * @param text        Text to compare
   */
  protected void checkTextNotContains(String cssSelector, String text) {
    checkTextNotContains(waitForCssSelector(cssSelector), text);
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
      waitUntil(and(visibilityOfElementLocated(selector), checkIfGridLoaderIsNotVisible()));

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
    waitUntil(and(visibilityOfElementLocated(selector), checkIfGridLoaderIsNotVisible()));

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

    ExpectedCondition<Boolean> condition = and(invisibilityOfElementLocated(selector), checkIfGridLoaderIsNotVisible());

    // Assert element is not located
    assertWithScreenshot(condition.toString(), condition.apply(seleniumModel.getDriver()));
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
    waitUntil(presenceOfElementLocated(selector));

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
      .forEach(criterionName -> waitUntil(presenceOfElementLocated(frontEndInstructions.getCheckboxChecked(criterionName, isChecked))));
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
    waitUntil(visibilityOfElementLocated(selector));

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
    waitUntil(and(
      visibilityOfElementLocated(selector),
      invisibilityOfElementLocated(frontEndInstructions.getSuggestLoader(frontEndInstructions.getCriterionCss(criterionName)))));

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
    assertWithScreenshot("Number of elements doesn't match", number == getElements(frontEndInstructions.getSelectDropdownListElements()).size());
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
    waitUntil(visibilityOfElementLocated(selector));

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
    List<WebElement> messages = getElements(messageSelector);

    // Check there are no messages of messageType
    assertEquals(0, messages.size());
  }

  /**
   * Check element is present
   *
   * @param cssSelector CSS selector
   */
  protected void checkPresence(String cssSelector) {
    By selector = By.cssSelector(cssSelector);

    // Wait until visible
    waitUntil(presenceOfElementLocated(selector));
  }

  /**
   * Check element is visible
   *
   * @param cssSelector CSS selector
   */
  protected void checkVisible(String cssSelector) {
    checkVisible(By.cssSelector(cssSelector));
  }

  /**
   * Check element is visible
   *
   * @param selector Selector
   */
  protected void checkVisible(By selector) {
    // Wait until visible
    waitUntil(visibilityOfElementLocated(selector));
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
    checkNotVisible(By.cssSelector(cssSelector));
  }

  /**
   * Check element is not visible
   *
   * @param selector selector
   */
  protected void checkNotVisible(By selector) {
    // Wait until visible
    waitUntil(invisibilityOfElementLocated(selector));
  }

  /**
   * Starting point; Go to a determined url
   *
   * @param url Start url
   */
  protected void goToUrl(String url) {
    assertNotNull(seleniumModel.getDriver());
    seleniumModel.setCurrentOption("login");

    log.info("Launching tests with '{}' browser: {}'", properties.getBrowser(), seleniumModel.getBaseUrl());

    // Set driver timeout
    seleniumModel.getDriver().manage().timeouts().scriptTimeout(properties.getTimeout());

    // Open page in different browsers
    seleniumModel.getDriver().get(url);

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
    // Go to base URL
    goToUrl(seleniumModel.getBaseUrl());

    // Test title
    setTestTitle("Login test: Log into the application");

    // Wait for element present
    waitForButton("ButLogIn");

    // Write username
    writeText("cod_usr", username);

    // Write password
    writeText("pwd_usr", password);

    // Click button
    clickButton("ButLogIn", true);

    // Wait for element present
    waitForSelector(By.cssSelector(cssSelector));

    // Assertion
    checkText(cssSelector, checkText);
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
}
