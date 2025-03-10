import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweButtonRadio from "../../../src/criteria/AweButtonRadio";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweButtonRadioTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      radio: {
        address: {component: 'radio', view: 'report'},
        model: {values: [{label: 'test', value: '1', selected: true}]},
        attributes: {
          placeholder: "placeholder",
          readonly: false,
          label: "test"
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Button Radio component', () => {
    renderWithProviders(<AweButtonRadio id="radio"/>, {preloadedState});

    // check
    expect(screen.findByRole("button")).not.toBeNull();
  });

});
