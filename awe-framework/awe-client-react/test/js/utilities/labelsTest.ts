import {translateLabel} from "../../../src/utilities/labels";

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
