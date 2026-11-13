/**
 * Sample revenue of the chart examples, in thousands of euros: invented figures of a software company by month and by
 * product line. The rows have the shape the server sends: a field by name and one row by point.
 */
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const SOFTWARE = [182, 191, 205, 214, 229, 241, 238, 252, 268, 281, 297, 318];
const SERVICES = [96, 88, 104, 112, 109, 121, 98, 93, 126, 134, 141, 156];
const SUPPORT = [54, 55, 57, 58, 61, 62, 64, 66, 67, 71, 73, 76];

/** Monthly revenue by product line: month, software, services, support. */
const monthly = MONTHS.map((month, index) => ({
  id: index + 1, month, software: SOFTWARE[index], services: SERVICES[index], support: SUPPORT[index],
}));

/** The three product lines of the monthly revenue, as the series of a chart. */
const SERIES = [
  {id: 'software', label: 'Software', field: 'software'},
  {id: 'services', label: 'Services', field: 'services'},
  {id: 'support', label: 'Support', field: 'support'},
];

const total = field => monthly.reduce((sum, row) => sum + row[field], 0);

/** Revenue of the year by product line: one row by line, to draw a share (pie, donut). */
const shares = SERIES.map(({field, label}, index) => ({id: index + 1, line: label, revenue: total(field)}));

/** The one series of a share chart: the revenue of each line. */
const SHARE_SERIES = [{id: 'share', label: 'Revenue', field: 'revenue'}];

/** Closed deals: size (kEUR), margin (%) and seats of the licence, to draw a scatter or a bubble chart. */
const DEALS = [
  [22, 31, 5], [35, 28, 8], [48, 36, 12], [60, 33, 10], [75, 41, 20], [90, 38, 18], [110, 44, 25], [125, 40, 30],
  [140, 47, 28], [160, 43, 40], [185, 51, 45], [210, 46, 38], [240, 54, 60], [275, 49, 55], [310, 57, 80], [360, 52, 70],
].map(([size, margin, seats], index) => ({id: index + 1, size, margin, seats}));

/** The deals as the series of a scatter (size and margin) and of a bubble chart (the seats give the size of the bubble). */
const DEAL_SERIES = [{id: 'deals', label: 'Deals', field: 'margin'}];
const BUBBLE_SERIES = [{id: 'deals', label: 'Deals', field: 'margin', z: 'seats'}];

/** Forecast of the monthly revenue: the low and the high of the range, in thousands of euros. */
const FORECAST = MONTHS.map((month, index) => ({
  id: index + 1, month, low: Math.round(SOFTWARE[index] * 0.88), high: Math.round(SOFTWARE[index] * 1.12),
}));
const RANGE_SERIES = [{id: 'forecast', label: 'Software forecast', field: 'low', z: 'high'}];

/** Scores (0-100) of each product line on five measures, to draw a radar: one row by measure, one field by line. */
const PROFILES = [
  {id: 1, measure: 'Growth', software: 82, services: 61, support: 44},
  {id: 2, measure: 'Margin', software: 74, services: 52, support: 68},
  {id: 3, measure: 'Retention', software: 88, services: 57, support: 91},
  {id: 4, measure: 'Satisfaction', software: 79, services: 72, support: 85},
  {id: 5, measure: 'Adoption', software: 91, services: 48, support: 63},
];

/** Orders of the shop by weekday and time slot, to draw a heatmap: one row by cell. */
const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];
const SLOTS = ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00'];
const LOAD = [[12, 31, 54, 38, 42, 20], [15, 36, 61, 41, 47, 24], [14, 33, 58, 45, 52, 27], [18, 40, 66, 49, 55, 31], [22, 44, 72, 52, 63, 48]];
const ACTIVITY = DAYS.flatMap((day, dayIndex) => SLOTS.map((slot, slotIndex) => ({
  id: dayIndex * SLOTS.length + slotIndex + 1, day, slot, orders: LOAD[dayIndex][slotIndex],
})));
const HEATMAP_SERIES = [{id: 'orders', label: 'Orders', field: 'day', z: 'orders'}];

/** The sales target of the year: the value reached and the target, to draw a gauge. */
const TARGET = [{id: 1, reached: 78, target: 100}];
const GAUGE_SERIES = [{id: 'target', label: 'Target reached (%)', field: 'reached', z: 'target'}];

/** Sales pipeline: the deals that reach each step, to draw a funnel. */
const PIPELINE = [
  {id: 1, step: 'Leads', deals: 1200}, {id: 2, step: 'Qualified', deals: 840}, {id: 3, step: 'Proposal', deals: 460},
  {id: 4, step: 'Negotiation', deals: 250}, {id: 5, step: 'Won', deals: 130},
];
const FUNNEL_SERIES = [{id: 'pipeline', label: 'Deals', field: 'deals'}];

/** Measures of the radar as series: the product lines, read from the profile rows. */
const RADAR_SERIES = SERIES;

module.exports = {
  monthly, shares, SERIES, SHARE_SERIES, DEALS, DEAL_SERIES, BUBBLE_SERIES, FORECAST, RANGE_SERIES, PROFILES, RADAR_SERIES,
  ACTIVITY, HEATMAP_SERIES, TARGET, GAUGE_SERIES, PIPELINE, FUNNEL_SERIES,
};
