import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import {cleanup, screen} from "@testing-library/react";
import AwePicklist from "../../../src/criteria/AwePicklist";

describe('awe-react-client/test/js/criteria/AwePicklistTest.jsx', () => {

  afterAll(cleanup);

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      picklist: {
        address: {component: 'picklist', view: 'report'},
        model: {values: [{label: 'textToCheck', value: 'test', selected: true}]},
        attributes: {
          readonly: false,
        },
        validationRules: {
          required: false
        },
        specificAttributes: {sort: []}
      }
    }
  };

  it('renders Awe Picklist component', async () => {
    renderWithProviders(<AwePicklist id="picklist"/>, {preloadedState});

    // check: a picklist has two lists, the source one and the target one
    expect(await screen.findAllByRole("listbox")).toHaveLength(2);
  });

});
