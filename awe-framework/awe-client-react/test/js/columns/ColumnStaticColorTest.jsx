import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {screen} from '@testing-library/react';

describe('awe-react-client/test/js/columns/ColumnStaticColorTest.jsx', () => {

  it('renders Column Static Color component', () => {
    renderWithProviders(Columns({
      component: 'color',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jasmine.createSpy("updateModel"),
      updateAttributes: jasmine.createSpy("updateAttributes"),
      addActionsTop: jasmine.createSpy("addActionsTop"),
      address: {component: 'color', view: 'report', column: 'column', row: 'row'},
      t: jasmine.createSpy("t"),
      settings: {}
    }, {value: "#abcdef"}, {},false));

    // should render a color swatch span for static color
    expect(screen.getByText("#abcdef")).toBeDefined();
  });
});
