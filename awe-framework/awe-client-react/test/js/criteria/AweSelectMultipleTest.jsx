import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSelectMultiple from "../../../src/criteria/AweSelectMultiple";
import {act, cleanup, fireEvent, screen, waitFor} from "@testing-library/react";
import i18n from "../i18nForTests";

describe('awe-react-client/test/js/criteria/AweSelectMultipleTest.jsx', () => {

  const WIDTH_PER_CHARACTER = 10;
  const COUNTER_WIDTH = 20;
  let availableWidth;
  let resizeCallbacks;

  const stateWith = (selected, attributes = {}) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      "select-multiple": {
        address: {component: "select-multiple", view: "report"},
        attributes: {placeholder: "placeholder", readonly: false, optional: true, label: "Values", ...attributes},
        validationRules: {required: false},
        specificAttributes: {sort: []},
        model: {values: ["test", "tutu", "toto"].map(value => ({label: value, value, selected: selected.includes(value)}))}
      }
    }
  });

  const render = (selected, attributes) =>
    renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {preloadedState: stateWith(selected, attributes)});

  const resize = () => act(() => resizeCallbacks.forEach(callback => callback()));
  const shownChips = container =>
    Array.from(container.querySelectorAll("[data-testid='select-choice']")).filter(chip => !chip.hasAttribute("aria-hidden"));
  const counter = container => container.querySelector("button[aria-haspopup='dialog']");

  beforeAll(() => {
    i18n.addResourceBundle("en", "translation", {
      SCREEN_TEXT_SELECTED_VALUES_SHOW_ALL: "+{{count}} selected values, show all",
      SCREEN_TEXT_REMOVE_VALUE: "Remove {{label}}"
    }, true, true);
  });

  // JSDOM has no layout: a chip is as wide as its label, the counter is fixed and the field width is set by each test
  beforeEach(() => {
    availableWidth = 1000;
    resizeCallbacks = [];
    global.ResizeObserver = class {
      constructor(callback) {
        resizeCallbacks.push(callback);
      }
      observe() {}
      disconnect() {}
    };
    jest.spyOn(HTMLElement.prototype, "scrollWidth", "get").mockImplementation(function () {
      return this.hasAttribute("data-chip") ? this.textContent.length * WIDTH_PER_CHARACTER : 0;
    });
    jest.spyOn(HTMLElement.prototype, "offsetWidth", "get").mockImplementation(function () {
      return this.getAttribute("aria-haspopup") ? COUNTER_WIDTH : 0;
    });
    jest.spyOn(HTMLElement.prototype, "clientWidth", "get").mockImplementation(function () {
      return this.classList.contains("awe-select-multiple-chips") ? availableWidth : 0;
    });
  });

  afterEach(() => {
    jest.restoreAllMocks();
    cleanup();
  });

  it("shows every chosen value as a chip, in option order, with no counter when they fit", () => {
    const {container} = render(["toto", "test"]);

    expect(shownChips(container).map(chip => chip.textContent)).toEqual(["test", "toto"]);
    expect(counter(container)).toBeNull();
  });

  it("shows the chips that fit and a counter with the number of hidden values", () => {
    availableWidth = 100;
    const {container} = render(["test", "tutu", "toto"]);

    expect(shownChips(container).map(chip => chip.textContent)).toEqual(["test", "tutu"]);
    expect(counter(container).textContent).toBe("+1");
    expect(counter(container).getAttribute("aria-label")).toBe("+1 selected values, show all");
    expect(counter(container).getAttribute("aria-expanded")).toBe("false");
  });

  it("recomputes the counter when the field is resized", () => {
    availableWidth = 100;
    const {container} = render(["test", "tutu", "toto"]);

    availableWidth = 400;
    resize();

    expect(shownChips(container)).toHaveLength(3);
    expect(counter(container)).toBeNull();
  });

  it("gives every chip its full label as a title", () => {
    const {container} = render(["test"]);

    expect(shownChips(container)[0].getAttribute("title")).toBe("test");
  });

  it("opens a popover with every value and no option list when the counter is clicked", async () => {
    availableWidth = 100;
    const {container} = render(["test", "tutu", "toto"]);

    fireEvent.click(counter(container));

    expect(counter(container).getAttribute("aria-expanded")).toBe("true");
    expect((await screen.findAllByLabelText(/^Remove /)).map(button => button.getAttribute("aria-label")))
      .toEqual(["Remove test", "Remove tutu", "Remove toto"]);
    expect(document.querySelector("[data-testid='select-dropdown']")).toBeNull();
  });

  it("removes a value from the popover and updates the field and the counter", async () => {
    availableWidth = 100;
    const {container, store} = render(["test", "tutu", "toto"]);
    fireEvent.click(counter(container));

    fireEvent.click(await screen.findByLabelText("Remove test"));

    expect(store.getState().components["select-multiple"].model.values.filter(value => value.selected).map(value => value.value))
      .toEqual(["tutu", "toto"]);
    expect(shownChips(container).map(chip => chip.textContent)).toEqual(["tutu", "toto"]);
    expect(counter(container)).toBeNull();
  });

  it("closes the popover with Escape and gives the focus back to the counter", async () => {
    availableWidth = 100;
    const {container} = render(["test", "tutu", "toto"]);
    fireEvent.click(counter(container));
    await screen.findByLabelText("Remove test");

    fireEvent.keyDown(document, {key: "Escape", code: "Escape"});

    await waitFor(() => expect(document.activeElement).toBe(counter(container)));
    expect(counter(container).getAttribute("aria-expanded")).toBe("false");
  });

  it("names the combobox with the id the label points to", () => {
    const {container} = render(["test"]);

    expect(container.querySelector("input[role='combobox']").id).toBe("select-multiple");
    expect(container.querySelector("label").getAttribute("for")).toBe("select-multiple");
  });

  it("keeps the chips of PrimeReact, with a remove icon, when the criterion asks for display chip", () => {
    const {container} = render(["test", "tutu"], {display: "chip"});

    expect(container.querySelectorAll(".p-multiselect-token")).toHaveLength(2);
    expect(counter(container)).toBeNull();
  });
});
