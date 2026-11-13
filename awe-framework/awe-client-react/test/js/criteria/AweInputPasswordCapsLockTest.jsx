import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputPassword from "../../../src/criteria/AweInputPassword";
import {act, cleanup, fireEvent} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputPasswordCapsLockTest.jsx', () => {

  afterEach(() => {
    cleanup();
    act(() => {
      fireEvent.keyUp(document, {key: "CapsLock", modifierCapsLock: false});
    });
  });

  const render = () => renderWithProviders(<AweInputPassword id="password"/>, {
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      components: {
        password: {
          address: {component: 'password', view: 'report'},
          model: {values: []},
          attributes: {label: "Password", readonly: false, visible: true},
          validationRules: {}
        }
      }
    }
  });

  const getInput = () => document.querySelector("input#password");
  const getWarning = () => document.querySelector(".caps-lock-icon");

  it('does not warn when the caps lock is off', () => {
    render();
    fireEvent.keyDown(getInput(), {key: "a", modifierCapsLock: false});
    expect(getWarning()).toBeNull();
    expect(getInput().classList.contains("caps-lock")).toBe(false);
  });

  it('warns while the caps lock is on', () => {
    render();
    fireEvent.keyDown(getInput(), {key: "A", modifierCapsLock: true});
    expect(getWarning()).not.toBeNull();
    expect(getInput().classList.contains("caps-lock")).toBe(true);
  });

  it('stops warning when the caps lock is switched off', () => {
    render();
    fireEvent.keyDown(getInput(), {key: "A", modifierCapsLock: true});
    fireEvent.keyUp(getInput(), {key: "CapsLock", modifierCapsLock: false});
    expect(getWarning()).toBeNull();
  });

  it('warns also when the caps lock was switched on outside of the criterion', () => {
    render();
    fireEvent.keyDown(document.body, {key: "A", modifierCapsLock: true});
    expect(getWarning()).not.toBeNull();
  });
});
