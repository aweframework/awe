import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputText from "../../../src/criteria/AweInputText";

describe('awe-react-client/test/js/criteria/AweInputTextTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      text: {
        address: {component: 'text', view: 'report'},
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

  it('renders Awe Input Text component', () => {
    renderWithProviders(<AweInputText id="text"/>, {preloadedState});

    // check
    expect(document.querySelector("input#text")).not.toBeNull();
  });

});
