import React from "react";
import {cleanup, fireEvent} from "@testing-library/react";
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
});
