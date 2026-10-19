// The Jest tests load the CommonJS bundle of ECharts (echarts/dist/echarts.js, see jest.config.js), which already holds
// every chart, component and renderer: the extensions that the client registers with echarts.use() are empty here.
// It saves the transform of the ES modules of ECharts and zrender (thousands of files) in every test run.
const extension = {install: () => undefined};

module.exports = {
  BarChart: extension,
  LineChart: extension,
  PieChart: extension,
  ScatterChart: extension,
  AriaComponent: extension,
  DataZoomComponent: extension,
  GraphicComponent: extension,
  GridComponent: extension,
  LegendComponent: extension,
  TitleComponent: extension,
  TooltipComponent: extension,
  SVGRenderer: extension
};
