import React from "react";
import { fireEvent } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

// Non-selectable grids (#303): with selectable="false" a row click never selects (or deselects) the row, nothing is
// highlighted, and the events of the cells keep firing
describe("awe-react-client/test/js/components/AweGridSelectableTest.jsx", () => {
  const ROWS = [
    { id: 1, name: "Chair", selected: false },
    { id: 2, name: "Table", selected: true }
  ];

  const state = (attributes = {}) => ({
    settings: DEFAULT_SETTINGS,
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
    components: {
      grid: {
        address: { component: "grid", view: "report" },
        model: { values: ROWS, records: ROWS.length },
        attributes: {
          columnModel: [{ name: "name", label: "Name" }], headerModel: [], buttonModel: [], visible: true, loadAll: true,
          ...attributes
        },
        specificAttributes: { sort: [] }
      }
    }
  });

  const render = (attributes) => renderWithProviders(<AweGrid id="grid" />, { preloadedState: state(attributes), spyDispatch: true });

  const selectRowActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "select-row");

  const clickRow = (container, index) => fireEvent.click(container.querySelectorAll("tbody.p-datatable-tbody > tr")[index].querySelector("td"));

  it("selects the clicked row by default", () => {
    const { container, dispatchSpy } = render({});

    expect(container.querySelector(".p-datatable-selectable, .p-datatable-hoverable-rows")).not.toBeNull();
    expect(container.querySelectorAll("tr.p-highlight")).toHaveLength(1);
    clickRow(container, 0);

    expect(selectRowActions(dispatchSpy)).toEqual([expect.objectContaining({ parameters: { values: [1] } })]);
  });

  it("selects the clicked row with selectable true", () => {
    const { container, dispatchSpy } = render({ selectable: true });

    clickRow(container, 0);

    expect(selectRowActions(dispatchSpy)).toHaveLength(1);
  });

  it("does not select nor deselect a row on click with selectable false", () => {
    const { container, dispatchSpy } = render({ selectable: false });

    clickRow(container, 0);
    clickRow(container, 1);

    expect(selectRowActions(dispatchSpy)).toEqual([]);
  });

  it("shows no selection and no hover highlight with selectable false", () => {
    const { container } = render({ selectable: false });

    expect(container.querySelectorAll("tr.p-highlight")).toHaveLength(0);
    expect(container.querySelectorAll("tr.p-selectable-row")).toHaveLength(0);
    expect(container.querySelector(".p-datatable-hoverable-rows, .p-datatable-selectable")).toBeNull();
  });

  it("shows no selection checkbox column with selectable false even when multiselect is set", () => {
    const { container, dispatchSpy } = render({ selectable: false, multiselect: true });

    expect(container.querySelectorAll(".p-checkbox")).toHaveLength(0);
    clickRow(container, 0);
    expect(selectRowActions(dispatchSpy)).toEqual([]);
  });

  it("keeps the selection checkbox column of a multiselect grid by default", () => {
    const { container } = render({ multiselect: true });

    expect(container.querySelectorAll(".p-checkbox").length).toBeGreaterThan(0);
  });

  it("keeps the cell buttons working with selectable false", () => {
    const { container, dispatchSpy } = render({
      selectable: false,
      columnModel: [{ name: "name", label: "Name" },
        { name: "edit", component: "button", label: "Edit", actions: [{ type: "dialog", target: "PnlUsr" }] }]
    });
    // Cell values are keyed by column
    const button = container.querySelector("tbody button");

    expect(button).not.toBeNull();
    fireEvent.click(button);

    expect(dispatchSpy).toHaveBeenCalled();
  });
});
