import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweMap from "../../../src/components/AweMap";
import {updateModel} from "../../../src/redux/actions/components";

// Leaflet needs a real layout (jsdom has none): the component is tested against a fake that records what it is given
const mockLeaflet = {maps: [], tiles: [], groups: [], markers: [], polylines: []};
jest.mock("leaflet", () => {
  return {
    __esModule: true,
    map: jest.fn((element, options) => {
      const map = {element, options, setView: jest.fn(), fitBounds: jest.fn(), invalidateSize: jest.fn(), remove: jest.fn()};
      mockLeaflet.maps.push(map);
      return map;
    }),
    tileLayer: jest.fn((url, options) => {
      const tile = {url, options, addTo: jest.fn(() => tile)};
      mockLeaflet.tiles.push(tile);
      return tile;
    }),
    layerGroup: jest.fn(() => {
      const group = {layers: [], addTo: jest.fn(() => group), clearLayers: jest.fn(() => {
        group.layers = [];
      })};
      group.addLayer = jest.fn((layer) => {
        group.layers.push(layer);
        return group;
      });
      mockLeaflet.groups.push(group);
      return group;
    }),
    circleMarker: jest.fn((latlng, options) => {
      const marker = {latlng, options, element: globalThis.document.createElement("path"), handlers: {}};
      marker.on = jest.fn((event, handler) => {
        marker.handlers[event] = handler;
        return marker;
      });
      marker.bindPopup = jest.fn(() => marker);
      marker.setStyle = jest.fn((style) => {
        marker.options = {...marker.options, ...style};
        return marker;
      });
      marker.getElement = jest.fn(() => marker.element);
      mockLeaflet.markers.push(marker);
      return marker;
    }),
    polyline: jest.fn((latlngs, options) => {
      const line = {latlngs, options, element: globalThis.document.createElement("path")};
      line.getElement = jest.fn(() => line.element);
      mockLeaflet.polylines.push(line);
      return line;
    })
  };
});

const rows = [
  {id: "1", latitude: 40.4, longitude: -3.7, name: "Madrid"},
  {id: "2", latitude: 41.38, longitude: 2.17, name: "Barcelona"},
  {id: "3", latitude: null, longitude: 2.17, name: "Nowhere"},
  {id: "4", latitude: "abc", longitude: 2.17, name: "Broken"}
];

const stateOf = (attributes = {}, values = rows, model = {}) => ({
  settings: {...DEFAULT_SETTINGS, mapTileUrl: "https://tiles.example.com/{z}/{x}/{y}.png"},
  components: {
    fleet: {
      address: {component: "fleet", view: "report"},
      model: {values, ...model},
      attributes: {id: "fleet", ...attributes},
      specificAttributes: {sort: []}
    }
  }
});

const render = (attributes, values, model) => renderWithProviders(<AweMap id="fleet"/>,
  {preloadedState: stateOf(attributes, values, model)});

describe('awe-react-client/test/js/components/AweMapTest.jsx', () => {
  const observers = [];

  beforeEach(() => {
    Object.values(mockLeaflet).forEach(list => list.splice(0));
    observers.splice(0);
    global.ResizeObserver = class {
      constructor(callback) {
        this.callback = callback;
        this.observe = jest.fn();
        this.disconnect = jest.fn();
        observers.push(this);
      }
    };
  });

  afterEach(() => {
    delete global.ResizeObserver;
  });

  it('limits the tiles to the zoom the OpenStreetMap servers provide', () => {
    render();

    expect(mockLeaflet.tiles[0].options.maxZoom).toBe(19);
  });

  it('recalculates the map size when its container is resized', () => {
    render();

    expect(observers).toHaveLength(1);
    expect(observers[0].observe).toHaveBeenCalledWith(document.querySelector(".awe-map-canvas"));
    act(() => observers[0].callback());
    expect(mockLeaflet.maps[0].invalidateSize).toHaveBeenCalled();
  });

  it('stops observing the container when it unmounts', () => {
    const {unmount} = render();

    unmount();

    expect(observers[0].disconnect).toHaveBeenCalled();
  });

  it('works without ResizeObserver', () => {
    delete global.ResizeObserver;

    render();

    expect(mockLeaflet.maps).toHaveLength(1);
  });

  it('renders the map container with its test hook', () => {
    render();

    const container = document.querySelector('[data-testid="map"]');
    expect(container).not.toBeNull();
    expect(container.getAttribute("map-id")).toBe("fleet");
    expect(mockLeaflet.maps).toHaveLength(1);
    expect(mockLeaflet.maps[0].element).toBe(container.querySelector(".awe-map-canvas"));
    expect(mockLeaflet.maps[0].options).toEqual(expect.objectContaining({minZoom: 3, maxZoom: 19}));
  });

  it('draws the tiles of the url in the settings with the OpenStreetMap attribution', () => {
    render();

    const [tile] = mockLeaflet.tiles;
    expect(tile.url).toBe("https://tiles.example.com/{z}/{x}/{y}.png");
    expect(tile.options.attribution)
      .toBe('&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors');
    expect(tile.addTo).toHaveBeenCalledWith(mockLeaflet.maps[0]);
  });

  it('draws one marker per row with valid coordinates', () => {
    render();

    expect(mockLeaflet.markers.map(marker => marker.latlng)).toEqual([[40.4, -3.7], [41.38, 2.17]]);
    const hooks = mockLeaflet.markers.map(marker => [
      marker.element.getAttribute("data-testid"), marker.element.getAttribute("row-id")]);
    expect(hooks).toEqual([["map-marker", "1"], ["map-marker", "2"]]);
  });

  it('reads the coordinates from the columns the attributes name', () => {
    render({latitude: "lat", longitude: "lon"}, [{id: "9", lat: "10.5", lon: 20, latitude: 1, longitude: 1}]);

    expect(mockLeaflet.markers.map(marker => marker.latlng)).toEqual([[10.5, 20]]);
  });

  it('binds a popup with the marker label column', () => {
    render({markerLabel: "name"});

    expect(mockLeaflet.markers[0].bindPopup).toHaveBeenCalledWith("Madrid");
    expect(mockLeaflet.markers[1].bindPopup).toHaveBeenCalledWith("Barcelona");
  });

  it('does not bind a popup without a marker label column', () => {
    render();

    expect(mockLeaflet.markers[0].bindPopup).not.toHaveBeenCalled();
  });

  it('centers on the center and zoom attributes', () => {
    render({centerLatitude: 10, centerLongitude: 20, zoom: 6});

    expect(mockLeaflet.maps[0].setView).toHaveBeenCalledWith([10, 20], 6);
    expect(mockLeaflet.maps[0].fitBounds).not.toHaveBeenCalled();
  });

  it('uses zoom 4 when only the center is set', () => {
    render({centerLatitude: 10, centerLongitude: 20});

    expect(mockLeaflet.maps[0].setView).toHaveBeenCalledWith([10, 20], 4);
  });

  it('fits the bounds to the markers when there is no center', () => {
    render();

    expect(mockLeaflet.maps[0].fitBounds).toHaveBeenCalledWith([[40.4, -3.7], [41.38, 2.17]]);
  });

  it('shows the default view when there are no markers and no center', () => {
    render({}, []);

    expect(mockLeaflet.maps[0].setView).toHaveBeenCalledWith([43.8667, 1.1111], 4);
  });

  it('selects the row of a clicked marker', () => {
    const {store} = render();

    act(() => mockLeaflet.markers[1].handlers.click());

    expect(store.getState().components.fleet.model.selected).toEqual(["2"]);
    expect(mockLeaflet.markers[1].element.getAttribute("data-selected")).toBe("true");
    expect(mockLeaflet.markers[0].element.getAttribute("data-selected")).toBe("false");
  });

  it('does not draw the markers again when the selection changes', () => {
    render();

    act(() => mockLeaflet.markers[0].handlers.click());

    expect(mockLeaflet.markers).toHaveLength(2);
  });

  it('draws the markers again when the values change', () => {
    const {store} = render();

    act(() => {
      store.dispatch(updateModel({component: "fleet", view: "report"}, {values: [rows[0]]}));
    });

    expect(mockLeaflet.markers).toHaveLength(3);
    expect(mockLeaflet.groups[0].clearLayers).toHaveBeenCalled();
  });

  it('marks the map as rendered once the markers are drawn', () => {
    render();

    expect(document.querySelector('[data-testid="map"]').getAttribute("data-rendered")).toBe("true");
  });

  describe('route', () => {
    const route = [[10, 20], [11, 21], [12, 22]];

    it('draws the route in a polyline and recenters on its first point with zoom 8', () => {
      render({}, rows, {route});

      expect(mockLeaflet.polylines).toHaveLength(1);
      expect(mockLeaflet.polylines[0].latlngs).toEqual(route);
      expect(mockLeaflet.polylines[0].options).toEqual(expect.objectContaining({color: "#6060FB", weight: 4}));
      expect(mockLeaflet.maps[0].setView).toHaveBeenLastCalledWith([10, 20], 8);
    });

    it('exposes the test hook of the route', () => {
      render({}, rows, {route});

      const element = mockLeaflet.polylines[0].element;
      expect(element.getAttribute("data-testid")).toBe("map-route");
      expect(element.getAttribute("map-id")).toBe("fleet");
    });

    it('hides the markers while the route is shown', () => {
      render({}, rows, {route});

      expect(mockLeaflet.markers).toHaveLength(0);
    });

    it('draws the markers when the route is empty', () => {
      render({}, rows, {route: []});

      expect(mockLeaflet.polylines).toHaveLength(0);
      expect(mockLeaflet.markers).toHaveLength(2);
    });

    it('draws the route when it arrives after the markers', () => {
      const {store} = render();

      act(() => {
        store.dispatch(updateModel({component: "fleet", view: "report"}, {route}));
      });

      expect(mockLeaflet.polylines).toHaveLength(1);
      expect(mockLeaflet.maps[0].setView).toHaveBeenLastCalledWith([10, 20], 8);
      expect(mockLeaflet.groups[0].clearLayers).toHaveBeenCalled();
    });
  });

  it('renders nothing when it is not visible', () => {
    render({visible: false});

    expect(document.querySelector('[data-testid="map"]')).toBeNull();
    expect(mockLeaflet.maps).toHaveLength(0);
  });

  it('removes the map when it unmounts', () => {
    const {unmount} = render();

    unmount();

    expect(mockLeaflet.maps[0].remove).toHaveBeenCalled();
  });
});
