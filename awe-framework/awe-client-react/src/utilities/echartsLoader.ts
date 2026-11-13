import type {EChartsCoreOption} from "echarts/core";
import {renderSvgWith} from "./echartsSvg";
import type {Echarts} from "./echartsSvg";

export {getEChartsLocale} from "./echartsLocale";

/** ECharts, configured, with the function that draws a chart to an SVG image */
export interface LoadedEcharts {
  echarts: Echarts;
  renderSvg: (option: EChartsCoreOption, size: {width: number; height: number}, language?: string) => string;
}

let loading: Promise<LoadedEcharts> | null = null;

/**
 * Load Apache ECharts on demand, with only the charts and components that AWE needs, drawing SVG. The chart server does
 * not use this loader: it bundles ECharts at once (see echartsSetup). The list of charts is the one of echartsSetup.
 * @returns {Promise<LoadedEcharts>} ECharts, loaded once
 */
export function loadEcharts(): Promise<LoadedEcharts> {
  loading ??= (async () => {
    const [echarts, charts, components, renderers, langEN, langES, langFR] = await Promise.all([
      import("echarts/core"),
      import("echarts/charts"),
      import("echarts/components"),
      import("echarts/renderers"),
      import("echarts/i18n/langEN-obj"),
      import("echarts/i18n/langES-obj"),
      import("echarts/i18n/langFR-obj")
    ]);
    echarts.use([
      charts.LineChart, charts.BarChart, charts.PieChart, charts.ScatterChart, charts.RadarChart, charts.HeatmapChart,
      charts.GaugeChart, charts.FunnelChart,
      components.GridComponent, components.RadarComponent, components.VisualMapComponent, components.TitleComponent,
      components.LegendComponent, components.TooltipComponent, components.DataZoomComponent, components.GraphicComponent,
      components.AriaComponent,
      renderers.SVGRenderer
    ]);
    // The locale modules are namespaces without prototype, and ECharts needs plain objects
    Object.entries({EN: langEN, ES: langES, FR: langFR}).forEach(([name, locale]) =>
      echarts.registerLocale(name, {...locale} as Parameters<typeof echarts.registerLocale>[1]));
    const loaded: LoadedEcharts = {echarts, renderSvg: (option, size, language) => renderSvgWith(echarts, option, size, language)};
    return loaded;
  })().catch((error) => {
    // A failed load (network) can be tried again
    loading = null;
    throw error;
  });
  return loading;
}
