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

  it('cancels Enter so the ancestors do not receive it', () => {
    const onParentKeyDown = jest.fn();
    const onWindowKeyDown = jest.fn();
    window.addEventListener("keydown", onWindowKeyDown);
    const {dispatchSpy} = renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputPassword id="password"/></div>, {preloadedState, spyDispatch: true});
    const input = document.querySelector("input#password");

    const notPrevented = fireEvent.keyDown(input, {key: "Enter"});
    window.removeEventListener("keydown", onWindowKeyDown);

    expect(notPrevented).toBe(false);
    expect(onParentKeyDown).not.toHaveBeenCalled();
    expect(onWindowKeyDown).not.toHaveBeenCalled();
    expect(submitActions(dispatchSpy)).toEqual([expect.objectContaining({type: "submit", address: preloadedState.components.password.address})]);
  });

  it('lets another key reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputPassword id="password"/></div>, {preloadedState});

    expect(fireEvent.keyDown(document.querySelector("input#password"), {key: "a"})).toBe(true);
    expect(onParentKeyDown).toHaveBeenCalledTimes(1);
  });

  describe('show / hide button (#793 S17)', () => {
    const input = () => document.querySelector("input#password");
    const toggle = () => document.querySelector("button.password-toggle");
    const withAttributes = attributes => ({...preloadedState, components: {password: {
      ...preloadedState.components.password, attributes: {...preloadedState.components.password.attributes, ...attributes}}}});

    it('draws the eye button at the end of the field, controlling the input', () => {
      renderWithProviders(<AweInputPassword id="password"/>, {preloadedState});

      expect(document.querySelectorAll("button.password-toggle")).toHaveLength(1);
      expect(toggle().getAttribute("type")).toBe("button");
      expect(toggle().getAttribute("aria-controls")).toBe("password");
      expect(toggle().getAttribute("aria-pressed")).toBe("false");
      expect(toggle().getAttribute("aria-label")).toBe("SCREEN_TEXT_SHOW_PASSWORD");
      expect(toggle().querySelector("svg use").getAttribute("href")).toMatch(/#eye$/);
      expect(toggle().disabled).toBe(false);
      expect(toggle().classList.contains("signin-password-toggle")).toBe(false);
      expect(input().nextElementSibling).toBe(toggle());
      expect(input().classList.contains("password-field")).toBe(true);
    });

    it('shows and hides the password keeping its value', () => {
      renderWithProviders(<AweInputPassword id="password"/>, {preloadedState});
      expect(input().getAttribute("type")).toBe("password");
      expect(input().value).toBe("test");

      fireEvent.click(toggle());

      expect(input().getAttribute("type")).toBe("text");
      expect(input().value).toBe("test");
      expect(toggle().getAttribute("aria-pressed")).toBe("true");
      expect(toggle().getAttribute("aria-label")).toBe("SCREEN_TEXT_HIDE_PASSWORD");
      expect(toggle().querySelector("svg use").getAttribute("href")).toMatch(/#eye-off$/);

      fireEvent.click(toggle());

      expect(input().getAttribute("type")).toBe("password");
      expect(input().value).toBe("test");
      expect(toggle().getAttribute("aria-pressed")).toBe("false");
      expect(toggle().getAttribute("aria-label")).toBe("SCREEN_TEXT_SHOW_PASSWORD");
    });

    it('does not change the model nor submit when it is clicked', () => {
      const {dispatchSpy, store} = renderWithProviders(<AweInputPassword id="password"/>, {preloadedState, spyDispatch: true});
      const model = store.getState().components.password.model;

      fireEvent.click(toggle());

      expect(store.getState().components.password.model).toEqual(model);
      expect(submitActions(dispatchSpy)).toEqual([]);
    });

    it('is disabled when the criterion is readonly', () => {
      renderWithProviders(<AweInputPassword id="password"/>, {preloadedState: withAttributes({readonly: true})});

      expect(toggle().disabled).toBe(true);
      expect(input().disabled).toBe(true);
    });
  });

});
