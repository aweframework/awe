import React from "react";
import {fireEvent, screen, within} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

const VIEW = "report";
const GRID = "grid1";
const COLUMN = "choice";

// The options of the column come from its list, like the ones of a select column
const COLUMN_MODEL = {
  name: COLUMN,
  component: "radio",
  label: "Choice",
  model: {values: [{value: "A", label: "Option A"}, {value: "B", label: "Option B"}, {value: "C", label: "Option C"}]}
};

const gridState = () => ({
  settings: {useComponentRegistry: false},
  components: {
    [GRID]: {
      address: {view: VIEW, component: GRID},
      attributes: {columnModel: [COLUMN_MODEL]},
      model: {
        values: [
          {id: "1", [COLUMN]: {value: "B", label: "Option B"}},
          {id: "2", [COLUMN]: {value: null}},
          {id: "3"}
        ]
      }
    }
  }
});

// A cell of the grid
function Cell({row, readonly = false, editing = false, attrs = {}, data, column = COLUMN_MODEL}) {
  return <div>{Columns({
    ...column,
    readonly,
    address: {view: VIEW, component: GRID, row, column: COLUMN}
  }, data ?? {value: null}, attrs, editing)}</div>;
}

const renderCell = (row, value, props = {}) => renderWithProviders(
  <Cell row={row} data={{value}} {...props}/>, {preloadedState: gridState()});

const getRow = (store, row) => store.getState().components[GRID].model.values.find(value => value.id === row);
const getRadio = (label) => screen.getByRole("radio", {name: label});

describe("awe-react-client/test/js/columns/ColumnRadioTest.jsx", () => {
  it("shows every option of the list of the column with its label", () => {
    renderCell("1", "B");

    expect(screen.getAllByRole("radio").map(radio => radio.getAttribute("aria-label")))
      .toEqual(["Option A", "Option B", "Option C"]);
    expect(screen.getByText("Option A")).toBeInTheDocument();
  });

  it("translates the labels of the options", () => {
    renderCell("1", "B", {column: {...COLUMN_MODEL, model: {values: [{value: "A", label: "SOME_KEY_NOT_TRANSLATED"}]}}});

    expect(screen.getByText("SOME_KEY_NOT_TRANSLATED")).toBeInTheDocument();
  });

  it("checks only the option that matches the value of the cell", () => {
    renderCell("1", "B");

    expect(getRadio("Option A")).not.toBeChecked();
    expect(getRadio("Option B")).toBeChecked();
    expect(getRadio("Option C")).not.toBeChecked();
  });

  it("checks nothing when the cell has no value", () => {
    renderCell("2", null);

    screen.getAllByRole("radio").forEach(radio => expect(radio).not.toBeChecked());
  });

  it("is one radio group labelled by the label of the column", () => {
    renderCell("1", "B");

    expect(screen.getByRole("radiogroup", {name: "Choice"})).toBeInTheDocument();
  });

  it("shares one radio name between the options of a cell and not between rows", () => {
    const first = renderCell("1", "B");
    const names = within(first.container).getAllByRole("radio").map(radio => radio.getAttribute("name"));
    expect(new Set(names).size).toBe(1);

    const second = renderCell("2", null);
    const secondNames = within(second.container).getAllByRole("radio").map(radio => radio.getAttribute("name"));
    expect(secondNames[0]).not.toEqual(names[0]);
  });

  it("stores the chosen option as the value of the cell, like a select column", () => {
    const {store} = renderCell("2", null);

    fireEvent.click(getRadio("Option C"));

    expect(getRow(store, "2")[COLUMN]).toMatchObject({value: "C", label: "Option C"});
  });

  it("changes the choice of the row and keeps the other rows untouched", () => {
    const {store} = renderCell("1", "B");

    fireEvent.click(getRadio("Option A"));

    expect(getRow(store, "1")[COLUMN].value).toBe("A");
    expect(getRow(store, "2")[COLUMN].value).toBeNull();
    expect(getRow(store, "3")[COLUMN]).toBeUndefined();
  });

  it("clears the validation error of the cell once a choice is made", () => {
    const state = gridState();
    state.components[GRID].model.values[1].$attrs = {[COLUMN]: {error: {message: "VALIDATOR_MESSAGE_REQUIRED"}, readonly: false}};
    const {store} = renderWithProviders(<Cell row="2" data={{value: null}}/>, {preloadedState: state});

    fireEvent.click(getRadio("Option B"));

    expect(getRow(store, "2").$attrs).toEqual({[COLUMN]: {readonly: false}});
  });

  it("keeps the model unchanged when the chosen option is clicked again", () => {
    const {store} = renderCell("1", "B");
    const before = store.getState().components[GRID].model.values;

    fireEvent.click(getRadio("Option B"));

    expect(store.getState().components[GRID].model.values).toBe(before);
  });

  it("is disabled when the column is readonly", () => {
    renderCell("2", null, {readonly: true});

    screen.getAllByRole("radio").forEach(radio => expect(radio).toBeDisabled());
  });

  it("is disabled when the cell is readonly", () => {
    renderCell("2", null, {attrs: {readonly: true}});

    screen.getAllByRole("radio").forEach(radio => expect(radio).toBeDisabled());
  });

  it("does not change a readonly cell", () => {
    const {store} = renderCell("1", "B", {readonly: true});

    fireEvent.click(getRadio("Option A"));

    expect(getRow(store, "1")[COLUMN].value).toBe("B");
  });

  it("works while the row is being edited", () => {
    const {store} = renderCell("3", undefined, {editing: true});

    fireEvent.click(getRadio("Option B"));

    expect(getRow(store, "3")[COLUMN].value).toBe("B");
  });

  it("moves the choice to the next option with the right arrow key", () => {
    const {store} = renderCell("1", "A");

    fireEvent.keyDown(getRadio("Option A"), {key: "ArrowRight"});

    expect(getRow(store, "1")[COLUMN].value).toBe("B");
    expect(getRadio("Option B")).toHaveFocus();
  });

  it("moves the choice to the previous option with the left arrow key", () => {
    const {store} = renderCell("1", "B");

    fireEvent.keyDown(getRadio("Option B"), {key: "ArrowLeft"});

    expect(getRow(store, "1")[COLUMN].value).toBe("A");
  });

  it("goes round from the last option to the first one", () => {
    const {store} = renderCell("1", "C");

    fireEvent.keyDown(getRadio("Option C"), {key: "ArrowRight"});

    expect(getRow(store, "1")[COLUMN].value).toBe("A");
  });

  it("goes round from the first option to the last one", () => {
    const {store} = renderCell("1", "A");

    fireEvent.keyDown(getRadio("Option A"), {key: "ArrowLeft"});

    expect(getRow(store, "1")[COLUMN].value).toBe("C");
  });

  it("skips the disabled options when moving with the arrow keys", () => {
    const column = {...COLUMN_MODEL, model: {values: [
      {value: "A", label: "Option A"}, {value: "B", label: "Option B", disabled: true}, {value: "C", label: "Option C"}]}};
    const {store} = renderCell("1", "A", {column});

    fireEvent.keyDown(getRadio("Option A"), {key: "ArrowRight"});

    expect(getRow(store, "1")[COLUMN].value).toBe("C");
  });

  it("moves the choice with the arrow keys whatever modifiers are pressed", () => {
    const {store} = renderCell("1", "A");

    fireEvent.keyDown(getRadio("Option A"), {key: "ArrowDown", shiftKey: true, ctrlKey: true});

    expect(getRow(store, "1")[COLUMN].value).toBe("B");
  });

  it("ignores the other keys", () => {
    const {store} = renderCell("1", "B");

    fireEvent.keyDown(getRadio("Option B"), {key: "Enter"});

    expect(getRow(store, "1")[COLUMN].value).toBe("B");
  });

  it("marks the options as invalid when the cell has an error", () => {
    renderCell("2", null, {attrs: {error: {message: "VALIDATOR_MESSAGE_REQUIRED"}}});

    screen.getAllByRole("radio").forEach(radio => expect(radio.closest(".p-radiobutton")).toHaveClass("p-invalid"));
  });

  it("exposes every option to the tests with its value and its state", () => {
    const {container} = renderCell("1", "B");

    const options = Array.from(container.querySelectorAll("[data-testid='criterion-input']"));
    expect(options.map(option => option.getAttribute("option-id"))).toEqual(["A", "B", "C"]);
    expect(options.map(option => option.getAttribute("data-selected"))).toEqual(["false", "true", "false"]);
  });
});
