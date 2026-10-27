import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The files of the AngularJS shell (#885): header, main menu, menu search, breadcrumb bar and page
const SHELL_FILES = ["pages/home.less", "directives/menu.less", "directives/menu-horizontal.less",
  "directives/menu-vertical.less", "directives/menu-search.less", "breadcrumbs.less", "directives/avatar.less",
  "directives/info.less"];

const DESKTOP = "@media (min-width: 768px)";
const PHONE = "@media (max-width: 767px)";
const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;

describe("AngularJS shell over the pixel-admin markup (#885)", () => {
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

  describe("tokens only", () => {
    it.each(SHELL_FILES)("%s has no colour literal and no mode or density selector", file => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
      expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
      expect(source).not.toMatch(/\.dark\b|data-awe-density/);
    });

    it("drops the classic menu and breadcrumb colours of the theme mixin", () => {
      const themed = rules.flatMap(rule => rule.selectors)
        .filter(selector => /^\.base-theme (#main-menu|\.breadcrumb)/.test(selector));
      expect(themed).toEqual([]);
    });
  });

  describe("header", () => {
    it("is the light translucent header of the tokens, as high as the header token", () => {
      expect(style("#main-navbar")).toMatchObject({
        "min-height": "var(--awe-header-height)",
        "color": "var(--awe-color-text)",
        "background": "var(--awe-header-background)",
        "border-bottom": "1px solid var(--awe-header-border)",
        "backdrop-filter": "var(--awe-header-backdrop-filter)"
      });
    });

    it("gives the brand slot the width of the side menu", () => {
      expect(style("#main-navbar .navbar-header")).toMatchObject({"min-width": "var(--awe-menu-width)"});
      expect(style("#main-navbar .navbar-header", DESKTOP)).toMatchObject({"width": "var(--awe-menu-width)"});
    });

    it("draws the menu toggle muted, darker on hover and with an inset focus ring", () => {
      expect(style("#main-navbar #main-menu-toggle")).toMatchObject({"color": "var(--awe-color-text-muted)"});
      expect(style("#main-navbar #main-menu-toggle:hover")).toMatchObject({"color": "var(--awe-color-text-hover)"});
      expect(style("#main-navbar #main-menu-toggle:focus-visible")).toMatchObject({
        "box-shadow": "inset 0 0 0 var(--awe-focus-ring-width) var(--awe-color-focus-ring)"
      });
    });

    it("turns the info buttons into ghost icon buttons centred in the header", () => {
      expect(style("#main-navbar .right", DESKTOP)).toMatchObject({"padding-right": "var(--awe-page-padding)"});
      expect(style("#main-navbar .right-navbar-nav", DESKTOP)).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "gap": "var(--awe-space-1)"
      });
      expect(style("#main-navbar .right-navbar-nav > li > a", DESKTOP)).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "justify-content": "center",
        "height": "var(--awe-size-control)",
        "min-width": "var(--awe-size-control)",
        "margin-top": "calc((var(--awe-header-height) - var(--awe-size-control)) / 2)",
        "padding": "0 var(--awe-space-2)",
        "color": "var(--awe-color-text-muted)",
        "border-radius": "var(--awe-radius-md)"
      });
      for (const state of ["#main-navbar .right-navbar-nav > li > a:hover", "#main-navbar .right-navbar-nav > li.open > a"]) {
        expect(style(state, DESKTOP)).toMatchObject({
          "color": "var(--awe-color-text-hover)",
          "background": "var(--awe-color-surface-hover)"
        });
      }
      expect(style("#main-navbar .right-navbar-nav > li > a:focus-visible", DESKTOP)).toMatchObject({
        "outline": "var(--awe-focus-ring)",
        "outline-offset": "var(--awe-focus-ring-offset)"
      });
    });

    it("shows the user menu as one rounded button with the avatar and the name", () => {
      expect(style("#main-navbar .right-navbar-nav > li.avatar > a", DESKTOP)).toMatchObject({
        "gap": "var(--awe-space-2)",
        "padding": "0 var(--awe-space-2) 0 var(--awe-space-1)",
        "border-radius": "var(--awe-radius-full)"
      });
      expect(style(".nav li.avatar .avatar-image")).toMatchObject({
        "width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "color": "var(--awe-color-on-primary-subtle)",
        "background-color": "var(--awe-color-primary-subtle)",
        "border-radius": "var(--awe-radius-full)",
        "box-shadow": "none"
      });
      expect(style(".nav li.avatar .avatar-text")).toMatchObject({
        "font-size": "var(--awe-font-size-sm)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)"
      });
    });

    it("opens the header dropdowns as overlays with list options", () => {
      expect(style("#main-navbar .dropdown-menu")).toMatchObject({
        "padding": "var(--awe-list-padding)",
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-lg)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style("#main-navbar .dropdown-menu > li > a")).toMatchObject({
        "min-height": "var(--awe-size-control)",
        "color": "var(--awe-color-overlay-text)",
        "border-radius": "var(--awe-radius-list-option)"
      });
      expect(style("#main-navbar .dropdown-menu > li > a:hover")).toMatchObject({
        "color": "var(--awe-color-list-option-text-focus)",
        "background": "var(--awe-color-list-option-focus)"
      });
    });

    it("lists the collapsed header items on the surface on phones", () => {
      expect(style("#main-navbar .main-navbar-collapse", PHONE)).toMatchObject({
        "background": "var(--awe-color-surface)",
        "border-bottom": "1px solid var(--awe-header-border)"
      });
    });
  });

  describe("main menu", () => {
    it("draws every option as a rounded row with the menu tokens", () => {
      expect(style("#main-menu ul.navigation li > a")).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "gap": "var(--awe-nav-item-gap)",
        "min-height": "var(--awe-menu-item-height)",
        "padding": "var(--awe-nav-item-padding)",
        "font-size": "var(--awe-font-size-base)",
        "line-height": "var(--awe-line-height-snug)",
        "color": "var(--awe-menu-item-text)",
        "border-radius": "var(--awe-menu-item-radius)"
      });
      expect(style("#main-menu ul.navigation > li > a")).toMatchObject({"font-weight": "var(--awe-font-weight-medium)"});
      expect(style("#main-menu ul.navigation .menu-icon")).toMatchObject({
        "width": "var(--awe-size-icon)",
        "height": "var(--awe-size-icon)",
        "margin": "0",
        "color": "var(--awe-menu-item-icon)"
      });
    });

    it("indents the nested levels with their list, not with the left padding of the pixel-admin links", () => {
      const level = depth => `#main-menu ul.navigation ${".mm-dropdown > ul > ".repeat(depth)}li > a`;
      for (const depth of [1, 2, 3, 4, 5]) {
        expect(style(level(depth))).toMatchObject({"padding": "var(--awe-nav-item-padding)"});
      }
      expect(style("#main-menu ul.navigation .mm-dropdown > ul")).toMatchObject({
        "padding": "0 0 var(--awe-space-1) var(--awe-menu-indent)"
      });
      expect(style("#main-menu ul.navigation .mm-dropdown > ul .mm-dropdown > ul")).toMatchObject({
        "padding-left": "var(--awe-menu-indent-l3)"
      });
    });

    it("draws a guide line per nested level", () => {
      expect(style("#main-menu ul.navigation .mm-dropdown > ul > li::before")).toMatchObject({
        "position": "absolute",
        "left": "calc(var(--awe-menu-guide-l2) - var(--awe-menu-indent))",
        "width": "1px",
        "background": "var(--awe-menu-guide)"
      });
      expect(style("#main-menu ul.navigation .mm-dropdown > ul .mm-dropdown > ul > li::before")).toMatchObject({
        "left": "calc(var(--awe-menu-guide-l3) - var(--awe-menu-indent-l3))"
      });
    });

    it("mutes the nested levels and puts the chevron at the end of the row", () => {
      expect(style("#main-menu ul.navigation .mm-dropdown > ul a")).toMatchObject({
        "font-weight": "var(--awe-font-weight-normal)",
        "color": "var(--awe-menu-item-sub-text)"
      });
      expect(style("#main-menu ul.navigation .mm-dropdown > a:before")).toMatchObject({
        "position": "static",
        "order": "1",
        "margin-left": "auto",
        "color": "var(--awe-menu-item-icon)"
      });
      expect(style("#main-menu ul.navigation .mm-text")).toMatchObject({
        "flex": "1",
        "min-width": "0",
        "overflow": "hidden",
        "text-overflow": "ellipsis"
      });
    });

    it("marks hover, the current option and its trail", () => {
      expect(style("#main-menu ul.navigation li > a:hover")).toMatchObject({
        "color": "var(--awe-color-nav-item-text-active)",
        "background": "var(--awe-menu-item-hover-background)"
      });
      expect(style("#main-menu ul.navigation li.mm-dropdown:has(li.active) > a")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)"
      });
      expect(style("#main-menu ul.navigation li.active > a")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-menu-item-active-text)",
        "background": "var(--awe-menu-item-active-background)"
      });
      expect(style("#main-menu ul.navigation li.active > a .menu-icon")).toMatchObject({
        "color": "var(--awe-menu-item-active-icon)"
      });
      expect(style("#main-menu ul.navigation li > a:focus-visible")).toMatchObject({
        "box-shadow": "inset 0 0 0 var(--awe-focus-ring-width) var(--awe-color-focus-ring)"
      });
    });

    it("keeps the floating dropdowns out of the generic menu chrome", () => {
      expect(style(".menu-shadow")).toMatchObject({"box-shadow": "var(--awe-shadow-overlay)"});
      expect(style("#main-menu.menu-shadow")).toMatchObject({"box-shadow": "none"});
    });

    describe("horizontal", () => {
      it("is a bar under the header with the menu surface", () => {
        expect(style("#main-menu.horizontal")).toMatchObject({
          "background": "var(--awe-menu-background)",
          "border-bottom": "1px solid var(--awe-menu-border)"
        });
        expect(style("#main-menu.horizontal ul.navigation")).toMatchObject({
          "display": "flex",
          "flex-wrap": "wrap",
          "align-items": "center",
          "gap": "var(--awe-list-gap)",
          "padding": "var(--awe-space-1) calc(var(--awe-page-padding) + var(--awe-size-control-sm) + var(--awe-space-2)) var(--awe-space-1) var(--awe-page-padding)"
        });
        expect(style("#main-menu.horizontal ul.navigation > li > a")).toMatchObject({
          "min-height": "var(--awe-size-control-sm)",
          "padding": "0 var(--awe-space-3)"
        });
      });

      it("opens the first level as an overlay without indent or guide", () => {
        expect(style("#main-menu.horizontal ul.navigation > li > ul")).toMatchObject({
          "min-width": "var(--awe-menu-flyout-width)",
          "margin-top": "var(--awe-space-1)",
          "padding": "var(--awe-list-padding)",
          "color": "var(--awe-color-overlay-text)",
          "background": "var(--awe-color-overlay)",
          "border": "1px solid var(--awe-color-overlay-border)",
          "border-radius": "var(--awe-radius-lg)",
          "box-shadow": "var(--awe-shadow-overlay)",
          "z-index": "var(--awe-z-dropdown)"
        });
        expect(style("#main-menu.horizontal ul.navigation > li > ul > li::before")).toMatchObject({"display": "none"});
      });

      it("separates groups of options with a vertical rule", () => {
        expect(style("#main-menu.horizontal ul.navigation > li.divider")).toMatchObject({
          "height": "var(--awe-size-icon)",
          "margin": "0 var(--awe-space-1)",
          "border-left": "1px solid var(--awe-color-border)",
          "opacity": "1"
        });
      });
    });

    describe("vertical", () => {
      it("is a column with the menu surface and the width of the tokens", () => {
        expect(style("#main-menu.vertical #main-menu-bg")).toMatchObject({
          "background": "var(--awe-menu-background)",
          "border-right": "1px solid var(--awe-menu-border)"
        });
        expect(style("#main-menu.vertical ul.navigation")).toMatchObject({
          "padding": "calc(var(--awe-header-height) + var(--awe-space-2)) var(--awe-space-3) var(--awe-space-4)"
        });
        expect(style("#main-menu.vertical ul.navigation", "@media (min-width: 640px)")).toMatchObject({
          "width": "var(--awe-menu-width)"
        });
        expect(style("#main-menu", DESKTOP)).toMatchObject({"width": "var(--awe-menu-width)"});
        expect(style(".mmc #main-menu", DESKTOP)).toMatchObject({"width": "var(--awe-menu-width-collapsed)"});
        expect(style("#main-wrapper", DESKTOP)).toMatchObject({"padding-left": "var(--awe-menu-width)"});
        expect(style(".mmc #main-wrapper", DESKTOP)).toMatchObject({
          "padding-left": "var(--awe-menu-width-collapsed)"
        });
      });

      it("shows only the centred icons when collapsed and opens the levels as a flyout", () => {
        expect(style(".mmc #main-menu.vertical ul.navigation > li > a", DESKTOP)).toMatchObject({
          "justify-content": "center",
          "padding-left": "0",
          "padding-right": "0"
        });
        expect(style(".mmc #main-menu.vertical ul.navigation > li.mm-dropdown > a:before", DESKTOP))
          .toMatchObject({"display": "none"});
        expect(style("#main-menu ul.navigation .mm-dropdown > ul.mmc-dropdown-open-ul")).toMatchObject({
          "padding": "var(--awe-list-padding)",
          "color": "var(--awe-color-overlay-text)",
          "background": "var(--awe-color-overlay)",
          "border": "1px solid var(--awe-color-overlay-border)",
          "border-radius": "var(--awe-radius-lg)",
          "box-shadow": "var(--awe-shadow-overlay)"
        });
        expect(style("#main-menu .mmc-dropdown-open-ul > .mmc-title")).toMatchObject({
          "font-size": "var(--awe-font-size-xs)",
          "color": "var(--awe-menu-section-label)",
          "text-transform": "uppercase"
        });
      });

      it("keeps a footer with the AWE brand and the collapse button under the options, as the prototype", () => {
        expect(style("#main-menu.vertical #main-menu-bg")).toMatchObject({
          "display": "flex",
          "flex-direction": "column"
        });
        expect(style("#main-menu.vertical #main-menu-bg > awe-menu")).toMatchObject({
          "flex": "1 1 auto",
          "min-height": "0"
        });
        expect(style(".sidebar-footer")).toMatchObject({
          "display": "flex",
          "flex": "none",
          "border-top": "1px solid var(--awe-menu-border)"
        });
        expect(style(".sidebar-powered")).toMatchObject({
          "font-size": "var(--awe-font-size-xs)",
          "color": "var(--awe-color-text-muted)"
        });
        expect(style(".sidebar-powered strong")).toMatchObject({"color": "var(--awe-color-text)"});
        expect(style(".sidebar-powered::before").background).toMatch(/awe-logo-mark\.svg/);
        // The label names the icon button for assistive technologies, out of sight
        expect(style(".sidebar-footer .sidebar-collapse .button-text")).toMatchObject({
          "position": "absolute",
          "width": "1px",
          "height": "1px",
          "overflow": "hidden",
          "clip-path": "inset(50%)"
        });
        // Collapsed, as the prototype: no brand and no option search, only the button, centred
        expect(style(".mmc .sidebar-powered", DESKTOP)).toMatchObject({"display": "none"});
        expect(style(".mmc .sidebar-footer", DESKTOP)).toMatchObject({"justify-content": "center"});
        expect(style(".mmc #main-menu.vertical .awe-menu-search", DESKTOP)).toMatchObject({"display": "none"});
        // The icon button of the prototype: square with rounded corners
        expect(style(".sidebar-footer .sidebar-collapse")).toMatchObject({"border-radius": "var(--awe-radius-field)"});
        expect(style(".mmc .sidebar-footer .sidebar-collapse .button-icon", DESKTOP)).toMatchObject({
          "transform": "rotate(180deg)"
        });
      });

      it("declares the footer in the vertical home screen, after the menu", () => {
        const xml = fs.readFileSync(path.join(__dirname, "../../../../../awe-generic-screens/src/main/resources/application/awe/screen/home/home_vertical.xml"), "utf8");
        const footer = xml.indexOf('style="sidebar-footer"');
        expect(footer).toBeGreaterThan(xml.indexOf("<menu-container"));
        const markup = xml.slice(footer, xml.indexOf('id="content-wrapper"'));
        expect(markup).toMatch(/style="sidebar-powered"[\s\S]*label="SIDEBAR_POWERED_BY"[\s\S]*type="strong" label="SIDEBAR_POWERED_BRAND"/);
        expect(markup).toMatch(/<button label="BUTTON_TOGGLE_MENU" button-type="button" id="collapse-menu" icon="lucide:chevrons-left" style="sidebar-collapse no-class">/);
        expect(markup).toMatch(/<button-action type="toggle-menu"\s*\/>/);
      });

      it("toggles the side menu from the header with the panel-left icon of the prototype", () => {
        ["home_vertical.xml", "public_vertical.xml"].forEach(file => {
          const xml = fs.readFileSync(path.join(__dirname, "../../../../../awe-generic-screens/src/main/resources/application/awe/screen/home", file), "utf8");
          expect(xml).toMatch(/id="main-menu-toggle" icon="lucide:panel-left icon navbar-icon"/);
        });
      });

      it("emphasises the options of the first list of the flyout of the collapsed menu, as the prototype", () => {
        expect(style(".mmc #main-menu ul.navigation .mmc-dropdown-open-ul > li > a")).toMatchObject({
          "font-weight": "var(--awe-font-weight-semibold)",
          "color": "var(--awe-color-text-emphasis)"
        });
        // As the guide (levels 2 and 3 have no icon), in the side menu and in its flyout
        expect(style("#main-menu.vertical ul.navigation ul .menu-icon")).toMatchObject({"display": "none"});
      });

      it("separates groups of options with a subtle rule", () => {
        expect(style("#main-menu.vertical ul.navigation li.divider")).toMatchObject({
          "height": "0",
          "margin": "var(--awe-space-2) var(--awe-space-3)",
          "border-top": "1px solid var(--awe-color-border-subtle)",
          "opacity": "1"
        });
      });
    });
  });

  describe("menu search", () => {
    it("draws the toggle as a menu row and the panel as an overlay", () => {
      expect(style(".awe-menu-search-toggle")).toMatchObject({
        "min-height": "var(--awe-menu-item-height)",
        "padding": "var(--awe-nav-item-padding)",
        "color": "var(--awe-menu-item-icon)",
        "border-radius": "var(--awe-menu-item-radius)"
      });
      expect(style(".awe-menu-search-toggle:hover")).toMatchObject({
        "color": "var(--awe-color-text-hover)",
        "background": "var(--awe-menu-item-hover-background)"
      });
      expect(style(".awe-menu-search-panel")).toMatchObject({
        "width": "var(--awe-header-search-width)",
        "padding": "var(--awe-list-padding)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-lg)",
        "box-shadow": "var(--awe-shadow-overlay)",
        "z-index": "var(--awe-z-dropdown)"
      });
    });

    it("uses a field for the query and list options for the results", () => {
      expect(style(".awe-menu-search .awe-menu-search-input")).toMatchObject({
        "height": "var(--awe-size-control)",
        "color": "var(--awe-color-field-text)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)"
      });
      expect(style(".awe-menu-search .awe-menu-search-input:focus")).toMatchObject({
        "border-color": "var(--awe-color-field-border-focus)",
        "box-shadow": "var(--awe-shadow-field-focus)"
      });
      expect(style(".awe-menu-search-results > li.active")).toMatchObject({
        "color": "var(--awe-color-list-option-text-focus)",
        "background": "var(--awe-color-list-option-focus)"
      });
      expect(style(".awe-menu-search-breadcrumb")).toMatchObject({
        "font-size": "var(--awe-font-size-xs)",
        "color": "var(--awe-color-text-muted)"
      });
    });
  });

  describe("breadcrumb bar and page", () => {
    it("puts the breadcrumb on the page background with the page padding", () => {
      expect(style(".window-header")).toMatchObject({
        "padding": "var(--awe-space-4) var(--awe-page-padding) var(--awe-space-3)"
      });
      expect(style(".breadcrumb.breadcrumb-page")).toMatchObject({
        "display": "flex",
        "flex-wrap": "wrap",
        "align-items": "center",
        "gap": "var(--awe-space-1-5)",
        "min-height": "var(--awe-size-control)",
        "margin": "0",
        "padding": "0",
        "background": "none",
        "border": "0",
        "box-shadow": "none",
        "font-size": "var(--awe-font-size-sm)"
      });
    });

    it("shows the levels muted with a separator and the screen title emphasised", () => {
      expect(style(".breadcrumb.breadcrumb-page > li")).toMatchObject({
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text-muted)"
      });
      expect(style(".breadcrumb.breadcrumb-page > li:before")).toMatchObject({
        "position": "static",
        "color": "var(--awe-color-nav-item-icon)"
      });
      expect(style(".breadcrumb.breadcrumb-page > li.active")).toMatchObject({
        "padding-left": "0",
        "font-weight": "var(--awe-font-weight-medium)",
        "color": "var(--awe-color-text-emphasis)"
      });
      expect(style(".breadcrumb.breadcrumb-page .breadcrumb-label")).toMatchObject({
        "float": "none",
        "color": "var(--awe-color-text-muted)"
      });
    });

    it("aligns the screen buttons with the breadcrumb and the content with the page padding", () => {
      expect(style(".window-buttons", DESKTOP)).toMatchObject({
        "top": "var(--awe-space-4)",
        "right": "var(--awe-page-padding)"
      });
      // The windows are cards without side margin (#885 T27), so the content takes the page padding as it is
      expect(style(".window-center")).toMatchObject({
        "padding": "0 var(--awe-page-padding)"
      });
    });
  });
});
