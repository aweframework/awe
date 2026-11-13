// The Jest tests load the CommonJS bundle of ECharts (echarts/dist/echarts.js, see jest.config.js), which already holds
// every chart, component and renderer: the extensions that the client registers with echarts.use() are empty here.
// It saves the transform of the ES modules of ECharts and zrender (thousands of files) in every test run.
const extension = {install: () => undefined};

// Keep it in step with the imports of awe-client-react/src/utilities/echartsSetup.ts: a missing one is undefined there
// and echarts.use() fails while loading the client
module.exports = {
  BarChart: extension,
  FunnelChart: extension,
  GaugeChart: extension,
  HeatmapChart: extension,
  LineChart: extension,
  PieChart: extension,
  RadarChart: extension,
  ScatterChart: extension,
  AriaComponent: extension,
  DataZoomComponent: extension,
  GraphicComponent: extension,
  GridComponent: extension,
  LegendComponent: extension,
  RadarComponent: extension,
  TitleComponent: extension,
  TooltipComponent: extension,
  VisualMapComponent: extension,
  SVGRenderer: extension
};
