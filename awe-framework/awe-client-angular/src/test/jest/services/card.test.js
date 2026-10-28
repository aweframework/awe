import {
  buildCard, cardSparklineMarkup, formatCardNumber, getCardField, getCardTone, getCardTrend
} from "../../../main/resources/js/awe/services/card";

// Keys are translated with a marker, so the tests see which texts go through the translation
const translate = text => `t(${text})`;
const build = (attributes, row, language = "en-GB") => buildCard(attributes, row, {language, translate});

describe("awe-framework/awe-client-angular/src/test/jest/services/card.test.js", () => {
  describe("data precedence", () => {
    it("takes a field from the row when it is present, else from the attributes", () => {
      expect(getCardField("unit", {unit: "€"}, {unit: "$"})).toBe("$");
      expect(getCardField("unit", {unit: "€"}, {})).toBe("€");
      expect(getCardField("unit", {unit: "€"}, undefined)).toBe("€");
    });

    it("ignores null and empty row fields", () => {
      expect(getCardField("unit", {unit: "€"}, {unit: null})).toBe("€");
      expect(getCardField("unit", {unit: "€"}, {unit: ""})).toBe("€");
    });

    it("keeps a zero of the row", () => {
      expect(getCardField("value", {value: 5}, {value: 0})).toBe(0);
    });

    it("titles the card with the title, else with the label, translated", () => {
      expect(build({title: "TITLE", label: "LABEL"}).title).toBe("t(TITLE)");
      expect(build({label: "LABEL"}).title).toBe("t(LABEL)");
      expect(build({label: "LABEL"}, {title: "ROW_TITLE"}).title).toBe("t(ROW_TITLE)");
    });

    it("translates the texts and leaves out the missing ones", () => {
      const card = build({subtitle: "SUB", caption: "CAP", description: "DESC", footer: "FOOT", delta: 1});

      expect(card.subtitle).toBe("t(SUB)");
      expect(card.caption).toBe("t(CAP)");
      expect(card.description).toBe("t(DESC)");
      expect(card.footer).toBe("t(FOOT)");

      const empty = build({});
      expect(empty.title).toBeNull();
      expect(empty.subtitle).toBeNull();
      expect(empty.caption).toBeNull();
      expect(empty.description).toBeNull();
      expect(empty.footer).toBeNull();
      expect(empty.value).toBeNull();
      expect(empty.delta).toBeNull();
      expect(empty.progress).toBeNull();
      expect(empty.icon).toBeNull();
    });
  });

  describe("numbers", () => {
    it("formats with the user language and up to 2 decimals", () => {
      expect(formatCardNumber(1284, "en-GB")).toBe("1,284");
      expect(formatCardNumber(1284.567, "en-GB")).toBe("1,284.57");
      expect(formatCardNumber(1284.5, "es-ES")).toBe("1284,5");
      expect(formatCardNumber(12840.5, "es-ES")).toBe("12.840,5");
    });

    it("formats with exactly numberDecimals decimals when set", () => {
      expect(formatCardNumber(1284, "en-GB", 2)).toBe("1,284.00");
      expect(formatCardNumber(1284.567, "en-GB", 0)).toBe("1,285");
      expect(build({value: 3, numberDecimals: 1}).value.number).toBe("3.0");
    });

    it("keeps up to 2 decimals in the delta: number-decimals only applies to the value and the target", () => {
      const card = build({value: 1284, delta: 4.256, numberDecimals: 0});
      expect(card.value.number).toBe("1,284");
      expect(card.delta.text).toBe("+4.26%");
    });

    it("takes numeric strings as numbers and shows a non numeric value as is", () => {
      expect(build({value: "1284.5"}).value.number).toBe("1,284.5");
      expect(build({value: "N/A"}).value.number).toBe("N/A");
    });

    it("shows the value unit apart", () => {
      expect(build({value: 96.8, unit: "%"}).value).toEqual({number: "96.8", unit: "%"});
      expect(build({value: 10}).value).toEqual({number: "10", unit: null});
    });
  });

  describe("trend", () => {
    it("comes from the sign of the delta, zero included", () => {
      expect(getCardTrend(undefined, -2)).toBe("down");
      expect(getCardTrend(undefined, 0)).toBe("flat");
      expect(getCardTrend(undefined, 3)).toBe("up");
    });

    it("is the explicit trend when it is valid, even against the sign of the delta", () => {
      expect(getCardTrend("down", 4)).toBe("down");
      expect(getCardTrend("flat", 4)).toBe("flat");
      expect(getCardTrend("sideways", 4)).toBe("up");
    });

    it("gives no delta badge without a numeric delta", () => {
      expect(build({trend: "up"}).delta).toBeNull();
      expect(build({delta: "abc"}).delta).toBeNull();
    });
  });

  describe("delta text", () => {
    it("is signed from the delta, with a true minus sign and the percent glued by default", () => {
      expect(build({delta: 4.2}).delta.text).toBe("+4.2%");
      expect(build({delta: -4.2}).delta.text).toBe("\u22124.2%");
      expect(build({delta: 0}).delta.text).toBe("0%");
    });

    it("is unsigned with an explicit trend", () => {
      expect(build({delta: -4.2, trend: "up"}).delta.text).toBe("4.2%");
      expect(build({delta: 4.2, trend: "down"}).delta.text).toBe("4.2%");
    });

    it("puts any other unit after a space", () => {
      expect(build({delta: 2.4, deltaUnit: "pt"}).delta.text).toBe("+2.4 pt");
      expect(build({delta: 12.4, deltaUnit: "%"}).delta.text).toBe("+12.4%");
    });

    it("has the arrow of its trend and the translated trend label", () => {
      expect(build({delta: 1}).delta).toMatchObject({trend: "up", arrow: "\u2197", label: "t(SCREEN_TEXT_CARD_TREND_UP)"});
      expect(build({delta: -1}).delta).toMatchObject({trend: "down", arrow: "\u2198", label: "t(SCREEN_TEXT_CARD_TREND_DOWN)"});
      expect(build({delta: 0}).delta).toMatchObject({trend: "flat", arrow: "\u2192", label: "t(SCREEN_TEXT_CARD_TREND_FLAT)"});
    });
  });

  describe("tone", () => {
    it("is the tone field when it is valid", () => {
      expect(getCardTone({tone: "warning", trend: "up", goodTrend: "up"})).toBe("warning");
      expect(getCardTone({tone: "info", trend: "flat"})).toBe("info");
      expect(getCardTone({tone: "loud", trend: "up"})).toBe("success");
    });

    it("is neutral when flat, success when the trend is the good one (up by default), danger otherwise", () => {
      expect(getCardTone({trend: "flat", goodTrend: "down"})).toBe("neutral");
      expect(getCardTone({trend: "up"})).toBe("success");
      expect(getCardTone({trend: "down"})).toBe("danger");
      expect(getCardTone({trend: "down", goodTrend: "down"})).toBe("success");
      expect(getCardTone({trend: "up", goodTrend: "down"})).toBe("danger");
    });

    it("never depends on the sign of the value", () => {
      expect(build({value: -500, delta: 3, goodTrend: "up"}).delta.tone).toBe("success");
      expect(build({value: 500, delta: -3}).delta.tone).toBe("danger");
    });

    it("follows the explicit trend, the good trend and the tone of the row", () => {
      expect(build({delta: 3, trend: "down"}).delta.tone).toBe("danger");
      expect(build({delta: 3, goodTrend: "down"}).delta.tone).toBe("danger");
      expect(build({delta: 3, tone: "info"}, {tone: "warning"}).delta.tone).toBe("warning");
    });
  });

  describe("placement", () => {
    it("puts the delta in the footer when there is an icon", () => {
      const card = build({icon: "users", delta: 4});

      expect(card.icon).toBe("users");
      expect(card.iconTone).toBe("primary");
      expect(card.deltaInHeader).toBe(false);
      expect(card.deltaInFooter).toBe(true);
    });

    it("puts the delta in the header without icon", () => {
      const card = build({delta: 4});

      expect(card.deltaInHeader).toBe(true);
      expect(card.deltaInFooter).toBe(false);
    });

    it("colours the icon tile with the icon tone", () => {
      expect(build({icon: "users", iconTone: "warning"}).iconTone).toBe("warning");
    });
  });

  describe("progress", () => {
    it("is the rounded percent of the value over the target, with the formatted target and its unit", () => {
      expect(build({value: 410000, target: 500000, unit: "€"}).progress)
        .toEqual({percent: 82, width: 82, text: "82 % t(SCREEN_TEXT_CARD_TARGET_OF) 500,000 €"});
      expect(build({value: 1, target: 3}).progress)
        .toEqual({percent: 33, width: 33, text: "33 % t(SCREEN_TEXT_CARD_TARGET_OF) 3"});
    });

    it("clamps the bar width to 0..100", () => {
      expect(build({value: 600, target: 500}).progress).toMatchObject({percent: 120, width: 100});
      expect(build({value: -50, target: 500}).progress).toMatchObject({percent: -10, width: 0});
    });

    it("is absent without a numeric non zero target or a numeric value", () => {
      expect(build({value: 10}).progress).toBeNull();
      expect(build({value: 10, target: 0}).progress).toBeNull();
      expect(build({value: 10, target: "abc"}).progress).toBeNull();
      expect(build({value: "N/A", target: 10}).progress).toBeNull();
    });
  });

  describe("sparkline", () => {
    it("draws the series of the attribute text, as an area by default, in the current colour", () => {
      const sparkline = build({sparkline: "[93.1,93.8,92.6]"}).sparkline;

      expect(sparkline.type).toBe("area");
      expect(sparkline.label).toBe("93.1, 93.8, 92.6");
      expect(sparkline.shapes.some(shape => Object.values(shape.attrs).includes("currentColor"))).toBe(true);
    });

    it("draws the series of a row array, the row over the attribute", () => {
      expect(build({}, {sparkline: [1, 2, 3]}).sparkline.label).toBe("1, 2, 3");
      expect(build({sparkline: "[9,8]"}, {sparkline: [1, 2, 3]}).sparkline.label).toBe("1, 2, 3");
      expect(build({sparkline: "[9,8]"}, {sparkline: null}).sparkline.label).toBe("9, 8");
    });

    it("takes the sparkline type, area when it is not line or bar", () => {
      expect(build({sparkline: "[1,2]", sparklineType: "bar"}).sparkline.type).toBe("bar");
      expect(build({sparkline: "[1,2]", sparklineType: "line"}).sparkline.type).toBe("line");
      expect(build({sparkline: "[1,2]", sparklineType: "pie"}).sparkline.type).toBe("area");
      expect(build({sparkline: "[1,2]", sparklineType: "line"}, {sparklineType: "bar"}).sparkline.type).toBe("bar");
    });

    it("draws nothing with an invalid or empty series", () => {
      expect(build({}).sparkline).toBeNull();
      expect(build({sparkline: "[]"}).sparkline).toBeNull();
      expect(build({sparkline: "not json"}).sparkline).toBeNull();
      expect(build({sparkline: "[1,\"a\"]"}).sparkline).toBeNull();
    });

    it("writes an SVG that fills the width of the card", () => {
      const markup = cardSparklineMarkup(build({sparkline: "[1,2,3]", sparklineType: "line"}).sparkline);

      expect(markup).toMatch(/^<svg class="awe-sparkline" data-type="line" viewBox="0 0 100 24" preserveAspectRatio="none" role="img" aria-label="1, 2, 3" focusable="false">/);
      expect(markup).not.toMatch(/<svg[^>]* (width|height)=/);
      expect(cardSparklineMarkup(null)).toBe("");
    });
  });

  describe("progress style", () => {
    it("is a bar by default and a gauge only when asked", () => {
      expect(build({value: 1, target: 2}).progressStyle).toBe("bar");
      expect(build({value: 1, target: 2, progressStyle: "gauge"}).progressStyle).toBe("gauge");
      expect(build({value: 1, target: 2, progressStyle: "dial"}).progressStyle).toBe("bar");
      expect(build({value: 1, target: 2, progressStyle: "bar"}, {progressStyle: "gauge"}).progressStyle).toBe("gauge");
    });
  });

  describe("visibility", () => {
    it("is hidden only when visible is false", () => {
      expect(build({}).visible).toBe(true);
      expect(build({visible: true}).visible).toBe(true);
      expect(build({visible: false}).visible).toBe(false);
    });
  });
});
