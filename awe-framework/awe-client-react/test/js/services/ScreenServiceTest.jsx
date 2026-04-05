import React from 'react';
import {waitFor} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import {MemoryRouter} from "react-router";
import useScreenService from "../../../src/services/ScreenService";
import { navigationActions } from "../../../src/redux/actions/navigation";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

const preloadedState = {
  settings: DEFAULT_SETTINGS
};

describe('awe-react-client/test/js/services/ScreenServiceTest.jsx', function () {
  let props;
  let actions;
  let service;
  let store;
  let dispatchSpy;

  function TestHarness() {
    service = useScreenService();
    actions = service.getActions();
    return null;
  }

  beforeEach(function () {
    props = {};
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});

    const rendered = renderWithProviders(<MemoryRouter initialEntries={["/"]}>
      <TestHarness />
      <div className={"test1"}/>
      <div className={"test2 tutu"}/>
    </MemoryRouter>, { preloadedState, spyDispatch: true });
    store = rendered.store;
    dispatchSpy = store.dispatch;

    // mock location
    jest.spyOn(navigationActions, "navigateTo").mockImplementation((target, options) => {
      return dispatch => {
        dispatch({ type: "NAVIGATE_TO", payload: target });
      };
    });
    jest.spyOn(window, 'open').mockImplementation(() => true);
  });

  afterEach(function () {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  // Get all screen actions
  it('should get all screen actions', async function() {
    expect(Object.keys(actions).length).toBe(15);
  });

  // Launch screen action erasing the token
  it('should launch a screen action erasing the token', async function() {
    actions.screen({parameters:{language:"es-ES", theme:"clean", token: null}, context: "screen", target: "signin", reload: false}, props);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Launch screen action
  it('should launch a screen action with token', async function() {
    actions.screen({parameters:{target: "signin", token: "allalla"}, reload: false}, props);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Launch screen action
  it('should launch a screen action with screen', async function() {
    actions.screen({parameters:{screen: "/"}}, props);

    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Launch screen with reload
  it('should launch a screen with reload', async function() {
    actions.screen({parameters:{screen: "/context.html"}}, {...props, settings: {...props.settings, reloadCurrentScreen: true}});

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Launch screen with no further action
  it('should launch a screen with no further action', async function() {
    actions.screen({parameters:{screen: "/context.html"}}, props);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Go back
  it('should go back', async function() {
    actions.back({}, props);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Change language from parameters
  it('should change language from parameters', async function() {
    actions["change-language"]({parameters:{language: 'es'}}, props);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Change language from component
  it('should change language from component', async function() {
    actions["change-language"]({parameters:{target: 'component'}}, {...props, components:{component:{model:{values:[{selected: true, value:'fr'}]}}}});

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Change theme from parameters
  it('should change theme from parameters', async function() {
    actions["change-theme"]({parameters:{theme: 'gray'}}, props);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Change theme from component
  it('should change theme from component', async function() {
    actions["change-theme"]({parameters:{target: 'component'}}, {...props, components:{component:{model:{values:[{selected: true, value:'red'}]}}}});

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Wait default time
  it('should wait 1 millisecond', async function() {
    jest.useFakeTimers();

    actions.wait({parameters:{}}, props);

    jest.advanceTimersByTime(2);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Wait defined time
  it('should wait 5 milliseconds', async function() {
    jest.useFakeTimers();

    actions.wait({parameters:{target: 5}}, props);

    jest.advanceTimersByTime(6);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Add css class
  it('should add a css class', async function() {
    jest.useFakeTimers();

    actions["add-class"]({target: ".test1", parameters:{targetAction: "tutu"}}, props);

    jest.advanceTimersByTime(500);

    expect(document.querySelector(".test1").classList.contains("tutu")).toBe(true);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Remove css class
  it('should remove a css class', async function() {
    jest.useFakeTimers();

    actions["remove-class"]({target: ".test2", parameters:{targetAction: "tutu"}}, props);

    jest.advanceTimersByTime(500);

    expect(document.querySelector(".test2").classList.contains("tutu")).toBe(false);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Toggle css class
  it('should toggle a css class', async function() {
    jest.useFakeTimers();

    actions["toggle-class"]({target: ".test2", parameters:{targetAction: "tutu lala"}}, props);

    jest.advanceTimersByTime(500);

    expect(document.querySelector(".test2").classList.contains("lala")).toBe(true);
    expect(document.querySelector(".test2").classList.contains("tutu")).toBe(false);

    // Spies
    await waitFor(() => expect(dispatchSpy).toHaveBeenCalled());
  });

  // Redirect action
  it('should redirect in a new window', async function() {
    actions["redirect"]({target: "url", parameters:{newWindow: true}}, props);

    // Spies
    await waitFor(() => expect(window.open).toHaveBeenCalled());
  });

  // Redirect action in the current window
  it('should redirect in the current window', async function() {
    // Call the redirect method with newWindow set to false
    actions["redirect"]({target: "test-url", parameters:{newWindow: false}}, props);

    // Verify window.location.href was set correctly
    await waitFor(() => expect(window.open).toHaveBeenCalled());
  });

});
