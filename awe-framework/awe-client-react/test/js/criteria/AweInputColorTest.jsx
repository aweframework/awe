import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputColor from "../../../src/criteria/AweInputColor";

describe('awe-react-client/test/js/criteria/AweInputColorTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      color: {
        address: {component: 'color', view: 'report'},
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

  it('renders Awe Input Color component', () => {
    renderWithProviders(<AweInputColor id="color"/>, {preloadedState});

    // check
    expect(document.querySelector("input#color")).not.toBeNull();
  });

  const submitActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");

  it('submits the screen with Enter', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputColor id="color"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#color");

    fireEvent.keyDown(input, {key: "Enter"});

    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.color.address})]);
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputColor id="color"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#color");

    fireEvent.keyDown(input, {key: "a"});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

  it('cancels Enter so the ancestors do not receive it', () => {
    const onParentKeyDown = jest.fn();
    const onWindowKeyDown = jest.fn();
    window.addEventListener("keydown", onWindowKeyDown);
    const {dispatchSpy} = renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputColor id="color"/></div>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#color");

    const notPrevented = fireEvent.keyDown(input, {key: "Enter"});
    window.removeEventListener("keydown", onWindowKeyDown);

    expect(notPrevented).toBe(false);
    expect(onParentKeyDown).not.toHaveBeenCalled();
    expect(onWindowKeyDown).not.toHaveBeenCalled();
    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.color.address})]);
  });

  it('lets another key reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputColor id="color"/></div>, {preloadedState});

    expect(fireEvent.keyDown(document.querySelector("input#color"), {key: "a"})).toBe(true);
    expect(onParentKeyDown).toHaveBeenCalledTimes(1);
  });

});
