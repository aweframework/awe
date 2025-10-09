import {screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnStaticNumericTest.jsx', () => {

  it('renders Column Static Numeric component', () => {
    renderWithProviders(Columns({
      component: 'numeric',
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'grid', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value:1123123}, false));

    // fails
    expect(screen.getByText(/1\.123\.123/)).toBeDefined();
  });

  it('renders Column Static Numeric component with suffix', () => {
    renderWithProviders(Columns({
      component: 'numeric',
      numberFormat: {aSign: ' EUR'},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'grid', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value:1123123}, false));

    // fails
    expect(screen.getByText(/1\.123\.123 EUR/)).toBeDefined();
  });

  it('renders Column Static Numeric component with empty value', () => {
    renderWithProviders(Columns({
      component: 'numeric',
      numberFormat: {aSign: ' EUR'},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'grid', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value:null}, false));

    // fails
    expect(screen.queryByText(/EUR/)).toBeNull();
  });
});
