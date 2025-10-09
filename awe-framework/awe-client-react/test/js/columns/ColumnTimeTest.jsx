import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnTimeTest.jsx', () => {
  it('renders Column Time component (editor)', () => {
    renderWithProviders(Columns({
      component: 'time',
      model: {values: []},
      numberFormat: {},
      address: {component: 'time', view: 'report', column: 'column', row: 'row'},
      settings: {language: 'en'}
    }, {value: "12:34:56"}, true));

    // PrimeReact Calendar root element should be present
    expect(document.querySelector('.p-calendar')).not.toBeNull();
  });
});
