import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnSelectTest.jsx', () => {
  it('renders Column Select component (editor)', () => {
    renderWithProviders(Columns({
      component: 'select',
      model: {values: [
        {label: 'Option 1', value: '1'},
        {label: 'Option 2', value: '2'}
      ]},
      numberFormat: {},
      address: {component: 'select', view: 'report', column: 'column', row: 'row'},
      settings: {},
      placeholder: 'Choose...'
    }, {value: '1'}, {},true));

    // PrimeReact Dropdown root element should be present
    expect(document.querySelector('.p-dropdown')).not.toBeNull();
  });
});
