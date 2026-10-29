import React from "react";
import { act, fireEvent } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGridSearch from "../../../src/criteria/AweGridSearch";
import { filterAction } from "../../../src/redux/thunks/form";
import { getFormValues } from "../../../src/redux/selectors/form";

const hook = (id) => `[data-testid="${id}"]`;

function state(value = "", attributes = {}) {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "users", option: "users" } },
    components: {
      GrdUsr: {
        address: { component: "GrdUsr", view: "report" },
        model: { values: [], records: 0 },
        attributes: { id: "GrdUsr", component: "grid", serverAction: "data", targetAction: "UsrLst", search: "CrtUsrTxt",
          columnModel: [], headerModel: [], buttonModel: [], visible: true },
        specificAttributes: { sort: [] },
        context: { view: "report", source: [] }
      },
      CrtUsrTxt: {
        address: { component: "CrtUsrTxt", view: "report" },
        model: { values: value ? [{ value, selected: true }] : [] },
        attributes: { id: "CrtUsrTxt", component: "grid-search", placeholder: "SCREEN_TEXT_USER_SEARCH", visible: true,
          ...attributes },
        specificAttributes: {},
        validationRules: {},
        context: { view: "report", source: [] }
      }
    }
  };
}

const render = (...args) => renderWithProviders(<AweGridSearch id="CrtUsrTxt" grid="GrdUsr" />, { preloadedState: state(...args) });
const input = () => document.querySelector(`${hook("grid-search")} input`);
const clearButton = () => document.querySelector(hook("grid-search-clear"));
const queued = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async];
const filters = (store) => queued(store).filter(action => action.type === "filter").map(action => action.address);
const type = (text) => {
  input().focus();
  fireEvent.change(input(), { target: { value: text } });
};

describe("awe-react-client/test/js/criteria/AweGridSearchTest.jsx", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    global.fetch = jest.fn(() => new Promise(() => undefined));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("renders a search field with the search icon, its placeholder and its accessible name", () => {
    render();

    const search = document.querySelector(hook("grid-search"));
    expect(search.getAttribute("role")).toBe("search");
    expect(search.getAttribute("data-testid-owner")).toBe("GrdUsr");
    expect(search.getAttribute("criterion-id")).toBe("CrtUsrTxt");
    expect(input().getAttribute("type")).toBe("search");
    expect(input().getAttribute("placeholder")).toBe("SCREEN_TEXT_USER_SEARCH");
    expect(input().getAttribute("aria-label")).toBe("BUTTON_SEARCH");
    expect(input().getAttribute("data-testid")).toBe("criterion-input");
    expect(search.querySelector(".grid-search-icon svg use").getAttribute("href")).toContain("#search");
    expect(clearButton()).toBeNull();
  });

  it("names the field with its label and shows its value", () => {
    render("adm", { label: "SCREEN_TEXT_USER_SEARCH_LABEL" });

    expect(input().getAttribute("aria-label")).toBe("SCREEN_TEXT_USER_SEARCH_LABEL");
    expect(input().value).toBe("adm");
    expect(clearButton().getAttribute("aria-label")).toBe("BUTTON_CLEAR");
  });

  it("sends its value as a criterion and filters the grid 300 ms after the user stops typing", () => {
    const { store } = render();

    type("ad");
    act(() => jest.advanceTimersByTime(200));
    type("adm");
    act(() => jest.advanceTimersByTime(299));
    expect(filters(store)).toEqual([]);

    act(() => jest.advanceTimersByTime(1));
    expect(getFormValues(store.getState()).CrtUsrTxt).toBe("adm");
    expect(filters(store)).toEqual([{ view: "report", component: "GrdUsr" }]);
  });

  it("sends its value with the request of the grid", () => {
    const { store } = render("adm");

    act(() => {
      store.dispatch(filterAction({ type: "filter", address: { view: "report", component: "GrdUsr" } }));
    });

    const request = queued(store).find(action => action.type === "server" && action.parameters.component === "GrdUsr");
    expect(request.parameters.targetAction).toBe("UsrLst");
    expect(request.parameters.CrtUsrTxt).toBe("adm");
  });

  it("filters the grid at once with Enter, without waiting nor filtering twice", () => {
    const { store } = render();

    type("adm");
    const enter = fireEvent.keyDown(input(), { key: "Enter" });

    expect(enter).toBe(false);
    expect(filters(store).length).toBe(1);
    act(() => jest.advanceTimersByTime(300));
    fireEvent.keyDown(input(), { key: "Enter" });
    expect(filters(store).length).toBe(1);
  });

  it("empties the field and filters the grid with the clear button, keeping the focus in the field", () => {
    const { store } = render("adm");

    fireEvent.click(clearButton());

    expect(input().value).toBe("");
    expect(getFormValues(store.getState()).CrtUsrTxt).toBe("");
    expect(filters(store)).toEqual([{ view: "report", component: "GrdUsr" }]);
    expect(document.activeElement).toBe(input());
    expect(clearButton()).toBeNull();
  });

  it("empties the field and filters the grid with Escape, and does nothing when it is empty", () => {
    const { store } = render("adm");
    const escape = () => {
      const event = new KeyboardEvent("keydown", { key: "Escape", bubbles: true, cancelable: true });
      const stop = jest.spyOn(event, "stopPropagation");
      fireEvent(input(), event);
      return stop;
    };

    expect(escape()).toHaveBeenCalled();
    expect(input().value).toBe("");
    expect(filters(store).length).toBe(1);

    expect(escape()).not.toHaveBeenCalled();
    expect(filters(store).length).toBe(1);
  });

  it("does not filter when the text is typed back to the filtered one, nor when it is read only", () => {
    const { store, unmount } = render("adm");

    type("admi");
    type("adm");
    act(() => jest.advanceTimersByTime(300));
    expect(filters(store)).toEqual([]);
    unmount();

    render("", { readonly: true });
    expect(input().disabled).toBe(true);
  });
});
