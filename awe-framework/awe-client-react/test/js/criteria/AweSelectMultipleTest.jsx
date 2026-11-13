import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSelectMultiple from "../../../src/criteria/AweSelectMultiple";
import {act, cleanup, fireEvent, screen, waitFor} from "@testing-library/react";
import i18n from "../i18nForTests";

const hook = (testId) => `[data-testid='${testId}']`;

// The chips of PrimeReact (display="chip"), each one with its own remove icon
function criterion(values, placeholder = "placeholder") {
  return {
    settings: DEFAULT_SETTINGS,
    components: {
      "select-multiple": {
        address: {component: 'select-multiple', view: 'report'},
        model: {values},
        attributes: {
          placeholder,
          readonly: false,
          display: "chip",
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };
}

describe('awe-react-client/test/js/criteria/AweSelectMultipleTest.jsx (display chip)', () => {

  afterEach(() => {
    cleanup();
    jest.restoreAllMocks();
  });

  const preloadedState = criterion([{label: 'test', value: 'test', selected: true}, {
    label: 'tutu',
    value: 'tutu',
    selected: true
  }]);

  it('renders Awe Select Multiple component', async () => {
    renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {preloadedState});

    // check
    expect(await screen.findByText("test")).not.toBeNull();
    expect(await screen.findByText("tutu")).not.toBeNull();
  });

  it('removes the value of the chip whose remove icon is clicked, also when the chips overflow the field', () => {
    // The chips are wider than the field: PrimeReact ignores the click on a chip it considers out of sight
    const isLabel = (element) => element.getAttribute?.("data-pc-section") === "label";
    jest.spyOn(HTMLElement.prototype, "clientWidth", "get").mockImplementation(function () {
      return isLabel(this) ? 200 : 0;
    });
    jest.spyOn(HTMLElement.prototype, "scrollWidth", "get").mockImplementation(function () {
      return isLabel(this) ? 600 : 0;
    });
    const {container, store} = renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {
      preloadedState: criterion([
        {label: 'All resources · Read', value: '*.read', selected: true},
        {label: 'Notification subscriptions · All', value: 'subscriptions.*', selected: true},
        {label: 'Users · Update', value: 'users.update', selected: true},
        {label: 'Users · Read', value: 'users.read'}
      ])
    });
    const chips = () => Array.from(container.querySelectorAll(hook("select-choice")));
    const selected = () => store.getState().components["select-multiple"].model.values
      .filter(value => value.selected).map(value => value.value);

    fireEvent.click(chips()[1].querySelector(hook("select-choice-close")));
    expect(chips().map(chip => chip.textContent)).toEqual(['All resources · Read', 'Users · Update']);
    expect(selected()).toEqual(['*.read', 'users.update']);

    fireEvent.click(chips()[1].querySelector(hook("select-choice-close")));
    expect(chips().map(chip => chip.textContent)).toEqual(['All resources · Read']);
    expect(selected()).toEqual(['*.read']);
  });

  it.each([
    ["its placeholder", "placeholder", "placeholder"],
    ["nothing without placeholder", "", ""]
  ])('shows %s once every chip is removed', (_, placeholder, label) => {
    const values = [{label: 'test', value: 'test', selected: true}, {label: 'tutu', value: 'tutu', selected: true}];
    const {container} = renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {
      preloadedState: criterion(values, placeholder)
    });
    const close = () => container.querySelector(hook("select-choice-close"));

    while (close()) {
      fireEvent.click(close());
    }
    expect(container.querySelectorAll(hook("select-choice"))).toHaveLength(0);
    expect(container.querySelector(".p-multiselect-label").textContent).toBe(label);
  });

  it('removes the value of a chip from the keyboard', () => {
    const {container, store} = renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {preloadedState});

    fireEvent.keyDown(container.querySelectorAll(hook("select-choice-close"))[1], {key: "Enter", code: "Enter"});
    expect(store.getState().components["select-multiple"].model.values.map(value => value.selected))
      .toEqual([true, false]);
  });

  it('lists the options under the header of their group, in the order of their first option', () => {
    const {container} = renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {
      preloadedState: criterion([
        {label: 'Everything', value: '*'},
        {label: 'Users · Read', value: 'users.read', group: 'Users'},
        {label: 'Profiles · Read', value: 'profiles.read', group: 'Profiles'},
        {label: 'Users · Update', value: 'users.update', group: 'Users', selected: true}
      ])
    });

    fireEvent.click(container.querySelector(hook("select-trigger")));
    const panel = document.querySelector(hook("select-dropdown"));
    const rows = Array.from(panel.querySelectorAll("[data-pc-section='itemgroup'], " + hook("select-option")))
      .map(row => row.getAttribute("data-pc-section") === "itemgroup" ? `# ${row.textContent}`.trim() : row.textContent);
    // An option without group keeps its place, without header; an option does not repeat the header of its group, its
    // chip keeps the whole label
    expect(rows).toEqual(['#', 'Everything', '# Users', 'Read', 'Update', '# Profiles', 'Read']);
    expect(Array.from(container.querySelectorAll(hook("select-choice"))).map(chip => chip.textContent))
      .toEqual(['Users · Update']);
    // Selecting every option of a grouped list at once is not offered
    expect(panel.querySelector(".p-multiselect-header .p-checkbox")).toBeNull();
  });

  it('offers selecting every option of a list without groups', () => {
    const {container} = renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {preloadedState});

    fireEvent.click(container.querySelector(hook("select-trigger")));
    expect(document.querySelector(hook("select-dropdown")).querySelector(".p-multiselect-header .p-checkbox"))
      .not.toBeNull();
  });
});

describe('awe-react-client/test/js/criteria/AweSelectMultipleTest.jsx (one line)', () => {
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

  it.each([
    ["its placeholder", "placeholder", "placeholder"],
    ["nothing without placeholder", "", ""]
  ])("shows %s while no value is chosen", (_, placeholder, label) => {
    const {container} = render([], {placeholder});

    expect(container.querySelector(".p-multiselect-label").textContent).toBe(label);
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

    expect(container.querySelectorAll(".awe-select-chip")).toHaveLength(2);
    expect(container.querySelectorAll(".awe-select-chip [data-testid='select-choice-close']")).toHaveLength(2);
    expect(counter(container)).toBeNull();
  });

  it("lists the values of a readonly field without offering to remove them", async () => {
    availableWidth = 100;
    const {container} = render(["test", "tutu", "toto"], {readonly: true});

    fireEvent.click(counter(container));

    expect(await screen.findByText("toto", {selector: ".awe-select-multiple-list span"})).not.toBeNull();
    expect(screen.queryAllByLabelText(/^Remove /)).toHaveLength(0);
    expect(document.querySelector("[data-testid='select-choice-close']")).toBeNull();
  });
});
