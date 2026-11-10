/**
 * Sample stores of the map examples: an invented chain with one store in each of these cities (the coordinates are the
 * ones of the city centres). The rows have the shape the server sends: the fields named by the `latitude`, `longitude` and
 * `marker-label` attributes of the map.
 */
const CITIES = [
  ['Madrid', 'Spain', 40.4168, -3.7038, 48],
  ['Barcelona', 'Spain', 41.3874, 2.1686, 41],
  ['Valencia', 'Spain', 39.4699, -0.3763, 27],
  ['Seville', 'Spain', 37.3891, -5.9845, 24],
  ['Bilbao', 'Spain', 43.263, -2.935, 19],
  ['Zaragoza', 'Spain', 41.6488, -0.8891, 16],
  ['Malaga', 'Spain', 36.7213, -4.4214, 18],
  ['Lisbon', 'Portugal', 38.7223, -9.1393, 31],
  ['Porto', 'Portugal', 41.1579, -8.6291, 17],
  ['Paris', 'France', 48.8566, 2.3522, 52],
  ['Lyon', 'France', 45.764, 4.8357, 22],
  ['Toulouse', 'France', 43.6047, 1.4442, 15],
];

/** Rows of the map: one store by city, with its coordinates, its country and its staff. */
const stores = CITIES.map(([city, country, latitude, longitude, staff], index) => ({
  id: index + 1,
  name: `${city} Central`,
  city,
  country,
  latitude,
  longitude,
  staff,
}));

module.exports = {stores};
