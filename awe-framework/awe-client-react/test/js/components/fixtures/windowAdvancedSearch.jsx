// Shared fixtures of the advanced search of a window (#790) and its saved searches (#790, #915): a window with an
// essential criterion (User) and an advanced block with two advanced ones (Theme and Blocked), the store they live in,
// the helpers that find their parts and the fake server of the saved searches
import {DEFAULT_SETTINGS} from "../../../../src/redux/actions/settings";
import React from "react";
import {act, fireEvent} from "@testing-library/react";
import {renderWithProviders} from "../../test-utils";
import AweWindow from "../../../../src/components/AweWindow";
import {updateModel} from "../../../../src/redux/actions/components";
import ViewRegistry from "../../../../src/redux/registry/ViewRegistry";

export const VIEW = "report";
export const THEMES = [{value: "1", label: "Light"}, {value: "2", label: "Dark"}];
export const select = (selected) => THEMES.map(option => ({...option, selected: option.value === selected}));

export const criterion = (id, component, label, values, defaults, source = ["SearchWindow", "SearchBody", id]) => ({
  address: {component: id, view: VIEW},
  model: {values},
  storedModel: {values},
  defaultModel: {values: defaults},
  attributes: {id, component, label, visible: true},
  validationRules: {},
  specificAttributes: {},
  context: {view: VIEW, source},
  actions: [],
  dependencies: []
});

export const advancedSource = (id) => ["SearchWindow", "SearchBody", "AdvCrt", id];

// User (essential) is in use, Theme (advanced) differs from its default Light and Blocked (advanced) is checked
export const preloadedState = () => ({
  settings: DEFAULT_SETTINGS,
  components: {
    CrtUsr: criterion("CrtUsr", "text", "User", [{value: "ana", label: "ana", selected: true}], []),
    CrtThm: criterion("CrtThm", "select", "Theme", select("2"), select("1"), advancedSource("CrtThm")),
    CrtBlk: criterion("CrtBlk", "checkbox", "Blocked", [{value: 1, selected: true}], [{value: 1, selected: false}],
      advancedSource("CrtBlk"))
  }
});

// As the server sends it: the essential criteria, then the advanced block with the advanced ones, in the body tag
export const advancedBlock = {elementType: "Advanced", id: "AdvCrt", elementList: [
  {elementType: "Criteria", component: "select", id: "CrtThm"},
  {elementType: "Criteria", component: "checkbox", id: "CrtBlk"}
]};
export const elementList = [
  {elementType: "Tag", type: "div", id: "SearchBody", style: "panel-body", elementList: [
    {elementType: "Criteria", component: "text", id: "CrtUsr"},
    advancedBlock
  ]}
];

export const renderWindow = (props = {}, options = {}) => renderWithProviders(
  <AweWindow id="SearchWindow" label="Search" advancedMode="inline" advancedCriteria={["CrtThm", "CrtBlk"]}
             searchCriteria={["CrtUsr", "CrtThm", "CrtBlk"]} elementList={elementList} {...props}/>,
  {preloadedState: preloadedState(), ...options});

export const byTestId = (id, container = document) => container.querySelector(`[data-testid='${id}']`);
export const allByTestId = (id, container = document) => [...container.querySelectorAll(`[data-testid='${id}']`)];
export const field = (id) => document.querySelector(`[criterion-id='${id}']`);
export const toggle = () => byTestId("window-advanced-toggle");
export const badge = () => byTestId("window-advanced-count");
export const chips = () => allByTestId("window-filter-chip");
export const chip = (text) => chips().find(element => element.textContent.includes(text));
export const block = () => document.querySelector(".window-advanced");
export const row = () => document.querySelector(".window-advanced-row");
export const region = () => document.querySelector(".window-advanced-region");
export const selected = (store, id) => store.getState().components[id].model.values
  .filter(value => value.selected).map(value => value.value);
export const update = (store, id, values) => act(() => {
  store.dispatch(updateModel({component: id, view: VIEW}, {values}));
});
export const submits = (dispatchSpy) => dispatchSpy.mock.calls.map(([action]) => action)
  .filter(action => action?.type === "ADD_ACTIONS_TOP")
  .flatMap(action => action.payload)
  .filter(action => action.type === "submit");

// Saved searches of the window: the own search Dark themes and Blocked users, published for the ADM profile
export const SEARCHES = [
  {id: 1, name: "Dark themes", own: 1, profile: "", values: JSON.stringify({
    CrtUsr: [{value: "bob", label: "Bob"}], CrtThm: [{value: "2", label: "Dark"}], CrtBlk: []})},
  {id: 2, name: "Blocked users", own: 0, profile: "ADM", values: JSON.stringify({CrtBlk: [{value: 1}]})}
];
export const PROFILES = [{value: "ADM", label: "Administrator"}, {value: "OPE", label: "Operator"}];
export const fill = (rows) => [{type: "fill", parameters: {datalist: {rows, total: 1, page: 1, records: rows.length}}}];
export const answer = (body) => Promise.resolve({ok: true, status: 200, json: () => Promise.resolve(body),
  text: () => Promise.resolve(JSON.stringify(body))});
// Parameters of every call to a target of the server
export const calls = (target) => global.fetch.mock.calls.filter(([url]) => url.endsWith(`/${target}`))
  .map(([, options]) => JSON.parse(options.body));
// Actions queued to be launched (a thunk dispatches them inside the store, so a dispatch spy does not see them)
export const queued = (store, type) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async.flat()]
  .filter(action => action?.type === type);
export const flush = async () => {
  for (let index = 0; index < 6; index++) {
    await act(async () => {
      await Promise.resolve();
    });
  }
};

/**
 * Fake server of the saved searches: it lists the searches and the profiles of the returned object (a test changes
 * them), answers the targets of its answers, and fails the targets it lists in fail
 * @return {{searches: object[], profiles: object[], answers: object, fail: string[]}} Server
 */
export const installSavedSearchServer = () => {
  const server = {searches: SEARCHES, profiles: [], answers: {}, fail: []};
  global.fetch = jest.fn((url) => {
    const target = url.split("/").pop();
    if (server.fail.includes(target)) {
      return Promise.reject(new Error("down"));
    }
    if (target === "getWindowSavedSearches") {
      return answer(fill(server.searches));
    }
    if (target === "getSavedSearchProfiles") {
      return answer(fill(server.profiles));
    }
    return answer(server.answers[target] ?? []);
  });
  ViewRegistry.setView(VIEW, {name: "UsrLst", loading: false});
  return server;
};

export const savedState = (attributes) => {
  const state = preloadedState();
  state.components.SearchWindow = {address: {component: "SearchWindow", view: VIEW}, attributes,
    validationRules: {}, specificAttributes: {}};
  return state;
};
export const renderSaved = (props = {}, attributes = {savedSearches: true}) =>
  renderWindow(props, {preloadedState: savedState(attributes)});
export const button = () => byTestId("window-saved-searches");
export const menu = () => byTestId("window-saved-searches-menu");
export const items = () => allByTestId("window-saved-search-item");
export const item = (id) => items().find(element => element.getAttribute("data-saved-search-id") === String(id));
export const menuItems = () => [...menu().querySelectorAll("[role='menuitem']")];
export const openMenu = async () => {
  fireEvent.click(button());
  await flush();
};
export const saveDialog = () => byTestId("window-saved-search-dialog");
export const manageDialog = () => byTestId("window-saved-search-manage-dialog");
export const nameInput = () => byTestId("window-saved-search-name");
export const openSaveDialog = async () => {
  await openMenu();
  fireEvent.click(byTestId("window-saved-search-save"));
  await flush();
};
export const openManageDialog = async () => {
  await openMenu();
  fireEvent.click(byTestId("window-saved-search-manage"));
  await flush();
};
export const rows = () => allByTestId("window-saved-search-row");
export const manageRow = (id) => rows().find(element => element.getAttribute("data-saved-search-id") === String(id));
export const labels = (store, id) => store.getState().components[id].model.values
  .filter(value => value.selected).map(value => value.label);
