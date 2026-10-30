import {aweApplication} from "../awe";
import * as L from "leaflet";
import {DEFAULT_LATITUDE, DEFAULT_LONGITUDE, toPosition} from "awe-client-react/utilities/map";
import {TestAttributes, TestIds} from "../data/testIds";
import {ClientActions} from "../data/actions";

const DEFAULT_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const MAP_OPTIONS = {minZoom: 3, maxZoom: 19};
const DEFAULT_CENTER = [43.8667, 1.1111];
const DEFAULT_ZOOM = 4;
const ROUTE_ZOOM = 8;
const ROUTE_STYLE = {weight: 4, className: "awe-map-route"};
// The colours of the markers come from the design tokens (map less): Leaflet only gets their classes and sizes
const MARKER_CLASS = "awe-map-marker";
const SELECTED_CLASS = "awe-map-marker-selected";
const MARKER_STYLE = {radius: 8, weight: 2};
const SELECTED_STYLE = {radius: 11, weight: 3};

/**
 * Check that a value is set
 * @param {*} value Value
 * @returns {boolean} The value is not null nor undefined
 */
const isSet = value => value !== null && value !== undefined;

/**
 * Set attributes on a DOM element
 * @param {Element} element Element (Leaflet gives none before the layer is drawn)
 * @param {object} attributes Attributes
 */
function setAttributes(element, attributes) {
  if (element) {
    Object.entries(attributes).forEach(([name, value]) => element.setAttribute(name, value));
  }
}

// Map service
aweApplication.factory('GeoMap',
  ['Component', 'Control', 'AweSettings', 'AweUtilities', 'ActionController',
    /**
     * Map component: shows the rows of the component as markers over an OpenStreetMap map drawn with Leaflet, with the
     * same behaviour as the React map. The markers are circle markers, which Leaflet draws as SVG paths: they need no
     * marker image
     * @param {Function} Component Component class
     * @param {Object} Control Control service
     * @param {Object} $settings Settings service
     * @param {Object} Utilities Utilities service
     * @param {Object} $actionController Action controller
     * @returns {Function} Map class
     */
    function (Component, Control, $settings, Utilities, $actionController) {

      // Map actions: their methods receive the parameters of the action and the scope of the map
      const MapActions = {
        /**
         * Show a route: the rows of the action (with the latitude and longitude columns of the map) become the route
         * @param {Object} parameters Action parameters, with the route rows in "rows"
         * @param {Object} scope Map scope
         */
        onPolyline: function ({rows}, scope) {
          scope.component.drawRoute(Array.isArray(rows) ? rows : []);
        }
      };

      /**
       * Map constructor
       * @param {Object} scope Map scope
       * @param {String} id Map id
       * @param {Object} element Map element
       * @returns {Object} Map component
       */
      function GeoMap(scope, id, element) {
        this.scope = scope;
        this.id = id;
        this.element = element;
        this.component = new Component(this.scope, this.id);
        this.component.geoMap = this;
        this.component.asGeoMap = function () {
          return this.geoMap.init();
        };
        return this.component;
      }

      GeoMap.prototype = {
        init: function () {
          // Init as component
          const component = this.component;
          if (!component.asComponent()) {
            // If component initialization is wrong, cancel initialization
            return false;
          }
          const controller = component.controller;
          const container = this.element[0].querySelector(".awe-map-canvas");
          let markerById = {};

          /**
           * Retrieve the ids of the selected rows
           * @returns {string[]} Selected ids
           */
          const getSelectedIds = () => Utilities.asArray(component.model.selected).filter(isSet).map(String);

          /**
           * Retrieve the selected row
           * @returns {object|undefined} Selected row
           */
          const getSelectedRow = () => {
            const [selected] = getSelectedIds();
            return (component.model.values || []).find(row => String(row.id) === selected);
          };

          /**
           * Retrieve the position of a row
           * @param {object} row Row
           * @returns {number[]|null} [latitude, longitude], null when a coordinate is empty or not numeric
           */
          const getPosition = row => toPosition(row, controller.latitude || DEFAULT_LATITUDE,
            controller.longitude || DEFAULT_LONGITUDE);

          /**
           * Retrieve the points of the rows that have valid coordinates
           * @returns {object[]} Points: {id, position: [latitude, longitude], label}
           */
          const getPoints = () => {
            return (component.model.values || []).flatMap(row => {
              const position = getPosition(row);
              if (!position) {
                return [];
              }
              const text = controller.markerLabel ? row[controller.markerLabel] : undefined;
              return [{id: String(row.id), position, label: isSet(text) ? String(text) : undefined}];
            });
          };

          // Create the map and its tiles
          const map = L.map(container, MAP_OPTIONS);
          L.tileLayer($settings.get("mapTileUrl") || DEFAULT_TILE_URL, {attribution: ATTRIBUTION, ...MAP_OPTIONS}).addTo(map);
          const markers = L.layerGroup().addTo(map);
          component.map = map;

          /**
           * Style the markers and expose their test hooks
           */
          component.styleMarkers = function () {
            const selectedIds = getSelectedIds();
            Object.entries(markerById).forEach(([pointId, marker]) => {
              const isSelected = selectedIds.includes(pointId);
              marker.setStyle(isSelected ? SELECTED_STYLE : MARKER_STYLE);
              const markerElement = marker.getElement();
              if (markerElement) {
                markerElement.classList.toggle(SELECTED_CLASS, isSelected);
                setAttributes(markerElement, {[TestAttributes.testId]: TestIds.mapMarker, "row-id": pointId, "map-id": component.id,
                  [TestAttributes.selected]: String(isSelected)});
              }
            });
          };

          /**
           * Select the row of a marker: the selection runs the dependencies and is sent with the form
           * @param {string} pointId Row id
           */
          component.selectRow = function (pointId) {
            // Leaflet events come from outside AngularJS
            Utilities.timeout(() => {
              const changes = {selected: [pointId]};
              Control.changeModelAttribute(component.address, changes, false);
              // A dependency element with a column (selectedRowValue) listens to the cell of the selected row, as with
              // a grid row: the change is published for the map and for every cell of the row
              const launchers = {[Utilities.getAddressId(component.address)]: changes};
              const row = getSelectedRow() || {};
              Object.keys(row).forEach(column => {
                launchers[Utilities.getAddressId({...component.address, column, row: pointId})] = changes;
              });
              Control.publish("modelChanged", launchers);
              component.styleMarkers();
            });
          };

          /**
           * Draw the markers and place the view
           */
          component.drawMarkers = function () {
            markers.clearLayers();
            markerById = {};
            const route = component.model.route || [];
            if (route.length > 0) {
              // A route replaces the markers and the view goes to its first point
              const line = L.polyline(route, ROUTE_STYLE);
              markers.addLayer(line);
              setAttributes(line.getElement(), {[TestAttributes.testId]: TestIds.mapRoute, "map-id": component.id});
              map.setView(route[0], ROUTE_ZOOM);
              component.rendered = true;
              return;
            }
            const points = getPoints();
            points.forEach(point => {
              const marker = L.circleMarker(point.position, {...MARKER_STYLE, className: MARKER_CLASS});
              if (point.label !== undefined) {
                marker.bindPopup(point.label);
              }
              marker.on("click", () => component.selectRow(point.id));
              markers.addLayer(marker);
              markerById[point.id] = marker;
            });

            if (isSet(controller.centerLatitude) && isSet(controller.centerLongitude)) {
              map.setView([Number(controller.centerLatitude), Number(controller.centerLongitude)], controller.zoom ?? DEFAULT_ZOOM);
            } else if (points.length > 0) {
              map.fitBounds(points.map(point => point.position));
            } else {
              map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
            }
            component.styleMarkers();
            component.rendered = true;
          };

          /**
           * Show a route instead of the markers (an empty route shows the markers again). The route is not a change of
           * the data of the map, so no dependency is checked
           * @param {object[]} rows Rows of the route
           */
          component.drawRoute = function (rows) {
            component.model.route = rows.map(getPosition).filter(Boolean);
            component.drawMarkers();
          };

          /**
           * Update the map on model changed
           * @param {Object} changes Model changes
           */
          component.onModelChanged = function (changes) {
            if ("values" in changes) {
              component.drawMarkers();
            } else {
              component.styleMarkers();
            }
          };

          /**
           * Recompute the size of the map: a map drawn in a hidden or resized container is grey until it knows its size
           */
          component.invalidateSize = function () {
            map.invalidateSize();
          };

          /**********************************************************************/
          /* ATTRIBUTES AND API                                                 */
          /**********************************************************************/

          _.merge(component.attributeMethods, {
            selectedRow: () => getSelectedIds()[0] ?? null,
            selectedRowValue: (comp, column) => getSelectedRow()?.[column] ?? null,
            value: () => getSelectedIds()[0] ?? null
          });
          component.getAttribute = function (attribute, column, row) {
            return component.attributeMethods[attribute](component, column, row);
          };

          /**
           * Retrieve the data of the component that should be sent to the server: the selected row, by its value
           * @return {Object} Data to send to the server
           */
          Control.getAddressApi(component.address).getData = function () {
            const selectedIds = getSelectedIds();
            const values = (component.model.values || []).filter(row => selectedIds.includes(String(row.id)))
              .map(row => row.value ?? row.id);
            return {[component.address.component]: Control.formatDataList(values)};
          };

          /**********************************************************************/
          /* EVENTS                                                             */
          /**********************************************************************/

          component.listeners = component.listeners || {};
          $actionController.defineActionListeners(component.listeners, ClientActions.map, component.scope, MapActions);
          Utilities.defineModelChangeListeners(component.listeners, {scope: component.scope, check: ["values", "selected"],
            service: component, method: "onModelChanged"});
          component.listeners["resize"] = component.scope.$on("resize", component.invalidateSize);
          component.listeners["resize-action"] = component.scope.$on("resize-action", component.invalidateSize);
          component.listeners["visible"] = component.scope.$watch("controller.visible", (visible, previous) => {
            if (visible && visible !== previous) {
              Utilities.timeout(component.invalidateSize);
            }
          });
          let observer = null;
          if (typeof window.ResizeObserver !== "undefined") {
            observer = new window.ResizeObserver(component.invalidateSize);
            observer.observe(container);
          }
          component.scope.$on("$destroy", () => {
            observer?.disconnect();
            map.remove();
          });

          // Draw the markers of the initial model
          component.drawMarkers();
          return true;
        }
      };
      return GeoMap;
    }
  ]);
