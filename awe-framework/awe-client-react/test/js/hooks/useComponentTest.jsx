import React from 'react';
import useComponent from '../../../src/hooks/useComponent';
import { renderWithProviders } from '../test-utils';
import { ADD_ACTIONS_TOP } from '../../../src/redux/actions/actions';
import { updateAttributes } from '../../../src/redux/actions/components';
import {act} from "@testing-library/react";
import ComponentRegistry from '../../../src/redux/registry/ComponentRegistry';

// Small harness that just uses the hook
function HookHarness({ id }) {
  useComponent(id);
  return <div data-testid="harness" />;
}

function getAddActionsTopCalls(dispatchSpy) {
  return dispatchSpy.mock.calls.filter(args => args[0] && args[0].type === ADD_ACTIONS_TOP);
}

describe('awe-react-client/test/js/hooks/useComponentTest.jsx', () => {
  const baseAddress = { view: 'tortilla', component: 'comp1' };

  beforeEach(() => {
    ComponentRegistry.clearAll();
  });

  afterEach(() => {
    ComponentRegistry.clearAll();
  });

  it('dispatches a filter action on mount when autoload is true', () => {
    const preloadedState = {
      components: {
        comp1: {
          address: baseAddress,
          attributes: { autoload: true, autorefresh: 0 }
        }
      }
    };

    const { dispatchSpy } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState, spyDispatch: true });

    const calls = getAddActionsTopCalls(dispatchSpy);
    expect(calls.length).toBeGreaterThan(0);
    const action = calls[0][0];
    expect(action.type).toBe(ADD_ACTIONS_TOP);
    expect(Array.isArray(action.payload)).toBe(true);
    expect(action.payload[0].type).toBe('filter');
    expect(action.payload[0].address).toEqual(baseAddress);
  });

  it('does not auto-refresh when autorefresh is 0', () => {
    jest.useFakeTimers();
    try {
      const preloadedState = {
        components: {
          comp1: {
            address: baseAddress,
            attributes: { autoload: false, autorefresh: 0 }
          }
        }
      };

      const { dispatchSpy } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState, spyDispatch: true });

      jest.advanceTimersByTime(3000);

      const calls = getAddActionsTopCalls(dispatchSpy);
      expect(calls.length).toBe(0);
    } finally {
      jest.useRealTimers();
    }
  });

  it('auto-refreshes by dispatching filter periodically when autorefresh > 0', () => {
    jest.useFakeTimers();
    try {
      const preloadedState = {
        components: {
          comp1: {
            address: baseAddress,
            attributes: { autoload: false, autorefresh: 1 }
          }
        }
      };

      const { dispatchSpy } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState, spyDispatch: true });

      // Initially no dispatch yet
      expect(getAddActionsTopCalls(dispatchSpy).length).toBe(0);

      jest.advanceTimersByTime(1000);
      expect(getAddActionsTopCalls(dispatchSpy).length).toBe(1);

      jest.advanceTimersByTime(2000);
      expect(getAddActionsTopCalls(dispatchSpy).length).toBe(3);
    } finally {
      jest.useRealTimers();
    }
  });

  it('updates interval cadence when autorefresh changes in Redux', () => {
    jest.useFakeTimers();
    try {
      const preloadedState = {
        components: {
          comp1: {
            address: baseAddress,
            attributes: { autoload: false, autorefresh: 1 }
          }
        }
      };

      const { dispatchSpy, store } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState, spyDispatch: true });

      // After 1s -> 1 dispatch
      jest.advanceTimersByTime(1000);
      expect(getAddActionsTopCalls(dispatchSpy).length).toBe(1);

      // Change autorefresh to 2 seconds
      act(() => store.dispatch(updateAttributes(baseAddress, { autorefresh: 2 })));

      // Next 1s should not trigger (since now needs 2s)
      jest.advanceTimersByTime(1000);
      expect(getAddActionsTopCalls(dispatchSpy).length).toBe(1);

      // After another 1s (total 2s since change) -> +1 dispatch (now 2 total)
      jest.advanceTimersByTime(1000);
      expect(getAddActionsTopCalls(dispatchSpy).length).toBe(2);
    } finally {
      jest.useRealTimers();
    }
  });

  it('cleans interval on unmount so no more dispatches happen after', () => {
    jest.useFakeTimers();
    try {
      const preloadedState = {
        components: {
          comp1: {
            address: baseAddress,
            attributes: { autoload: false, autorefresh: 1 }
          }
        }
      };

      const { dispatchSpy, unmount } = renderWithProviders(<HookHarness id="comp1" />, { preloadedState, spyDispatch: true });

      jest.advanceTimersByTime(1000);
      const before = getAddActionsTopCalls(dispatchSpy).length; // should be 1
      expect(before).toBe(1);

      // Unmount and advance clock; no further calls should be recorded
      unmount();
      jest.advanceTimersByTime(3000);
      const after = getAddActionsTopCalls(dispatchSpy).length;
      expect(after).toBe(before);
    } finally {
      jest.useRealTimers();
    }
  });
});
