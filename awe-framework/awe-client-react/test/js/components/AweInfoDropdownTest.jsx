import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInfoDropdown from "../../../src/components/AweInfoDropdown";

describe('awe-react-client/test/js/criteria/AweInfoDropdownTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      infodropdown: {
        address: {component: 'infodropdown', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          help: "help",
          helpImage: "helpImage"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Info dropdown component', () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown"></AweInfoDropdown>, {preloadedState});

    // check
    expect(document.querySelector("button#infodropdown")).not.toBeNull();
  });

  it('is not disabled while actions run, as in the AngularJS engine', () => {
    renderWithProviders(<AweInfoDropdown id="infodropdown"></AweInfoDropdown>, {
      preloadedState: {...preloadedState, actions: {running: true, sync: [[]], async: []}}
    });

    expect(document.querySelector("button#infodropdown").hasAttribute("disabled")).toBe(false);
  });

  it('shows the unit as a PrimeReact info badge over the button', () => {
    const state = {
      ...preloadedState,
      components: {
        infodropdown: {
          ...preloadedState.components.infodropdown,
          attributes: {...preloadedState.components.infodropdown.attributes, unit: "3"}
        }
      }
    };

    renderWithProviders(<AweInfoDropdown id="infodropdown"/>, {preloadedState: state});

    const badge = document.querySelector('[data-pc-name="badge"]');
    expect(badge).not.toBeNull();
    expect(badge.textContent).toBe("3");
    expect(badge.className).toContain("p-badge-info");
  });
});
