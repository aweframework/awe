import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {act} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputText from "../../../src/criteria/AweInputText";
import AweInputPassword from "../../../src/criteria/AweInputPassword";
import AweInputNumeric from "../../../src/criteria/AweInputNumeric";
import AweInputDate from "../../../src/criteria/AweInputDate";
import AweInputTime from "../../../src/criteria/AweInputTime";
import AweInputTextarea from "../../../src/criteria/AweInputTextarea";
import AweSuggest from "../../../src/criteria/AweSuggest";
import AweSuggestMultiple from "../../../src/criteria/AweSuggestMultiple";
import AweSelect from "../../../src/criteria/AweSelect";
import AweSelectMultiple from "../../../src/criteria/AweSelectMultiple";

// A required criterion without label takes no label row and shows the required state in the field (#898)
describe('awe-react-client/test/js/criteria/AweCriterionRequiredTest.jsx', () => {

  const stateWith = (attributes, validationRules = {required: true}) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      field: {
        address: {component: 'field', view: 'report'},
        model: {values: [{label: 'One', value: '1'}, {label: 'Two', value: '2'}]},
        attributes: {placeholder: "Username", readonly: false, visible: true, ...attributes},
        validationRules,
        specificAttributes: {sort: []}
      }
    }
  });

  const render = async (Component, attributes, validationRules) => {
    await act(async () => {
      renderWithProviders(<Component id="field"/>, {preloadedState: stateWith(attributes, validationRules)});
    });
    return document.querySelector("[criterion-id='field']");
  };

  // Components whose focusable element is an input with the placeholder
  const inputCriteria = [
    ["text", AweInputText, "input#field"],
    ["password", AweInputPassword, "input#field"],
    ["numeric", AweInputNumeric, "input#field, #field input"],
    ["date", AweInputDate, "input#field, #field input"],
    ["time", AweInputTime, "input#field, #field input"],
    ["textarea", AweInputTextarea, "textarea#field"],
    ["suggest", AweSuggest, "#field input[aria-autocomplete]"]
  ];

  describe.each(inputCriteria)('%s', (name, Component, selector) => {
    it('draws no label row and appends the asterisk to the placeholder when it is required', async () => {
      const criterion = await render(Component, {});

      expect(criterion.querySelector("label, div.block")).toBeNull();
      expect(criterion.querySelector(selector)).toHaveAttribute("placeholder", "Username *");
    });

    it('marks the focusable input as required for assistive technology', async () => {
      const criterion = await render(Component, {});

      expect(criterion.querySelector(selector)).toHaveAttribute("aria-required", "true");
    });

    it('keeps the label and the plain placeholder when the criterion has a label', async () => {
      const criterion = await render(Component, {label: "Name"});

      expect(criterion.querySelector("label")).toHaveTextContent("Name");
      expect(criterion.querySelector(selector)).toHaveAttribute("placeholder", "Username");
    });

    it('is not marked as required when it has no required rule', async () => {
      const criterion = await render(Component, {}, {});

      expect(criterion.querySelector(selector)).toHaveAttribute("placeholder", "Username");
      expect(criterion.querySelector(selector)).not.toHaveAttribute("aria-required");
    });
  });

  it('keeps the label row of a criterion with a left label and no text', async () => {
    const criterion = await render(AweInputText, {leftLabel: "10"});

    expect(criterion.querySelector("div.block")).not.toBeNull();
  });

  it('draws no label row for the suggest with multiple choices and marks its input', async () => {
    const criterion = await render(AweSuggestMultiple, {});
    const input = criterion.querySelector("#field input[aria-autocomplete], input[aria-autocomplete]");

    expect(criterion.querySelector("label, div.block")).toBeNull();
    expect(input).toHaveAttribute("aria-required", "true");
    expect(input).toHaveAttribute("placeholder", "Username *");
  });

  describe.each([["select", AweSelect], ["multiple select", AweSelectMultiple]])('%s', (name, Component) => {
    it('draws no label row and leaves the asterisk of the placeholder to the styles, so it shows only one', async () => {
      const criterion = await render(Component, {});

      expect(criterion.querySelector("label, div.block")).toBeNull();
      expect(criterion).toHaveClass("required");
      expect(criterion.textContent).not.toContain("*");
    });

    it('marks the focusable element as required for assistive technology', async () => {
      const criterion = await render(Component, {});

      expect(criterion.querySelector("[aria-required='true']")).not.toBeNull();
    });

    it('is not marked as required when it has no required rule', async () => {
      const criterion = await render(Component, {}, {});

      expect(criterion.querySelector("[aria-required]")).toBeNull();
    });
  });
});
