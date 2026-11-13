import {
  cardDeltaText,
  cardChipTone,
  cardField,
  cardNumber,
  cardProgress,
  cardProgressStyle,
  cardSparklineType,
  cardTone,
  cardTrend,
  formatCardNumber
} from "../../../src/utilities/card";

// Rules of the shared client contract of the card (#899, S10): the AngularJS client applies the same ones
describe("awe-react-client/test/js/utilities/cardTest.js", () => {

  describe("cardField", () => {
    it("takes the field of the row over the attribute", () => {
      expect(cardField({value: 12}, {value: 3}, "value")).toBe(12);
    });

    it("falls back to the attribute when the row has no value for the field", () => {
      expect(cardField({value: null}, {value: 3}, "value")).toBe(3);
      expect(cardField({value: ""}, {value: 3}, "value")).toBe(3);
      expect(cardField({}, {value: 3}, "value")).toBe(3);
      expect(cardField(undefined, {value: 3}, "value")).toBe(3);
    });

    it("keeps a zero of the row", () => {
      expect(cardField({delta: 0}, {delta: 5}, "delta")).toBe(0);
    });
  });

  describe("cardNumber", () => {
    it("reads finite numbers and numeric strings", () => {
      expect(cardNumber(4.2)).toBe(4.2);
      expect(cardNumber("-4.2")).toBe(-4.2);
      expect(cardNumber(" 0 ")).toBe(0);
    });

    it("is null for anything that is not a finite number", () => {
      [undefined, null, "", "  ", "abc", "12 €", NaN, Infinity, true].forEach(value => expect(cardNumber(value)).toBeNull());
    });
  });

  describe("formatCardNumber", () => {
    it("formats with the language and up to 2 decimals", () => {
      expect(formatCardNumber(1284, "en-GB")).toBe("1,284");
      expect(formatCardNumber(1234.5678, "en-GB")).toBe("1,234.57");
      expect(formatCardNumber(1234.5, "es-ES")).toBe("1234,5");
    });

    it("formats with exactly the decimals given", () => {
      expect(formatCardNumber(412600, "en-GB", 2)).toBe("412,600.00");
      expect(formatCardNumber(4.25, "en-GB", 1)).toBe("4.3");
      expect(formatCardNumber(4.2, "en-GB", "0")).toBe("4");
    });

    it("ignores decimals that are not a whole number of zero or more", () => {
      expect(formatCardNumber(4.256, "en-GB", -1)).toBe("4.26");
      expect(formatCardNumber(4.256, "en-GB", 1.5)).toBe("4.26");
      expect(formatCardNumber(4.256, "en-GB", "abc")).toBe("4.26");
    });

    it("formats with the default language when the language is not valid", () => {
      expect(formatCardNumber(4, "not a language!")).toBe(new Intl.NumberFormat().format(4));
    });
  });

  describe("cardTrend", () => {
    it("takes an explicit trend over the sign of the delta", () => {
      expect(cardTrend("down", 4.2)).toBe("down");
      expect(cardTrend("up", -4.2)).toBe("up");
      expect(cardTrend("flat", 3)).toBe("flat");
    });

    it("derives the trend from the sign of the delta", () => {
      expect(cardTrend(undefined, -0.5)).toBe("down");
      expect(cardTrend(undefined, 0)).toBe("flat");
      expect(cardTrend(undefined, 4.2)).toBe("up");
      expect(cardTrend("sideways", 4.2)).toBe("up");
    });

    it("has no trend without a numeric delta", () => {
      expect(cardTrend(undefined, null)).toBeNull();
      expect(cardTrend("up", null)).toBeNull();
    });
  });

  describe("cardDeltaText", () => {
    it("signs the delta when the trend comes from it, with a minus sign (U+2212)", () => {
      expect(cardDeltaText(12.4, {language: "en-GB"})).toBe("+12.4%");
      expect(cardDeltaText(-4.2, {language: "en-GB"})).toBe("\u22124.2%");
      expect(cardDeltaText(0, {language: "en-GB"})).toBe("0%");
    });

    it("shows the delta without sign when the trend is explicit", () => {
      expect(cardDeltaText(-4.2, {language: "en-GB", explicitTrend: true})).toBe("4.2%");
      expect(cardDeltaText(4.2, {language: "en-GB", explicitTrend: true})).toBe("4.2%");
    });

    it("glues the percent and writes any other unit after a space", () => {
      expect(cardDeltaText(2.4, {language: "en-GB", unit: "pt"})).toBe("+2.4 pt");
      expect(cardDeltaText(2.4, {language: "en-GB", unit: "%"})).toBe("+2.4%");
    });

    it("keeps up to 2 decimals in the delta: number-decimals only applies to the value and the target", () => {
      expect(cardDeltaText(4.2, {language: "en-GB", decimals: 0})).toBe("+4.2%");
      expect(cardDeltaText(4.256, {language: "en-GB"})).toBe("+4.26%");
    });
  });

  describe("cardTone", () => {
    it("takes up as the good trend when the good trend is not up or down", () => {
      expect(cardTone(undefined, "up", "sideways")).toBe("success");
      expect(cardTone(undefined, "down", "flat")).toBe("danger");
    });

    it("takes a valid tone as is", () => {
      expect(cardTone("warning", "up", "up")).toBe("warning");
      expect(cardTone("info", "down", "up")).toBe("info");
      expect(cardTone("neutral", "up", "up")).toBe("neutral");
    });

    it("maps the trend to the tone with the good trend (up by default)", () => {
      expect(cardTone(undefined, "up")).toBe("success");
      expect(cardTone(undefined, "down")).toBe("danger");
      expect(cardTone(undefined, "up", "down")).toBe("danger");
      expect(cardTone(undefined, "down", "down")).toBe("success");
      expect(cardTone("loud", "up", "up")).toBe("success");
    });

    it("is neutral for a flat trend, whatever the good trend", () => {
      expect(cardTone(undefined, "flat", "up")).toBe("neutral");
      expect(cardTone(undefined, "flat", "down")).toBe("neutral");
    });
  });

  describe("cardProgress", () => {
    it("is the rounded percent of the value over the target", () => {
      expect(cardProgress(412600, 500000)).toEqual({percent: 83, width: 83});
      expect(cardProgress(410000, 500000)).toEqual({percent: 82, width: 82});
    });

    it("clamps the width of the bar to 0..100", () => {
      expect(cardProgress(150, 100)).toEqual({percent: 150, width: 100});
      expect(cardProgress(-20, 100)).toEqual({percent: -20, width: 0});
    });

    it("is null without a numeric value or a numeric non zero target", () => {
      expect(cardProgress(10, null)).toBeNull();
      expect(cardProgress(10, 0)).toBeNull();
      expect(cardProgress(null, 100)).toBeNull();
    });
  });
  describe("cardSparklineType", () => {
    it.each(["line", "area", "bar"])("keeps the type %s", type => {
      expect(cardSparklineType(type)).toBe(type);
    });

    it.each([undefined, null, "", "pie"])("draws an area with the type %s", type => {
      expect(cardSparklineType(type)).toBe("area");
    });
  });

  describe("cardProgressStyle", () => {
    it("keeps the gauge", () => {
      expect(cardProgressStyle("gauge")).toBe("gauge");
    });

    it.each([undefined, null, "", "bar", "ring"])("draws a bar with the style %s", style => {
      expect(cardProgressStyle(style)).toBe("bar");
    });
  });

  describe("cardChipTone", () => {
    it("keeps a known tone", () => {
      ["success", "warning", "danger", "info", "neutral"].forEach(tone => expect(cardChipTone(tone)).toBe(tone));
    });

    it("is neutral for a missing or unknown tone", () => {
      expect(cardChipTone(undefined)).toBe("neutral");
      expect(cardChipTone("")).toBe("neutral");
      expect(cardChipTone("red")).toBe("neutral");
    });
  });
});
