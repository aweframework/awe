import {cleanupHeavyComponentTest, initHeavyComponentTest} from "./heavyComponentTestUtils";
import {cacheGridTemplates} from "./gridTestUtils";

const hook = testId => `[data-testid='${testId}']`;
const VIEW = "base";
const GRID = "GrdUsr";
const ROWS = [{id: 1, name: "Carmen"}, {id: 2, name: "David"}, {id: 3, name: "Elena"}];

const button = (id, extra = {}) => ({id, label: `LABEL_${id}`, icon: "trash", visible: true, actions: [
  {type: "server", parameters: {serverAction: "maintain", targetAction: `Mnt${id}`}}
], ...extra});

/**
 * Selection bar of a grid (#793): the buttons of the grid with selection="true" leave the footer and are shown in a bar
 * between the toolbar and the header while there are selected rows, with the number of selected rows
 */
describe("grid selection bar", () => {
  let refs;
  let element;
  let scope;
  let $serverData;
  let $actionController;

  const storage = kind => refs.$storage.get(kind)[VIEW];
  const digest = () => refs.$rootScope.$digest();
  const bar = () => element.find(hook("grid-selection-bar"));
  const footerButtons = () => element.find(".table-footer button").map((index, node) => node.id).get();
  const barButtons = () => bar().find("button").map((index, node) => node.id).get();

  const store = (id, controller, model = {}) => {
    $serverData.storeComponent({id, controller, model}, storage("model"), storage("controller"), storage("api"), "users");
  };

  /**
   * Mount a grid with its buttons, as the server renders it (the buttons inside the grid), with the real storage
   * @param {object[]} buttons Buttons of the grid
   * @param {object} controller Grid controller attributes
   * @returns {object} Grid component
   */
  const mount = (buttons, controller = {}) => {
    inject(["$templateCache", cacheGridTemplates]);
    ["controller", "model", "api", "messages", "screen", "status"].forEach(kind => refs.$storage.put(kind, {[VIEW]: {}}));
    refs.$storage.get("screen")[VIEW] = {name: "users", option: "users"};
    refs.$storage.get("status")[VIEW] = "loaded";
    refs.$storage.put("actions-running", false);
    store(GRID, {
      id: GRID, visible: true, loading: false, serverAction: "data", targetAction: "UsrLst", loadAll: false,
      multiselect: true, editable: false, enableFilters: false, showTotals: false, disablePagination: false, max: 10,
      pagerValues: [10, 20], columnModel: [{name: "name", id: "name", label: "Name", sendable: true}],
      buttonModel: buttons, headerModel: [], ...controller
    }, {page: 1, records: ROWS.length, total: 1, selected: [], values: ROWS.map(row => ({...row}))});
    buttons.forEach(definition => store(definition.id, {...definition}));

    jest.spyOn(refs.$utilities, "timeout").mockImplementation(callback => callback());
    refs.$utilities.timeout.cancel = jest.fn();
    jest.spyOn(console, "error").mockImplementation(() => undefined);

    scope = refs.$rootScope.$new();
    const children = buttons.map(definition => `<awe-button button-id='${definition.id}'></awe-button>`).join("");
    const form = refs.$compile(`<form awe-form><awe-grid grid-id='${GRID}' style='height: 300px; width: 400px'>${children}</awe-grid></form>`)(scope);
    $(document.body).append(form);
    digest();
    element = form.find(".grid").first();
    return element.isolateScope().component;
  };

  // The rows are selected in the grid, as a click on them does
  const select = (component, ids) => {
    component.grid.api.selection.clearSelectedRows();
    ids.forEach(id => component.grid.api.selection.selectRow(component.model.values.find(row => row.id === id)));
    digest();
  };

  beforeEach(() => {
    refs = initHeavyComponentTest();
    inject(["ServerData", "ActionController", "$translate", (ServerData, ActionController, $translate) => {
      $serverData = ServerData;
      $actionController = ActionController;
      // The count is interpolated in the text of the bar
      const instant = $translate.instant.bind($translate);
      jest.spyOn($translate, "instant").mockImplementation((key, parameters) =>
        parameters ? `${key}(${parameters.count})` : instant(key));
    }]);
  });

  afterEach(() => {
    element?.closest("form").remove();
    scope?.$destroy();
    element = null;
    cleanupHeavyComponentTest();
  });

  it("moves the selection buttons out of the footer into the bar, in the order of the screen, and keeps the others", () => {
    mount([button("ButNew"), button("ButRst", {selection: true}), button("ButExp"), button("ButDel", {selection: true})]);

    expect(footerButtons()).toEqual(["ButNew", "ButExp"]);
    expect(barButtons()).toEqual(["ButRst", "ButDel"]);
    expect(bar().attr("data-testid-owner")).toBe(GRID);
  });

  it("places the bar between the toolbar and the table header", () => {
    mount([button("ButDel", {selection: true})]);

    const previous = $(bar()[0].previousElementSibling);
    const next = $(bar()[0].nextElementSibling);
    expect(previous.is(hook("grid-toolbar")) || previous.find(hook("grid-toolbar")).length === 1).toBe(true);
    expect(next.hasClass("grid-container")).toBe(true);
  });

  it("is hidden while no row is selected", () => {
    mount([button("ButDel", {selection: true})]);

    expect(bar().length).toBe(1);
    expect(bar().hasClass("is-visible")).toBe(false);
    expect(bar().attr("data-count")).toBe("0");
  });

  it("shows the number of selected rows with the singular and plural texts", () => {
    const component = mount([button("ButDel", {selection: true})]);

    select(component, [2]);
    expect(bar().hasClass("is-visible")).toBe(true);
    expect(bar().attr("data-count")).toBe("1");
    expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("SCREEN_TEXT_GRID_SELECTED_ROW(1)");

    select(component, [1, 3]);
    expect(bar().attr("data-count")).toBe("2");
    expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("SCREEN_TEXT_GRID_SELECTED_ROWS(2)");
  });

  it("is a polite live region with a check icon", () => {
    mount([button("ButDel", {selection: true})]);

    expect(bar().attr("aria-live")).toBe("polite");
    expect(bar().find(".grid-selection-bar-icon svg").attr("aria-hidden")).toBe("true");
    expect(bar().find(".grid-selection-bar-icon svg use").attr("href")).toContain("#circle-check");
  });

  it("hides again when the selection is cleared, as a reload of the grid does", () => {
    const component = mount([button("ButDel", {selection: true})]);
    select(component, [1, 2]);

    component.unselectRows();
    digest();

    expect(bar().hasClass("is-visible")).toBe(false);
    expect(bar().attr("data-count")).toBe("0");
  });

  it("shows the one selected row of a grid without multiselect", () => {
    const component = mount([button("ButDel", {selection: true})], {multiselect: false});

    select(component, [3]);

    expect(bar().hasClass("is-visible")).toBe(true);
    expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("SCREEN_TEXT_GRID_SELECTED_ROW(1)");
  });

  it("runs the actions of a button of the bar as from the footer", () => {
    const component = mount([button("ButRst", {selection: true})]);
    select(component, [1]);
    const addActionList = jest.spyOn($actionController, "addActionList").mockImplementation(() => undefined);

    bar().find("button#ButRst")[0].click();
    digest();

    expect(addActionList).toHaveBeenCalledWith(storage("controller").ButRst.actions, true,
      expect.objectContaining({address: expect.objectContaining({component: "ButRst"})}));
  });

  it("keeps the style of a btn-danger button, drawn as the danger variant", () => {
    mount([button("ButRst", {selection: true}), button("ButDel", {selection: true, style: "btn-danger"})]);

    const reset = bar().find("button#ButRst");
    const remove = bar().find("button#ButDel");
    expect(reset.hasClass("btn-danger")).toBe(false);
    expect(remove.hasClass("btn-danger")).toBe(true);
    expect(bar().find(".grid-selection-bar-actions").children().length).toBe(2);
  });

  it("leaves a grid without selection buttons as it was: no bar, every button in the footer", () => {
    mount([button("ButNew"), button("ButDel")]);

    expect(bar().length).toBe(0);
    expect(footerButtons()).toEqual(["ButNew", "ButDel"]);
  });
});
