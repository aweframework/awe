import React from "react";
import {act} from "@testing-library/react";
import {shallowEqual, useSelector} from "react-redux";
import {MemoryRouter} from "react-router";
import {renderWithProviders} from "../test-utils";
import ActionsContainer, {MAX_CHAINED_RUNS, RENDER_BOUND_ACTIONS} from "../../../src/containers/ActionsContainer";
import useClientActions from "../../../src/services/ClientActionsService";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {acceptAction, addActionsTop} from "../../../src/redux/actions/actions";
import * as utilities from "../../../src/utilities";
import {initializeDependencies} from "../../../src/redux/actions/dependencies";

// Renders of a component subscribed to the components and the actions: every change of them that reaches React
const probe = {renders: 0};
function ComponentsProbe() {
  useSelector((state) => [state.components, state.actions], shallowEqual);
  probe.renders++;
  return null;
}

const criterion = (id, extra = {}) => ({
  id,
  address: {view: "base", component: id},
  attributes: {},
  model: {values: [{value: "initial", selected: true}]},
  ...extra
});

function renderEngine(components) {
  const preloadedState = {
    settings: {...DEFAULT_SETTINGS, actionsStack: 0, activeDependencies: true},
    components
  };
  const result = renderWithProviders(
    <MemoryRouter><ActionsContainer/><ComponentsProbe/></MemoryRouter>, {preloadedState});
  initializeDependencies("base", result.store.getState(), result.store.dispatch);
  probe.renders = 0;
  return result;
}

const select = (target, value) => ({type: "select", target, view: "base", parameters: {values: [value]}});
const selectedValue = (store, id) => store.getState().components[id].model.values
  .filter((item) => item.selected).map((item) => item.value);
const pendingActions = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async];

describe("awe-react-client/test/js/containers/ActionsBatchTest.jsx", () => {
  afterEach(() => jest.restoreAllMocks());

  it("applies the client actions of one response in one render, in the order the server sent them", async () => {
    const ids = Array.from({length: 20}, (_, index) => `criterion${index}`);
    const {store} = renderEngine(Object.fromEntries(ids.map((id) => [id, criterion(id)])));

    // Every criterion is selected twice: the last value sent wins
    await act(async () => {
      store.dispatch(addActionsTop([
        ...ids.map((id) => select(id, `${id}-first`)),
        ...ids.map((id) => select(id, `${id}-last`))
      ]));
    });

    ids.forEach((id) => expect(selectedValue(store, id)).toEqual([`${id}-last`]));
    expect(pendingActions(store)).toEqual([]);
    expect(store.getState().actions.running).toBe(false);
    expect(probe.renders).toBeLessThanOrEqual(2);
  });

  // Record, for every action the container launches, how many renders the probe had done and the launch order
  function spyLaunches() {
    const launches = [];
    const checkActions = jest.requireActual("../../../src/utilities").checkActions;
    jest.spyOn(utilities, "checkActions").mockImplementation((allowed, actions, props) => {
      actions.runningActions.forEach((action) => launches.push({type: action.type, renders: probe.renders, action}));
      return checkActions(allowed, actions, props);
    });
    return launches;
  }

  it("waits for a render after an action whose handler reads the last render, and only after it", async () => {
    const {store} = renderEngine({target: criterion("target")});
    const launches = spyLaunches();

    await act(async () => {
      store.dispatch(addActionsTop([select("target", "before"), {type: "reload-language"}, select("target", "after")]));
    });

    expect(selectedValue(store, "target")).toEqual(["after"]);
    const [first, renderBound, last] = launches;
    expect(launches.map((launch) => launch.type)).toEqual(["select", "reload-language", "select"]);
    // A plain action chains the next one in the same render; the render-bound one makes the next wait for a render
    expect(renderBound.renders).toBe(first.renders);
    expect(last.renders).toBeGreaterThan(renderBound.renders);
  });

  it("names only registered client actions as render-bound", () => {
    const registered = {};
    function Registry() {
      Object.assign(registered, useClientActions());
      return null;
    }
    renderWithProviders(<MemoryRouter><Registry/></MemoryRouter>, {preloadedState: {settings: DEFAULT_SETTINGS}});

    [...RENDER_BOUND_ACTIONS].forEach((type) => expect(Object.keys(registered)).toContain(type));
  });

  it("launches the actions of one response in the order they were received, async ones included", async () => {
    const {store} = renderEngine({first: criterion("first"), last: criterion("last")});
    const launches = spyLaunches();

    await act(async () => {
      store.dispatch(addActionsTop([
        select("first", "one"),
        {type: "wait", async: true, parameters: {target: 1}},
        select("last", "two")
      ]));
      await new Promise((resolve) => setTimeout(resolve, 10));
    });

    expect(launches.map((launch) => launch.type)).toEqual(["select", "wait", "select"]);
    expect(pendingActions(store)).toEqual([]);
  });

  it("renders before going on when more actions than the limit run in a row, and loses none", async () => {
    const {store} = renderEngine({target: criterion("target")});
    const total = MAX_CHAINED_RUNS + 10;
    let launched = 0;
    const launches = [];
    jest.spyOn(console, "warn").mockImplementation(() => {});
    // Every action accepts itself and queues the next one at once, as an action that keeps launching itself would
    jest.spyOn(utilities, "checkActions").mockImplementation((allowed, actions, props) => {
      actions.runningActions.forEach((action) => {
        launches.push(probe.renders);
        launched++;
        props.dispatch(acceptAction(action));
        if (launched < total) {
          props.dispatch(addActionsTop([{type: "loop"}]));
        }
      });
    });

    await act(async () => {
      store.dispatch(addActionsTop([{type: "loop"}]));
    });

    expect(launched).toBe(total);
    expect(launches.filter((renders) => renders === launches[0]).length).toBe(MAX_CHAINED_RUNS);
    // Only the warning of the chain: a loaded runner may add the slow-middleware warning of Redux Toolkit
    expect(console.warn.mock.calls.filter(([message]) => String(message).startsWith("[ACTIONS]"))).toHaveLength(1);
    expect(pendingActions(store)).toEqual([]);
  });

  it("runs the actions a dependency launches before the rest of the response, as each action fires it", async () => {
    const {store} = renderEngine({
      source: criterion("source"),
      target: criterion("target", {
        dependencies: [{
          type: "and", initial: false,
          elements: [{id: "source", checkChanges: true}],
          actions: [select("target", "from-dependency")]
        }]
      })
    });

    // The dependency fires on the first select and its action runs before the next select of the response
    await act(async () => {
      store.dispatch(addActionsTop([select("source", "changed"), select("target", "from-response")]));
    });

    expect(selectedValue(store, "target")).toEqual(["from-response"]);
    expect(pendingActions(store)).toEqual([]);
  });

  it("does not evaluate again, on every action of the response, a dependency whose components did not change", async () => {
    const ids = Array.from({length: 20}, (_, index) => `criterion${index}`);
    const {store} = renderEngine({
      ...Object.fromEntries(ids.map((id) => [id, criterion(id)])),
      // Its element is not in the screen: every evaluation of the dependency logs it
      watcher: criterion("watcher", {
        dependencies: [{type: "and", target: "hide", elements: [{id: "missing", checkChanges: true}]}]
      })
    });
    const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

    await act(async () => {
      store.dispatch(addActionsTop(ids.map((id) => select(id, `${id}-new`))));
    });

    const missingWarnings = () => warn.mock.calls.filter(([message]) => String(message).includes("missing is not defined"));
    ids.forEach((id) => expect(selectedValue(store, id)).toEqual([`${id}-new`]));
    expect(missingWarnings()).toEqual([]);
    expect(pendingActions(store)).toEqual([]);

    // The component that holds it changes: the dependency is evaluated again (and logs its missing element)
    await act(async () => {
      store.dispatch(addActionsTop([select("watcher", "changed")]));
    });
    expect(missingWarnings().length).toBeGreaterThan(0);
  });

  it("fires a dependency on every intermediate value of the response, not only on the final one", async () => {
    const {store} = renderEngine({
      source: criterion("source"),
      counter: criterion("counter", {
        dependencies: [{
          type: "and", initial: false,
          elements: [{id: "source", checkChanges: true}],
          actions: [{type: "fill", target: "counter", view: "base", parameters: {datalist: {rows: []}}}]
        }]
      })
    });
    const fills = new Set();
    store.subscribe(() => pendingActions(store)
      .filter((action) => action.type === "fill")
      .forEach((action) => fills.add(action.id)));

    // The value goes and comes back: the final state equals the initial one, but the dependency fired twice
    await act(async () => {
      store.dispatch(addActionsTop([select("source", "changed"), select("source", "initial")]));
    });

    expect(selectedValue(store, "source")).toEqual(["initial"]);
    expect(fills.size).toBe(2);
    expect(pendingActions(store)).toEqual([]);
  });
});
