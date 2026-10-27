import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputDateRange from "../../../src/criteria/AweInputDateRange";
import {act, cleanup, fireEvent, screen, within} from "@testing-library/react";
import {updateModelWithDependencies} from "../../../src/redux/thunks/components";
import i18n from "i18next";
import {CriteriaList} from "../../../src/criteria/AweCriteria";

describe('awe-react-client/test/js/criteria/AweInputDateRangeTest.jsx', () => {
  const address = {component: 'period', view: 'report'};
  const FROM = "date-range-from";
  const TO = "date-range-to";
  const PRESET = "date-range-preset";

  const stateWith = (values, attributes = {}) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      period: {
        address,
        model: {values},
        attributes: {id: "period", component: "date-range", label: "Period", readonly: false, ...attributes},
        validationRules: {required: false},
        specificAttributes: {sort: []}
      }
    }
  });
  const range = (from, to) => [{value: from, label: from, selected: true}, {value: to, label: to, selected: true}];
  const byTestId = (testId) => document.querySelectorAll(`[data-testid="${testId}"]`);
  const presetButton = (code) => document.querySelector(`[data-testid="${PRESET}"][option-id="${code}"]`);
  const modelOf = (store) => store.getState().components.period.model.values.map(item => item.value);

  beforeAll(() => {
    i18n.addResources("en-GB", "translation", {DATE_RANGE_PRESET_YTD: "Year to date"});
  });

  beforeEach(() => {
    // Only the date is faked, timers and microtasks keep running
    jest.useFakeTimers({
      now: new Date(2026, 9, 12),
      doNotFake: ["setTimeout", "clearTimeout", "setInterval", "clearInterval", "setImmediate", "clearImmediate",
        "nextTick", "queueMicrotask", "requestAnimationFrame", "cancelAnimationFrame", "performance"]
    });
  });

  afterEach(() => {
    jest.useRealTimers();
    cleanup();
  });

  it('renders a start and an end input with their dates', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("01/01/2026", "31/01/2026"))});

    expect(byTestId(FROM)).toHaveLength(1);
    expect(byTestId(TO)).toHaveLength(1);
    expect(byTestId(FROM)[0].value).toBe("01/01/2026");
    expect(byTestId(TO)[0].value).toBe("31/01/2026");
  });

  it('renders empty inputs when the model has no dates', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([])});

    expect([byTestId(FROM)[0].value, byTestId(TO)[0].value]).toEqual(["", ""]);
  });

  it('renders a button for each configured preset, translated', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "1M, YTD, all"})});

    expect([...byTestId(PRESET)].map(button => button.getAttribute("option-id"))).toEqual(["1M", "YTD", "ALL"]);
    expect(within(presetButton("YTD")).getByText("Year to date")).not.toBeNull();
  });

  it('renders no presets when none is configured', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([])});

    expect(byTestId(PRESET)).toHaveLength(0);
  });

  it('shows the code of a preset without translation', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "45D"})});

    expect(within(presetButton("45D")).getByText("45D")).not.toBeNull();
  });

  it('ignores an unknown preset', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "1M,FOREVER"})});

    expect([...byTestId(PRESET)].map(button => button.getAttribute("option-id"))).toEqual(["1M"]);
  });

  it('fills both dates from the current date when a preset is clicked', () => {
    const {store} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "1M,YTD,ALL"})});

    fireEvent.click(presetButton("1M"));

    expect(modelOf(store)).toEqual(["12/09/2026", "12/10/2026"]);
    expect(byTestId(FROM)[0].value).toBe("12/09/2026");
    expect(byTestId(TO)[0].value).toBe("12/10/2026");
  });

  it('clears both dates with the all preset', () => {
    const {store} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("01/01/2026", "31/01/2026"), {presets: "ALL"})});

    fireEvent.click(presetButton("ALL"));

    expect(modelOf(store)).toEqual(["", ""]);
  });

  it('marks the preset applied as active', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "1M,YTD"})});
    expect(presetButton("1M").getAttribute("data-active")).toBe("false");

    fireEvent.click(presetButton("1M"));

    expect(presetButton("1M").getAttribute("data-active")).toBe("true");
    expect(presetButton("YTD").getAttribute("data-active")).toBe("false");
  });

  it('clears the active preset when a date is edited', () => {
    const {store} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "1M"})});
    fireEvent.click(presetButton("1M"));

    act(() => {
      store.dispatch(updateModelWithDependencies(address, {values: range("15/09/2026", "12/10/2026")}));
    });

    expect(presetButton("1M").getAttribute("data-active")).toBe("false");
  });

  it('updates the start of the range when the start input is changed', () => {
    const {store} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("", "31/01/2026"))});

    fireEvent.input(byTestId(FROM)[0], {target: {value: "05/01/2026"}});

    expect(modelOf(store)).toEqual(["05/01/2026", "31/01/2026"]);
  });

  it('updates the end of the range when the end input is changed', () => {
    const {store} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("01/01/2026", ""))});

    fireEvent.input(byTestId(TO)[0], {target: {value: "20/01/2026"}});

    expect(modelOf(store)).toEqual(["01/01/2026", "20/01/2026"]);
  });

  it('disables the inputs and the presets when it is readonly', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([], {presets: "1M", readonly: true})});

    expect(byTestId(FROM)[0].disabled).toBe(true);
    expect(presetButton("1M").disabled).toBe(true);
  });

  it('shows the validation error', () => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("15/01/2026", "14/01/2026"), {error: {message: "VALIDATOR_MESSAGE_DATE_RANGE"}})});

    expect(screen.getByText("VALIDATOR_MESSAGE_DATE_RANGE")).not.toBeNull();
  });

  it('is registered as the date-range criterion', () => {
    expect(CriteriaList["date-range"]).toBe(AweInputDateRange);
  });

  const submitActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");

  it.each([[FROM, "05/01/2026", ["05/01/2026", "31/01/2026"]], [TO, "20/01/2026", ["01/01/2026", "20/01/2026"]]])(
    'commits the typed value of the %s input and submits the screen with Enter', (testId, typed, expected) => {
      const {store, dispatchSpy} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("01/01/2026", "31/01/2026")), spyDispatch: true});
      const input = byTestId(testId)[0];

      fireEvent.input(input, {target: {value: typed}});
      const notPrevented = fireEvent.keyDown(input, {key: "Enter"});

      expect(notPrevented).toBe(false);
      expect(modelOf(store)).toEqual(expected);
      expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address})]);
    });

  it.each([[FROM], [TO]])('keeps the focus on the %s input when its calendar opens, so the date can be typed', async (testId) => {
    renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith([])});
    const input = byTestId(testId)[0];

    fireEvent.mouseDown(input);
    act(() => input.focus());
    fireEvent.mouseUp(input);
    fireEvent.click(input);
    await act(() => new Promise(resolve => setTimeout(resolve, 300)));

    expect(document.querySelector('[data-testid="datepicker"]')).not.toBeNull();
    expect(document.activeElement).toBe(input);
  });

  it('does not let Enter reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputDateRange id="period"/></div>, {preloadedState: stateWith(range("", ""))});

    fireEvent.keyDown(byTestId(FROM)[0], {key: "Enter"});
    fireEvent.keyDown(byTestId(TO)[0], {key: "Enter"});

    expect(onParentKeyDown).not.toHaveBeenCalled();
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("", "")), spyDispatch: true});

    fireEvent.keyDown(byTestId(FROM)[0], {key: "a"});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

  it('selects a day with Enter in the calendar without submitting the screen', () => {
    const {store, dispatchSpy} = renderWithProviders(<AweInputDateRange id="period"/>, {preloadedState: stateWith(range("01/01/2026", "")), spyDispatch: true});
    fireEvent.click(byTestId(TO)[0]);

    const day = document.querySelector('[data-testid="datepicker-day"][data-selected="false"][data-disabled="false"][data-outside-month="false"]');
    expect(day).not.toBeNull();
    fireEvent.keyDown(day.querySelector("span"), {key: "Enter", code: "Enter"});

    expect(modelOf(store)[1]).not.toBe("");
    expect(submitActions(dispatchSpy)).toEqual([]);
  });

});
