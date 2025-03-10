import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputColor from "../../../src/criteria/AweInputColor";

describe('awe-react-client/test/js/criteria/AweInputColorTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      color: {
        address: {component: 'color', view: 'report'},
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

  it('renders Awe Input Color component', () => {
    renderWithProviders(<AweInputColor id="color"/>, {preloadedState});

    // check
    expect(document.querySelector("input#color")).not.toBeNull();
  });

});
