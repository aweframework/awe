import React from "react";
import { act, cleanup } from "@testing-library/react";
import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { ACCEPT_ACTION } from "../../../src/redux/actions/actions";
import { showContextMenuAction } from "../../../src/redux/thunks/contextMenu";
import { isComponentAction } from "../../../src/redux/thunks/targets";
import { renderWithProviders } from "../test-utils";
import AweButton from "../../../src/components/AweButton";
import AweGrid from "../../../src/components/AweGrid";
import useComponentService from "../../../src/services/ComponentService";

const view = "report";
const hook = (testId) => `[data-testid='${testId}']`;
const rect = { left: 200, right: 260, top: 100, bottom: 130, width: 60, height: 30 };

const optionState = {
  OptAdd: { address: { component: "OptAdd", view }, model: { values: [] }, attributes: { label: "Add", visible: true } }
};

const hiddenOption = () => ({
  address: { component: "OptAdd", view }, model: { values: [] }, attributes: { label: "Add", visible: false }
});

const buttonState = (contextMenu = [{ id: "OptAdd" }]) => ({
  settings: DEFAULT_SETTINGS,
  actions: { running: false },
  screen: { size: { width: 1000, height: 1000 } },
  components: {
    ...optionState,
    but: {
      address: { component: "but", view }, model: { values: [] }, elementList: [], actions: [],
      attributes: { label: "Open", visible: true, contextMenu }
    }
  }
});

const gridState = (contextMenu = [{ id: "OptAdd" }]) => ({
  settings: DEFAULT_SETTINGS,
  actions: { running: false },
  screen: { size: { width: 1000, height: 1000 }, breadcrumbs: [], report: { name: "opcion", option: "opcion" } },
  components: {
    ...optionState,
    grid: {
      address: { component: "grid", view },
      model: { values: [{ id: 1, test: "one" }, { id: 2, test: "two" }], selected: [] },
      attributes: {
        loadAll: true, max: 30, buttonModel: [], headerModel: [], contextMenu,
        columnModel: [
          { name: "id", label: "id", hidden: true, identifier: true },
          { name: "test", label: "test", hidden: false }
        ]
      },
      specificAttributes: { sort: [] }
    }
  }
});

function ServiceHarness({ onActions }) {
  onActions(useComponentService().getActions());
  return null;
}

describe("awe-react-client/test/js/components/ShowContextMenuActionTest.jsx", () => {
  let rectSpy;
  beforeEach(() => {
    rectSpy = jest.spyOn(Element.prototype, "getBoundingClientRect").mockReturnValue(rect);
  });
  afterEach(() => {
    cleanup();
    // A menu that was open stays in the body while PrimeReact finishes its animation
    document.querySelectorAll(".p-contextmenu").forEach((node) => node.remove());
    rectSpy.mockRestore();
  });

  // Runs the thunk with a dispatch that records what it dispatches, to tell that the action finished
  const run = async (store, address) => {
    const dispatched = [];
    const dispatch = (item) => {
      dispatched.push(item);
      return store.dispatch(item);
    };
    await act(async () => {
      showContextMenuAction({ type: "show-context-menu", address })(dispatch, store.getState);
    });
    return dispatched;
  };
  const accepted = (dispatched) => dispatched.filter((a) => a?.type === ACCEPT_ACTION).length;

  it("is a component action registered by the component service, guarded by its target", () => {
    let actions;
    renderWithProviders(<ServiceHarness onActions={(a) => { actions = a; }} />, { preloadedState: { settings: DEFAULT_SETTINGS } });

    expect(typeof actions["show-context-menu"]).toBe("function");
    expect(isComponentAction("show-context-menu")).toBe(true);
  });

  describe("on a button outside a grid", () => {
    it("opens its own menu with its top left corner at the bottom left corner of the button", async () => {
      const { store } = renderWithProviders(<AweButton id="but" />, {
        preloadedState: buttonState()
      });

      const dispatched = await run(store, { component: "but", view });

      const menu = document.querySelector(".p-contextmenu");
      expect(menu).not.toBeNull();
      expect(menu.style.left).toBe("200px");
      expect(menu.style.top).toBe("130px");
      expect(document.querySelector(hook("context-menu"))).not.toBeNull();
      expect(accepted(dispatched)).toBe(1);
    });

    it("opens nothing when none of its options is visible, and still finishes", async () => {
      const state = buttonState();
      state.components.OptAdd = hiddenOption();
      const { store } = renderWithProviders(<AweButton id="but" />, { preloadedState: state });

      const dispatched = await run(store, { component: "but", view });

      expect(document.querySelector(".p-contextmenu")).toBeNull();
      expect(accepted(dispatched)).toBe(1);
    });

    it("finishes without opening anything when the button declares no menu", async () => {
      const { store } = renderWithProviders(<AweButton id="but" />, {
        preloadedState: buttonState([])
      });

      const dispatched = await run(store, { component: "but", view });

      expect(document.querySelector(".p-contextmenu")).toBeNull();
      expect(accepted(dispatched)).toBe(1);
    });
  });

  describe("on a button of a grid row", () => {
    it("selects the row of the button and opens the menu of the grid under the button", async () => {
      const { store } = renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState()
      });
      // The button of the row registers its anchor like the column button does
      const button = document.createElement("button");
      document.body.appendChild(button);
      const { registerContextMenuAnchor } = require("../../../src/utilities/contextMenuRegistry");
      const off = registerContextMenuAnchor({ component: "grid", view, row: 2, column: "btn" }, () => button);

      const dispatched = await run(store, { component: "grid", view, row: 2, column: "btn" });
      off();
      button.remove();

      expect(store.getState().components.grid.model.selected).toEqual([2]);
      const menu = document.querySelector(".p-contextmenu");
      expect(menu).not.toBeNull();
      expect(menu.style.left).toBe("200px");
      expect(menu.style.top).toBe("130px");
      expect(accepted(dispatched)).toBe(1);
    });

    it("finds the real button of the row in the grid through the address its click carries", async () => {
      const state = gridState();
      state.components.grid.attributes.columnModel.push({
        name: "btn", label: "btn", component: "button", hidden: false, actions: [{ type: "show-context-menu" }]
      });
      state.components.grid.model.values = [{ id: 1, test: "one", btn: "x" }, { id: 2, test: "two", btn: "y" }];
      const { store } = renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });
      const buttons = document.querySelectorAll("tbody button");
      expect(buttons.length).toBe(2);

      const dispatched = await run(store, { component: "grid", view, row: 2, column: "btn" });

      expect(store.getState().components.grid.model.selected).toEqual([2]);
      expect(document.querySelector(".p-contextmenu")).not.toBeNull();
      expect(accepted(dispatched)).toBe(1);
    });

    it("opens the menu even when no option is visible yet: the options depend on the selected row", async () => {
      // Dependencies of the options resolve after the selection (#801 t042), so the grid cannot tell hidden options
      // from options still to be resolved
      const state = gridState();
      state.components.OptAdd = hiddenOption();
      const { store } = renderWithProviders(<AweGrid id="grid" />, { preloadedState: state });
      const button = document.createElement("button");
      document.body.appendChild(button);
      const { registerContextMenuAnchor } = require("../../../src/utilities/contextMenuRegistry");
      const off = registerContextMenuAnchor({ component: "grid", view, row: 2, column: "btn" }, () => button);

      const dispatched = await run(store, { component: "grid", view, row: 2, column: "btn" });
      off();
      button.remove();

      expect(store.getState().components.grid.model.selected).toEqual([2]);
      expect(document.querySelector(".p-contextmenu")).not.toBeNull();
      expect(accepted(dispatched)).toBe(1);
    });

    it("neither selects nor opens anything when the grid declares no menu, and still finishes", async () => {
      const { store } = renderWithProviders(<AweGrid id="grid" />, {
        preloadedState: gridState([])
      });

      const dispatched = await run(store, { component: "grid", view, row: 2, column: "btn" });

      expect(store.getState().components.grid.model.selected).toEqual([]);
      expect(document.querySelector(".p-contextmenu")).toBeNull();
      expect(accepted(dispatched)).toBe(1);
    });
  });
});
