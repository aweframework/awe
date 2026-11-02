package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * Settings of the application: sequences, application parameters and queues. The tests of the two administration screens
 * create, update and delete their own records through their side panel (sequence-admin and application-parameter-admin),
 * search them, check the counters of their quick filters and delete them from the row menu and the selection bar; every
 * test opens its own screen. The themes have their own screen and tests (ThemeAdminTestsIT).
 */
@Tag("ApplicationIntegrationIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class ApplicationSettingsTestsIT extends AbstractApplicationTests {

  private static final String TOOLS = "tools";
  private static final String SEQUENCES = "sequences";
  private static final String PARAMETERS = "application-parameters";
  private static final String EDIT_BUTTON = "EdtBtn";
  private static final String ROW_MENU_BUTTON = "MorBtn";

  // Sequences screen (sequence-admin). The name of a sequence holds 20 characters, so the words of the class are short
  private static final String KEY_GRID = "GrdKeyAdm";
  private static final String KEY_SEARCH = "CrtKeyTxt";
  private static final String KEY_QUICK_FILTER = "CrtKeySta";
  private static final String KEY_PANEL = "PnlKey";
  private static final String KEY_PREFIX = "Seqadmtest";
  private static final String CREATED_SEQUENCE = KEY_PREFIX + " new";
  private static final String COUNT_PREFIX = KEY_PREFIX + " cnt";
  private static final String COUNT_ACTIVE_SEQUENCE = COUNT_PREFIX + " act";
  private static final String COUNT_INACTIVE_SEQUENCE = COUNT_PREFIX + " ina";
  private static final String ROW_DELETE_SEQUENCE = KEY_PREFIX + " row";
  private static final String BAR_DELETE_SEQUENCE = KEY_PREFIX + " bar";
  private static final String SEQUENCE_VALUE = "7";
  private static final String NEW_SEQUENCE_VALUE = "9";

  // Application parameters screen (application-parameter-admin)
  private static final String PAR_GRID = "GrdAppParAdm";
  private static final String PAR_SEARCH = "CrtAppParTxt";
  private static final String PAR_QUICK_FILTER = "CrtAppParSta";
  private static final String PAR_PANEL = "PnlAppPar";
  private static final String PAR_PREFIX = "Appparadmtest";
  private static final String CREATED_PARAMETER = PAR_PREFIX + " new";
  private static final String COUNT_PREFIX_PAR = PAR_PREFIX + " cnt";
  private static final String COUNT_ACTIVE_PARAMETER = COUNT_PREFIX_PAR + " act";
  private static final String COUNT_INACTIVE_PARAMETER = COUNT_PREFIX_PAR + " ina";
  private static final String ROW_DELETE_PARAMETER = PAR_PREFIX + " row";
  private static final String BAR_DELETE_PARAMETER = PAR_PREFIX + " bar";
  private static final String PARAMETER_VALUE = "1";
  private static final String NEW_PARAMETER_VALUE = "0";
  private static final String GENERAL = "General";
  private static final String SECURITY = "Security";
  private static final String REPORTS = "Reports";
  private static final String DESCRIPTION = "Value of a parameter of the tests";
  private static final String NEW_DESCRIPTION = "Value of a parameter of the tests, updated";
  private static final String COUNT_DESCRIPTION = "Appparadmtest parameter that counts as active";
  private static final String NEW_COUNT_DESCRIPTION = "Appparadmtest parameter that counts as inactive";

  /**
   * Log into the application
   */
  @Test
  @StartsFrom(Session.BLANK)
  void t000_loginTest() {
    checkLogin("test", "test", "Manager (test)");
  }

  /**
   * Log out from the application
   */
  @Test
  void t999_logoutTest() {
    checkLogout();
  }

  /**
   * Open a screen of the tools group, going through another one first: the client does not load again the screen that
   * the previous test left open, with its search and its selected row
   *
   * @param option Option of the tools group
   */
  private void openToolsScreen(String option) {
    gotoScreen(TOOLS, option.equals(SEQUENCES) ? PARAMETERS : SEQUENCES);
    gotoScreen(TOOLS, option);
  }

  /**
   * Open the sequences screen and search the sequences whose name contains a text
   *
   * @param text Text of the name
   */
  private void searchSequences(String text) {
    openToolsScreen(SEQUENCES);
    searchGrid(KEY_SEARCH, text);
  }

  /**
   * Delete the sequences of the class, when a failed attempt of the class left one: every test creates its own
   * sequences and deletes them at the end
   */
  private void deleteSequencesOfTheClass() {
    for (String name : List.of(CREATED_SEQUENCE, COUNT_ACTIVE_SEQUENCE, COUNT_INACTIVE_SEQUENCE, ROW_DELETE_SEQUENCE,
      BAR_DELETE_SEQUENCE)) {
      deleteSequenceIfPresent(name);
    }

    searchSequences(KEY_PREFIX);
    checkRowGone(KEY_GRID, KEY_PREFIX);
  }

  /**
   * Delete the sequence with a name, when it is in the grid
   *
   * @param name Name of the sequence
   */
  private void deleteSequenceIfPresent(String name) {
    searchSequences(name);
    if (hasRowContents(name)) {
      clickGridRowButton(KEY_GRID, name, ROW_MENU_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      waitForLoadingBar();
    }
  }

  /**
   * Open the side panel of a new sequence: no selected sequence, so the header shows the label of the panel, and it
   * starts active
   */
  private void openNewSequencePanel() {
    openToolsScreen(SEQUENCES);
    clickButton("ButNew");
    checkSidePanelOpen(KEY_PANEL, "right", "520px");
    checkSidePanelTitle(KEY_PANEL, "Sequence");
    checkCheckboxRadio(true, "Act");
  }

  /**
   * Create a sequence from the side panel
   *
   * @param name   Name of the sequence
   * @param value  Value of the sequence
   * @param active Leave the Active switch on
   */
  private void createSequence(String name, String value, boolean active) {
    openNewSequencePanel();
    writeText("SeqNam", name);
    writeText("KeyVal", value);
    if (!active) {
      clickCheckbox("Act");
      checkCheckboxRadio(false, "Act");
    }
    clickButtonAndConfirm("ButPnlNew");
    waitForLoadingBar();
  }

  /**
   * Check the row of a sequence: its name, its value and its state
   *
   * @param name  Name of the sequence
   * @param value Value of the sequence
   * @param state State of the sequence: Active or Inactive
   */
  private void checkSequenceRow(String name, String value, String state) {
    searchSequences(name);
    checkRowContentsGrid(KEY_GRID, name, value, state);
  }

  /**
   * Delete a sequence from the row menu of its row (the ellipsis selects only its row)
   *
   * @param name Name of the sequence
   */
  private void deleteSequenceFromTheRowMenu(String name) {
    searchSequences(name);
    if (hasRowContents(name)) {
      clickGridRowButton(KEY_GRID, name, ROW_MENU_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      waitForLoadingBar();
    }

    searchSequences(name);
    checkRowGone(KEY_GRID, name);
  }

  /**
   * The side panel creates a sequence and its row shows its name, its value and its state
   */
  @Test
  void t011_newSequenceFromThePanel() {
    setTestTitle("Add a sequence from the side panel");

    deleteSequencesOfTheClass();
    createSequence(CREATED_SEQUENCE, SEQUENCE_VALUE, true);

    checkSequenceRow(CREATED_SEQUENCE, SEQUENCE_VALUE, "Active");

    deleteSequenceFromTheRowMenu(CREATED_SEQUENCE);
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the sequence of its row (whose header shows the
   * sequence and its state) and its row shows the value and the state saved from the panel
   */
  @Test
  void t013_editSequenceFromThePencil() {
    setTestTitle("Edit a sequence from the pencil of its row");

    deleteSequencesOfTheClass();
    createSequence(CREATED_SEQUENCE, SEQUENCE_VALUE, true);

    searchSequences(CREATED_SEQUENCE);
    clickGridRowButton(KEY_GRID, CREATED_SEQUENCE, EDIT_BUTTON);
    checkSidePanelTitle(KEY_PANEL, CREATED_SEQUENCE);
    checkSidePanelTag(KEY_PANEL, "Active");
    checkCriterionContents("SeqNam", CREATED_SEQUENCE);
    checkCriterionContents("KeyVal", SEQUENCE_VALUE);
    checkCheckboxRadio(true, "Act");

    // The name of a sequence is its primary key and the panel shows it read-only when editing: only the value and the
    // state change
    writeText("KeyVal", NEW_SEQUENCE_VALUE);
    clickCheckbox("Act");
    checkCheckboxRadio(false, "Act");
    clickButtonAndConfirm("ButPnlUpd");
    waitForLoadingBar();

    checkSequenceRow(CREATED_SEQUENCE, NEW_SEQUENCE_VALUE, "Inactive");

    deleteSequenceFromTheRowMenu(CREATED_SEQUENCE);
  }

  /**
   * The search of the grid finds a sequence by its name ignoring case and the quick filters count the sequences of the
   * search (All, Active, Inactive) and show the ones of their state
   */
  @Test
  void t015_searchSequencesByTheirNameAndTheQuickFilters() {
    setTestTitle("Search the sequences by name and count them with the quick filters");

    deleteSequencesOfTheClass();
    createSequence(COUNT_ACTIVE_SEQUENCE, SEQUENCE_VALUE, true);
    createSequence(COUNT_INACTIVE_SEQUENCE, NEW_SEQUENCE_VALUE, false);

    // The words of this test are its own, so its counters do not depend on the sequences other tests may have left
    searchSequences(COUNT_PREFIX);
    checkRowContentsGrid(KEY_GRID, COUNT_ACTIVE_SEQUENCE, COUNT_INACTIVE_SEQUENCE);

    checkGridQuickFilterOption(KEY_QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "active", "Active 1");
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(KEY_QUICK_FILTER, "");

    // The search finds a sequence by its name, ignoring case, hides the other one and the counts follow it
    typeGridSearch(KEY_SEARCH, COUNT_INACTIVE_SEQUENCE.toLowerCase());
    checkRowGone(KEY_GRID, COUNT_ACTIVE_SEQUENCE);
    checkRowContentsGrid(KEY_GRID, COUNT_INACTIVE_SEQUENCE);
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "active", "Active 0");

    // Each quick filter shows the sequences of its state, and All shows them all again
    searchSequences(COUNT_PREFIX);
    clickGridQuickFilterOption(KEY_QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(KEY_QUICK_FILTER, "inactive");
    checkRowGone(KEY_GRID, COUNT_ACTIVE_SEQUENCE);
    checkRowContentsGrid(KEY_GRID, COUNT_INACTIVE_SEQUENCE);

    clickGridQuickFilterOption(KEY_QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(KEY_QUICK_FILTER, "");
    checkRowContentsGrid(KEY_GRID, COUNT_ACTIVE_SEQUENCE, COUNT_INACTIVE_SEQUENCE);

    deleteSequenceFromTheRowMenu(COUNT_ACTIVE_SEQUENCE);
    deleteSequenceFromTheRowMenu(COUNT_INACTIVE_SEQUENCE);
  }

  /**
   * Delete a sequence from the row menu of its row and another one from the selection bar of the grid
   */
  @Test
  void t017_deleteSequences() {
    setTestTitle("Delete the sequences from the row menu and the selection bar");

    deleteSequencesOfTheClass();
    createSequence(ROW_DELETE_SEQUENCE, SEQUENCE_VALUE, true);
    createSequence(BAR_DELETE_SEQUENCE, NEW_SEQUENCE_VALUE, true);

    // The ellipsis of a row deletes its sequence from the row menu
    searchSequences(ROW_DELETE_SEQUENCE);
    clickGridRowButton(KEY_GRID, ROW_DELETE_SEQUENCE, ROW_MENU_BUTTON);
    clickContextButton("CtxButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(KEY_GRID, ROW_DELETE_SEQUENCE);

    // The selection bar deletes the selected sequences
    searchSequences(BAR_DELETE_SEQUENCE);
    clickRowContents(KEY_GRID, BAR_DELETE_SEQUENCE);
    checkGridSelectionBar(KEY_GRID, "1 selected");
    clickGridSelectionButton(KEY_GRID, "ButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(KEY_GRID, BAR_DELETE_SEQUENCE);

    // No sequence of the class is left
    searchSequences(KEY_PREFIX);
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "", "All 0");
  }

  /**
   * Add a new queue
   */
  @Test
  void t061_newQueue() {
    // Title
    setTestTitle("Add a new queue");

    addNew("tools", "queues");

    // Insert text
    writeText("Als", "Queue test");

    // Insert text
    writeText("JmsBrk", "Broker");

    // Insert text
    writeText("DstNam", "Destination");

    // Select on selector
    selectContain("ConTyp", "JNDI");

    // Insert text
    writeText("Des", "Queue description");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtAls", "test", "test");

    // Search on grid
    searchAndWait();

    // Check contents
    checkRowContents("Queue test", "Broker", "Destination", "JNDI", "Queue description");
  }

  /**
   * Update a queue
   */
  @Test
  void t063_updateQueue() {
    // Title
    setTestTitle("Update a queue");

    // Go to update
    update("CrtAls", "test", "tools", "queues");

    // Insert text
    writeText("Als", "Queue changed");

    // Insert text
    writeText("JmsBrk", "New broker");

    // Insert text
    writeText("DstNam", "New destination");

    // Insert text
    writeText("Des", "Queue changed description");

    // Store and confirm
    clickButtonAndConfirm("ButCnf");

    // Wait for button
    clickButton("ButRst");

    // Suggest on column selector
    suggest("CrtAls", "changed", "changed");

    // Search on grid
    searchAndWait();

    // Check contents
    checkRowContents("Queue changed", "New broker", "New destination", "JNDI", "Queue changed description");
  }

  /**
   * Delete a queue
   */
  @Test
  void t065_deleteQueue() {
    // Title
    setTestTitle("Delete a queue");

    // Delete a queue
    delete("CrtAls", "Queue changed", "tools", "queues");

    // Verify
    verifyDeleted("Queue changed");
  }

  /**
   * Open the application parameters screen and search the parameters whose name or description contains a text
   *
   * @param text Text of the name or the description
   */
  private void searchParameters(String text) {
    openToolsScreen(PARAMETERS);
    searchGrid(PAR_SEARCH, text);
  }

  /**
   * Delete the parameters of the class, when a failed attempt of the class left one
   */
  private void deleteParametersOfTheClass() {
    for (String name : List.of(CREATED_PARAMETER, COUNT_ACTIVE_PARAMETER, COUNT_INACTIVE_PARAMETER,
      ROW_DELETE_PARAMETER, BAR_DELETE_PARAMETER)) {
      deleteParameterIfPresent(name);
    }

    searchParameters(PAR_PREFIX);
    checkRowGone(PAR_GRID, PAR_PREFIX);
  }

  /**
   * Delete the parameter with a name, when it is in the grid
   *
   * @param name Name of the parameter
   */
  private void deleteParameterIfPresent(String name) {
    searchParameters(name);
    if (hasRowContents(name)) {
      clickGridRowButton(PAR_GRID, name, ROW_MENU_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      waitForLoadingBar();
    }
  }

  /**
   * Open the side panel of a new parameter: no selected parameter, so the header shows the label of the panel, and it
   * starts in the general category and active, the defaults of the table
   */
  private void openNewParameterPanel() {
    openToolsScreen(PARAMETERS);
    clickButton("ButNew");
    checkSidePanelOpen(PAR_PANEL, "right", "560px");
    checkSidePanelTitle(PAR_PANEL, "Application parameter");
    checkSelectContents("Cat", GENERAL);
    checkCheckboxRadio(true, "Act");
  }

  /**
   * Create an application parameter from the side panel
   *
   * @param name        Name of the parameter
   * @param value       Value of the parameter
   * @param category    Category of the parameter: General, Security or Reports
   * @param description Description of the parameter
   * @param active      Leave the Active switch on
   */
  private void createParameter(String name, String value, String category, String description, boolean active) {
    openNewParameterPanel();
    writeText("ParNam", name);
    selectContain("Cat", category);
    writeText("ParVal", value);
    writeText("Des", description);
    if (!active) {
      clickCheckbox("Act");
      checkCheckboxRadio(false, "Act");
    }
    clickButtonAndConfirm("ButPnlNew");
    waitForLoadingBar();
  }

  /**
   * Check the row of a parameter: its name with the description under it, its value, its category and its state
   *
   * @param name        Name of the parameter
   * @param description Description of the parameter (secondary line of its name)
   * @param value       Value of the parameter
   * @param category    Category of the parameter
   * @param state       State of the parameter: Active or Inactive
   */
  private void checkParameterRow(String name, String description, String value, String category, String state) {
    searchParameters(name);
    checkRowContentsGrid(PAR_GRID, name, description, value, category, state);
  }

  /**
   * Delete a parameter from the row menu of its row
   *
   * @param name Name of the parameter
   */
  private void deleteParameterFromTheRowMenu(String name) {
    searchParameters(name);
    if (hasRowContents(name)) {
      clickGridRowButton(PAR_GRID, name, ROW_MENU_BUTTON);
      clickContextButton("CtxButDel");
      acceptConfirm();
      waitForLoadingBar();
    }

    searchParameters(name);
    checkRowGone(PAR_GRID, name);
  }

  /**
   * The side panel creates an application parameter and its row shows its name with the description under it, its value,
   * its category and its state
   */
  @Test
  void t041_newParameterFromThePanel() {
    setTestTitle("Add an application parameter from the side panel");

    deleteParametersOfTheClass();

    // The screen can print the list of the parameters, as the rest of the administration screens
    openToolsScreen(PARAMETERS);
    checkButtonVisible("ButPrn");

    createParameter(CREATED_PARAMETER, PARAMETER_VALUE, SECURITY, DESCRIPTION, true);

    checkParameterRow(CREATED_PARAMETER, DESCRIPTION, PARAMETER_VALUE, SECURITY, "Active");

    deleteParameterFromTheRowMenu(CREATED_PARAMETER);
  }

  /**
   * The pencil of a row opens the side panel in edit mode filled with the parameter of its row (whose header shows the
   * parameter and its state) and its row shows the value, the category and the state saved from the panel
   */
  @Test
  void t043_editParameterFromThePencil() {
    setTestTitle("Edit an application parameter from the pencil of its row");

    deleteParametersOfTheClass();
    createParameter(CREATED_PARAMETER, PARAMETER_VALUE, SECURITY, DESCRIPTION, true);

    searchParameters(CREATED_PARAMETER);
    clickGridRowButton(PAR_GRID, CREATED_PARAMETER, EDIT_BUTTON);
    checkSidePanelTitle(PAR_PANEL, CREATED_PARAMETER);
    checkSidePanelTag(PAR_PANEL, "Active");
    checkCriterionContents("ParNam", CREATED_PARAMETER);
    checkSelectContents("Cat", SECURITY);
    checkCriterionContents("ParVal", PARAMETER_VALUE);
    checkCriterionContents("Des", DESCRIPTION);
    checkCheckboxRadio(true, "Act");

    // The name of a parameter is the key the application reads and the panel shows it read-only when editing: the value,
    // the category, the description and the state change
    writeText("ParVal", NEW_PARAMETER_VALUE);
    selectContain("Cat", GENERAL);
    writeText("Des", NEW_DESCRIPTION);
    clickCheckbox("Act");
    checkCheckboxRadio(false, "Act");
    clickButtonAndConfirm("ButPnlUpd");
    waitForLoadingBar();

    checkParameterRow(CREATED_PARAMETER, NEW_DESCRIPTION, NEW_PARAMETER_VALUE, GENERAL, "Inactive");

    deleteParameterFromTheRowMenu(CREATED_PARAMETER);
  }

  /**
   * The search of the grid finds a parameter by its description ignoring case and the quick filters count the parameters
   * of the search (All, Active, Inactive) and show the ones of their state
   */
  @Test
  void t045_searchParametersByTheirDescriptionAndTheQuickFilters() {
    setTestTitle("Search the application parameters by description and count them with the quick filters");

    deleteParametersOfTheClass();
    createParameter(COUNT_ACTIVE_PARAMETER, PARAMETER_VALUE, GENERAL, COUNT_DESCRIPTION, true);
    createParameter(COUNT_INACTIVE_PARAMETER, NEW_PARAMETER_VALUE, REPORTS, NEW_COUNT_DESCRIPTION, false);

    // The words of this test are its own, so its counters do not depend on the parameters other tests may have left
    searchParameters(COUNT_PREFIX_PAR);
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER, COUNT_INACTIVE_PARAMETER);

    checkGridQuickFilterOption(PAR_QUICK_FILTER, "", "All 2");
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "active", "Active 1");
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "inactive", "Inactive 1");
    checkGridQuickFilterOptionChosen(PAR_QUICK_FILTER, "");

    // The search reads the description too: it finds one parameter by the words of its description, ignoring case
    typeGridSearch(PAR_SEARCH, NEW_COUNT_DESCRIPTION.toLowerCase());
    checkRowGone(PAR_GRID, COUNT_ACTIVE_PARAMETER);
    checkRowContentsGrid(PAR_GRID, COUNT_INACTIVE_PARAMETER);
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "", "All 1");
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "inactive", "Inactive 1");

    // Each quick filter shows the parameters of its state, and All shows them all again
    searchParameters(COUNT_PREFIX_PAR);
    clickGridQuickFilterOption(PAR_QUICK_FILTER, "active");
    checkGridQuickFilterOptionChosen(PAR_QUICK_FILTER, "active");
    checkRowGone(PAR_GRID, COUNT_INACTIVE_PARAMETER);
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER);

    clickGridQuickFilterOption(PAR_QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(PAR_QUICK_FILTER, "");
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER, COUNT_INACTIVE_PARAMETER);

    deleteParameterFromTheRowMenu(COUNT_ACTIVE_PARAMETER);
    deleteParameterFromTheRowMenu(COUNT_INACTIVE_PARAMETER);
  }

  /**
   * Delete an application parameter from the row menu of its row and another one from the selection bar of the grid
   */
  @Test
  void t047_deleteParameters() {
    setTestTitle("Delete the application parameters from the row menu and the selection bar");

    deleteParametersOfTheClass();
    createParameter(ROW_DELETE_PARAMETER, PARAMETER_VALUE, GENERAL, DESCRIPTION, true);
    createParameter(BAR_DELETE_PARAMETER, NEW_PARAMETER_VALUE, GENERAL, NEW_DESCRIPTION, true);

    // The ellipsis of a row deletes its parameter from the row menu
    searchParameters(ROW_DELETE_PARAMETER);
    clickGridRowButton(PAR_GRID, ROW_DELETE_PARAMETER, ROW_MENU_BUTTON);
    clickContextButton("CtxButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(PAR_GRID, ROW_DELETE_PARAMETER);

    // The selection bar deletes the selected parameters
    searchParameters(BAR_DELETE_PARAMETER);
    clickRowContents(PAR_GRID, BAR_DELETE_PARAMETER);
    checkGridSelectionBar(PAR_GRID, "1 selected");
    clickGridSelectionButton(PAR_GRID, "ButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(PAR_GRID, BAR_DELETE_PARAMETER);

    // No parameter of the class is left
    searchParameters(PAR_PREFIX);
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "", "All 0");
  }
}
