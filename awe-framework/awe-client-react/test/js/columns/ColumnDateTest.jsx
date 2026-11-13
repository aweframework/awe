import React from "react";
import {act, cleanup, fireEvent} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {updateModelWithDependencies} from "../../../src/redux/thunks/components";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

jest.mock("../../../src/redux/thunks/components", () => ({
  ...jest.requireActual("../../../src/redux/thunks/components"),
  updateModelWithDependencies: jest.fn((address, data) => ({type: "TEST_UPDATE", address, data}))
}));

describe('awe-react-client/test/js/columns/ColumnDateTest.jsx', () => {

  afterEach(cleanup);

  const renderColumn = (options = {}, value = "15/03/2024") => {
    const rendered = renderWithProviders(Columns({
      component: 'date',
      model: {values: []},
      address: {component: 'grid', view: 'report', column: 'date', row: '1'},
      ...options
    }, {value}, {}, true), {preloadedState: {settings: DEFAULT_SETTINGS}, spyDispatch: true});
    const input = document.querySelector('input');
    fireEvent.click(input);
    return {...rendered, input};
  };

  it('shows the default format dd/mm/yyyy', () => {
    const {input} = renderColumn();
    expect(input.value).toBe("15/03/2024");
  });

  it('shows the date with the date-format of the column', () => {
    const {input} = renderColumn({dateFormat: "yyyy-mm-dd"}, "2024-03-15");
    expect(input.value).toBe("2024-03-15");
  });

  it('sends the selected date with the date-format of the column', () => {
    const {dispatchSpy} = renderColumn({dateFormat: "yyyy-mm-dd"}, "2024-03-15");
    fireEvent.click(document.querySelector('.p-datepicker-calendar td[data-p-day="20"]:not(.p-datepicker-other-month) > span'));
    expect(updateModelWithDependencies).toHaveBeenCalledWith(expect.anything(), {values: "2024-03-20"});
    expect(dispatchSpy).toHaveBeenCalled();
  });

  it('shows months only with date-view-mode months', () => {
    renderColumn({dateFormat: "mm/yyyy", dateViewMode: "months"}, "03/2024");
    expect(document.querySelector('.p-datepicker-calendar')).toBeNull();
    expect(document.querySelectorAll('.p-monthpicker-month').length).toBe(12);
  });

  it('disables the weekends with show-weekends false', () => {
    renderColumn({showWeekends: false});
    const disabled = Array.from(document.querySelectorAll('.p-datepicker-calendar td:not(.p-datepicker-other-month) > span.p-disabled'))
      .map(day => day.textContent);
    expect(disabled).toEqual(expect.arrayContaining(["16", "17"]));
    expect(disabled).not.toContain("18");
  });

  it('hides the today button with date-show-today-button false', () => {
    renderColumn({showTodayButton: false});
    expect(document.querySelectorAll('.p-datepicker-buttonbar .p-button.hidden').length).toBe(1);
  });

  it('keeps the focus on the cell input when its calendar opens, so the date can be typed', async () => {
    renderWithProviders(Columns({
      component: 'date',
      model: {values: []},
      address: {component: 'grid', view: 'report', column: 'date', row: '1'}
    }, {value: "15/03/2024"}, {}, true), {preloadedState: {settings: DEFAULT_SETTINGS}});
    const input = document.querySelector('input');

    fireEvent.mouseDown(input);
    act(() => input.focus());
    fireEvent.mouseUp(input);
    fireEvent.click(input);
    await act(() => new Promise(resolve => setTimeout(resolve, 300)));

    expect(document.querySelector('[data-testid="datepicker"]')).not.toBeNull();
    expect(document.activeElement).toBe(input);
  });

  describe('inside a grid that saves the row with Enter', () => {
    const renderInGrid = () => {
      const onGridKeyDown = jest.fn();
      const rendered = renderWithProviders(<div onKeyDown={onGridKeyDown}>{Columns({
        component: 'date',
        model: {values: []},
        address: {component: 'grid', view: 'report', column: 'date', row: '1'}
      }, {value: "15/03/2024"}, {}, true)}</div>, {preloadedState: {settings: DEFAULT_SETTINGS}, spyDispatch: true});
      fireEvent.click(document.querySelector('input'));
      return {...rendered, onGridKeyDown};
    };

    it('lets Enter in the cell input reach the grid, which saves the row', () => {
      const {onGridKeyDown} = renderInGrid();

      fireEvent.keyDown(document.querySelector('input'), {key: "Enter", code: "Enter"});

      expect(onGridKeyDown).toHaveBeenCalledTimes(1);
    });

    it('keeps Enter on a day of the calendar from reaching the grid, and selects the day', () => {
      const {onGridKeyDown, dispatchSpy} = renderInGrid();
      const day = document.querySelector('[data-testid="datepicker-day"][data-selected="false"][data-disabled="false"][data-outside-month="false"]');

      fireEvent.keyDown(day.querySelector("span"), {key: "Enter", code: "Enter"});

      expect(onGridKeyDown).not.toHaveBeenCalled();
      expect(updateModelWithDependencies).toHaveBeenCalled();
    });
  });

});
