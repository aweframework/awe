import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnButtonTest.jsx', () => {

  it('renders Column Button component', () => {
    renderWithProviders(Columns({
      component: 'button',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'button', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // fails
    expect(document.querySelector("button.p-button")).not.toBeNull();
  });
});
