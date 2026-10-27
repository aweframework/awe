import {compileStyles} from "./support/compileStyles";
import {parseRules, splitSelectors} from "./support/tokenRules";

// The AngularJS grid edition, toolbar and selection (#885), as the React grid and the design system prototype: the
// editors of the edited row share the small control height centred on the row (which keeps its height), the toolbar
// of a bordered grid keeps the inset of its footer and the selection column draws the check boxes of the prototype
const MEDIA = /^(@media\s+(?:(?:and|screen|print)\s+|\([^)]*\)\s*)+)(.*)$/;
const CELL = ".grid .ui-grid-cell";
const EDITOR_HEIGHT = "min(var(--awe-size-control-sm), calc(var(--grid-row-height, 27px) - 1px - 2 * var(--awe-space-0-5)))";
const SOME_ROWS_SELECTED = ".grid:has(.ui-grid-row [data-testid=\"grid-row-checkbox\"][data-selected=\"true\"])";

describe("AngularJS grid edition, toolbar and selection (#885)", () => {
  let rules;

  beforeAll(async () => {
    rules = parseRules(await compileStyles()).map(rule => {
      const match = rule.selector.match(MEDIA);
      return {
        media: match ? match[1].trim() : "",
        // LESS writes a relative selector of :has() with a space after the parenthesis
        selectors: splitSelectors(match ? match[2] : rule.selector).map(selector => selector.replace(/\(\s+/g, "(")),
        declarations: rule.declarations
      };
    });
  });

  // Declarations of a selector out of any media; a later rule overrides an earlier one
  const style = selector => rules
    .filter(rule => rule.media === "" && rule.selectors.includes(selector))
    .reduce((declarations, rule) => ({...declarations, ...rule.declarations}), {});

  describe("edited row", () => {
    it("centres the editor of a cell in the row while it is shown", () => {
      expect(style(`${CELL} .ui-grid-cell-contents.component > .column-input:has(> .edition[style]:not([style*="none"]))`))
        .toMatchObject({"display": "flex", "flex-direction": "column", "justify-content": "center"});
    });

    it("keeps the value of a cell centred after its row leaves the edition (shown again as a block)", () => {
      // The grid shows the value again with display: block in its style attribute: it takes the line of the cell
      // instead of the line of a field of its size, which put it at the top of the row
      expect(style(`${CELL} .ui-grid-cell-contents.component > .column-input > .visible-value`))
        .toMatchObject({"line-height": "inherit"});
    });

    it.each([
      "input.form-control",
      "textarea.form-control",
      ".input-group-addon",
      ".select2-container",
      ".select2-container .select2-choice",
      ".pixel-file-input"
    ])("gives the %s of an editor the small control height, within the row", control => {
      expect(style(`${CELL} .edition ${control}`)).toMatchObject({"height": EDITOR_HEIGHT, "min-height": "0"});
    });

    it("does not size the textarea by the size of the grid", () => {
      [".grid.grid-sm", ".grid.grid-md", ".grid.grid-lg"].forEach(size =>
        expect(style(`${size} .ui-grid-cell-contents.component .column-input textarea`)).not.toHaveProperty("height"));
    });

    it("draws the text of the inputs and the addon icons on the middle of the control", () => {
      [`${CELL} .edition input.form-control`, `${CELL} .edition .input-group-addon`].forEach(selector =>
        expect(style(selector)).toMatchObject({"padding-top": "0", "padding-bottom": "0"}));
      expect(style(`${CELL} .edition .input-group-addon`)).toMatchObject({"line-height": "1"});
      expect(style(`${CELL} .edition .select2-container .select2-choice`)).toMatchObject({
        "display": "flex",
        "align-items": "center"
      });
    });

    it("drops the margin of the group that wraps the colour editor", () => {
      // Inline block on the top of its line, so the line adds no room below the editor
      expect(style(`${CELL} .edition .form-group`)).toMatchObject({"margin": "0", "vertical-align": "top"});
    });

    it("centres the checkbox column on the row in any grid size", () => {
      expect(style(`${CELL} .ui-grid-cell-contents.component > .column-checkbox`)).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "justify-content": "center"
      });
      const checkbox = `${CELL} .ui-grid-cell-contents.component > .column-checkbox`;
      // The label and its box take the size of the box, so the box is what the column centres
      [`${checkbox} label.checkbox`, `${checkbox} .lbl`].forEach(selector => expect(style(selector)).toMatchObject({
        "top": "0",
        "margin": "0",
        "width": "var(--awe-size-checkbox)",
        "height": "var(--awe-size-checkbox)"
      }));
      expect(style(`${checkbox} input.px + .lbl:before`)).toMatchObject({
        "top": "0",
        "width": "var(--awe-size-checkbox)",
        "height": "var(--awe-size-checkbox)",
        "background": "var(--awe-color-field)",
        "border": "1px solid var(--awe-color-field-border)",
        "border-radius": "var(--awe-radius-sm)"
      });
      expect(style(`${checkbox} input.px:checked + .lbl:before`)).toMatchObject({
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      expect(style(`${checkbox} input.px + .lbl:after`).mask).toMatch(/M20 6 9 17l-5-5/);
      [".grid.grid-sm", ".grid.grid-md", ".grid.grid-lg"].forEach(size =>
        expect(style(`${size} .ui-grid-cell-contents.component .column-input label.checkbox`)).not.toHaveProperty("top"));
    });

    it("centres the button column on the row, keeping its alignment", () => {
      const column = `${CELL} .ui-grid-cell-contents.component > .column`;
      expect(style(column)).toMatchObject({
        "display": "flex",
        "align-items": "center",
        "height": "calc(var(--grid-row-height, 27px) - 1px)"
      });
      expect(style(`${column}.text-center`)).toMatchObject({"justify-content": "center"});
      expect(style(`${column}.text-right`)).toMatchObject({"justify-content": "flex-end"});
    });
  });

  describe("toolbar", () => {
    it("keeps the inset of the footer in every grid, with or without border (Maximize off the edge)", () => {
      expect(style(".grid-toolbar")).toMatchObject({
        "padding": "var(--awe-space-2) var(--awe-grid-cell-padding-x)"
      });
    });
  });

  describe("selection column", () => {
    const BOXES = [".grid .ui-grid-checkbox", ".grid .ui-grid-header-checkbox"];

    it.each(BOXES)("draws the check of %s as the Lucide check of the prototype", box => {
      const mark = style(`${box} input.px + .lbl:after`);
      expect(mark).toMatchObject({
        "content": "\"\"",
        "background": "var(--awe-color-on-primary)"
      });
      expect(mark.mask).toMatch(/^url\("data:image\/svg\+xml,.*M20 6 9 17l-5-5.*"\) center \/ var\(--awe-checkbox-icon-size\) no-repeat$/);
    });

    it("tints the cells of a selected row with the row (selected or edited), not with the colour of ui-grid", () => {
      expect(style(".grid .ui-grid-row.ui-grid-row-selected > [ui-grid-row] > .ui-grid-cell")).toMatchObject({
        "background-color": "inherit"
      });
    });

    it("shows the header box filled with a minus while some rows are selected", () => {
      const header = `${SOME_ROWS_SELECTED} .ui-grid-header-checkbox input.px:not(:checked) + .lbl`;
      expect(style(`${header}:before`)).toMatchObject({
        "background": "var(--awe-color-primary)",
        "border-color": "var(--awe-color-primary)"
      });
      const minus = style(`${header}:after`);
      expect(minus).toMatchObject({"display": "block"});
      expect(minus.mask).toMatch(/^url\("data:image\/svg\+xml,.*M5 12h14.*"\) center \/ var\(--awe-checkbox-icon-size\) no-repeat$/);
    });
  });
});
