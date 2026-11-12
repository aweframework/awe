/**
 * Playground of the AWE chart. Every attribute is one the XSD declares for `chart`, `chart-serie`, `x-axis`, `y-axis`,
 * `chart-legend` and `chart-tooltip` and the React client reads. The server turns those elements into the `echartsModel` of the
 * component and sends it with the rows of a query; the stage gets the same model from data/chartModel.js and the rows from a
 * fixture (data/revenue.js). The chart is drawn by Apache ECharts, with the colours of the AWE theme.
 * A playground spec is described in playgroundModel.js.
 */
const {buildEchartsModel} = require('./data/chartModel');
const data = require('./data/revenue');

const {monthly, shares, SERIES, SHARE_SERIES} = data;

const CARTESIAN = ['line', 'spline', 'area', 'areaspline', 'column', 'bar'];
const PIES = ['pie', 'donut', 'semicircle'];
const POINTS = ['scatter', 'bubble'];
const RANGES = ['arearange', 'areasplinerange'];
const STACKABLE = ['area', 'areaspline', 'column', 'bar'];
const MONTHLY = [...CARTESIAN, ...RANGES];
const WITH_LEGEND = [...CARTESIAN, ...POINTS, ...RANGES, 'radar'];
const TITLE = 'Revenue by product line';
const SUBTITLE = 'Thousands of euros, 2025';

// What each kind of chart reads: its rows, the series and the field that names a point, and the labels of its axes
const MONTH_AXES = {xLabel: 'Month', yLabel: 'Revenue (kEUR)'};
const KINDS = {
  point: {types: POINTS, category: 'size', rows: data.DEALS, xLabel: 'Deal size (kEUR)', yLabel: 'Margin (%)'},
  range: {types: RANGES, category: 'month', rows: data.FORECAST, series: data.RANGE_SERIES, ...MONTH_AXES},
  radar: {types: ['radar'], category: 'measure', rows: data.PROFILES, series: data.RADAR_SERIES},
  heatmap: {types: ['heatmap'], category: 'slot', rows: data.ACTIVITY, series: data.HEATMAP_SERIES},
  gauge: {types: ['gauge'], category: undefined, rows: data.TARGET, series: data.GAUGE_SERIES},
  funnel: {types: ['funnel'], category: 'step', rows: data.PIPELINE, series: data.FUNNEL_SERIES},
  share: {types: PIES, category: 'line', rows: shares, series: SHARE_SERIES},
  month: {types: CARTESIAN, category: 'month', rows: monthly, series: SERIES, ...MONTH_AXES},
};

/**
 * The rows, the series and the axis labels a chart type reads
 * @param {string} type AWE chart type
 * @returns {{category: string, rows: Object[], series: Object[], xLabel: string, yLabel: string}}
 */
const kindOf = type => {
  const kind = Object.values(KINDS).find(candidate => candidate.types.includes(type)) ?? KINDS.month;
  return {series: type === 'bubble' ? data.BUBBLE_SERIES : data.DEAL_SERIES, ...kind};
};

const chart = {
  name: 'chart',
  element: 'Chart',
  tag: 'chart',
  id: 'demo',
  reference: {element: 'chart', page: 'screen'},
  api: '/docs/api/chart',
  stage: 'wide',
  layout: 'stacked',
  height: '28rem',
  controls: [
    {
      id: 'type', type: 'select', label: 'Chart type', xml: 'type', client: '', default: '', initial: 'column',
      options: [
        {value: 'line', label: 'Line'},
        {value: 'spline', label: 'Smooth line'},
        {value: 'area', label: 'Area'},
        {value: 'areaspline', label: 'Smooth area'},
        {value: 'column', label: 'Columns'},
        {value: 'bar', label: 'Horizontal bars'},
        {value: 'pie', label: 'Pie'},
        {value: 'donut', label: 'Donut'},
        {value: 'semicircle', label: 'Semicircle'},
        {value: 'scatter', label: 'Scatter'},
        {value: 'bubble', label: 'Bubbles'},
        {value: 'arearange', label: 'Range'},
        {value: 'areasplinerange', label: 'Smooth range'},
        {value: 'radar', label: 'Radar'},
        {value: 'heatmap', label: 'Heatmap'},
        {value: 'gauge', label: 'Gauge'},
        {value: 'funnel', label: 'Funnel'},
      ],
    },
    {
      id: 'stacking', type: 'segmented', label: 'Stacking', xml: 'stacking', client: '', default: '', initial: 'normal',
      when: {id: 'type', in: STACKABLE},
      options: [
        {value: '', label: 'None'},
        {value: 'normal', label: 'Stacked'},
        {value: 'percent', label: 'Percent'},
      ],
    },
    {id: 'title', type: 'text', label: 'Title', xml: 'label', client: '', default: '', initial: TITLE},
    {id: 'subtitle', type: 'text', label: 'Subtitle', xml: 'subtitle', client: '', default: '', initial: SUBTITLE},
    {id: 'dataLabels', type: 'switch', label: 'Data labels', xml: 'enable-data-labels', client: '', default: false},
    {
      id: 'legend', type: 'switch', label: 'Legend', client: '', writes: 'chart-legend', default: false, initial: true,
      when: {id: 'type', in: WITH_LEGEND},
    },
    {id: 'sharedTooltip', type: 'switch', label: 'Shared tooltip', client: '', writes: 'chart-tooltip', default: false, when: {id: 'type', in: MONTHLY}},
  ],
  xmlAttributes: () => [['initial-load', 'query'], ['target-action', 'Revenue']],
  children: [
    {when: {id: 'type', in: MONTHLY}, tag: 'x-axis', attrs: {label: 'Month', type: 'category'}},
    {when: {id: 'type', in: MONTHLY}, tag: 'y-axis', attrs: {label: 'Revenue (kEUR)'}},
    {when: {id: 'type', in: POINTS}, tag: 'x-axis', attrs: {label: 'Deal size (kEUR)'}},
    {when: {id: 'type', in: POINTS}, tag: 'y-axis', attrs: {label: 'Margin (%)'}},
    ...SERIES.map(serie => ({
      when: {id: 'type', in: CARTESIAN},
      tag: 'chart-serie',
      attrs: {id: serie.id, label: serie.label, 'x-value': 'month', 'y-value': serie.field},
    })),
    {when: {id: 'type', in: PIES}, tag: 'chart-serie', attrs: {id: 'share', label: 'Revenue', 'x-value': 'line', 'y-value': 'revenue'}},
    {when: {id: 'type', in: ['scatter']}, tag: 'chart-serie', attrs: {id: 'deals', label: 'Deals', 'x-value': 'size', 'y-value': 'margin'}},
    {
      when: {id: 'type', in: ['bubble']},
      tag: 'chart-serie',
      attrs: {id: 'deals', label: 'Deals', 'x-value': 'size', 'y-value': 'margin', 'z-value': 'seats'},
    },
    {
      when: {id: 'type', in: RANGES},
      tag: 'chart-serie',
      attrs: {id: 'forecast', label: 'Software forecast', 'x-value': 'month', 'y-value': 'low', 'z-value': 'high'},
    },
    {when: {id: 'type', in: ['radar']}, tag: 'chart-serie', attrs: {id: 'software', label: 'Software', 'x-value': 'measure', 'y-value': 'software'}},
    {when: {id: 'type', in: ['radar']}, tag: 'chart-serie', attrs: {id: 'services', label: 'Services', 'x-value': 'measure', 'y-value': 'services'}},
    {when: {id: 'type', in: ['radar']}, tag: 'chart-serie', attrs: {id: 'support', label: 'Support', 'x-value': 'measure', 'y-value': 'support'}},
    {
      when: {id: 'type', in: ['heatmap']},
      tag: 'chart-serie',
      attrs: {id: 'orders', label: 'Orders', 'x-value': 'slot', 'y-value': 'day', 'z-value': 'orders'},
    },
    {
      when: {id: 'type', in: ['gauge']},
      tag: 'chart-serie',
      attrs: {id: 'target', label: 'Target reached (%)', 'y-value': 'reached', 'z-value': 'target'},
    },
    {when: {id: 'type', in: ['funnel']}, tag: 'chart-serie', attrs: {id: 'pipeline', label: 'Deals', 'x-value': 'step', 'y-value': 'deals'}},
    {when: 'legend', tag: 'chart-legend', attrs: {enabled: 'true'}},
    {when: 'sharedTooltip', tag: 'chart-tooltip', attrs: {shared: 'true', suffix: ' kEUR'}},
  ],
  scene: values => {
    const kind = kindOf(values.type);
    return {
      attributes: {
        echartsModel: buildEchartsModel({
          type: values.type,
          category: kind.category,
          series: kind.series,
          stacking: STACKABLE.includes(values.type) ? values.stacking || undefined : undefined,
          legend: WITH_LEGEND.includes(values.type) ? values.legend : undefined,
          dataLabels: values.dataLabels,
          sharedTooltip: MONTHLY.includes(values.type) && values.sharedTooltip,
          title: values.title,
          subtitle: values.subtitle,
          xLabel: kind.xLabel,
          yLabel: kind.yLabel,
        }),
      },
      model: {values: kind.rows},
    };
  },
  events: [
    {name: 'click', description: 'A point is clicked: its x, y and series are written to the chart and its dependencies run.'},
    {name: 'double-click', description: 'A point is double clicked.'},
    {name: 'context-menu', description: 'A point is right clicked: the context menu of the chart opens.'},
    {name: 'zoom', description: 'The chart is zoomed (zoom-type): the new limits are written to the chart attributes.'},
  ],
  actions: [
    {name: 'filter', description: 'Reloads the chart from its query, with the criteria of the screen.'},
    {name: 'add-points', description: 'Adds points to the series of the chart without reloading it.'},
    {name: 'add-chart-series', description: 'Adds a series; remove-chart-series and replace-chart-series change the ones it has.'},
  ],
};

/**
 * Props of an AweLive example with a chart over the monthly revenue.
 * @param {Object} description What buildEchartsModel takes, over the monthly revenue of the three product lines
 */
const chartExample = (description = {}) => {
  const kind = kindOf(description.type);
  return {
    components: {
      chart: {
        attributes: {
          echartsModel: buildEchartsModel({category: kind.category, series: kind.series, ...description}),
        },
        model: {values: kind.rows},
      },
    },
    nodes: [{elementType: 'Chart', id: 'chart'}],
  };
};

module.exports = {chart, chartExample};
