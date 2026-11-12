import React from "react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

/**
 * The definition of the column of a cell comes from a lookup built once per column model, not from a scan of the
 * column model for each cell of each row (#962)
 */
describe("awe-react-client/test/js/performance/GridColumnLookupTest.jsx", () => {
  const COLUMNS = ["name", "other", "third", "fourth"];
  const rows = Array.from({length: 10}, (_, i) => ({id: i + 1, name: `a${i}`, other: `b${i}`, third: `c${i}`, fourth: `d${i}`}));

  const render = (columnModel) => renderWithProviders(<AweGrid id="grid"/>, {
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
      components: {
        grid: {
          address: {component: "grid", view: "report"},
          model: {values: rows, records: rows.length},
          attributes: {columnModel, headerModel: [], buttonModel: [], visible: true, loadAll: true},
          specificAttributes: {sort: []}
        }
      }
    }
  });

  it("does not scan the column model for each cell", () => {
    const columnModel = COLUMNS.map(name => ({name, label: name}));
    const find = jest.spyOn(columnModel, "find");

    const {container} = render(columnModel);

    // All the cells are shown and each one has the definition of its column
    expect(container.querySelectorAll("tbody.p-datatable-tbody > tr")).toHaveLength(rows.length);
    expect(container.querySelector("tbody.p-datatable-tbody > tr").textContent).toContain("a0");
    expect(find.mock.calls.length).toBeLessThan(rows.length);
  });

  it("gives each cell the definition of its own column", () => {
    const columnModel = [
      {name: "name", label: "name"},
      {name: "amount", label: "amount", component: "numeric", numberFormat: {vMin: "-999999", mDec: 2, aSign: " EUR", pSign: "s", aPad: true}}
    ];
    const numbers = [{id: 1, name: "1234", amount: "1234"}, {id: 2, name: "56", amount: "56"}];

    const {container} = renderWithProviders(<AweGrid id="grid"/>, {
      preloadedState: {
        settings: DEFAULT_SETTINGS,
        screen: {size: {width: 100, height: 100}, breadcrumbs: [], report: {name: "opcion", option: "opcion"}},
        components: {
          grid: {
            address: {component: "grid", view: "report"},
            model: {values: numbers, records: numbers.length},
            attributes: {columnModel, headerModel: [], buttonModel: [], visible: true, loadAll: true},
            specificAttributes: {sort: []}
          }
        }
      }
    });

    // Same raw value in both columns: only the numeric one is formatted with its own number format
    const cells = [...container.querySelectorAll("tbody.p-datatable-tbody > tr")].map(row => row.textContent);
    expect(cells[0]).toContain("1234");
    expect(cells[0]).toContain("1,234.00 EUR");
    expect(cells[1]).toContain("56.00 EUR");
  });
});
