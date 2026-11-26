import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {render} from "@testing-library/react";
import {getIconNode} from "../../../src/utilities";

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

  // The clear icon is the Lucide x of the suggest (#997), and keeps the keyboard access and the label of PrimeReact
  const expectLucideClearIcon = (clearIcon) => {
    const lucideX = render(getIconNode("lucide:x", {"aria-hidden": "true"})).container.innerHTML;
    expect(clearIcon).not.toBeNull();
    expect(clearIcon.innerHTML).toBe(lucideX);
    expect(clearIcon.getAttribute("tabindex")).toBe("0");
    expect(clearIcon.getAttribute("aria-label")).toBeTruthy();
  };

  it('clears an optional select column with the Lucide x', () => {
    const {container} = renderWithProviders(Columns({
      component: 'select',
      optional: true,
      model: {values: [
        {label: 'Option 1', value: '1'},
        {label: 'Option 2', value: '2'}
      ]},
      numberFormat: {},
      address: {component: 'select', view: 'report', column: 'column', row: 'row'},
      settings: {}
    }, {value: '1'}, {}, true));

    expectLucideClearIcon(container.querySelector(".p-dropdown-clear-icon"));
  });
});
