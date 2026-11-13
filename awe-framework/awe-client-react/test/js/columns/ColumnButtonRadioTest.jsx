import React from "react";
import {fireEvent, screen} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";
import {getAllComponents} from "../../../src/redux/selectors/componentSelectors";

const VIEW = "report";
const GRID = "grid1";
const COLUMN = "mode";

// The options of the column come from its list, like the ones of a select column
const COLUMN_MODEL = {
  name: COLUMN,
  component: "button-radio",
  label: "Mode",
  model: {values: [{value: "L", label: "Low"}, {value: "M", label: "Medium"}, {value: "H", label: "High"}]}
};

const gridState = () => ({
  components: {
    [GRID]: {
      address: {view: VIEW, component: GRID},
      attributes: {columnModel: [COLUMN_MODEL]},
      model: {values: [{id: "1", [COLUMN]: {value: "M"}}, {id: "2", [COLUMN]: {value: null}}]}
    }
  }
});

const renderCell = (row, value, {readonly = false, attrs = {}, column = COLUMN_MODEL} = {}) => renderWithProviders(
  <div>{Columns({
    ...column,
    readonly,
    address: {view: VIEW, component: GRID, row, column: COLUMN}
  }, {value}, attrs, false)}</div>, {preloadedState: registered(gridState())});

// The application registers every component as the frozen base and keeps only the changes in Redux
const registered = (state) => {
  Object.entries(state.components).forEach(([id, component]) => ComponentRegistry.register(id, _.cloneDeep(component)));
  return state;
};

const getRow = (store, row) => getAllComponents(store.getState())[GRID].model.values.find(value => value.id === row);
const getRadio = (label) => screen.getByRole("radio", {name: label});

describe("awe-react-client/test/js/columns/ColumnButtonRadioTest.jsx", () => {
  it("renders every option of the list as a button of one joined bar", () => {
    const {container} = renderCell("1", "M");

    const bar = screen.getByRole("radiogroup", {name: "Mode"});
    expect(bar).toHaveClass("column-button-radio", "p-selectbutton");
    expect(container.querySelectorAll(".column-button-radio")).toHaveLength(1);
    expect(Array.from(bar.querySelectorAll(".p-button")).map(button => button.textContent)).toEqual(["Low", "Medium", "High"]);
  });

  it("highlights only the chosen option", () => {
    renderCell("1", "M");

    expect(getRadio("Medium")).toBeChecked();
    expect(getRadio("Medium").closest(".p-button")).toHaveClass("p-highlight");
    expect(getRadio("Low").closest(".p-button")).not.toHaveClass("p-highlight");
    expect(getRadio("High").closest(".p-button")).not.toHaveClass("p-highlight");
  });

  it("highlights nothing when the cell has no value", () => {
    renderCell("2", null);

    screen.getAllByRole("radio").forEach(radio => expect(radio.closest(".p-button")).not.toHaveClass("p-highlight"));
  });

  it("stores the clicked option as the value of the cell of its row only", () => {
    const {store} = renderCell("1", "M");

    fireEvent.click(getRadio("High"));

    expect(getRow(store, "1")[COLUMN]).toMatchObject({value: "H", label: "High"});
    expect(getRow(store, "2")[COLUMN].value).toBeNull();
  });

  it("moves the choice with the arrow keys and goes round", () => {
    const {store} = renderCell("1", "H");

    fireEvent.keyDown(getRadio("High"), {key: "ArrowRight"});

    expect(getRow(store, "1")[COLUMN].value).toBe("L");
  });

  it("goes round from the first option to the last one with the left arrow key", () => {
    const {store} = renderCell("1", "L");

    fireEvent.keyDown(getRadio("Low"), {key: "ArrowLeft"});

    expect(getRow(store, "1")[COLUMN].value).toBe("H");
  });

  it("skips the disabled options with the arrow keys", () => {
    const column = {...COLUMN_MODEL, model: {values: [
      {value: "L", label: "Low"}, {value: "M", label: "Medium", disabled: true}, {value: "H", label: "High"}]}};
    const {store} = renderCell("1", "L", {column});

    fireEvent.keyDown(getRadio("Low"), {key: "ArrowRight"});

    expect(getRow(store, "1")[COLUMN].value).toBe("H");
  });

  it("cannot be changed when it is readonly", () => {
    const {store} = renderCell("1", "M", {readonly: true});

    expect(getRadio("Low")).toBeDisabled();
    expect(getRadio("Low").closest(".p-button")).toHaveClass("p-disabled");
    fireEvent.click(getRadio("Low"));
    expect(getRow(store, "1")[COLUMN].value).toBe("M");
  });

  it("is invalid when the cell has an error", () => {
    renderCell("2", null, {attrs: {error: {message: "VALIDATOR_MESSAGE_REQUIRED"}}});

    expect(screen.getByRole("radiogroup")).toHaveClass("p-invalid");
  });

  it("exposes every option to the tests with its value and its state", () => {
    const {container} = renderCell("1", "M");

    const options = Array.from(container.querySelectorAll("[data-testid='criterion-input']"));
    expect(options.map(option => option.getAttribute("option-id"))).toEqual(["L", "M", "H"]);
    expect(options.map(option => option.getAttribute("data-selected"))).toEqual(["false", "true", "false"]);
  });
});
