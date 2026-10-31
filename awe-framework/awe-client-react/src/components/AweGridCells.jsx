import React from "react";
import {extractCellModel} from "../utilities/grid";
import {getFirstDefinedAndNotNullValue} from "../utilities/general";
import {getInitials} from "../utilities/initials";
import {getSwatchColor} from "../utilities/color";
import {getIconNode} from "../utilities";

// Tones of a status cell and of an avatar (any other tone is shown as neutral)
export const STATUS_TONES = ["success", "warning", "danger", "info", "neutral"];
export const AVATAR_TONES = ["primary", ...STATUS_TONES];
const DEFAULT_CHIP_SEPARATOR = ",";

/**
 * Text of a field of the row (its label, or its value), empty when the row has not the field
 * @param {Object} row Row data
 * @param {string} field Field name
 * @return {string} Text of the field
 */
export function rowText(row, field) {
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
  return rowTone(row, statusField, STATUS_TONES);
}

/**
 * Tone of a field of the row: one of the given tones, neutral otherwise
 * @param {Object} row Row data
 * @param {string} field Field of the row with the tone
 * @param {string[]} tones Known tones
 * @return {string} Tone
 */
export function rowTone(row, field, tones) {
  const {value} = extractCellModel(row?.[field]);
  const tone = String(value ?? "").trim().toLowerCase();
  return tones.includes(tone) ? tone : "neutral";
}

/**
 * Colour of a swatch field of the row (#793): a known palette or a hex, empty for any other value (see getSwatchColor)
 * @param {Object} row Row data
 * @param {string} field Field of the row with the colour
 * @return {string} CSS colour
 */
export function rowSwatch(row, field) {
  return field ? getSwatchColor(extractCellModel(row?.[field]).value) : "";
}

/**
 * Avatar of a cell (#793): the initials of the avatar field of the row on its tone, or a circle of the colour of its
 * swatch field, which takes the place of the avatar while the row has a valid colour. It is decorative (the main value is
 * the accessible text); without both an empty space of the same size keeps the names of the column aligned
 * @param {Object} column Column definition (avatarField, avatarToneField, swatchField)
 * @param {Object} [row] Row data
 * @return {React.ReactElement} Avatar
 */
function avatarCell(column, row) {
  const swatch = rowSwatch(row, column.swatchField);
  if (swatch !== "") {
    return <span className="awe-cell-swatch" aria-hidden="true" style={{"--awe-swatch-color": swatch}}/>;
  }
  const initials = column.avatarField ? getInitials(rowText(row, column.avatarField)) : "";
  if (initials === "") {
    return <span className="awe-cell-avatar-spacer" aria-hidden="true"/>;
  }
  const tone = column.avatarToneField ? rowTone(row, column.avatarToneField, AVATAR_TONES) : "neutral";
  return <span className={`awe-cell-avatar awe-cell-avatar-${tone}`} data-tone={tone} aria-hidden="true">{initials}</span>;
}

/**
 * Icon of a cell (#793): the icon of the icon field of the row, decorative (the text of the cell is its accessible value);
 * null when the row has no icon or an unknown one
 * @param {Object} row Row data
 * @param {string} iconField Field of the row with the icon
 * @return {React.ReactElement|null} Icon
 */
export function cellIcon(row, iconField) {
  const {value} = extractCellModel(row?.[iconField]);
  return getIconNode(value == null ? "" : String(value), {tag: "span", className: "awe-cell-icon", "aria-hidden": "true"});
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
 * Whether a column shows its cells with a secondary line, a status, chips, an avatar, a swatch or an icon (only the
 * columns without a component)
 * @param {Object} column Column definition
 * @return {boolean} The column has rich cells
 */
export function hasRichCell(column = {}) {
  return !column.component && (!!column.secondaryField || !!column.statusField || column.chips === true || !!column.avatarField
    || !!column.swatchField || !!column.iconField);
}

/**
 * Cell with a secondary line, a status, chips (#832), an avatar, a swatch or an icon (#793). The values are always text, never HTML
 * @param {string} value Main text of the cell
 * @param {Object} column Column definition
 * @param {Object} [row] Row data, where the secondary, the status and the avatar fields are read
 * @param {function} renderText Renders the main text when it is a plain text
 * @return {React.ReactElement} Cell
 */
export function richCell(value, column, row, renderText) {
  if (!column.avatarField && !column.swatchField) {
    return textContents(value, column, row, renderText);
  }
  return <div className="awe-cell-entity">
    {avatarCell(column, row)}
    <div className="awe-cell-entity-text">
      {textContents(value, column, row, () => <span className="awe-cell-main" title={value}>{value}</span>)}
    </div>
  </div>;
}

/**
 * Text of a cell: its main value, as a status or chips, with its icon and its secondary line. With an icon field the
 * status is a plain tag: the icon of the row (if any) takes the place of the dot
 * @param {string} value Main text of the cell
 * @param {Object} column Column definition
 * @param {Object} [row] Row data, where the secondary, the status and the icon fields are read
 * @param {function} renderText Renders the main text when it is a plain text
 * @return {React.ReactElement} Text of the cell
 */
function textContents(value, column, row, renderText) {
  const {secondaryField, statusField, chips, iconField} = column;
  let main;
  if (chips === true) {
    main = chipsCell(value, column);
  } else if (statusField && value !== "") {
    const tone = getStatusTone(row, statusField);
    main = iconField
      ? <span className={`awe-cell-status awe-cell-status-${tone} awe-cell-status-plain`} data-tone={tone} title={value}>
        {cellIcon(row, iconField)}{value}</span>
      : <span className={`awe-cell-status awe-cell-status-${tone}`} data-tone={tone} title={value}>{value}</span>;
  } else if (iconField) {
    main = <span className="awe-cell-icon-text" title={value}>{cellIcon(row, iconField)}{value}</span>;
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
