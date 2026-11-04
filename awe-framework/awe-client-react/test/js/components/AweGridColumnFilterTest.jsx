import React from 'react';
import {act, fireEvent, screen, waitFor, within} from '@testing-library/react';
import i18n from 'i18next';
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {ADD_ACTIONS_TOP} from "../../../src/redux/actions/actions";
import {renderWithProviders} from "../test-utils";
import AweGrid from "../../../src/components/AweGrid";
import {SEARCH_DELAY} from "../../../src/components/AweGridColumnFilter";
import AweTreeGrid from "../../../src/components/AweTreeGrid";

// Grid column autofilter (#912, S13-S15, S17): the funnel of the columns that declare a filter and its menu
describe('awe-react-client/test/js/components/AweGridColumnFilterTest.jsx', () => {

  const PROFILES = [{value: "ADM", label: "Administrator", count: 2}, {value: "OPE", label: "Operator", count: 5},
    {value: null, label: null, count: 1}];
  const COLUMNS = [
    {name: "Nam", label: "Name", filter: "text", sortable: true},
    {name: "ProTxt", label: "Profile", filter: "values", filterFields: "Pro", sortable: true},
    {name: "LstAcc", label: "Last access", filter: "date", sortable: true},
    {name: "Cod", label: "Code", sortable: true}
  ];

  beforeAll(() => {
    i18n.addResourceBundle('en-GB', 'translation', {
      GRID_FILTER_BUTTON: "Filter and sort {{label}}",
      GRID_FILTER_SELECT_ALL: "(Select all)",
      GRID_FILTER_BLANKS: "(Blanks)",
      GRID_FILTER_SEARCH: "Search",
      GRID_FILTER_CUSTOM: "Custom filter…",
      GRID_FILTER_OPERATOR_CONTAINS: "Contains",
      GRID_FILTER_LAST_MONTH: "Last month",
      GRID_FILTER_CONDITION_VALUE: "Value of condition {{number}}",
      BUTTON_ACCEPT: "Accept",
      BUTTON_CANCEL: "Cancel"
    }, true, true);
  });

  beforeEach(() => {
    global.fetch = jest.fn();
    window.fetch = global.fetch;
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  // The column-values action answers these values
  const answerValues = (rows) => window.fetch.mockImplementation(() => Promise.resolve({
    ok: true,
    json: () => Promise.resolve([{type: "column-values", parameters: {datalist: {rows}, more: false}}, {type: "end-load"}])
  }));

  const render = (attributes = {}, specificAttributes = {}, Component = AweGrid) => renderWithProviders(<Component id="grid"/>, {
    spyDispatch: true,
    preloadedState: {
      settings: DEFAULT_SETTINGS,
      screen: {size: {width: 100, height: 100}},
      components: {
        grid: {
          address: {component: 'grid', view: 'report'},
          model: {values: [{id: 1, Nam: "Ana", ProTxt: "Operator", LstAcc: "01/10/2026", Cod: "A"}]},
          attributes: {columnModel: COLUMNS, headerModel: [], buttonModel: [], targetAction: "UsrLst", ...attributes},
          specificAttributes: {sort: [], page: 1, first: 0, ...specificAttributes}
        }
      }
    }
  });

  // Client actions queued on top of the action stack
  const queued = (dispatchSpy) => dispatchSpy.mock.calls
    .map(([action]) => action)
    .filter(action => action.type === ADD_ACTIONS_TOP)
    .flatMap(action => action.payload);

  const header = (container, column) => container.querySelector(`th[column-id='${column}']`);
  const funnel = (name) => screen.getByRole("button", {name: `Filter and sort ${name}`});

  const openMenu = async (name) => {
    fireEvent.click(funnel(name));
    const menu = await screen.findByRole("dialog", {name: `Filter and sort ${name}`});
    await waitFor(() => expect(within(menu).getByRole("button", {name: "Accept"})).toBeEnabled());
    return menu;
  };

  it('shows the funnel only on the columns that declare a filter and keeps the native filter on the others', () => {
    const {container} = render({enableFilters: true, loadAll: true});

    expect(within(header(container, "Nam")).getByRole("button", {name: "Filter and sort Name"}))
      .toHaveAttribute("aria-haspopup", "dialog");
    expect(funnel("Name")).toHaveAttribute("aria-expanded", "false");
    expect(header(container, "Cod").querySelector("[data-testid='grid-column-filter']")).toBeNull();
    expect(header(container, "Cod").querySelector(".p-column-filter-menu-button")).not.toBeNull();
    expect(header(container, "Nam").querySelector(".p-column-filter-menu-button")).toBeNull();
  });

  it('marks the funnel of a column with a filter in effect', () => {
    render({}, {columnFilters: [{id: "ProTxt", fields: ["Pro"], type: "values", values: ["OPE"]}]});

    expect(funnel("Profile")).toHaveAttribute("data-active", "true");
    expect(funnel("Name")).toHaveAttribute("data-active", "false");
  });

  it('shows the funnel on the columns of a tree grid', () => {
    render({treegrid: true}, {}, AweTreeGrid);

    expect(funnel("Profile")).toHaveAttribute("aria-haspopup", "dialog");
  });

  it('lists the values with Select all and Blanks, leaves Select all mixed and sends only the blanks when every value is unchecked', async () => {
    answerValues(PROFILES);
    const {dispatchSpy} = render();

    const menu = await openMenu("Profile");

    const list = within(menu).getByRole("group", {name: "GRID_FILTER_VALUES"});
    expect(within(list).getAllByRole("checkbox").map(box => [box.textContent, box.getAttribute("aria-checked")])).toEqual([
      ["(Select all)", "true"], ["Administrator2", "true"], ["Operator5", "true"], ["(Blanks)1", "true"]]);
    fireEvent.click(within(list).getByRole("checkbox", {name: /Administrator/}));
    expect(within(list).getByRole("checkbox", {name: "(Select all)"})).toHaveAttribute("aria-checked", "mixed");
    fireEvent.click(within(list).getByRole("checkbox", {name: "(Select all)"}));
    expect(within(list).getAllByRole("checkbox").every(box => box.getAttribute("aria-checked") === "true")).toBe(true);

    fireEvent.click(within(list).getByRole("checkbox", {name: "(Select all)"}));
    expect(within(menu).getByRole("button", {name: "Accept"})).toBeDisabled();
    fireEvent.click(within(list).getByRole("checkbox", {name: /Blanks/}));
    fireEvent.click(within(menu).getByRole("button", {name: "Accept"}));
    expect(queued(dispatchSpy)[0].parameters.columnFilters).toEqual([
      {id: "ProTxt", fields: ["Pro"], type: "values", values: [], empty: true}]);
  });

  it('applies the checked values from the first page and reloads a load-all grid from the server', async () => {
    answerValues(PROFILES);
    const {dispatchSpy} = render({loadAll: true}, {page: 3, first: 20});
    const menu = await openMenu("Profile");

    fireEvent.click(within(menu).getByRole("checkbox", {name: /Administrator/}));
    fireEvent.click(within(menu).getByRole("button", {name: "Accept"}));

    expect(queued(dispatchSpy).map(({type, parameters}) => [type, parameters])).toEqual([
      ["change-column-filters", {columnFilters: [{id: "ProTxt", fields: ["Pro"], type: "values", values: ["OPE"], empty: true}],
        labels: {ProTxt: {OPE: "Operator"}}}],
      ["filter", undefined]
    ]);
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it('asks the values again with the search after a short pause', async () => {
    answerValues(PROFILES);
    render();
    const menu = await openMenu("Profile");
    jest.useFakeTimers();

    fireEvent.change(within(menu).getByRole("searchbox", {name: "Search"}), {target: {value: "adm"}});
    act(() => {
      jest.advanceTimersByTime(SEARCH_DELAY - 1);
    });
    expect(window.fetch).toHaveBeenCalledTimes(1);
    await act(async () => {
      jest.advanceTimersByTime(1);
    });

    expect(window.fetch).toHaveBeenCalledTimes(2);
    expect(JSON.parse(window.fetch.mock.calls[1][1].body).columnValues).toEqual(
      {id: "ProTxt", fields: ["Pro"], type: "values", search: "adm"});
  });

  it('closes with Escape without changes and gives the focus back to the funnel', async () => {
    answerValues(PROFILES);
    const {dispatchSpy} = render();
    const menu = await openMenu("Profile");

    fireEvent.click(within(menu).getByRole("checkbox", {name: /Administrator/}));
    fireEvent.keyDown(within(menu).getByRole("searchbox"), {key: "Escape"});

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(funnel("Profile")).toHaveFocus();
    expect(funnel("Profile")).toHaveAttribute("aria-expanded", "false");
    expect(queued(dispatchSpy)).toEqual([]);
  });

  it('moves through the values with the arrows and checks them with Space', async () => {
    answerValues(PROFILES);
    render();
    const menu = await openMenu("Profile");
    const list = within(menu).getByRole("group", {name: "GRID_FILTER_VALUES"});

    fireEvent.keyDown(within(list).getAllByRole("checkbox")[0], {key: "ArrowDown"});
    const administrator = within(list).getByRole("checkbox", {name: /Administrator/});
    await waitFor(() => expect(administrator).toHaveFocus());
    fireEvent.keyDown(administrator, {key: " "});

    expect(administrator).toHaveAttribute("aria-checked", "false");
  });

  it('sorts the column from the menu as its header does', async () => {
    answerValues(PROFILES);
    const {dispatchSpy} = render();
    const menu = await openMenu("Profile");

    fireEvent.click(within(menu).getByRole("button", {name: "GRID_FILTER_SORT_DESC_TEXT"}));

    expect(queued(dispatchSpy).map(({type, parameters}) => [type, parameters])).toEqual([
      ["change-sort", {sort: [{id: "ProTxt", order: -1, direction: "desc"}]}],
      ["filter", undefined]
    ]);
  });

  it('clears the filter of the column', async () => {
    answerValues(PROFILES);
    const other = {id: "Nam", type: "text", conditions: [{operator: "eq", value: "Ana"}]};
    const {dispatchSpy} = render({}, {columnFilters: [{id: "ProTxt", type: "values", values: ["OPE"]}, other]});
    const menu = await openMenu("Profile");

    fireEvent.click(within(menu).getByRole("button", {name: "GRID_FILTER_CLEAR"}));

    expect(queued(dispatchSpy)[0].parameters.columnFilters).toEqual([other]);
  });

  it('applies a custom text condition chosen in the text filters', async () => {
    answerValues([{value: "Ana", label: "Ana", count: 1}]);
    const {dispatchSpy} = render();
    const menu = await openMenu("Name");

    fireEvent.click(within(menu).getByRole("button", {name: "GRID_FILTER_TEXT_FILTERS"}));
    fireEvent.click(within(menu).getByRole("menuitem", {name: "Contains…"}));
    fireEvent.change(within(menu).getByRole("textbox", {name: "Value of condition 1"}), {target: {value: "an"}});
    fireEvent.click(within(menu).getByRole("button", {name: "Accept"}));

    expect(queued(dispatchSpy)[0].parameters.columnFilters).toEqual([
      {id: "Nam", fields: ["Nam"], type: "text", conditions: [{operator: "contains", value: "an"}]}]);
  });

  it('shows the dates as a tree of years, months and days', async () => {
    answerValues([{value: "01/10/2026", count: 2}, {value: "15/10/2026", count: 1}, {value: "03/11/2026", count: 4}]);
    render();
    const menu = await openMenu("Last access");
    const tree = within(menu).getByRole("tree");

    // A single year starts open
    const year = within(tree).getByRole("treeitem", {name: /2026/});
    expect(year).toHaveAttribute("aria-expanded", "true");
    const months = within(tree).getAllByRole("treeitem").filter(item => item.getAttribute("aria-level") === "2");
    expect(months).toHaveLength(2);
    fireEvent.click(months[0]);

    expect(months[0]).toHaveAttribute("aria-checked", "false");
    expect(year).toHaveAttribute("aria-checked", "mixed");
    act(() => {
      fireEvent.keyDown(months[0], {key: "ArrowRight"});
    });
    expect(within(tree).getAllByRole("treeitem").filter(item => item.getAttribute("aria-level") === "3")
      .map(item => [item.textContent, item.getAttribute("aria-checked")])).toEqual([["012", "false"], ["151", "false"]]);
  });
});
