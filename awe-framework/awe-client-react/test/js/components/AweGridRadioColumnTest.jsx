import React from "react";
import { cleanup, fireEvent, within } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";

const choiceValues = [{ value: "A", label: "Option A" }, { value: "B", label: "Option B" }];
const modeValues = [{ value: "1", label: "Slow" }, { value: "2", label: "Fast" }];
const columnModel = [
  { name: "name", label: "Name" },
  { name: "choice", label: "Choice", component: "radio", sendable: true, model: { values: choiceValues } },
  { name: "mode", label: "Mode", component: "button-radio", sendable: true, model: { values: modeValues } }
];

function gridState() {
  return {
    settings: { ...DEFAULT_SETTINGS, useComponentRegistry: false },
    screen: { size: { width: 100, height: 100 } },
    components: {
      Grd: {
        address: { component: "Grd", view: "report" },
        model: {
          values: [
            { id: "r1", name: "First", choice: { value: "B" }, mode: { value: "1" } },
            { id: "r2", name: "Second", choice: { value: null } }
          ],
          records: 2
        },
        attributes: { columnModel, headerModel: [], buttonModel: [], visible: true, max: 30, sendAll: true },
        specificAttributes: { sort: [] }
      }
    }
  };
}

const getRowCells = (container, rowIndex) => within(container.querySelectorAll("tbody tr")[rowIndex]);
const getValues = (store) => store.getState().components.Grd.model.values;

describe("awe-react-client/test/js/components/AweGridRadioColumnTest.jsx", () => {
  afterEach(cleanup);

  it("shows the options of the list in every row with the choice of the cell", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    const first = getRowCells(container, 0);
    expect(first.getByRole("radio", { name: "Option B" })).toBeChecked();
    expect(first.getByRole("radio", { name: "Option A" })).not.toBeChecked();
    expect(first.getByRole("radio", { name: "Slow" })).toBeChecked();
    expect(first.getByRole("radio", { name: "Fast" })).not.toBeChecked();
    getRowCells(container, 1).getAllByRole("radio").forEach(radio => expect(radio).not.toBeChecked());
  });

  it("draws the options of a button-radio cell as one joined bar", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    expect(container.querySelectorAll("tbody tr:first-child .column-button-radio")).toHaveLength(1);
    expect(container.querySelectorAll("tbody tr:first-child .column-button-radio .p-button")).toHaveLength(2);
  });

  it("does not give the cells of the columns any position class", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    expect(container.querySelector("td[class*='radio-cell']")).toBeNull();
  });

  it("marks only the chosen radio as highlighted inside the radio column", () => {
    const { container } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    const checked = getRowCells(container, 0).getByRole("radio", { name: "Option B" });
    expect(checked.closest(".p-radiobutton")).toHaveClass("p-highlight");
    expect(checked.closest(".column-radio")).not.toBeNull();
    expect(getRowCells(container, 0).getByRole("radio", { name: "Option A" }).closest(".p-radiobutton")).not.toHaveClass("p-highlight");
  });

  it("chooses an option in one row without touching the other one", () => {
    const { container, store } = renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    fireEvent.click(getRowCells(container, 1).getByRole("radio", { name: "Option A" }));
    fireEvent.click(getRowCells(container, 0).getByRole("radio", { name: "Fast" }));

    expect(getValues(store).map(row => row.choice?.value)).toEqual(["B", "A"]);
    expect(getValues(store).map(row => row.mode?.value)).toEqual(["2", undefined]);
  });

  it("does not warn about a missing group", () => {
    const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

    renderWithProviders(<AweGrid id="Grd" />, { preloadedState: gridState() });

    expect(warn.mock.calls.filter(([message]) => String(message).includes("group"))).toHaveLength(0);
    warn.mockRestore();
  });
});
