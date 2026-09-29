import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputPassword from "../../../src/criteria/AweInputPassword";

describe('awe-react-client/test/js/criteria/AweInputPasswordTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      password: {
        address: {component: 'password', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          helpImage: "test"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Input Password component', () => {
    renderWithProviders(<AweInputPassword id="password"/>, {preloadedState});

    // check
    expect(document.querySelector("input#password")).not.toBeNull();
  });

});
