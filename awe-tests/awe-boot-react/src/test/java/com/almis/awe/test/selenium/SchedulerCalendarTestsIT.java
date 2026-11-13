package com.almis.awe.test.selenium;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Calendars of the scheduler, one flow on its screens: create a calendar and update it with dates (the editable grid of
 * dates), deactivate and activate it (each button replaces the other one), and delete it. The class creates and deletes its
 * own calendar and does not need any other record or class; its tests are an ordered sequence on that record, and each one
 * opens its own screen. The stored data, dates and states are proven by {@code SchedulerCalendarMaintainTest}.
 */
@Tag("SchedulerIT")
@TestMethodOrder(MethodOrderer.MethodName.class)
class SchedulerCalendarTestsIT extends AbstractSchedulerTests {

  private static final String CALENDAR = "Holidays calendar";
  private static final String CALENDAR_UPDATED = "Holidays calendar updated";

  /**
   * Generate a new scheduler calendar and update it with two dates
   */
  @Test
  void t001_newAndUpdateSchedulerCalendar() {
    // Title
    setTestTitle("Generate a new scheduler calendar and update it with dates");

    createCalendar(CALENDAR, "Holidays calendar description");

    // Go to the calendar to update: the search finds the new calendar
    update(CALENDAR_CRITERION, CALENDAR, CONFIRM_BUTTON, SCHEDULER, CALENDARS_SCREEN);

    // Insert text
    writeText("Nom", CALENDAR_UPDATED);

    // Insert text
    writeText(DESCRIPTION, "Holidays calendar description updated");

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
   * Deactivate the calendar and activate it again: each button toggles the state of the selected calendar and is
   * replaced by the other one
   */
  @Test
  void t003_deactivateAndActivateCalendar() {
    // Title
    setTestTitle("Deactivate and activate calendar");

    // Select the calendar
    selectRow(CALENDAR, SCHEDULER, CALENDARS_SCREEN);

    // Deactivate it
    clickButton("ButDea", true);

    // Check success execution
    checkButtonNotVisible("ButDea");
    checkButtonVisible("ButAct");

    // Activate it again
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
