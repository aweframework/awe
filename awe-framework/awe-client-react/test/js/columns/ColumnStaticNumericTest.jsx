import {screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnStaticNumericTest.jsx', () => {

  it('renders Column Static Numeric component', () => {
    renderWithProviders(Columns({
      component: 'numeric',
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'grid', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value:1123123}, {},false));

    // fails
    expect(screen.getByText(/1\.123\.123/)).toBeDefined();
  });

  it('renders Column Static Numeric component with suffix', () => {
    renderWithProviders(Columns({
      component: 'numeric',
      numberFormat: {aSign: ' EUR'},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'grid', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value:1123123}, false));

    // fails
    expect(screen.getByText(/1\.123\.123 EUR/)).toBeDefined();
  });

  it('renders Column Static Numeric component with empty value', () => {
    renderWithProviders(Columns({
      component: 'numeric',
      numberFormat: {aSign: ' EUR'},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'grid', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value:null}, false));

    // fails
    expect(screen.queryByText(/EUR/)).toBeNull();
  });
});
