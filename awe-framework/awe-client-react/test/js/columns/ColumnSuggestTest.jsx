import {screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnSuggestTest.jsx', () => {

  it('renders Column Suggest component', () => {
    renderWithProviders(Columns({
      component: 'suggest',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'suggest', view: 'report', column: 'column', row: 'row'},
      placeholder: "Suggest test",
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "test"}, true));

    // check component
    expect(screen.getByPlaceholderText("Suggest test")).toBeDefined();
  });
});
