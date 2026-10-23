package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Criteria of the test screens: every criterion type, their dependencies and the reset of the criteria.
 * Every test opens its own screen, so each one can run on its own. The login and the module selection are checked
 * here once; the rest of the tests start from the state that the setup leaves.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@Tag("CRUDCriteriaMatrixIT")
class CriteriaTestsIT extends AbstractSessionTests {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final String DATE_RANGE_SCREEN = "criteria-date-range";
  private static final String DATE_RANGE = "DatRng";
  private static final String DATE_RANGE_REQUIRED = "DatRngReq";
  private static final String DATE_RANGE_READONLY = "DatRngRea";
  private static final String DATE_RANGE_SEARCH = "ButSea";
  private static final String REQUIRED_FROM = "01/01/2026";
  private static final String REQUIRED_TO = "31/01/2026";
  private static final String UPLOADER_SCREEN = "criteria-uploader";
  private static final String UPLOADER_DROP = "UplDrop";
  private static final String UPLOADER_CAMERA = "UplCam";
  private static final String UPLOADER_READONLY = "UplDropRea";
  private static final String PNG = "image/png";
  private static final String PIXEL_FILE = "pixel.png";
  // A 1x1 pixel png
  private static final String PIXEL_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

  CriteriaTestsIT() {
    super(Session.TEST_MODULE);
  }

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
    checkLogoutWithConfirmation();
  }

  /**
   * Select test module on select criterion
   */
  @Test
  @StartsFrom(Session.LOGGED_IN)
  void t002_selectTestModule() {
    // Title
    setTestTitle("Select test module: Test to select test module");

    // Select module
    selectModule("Test");

    // Check the menu of the module
    checkMenuOption("test", "Tests");
  }

  /**
   * Test criteria: initialization
   */
  @Test
  void t010_criteriaTest() {
    // Title
    setTestTitle("Test criteria: Initialization");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Wait for button
    waitForButton("ButRst");

    // Wait for value
    checkCriterionContents("Tar", "checkbox off");

    // Check selector
    checkSuggestContents("Sug", "test (Manager)");

    // Click button
    clickButton("ButRst");

    // Check selector
    checkSuggestContents("Sug", "test (Manager)");
  }

  /**
   * Test criteria: text criteria
   */
  @Test
  void t011_criteriaTestText() {
    // Title
    setTestTitle("Test criteria: Text criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text on criterion
    writeText("Txt", "Texto Normal");

    // Check text on criterion
    checkCriterionLabel("Unt", "Texto Normal");

    // Write text on criterion
    writeText("TxtReq", "Text Required");

  }

  /**
   * Test criteria: numeric criteria
   */
  @Test
  void t012_criteriaTestNumeric() {
    // Title
    setTestTitle("Test criteria: Numeric criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text on numeric
    writeText("Num", "10000");

    // Check text on criterion
    checkCriterionLabel("Unt", "Numeric");

    // Write text on numeric
    writeText("NumReq", "-20000");

  }

  /**
   * Test criteria: date and time criteria
   */
  @Test
  void t013_criteriaTestDate() {
    // Title
    setTestTitle("Test criteria: Date and time criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Select a date
    selectDate("Cal", "23/10/1978");

    // Select a date
    selectDate("CalReq", "23/10/1978");

    // Write hour
    writeText("Tim", "12:23:41");

    // Write hour
    writeText("TimReq", "12:23:41");

    // Click on date
    clickDate("FilCal");

    // Click on selector
    clickEnabledDatepickerDay();

    // Click on date
    clickDate("FilCalReq");

    // Click on selector
    clickEnabledDatepickerDay();

    // Check date contents
    checkCriterionContents("Cal", "23/10/1978");

    // Check time contents
    checkCriterionContents("TimReq", "12:23:41");

    // The date-format is the one of the value shown and of the value sent
    checkCriterionContents("CalFmt", "2024-03-15");
    selectDay("CalFmt", 20);
    checkCriterionContents("CalFmt", "2024-03-20");

    // Month view with its own format
    checkCriterionContents("CalMon", "03/2024");
    selectMonth("CalMon", "May");
    checkCriterionContents("CalMon", "05/2024");

    // Weekends and future dates disabled: a past working day is accepted
    checkCriterionContents("CalPas", "15/03/2024");
    selectDay("CalPas", 14);
    checkCriterionContents("CalPas", "14/03/2024");

    // Password with the label on the left
    writeText("Pwd", "secret");
    checkCriterionContents("Pwd", "secret");
  }

  /**
   * Test criteria: Suggest and select criteria
   */
  @Test
  void t014_criteriaTestSuggestSelect() {
    // Title
    setTestTitle("Test criteria: Suggest and select criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Select on selector
    suggest("Sug", "test", "test");

    // Wait for loader
    waitForLoadingBar();

    // Select on selector
    selectContain("SelDep", "Yes");

    // Wait for loader
    waitForLoadingBar();

    // Pause
    pause(250);

    // Select on selector
    selectContain("SelDepDep", "Yes");

    // Select on selector
    suggest("SugReq", "a", "a");

    // Select on selector
    selectContain("Sel", "No");

    // Select on selector
    suggest("SugNum", "1", "1");

    // Select on selector
    selectContain("Sel", "Yes");

    // Select on selector
    selectContain("SelReq", "Administrator");

    // Check select
    checkSelectContents("SelReq", "Administrator");
  }

  /**
   * Test criteria: textarea criteria
   */
  @Test
  void t015_criteriaTestTextarea() {
    // Title
    setTestTitle("Test criteria: Textarea criteria");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text
    writeText("Tar", "Area de Texto");

    // Write text
    writeText("TarReq", "Area de Texto");

    // Check text
    checkCriterionContents("Tar", "Area de Texto");

    // Check text
    checkCriterionContents("TarReq", "Area de Texto");
  }

  /**
   * Test criteria: suggest multiple criteria
   */
  @Test
  void t016_criteriaTestSelectSuggestMultiple() {

    // Title
    setTestTitle("Test criteria: Select and suggest multiple");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Select on selector
    suggestMultiple("SelMul", "test@", "test@");

    // Verify text
    checkMultipleSelectorContents("SelMul", "test (test@test.com)");

    // Select on selector
    suggestMultiple("SelMulReq", "e", "e");

    // Verify text
    checkMultipleSelectorContents("SelMulReq", "General");

    // Select on selector
    suggestMultiple("SugMul", "test", "test");

    // Verify text
    checkMultipleSelectorContents("SugMul", "test (test@test.com)");

    // Select on selector
    suggestMultiple("SugMulReq", "test", "test");

    // Verify text
    checkMultipleSelectorContents("SugMulReq", "test (test@test.com)");
  }

  /**
   * Test criteria: Checkbox and radio
   */
  @Test
  void t017_criteriaTestCheckboxRadio() {

    // Title
    setTestTitle("Test criteria: Checkbox and radio");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Click checkbox
    clickCheckbox("ChkBoxVa1");

    // Check text on criterion
    checkCriterionLabel("Unt", "Inf");

    // Click checkbox
    clickCheckbox("ChkBoxVa2");

    // Check text on criterion
    checkCriterionUnit("Unt", "Inf");

    // Click checkbox
    clickCheckbox("ChkBoxVa5");

    // Click checkbox
    clickCheckbox("RadBox4");

    // Click checkbox
    clickCheckbox("RadBox1");

    // Check text on criterion
    checkCriterionUnit("Unt", "EUR");

    // Click checkbox
    clickCheckbox("RadBox3");

    // Check text on criterion
    checkCriterionUnit("Unt", "USD");

    // Click checkbox
    clickCheckbox("RadBox4");

    // Click an option of the button radio (this screen groups them in one criterion)
    clickCheckboxOption("RadBoxButGrp", "Radio5");
  }

  /**
   * Test criteria: Dependencies
   */
  @Test
  void t018_criteriaTestDependencies() {

    // Title
    setTestTitle("Test criteria: Dependencies");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // Write text
    writeText("Txt", "radios");

    // Click checkbox
    clickCheckboxOption("ChkBoxButGrp", "ChkBoxVa21");

    // Wait for value
    checkCriterionContents("Tar", "checkbox on");

    // Click checkbox
    clickCheckboxOption("ChkBoxButGrp", "ChkBoxVa21");

    // Wait for value
    checkCriterionContents("Tar", "checkbox off");

    // Click button
    clickButton("ButCnf");

    // Wait for error message
    checkValidationErrorVisible();

    // Check suggest value
    checkSuggestContents("SugRea", "test (Manager)");

    // Click button
    clickButton("ButRst");

    // Check value
    checkCriterionContents("Num", "-123.456,10 EUR");

    // Check value
    checkCriterionContents("NumReq", "-123.456,10 EUR");

    // Check checked (the button groups of this screen have no option checked by default)
    checkCheckboxRadio(true, "ChkBoxVa5", "RadBox1");

    // Check not checked
    checkCheckboxRadio(false, "ChkBoxVa1", "ChkBoxVa2", "RadBox4");
  }

  /**
   * Test criteria: a target message is shown as a popover over its criterion
   */
  @Test
  void t019_criteriaTestTargetMessage() {

    // Title
    setTestTitle("Test criteria: Target message");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-test");

    // The dependency of the criterion launches a server action that answers with a target message
    writeText("Txt", "popover");

    // Check the popover over the criterion
    checkMessagePopover("Txt", "Target message", "Shown over the criterion");

    // Dismiss it as a user does: the test does not depend on the timeout of a warning
    dismissMessagePopover("Txt");
  }

  /**
   * Criteria reset test
   */
  @Test
  void t020_criteriaReset() {
    // Title
    setTestTitle("Criteria reset");

    // Go to screen
    gotoScreen("test", "criteria", "criteria-reset");

    // Wait for button
    waitForButton("ButRst");

    // Write text
    writeText("CrtTst", "test");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");

    // Click button
    clickButton("ButRstTar");

    // Wait for button
    waitForButton("ButRst");

    // Check text
    checkCriterionContents("CrtTst", "1");

    // Click button
    clickButton("ButRst");

    // Wait for button
    waitForButton("ButTxt");

    // Check text
    checkCriterionContents("CrtTst", "xml");

    // Click button
    clickButton("ButTxt");

    // Wait for button
    waitForButton("ButRstSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "otra cosa");

    // Click button
    clickButton("ButRstSpe");

    // Wait for button
    waitForButton("ButTxt");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");

    // Click button
    clickButton("ButTxt");

    // Wait for button
    waitForButton("ButRstTarSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "otra cosa");

    // Click button
    clickButton("ButRstTarSpe");

    // Wait for button
    waitForButton("ButRst");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");

    // Click button
    clickButton("ButRst");

    // Wait for button
    waitForButton("ButTxt");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "");

    // Check text
    checkCriterionContents("CrtTstHid", "RstTst");

    // Click button
    clickButton("ButTxt");

    // Wait for button
    waitForButton("ButRstSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "otra cosa");

    // Click button
    clickButton("ButRstSpe");

    // Wait for button
    waitForButton("ButRstSpe");

    // Check text
    checkCriterionContents("CrtTstTxtHid", "RstTst");
  }

  /**
   * Date range criterion: default dates, read-only range, presets, the dates the search sends and the required range
   */
  @Test
  void t030_criteriaDateRange() {
    // Title
    setTestTitle("Date range criterion: defaults, presets and search");

    // Go to screen
    gotoScreen("test", "criteria", DATE_RANGE_SCREEN);

    // Wait for button
    waitForButton(DATE_RANGE_SEARCH);

    // Check the default dates: none, the ones of the required range and the ones of the read-only range
    checkDateRange(DATE_RANGE, "", "");
    checkDateRange(DATE_RANGE_REQUIRED, REQUIRED_FROM, REQUIRED_TO);
    checkDateRange(DATE_RANGE_READONLY, "01/02/2026", "28/02/2026");

    // The read-only range cannot be edited and the range without presets offers none
    checkDateRangeDisabled(DATE_RANGE_READONLY);
    checkDateRangePresetCount("DatRngNoPre", 0);
    checkDateRangePresetCount(DATE_RANGE, 6);

    // Click on the preset of the last month: the end is today, the start is one month before and the preset is active
    LocalDate today = LocalDate.now();
    clickDateRangePreset(DATE_RANGE, "1M");
    checkDateRange(DATE_RANGE, today.minusMonths(1).format(DATE_FORMAT), today.format(DATE_FORMAT));
    checkDateRangePresetActive(DATE_RANGE, "1M");

    // Search: the dates of the range reach the server as two parameters
    clickButton(DATE_RANGE_SEARCH);
    checkRowContents(today.minusMonths(1).format(DATE_FORMAT), today.format(DATE_FORMAT), REQUIRED_FROM, REQUIRED_TO);

    // Click on the preset of the year to date
    clickDateRangePreset(DATE_RANGE, "YTD");
    checkDateRange(DATE_RANGE, today.withDayOfYear(1).format(DATE_FORMAT), today.format(DATE_FORMAT));
    checkDateRangePresetActive(DATE_RANGE, "YTD");

    // Click on the preset that clears the range
    clickDateRangePreset(DATE_RANGE, "ALL");
    checkDateRange(DATE_RANGE, "", "");
    checkDateRangePresetActive(DATE_RANGE, "ALL");

    // Clear the start of the required range and search: the validation fails
    clearDateRangeFrom(DATE_RANGE_REQUIRED);
    clickButton(DATE_RANGE_SEARCH);
    checkValidationErrorVisible();
  }

  /**
   * Uploader criterion: drop zone, preview of an image, read-only drop zone and camera capture
   */
  @Test
  void t032_criteriaUploader(@TempDir Path directory) throws IOException {
    // Title
    setTestTitle("Uploader criterion: drop zone and camera capture");

    // The picture that the camera returns
    Path photo = Files.write(directory.resolve("capture.png"), Base64.getDecoder().decode(PIXEL_PNG));

    // Go to screen
    gotoScreen("test", "criteria", UPLOADER_SCREEN);

    // Nothing is uploaded yet
    checkNoUploadedFile(UPLOADER_DROP);

    // Drop an image: its name and a preview of it are shown
    dropFile(UPLOADER_DROP, PIXEL_FILE, PNG, PIXEL_PNG);
    checkUploadedFile(UPLOADER_DROP, PIXEL_FILE);
    checkUploadPreview(UPLOADER_DROP);

    // Clear the file: the name and the preview disappear
    clearUpload(UPLOADER_DROP);
    checkNoUploadedFile(UPLOADER_DROP);
    checkNoUploadPreview(UPLOADER_DROP);

    // Drop a document: its name is shown but there is no preview
    dropFile(UPLOADER_DROP, "document.pdf", "application/pdf",
      Base64.getEncoder().encodeToString("%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
    checkUploadedFile(UPLOADER_DROP, "document.pdf");
    checkNoUploadPreview(UPLOADER_DROP);
    clearUpload(UPLOADER_DROP);
    checkNoUploadedFile(UPLOADER_DROP);

    // A read-only uploader ignores what is dropped on it
    dropFile(UPLOADER_READONLY, PIXEL_FILE, PNG, PIXEL_PNG);
    pause(1000);
    checkNoUploadedFile(UPLOADER_READONLY);
    checkNoUploadPreview(UPLOADER_READONLY);

    // The uploader with a capture offers the camera button, the one with a drop zone only does not
    checkUploadCaptureButton(UPLOADER_CAMERA, true);
    checkUploadCaptureButton(UPLOADER_DROP, false);

    // Choose the picture with the camera input: its name and a preview of it are shown
    chooseCapturedFile(UPLOADER_CAMERA, photo.toString());
    checkUploadedFile(UPLOADER_CAMERA, "capture.png");
    checkUploadPreview(UPLOADER_CAMERA);
  }
}
