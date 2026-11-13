import {useRef} from "react";
import {getViewMode, toCalendarFormat, toDate} from "../utilities/dates";

// Days of the week (Sunday and Saturday) disabled when the weekends are not shown
const WEEKENDS = [0, 6];

/**
 * Check that an option of the contract is explicitly disabled (false or "false")
 * @param {boolean|string} option Option
 * @returns {boolean} The option is disabled
 */
function isDisabled(option) {
  return option === false || option === "false";
}

/**
 * Date options of the contract of a date criterion or of a date column, as the AngularJS engine applies them:
 * date-format (also the format of the value sent to the server), date-view-mode, show-weekends,
 * show-future-dates (no dates after the date selected when the criterion starts) and date-show-today-button.
 * @param {object} options Options of the contract
 * @param {string} [options.dateFormat] Date format (bootstrap-datepicker tokens)
 * @param {string} [options.dateViewMode] Minimum view mode: days, months or years
 * @param {boolean} [options.showWeekends] Show the weekends
 * @param {boolean} [options.showFutureDates] Show the dates after the date selected at the start
 * @param {boolean} [options.showTodayButton] Show the today button
 * @param {string} value Value of the criterion (used once, when the criterion starts)
 * @param {string} language Language
 * @returns {object} Format of the value and properties of the calendar
 */
export default function useDateOptions({dateFormat, dateViewMode, showWeekends, showFutureDates, showTodayButton}, value, language) {
  // AngularJS reads the selected value when the criterion starts, and that is the last date allowed
  const initialValue = useRef(value);
  const maxDate = isDisabled(showFutureDates) ? toDate(initialValue.current, dateFormat, language) : null;

  return {
    format: dateFormat,
    language,
    calendarProps: {
      dateFormat: toCalendarFormat(dateFormat),
      view: getViewMode(dateViewMode),
      disabledDays: isDisabled(showWeekends) ? WEEKENDS : undefined,
      maxDate: maxDate || undefined,
      // An undefined class would replace the default class of the button, so it is only sent to hide it
      ...(isDisabled(showTodayButton) && {todayButtonClassName: "hidden"})
    }
  };
}
