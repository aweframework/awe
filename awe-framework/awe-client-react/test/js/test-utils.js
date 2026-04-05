import React from 'react';
import { render } from '@testing-library/react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
// As a basic setup, import your same slice reducers
import createRootReducer from '../../src/redux/reducers';

/**
 * Render a UI wrapped with a Redux Provider and return RTL utils plus the store.
 *
 * Options:
 * - preloadedState: initial state for Redux store
 * - store: provide an existing store instance (optional)
 * - spyDispatch: when true, also returns a dispatchSpy that spies on store.dispatch.
 *   This is the recommended way to assert dispatches in tests instead of mocking useDispatch.
 *   It supports both Jest (jest.spyOn) and Jasmine (spyOn(...).and.callThrough()).
 *
 * Example (Jest):
 *   const { store, dispatchSpy } = renderWithProviders(<MyComp />, { spyDispatch: true });
 *   // trigger something ...
 *   expect(dispatchSpy).toHaveBeenCalled();
 *
 * Example (Karma/Jasmine):
 *   const { store, dispatchSpy } = renderWithProviders(<MyComp />, { spyDispatch: true });
 *   expect(dispatchSpy).toHaveBeenCalled();
 */
export function renderWithProviders(
  ui,
  {
    preloadedState = {},
    // Automatically create a store instance if no store was passed in
    store = configureStore({
      reducer: createRootReducer({}),
      preloadedState,
      middleware: (getDefaultMiddleware) => getDefaultMiddleware({
        serializableCheck: {
          ignoredActions: ['UPDATE_ATTRIBUTES'],
          ignoredActionPaths: [/^data\.(onChange|aggregators|renderers|tableColorScaleGenerator)$/],
          ignoredPaths: [/^components\..*\.attributes\.(onChange|aggregators|renderers|tableColorScaleGenerator)$/]
        }
      })
    }),
    spyDispatch = false,
    ...renderOptions
  } = {}
) {
  function Wrapper({ children }) {
    return <Provider store={store}>{children}</Provider>;
  }

  let dispatchSpy = null;
  if (spyDispatch) {
    if (typeof jest !== 'undefined' && typeof jest.spyOn === 'function') {
      dispatchSpy = jest.spyOn(store, 'dispatch');
    } else {
      try {
        dispatchSpy = globalThis.spyOn(store, 'dispatch');
        if (dispatchSpy.and && typeof dispatchSpy.and.callThrough === 'function') {
          dispatchSpy.and.callThrough();
        }
      } catch (e) {
        // ignore
      }
    }
  }

  const rendered = render(ui, { wrapper: Wrapper, ...renderOptions });
  // Return an object with the store, optional dispatchSpy and all of RTL's query functions
  return { store, dispatchSpy, ...rendered };
}
