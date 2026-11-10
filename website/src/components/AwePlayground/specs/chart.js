/**
 * Playground of the AWE chart. Every attribute is one the XSD declares for `chart`, `chart-serie`, `x-axis`, `y-axis`,
 * `chart-legend` and `chart-tooltip` and the React client reads. The server turns those elements into the `echartsModel` of the
 * component and sends it with the rows of a query; the stage gets the same model from data/chartModel.js and the rows from a
 * fixture (data/revenue.js). The chart is drawn by Apache ECharts, with the colours of the AWE theme.
 * A playground spec is described in playgroundModel.js.
 */
const {buildEchartsModel} = require('./data/chartModel');
const {monthly, shares, SERIES, SHARE_SERIES} = require('./data/revenue');

const CARTESIAN = ['line', 'spline', 'area', 'areaspline', 'column', 'bar'];
const PIES = ['pie', 'donut', 'semicircle'];
const STACKABLE = ['area', 'areaspline', 'column', 'bar'];
const TITLE = 'Revenue by product line';
const SUBTITLE = 'Thousands of euros, 2025';

const isPie = values => PIES.includes(values.type);
const seriesOf = values => (isPie(values) ? SHARE_SERIES : SERIES);

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
      when: {id: 'type', in: CARTESIAN},
    },
    {id: 'sharedTooltip', type: 'switch', label: 'Shared tooltip', client: '', writes: 'chart-tooltip', default: false, when: {id: 'type', in: CARTESIAN}},
  ],
  xmlAttributes: () => [['initial-load', 'query'], ['target-action', 'Revenue']],
  children: [
    {when: {id: 'type', in: CARTESIAN}, tag: 'x-axis', attrs: {label: 'Month', type: 'category'}},
    {when: {id: 'type', in: CARTESIAN}, tag: 'y-axis', attrs: {label: 'Revenue (kEUR)'}},
    ...SERIES.map(serie => ({
      when: {id: 'type', in: CARTESIAN},
      tag: 'chart-serie',
      attrs: {id: serie.id, label: serie.label, 'x-value': 'month', 'y-value': serie.field},
    })),
    {when: {id: 'type', in: PIES}, tag: 'chart-serie', attrs: {id: 'share', label: 'Revenue', 'x-value': 'line', 'y-value': 'revenue'}},
    {when: 'legend', tag: 'chart-legend', attrs: {enabled: 'true'}},
    {when: 'sharedTooltip', tag: 'chart-tooltip', attrs: {shared: 'true', suffix: ' kEUR'}},
  ],
  scene: values => ({
    attributes: {
      echartsModel: buildEchartsModel({
        type: values.type,
        category: isPie(values) ? 'line' : 'month',
        series: seriesOf(values),
        stacking: STACKABLE.includes(values.type) ? values.stacking || undefined : undefined,
        legend: isPie(values) ? undefined : values.legend,
        dataLabels: values.dataLabels,
        sharedTooltip: !isPie(values) && values.sharedTooltip,
        title: values.title,
        subtitle: values.subtitle,
        xLabel: isPie(values) ? undefined : 'Month',
        yLabel: isPie(values) ? undefined : 'Revenue (kEUR)',
      }),
    },
    model: {values: isPie(values) ? shares : monthly},
  }),
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
  const pie = PIES.includes(description.type);
  return {
    components: {
      chart: {
        attributes: {
          echartsModel: buildEchartsModel({
            category: pie ? 'line' : 'month', series: pie ? SHARE_SERIES : SERIES, ...description,
          }),
        },
        model: {values: pie ? shares : monthly},
      },
    },
    nodes: [{elementType: 'Chart', id: 'chart'}],
  };
};

module.exports = {chart, chartExample};
