import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnProgressTest.jsx', () => {

  it('renders Column Progress component', () => {
    renderWithProviders(Columns({
      component: 'progress',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'progress', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // fails
    expect(document.querySelector("div")).not.toBeNull();
  });
});
