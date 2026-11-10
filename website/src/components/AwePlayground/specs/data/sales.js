/**
 * Sample sales of the pivot table examples: invented figures of a hardware reseller, one row by region, product and
 * quarter. The values come from fixed formulas (no randomness), so every build shows the same numbers.
 */
const REGIONS = ['North', 'South', 'East', 'West'];
const PRODUCTS = [
  {name: 'Laptop', price: 940},
  {name: 'Monitor', price: 310},
  {name: 'Dock', price: 120},
  {name: 'Headset', price: 85},
];
const CHANNELS = ['Online', 'Retail', 'Partners'];
const QUARTERS = ['Q1', 'Q2', 'Q3', 'Q4'];

/** Rows of the pivot table: Region, Product, Channel, Quarter, Units and Revenue. */
const sales = REGIONS.flatMap((region, r) => PRODUCTS.flatMap((product, p) => QUARTERS.map((quarter, q) => {
  const units = 6 + ((r * 7 + p * 5 + q * 3 + 4) % 11) * (4 - p) + q * (2 + r);
  return {
    id: r * 16 + p * 4 + q + 1,
    Region: region,
    Product: product.name,
    Channel: CHANNELS[(r + p + q) % CHANNELS.length],
    Quarter: quarter,
    Units: units,
    Revenue: units * product.price,
  };
})));

module.exports = {sales, REGIONS, PRODUCTS, CHANNELS, QUARTERS};
