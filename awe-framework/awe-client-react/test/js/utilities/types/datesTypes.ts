// Type-level assertions for utilities/dates, checked by `npm run typecheck` (not run by Jest)
import {
  DEFAULT_DATE_FORMAT,
  DateRange,
  formatDate,
  fromDate,
  fromTime,
  getAvailableDates,
  getDisabledDates,
  getMaxDate,
  getMinDate,
  getViewMode,
  isDateRangeValid,
  parsePresets,
  resolvePreset,
  toCalendarFormat,
  toDate,
  toTime
} from "../../../../src/utilities/dates";

const format: "dd/mm/yyyy" = DEFAULT_DATE_FORMAT;
// @ts-expect-error the default format is a literal
const wrongFormat: "yyyy-mm-dd" = DEFAULT_DATE_FORMAT;

// A date is null when the value is empty or does not match the format
const date: Date | null = toDate("01/02/2024", "dd/mm/yyyy", "es");
const noValue: Date | null = toDate(null);
const emptyValue: Date | null = toDate(undefined, undefined, undefined);
// @ts-expect-error the date can be null
const alwaysDate: Date = toDate("01/02/2024");
// @ts-expect-error a date is read from text
toDate(new Date());

// The text of a date is null when there is no date
const text: string | null = fromDate(new Date(), "dd/mm/yyyy", "en");
const noDate: string | null = fromDate(null);
// @ts-expect-error the text can be null
const alwaysText: string = fromDate(new Date());
const time: string | null = fromTime(undefined);
const parsedTime: Date | null = toTime("10:20:30");
// @ts-expect-error a time is read from text
toTime(10);

// The format of the calendar and the view are text
const calendar: string = toCalendarFormat("dd/mm/yyyy");
const defaultCalendar: string = toCalendarFormat(undefined);
const view: "date" | "month" | "year" = getViewMode("months");
const viewByIndex: "date" | "month" | "year" = getViewMode(2);
// @ts-expect-error there is no such view
const wrongView: "week" = getViewMode(0);

// Available dates come from a model of values
const available: Date[] = getAvailableDates([{value: "01/02/2024"}, {value: null}]);
const noModel: Date[] = getAvailableDates(undefined);
const disabled: Date[] = getDisabledDates(available);
const max: Date | null = getMaxDate(available);
const min: Date | null = getMinDate(available);
// @ts-expect-error the minimum can be null
const alwaysMin: Date = getMinDate(available);

// Presets resolve to a range of texts, or to nothing when unknown
const formatted: string = formatDate(new Date());
const emptyFormatted: string = formatDate(null);
const presets: string[] = parsePresets("7D, 1M");
const noPresets: string[] = parsePresets(undefined);
const range: DateRange | null = resolvePreset("TODAY", new Date());
const rangeFrom: string | undefined = resolvePreset("ALL")?.from;
// @ts-expect-error the range can be null
const alwaysRange: DateRange = resolvePreset("TODAY");
const valid: boolean = isDateRangeValid("01/02/2024", "02/02/2024");
const open: boolean = isDateRangeValid("", undefined);
// @ts-expect-error the answer is a boolean
const validText: string = isDateRangeValid(null, null);

export {
  format, wrongFormat, date, noValue, emptyValue, alwaysDate, text, noDate, alwaysText, time, parsedTime, calendar,
  defaultCalendar, view, viewByIndex, wrongView, available, noModel, disabled, max, min, alwaysMin, formatted,
  emptyFormatted, presets, noPresets, range, rangeFrom, alwaysRange, valid, open, validText
};
