import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputTime from "../../../src/criteria/AweInputTime";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputTimeTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      time: {
        address: {component: 'time', view: 'report'},
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

  it('renders Awe Input Time component', () => {
    renderWithProviders(<AweInputTime id="time"/>, {preloadedState});

    // check
    expect(screen.findByRole("combobox")).not.toBeNull();
  });

});
