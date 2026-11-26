import React from "react";
import { fireEvent } from "@testing-library/react";

import { DEFAULT_SETTINGS } from "../../../src/redux/actions/settings";
import { renderWithProviders } from "../test-utils";
import AweGridQuickFilter from "../../../src/criteria/AweGridQuickFilter";
import { getFormValues } from "../../../src/redux/selectors/form";

const hook = (id) => `[data-testid="${id}"]`;

const ORIGINS = [
  { value: "", label: "All" },
  { value: "manual", label: "Manual", count: 3 },
  { value: "file", label: "File", count: 1, tone: "danger" }
];

const WINDOWS = [
  { value: "", label: "Any date" },
  { value: "24", label: "Last 24 h" },
  { value: "168", label: "Last 7 days" }
];

function state(display, values, label = "Origin") {
  return {
    settings: { ...DEFAULT_SETTINGS, token: "token" },
    screen: { size: { width: 100, height: 100 }, breadcrumbs: [], report: { name: "activity", option: "activity" } },
    components: {
      GrdExe: {
        address: { component: "GrdExe", view: "report" },
        model: { values: [], records: 0 },
        attributes: { id: "GrdExe", component: "grid", serverAction: "data", targetAction: "ExeLst", quickFilters: ["CrtFlt"],
          columnModel: [], headerModel: [], buttonModel: [], visible: true },
        specificAttributes: { sort: [] },
        context: { view: "report", source: [] }
      },
      CrtFlt: {
        address: { component: "CrtFlt", view: "report" },
        model: { values: values.map((option, index) => ({ ...option, selected: index === 0 })) },
        attributes: { id: "CrtFlt", component: "quick-filter", serverAction: "data", targetAction: "FltLst", visible: true,
          display, label },
        specificAttributes: {},
        validationRules: {},
        context: { view: "report", source: [] }
      }
    }
  };
}

const render = (display = "dropdown", values = ORIGINS) =>
  renderWithProviders(<AweGridQuickFilter id="CrtFlt" grid="GrdExe" />, { preloadedState: state(display, values) });
const toggle = () => document.querySelector(hook("grid-quick-filter-toggle"));
const items = () => Array.from(document.querySelectorAll(`${hook("grid-quick-filter-menu")} [role^='menuitem']`));
const text = (element) => element.textContent.replace(/\s+/g, " ").trim();
const filters = (store) => [...store.getState().actions.sync.flat(), ...store.getState().actions.async]
  .filter(action => action.type === "filter");

describe("awe-react-client/test/js/criteria/AweGridQuickFilterMenuTest.jsx", () => {
  beforeEach(() => {
    global.fetch = jest.fn(() => new Promise(() => undefined));
  });

  it("shows a dropdown quick filter as a menu button «Label: option» whose menu lists the options and their counts", () => {
    render();

    expect(document.querySelector("[role='radiogroup']")).toBeNull();
    expect(text(toggle())).toBe("Origin: All");
    expect(toggle().getAttribute("aria-haspopup")).toBe("menu");
    expect(toggle().getAttribute("aria-expanded")).toBe("false");

    fireEvent.click(toggle());

    expect(toggle().getAttribute("aria-expanded")).toBe("true");
    expect(items().map(text)).toEqual(["All", "Manual 3", "File 1"]);
    expect(items().map(item => item.getAttribute("role"))).toEqual(["menuitemradio", "menuitemradio", "menuitemradio"]);
    expect(items().map(item => item.getAttribute("aria-checked"))).toEqual(["true", "false", "false"]);
    expect(items()[2].querySelector(".grid-quick-filter-count").getAttribute("data-tone")).toBe("danger");
  });

  it("filters the grid with the option chosen in the menu, closes it and shows the option on the button", () => {
    const { store } = render();

    fireEvent.click(toggle());
    fireEvent.click(items()[1]);

    expect(getFormValues(store.getState()).CrtFlt).toBe("manual");
    expect(filters(store).map(action => action.address)).toEqual([{ view: "report", component: "GrdExe" }]);
    expect(toggle().getAttribute("aria-expanded")).toBe("false");
    expect(text(toggle())).toBe("Origin: Manual");
  });

  it("opens with the arrow keys on the chosen option, moves with them and closes with Escape on the button", () => {
    render();

    fireEvent.keyDown(toggle(), { key: "ArrowDown" });
    expect(toggle().getAttribute("aria-expanded")).toBe("true");
    expect(document.activeElement).toBe(items()[0]);

    fireEvent.keyDown(items()[0], { key: "ArrowDown" });
    expect(document.activeElement).toBe(items()[1]);
    fireEvent.keyDown(items()[1], { key: "ArrowUp" });
    fireEvent.keyDown(items()[0], { key: "ArrowUp" });
    expect(document.activeElement).toBe(items()[2]);

    fireEvent.keyDown(items()[2], { key: "Escape" });
    expect(toggle().getAttribute("aria-expanded")).toBe("false");
    expect(document.activeElement).toBe(toggle());
  });

  it("sends the preset of a date range quick filter as its value and an empty range", () => {
    const { store } = renderWithProviders(<AweGridQuickFilter id="CrtFlt" grid="GrdExe" />,
      { preloadedState: state("date-range", WINDOWS, "Started") });

    expect(text(toggle())).toBe("Started: Any date");
    fireEvent.click(toggle());
    expect(items().map(text)).toEqual(["Any date", "Last 24 h", "Last 7 days", "Custom range…"]);
    fireEvent.click(items()[1]);

    expect(getFormValues(store.getState())).toMatchObject({ CrtFlt: "24", "CrtFlt.from": null, "CrtFlt.to": null });
    expect(text(toggle())).toBe("Started: Last 24 h");
    expect(filters(store).length).toBe(1);
  });

  it("applies a custom range: sends it as <id>.from and <id>.to with no preset, shows it and filters the grid", () => {
    const { store } = renderWithProviders(<AweGridQuickFilter id="CrtFlt" grid="GrdExe" />,
      { preloadedState: state("date-range", WINDOWS, "Started") });

    fireEvent.click(toggle());
    fireEvent.click(document.querySelector(hook("grid-quick-filter-custom")));
    fireEvent.input(document.querySelector(hook("date-range-from")), { target: { value: "01/11/2026" } });
    fireEvent.input(document.querySelector(hook("date-range-to")), { target: { value: "02/11/2026" } });
    fireEvent.click(document.querySelector(hook("grid-quick-filter-range-apply")));

    expect(getFormValues(store.getState())).toMatchObject({ CrtFlt: null, "CrtFlt.from": "01/11/2026", "CrtFlt.to": "02/11/2026" });
    expect(text(toggle())).toBe("Started: 01/11/2026 – 02/11/2026");
    expect(filters(store).length).toBe(1);

    // Clear goes back to any date
    fireEvent.click(toggle());
    expect(items()[3].getAttribute("aria-checked")).toBe("true");
    fireEvent.click(items()[3]);
    fireEvent.click(document.querySelector(hook("grid-quick-filter-range-clear")));
    expect(getFormValues(store.getState())).toMatchObject({ CrtFlt: "", "CrtFlt.from": null, "CrtFlt.to": null });
    expect(text(toggle())).toBe("Started: Any date");
    expect(filters(store).length).toBe(2);
  });
});
