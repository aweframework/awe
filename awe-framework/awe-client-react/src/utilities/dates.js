import _ from "lodash";

/**
 * Date utility functions
 * @category Utilities
 * @namespace Dates
 */

/**
 * Transform string to date
 * @param {string} date
 * @returns {Date}
 * @memberOf Dates
 */
export function toDate(date) {
  if (date) {
    const parts = String(date).split("/");
    return new Date(Number(parts[2]), Number(parts[1]) - 1, Number(parts[0]));
  }
  return null;
}

/**
 * Transform date to string
 * @param {Date} date
 * @returns {string}
 * @memberOf Dates
 */
export function fromDate(date) {
  return date ? date.toLocaleDateString("fr-FR") : null;
}

/**
 * Transform date (time) to string
 * @param {Date} date
 * @returns {string}
 * @memberOf Dates
 */
export function fromTime(date) {
  return date ? date.toLocaleTimeString("fr-FR") : null;
}

/**
 * Transform string to date (time)
 * @param {string} date
 * @returns {Date}
 * @memberOf Dates
 */
export function toTime(date) {
  if (date) {
    const parts = date.split(":");
    return new Date(0, 0, 0, Number(parts[0]), Number(parts[1]), Number(parts[2]) || 0);
  }
  return null;
}

/**
 * Get available dates
 * @param model Model
 * @returns {Date[]}
 * @memberOf Dates
 */
export function getAvailableDates(model) {
  return (model || [])
    .map(data => toDate(data.value))
    .sort((a, b) => a - b);
}

/**
 * Get disabled dates
 * @param availableDates
 * @returns {Date[]}
 * @memberOf Dates
 */
export function getDisabledDates(availableDates) {
  const disabledDates = [];
  const availableDays = new Set((availableDates || []).map(toDayKey));

  // Retrieve all disabled dates in values between min and max
  let currentDate = getMinDate(availableDates);
  const maxDate = getMaxDate(availableDates);

  while (currentDate && currentDate < maxDate) {
    if (!availableDays.has(toDayKey(currentDate))) {
      disabledDates.push(currentDate);
    }
    // A new Date for each day: the ones already collected must not change
    currentDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), currentDate.getDate() + 1);
  }

  return disabledDates;
}

/**
 * Get a key identifying the day of a date (ignoring its time)
 * @param {Date} date
 * @returns {string}
 */
function toDayKey(date) {
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

/**
 * Get max available date
 * @param availableDates
 * @returns {Date}
 * @memberOf Dates
 */
export function getMaxDate(availableDates) {
  let maxDate = _.last(availableDates);
  if (maxDate) {
    let nextDate = new Date(maxDate.getTime());
    nextDate.setDate(nextDate.getDate() + 1);
    return nextDate;
  }
  return null;
}

/**
 * Get min available date
 * @param availableDates
 * @returns {Date}
 * @memberOf Dates
 */
export function getMinDate(availableDates) {
  let minDate = _.head(availableDates);
  return minDate ? new Date(minDate.getTime()) : null;
}

const PRESET_SEPARATOR = /[\s,]+/;
const PRESET_PERIOD = /^(\d+)([DWMY])$/;
const PRESET_TODAY = "TODAY";
const PRESET_YEAR_TO_DATE = "YTD";
const PRESET_ALL = "ALL";

/**
 * Transform a date to a dd/mm/yyyy string
 * @param {Date} date
 * @returns {string} Date formatted, empty when there is no date
 * @memberOf Dates
 */
export function formatDate(date) {
  if (!date) {
    return "";
  }
  const pad = (value) => String(value).padStart(2, "0");
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
}

/**
 * Parse the list of presets of a date range criterion
 * @param {string} presets Presets separated by commas or spaces
 * @returns {string[]} Preset codes, upper-cased
 * @memberOf Dates
 */
export function parsePresets(presets) {
  return String(presets || "")
    .split(PRESET_SEPARATOR)
    .map(preset => preset.trim().toUpperCase())
    .filter(preset => preset.length > 0);
}

/**
 * Subtract months to a date keeping the last day of the month when the day does not exist
 * @param {Date} date Date
 * @param {number} months Months to subtract
 * @returns {Date} New date
 */
function subtractMonths(date, months) {
  const target = new Date(date.getFullYear(), date.getMonth() - months, 1);
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  return new Date(target.getFullYear(), target.getMonth(), Math.min(date.getDate(), lastDay));
}

/**
 * Subtract a period to a date
 * @param {Date} date Date
 * @param {number} amount Amount of units
 * @param {string} unit D (days), W (weeks), M (months) or Y (years)
 * @returns {Date} New date
 */
function subtractPeriod(date, amount, unit) {
  switch (unit) {
    case "D":
      return new Date(date.getFullYear(), date.getMonth(), date.getDate() - amount);
    case "W":
      return new Date(date.getFullYear(), date.getMonth(), date.getDate() - amount * 7);
    case "M":
      return subtractMonths(date, amount);
    default:
      return subtractMonths(date, amount * 12);
  }
}

/**
 * Resolve the dates of a quick preset of the date range criterion from the current date.
 * Supported presets: TODAY, YTD (year to date), ALL (no dates) and a period back from today: nD, nW, nM or nY
 * (days, weeks, months, years; for instance 7D, 1M, 3M, 12M, 3Y)
 * @param {string} code Preset code
 * @param {Date} today Current date
 * @returns {{from: string, to: string}|null} Range as dd/mm/yyyy dates or null when the preset is unknown
 * @memberOf Dates
 */
export function resolvePreset(code, today = new Date()) {
  const preset = String(code || "").toUpperCase();
  const period = PRESET_PERIOD.exec(preset);
  if (preset === PRESET_ALL) {
    return {from: "", to: ""};
  } else if (preset === PRESET_TODAY) {
    return {from: formatDate(today), to: formatDate(today)};
  } else if (preset === PRESET_YEAR_TO_DATE) {
    return {from: formatDate(new Date(today.getFullYear(), 0, 1)), to: formatDate(today)};
  } else if (period) {
    return {from: formatDate(subtractPeriod(today, Number(period[1]), period[2])), to: formatDate(today)};
  }
  return null;
}

/**
 * Check that the end of a range is not before its start (an open end is always valid)
 * @param {string} from Start date as dd/mm/yyyy
 * @param {string} to End date as dd/mm/yyyy
 * @returns {boolean} Range is valid
 * @memberOf Dates
 */
export function isDateRangeValid(from, to) {
  return !from || !to || toDate(to) >= toDate(from);
}
