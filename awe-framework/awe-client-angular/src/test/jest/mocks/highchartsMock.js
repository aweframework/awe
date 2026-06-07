const chartInstance = {
  destroy: jest.fn(),
  reflow: jest.fn()
};

const Highcharts = {
  theme: {},
  chart: jest.fn(() => chartInstance),
  stockChart: jest.fn(() => chartInstance),
  getOptions: jest.fn(() => ({})),
  setOptions: jest.fn(options => options)
};

module.exports = Highcharts;
