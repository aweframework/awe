import {fireEvent, screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnUploaderTest.jsx', () => {

  it('renders Column Uploader component', () => {
    renderWithProviders(Columns({
      component: 'uploader',
      model: {values: []},
      numberFormat: {},
      address: {component: 'button', view: 'report', column: 'column', row: 'row'},
      settings: {}
    }, {value: "test"}, {},true));

    // check
    expect(screen.getByText("Choose")).toBeDefined();
  });

  it('renders Column Uploader component and deletes a file', () => {
    renderWithProviders(Columns({
      component: 'uploader',
      model: {values: [{label: 'test', value: 'test', selected: true}]},
      numberFormat: {},
      address: {component: 'button', view: 'report', column: 'column', row: 'row'},
      settings: {}
    }, {value: "test"}, {}, true));

    // check
    expect(document.querySelector("button.p-button-secondary")).not.toBeNull();

    // click on delete file
    fireEvent.click(document.querySelector("button.p-button-secondary"));

    // check button clear
    expect(document.querySelector("button.p-button-secondary")).toHaveClass("hidden");
  });
});
