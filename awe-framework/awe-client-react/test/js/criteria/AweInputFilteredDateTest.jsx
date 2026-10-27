import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputFilteredDate from "../../../src/criteria/AweInputFilteredDate";
import {cleanup, fireEvent, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputFilteredDateTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      filteredDate: {
        address: {component: 'filteredDate', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          helpImage: "test"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Input FilteredDate component', async () => {
    renderWithProviders(<AweInputFilteredDate id="filteredDate"/>, {preloadedState});

    // check
    expect(await screen.findByRole("combobox")).not.toBeNull();
  });


  const submitActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");
  const modelValue = (store) => store.getState().components.filteredDate.model.values.filter(item => item.selected).map(item => item.value);

  it('commits the typed value and submits the screen with Enter', () => {
    const {store, dispatchSpy} = renderWithProviders(<AweInputFilteredDate id="filteredDate"/>, {preloadedState, spyDispatch: true});
    const input = screen.getByRole("combobox");

    fireEvent.input(input, {target: {value: "15/03/2026"}});
    const notPrevented = fireEvent.keyDown(input, {key: "Enter"});

    expect(notPrevented).toBe(false);
    expect(modelValue(store)).toEqual(["15/03/2026"]);
    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.filteredDate.address})]);
  });

  it('does not let Enter reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputFilteredDate id="filteredDate"/></div>, {preloadedState});

    fireEvent.keyDown(screen.getByRole("combobox"), {key: "Enter"});

    expect(onParentKeyDown).not.toHaveBeenCalled();
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputFilteredDate id="filteredDate"/>, {preloadedState, spyDispatch: true});

    fireEvent.keyDown(screen.getByRole("combobox"), {key: "a"});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

  it('selects a day with Enter in the calendar without submitting the screen', () => {
    const {store, dispatchSpy} = renderWithProviders(<AweInputFilteredDate id="filteredDate"/>, {preloadedState, spyDispatch: true});
    fireEvent.input(screen.getByRole("combobox"), {target: {value: "15/03/2026"}});
    fireEvent.click(screen.getByRole("combobox"));

    const day = document.querySelector('[data-testid="datepicker-day"][data-selected="false"][data-disabled="false"][data-outside-month="false"]');
    expect(day).not.toBeNull();
    fireEvent.keyDown(day.querySelector("span"), {key: "Enter", code: "Enter"});

    expect(modelValue(store)).not.toEqual(["15/03/2026"]);
    expect(submitActions(dispatchSpy)).toEqual([]);
  });

});
