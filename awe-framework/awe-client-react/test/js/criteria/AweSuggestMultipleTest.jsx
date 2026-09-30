import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSuggestMultiple from "../../../src/criteria/AweSuggestMultiple";

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

});
