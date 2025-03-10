import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweImage from "../../../src/components/AweImage";

describe('awe-react-client/test/js/criteria/AweImageTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      image: {
        address: {component: 'image', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "image",
          title: "image",
          url: "helpImage",
          alternateUrl: "alternateURL"
        },
        validationRules: {
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Image component', () => {
    renderWithProviders(<AweImage id="image"/>, {preloadedState});

    // check
    expect(document.querySelector("img")).not.toBeNull();
  });

});
