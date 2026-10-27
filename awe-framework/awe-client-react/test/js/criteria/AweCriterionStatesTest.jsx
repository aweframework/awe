import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {screen} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputText from "../../../src/criteria/AweInputText";

// The states of a criterion keep their classes, attributes and test hooks with the AWE 5 look (#885)
describe('awe-react-client/test/js/criteria/AweCriterionStatesTest.jsx', () => {

  const stateWith = (attributes, validationRules = {}) => ({
    settings: DEFAULT_SETTINGS,
    components: {
      text: {
        address: {component: 'text', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {label: "Name", readonly: false, visible: true, ...attributes},
        validationRules,
        specificAttributes: {sort: []}
      }
    }
  });

  const criterion = () => document.querySelector("[criterion-id='text']");

  it('keeps the invalid class and shows the message with a decorative icon under the field', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState: stateWith({error: {message: "Required field"}})});

    expect(document.querySelector("input#text")).toHaveClass("p-invalid");
    const message = screen.getByTestId("criterion-error");
    expect(message.tagName).toBe("SMALL");
    expect(message).toHaveClass("p-invalid");
    expect(message).toHaveTextContent("Required field");
    expect(message.parentElement).toBe(criterion());
    const icon = message.querySelector(".criterion-error-icon");
    expect(icon).not.toBeNull();
    expect(icon.querySelector("svg.awe-icon")).toHaveAttribute("aria-hidden", "true");
    expect(icon.querySelector("use").getAttribute("href")).toMatch(/#circle-alert$/);
  });

  it('shows no message while the criterion is valid', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState: stateWith({})});

    expect(document.querySelector("input#text")).not.toHaveClass("p-invalid");
    expect(screen.queryByTestId("criterion-error")).toBeNull();
  });

  it('keeps the read-only criterion disabled and marked with p-disabled', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState: stateWith({readonly: true})});

    expect(criterion()).toHaveClass("field", "p-disabled");
    expect(document.querySelector("input#text")).toBeDisabled();
  });

  it('marks the required criterion and draws its label above the field', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState: stateWith({}, {required: true})});

    expect(criterion()).toHaveClass("field", "required");
    const label = criterion().firstElementChild;
    expect(label.tagName).toBe("LABEL");
    expect(label).toHaveAttribute("for", "text");
    expect(label).toHaveTextContent("Name");
  });

  it('keeps an empty label the size of a label, without a spacing utility', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState: stateWith({label: null})});

    const spacer = criterion().firstElementChild;
    expect(spacer.tagName).toBe("DIV");
    expect(spacer.className).toBe("block");
  });
});
