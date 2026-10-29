import React from "react";
import { act, fireEvent } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGridQuickFilter from "../../../src/criteria/AweGridQuickFilter";
import { fillAction, filterAction } from "../../../src/redux/thunks/form";
import { getFormValues } from "../../../src/redux/selectors/form";

const hook = (id) => `[data-testid="${id}"]`;

const OPTIONS = [
  { value: "", label: "All", count: 128 },
  { value: "LCK", label: "Locked", count: 3, tone: "danger" },
  { value: "NO2FA", label: "Without 2FA", count: 41 },
  { value: "INA", label: "Inactive", count: 12 }
];

function state(selected = "", values = OPTIONS, attributes = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "users", option: "users" } },
    components: {
      GrdUsr: {
        address: { component: "GrdUsr", view: "report" },
        model: { values: [], records: 0 },
        attributes: { id: "GrdUsr", component: "grid", serverAction: "data", targetAction: "UsrLst", quickFilter: "CrtUsrSta",
          columnModel: [], headerModel: [], buttonModel: [], visible: true },
        specificAttributes: { sort: [] },
        context: { view: "report", source: [] }
      },
      CrtUsrSta: {
        address: { component: "CrtUsrSta", view: "report" },
        model: { values: values.map(option => ({ ...option, selected: selected !== null && option.value === selected })) },
        attributes: { id: "CrtUsrSta", component: "quick-filter", serverAction: "data", targetAction: "UsrStaCnt", visible: true,
          ...attributes },
        specificAttributes: {},
        validationRules: {},
        context: { view: "report", source: [] }
      }
    }
  };
}

const render = (...args) => renderWithProviders(<AweGridQuickFilter id="CrtUsrSta" grid="GrdUsr" />, { preloadedState: state(...args) });
const options = () => Array.from(document.querySelectorAll(hook("grid-quick-filter-option")));
const radios = () => Array.from(document.querySelectorAll(`${hook("grid-quick-filter")} input[type=radio]`));
const checked = () => radios().map(radio => radio.checked);
const queued = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async];

describe("awe-react-client/test/js/criteria/AweGridQuickFilterTest.jsx", () => {
  beforeEach(() => {
    global.fetch = jest.fn(() => new Promise(() => undefined));
  });

  it("renders a radio group with one segment per option, its label and its count", () => {
    render();

    const group = document.querySelector(hook("grid-quick-filter"));
    expect(group.getAttribute("role")).toBe("radiogroup");
    expect(group.getAttribute("aria-label")).toBe("SCREEN_TEXT_FILTERS");
    expect(group.getAttribute("data-testid-owner")).toBe("GrdUsr");
    expect(options().map(option => option.textContent.replace(/\s+/g, " ").trim()))
      .toEqual(["All 128", "Locked 3", "Without 2FA 41", "Inactive 12"]);
    expect(options()[1].querySelector(".grid-quick-filter-count").getAttribute("data-tone")).toBe("danger");
    expect(options()[0].querySelector(".grid-quick-filter-count").hasAttribute("data-tone")).toBe(false);
    expect(options().map(option => option.getAttribute("option-id"))).toEqual(["", "LCK", "NO2FA", "INA"]);
    expect(new Set(radios().map(radio => radio.name)).size).toBe(1);
  });

  it("shows the chosen option, and the first one when none is chosen", () => {
    const { unmount } = render("LCK");
    expect(checked()).toEqual([false, true, false, false]);
    unmount();

    render(null);
    expect(checked()).toEqual([true, false, false, false]);
  });

  it("hides the count of an option without it", () => {
    render("", [{ value: "", label: "All" }, { value: "LCK", label: "Locked", count: 0 }]);

    expect(options()[0].querySelector(".grid-quick-filter-count")).toBeNull();
    expect(options()[1].querySelector(".grid-quick-filter-count").textContent).toBe("0");
  });

  it("sends its value as a criterion and filters the grid when another option is chosen", () => {
    const { store } = render();

    fireEvent.click(radios()[1]);

    expect(checked()).toEqual([false, true, false, false]);
    expect(getFormValues(store.getState()).CrtUsrSta).toBe("LCK");
    const filters = queued(store).filter(action => action.type === "filter");
    expect(filters.map(action => action.address)).toEqual([{ view: "report", component: "GrdUsr" }]);
  });

  it("does not filter the grid again when the chosen option is clicked", () => {
    const { store } = render("LCK");

    fireEvent.click(radios()[1]);

    expect(queued(store).filter(action => action.type === "filter")).toEqual([]);
  });

  it("moves the choice with the arrow keys and filters the grid", () => {
    const { store } = render();

    radios()[0].focus();
    fireEvent.keyDown(radios()[0], { key: "ArrowRight" });
    expect(checked()).toEqual([false, true, false, false]);
    expect(document.activeElement).toBe(radios()[1]);

    fireEvent.keyDown(radios()[1], { key: "ArrowLeft" });
    fireEvent.keyDown(radios()[0], { key: "ArrowUp" });
    expect(checked()).toEqual([false, false, false, true]);
    expect(getFormValues(store.getState()).CrtUsrSta).toBe("INA");
    expect(queued(store).filter(action => action.type === "filter").length).toBe(3);
  });

  it("reloads its options, silently, when its grid is filtered", () => {
    const { store } = render();

    act(() => {
      store.dispatch(filterAction({ type: "filter", address: { view: "report", component: "GrdUsr" } }));
    });

    const servers = queued(store).filter(action => action.type === "server");
    expect(servers.map(action => action.parameters.component)).toEqual(expect.arrayContaining(["GrdUsr", "CrtUsrSta"]));
    const reload = servers.find(action => action.parameters.component === "CrtUsrSta");
    expect(reload.silent).toBe(true);
    expect(reload.async).toBe(true);
    expect(reload.parameters.targetAction).toBe("UsrStaCnt");
    expect(reload.parameters.serverAction).toBe("data");
  });

  it("shows the new options and counts when they reload, keeping the chosen one", () => {
    const { store } = render("LCK");

    act(() => {
      store.dispatch(fillAction({
        type: "fill",
        address: { view: "report", component: "CrtUsrSta" },
        parameters: { datalist: { rows: [{ value: "", label: "All", count: 127 }, { value: "LCK", label: "Locked", count: 2, tone: "danger" }] } }
      }));
    });

    expect(options().map(option => option.textContent.replace(/\s+/g, " ").trim())).toEqual(["All 127", "Locked 2"]);
    expect(checked()).toEqual([false, true]);
  });

  it("names the group with its label when it has one", () => {
    render("", OPTIONS, { label: "SCREEN_TEXT_USER" });

    expect(document.querySelector(hook("grid-quick-filter")).getAttribute("aria-label")).toBe("SCREEN_TEXT_USER");
  });
});
