import {screen} from '@testing-library/react';

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
});
