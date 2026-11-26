import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {render} from "@testing-library/react";
import {getIconNode} from "../../../src/utilities";

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

  // The clear icon is the Lucide x of the suggest (#997), and keeps the keyboard access and the label of PrimeReact
  const expectLucideClearIcon = (clearIcon) => {
    const lucideX = render(getIconNode("lucide:x", {"aria-hidden": "true"})).container.innerHTML;
    expect(clearIcon).not.toBeNull();
    expect(clearIcon.innerHTML).toBe(lucideX);
    expect(clearIcon.getAttribute("tabindex")).toBe("0");
    expect(clearIcon.getAttribute("aria-label")).toBeTruthy();
  };

  it('clears an optional select-multiple column with the Lucide x', () => {
    const {container} = renderWithProviders(Columns({
      component: 'select-multiple',
      optional: true,
      model: {values: [
        {label: 'Option 1', value: '1'},
        {label: 'Option 2', value: '2'}
      ]},
      numberFormat: {},
      address: {component: 'select-multiple', view: 'report', column: 'column', row: 'row'},
      settings: {}
    }, [{value: '1', label: 'Option 1', selected: true}, {value: '2', label: 'Option 2'}], {}, true));

    expectLucideClearIcon(container.querySelector(".p-multiselect-clear-icon"));
  });
});
