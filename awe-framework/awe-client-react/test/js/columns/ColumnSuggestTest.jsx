import React from 'react';
import {act, fireEvent, render, screen} from '@testing-library/react';
import ColumnSuggestInput from "../../../src/columns/ColumnSuggestInput";

import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnSuggestTest.jsx', () => {

  it('renders Column Suggest component', () => {
    renderWithProviders(Columns({
      component: 'suggest',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'suggest', view: 'report', column: 'column', row: 'row'},
      placeholder: "Suggest test",
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // check component
    expect(screen.getByPlaceholderText("Suggest test")).toBeDefined();
  });
  it('does not crash on blur when the labels of the suggestions are numbers', async () => {
    const errors = [];
    const onError = event => { errors.push(event.error || event.message); event.preventDefault(); };
    window.addEventListener("error", onError);
    const consoleError = jest.spyOn(console, "error").mockImplementation(() => {});

    const {container} = render(<ColumnSuggestInput
      autocompleteRef={React.createRef()}
      value={{label: 1, value: 1, selected: true}}
      placeholder="Suggest test"
      suggestions={[{label: 1, value: 1}, {label: 2, value: 2}]}
      onChange={jest.fn()}
      onClear={jest.fn()}
      onSuggest={jest.fn()}
      onKeyPress={jest.fn()}
      owner="column"
      t={jest.fn()}
    />);
    const input = container.querySelector("input[aria-autocomplete]");

    await act(async () => {
      fireEvent.focus(input);
      fireEvent.change(input, {target: {value: "2"}});
      fireEvent.blur(input);
    });

    window.removeEventListener("error", onError);
    consoleError.mockRestore();
    expect(errors).toEqual([]);
  });
});
