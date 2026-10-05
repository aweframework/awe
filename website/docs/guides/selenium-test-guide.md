---
id: selenium-testing
title: Selenium Tests
sidebar_label: Selenium Tests
---

This document gives a basic insight on how to start developing *Selenium* tests for applications developed with AWE. Before starting test development, make sure to read the *Selenium Test Development Guide*, specially the *Optimization/Help tips* section. Basic aspects you should know before starting to develop *Selenium* tests, such as general configurations and integration with *Jenkins*, are not treated in this document.

All the contents of this document are explained in a way that it is assumed the reader already knows how to use the tools and commands concerning *Selenium*.

:::note Where to find what
- Writing a test: [Basic instructions](#basic-instructions), then the [step catalogue](#step-catalogue) to find the step you need.
- Something differs between the AngularJS and the React clients: [AngularJS and React clients](#angularjs-and-react-clients).
- A step or a hook is missing: [Adding a missing step](#adding-a-missing-step) and [Stable test hooks](#stable-test-hooks-data-testid).
- A test fails: [Running the suites and troubleshooting](#running-the-suites-and-troubleshooting).
- Details and examples per component: the *Criteria* and [Grid cells](#grid-cells) sections further down.
:::

:::tip
It is very important to comment all the tests. **Each block of commands** that is related to the interaction of a component **MUST start with a comment** that indicates what the test is trying to do.
:::

## Basic instructions

Currently our selenium tests definition are based on **Java WebDrivers**. 
These drivers allow the developer to launch a defined browser and actions 
over it to test the user interface.

To develop integration tests with WebDrivers on AWE, just follow the next steps:

- Import `awe-testing` package on your pom dependencies:

  ```xml
  <!-- Test dependencies -->
  <dependency>
    <groupId>com.almis.awe</groupId>
    <artifactId>awe-testing</artifactId>
    <scope>test</scope>
  </dependency>
  ```

- Define integration tests on pom:

  ```xml
  <!-- Spring boot -->
  <plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
      ...
      <execution>
        <id>pre-integration-test</id>
        <configuration>
          <wait>1000</wait>
          <maxAttempts>180</maxAttempts>
        </configuration>
        <goals>
          <goal>start</goal>
        </goals>
      </execution>
      <execution>
        <id>post-integration-test</id>
        <goals>
          <goal>stop</goal>
        </goals>
      </execution>
      ...
    </executions>
  </plugin>
  ```

- Define `failsafe` maven plugin to launch IT (integration) tests:

  ```xml
  <!-- Failsafe -->
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
      <execution>
        <id>integration-test</id>
        <goals>
          <goal>integration-test</goal>
        </goals>
      </execution>
    </executions>
  </plugin>
  ```

- And it's Done! Just launch it with maven:

  ```
  mvn verify
  ```

### Test definition

To generate a selenium test just extend the `SeleniumUtilities` class on your integration test classes (XxxxxXxxIT.class):

```java
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebsocketTestsIT extends SeleniumUtilities {

  /**
   * Log into the application
   * @throws Exception
   */
  @Test
  public void t000_loginTest() throws Exception {
    checkLogin("test", "test", "span.avatar-text", "Manager (test)");
  }

  /**
   * Log out from the application
   * @throws Exception
   */
  @Test
  public void t999_logoutTest() throws Exception {
    checkLogout(".slogan", "Almis Web Engine");
  }

  /**
   * Tests something
   * @throws Exception Error on test
   */
  @Test
  public void t001_myFirstTest() throws Exception {
    // Title
    setTestTitle("My first test");

    // Test things
    // ...
  }
}
```

### Set test title

Every test should start with a title, just to find where the test starts
in the log file:

```java
// Test title
setTestTitle("My first test");
```

### Log in and log out

We've created two methods to simplify the login and logout actions:

To login the application just call to `checkLogin` method with the following 
parameters:
- **user** - Username
- **password** - Password
- **userName** - Name that the application shows for the logged user

 ```java
 // Log in and check the name of the logged user
 checkLogin("test", "test", "Manager (test)");
 ```

 The step does not depend on the client (AngularJS or React): it knows where each one shows the user. There is also a
 form with a CSS selector and a text, `checkLogin("test", "test", "span.avatar-text", "Manager (test)")`, that ties the
 test to one rendering: prefer the one above.

 To logout the application just call the `checkLogout` method, which checks that the *signin screen* is shown:

 ```java
 // Log out
 checkLogout();
 ```

 If the application asks for a confirmation before logging out, use `checkLogoutWithConfirmation()`. To check that
 the application rejects some credentials, use `checkLoginRejected(user, password, messageType, title, message)`.

### Go to a new page

To go to a new page, you need to call the `gotoScreen` method with a list of 
option names corresponding to the menu options you need to click to go to 
the selected screen. i.e. To go to the `sites` screen you need to click first 
on the `tools` menu option: 

```java
// Go to sites screen
gotoScreen("tools", "sites");
```  

### Click on a button 

To click on a button just call the `clickButton` method with the button name as parameter:

```java
// Click "ButSnd" button
clickButton("ButSnd");
```

If the button makes a new screen to load, you can tell the method to wait after the button call:

```java
// Click "ButNew" button and wait the screen to load
clickButton("ButNew", true);
```

If the button is a search button, you can call the `searchAndWait` method to wait the grid to load
(if the button is named `ButSch`):

```java
// Search and wait
searchAndWait();
```

If the button name is another one, just set it as first argument:

```java
// Search and wait
searchAndWait("mySearchButton");
``` 

### Wait for actionable inputs and buttons

When a custom flow needs to wait until a criterion can safely receive input or until a button becomes clickable, `SeleniumUtilities` exposes two protected helpers for subclasses:

- `waitForInputActionability(String criterionName)`
- `waitForButtonClickability(String buttonName)`

These helpers are generic and can be reused in any UI flow that needs actionable inputs or clickable buttons.

```java
// Wait until a criterion can receive input
waitForInputActionability("cod_usr");

// Wait until a button is clickable
waitForButtonClickability("ButLogIn");
```

Use them when the UI renders the element early but a later frontend update is still enabling the control.

### Wait for a grid to load

To wait a grid to load, call the `waitForLoadingGrid` method:

```java
// Wait a grid to load
waitForLoadingGrid();
``` 

### Accept a confirm dialog

To confirm a dialog just call the `acceptConfirm` method:

```java
// Confirm a dialog
acceptConfirm();
``` 

### Check and close a message

When the application returns a message, you can verify and close it with the `checkAndCloseMessage` method:

```java
// Accept message
checkAndCloseMessage(messageType);
```

Where **messageType** depends on the message you want to verify. You have the following message types:
* **success** - green message, when all is ok.
* **warning** - yellow message, there is a warning.
* **info** - blue message, a notification to the user.
* **danger** - red message, the application has failed somehow.

### Combo: Click button, accept confirm dialog and close message

There are operations which are usually launched together. 
The combination of clicking a button (confirmation button), accepting a dialog
and closing a success message is one of them. We've created a functionality to do all these
actions in one simple method:

```java
// Store and confirm
clickButtonAndConfirm("ButCnf");
```

- Clicks on "ButCnf" button (or defined one)
- Clicks on accept confirm dialog
- Waits for a **success** message and closes it

## Step catalogue

Every step a test can call is a `protected` method of `SeleniumUtilities`, so a test class that extends it calls them
directly. This catalogue lists all of them, grouped by area, so you can scan it before writing a test. Parameter names
follow the Java signature: `criterionName` is the criterion identifier, `gridId`, `rowId` and `columnId` are the grid,
row and column identifiers, and `messageType` is `success`, `info`, `warning` or `danger`. The *Engine* column is empty when
the step behaves the same in the AngularJS and the React client; otherwise it says what differs (see
[AngularJS and React clients](#angularjs-and-react-clients)).

A step that is not listed here does not exist: add it (see [Adding a missing step](#adding-a-missing-step)) instead of
writing a selector in your test. This list is checked by a unit test of `awe-testing` (`SeleniumTestGuideSyncTest`): the
build fails if a public or protected, non-deprecated method of `SeleniumUtilities` is not mentioned in this guide.

:::note Deprecated steps
No step is deprecated at this moment. A step that is replaced is marked `@Deprecated` in the source and kept through the
whole 5.x line (see [API compatibility](#api-compatibility-of-awe-testing)), and its row here says what replaces it.
:::

### Session and navigation

| Step | What it does |
|---|---|
| `goToUrl(String url)` | Opens a URL: the starting point of a suite |
| `setTestTitle(String title)` | Writes the title of the test in the log. Start every test with it |
| `checkLogin(String username, String password, String userName)` | Logs in and checks the name the application shows for the logged user. Preferred form |
| `checkLogin(String username, String password, String cssSelector, String checkText)` | Logs in and checks a text inside a CSS selector. Raw-selector form: prefer the three-argument one |
| `checkLoginRejected(String username, String password, String messageType, String title, String message)` | Tries to log in with credentials that the application rejects and checks the message it shows |
| `checkLogout()` | Logs out and checks that the login screen is shown. Preferred form |
| `checkLogout(String cssSelector, String checkText)` | Logs out and checks a text inside a CSS selector. Raw-selector form: prefer `checkLogout()` |
| `checkLogoutWithConfirmation()` | Logs out of an application that asks for a confirmation first, accepts it and checks the login screen |
| `gotoScreen(String... menuOptions)` | Clicks the menu options in order to open a screen, and waits for the menu to take effect |
| `waitForMenuOption(String option)` | Waits until the click on a menu option has taken effect. `gotoScreen` already calls it |
| `checkMenuOption(String option, String text)` | Checks that a menu option is visible and contains a text |
| `selectModule(String moduleName)` | Opens the settings menu (`ButSetTog`) and selects a module in the `module` selector |
| `broadcastMessageToUser(String user, String text)` | Goes to the `tools > broadcast-messages` screen of the AWE test applications, sends a text to a user and closes the `success` and `info` messages |
| `invalidateSession()` | Invalidates the session of the user from another window, as if it had been closed on the server |

### Waiting and pausing

| Step | What it does |
|---|---|
| `waitForInputActionability(String criterionName)` | Waits until a criterion can receive input |
| `waitForButtonClickability(String buttonName)` | Waits until a button can be clicked |
| `waitForButton(String buttonName)` | Waits until a button is clickable |
| `waitForTab(String tabCriterionName)` | Waits until a tab criterion is clickable |
| `waitForContextButton(String buttonName)` | Waits until a context menu button is clickable |
| `waitForLoadingBar()` | Waits for the loading bar of the page to disappear |
| `waitForLoadingGrid()` | Waits for the loading of a grid to finish |
| `waitForText(String clazz, String contains)` | Waits for a text inside the tag with a CSS class. Raw-selector form |
| `waitForText(By selector, String contains)` | Waits for a text inside a selector. Selenium type: only for helpers of your product |
| `waitForValue(By selector, String contains)` | Waits until the value of an input contains a text. Selenium type: only for helpers of your product |
| `waitForEmptyText(By selector, String text)` | Waits until the value of an input no longer contains a text. Selenium type: only for helpers of your product |
| `waitForCssSelector(String cssSelector)` | Waits for a CSS selector and returns it as a `By`. Raw-selector form |
| `pause(Integer time)` | Sleeps for a number of milliseconds. Use it only when there is no state to wait for |
| `showMouse()` | Shows a pointer that follows the real mouse, a visual aid for videos and screenshots |

### Buttons, menus and info

| Step | What it does |
|---|---|
| `clickButton(String buttonName)` | Clicks a button |
| `clickButton(String buttonName, boolean waitForLoadingBar)` | Clicks a button and, when `true`, waits for the screen to load |
| `clickButtonAndConfirm(String button)` | Clicks a button, accepts the confirm dialog and closes the `success` message |
| `clickButtonAndConfirm(String button, String messageType)` | Same, closing a message of another type |
| `checkButtonVisible(String buttonId)` | Checks that a button is displayed, whatever its state |
| `checkButtonNotVisible(String buttonId)` | Checks that a button is not displayed |
| `checkButtonDisabled(String buttonId)` | Checks that a button is displayed and disabled |
| `clickInfoButton(String infoButtonName)` | Clicks an info button (the user menu, the module list...) |
| `clickContextButton(String... contextButtonOptionList)` | Clicks the options of a context menu in order |
| `closeContextMenu()` | Closes the open context menu without choosing an option |
| `checkContextMenuNotVisible()` | Checks that no context menu is displayed |
| `searchAndWait()` | Clicks `ButSch` and waits for the grid to load |
| `searchAndWait(String buttonName)` | Same with another search button |

### Messages and dialogs

| Step | What it does |
|---|---|
| `acceptConfirm()` | Accepts the confirm dialog and waits for it to disappear |
| `checkAndCloseMessage(String messageType)` | Checks that a message of the type is shown and closes it |
| `closeMessages(String messageType, int count)` | Closes the messages of a stack one by one. A screen with several messages (one per failed query) keeps its controls blocked until all of them are closed |
| `checkMessageTitle(String messageType, String text)` | Checks the title of a message |
| `checkMessageText(String messageType, String text)` | Checks the text of a message |
| `checkMessageMissing(String messageType)` | Checks that no message of the type is shown |
| `checkDialogClosed(String dialogId)` | Checks that a modal dialog has been closed |
| `checkValidationErrorVisible()` | Checks that the screen shows validation errors |

### Criteria: writing and reading

| Step | What it does |
|---|---|
| `writeText(String criterionName, CharSequence text)` | Types a text in a criterion, clearing it first |
| `writeText(String criterionName, CharSequence text, boolean clearText)` | Same, choosing whether to clear the criterion first |
| `writeText(By selector, CharSequence text)` | Types a text in a selector. Selenium type: only for helpers of your product |
| `writeTextOnDriver(By selector, CharSequence... text)` | Sends keys to a selector as the driver would. Selenium type: only for helpers of your product |
| `clearText(String cssSelector)` | Clears the text of the input found by a CSS selector. Raw-selector form |
| `getText(String criterionName)` | Returns the text of a criterion |
| `checkCriterionContents(String criterionName, String search)` | Checks the value of a text criterion |
| `checkCriterionLabel(String criterionName, String text)` | Checks the label of a criterion |
| `checkCriterionUnit(String criterionName, String text)` | Checks the unit addon of a criterion (the text after the input) |
| `checkCheckboxRadio(boolean isChecked, String... criteriaNames)` | Checks that criteria are checked (or not) |
| `clickCheckbox(String criterionName)` | Clicks a checkbox or radio button |
| `clickCheckboxOption(String criterionName, String optionId)` | Clicks one option of a button group (a button checkbox or radio drawn as one group) |

### Criteria: dates

| Step | What it does |
|---|---|
| `clickDate(String criterionName)` | Opens the datepicker of a criterion |
| `selectDate(String dateName, CharSequence dateValue)` | Picks a full date |
| `selectDay(String dateName, Integer day)` | Picks a day of the current month |
| `selectMonth(String dateName, String month)` | Picks a month |
| `selectYear(String dateName, Integer year)` | Picks a year |
| `clickEnabledDatepickerDay()` | Clicks a day of the open datepicker that can be picked |
| `getTodayDay()` | Returns today's day of the month |
| `getTomorrowDay()` | Returns tomorrow's day of the month |

### Criteria: select and suggest

| Step | What it does |
|---|---|
| `selectFirst(String criterionName)` | Picks the first value of a select |
| `selectLast(String criterionName)` | Picks the last value of a select |
| `selectContain(String criterionName, String label)` | Picks the value of a select whose label contains a text |
| `selectResult(String match)` | Picks a result of the open select list |
| `suggest(String criterionName, String search, String label)` | Types a search in a suggest and picks the result whose label contains a text |
| `suggestLast(String criterionName, String search)` | Types a search in a suggest and picks the last result |
| `suggestResult(String match)` | Picks a result of the open suggest list |
| `suggestMultiple(String criterionName, String search, String label)` | Adds a value to a multiple select or suggest |
| `suggestMultiple(String criterionName, boolean clear, String search, String label)` | Same, clearing the chosen values first |
| `suggestMultipleList(String criterionName, String... items)` | Adds several values to a multiple select or suggest |
| `openSuggest(String criterionName)` | Opens the list of a select or suggest |
| `writeSuggestSearch(String criterionName, CharSequence text)` | Types in the search box of the open list |
| `suggestReplacingSearch(String criterionName, String search1, String search2, String match, Integer pause)` | Types a search, replaces it with another while the first is still loading and picks a result |
| `suggestMultipleReplacingSearch(String criterionName, String search1, String search2, String match, Integer pause)` | Same, for a multiple suggest |
| `checkSelectContents(String criterionName, String search)` | Checks the chosen value of a **select** |
| `checkSuggestContents(String criterionName, String search)` | Checks the chosen value of a **suggest**. The client shows it as text (AngularJS) or in an input (React): the step knows which |
| `checkMultipleSelectorContents(String criterionName, String search)` | Checks a chosen value of a multiple select or suggest |
| `checkSelectNumberOfResults(String criterionName, Integer number)` | Opens a select and checks how many results it lists |
| `checkSuggestResultCount(int expected)` | Checks how many results the open select or suggest list shows |

Use `checkSelectContents` for a select and `checkSuggestContents` for a suggest: a suggest is not rendered like a select.

### Grids: rows, cells and columns

The steps on a cell come in two forms: with a `rowId` (the cell of a given row) and without it (the cell of the row being
edited). The table lists one form and says "(also with `rowId`)" when the other exists.

| Step | What it does |
|---|---|
| `clickRowContents(String search)` | Selects the row that contains a text |
| `clickRowContents(String gridId, String search)` | Same in a given grid. A row that is already selected stays selected |
| `toggleRowContents(String gridId, String search)` | Clicks a row even if it is already selected, to toggle its selection |
| `editRow(String search)` | Starts the edition of the row that contains a text |
| `editRow(String gridId, String search)` | Same in a given grid |
| `editRow(String gridId, String rowId, String columnId)` | Starts the edition of a row by clicking one of its cells |
| `clickCell(String gridId, String columnId)` | Clicks a cell of the selected row |
| `clickCell(String gridId, String rowId, String columnId)` | Clicks a cell |
| `contextMenuRowContents(String search)` | Opens the context menu of the row that contains a text |
| `contextMenuRowContents(String gridId, String search)` | Same in a given grid |
| `contextMenu(String gridId, String rowId, String columnId)` | Opens the context menu of a cell |
| `selectAllRowsOfGrid(String gridId)` | Selects every row with the checkbox of the header |
| `checkAllRowsSelected(String gridId)` | Checks that every row of a grid is selected |
| `clickGridViewport(String gridId)` | Clicks the area of the grid that shows the rows, outside any row |
| `sortGrid(String gridId, String columnId)` | Clicks a column header to sort |
| `scrollGrid(String gridId, int horizontal, int vertical)` | Scrolls the grid by pixels |
| `saveRow()` / `saveRow(String gridId)` | Clicks the save button of an editable grid and waits |
| `getText(String gridId, String columnId)` | Returns the text of the editor of a cell of the row being edited |
| `getText(String gridId, String rowId, String columnId)` | Returns the text of a cell |
| `writeText(String gridId, String columnId, CharSequence text)` | Types in the editor of a cell of the row being edited |
| `writeText(String gridId, String rowId, String columnId, CharSequence text)` | Types in the editor of a cell |
| `writeText(String gridId, String rowId, String columnId, CharSequence text, boolean clearText)` | Same, choosing whether to clear first |
| `clickCheckbox(String gridId, String columnId)` | Clicks the checkbox or radio of a cell of the row being edited |
| `clickCheckbox(String gridId, String rowId, String columnId)` | Clicks the checkbox or radio of a cell |
| `clickDate(String gridId, String columnId)` | Opens the datepicker of a cell (also with `rowId`) |
| `selectDate(String gridId, String columnId, CharSequence dateValue)` | Picks a date in a cell (also with `rowId`) |
| `selectDay(String gridId, String columnId, Integer day)` | Picks a day of the current month in a cell (also with `rowId`) |
| `selectMonth(String gridId, String columnId, String month)` | Picks a month in a cell (also with `rowId`) |
| `selectYear(String gridId, String columnId, Integer year)` | Picks a year in a cell (also with `rowId`) |
| `selectFirst(String gridId, String columnId)` | Picks the first value of a select cell (also with `rowId`) |
| `selectLast(String gridId, String columnId)` | Picks the last value of a select cell (also with `rowId`) |
| `selectContain(String gridId, String columnId, String label)` | Picks a value in a select cell (also with `rowId`) |
| `suggest(String gridId, String columnId, String search, String label)` | Suggests in a cell (also with `rowId`) |
| `suggestLast(String gridId, String columnId, String search)` | Suggests the last result in a cell (also with `rowId`) |
| `suggestMultiple(String gridId, String columnId, String search, String label)` | Adds a value to a multiple cell (also with `rowId`, and with `clear`) |

### Grids: checks

| Step | What it does |
|---|---|
| `checkRowContents(String... searchList)` | Checks that the grid contains every text |
| `checkRowContentsGrid(String gridId, String... searchList)` | Same in a given grid |
| `checkRowNotContains(String search)` | Checks that the grid does not contain a text |
| `checkCellContents(String gridId, String rowId, String columnId, String search)` | Checks the content of a cell |
| `checkGridPresent(String gridId)` | Checks that a grid exists in the screen, even if it is hidden |
| `checkGridNotVisible(String gridId)` | Checks that a grid is not displayed |
| `checkGridPageSize(String size)` | Checks the number of rows that a grid shows in each page |
| `checkGridIconVisible(String gridId, String columnId, String icon)` | Checks that the icon of an icon column is displayed (`plus`, without the library prefix) |
| `checkColumnSuccessIcon(String columnId)` | Checks that a column shows a success icon |

### Tree grids

| Step | What it does |
|---|---|
| `clickTreeButton(String gridId, String rowId)` | Expands or collapses a row |
| `checkTreeRowVisible(String gridId, String rowId)` | Checks that a row is displayed |
| `checkTreeRowNotVisible(String gridId, String rowId)` | Checks that a row is not displayed |
| `checkTreeRowDeleted(String gridId, String rowId)` | Checks that a row is displayed as deleted (marked to be deleted on save) |
| `checkTreeIconVisible(String gridId, String rowId)` | Checks that the expand/collapse icon of a row is displayed |
| `checkTreeIconNotVisible(String gridId, String rowId)` | Checks that the expand/collapse icon of a row is not displayed |

### Tabs, wizards, charts, tag lists and frames

| Step | What it does |
|---|---|
| `clickTab(String tabName, String tabLabel)` | Clicks a tab of a tab criterion. The label is the locale key (AngularJS) or the translated text (React) |
| `checkActiveWizardStep(String number)` | Checks the number of the active step of a wizard |
| `checkTagListContains(String tagListId, String text)` | Checks that a tag list (text view) contains a text |
| `checkChartVisible(String chartId)` | Checks that a chart is displayed (the chart has been drawn) |
| `checkLogViewerContains(String text)` | Waits until the log viewer shows a text |
| `checkTextInEmbeddedFrame(String contentCssSelector, String text)` | Checks a text inside the frame that embeds an external application. The selector belongs to that application, so it is the one literal you may pass |

### Raw checks

These steps take a CSS selector. They exist for the helpers of your product and for components AWE does not know, not for
test classes: see [Keeping browser tests free of selectors](#keeping-browser-tests-free-of-selectors).

| Step | What it does |
|---|---|
| `click(String cssSelector)` | Clicks an element |
| `checkText(String cssSelector, String text)` | Checks the whole text of an element |
| `checkTextContains(String cssSelector, String text)` | Checks that the text of an element contains a text |
| `checkTextNotContains(String cssSelector, String text)` | Checks that the text of an element does not contain a text |
| `checkPresence(String cssSelector)` | Checks that an element is present |
| `checkVisible(String cssSelector)` | Checks that an element is visible (also with a `By`) |
| `checkNotVisible(String cssSelector)` | Checks that an element is not visible (also with a `By`) |
| `checkVisibleAndContains(String cssSelector, String search)` | Checks that an element is visible and contains a text |

## API compatibility of `awe-testing`

Products extend `SeleniumUtilities` and compile against `awe-testing`, so its **public and protected API is a compatibility
contract**: a new `awe-testing` release must not break a test suite that compiled against the previous one. Adding members
or deprecating them (`@Deprecated`) is allowed; removing or changing the signature of a public or protected member is not.

The contract is enforced by the build. The `awe-testing` module runs [japicmp](https://siom79.github.io/japicmp/) in the
`verify` phase and compares the freshly built jar with the last published release, set by the
`awe-testing.api-baseline.version` property in `awe-framework/awe-testing/pom.xml`. The build fails on any binary or source
incompatible change of a public or protected member of an `awe-testing` class. The report is written to
`awe-framework/awe-testing/target/japicmp/`.

The check only sees the `awe-testing` classes: their third-party supertypes (Selenium, JUnit, Spring) are not on the
comparison classpath. A change that comes from those libraries (for example a Selenium upgrade that changes a type used
in a signature) is not detected by this check; it shows up when the product's suite is compiled.

If a break is intentional, document it in the changelog and add a narrow, commented exclusion in that pom (never a blanket
ignore). After each release of `develop` (5.x), bump the baseline property to the version just released, so the next
release is checked against it (see the release steps in `CONTRIBUTING.md`). To run the check on its own:

```
mvn verify -pl awe-framework/awe-testing -DskipTests
```

Use `-Djapicmp.skip=true` to skip it locally (for example when you are offline and the baseline is not in your local repository).

## Stable test hooks (`data-testid`)

AWE components expose a small, fixed set of `data-testid` attributes. They name the **part** of a component, never an
instance, so a test keeps working when AWE replaces the library that renders the component (select2, datepicker,
ui-grid, Bootstrap...). Prefer these hooks to library classes (`.select2-*`, `.datepicker-*`, `.ui-grid-*`, `.modal`,
`.nav-tabs`, `.fa-*`...) in your own selectors.

The instance is always identified with the attribute AWE already renders: `criterion-id` on a criterion container,
`grid-id` / `tree-grid-id`, `row-id` and `column-id` in grids, `option-id` in a context menu, `name` in the application
menu, `info-dropdown-id`, `dialog-id` and the `id` of buttons and panes. Some libraries append their DOM to the end of the
`<body>` (the select dropdown, the datepicker popup, popovers). Those elements carry `data-testid-owner="<component id>"`
so a test can tell which component opened them.

### Criteria

| `data-testid` | Element | Where to find it |
|---|---|---|
| `criterion-input` | The real control of the criterion: the `input`, `textarea`, checkbox, radio or hidden input, the value of a text view, or the file chooser of an uploader. In a select or suggest it is the hidden input that holds the value: interact with `select`. The editors of the cells of an editable grid carry it too | Inside `[criterion-id='X']`, or inside the grid cell |
| `select` | Visible select2 container of a select or suggest (the element to click) | Inside `[criterion-id='X']` |
| `select-value` | Chosen value of a single select | Inside `[criterion-id='X']` |
| `select-choice`, `select-choice-close` | One chosen item of a multiple select or suggest, and the link that removes it | Inside `[criterion-id='X']` |
| `select-search` | Search input (the one in the dropdown for a single select, the one in the container for a multiple select) | Dropdown, or inside `[criterion-id='X']` |
| `select-dropdown` | The open dropdown. Only the open dropdown carries the hook | End of `<body>`, with `data-testid-owner` |
| `select-option` | One option of the open dropdown | Inside the dropdown, with `data-testid-owner` |
| `datepicker` | The open date popup | End of `<body>`, with `data-testid-owner` |
| `datepicker-day`, `datepicker-month`, `datepicker-year` | Day, month and year cells of the popup | Inside the popup, with `data-testid-owner` |
| `upload-filename`, `upload-clear` | Name of the uploaded file and the button that clears it | Inside `[criterion-id='X']`, or inside the grid cell |
| `loader` | Loader of a criterion or any other component | Inside the component |
| `select-trigger` | The arrow that opens the panel of a select. Click it rather than the middle of a short select, where the clear icon can be. **React only** | Inside `[criterion-id='X']` |
| `criterion-unit` | The unit addon of a criterion (the text after the input, such as `EUR`). **React only** | Inside `[criterion-id='X']` |
| `criterion-error` | The validation error of a criterion (AngularJS draws it in the shared error container). **React only** | Inside `[criterion-id='X']` |

In a button checkbox or radio the React client renders **one criterion for the whole group**, and every option is a
`criterion-input` that carries `option-id` with its value: `[criterion-id='X'] [data-testid='criterion-input'][option-id='Y']`.
AngularJS renders every option as a criterion of its own. Use `clickCheckboxOption(criterionName, optionId)`.

### Grids and trees

| `data-testid` | Element | Where to find it |
|---|---|---|
| `grid` | Root of a grid or tree grid | `[grid-id='X']`, `[tree-grid-id='X']` |
| `grid-header-cell` | Header cell of a column | Inside the grid, with `column-id` |
| `grid-header-checkbox` | Label of the "select all" checkbox. `data-selected` | Inside the grid |
| `grid-viewport` | Scrollable zone of the rows. `data-container` is `body`, `left` or `right` (frozen columns) | Inside the grid |
| `grid-row` | A row, with `row-id`. `data-selected` | Inside the viewport |
| `grid-cell` | A cell: the element that carries `column-id`, whatever it renders (value, editor, checkbox, tree icon) | Inside a row, with `column-id` |
| `grid-row-checkbox` | Label of the selection checkbox of a row. `data-selected` | Inside the cell |
| `grid-row-save`, `grid-row-cancel` | Save and cancel buttons of an editable grid | Inside the grid |
| `grid-pagination`, `grid-page-previous`, `grid-page-next`, `grid-goto-page`, `grid-page-size` | Footer pagination, its previous/next arrows (`data-disabled`) of the compact pager, the "go to page" input (AngularJS only) and the page size select. In React the page size also carries `data-value`, because the dropdown repeats the text | Inside the grid |
| `grid-loader` | Loader of the grid (also the pivot table) | Inside the grid |
| `tree-icon` | Expand/collapse icon of a tree row. `data-expanded` and `data-loading` | Inside the cell |
| `tree-header-icon` | Expand/collapse all icon of the header. AngularJS only | Inside the grid |
| `column-icon` | The icon of an icon column. `data-icon` carries the icon of the cell value (for instance `fa-plus` in a multioperation grid) | Inside the cell |
| `grid-row-edit` | The button that starts the edition of a row. **React only** | Inside the row |

### Tabs and wizards

| `data-testid` | Element | Where to find it |
|---|---|---|
| `tab-list` | The tab headers of a tab criterion. `data-disabled` | Inside `[criterion-id='X']` |
| `tab`, `tab-link`, `tab-label` | A tab header (`li` with the `tab-<value>` id and `data-active`), the link to click and the label | Inside the tab list, or inside the `tabdrop-menu` when the tab does not fit |
| `tab-pane` | Content of a tab, with the pane `id`. `data-active` | Inside `[criterion-id='X']` |
| `tabdrop`, `tabdrop-toggle`, `tabdrop-menu` | The "more" dropdown that holds the tabs that do not fit, the button that opens it and its menu. AngularJS only: the React tab list has no "more" menu | Inside the tab list |
| `wizard-step`, `wizard-pane` | A step header (`data-active` and `data-completed`) and a content pane (`data-active`) | Inside `[criterion-id='X']` |
| `wizard-step-number` | The number shown by a step. **React only**: the step shows an icon instead of the number when it has one, and then `data-step-number` on the `wizard-step` carries the number | Inside the step |

### Buttons, menus and info

| `data-testid` | Element | Where to find it |
|---|---|---|
| `button` | The `<button>` of a button, with the button `id` (also inside grid cells) | Anywhere |
| `context-menu`, `context-menu-option`, `context-menu-link`, `context-submenu` | A context menu, an option (with `option-id`), its link (`data-disabled`) and a nested menu | Inside the component that owns the menu |
| `menu`, `menu-option`, `menu-link`, `menu-dropdown`, `menu-submenu` | The application menu, an option (`data-active`, `data-open`), its link (with `name`), the first level dropdown and the nested submenus (`data-open`) | Inside the menu |
| `info-dropdown`, `info-dropdown-toggle`, `info-dropdown-menu` | An info dropdown (with `info-dropdown-id`), the link that opens it and its menu | Anywhere |
| `info-button`, `info-button-link` | An info button and its link (`info-button-link`: AngularJS only) | Anywhere |
| `avatar`, `avatar-name` | The avatar of the logged user in the header (it carries the id and the user name as title) and the name next to it. **React only** | Header |

### Charts, tag lists and logs

| `data-testid` | Element | Where to find it |
|---|---|---|
| `chart` | A chart. It is identified by `chart-id`, and `data-rendered` is `true` once the chart has been drawn: wait on it. **React only** (AngularJS: the `svg` of `[chart-id='X']`) | Inside the screen |
| `tag-list` | A tag list (text view), identified by `tag-list-id`. **React only** (AngularJS: `[awe-tag-list='X']`) | Inside the screen |
| `log-viewer` | The element that holds the text of the log viewer | Inside the screen |

Use `checkChartVisible(chartId)`, `checkTagListContains(tagListId, text)` and `checkLogViewerContains(text)` instead of
these selectors.

### Messages, dialogs and loaders

| `data-testid` | Element | Where to find it |
|---|---|---|
| `alert`, `alert-title`, `alert-message`, `alert-close` | An alert of the alert zone (`data-type` is `success`, `info`, `warning` or `danger`), its title, its text and its close button | Alert zone |
| `popover`, `popover-title`, `popover-content` | **AngularJS only**: the React client has no popover. The message shown over a component (`data-type`, and `data-testid-owner` with the component it points at) | End of `<body>` |
| `help-popover` | **AngularJS only.** The help of a component, rendered once for the whole application. It is displayed only while `data-open` is `true` | Alert zone |
| `dialog`, `dialog-close` | A modal dialog (with `data-testid-owner` = dialog id, and `data-open`) and the button of its header that closes it | Inside `[dialog-id='X']` |
| `confirm-dialog`, `confirm-accept`, `confirm-cancel` | The confirm dialog and its buttons | End of the alert zone |
| `loader` | A component loader (criteria, columns, selects). Grids use `grid-loader` | Inside the component |
| `error-boundary`, `error-boundary-title`, `error-boundary-message`, `error-boundary-details`, `error-boundary-retry`, `error-boundary-reload` | **React only.** The panel an error boundary shows when a part of the application fails to render (`data-scope` is `app` or `view`), its title, the message of the error, its component stack and its buttons. Read the message and the stack from the page source of the failure evidence to diagnose a white screen | Instead of the part that failed (`view`) or of the whole application (`app`) |
| `loading-bar`, `loading-spinner` | The global loading bar (AngularJS only) and its spinner. React has no loading bar: it shows the spinner while a view loads. They exist only while the application is loading | End of `<body>` |

### State

The state a test needs is exposed as data attributes, so it does not depend on library classes. They are always `"true"`
or `"false"`, except `data-type`, `data-container`, `data-icon`, `data-value` and `data-step-number`:

| Attribute | Meaning |
|---|---|
| `data-selected` | Selected row or checkbox, selected datepicker cell |
| `data-active` | Active tab, wizard step, wizard or tab pane and menu option; the datepicker cell that has the keyboard focus |
| `data-disabled` | Disabled tab list, context menu link, previous/next page arrow or datepicker cell |
| `data-open` | Menu option or submenu that is open; help popover that is displayed; dialog that is open. A dialog is `false` again only when its backdrop has been removed, so the screen is interactive |
| `data-expanded`, `data-loading` | Tree row that is expanded, or that is loading its children |
| `data-completed` | Wizard step that is already done |
| `data-outside-month` | Datepicker day that belongs to the previous or next month |
| `data-type` | Type of a message |
| `data-container` | Container of a grid viewport: `body`, `left` or `right` |
| `data-icon` | Icon classes shown by an icon column. Match one with `[data-icon~='fa-plus']` |
| `data-editing` | Grid row that is being edited. **React only**: the React client can edit a row that is not selected (AngularJS edits the selected row) |
| `data-deleted` | Grid row marked to be deleted when the grid is saved (multioperation grids and tree grids). **React only**. See `checkTreeRowDeleted(gridId, rowId)` |
| `data-rendered` | A chart that has been drawn. **React only** |
| `data-step-number` | Number of a wizard step (a text, not a boolean). **React only** |
| `data-value` | Value of a control whose text repeats it, such as the page size of a grid (a text, not a boolean). **React only** |

```java
// Value of the input of a criterion
By input = By.cssSelector("[criterion-id='Txt'] [data-testid='criterion-input']");

// Option "Yes" of the open dropdown of the criterion "Sta"
By option = By.cssSelector("[data-testid='select-option'][data-testid-owner='Sta']");

// Enabled days of the current month in the popup of the criterion "Cal"
By days = By.cssSelector("[data-testid='datepicker'][data-testid-owner='Cal'] "
  + "[data-testid='datepicker-day'][data-outside-month='false'][data-disabled='false']");

// Cell "name" of the selected rows of the grid "Grd"
By cell = By.cssSelector("[grid-id='Grd'] [data-testid='grid-row'][data-selected='true'] "
  + "[data-testid='grid-cell'][column-id='name']");

// Active tab of the criterion "Tab" and the danger alerts
By tab = By.cssSelector("[criterion-id='Tab'] [data-testid='tab'][data-active='true']");
By danger = By.cssSelector("[data-testid='alert'][data-type='danger']");
```

The vocabulary lives in a JavaScript constant for each client: `TestIds` and `TestAttributes` in
`awe-client-angular` (`js/awe/data/testIds.js`, also available as an AngularJS constant) and in `awe-client-react`
(`src/utilities/testIds.js`). The same concept uses the same value in both, and the Java constants of `awe-testing`
(`TestIds`, `TestAttributes`) mirror them: `TestIdsVocabularyTest` fails if they drift. A part that only one client
renders is marked **React only** or **AngularJS only** in the tables above. Hooks are additive: no existing class, id or
attribute is removed.

You rarely need these hooks in a test: a [step](#step-catalogue) already uses them. They are for the helpers of your
product and for a component AWE has no step for yet (then consider [adding the step](#adding-a-missing-step)).

## AngularJS and React clients

AWE 5 has two web clients, and the test application of each one runs its own suite (`awe-tests/awe-boot` for AngularJS,
`awe-tests/awe-boot-react` for React). The client under test is set with `awe.test.frontend` (`angular` by default, `react`).
The steps of `SeleniumUtilities` hide the differences, so the same step works on both. These are the differences a test
writer still meets, because they are visible in the screens and not in the rendering:

| Topic | AngularJS | React | What to do in a test |
|---|---|---|---|
| Logout | The logout button is visible in the shell | The logout button is inside the menu of the avatar, which `checkLogout()` opens | Use `checkLogout()`. If the application asks for a confirmation (the React reference application does), use `checkLogoutWithConfirmation()` |
| Tab label | `clickTab` matches the **locale key** of the label (`ENUM_MATRIX_EDITABLE`) | `clickTab` matches the **translated text** the user sees (`Editable`) | Pass the value of the client you test. If one suite targets both, keep the label in a constant per suite |
| Suggest value | The chosen value is shown as text | The chosen value is the **value of an input** | Use `checkSuggestContents` for a suggest and `checkSelectContents` for a select; never read the value with a selector |
| Button groups | Every option of a button checkbox or radio is a criterion of its own | A button group is **one criterion** and every option carries its `option-id` | Use `clickCheckboxOption(criterionName, optionId)`, and `checkCheckboxRadio(...)` for the state. A group may have no option checked by default |
| Edited row and selected row | The row you edit is the selected row | A row can be **edited without being selected** (`data-editing`), and a multiselect grid toggles the selection when you double click to edit | Use `editRow(...)` and the cell steps; use `toggleRowContents(gridId, search)` when you need the selection to change even if the row was selected |
| Editing gesture | Single click on the row | Double click, retried until the editor is open | Use `editRow(...)`. Do not click the cell yourself |
| Numbers | Nothing special: type the value | The numeric input (PrimeReact `InputNumber`) takes a typed `.` or `,` as its **decimal separator** and adds the thousands separator by itself | Type the digits and the decimal separator only, not the thousands separator: `writeText("Unt", "325274,50")`, then check `325.274,50` with `checkCriterionContents` |
| Criterion errors | Shown in the shared error container | Shown under the criterion (`criterion-error`) | Use `checkValidationErrorVisible()` |
| Info popovers | `popover`, `help-popover` | None | Use `checkMessageTitle` and `checkMessageText` for messages |
| Tabs that do not fit | A "more" menu (`tabdrop`) holds them | No "more" menu | `clickTab` opens the menu when the client has one |
| Loading bar | A loading bar on top of the page | Only a spinner while a view loads | Use `waitForLoadingBar()`, which knows what to wait for |
| Print page size | A native selector | A dropdown with a hidden selector: the text repeats the value, so the page size is exposed in `data-value` | Use `checkGridPageSize(size)` |

When a behaviour differs and no row above helps, do not branch on the client in your test: add a step (see
[Adding a missing step](#adding-a-missing-step)) and let the engine profile of each client decide.

### Tool-neutral locators (preview)

The engine profiles (`IAweFrontEndInstructions`) still return Selenium `By` in AWE 5.0, so nothing changes for your tests.
So that browser tests can later run on another automation tool without being rewritten, `com.almis.awe.testing.driver.Locator`
is a tool-neutral, immutable locator (`Locator.css(...)` or `Locator.xpath(...)`, with `kind()` and `expression()`). You get one
from a `By` with `Locator.from(by)` (or `Locator.from(listOfBy)`): CSS and XPath are kept as they are and `By.id` becomes the
equivalent, escaped CSS id selector. Other kinds of `By` (name, class name, link text, tag name) are rejected with an
`IllegalArgumentException`, because the profiles do not use them. `toBy()` goes back to Selenium. A unit test checks that every
locator of the AngularJS and React profiles converts. `Locator` is a preview: its Selenium conversion methods may move to the
Selenium adapter in a later release.

## Writing Selenium tests for your product

Your product tests should not know which libraries AWE uses to draw its components. If they do, replacing a library (as AWE
5 does with select2, ui-grid or the datepicker) breaks every suite. Locate components in this order:

1. **A `SeleniumUtilities` method.** `clickButton`, `selectContain`, `suggest`, `selectDate`, `editRow`, `clickTab`,
   `checkAndCloseMessage`... already know how to find each component and are kept working when AWE changes. The sections
   below describe them.
2. **A `data-testid` hook plus the AWE attributes**, when there is no method for what you need. The hook names the part of
   the component and the AWE attribute names the instance: `criterion-id`, `grid-id`, `row-id`, `column-id`, `option-id`,
   `info-dropdown-id` or the `id` of a button. `TestIds` and `TestAttributes` (`com.almis.awe.testing.selenium`) declare
   the vocabulary so you do not type the strings.
3. **Never a library class** (`.select2-*`, `.datepicker`, `.ui-grid-*`, `.modal`, `.nav-tabs`, `.alert`, `.popover`,
   `.btn`, `fa-*`...) or a position in a library's markup.

```java
import com.almis.awe.testing.selenium.TestAttributes;
import com.almis.awe.testing.selenium.TestIds;

// Wait for the warning alert of the login and check its text
checkText(TestIds.css(TestIds.ALERT) + TestAttributes.css(TestAttributes.TYPE, "warning") + " "
  + TestIds.css(TestIds.ALERT_MESSAGE), "The credentials entered for the user -test- are not valid");

// Click the first day of the open datepicker that is not disabled
click(TestIds.css(TestIds.DATEPICKER_DAY) + TestAttributes.css(TestAttributes.DISABLED, false));

// Type in the search box of the open select dropdown
writeText(By.cssSelector(TestIds.css(TestIds.SELECT_DROPDOWN) + " " + TestIds.css(TestIds.SELECT_SEARCH)), "tee");
```

### The owner attribute

The dropdown of a select, the datepicker popup and the message popovers are appended to the end of the `<body>`, outside
the component. They carry `data-testid-owner="<component id>"`. Use it when several components could be open or when you
need to be sure which one opened the element. Only one select dropdown (and one datepicker) is open at a time, and only
the open one carries its hook, so `select-dropdown` alone is enough in most tests:

```java
// Dropdown and options opened by the select "Sta"
By dropdown = By.cssSelector("[data-testid='select-dropdown'][data-testid-owner='Sta']");
```

### Wait on state, not on classes

Expose what you wait for as state: `data-selected`, `data-active`, `data-open`, `data-expanded`, `data-loading`...
(see *State* above). Wait on a state that stays set until the screen has finished re-rendering, for instance
`data-loading='false'` on a tree icon, or the selected state of a row after clicking it. Library classes such as `.active`
or `.fa-spin` change for reasons that have nothing to do with AWE.

### Match the whole text of an element

A library may split the text of an element into several nodes: when you type in a select, the matching letters are
highlighted (`<span class="select2-match">B</span>ase`), and no single text node contains `Base`. Compare the text of the
whole element, with `contains(normalize-space(.), 'Base')` in an xpath, or with `getText()`. Never use
`//text()[contains(., 'Base')]` over AWE components.

```java
By option = By.xpath("//*[@data-testid='select-dropdown']//*[@data-testid='select-option']"
  + "[contains(normalize-space(.),'Base')]");
```

## Keeping browser tests free of selectors

The `*IT` test classes should only describe **screen steps**: click this button, select this value, check this message.
How an element is found (a selector, a `data-testid` hook, an xpath) and which tool drives the browser belong to the
front-end instructions and `SeleniumUtilities`, so the same test keeps working when the web engine is replaced (AngularJS
or React) or when the automation tool changes. Locators written in the examples above (`TestIds`, `By`) are for the
helper classes of your product that extend `SeleniumUtilities`, not for the test classes.

In a test class do not use:

- Selenium imports (`org.openqa.selenium.*`) or types: `By`, `WebDriver` (`getDriver()`), `WebElement`, `Select`,
  `JavascriptExecutor` (`executeScript(...)`), `Actions`.
- `TestIds` and `TestAttributes`.
- Selector literals in the helpers that take one: `click`, `clearText`, `checkText`, `checkTextContains`,
  `checkTextNotContains`, `checkPresence`, `checkVisible`, `checkNotVisible`, `checkVisibleAndContains`,
  `checkTextInEmbeddedFrame`, `waitForCssSelector`, `waitForText` with a class, `checkLogin` with a selector and
  `checkLogout` with a selector. Use the semantic steps instead, for instance `checkLogin("test", "test", "Manager (test)")`
  and `checkLogout()`.

`BrowserTestSourceGuard` (`com.almis.awe.testing.guard`, in `awe-testing`) checks this rule over the Java sources, without
needing a browser or any Selenium class. By default it scans only the test classes (`*IT.java`), because the helper
classes of your product are where locators belong; `.files("*Page.java")` (any glob over the file name) scans other files. AWE applies it to its own test applications in a unit test (`All UT`), and a
product can do the same with a test of its own:

```java
@Test
void shouldKeepTheBrowserTestsFreeOfSelectors() throws IOException {
  Report report = BrowserTestSourceGuard.create()
    .allow("FileManagerIT.java", "checkTextInEmbeddedFrame(\"ol.breadcrumb a\", \"Files\")",
      "The file manager is a third-party application inside a frame")
    .scan(Path.of("src/test/java/com/mycompany/selenium")); // only the *IT.java files unless .files(glob) is set

  assertThat(report.isClean()).as(report.describe()).isTrue();
}
```

The report lists every violation with its file, line, rule and snippet. The selector check is a heuristic: it only looks
at the selector argument of the helpers above, and a literal counts as a selector when it contains `[ ] # . > + ~ * / :`
or a space. A plain tag name (`"button"`) is not detected, and neither are selectors built in a variable, so the guard
prevents the common cases but does not replace review.

### Allowing a justified exception

When an exception is justified, allow that exact snippet in that file and say why. The reason is mandatory, and an
allowance that no longer matches the code is reported (as stale) so it is removed with the code it excused. The snippet is
the one printed in the report: the offending line, or the whole call for a selector literal. Use
`allow(file, snippet, reason, times)` to allow an exact number of occurrences.

### Migrating an existing suite step by step

If a suite already has violations, list them in a baseline file and let the guard work as a ratchet: a new violation fails,
and so does a listed violation that has disappeared, so the baseline only shrinks until the suite is migrated and the
file is deleted.

```java
String baseline = BrowserTestSourceGuard.create().scan(testSources).toBaseline(); // write it to a file once

Report report = BrowserTestSourceGuard.create()
  .allowBaseline(Path.of("src/test/resources/browser-test-guard-baseline.txt"), "Pending migration to semantic steps")
  .scan(testSources);
```

Each line of the baseline is `file:snippet`; blank lines and lines starting with `#` are ignored, and a violation repeated
in a file is listed once per occurrence.

## Adding a missing step

If the catalogue has no step for what your test needs, do not write a selector in the test: add the step once, for every
client. Do it in this order, test first:

1. **Check what exists.** Search the [step catalogue](#step-catalogue) and the [hooks](#stable-test-hooks-data-testid).
   Many "missing" steps are an overload (the same step with a `gridId` and a `rowId`).
2. **Write the step in the façade.** Add a `protected` method to `SeleniumUtilities` (`com.almis.awe.testing.utilities`)
   with a javadoc. It states **what the user does or sees**, takes only strings, numbers and booleans (never a
   Selenium type, `By` or CSS) and asks `frontEndInstructions` where the element is. Wait on state (`data-*`) before
   acting. Write the unit test first (see `SeleniumUtilitiesSemanticStepsTest`) and watch it fail.
3. **Add the locator to the engine profile.** In `IAweFrontEndInstructions` add a `default` method that returns the
   locator for the AngularJS rendering, through a `data-testid` hook whenever there is one. Never add an abstract method:
   an implementation written before it would stop compiling. When the React client renders something else, override it
   in `ReactAweInstructions` (and in `AngularAweInstructions` if the default is not enough). Locators must use hooks and
   AWE attributes, never library classes: `AngularAweInstructionsSelectorGuardTest`, `ReactAweInstructionsSelectorGuardTest`
   and `SeleniumUtilitiesLiteralsGuardTest` fail otherwise.
4. **Add the hook if the client has none.** Declare the value in the vocabulary of each client that renders it
   (`awe-client-angular/src/main/resources/js/awe/data/testIds.js` and `awe-client-react/src/utilities/testIds.js`) and in
   `TestIds` / `TestAttributes` (`awe-testing`). The same concept uses the same value in both clients, and a part that only
   one client renders is marked "React only" or "AngularJS only" in a comment. `TestIdsVocabularyTest` and the Jest tests
   of the clients fail when the vocabularies drift. Hooks are additive: never remove or rename an existing class, id or
   attribute.
5. **Document it.** Add a row to the [step catalogue](#step-catalogue) and, if there is a new hook or attribute, to
   [Stable test hooks](#stable-test-hooks-data-testid). `SeleniumTestGuideSyncTest` fails the build while a public or
   protected step is not mentioned in this guide as `name(`.
6. **Check the compatibility contract.** The change must be additive: do not remove or change a public or protected
   member. Run the full module, including the API check:

   ```
   mvn verify -pl awe-framework/awe-testing
   ```

   Then run the suite of the client you changed (see [Running the suites](#running-the-suites-and-troubleshooting)).
   Replace a step only by deprecating the old one (`@Deprecated`, naming the replacement in its javadoc and in the
   catalogue).

## Running the suites and troubleshooting

### Run a suite locally

The test applications are `awe-tests/awe-boot` (AngularJS) and `awe-tests/awe-boot-react` (React). Build the project once
(`mvn install -DskipTests`), then run one IT class with a headless browser from the repository root:

```
mvn -f awe-tests/awe-boot-react/pom.xml verify -Dskip.junit=true -Dskip.selenium=false \
  -Dawe.test.browser=headless-chrome -Dit.test=SchedulerTestsIT
```

- `-Dawe.test.browser` takes `headless-chrome` or `headless-firefox` (also `chrome` and `firefox` to watch the browser).
- `-Dit.test=` takes a class, several separated by commas (`CRUDTestsIT,CriteriaAndMatrixTestsIT`) or a method
  (`SchedulerTestsIT#t003_...`). In `awe-boot` the suites are also selected by tag (`-Dgroups=SchedulerIT`).
- The application starts on port 8080. To use another one, set it in the environment (`SERVER_PORT=8090`); the browser
  reads the address from `awe.test.start-url`, which follows `server.port`.
- Add `xvfb-run -a` in front of `mvn` when the machine has no display (for instance WSL).
- A suite shares state between its tests (`t000_...` to `t999_...` run in name order), so run the class, not a lone
  method, unless the method does not depend on the previous ones.

### Failure evidence

When a test fails, `awe-testing` collects its evidence next to each other in `awe.test.screenshot-path`
(`target/tests/selenium/screenshots/` by default; the CI sets `selenium-evidence/`) and prints a link to each file in
the test output:

| File | What it is |
|---|---|
| `<test>.png` | Screenshot of the screen at the moment of the failure |
| `<test>.html` | Page source (DOM): look here for the hooks and attributes of the element the test did not find |
| `<test>.console.log` | Console of the browser (JavaScript errors, warnings and logs). The severe entries are also printed in the test output, since a client error often explains a blank screen |
| video | The recording of the test. Videos are kept only for failed tests (`awe.test.video-save=FAILED`); `awe.test.allowed-recording=false` turns recording off |

Start with the console and the HTML: a failed step usually means the element was not in the DOM yet, did not carry the
expected state attribute, or the client threw an error before rendering it.

### Reproduce a failure of the CI browser

A test that passes with `headless-chrome` can fail in CI because CI runs the browser from a Selenoid container image, on a
slower machine and with a different Firefox or Chrome build. To reproduce it, run the same image locally:

1. Take the pinned image of the browser from `.gitlab-ci.yml` (`.chrome-testing` and `.firefox-testing`, the
   `selenoid/chrome` and `selenoid/firefox` services, with their `@sha256:` digest) and start it:

   ```
   docker run -d --rm --platform linux/amd64 -p 4444:4444 -e SCREEN_RESOLUTION=1440x1080x24 \
     selenoid/chrome:latest@sha256:<digest from .gitlab-ci.yml>
   ```

2. Start the application so that the container can reach it, bound to all interfaces (`SERVER_ADDRESS=0.0.0.0`).
3. Run the class against the container browser:

   ```
   SERVER_ADDRESS=0.0.0.0 mvn -f awe-tests/awe-boot-react/pom.xml verify -Dskip.junit=true -Dskip.selenium=false \
     -Dawe.test.browser=service-chrome -Dawe.test.browser-host=localhost -Dawe.test.browser-port=4444 \
     -Dawe.test.server-host=host.docker.internal -Dawe.test.server-port=8080 \
     -Dawe.test.allowed-recording=false -Dit.test=SchedulerTestsIT
   ```

   Use `service-firefox` with the `selenoid/firefox` image for Firefox. `-Dawe.test.allowed-recording=false` is needed
   because the video recorder is another service of the CI job that you are not running.

:::warning The browser opens `server-host:server-port`, not `start-url`
A remote browser (`service-*` and `remote-*`) does not use `awe.test.start-url`: the test builds the application URL from
`awe.test.server-host` and `awe.test.server-port` (default `8080`) plus `awe.test.context-path`. If the application listens
on another port, or the host is not reachable from the container, the browser opens the wrong address and **every test
fails on the first step** with a blank screen. When no `server-host` is set, it uses the IP of the machine on Linux and
`host.docker.internal` elsewhere.
:::

:::tip Make timing failures reproducible
Many CI-only failures are timing failures: the CI container has little CPU. Limit the CPU of the browser container
(`docker update --cpus=1 <container>`) before the run to slow the browser down like CI does, and raise it again
(`--cpus=4`) to check that a fix does not depend on a fast machine.
:::

When the failure shows only on CI, download the job artifacts (`selenium-evidence/` has the screenshot, the page source,
the browser console and the video of every failed test) before changing code: the evidence usually says whether the step
needs a better wait (a state to wait on) or whether the client has a defect.

## Criteria

The following points describe how to fill the different type of criteria available in AWE screens:

### Input and Textarea

Simply call `writeText` method with the following parameters:
 - **criterionId** - Criterion identifier
 - **text** - Text to write

```java
// Insert text
writeText("criterionId", "textToWrite");
```

### Date

#### Pick a specific date in the datepicker

Call `selectDate` method with the following parameters:
 - **criterionId** - Date criterion identifier
 - **date** - Date to select

```java
// Select a date
selectDate("Cal", "23/10/1978");
```

#### Pick a day from the current month

Call `selectDay` method with the following parameters:
 - **criterionId** - Date criterion identifier
 - **day** - Day to select

```java
// Select a day in current month
selectDay("Cal", 23);
```

#### Pick a month in the month-selector

Call `selectMonth` method with the following parameters:
 - **criterionId** - Date criterion identifier
 - **month** - Month to select

```java
// Select a month in the month selector
selectMonth("Cal", 23);
```

#### Pick a year in the year-selector

Call `selectYear` method with the following parameters:
 - **criterionId** - Date criterion identifier
 - **year** - Year to select

```java
// Select a year in the year selector
selectYear("Cal", 2019);
```

### Time

Same way as [input and textarea](#input-and-textarea):

```java
// Write hour
writeText("Tim", "12:23:41");
```

### Select

To pick a result on a select criterion, call the `selectContain` method:
 - **criterionId** - Criterion identifier
 - **text** - Text to search on the result list

```java
// Select on selector
selectContain("Sta",  "Yes");
```

### Suggest

To use a suggest criterion, call the `suggest` method:
 - **criterionId** - Criterion identifier
 - **text to suggest** - Text to search for
 - **result label** - Text to search on the result list

```java
// Suggest on selector
suggest("Pro", "TS1", "TS1");
```

### Multiple select and suggest

#### Select one value

Select a single value with the `suggestMultiple` criterion:
 - **criterionId** - Criterion identifier
 - **text to suggest** - Text to search for
 - **result label** - Text to search on the result list

```java
// Suggest
suggestMultiple("CrtOpc", "application-info", "application-info");
```
#### More than one value

Select more than one value with the `suggestMultipleList` criterion:
- **criterionId** - Criterion identifier
- **text 1 to suggest** - Text 1 to search for
- **text 2 to suggest** - Text 2 to search for
- **...** - More texts to search for

```java
// Suggest
suggestMultipleList("CrtOpc", "application-info", "application-warning", "application-error");
```

### Tabs

#### Check active tab

To check if a tab is active, call the `checkText` method:
- **cssSelector** - Selector to find the text node to check 
 - **text** - Text to check

```java
// Check if tab is active
checkText("[criterion-id='" + tabId + "'] li.active a", "Tab text");
```

#### Click on a tab

To click on a tab you can call the `clickTab` method:
 - **tabId** - Tab identifier
 - **tabOption** - Tab option label: the locale key in the AngularJS client, the translated text in the React client
   (see [AngularJS and React clients](#angularjs-and-react-clients))
 
```java
// Click on tab
clickTab("TabSelMat", "ENUM_MATRIX_MULTISELECT");
```

### Checkbox and radio button

Click on a checkbox or a radio button the same way with the `clickCheckbox` method:
 - **checkboxRadioId** - Criterion identifier

```java
// Click checkbox or radio button
clickCheckbox("ChkBoxVa1");
```

### Text view

To check if a text view component contains a text it depends on the text-view structure:
- **cssSelector** - Selector to find the text node to check 
- **text** - Text to check

```java
// Check the contents of a tag list (text view)
checkTagListContains(textViewId, textToCheck);
```

### Verify criteria values

#### Text criteria

To verify text criteria contents, use the `checkCriterionContents` method:
 - **criterionId** - Criterion identifier
 - **text** - Text to match

```java
// Check criterion
checkCriterionContents("Nam", "Inf Changed");
```

#### Select and suggest criteria

To verify select and suggest criteria contents, use the `checkSelectContents` method for a select and the
`checkSuggestContents` method for a suggest (a suggest is not drawn like a select, and the two clients show its value
differently):
 - **criterionId** - Criterion identifier
 - **text** - Text to match

```java
// Check a select
checkSelectContents("Scr", "Usr");

// Check a suggest
checkSuggestContents("Sug", "Test");
```

## Grid cells

### Input and textarea column

Simply call `writeText` method with the following parameters:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier 
 - **text** - Text to write

```java
// Write on text
writeText("GrdMuo", "Des2", "asdasda");
```

### Date column

#### Pick a specific date in the datepicker in a column

To pick a date on a grid row, call the selectDate method:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier
 - **date** - Date to pick

```java
// Click on date
selectDate("GrdEdi", "Dat", "23/10/1978");
```

#### Pick a day from the current month in a column

Call `selectDay` method with the following parameters:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier
 - **day** - Day to select

```java
// Select a day in current month
selectDay("GrdEdi", "Dat", 23);
```

### Time column

Same way as [input and textarea column](#input-and-textarea-column):
 - **gridId** - Grid identifier
 - **columnId** - Column identifier
 - **time** - Time to select
 
```java
// Write hour
writeText("gridId", "timeColumn", "12:23:41");
```

### Select column

To pick a result on a select criterion, call the `selectContain` method:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier
 - **text** - Text to search on the result list

```java
// Select text
selectContain("GrdScrCnf", "Act", "Yes");
```

### Suggest column

To use a suggest criterion, call the `suggest` method:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier
 - **text to suggest** - Text to search for
 - **result label** - Text to search on the result list

```java
// Search for text
suggest("GrdScrCnf", "Atr", "visible", "Visible");
```

### Multiple select and suggest in a column

These two criteria can be tested the same way with the `suggestMultiple` criterion:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier
 - **text to suggest** - Text to search for
 - **result label** - Text to search on the result list

```java
// Suggest
suggestMultiple("gridId", "columnId", "application-info", "application-info");
```

### Checkbox column

Click on a checkbox or a radio button the same way with the `clickCheckbox` method:
 - **gridId** - Grid identifier
 - **columnId** - Column identifier

```java
// Click checkbox on a grid
clickCheckbox("gridId", "columnId");
```

### Save button

To click on a grid save button, call the `saveRow` method.

```java
// Save line
saveRow();
```

If there are some grids in the screen, you need to add the grid identifier to the method:

```java
// Save line
saveRow("myGridIdentifier");
```

### Check a row value

To check if there are some specific texts inside a grid, call the `checkRowContents` method:

```java
// Check row contents
checkRowContents("test", "ADM", "Site changed");
```

You can add as many texts as you want to check.

If you want to check the contents of a specific cell, you can call the `checkCellContents` method:
 - **gridId** - Grid identifier
 - **rowId** - Row identifier
 - **columnId** - Column identifier
 - **text** - Text to match

```java
// Check date on second row
checkCellContents("GrdEdi", "2", "Dat", date);
```

### Click on a row

To click on a row with a defined text, call the `clickRowContents` method:
 - **gridId** - Grid identifier
 - **text** - Text to match

```java
// Click on grid
clickRowContents("GrdEdi", "asphalt");
```

Or if you want to click on a specific cell, you can call the `clickCell` method:
 - **gridId** - Grid identifier
 - **rowId** - Row identifier
 - **columnId** - Column identifier

```java
  // Click on a cell
  clickCell("GrdMuo", "1", "Des2");
```

### Expand or collapse a row

To expand or collapse a treegrid row, we've defined the `clickTreeButton` method:
 - **gridId** - Grid identifier
 - **rowId** - Row identifier
 
```java
// Click on button
clickTreeButton("TreGrdLoaEdi", "Prooperator");
```

### Context menu on a row

To open a context menu on a grid row, just call the `contextMenu` method:
 - **gridId** - Grid identifier
 - **rowId** - Row identifier
 - **columnId** - Column identifier
 
```java
// Context menu
contextMenu("TreGrdLoaEdi", "Progeneral-ModBase", "TreGrdLoaEdi_Nam");
```

### Context menu option

You can click on a context menu option with the `clickContextButton` option.
 - **menuOptions** - Options to click (ordered) in the context menu

```java
// Select context menu option
clickContextButton("CtxTreGrdLoaEdiAddSel", "CtxTreGrdLoaEdiAddChl");
```

## Samples

### Add a new site

```java
/**
 * Add a new site
 * @throws Exception
 */
@Test
public void t001_newSite() throws Exception {
  // Title
  setTestTitle("Add a new site");

  // Go to screen
  gotoScreen("tools", "sites");

  // Click on new button
  clickButton("ButNew", true);

  // Wait for button
  waitForButton("ButCnf");

  // Write on criterion
  writeText("Nam", "Site test");

  // Select last element
  selectLast("Act");

  // Write on criterion
  writeText("Ord", "3");

  // Click on button
  clickButton("ButGrdAdd");

  // Suggest on column selector
  suggest("SitModDbsLst", "IdeMod", "Base", "Base");

  // Suggest on column selector
  suggest("SitModDbsLst", "IdeDbs", "awedb", "awedb");

  // Write on criterion
  writeText("SitModDbsLst", "Order", "3");

  // Save line
  saveRow();

  // Check row values
  checkRowContents("Base", "awedb", "3");

  // Store and confirm
  clickButtonAndConfirm("ButCnf");
  
  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtSit", "Site test", "Site test");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("Site test");

  // Click on button
  clickButton("ButViw", true);

  // Wait for button
  waitForButton("ButBck");
  
  // Check row contents
  checkRowContents("Base", "awedb");
}
```

### Delete a module

```java
/**
 * Delete a module
 * @throws Exception
 */
@Test
public void t056_deleteModule() throws Exception {
  // Title
  setTestTitle("Delete a module");

  // Go to screen
  gotoScreen("tools", "modules");

  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtMod", "Inf", "Inf");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("Inf");

  // Store and confirm
  clickButtonAndConfirm("ButDel");

  // Wait for button
  clickButton("ButRst");

  // Search on grid
  searchAndWait();

  // Click row
  checkRowNotContains("Inf");  
}
```

### Update a database

```java
/**
 * Update a database connection
 * @throws Exception
 */
@Test
public void t033_updateDatabase() throws Exception {
  // Title
  setTestTitle("Update a database connection");

  // Go to screen
  gotoScreen("tools", "databases");

  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtAls", "DBSTest", "DBSTest");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("DBSTest");

  // Click on button
  clickButton("ButUpd", true);

  // Wait for button
  waitForButton("ButCnf");

  // Insert text
  writeText("Als", "DBSTest Changed");

  // Select on selector
  selectContain("Dct",  "Jdbc");

  // Insert text
  writeText("Dbc", "Test");

  // Insert text
  writeText("Des", "This is a database connection update test case");

  // Click on row
  clickRowContents("Site changed");

  // Suggest on column selector
  suggest("SitModDbsLst", "IdeMod", "Test", "Test");

  // Save line
  saveRow();

  // Check row
  checkRowContents("Test");

  // Store and confirm
  clickButtonAndConfirm("ButCnf");

  // Wait for button
  clickButton("ButRst");

  // Suggest on column selector
  suggest("CrtAls", "DBSTest Changed", "DBSTest Changed");

  // Search on grid
  searchAndWait();

  // Click row
  clickRowContents("DBSTest Changed");

  // Click on button
  clickButton("ButViw", true);

  // Wait for button
  waitForButton("ButBck");

  // Check contents
  checkCriterionContents("Als", "DBSTest Changed");

  // Check row contents
  checkRowContents("Test");
}
```
