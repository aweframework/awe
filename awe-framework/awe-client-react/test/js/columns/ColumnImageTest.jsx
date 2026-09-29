import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnImageTest.jsx', () => {

  it('renders Column Image component', () => {
    renderWithProviders(Columns({
      component: 'image',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'image', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // fails
    expect(document.querySelector("img")).not.toBeNull();
  });
});
