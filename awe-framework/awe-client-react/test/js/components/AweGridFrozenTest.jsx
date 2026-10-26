import React from "react";
import { cleanup } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import { arrangeFrozenColumns } from "../../../src/utilities/grid";

const LONG = "A long description that does not fit in the width of the column and needs several lines";

function gridState(columnModel, values, attributes = {}) {
  return {
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: { component: "Grd", view: "report" },
        model: { values, records: values.length },
        attributes: { columnModel, headerModel: [], buttonModel: [], visible: true, max: 30, ...attributes },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const frozenCells = container => [...container.querySelectorAll("td.p-frozen-column")];
const headers = container => [...container.querySelectorAll("thead th")];
const columns = [
  { name: "a", label: "A", charlength: 10 },
  { name: "b", label: "B", charlength: 10, frozen: true },
  { name: "c", label: "C", charlength: 10 }
];
const values = [{ id: "r1", a: "A1", b: "B1", c: "C1" }];

describe("awe-react-client/test/js/components/AweGridFrozenTest.jsx", () => {
  afterEach(cleanup);

  describe("arrangeFrozenColumns", () => {
    it("moves the frozen columns to the left keeping the relative order", () => {
      const result = arrangeFrozenColumns([{ name: "a" }, { name: "b", frozen: true }, { name: "c" }, { name: "d", frozen: true }], []);

      expect(result.columns.map(c => c.name)).toEqual(["b", "d", "a", "c"]);
      expect(result.hasFrozen).toBe(true);
    });

    it("leaves the columns as they are without frozen columns", () => {
      const list = [{ name: "a" }, { name: "b" }];
      const result = arrangeFrozenColumns(list, []);

      expect(result.columns).toEqual(list);
      expect(result.hasFrozen).toBe(false);
    });

    it("ignores frozen when the grid has grouped headers", () => {
      const list = [{ name: "a" }, { name: "b", frozen: true }];
      const result = arrangeFrozenColumns(list, [{ startColumnName: "a", numberOfColumns: 2 }]);

      expect(result.columns.map(c => c.name)).toEqual(["a", "b"]);
      expect(result.hasFrozen).toBe(false);
    });
  });

  describe("grid", () => {
    it("pins a frozen column (header and cell) and moves it to the left", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState(columns, values) });

      expect(frozenCells(container)).toHaveLength(1);
      expect(frozenCells(container)[0]).toHaveClass("b");
      expect(headers(container).filter(th => th.classList.contains("p-frozen-column"))).toHaveLength(1);
      const order = [...container.querySelectorAll("tbody tr td")].map(td => td.textContent);
      expect(order.slice(0, 3)).toEqual(["B1", "A1", "C1"]);
    });

    it("pins nothing when no column is frozen", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(columns.map(c => ({ ...c, frozen: false })), values)
      });

      expect(container.querySelectorAll(".p-frozen-column")).toHaveLength(0);
    });

    it("pins the row number and the checkbox together with the frozen columns", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(columns, values, { rowNumbers: true, multiselect: true })
      });

      expect(frozenCells(container)).toHaveLength(3);
      expect(container.querySelector("td.p-row-number-cell")).toHaveClass("p-frozen-column");
    });

    it("pins the row detail toggle too, as the first of the frozen columns on the left", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(columns, values, { rowDetail: { template: "{a}" }, multiselect: true })
      });

      const firstRowCells = [...container.querySelector("tbody tr").querySelectorAll("td")];
      expect(frozenCells(container)).toHaveLength(3);
      expect(firstRowCells[0]).toHaveClass("p-frozen-column");
    });

    it("keeps the row number and the checkbox free when no column is frozen", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState([{ name: "a", label: "A" }], values, { rowNumbers: true, multiselect: true })
      });

      expect(container.querySelectorAll(".p-frozen-column")).toHaveLength(0);
    });

    it("pins the footer of a frozen column", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(columns, values, { showTotals: true })
      });

      expect(container.querySelectorAll("tfoot .p-frozen-column")).toHaveLength(1);
    });

    it("does not wrap the cells of a grid with frozen columns", () => {
      const wrapping = [{ name: "a", label: "A", wrap: true, charlength: 10 }, { name: "b", label: "B", frozen: true, charlength: 10 }];
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(wrapping, [{ id: "r1", a: LONG, b: "B1" }])
      });

      expect(container.querySelector("td.a")).not.toHaveClass("p-cell-wrapping");
      expect(container.querySelector(".p-cell-wrap")).toBeNull();
    });

    it("keeps wrapping when no column is frozen", () => {
      const wrapping = [{ name: "a", label: "A", wrap: true, charlength: 10 }];
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState(wrapping, [{ id: "r1", a: LONG }])
      });

      expect(container.querySelector("td.a")).toHaveClass("p-cell-wrapping");
    });
  });
});
