import type {EChartsCoreOption} from "echarts/core";
import {getEChartsLocale} from "./echartsLocale";

/** The ECharts library, as the static setup and the loader of the browser give it */
export type Echarts = typeof import("echarts/core");

// The images are also drawn in the PDF of the reports by Batik, which only reads SVG 1.1:
// - the style element only holds the hover rules of the chart, which a static image does not need, and Batik fails on a
//   style element without a base URI;
// - the shadows (feDropShadow, SVG 2) are drawn with the SVG 1.1 filter chain that does the same.
const SVG_STYLE = /<style[^>]*>[\s\S]*?<\/style>/g;
const DROP_SHADOW = /<feDropShadow\b([^>]*?)\/?>(?:\s*<\/feDropShadow>)?/g;
const ATTRIBUTE = /([\w-]+)="([^"]*)"/g;

/**
 * SVG 1.1 filter chain of a drop shadow
 * @param {string} attributes Attributes of the feDropShadow element
 * @returns {string} Filter primitives
 */
function dropShadow(attributes: string): string {
  const {dx = "2", dy = "2", stdDeviation = "2", "flood-color": color = "black", "flood-opacity": opacity = "1"} =
    Object.fromEntries([...attributes.matchAll(ATTRIBUTE)].map(([, name, value]) => [name, value]));
  return `<feGaussianBlur in="SourceAlpha" stdDeviation="${stdDeviation}" result="blur"/>` +
    `<feOffset in="blur" dx="${dx}" dy="${dy}" result="offset"/>` +
    `<feFlood flood-color="${color}" flood-opacity="${opacity}"/>` +
    `<feComposite in2="offset" operator="in" result="shadow"/>` +
    `<feMerge><feMergeNode in="shadow"/><feMergeNode in="SourceGraphic"/></feMerge>`;
}

/**
 * Render an option to an SVG image, without a DOM. The image is SVG 1.1 (see SVG_STYLE and DROP_SHADOW)
 * @param {object} option ECharts option
 * @param {{width: number, height: number}} size Size of the image
 * @param {string} [language] Language of the texts of the chart
 * @returns {string} SVG image
 */
export function renderSvgWith(echarts: Echarts, option: EChartsCoreOption, {width, height}: {width: number; height: number}, language?: string): string {
  const chart = echarts.init(null, null, {
    renderer: "svg", ssr: true, width, height, locale: getEChartsLocale(language)
  });
  try {
    chart.setOption({...option, animation: false});
    return chart.renderToSVGString().replace(SVG_STYLE, "").replace(DROP_SHADOW, (match, attributes) => dropShadow(attributes));
  } finally {
    chart.dispose();
  }
}
