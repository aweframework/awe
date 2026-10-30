import {overlaps} from "./support/importantGuard";

describe("awe-framework/awe-client-angular/src/test/jest/styles/importantGuard", () => {
  describe("overlaps", () => {
    it.each([
      ["padding", "padding"],
      ["padding", "padding-left"],
      ["margin-top", "margin"],
      ["border-width", "border-left-width"],
      ["border-left-width", "border"],
      ["border-left", "border-left-width"],
      ["border-left", "border-width"],
      ["border-top", "border"],
      ["border-top", "border-top-color"],
      ["border", "border-style"],
      ["border-radius", "border-top-left-radius"],
      ["border-bottom-right-radius", "border-radius"],
      ["border-color", "border-left"]
    ])("%s and %s override one another", (first, second) => {
      expect(overlaps(first, second)).toBe(true);
      expect(overlaps(second, first)).toBe(true);
    });

    it.each([
      ["padding", "margin"],
      ["padding-left", "padding-right"],
      ["padding-left", "margin-left"],
      ["border-left-width", "border-right-width"],
      ["border-left", "border-right-width"],
      ["border-left", "border-top"],
      ["border-width", "border-style"],
      ["border-left-width", "border-left-color"],
      ["border-top-left-radius", "border-top-right-radius"],
      ["border-radius", "border-width"],
      ["border", "border-radius"],
      ["display", "visibility"],
      ["float", "padding"]
    ])("%s and %s are independent", (first, second) => {
      expect(overlaps(first, second)).toBe(false);
      expect(overlaps(second, first)).toBe(false);
    });
  });
});
