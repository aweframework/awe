import React, {useCallback, useEffect, useRef, useState} from "react";
import {useDispatch, useSelector} from "react-redux";
import _ from "lodash";
import PropTypes from "prop-types";
import * as L from "leaflet";
import "leaflet/dist/leaflet.css";
import {useComponentState} from "../hooks/useComponentState";
import {classNames} from "../utilities/components";
import {TestIds, testHook} from "../utilities/testIds";
import {updateModelWithDependencies} from "../redux/thunks/components";
import {DEFAULT_LATITUDE, DEFAULT_LONGITUDE, toPosition} from "../utilities/map";
import "./AweMap.less";

const DEFAULT_TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const MAP_OPTIONS = {minZoom: 3, maxZoom: 19};
const DEFAULT_CENTER = [43.8667, 1.1111];
const DEFAULT_ZOOM = 4;
const ROUTE_ZOOM = 8;
const ROUTE_STYLE = {color: "#6060FB", weight: 4};
const MARKER_STYLE = {radius: 8, color: "#3388ff", fillColor: "#3388ff", fillOpacity: 0.6, weight: 2};
const SELECTED_STYLE = {radius: 11, color: "#d9534f", fillColor: "#d9534f", fillOpacity: 0.85, weight: 3};

/**
 * Keep the previous value while the new one is equal: the markers must not be drawn again (they would lose their
 * popup) when the state of the component is copied with equal values
 * @param {*} value Value
 * @returns {*} The previous value when it equals the new one
 */
function useStable(value) {
  const ref = useRef(value);
  if (!_.isEqual(ref.current, value)) {
    ref.current = value;
  }
  return ref.current;
}

/**
 * Retrieve the points of the rows that have valid coordinates
 * @param {object[]} rows Rows of the model
 * @param {string} latitude Name of the latitude column
 * @param {string} longitude Name of the longitude column
 * @param {string} [label] Name of the marker label column
 * @returns {object[]} Points: {id, position: [latitude, longitude], label}
 */
function getPoints(rows, latitude, longitude, label) {
  return rows.flatMap(row => {
    const position = toPosition(row, latitude, longitude);
    if (!position) {
      return [];
    }
    const text = label ? row[label] : undefined;
    return [{id: row.id, position, label: text === null || text === undefined ? undefined : String(text)}];
  });
}

/**
 * Retrieve the ids of the selected markers: the selection of the model and the rows marked as selected
 * @param {object} model Model of the map
 * @returns {string[]} Selected ids
 */
function getSelectedIds(model) {
  const selected = [model.selected, ...(model.values || []).filter(row => row.selected).map(row => row.id)];
  return selected.flat(Infinity).filter(id => id !== null && id !== undefined).map(String);
}

/**
 * AWE Map component: shows the rows of the component as markers over an OpenStreetMap map drawn with Leaflet.
 * Leaflet is used directly (no wrapper) and the markers are circle markers, which Leaflet draws as SVG paths: they
 * need no marker image, so webpack does not have to resolve the default icon paths of Leaflet.
 * @category Components
 * @subcategory Map
 */
function AweMap(props) {
  const {id} = props;
  const {model = {values: []}, attributes = {}, address} = useComponentState(id);
  const mapTileUrl = useSelector(state => state.settings?.mapTileUrl) || DEFAULT_TILE_URL;
  const dispatch = useDispatch();
  const {style, visible = true, markerLabel, centerLatitude, centerLongitude, zoom} = attributes;
  const latitude = attributes.latitude || DEFAULT_LATITUDE;
  const longitude = attributes.longitude || DEFAULT_LONGITUDE;
  const points = useStable(getPoints(model.values || [], latitude, longitude, markerLabel));
  const route = useStable(model.route || []);
  const selectedIds = useStable(getSelectedIds(model));
  const shown = visible !== false;

  const containerRef = useRef(null);
  const mapRef = useRef(null);
  const markersRef = useRef(null);
  const markerById = useRef({});
  const [mapReady, setMapReady] = useState(false);
  const [rendered, setRendered] = useState(false);

  // The latest inputs, for the callbacks of the markers
  const latest = useRef({});
  latest.current = {address, values: model.values || []};

  const select = useCallback((pointId) => {
    const {address: target, values} = latest.current;
    dispatch(updateModelWithDependencies(target, {
      selected: [pointId],
      // A component data is sent from the selected rows, by their value
      values: values.map(row => ({...row, value: row.value ?? row.id, selected: String(row.id) === String(pointId)}))
    }));
  }, [dispatch]);

  // Create the map (again when it is shown after being hidden) and its tiles
  useEffect(() => {
    if (!shown || !containerRef.current) {
      return undefined;
    }
    const map = L.map(containerRef.current, MAP_OPTIONS);
    L.tileLayer(mapTileUrl, {attribution: ATTRIBUTION, ...MAP_OPTIONS}).addTo(map);
    markersRef.current = L.layerGroup().addTo(map);
    mapRef.current = map;
    // A map created in a hidden or zero-size container (tabs, dialogs) is grey until it knows its real size
    let observer = null;
    if (typeof ResizeObserver !== "undefined") {
      observer = new ResizeObserver(() => map.invalidateSize());
      observer.observe(containerRef.current);
    }
    setMapReady(true);
    return () => {
      observer?.disconnect();
      map.remove();
      mapRef.current = null;
      markersRef.current = null;
      markerById.current = {};
      setMapReady(false);
      setRendered(false);
    };
  }, [shown, mapTileUrl]);

  // Draw the markers and place the view
  useEffect(() => {
    const map = mapRef.current;
    if (!mapReady || !map) {
      return;
    }
    markersRef.current.clearLayers();
    markerById.current = {};
    if (route.length > 0) {
      // A route replaces the markers (as in AngularJS) and the view goes to its first point
      const line = L.polyline(route, ROUTE_STYLE);
      markersRef.current.addLayer(line);
      const element = line.getElement?.();
      if (element) {
        Object.entries(testHook(TestIds.mapRoute, {attributes: {"map-id": id}}))
          .forEach(([name, value]) => element.setAttribute(name, value));
      }
      map.setView(route[0], ROUTE_ZOOM);
      setRendered(true);
      return;
    }
    points.forEach(point => {
      const marker = L.circleMarker(point.position, MARKER_STYLE);
      if (point.label !== undefined) {
        marker.bindPopup(point.label);
      }
      marker.on("click", () => select(point.id));
      markersRef.current.addLayer(marker);
      markerById.current[String(point.id)] = marker;
    });

    if (centerLatitude !== undefined && centerLatitude !== null && centerLongitude !== undefined && centerLongitude !== null) {
      map.setView([Number(centerLatitude), Number(centerLongitude)], zoom ?? DEFAULT_ZOOM);
    } else if (points.length > 0) {
      map.fitBounds(points.map(point => point.position));
    } else {
      map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
    }
    setRendered(true);
  }, [mapReady, points, route, centerLatitude, centerLongitude, zoom, select, id]);

  // Style the markers and expose their test hooks (also after they are drawn again)
  useEffect(() => {
    if (!rendered) {
      return;
    }
    Object.entries(markerById.current).forEach(([pointId, marker]) => {
      const isSelected = selectedIds.includes(pointId);
      marker.setStyle(isSelected ? SELECTED_STYLE : MARKER_STYLE);
      const element = marker.getElement?.();
      if (element) {
        const hook = testHook(TestIds.mapMarker, {selected: isSelected, attributes: {"row-id": pointId, "map-id": id}});
        Object.entries(hook).forEach(([name, value]) => element.setAttribute(name, value));
      }
    });
  }, [rendered, points, selectedIds, id]);

  if (!shown) {
    return <></>;
  }
  const classes = classNames("awe-map", "expand", style);
  return <div className={classes} id={id} {...testHook(TestIds.map, {rendered, attributes: {"map-id": id}})}>
    <div className="awe-map-canvas" ref={containerRef}/>
  </div>;
}

AweMap.propTypes = {
  id: PropTypes.string
};

export default AweMap;
