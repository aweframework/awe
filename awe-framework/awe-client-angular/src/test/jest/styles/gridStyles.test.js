import fs from "fs";
import path from "path";
import {compileStyles, LESS_DIR} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The AngularJS grids (#885): the ui-grid markup restyled with the AWE 5 grid tokens, as the React grid and the design
// system prototype (rows, borders, hover, selected, sorted header, edited row, footer, paginator, empty state, loading,
// tree, row detail, toolbar, selection column and columns dialog)
const GRID_FILES = ["directives/grid.less", "directives/treegrid.less"];

const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;
const REDUCED_MOTION = "@media (prefers-reduced-motion: reduce)";
const EDITING_ROW = ".grid .ui-grid-row.ui-grid-row-selected:has(.edition[style]:not([style*=\"none\"]))";

describe("AngularJS grid over ui-grid (#885)", () => {
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
    it.each(GRID_FILES)("%s has no colour literal and no mode or density selector", file => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe", file), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
      expect(source).not.toMatch(/:\s*[^;{}]*#[0-9a-fA-F]{3,8}\b/);
      expect(source).not.toMatch(/\b(rgba?|hsla?)\(/);
      expect(source).not.toMatch(/(?<![\w-])(white|black)(?![\w-])/);
      expect(source).not.toMatch(/\.dark\b|data-awe-density/);
      expect(source).not.toMatch(/@brand-primary|@grid-border-color|@grid-header-color|@table-border-color/);
    });

    it("drops the classic grid colours of the theme mixin", () => {
      const themed = selectors().filter(selector => /^\.base-theme \.(grid|treegrid) /.test(selector));
      expect(themed).toEqual([]);
    });
  });

  describe("header", () => {
    it("draws the header with the grid header tokens", () => {
      expect(style(".grid .ui-grid-top-panel")).toMatchObject({
        "font-size": "var(--awe-grid-header-font-size)",
        "font-weight": "var(--awe-grid-header-font-weight)",
        "line-height": "var(--awe-line-height-snug)",
        "color": "var(--awe-grid-header-text)",
        "background": "var(--awe-grid-header-background)"
      });
      expect(style(".grid .ui-grid-header")).toMatchObject({"border-bottom": "1px solid var(--awe-grid-border)"});
      expect(style(".grid .ui-grid-header-cell .ui-grid-cell-contents")).toMatchObject({
        "padding": "var(--awe-space-2) var(--awe-grid-cell-padding-x)"
      });
    });

    it("marks the sorted column with emphasis text and a primary arrow, without a fill", () => {
      const sorted = ".grid .ui-grid-header-cell [aria-sort=\"ascending\"]";
      expect(style(sorted)).toMatchObject({"color": "var(--awe-color-text-emphasis)"});
      expect(style(sorted)).not.toHaveProperty("background");
      expect(style(sorted)).not.toHaveProperty("background-color");
      expect(style(".grid .ui-grid-header-cell [aria-sort=\"descending\"] .awe-icon")).toMatchObject({
        "width": "var(--awe-size-icon-sm)",
        "height": "var(--awe-size-icon-sm)",
        "color": "var(--awe-color-primary-text)"
      });
    });
  });

  describe("rows", () => {
    it("draws the rows and their borders with the grid tokens, without vertical lines between cells", () => {
      expect(style(".grid .ui-grid")).toMatchObject({"border-color": "var(--awe-grid-border)"});
      expect(style(".grid .ui-grid-row")).toMatchObject({
        "color": "var(--awe-color-text)",
        "background-color": "var(--awe-grid-row-background)",
        "border-bottom": "1px solid var(--awe-grid-border)"
      });
      expect(style(".grid .ui-grid-cell")).toMatchObject({"border-right-color": "transparent"});
      expect(style(".grid .ui-grid-header-cell")).toMatchObject({"border-right-color": "transparent"});
    });

    it("pads the cells by density and lines up the figures", () => {
      expect(style(".grid .ui-grid-cell .ui-grid-cell-contents")).toMatchObject({
        "padding": "0 var(--awe-grid-cell-padding-x)",
        "font-variant-numeric": "var(--awe-font-variant-numeric)"
      });
    });

    it("centres the line of the cells in the row height of the grid, whatever the size of the grid", () => {
      expect(style(".grid .ui-grid-cell")).toMatchObject({"line-height": "calc(var(--grid-row-height, 27px) - 1px)"});
      [".grid.grid-sm .ui-grid-cell", ".grid.grid-md .ui-grid-cell", ".grid.grid-lg .ui-grid-cell"].forEach(selector =>
        expect(style(selector)).not.toHaveProperty("line-height"));
    });

    it("centres the component cells (text view, radio, button radio) on the line of the text cells in any density", () => {
      const component = ".grid .ui-grid-cell .ui-grid-cell-contents.component";
      expect(style(`${component} > .column-input`)).toMatchObject({"height": "calc(var(--grid-row-height, 27px) - 1px)"});
      [".grid.grid-sm", ".grid.grid-md", ".grid.grid-lg"].forEach(size =>
        expect(style(`${size} .ui-grid-cell-contents.component .column-input`)).not.toHaveProperty("height"));
      // The value of a text view carries the size of a field (input-sm...): it takes the line of the cell instead
      expect(style(`${component} > .column-input > [class*="col-xs-"]`)).toMatchObject({
        "height": "auto",
        "padding-top": "0",
        "padding-bottom": "0",
        "line-height": "inherit"
      });
    });

    // #898: the dot of a checked radio was painted by a global rule of pixel-admin that #885 scoped to the criteria
    // (.form-group), so the radio column drew no dot; it is drawn like the criteria radios now
    it("draws the checked radio of a radio column like the radios of the criteria", () => {
      const radio = ".grid .ui-grid-cell .ui-grid-cell-contents.component .column-radio";
      expect(style(`${radio} input.px:checked + .lbl:before`)).toMatchObject({
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      expect(style(`${radio} input.px:checked + .lbl:after`)).toMatchObject({
        "display": "block",
        "background": "var(--awe-color-on-primary)"
      });
    });

    // #898: the buttons of AWE 5 have no vertical padding (their height is a token), so a joined bar with height: auto
    // shrank to its text; the buttons take the height of the editors of the row and the padding of the joined criteria
    it("gives the buttons of a button radio column the height of the row editors", () => {
      expect(style(".grid .ui-grid-cell .ui-grid-cell-contents.component .column-button-radio > label.btn"))
        .toMatchObject({
          "height": "min(var(--awe-size-control-sm), calc(var(--grid-row-height, 27px) - 1px - 2 * var(--awe-space-0-5)))",
          "padding": "0 var(--awe-space-3)"
        });
    });

    it("gives the row number, selection and multioperation cells a padding that fits their content", () => {
      [".grid .ui-grid-cell .ui-grid-cell-contents.ui-grid-rownumber",
        ".grid .ui-grid-cell .ui-grid-cell-contents.ui-grid-checkbox",
        ".grid .ui-grid-cell .ui-grid-cell-contents.multioperation-icon",
        ".grid .ui-grid-cell .ui-grid-cell-contents.ui-grid-row-detail",
        ".grid .ui-grid-row-header-cell .ui-grid-cell-contents"].forEach(selector =>
        expect(style(selector)).toMatchObject({"padding": "0 var(--awe-space-1)", "text-overflow": "clip"}));
    });

    it("shows the hover and the selected rows with the grid tokens", () => {
      expect(style(".grid .ui-grid-row.ui-grid-row-hover")).toMatchObject({
        "background-color": "var(--awe-grid-row-hover-background)"
      });
      expect(style(".grid .ui-grid-row.ui-grid-row-selected")).toMatchObject({
        "color": "var(--awe-grid-row-selected-text)",
        "background-color": "var(--awe-grid-row-selected-background)"
      });
    });

    it("outlines the edited row in the primary with the row marker", () => {
      expect(style(EDITING_ROW)).toMatchObject({
        "position": "relative",
        "color": "var(--awe-color-text)",
        "background-color": "var(--awe-grid-row-background)"
      });
      expect(style(`${EDITING_ROW}::after`)).toMatchObject({
        "pointer-events": "none",
        "border-top": "1px solid var(--awe-color-primary)",
        "border-bottom": "1px solid var(--awe-color-primary)",
        "box-shadow": "inset var(--awe-grid-row-marker-width) 0 0 var(--awe-color-primary)"
      });
    });

    it("tones the subtotal, total, deleted and faded rows with tokens", () => {
      expect(style(".grid .SUBTOTAL")).toMatchObject({"background-color": "var(--awe-color-surface-subtle) !important"});
      expect(style(".grid .TOTAL")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)",
        "background-color": "var(--awe-grid-header-background) !important",
        "border-top": "2px solid var(--awe-grid-border)"
      });
      expect(style(".ui-grid-row.faded")).toMatchObject({
        "color": "var(--awe-color-text-muted)",
        "background-color": "var(--awe-color-surface-subtle)"
      });
    });
  });

  describe("footer and paginator", () => {
    it("draws the totals footer like the header band", () => {
      expect(style(".grid .ui-grid-footer-panel")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)",
        "background": "var(--awe-grid-header-background)",
        "border-top": "1px solid var(--awe-grid-border)"
      });
      expect(style(".grid .table-footer")).toMatchObject({
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text-muted)",
        "border-color": "var(--awe-grid-border)"
      });
    });

    it("draws the page buttons as ghost buttons on the small control height", () => {
      expect(style(".grid .pagination > li > a")).toMatchObject({
        "min-width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "font-variant-numeric": "var(--awe-font-variant-numeric)",
        "color": "var(--awe-color-text-muted)",
        "background": "transparent",
        "border": "0",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".grid .pagination > li > a:hover")).toMatchObject({
        "color": "var(--awe-color-text-hover)",
        "background": "var(--awe-color-surface-hover)"
      });
    });

    it("shows the current page with the highlight, readable in light and dark", () => {
      expect(style(".grid .pagination > li.active > a")).toMatchObject({
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-on-highlight)",
        "background": "var(--awe-color-highlight)"
      });
    });

    it("draws the go to page and page size fields with the field tokens", () => {
      [".grid .pagination-goto", ".grid .grid-pager"].forEach(selector => expect(style(selector)).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "color": "var(--awe-color-field-text)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)"
      }));
    });
  });

  describe("states", () => {
    it("draws the empty state as the prototype", () => {
      expect(style(".grid-empty-state .grid-empty-icon")).toMatchObject({
        "color": "var(--awe-color-text-muted)",
        "background": "var(--awe-color-secondary)",
        "border-radius": "var(--awe-radius-full)",
        "box-shadow": "0 0 0 var(--awe-space-2) var(--awe-color-surface-subtle)"
      });
      expect(style(".grid-empty-state .grid-empty-title")).toMatchObject({
        "font-size": "var(--awe-font-size-md)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-text-emphasis)"
      });
      expect(style(".grid-empty-state .grid-empty-text")).toMatchObject({"color": "var(--awe-color-text-muted)"});
    });

    it("lets the clicks through the empty state of a short grid to its footer buttons, except its own action", () => {
      // The empty state lies over the rows area: in a short grid it reaches the footer (New, Delete...)
      expect(style(".grid-empty-state")).toMatchObject({
        "pointer-events": "none",
        "padding": "var(--awe-space-6) var(--awe-space-4)"
      });
      expect(style(".grid-empty-state .grid-empty-action")).toMatchObject({"pointer-events": "auto"});
    });

    it("shows a loading bar on top of a grid that reloads its rows, still with reduced motion", () => {
      expect(style(".grid.grid-reloading::before")).toMatchObject({
        "position": "absolute",
        "height": "var(--awe-space-0-5)",
        "background-color": "var(--awe-color-primary-subtle)",
        "background-image": "linear-gradient(var(--awe-color-loading-bar), var(--awe-color-loading-bar))"
      });
      expect(style(".grid.grid-reloading::before", REDUCED_MOTION)).toMatchObject({"animation": "none"});
    });

    it("pads the skeleton cells like the cells", () => {
      expect(style(".grid-skeleton .grid-skeleton-cell")).toMatchObject({
        "padding": "var(--awe-space-2) var(--awe-grid-cell-padding-x)"
      });
    });
  });

  describe("progress column", () => {
    it("has no colour literal and no white label", () => {
      const source = fs.readFileSync(path.join(LESS_DIR, "awe", "directives/progress.less"), "utf8")
        .replace(/\/\*[\s\S]*?\*\//g, "")
        .replace(/\/\/.*$/gm, "");
      expect(source).not.toMatch(/#[0-9a-fA-F]{3,8}\b|\b(rgba?|hsla?)\(|(?<![\w-])(white|black)(?![\w-])/);
      expect(source).not.toMatch(/text-shadow:(?!\s*none)/);
    });

    it("draws a thin bar on the secondary track, filled with the primary, without stripes", () => {
      expect(style(".column-progress .progress")).toMatchObject({
        "height": "var(--awe-space-2)",
        "margin": "0",
        "background-color": "var(--awe-color-secondary)",
        "border-radius": "var(--awe-radius-full)",
        "box-shadow": "none"
      });
      expect(style(".column-progress .progress.progress-striped .progress-bar")).toMatchObject({
        "background-image": "none",
        "border": "0",
        "border-radius": "var(--awe-radius-full)",
        "animation": "none"
      });
      expect(style(".column-progress .progress .progress-bar:not([class*=\"progress-bar-\"])")).toMatchObject({
        "background-color": "var(--awe-color-primary)"
      });
    });

    it("shows the value beside the bar in the text colour, readable in light and dark", () => {
      expect(style(".column-progress")).toMatchObject({"display": "flex", "align-items": "center"});
      expect(style(".column-progress .progress-text")).toMatchObject({
        "position": "static",
        "order": "1",
        "color": "var(--awe-color-text)",
        "font-variant-numeric": "var(--awe-font-variant-numeric)",
        "text-shadow": "none"
      });
    });
  });

  describe("tree, row detail and selection", () => {
    it("draws the tree expander as a small icon button", () => {
      expect(style(".grid .ui-grid-tree-base-row-header-buttons .tree-icon")).toMatchObject({
        "width": "var(--awe-space-6)",
        "height": "var(--awe-space-6)",
        "color": "var(--awe-color-text-muted)",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".grid .ui-grid-tree-base-row-header-buttons .tree-icon:focus-visible")).toMatchObject({
        "outline": "var(--awe-focus-ring)"
      });
      expect(style(".grid .ui-grid-tree-base-row-header-buttons .tree-icon:hover")).toMatchObject({
        "color": "var(--awe-color-text-hover)",
        "background": "var(--awe-color-surface-hover)"
      });
    });

    it("draws the row grip and the drop line of a grid with move-rows with the grid tokens (#888)", () => {
      expect(style(".grid .ui-grid-row-grip .grid-row-grip")).toMatchObject({
        "cursor": "move",
        "color": "var(--awe-color-text-muted)"
      });
      expect(style(".grid .ui-grid-row-grip .grid-row-grip:hover")).toMatchObject({
        "color": "var(--awe-color-primary)"
      });
      expect(style(".grid .ui-grid-row.grid-row-drop-before .ui-grid-cell")).toMatchObject({
        "box-shadow": "inset 0 var(--awe-grid-drop-indicator-width) 0 0 var(--awe-color-primary)"
      });
      expect(style(".grid .ui-grid-row.grid-row-drop-after .ui-grid-cell")).toMatchObject({
        "box-shadow": "inset 0 calc(-1 * var(--awe-grid-drop-indicator-width)) 0 0 var(--awe-color-primary)"
      });
    });

    it("marks the row detail with the primary row marker", () => {
      expect(style(".grid.grid-with-row-detail .grid-row-detail")).toMatchObject({
        "padding": "var(--awe-space-3) var(--awe-space-4)",
        "color": "var(--awe-color-text)",
        "background": "var(--awe-color-surface-subtle)",
        "border-top": "1px solid var(--awe-grid-border)",
        "border-left": "var(--awe-grid-row-marker-width) solid var(--awe-color-primary)"
      });
      expect(style(".grid.grid-with-row-detail .grid-row-detail-toggle:focus-visible")).toMatchObject({
        "outline": "var(--awe-focus-ring)"
      });
    });

    it("draws the selection checkboxes with the field tokens", () => {
      expect(style(".grid .ui-grid-checkbox input.px + .lbl:before")).toMatchObject({
        "width": "var(--awe-size-checkbox)",
        "height": "var(--awe-size-checkbox)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(".grid .ui-grid-checkbox input.px:checked + .lbl:before")).toMatchObject({
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      expect(style(".grid .ui-grid-checkbox input.px + .lbl:after")).toMatchObject({
        "color": "var(--awe-color-on-primary)"
      });
    });

    // #793: the rows of a grid with wrapping or two-line cells (grid-wrap) grow, and the single-line cell contents of
    // these grids take the line height of the row with an automatic height; the selection cell kept only the height of
    // its checkbox with that line height, so the checkbox was pushed down and cut. It fills the grown cell instead, with
    // the checkbox centred on it as in the other grids
    it("keeps the selection checkbox whole and centred in the grown rows of a grid with wrapping or two-line cells", () => {
      const wrapCell = ".grid.grid-wrap .ui-grid-row .ui-grid-cell .ui-grid-cell-contents";
      expect(style(wrapCell)).toMatchObject({"height": "auto"});
      expect(style(`${wrapCell}.ui-grid-checkbox`)).toMatchObject({"height": "100%", "line-height": "100%"});
    });
  });

  describe("search", () => {
    it("draws the search as a small field with the field tokens at the start of the toolbar", () => {
      expect(style(".grid-search")).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "margin-inline-end": "auto",
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-field-text)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)",
        "box-shadow": "var(--awe-shadow-field)"
      });
      expect(style(".grid-search:has(+ .grid-quick-filter)")).toMatchObject({"margin-inline-end": "0"});
      expect(style(".grid-search .grid-search-icon")).toMatchObject({"color": "var(--awe-color-field-icon)"});
      expect(style(".grid-search input::placeholder")).toMatchObject({"color": "var(--awe-color-field-placeholder)"});
    });

    it("marks the hovered and focused field and the focused clear button", () => {
      expect(style(".grid-search:hover")).toMatchObject({"border-color": "var(--awe-color-field-border-hover)"});
      expect(style(".grid-search:focus-within")).toMatchObject({
        "border-color": "var(--awe-color-field-border-focus)",
        "box-shadow": "var(--awe-shadow-field-focus)"
      });
      expect(style(".grid-search .grid-search-clear:hover")).toMatchObject({"background": "var(--awe-color-surface-hover)"});
      expect(style(".grid-search .grid-search-clear:focus-visible")).toMatchObject({"outline": "var(--awe-focus-ring)"});
      expect(style(".grid-search", REDUCED_MOTION)).toMatchObject({"transition": "none"});
    });
  });

  describe("cell icon (#793)", () => {
    it("draws the icon of a cell as a small icon before the text", () => {
      expect(style(".awe-cell-icon")).toMatchObject({
        "display": "inline-flex",
        "margin-inline-end": "var(--awe-space-1-5)",
        "vertical-align": "middle"
      });
      expect(style(".awe-cell-icon > .awe-icon")).toMatchObject({
        "width": "var(--awe-size-icon-sm)",
        "height": "var(--awe-size-icon-sm)"
      });
      expect(style(".awe-cell-icon-text")).toMatchObject({"white-space": "nowrap", "text-overflow": "ellipsis"});
    });

    it("draws a status with icon as a plain tag: the icon takes the place of the dot", () => {
      expect(style(".awe-cell-status-plain::before")).toMatchObject({"display": "none"});
    });

    it("draws status tags and chips as rectangles with slightly rounded corners, with a round dot", () => {
      expect(style(".awe-cell-status")).toMatchObject({"border-radius": "var(--awe-radius-sm)"});
      expect(style(".awe-cell-chip")).toMatchObject({"border-radius": "var(--awe-radius-sm)"});
      expect(style(".awe-cell-status::before")).toMatchObject({"border-radius": "var(--awe-radius-full)"});
    });
  });

  describe("cell avatar (#793)", () => {
    it("draws the avatar as a small circle of initials before the text, as the avatar of the prototype", () => {
      expect(style(".awe-cell-entity")).toMatchObject({"display": "flex", "align-items": "center", "gap": "var(--awe-space-3)"});
      expect(style(".awe-cell-avatar")).toMatchObject({
        "width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "flex": "none",
        "border-radius": "var(--awe-radius-full)",
        "font-size": "var(--awe-font-size-xs)",
        "font-weight": "var(--awe-font-weight-semibold)",
        "color": "var(--awe-color-on-secondary)",
        "background-color": "var(--awe-color-secondary)"
      });
      expect(style(".awe-cell-avatar-spacer")).toMatchObject({"width": "var(--awe-size-control-sm)", "flex": "none"});
      expect(style(".awe-cell-avatar-spacer")).not.toHaveProperty("background-color");
    });

    it.each(["info", "success", "warning", "danger"])("draws the %s tone with its text and subtle tokens", tone => {
      expect(style(`.awe-cell-avatar-${tone}`)).toMatchObject({
        "color": `var(--awe-color-${tone}-text)`,
        "background-color": `var(--awe-color-${tone}-subtle)`
      });
    });

    it("draws the primary tone on the primary subtle colour", () => {
      expect(style(".awe-cell-avatar-primary")).toMatchObject({
        "color": "var(--awe-color-on-primary-subtle)",
        "background-color": "var(--awe-color-primary-subtle)"
      });
    });
  });

  describe("cell swatch (#793)", () => {
    it("draws the swatch as a circle of the size of the avatar, filled with the colour of the row and the border token", () => {
      expect(style(".awe-cell-swatch")).toMatchObject({
        "display": "inline-block",
        "flex": "none",
        "width": "var(--awe-size-control-sm)",
        "height": "var(--awe-size-control-sm)",
        "border": "1px solid var(--awe-color-border)",
        "border-radius": "var(--awe-radius-full)",
        "background-color": "var(--awe-swatch-color)"
      });
    });
  });

  describe("selection bar", () => {
    it("draws the bar on the highlight, hidden until there are selected rows", () => {
      expect(style(".grid-selection-bar")).toMatchObject({
        "display": "none",
        "gap": "var(--awe-space-2)",
        "padding": "var(--awe-space-2) var(--awe-grid-cell-padding-x)",
        "font-size": "var(--awe-font-size-sm)",
        "font-weight": "var(--awe-font-weight-medium)",
        "color": "var(--awe-color-on-highlight)",
        "background": "var(--awe-color-highlight)",
        "border-bottom": "1px solid var(--awe-grid-border)"
      });
      expect(style(".grid-selection-bar.is-visible")).toMatchObject({
        "display": "flex",
        "animation": "awe-grid-selection-bar-in var(--awe-motion-duration-fast) var(--awe-motion-easing-enter)"
      });
      expect(style(".grid-selection-bar.is-visible", REDUCED_MOTION)).toMatchObject({"animation": "none"});
      expect(style(".grid-selection-bar .grid-selection-bar-spacer")).toMatchObject({"flex": "1"});
    });

    it("draws its buttons small, ghost, and the btn-danger one filled with the danger colour", () => {
      expect(style(".grid-selection-bar .btn.btn-awe")).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "padding": "0 var(--awe-space-3)",
        "font-size": "var(--awe-font-size-sm)"
      });
      expect(style(".grid-selection-bar .btn.btn-awe:not(.btn-danger)")).toMatchObject({
        "color": "inherit",
        "background": "transparent",
        "border-color": "transparent",
        "box-shadow": "none"
      });
      expect(style(".grid-selection-bar .btn.btn-awe:not(.btn-danger):hover")).toMatchObject({
        "background": "var(--awe-color-surface-hover) !important"
      });
      expect(style(".grid-selection-bar .btn.btn-awe:not(.btn-danger):focus-visible")["box-shadow"])
        .toContain("var(--awe-color-focus-ring)");
      // The danger variant is the one of every AWE button with btn-danger
      expect(style(".btn.btn-awe.btn-danger")).toMatchObject({"background": "var(--awe-color-danger)"});
      expect(selectors()).not.toContain(".grid-selection-bar .btn.btn-awe.btn-danger");
    });
  });

  describe("quick filters", () => {
    it("draws the quick filters as a small segmented control at the start of the toolbar", () => {
      expect(style(".grid-quick-filter")).toMatchObject({
        "margin-inline-end": "auto",
        "background": "var(--awe-segmented-track)",
        "border": "1px solid var(--awe-segmented-border)",
        "border-radius": "var(--awe-radius-md)"
      });
      expect(style(".grid-quick-filter .grid-quick-filter-segment")).toMatchObject({
        "height": "calc(var(--awe-size-control-sm) - 2 * (var(--awe-space-0-5) + 1px))",
        "font-size": "var(--awe-font-size-xs)",
        "color": "var(--awe-segmented-text)"
      });
    });

    it("marks the chosen option, the hovered one, the focused one and the disabled ones with the segmented tokens", () => {
      expect(style(".grid-quick-filter input:checked + .grid-quick-filter-segment")).toMatchObject({
        "color": "var(--awe-segmented-selected-text)",
        "background": "var(--awe-segmented-selected)",
        "border-color": "var(--awe-segmented-selected-border)",
        "box-shadow": "var(--awe-shadow-sm)"
      });
      expect(style(".grid-quick-filter .grid-quick-filter-option:hover > .grid-quick-filter-segment")).toMatchObject({
        "color": "var(--awe-segmented-hover-text)"
      });
      expect(style(".grid-quick-filter input:focus-visible + .grid-quick-filter-segment")).toMatchObject({
        "outline": "var(--awe-focus-ring)"
      });
      expect(style(".grid-quick-filter input:disabled + .grid-quick-filter-segment")).toMatchObject({
        "opacity": "var(--awe-opacity-disabled)"
      });
      expect(style(".grid-quick-filter .grid-quick-filter-segment", REDUCED_MOTION)).toMatchObject({"transition": "none"});
    });

    it("shows the count muted, or in the colour of its tone", () => {
      expect(style(".grid-quick-filter .grid-quick-filter-count")).toMatchObject({"color": "var(--awe-color-text-muted)"});
      ["danger", "warning", "success", "info"].forEach(tone => {
        expect(style(`.grid-quick-filter .grid-quick-filter-count[data-tone="${tone}"]`)).toMatchObject({
          "color": `var(--awe-color-${tone}-text)`
        });
      });
    });
  });

  describe("toolbar and columns dialog", () => {
    it("draws the toolbar buttons as small outlined buttons", () => {
      expect(style(".grid-toolbar .grid-toolbar-button")).toMatchObject({
        "height": "var(--awe-size-control-sm)",
        "padding": "0 var(--awe-space-3)",
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text)",
        "background": "var(--awe-color-surface)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-field)"
      });
      expect(style(".grid-toolbar .grid-toolbar-button:focus-visible")).toMatchObject({
        "box-shadow": "var(--awe-shadow-field-focus)"
      });
    });

    it("keeps the toolbar buttons on the tokens while hovered, focused or pressed, over the pixel-admin gradient", () => {
      // pixel-admin paints .btn-default:hover/:focus/:active with a light gradient marked !important
      // The same states as the buttons of the window (.btn.btn-awe in button.less)
      for (const state of ["hover", "focus"]) {
        expect(style(`.grid-toolbar .grid-toolbar-button:${state}`)).toMatchObject({
          "color": "var(--awe-color-text) !important",
          "background": "var(--awe-color-surface-subtle) !important",
          "background-image": "none !important",
          "border-color": "var(--awe-color-field-border-hover)"
        });
      }
      expect(style(".grid-toolbar .grid-toolbar-button:active")).toMatchObject({
        "background": "var(--awe-color-surface-hover) !important",
        "background-image": "none !important"
      });
    });

    it("draws the columns dialog as an overlay over the mask", () => {
      expect(style(".grid-columns-backdrop")).toMatchObject({
        "z-index": "var(--awe-z-modal)",
        "background": "var(--awe-color-mask)"
      });
      expect(style(".grid-columns-dialog")).toMatchObject({
        "z-index": "var(--awe-z-modal)",
        "padding": "var(--awe-modal-padding)",
        "color": "var(--awe-color-overlay-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-modal)",
        "box-shadow": "var(--awe-shadow-modal)"
      });
      expect(style(".grid-columns-dialog .grid-column-group")).toMatchObject({
        "border-left": "var(--awe-grid-row-marker-width) solid var(--awe-color-primary)"
      });
    });

    it("draws the columns popover as an overlay over the screen and its dialogs, without a mask", () => {
      expect(style(".grid-columns-popover")).toMatchObject({
        "position": "fixed",
        "z-index": "var(--awe-z-modal)",
        "width": "var(--awe-grid-columns-panel-width)",
        "max-width": "95vw",
        "color": "var(--awe-color-text)",
        "background": "var(--awe-color-overlay)",
        "border": "1px solid var(--awe-color-overlay-border)",
        "border-radius": "var(--awe-radius-overlay)",
        "box-shadow": "var(--awe-shadow-overlay)"
      });
      expect(style(".grid-columns-popover .grid-columns-list")).toMatchObject({"overflow-y": "auto"});
    });

    it("shows the counter as the subtitle of the header of the Columns", () => {
      expect(style(".grid-columns-popover .grid-columns-counter")).toMatchObject({
        "font-size": "var(--awe-font-size-sm)",
        "color": "var(--awe-color-text-muted)"
      });
      expect(style(".modal.grid-columns-side-panel .grid-columns-counter")).toMatchObject({
        "color": "var(--awe-color-text-muted)"
      });
    });

    it("mutes the label of a hidden column", () => {
      expect(style(".grid-columns-popover .grid-column-item .grid-column-label.grid-column-hidden")).toMatchObject({
        "color": "var(--awe-color-text-muted)"
      });
    });

    it("shows the arrows of a column only on the hovered or focused row, keeping them reachable with the keyboard", () => {
      const arrows = style(".grid-columns-popover .grid-column-item .grid-column-move");
      expect(arrows).toMatchObject({"opacity": "0"});
      expect(arrows).not.toHaveProperty("visibility");
      expect(arrows).not.toHaveProperty("display", "none");
      for (const state of ["hover", "focus-within"]) {
        expect(style(`.grid-columns-popover .grid-column-item:${state} .grid-column-move`)).toMatchObject({"opacity": "1"});
        expect(style(`.grid-columns-popover .grid-column-item:${state} .grid-column-move[disabled]`)).toMatchObject({
          "opacity": "var(--awe-opacity-disabled)"
        });
      }
      expect(style(".grid-columns-popover .grid-column-item .grid-column-move", REDUCED_MOTION)).toMatchObject({"transition": "none"});
    });

    it("never shrinks the columns and groups of the list, so a long list scrolls instead of overlapping them", () => {
      expect(style(".grid-columns-popover .grid-columns-list > li")).toMatchObject({"flex": "none"});
      expect(style(".grid-columns-popover .grid-column-item")).toMatchObject({"flex": "none"});
    });

    it("keeps the actions of the Columns in one line", () => {
      expect(style(".grid-columns-popover .grid-columns-actions")).toMatchObject({"flex-wrap": "nowrap"});
      expect(style(".grid-columns-popover .grid-columns-actions > .btn-awe")).toMatchObject({
        "flex": "none",
        "white-space": "nowrap"
      });
    });

    it("keeps Save on its colour while it saves (disabled, not dimmed)", () => {
      expect(style(".grid-columns-popover .grid-columns-actions > .btn-awe.is-loading[disabled]")).toMatchObject({
        "cursor": "progress",
        "opacity": "1"
      });
    });

    it("draws Restore default as a text button at the start of the actions", () => {
      expect(style(".grid-columns-popover .grid-columns-actions > .btn-awe.grid-columns-reset")).toMatchObject({
        "margin-right": "auto",
        "color": "var(--awe-color-primary-text)",
        "background": "transparent",
        "border-color": "transparent"
      });
    });

    it("draws a maximized grid as a card", () => {
      expect(style(".grid.maximized")).toMatchObject({
        "background": "var(--awe-color-surface)",
        "border": "1px solid var(--awe-color-border)",
        "border-radius": "var(--awe-radius-content)",
        "box-shadow": "var(--awe-shadow-card)"
      });
    });
  });
});
