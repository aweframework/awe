import {
  ComponentType,
  DATE_RANGE_FROM_SUFFIX,
  DATE_RANGE_TO_SUFFIX,
  compareEqualValues,
  getFirstDefinedAndNotNullValue,
  getFirstDefinedValue,
  isEmpty,
  isEmptyCell,
  isMultipleComponent
} from "../../../src/utilities/general";

describe("awe-react-client/test/js/utilities/generalTest.ts", () => {
  describe("constants", () => {
    it("keeps the component types the server sends", () => {
      expect(ComponentType.COMPONENT_SELECT_MULTIPLE).toBe("select-multiple");
      expect(ComponentType.COMPONENT_PICKLIST).toBe("picklist");
      expect(Object.keys(ComponentType)).toHaveLength(35);
    });

    it("keeps the date range suffixes", () => {
      expect([DATE_RANGE_FROM_SUFFIX, DATE_RANGE_TO_SUFFIX]).toEqual([".from", ".to"]);
    });
  });

  describe("isMultipleComponent", () => {
    it.each([["select-multiple", true], ["suggest-multiple", true], ["picklist", true], ["select", false], ["", false]])(
      "answers for %s",
      (type, expected) => {
        expect(isMultipleComponent(type)).toBe(expected);
      }
    );
  });

  describe("compareEqualValues", () => {
    it.each([
      ["same type and value", 1, 1, true],
      ["same type, other value", "a", "b", false],
      ["number and its text", 1, "1", true],
      ["number and other text", 1, "2", false],
      ["null and undefined", null, undefined, true],
      ["null and blank text", null, "  ", true],
      ["undefined and empty text", undefined, "", true],
      ["zero and empty text", 0, "", false],
      ["boolean and its text", true, "true", true]
    ])("compares %s", (_name, a, b, expected) => {
      expect(compareEqualValues(a, b)).toBe(expected);
    });
  });

  describe("getFirstDefinedValue", () => {
    it("returns the first value that is not undefined", () => {
      expect(getFirstDefinedValue(undefined, null, 3)).toBeNull();
      expect(getFirstDefinedValue(undefined, 0, 3)).toBe(0);
    });

    it("returns undefined without values", () => {
      expect(getFirstDefinedValue()).toBeUndefined();
    });

    it("returns the last value when all of them are undefined", () => {
      expect(getFirstDefinedValue(undefined, undefined)).toBeUndefined();
    });
  });

  describe("getFirstDefinedAndNotNullValue", () => {
    it("skips undefined and null", () => {
      expect(getFirstDefinedAndNotNullValue(undefined, null, "x", "y")).toBe("x");
      expect(getFirstDefinedAndNotNullValue(null, false, 1)).toBe(false);
      expect(getFirstDefinedAndNotNullValue(undefined, 0)).toBe(0);
    });

    it("returns the last value when every value is undefined or null", () => {
      expect(getFirstDefinedAndNotNullValue(undefined, null)).toBeNull();
      expect(getFirstDefinedAndNotNullValue(null, undefined)).toBeUndefined();
    });

    it("returns undefined without values", () => {
      expect(getFirstDefinedAndNotNullValue()).toBeUndefined();
    });
  });

  describe("isEmpty", () => {
    it.each([[null], [undefined], [""], ["   "], ["\n\t"]])("%j is empty", (value) => {
      expect(isEmpty(value)).toBe(true);
    });

    it("treats an empty list as empty (it is converted to an empty text)", () => {
      expect(isEmpty([])).toBe(true);
    });

    it.each([[0], [false], ["a"], [{}], [" a "]])("%j is not empty", (value) => {
      expect(isEmpty(value)).toBe(false);
    });
  });

  describe("isEmptyCell", () => {
    it("is empty when the value is empty", () => {
      expect(isEmptyCell(null as never)).toBe(true);
      expect(isEmptyCell("")).toBe(true);
    });

    it("is empty when the cell value is empty", () => {
      expect(isEmptyCell({value: ""})).toBe(true);
      expect(isEmptyCell({value: null})).toBe(true);
      expect(isEmptyCell({})).toBe(true);
    });

    it("is not empty with a cell value or a plain value", () => {
      expect(isEmptyCell({value: 0})).toBe(false);
      expect(isEmptyCell("a")).toBe(false);
      expect(isEmptyCell(0)).toBe(false);
    });
  });
});
