import React from "react";
import fs from "fs";
import path from "path";
import { act, cleanup, fireEvent } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { updateAttributes, updateSpecificAttributes } from "../../../src/redux/actions/components";
import { configureStore } from "@reduxjs/toolkit";
import createRootReducer from "../../../src/redux/reducers";
import { renderWithProviders } from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import ComponentRegistry from "../../../src/redux/registry/ComponentRegistry";
import { getAllComponents } from "../../../src/redux/selectors/componentSelectors";
import { getFormValues } from "../../../src/redux/selectors/form";
import { fillAction, filterAction } from "../../../src/redux/thunks/form";
import { serverAction } from "../../../src/redux/thunks/server";
import { closeAllActions } from "../../../src/redux/actions/actions";

const hook = (testId) => `[data-testid='${testId}']`;
const TOGGLE = hook("grid-row-detail-toggle");
const DETAIL = hook("grid-row-detail");
const VIEW = "report";
const PRODUCTS = { view: VIEW, component: "GrdProducts" };
const SEE_ORDER = { type: "server", parameters: { serverAction: "maintain", targetAction: "SeeOrder" } };
const ORDERS_TEMPLATE = Object.freeze({
  id: "GrdOrders",
  controller: {
    id: "GrdOrders",
    component: "grid",
    serverAction: "data",
    targetAction: "OrdersOfProduct",
    autoload: true,
    visible: true,
    max: 5,
    columnModel: [
      { name: "order", label: "Order", sendable: true },
      { name: "status", label: "Status", sendable: true, component: "button", actions: [SEE_ORDER] }
    ],
    headerModel: [],
    buttonModel: [{ id: "ButOrdSee", label: "See", component: "button", actions: [SEE_ORDER] }]
  },
  model: { values: [] }
});
const PRODUCT_ROWS = [
  { id: 7, name: "Chair", code: { value: "C7", label: "Chair code" }, selected: false },
  { id: 8, name: "Table", code: { value: "T8", label: "Table code" }, selected: false }
];

function screenState({ rowDetail = { style: "row-detail-card", component: ORDERS_TEMPLATE }, settings = {} } = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, ...settings },
    screen: { size: { width: 100, height: 100 } },
    components: {
      CrtName: {
        address: { view: VIEW, component: "CrtName" },
        context: { view: VIEW, source: ["center", "CrtName"] },
        attributes: { id: "CrtName", component: "text" },
        model: { values: [{ value: "Cha", selected: true }] }
      },
      ButScreen: {
        address: { view: VIEW, component: "ButScreen" },
        context: { view: VIEW, source: ["buttons", "ButScreen"] },
        attributes: { id: "ButScreen", component: "button" },
        model: { values: [] }
      },
      GrdProducts: {
        address: PRODUCTS,
        context: { view: VIEW, source: ["center", "GrdProducts"] },
        model: { values: PRODUCT_ROWS, records: PRODUCT_ROWS.length },
        attributes: {
          id: "GrdProducts",
          component: "grid",
          columnModel: [{ name: "name", label: "Name", sendable: true }, { name: "code", label: "Code", hidden: true, sendable: true }],
          headerModel: [],
          buttonModel: [],
          visible: true,
          max: 30,
          ...(rowDetail ? { rowDetail } : {})
        },
        specificAttributes: { sort: [] }
      }
    }
  };
}

// Records every plain action, also the ones the thunks dispatch
function renderScreen({ preloadedState }) {
  const dispatched = [];
  const store = configureStore({
    reducer: createRootReducer({}),
    preloadedState,
    middleware: (getDefaultMiddleware) => getDefaultMiddleware({ serializableCheck: false, immutableCheck: false })
      .concat(() => next => action => {
        dispatched.push(action);
        return next(action);
      })
  });
  const dispatchSpy = { mock: { get calls() { return dispatched.map(action => [action]); } }, mockClear: () => dispatched.splice(0) };
  return { ...renderWithProviders(<AweGrid id="GrdProducts" />, { store }), dispatchSpy };
}
const toggleOf = (container, rowId) => container.querySelector(`${TOGGLE}[row-id='${rowId}']`);
const detailOf = (container, rowId) => container.querySelector(`${DETAIL}[row-id='${rowId}']`);
const nestedGridOf = (container, rowId) => detailOf(container, rowId)?.querySelector(`${hook("grid")}[grid-id='GrdOrders-${rowId}']`);
const queued = (dispatchSpy) => dispatchSpy.mock.calls
  .map(([action]) => action)
  .filter(action => action?.type === "ADD_ACTIONS_TOP")
  .flatMap(action => action.payload);
const filtersOf = (dispatchSpy, component) => queued(dispatchSpy)
  .filter(action => action.type === "filter" && action.address?.component === component);
const instanceIds = (state) => Object.keys(getAllComponents(state)).filter(id => /-(7|8)$/.test(id));
const fill = (store, component, rows) => act(() => {
  store.dispatch(fillAction({ type: "fill", address: { view: VIEW, component }, parameters: { datalist: { rows, records: rows.length, page: 1, total: 1 } } }));
});
const requestOf = async (store, action) => {
  global.fetch.mockClear();
  await act(async () => {
    await store.dispatch(serverAction(action));
  });
  return JSON.parse(global.fetch.mock.calls[0][1].body);
};

describe("awe-react-client/test/js/components/AweGridRowDetailComponentsTest.jsx", () => {
  beforeEach(() => {
    ComponentRegistry.clearAll();
    global.fetch = jest.fn(() => Promise.resolve({ ok: true, status: 200, json: () => Promise.resolve([]) }));
    window.fetch = global.fetch;
  });

  afterEach(() => {
    cleanup();
    ComponentRegistry.clearAll();
    jest.restoreAllMocks();
  });

  it("registers one instance per open row with its own id and a clone of the template, and renders it in the detail", () => {
    const { container, store, dispatchSpy } = renderScreen({ preloadedState: screenState() });

    expect(instanceIds(store.getState())).toEqual([]);
    fireEvent.click(toggleOf(container, 7));

    const instance = getAllComponents(store.getState())["GrdOrders-7"];
    expect(instance.address).toEqual({ view: VIEW, component: "GrdOrders-7" });
    expect(instance.context).toEqual({ view: VIEW, source: ["center", "GrdProducts", "GrdOrders-7"] });
    expect(instance.attributes.id).toBe("GrdOrders");
    expect(instance.attributes.targetAction).toBe("OrdersOfProduct");
    expect(instance.attributes.columnModel.map(column => column.name)).toEqual(["order", "status"]);
    expect(instance.attributes.buttonModel.map(button => button.id)).toEqual(["ButOrdSee-7"]);
    expect(instance.attributes.autoload).toBe(false);
    expect(instance.specificAttributes).toEqual({ max: 5, rows: 5, sort: [], page: 1, first: 0 });
    expect(instance.model.values).toEqual([]);
    expect(getAllComponents(store.getState())["ButOrdSee-7"].address).toEqual({ view: VIEW, component: "ButOrdSee-7" });
    // The template is not touched and is not a component
    expect(ORDERS_TEMPLATE.controller.buttonModel[0].id).toBe("ButOrdSee");
    expect(getAllComponents(store.getState()).GrdOrders).toBeUndefined();
    // It loads once, with filter
    expect(filtersOf(dispatchSpy, "GrdOrders-7")).toEqual([{ type: "filter", address: { view: VIEW, component: "GrdOrders-7" } }]);
    // The detail keeps the toggle, its classes and test ids, and shows the nested grid
    const detail = detailOf(container, 7);
    expect([...detail.classList]).toEqual(["grid-row-detail", "grid-row-detail-components", "row-detail-card"]);
    expect(toggleOf(container, 7).getAttribute("aria-expanded")).toBe("true");
    expect(nestedGridOf(container, 7)).not.toBeNull();
    expect(detail.textContent).not.toContain("undefined");
  });

  it("points the empty state action of an instance to the button of the same instance", () => {
    const template = { ...ORDERS_TEMPLATE, controller: { ...ORDERS_TEMPLATE.controller, emptyAction: "ButOrdSee" } };
    const { container, store } = renderScreen({ preloadedState: screenState({ rowDetail: { component: template } }) });

    fireEvent.click(toggleOf(container, 7));

    expect(getAllComponents(store.getState())["GrdOrders-7"].attributes.emptyAction).toBe("ButOrdSee-7");
    expect(template.controller.emptyAction).toBe("ButOrdSee");
  });

  it("sends every column of the parent row (also hidden ones, cell objects as their value) only with the requests of its instance", () => {
    const { container, store, dispatchSpy } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));

    dispatchSpy.mockClear();
    act(() => {
      store.dispatch(filterAction({ type: "filter", address: { view: VIEW, component: "GrdOrders-7" } }));
    });
    const request = queued(dispatchSpy).find(action => action.type === "server");

    expect(request.address).toEqual({ view: VIEW, component: "GrdOrders-7" });
    expect(request.parameters).toMatchObject({
      "GrdProducts.row.id": 7,
      "GrdProducts.row.name": "Chair",
      "GrdProducts.row.code": "C7",
      CrtName: "Cha",
      GrdProducts: [],
      max: 5,
      sort: [],
      page: 1,
      targetAction: "OrdersOfProduct",
      component: "GrdOrders-7"
    });
    expect(Object.keys(request.parameters).filter(key => key.startsWith("GrdProducts.row."))).toEqual([
      "GrdProducts.row.id", "GrdProducts.row.name", "GrdProducts.row.code"
    ]);
    // Neither the other instance nor its parent row
    expect(request.parameters).not.toHaveProperty("GrdOrders-8");
    expect(Object.values(request.parameters)).not.toContain("Table");

    // A screen component filter does not carry them
    dispatchSpy.mockClear();
    act(() => {
      store.dispatch(filterAction({ type: "filter", address: PRODUCTS }));
    });
    const screenRequest = queued(dispatchSpy).find(action => action.type === "server");
    expect(Object.keys(screenRequest.parameters).filter(key => key.includes(".row.") || key.startsWith("GrdOrders"))).toEqual([]);
  });

  it("keeps two open rows as independent instances: rows, sort and page of one do not change the other", () => {
    const { container, store } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));

    fill(store, "GrdOrders-7", [{ id: "o1", order: "Order one" }, { id: "o2", order: "Order two" }]);
    act(() => {
      store.dispatch(updateSpecificAttributes({ view: VIEW, component: "GrdOrders-7" }, { sort: [{ id: "order", direction: "desc" }], page: 2 }));
    });

    const components = getAllComponents(store.getState());
    expect(components["GrdOrders-7"].model.values.map(row => row.id)).toEqual(["o1", "o2"]);
    expect(components["GrdOrders-8"].model.values).toEqual([]);
    expect(components["GrdOrders-8"].specificAttributes).toMatchObject({ sort: [], page: 1 });
    expect(nestedGridOf(container, 7).textContent).toContain("Order two");
    expect(nestedGridOf(container, 8).textContent).not.toContain("Order two");
    // The nested grid is not the parent: the parent rows are still the products
    expect(components.GrdProducts.model.values.map(row => row.id)).toEqual([7, 8]);
  });

  it("removes the instance and its buttons from the state and the registry when its row closes, and a late response does not bring them back", () => {
    const { container, store } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));

    fireEvent.click(toggleOf(container, 7));

    expect(instanceIds(store.getState())).toEqual(["GrdOrders-8", "ButOrdSee-8"]);
    expect(store.getState().components).not.toHaveProperty("GrdOrders-7");
    expect(ComponentRegistry.get("GrdOrders-7")).toBeNull();
    expect(ComponentRegistry.get("ButOrdSee-7")).toBeNull();
    expect(detailOf(container, 7)).toBeNull();

    fill(store, "GrdOrders-7", [{ id: "o1", order: "Late" }]);
    act(() => {
      store.dispatch(updateAttributes({ view: VIEW, component: "GrdOrders-7" }, { loading: false }));
    });
    expect(store.getState().components).not.toHaveProperty("GrdOrders-7");
    expect(ComponentRegistry.get("GrdOrders-7")).toBeNull();
  });

  it("removes every instance when the parent reloads its data", () => {
    const { container, store } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));
    expect(instanceIds(store.getState())).toHaveLength(4);

    act(() => {
      store.dispatch(updateAttributes(PRODUCTS, { loading: true }));
    });

    expect(instanceIds(store.getState())).toEqual([]);
    expect(ComponentRegistry.getAllIds().filter(id => id.startsWith("GrdOrders") || id.startsWith("ButOrdSee"))).toEqual([]);
    expect(container.querySelector(DETAIL)).toBeNull();
  });

  it("removes every instance when the parent grid is unmounted", () => {
    const { container, store, unmount } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));
    expect(instanceIds(store.getState())).toHaveLength(4);

    unmount();

    expect(instanceIds(store.getState())).toEqual([]);
    expect(ComponentRegistry.getAllIds().filter(id => /-(7|8)$/.test(id))).toEqual([]);
  });

  it("excludes the instances from the form values of the screen without duplicate warnings", () => {
    const { container, store } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));
    fill(store, "GrdOrders-7", [{ id: "o1", order: "Order one", selected: true }]);
    expect(getAllComponents(store.getState())["GrdOrders-7"].model.values).toHaveLength(1);
    const warn = jest.spyOn(console, "warn");

    const values = getFormValues(store.getState());

    expect(values.CrtName).toBe("Cha");
    expect(values.GrdProducts).toEqual([]);
    expect(values).not.toHaveProperty("GrdOrders");
    expect(values).not.toHaveProperty("order");
    expect(Object.keys(values).filter(key => key.includes(".row.") || key.startsWith("ButOrdSee"))).toEqual([]);
    expect(warn).not.toHaveBeenCalled();
  });

  it("renders the buttons and button columns of the nested grid within their instance, and their requests carry its values", async () => {
    const { container, store, dispatchSpy } = renderScreen({ preloadedState: screenState() });
    fireEvent.click(toggleOf(container, 7));
    fireEvent.click(toggleOf(container, 8));
    fill(store, "GrdOrders-7", [{ id: "o1", order: "Order one", status: { label: "Open", value: "O" }, selected: true }, { id: "o2", order: "Order two" }]);

    // The queued loads are not run here: finish them so the buttons are enabled
    act(() => {
      store.dispatch(closeAllActions());
    });

    // Toolbar button of the nested grid
    const button = detailOf(container, 7).querySelector("#ButOrdSee-7");
    expect(button).not.toBeNull();
    expect(button.textContent).toContain("See");
    dispatchSpy.mockClear();
    fireEvent.click(button);
    const buttonAction = queued(dispatchSpy).find(action => action.type === "server");
    expect(buttonAction.address).toEqual({ view: VIEW, component: "ButOrdSee-7" });

    const buttonRequest = await requestOf(store, buttonAction);
    expect(buttonRequest).toMatchObject({
      CrtName: "Cha",
      GrdOrders: ["o1"],
      "GrdOrders.selected": "o1",
      order: ["Order one"],
      "GrdProducts.row.id": 7,
      "GrdProducts.row.code": "C7"
    });

    // Button column of the nested grid
    const cellButton = nestedGridOf(container, 7).querySelector(`${hook("grid-row")}[row-id='o1'] button${hook("button")}`);
    dispatchSpy.mockClear();
    fireEvent.click(cellButton);
    const cellAction = queued(dispatchSpy).find(action => action.type === "server");
    expect(cellAction.address).toEqual({ view: VIEW, component: "GrdOrders-7", column: "status", row: "o1" });
    const cellRequest = await requestOf(store, cellAction);
    expect(cellRequest).toMatchObject({ GrdOrders: ["o1"], "GrdProducts.row.id": 7 });

    // A screen button does not get the values of any instance
    const screenRequest = await requestOf(store, { ...SEE_ORDER, address: { view: VIEW, component: "ButScreen" } });
    expect(screenRequest.CrtName).toBe("Cha");
    expect(Object.keys(screenRequest).filter(key => key.includes(".row.") || key.startsWith("GrdOrders") || key === "order")).toEqual([]);
  });

  it("gives the nested grid a fixed height, since the detail has no height for its flexible table to fill", () => {
    const styles = fs.readFileSync(path.join(__dirname, "../../../src/components/AweGrid.less"), "utf8");
    const rule = /\.grid-row-detail\.grid-row-detail-components\s*\{([\s\S]*?)\n\}/.exec(styles)?.[1] || "";

    expect(rule).toMatch(/>\s*\.grid-container\s*\{\s*height:\s*calc\(var\(--awe-size-control\) \* 7\);/);
    expect(rule).toMatch(/white-space:\s*normal;/);
  });

  it("registers nothing for a grid with a template row detail (delivery 1 unchanged)", () => {
    const { container, store, dispatchSpy } = renderScreen({ preloadedState: screenState({ rowDetail: { template: "Code {code}" } }) });

    fireEvent.click(toggleOf(container, 7));

    expect(detailOf(container, 7).textContent).toBe("Code Chair code");
    expect(Object.keys(getAllComponents(store.getState()))).toEqual(["CrtName", "ButScreen", "GrdProducts"]);
    expect(queued(dispatchSpy).filter(action => action.type === "filter")).toEqual([]);
  });

  it("registers nothing for a grid without row detail", () => {
    const { container, store, dispatchSpy } = renderScreen({ preloadedState: screenState({ rowDetail: null }) });

    expect(container.querySelector(TOGGLE)).toBeNull();
    expect(Object.keys(getAllComponents(store.getState()))).toEqual(["CrtName", "ButScreen", "GrdProducts"]);
    expect(queued(dispatchSpy).filter(action => action.type === "filter")).toEqual([]);
  });
});
