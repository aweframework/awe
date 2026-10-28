import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The files of the AngularJS criteria, buttons, windows and tabs (#885): Bootstrap 3 / pixel-admin markup restyled
// with the AWE 5 tokens, as the React criteria and the design system prototype
const FORM_FILES = ["directives/criterion.less", "directives/form.less", "directives/uploader.less",
  "directives/button.less", "directives/panel.less", "directives/tab.less"];

const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;
const FOCUS_RING = "0 0 0 var(--awe-focus-ring-offset) var(--awe-color-surface), "
  + "0 0 0 calc(var(--awe-focus-ring-offset) + var(--awe-focus-ring-width)) var(--awe-color-focus-ring)";

describe("AngularJS criteria, buttons, windows and tabs over Bootstrap 3 (#885)", () => {
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

  // Declarations of a selector in the given media (none by default); a later rule overrides an earlier one
  const style = (selector, media = "") => rules
    .filter(rule => rule.media === media && rule.selectors.includes(selector))
    .reduce((declarations, rule) => ({...declarations, ...rule.declarations}), {});

  const selectors = () => rules.flatMap(rule => rule.selectors);

  describe("tokens only", () => {
    it.each(FORM_FILES)("%s has no colour literal and no mode or density selector", file => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
      expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
      expect(source).not.toMatch(/(?<![\w-])(white|black)(?![\w-])/);
      expect(source).not.toMatch(/\.dark\b|data-awe-density/);
    });

    it("drops the classic button, panel, select and file input colours of the theme mixin", () => {
      const themed = selectors()
        .filter(selector => /^\.base-theme (\.btn-awe|\.btn-default|\.panel-awe|\.select2|\.pixel-file-input)/.test(selector));
      expect(themed).toEqual([]);
    });

    it("drops the tint of the required fields (the label carries the required mark)", () => {
      expect(style(".required")).not.toHaveProperty("background-color");
    });
  });

  describe("fields", () => {
    it("draws every Bootstrap field with the field tokens", () => {
      expect(style(".form-control")).toMatchObject({
        "color": "var(--awe-color-field-text)",
        "background-color": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)",
        "box-shadow": "var(--awe-shadow-field)"
      });
      expect(style(".form-control:hover")).toMatchObject({"border-color": "var(--awe-color-field-border-hover)"});
      expect(style(".form-control::placeholder")).toMatchObject({
        "color": "var(--awe-color-field-placeholder)",
        "opacity": "1"
      });
    });

    it("shows the focus ring of the fields", () => {
      expect(style(".form-control:focus")).toMatchObject({
        "outline": "0",
        "border-color": "var(--awe-color-field-border-focus)",
        "box-shadow": "var(--awe-shadow-field-focus)"
      });
    });

    it("gives the criteria the control height and padding of the density, in the three sizes", () => {
      expect(style(".form-group .form-control")).toMatchObject({
        "height": "var(--awe-size-control)",
        "padding": "0 var(--awe-field-padding-x)",
        "font-size": "var(--awe-font-size-base)"
      });
      expect(style(".form-group .form-control.input-sm")).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "font-size": "var(--awe-font-size-sm)"
      });
      expect(style(".form-group .form-control.input-lg")).toMatchObject({
        "height": "var(--awe-size-control-lg)",
        "font-size": "var(--awe-font-size-md)"
      });
      expect(style(".form-group textarea.form-control")).toMatchObject({
        "height": "auto",
        "padding": "var(--awe-field-padding-y) var(--awe-field-padding-x)"
      });
    });

    it("keeps a read-only field readable and filled instead of greyed out", () => {
      expect(style(".form-control[disabled]")).toMatchObject({
        "color": "var(--awe-color-field-text)",
        "background-color": "var(--awe-color-field-filled)",
        "border-color": "var(--awe-color-field-border)",
        "box-shadow": "none",
        "opacity": "1"
      });
    });

    it("draws the units and icons of an input group on the field background", () => {
      expect(style(".input-group-addon")).toMatchObject({
        "min-width": "var(--awe-size-control)",
        "padding": "0 var(--awe-field-padding-x)",
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text-muted)",
        "background-color": "var(--awe-color-field)",
        "border-color": "var(--awe-color-field-border)"
      });
      expect(style(".input-group:focus-within > .input-group-addon")).toMatchObject({
        "border-color": "var(--awe-color-field-border-focus)"
      });
    });

    it("shows the numbers with tabular figures", () => {
      expect(style(".criterion input[ui-numeric]")).toMatchObject({
        "font-variant-numeric": "var(--awe-font-variant-numeric)"
      });
    });

    it("draws the icon of a criterion inside the field, muted", () => {
      expect(style(".form-group.w-icon .form-icon")).toMatchObject({
        "top": "0",
        "width": "var(--awe-size-control)",
        "height": "var(--awe-size-control)",
        "color": "var(--awe-color-field-icon)",
        "border-right": "0"
      });
      expect(style(".form-group.w-icon .input > input")).toMatchObject({
        "padding-left": "var(--awe-size-control) !important"
      });
    });
  });

  describe("labels", () => {
    it("puts the label above the field with the label tokens", () => {
      expect(style(".criterion .control-label")).toMatchObject({
        "margin": "0 0 var(--awe-gap-label)",
        "font-size": "var(--awe-font-size-label)",
        "font-weight": "var(--awe-font-weight-medium)",
        "line-height": "var(--awe-line-height-snug)",
        "color": "var(--awe-color-field-label)"
      });
    });

    it("marks the label of a required field", () => {
      expect(style(".criterion .form-group:has(.required:not(.px)) > .control-label:after")).toMatchObject({
        "content": "\" *\"",
        "color": "var(--awe-color-required)"
      });
    });

    it("draws the help icon muted at the small icon size", () => {
      expect(style(".criterion .help-target")).toMatchObject({"color": "var(--awe-color-field-help)"});
      expect(style(".criterion .help-target > .awe-icon")).toMatchObject({
        "width": "var(--awe-size-icon-sm)",
        "height": "var(--awe-size-icon-sm)"
      });
    });

    // The help icon is an <i> around the Lucide icon, and every <i> has the 14px of the old icon font (font.less): with
    // the line height of the label it made the line of a label with help 2px taller than the others, so the criteria
    // floats snagged on it and the rows of the criteria grid broke (#898). The icon font set line-height: 1 on it
    it("keeps the label of a criterion with help as high as the others", () => {
      expect(style(".criterion .help-target")).toMatchObject({"line-height": "1"});
    });

    it("aligns a left label with the middle of the field", () => {
      const desktop = "@media (min-width: 768px)";
      expect(style(".criterion.label-left label", desktop)).toMatchObject({
        "padding": "0 var(--awe-space-2)",
        "color": "var(--awe-color-field-label)"
      });
      expect(style(".criterion.label-left .label-md", desktop)).toMatchObject({
        "line-height": "var(--awe-size-control)"
      });
    });

    // The width of a left label comes from the screen (left-label characters); in a narrow column it took most of
    // the width and left the field at a few pixels
    it("gives the field of a left label the rest of the column, and at most half of it to the label", () => {
      const desktop = "@media (min-width: 768px)";
      expect(style(".criterion.label-left .form-group", desktop)).toMatchObject({
        "display": "flex",
        "align-items": "flex-start"
      });
      expect(style(".criterion.label-left .form-group > label", desktop)).toMatchObject({
        "flex": "0 1 auto",
        "max-width": "50%"
      });
      expect(style(".criterion.label-left .form-group > :not(label)", desktop)).toMatchObject({
        "flex": "1 1 0",
        "min-width": "0"
      });
      // The label is no longer a table cell beside a table
      expect(style(".criterion.label-left label", desktop)).not.toHaveProperty("display", "table-cell");
    });
  });

  describe("invalid state", () => {
    it("draws the invalid field with the danger border and halo, and keeps the label colour", () => {
      expect(style(".criterion.has-error .form-control")).toMatchObject({
        "border-color": "var(--awe-color-field-border-invalid)"
      });
      expect(style(".criterion.has-error .form-control:focus")).toMatchObject({
        "box-shadow": "var(--awe-shadow-field-invalid)"
      });
      expect(style(".criterion.has-error .form-control::placeholder")).toMatchObject({
        "color": "var(--awe-color-field-placeholder-invalid)"
      });
      expect(style(".criterion.has-error .control-label")).toMatchObject({"color": "var(--awe-color-field-label)"});
      expect(style(".criterion.has-error .input-group-addon")).toMatchObject({
        "color": "var(--awe-color-text-muted)",
        "background-color": "var(--awe-color-field)",
        "border-color": "var(--awe-color-field-border-invalid)"
      });
    });

    it("shows the validation message under the field as a small danger message", () => {
      expect(style(".error-container.has-error .help-block")).toMatchObject({
        "padding": "var(--awe-space-1-5) var(--awe-space-2)",
        "font-size": "var(--awe-font-size-xs)",
        "font-weight": "var(--awe-font-weight-medium)",
        "line-height": "var(--awe-line-height-snug)",
        "color": "var(--awe-color-danger-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-danger-border)",
        "border-radius": "var(--awe-radius-md)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style(".error-container.has-error .help-block:before")).toMatchObject({
        "border-bottom-color": "var(--awe-color-danger-border)"
      });
      expect(style(".error-container.has-error .help-block:after")).toMatchObject({
        "border-bottom-color": "var(--awe-color-overlay)"
      });
    });
  });

  describe("checkbox and radio", () => {
    it("puts the control and its label on one line at the height of the fields", () => {
      expect(style(".criterion .form-group .checkbox")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "min-height": "var(--awe-size-control)",
        "margin": "0"
      });
    });

    it("draws the box with the field tokens and fills it with the primary when checked", () => {
      expect(style(".gt-ie8 .form-group input.px + .lbl:before")).toMatchObject({
        "top": "50%",
        "width": "var(--awe-size-checkbox)",
        "height": "var(--awe-size-checkbox)",
        "margin": "0 !important",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "box-shadow": "var(--awe-shadow-field)"
      });
      expect(style(".gt-ie8 .form-group input[type=\"checkbox\"].px + .lbl:before")).toMatchObject({
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".gt-ie8 .form-group input[type=\"radio\"].px + .lbl:before")).toMatchObject({
        "border-radius": "var(--awe-radius-full)"
      });
      expect(style(".gt-ie8 .form-group input.px:checked + .lbl:before")).toMatchObject({
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      expect(style(".gt-ie8 .form-group input[type=\"checkbox\"].px + .lbl:after")).toMatchObject({
        "font-size": "var(--awe-checkbox-icon-size)",
        "color": "var(--awe-color-on-primary)"
      });
      expect(style(".gt-ie8 .form-group input[type=\"radio\"].px + .lbl:after")).toMatchObject({
        "width": "var(--awe-radio-dot-size)",
        "height": "var(--awe-radio-dot-size)",
        "background": "var(--awe-color-on-primary)"
      });
    });

    it("draws the check of a checkbox with the Lucide check of the prototype instead of the icon font glyph", () => {
      const after = style(".gt-ie8 .form-group input[type=\"checkbox\"].px + .lbl:after");
      expect(after).toMatchObject({
        "content": "\"\"",
        "font-family": "inherit",
        "background": "var(--awe-color-on-primary)"
      });
      expect(after.mask).toMatch(/M20 6 9 17l-5-5/);
      // A read-only checked box keeps its check in the disabled text colour
      expect(style(".gt-ie8 .form-group input[type=\"checkbox\"].px:disabled + .lbl:after")).toMatchObject({
        "background": "var(--awe-color-disabled-text) !important",
        "opacity": "1 !important"
      });
    });

    it("shows the focus ring around the box", () => {
      expect(style(".gt-ie8 .form-group input.px:focus-visible + .lbl:before")).toMatchObject({
        "outline": "var(--awe-focus-ring)",
        "outline-offset": "var(--awe-focus-ring-offset)"
      });
    });

    it("draws an invalid box with the invalid border and keeps the fill of a checked box", () => {
      const invalid = style(".criterion.has-error .lbl:before");
      expect(invalid).toMatchObject({"border-color": "var(--awe-color-field-border-invalid) !important"});
      expect(invalid).not.toHaveProperty("background");
    });
  });

  describe("button checkbox and button radio", () => {
    it("groups the options in a segmented control", () => {
      expect(style(".btn-group:has(> .criterion.btn)")).toMatchObject({
        "display": "flex",
        "flex-wrap": "wrap",
        "gap": "var(--awe-space-0-5)",
        "padding": "var(--awe-space-0-5)",
        "background": "var(--awe-segmented-track)",
        "border": "1px solid var(--awe-segmented-border)",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".btn-group > label.criterion.btn.btn-awe.validator")).toMatchObject({
        "flex": "1 0 auto",
        "width": "auto",
        "height": "calc(var(--awe-size-control) - 2 * (var(--awe-space-0-5) + 1px))",
        "margin": "0",
        "color": "var(--awe-segmented-text)",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".btn-group > label.criterion.btn.btn-awe.validator.active")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-segmented-selected-text)",
        "background": "var(--awe-segmented-selected) !important",
        "border-color": "var(--awe-segmented-selected-border)",
        "box-shadow": "var(--awe-shadow-sm)"
      });
    });

    it("draws an invalid group with the invalid border", () => {
      expect(style(".btn-group:has(> .criterion.btn.has-error)")).toMatchObject({
        "border-color": "var(--awe-color-field-border-invalid)"
      });
    });
  });

  describe("select and suggest", () => {
    // select2 copies the classes of its input to its container: the visible field is the choice, so the container
    // drops the padding, colour and frame of a Bootstrap field and the choice is as wide as the other fields
    it("keeps the select2 container out of the field drawing", () => {
      const bare = {"padding": "0", "background": "transparent", "border": "0", "box-shadow": "none"};
      expect(style(".select2-container.form-control")).toMatchObject(bare);
      expect(style(".form-group .select2-container.form-control")).toMatchObject(bare);
      // After the field sizes of the density, which have the same specificity
      const last = selector => rules.map(rule => rule.selectors.includes(selector) && rule.media === "")
        .lastIndexOf(true);
      expect(last(".form-group .select2-container.form-control"))
        .toBeGreaterThan(last(".form-group .input-group-lg > .form-control"));
      expect(last(".form-group .select2-container.form-control"))
        .toBeGreaterThan(last(".form-group .input-group-sm > .form-control"));
    });

    it("draws the select2 field as a field of the density", () => {
      expect(style(".form-group .select2-container .select2-choice")).toMatchObject({
        "height": "var(--awe-size-control)",
        "padding": "0 var(--awe-field-padding-x)",
        "line-height": "calc(var(--awe-size-control) - 2px)",
        "color": "var(--awe-color-field-text)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)",
        "box-shadow": "var(--awe-shadow-field)"
      });
      expect(style(".form-group .select2-container-active .select2-choice")).toMatchObject({
        "border-color": "var(--awe-color-field-border-focus)",
        "box-shadow": "var(--awe-shadow-field-focus)"
      });
      expect(style(".form-group .select2-container-multi .select2-choices .select2-search-choice")).toMatchObject({
        "color": "var(--awe-color-on-secondary)",
        "background": "var(--awe-color-secondary)",
        "border": "0",
        "border-radius": "var(--awe-radius-sm)"
      });
    });

    it("opens the options in an overlay list", () => {
      expect(style(".select2-drop")).toMatchObject({
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-overlay)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style(".select2-results .select2-result-label")).toMatchObject({
        "padding": "var(--awe-list-option-padding)",
        "color": "var(--awe-color-list-option-text)",
        "border-radius": "var(--awe-radius-list-option)"
      });
      expect(style(".select2-results .select2-highlighted")).toMatchObject({
        "color": "var(--awe-color-list-option-text-focus)",
        "background": "var(--awe-color-list-option-focus)"
      });
    });

    it("keeps a read-only select readable and filled", () => {
      expect(style(".criterion .select2-container.select2-container-disabled .select2-choice")).toMatchObject({
        "color": "var(--awe-color-field-text)",
        "background": "var(--awe-color-field-filled)",
        "box-shadow": "none"
      });
    });
  });

  describe("slider, colour and uploader", () => {
    it("draws the slider flat with the primary", () => {
      expect(style(".slider .slider-track")).toMatchObject({
        "background": "var(--awe-color-border)",
        "box-shadow": "none"
      });
      expect(style(".slider .slider-selection")).toMatchObject({"background": "var(--awe-color-primary)"});
      expect(style(".slider .slider-handle")).toMatchObject({
        "background": "var(--awe-color-surface-raised)",
        "border": "2px solid var(--awe-color-primary)",
        "box-shadow": "var(--awe-shadow-sm)"
      });
    });

    it("draws the colour swatch with the small radius and the field border", () => {
      expect(style(".criterion .colorpicker-element .input-group-addon i")).toMatchObject({
        "width": "var(--awe-size-icon)",
        "height": "var(--awe-size-icon)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-sm)"
      });
    });

    it("draws the file input as a field and the drop zone with a dashed field border", () => {
      expect(style(".criterion .pixel-file-input")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "min-height": "var(--awe-size-control)",
        "padding": "0 var(--awe-field-padding-x)",
        "color": "var(--awe-color-field-text)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)",
        "box-shadow": "var(--awe-shadow-field)"
      });
      expect(style(".criterion .upload-drop-zone")).toMatchObject({
        "border": "2px dashed var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)"
      });
      expect(style(".criterion .upload-drop-zone.upload-drop-zone-active")).toMatchObject({
        "border-color": "var(--awe-color-primary)",
        "background": "var(--awe-color-highlight)"
      });
    });
  });

  describe("buttons", () => {
    it("draws the AWE buttons as the outlined buttons of the design system", () => {
      expect(style(".btn.btn-awe")).toMatchObject({
        "display": "inline-flex",
        "align-items": "center",
        "justify-content": "center",
        "gap": "var(--awe-gap-inline)",
        "height": "var(--awe-size-control)",
        "padding": "0 var(--awe-button-padding-x)",
        "font-size": "var(--awe-font-size-base)",
        "font-weight": "var(--awe-font-weight-medium)",
        "color": "var(--awe-color-text)",
        "background": "var(--awe-color-surface)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)",
        "box-shadow": "var(--awe-shadow-xs)"
      });
      expect(style(".btn.btn-awe:hover")).toMatchObject({
        "color": "var(--awe-color-text)",
        "background": "var(--awe-color-surface-subtle) !important",
        "border-color": "var(--awe-color-field-border-hover)"
      });
    });

    it("draws the primary button with the primary fill", () => {
      expect(style(".btn.btn-awe.btn-primary")).toMatchObject({
        "color": "var(--awe-color-on-primary)",
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      expect(style(".btn.btn-awe.btn-primary:hover")).toMatchObject({
        "color": "var(--awe-color-on-primary)",
        "background": "var(--awe-color-primary-hover) !important",
        "border-color": "var(--awe-color-primary-hover)"
      });
    });

    it("draws the default Bootstrap button as the secondary button", () => {
      expect(style(".btn.btn-default")).toMatchObject({
        "color": "var(--awe-color-on-secondary)",
        "background": "var(--awe-color-secondary)",
        "border": "1px solid var(--awe-color-secondary)"
      });
    });

    it("draws the danger, success and warning buttons with their fills", () => {
      expect(style(".btn.btn-awe.btn-danger")).toMatchObject({
        "color": "var(--awe-color-on-danger)",
        "background": "var(--awe-color-danger)"
      });
      expect(style(".btn.btn-awe.btn-success")).toMatchObject({
        "color": "var(--awe-color-on-success)",
        "background": "var(--awe-color-success)"
      });
      expect(style(".btn.btn-awe.btn-warning")).toMatchObject({
        "color": "var(--awe-color-on-warning)",
        "background": "var(--awe-color-warning)"
      });
    });

    it("gives the small and large buttons the control heights", () => {
      expect(style(".btn.btn-awe.btn-sm")).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "padding": "0 var(--awe-space-3)",
        "font-size": "var(--awe-font-size-sm)"
      });
      expect(style(".btn.btn-awe.btn-xs")).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "font-size": "var(--awe-font-size-xs)"
      });
      expect(style(".btn.btn-awe.btn-lg")).toMatchObject({
        "height": "var(--awe-size-control-lg)",
        "font-size": "var(--awe-font-size-md)"
      });
    });

    it("shows the focus ring and fades a disabled button", () => {
      expect(style(".btn.btn-awe:focus-visible")).toMatchObject({"box-shadow": FOCUS_RING});
      expect(style(".btn.btn-awe[disabled]")).toMatchObject({
        "opacity": "var(--awe-opacity-disabled)",
        "box-shadow": "none"
      });
    });

    it("keeps the hover background for the pointer only, as the prototype: a clicked button does not stay lit", () => {
      const buttons = {
        ".btn.btn-awe": "var(--awe-color-surface)",
        ".btn.btn-default": "var(--awe-color-secondary)",
        ".btn.btn-awe.btn-primary": "var(--awe-color-primary)",
        ".btn.btn-awe.btn-danger": "var(--awe-color-danger)",
        ".btn.btn-awe.btn-success": "var(--awe-color-success)",
        ".btn.btn-awe.btn-warning": "var(--awe-color-warning)",
        ".btn.btn-awe.btn-dark": "var(--awe-color-contrast)"
      };
      Object.entries(buttons).forEach(([button, background]) => {
        expect(style(button).background).toBe(background);
        // pixel-admin lights .btn:focus and .btn.focused with an !important background: they keep the resting one
        [`${button}:focus`, `${button}.focused`].forEach(selector => {
          expect(style(selector)).toMatchObject({"background": `${background} !important`,
            "background-image": "none !important"});
        });
        expect(style(`${button}:hover`)).toHaveProperty("background");
      });
    });

    it("shows the focus ring when AWE moves the focus to a button (focused class), as the keyboard focus", () => {
      [".btn.btn-awe", ".btn.btn-default"].forEach(button => {
        expect(style(`${button}:focus-visible`)).toMatchObject({"box-shadow": FOCUS_RING});
        expect(style(`${button}.focused`)).toMatchObject({"box-shadow": FOCUS_RING});
      });
    });

    it("keeps a launcher button a tile with its icon above its text", () => {
      expect(style(".btn.btn-awe.menu-screen-button")).toMatchObject({
        "display": "inline-block",
        "height": "120px",
        "border": "0",
        "background": "transparent !important",
        "box-shadow": "none"
      });
      expect(style(".btn.btn-awe.menu-screen-button:focus-visible")).toMatchObject({"box-shadow": FOCUS_RING});
    });

    it("keeps the icon above the text in the button bar of a phone", () => {
      expect(style(".window-buttons .btn", "@media (max-width: 767px)")).toMatchObject({
        "flex-direction": "column",
        "height": "auto",
        "min-height": "var(--awe-size-control-lg)"
      });
    });

    it("spaces the buttons of a group with the inline gap", () => {
      expect(style(".btn-group > .btn:first-child")).toMatchObject({"margin-left": "var(--awe-space-1-5)"});
    });
  });

  describe("windows", () => {
    it("draws a window as a card", () => {
      expect(style(".panel.panel-awe")).toMatchObject({
        "color": "var(--awe-color-panel-text)",
        "background": "var(--awe-color-panel)",
        "border": "1px solid var(--awe-color-panel-border)",
        "border-radius": "var(--awe-radius-card)",
        "box-shadow": "var(--awe-shadow-card)"
      });
    });

    it("gives the window a divided header with the section title", () => {
      expect(style(".panel.panel-awe > .panel-heading")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "gap": "var(--awe-space-2)",
        "min-height": "calc(var(--awe-size-control) + var(--awe-space-3))",
        "padding": "var(--awe-space-1-5) var(--awe-content-padding)",
        "font-size": "var(--awe-font-size-section-title)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)",
        "background": "transparent",
        "border-bottom": "1px solid var(--awe-color-border)"
      });
    });

    it("pads the content with the content padding of the density", () => {
      expect(style(".panel.panel-awe .panel-body")).toMatchObject({"padding": "var(--awe-content-padding)"});
    });

    it("separates the windows with the layout gap and no side margin", () => {
      for (const size of ["sm", "md", "lg"]) {
        expect(style(`.panel.panel-${size}`)).toMatchObject({"margin": "0 0 var(--awe-layout-gap)"});
      }
    });

    it("draws the maximize button as a ghost icon button", () => {
      expect(style(".maximize-button")).toMatchObject({
        "color": "var(--awe-color-text-muted)",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".maximize-button:hover")).toMatchObject({
        "color": "var(--awe-color-text-hover)",
        "background": "var(--awe-color-surface-hover)"
      });
      expect(style(".maximize-button:focus-visible")).toMatchObject({
        "outline": "var(--awe-focus-ring)"
      });
    });
  });

  describe("tabs", () => {
    it("draws the tab list underlined", () => {
      expect(style(".tab-container .nav-tabs")).toMatchObject({
        "display": "flex",
        "gap": "var(--awe-space-3)",
        "border-bottom": "1px solid var(--awe-color-border)"
      });
      expect(style(".tab-container .nav-tabs > li > a")).toMatchObject({
        "padding": "var(--awe-space-3) var(--awe-space-2)",
        "margin": "0 0 -1px",
        "font-weight": "var(--awe-font-weight-medium)",
        "color": "var(--awe-color-text-muted)",
        "background": "transparent",
        "border": "0",
        "border-bottom": "2px solid transparent",
        "border-radius": "0"
      });
      expect(style(".tab-container .nav-tabs > li > a:hover")).toMatchObject({
        "color": "var(--awe-color-text-hover)",
        "background": "transparent"
      });
    });

    // bootstrap-tabdrop moves into its menu every tab whose offsetTop is not 0: the tabs of the first row must all start at
    // the top of the list, whatever the height of the tabdrop toggle (an icon) or of the other tabs
    it("keeps every tab of the first row at the top of the list, so tabdrop only moves the wrapped ones", () => {
      expect(style(".tab-container .nav-tabs")).toMatchObject({"align-items": "stretch"});
      expect(style(".tab-container .nav-tabs > li")).toMatchObject({"display": "flex"});
      expect(style(".tab-container .nav-tabs > li")).not.toHaveProperty("margin-top");
    });

    it("draws the tabdrop toggle as a tab at the end of the list", () => {
      expect(style(".tab-container .nav-tabs > li.tabdrop")).toMatchObject({"order": "1"});
    });

    it("marks the selected tab with the primary text and underline", () => {
      expect(style(".tab-container .nav-tabs > li.active:not(.maximize-handler) > a")).toMatchObject({
        "color": "var(--awe-color-primary-text)",
        "background": "transparent",
        "border-bottom-color": "var(--awe-color-primary-text)"
      });
    });

    it("shows an invalid tab with the danger text", () => {
      expect(style(".nav-tabs > li.tab-invalid > a")).toMatchObject({"color": "var(--awe-color-danger-text)"});
    });

    it("separates the tab containers with the layout gap and no side margin", () => {
      for (const size of ["sm", "md", "lg"]) {
        expect(style(`.tab-container-${size}`)).toMatchObject({"margin": "0 0 var(--awe-layout-gap)"});
      }
    });
  });
});
