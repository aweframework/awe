import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSuggestMultiple from "../../../src/criteria/AweSuggestMultiple";
import {updateModel} from "../../../src/redux/actions/components";
import {selectAction} from "../../../src/redux/thunks/form";
import {act, cleanup, fireEvent, screen} from "@testing-library/react";
import i18n from "../i18nForTests";

describe('awe-react-client/test/js/criteria/AweSuggestMultipleTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      "suggest-multiple": {
        address: {component: 'suggest-multiple', view: 'report'},
        model: {
          values: [{label: 'test', value: 'test', selected: true}, {
            label: 'tutu',
            value: 'tutu',
            selected: true
          }]
        },
        attributes: {
          placeholder: "placeholder",
          readonly: false,
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Suggest Multiple component', () => {
    renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState});

    // check
    //console.info(document.querySelector("#suggest-multiple"));
    expect(document.querySelector("#suggest-multiple input[aria-autocomplete]")).not.toBeNull();
  });

  it('shows the values that an update of the model selects after it was rendered', async () => {
    const {store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState});
    expect(document.querySelectorAll("#suggest-multiple .p-autocomplete-token")).toHaveLength(2);

    // A dependency (a server action that selects values) replaces the selected values of the criterion
    await act(async () => {
      store.dispatch(updateModel({component: 'suggest-multiple', view: 'report'}, {
        values: [{label: 'pei (pei@test.com)', value: 'pei', selected: true}]
      }));
    });

    const tokens = Array.from(document.querySelectorAll("#suggest-multiple .p-autocomplete-token"));
    expect(tokens.map(token => token.textContent)).toEqual(['pei (pei@test.com)']);
  });

  it('shows the values with a numeric label as text chips', async () => {
    const numericState = {
      ...preloadedState,
      components: {
        "suggest-multiple": {
          ...preloadedState.components["suggest-multiple"],
          model: {values: [{label: 1, value: 1, selected: true}, {label: 2, value: 2, selected: true}]}
        }
      }
    };
    const errors = [];
    const onError = event => { errors.push(event.error || event.message); event.preventDefault(); };
    window.addEventListener("error", onError);

    await act(async () => {
      renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState: numericState});
    });
    const input = document.querySelector("#suggest-multiple input[aria-autocomplete]");
    await act(async () => {
      fireEvent.focus(input);
      fireEvent.blur(input);
    });

    window.removeEventListener("error", onError);
    expect(errors).toEqual([]);
    const tokens = Array.from(document.querySelectorAll("#suggest-multiple .p-autocomplete-token"));
    expect(tokens.map(token => token.textContent)).toEqual(['1', '2']);
  });

  describe('chips', () => {
    const hook = (testId) => `[data-testid='${testId}']`;
    const threeValues = {
      ...preloadedState,
      components: {
        "suggest-multiple": {
          ...preloadedState.components["suggest-multiple"],
          model: {values: [{label: 'test', value: 'test', selected: true}, {label: 'tutu', value: 'tutu', selected: true},
            {label: 'tete', value: 'tete', selected: true}]}
        }
      }
    };
    const selected = (store) => store.getState().components["suggest-multiple"].model.values
      .filter(item => item.selected).map(item => item.value);

    it('removes the value of the chip whose remove icon is clicked, drawn with the Lucide x of the design system', () => {
      const {container, store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>,
        {preloadedState: threeValues});
      const chips = () => Array.from(container.querySelectorAll(hook("select-choice")));
      expect(chips().map(chip => chip.textContent)).toEqual(['test', 'tutu', 'tete']);

      const close = chips()[1].querySelector(hook("select-choice-close"));
      expect(close.querySelector("svg use")).not.toBeNull();
      fireEvent.click(close);

      expect(chips().map(chip => chip.textContent)).toEqual(['test', 'tete']);
      expect(selected(store)).toEqual(['test', 'tete']);
    });

    it('removes the value of a chip from the keyboard', () => {
      const {container, store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>,
        {preloadedState: threeValues});

      fireEvent.keyDown(container.querySelectorAll(hook("select-choice-close"))[1], {key: "Enter", code: "Enter"});

      expect(selected(store)).toEqual(['test', 'tete']);
    });
  });

  describe('values selected by a value dependency', () => {
    let originalFetch;
    const profiles = [{value: 7, label: 'LEC - Lector'}, {value: 9, label: 'ADM - Administrator'},
      {value: 12, label: 'CTB - Contributor'}];

    beforeAll(() => {
      originalFetch = window.fetch;
      // The check target answers the rows of the values it receives (a list of them for a multiple suggest)
      window.fetch = (url, {body}) => {
        const suggested = [JSON.parse(body).suggest].flat().map(String);
        const rows = profiles.filter(profile => suggested.includes(String(profile.value)));
        return Promise.resolve({ok: true, json: () => Promise.resolve([{type: "fill", parameters: {datalist: {rows}}}])});
      };
    });

    afterAll(() => {
      window.fetch = originalFetch;
    });

    const checkTargetState = (values) => ({
      settings: DEFAULT_SETTINGS,
      components: {
        "suggest-multiple": {
          address: {component: 'suggest-multiple', view: 'report'},
          model: {values},
          attributes: {placeholder: "", readonly: false, targetAction: "ProSug", checkTarget: "UsrAddProIni"},
          validationRules: {required: false},
          specificAttributes: {sort: []}
        }
      }
    });

    // The "value" server action of a dependency answers a select action with the rows of its query
    const select = (store, values) => act(async () => {
      store.dispatch(selectAction({type: "select", address: {component: 'suggest-multiple', view: 'report'},
        parameters: {values}}));
    });
    const chips = () => Array.from(document.querySelectorAll("#suggest-multiple .p-autocomplete-token"))
      .map(token => token.textContent);

    it('shows the labels of the values it selects by their id', async () => {
      const {store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>,
        {preloadedState: checkTargetState([])});

      await select(store, [{value: 7, label: 'LEC - Lector'}]);

      expect(chips()).toEqual(['LEC - Lector']);
    });

    it('keeps every value it selects when only some of them were already selected (another user is edited)', async () => {
      const {store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {
        preloadedState: checkTargetState([{...profiles[0], selected: true}, {...profiles[1], selected: true}])
      });

      await select(store, [{value: 9}, {value: 12}]);

      expect(chips()).toEqual(['ADM - Administrator', 'CTB - Contributor']);
      expect(store.getState().components["suggest-multiple"].model.values.filter(item => item.selected)
        .map(item => item.value)).toEqual([9, 12]);
    });
  });

  describe('typing and picking a result', () => {
    let originalFetch;
    const answers = {};

    beforeAll(() => {
      originalFetch = window.fetch;
      // The answer to a search is the one registered for the text (the delay of the server is simulated)
      window.fetch = (url, {body, signal}) => {
        const {suggest} = JSON.parse(body);
        const {rows, delay} = answers[suggest] || {rows: [], delay: 0};
        return new Promise((resolve, reject) => {
          const timer = setTimeout(() => resolve({
            ok: true,
            json: () => Promise.resolve([{type: "fill", parameters: {datalist: {rows}}}])
          }), delay);
          signal?.addEventListener("abort", () => {
            clearTimeout(timer);
            reject(Object.assign(new Error("aborted"), {name: "AbortError"}));
          });
        });
      };
    });

    afterAll(() => {
      window.fetch = originalFetch;
    });

    const strictFalseState = {
      settings: DEFAULT_SETTINGS,
      components: {
        "suggest-multiple": {
          address: {component: 'suggest-multiple', view: 'report'},
          model: {values: []},
          attributes: {placeholder: "", readonly: false, strict: false, timeout: 10, targetAction: "Delayed"},
          validationRules: {required: true},
          specificAttributes: {sort: []}
        }
      }
    };

    it('keeps the chip of the result picked after a search that was replaced while it was loading', async () => {
      answers.tee = {rows: [], delay: 400};
      answers.test = {rows: [{label: 'test (test@test.com)', value: 'test'}], delay: 400};
      renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState: strictFalseState});
      const input = () => document.querySelector("#suggest-multiple input[aria-autocomplete]");
      const wait = (ms) => act(async () => { await new Promise(resolve => setTimeout(resolve, ms)); });

      // First search, replaced by a second one before the first answer arrives
      fireEvent.change(input(), {target: {value: "tee"}});
      await wait(200);
      fireEvent.change(input(), {target: {value: ""}});
      fireEvent.change(input(), {target: {value: "test"}});
      await wait(700);

      // Pick the result
      const option = Array.from(document.querySelectorAll(".p-autocomplete-item"))
        .find(item => item.textContent.includes("test"));
      expect(option).toBeDefined();
      await act(async () => { fireEvent.click(option); });
      await wait(200);

      const tokens = Array.from(document.querySelectorAll("#suggest-multiple .p-autocomplete-token"));
      expect(tokens.map(token => token.textContent)).toEqual(['test (test@test.com)']);
    });

    it('keeps the focus on the input while the mouse is pressed on a result, so the list stays where it is', async () => {
      // Taking the focus away re-aligns the list (it can flip from above to below the input after the page scrolled),
      // and the click would then land outside of the result that was pressed (a real browser, not jsdom)
      answers.test = {rows: [{label: 'test (test@test.com)', value: 'test'}], delay: 0};
      renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState: strictFalseState});
      const input = document.querySelector("#suggest-multiple input[aria-autocomplete]");
      const wait = (ms) => act(async () => { await new Promise(resolve => setTimeout(resolve, ms)); });
      fireEvent.change(input, {target: {value: "test"}});
      await wait(300);

      const option = document.querySelector(".p-autocomplete-item");
      expect(option).not.toBeNull();

      // fireEvent returns false when the default action of the event (the focus change) was prevented
      expect(fireEvent.mouseDown(option)).toBe(false);
    });

    it('keeps one chip when the same value is chosen twice', async () => {
      answers.test = {rows: [{label: 'test (test@test.com)', value: 'test'}], delay: 0};
      const {store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState: strictFalseState});
      const input = () => document.querySelector("#suggest-multiple input[aria-autocomplete]");
      const wait = (ms) => act(async () => { await new Promise(resolve => setTimeout(resolve, ms)); });

      // The server answers the same option again as a new object, so picking the same value a second time describes
      // two elements of the list unless the client compares the value
      const pick = async () => {
        fireEvent.change(input(), {target: {value: "test"}});
        await wait(300);
        const option = document.querySelector(".p-autocomplete-item");
        await act(async () => { fireEvent.click(option); });
        await wait(200);
      };
      await pick();
      await pick();

      expect(document.querySelectorAll("#suggest-multiple .p-autocomplete-token")).toHaveLength(1);
      expect(store.getState().components["suggest-multiple"].model.values.filter(item => item.selected)).toHaveLength(1);
    });

    it('shows the dropdown trigger of a suggest', () => {
      renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState: strictFalseState});

      // The same chevron of a suggest, which opens the list without typing anything
      const trigger = document.querySelector("[data-testid='select-trigger']");
      expect(trigger).not.toBeNull();
      expect(trigger.nodeName).toBe("BUTTON");
    });
  });

  describe('clearing the field', () => {
    it('empties every chosen value from the clear button of the field', async () => {
      const {store} = renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState});
      expect(document.querySelectorAll("#suggest-multiple .p-autocomplete-token")).toHaveLength(2);

      const clear = document.querySelector("[data-testid='select-clear']");
      expect(clear).not.toBeNull();

      await act(async () => { fireEvent.click(clear); });

      expect(document.querySelectorAll("#suggest-multiple .p-autocomplete-token")).toHaveLength(0);
      expect(store.getState().components["suggest-multiple"].model.values.filter(item => item.selected)).toEqual([]);
    });

    it('does not offer the clear button of a required suggest', () => {
      const requiredState = {
        ...preloadedState,
        components: {
          "suggest-multiple": {
            ...preloadedState.components["suggest-multiple"],
            validationRules: {required: true},
            attributes: {placeholder: "placeholder", readonly: false}
          }
        }
      };
      renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {preloadedState: requiredState});

      expect(document.querySelector("[data-testid='select-clear']")).toBeNull();
    });
  });

  // display="summary": the chips that fit in one line and a +N counter that lists them all (#845)
  describe('one line (display summary)', () => {
    const WIDTH_PER_CHARACTER = 10;
    const COUNTER_WIDTH = 20;
    let availableWidth;

    const render = (attributes = {}) => renderWithProviders(<AweSuggestMultiple id="suggest-multiple"/>, {
      preloadedState: {
        ...preloadedState,
        components: {
          "suggest-multiple": {
            ...preloadedState.components["suggest-multiple"],
            attributes: {...preloadedState.components["suggest-multiple"].attributes, ...attributes},
            model: {values: ["test", "tutu", "toto"].map(value => ({label: value, value, selected: true}))}
          }
        }
      }
    });
    const shownChips = container => Array.from(container.querySelectorAll("[data-testid='select-choice']"))
      .filter(chip => !chip.hasAttribute("aria-hidden"));
    const counter = container => container.querySelector("button[aria-haspopup='dialog']");

    beforeAll(() => {
      i18n.addResourceBundle("en", "translation", {
        SCREEN_TEXT_SELECTED_VALUES_SHOW_ALL: "+{{count}} selected values, show all",
        SCREEN_TEXT_REMOVE_VALUE: "Remove {{label}}"
      }, true, true);
    });

    // JSDOM has no layout: a chip is as wide as its label, the counter is fixed and the field is 100 wide
    beforeEach(() => {
      availableWidth = 100;
      global.ResizeObserver = class {
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

    it('shows the chips that fit and a counter with the number of hidden values', () => {
      const {container} = render({display: "summary"});

      expect(shownChips(container).map(chip => chip.textContent)).toEqual(["test", "tutu"]);
      expect(counter(container).textContent).toBe("+1");
      expect(counter(container).getAttribute("aria-label")).toBe("+1 selected values, show all");
    });

    it('removes a value from the list of the counter and updates the field and the model', async () => {
      const {container, store} = render({display: "summary"});
      fireEvent.click(counter(container));

      fireEvent.click(await screen.findByLabelText("Remove test"));

      expect(store.getState().components["suggest-multiple"].model.values.filter(item => item.selected)
        .map(item => item.value)).toEqual(["tutu", "toto"]);
      expect(shownChips(container).map(chip => chip.textContent)).toEqual(["tutu", "toto"]);
      expect(counter(container)).toBeNull();
    });

    it('keeps every chip, with no counter, without display summary', () => {
      const {container} = render();

      expect(shownChips(container).map(chip => chip.textContent)).toEqual(["test", "tutu", "toto"]);
      expect(counter(container)).toBeNull();
    });
  });
});
