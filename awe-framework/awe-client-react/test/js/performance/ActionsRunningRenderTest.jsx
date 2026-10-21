import React from "react";
import {act} from "@testing-library/react";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {ACCEPT_ACTION, ADD_ACTIONS} from "../../../src/redux/actions/actions";
import {ButtonTypes} from "../../../src/redux/actions/components";
import {renderWithProviders} from "../test-utils";
import AweButton from "../../../src/components/AweButton";
import AweInfoButton from "../../../src/components/AweInfoButton";
import AweTabs from "../../../src/components/AweTabs";
import AweSteps from "../../../src/components/AweSteps";
import {useTranslation} from "react-i18next";

// Count the renders of the panel content shown by tabs and steps
const contentRenders = {count: 0};
jest.mock("../../../src/utilities/structure", () => {
  const ReactMock = require("react");
  const Content = () => {
    contentRenders.count++;
    return ReactMock.createElement("div", {className: "panel-content"});
  };
  return {
    Components: (node, index) => ReactMock.createElement(Content, {key: index})
  };
});

// Button bodies call useTranslation once per render
jest.mock("react-i18next", () => {
  const actual = jest.requireActual("react-i18next");
  return {...actual, useTranslation: jest.fn(actual.useTranslation)};
});

/**
 * Render count tests of the actions running flag: a running/idle transition (a row selection runs a non silent action)
 * only renders the controls that it disables, not the body of the component nor the panels it shows
 */
describe("awe-react-client/test/js/performance/ActionsRunningRenderTest.jsx", () => {
  const address = (component) => ({component, view: "report"});
  const panelable = (id, elementType) => ({
    address: address(id),
    model: {values: [{label: "one", value: "one", selected: true}, {label: "two", value: "two"}]},
    attributes: {},
    elementList: [{elementType, id: id + "One"}, {elementType, id: id + "Two"}]
  });

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    actions: {running: false, sync: [[]], async: []},
    components: {
      button: {address: address("button"), attributes: {label: "Button", visible: true, buttonType: ButtonTypes.BUTTON_NORMAL}, actions: []},
      infoButton: {address: address("infoButton"), attributes: {label: "Info", visible: true}, actions: []},
      tabs: panelable("tabs", "TabContainer"),
      steps: panelable("steps", "WizardPanel")
    }
  };

  const selectRow = (store) => {
    act(() => {
      store.dispatch({type: ADD_ACTIONS, payload: [{type: "select-row", address: address("grid")}]});
    });
    const running = store.getState().actions.running;
    act(() => {
      const [action] = store.getState().actions.sync[0];
      store.dispatch({type: ACCEPT_ACTION, payload: action});
    });
    return running;
  };

  beforeEach(() => {
    contentRenders.count = 0;
    useTranslation.mockClear();
  });

  it("disables a button while the row selection runs without rendering the button body again", () => {
    const {store, container} = renderWithProviders(<AweButton id="button"/>, {preloadedState});
    const button = container.querySelector("#button");
    useTranslation.mockClear();

    act(() => {
      store.dispatch({type: ADD_ACTIONS, payload: [{type: "select-row", address: address("grid")}]});
    });
    expect(button.hasAttribute("disabled")).toBe(true);

    act(() => {
      store.dispatch({type: ACCEPT_ACTION, payload: store.getState().actions.sync[0][0]});
    });
    expect(button.hasAttribute("disabled")).toBe(false);
    expect(useTranslation).not.toHaveBeenCalled();
  });

  it("does not render an info button body again on a row selection", () => {
    const {store, container} = renderWithProviders(<AweInfoButton id="infoButton"/>, {preloadedState});
    useTranslation.mockClear();

    expect(selectRow(store)).toBe(true);

    expect(useTranslation).not.toHaveBeenCalled();
    expect(container.querySelector("#infoButton").hasAttribute("disabled")).toBe(false);
  });

  it("disables the tabs while the row selection runs without rendering the tab content again", () => {
    const {store, container} = renderWithProviders(<AweTabs id="tabs" elementList={preloadedState.components.tabs.elementList}/>,
      {preloadedState});
    expect(contentRenders.count).toBe(1);

    act(() => {
      store.dispatch({type: ADD_ACTIONS, payload: [{type: "select-row", address: address("grid")}]});
    });
    expect(container.querySelectorAll(".p-tabmenuitem.p-disabled").length).toBe(2);

    act(() => {
      store.dispatch({type: ACCEPT_ACTION, payload: store.getState().actions.sync[0][0]});
    });
    expect(container.querySelectorAll(".p-tabmenuitem.p-disabled").length).toBe(0);
    expect(contentRenders.count).toBe(1);
  });

  it("does not render the wizard panel again on a row selection", () => {
    const {store} = renderWithProviders(<AweSteps id="steps" elementList={preloadedState.components.steps.elementList}/>,
      {preloadedState});
    expect(contentRenders.count).toBe(1);

    expect(selectRow(store)).toBe(true);

    expect(contentRenders.count).toBe(1);
  });
});
