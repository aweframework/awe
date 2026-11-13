import {Columns} from "../../../src/utilities/structure";
import {fireEvent, waitFor} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";

describe('awe-react-client/test/js/columns/ColumnColorTest.jsx', () => {

  const renderColumn = () => renderWithProviders(Columns({
      component: 'color',
      model: {values: []},
      numberFormat: {},
      updateModelWithDependencies: jest.fn(),
      updateAttributes: jest.fn(),
      addActionsTop: jest.fn(),
      address: {component: 'color', view: 'report', column: 'column', row: 'row'},
      t: jest.fn(),
      settings: {}
    }, {value: "test"}, {},true));

  it('renders Column Color component', () => {
    renderColumn();

    // fails
    expect(document.querySelector("button")).not.toBeNull();
  });

  it('keeps the picker inside the viewport when the swatch is at its right edge', async () => {
    // As in the browser, the picker has no width yet when the panel starts to open, and its final width afterwards
    const PICKER_WIDTH = 220;
    let pickerReads = 0;
    const offsetWidth = jest.spyOn(HTMLElement.prototype, "offsetWidth", "get").mockImplementation(function () {
      if (this.classList.contains("p-overlaypanel")) {
        return pickerReads++ === 0 ? 0 : PICKER_WIDTH;
      }
      return this.classList.contains("colorpicker") ? 24 : 0;
    });
    renderColumn();
    const swatch = document.querySelector("button.colorpicker");
    swatch.getBoundingClientRect = () => ({left: window.innerWidth - 24, top: 10, right: window.innerWidth, bottom: 34, width: 24, height: 24});

    fireEvent.click(swatch);

    await waitFor(() => {
      const panel = document.querySelector(".p-overlaypanel");
      expect(parseFloat(panel.style.left) + PICKER_WIDTH).toBeLessThanOrEqual(window.innerWidth);
    });
    offsetWidth.mockRestore();
  });
});
