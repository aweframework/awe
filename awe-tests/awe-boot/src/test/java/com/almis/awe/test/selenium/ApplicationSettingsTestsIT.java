package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Settings of the application: sequences, application parameters and queues, one flow per screen. The flows of the two
 * administration screens (sequence-admin and application-parameter-admin) create two records through the side panel,
 * check the search and the quick filters (no query test runs them), delete one record from the selection bar, edit the
 * other from the pencil of its row and delete it from the row menu. Each flow opens its screen once, searches the words
 * of its own records and deletes the ones a failed attempt left, in the screen it is on. The themes have their own screen;
 * the side-panel admin pattern itself is proven by UserAdminTestsIT.
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
  private static final String COUNT_PREFIX = KEY_PREFIX + " cnt";
  private static final String COUNT_ACTIVE_SEQUENCE = COUNT_PREFIX + " act";
  private static final String COUNT_INACTIVE_SEQUENCE = COUNT_PREFIX + " ina";
  private static final String SEQUENCE_VALUE = "7";
  private static final String INACTIVE_SEQUENCE_VALUE = "8";
  private static final String NEW_SEQUENCE_VALUE = "9";

  // Application parameters screen (application-parameter-admin)
  private static final String PAR_GRID = "GrdAppParAdm";
  private static final String PAR_SEARCH = "CrtAppParTxt";
  private static final String PAR_QUICK_FILTER = "CrtAppParSta";
  private static final String PAR_PANEL = "PnlAppPar";
  private static final String PAR_PREFIX = "Appparadmtest";
  private static final String COUNT_PREFIX_PAR = PAR_PREFIX + " cnt";
  private static final String COUNT_ACTIVE_PARAMETER = COUNT_PREFIX_PAR + " act";
  private static final String COUNT_INACTIVE_PARAMETER = COUNT_PREFIX_PAR + " ina";
  private static final String PARAMETER_VALUE = "1";
  private static final String INACTIVE_PARAMETER_VALUE = "2";
  private static final String NEW_PARAMETER_VALUE = "0";
  private static final String GENERAL = "General";
  private static final String SECURITY = "Security";
  private static final String REPORTS = "Reports";
  private static final String DESCRIPTION = "Value of a parameter of the tests";
  private static final String NEW_DESCRIPTION = "Value of a parameter of the tests, updated";
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
   * Open the sequences screen, search the words of the sequences of a test and delete the ones that a failed attempt of
   * the test left: the grid keeps the search after every save and delete, so the test goes on in it
   *
   * @param search    Words of the sequences of the test
   * @param leftovers Sequences of the test
   */
  private void openSequences(String search, String... leftovers) {
    openToolsScreen(SEQUENCES);
    searchGrid(KEY_SEARCH, search);
    for (String name : leftovers) {
      if (hasRowContentsGrid(KEY_GRID, name)) {
        deleteSequenceFromTheRowMenu(name);
      }
    }
  }

  /**
   * Open the side panel of a new sequence: no selected sequence, so the header shows the label of the panel, and it
   * starts active
   */
  private void openNewSequencePanel() {
    clickButton("ButNew");
    checkSidePanelOpen(KEY_PANEL, "right", "520px");
    checkSidePanelTitle(KEY_PANEL, "Sequence");
    checkCheckboxRadio(true, "Act");
  }

  /**
   * Create a sequence from the side panel of the open screen
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
   * Delete a sequence from the row menu of its row (the ellipsis selects only its row)
   *
   * @param name Name of the sequence
   */
  private void deleteSequenceFromTheRowMenu(String name) {
    clickGridRowButton(KEY_GRID, name, ROW_MENU_BUTTON);
    clickContextButton("CtxButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(KEY_GRID, name);
  }

  /**
   * The side panel creates two sequences and their rows show their name, their value and their state; the search of the
   * grid finds a sequence by its name ignoring case and the quick filters count the sequences of the search (All,
   * Active, Inactive) and show the ones of their state; the pencil of a row opens the panel in edit mode filled with
   * its sequence (whose header shows the sequence and its state) and its row shows the value and the state saved from
   * the panel; the selection bar deletes one and the row menu the other
   */
  @Test
  void t011_addSearchEditAndDeleteSequences() {
    setTestTitle("Add, search, edit and delete sequences from their screen");

    // The words of this test are its own, so its counters do not depend on the sequences other tests may have left
    openSequences(COUNT_PREFIX, COUNT_ACTIVE_SEQUENCE, COUNT_INACTIVE_SEQUENCE);
    createSequence(COUNT_ACTIVE_SEQUENCE, SEQUENCE_VALUE, true);
    checkRowContentsGrid(KEY_GRID, COUNT_ACTIVE_SEQUENCE, SEQUENCE_VALUE, "Active");
    createSequence(COUNT_INACTIVE_SEQUENCE, INACTIVE_SEQUENCE_VALUE, false);
    checkRowContentsGrid(KEY_GRID, COUNT_INACTIVE_SEQUENCE, INACTIVE_SEQUENCE_VALUE, "Inactive");

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
    searchGrid(KEY_SEARCH, COUNT_PREFIX);
    clickGridQuickFilterOption(KEY_QUICK_FILTER, "inactive");
    checkGridQuickFilterOptionChosen(KEY_QUICK_FILTER, "inactive");
    checkRowGone(KEY_GRID, COUNT_ACTIVE_SEQUENCE);
    checkRowContentsGrid(KEY_GRID, COUNT_INACTIVE_SEQUENCE);
    clickGridQuickFilterOption(KEY_QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(KEY_QUICK_FILTER, "");
    checkRowContentsGrid(KEY_GRID, COUNT_ACTIVE_SEQUENCE, COUNT_INACTIVE_SEQUENCE);

    // The selection bar deletes the selected sequences
    clickRowContents(KEY_GRID, COUNT_INACTIVE_SEQUENCE);
    checkGridSelectionBar(KEY_GRID, "1 selected");
    clickGridSelectionButton(KEY_GRID, "ButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(KEY_GRID, COUNT_INACTIVE_SEQUENCE);
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "", "All 1");

    clickGridRowButton(KEY_GRID, COUNT_ACTIVE_SEQUENCE, EDIT_BUTTON);
    checkSidePanelTitle(KEY_PANEL, COUNT_ACTIVE_SEQUENCE);
    checkSidePanelTag(KEY_PANEL, "Active");
    checkCriterionContents("SeqNam", COUNT_ACTIVE_SEQUENCE);
    checkCriterionContents("KeyVal", SEQUENCE_VALUE);
    checkCheckboxRadio(true, "Act");

    // The name of a sequence is its primary key and the panel shows it read-only when editing: only the value and the
    // state change
    writeText("KeyVal", NEW_SEQUENCE_VALUE);
    clickCheckbox("Act");
    checkCheckboxRadio(false, "Act");
    clickButtonAndConfirm("ButPnlUpd");
    waitForLoadingBar();
    checkRowContentsGrid(KEY_GRID, COUNT_ACTIVE_SEQUENCE, NEW_SEQUENCE_VALUE, "Inactive");
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "active", "Active 0");
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "inactive", "Inactive 1");

    deleteSequenceFromTheRowMenu(COUNT_ACTIVE_SEQUENCE);
    checkGridQuickFilterOption(KEY_QUICK_FILTER, "", "All 0");
  }

  /**
   * Add a queue, update it and delete it from the queues screen
   */
  @Test
  void t061_queueFromCreationToDeletion() {
    setTestTitle("Add, update and delete a queue");

    addNew(TOOLS, "queues");
    writeText("Als", "Queue test");
    writeText("JmsBrk", "Broker");
    writeText("DstNam", "Destination");
    selectContain("ConTyp", "JNDI");
    writeText("Des", "Queue description");
    clickButtonAndConfirm("ButCnf");

    // The screen goes back to the list of queues
    clickButton("ButRst");
    suggest("CrtAls", "test", "test");
    searchAndWait();
    checkRowContents("Queue test", "Broker", "Destination", "JNDI", "Queue description");

    // Update the queue of the list
    clickRowContents("Queue test");
    clickButton("ButUpd", true);
    waitForButton("ButCnf");
    waitForLoadingBar();
    writeText("Als", "Queue changed");
    writeText("JmsBrk", "New broker");
    writeText("DstNam", "New destination");
    writeText("Des", "Queue changed description");
    clickButtonAndConfirm("ButCnf");

    clickButton("ButRst");
    suggest("CrtAls", "changed", "changed");
    searchAndWait();
    checkRowContents("Queue changed", "New broker", "New destination", "JNDI", "Queue changed description");

    // Delete the queue of the list
    clickRowContents("Queue changed");
    clickButtonAndConfirm("ButDel");
    verifyDeleted("Queue changed");
  }

  /**
   * Open the application parameters screen, search the words of the parameters of a test and delete the ones that a
   * failed attempt of the test left: the grid keeps the search after every save and delete, so the test goes on in it
   *
   * @param search    Words of the name or the description of the parameters of the test
   * @param leftovers Parameters of the test
   */
  private void openParameters(String search, String... leftovers) {
    openToolsScreen(PARAMETERS);
    searchGrid(PAR_SEARCH, search);
    for (String name : leftovers) {
      if (hasRowContentsGrid(PAR_GRID, name)) {
        deleteParameterFromTheRowMenu(name);
      }
    }
  }

  /**
   * Open the side panel of a new parameter: no selected parameter, so the header shows the label of the panel, and it
   * starts in the general category and active, the defaults of the table
   */
  private void openNewParameterPanel() {
    clickButton("ButNew");
    checkSidePanelOpen(PAR_PANEL, "right", "560px");
    checkSidePanelTitle(PAR_PANEL, "Application parameter");
    checkSelectContents("Cat", GENERAL);
    checkCheckboxRadio(true, "Act");
  }

  /**
   * Create an application parameter from the side panel of the open screen
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
   * Delete a parameter from the row menu of its row
   *
   * @param name Name of the parameter
   */
  private void deleteParameterFromTheRowMenu(String name) {
    clickGridRowButton(PAR_GRID, name, ROW_MENU_BUTTON);
    clickContextButton("CtxButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(PAR_GRID, name);
  }

  /**
   * The side panel creates two application parameters and their rows show their name with the description under it,
   * their value, their category and their state; the search of the grid finds a parameter by its description ignoring
   * case and the quick filters count the parameters of the search (All, Active, Inactive) and show the ones of their
   * state; the pencil of a row opens the panel in edit mode filled with its parameter (whose header shows the parameter
   * and its state) and its row shows the value, the category and the state saved from the panel; the selection bar and
   * the row menu delete one each
   */
  @Test
  void t041_addSearchEditAndDeleteParameters() {
    setTestTitle("Add, search, edit and delete application parameters from their screen");

    // The words of this test are its own, so its counters do not depend on the parameters other tests may have left
    openParameters(COUNT_PREFIX_PAR, COUNT_ACTIVE_PARAMETER, COUNT_INACTIVE_PARAMETER);
    createParameter(COUNT_ACTIVE_PARAMETER, PARAMETER_VALUE, SECURITY, DESCRIPTION, true);
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER, DESCRIPTION, PARAMETER_VALUE, SECURITY, "Active");
    createParameter(COUNT_INACTIVE_PARAMETER, INACTIVE_PARAMETER_VALUE, REPORTS, NEW_COUNT_DESCRIPTION, false);
    checkRowContentsGrid(PAR_GRID, COUNT_INACTIVE_PARAMETER, NEW_COUNT_DESCRIPTION, INACTIVE_PARAMETER_VALUE, REPORTS,
      "Inactive");

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
    searchGrid(PAR_SEARCH, COUNT_PREFIX_PAR);
    clickGridQuickFilterOption(PAR_QUICK_FILTER, "active");
    checkGridQuickFilterOptionChosen(PAR_QUICK_FILTER, "active");
    checkRowGone(PAR_GRID, COUNT_INACTIVE_PARAMETER);
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER);
    clickGridQuickFilterOption(PAR_QUICK_FILTER, "");
    checkGridQuickFilterOptionChosen(PAR_QUICK_FILTER, "");
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER, COUNT_INACTIVE_PARAMETER);

    // The selection bar deletes the selected parameters
    clickRowContents(PAR_GRID, COUNT_INACTIVE_PARAMETER);
    checkGridSelectionBar(PAR_GRID, "1 selected");
    clickGridSelectionButton(PAR_GRID, "ButDel");
    acceptConfirm();
    waitForLoadingBar();
    checkRowGone(PAR_GRID, COUNT_INACTIVE_PARAMETER);
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "", "All 1");

    clickGridRowButton(PAR_GRID, COUNT_ACTIVE_PARAMETER, EDIT_BUTTON);
    checkSidePanelTitle(PAR_PANEL, COUNT_ACTIVE_PARAMETER);
    checkSidePanelTag(PAR_PANEL, "Active");
    checkCriterionContents("ParNam", COUNT_ACTIVE_PARAMETER);
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
    checkRowContentsGrid(PAR_GRID, COUNT_ACTIVE_PARAMETER, NEW_DESCRIPTION, NEW_PARAMETER_VALUE, GENERAL,
      "Inactive");
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "active", "Active 0");
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "inactive", "Inactive 1");

    deleteParameterFromTheRowMenu(COUNT_ACTIVE_PARAMETER);
    checkGridQuickFilterOption(PAR_QUICK_FILTER, "", "All 0");
  }
}
