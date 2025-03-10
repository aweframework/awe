import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSelectMultiple from "../../../src/criteria/AweSelectMultiple";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweSelectMultipleTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      "select-multiple": {
        address: {component: 'select-multiple', view: 'report'},
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

  it('renders Awe Select Multiple component', () => {
    renderWithProviders(<AweSelectMultiple id="select-multiple"/>, {preloadedState});

    // check
    expect(screen.findByText("test")).not.toBeNull();
    expect(screen.findByText("tutu")).not.toBeNull();
  });

});
