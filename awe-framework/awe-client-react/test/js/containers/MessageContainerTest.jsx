import React from 'react';
import { act, waitFor } from '@testing-library/react';
import {DEFAULT_SETTINGS, updateSettings} from "../../../src/redux/actions/settings";
import {renderWithProviders} from "../test-utils";
import MessageContainer from "../../../src/containers/MessageContainer";
import { addMessage } from "../../../src/redux/actions/messages";
import { MemoryRouter } from 'react-router';

const mockShow = jest.fn();
const mockRemove = jest.fn();

jest.mock('primereact/toast', () => {
  const React = require('react');

  return {
    Toast: React.forwardRef((props, ref) => {
      React.useImperativeHandle(ref, () => ({
        show: mockShow,
        remove: mockRemove
      }), []);

      return <div className="p-toast" data-position={props.position}/>;
    })
  };
});

describe('awe-react-client/test/js/containers/MessageContainerTest.jsx', () => {

  const preloadedState = {
    settings: DEFAULT_SETTINGS
  };

  beforeEach(() => {
    jest.spyOn(console, 'warn').mockImplementation(() => {});
    jest.spyOn(console, 'error').mockImplementation(() => {});
    mockShow.mockClear();
    mockRemove.mockClear();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders message container', () => {
    renderWithProviders(<MessageContainer/>, {preloadedState:{...preloadedState, messages: {showing: []}}});

    // check
    expect(document.querySelector(".p-toast")).not.toBeNull();
  });

  it('renders a message container with messages', () => {
    renderWithProviders(<MessageContainer/>, {preloadedState:{...preloadedState, messages: {showing: [
      {
        severity:"warn", summary:"Invalid credentials", detail:"The credentials entered for the user -test- are not valid",
        sticky:false, life:4000, id:1,  closable:true, show:true
      }
      ]}}});

    // check
    expect(document.querySelector(".p-toast")).not.toBeNull();
  });

  it('does not show the same toast twice after it is marked as shown', async () => {
    const message = {
      severity: 'warn', summary: 'Invalid credentials', detail: 'The credentials entered for the user -test- are not valid',
      sticky: false, life: 4000, id: 1, closable: true
    };

    const {store} = renderWithProviders(<MessageContainer/>, {
      preloadedState: {...preloadedState, messages: {showing: [message]}}
    });

    await waitFor(() => {
      expect(mockShow).toHaveBeenCalledTimes(1);
      expect(mockShow).toHaveBeenCalledWith([message]);
      expect(store.getState().messages.showing[0].show).toBe(true);
    });

    store.dispatch(updateSettings({ messagePosition: 'bottom-left' }));

    await waitFor(() => {
      expect(mockShow).toHaveBeenCalledTimes(1);
    });
  });
});

// Regression: Issue 667 — message must be shown under a render storm even when
// requestAnimationFrame never fires (the fix calls Toast.show synchronously inside
// the effect rather than scheduling it via rAF).
describe('Issue 667 regression: message shown under render storm without requestAnimationFrame', () => {
  function preloaded() {
    return {
      settings: {
        actionsStack: 0, helpTimeout: 0, language: 'en', theme: 'default',
        messagePosition: 'top-right',
        messageTimeout: { ok: 3000, info: 3000, error: 0, warning: 5000 }
      },
      messages: { showing: [], defined: { base: {}, report: {} }, confirm: undefined }
    };
  }

  beforeEach(() => {
    mockShow.mockClear();
    mockRemove.mockClear();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('marks a backend message as shown even when requestAnimationFrame never fires and settings churn re-renders concurrently', async () => {
    // Disable rAF to verify the fix does not depend on animation frames.
    const realRAF = window.requestAnimationFrame;
    const realCAF = window.cancelAnimationFrame;
    window.requestAnimationFrame = () => 0;
    window.cancelAnimationFrame = () => {};

    try {
      const { store } = renderWithProviders(
        <MemoryRouter><MessageContainer /></MemoryRouter>,
        { preloadedState: preloaded() }
      );

      await act(async () => {
        store.dispatch(addMessage({
          severity: 'error', summary: 'Initial load failed', detail: 'Backend action should be shown',
          id: 0, life: 0, closable: true, action: { type: 'message' }
        }));
      });

      // Simulate a render storm (as during screen load).
      await act(async () => { store.dispatch(updateSettings({ actionsStack: 0, __churn: 1 })); });
      await act(async () => { store.dispatch(updateSettings({ actionsStack: 0, __churn: 2 })); });

      const showing = store.getState().messages.showing.map((m) => ({ id: m.id, show: m.show }));

      // The message must be marked shown despite rAF never firing.
      expect(showing[0].show).toBe(true);

      // The toast show API must have been called (proves the message reached the UI layer).
      expect(mockShow).toHaveBeenCalled();
    } finally {
      window.requestAnimationFrame = realRAF;
      window.cancelAnimationFrame = realCAF;
    }
  });
});
