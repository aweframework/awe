import React from "react";
import { cleanup } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import AweTreeGrid from "../../../src/components/AweTreeGrid";
import ColumnRowEditor from "../../../src/columns/ColumnRowEditor";

const hook = (testId) => `[data-testid='${testId}']`;

function gridState(attributes = {}, values = [], tree = false) {
  return {
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: { component: "Grd", view: "report" },
        model: { values },
        attributes: {
          columnModel: [{ name: "name", label: "Name" }, { name: "age", label: "Age" }],
          headerModel: [],
          buttonModel: [],
          visible: true,
          max: 30,
          ...(tree ? { treegrid: true } : {}),
          ...attributes
        },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const rows = [
  { id: "r1", name: "Ann", age: 30, selected: true },
  { id: "r2", name: "Bob", age: 41 }
];

describe("awe-react-client/test/js/components/TestIdsGridTest.jsx", () => {
  // Known warnings PrimeReact raises for the Row/ColumnGroup of the header (they exist without these tests too)
  const KNOWN_WARNINGS = [
    /does not recognize the[\s\S]*__TYPE/, /does not recognize the[\s\S]*ptOptions/, /non-boolean attribute[\s\S]*unstyled/
  ];
  beforeEach(() => {
    jest.spyOn(console, "error").mockImplementation((...args) => {
      if (!KNOWN_WARNINGS.some(known => known.test(String(args.join(" "))))) {
        process.stderr.write(`${args.join(" ")}\n`);
      }
    });
  });
  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
  });

  describe("grid", () => {
    it("exposes the grid, its viewport and its header cells", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({}, rows) });

      const grid = container.querySelector("[grid-id='Grd']");
      expect(grid.getAttribute("data-testid")).toBe("grid");
      expect(grid.id).toBe("Grd");
      expect(grid.querySelector(hook("grid-viewport")).getAttribute("data-container")).toBe("body");
      const headers = Array.from(grid.querySelectorAll(hook("grid-header-cell")));
      expect(headers.map(h => h.getAttribute("column-id"))).toEqual(["name", "age"]);
      expect(headers.map(h => h.textContent)).toEqual(["Name", "Age"]);
    });

    it("exposes the rows with their id and selection", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({}, rows) });

      const trs = Array.from(container.querySelectorAll(`[grid-id='Grd'] ${hook("grid-row")}`));
      expect(trs.map(tr => tr.getAttribute("row-id"))).toEqual(["r1", "r2"]);
      expect(trs.map(tr => tr.getAttribute("data-selected"))).toEqual(["true", "false"]);
      expect(trs[0].tagName).toBe("TR");
    });

    it("exposes which rows are being edited apart from which are selected", () => {
      // A row can be edited without being selected (a multiselect grid toggles the selection on a double click)
      const editing = [
        { id: "r1", name: "Ann", age: 30, selected: true },
        { id: "r2", name: "Bob", age: 41, $row: { editing: true } }
      ];
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({}, editing) });

      const trs = Array.from(container.querySelectorAll(`[grid-id='Grd'] ${hook("grid-row")}`));
      expect(trs.map(tr => tr.getAttribute("data-editing"))).toEqual(["false", "true"]);
      expect(trs.map(tr => tr.getAttribute("data-selected"))).toEqual(["true", "false"]);
    });

    it("exposes which rows are marked to be deleted", () => {
      const deleting = [
        { id: "r1", name: "Ann", age: 30, $row: { operation: "UPDATE" } },
        { id: "r2", name: "Bob", age: 41, $row: { operation: "DELETE" } },
        { id: "r3", name: "Eve", age: 25 }
      ];
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState({ multioperation: true }, deleting)
      });

      const trs = Array.from(container.querySelectorAll(`[grid-id='Grd'] ${hook("grid-row")}`));
      expect(trs.map(tr => tr.getAttribute("data-deleted"))).toEqual(["false", "true", "false"]);
    });

    it("exposes the cells with their column and row", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({}, rows) });

      const cell = container.querySelector(`${hook("grid-row")}[row-id='r2'] ${hook("grid-cell")}[column-id='name']`);
      expect(cell).not.toBeNull();
      expect(cell.getAttribute("row-id")).toBe("r2");
      expect(cell.textContent).toBe("Bob");
    });

    it("keeps the rows of the current page when the grid pages on the client", () => {
      const many = Array.from({ length: 6 }, (_, i) => ({ id: `r${i}`, name: `N${i}`, age: i }));
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState({ loadAll: true, max: 2 }, many)
      });

      const trs = Array.from(container.querySelectorAll(hook("grid-row")));
      expect(trs.map(tr => tr.getAttribute("row-id"))).toEqual(["r0", "r1"]);
    });

    describe("client side paging and sorting", () => {
      const many = Array.from({ length: 6 }, (_, i) => ({ id: `r${i}`, name: `N${i}`, age: i }));
      const render = (specificAttributes) => {
        const state = gridState({ loadAll: true, max: 2 }, many);
        state.components.Grd.specificAttributes = { sort: [], ...specificAttributes };
        return renderWithProviders(<AweGrid id="Grd" />, { preloadedState: state }).container;
      };
      const rowsOf = (container) => Array.from(container.querySelectorAll(hook("grid-row")))
        .map(tr => [tr.getAttribute("row-id"), tr.querySelector(`${hook("grid-cell")}[column-id='name']`).textContent]);

      it("exposes the row ids of the second page", () => {
        expect(rowsOf(render({ first: 2, rows: 2 }))).toEqual([["r2", "N2"], ["r3", "N3"]]);
      });

      it("follows the order the grid sorts on the client", () => {
        const sort = [{ id: "name", direction: "desc" }];
        expect(rowsOf(render({ sort, first: 0, rows: 2 }))).toEqual([["r5", "N5"], ["r4", "N4"]]);
        cleanup();
        expect(rowsOf(render({ sort, first: 2, rows: 2 }))).toEqual([["r3", "N3"], ["r2", "N2"]]);
      });
    });

    it("exposes the selection checkboxes of a multiselect grid", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, {
        preloadedState: gridState({ multiselect: true }, rows)
      });

      const header = container.querySelector(hook("grid-header-checkbox"));
      expect(header).not.toBeNull();
      expect(header.getAttribute("data-selected")).toBe("false");
      const checks = Array.from(container.querySelectorAll(hook("grid-row-checkbox")));
      expect(checks.map(check => check.getAttribute("data-selected"))).toEqual(["true", "false"]);
    });

    it("exposes the pagination", () => {
      const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({}, rows) });

      const pagination = container.querySelector(`[grid-id='Grd'] ${hook("grid-pagination")}`);
      expect(pagination).not.toBeNull();
      const previous = pagination.querySelector(hook("grid-page-previous"));
      const next = pagination.querySelector(hook("grid-page-next"));
      expect(previous.getAttribute("data-disabled")).toBe("true");
      expect(next.getAttribute("data-disabled")).toBe("true");
      const pageSize = pagination.querySelector(hook("grid-page-size"));
      expect(pageSize).not.toBeNull();
      // The text of the dropdown also holds its hidden native selector: the value is exposed to be read without it
      expect(pageSize.getAttribute("data-value")).toBe(pageSize.querySelector("[data-pc-section='input']:not(input)").textContent);
    });

    it("exposes the loader only while the grid is loading", () => {
      const idle = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({}, rows) });
      expect(idle.container.querySelector(hook("grid-loader"))).toBeNull();
      cleanup();

      const loading = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState({ loading: true }, rows) });
      expect(loading.container.querySelector(`[grid-id='Grd'] ${hook("grid-loader")}`)).not.toBeNull();
    });
  });

  describe("row editor", () => {
    const noop = () => {};

    it("exposes the edit button", () => {
      const { container } = renderWithProviders(
        <ColumnRowEditor rowData={{ id: "r1" }} editRow={noop} saveRow={noop} cancelRow={noop} />);

      expect(container.querySelector(hook("grid-row-edit")).getAttribute("role")).toBe("edit-row");
    });

    it("exposes the save and cancel buttons while editing", () => {
      const { container } = renderWithProviders(
        <ColumnRowEditor rowData={{ id: "r1", $row: { editing: true } }} editRow={noop} saveRow={noop} cancelRow={noop} />);

      expect(container.querySelector(hook("grid-row-save")).getAttribute("role")).toBe("save-edit-row");
      expect(container.querySelector(hook("grid-row-cancel")).getAttribute("role")).toBe("cancel-edit-row");
    });
  });

  describe("tree grid", () => {
    const nodes = [
      { id: "p1", parent: "", name: "Parent", age: 1, $row: { expanded: true }, isLeaf: false },
      { id: "c1", parent: "p1", name: "Child", age: 2, isLeaf: true }
    ];

    it("exposes the tree grid, its header cells and its cells", () => {
      const { container } = renderWithProviders(<AweTreeGrid id="Grd" />, {
        preloadedState: gridState({ loadAll: true, expandColumn: "name" }, nodes, true)
      });

      const grid = container.querySelector("[tree-grid-id='Grd']");
      expect(grid.getAttribute("data-testid")).toBe("grid");
      expect(grid.querySelector(hook("grid-viewport"))).not.toBeNull();
      expect(Array.from(grid.querySelectorAll(hook("grid-header-cell"))).map(h => h.getAttribute("column-id")))
        .toEqual(["name", "age"]);
      const cells = Array.from(grid.querySelectorAll(`${hook("grid-cell")}[column-id='age']`));
      expect(cells.map(cell => cell.textContent)).toEqual(["1", "2"]);
    });

    it("exposes which rows are marked to be deleted", () => {
      const deleting = [
        { id: "p1", parent: "", name: "Parent", age: 1, $row: { expanded: true, operation: "DELETE" }, isLeaf: false },
        { id: "c1", parent: "p1", name: "Child", age: 2, isLeaf: true }
      ];
      const { container } = renderWithProviders(<AweTreeGrid id="Grd" />, {
        preloadedState: gridState({ loadAll: true, expandColumn: "name" }, deleting, true)
      });

      const trs = Array.from(container.querySelectorAll(`[tree-grid-id='Grd'] ${hook("grid-row")}`));
      expect(trs.map(tr => tr.getAttribute("row-id"))).toEqual(["p1", "c1"]);
      expect(trs.map(tr => tr.getAttribute("data-deleted"))).toEqual(["true", "false"]);
    });

    it("exposes the tree icon of each row with its state", () => {
      const { container } = renderWithProviders(<AweTreeGrid id="Grd" />, {
        preloadedState: gridState({ loadAll: true, expandColumn: "name" }, nodes, true)
      });

      const icons = Array.from(container.querySelectorAll(`[tree-grid-id='Grd'] ${hook("tree-icon")}`));
      expect(icons.map(icon => icon.getAttribute("row-id"))).toEqual(["p1", "c1"]);
      expect(icons.map(icon => icon.getAttribute("data-expanded"))).toEqual(["true", "false"]);
    });

    it("exposes the loader and the pagination", () => {
      const { container } = renderWithProviders(<AweTreeGrid id="Grd" />, {
        preloadedState: gridState({ loadAll: true, loading: true }, nodes, true)
      });

      expect(container.querySelector(hook("grid-loader"))).not.toBeNull();
      expect(container.querySelector(hook("grid-pagination"))).not.toBeNull();
    });
  });
});
