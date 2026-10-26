import _ from "lodash";

/**
 * Date utility functions
 * @category Utilities
 * @namespace Dates
 */

/**
 * Default format of the date criteria (bootstrap-datepicker format, the one of the AngularJS engine)
 * @type {string}
 * @memberOf Dates
 */
export const DEFAULT_DATE_FORMAT = "dd/mm/yyyy";

/**
 * Range of dates, as dd/mm/yyyy texts (empty when open)
 */
export interface DateRange {
  from: string;
  to: string;
}

/**
 * Part of a date format: a token of the format or the text between tokens
 */
interface FormatPart {
  token: boolean;
  text: string;
}

/**
 * Values read from a text while it is parsed with a date format
 */
interface ParsedDate {
  year: number;
  month: number;
  day: number;
}

/**
 * View of the calendar
 */
export type CalendarView = typeof VIEW_MODES[number];

/**
 * Model of the available dates: values that are dates as dd/mm/yyyy texts
 */
export type AvailableDatesModel = {value?: string | null}[];

// Tokens of the date-format of the contract (the ones of the bootstrap-datepicker plugin)
const FORMAT_TOKENS = /dd?|DD?|mm?|MM?|yy(?:yy)?/g;

// Tokens of the contract whose equivalent in the calendar is different (4 digit year is "yy" and 2 digit year is "y")
const CALENDAR_TOKENS: Record<string, string> = {yyyy: "yy", yy: "y"};

// Views of the calendar by the minimum view mode of the contract
const VIEW_MODES = ["date", "month", "year"] as const;
const VIEW_MODE_NAMES: Record<string, number> = {days: 0, months: 1, years: 2};

// Numeric tokens of the date format
const NUMERIC_TOKENS: Record<string, RegExp> = {d: /^\d{1,2}/, dd: /^\d{2}/, m: /^\d{1,2}/, mm: /^\d{2}/, yy: /^\d{2}/, yyyy: /^\d{4}/};

/**
 * Split a date format in tokens and separators
 * @param {string} format Date format
 * @returns {FormatPart[]} Parts of the format
 */
function splitFormat(format: string): FormatPart[] {
  const parts: FormatPart[] = [];
  let last = 0;
  for (const match of String(format).matchAll(FORMAT_TOKENS)) {
    if (match.index > last) {
      parts.push({token: false, text: format.slice(last, match.index)});
    }
    parts.push({token: true, text: match[0]});
    last = match.index + match[0].length;
  }
  if (last < format.length) {
    parts.push({token: false, text: format.slice(last)});
  }
  return parts;
}

/**
 * Convert the date-format of the contract (bootstrap-datepicker tokens) to the format of the calendar
 * @param {string} format Date format of the contract, "dd/mm/yyyy" by default
 * @returns {string} Format of the calendar
 * @memberOf Dates
 */
export function toCalendarFormat(format?: string | null): string {
  return splitFormat(format || DEFAULT_DATE_FORMAT)
    .map(({token, text}) => token
      ? CALENDAR_TOKENS[text] || text
      : text.replace(/[A-Za-z@!']+/g, (literal: string) => `'${literal.replaceAll("'", "''")}'`))
    .join("");
}

/**
 * Get the view of the calendar of a minimum view mode of the contract
 * @param {string|number} mode Date view mode: days, months or years (also 0, 1 or 2)
 * @returns {string} View of the calendar: date (default), month or year
 * @memberOf Dates
 */
export function getViewMode(mode: string | number): CalendarView {
  const index = mode in VIEW_MODE_NAMES ? VIEW_MODE_NAMES[mode] : Number(mode);
  return VIEW_MODES[index] || VIEW_MODES[0];
}

/**
 * Create a date formatter, falling back to English when Intl rejects the language tag
 * @param {string} language Language tag
 * @param {object} options Intl.DateTimeFormat options
 * @returns {Intl.DateTimeFormat} Formatter
 */
function createFormatter(language: string | undefined, options: Intl.DateTimeFormatOptions): Intl.DateTimeFormat {
  try {
    return new Intl.DateTimeFormat(language || "en", options);
  } catch {
    // A RangeError for an invalid tag must not break the render
    return new Intl.DateTimeFormat("en", options);
  }
}

/**
 * Get the names of the months or of the days of the week of a language
 * @param {string} language Language
 * @param {string} unit month or weekday
 * @param {string} width long or short
 * @returns {string[]} Names, with the first letter in uppercase
 */
function getNames(
  language: string | undefined,
  unit: "month" | "weekday",
  width: "long" | "short"
): string[] {
  const formatter = createFormatter(language, {[unit]: width, timeZone: "UTC"});
  const isMonth = unit === "month";
  return Array.from({length: isMonth ? 12 : 7}, (_value, index) => {
    const name = formatter.format(new Date(Date.UTC(2024, isMonth ? index : 0, isMonth ? 1 : index + 7))).replace(/\.$/, "");
    return name.charAt(0).toUpperCase() + name.slice(1);
  });
}

/**
 * Transform string to date
 * @param {string} date Date value
 * @param {string} [format] Date format of the contract (bootstrap-datepicker tokens), "dd/mm/yyyy" by default
 * @param {string} [language] Language of the month and day names
 * @returns {Date} Date or null if the value is empty or does not match the format
 * @memberOf Dates
 */
export function toDate(date?: string | null, format?: string | null, language?: string): Date | null {
  if (!date) {
    return null;
  }
  if (!format || format === DEFAULT_DATE_FORMAT) {
    return parseDefaultDate(String(date));
  }
  return parseDate(String(date), format, language);
}

/**
 * Parse a date in the default format (dd/mm/yyyy, the day and the month may have one digit)
 * @param {string} text Date value
 * @returns {Date|null} Date or null if the text is not a real date
 */
function parseDefaultDate(text: string): Date | null {
  const match = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(text.trim());
  if (!match) {
    return null;
  }
  const [day, month, year] = match.slice(1).map(Number);
  const date = new Date(year, month - 1, day);
  return date.getMonth() === month - 1 && date.getDate() === day ? date : null;
}

/**
 * Parse a date with a format
 * @param {string} text Date value
 * @param {string} format Date format
 * @param {string} language Language
 * @returns {Date|null} Date or null
 */
function parseDate(text: string, format: string, language: string | undefined): Date | null {
  const result: ParsedDate = {year: new Date().getFullYear(), month: 0, day: 1};
  let position = 0;
  for (const {token, text: part} of splitFormat(format)) {
    if (token) {
      const read = readToken(part, text.slice(position), language, result);
      if (read === null) {
        return null;
      }
      position += read;
    } else if (text.startsWith(part, position)) {
      position += part.length;
    } else {
      return null;
    }
  }
  const date = new Date(result.year, result.month, result.day);
  const valid = date.getMonth() === result.month && date.getDate() === result.day && position === text.length;
  return valid ? date : null;
}

/**
 * Read a token of the format at the start of a text and store its value
 * @param {string} token Format token
 * @param {string} rest Text to read
 * @param {string} language Language
 * @param {object} result Parsed values (year, month and day)
 * @returns {number|null} Number of characters read or null if the text does not match
 */
function readToken(token: string, rest: string, language: string | undefined, result: ParsedDate): number | null {
  return token in NUMERIC_TOKENS ? readNumber(token, rest, result) : readName(token, rest, language, result);
}

/**
 * Read a numeric token (day, month or year) at the start of a text and store its value
 * @param {string} token Format token
 * @param {string} rest Text to read
 * @param {object} result Parsed values (year, month and day)
 * @returns {number|null} Number of characters read or null if the text does not match
 */
function readNumber(token: string, rest: string, result: ParsedDate): number | null {
  const match = NUMERIC_TOKENS[token].exec(rest);
  if (!match) {
    return null;
  }
  const value = Number(match[0]);
  if (token.startsWith("d")) {
    result.day = value;
  } else if (token.startsWith("m")) {
    result.month = value - 1;
  } else {
    result.year = token === "yy" ? expandYear(value) : value;
  }
  return match[0].length;
}

/**
 * Read a month or weekday name at the start of a text and store the month
 * @param {string} token Format token
 * @param {string} rest Text to read
 * @param {string} language Language
 * @param {object} result Parsed values (year, month and day)
 * @returns {number|null} Number of characters read or null if the text does not match
 */
function readName(token: string, rest: string, language: string | undefined, result: ParsedDate): number | null {
  const isMonth = token.startsWith("M");
  const width = token.length === 2 ? "long" : "short";
  const names = getNames(language, isMonth ? "month" : "weekday", width);
  const index = names.findIndex(name => rest.toLowerCase().startsWith(name.toLowerCase()));
  if (index < 0) {
    return null;
  }
  if (isMonth) {
    result.month = index;
  }
  return names[index].length;
}

/**
 * Expand a two digits year (as the bootstrap-datepicker plugin does)
 * @param {number} year Two digits year
 * @returns {number} Year
 */
function expandYear(year: number): number {
  const expanded = 2000 + year;
  return expanded > new Date().getFullYear() + 10 ? expanded - 100 : expanded;
}

/**
 * Format a part of a date
 * @param {Date} date Date
 * @param {string} token Format token
 * @param {string} language Language
 * @returns {string} Part of the date
 */
function formatToken(date: Date, token: string, language: string | undefined): string {
  const pad = (value: number, length = 2) => String(value).padStart(length, "0");
  switch (token) {
    case "d": return String(date.getDate());
    case "dd": return pad(date.getDate());
    case "m": return String(date.getMonth() + 1);
    case "mm": return pad(date.getMonth() + 1);
    case "yy": return pad(date.getFullYear() % 100);
    case "yyyy": return pad(date.getFullYear(), 4);
    case "M": return getNames(language, "month", "short")[date.getMonth()];
    case "MM": return getNames(language, "month", "long")[date.getMonth()];
    case "D": return getNames(language, "weekday", "short")[date.getDay()];
    default: return getNames(language, "weekday", "long")[date.getDay()];
  }
}

/**
 * Transform date to string
 * @param {Date} date Date
 * @param {string} [format] Date format of the contract (bootstrap-datepicker tokens), "dd/mm/yyyy" by default.
 * It is the format of the value sent to the server
 * @param {string} [language] Language of the month and day names
 * @returns {string} Date formatted or null
 * @memberOf Dates
 */
export function fromDate(date?: Date | null, format?: string | null, language?: string): string | null {
  if (!date) {
    return null;
  }
  if (!format || format === DEFAULT_DATE_FORMAT) {
    return date.toLocaleDateString("fr-FR");
  }
  return splitFormat(format)
    .map(({token, text}) => token ? formatToken(date, text, language) : text)
    .join("");
}

/**
 * Transform date (time) to string
 * @param {Date} date
 * @returns {string}
 * @memberOf Dates
 */
export function fromTime(date?: Date | null): string | null {
  return date ? date.toLocaleTimeString("fr-FR") : null;
}

/**
 * Transform string to date (time)
 * @param {string} date
 * @returns {Date}
 * @memberOf Dates
 */
export function toTime(date?: string | null): Date | null {
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
export function getAvailableDates(model?: AvailableDatesModel | null): Date[] {
  return (model || [])
    .map(data => toDate(data.value))
    .filter((date): date is Date => Boolean(date))
    .sort((a, b) => a.getTime() - b.getTime());
}

/**
 * Get disabled dates
 * @param availableDates
 * @returns {Date[]}
 * @memberOf Dates
 */
export function getDisabledDates(availableDates?: readonly Date[] | null): Date[] {
  const disabledDates: Date[] = [];
  const availableDays = new Set((availableDates || []).map(toDayKey));

  // Retrieve all disabled dates in values between min and max
  let currentDate = getMinDate(availableDates);
  const maxDate = getMaxDate(availableDates);

  while (currentDate && maxDate && currentDate < maxDate) {
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
function toDayKey(date: Date): string {
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

/**
 * Get max available date
 * @param availableDates
 * @returns {Date}
 * @memberOf Dates
 */
export function getMaxDate(availableDates?: readonly Date[] | null): Date | null {
  const maxDate = _.last(availableDates);
  if (maxDate) {
    const nextDate = new Date(maxDate.getTime());
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
export function getMinDate(availableDates?: readonly Date[] | null): Date | null {
  const minDate = _.head(availableDates);
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
export function formatDate(date?: Date | null): string {
  if (!date) {
    return "";
  }
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
}

/**
 * Parse the list of presets of a date range criterion
 * @param {string} presets Presets separated by commas or spaces
 * @returns {string[]} Preset codes, upper-cased
 * @memberOf Dates
 */
export function parsePresets(presets?: string | null): string[] {
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
function subtractMonths(date: Date, months: number): Date {
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
function subtractPeriod(date: Date, amount: number, unit: string): Date {
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
 * @returns {DateRange|null} Range as dd/mm/yyyy dates or null when the preset is unknown
 * @memberOf Dates
 */
export function resolvePreset(code?: string | null, today: Date = new Date()): DateRange | null {
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
export function isDateRangeValid(from?: string | null, to?: string | null): boolean {
  if (!from || !to) {
    return true;
  }
  const start = toDate(from);
  const end = toDate(to);
  return start !== null && end !== null && end >= start;
}
