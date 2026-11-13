import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnLinkTest.jsx', () => {

  it('renders Column Link component', () => {
    renderWithProviders(Columns({
      component: 'link',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'link', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

    // fails
    expect(document.querySelector("a")).not.toBeNull();
  });
});
