import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputDate from "../../../src/criteria/AweInputDate";
import {cleanup, fireEvent, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputDateOptionsTest.jsx', () => {

  afterEach(cleanup);

  const stateWith = (attributes = {}, value = "15/03/2024") => ({
    settings: DEFAULT_SETTINGS,
    components: {
      date: {
        address: {component: 'date', view: 'report'},
        model: {values: value ? [{label: value, value, selected: true}] : []},
        attributes: {readonly: false, ...attributes},
        validationRules: {}
      }
    }
  });

  const open = async (attributes, value) => {
    const rendered = renderWithProviders(<AweInputDate id="date"/>, {preloadedState: stateWith(attributes, value)});
    const input = await screen.findByRole("combobox");
    fireEvent.click(input);
    return {...rendered, input};
  };

  const getModelValue = (store) => store.getState().components.date.model.values[0]?.value;

  it('shows the default format dd/mm/yyyy', async () => {
    const {input} = await open({});
    expect(input.value).toBe("15/03/2024");
  });

  it('shows the date with the date-format of the contract', async () => {
    const {input} = await open({dateFormat: "yyyy-mm-dd"}, "2024-03-15");
    expect(input.value).toBe("2024-03-15");
  });

  it('sends the selected date with the date-format', async () => {
    const {input, store} = await open({dateFormat: "yyyy-mm-dd"}, "2024-03-15");
    const day = document.querySelector('.p-datepicker-calendar td[data-p-day="20"]:not(.p-datepicker-other-month) > span');
    fireEvent.click(day);
    expect(input.value).toBe("2024-03-20");
    expect(getModelValue(store)).toBe("2024-03-20");
  });

  it('shows months only with date-view-mode months and keeps the format', async () => {
    const {store} = await open({dateFormat: "mm/yyyy", dateViewMode: "months"}, "03/2024");
    expect(document.querySelector('.p-datepicker-calendar')).toBeNull();
    const months = document.querySelectorAll('.p-monthpicker-month');
    expect(months.length).toBe(12);
    fireEvent.click(months[4]);
    expect(getModelValue(store)).toBe("05/2024");
  });

  it('shows years only with date-view-mode years', async () => {
    const {store} = await open({dateFormat: "yyyy", dateViewMode: "years"}, "2024");
    expect(document.querySelectorAll('.p-yearpicker-year').length).toBeGreaterThan(0);
    const year = Array.from(document.querySelectorAll('.p-yearpicker-year')).find(y => y.textContent === "2022");
    fireEvent.click(year);
    expect(getModelValue(store)).toBe("2022");
  });

  it('disables the weekends with show-weekends false', async () => {
    await open({showWeekends: false});
    // 16/03/2024 is Saturday, 17/03/2024 Sunday and 18/03/2024 Monday
    const disabled = Array.from(document.querySelectorAll('.p-datepicker-calendar td:not(.p-datepicker-other-month) > span.p-disabled')).map(td => td.textContent);
    expect(disabled).toEqual(expect.arrayContaining(["16", "17"]));
    expect(disabled).not.toContain("18");
  });

  it('keeps the weekends enabled by default', async () => {
    await open({});
    expect(document.querySelectorAll('.p-datepicker-calendar td:not(.p-datepicker-other-month) > span.p-disabled').length).toBe(0);
  });

  it('disables the dates after the selected one with show-future-dates false', async () => {
    await open({showFutureDates: false});
    const disabled = Array.from(document.querySelectorAll('.p-datepicker-calendar td:not(.p-datepicker-other-month) > span.p-disabled'))
      .map(td => td.textContent);
    expect(disabled).toContain("16");
    expect(disabled).toContain("31");
    expect(disabled).not.toContain("15");
    expect(disabled).not.toContain("14");
  });

  it('does not limit the dates with show-future-dates false and no value', async () => {
    await open({showFutureDates: false}, null);
    expect(document.querySelectorAll('.p-datepicker-calendar td:not(.p-datepicker-other-month) > span.p-disabled').length).toBe(0);
  });

  it('shows the today button by default', async () => {
    await open({});
    expect(document.querySelectorAll('.p-datepicker-buttonbar .p-button').length).toBe(2);
    expect(document.querySelectorAll('.p-datepicker-buttonbar .p-button.hidden').length).toBe(0);
    expect(document.querySelectorAll('.p-datepicker-buttonbar .p-button.p-button-secondary').length).toBe(2);
  });

  it('hides the today button with date-show-today-button false', async () => {
    await open({showTodayButton: false});
    expect(document.querySelectorAll('.p-datepicker-buttonbar .p-button.hidden').length).toBe(1);
  });
});
