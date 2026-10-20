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

    fireEvent.keyPress(input, {key: "Enter", charCode: 13});

    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.color.address})]);
  });

  it('does not submit the screen with another key', () => {
    const {dispatchSpy} = renderWithProviders(<AweInputColor id="color"/>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#color");

    fireEvent.keyPress(input, {key: "a", charCode: 97});

    expect(submitActions(dispatchSpy)).toEqual([]);
  });

});
