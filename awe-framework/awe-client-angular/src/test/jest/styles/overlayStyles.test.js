import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The AngularJS overlays and feedback (#885): Bootstrap modals (screen dialogs, side panels, confirm) and their mask,
// the alerts of the message zone as toasts, the popovers of the target messages, tooltips, dropdown menus, loaders and
// the loading bar, restyled with the AWE 5 tokens as the React overlays and the design system prototype (the markup
// and the test hooks do not change)
const OVERLAY_FILES = ["controllers/message.less", "directives/modal.less", "directives/overlay.less",
  "directives/popover.less", "directives/side-panel.less", "directives/context-menu.less",
  "directives/loaders/loader.less", "directives/loaders/carpet.less", "directives/loaders/circlebar.less",
  "directives/loaders/circles.less", "directives/loaders/cubes.less", "directives/loaders/dots.less",
  "directives/loaders/folding.less", "directives/loaders/square.less", "directives/loaders/squarebar.less"];

const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;
const REDUCED_MOTION = "@media (prefers-reduced-motion: reduce)";
const MESSAGE_TYPES = {success: "success", info: "info", warning: "warning", danger: "danger"};
const LOADERS = {
  ".loader .carpet-loader .sk-cube": "background-color",
  ".loader .circlebar-loader > div": "background-color",
  ".loader .circles-loader .double-bounce1": "background-color",
  ".loader .cubes-loader .cube1": "background-color",
  ".loader .dots-loader .dot1": "background-color",
  ".loader .folding-loader .sk-cube:before": "background-color",
  ".loader .square-loader": "background-color",
  ".loader .squarebar-loader > div": "background-color"
};

describe("AngularJS overlays and feedback over Bootstrap 3 (#885)", () => {
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
    it.each(OVERLAY_FILES)("%s has no colour literal, no px size and no mode or density selector", file => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
      expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
      expect(source).not.toMatch(/(?<![\w-])(white|black)(?![\w-])/);
      expect(source).not.toMatch(/\.dark\b|data-awe-density/);
      // Only hairlines are written in px: every other size comes from a token
      expect(source.match(/\b(?!1px\b)\d+(\.\d+)?px\b/g)).toBeNull();
      expect(source).not.toMatch(/var\(--awe-palette-/);
    });

    it("drops the modal and loading bar colours of the theme mixin", () => {
      const themed = selectors().filter(selector => /^\.base-theme (\.modal-content|#loading-bar)/.test(selector));
      expect(themed).toEqual([]);
    });
  });

  describe("dialogs", () => {
    it("draws the dialog as an overlay with the modal radius and shadow", () => {
      expect(style(".modal-content")).toMatchObject({
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-modal)",
        "box-shadow": "var(--awe-shadow-modal)"
      });
    });

    it("writes the header as the prototype: no band, the title in the dialog title type", () => {
      expect(style(".modal-header")).toMatchObject({
        "padding": "var(--awe-modal-padding)",
        "background": "transparent",
        "border-bottom": "0"
      });
      expect(style(".modal-title")).toMatchObject({
        "font-size": "var(--awe-font-size-dialog-title)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "line-height": "var(--awe-line-height-snug)",
        "letter-spacing": "var(--awe-letter-spacing-tight)",
        "color": "var(--awe-color-text-emphasis)"
      });
    });

    it("closes the dialog with a ghost icon button with a focus ring", () => {
      expect(style(".modal-header .close")).toMatchObject({
        "width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "color": "var(--awe-color-text-muted)",
        "text-shadow": "none",
        "opacity": "1",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".modal-header .close:hover")).toMatchObject({
        "color": "var(--awe-color-text-hover)",
        "background": "var(--awe-color-surface-hover)"
      });
      expect(style(".modal-header .close:focus-visible")).toMatchObject({"outline": "var(--awe-focus-ring)"});
    });

    it("pads the body with the modal padding and draws the footer as a band", () => {
      expect(style(".modal-body")).toMatchObject({"padding": "0 var(--awe-modal-padding) var(--awe-modal-padding)"});
      expect(style(".modal-content > div:first-child > .modal-body:first-child"))
        .toMatchObject({"padding-top": "var(--awe-modal-padding)"});
      expect(style(".modal-footer")).toMatchObject({
        "margin": "0",
        "padding": "var(--awe-space-3) var(--awe-modal-padding)",
        "background": "var(--awe-color-surface-subtle)",
        "border-top": "1px solid var(--awe-color-border)",
        "border-radius": "0 0 var(--awe-radius-modal) var(--awe-radius-modal)"
      });
    });

    it("darkens and slightly blurs the page under the mask", () => {
      expect(style(".modal-backdrop")).toMatchObject({
        "background": "var(--awe-color-mask)",
        "backdrop-filter": "var(--awe-mask-backdrop-filter)"
      });
      expect(style(".modal-backdrop.in")).toMatchObject({"opacity": "1"});
    });

    it("opens and closes the dialogs without motion when the user prefers reduced motion", () => {
      expect(style(".modal.fade .modal-dialog", REDUCED_MOTION)).toMatchObject({"transition": "none"});
      expect(style(".modal-backdrop.fade", REDUCED_MOTION)).toMatchObject({"transition": "none"});
    });

    it("keeps the side panels and the popover containers opaque under their sticky footer", () => {
      expect(style(".modal.side-panel .modal-content")).toMatchObject({
        "background-color": "var(--awe-color-overlay)",
        "box-shadow": "var(--awe-shadow-modal)",
        "border-radius": "0"
      });
      expect(style(".modal.side-panel .modal-footer")).toMatchObject({
        "background-color": "var(--awe-color-overlay)",
        "border-radius": "0"
      });
      expect(style(".awe-popover")).toMatchObject({
        "background-color": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-overlay)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style(".awe-popover .modal-footer")).toMatchObject({
        "background-color": "var(--awe-color-overlay)",
        "border-top": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "0 0 var(--awe-radius-overlay) var(--awe-radius-overlay)"
      });
    });
  });

  describe("confirm", () => {
    const CONFIRM = ".modal.modal-alert";

    it("is a narrow dialog centred on every screen", () => {
      expect(style(`${CONFIRM} .modal-dialog`)).toMatchObject({
        "width": "min(calc(var(--awe-space-16) * 7), calc(100vw - var(--awe-space-8)))",
        "margin-right": "auto",
        "margin-left": "auto"
      });
    });

    it("shows the warning in a 40px round badge next to the title and the message", () => {
      expect(style(`${CONFIRM} .modal-content`)).toMatchObject({
        "display": "grid",
        "grid-template-columns": "auto minmax(0, 1fr)",
        "column-gap": "var(--awe-space-4)"
      });
      expect(style(`${CONFIRM} .modal-content::before`)).toMatchObject({"content": "none"});
      expect(style(`${CONFIRM} .modal-header`)).toMatchObject({
        "grid-row": "1 / span 2",
        "background": "none",
        "border": "0"
      });
      expect(style(`${CONFIRM} .modal-header > i`)).toMatchObject({
        "display": "grid",
        "place-items": "center",
        "width": "var(--awe-space-10)",
        "height": "var(--awe-space-10)",
        "font-size": "var(--awe-size-icon-lg)",
        "color": "var(--awe-color-warning-text)",
        "background": "var(--awe-color-warning-subtle)",
        "border-radius": "var(--awe-radius-full)"
      });
      expect(style(`${CONFIRM} .modal-title`)).toMatchObject({
        "text-align": "start !important",
        "font-size": "var(--awe-font-size-dialog-title)",
        "color": "var(--awe-color-text-emphasis)"
      });
      expect(style(`${CONFIRM} .modal-body`)).toMatchObject({
        "text-align": "start !important",
        "color": "var(--awe-color-text-muted)"
      });
    });

    it("puts the actions at the end of the footer band", () => {
      expect(style(`${CONFIRM} .modal-footer`)).toMatchObject({
        "grid-column": "1 / -1",
        "display": "flex",
        "flex-wrap": "wrap",
        "justify-content": "flex-end",
        "gap": "var(--awe-space-2)",
        "margin": "0",
        "padding": "var(--awe-space-3) var(--awe-modal-padding)",
        "background": "var(--awe-color-surface-subtle)",
        "border-top": "1px solid var(--awe-color-border)"
      });
      expect(style(`${CONFIRM} .modal-footer .btn + .btn`)).toMatchObject({"margin-left": "0"});
    });
  });

  describe("messages", () => {
    it("lays the message zone over the page in the toast layer without catching the clicks around the toasts", () => {
      expect(style(".alert-zone")).toMatchObject({
        "z-index": "var(--awe-z-toast)",
        "pointer-events": "none"
      });
      expect(style(".alert-zone > *")).toMatchObject({"pointer-events": "auto"});
    });

    it("shows every message as a toast stacked at the top right, under the header", () => {
      expect(style(".alert.alert-awe")).toMatchObject({
        "position": "relative",
        "top": "calc(var(--awe-header-height) + var(--awe-space-3))",
        "display": "flex",
        "align-items": "flex-start",
        "gap": "var(--awe-space-3)",
        "width": "min(calc(var(--awe-space-16) * 6), calc(100vw - var(--awe-space-8)))",
        "margin": "0 var(--awe-space-4) var(--awe-space-2) auto",
        "padding": "var(--awe-space-3) var(--awe-space-3) var(--awe-space-3) var(--awe-space-4)",
        "color": "var(--awe-color-text)",
        "text-shadow": "none",
        "border": "1px solid var(--awe-color-info-border)",
        "border-radius": "var(--awe-radius-lg)",
        "box-shadow": "var(--awe-shadow-overlay)",
        "backdrop-filter": "var(--awe-toast-backdrop-filter)",
        "background": "color-mix(in srgb, var(--awe-color-info-subtle), var(--awe-color-overlay) 40%)"
      });
    });

    it.each(Object.entries(MESSAGE_TYPES))("tints a %s toast and its title with the colours of its type", (type, color) => {
      expect(style(`.alert.alert-awe.alert-${type}`)).toMatchObject({
        "background": `color-mix(in srgb, var(--awe-color-${color}-subtle), var(--awe-color-overlay) 40%)`,
        "border-color": `var(--awe-color-${color}-border)`
      });
      expect(style(`.alert.alert-awe.alert-${type} strong`)).toMatchObject({"color": `var(--awe-color-${color}-text)`});
    });

    it("writes the title in semibold and the detail under it in the small size", () => {
      expect(style(".alert.alert-awe strong")).toMatchObject({"font-weight": "var(--awe-font-weight-semibold)"});
      expect(style(".alert.alert-awe .description")).toMatchObject({
        "display": "block",
        "margin-top": "var(--awe-space-0-5)",
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text)"
      });
    });

    it("closes a toast with a ghost icon button at its end", () => {
      expect(style(".alert.alert-awe .close")).toMatchObject({
        "order": "1",
        "position": "static",
        "width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "color": "var(--awe-color-text-muted)",
        "opacity": "1",
        "text-shadow": "none",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".alert.alert-awe .close:focus-visible")).toMatchObject({"outline": "var(--awe-focus-ring)"});
    });

    it("slides the toasts in from the side, and without motion when the user prefers reduced motion", () => {
      expect(style(".alert.ng-enter")).toMatchObject({"opacity": "0", "translate": "var(--awe-space-4) 0"});
      expect(style(".alert.ng-enter.ng-enter-active")).toMatchObject({"opacity": "1", "translate": "0 0"});
      expect(style(".alert.ng-enter", REDUCED_MOTION)).toMatchObject({"transition": "none"});
    });
  });

  describe("popovers and tooltips", () => {
    it("draws the popovers as overlays", () => {
      expect(style(".popover")).toMatchObject({
        "max-width": "calc(var(--awe-space-16) * 5)",
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-overlay)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style(".popover .popover-title")).toMatchObject({
        "background": "transparent",
        "border-bottom": "0",
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)"
      });
      expect(style(".popover .popover-content")).toMatchObject({"font-size": "var(--awe-font-size-sm)"});
    });

    it("points the popover at its target with the outlined tip of the prototype", () => {
      expect(style(".popover > .arrow")).toMatchObject({
        "width": "var(--awe-space-2)",
        "height": "var(--awe-space-2)",
        "background": "var(--awe-color-overlay)",
        "border": "0",
        "rotate": "45deg"
      });
      expect(style(".popover > .arrow:after")).toMatchObject({"display": "none"});
      expect(style(".popover.bottom > .arrow")).toMatchObject({
        "border-top": "1px solid var(--awe-color-overlay-border)",
        "border-left": "1px solid var(--awe-color-overlay-border)"
      });
      expect(style(".popover.top > .arrow")).toMatchObject({
        "border-right": "1px solid var(--awe-color-overlay-border)",
        "border-bottom": "1px solid var(--awe-color-overlay-border)"
      });
    });

    it.each(Object.entries(MESSAGE_TYPES))("marks the edge and the title of a %s target message with its type", (type, color) => {
      expect(style(`.target-message.popover-${type} + .popover`)).toMatchObject({
        "border-left": `var(--awe-space-1) solid var(--awe-color-${color})`
      });
      expect(style(`.target-message.popover-${type} + .popover .popover-title`)).toMatchObject({
        "color": `var(--awe-color-${color}-text)`,
        "background": "transparent"
      });
    });

    it("writes the tooltips in the small medium type of the prototype", () => {
      expect(style(".tooltip")).toMatchObject({"z-index": "var(--awe-z-tooltip)"});
      expect(style(".tooltip.in")).toMatchObject({"opacity": "1"});
      expect(style(".tooltip-inner")).toMatchObject({
        "padding": "var(--awe-space-1) var(--awe-space-2)",
        "font-size": "var(--awe-font-size-xs)",
        "font-weight": "var(--awe-font-weight-medium)",
        "line-height": "var(--awe-line-height-snug)",
        "color": "var(--awe-color-on-tooltip)",
        "background-color": "var(--awe-color-tooltip)",
        "border-radius": "var(--awe-radius-sm)",
        "box-shadow": "var(--awe-shadow-md)"
      });
      expect(style(".tooltip.bottom .tooltip-arrow")).toMatchObject({"border-bottom-color": "var(--awe-color-tooltip)"});
      expect(style(".tooltip.top .tooltip-arrow")).toMatchObject({"border-top-color": "var(--awe-color-tooltip)"});
    });
  });

  describe("dropdown menus", () => {
    it("opens the context, tab and info menus as overlays with list options", () => {
      expect(style("ul.dropdown-menu")).toMatchObject({
        "padding": "var(--awe-list-padding)",
        "font-size": "var(--awe-font-size-base)",
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-lg)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style("ul.dropdown-menu > li > a")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "min-height": "var(--awe-size-control)",
        "margin": "0",
        "padding": "0 var(--awe-space-3)",
        "color": "var(--awe-color-list-option-text)",
        "border-radius": "var(--awe-radius-list-option)"
      });
      expect(style("ul.dropdown-menu > li > a:hover")).toMatchObject({
        "color": "var(--awe-color-list-option-text-focus)",
        "background": "var(--awe-color-list-option-focus)"
      });
      expect(style("ul.dropdown-menu > .active > a")).toMatchObject({
        "color": "var(--awe-color-list-option-text-selected)",
        "background": "var(--awe-color-list-option-selected)"
      });
      expect(style("ul.dropdown-menu .divider")).toMatchObject({
        "margin": "var(--awe-space-1) 0",
        "background-color": "var(--awe-color-border-subtle)"
      });
    });

    it("points the submenus of a context menu with a muted arrow", () => {
      expect(style(".dropdown-submenu > a:after")).toMatchObject({
        "margin": "0 0 0 auto",
        "border-left-color": "var(--awe-color-text-muted)"
      });
      expect(style(".dropdown-submenu:hover > a:after")).toMatchObject({
        "border-left-color": "var(--awe-color-list-option-text-focus)"
      });
    });
  });

  describe("loaders", () => {
    it.each(Object.entries(LOADERS))("draws %s in the primary colour", (selector, property) => {
      expect(style(selector)).toMatchObject({[property]: "var(--awe-color-primary)"});
    });

    it("sizes the loaders from the 40px loader token", () => {
      [".loader .carpet-loader", ".loader .circles-loader", ".loader .cubes-loader", ".loader .dots-loader",
        ".loader .folding-loader", ".loader .square-loader"].forEach(selector =>
        expect(style(selector)).toMatchObject({"width": "var(--awe-space-10)", "height": "var(--awe-space-10)"}));
    });

    it("stops the loaders when the user prefers reduced motion", () => {
      expect(style(".loader *", REDUCED_MOTION)).toMatchObject({"animation": "none"});
      expect(style(".loader *::before", REDUCED_MOTION)).toMatchObject({"animation": "none"});
    });

    it("draws the loading bar and its spinner in the loading bar colour", () => {
      expect(style("#loading-bar .bar")).toMatchObject({
        "height": "var(--awe-space-0-5)",
        "background": "var(--awe-color-loading-bar)"
      });
      expect(style("#loading-bar .peg")).toMatchObject({
        "box-shadow": "0 0 var(--awe-space-1-5) var(--awe-color-loading-bar)"
      });
      expect(style("#loading-bar-spinner .spinner-icon")).toMatchObject({
        "width": "var(--awe-size-icon-sm)",
        "height": "var(--awe-size-icon-sm)",
        "border-width": "var(--awe-space-0-5)",
        "border-top-color": "var(--awe-color-loading-bar)",
        "border-left-color": "var(--awe-color-loading-bar)"
      });
      expect(style("#loading-bar-spinner .spinner-icon", REDUCED_MOTION)).toMatchObject({"animation": "none"});
    });
  });
});
