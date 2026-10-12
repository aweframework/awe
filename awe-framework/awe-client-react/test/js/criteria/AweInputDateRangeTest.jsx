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
});
