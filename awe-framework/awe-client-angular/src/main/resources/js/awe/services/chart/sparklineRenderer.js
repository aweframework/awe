/**
 * Sparkline renderer of the AngularJS client: the shared geometry (sparkline.js, identical to the React client copy)
 * plus the markup writer, because AngularJS writes the SVG as text while React builds its elements.
 */
import {buildSparkline, parseSparklineSeries, SPARKLINE_HEIGHT, SPARKLINE_TYPES, SPARKLINE_WIDTH} from "./sparkline";

export {buildSparkline, parseSparklineSeries, SPARKLINE_HEIGHT, SPARKLINE_TYPES, SPARKLINE_WIDTH};

/**
 * Escapes a value to write it inside an attribute or a text node of the markup
 * @param {*} value
 * @return {string}
 */
const escapeMarkup = value => String(value)
  .replace(/&/g, "&amp;")
  .replace(/</g, "&lt;")
  .replace(/>/g, "&gt;")
  .replace(/"/g, "&quot;");

/**
 * Writes the model of a sparkline as the markup of an SVG
 * @param {Object|null} model Model built by buildSparkline
 * @return {string} The SVG markup, or an empty string when there is nothing to draw
 */
export function sparklineMarkup(model) {
  if (!model) {
    return "";
  }
  const shapes = model.shapes.map(shape => {
    const attrs = Object.keys(shape.attrs).map(name => ` ${name}="${escapeMarkup(shape.attrs[name])}"`).join("");
    return shape.title === undefined
      ? `<${shape.tag}${attrs}/>`
      : `<${shape.tag}${attrs}><title>${escapeMarkup(shape.title)}</title></${shape.tag}>`;
  }).join("");
  return `<svg class="awe-sparkline" data-type="${model.type}" viewBox="${model.viewBox}" width="${model.width}" height="${model.height}" ` +
    `role="img" aria-label="${escapeMarkup(model.label)}" focusable="false">${shapes}</svg>`;
}
