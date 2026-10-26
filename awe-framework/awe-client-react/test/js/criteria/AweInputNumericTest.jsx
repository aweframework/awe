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

  it('commits zero as a valid value for a required numeric criterion', async () => {
    const numericState = {
      ...preloadedState,
      components: {
        numeric: {
          ...preloadedState.components.numeric,
          model: { values: [] },
          validationRules: { required: true, ge: 0 },
          attributes: {
            ...preloadedState.components.numeric.attributes,
            numberFormat: { vMin: '0', mDec: 2, aPad: true }
          }
        }
      }
    };

    const { store } = renderWithProviders(<AweInputNumeric id="numeric"/>, { preloadedState: numericState });
    const input = document.querySelector("input.p-inputnumber-input");

    fireEvent.change(input, { target: { value: '0' } });
    fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', charCode: 13 });

    await waitFor(() => {
      expect(store.getState().components.numeric.model.values[0]).toEqual({
        value: 0,
        label: '0.00',
        selected: true
      });
    });
  });

  it('commits the typed value with Enter and cancels the event so the ancestors do not receive it', async () => {
    const onParentKeyDown = jest.fn();
    const onWindowKeyDown = jest.fn();
    window.addEventListener("keydown", onWindowKeyDown);
    const { store } = renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputNumeric id="numeric"/></div>, { preloadedState });
    const input = document.querySelector("input.p-inputnumber-input");

    fireEvent.change(input, { target: { value: '7' } });
    const notPrevented = fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
    window.removeEventListener("keydown", onWindowKeyDown);

    expect(notPrevented).toBe(false);
    expect(onParentKeyDown).not.toHaveBeenCalled();
    expect(onWindowKeyDown).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(store.getState().components.numeric.model.values[0]).toEqual(expect.objectContaining({ value: 7, selected: true }));
    });
  });

  it('lets another key reach the ancestors', () => {
    const onParentKeyDown = jest.fn();
    renderWithProviders(<div onKeyDown={onParentKeyDown}><AweInputNumeric id="numeric"/></div>, { preloadedState });

    fireEvent.keyDown(document.querySelector("input.p-inputnumber-input"), { key: 'ArrowLeft', code: 'ArrowLeft' });
    expect(onParentKeyDown).toHaveBeenCalledTimes(1);
  });

});
