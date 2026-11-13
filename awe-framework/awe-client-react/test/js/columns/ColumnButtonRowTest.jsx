import {act} from "@testing-library/react";
import {Columns} from "../../../src/utilities/structure";
import {renderWithProviders} from "../test-utils";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import {ADD_ACTIONS_TOP} from "../../../src/redux/actions/actions";
import {configureStore} from "@reduxjs/toolkit";
import createRootReducer from "../../../src/redux/reducers";

// The button of a grid row (#793) selects its row (only it) before its actions, and an icon-only cell button has the
// title of its column as tooltip and accessible name
describe('awe-react-client/test/js/columns/ColumnButtonRowTest.jsx', () => {
  const actions = [{type: "dialog", target: "PnlUsr"}];

  const state = (selected) => ({
    settings: DEFAULT_SETTINGS,
    actions: {running: false},
    components: {
      GrdUsr: {
        address: {view: "v", component: "GrdUsr"},
        attributes: {multiselect: true, columnModel: []},
        model: {values: [1, 2, 3].map(id => ({id, selected: selected.includes(id)})), selected}
      }
    }
  });

  const render = (selected, data = {}, column = {}) => renderWithProviders(Columns({
    component: "button",
    name: "EdtBtn",
    icon: "lucide:pencil",
    title: "Edit",
    style: "btn-ghost no-label",
    actions,
    address: {view: "v", component: "GrdUsr", row: 2, column: "EdtBtn"},
    ...column
  }, data, {}, false), {preloadedState: state(selected), spyDispatch: true});

  const selectedRows = store => store.getState().components.GrdUsr.model.values.filter(row => row.selected).map(row => row.id);

  it('leaves only its row selected and then adds its actions', () => {
    // A store whose dispatch records the selected rows when the actions of the button are added
    let selectedWhenActionsAdded = null;
    const store = configureStore({reducer: createRootReducer({}), preloadedState: state([1, 3]),
      middleware: getDefaultMiddleware => getDefaultMiddleware({serializableCheck: false})});
    const dispatch = store.dispatch;
    store.dispatch = action => {
      if (action && action.type === ADD_ACTIONS_TOP) {
        selectedWhenActionsAdded = selectedRows(store);
      }
      return dispatch(action);
    };
    const {container} = renderWithProviders(Columns({
      component: "button", name: "EdtBtn", icon: "lucide:pencil", title: "Edit", actions,
      address: {view: "v", component: "GrdUsr", row: 2, column: "EdtBtn"}
    }, {}, {}, false), {store});

    act(() => container.querySelector("button").click());

    expect(selectedRows(store)).toEqual([2]);
    expect(selectedWhenActionsAdded).toEqual([2]);
  });

  it('selects its row when no row is selected', () => {
    const {container, store} = render([]);

    act(() => container.querySelector("button").click());

    expect(selectedRows(store)).toEqual([2]);
  });

  it('leaves the selection as it is when its row is already the only selected one', () => {
    const {container, store, dispatchSpy} = render([2]);

    act(() => container.querySelector("button").click());

    expect(selectedRows(store)).toEqual([2]);
    const selectEvents = dispatchSpy.mock.calls.map(([action]) => action)
      .filter(action => action && action.type === "UPDATE_MODEL" && action.data && "selected" in action.data);
    expect(selectEvents).toEqual([]);
  });

  it('takes the title of the column as tooltip and accessible name of a cell without label', () => {
    const {container} = render([]);
    const button = container.querySelector("button");

    expect(button.classList.contains("p-button-icon-only")).toBe(true);
    expect(button.textContent.trim()).toBe("");
    expect(button.getAttribute("title")).toBe("Edit");
    expect(button.getAttribute("aria-label")).toBe("Edit");
    expect(button.className).toContain("p-button-text");
  });

  it('names a cell button with a label by its label', () => {
    const {container} = render([], {value: "open", label: "Open", title: "Open the user"});
    const button = container.querySelector("button");

    expect(button.querySelector(".p-button-label").textContent).toBe("Open");
    expect(button.getAttribute("title")).toBe("Open the user");
    expect(button.getAttribute("aria-label")).toBe("Open");
  });
});
