import {criterionPlaceholder, isCriterionRequired, requiredAria, translateLabel} from "../../../src/utilities/labels";

describe("awe-react-client/test/js/utilities/labelsTest.ts", () => {
  const upper = (key: string): string => key.toUpperCase();

  it("returns an empty string for the default label", () => {
    expect(translateLabel()).toBe("");
  });

  it("returns the label as is with the default translate function", () => {
    expect(translateLabel("hello world")).toBe("hello world");
  });

  it("translates each word of the label", () => {
    expect(translateLabel("hello world", upper)).toBe("HELLO WORLD");
  });

  it("removes the blank words", () => {
    expect(translateLabel("  hello   world ", upper)).toBe("HELLO WORLD");
  });

  it("replaces the numbered placeholders with the arguments", () => {
    expect(translateLabel("{0} of {1}", (key) => key, 3, "ten")).toBe("3 of ten");
  });

  it("replaces every occurrence of a placeholder", () => {
    expect(translateLabel("{0} and {0}", (key) => key, "x")).toBe("x and x");
  });

  it("converts a non string label to text", () => {
    expect(translateLabel(42, upper)).toBe("42");
  });
});

describe("awe-react-client/test/js/utilities/labelsTest.ts required criterion", () => {
  const upper = (key: string): string => key.toUpperCase();

  it("reads the required state from the attributes or the validation rules", () => {
    expect(isCriterionRequired({required: true}, {})).toBe(true);
    expect(isCriterionRequired({}, {required: true})).toBe(true);
    expect(isCriterionRequired({}, {})).toBe(false);
    expect(isCriterionRequired(undefined, undefined)).toBe(false);
  });

  it("appends the asterisk to the placeholder of a required criterion without label", () => {
    expect(criterionPlaceholder({placeholder: "user"}, {required: true}, upper)).toBe("USER *");
  });

  it("keeps the placeholder when the criterion has a label, is not required or has no placeholder", () => {
    expect(criterionPlaceholder({placeholder: "user", label: "Name"}, {required: true}, upper)).toBe("USER");
    expect(criterionPlaceholder({placeholder: "user"}, {}, upper)).toBe("USER");
    expect(criterionPlaceholder({}, {required: true}, upper)).toBe("");
  });

  it("appends the asterisk when the label is hidden by the no-label style", () => {
    expect(criterionPlaceholder({placeholder: "user", label: "Name", style: "no-label"}, {required: true}, upper))
      .toBe("USER *");
  });

  it("does not append the asterisk when the placeholder is drawn with its own asterisk", () => {
    expect(criterionPlaceholder({placeholder: "user"}, {required: true}, upper, false)).toBe("USER");
  });

  it("gives aria-required only to a required criterion", () => {
    expect(requiredAria({required: true}, {})).toEqual({"aria-required": "true"});
    expect(requiredAria({}, {})).toEqual({});
  });
});
