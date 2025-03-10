import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputNumeric from "../../../src/criteria/AweInputNumeric";

describe('awe-react-client/test/js/criteria/AweInputNumericTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      numeric: {
        address: {component: 'numeric', view: 'report'},
        model: {values: [{value: '12.2', selected: true}]},
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

  it('renders Awe Input Numeric component', () => {
    renderWithProviders(<AweInputNumeric id="numeric"/>, {preloadedState});

    // check
    expect(document.querySelector("input.p-inputnumber-input")).not.toBeNull();
  });

});
