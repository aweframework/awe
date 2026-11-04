import React from "react";
import i18n from "i18next";
import {act, fireEvent, waitFor} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweSuggest from "../../../src/criteria/AweSuggest";
import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";

/**
 * A suggest groups its options when its rows carry a group (a locale key): one header per group, in the order of first
 * appearance, that cannot be chosen. A suggest whose rows have no group shows its options as always (#791)
 */
describe("awe-react-client/test/js/criteria/AweSuggestGroupTest.jsx", () => {
  const GROUPED = [
    {value: "profile:1", label: "ADM - Administrator", group: "PARAMETER_PROFILE"},
    {value: "user:1", label: "test - Manager", group: "PARAMETER_USER"},
    {value: "profile:2", label: "OPE - Operator", group: "PARAMETER_PROFILE"}
  ];
  const FLAT = [
    {value: "1", label: "ADM - Administrator"},
    {value: "2", label: "OPE - Operator"}
  ];

  let originalFetch;
  let fetchRows = [];

  beforeAll(() => {
    i18n.addResources("en-GB", "translation", {PARAMETER_PROFILE: "Profile", PARAMETER_USER: "User"});
    originalFetch = window.fetch;
    window.fetch = () => Promise.resolve({
      ok: true,
      json: () => Promise.resolve([{type: "fill", parameters: {datalist: {rows: fetchRows}}}])
    });
  });

  afterAll(() => {
    window.fetch = originalFetch;
  });

  const state = () => ({
    settings: DEFAULT_SETTINGS,
    components: {
      suggest: {
        address: {component: "suggest", view: "report"},
        model: {values: []},
        // The text is searched at once
        attributes: {timeout: 1},
        validationRules: {required: false},
        specificAttributes: {sort: []}
      }
    }
  });

  const search = async (rows) => {
    fetchRows = rows;
    let rendered;
    await act(async () => {
      rendered = renderWithProviders(<AweSuggest id="suggest"/>, {preloadedState: state()});
    });
    const input = rendered.container.querySelector("#suggest input[aria-autocomplete]");
    await act(async () => {
      fireEvent.focus(input);
      fireEvent.change(input, {target: {value: "e"}});
    });
    await waitFor(() => expect(document.querySelector('[data-pc-section="item"]')).not.toBeNull());
    return {input, store: rendered.store};
  };

  const list = () => [...document.querySelectorAll('[data-pc-section="itemgroup"], [data-pc-section="item"]')]
    .map(row => `${row.dataset.pcSection === "itemgroup" ? "#" : ""}${row.textContent}`);
  const highlighted = () => document.querySelector('[data-pc-section="item"][data-p-highlight="true"]')?.textContent;
  const selected = store => store.getState().components.suggest.model.values.filter(item => item.selected);

  it("shows one translated header per group, in the order of first appearance", async () => {
    await search(GROUPED);

    expect(list()).toEqual(["#Profile", "ADM - Administrator", "OPE - Operator", "#User", "test - Manager"]);
  });

  it("does not choose a header, and the keyboard goes from option to option over them", async () => {
    const {input, store} = await search(GROUPED);

    fireEvent.click(document.querySelector('[data-pc-section="itemgroup"]'));
    expect(selected(store)).toEqual([]);

    // The first option is highlighted, not the header above it
    expect(highlighted()).toBe("ADM - Administrator");
    await act(async () => {
      fireEvent.keyDown(input, {key: "ArrowDown", code: "ArrowDown", which: 40, keyCode: 40});
      fireEvent.keyDown(input, {key: "ArrowDown", code: "ArrowDown", which: 40, keyCode: 40});
    });
    expect(highlighted()).toBe("test - Manager");
    await act(async () => {
      fireEvent.keyDown(input, {key: "Enter", code: "Enter", which: 13, keyCode: 13});
    });

    // The chosen option keeps its own label
    expect(selected(store).map(item => [item.value, item.label])).toEqual([["user:1", "test - Manager"]]);
    expect(input.value).toBe("test - Manager");
  });

  it("keeps the options of different groups with the same label", async () => {
    await search([
      {value: "profile:3", label: "test", group: "PARAMETER_PROFILE"},
      {value: "user:1", label: "test", group: "PARAMETER_USER"}
    ]);

    expect(list()).toEqual(["#Profile", "test", "#User", "test"]);
  });

  it("shows the options of rows without a group as always, without headers", async () => {
    const {input, store} = await search(FLAT);

    expect(list()).toEqual(["ADM - Administrator", "OPE - Operator"]);
    expect(highlighted()).toBe("ADM - Administrator");
    await act(async () => {
      fireEvent.keyDown(input, {key: "Enter", code: "Enter", which: 13, keyCode: 13});
    });
    expect(selected(store).map(item => item.value)).toEqual(["1"]);
  });
});
