import React from "react";
import { act, render } from "@testing-library/react";
import { configureStore } from "@reduxjs/toolkit";
import { Provider } from "react-redux";
import createRootReducer from "../../../src/redux/reducers";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import AweSuggest from "../../../src/criteria/AweSuggest";
import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";
import { updateViewComponentsWithDependencies } from "../../../src/redux/thunks/components";
import { restoreAction, restoreTargetAction } from "../../../src/redux/thunks/form";
import { fixModel, fixDefaultModel } from "../../../src/utilities/components";

const view = "report";
const LABEL = "test (Manager)";

/**
 * Register a suggest like the screen loader does (model, stored model and default model)
 */
const component = (id, model, attributes, defaultModel) => ({
  uid: id,
  address: { view, component: id },
  model: { ...model },
  storedModel: { ...model },
  defaultModel: { ...(defaultModel || model) },
  attributes,
  storedAttributes: {},
  validationRules: {},
  storedValidationRules: {},
  specificAttributes: {},
  actions: [],
  dependencies: [],
  contextMenu: [],
  context: { view, source: ["root", "panel", id] }
});

const launcher = {
  source: "launcher", targetAction: "TxtMgr", type: "and", initial: true, invert: false, target: "input", silent: false,
  async: false, elements: [{ id: "Txt", condition: "eq", value: "test", alias: "TxtMgr", checkChanges: true }], query: "TxtMgr", actions: []
};

const register = (store, serverModel, attributes = {}, withLauncher = false) => {
  const model = fixModel(serverModel, false);
  const defaultModel = fixDefaultModel(model, serverModel.defaultValues, false);
  store.dispatch(updateViewComponentsWithDependencies(view, {
    Sug: {
      ...component("Sug", model, {
        component: "suggest", serverAction: "data", targetAction: "TstUsrSug", timeout: 500, ...attributes
      }, defaultModel),
      dependencies: withLauncher ? [launcher] : []
    },
    Txt: component("Txt", { selected: ["test"], values: [{ value: "test", label: "test", selected: true }] }, { component: "text" }),
    ButRst: component("ButRst", { values: [] }, { component: "button" })
  }));
};

describe("awe-react-client/test/js/criteria/AweSuggestRestoreTest.jsx", () => {
  let store;
  let pendingLabels;
  const input = () => document.querySelector("#Sug input[aria-autocomplete]");

  const labelResponse = () => ({
    ok: true,
    json: () => Promise.resolve([{ type: "fill", parameters: { datalist: { rows: [{ value: "test", label: LABEL }] } } }])
  });
  // The label query is answered only when the test releases it, to control the order of events
  const holdLabels = () => {
    window.fetch = jest.fn(() => new Promise(resolve => pendingLabels.push(() => resolve(labelResponse()))));
  };
  const releaseLabels = async () => {
    await act(async () => {
      pendingLabels.splice(0).forEach(release => release());
      await new Promise(resolve => setTimeout(resolve, 20));
    });
  };

  beforeEach(() => {
    pendingLabels = [];
    window.fetch = jest.fn(() => Promise.resolve(labelResponse()));
    store = configureStore({
      reducer: createRootReducer({}),
      preloadedState: { settings: { ...DEFAULT_SETTINGS, token: "T" } },
      middleware: getDefault => getDefault({ serializableCheck: false, immutableCheck: false })
    });
  });

  const mount = async () => {
    await act(async () => {
      render(<Provider store={store}><AweSuggest id="Sug" /></Provider>);
    });
  };
  const restore = async (action = restoreAction) => {
    await act(async () => {
      store.dispatch(action({ address: { view, component: "ButRst" }, parameters: {} }));
      await new Promise(resolve => setTimeout(resolve, 20));
    });
  };

  it("keeps the label of a selected value hydrated by the server after a restore", async () => {
    register(store, { selected: ["test"], defaultValues: ["test"], values: [{ value: "test", label: LABEL }] });
    await mount();
    expect(input().value).toBe(LABEL);

    await restore();

    expect(input().value).toBe(LABEL);
  });

  it("shows the label of a value without label after a restore", async () => {
    register(store, { selected: ["test"], defaultValues: ["test"], values: [] });
    await mount();
    await releaseLabels();
    expect(input().value).toBe(LABEL);

    await restore();
    await releaseLabels();

    expect(input().value).toBe(LABEL);
  });

  it("applies a label that arrives after the restore", async () => {
    holdLabels();
    register(store, { selected: ["test"], defaultValues: ["test"], values: [] });
    await mount();

    // Restore while the label of the load is still in flight
    await restore();
    await releaseLabels();
    await releaseLabels();

    expect(input().value).toBe(LABEL);
  });

  it("restore-target keeps the label", async () => {
    register(store, { selected: ["test"], defaultValues: ["test"], values: [{ value: "test", label: LABEL }] });
    await mount();
    await restore(restoreTargetAction);
    expect(input().value).toBe(LABEL);
  });

  it("keeps the suggest filled by a launcher dependency after a restore", async () => {
    // Like the criteria test screen: Sug has no value of its own, it takes the one of Txt through a launcher dependency
    register(store, { selected: [], defaultValues: [], values: [] }, {}, true);
    await mount();
    await releaseLabels();
    expect(input().value).toBe(LABEL);

    await restore();
    await releaseLabels();

    expect(input().value).toBe(LABEL);
  });

  it("applies the label of a launcher value that arrives after the restore", async () => {
    holdLabels();
    register(store, { selected: [], defaultValues: [], values: [] }, {}, true);
    await mount();

    // Restore while the label of the first value is still in flight
    await restore();
    await releaseLabels();
    await releaseLabels();

    expect(input().value).toBe(LABEL);
  });
});
