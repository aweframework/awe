import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweInputFilteredDate from "../../../src/criteria/AweInputFilteredDate";
import {cleanup, screen} from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweInputFilteredDateTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      filteredDate: {
        address: {component: 'filteredDate', view: 'report'},
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

  it('renders Awe Input FilteredDate component', async () => {
    renderWithProviders(<AweInputFilteredDate id="filteredDate"/>, {preloadedState});

    // check
    expect(await screen.findByRole("combobox")).not.toBeNull();
  });

});
