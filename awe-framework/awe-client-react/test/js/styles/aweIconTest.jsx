import React from "react";
import {render} from "@testing-library/react";
import {getIcon, getIconCode, getIconNode, getIconSprite, ICON_SPRITE} from "../../../src/utilities";

const fs = require("fs");
const path = require("path");

const REACT = path.join(__dirname, "../../..");
const FRAMEWORK = path.join(REACT, "..");
const SHARED_ICON_STYLES = path.join(FRAMEWORK, "awe-design-tokens", "icons", "awe-icon.css");
const MAIN_CSS = path.join(REACT, "src", "main.css");
const ANGULAR_STYLES = path.join(FRAMEWORK, "awe-client-angular", "src", "main", "resources", "less", "awe.less");
const ANGULAR_ICON_STYLES = path.join(FRAMEWORK, "awe-client-angular", "src", "main", "resources", "less", "awe", "awe-icon.less");

// The markup of a node, rendered alone
const markupOf = node => {
  const {container, unmount} = render(<>{node}</>);
  const html = container.innerHTML;
  unmount();
  return html;
};

const svg = (name, classes = "") => `<svg class="awe-icon${classes ? ` ${classes}` : ""}" aria-hidden="true" focusable="false">`
  + `<use href="/icons/lucide-sprite.svg#${name}"></use></svg>`;

// The AWE 5 icons in the React client (#885): the same markup as the AngularJS client, the svg of the Lucide sprite
// inside the element that held the glyph
describe("awe-react-client/test/js/styles/aweIconTest.jsx", () => {
  let warn;

  beforeEach(() => {
    warn = jest.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    warn.mockRestore();
    document.head.querySelectorAll("base").forEach(base => base.remove());
  });

  describe("getIconCode", () => {
    it("draws a mapped icon of the screen XML with the Lucide sprite inside the icon element", () => {
      expect(markupOf(getIconCode("plus"))).toBe(`<i role="icon">${svg("plus")}</i>`);
      expect(markupOf(getIconCode("fa-plus"))).toBe(`<i role="icon">${svg("plus")}</i>`);
      expect(markupOf(getIconCode("fa:floppy-o"))).toBe(`<i role="icon">${svg("save")}</i>`);
      expect(markupOf(getIconCode("filter"))).toBe(`<i role="icon">${svg("funnel")}</i>`);
    });

    it("keeps the classes of its context on the icon element", () => {
      expect(markupOf(getIconCode("plus", "p-button-icon p-c p-button-icon-left")))
        .toBe(`<i role="icon" class="p-button-icon p-c p-button-icon-left">${svg("plus")}</i>`);
      expect(markupOf(getIconCode("check", "text-success"))).toBe(`<i role="icon" class="text-success">${svg("check")}</i>`);
    });

    it("draws a mapped Google Material Icons ligature of the mdi: family", () => {
      expect(markupOf(getIconCode("mdi:dark_mode", "p-menuitem-icon")))
        .toBe(`<i role="icon" class="p-menuitem-icon">${svg("moon")}</i>`);
    });

    it("draws a lucide: icon, its alias as its current name, and nothing for an unknown name", () => {
      expect(markupOf(getIconCode("lucide:house"))).toBe(`<i role="icon">${svg("house")}</i>`);
      expect(markupOf(getIconCode("lucide:trash-2"))).toBe(`<i role="icon">${svg("trash")}</i>`);
      expect(getIconCode("lucide:not-an-icon")).toBeNull();
      expect(warn).toHaveBeenCalledTimes(1);
    });

    it("keeps the current markup of an icon without Lucide equivalence", () => {
      expect(markupOf(getIconCode("rebel", "fa-fw"))).toBe(`<i role="icon" class="fa fa-rebel fa-fw"></i>`);
      expect(markupOf(getIconCode("fa-youtube-play"))).toBe(`<i role="icon" class="fa fa-youtube-play "></i>`);
      expect(markupOf(getIconCode("mdi:not_an_icon", "p-c"))).toBe(`<i role="icon" class="material-icons p-c">not_an_icon</i>`);
      expect(markupOf(getIconCode("not-an-icon big"))).toBe(`<i role="icon" class="fa fa-not-an-icon big "></i>`);
    });

    it("draws nothing without an icon", () => {
      expect(getIconCode("")).toBeNull();
      expect(getIconCode(undefined)).toBeNull();
      expect(getIconCode(null, "p-c")).toBeNull();
    });

    it("turns the Font Awesome modifiers into classes of the AWE icon, and drops fa-fw", () => {
      expect(markupOf(getIconCode("refresh fa-spin"))).toBe(`<i role="icon">${svg("refresh-cw", "awe-icon-spin")}</i>`);
      expect(markupOf(getIconCode("refresh", "fa-spin"))).toBe(`<i role="icon">${svg("refresh-cw", "awe-icon-spin")}</i>`);
      expect(markupOf(getIconCode("refresh fa-pulse"))).toBe(`<i role="icon">${svg("refresh-cw", "awe-icon-spin")}</i>`);
      expect(markupOf(getIconCode("plus fa-lg", "fa-fw"))).toBe(`<i role="icon">${svg("plus", "awe-icon-lg")}</i>`);
      expect(markupOf(getIconCode("circle", "fa-fw fade1 animation-dot")))
        .toBe(`<i role="icon" class="fade1 animation-dot">${svg("circle")}</i>`);
    });

    it("keeps the other classes of the icon value on the svg, as the AngularJS client does", () => {
      expect(markupOf(getIconCode("level-up icon-rotate-90", "p-c")))
        .toBe(`<i role="icon" class="p-c">${svg("corner-right-up", "icon-rotate-90")}</i>`);
    });
  });

  describe("getIconNode", () => {
    it("draws an icon written in the client inside its element, which keeps its classes and attributes", () => {
      expect(markupOf(getIconNode("pi-question-circle", {className: "help-icon", role: "note"})))
        .toBe(`<i class="help-icon" role="note">${svg("circle-question-mark")}</i>`);
      expect(markupOf(getIconNode("pi-check", {tag: "span", className: "p-row-editor-save-icon pi-fw p-clickable",
        "data-testid": "save"})))
        .toBe(`<span class="p-row-editor-save-icon p-clickable" data-testid="save">${svg("check")}</span>`);
      expect(markupOf(getIconNode("pi-spinner pi-spin"))).toBe(`<i>${svg("loader-circle", "awe-icon-spin")}</i>`);
    });

    it("keeps the current font of an icon without Lucide equivalence", () => {
      expect(markupOf(getIconNode("pi-not-an-icon", {className: "help-icon", "aria-hidden": "true"})))
        .toBe(`<i class="help-icon pi pi-not-an-icon" aria-hidden="true"></i>`);
      expect(markupOf(getIconNode("rebel"))).toBe(`<i class="fa fa-rebel"></i>`);
      expect(markupOf(getIconNode("mdi:not_an_icon"))).toBe(`<i class="material-icons">not_an_icon</i>`);
      expect(getIconNode("")).toBeNull();
    });
  });

  describe("getIcon", () => {
    it("resolves the icon value once and tells a Lucide icon from one in its current font", () => {
      expect(getIcon("refresh fa-spin text-info")).toEqual({family: "lucide", name: "refresh-cw", classes: "awe-icon-spin text-info"});
      expect(getIcon("refresh fa-spin text-info")).toBe(getIcon("refresh fa-spin text-info"));
      expect(getIcon("rebel")).toEqual({family: "fa", name: "rebel", classes: ""});
      expect(getIcon("")).toEqual({family: null, name: null});
    });
  });

  describe("sprite", () => {
    it("is read from the static files of the application, under its context path", () => {
      expect(ICON_SPRITE).toBe("/icons/lucide-sprite.svg");
      expect(getIconSprite()).toBe("/icons/lucide-sprite.svg");

      const base = document.createElement("base");
      base.setAttribute("href", `${location.origin}/awe/`);
      document.head.appendChild(base);

      expect(getIconSprite()).toBe("/awe/icons/lucide-sprite.svg");
      expect(markupOf(getIconCode("plus"))).toContain(`<use href="/awe/icons/lucide-sprite.svg#plus"></use>`);
    });

    it("is the file the build emits into the static files of the package", () => {
      const webpack = fs.readFileSync(path.join(REACT, "webpack.common.js"), "utf8");
      expect(webpack).toContain(`filename: "static${ICON_SPRITE}"`);
      expect(webpack).toContain(`{from: "src/static", to: "static/"}`);
    });
  });

  describe("styles", () => {
    // Declarations of the rule with exactly this selector
    const declarationsOf = (css, selector) => {
      const escaped = selector.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
      const match = new RegExp(`(?:^|\\})\\s*${escaped}\\s*\\{([^}]*)\\}`, "m").exec(css);
      return match ? Object.fromEntries(match[1].split(";").map(part => part.trim()).filter(Boolean)
        .map(part => [part.slice(0, part.indexOf(":")).trim(), part.slice(part.indexOf(":") + 1).trim()])) : null;
    };
    const withoutComments = css => css.replace(/\/\*[\s\S]*?\*\//g, "");
    const importedFiles = (file, pattern) => [...fs.readFileSync(file, "utf8").matchAll(pattern)]
      .map(match => path.resolve(path.dirname(file), match[1]));

    it("are one file shared by both clients", () => {
      expect(importedFiles(MAIN_CSS, /@import\s+(?:url\()?["']([^"']+)["']/g)).toContain(SHARED_ICON_STYLES);
      expect(importedFiles(ANGULAR_STYLES, /@import\s+\(inline\)\s+"([^"]+)"/g)).toContain(SHARED_ICON_STYLES);
      expect(fs.readFileSync(ANGULAR_ICON_STYLES, "utf8")).not.toMatch(/^\.awe-icon\s*\{/m);
    });

    it("draw the Lucide icons with the size tokens, the text colour and the stroke of the prototype", () => {
      const css = withoutComments(fs.readFileSync(SHARED_ICON_STYLES, "utf8"));
      expect(declarationsOf(css, ".awe-icon")).toEqual({
        "display": "inline-block",
        "width": "var(--awe-size-icon)",
        "height": "var(--awe-size-icon)",
        "flex": "none",
        "fill": "none",
        "stroke": "currentColor",
        "stroke-width": "1.75",
        "stroke-linecap": "round",
        "stroke-linejoin": "round",
        "vertical-align": "middle"
      });
      expect(declarationsOf(css, ".awe-icon-sm")).toEqual({"width": "var(--awe-size-icon-sm)", "height": "var(--awe-size-icon-sm)"});
      expect(declarationsOf(css, ".awe-icon-lg")).toEqual({"width": "var(--awe-size-icon-lg)", "height": "var(--awe-size-icon-lg)"});
      expect(css).not.toMatch(/#[0-9a-f]{3,8}\b|\b\d+(\.\d+)?(px|rem|em)\b|rgba?\(/i);
    });

    it("spin like the Font Awesome icons, except when the user prefers reduced motion", () => {
      const css = withoutComments(fs.readFileSync(SHARED_ICON_STYLES, "utf8"));
      expect(declarationsOf(css, ".awe-icon-spin")).toEqual({
        "-webkit-animation": "fa-spin 2s infinite linear",
        "animation": "fa-spin 2s infinite linear"
      });
      expect(css.indexOf("prefers-reduced-motion")).toBeGreaterThan(css.indexOf(".awe-icon-spin"));
      expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)\s*\{\s*\.awe-icon-spin\s*\{\s*animation:\s*none;?\s*\}/);
    });

    it("give the icons their context shows larger than the text the font size of that context", () => {
      const css = withoutComments(fs.readFileSync(MAIN_CSS, "utf8"));
      expect(css).toMatch(/\.grid-empty-icon > \.awe-icon,\s*\.confirm-icon > \.awe-icon,\s*\.animation-dot > \.awe-icon\s*\{\s*width: 1em;\s*height: 1em;\s*\}/);
      expect(css).toMatch(/\}\s*\.animation-dot > \.awe-icon\s*\{\s*fill: currentColor;\s*\}/);
    });
  });
});
