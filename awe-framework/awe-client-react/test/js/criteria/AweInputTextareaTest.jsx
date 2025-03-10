import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputTextarea from "../../../src/criteria/AweInputTextarea";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputTextareaTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      textarea: {
        address: {component: 'textarea', view: 'report'},
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

  it('renders Awe Input Textarea component', () => {
    renderWithProviders(<AweInputTextarea id="textarea"/>, {preloadedState});

    // check
    expect(document.querySelector("textarea#textarea")).not.toBeNull();
    expect(screen.findByPlaceholderText("placeholder")).not.toBeNull();
  });

});
