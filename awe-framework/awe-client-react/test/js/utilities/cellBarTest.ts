import {barFill, barScale, barSegments, hasBarOptions, toBarNumber} from "../../../src/utilities/cellBar";

describe("awe-react-client/test/js/utilities/cellBarTest.ts", () => {
  describe("toBarNumber", () => {
    it("reads numbers and numeric strings", () => {
      expect(toBarNumber(4.13)).toBe(4.13);
      expect(toBarNumber(" 12.5 ")).toBe(12.5);
      expect(toBarNumber("-3")).toBe(-3);
      expect(toBarNumber(0)).toBe(0);
    });

    it("ignores anything that is not a finite number", () => {
      [undefined, null, "", "  ", "abc", NaN, Infinity, {}, true].forEach(value => expect(toBarNumber(value)).toBeNull());
    });
  });

  describe("barScale", () => {
    it("defaults to 0..100", () => {
      expect(barScale(undefined, undefined)).toEqual({min: 0, max: 100});
    });

    it("reads numbers and numeric strings", () => {
      expect(barScale("-40", 40)).toEqual({min: -40, max: 40});
    });

    it("falls back to 0..100 when the scale is not valid", () => {
      expect(barScale(10, 10)).toEqual({min: 0, max: 100});
      expect(barScale(50, 10)).toEqual({min: 0, max: 100});
      expect(barScale("a", "b")).toEqual({min: 0, max: 100});
    });
  });

  describe("barFill", () => {
    it("fills from the zero to the value", () => {
      const fill = barFill({min: 0, max: 40}, 4.13);
      expect(fill.left).toBeCloseTo(0);
      expect(fill.width).toBeCloseTo(10.325);
      expect(fill.negative).toBe(false);
    });

    it("fills a negative value from the value to the zero", () => {
      const fill = barFill({min: -40, max: 40}, -10);
      expect(fill.left).toBeCloseTo(37.5);
      expect(fill.width).toBeCloseTo(12.5);
      expect(fill.negative).toBe(true);
    });

    it("fills a positive value from the zero in the middle", () => {
      const fill = barFill({min: -40, max: 40}, 20);
      expect(fill.left).toBeCloseTo(50);
      expect(fill.width).toBeCloseTo(25);
      expect(fill.negative).toBe(false);
    });

    it("clamps the value to the scale", () => {
      expect(barFill({min: 0, max: 100}, 250)).toMatchObject({left: 0, width: 100});
      expect(barFill({min: -40, max: 40}, -90)).toMatchObject({left: 0, width: 50, negative: true});
    });

    it("takes the zero as the start when the scale does not include it", () => {
      expect(barFill({min: 10, max: 20}, 15)).toMatchObject({left: 0, width: 50});
      expect(barFill({min: -20, max: -10}, -15)).toMatchObject({left: 50, width: 50});
    });
  });

  describe("barSegments", () => {
    it("draws consecutive segments from the zero", () => {
      const segments = barSegments({min: 0, max: 100}, [10, 20, 30]);
      expect(segments.map(segment => segment.index)).toEqual([0, 1, 2]);
      expect(segments.map(segment => segment.left)).toEqual([0, 10, 30]);
      expect(segments.map(segment => segment.width)).toEqual([10, 20, 30]);
    });

    it("ignores values that are not positive numbers but keeps the index of the others", () => {
      const segments = barSegments({min: 0, max: 100}, [10, null, -5, 0, 20]);
      expect(segments.map(segment => segment.index)).toEqual([0, 4]);
      expect(segments.map(segment => segment.left)).toEqual([0, 10]);
    });

    it("clamps the segments to the end of the scale", () => {
      const segments = barSegments({min: 0, max: 50}, [30, 30, 30]);
      expect(segments.map(segment => [segment.left, segment.width])).toEqual([[0, 60], [60, 40]]);
      expect(segments.map(segment => segment.index)).toEqual([0, 1]);
    });

    it("starts at the zero of a signed scale", () => {
      const segments = barSegments({min: -50, max: 50}, [25]);
      expect(segments[0]).toMatchObject({index: 0, left: 50, width: 25});
    });
  });

  describe("hasBarOptions", () => {
    it("is false without any bar attribute", () => {
      expect(hasBarOptions({})).toBe(false);
      expect(hasBarOptions({barSecondField: "", barStackFields: "", barLeafOnly: false})).toBe(false);
    });

    it.each([
      {barMin: 0}, {barMax: "40"}, {barSecondField: "other"}, {barStackFields: "a,b"}, {barLeafOnly: true}
    ])("is true with %j", (node) => {
      expect(hasBarOptions(node)).toBe(true);
    });
  });
});
