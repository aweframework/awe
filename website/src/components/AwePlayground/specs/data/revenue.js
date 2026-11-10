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

module.exports = {monthly, shares, SERIES, SHARE_SERIES};
