import React from "react";
import {extractCellModel} from "../utilities/grid";
import {getFirstDefinedAndNotNullValue} from "../utilities/general";

const STATUS_TONES = ["success", "warning", "danger", "info", "neutral"];
const DEFAULT_CHIP_SEPARATOR = ",";

/**
 * Text of a field of the row (its label, or its value), empty when the row has not the field
 * @param {Object} row Row data
 * @param {string} field Field name
 * @return {string} Text of the field
 */
function rowText(row, field) {
  const {label, value} = extractCellModel(row?.[field]);
  return String(getFirstDefinedAndNotNullValue(label, value, ""));
}

/**
 * Tone of a status cell: one of the known tones, neutral otherwise
 * @param {Object} row Row data
 * @param {string} statusField Field of the row with the tone
 * @return {string} Tone
 */
export function getStatusTone(row, statusField) {
  const {value} = extractCellModel(row?.[statusField]);
  const tone = String(value ?? "").trim().toLowerCase();
  return STATUS_TONES.includes(tone) ? tone : "neutral";
}

/**
 * Items of a chips cell: the value split by the separator, trimmed and without the empty ones
 * @param {string} value Cell text
 * @param {string} [separator] Separator (a comma by default)
 * @return {string[]} Items
 */
export function getChipItems(value, separator) {
  return String(value ?? "")
    .split(separator || DEFAULT_CHIP_SEPARATOR)
    .map(item => item.trim())
    .filter(item => item !== "");
}

/**
 * Chips of a cell. Over the maximum, the first chips and a "+n" chip that lists the hidden ones
 * @param {string} value Cell text
 * @param {Object} column Column definition (chipSeparator, maxChips)
 * @return {React.ReactElement} Chips
 */
function chipsCell(value, column) {
  const items = getChipItems(value, column.chipSeparator);
  const max = Number.isInteger(column.maxChips) && column.maxChips > 0 ? column.maxChips : items.length;
  const shown = items.slice(0, max);
  const hidden = items.slice(max);
  const hiddenText = hidden.join(", ");
  return <span className="awe-cell-chips">
    {shown.map((item, index) => <span key={index} className="awe-cell-chip">{item}</span>)}
    {hidden.length > 0 &&
      <span className="awe-cell-chip awe-cell-chip-more" title={hiddenText}
        aria-label={`${hidden.length} more: ${hiddenText}`}>{`+${hidden.length}`}</span>}
  </span>;
}

/**
 * Whether a column shows its cells with a secondary line, a status or chips (only the columns without a component)
 * @param {Object} column Column definition
 * @return {boolean} The column has rich cells
 */
export function hasRichCell(column = {}) {
  return !column.component && (!!column.secondaryField || !!column.statusField || column.chips === true);
}

/**
 * Cell with a secondary line, a status or chips (#832). The values are always text, never HTML
 * @param {string} value Main text of the cell
 * @param {Object} column Column definition
 * @param {Object} [row] Row data, where the secondary and the status fields are read
 * @param {function} renderText Renders the main text when it is a plain text
 * @return {React.ReactElement} Cell
 */
export function richCell(value, column, row, renderText) {
  const {secondaryField, statusField, chips} = column;
  let main;
  if (chips === true) {
    main = chipsCell(value, column);
  } else if (statusField && value !== "") {
    const tone = getStatusTone(row, statusField);
    main = <span className={`awe-cell-status awe-cell-status-${tone}`} data-tone={tone} title={value}>{value}</span>;
  }
  const secondary = secondaryField ? rowText(row, secondaryField) : "";
  if (secondary === "") {
    return main ?? renderText(value);
  }
  return <div className="awe-cell-lines">
    <span className="awe-cell-main">{main ?? value}</span>
    <span className="awe-cell-secondary">{secondary}</span>
  </div>;
}
