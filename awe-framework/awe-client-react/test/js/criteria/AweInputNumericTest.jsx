import {DEFAULT_SETTINGS} from "../../../src/redux/actions/settings";
import React from "react";
import {fireEvent, waitFor} from "@testing-library/react";
import {renderWithProviders} from "../test-utils";
import AweInputNumeric from "../../../src/criteria/AweInputNumeric";

describe('awe-react-client/test/js/criteria/AweInputNumericTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS,
    components: {
      numeric: {
        address: {component: 'numeric', view: 'report'},
        model: {values: [{value: '12.2', selected: true}]},
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

  it('renders Awe Input Numeric component', () => {
    renderWithProviders(<AweInputNumeric id="numeric"/>, {preloadedState});

    // check
    expect(document.querySelector("input.p-inputnumber-input")).not.toBeNull();
  });

  it('keeps decimal separator semantics for partial numeric formats above one thousand', async () => {
    const numericState = {
      ...preloadedState,
      components: {
        numeric: {
          ...preloadedState.components.numeric,
          model: { values: [{ value: '', selected: true }] },
          attributes: {
            ...preloadedState.components.numeric.attributes,
            numberFormat: { min: 0, precision: 2 }
          }
        }
      }
    };

    const { store } = renderWithProviders(<AweInputNumeric id="numeric"/>, { preloadedState: numericState });
    const input = document.querySelector("input.p-inputnumber-input");

    fireEvent.change(input, { target: { value: '1000.5' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', charCode: 13 });

    await waitFor(() => {
      expect(store.getState().components.numeric.model.values[0]).toEqual({
        value: 1000.5,
        label: '1,000.50',
        selected: true
      });
    });
  });

});
