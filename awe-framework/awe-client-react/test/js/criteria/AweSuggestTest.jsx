import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {renderWithProviders} from "../test-utils";
import AweSuggest from "../../../src/criteria/AweSuggest";
import {ADD_ACTIONS_TOP} from "../../../src/redux/actions/actions";
import { act, waitFor } from "@testing-library/react";

describe('awe-react-client/test/js/criteria/AweSuggestTest.jsx', () => {

  const baseState = {
    settings: DEFAULT_SETTINGS,
    components: {
      suggest: {
        address: {component: 'suggest', view: 'report'},
        model: {values: [{label: 'test', value: 'test', selected: true}]},
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

  it('renders Awe Suggest component', () => {
    renderWithProviders(<AweSuggest id="suggest"/>, {preloadedState: baseState});

    // check
    expect(document.querySelector("#suggest input[aria-autocomplete]")).not.toBeNull();
  });

  it('renders a skeleton when address is missing', () => {
    const preloadedState = {
      ...baseState,
      components: {
        suggest: {
          // no address
          model: { values: [{ value: 'X', selected: true }] },
          attributes: { placeholder: 'ph' },
          validationRules: { required: false }
        }
      }
    };

    renderWithProviders(<AweSuggest id="suggest" />, { preloadedState });

    // Should render a primereact Skeleton
    expect(document.querySelector('.p-skeleton')).not.toBeNull();
  });

});
