import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSelect from "../../../src/criteria/AweSelect";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweSelectTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      select: {
        address: {component: 'select', view: 'report'},
        model: {values: [{label: 'textToCheck', value: 'test', selected: true}]},
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

  it('renders Awe Select component', () => {
    renderWithProviders(<AweSelect id="select"/>, {preloadedState});

    // check
    expect(screen.findByRole("button")).not.toBeNull();
  });

});
