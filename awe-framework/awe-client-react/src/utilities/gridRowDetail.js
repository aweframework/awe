/**
 * Template of the row detail of a grid: the text of the `<row-detail>` element, where each `{name}` placeholder is
 * replaced with the field `name` of the row (any field of the datalist, also the ones of hidden columns).
 *
 * The result is plain text (it is never parsed as HTML). What is not a valid placeholder is kept as text.
 *
 * The template is read once, from left to right, and each closing brace is looked for after the opening one only, so
 * its time is linear. It does not use regular expressions on the template, only on single names.
 */

const FIELD_NAME = /^[\w.$-]+$/;

/**
 * Text of a field of a row: the label of a cell object, else its value. Missing values are an empty text
 * @param {object} row Row data
 * @param {string} name Field name
 * @returns {string} Field text
 */
function fieldText(row, name) {
  const field = row != null && Object.hasOwn(row, name) ? row[name] : undefined;
  let value = field;
  if (field !== null && typeof field === "object") {
    value = field.label ?? field.value;
  }
  return value === null || value === undefined ? "" : String(value);
}

/**
 * Render the row detail template of a grid with the data of a row
 * @param {string} template Template with `{name}` placeholders
 * @param {object} row Row data
 * @returns {string} Plain text of the detail
 */
export function renderRowDetail(template, row) {
  const text = template === null || template === undefined ? "" : String(template);
  let result = "";
  let position = 0;
  while (position < text.length) {
    const start = text.indexOf("{", position);
    const end = start < 0 ? -1 : text.indexOf("}", start);
    if (end < 0) {
      break;
    }
    // The last opening brace before the closing one is the start of the placeholder
    const innerStart = text.lastIndexOf("{", end);
    const name = text.substring(innerStart + 1, end);
    if (FIELD_NAME.test(name)) {
      result += text.substring(position, innerStart) + fieldText(row, name);
    } else {
      result += text.substring(position, end + 1);
    }
    position = end + 1;
  }
  return result + text.substring(position);
}
