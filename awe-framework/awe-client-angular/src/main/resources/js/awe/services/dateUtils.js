import dayjs from "dayjs";

/**
 * Date helpers of the AngularJS client, built on day.js (they replace the former moment.js usage).
 * Dates travel as dd/mm/yyyy strings between the client and the server.
 */

// Format of the dates the server reads and writes
export const DATE_FORMAT = "DD/MM/YYYY";

const STRICT_DATE = /^(\d{2})\/(\d{2})\/(\d{4})$/;
// The lenient form keeps what moment accepted: one to four digits for the year, two digits meaning 1969-2068
const LENIENT_DATE = /^(\d{1,2})\/(\d{1,2})\/(\d{1,4})$/;
const TWO_DIGIT_YEAR_PIVOT = 68;

/**
 * Parse a dd/mm/yyyy date
 * @param {string} value Date to parse
 * @param {boolean} [strict=false] Require two digits for the day and the month
 * @returns {dayjs.Dayjs} Parsed date (check isValid(): impossible dates like 31/02/2020 are invalid)
 */
export function parseDate(value, strict = false) {
  const parts = (strict ? STRICT_DATE : LENIENT_DATE).exec(typeof value === "string" ? value.trim() : "");
  if (!parts) {
    return dayjs(NaN);
  }
  const [day, month] = [Number(parts[1]), Number(parts[2])];
  let year = Number(parts[3]);
  if (parts[3].length === 2) {
    year += year > TWO_DIGIT_YEAR_PIVOT ? 1900 : 2000;
  }
  // new Date(year, ...) maps the years 0-99 to the 1900s, so set the year apart
  const date = new Date(2000, month - 1, day);
  date.setFullYear(year);
  // The Date constructor rolls over impossible dates (31/02 becomes 02/03), so check that nothing moved
  const intact = date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
  return intact ? dayjs(date) : dayjs(NaN);
}

/**
 * Format a date
 * @param {Date|dayjs.Dayjs|string|number} date Date to format
 * @param {string} [format=DATE_FORMAT] day.js format
 * @returns {string} Formatted date
 */
export function formatDate(date, format = DATE_FORMAT) {
  return dayjs(date).format(format);
}
