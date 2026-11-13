import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputCheckbox from "../../../src/criteria/AweInputCheckbox";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputCheckboxTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      checkbox: {
        address: {component: 'checkbox', view: 'report'},
        model: {values: [{label: 'test', value: '1', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "test"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      },
      switch: {
        address: {component: 'switch', view: 'report'},
        model: {values: [{label: 'test', value: '1', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "test",
          style: "switch",
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Input Checkbox component', async () => {
    renderWithProviders(<AweInputCheckbox id="checkbox"/>, {preloadedState});

    // check
    expect(await screen.findByText("test")).not.toBeNull();
  });

  it('renders Awe Input Checkbox component as a switch', async () => {
    renderWithProviders(<AweInputCheckbox id="switch"/>, {preloadedState});

    // check
    expect(await screen.findByText("test")).not.toBeNull();
  });

  it('names the switch with its label, beside it, and tells its state (#793 S19)', async () => {
    renderWithProviders(<AweInputCheckbox id="switch"/>, {preloadedState});

    const control = await screen.findByRole("switch", {name: "test"});
    expect(control.getAttribute("aria-checked")).toBe("true");
    expect(control.disabled).toBe(false);
  });

  // As the AngularJS engine, a checkbox or switch keeps an empty label row above it, so it lines up with the fields of
  // its row; the no-label style removes it (#793)
  const labelRowOf = (id, style) => {
    const state = {...preloadedState, components: {...preloadedState.components,
      [id]: {...preloadedState.components[id], attributes: {...preloadedState.components[id].attributes, style}}}};
    const {container} = renderWithProviders(<AweInputCheckbox id={id}/>, {preloadedState: state});
    return container.querySelector('.field > div.block[aria-hidden="true"]');
  };

  it.each([['checkbox', ''], ['switch', 'switch']])('keeps an empty label row above a %s, as the AngularJS engine', (id, style) => {
    expect(labelRowOf(id, style)).not.toBeNull();
  });

  it('has no label row with the no-label style', () => {
    expect(labelRowOf('switch', 'switch no-label')).toBeNull();
  });
});
