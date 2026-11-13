import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputTime from "../../../src/criteria/AweInputTime";
import {cleanup, fireEvent, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputTimeTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      time: {
        address: {component: 'time', view: 'report'},
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

  it('renders Awe Input Time component', async () => {
    renderWithProviders(<AweInputTime id="time"/>, {preloadedState});

    // check
    expect(await screen.findByRole("combobox")).not.toBeNull();
  });


  const submitActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");
  const modelValue = (store) => store.getState().components.time.model.values.filter(item => item.selected).map(item => item.value);

  it('commits the typed value and submits the screen with Enter', () => {
    const {store, dispatchSpy} = renderWithProviders(<AweInputTime id="time"/>, {preloadedState, spyDispatch: true});
    const input = screen.getByRole("combobox");

    fireEvent.input(input, {target: {value: "10:20:30"}});
    const notPrevented = fireEvent.keyDown(input, {key: "Enter"});

    expect(notPrevented).toBe(false);
    expect(modelValue(store)).toEqual(["10:20:30"]);
    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.time.address})]);
  });

  it('does not let Enter reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputTime id="time"/></div>, {preloadedState});

    fireEvent.keyDown(screen.getByRole("combobox"), {key: "Enter"});

    expect(onParentKeyDown).not.toHaveBeenCalled();
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputTime id="time"/>, {preloadedState, spyDispatch: true});

    fireEvent.keyDown(screen.getByRole("combobox"), {key: "a"});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

  describe('time-format', () => {
    const stateWith = (timeFormat, value) => ({
      settings: DEFAULT_SETTINGS,
      components: {
        time: {
          address: {component: 'time', view: 'report'},
          model: {values: value ? [{label: value, value, selected: true}] : []},
          attributes: {readonly: false, ...(timeFormat ? {timeFormat} : {})},
          validationRules: {}
        }
      }
    });
    const typed = (timeFormat, text) => {
      const {store} = renderWithProviders(<AweInputTime id="time"/>, {preloadedState: stateWith(timeFormat)});
      fireEvent.input(screen.getByRole("combobox"), {target: {value: text}});
      return modelValue(store);
    };
    const shown = (timeFormat, value) => {
      renderWithProviders(<AweInputTime id="time"/>, {preloadedState: stateWith(timeFormat, value)});
      return screen.getByRole("combobox").value;
    };

    afterEach(cleanup);

    it('shows seconds and 24 hours by default', () => {
      expect(shown(undefined, "14:30:15")).toBe("14:30:15");
    });

    it('hides the seconds with HH:mm and sends the time without them', () => {
      expect(shown("HH:mm", "14:30")).toBe("14:30");
      cleanup();
      expect(typed("HH:mm", "10:20")).toEqual(["10:20"]);
    });

    it('keeps the seconds with HH:mm:ss', () => {
      expect(typed("HH:mm:ss", "10:20:30")).toEqual(["10:20:30"]);
    });

    it('shows a 12-hour clock with hh:mm a and sends 24 hours', () => {
      expect(shown("hh:mm a", "14:30")).toBe("02:30 PM");
      cleanup();
      expect(typed("hh:mm a", "02:30 PM")).toEqual(["14:30"]);
    });

    it('shows a 12-hour clock with seconds with hh:mm:ss a and sends 24 hours', () => {
      expect(shown("hh:mm:ss a", "14:30:15")).toBe("02:30:15 PM");
      cleanup();
      expect(typed("hh:mm:ss a", "02:30:15 PM")).toEqual(["14:30:15"]);
    });
  });

});
