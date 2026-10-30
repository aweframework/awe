import fs from "fs";
import path from "path";
import "../../../main/resources/js/awe/app";
import {DefaultSettings} from "../../../main/resources/js/awe/data/options";
import {TestAttributes, TestIds} from "../../../main/resources/js/awe/data/testIds";
import {ClientActions} from "../../../main/resources/js/awe/data/actions";

// Leaflet needs a real layout (jsdom has none): the map is tested against a fake that records what it is given
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
      const marker = {latlng, options, element: globalThis.document.createElementNS("http://www.w3.org/2000/svg", "path"), handlers: {}};
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
      const line = {latlngs, options, element: globalThis.document.createElementNS("http://www.w3.org/2000/svg", "path")};
      line.getElement = jest.fn(() => line.element);
      mockLeaflet.polylines.push(line);
      return line;
    })
  };
});

const TEMPLATE = fs.readFileSync(path.join(__dirname, "..", "..", "..", "main", "resources", "templates", "angular",
  "geoMap.html"), "utf8");

const ROWS = [
  {id: "1", Lat: 40.4, Lon: -3.7, Lug: "Madrid"},
  {id: "2", Lat: 41.38, Lon: 2.17, Lug: "Barcelona"},
  {id: "3", Lat: null, Lon: 2.17, Lug: "Nowhere"},
  {id: "4", Lat: "abc", Lon: 2.17, Lug: "Broken"}
];
const copy = (value) => JSON.parse(JSON.stringify(value));

describe("awe-client-angular/src/test/jest/directives/geoMap.test.js", () => {
  let $compile;
  let $rootScope;
  let $timeout;
  let $templateCache;
  let utilities;
  let serverData;
  let control;
  let api;
  let actionController;
  let controller;
  let model;
  let currentSettings;

  beforeEach(() => {
    Object.keys(mockLeaflet).forEach(key => {
      mockLeaflet[key] = [];
    });
    controller = {latitude: "Lat", longitude: "Lon", markerLabel: "Lug", visible: true};
    model = {values: copy(ROWS), selected: null};
    currentSettings = {mapTileUrl: "https://tiles.example.com/{z}/{x}/{y}.png"};
    api = {};
    control = {
      getAddressApi: jest.fn(() => api),
      getAddressModel: jest.fn(() => model),
      changeModelAttribute: jest.fn((address, attributes) => Object.assign(model, attributes)),
      publish: jest.fn(),
      // The AngularJS model keeps an empty selection as null and a single one as a value
      formatDataList: jest.fn(list => list.length > 1 ? list : list[0] ?? null)
    };
    actionController = {defineActionListeners: jest.fn(), acceptAction: jest.fn()};

    function ComponentMock(scope, id) {
      const component = {
        address: {view: "base", component: id}, attributeMethods: {}, controller, id, listeners: {}, model, scope,
        asComponent: jest.fn(() => true), storeEvent: jest.fn(),
        getAttribute(attribute) {
          return this.attributeMethods[attribute](this);
        }
      };
      scope.component = component;
      scope.controller = controller;
      return component;
    }

    angular.mock.module("aweApplication", {Component: ComponentMock, Control: control, ActionController: actionController});
    inject(["$compile", "$httpBackend", "$injector", "$rootScope", "$templateCache", "$timeout",
      (_$compile_, $httpBackend, $injector, _$rootScope_, _$templateCache_, _$timeout_) => {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $templateCache = _$templateCache_;
        $timeout = _$timeout_;
        serverData = $injector.get("ServerData");
        utilities = $injector.get("AweUtilities");
        jest.spyOn($injector.get("AweSettings"), "get").mockImplementation(key => currentSettings[key]);
        $httpBackend.when("POST", "settings").respond(DefaultSettings);
      }]);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  /**
   * Compile the map directive as a screen does (the server renders the geo-map tag)
   */
  function compileMap() {
    $templateCache.put(serverData.getAngularTemplateUrl("geoMap"), TEMPLATE);
    const element = $compile('<awe-geo-map geo-map-id="MapTst"></awe-geo-map>')($rootScope.$new());
    document.body.appendChild(element[0]);
    $rootScope.$digest();
    const component = element.isolateScope().component;
    return {element, root: element[0], component, map: mockLeaflet.maps[0], group: mockLeaflet.groups[0]};
  }

  const markerHooks = () => mockLeaflet.markers.map(marker => [marker.element.getAttribute(TestAttributes.testId),
    marker.element.getAttribute("row-id"), marker.element.getAttribute("map-id")]);

  it("creates a Leaflet map (zoom 3 to 19) with the tiles of the mapTileUrl setting and the OSM attribution", () => {
    const {root, map} = compileMap();

    expect(map.element).toBe(root.querySelector(".awe-map-canvas"));
    expect(map.options).toEqual({minZoom: 3, maxZoom: 19});
    expect(mockLeaflet.tiles[0].url).toBe("https://tiles.example.com/{z}/{x}/{y}.png");
    expect(mockLeaflet.tiles[0].options.maxZoom).toBe(19);
    expect(mockLeaflet.tiles[0].options.attribution).toContain("OpenStreetMap");
    expect(mockLeaflet.tiles[0].addTo).toHaveBeenCalledWith(map);
  });

  it("uses the OpenStreetMap tiles when the server sends no tile URL", () => {
    currentSettings = {};
    compileMap();

    expect(mockLeaflet.tiles[0].url).toBe("https://tile.openstreetmap.org/{z}/{x}/{y}.png");
  });

  it("draws one circle marker per row with valid coordinates, with the marker label as popup", () => {
    const {group} = compileMap();

    expect(mockLeaflet.markers.map(marker => marker.latlng)).toEqual([[40.4, -3.7], [41.38, 2.17]]);
    expect(group.layers).toEqual(mockLeaflet.markers);
    expect(mockLeaflet.markers[0].bindPopup).toHaveBeenCalledWith("Madrid");
    expect(mockLeaflet.markers[1].bindPopup).toHaveBeenCalledWith("Barcelona");
  });

  it("reads the default latitude and longitude columns and binds no popup without marker label", () => {
    controller = {visible: true};
    model.values = [{id: "1", latitude: "40.4", longitude: "-3.7"}];
    compileMap();

    expect(mockLeaflet.markers.map(marker => marker.latlng)).toEqual([[40.4, -3.7]]);
    expect(mockLeaflet.markers[0].bindPopup).not.toHaveBeenCalled();
  });

  it("exposes the browser test hooks of the map and its markers", () => {
    const {root} = compileMap();

    expect(root.getAttribute(TestAttributes.testId)).toBe(TestIds.map);
    expect(root.getAttribute("map-id")).toBe("MapTst");
    expect(root.getAttribute(TestAttributes.rendered)).toBe("true");
    expect(markerHooks()).toEqual([[TestIds.mapMarker, "1", "MapTst"], [TestIds.mapMarker, "2", "MapTst"]]);
  });

  it("fits the view to the markers", () => {
    const {map} = compileMap();

    expect(map.fitBounds).toHaveBeenCalledWith([[40.4, -3.7], [41.38, 2.17]]);
    expect(map.setView).not.toHaveBeenCalled();
  });

  it("centres the view on center-latitude and center-longitude with the zoom (4 by default)", () => {
    controller.centerLatitude = 40;
    controller.centerLongitude = -4;
    controller.zoom = 6;
    const {map} = compileMap();
    expect(map.setView).toHaveBeenCalledWith([40, -4], 6);
    expect(map.fitBounds).not.toHaveBeenCalled();

    delete controller.zoom;
    mockLeaflet.maps = [];
    const {map: other} = compileMap();
    expect(other.setView).toHaveBeenCalledWith([40, -4], 4);
  });

  it("shows the default view when there are no markers", () => {
    model.values = [];
    const {map} = compileMap();

    expect(map.setView).toHaveBeenCalledWith([43.8667, 1.1111], 4);
  });

  it("selects the row of a clicked marker, which runs the dependencies, and styles it as selected", () => {
    const {component} = compileMap();

    mockLeaflet.markers[1].handlers.click();
    $timeout.flush();

    expect(control.changeModelAttribute).toHaveBeenCalledWith(component.address, {selected: ["2"]}, false);
    const second = mockLeaflet.markers[1];
    expect(second.element.getAttribute(TestAttributes.selected)).toBe("true");
    expect(second.element.classList.contains("awe-map-marker-selected")).toBe(true);
    expect(second.options.radius).toBe(11);
    expect(mockLeaflet.markers[0].element.getAttribute(TestAttributes.selected)).toBe("false");
    expect(mockLeaflet.markers[0].options.radius).toBe(8);
  });

  it("launches the dependencies on the map and on the cells of the selected row (selectedRowValue)", () => {
    const {component} = compileMap();

    mockLeaflet.markers[1].handlers.click();
    $timeout.flush();

    // A dependency element with a column listens to the cell of the selected row, as with a grid row
    const cell = column => utilities.getAddressId({...component.address, column, row: "2"});
    expect(control.publish).toHaveBeenCalledTimes(1);
    const [event, launchers] = control.publish.mock.calls[0];
    expect(event).toBe("modelChanged");
    expect(launchers).toEqual({
      MapTst: {selected: ["2"]},
      [cell("id")]: {selected: ["2"]},
      [cell("Lat")]: {selected: ["2"]},
      [cell("Lon")]: {selected: ["2"]},
      [cell("Lug")]: {selected: ["2"]}
    });
    expect(cell("Lug")).not.toBe("MapTst");
  });

  it("styles the markers with classes, so the colours come from the design tokens", () => {
    compileMap();

    mockLeaflet.markers.forEach(marker => expect(marker.options.className).toBe("awe-map-marker"));
    expect(mockLeaflet.markers[0].options.color).toBeUndefined();
  });

  it("gives the selected row value and the selected row to the dependencies", () => {
    model.selected = "2";
    const {component} = compileMap();

    expect(component.getAttribute("selectedRow")).toBe("2");
    expect(component.getAttribute("selectedRowValue", "Lug")).toBe("Barcelona");
    expect(component.getAttribute("value")).toBe("2");
    model.selected = null;
    expect(component.getAttribute("selectedRowValue", "Lug")).toBeNull();
  });

  it("sends the selected row with the form, by its value", () => {
    model.values[1].value = "BCN";
    compileMap();

    expect(api.getData()).toEqual({MapTst: null});
    model.selected = ["2"];
    expect(api.getData()).toEqual({MapTst: "BCN"});
  });

  it("draws the markers again and keeps the selection when the values change", () => {
    const {component, group} = compileMap();
    model.selected = "1";

    model.values = [{id: "1", Lat: 1, Lon: 2, Lug: "One"}];
    component.onModelChanged({values: model.values});

    expect(group.layers.map(marker => marker.latlng)).toEqual([[1, 2]]);
    expect(group.layers[0].element.getAttribute(TestAttributes.selected)).toBe("true");
  });

  it("styles the markers again when another component changes the selection", () => {
    const {component} = compileMap();

    model.selected = "1";
    component.onModelChanged({selected: "1"});

    expect(mockLeaflet.markers[0].element.getAttribute(TestAttributes.selected)).toBe("true");
    expect(mockLeaflet.markers.length).toBe(2);
  });

  it("recomputes its size when the screen is resized or a container is shown", () => {
    const {element, map} = compileMap();
    const scope = element.isolateScope();

    scope.$broadcast("resize");
    scope.$broadcast("resize-action");
    expect(map.invalidateSize).toHaveBeenCalledTimes(2);

    controller.visible = false;
    $rootScope.$digest();
    controller.visible = true;
    $rootScope.$digest();
    $timeout.flush();
    expect(map.invalidateSize).toHaveBeenCalledTimes(3);
  });

  describe("polyline action", () => {
    const ROUTE = [{Lat: 40.4, Lon: -3.7}, {Lat: null, Lon: 1}, {Lat: "41.38", Lon: "2.17"}];

    /**
     * Run the polyline action as the action controller does: it calls the method of the map actions
     */
    function polyline(component, rows) {
      const [, actions, scope, executor] = actionController.defineActionListeners.mock.calls[0];
      expect(actions).toBe(ClientActions.map);
      executor[actions.polyline.method]({rows}, scope);
    }

    it("is a targeted map action", () => {
      expect(ClientActions.map.polyline).toEqual({method: "onPolyline", check: ["component", "view"]});
    });

    it("draws the route of the rows with valid coordinates, hides the markers and centres on its first point", () => {
      const {component, map, group} = compileMap();

      polyline(component, ROUTE);

      const [line] = mockLeaflet.polylines;
      expect(line.latlngs).toEqual([[40.4, -3.7], [41.38, 2.17]]);
      expect(line.options).toEqual({weight: 4, className: "awe-map-route"});
      expect(group.layers).toEqual([line]);
      expect(map.setView).toHaveBeenLastCalledWith([40.4, -3.7], 8);
      expect(line.element.getAttribute(TestAttributes.testId)).toBe(TestIds.mapRoute);
      expect(line.element.getAttribute("map-id")).toBe("MapTst");
      expect(component.model.route).toEqual([[40.4, -3.7], [41.38, 2.17]]);
    });

    it("keeps the route when the values change", () => {
      const {component, group} = compileMap();
      polyline(component, ROUTE);

      component.onModelChanged({values: model.values});

      expect(group.layers.map(layer => layer.latlngs)).toEqual([[[40.4, -3.7], [41.38, 2.17]]]);
    });

    it("shows the markers again with an empty route", () => {
      const {component, group} = compileMap();
      polyline(component, ROUTE);

      polyline(component, []);

      expect(mockLeaflet.polylines.length).toBe(1);
      expect(group.layers.map(layer => layer.latlng)).toEqual([[40.4, -3.7], [41.38, 2.17]]);
    });
  });

  it("replaces the legacy Google Maps directive and its library", () => {
    inject(["$injector", $injector => expect($injector.has("aweMapDirective")).toBe(false)]);
    const resources = path.join(__dirname, "..", "..", "..", "main", "resources");
    expect(fs.existsSync(path.join(resources, "js", "lib", "angular-google-maps"))).toBe(false);
    expect(fs.existsSync(path.join(resources, "templates", "angular", "map.html"))).toBe(false);
  });

  it("removes the Leaflet map when the screen is left", () => {
    const {element, map} = compileMap();

    element.isolateScope().$destroy();

    expect(map.remove).toHaveBeenCalled();
  });
});
