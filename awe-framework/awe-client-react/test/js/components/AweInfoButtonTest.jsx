import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInfoButton from "../../../src/components/AweInfoButton";

describe('awe-react-client/test/js/criteria/AweInfoButtonTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      infobutton: {
        address: {component: 'infobutton', view: 'report'},
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

  it('renders Info button component', () => {
    renderWithProviders(<AweInfoButton id="infobutton"/>, {preloadedState});

    // check
    expect(document.querySelector("button#infobutton")).not.toBeNull();
  });

});
