import React, {Profiler, Suspense} from "react";
import {act, cleanup} from "@testing-library/react";
import v8 from "v8";
import vm from "vm";
import {createStore} from "../../../src/redux/store";
import {renderWithProviders} from "../test-utils";
import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";
import {getAllComponents} from "../../../src/redux/selectors/componentSelectors";
import {updateModel} from "../../../src/redux/actions/components";
import {
  updateAttributesWithDependencies,
  updateModelWithDependencies,
  updateViewComponentsWithDependencies
} from "../../../src/redux/thunks/components";
import {selectRowGridAction} from "../../../src/redux/thunks/grid";
import AweInputText from "../../../src/criteria/AweInputText";
import AweSelect from "../../../src/criteria/AweSelect";
import AweInputNumeric from "../../../src/criteria/AweInputNumeric";
import AweInputDate from "../../../src/criteria/AweInputDate";
import AweGrid from "../../../src/components/AweGrid";

/**
 * BASELINE measurement of the ComponentRegistry + Redux deltas path (issue #805). It measures, it does not assert
 * performance. Gated by AWE_BENCH so the normal test run skips it. Run it with: npm run bench:registry
 */
const describeBench = process.env.AWE_BENCH ? describe : describe.skip;

// Synthetic heavy screen
const VIEW = "bench";
const GRIDS = 4;
const ROWS = 2000;
const COLUMNS = 10;
const CRITERIA = 40;
const SELECT_OPTIONS = 20;
const BUTTONS = 4;
const DEPENDENCIES = 6;
// Runs
const WARMUP = 2;
const RUNS = 9;
const LOAD_RUNS = 5;
const SELECTOR_REPEATS = 500;
// Screen rendered with the real components
const RENDER_GRIDS = 2;
const RENDER_ROWS = 200;
const RENDER_CRITERIA = 10;

// Real criteria components, in the same order as the kinds of makeCriterion (text, select, numeric, date)
const CRITERION_COMPONENTS = [AweInputText, AweSelect, AweInputNumeric, AweInputDate];

const address = (component, extra = {}) => ({view: VIEW, component, ...extra});
const gridId = (index) => `grid${index}`;

const forceGc = (() => {
  if (typeof global.gc === "function") {
    return global.gc;
  }
  try {
    v8.setFlagsFromString("--expose-gc");
    return vm.runInNewContext("gc");
  } catch (e) {
    return null;
  }
})();

const heapMb = () => {
  if (forceGc) {
    forceGc();
    forceGc();
  }
  return Math.round(process.memoryUsage().heapUsed / 1024 / 1024 * 10) / 10;
};

const round = (value) => Math.round(value * 100) / 100;

const stats = (samples) => {
  const sorted = [...samples].sort((a, b) => a - b);
  const at = (q) => sorted[Math.min(sorted.length - 1, Math.floor(q * sorted.length))];
  return {median: round(at(0.5)), p90: round(at(0.9)), min: round(sorted[0]), runs: sorted.length};
};

/**
 * Measure a function: warm-up runs first, then the measured ones. prepare() runs outside the timer and its result is
 * passed to fn
 */
const measure = (fn, {prepare = () => undefined, runs = RUNS, warmup = WARMUP} = {}) => {
  const samples = [];
  for (let i = 0; i < warmup + runs; i++) {
    const context = prepare(i);
    const start = performance.now();
    fn(context, i);
    const elapsed = performance.now() - start;
    if (i >= warmup) {
      samples.push(elapsed);
    }
  }
  return stats(samples);
};

const makeRows = (rows, columns, salt = "") => Array.from({length: rows}, (_, row) => {
  const result = {id: String(row), selected: false};
  for (let column = 1; column < columns; column++) {
    result[`c${column}`] = `${salt}value ${row}-${column}`;
  }
  return result;
});

const makeColumnModel = (columns) => Array.from({length: columns}, (_, column) => ({
  name: column === 0 ? "id" : `c${column}`,
  label: `Column ${column}`,
  hidden: column === 0,
  sortable: true,
  // Some editable columns with a component (the cells of a real editable grid)
  ...(column % 3 === 1 ? {component: "text", editable: true} : {}),
  align: "left"
}));

const baseComponent = (id, overrides = {}) => ({
  uid: id,
  address: address(id),
  model: {values: []},
  storedModel: {values: []},
  defaultModel: {values: []},
  attributes: {},
  specificAttributes: {},
  storedAttributes: {},
  validationRules: {},
  storedValidationRules: {},
  actions: [],
  dependencies: [],
  contextMenu: [],
  context: {view: VIEW, source: []},
  ...overrides
});

const makeGrid = (index, rows, columns) => {
  const id = gridId(index);
  const values = makeRows(rows, columns);
  return baseComponent(id, {
    model: {values},
    storedModel: {values},
    defaultModel: {values},
    attributes: {columnModel: makeColumnModel(columns), headerModel: [], buttonModel: [], editable: true, label: id},
    specificAttributes: {sort: []}
  });
};

const makeCriterion = (index) => {
  const id = `crit${index}`;
  const kind = index % 4;
  let model = {values: []};
  let attributes = {label: `Criterion ${index}`, visible: true};
  if (kind === 0) {
    model = {values: [{value: `text ${index}`, label: `text ${index}`, selected: true}]};
    attributes = {...attributes, component: "text"};
  } else if (kind === 1) {
    model = {
      values: Array.from({length: SELECT_OPTIONS}, (_, option) =>
        ({value: String(option), label: `Option ${option}`, selected: option === 0}))
    };
    attributes = {...attributes, component: "select"};
  } else if (kind === 2) {
    model = {values: [{value: String(index), label: String(index), selected: true}]};
    attributes = {...attributes, component: "numeric"};
  } else {
    model = {values: [{value: "01/01/2026", label: "01/01/2026", selected: true}]};
    attributes = {...attributes, component: "date"};
  }
  // A few criteria listen to another criterion, so checkDependencies does real work
  const dependencies = index < DEPENDENCIES ? [{
    type: "and", source: "value", value: "value", target: "unit", initial: true,
    elements: [{id: `crit${index + 1}`, checkChanges: true}],
    actions: []
  }] : [];
  return baseComponent(id, {model, storedModel: model, defaultModel: model, attributes, dependencies});
};

const makeButton = (index) => baseComponent(`button${index}`, {
  attributes: {label: `Button ${index}`, visible: true, buttonType: "button"}
});

const makeScreen = (grids, rows, criteria, buttons = BUTTONS) => {
  const data = {};
  for (let g = 0; g < grids; g++) {
    data[gridId(g)] = makeGrid(g, rows, COLUMNS);
  }
  for (let c = 0; c < criteria; c++) {
    data[`crit${c}`] = makeCriterion(c);
  }
  for (let b = 0; b < buttons; b++) {
    data[`button${b}`] = makeButton(b);
  }
  return data;
};

const freshStore = () => {
  ComponentRegistry.clearAll();
  return createStore();
};

const loadScreen = (store, data) => store.dispatch(updateViewComponentsWithDependencies(VIEW, data));

const withSettings = (store, action) => ({...action, settings: store.getState().settings});

const criterionValue = (iteration) => ({values: [{value: `new ${iteration}`, label: `new ${iteration}`, selected: true}]});

const registrySizeChars = () => ComponentRegistry.getAllIds()
  .reduce((total, id) => total + JSON.stringify(ComponentRegistry.get(id)).length, 0);

describeBench("awe-react-client/test/js/performance/RegistryDeltasBenchTest.jsx", () => {
  jest.setTimeout(30 * 60 * 1000);

  const results = {};
  let infoSpy;
  let warnSpy;

  beforeAll(() => {
    // The dependencies and thunks log while they run: keep the output readable
    infoSpy = jest.spyOn(console, "info").mockImplementation(() => {});
    jest.spyOn(console, "log").mockImplementation(() => {});
    warnSpy = jest.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterAll(() => {
    infoSpy.mockRestore();
    warnSpy.mockRestore();
    process.stdout.write(`\nBENCH_RESULT_BEGIN\n${JSON.stringify(results, null, 2)}\nBENCH_RESULT_END\n`);
  });

  it("measures time", () => {
    const data = makeScreen(GRIDS, ROWS, CRITERIA);
    results.config = {
      grids: GRIDS, rows: ROWS, columns: COLUMNS, criteria: CRITERIA, buttons: BUTTONS, dependencies: DEPENDENCIES,
      componentsInScreen: Object.keys(data).length, cellComponentsPerGrid: 0, runs: RUNS, warmup: WARMUP,
      gcAvailable: !!forceGc
    };
    const time = {};

    // a. screen load
    time.a_screenLoad = measure(
      ({store}) => loadScreen(store, data),
      {prepare: () => ({store: freshStore()}), runs: LOAD_RUNS, warmup: 1});

    // Shared loaded store for the rest
    const store = freshStore();
    loadScreen(store, data);

    // b. getAllComponents cold (right after a delta change) and repeated without change
    time.b_getAllComponentsCold = measure(
      () => getAllComponents(store.getState()),
      {
        prepare: (i) => {
          store.dispatch(withSettings(store, updateModel(address("crit1"), criterionValue(`b${i}`))));
        }
      });
    getAllComponents(store.getState());
    const repeatedStart = performance.now();
    for (let i = 0; i < SELECTOR_REPEATS; i++) {
      getAllComponents(store.getState());
    }
    time.b_getAllComponentsRepeatedMsPerCall = round((performance.now() - repeatedStart) / SELECTOR_REPEATS);

    // c. criterion change through the thunk (reducer + checkDependencies, end to end)
    time.c_criterionChangeThunk = measure(
      (_, i) => store.dispatch(updateModelWithDependencies(address("crit0"), criterionValue(`c${i}`))));
    // Same, on a criterion that nobody depends on
    time.c_criterionChangeThunkNoDependents = measure(
      (_, i) => store.dispatch(updateModelWithDependencies(address("crit30"), criterionValue(`c${i}`))));

    // d. single grid cell edit (reducer path with settings). _.isEqual stops at the first difference, so the row
    // edited matters: an early row is the best case, a late row the worst one (the whole model is walked)
    time.d_gridCellEditFirstEver = measure(
      ({store: fresh}) => fresh.dispatch(withSettings(fresh, updateModel(
        address(gridId(0), {row: "5", column: "c3"}), {values: "first edit"}))),
      {
        prepare: () => {
          const fresh = freshStore();
          loadScreen(fresh, data);
          return {store: fresh};
        },
        runs: LOAD_RUNS, warmup: 1
      });
    time.d_gridCellEditEarlyRow = measure(
      (_, i) => store.dispatch(withSettings(store, updateModel(
        address(gridId(0), {row: String(i + 5), column: "c3"}), {values: `edited ${i}`}))));
    time.d_gridCellEditLateRow = measure(
      (_, i) => store.dispatch(withSettings(store, updateModel(
        address(gridId(0), {row: String(ROWS - 1 - i), column: "c3"}), {values: `late ${i}`}))));

    // e. grid full reload (new rows)
    time.e_gridFullReload = measure(
      (_, i) => store.dispatch(withSettings(store, updateModel(
        address(gridId(1)), {values: makeRows(ROWS, COLUMNS, `reload${i} `)}))),
      {runs: 5, warmup: 1});

    // e2. reload where only the last row differs (the whole model is walked by the comparison)
    time.e2_gridReloadLastRowDiffers = measure(
      (_, i) => {
        const rows = makeRows(ROWS, COLUMNS, "same ");
        rows[ROWS - 1].c1 = `differs ${i}`;
        store.dispatch(withSettings(store, updateModel(address(gridId(3)), {values: rows})));
      },
      {runs: 5, warmup: 1});

    // f. select a row (thunk of the select-row action)
    time.f_gridSelectRow = measure(
      (_, i) => store.dispatch(selectRowGridAction({
        address: address(gridId(2)), parameters: {values: [String(10 + i)]}
      })));

    results.timeMs = time;
    // Every step was measured and the store ends with the last reload of grid 1
    Object.values(time).forEach(value => {
      expect(Number.isFinite(typeof value === "number" ? value : value.median)).toBe(true);
    });
    expect(getAllComponents(store.getState())[gridId(1)].model.values).toHaveLength(ROWS);
  });

  it("measures memory", () => {
    const memory = {};
    const data = makeScreen(GRIDS, ROWS, CRITERIA);
    const store = freshStore();
    const before = heapMb();

    loadScreen(store, data);
    const afterLoad = heapMb();
    memory.heapMb = {beforeLoad: before, afterLoad, loadDelta: round(afterLoad - before)};

    // Free the synthetic input: it is only the server payload, not held by the app after loading
    const payloadChars = JSON.stringify(data).length;
    const reduxAfterLoad = JSON.stringify(store.getState().components).length;
    const registryChars = registrySizeChars();

    const perGrid = [];
    let previous = heapMb();
    for (let g = 0; g < GRIDS; g++) {
      store.dispatch(withSettings(store, updateModel(
        address(gridId(g), {row: "5", column: "c3"}), {values: "first edit"})));
      const current = heapMb();
      perGrid.push({grid: gridId(g), deltaMb: round(current - previous)});
      previous = current;
    }
    memory.afterFirstCellEditPerGrid = perGrid;
    memory.heapMb.afterFirstEdits = previous;

    const reduxAfterEdits = JSON.stringify(store.getState().components).length;
    const gridDelta = store.getState().components[gridId(0)];
    memory.stateChars = {
      serverPayload: payloadChars,
      reduxComponentsAfterLoad: reduxAfterLoad,
      reduxComponentsAfterFirstEdits: reduxAfterEdits,
      registry: registryChars,
      grid0DeltaHasModel: !!gridDelta?.model,
      grid0DeltaHasColumnModel: !!gridDelta?.attributes?.columnModel,
      grid0DeltaModelRowsChars: JSON.stringify(gridDelta?.model || {}).length
    };
    results.memory = memory;
    // The frozen base holds the screen and Redux only the changes: empty after loading, larger once grids change
    expect(registryChars).toBeGreaterThan(reduxAfterLoad);
    expect(reduxAfterEdits).toBeGreaterThan(reduxAfterLoad);
  });

  it("counts re-renders with the real components", () => {
    const data = makeScreen(RENDER_GRIDS, RENDER_ROWS, RENDER_CRITERIA, 0);
    const store = freshStore();
    loadScreen(store, data);
    const commits = {};
    const reset = () => Object.keys(commits).forEach(id => (commits[id] = 0));
    const profile = (id, element) => (
      <Profiler key={id} id={id} onRender={() => (commits[id] = (commits[id] || 0) + 1)}>{element}</Profiler>
    );
    const ui = (
      <Suspense fallback={null}>
        {Array.from({length: RENDER_GRIDS}, (_, g) => profile(gridId(g), <AweGrid id={gridId(g)}/>))}
        {Array.from({length: RENDER_CRITERIA}, (_, c) => {
          const Input = CRITERION_COMPONENTS[c % 4];
          return profile(`crit${c}`, <Input id={`crit${c}`}/>);
        })}
      </Suspense>
    );
    renderWithProviders(ui, {store});
    const rendered = Object.keys(commits).length;
    reset();

    const touched = () => Object.fromEntries(Object.entries(commits).filter(([, count]) => count > 0));
    const scenarios = {};
    const run = (name, expected, action) => {
      reset();
      act(action);
      const actual = touched();
      scenarios[name] = {
        expectedToRender: expected,
        rendered: actual,
        unexpected: Object.keys(actual).filter(id => !expected.includes(id))
      };
    };

    run("editCriterion(crit1)", ["crit1", "crit0"], () => {
      store.dispatch(updateModelWithDependencies(address("crit1"), criterionValue("render")));
    });
    scenarios["editCriterion(crit1)"].modelChangedInState =
      JSON.stringify(store.getState().components.crit1?.model).includes("render");
    run("editGridCell(grid0, row 5)", [gridId(0)], () => {
      store.dispatch(withSettings(store, updateModel(
        address(gridId(0), {row: "5", column: "c3"}), {values: "rendered edit"})));
    });
    run("reloadGrid(grid1)", [gridId(1)], () => {
      store.dispatch(withSettings(store, updateModel(
        address(gridId(1)), {values: makeRows(RENDER_ROWS, COLUMNS, "reload ")})));
    });
    run("updateAttributes(crit1) [harness sanity check]", ["crit1"], () => {
      store.dispatch(updateAttributesWithDependencies(address("crit1"), {label: "Changed label"}));
    });
    results.renders = {profiledComponents: rendered, scenarios};
    cleanup();
    // Each action renders only the components it changes
    Object.values(scenarios).forEach(({unexpected}) => expect(unexpected).toEqual([]));
  });
});
