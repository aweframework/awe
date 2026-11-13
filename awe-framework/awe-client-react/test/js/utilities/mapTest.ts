import {DEFAULT_LATITUDE, DEFAULT_LONGITUDE, toCoordinate, toPosition} from "../../../src/utilities/map";

describe("awe-react-client/test/js/utilities/mapTest.ts", () => {
  it("keeps the default names of the coordinate columns", () => {
    expect([DEFAULT_LATITUDE, DEFAULT_LONGITUDE]).toEqual(["latitude", "longitude"]);
  });

  describe("toCoordinate", () => {
    it("converts numbers and numeric texts", () => {
      expect(toCoordinate(40.4)).toBe(40.4);
      expect(toCoordinate("-3.7")).toBe(-3.7);
      expect(toCoordinate(0)).toBe(0);
      expect(toCoordinate("0")).toBe(0);
    });

    it("returns null for empty values", () => {
      expect(toCoordinate(null)).toBeNull();
      expect(toCoordinate(undefined)).toBeNull();
      expect(toCoordinate("")).toBeNull();
    });

    it("returns null for values that are not finite numbers", () => {
      expect(toCoordinate("north")).toBeNull();
      expect(toCoordinate(NaN)).toBeNull();
      expect(toCoordinate(Infinity)).toBeNull();
      expect(toCoordinate({})).toBeNull();
    });
  });

  describe("toPosition", () => {
    it("builds the latitude and longitude pair of a row", () => {
      expect(toPosition({lat: "40.4", lng: -3.7}, "lat", "lng")).toEqual([40.4, -3.7]);
    });

    it("returns null when a coordinate is empty or not numeric", () => {
      expect(toPosition({lat: 40.4, lng: ""}, "lat", "lng")).toBeNull();
      expect(toPosition({lat: "x", lng: 1}, "lat", "lng")).toBeNull();
      expect(toPosition({lat: 40.4}, "lat", "lng")).toBeNull();
    });

    it("returns null when there is no row", () => {
      expect(toPosition(null as never, "lat", "lng")).toBeNull();
      expect(toPosition(undefined as never, "lat", "lng")).toBeNull();
    });
  });
});
