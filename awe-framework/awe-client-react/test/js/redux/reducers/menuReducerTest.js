import {menu} from "../../../../src/redux/reducers/menuReducer";
import {
  clearMenu,
  selectOption,
  updateAllOptions,
  updateBreadcrumbs,
  updateMenu,
  updateOption,
  updateOptions,
  updateStatus
} from "../../../../src/redux/actions/menu";

describe("awe-react-client/test/js/redux/reducers/menuReducerTest.js", () => {
  const tree = [
    {name: "home", visible: true, restricted: false, options: [{name: "child", visible: true, restricted: true}]},
    {name: "hidden", visible: false}
  ];

  it("starts empty with a desktop status", () => {
    expect(menu(undefined, {})).toEqual({
      options: [],
      breadcrumbs: {},
      status: {minimized: false, animating: false, resolution: "desktop"}
    });
  });

  it("loads the menu marking which options are allowed, at every level", () => {
    const state = menu(undefined, updateMenu(tree));
    expect(state.selected).toEqual({});
    expect(state.options).toEqual([
      {name: "home", visible: true, restricted: false, allowed: true, options: [
        {name: "child", visible: true, restricted: true, allowed: false, options: []}
      ]},
      {name: "hidden", visible: false, allowed: false, options: []}
    ]);
  });

  it("loads an empty menu when there is no data", () => {
    expect(menu(undefined, updateMenu(undefined)).options).toEqual([]);
  });

  it("updates one option by name, at any depth", () => {
    const state = menu({options: tree}, updateOption("child", {visible: false}));
    expect(state.options[0].options[0].visible).toBe(false);
    expect(state.options[0].visible).toBe(true);
  });

  it("updates several options by name", () => {
    const state = menu({options: tree}, updateOptions(["home", "hidden"], {restricted: true}));
    expect(state.options.map(option => option.restricted)).toEqual([true, true]);
    expect(state.options[0].options[0].restricted).toBe(true);
  });

  it("updates every option", () => {
    const state = menu({options: tree}, updateAllOptions({visible: true}));
    expect(state.options[1].visible).toBe(true);
    expect(state.options[0].options[0].visible).toBe(true);
  });

  it("merges the status", () => {
    const state = menu(undefined, updateStatus({minimized: true}));
    expect(state.status).toEqual({minimized: true, animating: false, resolution: "desktop"});
  });

  it("selects an option", () => {
    expect(menu(undefined, selectOption({name: "home"})).selected).toEqual({name: "home"});
  });

  it("clears the options and the selection", () => {
    expect(menu({options: tree, selected: {name: "home"}, breadcrumbs: {}, status: {}}, clearMenu()))
      .toEqual({options: [], selected: {}, breadcrumbs: {}, status: {}});
  });

  it("stores a copy of the breadcrumb items", () => {
    const items = [{name: "home"}];
    const state = menu(undefined, updateBreadcrumbs("home", items));
    expect(state.breadcrumbs).toEqual({items, option: "home"});
    expect(state.breadcrumbs.items).not.toBe(items);
  });

  it("returns the same state for unrelated actions", () => {
    const state = {options: []};
    expect(menu(state, {type: "OTHER"})).toBe(state);
  });
});
