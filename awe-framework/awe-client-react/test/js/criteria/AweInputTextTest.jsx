import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputText from "../../../src/criteria/AweInputText";

describe('awe-react-client/test/js/criteria/AweInputTextTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      text: {
        address: {component: 'text', view: 'report'},
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

  it('renders Awe Input Text component', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState});

    // check
    expect(document.querySelector("input#text")).not.toBeNull();
  });

  const submitActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");

  it('submits the screen with Enter', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputText id="text"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#text");

    fireEvent.keyDown(input, {key: "Enter"});

    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.text.address})]);
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputText id="text"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#text");

    fireEvent.keyDown(input, {key: "a"});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

  it('cancels Enter so the ancestors do not receive it', () => {
    const onParentKeyDown = jest.fn();
    const onWindowKeyDown = jest.fn();
    window.addEventListener("keydown", onWindowKeyDown);
    const {dispatchSpy} = renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputText id="text"/></div>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#text");

    const notPrevented = fireEvent.keyDown(input, {key: "Enter"});
    window.removeEventListener("keydown", onWindowKeyDown);

    expect(notPrevented).toBe(false);
    expect(onParentKeyDown).not.toHaveBeenCalled();
    expect(onWindowKeyDown).not.toHaveBeenCalled();
    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.text.address})]);
  });

  it('lets another key reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputText id="text"/></div>, {preloadedState});

    expect(fireEvent.keyDown(document.querySelector("input#text"), {key: "a"})).toBe(true);
    expect(onParentKeyDown).toHaveBeenCalledTimes(1);
  });

});
