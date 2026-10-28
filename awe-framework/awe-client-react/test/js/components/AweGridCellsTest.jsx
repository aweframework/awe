import React from "react";
import { cleanup, render } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import { Columns } from "../../../src/utilities/structure";

const address = { view: "report", component: "Grd", row: "1", column: "name" };

// Inner HTML of a cell rendered alone, to compare it with the DOM of S8
const cellHtml = (node, data, row) => render(<div>{Columns({ address, ...node }, data, {}, false, row)}</div>).container.firstChild.innerHTML;

function gridState(columnModel, values) {
  return {
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: { component: "Grd", view: "report" },
        model: { values, records: values.length },
        attributes: { columnModel, headerModel: [], buttonModel: [], visible: true, max: 30 },
        specificAttributes: { sort: [] }
      }
    }
  };
}

describe("awe-react-client/test/js/components/AweGridCellsTest.jsx", () => {
  afterEach(cleanup);

  describe("secondary line", () => {
    it("shows the secondary field of the row under the main value", () => {
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" }, { name: "Ana", email: "ana@almis.com" }))
        .toBe('<div class="awe-cell-lines"><span class="awe-cell-main">Ana</span><span class="awe-cell-secondary">ana@almis.com</span></div>');
    });

    it("reads the label of a secondary cell model", () => {
      expect(cellHtml({ secondaryField: "city" }, { value: "Ana" }, { city: { value: "M", label: "Madrid" } }))
        .toContain('<span class="awe-cell-secondary">Madrid</span>');
    });

    it("shows only the main line when the secondary value is empty or missing", () => {
      const plain = cellHtml({}, { value: "Ana" });
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" }, { email: "" })).toBe(plain);
      cleanup();
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" }, {})).toBe(plain);
      cleanup();
      expect(cellHtml({ secondaryField: "email" }, { value: "Ana" })).toBe(plain);
    });

    it("combines with a status", () => {
      expect(cellHtml({ secondaryField: "since", statusField: "tone" }, { value: "Active" }, { tone: "success", since: "2024" }))
        .toBe('<div class="awe-cell-lines"><span class="awe-cell-main"><span class="awe-cell-status awe-cell-status-success" data-tone="success" title="Active">Active</span></span><span class="awe-cell-secondary">2024</span></div>');
    });
  });

  describe("status", () => {
    it.each(["success", "warning", "danger", "info", "neutral"])("shows the %s tone as a dot and the text", (tone) => {
      expect(cellHtml({ statusField: "tone" }, { value: "State" }, { tone }))
        .toBe(`<span class="awe-cell-status awe-cell-status-${tone}" data-tone="${tone}" title="State">State</span>`);
    });

    it("reads the value of a tone cell model", () => {
      expect(cellHtml({ statusField: "tone" }, { value: "State" }, { tone: { value: "danger", label: "Danger" } }))
        .toContain('data-tone="danger"');
    });

    it.each([["an unknown tone", "purple"], ["an empty tone", ""], ["a missing tone", undefined], ["a markup tone", "<b>x</b>"]])
    ("falls back to neutral with %s", (_name, tone) => {
      expect(cellHtml({ statusField: "tone" }, { value: "State" }, { tone }))
        .toBe('<span class="awe-cell-status awe-cell-status-neutral" data-tone="neutral" title="State">State</span>');
    });

    it("shows the label of the main cell model", () => {
      expect(cellHtml({ statusField: "tone" }, { value: "A", label: "Active" }, { tone: "info" })).toContain('title="Active">Active</span>');
    });
  });

  describe("chips", () => {
    it("splits the main value by commas, trims the items and drops the empty ones", () => {
      expect(cellHtml({ chips: true }, { value: " red, green ,, blue ," }))
        .toBe('<span class="awe-cell-chips"><span class="awe-cell-chip">red</span><span class="awe-cell-chip">green</span><span class="awe-cell-chip">blue</span></span>');
    });

    it("splits by the separator of the column", () => {
      expect(cellHtml({ chips: true, chipSeparator: "|" }, { value: "a, b|c" }))
        .toBe('<span class="awe-cell-chips"><span class="awe-cell-chip">a, b</span><span class="awe-cell-chip">c</span></span>');
    });

    it("collapses the chips over the maximum into a +n chip that lists the hidden ones", () => {
      expect(cellHtml({ chips: true, maxChips: 2 }, { value: "a,b,c,d" }))
        .toBe('<span class="awe-cell-chips"><span class="awe-cell-chip">a</span><span class="awe-cell-chip">b</span>'
          + '<span class="awe-cell-chip awe-cell-chip-more" title="c, d" aria-label="2 more: c, d">+2</span></span>');
    });

    it("shows every chip when they do not exceed the maximum", () => {
      expect(cellHtml({ chips: true, maxChips: 3 }, { value: "a,b,c" })).not.toContain("awe-cell-chip-more");
    });

    it("ignores a maximum that is not a positive number", () => {
      expect(cellHtml({ chips: true, maxChips: 0 }, { value: "a,b" })).not.toContain("awe-cell-chip-more");
    });

    it("ignores the status", () => {
      const html = cellHtml({ chips: true, statusField: "tone" }, { value: "a,b" }, { tone: "danger" });
      expect(html).toContain("awe-cell-chips");
      expect(html).not.toContain("awe-cell-status");
    });
  });

  describe("values are text", () => {
    const MARKUP = "<img src=x onerror=alert(1)><b>bold</b>";

    it.each([
      ["secondary", { secondaryField: "extra" }, { extra: MARKUP }, { value: MARKUP }],
      ["status", { statusField: "tone" }, { tone: "info" }, { value: MARKUP }],
      ["chips", { chips: true, maxChips: 1 }, {}, { value: `${MARKUP},x` }]
    ])("renders the markup of a %s cell as text", (_name, node, row, data) => {
      const { container } = render(<div>{Columns({ address, ...node }, data, {}, false, row)}</div>);
      expect(container.querySelector("img, b")).toBeNull();
      expect(container.textContent).toContain(MARKUP);
      expect(container.innerHTML).toContain("&lt;img src=x onerror=alert(1)&gt;");
    });

    it("gives the status its main value as a text title", () => {
      const { container } = render(<div>{Columns({ address, statusField: "tone" }, { value: MARKUP }, {}, false, { tone: "info" })}</div>);
      expect(container.querySelector(".awe-cell-status").getAttribute("title")).toBe(MARKUP);
    });
  });

  describe("columns without the attributes or with a component", () => {
    it("renders a column without the attributes as before", () => {
      expect(cellHtml({}, { value: "Ana" }, { email: "x" }))
        .toBe('<span class="p-cell-text white-space-nowrap p-text-truncate" title="Ana">Ana</span>');
    });

    it("ignores the three features in a column with a component", () => {
      const node = { component: "text", secondaryField: "email", statusField: "tone", chips: true };
      const { container } = render(<div>{Columns({ address, ...node }, { value: "a,b" }, {}, false, { email: "x", tone: "danger" })}</div>);
      expect(container.querySelector(".awe-cell-lines, .awe-cell-status, .awe-cell-chips")).toBeNull();
    });
  });

  describe("grid", () => {
    it("reads the secondary and the status from the row of the grid", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(
          [{ name: "name", label: "Name", secondaryField: "email", statusField: "tone", sortable: true }],
          [{ id: "r1", name: "Ana", email: "ana@almis.com", tone: "warning" }])
      });

      const cell = container.querySelector("td.name");
      expect(cell.querySelector(".awe-cell-main .awe-cell-status-warning")).toHaveTextContent("Ana");
      expect(cell.querySelector(".awe-cell-secondary")).toHaveTextContent("ana@almis.com");
    });

    it("shows chips in a grid cell", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState([{ name: "tags", label: "Tags", chips: true, maxChips: 1 }], [{ id: "r1", tags: "a,b,c" }])
      });

      expect(container.querySelectorAll("td.tags .awe-cell-chip")).toHaveLength(2);
      expect(container.querySelector("td.tags .awe-cell-chip-more")).toHaveAttribute("aria-label", "2 more: b, c");
    });
  });
});
