import {effective, hoistSupported, mediasOf, namesOf, normalizeSelector, orderWinners} from "./support/compatEquivalence";
import {getRules, withShadowed} from "./support/compileTailwind";

describe("awe-framework/awe-client-angular/src/test/jest/styles/compatEquivalence (helpers of the compat equivalence tests)", () => {
  it("names the classes, the ids and the data-toggle marker of a selector", () => {
    expect(namesOf("#ui-grid-twbs .btn-group > .btn:hover")).toEqual(["#ui-grid-twbs", "btn-group", "btn"]);
    expect(namesOf("[data-toggle=\"buttons\"] > .btn input[type=\"radio\"]")).toEqual(["[data-toggle=\"buttons\"]", "btn"]);
  });

  it("writes pseudo-elements with one colon", () => {
    expect(normalizeSelector(".a::after")).toBe(".a:after");
    expect(normalizeSelector(".a:hover")).toBe(".a:hover");
  });

  it("keeps the !important of a declaration that a later normal one of the same rule repeats (`float: left !important; float: left`)", () => {
    const [rule] = getRules(".navbar-left { float: left !important; float: left; margin: 0; }");
    // The model the frozen rules were hashed with: the last declaration; the important one is kept apart, and withShadowed puts it back
    expect(rule.declarations).toEqual({float: "left", margin: "0"});
    expect(rule.shadowed).toEqual({float: "left !important"});
    expect(withShadowed([rule])[0].declarations).toEqual({float: "left !important", margin: "0"});
    expect(getRules(".a { float: left; float: right !important; }")[0].shadowed).toBeUndefined();
  });

  it("hoists a supported declaration over its fallback", () => {
    const css = ".a { color: red; @supports (color: color-mix(in srgb, red, blue)) { color: color-mix(in srgb, red, blue); } }";
    expect(getRules(hoistSupported(css))[0].declarations.color).toBe("color-mix(in srgb, red, blue)");
  });

  it("resolves the effective declarations: !important beats a later normal value, otherwise the last wins, --tw-* are ignored", () => {
    const rules = getRules(".a { color: red !important; margin: 1px; --tw-x: 1; } .a { color: blue; margin: 2px; } @media print { .a { margin: 3px; } }");
    expect(effective(rules, ".a", "")).toEqual({color: "red !important", margin: "2px"});
    expect(effective(rules, ".a", "print")).toEqual({margin: "3px"});
    expect(mediasOf(rules, ".a")).toEqual(["", "print"]);
  });

  it("tells which selector wins when only the order decides, and ignores !important and equal values", () => {
    const identity = value => value;
    const first = getRules(".a:hover { color: red; } .b:hover { color: blue; } .a:hover { top: 1px !important; } .b:hover { top: 2px !important; }");
    expect(orderWinners(first, [".a:hover", ".b:hover"], identity)).toEqual({"|.a:hover|.b:hover|color/color": ".b:hover"});
    const swapped = getRules(".b:hover { color: blue; } .a:hover { color: red; }");
    expect(orderWinners(swapped, [".a:hover", ".b:hover"], identity)).toEqual({"|.a:hover|.b:hover|color/color": ".a:hover"});
  });

  it("does not report an order-decided pair when another rule of the same selector sets the property with !important", () => {
    const identity = value => value;
    const rules = getRules(".a:hover { color: red; } .a:hover { color: green !important; } .b:hover { color: blue; }");
    expect(orderWinners(rules, [".a:hover", ".b:hover"], identity)).toEqual({});
  });

  it("produces no key for equal values", () => {
    const identity = value => value;
    expect(orderWinners(getRules(".a:hover { color: red; } .b:hover { color: red; }"), [".a:hover", ".b:hover"], identity)).toEqual({});
  });

  it("keeps the order of different media separate", () => {
    const identity = value => value;
    const rules = getRules(".a:hover { color: red; } @media print { .b:hover { color: blue; } }");
    expect(orderWinners(rules, [".a:hover", ".b:hover"], identity)).toEqual({});
  });

  describe("hoistSupported", () => {
    it("adds a supported declaration that has no fallback", () => {
      const css = ".a { margin: 0; @supports (color: color-mix(in srgb, red, blue)) { color: color-mix(in srgb, red, blue); } }";
      expect(getRules(hoistSupported(css))[0].declarations).toEqual({margin: "0", color: "color-mix(in srgb, red, blue)"});
    });

    it("hoists every declaration of one @supports block over its fallback", () => {
      const css = ".a { color: red; background: white; @supports (color: color-mix(in srgb, red, blue)) { color: color-mix(in srgb, red, blue); background: color-mix(in srgb, white, blue); } }";
      expect(getRules(hoistSupported(css))[0].declarations).toEqual({color: "color-mix(in srgb, red, blue)", background: "color-mix(in srgb, white, blue)"});
    });

    it("leaves an @supports that is not inside a rule untouched", () => {
      const css = "@supports (display: grid) { .a { display: grid; } }";
      expect(hoistSupported(css)).toContain("@supports (display: grid)");
    });
  });
});
