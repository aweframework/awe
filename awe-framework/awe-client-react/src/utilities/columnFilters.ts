import { formatDate, toDate } from "./dates";

/**
 * Grid column autofilter (#912): the entries of the "columnFilters" request parameter built from the menu of a column,
 * and the helpers of the menu (operators, relative dates, values list, date tree and chip summary).
 */

/** Type of the filter a column declares (filter attribute of the column) */
export type ColumnFilterType = "values" | "text" | "number" | "date";

/** Union of the two conditions of a custom filter */
export type ColumnFilterUnion = "and" | "or";

/** Condition of a column filter: an operator of the closed list and its value (and the end of a range) */
export interface ColumnFilterCondition {
  operator: string;
  value?: string;
  to?: string;
}

/** Entry of the "columnFilters" request parameter: the filter of one column */
export interface ColumnFilter {
  id: string;
  fields?: string[];
  type: ColumnFilterType;
  values?: unknown[];
  empty?: boolean;
  union?: ColumnFilterUnion;
  conditions?: ColumnFilterCondition[];
}

/** A column of the column model, as the autofilter reads it */
export interface FilterableColumn {
  name: string;
  filter?: string | boolean | null;
  filterFields?: string | string[] | null;
  sortField?: string | null;
  label?: string;
  hidden?: boolean;
}

/** A value of the values list of a column, as the column-values action answers it */
export interface ColumnValue {
  value: unknown;
  label?: unknown;
  count?: unknown;
}

/** Checked state of a checkbox that groups other ones */
export type CheckState = "checked" | "unchecked" | "mixed";

/** Relative date of the date submenu */
export type RelativeDate = "today" | "yesterday" | "thisWeek" | "lastMonth" | "thisYear";

/** Node of the date tree of a date column: a year, a month or a day (only the days carry a value) */
export interface DateTreeNode {
  key: string;
  label: string;
  count: number;
  level: number;
  value?: unknown;
  children: DateTreeNode[];
}

/** Translation function */
type Translate = (key: string, options?: Record<string, unknown>) => string;

const FILTER_TYPES: readonly ColumnFilterType[] = ["values", "text", "number", "date"];

// Operators that need no value
const NO_VALUE_OPERATORS = new Set(["empty", "notEmpty"]);

// Operators of the custom filter of each type (the closed list of S3)
const TEXT_OPERATORS = ["eq", "ne", "startsWith", "notStartsWith", "endsWith", "notEndsWith", "contains", "notContains",
  "empty", "notEmpty"];
const COMPARISON_OPERATORS = ["eq", "ne", "gt", "ge", "lt", "le", "between", "empty", "notEmpty"];

/** Key of the empty values (null) in the values list: "(Blanks)" */
export const BLANKS_KEY = "\u0000blanks";

/** Most labels a chip shows before it only counts the others */
export const CHIP_LABELS = 3;

/** Operators of the type submenu of each type, in the order the menu shows them (the custom filter goes last) */
export const SUBMENU_OPERATORS: Readonly<Record<ColumnFilterType, readonly string[]>> = Object.freeze({
  values: [],
  text: ["eq", "ne", "startsWith", "endsWith", "contains", "notContains"],
  number: ["eq", "ne", "gt", "ge", "lt", "le", "between"],
  date: ["eq", "lt", "gt", "between"]
});

/** Relative dates of the date submenu, in the order the menu shows them */
export const RELATIVE_DATES: readonly RelativeDate[] = ["today", "yesterday", "thisWeek", "lastMonth", "thisYear"];

/** Locale key of each operator; the date columns name their comparisons as before and after */
const OPERATOR_LABELS: Readonly<Record<string, string>> = Object.freeze({
  eq: "GRID_FILTER_OPERATOR_EQ",
  ne: "GRID_FILTER_OPERATOR_NE",
  contains: "GRID_FILTER_OPERATOR_CONTAINS",
  notContains: "GRID_FILTER_OPERATOR_NOT_CONTAINS",
  startsWith: "GRID_FILTER_OPERATOR_STARTS_WITH",
  endsWith: "GRID_FILTER_OPERATOR_ENDS_WITH",
  notStartsWith: "GRID_FILTER_OPERATOR_NOT_STARTS_WITH",
  notEndsWith: "GRID_FILTER_OPERATOR_NOT_ENDS_WITH",
  gt: "GRID_FILTER_OPERATOR_GT",
  ge: "GRID_FILTER_OPERATOR_GE",
  lt: "GRID_FILTER_OPERATOR_LT",
  le: "GRID_FILTER_OPERATOR_LE",
  between: "GRID_FILTER_OPERATOR_BETWEEN",
  empty: "GRID_FILTER_OPERATOR_EMPTY",
  notEmpty: "GRID_FILTER_OPERATOR_NOT_EMPTY"
});
const DATE_OPERATOR_LABELS: Readonly<Record<string, string>> = Object.freeze({
  gt: "GRID_FILTER_OPERATOR_AFTER",
  ge: "GRID_FILTER_OPERATOR_ON_OR_AFTER",
  lt: "GRID_FILTER_OPERATOR_BEFORE",
  le: "GRID_FILTER_OPERATOR_ON_OR_BEFORE"
});

/** Locale key of each relative date */
export const RELATIVE_DATE_LABELS: Readonly<Record<RelativeDate, string>> = Object.freeze({
  today: "GRID_FILTER_TODAY",
  yesterday: "GRID_FILTER_YESTERDAY",
  thisWeek: "GRID_FILTER_THIS_WEEK",
  lastMonth: "GRID_FILTER_LAST_MONTH",
  thisYear: "GRID_FILTER_THIS_YEAR"
});

/**
 * Type of the filter a column declares
 * @param {object} column Column
 * @returns {string|null} Filter type, null when the column has no filter (or an unknown one)
 */
export function getColumnFilterType(column?: FilterableColumn | null): ColumnFilterType | null {
  const type = column?.filter;
  return typeof type === "string" && (FILTER_TYPES as readonly string[]).includes(type) ? type as ColumnFilterType : null;
}

/**
 * Fields a column filters: its filter-fields, else its sort-field, else its name (the server sort resolution, S2)
 * @param {object} column Column
 * @returns {string[]} Field identifiers
 */
export function getFilterFields(column: FilterableColumn): string[] {
  const declared = Array.isArray(column.filterFields) ? column.filterFields : String(column.filterFields ?? "").split(",");
  const fields = declared.map(field => String(field).trim()).filter(field => field.length > 0);
  if (fields.length > 0) {
    return fields;
  }
  return [column.sortField || column.name];
}

/**
 * Filter of a column in the column filters of a grid
 * @param {object[]} columnFilters Column filters
 * @param {string} id Column name
 * @returns {object|undefined} Filter of the column
 */
export function getColumnFilter(columnFilters: readonly ColumnFilter[] | null | undefined, id: string): ColumnFilter | undefined {
  return (columnFilters || []).find(filter => filter.id === id);
}

/**
 * Put the filter of a column in the column filters of a grid: it replaces the previous one in its place, or goes last;
 * no filter removes it
 * @param {object[]} columnFilters Column filters
 * @param {string} id Column name
 * @param {object|null} filter New filter of the column, null to remove it
 * @returns {object[]} New column filters
 */
export function setColumnFilter(columnFilters: readonly ColumnFilter[] | null | undefined, id: string,
                                filter: ColumnFilter | null): ColumnFilter[] {
  const current = [...(columnFilters || [])];
  const index = current.findIndex(item => item.id === id);
  if (!filter) {
    return current.filter(item => item.id !== id);
  }
  if (index < 0) {
    return [...current, filter];
  }
  current[index] = filter;
  return current;
}

/**
 * Key of a value in the values list: the text of the value, or {@link BLANKS_KEY} for the empty value
 * @param {*} value Value
 * @returns {string} Key
 */
export function getValueKey(value: unknown): string {
  return value === null || value === undefined ? BLANKS_KEY : String(value);
}

/**
 * Filter entry of the checklist of values (S15). Every value checked is no filter on values
 * @param {object} column Column
 * @param {object[]} values Values of the list
 * @param {Set<string>} checked Keys of the checked values ({@link getValueKey})
 * @param {boolean} [complete=true] The list has every value of the column (no search, no more values than shown)
 * @returns {object|null} Filter entry, null when it filters nothing
 */
export function buildValuesFilter(column: FilterableColumn, values: readonly ColumnValue[], checked: ReadonlySet<string>,
                                  complete: boolean = true): ColumnFilter | null {
  const type = getColumnFilterType(column);
  const shown = values.filter(item => checked.has(getValueKey(item.value)));
  if (!type || (complete && shown.length === values.length)) {
    return null;
  }
  const filter: ColumnFilter = {
    id: column.name,
    fields: getFilterFields(column),
    type,
    values: shown.filter(item => item.value !== null && item.value !== undefined).map(item => item.value)
  };
  if (shown.length !== filter.values?.length) {
    filter.empty = true;
  }
  return filter;
}

/**
 * Check that a condition can be sent: an operator of the type and, unless it needs none, a value of the type (and the
 * end of the range of between)
 * @param {object} condition Condition
 * @param {string} type Filter type
 * @param {string} [decimalSeparator="."] Decimal separator of the application, to read the numbers
 * @returns {boolean} The condition is complete
 */
export function isConditionComplete(condition: ColumnFilterCondition | null | undefined, type: ColumnFilterType,
                                    decimalSeparator = "."): boolean {
  if (!condition?.operator || !getOperators(type).includes(condition.operator)) {
    return false;
  }
  if (NO_VALUE_OPERATORS.has(condition.operator)) {
    return true;
  }
  const values = condition.operator === "between" ? [condition.value, condition.to] : [condition.value];
  return values.every(value => isValueOfType(value, type, decimalSeparator));
}

/**
 * Text of a number as the server reads it: the decimal separator of the application (a comma in Spanish or French)
 * becomes a point. Group separators are not accepted.
 * @param {string} value Number as the user typed it
 * @param {string} [decimalSeparator="."] Decimal separator of the application
 * @returns {string} Number with a point as decimal separator
 */
export function toServerNumber(value: string | undefined, decimalSeparator = "."): string {
  const text = String(value ?? "").trim();
  return decimalSeparator === "." ? text : text.replace(decimalSeparator, ".");
}

/**
 * Check that a text is a value of a filter type: a number, a dd/MM/yyyy date or any text that is not blank
 * @param {string} value Value
 * @param {string} type Filter type
 * @param {string} [decimalSeparator="."] Decimal separator of the application, to read the numbers
 * @returns {boolean} The value fits the type
 */
function isValueOfType(value: string | undefined, type: ColumnFilterType, decimalSeparator = "."): boolean {
  const text = type === "number" ? toServerNumber(value, decimalSeparator) : String(value ?? "").trim();
  if (text.length === 0) {
    return false;
  }
  switch (type) {
    case "number":
      return Number.isFinite(Number(text));
    case "date":
      return toDate(text) !== null;
    default:
      return true;
  }
}

/**
 * Filter entry of the conditions of the type submenu (S15): the complete ones, at most two, joined by their union
 * @param {object} column Column
 * @param {object[]} conditions Conditions
 * @param {string} [union="and"] Union of two conditions
 * @param {string} [decimalSeparator="."] Decimal separator of the application, to read the numbers
 * @returns {object|null} Filter entry, null when no condition is complete
 */
export function buildConditionsFilter(column: FilterableColumn, conditions: readonly (ColumnFilterCondition | null)[],
                                      union: ColumnFilterUnion = "and", decimalSeparator = "."): ColumnFilter | null {
  const type = getColumnFilterType(column);
  if (!type) {
    return null;
  }
  const complete = conditions
    .filter((condition): condition is ColumnFilterCondition => isConditionComplete(condition, type, decimalSeparator))
    .slice(0, 2)
    .map(({ operator, value, to }) => {
      if (NO_VALUE_OPERATORS.has(operator)) {
        return { operator };
      }
      const fixed = (text?: string) => type === "number" ? toServerNumber(text, decimalSeparator) : String(text ?? "").trim();
      return operator === "between" ? { operator, value: fixed(value), to: fixed(to) } : { operator, value: fixed(value) };
    });
  if (complete.length === 0) {
    return null;
  }
  const filter: ColumnFilter = { id: column.name, fields: getFilterFields(column), type, conditions: complete };
  if (complete.length > 1) {
    filter.union = union;
  }
  return filter;
}

/**
 * Days of a relative date, from the current day: a week starts on Monday, last month is the whole previous month
 * @param {string} relative Relative date
 * @param {Date} [today] Current day
 * @returns {{from: string, to: string}} First and last day as dd/MM/yyyy
 */
export function getRelativeDateRange(relative: RelativeDate, today: Date = new Date()): { from: string; to: string } {
  const year = today.getFullYear();
  const month = today.getMonth();
  const day = today.getDate();
  switch (relative) {
    case "yesterday": {
      const yesterday = new Date(year, month, day - 1);
      return { from: formatDate(yesterday), to: formatDate(yesterday) };
    }
    case "thisWeek": {
      const monday = new Date(year, month, day - (today.getDay() + 6) % 7);
      return { from: formatDate(monday), to: formatDate(new Date(monday.getFullYear(), monday.getMonth(), monday.getDate() + 6)) };
    }
    case "lastMonth":
      return { from: formatDate(new Date(year, month - 1, 1)), to: formatDate(new Date(year, month, 0)) };
    case "thisYear":
      return { from: formatDate(new Date(year, 0, 1)), to: formatDate(new Date(year, 11, 31)) };
    default:
      return { from: formatDate(today), to: formatDate(today) };
  }
}

/**
 * Filter entry of a relative date: a between on its days (S15)
 * @param {object} column Column
 * @param {string} relative Relative date
 * @param {Date} [today] Current day
 * @returns {object|null} Filter entry
 */
export function buildRelativeDateFilter(column: FilterableColumn, relative: RelativeDate, today: Date = new Date()): ColumnFilter | null {
  const { from, to } = getRelativeDateRange(relative, today);
  return buildConditionsFilter(column, [{ operator: "between", value: from, to }]);
}

/**
 * Operators of the custom filter of a type
 * @param {string} type Filter type
 * @returns {string[]} Operators
 */
export function getOperators(type: ColumnFilterType): string[] {
  return type === "number" || type === "date" ? [...COMPARISON_OPERATORS] : [...TEXT_OPERATORS];
}

/**
 * Check that an operator needs a value
 * @param {string} operator Operator
 * @returns {boolean} It needs a value
 */
export function operatorNeedsValue(operator?: string | null): boolean {
  return !!operator && !NO_VALUE_OPERATORS.has(operator);
}

/**
 * Locale key of an operator for a type
 * @param {string} operator Operator
 * @param {string} type Filter type
 * @returns {string} Locale key
 */
export function getOperatorLabel(operator: string, type: ColumnFilterType): string {
  return (type === "date" && DATE_OPERATOR_LABELS[operator]) || OPERATOR_LABELS[operator] || operator;
}

/**
 * Checked state of a group of checkboxes
 * @param {number} checked Checked boxes
 * @param {number} total Boxes
 * @returns {string} checked, unchecked or mixed
 */
export function getCheckState(checked: number, total: number): CheckState {
  if (total > 0 && checked >= total) {
    return "checked";
  }
  return checked > 0 ? "mixed" : "unchecked";
}

/**
 * Keys checked when the menu of a column opens: the values of its filter (and the blanks), or every value when the
 * column filters no values
 * @param {object} filter Filter of the column
 * @param {object[]} values Values of the list
 * @returns {Set<string>} Checked keys
 */
export function getInitialChecked(filter: ColumnFilter | null | undefined, values: readonly ColumnValue[]): Set<string> {
  if (!filter?.values) {
    return new Set(values.map(item => getValueKey(item.value)));
  }
  const keys = new Set(filter.values.map(value => getValueKey(value)));
  if (filter.empty) {
    keys.add(BLANKS_KEY);
  }
  return keys;
}

/**
 * Date tree of the values of a date column (S14): years, their months and their days, sorted as the values. A value
 * that is not a dd/MM/yyyy date (the blanks) stays out of the tree
 * @param {object[]} values Values of the list (dd/MM/yyyy dates)
 * @param {string} [language] Language of the month names
 * @returns {object[]} Years
 */
export function buildDateTree(values: readonly ColumnValue[], language?: string): DateTreeNode[] {
  const monthName = new Intl.DateTimeFormat(language || undefined, { month: "long" });
  const years: DateTreeNode[] = [];
  values.forEach(item => {
    const date = item.value === null || item.value === undefined ? null : toDate(String(item.value));
    if (!date) {
      return;
    }
    const count = Number(item.count) || 0;
    const yearKey = `year:${date.getFullYear()}`;
    let year = years.find(node => node.key === yearKey);
    if (!year) {
      year = { key: yearKey, label: String(date.getFullYear()), count: 0, level: 0, children: [] };
      years.push(year);
    }
    const monthKey = `month:${date.getFullYear()}-${date.getMonth() + 1}`;
    let month = year.children.find(node => node.key === monthKey);
    if (!month) {
      month = { key: monthKey, label: monthName.format(date), count: 0, level: 1, children: [] };
      year.children.push(month);
    }
    month.children.push({ key: getValueKey(item.value), label: String(date.getDate()).padStart(2, "0"), count, level: 2,
      value: item.value, children: [] });
    month.count += count;
    year.count += count;
  });
  return years;
}

/**
 * Keys of the days under a node of the date tree (the node itself when it is a day)
 * @param {object} node Node
 * @returns {string[]} Keys of its days
 */
export function getLeafKeys(node: DateTreeNode): string[] {
  return node.children.length === 0 ? [node.key] : node.children.flatMap(getLeafKeys);
}

/**
 * Text of a value of a filter in a chip: a dd/MM/yyyy date stays as it is
 * @param {*} value Value
 * @returns {string} Text
 */
const valueText = (value: unknown): string => String(value ?? "");

/**
 * Summary of the filter of a column for its chip (S16): the labels of its values ("Operator, Viewer"; the first ones and
 * "+N" when there are many), or its conditions ("contains martin", a range as "01/10/2026 – 31/10/2026")
 * @param {object} filter Filter of the column
 * @param {function} t Translation function
 * @param {object} [labels] Label of each value key, as the values list showed them when the filter was applied
 * @returns {string} Summary
 */
export function summarizeColumnFilter(filter: ColumnFilter, t: Translate, labels: Record<string, string> = {}): string {
  if (filter.values) {
    const texts = filter.values.map(value => labels[getValueKey(value)] ?? valueText(value));
    if (filter.empty) {
      texts.push(t("GRID_FILTER_BLANKS"));
    }
    const shown = texts.slice(0, CHIP_LABELS).join(", ");
    return texts.length > CHIP_LABELS ? `${shown} +${texts.length - CHIP_LABELS}` : shown;
  }
  const conditions = (filter.conditions || []).map(condition => {
    if (condition.operator === "between") {
      return `${valueText(condition.value)} – ${valueText(condition.to)}`;
    }
    const operator = t(getOperatorLabel(condition.operator, filter.type)).toLowerCase();
    return operatorNeedsValue(condition.operator) ? `${operator} ${valueText(condition.value)}` : operator;
  });
  const union = t(filter.union === "or" ? "GRID_FILTER_OR" : "GRID_FILTER_AND").toLowerCase();
  return conditions.join(` ${union} `);
}
