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

});
