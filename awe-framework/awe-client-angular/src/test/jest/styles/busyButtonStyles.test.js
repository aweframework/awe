import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// Busy state of the button that launched actions and of the save button of the grid columns (#771): the login loading
// look, the spinner instead of the icon, disabled but not faded
const FILES = ["directives/button.less", "directives/grid.less"];
const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;

describe("busy buttons (#771)", () => {
  let rules;

  beforeAll(async () => {
    rules = parseRules(await compileStyles()).map(rule => {
      const match = rule.selector.match(MEDIA);
      return {
        media: match ? match[1].trim() : "",
        selectors: splitSelectors(match ? match[2] : rule.selector),
        declarations: rule.declarations
      };
    });
  });

  const style = selector => rules
    .filter(rule => rule.media === "" && rule.selectors.includes(selector))
    .reduce((declarations, rule) => ({...declarations, ...rule.declarations}), {});

  // Last rule of the compiled CSS that sets the property for one of the selectors, with the specificity of the selector
  const winner = (selectorList, property) => {
    const specificity = selector => {
      const ids = (selector.match(/#[\w-]+/g) || []).length;
      const classes = (selector.match(/\.[\w-]+|\[[^\]]+\]|:(?!not\()[\w-]+/g) || []).length;
      const types = (selector.replace(/:not\(([^)]*)\)/g, " $1").match(/(^|[\s>+~])[a-z][\w-]*/gi) || []).length;
      return ids * 10000 + classes * 100 + types;
    };
    let best = null;
    rules.filter(rule => rule.media === "").forEach((rule, index) => {
      if (!(property in rule.declarations)) {
        return;
      }
      rule.selectors.filter(selector => selectorList.includes(selector)).forEach(selector => {
        const weight = specificity(selector);
        if (!best || weight > best.weight || (weight === best.weight && index > best.index)) {
          best = {weight, index, value: rule.declarations[property]};
        }
      });
    });
    return best && best.value;
  };

  it.each(FILES)("%s has no colour literal and no mode or density selector", file => {
    const source = fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8")
      .replace(/\/\*[\s\S]*?\*\//g, "")
      .replace(/\/\/.*$/gm, "");
    expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
    expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
    expect(source).not.toMatch(/\.dark\b|data-awe-density/);
  });

  it("keeps a busy screen button opaque over the disabled look", () => {
    const busy = [".btn.btn-awe[disabled]", ".btn[disabled]", ".btn-awe.is-loading[disabled]"];
    expect(winner(busy, "opacity")).toBe("1");
    expect(winner(busy, "cursor")).toBe("progress");
  });

  it("keeps the busy save button of the grid columns opaque over the disabled grid button", () => {
    const dialog = [".grid-columns-dialog .grid-columns-actions > .btn-awe[disabled]",
      ".grid-columns-dialog .grid-columns-actions > .btn-awe.is-loading[disabled]"];
    const sidePanel = [".modal.grid-columns-side-panel .grid-columns-actions > .btn-awe[disabled]",
      ".modal.grid-columns-side-panel .grid-columns-actions > .btn-awe.is-loading[disabled]"];
    expect(winner(dialog, "opacity")).toBe("1");
    expect(winner(sidePanel, "opacity")).toBe("1");
  });

  it("shows the spinner instead of the icon of the button", () => {
    expect(style(".btn-awe.is-loading > .button-icon:not(.button-busy)")).toMatchObject({display: "none"});
    expect(style(".btn-awe .button-busy")).toMatchObject({display: "inline-flex"});
  });
});
