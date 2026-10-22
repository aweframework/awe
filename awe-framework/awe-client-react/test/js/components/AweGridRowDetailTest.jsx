import React from "react";
import { act, cleanup, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { updateAttributes, updateSpecificAttributes } from "../../../src/redux/actions/components";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

const hook = (testId) => `[data-testid='${testId}']`;
const TOGGLE = hook("grid-row-detail-toggle");
const DETAIL = hook("grid-row-detail");
const ADDRESS = { component: "Grd", view: "report" };
const ROWS = [
  { id: "r1", name: "Ann", notes: "First notes" },
  { id: "r2", name: "Bob", notes: "Second notes" },
  { id: "r3", name: "Carl", notes: "Third notes" }
];
const ROW_DETAIL = { template: "Notes of {name}: {notes}\n<b>plain</b>" };

function gridState({ values = ROWS, attributes = {}, specificAttributes = {} } = {}) {
  return {
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: ADDRESS,
        model: { values, records: values.length },
        attributes: {
          columnModel: [{ name: "name", label: "Name" }, { name: "notes", label: "Notes", hidden: true }],
          headerModel: [],
          buttonModel: [],
          visible: true,
          max: 30,
          ...attributes
        },
        specificAttributes: { sort: [], ...specificAttributes }
      }
    }
  };
}

const renderGrid = (options = {}) => renderWithProviders(<AweGrid id="Grd" />, { spyDispatch: true, ...options });
const toggleOf = (container, rowId) => container.querySelector(`${TOGGLE}[row-id='${rowId}']`);
const detailOf = (container, rowId) => container.querySelector(`${DETAIL}[row-id='${rowId}']`);
const rowOf = (container, rowId) => container.querySelector(`${hook("grid-row")}[row-id='${rowId}']`);
const selectActions = (dispatchSpy) => dispatchSpy.mock.calls
  .map(([action]) => action)
  .filter(action => action.type === "ADD_ACTIONS_TOP")
  .flatMap(action => action.payload)
  .filter(action => action.type === "select-row");
const headerCells = (container) => container.querySelectorAll("thead th").length;

describe("awe-react-client/test/js/components/AweGridRowDetailTest.jsx", () => {
  afterEach(cleanup);

  it("does not render the toggle column nor details in a grid without row detail", () => {
    const { container } = renderGrid({ preloadedState: gridState() });

    expect(container.querySelector(TOGGLE)).toBeNull();
    expect(container.querySelector(DETAIL)).toBeNull();
    // Name and the filler column
    expect(headerCells(container)).toBe(2);
  });

  it("keeps the header columns of the selection and row number columns without row detail", () => {
    const { container } = renderGrid({ preloadedState: gridState({ attributes: { multiselect: true, rowNumbers: true } }) });

    // Selection, row number, Name and the filler column
    expect(headerCells(container)).toBe(4);
    expect(container.querySelector(TOGGLE)).toBeNull();
  });

  it("adds one leading column to the header, the rows and the totals with row detail", () => {
    const { container } = renderGrid({
      preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL, multiselect: true, rowNumbers: true, showTotals: true } })
    });

    expect(headerCells(container)).toBe(5);
    expect(container.querySelector(`${hook("grid-row")}[row-id='r1']`).querySelectorAll("td").length).toBe(5);
    expect(container.querySelectorAll("tfoot td").length).toBe(5);
    expect(container.querySelectorAll(TOGGLE).length).toBe(3);
  });

  it("adds the leading column to the skeleton rows, and not without row detail", () => {
    const skeletonCells = (attributes) => {
      const { container } = renderGrid({ preloadedState: gridState({ values: [], attributes: { loading: true, ...attributes } }) });
      const count = container.querySelector(".grid-skeleton-row").querySelectorAll(".grid-skeleton-cell").length;
      cleanup();
      return count;
    };

    expect(skeletonCells({ multiselect: true })).toBe(2);
    expect(skeletonCells({ multiselect: true, rowDetail: ROW_DETAIL })).toBe(3);
  });

  it("adds the leading column to both rows of a grouped header", () => {
    const { container } = renderGrid({
      preloadedState: gridState({
        attributes: {
          rowDetail: ROW_DETAIL,
          columnModel: [{ name: "name", label: "Name" }, { name: "code", label: "Code" }],
          headerModel: [{ label: "Group", startColumnName: "name", numberOfColumns: 2, columns: ["name", "code"] }]
        }
      })
    });

    const toggleHeader = container.querySelector("thead tr").querySelector("th");
    expect(toggleHeader.getAttribute("rowspan")).toBe("2");
  });

  it("opens the detail of a row under it with the data of hidden columns, and closes it", () => {
    const { container } = renderGrid({ preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL } }) });
    const toggle = toggleOf(container, "r1");

    expect(toggle.tagName).toBe("BUTTON");
    expect(toggle.getAttribute("type")).toBe("button");
    expect(toggle.getAttribute("aria-expanded")).toBe("false");
    expect(toggle.getAttribute("aria-label")).toBe("SCREEN_TEXT_GRID_ROW_DETAIL_SHOW");
    expect(detailOf(container, "r1")).toBeNull();

    fireEvent.click(toggle);

    const detail = detailOf(container, "r1");
    expect(toggleOf(container, "r1").getAttribute("aria-expanded")).toBe("true");
    expect(toggleOf(container, "r1").getAttribute("aria-label")).toBe("SCREEN_TEXT_GRID_ROW_DETAIL_HIDE");
    expect(toggleOf(container, "r1").getAttribute("aria-controls")).toBe(detail.id);
    expect(detail.textContent).toBe("Notes of Ann: First notes\n<b>plain</b>");
    expect(detail.querySelector("b")).toBeNull();
    expect(detail.classList).toContain("grid-row-detail");
    // The detail row follows its row and fills its width
    const detailRow = detail.closest("tr");
    expect(rowOf(container, "r1").nextElementSibling).toBe(detailRow);
    expect(detailRow.querySelector("td").getAttribute("colspan")).toBe("3");

    fireEvent.click(toggleOf(container, "r1"));

    expect(toggleOf(container, "r1").getAttribute("aria-expanded")).toBe("false");
    expect(detailOf(container, "r1")).toBeNull();
  });

  it("adds the style classes of the row detail to its area", () => {
    const { container } = renderGrid({
      preloadedState: gridState({ attributes: { rowDetail: { ...ROW_DETAIL, style: "row-detail-card text-muted" } } })
    });

    fireEvent.click(toggleOf(container, "r1"));

    expect([...detailOf(container, "r1").classList]).toEqual(["grid-row-detail", "row-detail-card", "text-muted"]);
  });

  it("keeps only the grid-row-detail class on the area of a row detail without style", () => {
    const { container } = renderGrid({ preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL } }) });

    fireEvent.click(toggleOf(container, "r1"));

    expect(detailOf(container, "r1").getAttribute("class")).toBe("grid-row-detail");
  });

  it("keeps several details open at once", () => {
    const { container } = renderGrid({ preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL } }) });

    fireEvent.click(toggleOf(container, "r1"));
    fireEvent.click(toggleOf(container, "r3"));

    expect(detailOf(container, "r1").textContent).toContain("First notes");
    expect(detailOf(container, "r3").textContent).toContain("Third notes");
    expect(detailOf(container, "r2")).toBeNull();
    expect(toggleOf(container, "r2").getAttribute("aria-expanded")).toBe("false");
  });

  it("does not select the row when its detail is toggled", () => {
    const { container, dispatchSpy } = renderGrid({ preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL } }) });

    fireEvent.click(toggleOf(container, "r2"));
    fireEvent.click(toggleOf(container, "r2").querySelector("i"));

    expect(selectActions(dispatchSpy)).toEqual([]);

    // A click on a cell of the same row still selects it
    fireEvent.click(rowOf(container, "r2").querySelector(`${hook("grid-cell")}[column-id='name']`));
    expect(selectActions(dispatchSpy)).toEqual([{ type: "select-row", address: ADDRESS, parameters: { values: ["r2"] } }]);
  });

  it("toggles with the keyboard as a native button without selecting the row", () => {
    const { container, dispatchSpy } = renderGrid({ preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL } }) });
    const toggle = toggleOf(container, "r1");

    toggle.focus();
    expect(document.activeElement).toBe(toggle);
    // Enter and Space on the button reach PrimeReact's row keyboard handling first: it must not select the row
    fireEvent.keyDown(toggle, { key: "Enter", code: "Enter" });
    fireEvent.keyDown(toggle, { key: " ", code: "Space" });
    // The browser turns them into a click on a native button
    fireEvent.click(toggle);

    expect(toggleOf(container, "r1").getAttribute("aria-expanded")).toBe("true");
    expect(detailOf(container, "r1")).not.toBeNull();
    expect(selectActions(dispatchSpy)).toEqual([]);
  });

  it("closes every detail when the grid reloads its data", () => {
    const { container, store } = renderGrid({ preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL } }) });
    fireEvent.click(toggleOf(container, "r1"));
    fireEvent.click(toggleOf(container, "r2"));

    act(() => {
      store.dispatch(updateAttributes(ADDRESS, { loading: true }));
    });
    act(() => {
      store.dispatch(updateAttributes(ADDRESS, { loading: false }));
    });

    expect(container.querySelector(DETAIL)).toBeNull();
    expect(toggleOf(container, "r1").getAttribute("aria-expanded")).toBe("false");
    expect(toggleOf(container, "r2").getAttribute("aria-expanded")).toBe("false");
  });

  it("keeps the details open across client-side paging and sorting of a grid that loads all its rows", () => {
    const { container, store } = renderGrid({
      preloadedState: gridState({ attributes: { rowDetail: ROW_DETAIL, loadAll: true, max: 2 }, specificAttributes: { rows: 2 } })
    });
    fireEvent.click(toggleOf(container, "r1"));

    act(() => {
      store.dispatch(updateSpecificAttributes(ADDRESS, { first: 2 }));
    });
    expect(rowOf(container, "r3")).not.toBeNull();
    expect(detailOf(container, "r1")).toBeNull();

    act(() => {
      store.dispatch(updateSpecificAttributes(ADDRESS, { first: 0 }));
    });
    expect(detailOf(container, "r1").textContent).toContain("First notes");

    act(() => {
      store.dispatch(updateSpecificAttributes(ADDRESS, { sort: [{ id: "name", direction: "desc" }] }));
    });
    // Carl, Bob on the first page: Ann moved to the second one with its detail
    expect(rowOf(container, "r1")).toBeNull();
    act(() => {
      store.dispatch(updateSpecificAttributes(ADDRESS, { first: 2 }));
    });
    expect(rowOf(container, "r1").nextElementSibling).toBe(detailOf(container, "r1").closest("tr"));
  });
});
