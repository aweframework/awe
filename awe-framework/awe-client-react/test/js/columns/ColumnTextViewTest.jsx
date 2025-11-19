import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnTextViewTest.jsx', () => {

  it('renders Column Text View component', () => {
    renderWithProviders(Columns({
      component: 'text-view',
      model: {values: []},
      numberFormat: {},
      address: {component: 'text-view', view: 'report', column: 'column', row: 'row'},
      settings: {}
    }, {value: "test"}, {}, true));

    // fails
    expect(document.querySelector("button")).not.toBeNull();
  });
});
