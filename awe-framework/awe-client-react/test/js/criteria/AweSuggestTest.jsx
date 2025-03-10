import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSuggest from "../../../src/criteria/AweSuggest";

describe('awe-react-client/test/js/criteria/AweSuggestTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      suggest: {
        address: {component: 'suggest', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
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

  it('renders Awe Suggest component', () => {
    renderWithProviders(<AweSuggest id="suggest"/>, {preloadedState});

    // check
    expect(document.querySelector("#suggest input[aria-autocomplete]")).not.toBeNull();
  });

});
