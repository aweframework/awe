import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputPassword from "../../../src/criteria/AweInputPassword";

describe('awe-react-client/test/js/criteria/AweInputPasswordTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      password: {
        address: {component: 'password', view: 'report'},
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

  it('renders Awe Input Password component', () => {
    renderWithProviders(<AweInputPassword id="password"/>, {preloadedState});

    // check
    expect(document.querySelector("input#password")).not.toBeNull();
  });

  const submitActions = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === "ADD_ACTIONS_TOP")
    .flatMap(action => action.payload)
    .filter(action => action.type === "submit");

  it('submits the screen with Enter', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputPassword id="password"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#password");

    fireEvent.keyDown(input, {key: "Enter"});

    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.password.address})]);
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputPassword id="password"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#password");

    fireEvent.keyDown(input, {key: "a"});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

});
