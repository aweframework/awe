import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputRadio from "../../../src/criteria/AweInputRadio";
import {cleanup, screen} from '@testing-library/react';

describe('awe-react-client/test/js/criteria/AweInputRadioTest.jsx', () => {

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

  it('renders Awe Input Radio component', async () => {
    renderWithProviders(<AweInputRadio id="radio"/>, {preloadedState});

    // check
    expect(await screen.findByText("test")).not.toBeNull();
  });

});
