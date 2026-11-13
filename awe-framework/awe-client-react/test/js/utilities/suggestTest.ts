import {getSuggestionLabel, withTextLabel, withTextLabels} from "../../../src/utilities/suggest";

describe("awe-react-client/test/js/utilities/suggestTest.ts", () => {
  describe("getSuggestionLabel", () => {
    it.each([
      ["a text label", {value: 1, label: "One"}, "One"],
      ["a numeric label", {value: "a", label: 15}, "15"],
      ["a zero label", {value: "a", label: 0}, "0"],
      ["no label (the value)", {value: 7}, "7"],
      ["a null label (the value)", {value: "v", label: null}, "v"],
      ["neither label nor value", {}, ""],
      ["null", null, ""],
      ["undefined", undefined, ""],
      ["a null value", {value: null}, ""]
    ])("returns the label as text of %s", (_name, item, expected) => {
      expect(getSuggestionLabel(item)).toBe(expected);
    });
  });

  describe("withTextLabels", () => {
    it("converts the labels and keeps the values untouched", () => {
      expect(withTextLabels([{value: 1, label: 2}, {value: 3}])).toEqual([{value: 1, label: "2"}, {value: 3, label: "3"}]);
    });

    it.each([[null], [undefined]])("returns an empty list for %s", (items) => {
      expect(withTextLabels(items)).toEqual([]);
    });

    it("does not change the received suggestions", () => {
      const items = [{value: 1, label: 2}];
      withTextLabels(items);
      expect(items).toEqual([{value: 1, label: 2}]);
    });
  });

  describe("withTextLabel", () => {
    it("converts the labels of a list of suggestions", () => {
      expect(withTextLabel([{value: 1, label: 2}])).toEqual([{value: 1, label: "2"}]);
    });

    it("converts the label of a suggestion", () => {
      expect(withTextLabel({value: 1, label: 2})).toEqual({value: 1, label: "2"});
    });

    it("keeps an object without label as it is", () => {
      const item = {value: 1};
      expect(withTextLabel(item)).toBe(item);
    });

    it.each([["typed text"], [""], [null], [undefined]])("keeps %p as it is", (value) => {
      expect(withTextLabel(value)).toBe(value);
    });
  });
});
