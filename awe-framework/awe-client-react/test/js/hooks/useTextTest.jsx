import React from 'react';
import { renderWithProviders } from '../test-utils';
import {screen, fireEvent, act} from '@testing-library/react';
import useText from '../../../src/hooks/useText';
import { updateModel } from '../../../src/redux/actions/components';
import { ADD_ACTIONS_TOP } from '../../../src/redux/actions/actions';

function HookHarness({ id }) {
  const { value, onChange, onBlur, onSubmit } = useText(id);
  return (
    <div>
      <input data-testid="text-input" value={value || ''} onChange={onChange} onBlur={onBlur} />
      <button data-testid="submit" onClick={onSubmit}>Submit</button>
      <div data-testid="shown-value">{value}</div>
    </div>
  );
}

function getPreloaded(id = 'comp1', extra = {}) {
  const base = {
    components: {
      [id]: {
        address: { view: 'view1', component: id },
        model: { values: [{ value: 'hello', selected: true }] },
        attributes: { },
        validationRules: { },
        context: { source: ['root', 'view1', id] }
      }
    },
    settings: {}
  };
  return { ...base, ...extra };
}

function findAddActionsTop(dispatchSpy) {
  return dispatchSpy.mock.calls.map(args => args[0]).filter(a => a && a.type === ADD_ACTIONS_TOP);
}

describe('awe-react-client/test/js/hooks/useTextTest.jsx', () => {
  it('initializes value from selected model values', () => {
    const preloadedState = getPreloaded();
    renderWithProviders(<HookHarness id="comp1" />, { preloadedState });

    expect(screen.getByTestId('shown-value').textContent).toBe('hello');
  });

  it('initializes value from empty state', () => {
    const preloadedState = {
      components: {
        comp1: {}
      },
      settings: {}
    };
    renderWithProviders(<HookHarness id="comp1" />, { preloadedState });

    expect(screen.getByTestId('shown-value').textContent).toBe('');
  });

  it('onChange when not focused dispatches model update immediately (autofill path)', () => {
    const preloadedState = getPreloaded();
    const { store } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState });

    const input = screen.getByTestId('text-input');
    // ensure not focused
    expect(document.activeElement).not.toBe(input);

    fireEvent.change(input, { target: { value: 'auto' } });

    // Thunk should update the model in store
    const state = store.getState();
    const values = state.components.comp1.model.values;
    expect(values.length).toBe(1);
    expect(values[0].value).toBe('auto');
    expect(values[0].selected).toBe(true);
  });

  it('onChange when focused updates local value and onBlur persists to model', () => {
    const preloadedState = getPreloaded();
    const { store } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState });

    const input = screen.getByTestId('text-input');

    // Focus and type -> local state only
    input.focus();
    expect(document.activeElement).toBe(input);
    fireEvent.change(input, { target: { value: 'typed' } });

    // Local value updated in DOM
    expect(screen.getByTestId('shown-value').textContent).toBe('typed');

    // Model not yet updated
    let state = store.getState();
    expect(state.components.comp1.model.values[0].value).toBe('hello');

    // Blur -> persists
    fireEvent.blur(input);
    state = store.getState();
    expect(state.components.comp1.model.values[0].value).toBe('typed');
  });

  it('syncs local value when external model changes and not writing', () => {
    const preloadedState = getPreloaded();
    const { store } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState });

    // External update
    act(() => store.dispatch(updateModel({ view: 'view1', component: 'comp1' }, { values: [{ value: 'server', selected: true }] })));

    // The hook effect should sync value
    expect(screen.getByTestId('shown-value').textContent).toBe('server');
  });

  it('onSubmit triggers store and dispatches submit action', () => {
    const preloadedState = getPreloaded();
    const { dispatchSpy, store } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState, spyDispatch: true });

    const input = screen.getByTestId('text-input');
    // Change locally (focused) and submit
    input.focus();
    fireEvent.change(input, { target: { value: 'toSubmit' } });

    fireEvent.click(screen.getByTestId('submit'));

    // After submit, model should be stored (onBlur called inside)
    const state = store.getState();
    expect(state.components.comp1.model.values[0].value).toBe('toSubmit');

    const submitCalls = findAddActionsTop(dispatchSpy);
    expect(submitCalls.length).toBeGreaterThan(0);
    const action = submitCalls[submitCalls.length - 1];
    expect(Array.isArray(action.payload)).toBe(true);
    expect(action.payload[0].type).toBe('submit');
    expect(action.payload[0].address).toEqual({ view: 'view1', component: 'comp1' });
  });

  it('does not crash when address is missing and still exposes API', () => {
    const preloadedState = {
      components: {
        compX: {
          // no address
          model: { values: [{ value: 'x', selected: true }] },
          attributes: {},
          validationRules: {},
          context: { source: ['root', 'view1', 'compX'] }
        }
      },
      settings: {}
    };

    const { store } = renderWithProviders(<HookHarness id="compX" />, { preloadedState });

    // Interact
    const input = screen.getByTestId('text-input');
    fireEvent.change(input, { target: { value: 'y' } });
    fireEvent.blur(input);

    // Since address is missing, updateThunk early-returns; store should keep previous value
    const state = store.getState();
    expect(state.components.compX.model.values[0].value).toBe('x');
  });
});
