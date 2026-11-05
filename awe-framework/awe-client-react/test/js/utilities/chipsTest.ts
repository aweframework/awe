import {visibleChipCount} from "../../../src/utilities/chips";

describe("awe-react-client/test/js/utilities/chipsTest.ts", () => {
  describe("visibleChipCount", () => {
    it("shows every chip when they all fit, with no room kept for the counter", () => {
      expect(visibleChipCount([40, 50, 30], 130, 24, 4)).toBe(3);
    });

    it("keeps the room of the counter when some chips do not fit", () => {
      // 40 + 4 + 50 + 4 + 24 = 122 fits in 125; adding the third chip does not
      expect(visibleChipCount([40, 50, 30], 125, 24, 4)).toBe(2);
    });

    it("never shows less than one chip, which the layout cuts with an ellipsis", () => {
      expect(visibleChipCount([200, 50], 100, 24, 4)).toBe(1);
    });

    it("shows nothing when there are no chips", () => {
      expect(visibleChipCount([], 100, 24, 4)).toBe(0);
    });
  });
});
