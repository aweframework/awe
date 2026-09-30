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
