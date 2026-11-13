/**
 * Playground of the AWE map. Every attribute is one the XSD declares for `map` and the React client reads (Leaflet draws
 * it). The rows are a fixture (data/stores.js): the map shows each one as a marker, and a click on a marker selects its row
 * and opens its label. The tiles are the ones of the client by default, from OpenStreetMap: they need a network connection.
 * A playground spec is described in playgroundModel.js.
 */
const {stores} = require('./data/stores');

// Where the map looks at the start: the centre and zoom attributes, or none to fit every marker
const VIEWS = {
  fit: {},
  iberia: {'center-latitude': '40.2', 'center-longitude': '-4.5', zoom: '6'},
  city: {'center-latitude': '40.4168', 'center-longitude': '-3.7038', zoom: '11'},
};

const map = {
  name: 'map',
  element: 'GeoMap',
  tag: 'map',
  id: 'demo',
  reference: {element: 'map', page: 'screen'},
  api: '/docs/api/map',
  stage: 'wide',
  layout: 'stacked',
  height: '26rem',
  controls: [
    {
      id: 'view', type: 'segmented', label: 'Initial view', client: '', writes: 'center-latitude, center-longitude and zoom', default: 'fit', initial: 'fit',
      options: [
        {value: 'fit', label: 'Fit all markers'},
        {value: 'iberia', label: 'Iberian peninsula'},
        {value: 'city', label: 'One city'},
      ],
    },
    {
      id: 'markerLabel', type: 'segmented', label: 'Marker label', xml: 'marker-label', default: '', initial: 'name',
      options: [
        {value: '', label: 'None'},
        {value: 'name', label: 'Store'},
        {value: 'city', label: 'City'},
      ],
    },
  ],
  xmlAttributes: values => [
    ['latitude', 'latitude'],
    ['longitude', 'longitude'],
    ...Object.entries(VIEWS[values.view]),
    ['initial-load', 'query'],
    ['target-action', 'Stores'],
  ],
  scene: values => ({
    attributes: {
      latitude: 'latitude',
      longitude: 'longitude',
      ...(values.view !== 'fit' && {
        centerLatitude: Number(VIEWS[values.view]['center-latitude']),
        centerLongitude: Number(VIEWS[values.view]['center-longitude']),
        zoom: Number(VIEWS[values.view].zoom),
      }),
    },
    model: {values: stores},
  }),
  events: [
    {name: 'select-row', description: 'A marker is clicked: its row is selected and the dependencies of the map run.'},
  ],
  actions: [
    {name: 'filter', description: 'Reloads the markers from the query of the map, with the criteria of the screen.'},
    {name: 'polyline', description: 'Draws a route over the map instead of the markers.'},
  ],
};

/**
 * Props of an AweLive example with the stores map.
 * @param {Object} [attributes] Client attributes of the map (camelCase)
 * @param {number} [selected] Identifier of the store that starts selected
 */
const mapExample = (attributes = {}, selected = undefined) => ({
  components: {
    map: {
      attributes: {latitude: 'latitude', longitude: 'longitude', markerLabel: 'name', ...attributes},
      model: {values: stores.map(store => (store.id === selected ? {...store, selected: true} : store))},
    },
  },
  nodes: [{elementType: 'GeoMap', id: 'map'}],
});

module.exports = {map, mapExample};
