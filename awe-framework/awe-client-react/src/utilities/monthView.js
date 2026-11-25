/**
 * Month view of a list of dates (#993): pure functions from the rows of a grid of dates to the month the widget shows,
 * with its dates struck through, and to the strip of the months of the year with the number of dates of each one.
 *
 * Framework-agnostic: the AngularJS client imports this module (awe-client-react/utilities/monthView), so both engines
 * draw the same month for the same rows.
 */

// Operations of a row not saved yet: the React grid keeps them in $row.operation, the AngularJS one in RowTyp
const UNSAVED_OPERATIONS = ["INSERT", "UPDATE"];
const DELETE_OPERATION = "DELETE";
const DAYS_IN_WEEK = 7;
const MONTHS_IN_YEAR = 12;

// Locale keys of the texts of the month view
export const MONTH_VIEW_TEXTS = {
  previous: "MONTH_VIEW_PREVIOUS",
  next: "MONTH_VIEW_NEXT",
  legend: "MONTH_VIEW_LEGEND",
  strip: "MONTH_VIEW_STRIP",
  excluded: "MONTH_VIEW_EXCLUDED",
  unsaved: "MONTH_VIEW_UNSAVED",
  monthDates: "MONTH_VIEW_MONTH_DATES"
};

/**
 * Builds a day when it exists in the calendar
 * @param {number} year Year
 * @param {number} month Month (0 to 11)
 * @param {number} day Day of the month
 * @return {{year: number, month: number, day: number}|null} Day, or null when it does not exist
 */
function toDay(year, month, day) {
  const date = new Date(year, month, day);
  return date.getFullYear() === year && date.getMonth() === month && date.getDate() === day ? {year, month, day} : null;
}

/**
 * Reads the day of a date cell: a Date, a timestamp, a text dd/mm/yyyy (the AWE date format) or yyyy-mm-dd (ISO, with
 * or without time), or a cell object with that value
 * @param {*} value Cell value
 * @return {{year: number, month: number, day: number}|null} Day, or null when the value is not a date
 */
export function parseMonthViewDate(value) {
  if (value !== null && typeof value === "object" && !(value instanceof Date)) {
    return parseMonthViewDate(value.value);
  }
  if (value instanceof Date || typeof value === "number") {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : {year: date.getFullYear(), month: date.getMonth(), day: date.getDate()};
  }
  const text = typeof value === "string" ? value.trim() : "";
  const local = /^(\d{1,2})\/(\d{1,2})\/(\d{4})/.exec(text);
  if (local) {
    return toDay(Number(local[3]), Number(local[2]) - 1, Number(local[1]));
  }
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(text);
  return iso ? toDay(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3])) : null;
}

/**
 * Key of a day, sortable as a text
 * @param {{year: number, month: number, day: number}} day Day
 * @return {string} yyyy-mm-dd
 */
const dayKey = ({year, month, day}) => `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;

/**
 * Text of a cell of the name column
 * @param {*} value Cell value
 * @return {string} Name, or an empty text
 */
function readName(value) {
  const text = value !== null && typeof value === "object" ? value.label ?? value.value : value;
  return text === null || text === undefined ? "" : String(text).trim();
}

/**
 * Operation of a row of the grid not saved yet, in either engine
 * @param {object} row Grid row
 * @return {string|undefined} Operation (INSERT, UPDATE, DELETE) or nothing
 */
const rowOperation = row => row?.$row?.operation || row?.RowTyp || undefined;

/**
 * Reads the dates of the rows of a grid, the rows not saved yet included and the deleted ones left out, sorted by day
 * @param {object[]} rows Current rows of the grid
 * @param {{dateColumn: string, nameColumn: string}} columns Column of the date and, optionally, of its name
 * @return {object[]} Dates: year, month, day, key (yyyy-mm-dd), name and whether it is unsaved
 */
export function readMonthDates(rows, {dateColumn, nameColumn} = {}) {
  return (Array.isArray(rows) ? rows : [])
    .filter(row => row && rowOperation(row) !== DELETE_OPERATION)
    .map(row => ({row, day: parseMonthViewDate(row[dateColumn])}))
    .filter(({day}) => day)
    .map(({row, day}) => ({
      ...day,
      key: dayKey(day),
      name: nameColumn ? readName(row[nameColumn]) : "",
      unsaved: UNSAVED_OPERATIONS.includes(rowOperation(row))
    }))
    .sort((first, second) => first.key.localeCompare(second.key));
}

/**
 * The month the view opens on: the month of the next date from today (today included), else the month of today
 * @param {object[]} dates Dates (readMonthDates)
 * @param {Date} today Today
 * @return {{year: number, month: number}} Month (0 to 11) and year
 */
export function getInitialMonth(dates, today) {
  const todayKey = dayKey({year: today.getFullYear(), month: today.getMonth(), day: today.getDate()});
  const next = dates.find(date => date.key >= todayKey);
  return next ? {year: next.year, month: next.month} : {year: today.getFullYear(), month: today.getMonth()};
}

/**
 * Moves a month forward or back
 * @param {{year: number, month: number}} shown Month
 * @param {number} delta Months to move (negative to go back)
 * @return {{year: number, month: number}} Month
 */
export function shiftMonth({year, month}, delta) {
  const date = new Date(year, month + delta, 1);
  return {year: date.getFullYear(), month: date.getMonth()};
}

/**
 * Number of dates of every month of a year
 * @param {object[]} dates Dates (readMonthDates)
 * @param {number} year Year
 * @return {number[]} Twelve counts, January first
 */
export function countDatesByMonth(dates, year) {
  const counts = Array(MONTHS_IN_YEAR).fill(0);
  dates.filter(date => date.year === year).forEach(date => {
    counts[date.month] += 1;
  });
  return counts;
}

/**
 * Formatter of a language, falling back to English when Intl rejects the language tag
 * @param {string} language Language tag
 * @param {object} options Intl.DateTimeFormat options
 * @return {Intl.DateTimeFormat} Formatter
 */
function createFormatter(language, options) {
  try {
    return new Intl.DateTimeFormat(language || "en", {...options, timeZone: "UTC"});
  } catch (error) {
    return new Intl.DateTimeFormat("en", {...options, timeZone: "UTC"});
  }
}

/**
 * Text with its first letter in uppercase and without the final dot of an abbreviation
 * @param {string} text Text
 * @return {string} Capitalized text
 */
const capitalize = text => {
  const name = text.replace(/\.$/, "");
  return name.charAt(0).toUpperCase() + name.slice(1);
};

/**
 * Localized names of the months and of the days of the week, the week starting on Monday
 * @param {string} language Language
 * @return {{months: {long: string, short: string}[], weekdays: {long: string, narrow: string}[]}} Names
 */
export function getMonthViewNames(language) {
  const monthLong = createFormatter(language, {month: "long"});
  const monthShort = createFormatter(language, {month: "short"});
  const dayLong = createFormatter(language, {weekday: "long"});
  const dayNarrow = createFormatter(language, {weekday: "narrow"});
  // 1 January 2024 is a Monday
  return {
    months: Array.from({length: MONTHS_IN_YEAR}, (_value, index) => {
      const date = new Date(Date.UTC(2024, index, 1));
      return {long: capitalize(monthLong.format(date)), short: capitalize(monthShort.format(date))};
    }),
    weekdays: Array.from({length: DAYS_IN_WEEK}, (_value, index) => {
      const date = new Date(Date.UTC(2024, 0, 1 + index));
      return {long: dayLong.format(date), narrow: dayNarrow.format(date).toUpperCase()};
    })
  };
}

/**
 * Replaces the placeholders {0}, {1}... of a text
 * @param {string} text Text
 * @param {...*} values Values
 * @return {string} Text with the values
 */
const format = (text, ...values) => values.reduce((result, value, index) =>
  result.split(`{${index}}`).join(String(value)), String(text));

/**
 * One day of the month: its number, whether it is a weekend day or today and, when it is struck through, the dates of
 * the day with the text that tells it (accessible name and tooltip), so the state is not told only by colour
 * @param {object} day Day (year, month, day)
 * @param {number} column Column of the week (0 is Monday)
 * @param {Map} byDay Dates by key
 * @param {string} todayKey Key of today
 * @param {function} translate Translates a locale key
 * @return {object} Day of the view
 */
function buildDay(day, column, byDay, todayKey, translate) {
  const key = dayKey(day);
  const dates = byDay.get(key) || [];
  const cell = {key, day: day.day, weekend: column >= 5, today: key === todayKey, excluded: dates.length > 0};
  if (!cell.excluded) {
    return cell;
  }
  const unsaved = translate(MONTH_VIEW_TEXTS.unsaved);
  const names = dates.map(date => [date.name, date.unsaved ? `(${unsaved})` : ""].filter(Boolean).join(" "))
    .filter(Boolean).join(", ");
  const excluded = translate(MONTH_VIEW_TEXTS.excluded);
  return {...cell, label: names ? `${day.day}, ${excluded}: ${names}` : `${day.day}, ${excluded}`, title: names || excluded};
}

/**
 * Weeks of a month, Monday first, with empty days before the first day and after the last one
 * @param {{year: number, month: number}} shown Month
 * @param {object[]} dates Dates (readMonthDates)
 * @param {Date} today Today
 * @param {function} translate Translates a locale key
 * @return {{key: string, days: object[]}[]} Weeks
 */
function buildWeeks({year, month}, dates, today, translate) {
  const byDay = dates.reduce((map, date) => map.set(date.key, [...(map.get(date.key) || []), date]), new Map());
  const todayKey = dayKey({year: today.getFullYear(), month: today.getMonth(), day: today.getDate()});
  const offset = (new Date(year, month, 1).getDay() + DAYS_IN_WEEK - 1) % DAYS_IN_WEEK;
  const length = new Date(year, month + 1, 0).getDate();
  const cells = Math.ceil((offset + length) / DAYS_IN_WEEK) * DAYS_IN_WEEK;
  const days = Array.from({length: cells}, (_value, index) => {
    const number = index - offset + 1;
    const column = index % DAYS_IN_WEEK;
    return number < 1 || number > length ? {key: `empty-${index}`, weekend: column >= 5}
      : buildDay({year, month, day: number}, column, byDay, todayKey, translate);
  });
  return Array.from({length: cells / DAYS_IN_WEEK}, (_value, week) =>
    ({key: `week-${week}`, days: days.slice(week * DAYS_IN_WEEK, (week + 1) * DAYS_IN_WEEK)}));
}

/**
 * Everything the month view draws: the title of the month, the texts of its buttons and legend, the names of the days,
 * the weeks with their struck days and the strip of the months of the year with their counts
 * @param {object} options Options
 * @param {object[]} options.dates Dates (readMonthDates)
 * @param {{year: number, month: number}} options.shown Month shown
 * @param {Date} options.today Today
 * @param {string} options.language Language of the names
 * @param {function} options.translate Translates a locale key
 * @param {string} [options.legend] Locale key of the legend (MONTH_VIEW_LEGEND by default)
 * @return {object} View of the month
 */
export function buildMonthView({dates, shown, today, language, translate = key => key, legend}) {
  const names = getMonthViewNames(language);
  const counts = countDatesByMonth(dates, shown.year);
  const monthDates = translate(MONTH_VIEW_TEXTS.monthDates);
  return {
    title: `${names.months[shown.month].long} ${shown.year}`,
    previous: translate(MONTH_VIEW_TEXTS.previous),
    next: translate(MONTH_VIEW_TEXTS.next),
    legend: translate(legend || MONTH_VIEW_TEXTS.legend),
    strip: translate(MONTH_VIEW_TEXTS.strip),
    weekdays: names.weekdays,
    weeks: buildWeeks(shown, dates, today, translate),
    months: names.months.map((name, month) => ({
      month,
      short: name.short,
      count: counts[month],
      pressed: month === shown.month,
      label: format(monthDates, `${name.long} ${shown.year}`, counts[month])
    }))
  };
}
