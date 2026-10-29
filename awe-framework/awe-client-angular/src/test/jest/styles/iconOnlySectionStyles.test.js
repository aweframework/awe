import fs from "fs";
import path from "path";
import {compileStyles} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// Ghost and icon-only buttons, and section titles with an icon and a separator (#793), with the AWE 5 tokens
describe("ghost and icon-only buttons and section titles (#793)", () => {
  let rules;

  beforeAll(async () => {
    rules = parseRules(await compileStyles())
      .filter(rule => !rule.selector.startsWith("@media"))
      .map(rule => ({selectors: splitSelectors(rule.selector), declarations: rule.declarations}));
  });

  const style = selector => rules
    .filter(rule => rule.selectors.includes(selector))
    .reduce((declarations, rule) => ({...declarations, ...rule.declarations}), {});

  it("draws a ghost button without background nor border, with the muted text, lit by the pointer", () => {
    expect(style(".btn.btn-awe.btn-ghost")).toMatchObject({
      "color": "var(--awe-color-text-muted)",
      "background": "transparent",
      "border-color": "transparent",
      "box-shadow": "none"
    });
    expect(style(".btn.btn-awe.btn-ghost:hover")).toMatchObject({
      "color": "var(--awe-color-text-hover)",
      "background": "var(--awe-color-surface-hover) !important"
    });
    expect(style(".btn.btn-awe.btn-ghost:active")).toMatchObject({
      "background": "var(--awe-color-secondary-hover) !important"
    });
  });

  it("keeps the focus ring on a ghost button", () => {
    // The ring of every AWE button applies to it: the ghost rules do not set their own box-shadow on focus-visible
    expect(style(".btn.btn-awe.btn-ghost:focus-visible")).not.toHaveProperty("box-shadow");
    expect(style(".btn.btn-awe:focus-visible")).toHaveProperty("box-shadow");
  });

  it("draws an icon-only button as a square of the control size", () => {
    expect(style(".btn.btn-awe.no-label")).toMatchObject({"width": "var(--awe-size-control)", "padding": "0"});
    expect(style(".btn.btn-awe.no-label.btn-sm")).toMatchObject({"width": "var(--awe-size-control-sm)"});
    expect(style(".btn.btn-awe.no-label.btn-lg")).toMatchObject({"width": "var(--awe-size-control-lg)"});
  });

  it("draws a section title with its icon in one line and a separator above every title but the first", () => {
    expect(style(".awe-form-section-title")).toMatchObject({
      "display": "flex",
      "align-items": "center",
      "gap": "var(--awe-space-2)",
      "font-size": "var(--awe-font-size-section-title)",
      "font-weight": "var(--awe-font-weight-semibold)",
      "color": "var(--awe-color-text-emphasis)"
    });
    expect(style(".awe-form-section-title .tag-icon")).toMatchObject({"color": "var(--awe-color-text-muted)"});
    expect(style(".awe-form-section-title ~ .awe-form-section-title")).toMatchObject({
      "margin-top": "var(--awe-space-5)",
      "padding-top": "var(--awe-space-5)",
      "border-top": "1px solid var(--awe-color-border-subtle)"
    });
    expect(style(".awe-form-section-title")).not.toHaveProperty("border-top");
  });

  it("keeps the section title of the design system (#910) apart from the form section title", () => {
    expect(style(".awe-section-title")).toMatchObject({"text-transform": "uppercase"});
    expect(style(".awe-section-title")).not.toHaveProperty("border-top");
    expect(style(".awe-section-title::after")).toMatchObject({"background": "var(--awe-color-border)"});
  });

  it.each(["form.less", "section-title.less"])("uses only tokens in %s", file => {
    const source = fs.readFileSync(path.join(__dirname, "../../../main/resources/less/awe/directives", file), "utf8")
      .replace(/\/\/.*$/gm, "");
    expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
    expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
  });
});
