import * as echarts from "echarts/core";
import {BarChart, LineChart, PieChart, ScatterChart} from "echarts/charts";
import {
  AriaComponent,
  DataZoomComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from "echarts/components";
import {SVGRenderer} from "echarts/renderers";
import * as langEN from "echarts/i18n/langEN-obj";
import * as langES from "echarts/i18n/langES-obj";
import * as langFR from "echarts/i18n/langFR-obj";

/**
 * Apache ECharts, with only the charts and components that AWE needs (the library is tree shaken).
 * Charts are drawn as SVG, so that the printed image and the browser tests can read them from the DOM.
 */
echarts.use([
  LineChart, BarChart, PieChart, ScatterChart,
  GridComponent, TitleComponent, LegendComponent, TooltipComponent, DataZoomComponent, GraphicComponent,
  AriaComponent,
  SVGRenderer
]);

const LOCALES = {EN: langEN, ES: langES, FR: langFR};
// The locale modules are namespaces without prototype, and ECharts needs plain objects
Object.entries(LOCALES).forEach(([name, locale]) => echarts.registerLocale(name, {...locale}));

export {echarts};

/**
 * Name of the ECharts locale of a language
 * @param {string} language Language (es-ES, fr, en-GB...)
 * @returns {string} ES, FR or EN
 */
export function getEChartsLocale(language) {
  const name = String(language || "").substring(0, 2).toUpperCase();
  return LOCALES[name] ? name : "EN";
}

/**
 * Render an option to an SVG image, without a DOM
 * @param {object} option ECharts option
 * @param {{width: number, height: number}} size Size of the image
 * @param {string} [language] Language of the texts of the chart
 * @returns {string} SVG image
 */
export function renderSvg(option, {width, height}, language) {
  const chart = echarts.init(null, null, {
    renderer: "svg", ssr: true, width, height, locale: getEChartsLocale(language)
  });
  try {
    chart.setOption({...option, animation: false});
    return chart.renderToSVGString();
  } finally {
    chart.dispose();
  }
}

const parseColor = (text) => {
  const value = String(text || "").trim();
  const hex = /^#([\da-f]{3}|[\da-f]{6})$/i.exec(value);
  if (hex) {
    const digits = hex[1].length === 3 ? [...hex[1]].map(digit => digit + digit).join("") : hex[1];
    return [0, 2, 4].map(offset => Number.parseInt(digits.substring(offset, offset + 2), 16));
  }
  const rgb = /^rgba?\(\s*(\d+)[\s,]+(\d+)[\s,]+(\d+)(?:[\s,/]+([\d.]+))?/i.exec(value);
  // A transparent color says nothing about the surface
  return rgb && Number(rgb[4] ?? 1) > 0 ? [Number(rgb[1]), Number(rgb[2]), Number(rgb[3])] : null;
};

/**
 * Check if the application theme is dark. The AWE themes are CSS files, so the surface color of the theme
 * (`--surface-a`, or the background of the body when the theme does not define it) is read from the element
 * @param {Element} element Element inside the application
 * @returns {boolean} The surface is dark
 */
export function isDarkTheme(element) {
  if (!element || typeof getComputedStyle !== "function") {
    return false;
  }
  const style = getComputedStyle(element);
  const color = parseColor(style.getPropertyValue("--surface-a")) ||
    parseColor(getComputedStyle(document.body).backgroundColor);
  if (!color) {
    return false;
  }
  const [red, green, blue] = color;
  return (0.299 * red + 0.587 * green + 0.114 * blue) / 255 < 0.5;
}
