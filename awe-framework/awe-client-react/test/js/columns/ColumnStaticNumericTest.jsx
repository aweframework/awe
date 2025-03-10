import {render, screen} from '@testing-library/react';

import {Columns} from "../../../src/utilities/structure";

describe('awe-react-client/test/js/columns/ColumnStaticNumericTest.jsx', () => {

  it('renders Column Static Numeric component', () => {
    render(Columns({
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
    render(Columns({
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
    render(Columns({
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
