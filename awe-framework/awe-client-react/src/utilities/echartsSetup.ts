import * as echarts from "echarts/core";
import type {EChartsCoreOption} from "echarts/core";
import {getEChartsLocale} from "./echartsLocale";
import {renderSvgWith} from "./echartsSvg";
import {BarChart, FunnelChart, GaugeChart, HeatmapChart, LineChart, PieChart, RadarChart, ScatterChart} from "echarts/charts";
import {
  AriaComponent,
  DataZoomComponent,
  GraphicComponent,
  GridComponent,
  LegendComponent,
  RadarComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent
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
  LineChart, BarChart, PieChart, ScatterChart, RadarChart, HeatmapChart, GaugeChart, FunnelChart,
  GridComponent, RadarComponent, VisualMapComponent, TitleComponent, LegendComponent, TooltipComponent, DataZoomComponent, GraphicComponent,
  AriaComponent,
  SVGRenderer
]);

const LOCALES: Record<string, object> = {EN: langEN, ES: langES, FR: langFR};
// The locale modules are namespaces without prototype, and ECharts needs plain objects
Object.entries(LOCALES).forEach(([name, locale]) => echarts.registerLocale(name, {...locale} as Parameters<typeof echarts.registerLocale>[1]));

export {echarts, getEChartsLocale};

/**
 * Render an option to an SVG image, without a DOM (see renderSvgWith)
 * @param {object} option ECharts option
 * @param {{width: number, height: number}} size Size of the image
 * @param {string} [language] Language of the texts of the chart
 * @returns {string} SVG image
 */
export function renderSvg(option: EChartsCoreOption, size: {width: number; height: number}, language?: string): string {
  return renderSvgWith(echarts, option, size, language);
}
