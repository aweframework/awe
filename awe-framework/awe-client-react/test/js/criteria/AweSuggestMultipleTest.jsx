import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSuggestMultiple from "../../../src/criteria/AweSuggestMultiple";
import {updateModel} from "../../../src/redux/actions/components";
import {act, fireEvent} from "@testing-library/react";

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
  });
});
