import {fireEvent, render, screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";

describe('awe-react-client/test/js/columns/ColumnUploaderTest.jsx', () => {

  it('renders Column Uploader component', () => {
    render(Columns({
      component: 'uploader',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'button', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // check
    expect(screen.getByText("Choose")).toBeDefined();
  });

  it('renders Column Uploader component and deletes a file', () => {
    render(Columns({
      component: 'uploader',
      model: {values: [{label: 'test', value: 'test', selected: true}]},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'button', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // check
    expect(document.querySelector("button.p-button-secondary")).not.toBeNull();

    // click on delete file
    fireEvent.click(document.querySelector("button.p-button-secondary"));

    // check button clear
    expect(document.querySelector("button.p-button-secondary")).toHaveClass("hidden");
  });
});
