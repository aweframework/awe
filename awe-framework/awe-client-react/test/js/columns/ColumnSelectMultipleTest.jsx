import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnSelectMultipleTest.jsx', () => {
  it('renders Column Select Multiple component (editor)', () => {
    renderWithProviders(Columns({
      component: 'select-multiple',
      model: {values: [
        {label: 'Option 1', value: '1'},
        {label: 'Option 2', value: '2'}
      ]},
      numberFormat: {},
      address: {component: 'select-multiple', view: 'report', column: 'column', row: 'row'},
      settings: {},
      placeholder: 'Choose multiple...'
    }, [
      {value: '1', label: 'Option 1', selected: true},
      {value: '2', label: 'Option 2'}
    ], {}, true));

    expect(document.querySelector('.p-multiselect')).not.toBeNull();
  });
});
