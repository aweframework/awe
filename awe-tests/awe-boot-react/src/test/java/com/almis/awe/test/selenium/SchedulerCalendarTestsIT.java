package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Calendars of the scheduler: create, update (with dates), deactivate, activate and delete. The class creates and deletes
 * its own calendar and does not need any other record or class. The tests of the calendar are an ordered sequence on one
 * record, and each one opens its own screen.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerCalendarTestsIT extends AbstractSchedulerTests {

  private static final String CALENDAR = "Holidays calendar";
  private static final String CALENDAR_UPDATED = "Holidays calendar updated";

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
    // The application asks for a confirmation before logging out
    checkLogoutWithConfirmation();
  }

  /**
   * Generate a new scheduler calendar
   */
  @Test
  void t001_newSchedulerCalendar() {
    // Title
    setTestTitle("Generate a new scheduler calendar");

    createCalendar(CALENDAR, "Holidays calendar description");


    // Check element

    verifyNew(CALENDAR_CRITERION, CALENDAR);
  }

  /**
   * Update a scheduler calendar
   */
  @Test
  void t002_updateSchedulerCalendar() {
    // Title
    setTestTitle("Update a scheduler calendar");

    // Go to the calendar to update
    update(CALENDAR_CRITERION, CALENDAR, CONFIRM_BUTTON, SCHEDULER, CALENDARS_SCREEN);

    // Insert text
    writeText("Nom", CALENDAR_UPDATED);

    // Insert text
    writeText(DESCRIPTION, "Holidays calendar description updated");

    // Suggest on selector
    selectContain("Act", "Yes");

    // Add dates
    clickButton("ButDatAdd");

    // Select date
    selectDay("GrdDatLst", "Dat", 1);

    // Write name
    writeText("GrdDatLst", NAME, "First day of month");

    // Add dates
    clickButton("ButDatAdd");

    // Select date
    selectDay("GrdDatLst", "Dat", 3);

    // Write name
    writeText("GrdDatLst", NAME, "Third day of month");

    // Save row
    saveRow();

    // Store and confirm
    clickButtonAndConfirm(CONFIRM_BUTTON);

    // Check element
    verifyNew(CALENDAR_CRITERION, CALENDAR_UPDATED);
  }

  /**
   * Deactivate a calendar
   */
  @Test
  void t003_deactivateCalendar() {
    // Title
    setTestTitle("Deactivate calendar");

    // Select the calendar
    selectRow(CALENDAR, SCHEDULER, CALENDARS_SCREEN);

    // Deactivate it
    clickButton("ButDea", true);

    // Check success execution
    checkButtonNotVisible("ButDea");
    checkButtonVisible("ButAct");
  }

  /**
   * Activate a calendar
   */
  @Test
  void t004_activateCalendar() {
    // Title
    setTestTitle("Activate calendar");

    // Select the calendar
    selectRow(CALENDAR, SCHEDULER, CALENDARS_SCREEN);

    // Activate it
    clickButton("ButAct", true);

    // Check success execution
    checkButtonNotVisible("ButAct");
    checkButtonVisible("ButDea");
  }

  /**
   * Delete the scheduler calendar
   */
  @Test
  void t901_deleteSchedulerCalendar() {
    // Title
    setTestTitle("Delete the scheduler calendar");

    // Delete the calendar
    delete(CALENDAR_CRITERION, CALENDAR, SCHEDULER, CALENDARS_SCREEN);

    // Verify deleted calendar
    verifyDeleted(CALENDAR);
  }
}
