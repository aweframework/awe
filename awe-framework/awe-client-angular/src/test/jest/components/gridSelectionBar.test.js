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
 * while there are selected rows, with the number of selected rows. The bar takes the band of the toolbar, or the row of
 * the column headers of a grid without toolbar, so the rows never move; it shows the filter of the selection, clears it
 * with its button or Esc and gives the focus back to the search or the select-all checkbox
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
  const barButtons = () => bar().find(".grid-selection-bar-actions button").map((index, node) => node.id).get();
  const clearButton = () => bar().find(hook("grid-selection-clear"));
  const context = () => bar().find(".grid-selection-bar-context");
  const searchInput = () => element.find(`${hook("grid-search")} input`);
  const selectAll = () => element.find(`${hook("grid-header-checkbox")} input`);
  const pressEscape = target => {
    const event = new KeyboardEvent("keydown", {key: "Escape", bubbles: true, cancelable: true});
    target.dispatchEvent(event);
    digest();
    return event;
  };

  const store = (id, controller, model = {}) => {
    $serverData.storeComponent({id, controller, model}, storage("model"), storage("controller"), storage("api"), "users");
  };

  /**
   * Mount a grid with its buttons, as the server renders it (the buttons inside the grid), with the real storage
   * @param {object[]} buttons Buttons of the grid
   * @param {object} controller Grid controller attributes
   * @param {{search?: string, quickFilter?: string}} values Values of the search (CrtTxt) and the quick filter (CrtSta)
   * @returns {object} Grid component
   */
  const mount = (buttons, controller = {}, {search = "", quickFilter = ""} = {}) => {
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
    store("CrtTxt", {id: "CrtTxt", component: "grid-search", visible: true, contextMenu: [], dependencies: []},
      {selected: search, values: [], page: 1, records: 0, total: 0});
    store("CrtSta", {id: "CrtSta", component: "quick-filter", visible: true, contextMenu: [], dependencies: []},
      {selected: quickFilter, values: [{value: "", label: "LABEL_ALL", count: 3}, {value: "LCK", label: "LABEL_LOCKED", count: 1}],
        page: 1, records: 2, total: 1});

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

  it("takes the band of the toolbar, leaving the Columns and Maximize buttons out of it", () => {
    mount([button("ButDel", {selection: true})], {search: "CrtTxt"});

    const toolbar = element.find(hook("grid-toolbar"));
    expect(toolbar.find(hook("grid-selection-bar")).length).toBe(1);
    expect(bar().find(`${hook("grid-search")}, ${hook("grid-columns-button")}, ${hook("grid-maximize-button")}`).length).toBe(0);
    expect(toolbar.find(`${hook("grid-columns-button")}, ${hook("grid-maximize-button")}`).length).toBe(2);
  });

  it("covers the row of the column headers of a grid without toolbar, leaving the select-all checkbox out of it", () => {
    const component = mount([button("ButDel", {selection: true})], {showToolbar: false});
    select(component, [1]);

    expect(element.find(hook("grid-toolbar")).length).toBe(0);
    expect(bar().length).toBe(1);
    expect(bar().attr("data-count")).toBe("1");
    expect(selectAll().length).toBe(1);
    expect(bar().find(hook("grid-header-checkbox")).length).toBe(0);
  });

  it("is hidden while no row is selected", () => {
    mount([button("ButDel", {selection: true})]);

    expect(bar().length).toBe(1);
    expect(bar().attr("data-count")).toBe("0");
    expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("");
  });

  it("shows the number of selected rows with the singular and plural texts", () => {
    const component = mount([button("ButDel", {selection: true})]);

    select(component, [2]);
    expect(bar().attr("data-count")).toBe("1");
    expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("SCREEN_TEXT_GRID_SELECTED_ROW(1)");

    select(component, [1, 3]);
    expect(bar().attr("data-count")).toBe("2");
    expect(bar().find(hook("grid-selection-count")).text().trim()).toBe("SCREEN_TEXT_GRID_SELECTED_ROWS(2)");
  });

  it("is a named region whose count is a polite live region, with a decorative check icon", () => {
    mount([button("ButDel", {selection: true})]);

    expect(bar().attr("role")).toBe("region");
    expect(bar().attr("aria-label")).toBe("SCREEN_TEXT_SELECTED");
    expect(bar().find(hook("grid-selection-count")).attr("aria-live")).toBe("polite");
    expect(bar().find(".grid-selection-bar-icon svg").attr("aria-hidden")).toBe("true");
    expect(bar().find(".grid-selection-bar-icon svg use").attr("href")).toContain("#circle-check");
  });

  it("shows the active quick filter and the search text after the count, read as filtered by", () => {
    const component = mount([button("ButDel", {selection: true})], {search: "CrtTxt", quickFilter: "CrtSta"},
      {search: "adm", quickFilter: "LCK"});
    select(component, [1]);

    expect(context().find(".sr-only").text().trim()).toBe("SCREEN_TEXT_FILTERED_BY");
    expect(context().find(".grid-selection-bar-context-label").text().trim()).toBe("LABEL_LOCKED");
    expect(context().find(".grid-selection-bar-context-search").text().trim()).toBe("adm");
    expect(context().find("svg").attr("aria-hidden")).toBe("true");
  });

  it("shows no filter with the all option of the quick filter and an empty search", () => {
    const component = mount([button("ButDel", {selection: true})], {search: "CrtTxt", quickFilter: "CrtSta"});
    select(component, [1]);

    expect(context().length).toBe(0);
  });

  it("clears every selected row with the clear button at its start, named clear selection (Esc)", () => {
    const component = mount([button("ButDel", {selection: true})]);
    select(component, [1, 3]);

    expect(bar().children().first().is(clearButton())).toBe(true);
    expect(clearButton().attr("aria-label")).toBe("SCREEN_TEXT_GRID_CLEAR_SELECTION (Esc)");
    expect(clearButton().attr("title")).toBe("SCREEN_TEXT_GRID_CLEAR_SELECTION (Esc)");
    expect(clearButton().find("svg use").attr("href")).toContain("#x");
    clearButton()[0].click();
    digest();

    expect(component.getSelection()).toEqual([]);
    expect(bar().attr("data-count")).toBe("0");
  });

  it("clears the selection with Esc in the grid, with one or many selected rows", () => {
    const component = mount([button("ButDel", {selection: true})]);

    select(component, [2]);
    const event = pressEscape(element.find(".grid-container")[0]);
    expect(component.getSelection()).toEqual([]);
    expect(event.defaultPrevented).toBe(true);

    select(component, [1, 2, 3]);
    pressEscape(element.find(".grid-container")[0]);
    expect(component.getSelection()).toEqual([]);
  });

  it("keeps the selection on Esc while a row is being edited", () => {
    const component = mount([button("ButDel", {selection: true})], {editable: true, multiselect: false});
    select(component, [2]);
    component.isEditing = true;

    const event = pressEscape(element.find(".grid-container")[0]);

    expect(component.model.selected).toEqual([2]);
    expect(event.defaultPrevented).toBe(false);
  });

  it("gives the focus to the search after clearing, or to the select-all checkbox when the grid has no search", () => {
    const withSearch = mount([button("ButDel", {selection: true})], {search: "CrtTxt"});
    select(withSearch, [1]);
    clearButton()[0].click();
    digest();
    expect(document.activeElement).toBe(searchInput()[0]);

    element.closest("form").remove();
    const withoutSearch = mount([button("ButDel", {selection: true})]);
    select(withoutSearch, [1]);
    pressEscape(element.find(".grid-container")[0]);
    expect(document.activeElement).toBe(selectAll()[0]);
  });

  it("leaves the focus where it is when a row is selected", () => {
    const component = mount([button("ButDel", {selection: true})], {search: "CrtTxt"});
    const focused = element.find(hook("grid-columns-button"))[0];
    focused.focus();

    select(component, [1, 2]);

    expect(document.activeElement).toBe(focused);
  });

  it("hides again when the selection is cleared, as a reload of the grid does", () => {
    const component = mount([button("ButDel", {selection: true})]);
    select(component, [1, 2]);

    component.unselectRows();
    digest();

    expect(bar().attr("data-count")).toBe("0");
  });

  it("shows the one selected row of a grid without multiselect", () => {
    const component = mount([button("ButDel", {selection: true})], {multiselect: false});

    select(component, [3]);

    expect(bar().attr("data-count")).toBe("1");
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
    expect(barButtons()).toEqual(["ButRst", "ButDel"]);
  });

  it("leaves a grid without selection buttons as it was: no bar, every button in the footer", () => {
    mount([button("ButNew"), button("ButDel")]);

    expect(bar().length).toBe(0);
    expect(footerButtons()).toEqual(["ButNew", "ButDel"]);
  });
});
