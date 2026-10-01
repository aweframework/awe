/**
 * Fixed vocabulary of "data-testid" values.
 *
 * A value names the PART of a component (never an instance). The instance is identified by the attributes AWE already
 * renders ("criterion-id", "grid-id", "row-id", "column-id") or, for plugin DOM that lives outside the component
 * (select2 dropdown, datepicker popup), by the owner attribute "data-testid-owner".
 */
export const TestIds = Object.freeze({
  // Real control of every criterion (input, textarea, hidden input, text value...)
  criterionInput: "criterion-input",
  // Selectors (select2): container, chosen value, search input, multiple choice, global dropdown and its options
  select: "select",
  selectValue: "select-value",
  selectSearch: "select-search",
  selectChoice: "select-choice",
  selectDropdown: "select-dropdown",
  selectOption: "select-option",
  // Date criteria (bootstrap-datepicker): popup and its day, month and year cells
  datepicker: "datepicker",
  datepickerDay: "datepicker-day",
  datepickerMonth: "datepicker-month",
  datepickerYear: "datepicker-year",
  // Uploader criterion: name of the uploaded file and clear action
  uploadFilename: "upload-filename",
  uploadClear: "upload-clear"
});

/**
 * Attribute names that carry the test hooks. State is exposed as data attributes ("true"/"false") so tests do not
 * depend on library state classes.
 */
export const TestAttributes = Object.freeze({
  testId: "data-testid",
  owner: "data-testid-owner",
  selected: "data-selected",
  active: "data-active",
  disabled: "data-disabled",
  outsideMonth: "data-outside-month"
});
