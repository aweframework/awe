---
id: selenium-testing
title: Selenium Tests
sidebar_label: Selenium Tests
---

This document gives a basic insight on how to start developing *Selenium* tests for applications developed with AWE. Before starting test development, make sure to read the *Selenium Test Development Guide*, specially the *Optimization/Help tips* section. Basic aspects you should know before starting to develop *Selenium* tests, such as general configurations and integration with *Jenkins*, are not treated in this document.

All the contents of this document are explained in a way that it is assumed the reader already knows how to use the tools and commands concerning *Selenium*.

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
- **cssSelector** - CSS selector to find a text once logged into the application
- **text** - Text to find inside the selector node

 ```java
 // Title
 checkLogin("test", "test", "span.avatar-text", "Manager (test)");
 ```
 
 To logout the application just call to `checkLogout` method with the following 
 parameters:
 - **cssSelector** - CSS selector to find a text on the *signin screen*
 - **text** - Text to find inside the selector node
 
 ```java
 // Title
 checkLogout(".slogan", "Almis Web Engine");
 ```

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
| `grid-pagination`, `grid-page-previous`, `grid-page-next`, `grid-goto-page`, `grid-page-size` | Footer pagination, its previous/next arrows (`data-disabled`) of the compact pager, the "go to page" input and the page size select | Inside the grid |
| `grid-loader` | Loader of the grid (also the pivot table) | Inside the grid |
| `tree-icon` | Expand/collapse icon of a tree row. `data-expanded` and `data-loading` | Inside the cell |
| `tree-header-icon` | Expand/collapse all icon of the header | Inside the grid |
| `column-icon` | The icon of an icon column. `data-icon` carries the icon of the cell value (for instance `fa-plus` in a multioperation grid) | Inside the cell |

### Tabs and wizards

| `data-testid` | Element | Where to find it |
|---|---|---|
| `tab-list` | The tab headers of a tab criterion. `data-disabled` | Inside `[criterion-id='X']` |
| `tab`, `tab-link`, `tab-label` | A tab header (`li` with the `tab-<value>` id and `data-active`), the link to click and the label | Inside the tab list, or inside the `tabdrop-menu` when the tab does not fit |
| `tab-pane` | Content of a tab, with the pane `id`. `data-active` | Inside `[criterion-id='X']` |
| `tabdrop`, `tabdrop-toggle`, `tabdrop-menu` | The "more" dropdown that holds the tabs that do not fit, the button that opens it and its menu | Inside the tab list |
| `wizard-step`, `wizard-pane` | A step header (`data-active` and `data-completed`) and a content pane (`data-active`) | Inside `[criterion-id='X']` |

### Buttons, menus and info

| `data-testid` | Element | Where to find it |
|---|---|---|
| `button` | The `<button>` of a button, with the button `id` (also inside grid cells) | Anywhere |
| `context-menu`, `context-menu-option`, `context-menu-link`, `context-submenu` | A context menu, an option (with `option-id`), its link (`data-disabled`) and a nested menu | Inside the component that owns the menu |
| `menu`, `menu-option`, `menu-link`, `menu-dropdown`, `menu-submenu` | The application menu, an option (`data-active`, `data-open`), its link (with `name`), the first level dropdown and the nested submenus (`data-open`) | Inside the menu |
| `info-dropdown`, `info-dropdown-toggle`, `info-dropdown-menu` | An info dropdown (with `info-dropdown-id`), the link that opens it and its menu | Anywhere |
| `info-button`, `info-button-link` | An info button and its link | Anywhere |

### Messages, dialogs and loaders

| `data-testid` | Element | Where to find it |
|---|---|---|
| `alert`, `alert-title`, `alert-message`, `alert-close` | An alert of the alert zone (`data-type` is `success`, `info`, `warning` or `danger`), its title, its text and its close button | Alert zone |
| `popover`, `popover-title`, `popover-content` | The message shown over a component (`data-type`, and `data-testid-owner` with the component it points at) | End of `<body>` |
| `help-popover` | The help of a component, rendered once for the whole application. It is displayed only while `data-open` is `true` | Alert zone |
| `dialog`, `dialog-close` | A modal dialog (with `data-testid-owner` = dialog id, and `data-open`) and the button of its header that closes it | Inside `[dialog-id='X']` |
| `confirm-dialog`, `confirm-accept`, `confirm-cancel` | The confirm dialog and its buttons | End of the alert zone |
| `loader` | A component loader (criteria, columns, selects). Grids use `grid-loader` | Inside the component |
| `loading-bar`, `loading-spinner` | The global loading bar and its spinner. They exist only while the application is loading | End of `<body>` |

### State

The state a test needs is exposed as data attributes, so it does not depend on library classes. They are always `"true"`
or `"false"`, except `data-type`, `data-container` and `data-icon`:

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

The vocabulary lives in a single JavaScript constant, `TestIds` (`awe-client-angular`, `js/awe/data/testIds.js`), also
available as an AngularJS constant. Hooks are additive: no existing class, id or attribute is removed.

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
 - **tabOption** - Tab option label (locale)
 
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
// Check visibility and contents
checkVisibleAndContains("[awe-tag-list='" + textViewId + "'] span", textToCheck);
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

To verify select and suggest criteria contents, use the `checkSelectorContents` method:
 - **criterionId** - Criterion identifier
 - **text** - Text to match

```java
// Check criterion
checkSelectContents("Scr", "Usr");
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
