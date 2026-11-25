/**
 * Status strip: a pure function from the value of a cell to the ordered statuses of a strip, the tooltip of every
 * status and the accessible text that summarises them.
 *
 * Framework-agnostic. The AngularJS client keeps an identical copy (js/awe/services/grid/statusStrip.js, checked by
 * statusStripParity.jest.spec.js there), so both engines draw the same strip for the same cell.
 */

export const STATUS_STRIP_TONES = ["success", "warning", "danger", "info", "neutral"];
export const STATUS_STRIP_DEFAULT_TONE = "neutral";

/**
 * Reads the list of the cell: an already parsed array or the JSON text of an array
 * @param {*} value Cell value
 * @return {Array|null} List, or null when the value is not one
 */
function readList(value) {
  if (Array.isArray(value)) {
    return value;
  }
  if (typeof value !== "string") {
    return null;
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : null;
  } catch {
    // Not a JSON list
    return null;
  }
}

/**
 * Checks if an element of the list is a status: an object with a label that is a non-empty text
 * @param {*} item Element of the list
 * @return {boolean} It is a status
 */
function isStatus(item) {
  return item !== null && typeof item === "object" && !Array.isArray(item)
    && typeof item.label === "string" && item.label.trim() !== "";
}

/**
 * Parses the statuses of a cell, in their order. An unknown or missing tone is neutral; the optional title (the detail
 * that tells apart the statuses of the same label, as the date of a run) is kept only when it is a non-empty text.
 * @param {*} value JSON text or array of {tone, label, title?} items
 * @return {Array<{tone: string, label: string, title?: string}>} Statuses; empty when the value is not a list of statuses
 */
export function parseStatusStrip(value) {
  const list = readList(value);
  if (list === null || !list.every(isStatus)) {
    return [];
  }
  return list.map(item => {
    const status = {
      tone: STATUS_STRIP_TONES.includes(item.tone) ? item.tone : STATUS_STRIP_DEFAULT_TONE,
      label: item.label
    };
    if (typeof item.title === "string" && item.title.trim() !== "") {
      status.title = item.title;
    }
    return status;
  });
}

/**
 * Tooltip of a status: its label and, on a second line, its title ("OK\n#4807 · 17/11/2026 08:05"), or the label alone
 * @param {{label: string, title?: string}} item Status
 * @return {string} Tooltip
 */
export function statusStripTooltip(item) {
  return item.title ? `${item.label}\n${item.title}` : item.label;
}

/**
 * Accessible text of a strip: the count of every label, in the order each label first appears ("3 OK, 1 Error")
 * @param {Array<{label: string}>} items Statuses
 * @return {string} Summary
 */
export function statusStripSummary(items) {
  const counts = new Map();
  items.forEach(item => counts.set(item.label, (counts.get(item.label) || 0) + 1));
  return Array.from(counts, ([label, count]) => `${count} ${label}`).join(", ");
}

/**
 * Builds the strip of a cell
 * @param {*} value JSON text or array of {tone, label, title?} items
 * @return {{items: Array<{tone: string, label: string, title?: string}>, label: string}|null} Strip, or null when there is nothing to draw
 */
export function buildStatusStrip(value) {
  const items = parseStatusStrip(value);
  return items.length === 0 ? null : {items, label: statusStripSummary(items)};
}
