import React, {Profiler, Suspense, useEffect, useRef} from "react";
import {act, cleanup} from "@testing-library/react";
import {shallowEqual, useDispatch, useSelector} from "react-redux";
import {MemoryRouter} from "react-router";
import {createStore} from "../../../src/redux/store";
import {renderWithProviders} from "../test-utils";
import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";
import ActionsContainer from "../../../src/containers/ActionsContainer";
import useClientActions from "../../../src/services/ClientActionsService";
import {selectSettings} from "../../../src/redux/selectors/stateSelectors";
import {ActionStatus, addActionsTop} from "../../../src/redux/actions/actions";
import {updateViewComponentsWithDependencies} from "../../../src/redux/thunks/components";
import * as dependencies from "../../../src/redux/actions/dependencies";
import * as utilities from "../../../src/utilities";
import AweInputText from "../../../src/criteria/AweInputText";

/**
 * Measurement of one server response with many client actions on a screen with many dependencies (issue #960). It
 * measures, it does not assert performance. Gated by AWE_BENCH so the normal test run skips it. Run it with:
 * AWE_BENCH=1 npx jest --config jest.config.cjs test/js/performance/ActionsChainBenchTest.jsx
 */
const describeBench = process.env.AWE_BENCH ? describe : describe.skip;

const VIEW = "base";
const CRITERIA = Number(process.env.AWE_BENCH_CRITERIA || 150);
const DEPENDENCIES_PER_CRITERION = 3;
const ACTIONS = Number(process.env.AWE_BENCH_ACTIONS || 140);
const RENDERED = process.env.AWE_BENCH_RENDER !== "0";

const {STATUS_RUNNING} = ActionStatus;
const id = (index) => `crit${index}`;

const criterion = (index) => {
  const watched = (offset) => id((index + offset) % CRITERIA);
  const deps = [
    {type: "and", target: "hide", initial: true, elements: [{id: watched(1), condition: "eq", value: "hide"}]},
    {type: "and", target: "set-required", initial: true, elements: [{id: watched(2), condition: "ne", value: ""}]},
    {type: "or", source: "value", value: "u", target: "unit", initial: true, elements: [{id: watched(3)}, {id: watched(4)}]}
  ].slice(0, DEPENDENCIES_PER_CRITERION);
  // Three dependencies name a component that is not in the screen, as in the issue
  if (index < 3) {
    deps.push({type: "and", target: "hide", elements: [{id: `missing${index}`}]});
  }
  const model = {values: [{value: "initial", label: "initial", selected: true}]};
  return {
    uid: id(index), id: id(index), address: {view: VIEW, component: id(index)},
    model, storedModel: model, defaultModel: model,
    attributes: {component: "text", label: `Criterion ${index}`, visible: true},
    specificAttributes: {}, storedAttributes: {}, validationRules: {}, storedValidationRules: {},
    actions: [], dependencies: deps, contextMenu: [], context: {view: VIEW, source: []}
  };
};

const actionOrder = (action) => {
  const order = Number(String(action.id).replace("action-", ""));
  return Number.isNaN(order) ? Number.MAX_SAFE_INTEGER : order;
};
const selectRunningActions = (state) => [
  ...state.actions.sync[state.actions.sync.length - 1].filter((a) => a.status === STATUS_RUNNING),
  ...state.actions.async.filter((a) => a.status === STATUS_RUNNING)
].sort((first, second) => actionOrder(first) - actionOrder(second));

// The container as it was before #951 (a409f2545): one action per render
function Pre951ActionsContainer() {
  const dispatch = useDispatch();
  const prevRunningActionsRef = useRef([]);
  const settings = useSelector(selectSettings);
  const runningActions = useSelector(selectRunningActions, shallowEqual);
  const combinedActions = useClientActions();
  useEffect(() => {
    // In the browser every step ran in its own task; under act a synchronous chain hits React's nested update limit
    const prevRunningActions = prevRunningActionsRef.current;
    prevRunningActionsRef.current = runningActions;
    setTimeout(() => utilities.checkActions(combinedActions, {prevRunningActions, runningActions}, {dispatch, settings}), 0);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- same dependencies as the container it replicates
  }, [runningActions]);
  return null;
}

const round = (value) => Math.round(value * 10) / 10;

async function run(Container) {
  ComponentRegistry.clearAll();
  const store = createStore();
  const data = Object.fromEntries(Array.from({length: CRITERIA}, (_, index) => [id(index), criterion(index)]));
  store.dispatch(updateViewComponentsWithDependencies(VIEW, data));

  let commits = 0;
  const ui = (
    <MemoryRouter>
      <Profiler id="screen" onRender={() => commits++}>
        <Container/>
        {RENDERED && <Suspense fallback={null}>
          {Array.from({length: CRITERIA}, (_, index) => <AweInputText key={index} id={id(index)}/>)}
        </Suspense>}
      </Profiler>
    </MemoryRouter>
  );
  renderWithProviders(ui, {store});

  // Counters
  const counters = {checkDependencies: 0, checkDependenciesMs: 0, dispatches: 0, warnings: 0};
  const realCheck = jest.requireActual("../../../src/redux/actions/dependencies").checkDependencies;
  const checkSpy = jest.spyOn(dependencies, "checkDependencies").mockImplementation((...args) => {
    counters.checkDependencies++;
    const start = performance.now();
    const result = realCheck(...args);
    counters.checkDependenciesMs += performance.now() - start;
    return result;
  });
  const unsubscribe = store.subscribe(() => counters.dispatches++);
  console.warn.mockClear();
  commits = 0;

  const response = Array.from({length: ACTIONS}, (_, index) => ({
    type: "select", target: id(index % CRITERIA), view: VIEW, parameters: {values: [index % 7 === 0 ? "hide" : `v${index}`]}
  }));
  const start = performance.now();
  const pendingCount = () => [...store.getState().actions.sync.flat(), ...store.getState().actions.async].length;
  await act(async () => {
    store.dispatch(addActionsTop(response));
    while (pendingCount() > 0) {
      await new Promise((resolve) => setTimeout(resolve, 0));
    }
  });
  const totalMs = performance.now() - start;
  counters.warnings = console.warn.mock.calls.filter(([message]) => String(message).includes("[Dependency]")).length;

  unsubscribe();
  checkSpy.mockRestore();
  const pending = [...store.getState().actions.sync.flat(), ...store.getState().actions.async].length;
  const finalState = Object.fromEntries(Array.from({length: CRITERIA}, (_, index) => {
    const component = store.getState().components[id(index)] || {};
    return [id(index), JSON.stringify([component.model?.values, component.attributes?.visible,
      component.attributes?.unit, component.validationRules?.required])];
  }));
  cleanup();
  process.stderr.write(`[bench] ${Container.name} total=${round(totalMs)}ms checks=${counters.checkDependencies} checkMs=${round(counters.checkDependenciesMs)} pending=${pending}\n`);
  return {
    totalMs: round(totalMs), checkDependencies: counters.checkDependencies,
    checkDependenciesMs: round(counters.checkDependenciesMs), dispatches: counters.dispatches,
    warnings: counters.warnings, commits, pending, finalState
  };
}

describeBench("awe-react-client/test/js/performance/ActionsChainBenchTest.jsx", () => {
  jest.setTimeout(30 * 60 * 1000);

  beforeAll(() => {
    jest.spyOn(console, "info").mockImplementation(() => {});
    jest.spyOn(console, "log").mockImplementation(() => {});
    jest.spyOn(console, "warn").mockImplementation(() => {});
  });

  it("measures one response of many client actions", async () => {
    const results = {config: {criteria: CRITERIA, actions: ACTIONS, rendered: RENDERED,
      dependencies: CRITERIA * DEPENDENCIES_PER_CRITERION + 3}};
    // Warm-up both, then measure alternately
    const only = process.env.AWE_BENCH_ONLY;
    const variants = {pre951: Pre951ActionsContainer, develop: ActionsContainer};
    const names = Object.keys(variants).filter((name) => !only || name === only);
    const samples = Object.fromEntries(names.map((name) => [name, []]));
    for (const name of names) {
      await run(variants[name]);
    }
    for (let i = 0; i < 3; i++) {
      for (const name of names) {
        samples[name].push(await run(variants[name]));
      }
    }
    Object.entries(samples).forEach(([name, list]) => {
      const {finalState, ...last} = list[list.length - 1];
      results[name] = {...last, finalStateHash: require("crypto").createHash("sha1").update(JSON.stringify(finalState)).digest("hex"), totalMsRuns: list.map((sample) => sample.totalMs),
        checkMsRuns: list.map((sample) => sample.checkDependenciesMs)};
    });
    if (samples.pre951 && samples.develop) {
      results.sameFinalState = shallowEqual(samples.pre951[0].finalState, samples.develop[0].finalState);
    }
    process.stdout.write(`\nBENCH_RESULT_BEGIN\n${JSON.stringify(results, null, 2)}\nBENCH_RESULT_END\n`);
    Object.values(samples).forEach((list) => expect(list[0].pending).toBe(0));
  });
});
