import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates} from "./gridTestUtils";

const hook = testId => `[data-testid='${testId}']`;
const TOGGLE = hook("grid-row-detail-toggle");
const DETAIL = hook("grid-row-detail");
const VIEW = "base";
const PARENT = "GrdProducts";
const TEMPLATE = "GrdOrders";
const BUTTON = "ButShowOrders";
const ROWS = [
  {id: 7, name: "Chair", code: {value: "C7", label: "Chair code"}, price: 10, selected: false, $internal: "skip"},
  {id: 8, name: "Table", code: {value: "T8", label: "Table code"}, price: 20}
];

/**
 * Controller of the nested grid as the server sends it in the components mode of a row detail (L8): a full grid
 * controller, deferred, without toolbar, with its own buttons
 * @returns {object} Grid controller
 */
const nestedController = () => ({
  id: TEMPLATE,
  visible: true,
  serverAction: "data",
  targetAction: "getProductOrders",
  initialLoad: "query",
  autoload: true,
  deferred: true,
  showToolbar: false,
  loadAll: false,
  multiselect: false,
  editable: false,
  max: 5,
  pagerValues: [5, 10],
  emptyAction: BUTTON,
  columnModel: [{name: "order", id: "order", label: "Order", sendable: true}],
  buttonModel: [{id: BUTTON, label: "Show", visible: true, actions: [
    {type: "server", parameters: {serverAction: "maintain", targetAction: "ShowOrders"}}
  ]}],
  headerModel: []
});

const parentController = (rowDetail) => ({
  id: PARENT,
  visible: true,
  loading: false,
  serverAction: "data",
  targetAction: "getProducts",
  loadAll: false,
  multiselect: false,
  editable: false,
  enableFilters: false,
  showTotals: false,
  disablePagination: false,
  max: 10,
  pagerValues: [10, 20],
  columnModel: [{name: "name", id: "name", label: "Name", sendable: true}, {name: "price", id: "price", label: "Price", hidden: true, sendable: true}],
  buttonModel: [],
  headerModel: [],
  rowDetail
});

/**
 * The AngularJS grid creates one instance of the grid of a row detail in components mode per open row, as the React
 * grid does (L10): its own controller, model and api in the storage, loaded once with the fields of its parent row
 */
describe("grid row detail with components (awe-grid inside awe-grid)", () => {
  let refs;
  let element;
  let scope;
  let messages;
  let $serverData;

  const storage = kind => refs.$storage.get(kind)[VIEW];
  const find = selector => element.find(selector);
  const toggleOf = rowId => find(`${TOGGLE}[row-id='${rowId}']`);
  const detailOf = rowId => find(`${DETAIL}[row-id='${rowId}']`);
  const digest = () => refs.$rootScope.$digest();
  const click = node => {
    node[0].click();
    digest();
  };

  /**
   * Store a component as the screen load does and return its controller
   */
  const store = (id, controller, model) => {
    $serverData.storeComponent({id, controller, model}, storage("model"), storage("controller"), storage("api"), "products");
    return storage("controller")[id];
  };

  /**
   * Mount a screen with a parent grid (and a screen criterion) inside a form, with the real storage
   */
  const mount = (rowDetail = {component: {id: TEMPLATE, controller: nestedController(), model: {}}}) => {
    inject(["$templateCache", cacheGridTemplates]);
    ["controller", "model", "api", "messages", "screen", "status"].forEach(kind => refs.$storage.put(kind, {[VIEW]: {}}));
    refs.$storage.get("screen")[VIEW] = {name: "products", option: "products"};
    refs.$storage.get("status")[VIEW] = "loaded";
    refs.$storage.put("actions-running", false);
    store(PARENT, parentController(rowDetail), {page: 1, records: ROWS.length, total: 1, selected: [], values: ROWS.map(row => ({...row}))});
    store("CrtScreen", {id: "CrtScreen", visible: true}, {selected: "screen value"});
    storage("api").CrtScreen = {getData: () => ({CrtScreen: "screen value"})};

    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    jest.spyOn(console, "error").mockImplementation(() => undefined);
    // Every request is recorded and finished at once, so the next action of the stack runs
    inject(["Connection", "ActionController", (Connection, ActionController) => {
      jest.spyOn(Connection, "sendMessage").mockImplementation(message => {
        messages.push(message);
        ActionController.acceptAction(message.action);
      });
    }]);

    scope = refs.$rootScope.$new();
    const form = refs.$compile(`<form awe-form><awe-grid grid-id='${PARENT}' style='height: 300px; width: 400px'></awe-grid></form>`)(scope);
    $(document.body).append(form);
    digest();
    element = form.find(".grid").first();
    return element.isolateScope().component;
  };

  const requestsOf = component => messages.filter(message => message.target?.component === component);

  beforeEach(() => {
    refs = initHeavyComponentTest();
    messages = [];
    inject(["ServerData", ServerData => {
      $serverData = ServerData;
    }]);
  });

  afterEach(() => {
    element?.closest("form").remove();
    scope?.$destroy();
    element = null;
    cleanupHeavyComponentTest();
  });

  it("registers an instance of the nested grid and of its buttons for the open row and compiles it in its detail", () => {
    mount();

    click(toggleOf(7));

    const controller = storage("controller")[`${TEMPLATE}-7`];
    expect(controller).toBeDefined();
    expect(controller.autoload).toBe(false);
    expect(controller.optionId).toBe("products");
    expect(controller.buttonModel.map(button => button.id)).toEqual([`${BUTTON}-7`]);
    expect(controller.emptyAction).toBe(`${BUTTON}-7`);
    expect(storage("model")[`${TEMPLATE}-7`]).toEqual(expect.objectContaining({values: [], selected: null}));
    expect(storage("controller")[`${BUTTON}-7`]).toEqual(expect.objectContaining({label: "Show"}));
    expect(storage("api")[`${TEMPLATE}-7`].getData).toEqual(expect.any(Function));
    // The template itself is never a component of the screen
    expect(storage("controller")[TEMPLATE]).toBeUndefined();

    const detail = detailOf(7);
    expect(detail.length).toBe(1);
    expect(detail.find(`.grid[id='${TEMPLATE}-7']`).length).toBe(1);
    expect(detail.find(`button[id='${BUTTON}-7']`).length).toBe(1);
    expect(detail.hasClass("grid-row-detail")).toBe(true);
    // Its buttons are the ones of the instance, inside the nested grid
    expect(detail.find(`.grid[id='${TEMPLATE}-7'] button[id='${BUTTON}-7']`).length).toBe(1);
  });

  it("loads the instance once with the fields of its parent row, also the hidden ones, and its own data", () => {
    mount();

    click(toggleOf(7));
    digest();

    const requests = requestsOf(`${TEMPLATE}-7`);
    expect(requests.length).toBe(1);
    const values = requests[0].values;
    expect(values).toEqual(expect.objectContaining({
      [`${PARENT}.row.id`]: 7,
      [`${PARENT}.row.name`]: "Chair",
      [`${PARENT}.row.code`]: "C7",
      [`${PARENT}.row.price`]: 10,
      targetAction: "getProductOrders",
      max: 5,
      page: 1,
      CrtScreen: "screen value"
    }));
    expect(values[`${PARENT}.row.selected`]).toBeUndefined();
    expect(values[`${PARENT}.row.$internal`]).toBeUndefined();
    // The data of the instance goes under the keys of the template (as the React client)
    expect(values).toHaveProperty(TEMPLATE);
    expect(values[`${TEMPLATE}.data`]).toEqual(expect.objectContaining({max: 5, page: 1}));
    expect(values).toHaveProperty("order");
    expect(values).not.toHaveProperty([`${TEMPLATE}-7`]);
    expect(values).not.toHaveProperty([`${TEMPLATE}-7.data`]);
  });

  it("keeps each open row with its own instance and the parameters of its own row only", () => {
    mount();

    click(toggleOf(7));
    click(toggleOf(8));

    expect(storage("controller")[`${TEMPLATE}-7`]).not.toBe(storage("controller")[`${TEMPLATE}-8`]);
    expect(storage("model")[`${TEMPLATE}-7`]).not.toBe(storage("model")[`${TEMPLATE}-8`]);
    const first = requestsOf(`${TEMPLATE}-7`);
    const second = requestsOf(`${TEMPLATE}-8`);
    expect(first.length).toBe(1);
    expect(second.length).toBe(1);
    expect(first[0].values[`${PARENT}.row.id`]).toBe(7);
    expect(second[0].values[`${PARENT}.row.id`]).toBe(8);
    expect(second[0].values[`${PARENT}.row.name`]).toBe("Table");

    // The data of an instance does not change the other one (independent paging)
    storage("model")[`${TEMPLATE}-7`].page = 3;
    expect(storage("model")[`${TEMPLATE}-8`].page).not.toBe(3);
  });

  it("pages each instance by itself: its requests send its own page", () => {
    mount();
    click(toggleOf(7));
    click(toggleOf(8));
    const nested = detailOf(7).find(".grid").first().isolateScope().component;
    nested.model.records = 12;
    nested.model.total = 3;

    nested.setPage(2);
    digest();

    const requests = requestsOf(`${TEMPLATE}-7`);
    expect(requests.length).toBe(2);
    expect(requests[1].values.page).toBe(2);
    expect(requests[1].values[`${TEMPLATE}.data`].page).toBe(2);
    expect(requests[1].values[`${PARENT}.row.id`]).toBe(7);
    expect(requestsOf(`${TEMPLATE}-8`).length).toBe(1);
    expect($serverData.getFormValues({view: VIEW, component: `${TEMPLATE}-8`})[`${TEMPLATE}.data`].page).toBe(1);
  });

  it("checks the size of the parent when the rows of the nested grid are rendered", () => {
    const parent = mount();
    click(toggleOf(7));
    const nestedScope = detailOf(7).find(".grid").first().isolateScope();
    const update = jest.spyOn(parent, "updateGridScrollBars").mockImplementation(() => undefined);

    nestedScope.$broadcast("rows-rendered", {grid: PARENT});
    expect(update).not.toHaveBeenCalled();
    nestedScope.$broadcast("rows-rendered", {grid: `${TEMPLATE}-7`});
    expect(update).toHaveBeenCalledTimes(1);
  });

  it("keeps the instance of an open row whose detail leaves the rendered rows (client-side page), and does not load it again", () => {
    const parent = mount();
    click(toggleOf(7));
    const instance = storage("controller")[`${TEMPLATE}-7`];
    const row = detailOf(7).scope().row;

    // ui-grid stops rendering the row: the nested grid goes, its state stays while the row is open
    detailOf(7).scope().$destroy();
    digest();

    expect(parent.getRowDetailInstance(row)).toEqual(expect.objectContaining({id: `${TEMPLATE}-7`, loaded: true}));

    expect(storage("controller")[`${TEMPLATE}-7`]).toBe(instance);
    expect(requestsOf(`${TEMPLATE}-7`).length).toBe(1);
  });

  it("fills each instance by its own address", () => {
    mount();
    click(toggleOf(7));
    click(toggleOf(8));

    refs.$control.changeModelAttribute({view: VIEW, component: `${TEMPLATE}-7`}, {values: [{id: "o1", order: "Order 1"}], records: 1, page: 1, total: 1}, true);
    digest();

    expect(storage("model")[`${TEMPLATE}-7`].values.map(row => row.order)).toEqual(["Order 1"]);
    expect(storage("model")[`${TEMPLATE}-8`].values).toEqual([]);
  });

  it("removes the instance and its buttons from the storage when the row closes", () => {
    mount();
    click(toggleOf(7));
    expect(storage("controller")[`${BUTTON}-7`]).toBeDefined();

    click(toggleOf(7));

    expect(detailOf(7).length).toBe(0);
    ["controller", "model", "api"].forEach(kind => {
      expect(storage(kind)[`${TEMPLATE}-7`]).toBeUndefined();
      expect(storage(kind)[`${BUTTON}-7`]).toBeUndefined();
    });
  });

  it("removes every instance when the parent gets new data, and a late fill does not create it again", () => {
    const parent = mount();
    click(toggleOf(7));
    click(toggleOf(8));
    expect(storage("controller")[`${TEMPLATE}-8`]).toBeDefined();

    parent.api.updateModelValues({values: ROWS.map(row => ({...row})), records: 2, page: 1, total: 1});
    digest();

    expect(find(DETAIL).length).toBe(0);
    ["controller", "model", "api"].forEach(kind => {
      expect(Object.keys(storage(kind)).filter(id => id.startsWith(TEMPLATE) || id.startsWith(BUTTON))).toEqual([]);
    });

    refs.$control.changeModelAttribute({view: VIEW, component: `${TEMPLATE}-7`}, {values: [{id: "o1"}]}, true);
    digest();
    ["controller", "model", "api"].forEach(kind => expect(storage(kind)[`${TEMPLATE}-7`]).toBeUndefined());
  });

  it("removes every instance when the parent grid is destroyed", () => {
    mount();
    click(toggleOf(7));
    expect(storage("controller")[`${TEMPLATE}-7`]).toBeDefined();

    element.isolateScope().$destroy();

    ["controller", "model", "api"].forEach(kind => {
      expect(storage(kind)[`${TEMPLATE}-7`]).toBeUndefined();
      expect(storage(kind)[`${BUTTON}-7`]).toBeUndefined();
    });
    // The parent itself stays: the screen owns it
    expect(storage("controller")[PARENT]).toBeDefined();
  });

  it("leaves the instances out of the form values of the screen, without overwriting warnings", () => {
    mount();
    click(toggleOf(7));
    click(toggleOf(8));
    expect(storage("api")[`${TEMPLATE}-8`].getData).toEqual(expect.any(Function));
    let warn;
    inject(["$log", $log => {
      warn = jest.spyOn($log, "warn");
    }]);

    const values = $serverData.getFormValues();

    expect(warn).not.toHaveBeenCalled();

    expect(values.CrtScreen).toBe("screen value");
    expect(values).toHaveProperty(PARENT);
    expect(Object.keys(values).filter(key => key.startsWith(TEMPLATE) || key.startsWith(BUTTON))).toEqual([]);
    expect(Object.keys(values).filter(key => key.includes(".row."))).toEqual([]);
    expect(values).not.toHaveProperty("order");
  });

  it("sends the instance data and its parent row with the requests of the buttons of the nested grid, not with the screen ones", () => {
    mount();
    click(toggleOf(7));
    messages.length = 0;

    click(detailOf(7).find(`button[id='${BUTTON}-7']`));

    const requests = requestsOf(`${BUTTON}-7`);
    expect(requests.length).toBe(1);
    expect(requests[0].values).toEqual(expect.objectContaining({
      [`${PARENT}.row.id`]: 7,
      targetAction: "ShowOrders",
      CrtScreen: "screen value"
    }));
    expect(requests[0].values).toHaveProperty([`${TEMPLATE}.data`]);

    // A request of a screen component carries no instance data
    messages.length = 0;
    inject(["ActionController", ActionController => {
      ActionController.addActionList([{type: "server", parameters: {serverAction: "maintain", targetAction: "Save"}}], true, {address: {view: VIEW, component: "CrtScreen"}});
    }]);
    digest();
    const screenRequest = requestsOf("CrtScreen");
    expect(screenRequest.length).toBe(1);
    expect(Object.keys(screenRequest[0].values).filter(key => key.startsWith(TEMPLATE) || key.includes(".row."))).toEqual([]);
  });

  it("sends the instance data with the requests of the cells of the nested grid", () => {
    mount();
    click(toggleOf(7));

    const values = $serverData.getFormValues({view: VIEW, component: `${TEMPLATE}-7`, row: "o1", column: "order"});

    expect(values[`${PARENT}.row.id`]).toBe(7);
    expect(values).toHaveProperty([`${TEMPLATE}.data`]);
  });

  it("keeps the template mode unchanged: plain text detail and no instances", () => {
    mount({template: "Product {name}"});

    click(toggleOf(7));

    expect(detailOf(7).text()).toBe("Product Chair");
    expect(detailOf(7).find(".grid").length).toBe(0);
    expect(Object.keys(storage("controller")).sort()).toEqual(["CrtScreen", PARENT]);
  });

  it("keeps a grid without row detail unchanged in the form values", () => {
    mount(null);

    const values = $serverData.getFormValues();

    expect(find(TOGGLE).length).toBe(0);
    expect(values).toHaveProperty(PARENT);
    expect(values).toHaveProperty([`${PARENT}.data`]);
    expect(values.CrtScreen).toBe("screen value");
  });
});
