import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputDate from "../../../src/criteria/AweInputDate";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputDateTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      date: {
        address: {component: 'date', view: 'report'},
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

  it('renders Awe Input Date component', () => {
    renderWithProviders(<AweInputDate id="date"/>, {preloadedState});

    // check
    expect(screen.findByRole("combobox")).not.toBeNull();
  });

});
