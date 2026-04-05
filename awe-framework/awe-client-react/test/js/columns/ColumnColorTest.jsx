import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnColorTest.jsx', () => {

  it('renders Column Color component', () => {
    renderWithProviders(Columns({
      component: 'color',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'color', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // fails
    expect(document.querySelector("button")).not.toBeNull();
  });
});
