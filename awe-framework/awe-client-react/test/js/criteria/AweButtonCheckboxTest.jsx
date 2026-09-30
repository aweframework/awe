import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweButtonCheckbox from "../../../src/criteria/AweButtonCheckbox";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweButtonCheckboxTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      checkbox: {
        address: {component: 'checkbox', view: 'report'},
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

  it('renders Awe Button Checkbox component', async () => {
    renderWithProviders(<AweButtonCheckbox id="checkbox"/>, {preloadedState});

    // check
    expect(await screen.findByRole("button")).not.toBeNull();
  });

});
