import React from "react";
import {MemoryRouter} from "react-router";
import {act, renderHook} from "@testing-library/react";
import {Provider} from "react-redux";
import {renderWithProviders} from "../test-utils";
import ActionsContainer from "../../../src/containers/ActionsContainer";
import useGridService from "../../../src/services/components/GridService";
import useComponentService from "../../../src/services/ComponentService";
import {ActionStatus} from "../../../src/redux/actions/actions";
import {isComponentAction} from "../../../src/redux/thunks/targets";
import {checkActions} from "../../../src/utilities";
import {runAction} from "../../../src/redux/actions/actions";
import {updateComponent} from "../../../src/redux/actions/components";

const {STATUS_RUNNING, STATUS_INITIAL} = ActionStatus;
const VIEW = "base";
const MISSING = "not-on-screen";
const NEXT_ID = "next";

// Component actions of the AngularJS action groups whose target is discarded when it does not exist
const COMPONENT_ACTIONS = [
  "click", "file-uploaded", "clear-file",
  "next-step", "prev-step", "first-step", "last-step", "nth-step",
  "add-points", "add-chart-series", "remove-chart-series", "replace-chart-series",
  "set-pivot-sorters", "set-pivot-group-rows", "set-pivot-group-cols", "toggle-menu"
];

function buildState(type, components = {}) {
  return {
    settings: {actionsStack: 0},
    components,
    actions: {
      running: true,
      sync: [[
        {id: "target-action", type, target: MISSING, address: {view: VIEW, component: MISSING}, parameters: {}, status: STATUS_RUNNING},
        {id: NEXT_ID, type: "wait", parameters: {target: 600000}, status: STATUS_INITIAL}
      ]],
      async: []
    }
  };
}

function renderActions(preloadedState) {
  return renderWithProviders(<MemoryRouter><ActionsContainer/></MemoryRouter>, {preloadedState});
}

function serviceActions(useService) {
  const {store} = renderWithProviders(<div/>);
  const wrapper = ({children}) => <Provider store={store}>{children}</Provider>;
  return Object.keys(renderHook(() => useService(), {wrapper}).result.current.getActions());
}

describe("awe-react-client/test/js/containers/ActionsContainerMissingTargetTest.jsx", () => {
  const gridActions = serviceActions(useGridService);

  beforeEach(() => {
    jest.spyOn(console, "debug").mockImplementation(() => undefined);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it.each([...COMPONENT_ACTIONS, ...gridActions])("ignores the '%s' action when its target is not on screen", (type) => {
    const {store} = renderActions(buildState(type));

    const [stack] = store.getState().actions.sync;
    expect(stack.map(a => a.id)).toEqual([NEXT_ID]);
    expect(stack[0].status).toBe(ActionStatus.STATUS_STARTED);
  });

  it("covers every action of the grid service", () => {
    expect(gridActions.length).toBeGreaterThan(30);
    gridActions.forEach(type => expect(isComponentAction(type)).toBe(true));
  });

  it("covers the component actions of the component service and leaves the menu ones alone", () => {
    const types = serviceActions(useComponentService);
    expect(types.filter(isComponentAction).sort()).toEqual([...COMPONENT_ACTIONS].sort());
    expect(types.filter(type => !isComponentAction(type)).sort())
      .toEqual(["change-menu", "close", "close-cancel", "dialog", "toggle-navbar"]);
  });

  it("logs the ignored action at debug level", () => {
    renderActions(buildState("toggle-menu"));
    expect(console.debug).toHaveBeenCalledWith(expect.stringContaining("toggle-menu"), expect.objectContaining({id: "target-action"}));
  });

  it("keeps running an action whose target is on screen", () => {
    const address = {view: VIEW, component: MISSING};
    const key = MISSING;
    const {store} = renderActions(buildState("toggle-menu", {[key]: {address, attributes: {minimized: false}}}));

    expect(store.getState().components[key].attributes.minimized).toBe(true);
    expect(store.getState().actions.sync[0].map(a => a.id)).toEqual([NEXT_ID]);
  });

  it.each(["close", "close-cancel", "click", "select-row"])("runs the '%s' action that names no target", (type) => {
    const handler = jest.fn();
    const {store: {dispatch}} = renderWithProviders(<div/>);
    const action = {id: "no-target", type, parameters: {}, status: STATUS_RUNNING};

    checkActions({[type]: handler}, {prevRunningActions: [], runningActions: [action]}, {dispatch, settings: {actionsStack: 0}});

    expect(handler).toHaveBeenCalledWith(action, expect.anything());
  });

  it("runs a cell action whose address points at a row and column of a grid on screen", () => {
    // The scheduler refreshes the status icon of a task with update-cell on {view, component, row, column}
    const grid = {address: {view: VIEW, component: MISSING}, attributes: {}, model: {values: [{id: "1"}]}};
    const state = buildState("update-cell", {[MISSING]: grid});
    state.actions.sync[0][0] = {
      ...state.actions.sync[0][0],
      target: undefined,
      address: {view: VIEW, component: MISSING, row: "1", column: "icon"},
      parameters: {data: {value: "check"}}
    };

    renderActions(state);

    expect(console.debug).not.toHaveBeenCalled();
  });

  it("does not discard a dialog whose component registers after the action is queued", () => {
    const address = {view: VIEW, component: "late-dialog"};
    const state = buildState("dialog");
    state.actions.sync[0][0] = {...state.actions.sync[0][0], status: STATUS_INITIAL, target: "late-dialog", address};
    const {store} = renderActions(state);

    store.dispatch(updateComponent(address, {address, attributes: {isShowing: false}}));
    act(() => {
      store.dispatch(runAction(store.getState().actions.sync[0][0]));
    });

    expect(store.getState().components["late-dialog"].attributes.isShowing).toBe(true);
  });

  describe("an action whose target is on screen", () => {
    const run = (type, component, parameters = {}) => {
      const address = {view: VIEW, component: MISSING};
      const state = buildState(type, {[MISSING]: {address, ...component}});
      state.actions.sync[0][0].parameters = parameters;
      return renderActions(state).store.getState();
    };

    it("changes the grid", () => {
      const state = run("select-first-row", {attributes: {}, model: {values: [{id: 1}, {id: 2}]}});
      expect(state.components[MISSING].model.values.map(row => Boolean(row.selected))).toEqual([true, false]);
    });

    it("changes the chart", () => {
      const state = run("add-points", {attributes: {}, model: {values: [{x: 1}]}}, {value: {x: 2}});
      expect(state.components[MISSING].model.values).toEqual([{x: 1}, {x: 2}]);
    });

    it("changes the wizard", () => {
      const state = run("next-step", {attributes: {}, model: {values: [{value: "a", selected: true}, {value: "b"}]}});
      expect(state.components[MISSING].model.values.map(step => Boolean(step.selected))).toEqual([false, true]);
    });

    it("changes the uploader", () => {
      const state = run("file-uploaded", {attributes: {}, model: {values: []}}, {name: "f.txt", path: "p", size: 1, type: "text"});
      expect(state.components[MISSING].model.values[0].label).toBe("f.txt");
    });

    it("changes the pivot", () => {
      const state = run("set-pivot-group-rows", {attributes: {}}, {rows: "a,b"});
      expect(state.components[MISSING].attributes.rows).toBe("a,b");
    });

    it("clicks the button", () => {
      const state = run("click", {attributes: {}, actions: [{type: "wait", parameters: {target: 700000}}]});
      expect(state.actions.sync[0].map(a => a.type)).toEqual(["wait", "wait"]);
    });
  });
});
