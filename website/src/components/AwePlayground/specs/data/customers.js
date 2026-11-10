/**
 * Sample customers of the grid examples: invented companies, no real data. The rows have the shape the server sends:
 * a field by column name, `id` as row identifier, and a cell that needs more than a value is {value, label, icon...}.
 * The fields avatar, tone and email feed the rich cells of the name column (avatar, status dot, secondary line).
 */
const PLANS = {
  Starter: {tone: 'neutral'},
  Business: {tone: 'info'},
  Enterprise: {tone: 'success'},
};

const ROWS = [
  ['Alder & Finch', 'Spain', 'Enterprise', 6400, [48, 52, 55, 61, 64, 68], '2022-03-14', 'Lucia Ferrer'],
  ['Brightwave Labs', 'Portugal', 'Business', 2100, [18, 17, 19, 21, 20, 21], '2023-01-09', 'Tiago Matos'],
  ['Cobalt Freight', 'Germany', 'Enterprise', 5200, [60, 58, 54, 55, 52, 52], '2021-11-02', 'Jonas Keller'],
  ['Dune Analytics', 'Spain', 'Starter', 380, [2, 3, 3, 4, 4, 4], '2024-05-21', 'Marta Ruiz'],
  ['Evergreen Foods', 'France', 'Business', 1750, [14, 15, 16, 16, 17, 18], '2023-06-30', 'Claire Dubois'],
  ['Fjord Robotics', 'Norway', 'Enterprise', 7300, [55, 60, 66, 70, 72, 73], '2020-09-18', 'Ingrid Berg'],
  ['Granite Works', 'Italy', 'Business', 1990, [22, 21, 20, 20, 19, 20], '2022-10-05', 'Paolo Rossi'],
  ['Harbor & Co', 'United Kingdom', 'Starter', 420, [5, 4, 4, 4, 5, 4], '2024-02-12', 'Oliver Grant'],
  ['Indigo Health', 'Spain', 'Business', 2450, [19, 21, 22, 23, 24, 25], '2023-04-27', 'Elena Soler'],
  ['Juniper Mobility', 'Netherlands', 'Enterprise', 5900, [50, 53, 57, 58, 59, 59], '2021-07-15', 'Daan Visser'],
  ['Kestrel Energy', 'Spain', 'Enterprise', 6800, [70, 69, 68, 68, 66, 68], '2020-12-01', 'Pablo Navarro'],
  ['Lumen Studio', 'Portugal', 'Starter', 290, [3, 3, 3, 2, 3, 3], '2024-08-19', 'Ines Costa'],
  ['Mosaic Retail', 'France', 'Business', 2300, [20, 22, 21, 23, 23, 23], '2022-05-23', 'Hugo Martin'],
  ['Nimbus Cloud', 'Germany', 'Enterprise', 8100, [62, 68, 73, 77, 80, 81], '2019-10-10', 'Lena Vogel'],
  ['Orchid Pharma', 'Italy', 'Business', 1640, [17, 16, 16, 15, 16, 16], '2023-09-04', 'Giulia Conti'],
  ['Pinnacle Legal', 'United Kingdom', 'Business', 2050, [18, 19, 20, 21, 20, 20], '2022-08-16', 'Amelia Hart'],
  ['Quartz Software', 'Spain', 'Starter', 460, [3, 4, 4, 5, 5, 5], '2024-03-08', 'Sergio Molina'],
  ['Riverbend Farms', 'France', 'Starter', 350, [4, 4, 3, 3, 4, 4], '2024-06-25', 'Camille Roux'],
  ['Solstice Travel', 'Portugal', 'Business', 1880, [21, 19, 18, 18, 19, 19], '2023-02-20', 'Rui Almeida'],
  ['Tundra Logistics', 'Norway', 'Enterprise', 6100, [58, 57, 59, 60, 61, 61], '2021-04-13', 'Sven Lund'],
  ['Umbra Security', 'Germany', 'Business', 2700, [23, 24, 25, 26, 27, 27], '2022-12-07', 'Katrin Maier'],
  ['Vertex Finance', 'Netherlands', 'Enterprise', 7600, [66, 68, 70, 72, 75, 76], '2020-06-29', 'Femke de Vries'],
  ['Willow Education', 'Spain', 'Starter', 310, [2, 2, 3, 3, 3, 3], '2024-09-11', 'Carmen Gil'],
  ['Zenith Aerospace', 'Italy', 'Enterprise', 9200, [80, 84, 87, 90, 91, 92], '2019-05-17', 'Marco Bianchi'],
];

const initialsOf = name => name.split(/[ &]+/).filter(Boolean).slice(0, 2).map(word => word[0]).join('').toUpperCase();
const slugOf = name => name.toLowerCase().replace(/[^a-z]+/g, '');

/** Rows of the grid, with the identifier `id` and the fields the rich cells read. */
const customers = ROWS.map(([name, country, plan, mrr, trend, since, owner], index) => ({
  id: index + 1,
  name,
  initials: initialsOf(name),
  email: `billing@${slugOf(name)}.example`,
  country,
  plan,
  planTone: PLANS[plan].tone,
  owner,
  mrr,
  trend,
  since,
}));

module.exports = {customers, PLANS};
